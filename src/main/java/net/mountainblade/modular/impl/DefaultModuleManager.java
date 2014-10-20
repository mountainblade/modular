package net.mountainblade.modular.impl;

import com.google.common.base.Optional;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import lombok.Getter;
import lombok.extern.java.Log;
import net.mountainblade.modular.*;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Shutdown;
import net.mountainblade.modular.impl.location.ClassLocation;
import net.mountainblade.modular.impl.location.ClasspathLocation;
import net.mountainblade.modular.impl.resolver.ClassResolver;
import net.mountainblade.modular.impl.resolver.ClasspathResolver;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

/**
 * Represents the default implementation of a {@link net.mountainblade.modular.ModuleManager}.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Log
public final class DefaultModuleManager implements ModuleManager {
    /** A collection of all destroyable objects that get wiped once the manager shuts down */
    private final Collection<Destroyable> destroyables;

    /** The module registry that holds all the instances */
    private final ModuleRegistry registry;

    /** The module loader that loads modules and class paths */
    private final ModuleLoader loader;

    /** A list of all class resolvers, can be accessed to add support for new implementations */
    @Getter
    private final Collection<ClassResolver> locators;

    private final ClassWorld classWorld;
    private final JarCache jarCache;


    public DefaultModuleManager() {
        destroyables = new LinkedList<>();

        // Stuff we need
        classWorld = new ClassWorld();

        registry   = new ModuleRegistry();
        loader     = new ModuleLoader(classWorld, registry);
        locators   = new THashSet<>();
        jarCache   = new JarCache();

        // Add defaults
        getLocators().add(new ClasspathResolver());

        // Also register ourselves so other modules can use this as implementation via injection
        registry.addGhostModule(ModuleManager.class, this, new MavenModuleInformation());

        // Add destroyable objects for our shutdown
        destroyables.add(registry);
        destroyables.add(loader);
    }

    @Override
    public void provideSimple(Module module) {
        provide(module, false);
    }

    @Override
    public void provide(Module module) {
        provide(module, true);
    }

    private void provide(Module module, boolean inject) {
        if (module == null) {
            log.warning("Provided with null instance, will not add to registry");
            return;
        }

        // Get class entry and implementation annotation
        ModuleLoader.ClassEntry entry = loader.getClassEntry(module.getClass());
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
    }

    @Override
    public Collection<Module> loadModules(URI uri) {
        // Locate stuff from URI - using different providers (class pat, file, ...)
        Collection<ClassLocation> located = new LinkedList<>();

        for (ClassResolver locator : locators) {
            if (!locator.handles(uri)) {
                continue;
            }

            // Register all found locations
            for (ClasspathLocation location : locator.resolve(uri)) {
                registerLocation(location);
                located.add(location);
            }
        }

        // Collect a bunch of classes that are Modules, plus the interface they're implementing
        Collection<ModuleLoader.ClassEntry> candidates = loader.getCandidatesWithPattern(
                (UriHelper.everything().equals(uri)) ? null : UriHelper.createPattern(uri), located, jarCache);

        // Prepare topological sort so we don't have trouble with dependencies
        Map<ModuleLoader.ClassEntry, TopologicalSortedList.Node<ModuleLoader.ClassEntry>> nodes = new THashMap<>();
        TopologicalSortedList<ModuleLoader.ClassEntry> sortedCandidates = new TopologicalSortedList<>();

        for (ModuleLoader.ClassEntry classEntry : candidates) {
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

                Class<? extends Module> dependency = ((Injector.ModuleEntry) dependencyEntry).getDependency();
                ModuleLoader.ClassEntry depClassEntry = loader.getClassEntry(dependency);

                if (depClassEntry != null) {
                    TopologicalSortedList.Node<ModuleLoader.ClassEntry> depNode = nodes.get(depClassEntry);

                    if (depNode == null) {
                        depNode = sortedCandidates.addNode(depClassEntry);
                        nodes.put(depClassEntry, depNode);
                    }

                    depNode.isRequiredBefore(node);

                } else {
                    log.warning("Could not get class entry for dependency: " + dependency);
                }
            }
        }

        // Execute the sort
        Collection<Module> modules = new LinkedList<>();

        try {
            sortedCandidates.sort();

        } catch (TopologicalSortedList.CycleException e) {
            log.log(Level.WARNING, "Error sorting module load order, found dependency cycle", e);
            return modules;
        }

        // Load all, sorted modules using our loader
        for (TopologicalSortedList.Node<ModuleLoader.ClassEntry> candidate : sortedCandidates) {
            Module module = loader.loadModule(this, candidate.getValue());

            if (module == null) {
                log.warning("Could not load modules properly, cancelling loading procedure");
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
        // Send shut down signal to modules
        for (Module module : registry.getModules()) {
            try {
                Annotations.call(module, Shutdown.class, 0, new Class[]{ModuleManager.class}, this);

            } catch (IllegalAccessException | InvocationTargetException e) {
                log.log(Level.WARNING, "Could not invoke shutdown method on module: " + module, e);
            }

            // Set state to shutdown
            ModuleRegistry.Entry entry = registry.getEntry(loader.getClassEntry(module.getClass()).getModule());

            if (entry != null) {
                ModuleInformation information = entry.getInformation();

                if (information instanceof ModuleInformationImpl) {
                    ((ModuleInformationImpl) information).setState(ModuleState.SHUTDOWN);
                }
                continue;
            }

            log.warning("Unable to set state to shut down: Could not find entry for module: " + module);
        }

        // And destroy what we can
        for (Destroyable destroyable : destroyables) {
            destroyable.destroy();
        }
    }

    /**
     * Registers a new classpath location.
     *
     * @param location The location to register
     */
    public void registerLocation(ClasspathLocation location) {
        try {
            classWorld.newRealm(location.getRealm(), getClass().getClassLoader()).addURL(location.getUrl());

        } catch (DuplicateRealmException ignore) {
            // Happens for (local) realms ...
        }
    }

}
