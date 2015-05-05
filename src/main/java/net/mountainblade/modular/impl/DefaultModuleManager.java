package net.mountainblade.modular.impl;

import com.google.common.base.Optional;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.TLinkedHashSet;
import net.mountainblade.modular.Filter;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.ModuleManager;
import net.mountainblade.modular.ModuleState;
import net.mountainblade.modular.UriHelper;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Shutdown;
import net.mountainblade.modular.impl.location.ClassLocation;
import net.mountainblade.modular.impl.resolver.ClassResolver;
import net.mountainblade.modular.impl.resolver.ClasspathResolver;
import net.mountainblade.modular.impl.resolver.JarResolver;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the default implementation of a {@link net.mountainblade.modular.ModuleManager}.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class DefaultModuleManager implements ModuleManager {
    private static final Logger LOG = Logger.getLogger(DefaultModuleManager.class.getName());

    /** A collection of all destroyable objects that get wiped once the manager shuts down */
    private final Collection<Destroyable> destroyables;

    /** The classworld instance that contains all the class information, used for better/easier loading */
    private final ClassWorld classWorld;

    /** The module registry that holds all the instances */
    private final ModuleRegistry registry;

    /** The automagical injector */
    private final Injector injector;

    /** The module loader that loads modules and class paths */
    private final ModuleLoader loader;

    /** A list of all class resolvers, can be accessed to add support for new implementations */
    private final Collection<ClassResolver> locators;


    public DefaultModuleManager() {
        destroyables = new LinkedList<>();

        // Stuff we need
        classWorld = new ClassWorld();
        registry   = new DefaultModuleRegistry();
        injector   = new Injector(registry);
        loader     = new ModuleLoader(classWorld, registry, injector);
        locators   = new TLinkedHashSet<>();

        // Add defaults
        getLocators().add(new ClasspathResolver());
        getLocators().add(new JarResolver());

        // Also register ourselves so other modules can use this as implementation via injection
        registry.addGhostModule(ModuleManager.class, this, new MavenModuleInformation());

        // Add destroyable objects for our shutdown
        destroyables.add(registry);
        destroyables.add(loader);
    }

    protected ClassWorld getClassWorld() {
        return classWorld;
    }

    protected ModuleRegistry getRegistry() {
        return registry;
    }

    public Injector getInjector() {
        return injector;
    }

    public ModuleLoader getLoader() {
        return loader;
    }

    public Collection<ClassResolver> getLocators() {
        return locators;
    }

    @Override
    public <T extends Module> T provideSimple(T module) {
        return provide(module, false, registry, loader);
    }

    @Override
    public <T extends Module> T provide(T module) {
        return provide(module, true, registry, loader);
    }

    protected <T extends Module> T provide(T module, boolean inject, ModuleRegistry registry, ModuleLoader loader) {
        if (module == null) {
            LOG.warning("Provided with null instance, will not add to registry");
            return null;
        }

        // Get class entry and implementation annotation
        ModuleLoader.ClassEntry entry = loader.getClassEntry(module.getClass());
        if (entry == null) {
            LOG.warning("Provided with invalid module, will not at to registry");
            return null;
        }

        Implementation annotation = entry.getAnnotation();

        // Create registry entry
        ModuleInformationImpl information = new ModuleInformationImpl(annotation.version(), annotation.authors());
        ModuleRegistry.Entry moduleEntry = registry.createEntry(entry.getModule(), information);

        // Inject dependencies if specified
        if (inject) {
            loader.injectAndInitialize(this, module, information, moduleEntry);
        }

        // Register module
        loader.registerEntry(entry, module, information, moduleEntry);

        return module;
    }

    @Override
    public Collection<Module> loadModules(URI uri, Filter... filters) {
        return loadModules(uri, loader, filters);
    }

    protected Collection<Module> loadModules(URI uri, ModuleLoader loader, Filter... filters) {
        // Locate stuff from URI - using different providers (class pat, file, ...)
        final Collection<ClassLocation> located = new LinkedList<>();

        for (ClassResolver locator : locators) {
            if (!locator.handles(uri)) {
                continue;
            }

            // Register all found locations
            for (ClassLocation location : locator.resolve(uri)) {
                registerLocation(location);
                located.add(location);
            }
        }

        // Collect a bunch of classes that are Modules, plus the interface they're implementing
        final Collection<ModuleLoader.ClassEntry> candidates = loader.getCandidatesWithPattern(
                (UriHelper.everything().equals(uri)) ? null : UriHelper.createPattern(uri), located);

        // Apply filters - we do the filters first, so in case we don't have any, we don't iterate over all the entries
        final Set<ModuleLoader.ClassEntry> entries = new TLinkedHashSet<>(candidates);
        Iterator<ModuleLoader.ClassEntry> iterator;

        for (Filter filter : filters) {
            iterator = entries.iterator();

            while (iterator.hasNext()) {
                final ModuleLoader.ClassEntry classEntry = iterator.next();

                if (!filter.retain(classEntry)) {
                    iterator.remove();
                }
            }
        }

        // Prepare topological sort so we don't have trouble with dependencies
        Map<ModuleLoader.ClassEntry, TopologicalSortedList.Node<ModuleLoader.ClassEntry>> nodes = new THashMap<>();
        final TopologicalSortedList<ModuleLoader.ClassEntry> sortedCandidates = new TopologicalSortedList<>();

        for (ModuleLoader.ClassEntry classEntry : entries) {
            TopologicalSortedList.Node<ModuleLoader.ClassEntry> node = nodes.get(classEntry);

            if (node == null) {
                node = sortedCandidates.addNode(classEntry);
                nodes.put(classEntry, node);
            }

            for (Injector.Entry dependencyEntry : classEntry.getDependencies()) {
                // Skip the ones we don't need
                if (!(dependencyEntry instanceof Injector.ModuleEntry)) {
                    continue;
                }

                final Class<? extends Module> dependency = ((Injector.ModuleEntry) dependencyEntry).getDependency();
                ModuleLoader.ClassEntry depClassEntry = loader.getClassEntry(dependency);

                if (depClassEntry != null) {
                    TopologicalSortedList.Node<ModuleLoader.ClassEntry> depNode = nodes.get(depClassEntry);

                    if (depNode == null) {
                        depNode = sortedCandidates.addNode(depClassEntry);
                        nodes.put(depClassEntry, depNode);
                    }

                    depNode.isRequiredBefore(node);
                    continue;
                }

                LOG.warning("Could not get class entry for dependency: " + dependency);
            }
        }

        // Execute the sort
        final Collection<Module> modules = new LinkedList<>();

        try {
            sortedCandidates.sort();

        } catch (TopologicalSortedList.CycleException e) {
            LOG.log(Level.WARNING, "Error sorting module load order, found dependency cycle", e);
            return modules;
        }

        // Load all, sorted modules using our loader
        for (TopologicalSortedList.Node<ModuleLoader.ClassEntry> candidate : sortedCandidates) {
            final Module module = loader.loadModule(this, candidate.getValue());

            if (module == null) {
                LOG.warning("Could not load modules properly, cancelling loading procedure");
                break;
            }

            modules.add(module);
        }

        return modules;
    }

    @Override
    public <M extends Module> Optional<M> getModule(Class<M> module) {
        return Optional.fromNullable(registry.getModule(module));
    }

    @Override
    public Optional<ModuleInformation> getInformation(Class<? extends Module> module) {
        return Optional.fromNullable(registry.getInformation(module));
    }

    @Override
    public Collection<Module> getModules() {
        return registry.getModules();
    }

    @Override
    public void shutdown() {
        // Send shut down signal to all registered modules
        shutdown(getRegistry().getModuleCollection().iterator(), registry, loader);

        // And destroy what we can
        for (Destroyable destroyable : destroyables) {
            destroyable.destroy();
        }
    }

    protected void shutdown(Iterator<Module> iterator, ModuleRegistry registry, ModuleLoader loader) {
        while (iterator.hasNext()) {
            Module module = iterator.next();

            // Get module entry
            ModuleRegistry.Entry entry = registry.getEntry(loader.getClassEntry(module.getClass()).getModule());
            if (entry == null) {
                LOG.warning("Unable to set state to shut down: Could not find entry for module: " + module);
                continue;
            }

            // Skip already shut down modules
            ModuleInformation information = entry.getInformation();
            if (ModuleState.SHUTDOWN.equals(information.getState())) {
                continue;
            }

            LOG.fine("Shutting down " + module.getClass().getName());

            // Call shutdown function
            try {
                Annotations.call(module, Shutdown.class, 0, new Class[]{ModuleManager.class}, this);

            } catch (IllegalAccessException | InvocationTargetException e) {
                LOG.log(Level.WARNING, "Could not invoke shutdown method on module: " + module, e);
            }

            // Set state to "shutdown"
            if (information instanceof ModuleInformationImpl) {
                ((ModuleInformationImpl) information).setState(ModuleState.SHUTDOWN);
            }
        }
    }

    /**
     * Registers a new classpath location.
     *
     * @param location The location to register
     */
    public void registerLocation(ClassLocation location) {
        try {
            classWorld.newRealm(location.getRealm(), getClass().getClassLoader()).addURL(location.getUrl());

        } catch (DuplicateRealmException ignore) {
            // Happens for (local) realms ...
        }
    }

}
