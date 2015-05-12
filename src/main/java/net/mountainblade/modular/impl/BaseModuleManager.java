package net.mountainblade.modular.impl;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import net.mountainblade.modular.Filter;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.ModuleManager;
import net.mountainblade.modular.ModuleState;
import net.mountainblade.modular.annotations.Shutdown;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Represents a BaseModuleManager.
 *
 * @author spaceemotion
 * @version 1.0
 */
public abstract class BaseModuleManager implements ModuleManager {
    private static final Logger LOG = Logger.getLogger(DefaultModuleManager.class.getName());
    private static final ClassWorld CLASS_WORLD = new ClassWorld();

    private static final String CLASS_PATH_SEPARATOR = System.getProperty("path.separator");
    private static final String JAVA_CLASS_PATH = System.getProperty("java.class.path");
    private static final List<String> SYSTEM_PATH = Splitter.on(CLASS_PATH_SEPARATOR).splitToList(JAVA_CLASS_PATH);
    private static final List<URI> LOCAL_CLASSPATH = new LinkedList<>();
    private static final Map<URI, Collection<String>> JAR_CACHE = new THashMap<>();
    private static final Collection<String> BLACKLIST = new THashSet();

    static {
        Collections.addAll(BLACKLIST, "idea_rt.jar", "junit-rt.jar", ".git", ".idea");

        // Get root class loader
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        while (loader != null && loader.getParent() != null) {
            loader = loader.getParent();
        }

        // Add the system files to our "blacklist"
        final Collection<String> system = new LinkedList<>();
        if (loader != null && loader instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) loader).getURLs()) {
                system.add(url.getFile());
            }
        }

        // Check if the remaining files actually belong to a JRE or JDK
        loop: for (String aClass : SYSTEM_PATH) {
            for (String blacklisted : BLACKLIST) {
                if (aClass.contains(blacklisted)) {
                    continue loop;
                }
            }

            if (!aClass.contains("jre" + File.separatorChar) && !aClass.contains("jdk" + File.separatorChar) &&
                    !system.contains(aClass)) {
                LOCAL_CLASSPATH.add(new File(aClass).toURI());
            }
        }
    }

    private final Collection<Destroyable> destroyables;
    private final Collection<URI> classpath;

    private final ModuleRegistry registry;
    private final Injector injector;
    private final ModuleLoader loader;


    public BaseModuleManager(ModuleRegistry registry, ClassRealm parentRealm) {
        this.destroyables = new LinkedList<>();
        this.classpath = new THashSet<>(LOCAL_CLASSPATH);

        this.registry = registry;
        this.injector = new Injector(registry);
        this.loader = new ModuleLoader(newRealm(parentRealm), registry, injector);

        destroyables.add(registry);
        destroyables.add(injector);
        destroyables.add(loader);
    }


    // -------------------------------- Basic getters and setters --------------------------------

    protected final ModuleRegistry getRegistry() {
        return registry;
    }

    public final Injector getInjector() {
        return injector;
    }

    public final ModuleLoader getLoader() {
        return loader;
    }


    // -------------------------------- Providing new modules --------------------------------

    @Override
    public <T extends Module> T provideSimple(T module) {
        return provide(module, false);
    }

    @Override
    public <T extends Module> T provide(T module) {
        return provide(module, true);
    }

    private <T extends Module> T provide(T module, boolean inject) {
        if (module == null) {
            LOG.warning("Provided with null instance, will not add to registry");
            return null;
        }

        // Get class entry and implementation annotation
        final ModuleLoader.ClassEntry entry = loader.getClassEntry(module.getClass());
        if (entry == null) {
            LOG.warning("Provided with invalid module, will not at to registry");
            return null;
        }

        // Create registry entry
        final ModuleInformationImpl information = new ModuleInformationImpl(entry.getAnnotation());
        final ModuleRegistry.Entry moduleEntry = registry.createEntry(entry.getModule(), information);

        // Inject dependencies if specified
        if (inject) {
            loader.injectAndInitialize(this, module, information, moduleEntry);
        }

        // Register module
        loader.registerEntry(entry, module, information, moduleEntry);

        return module;
    }


    // -------------------------------- Loading modules --------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <M extends Module> M loadModule(Class<M> moduleClass, Filter... filters) {
        return (M) loader.loadModule(this, loader.getClassEntry(moduleClass));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Module> loadModules(String resource, Filter... filters) {
        try {
            // First check if the give name is an already known class
            final Class<?> theClass = Class.forName(resource, true, loader.getRealm());
            if (theClass != null && loader.isValidModuleClass(theClass)) {
                return Collections.singleton(loadModule((Class<? extends Module>) theClass, filters));
            }

        } catch (ClassNotFoundException ignore) {
            // Just ignore this and assume its a package name
        }

        // ... then try the package name and use the local classpath
        return loadModules(classpath, resource.replace('.', File.separatorChar), filters);
    }

    @Override
    public Collection<Module> loadModules(File file, Filter... filters) {
        return loadModules(file.toURI(), filters);
    }

    @Override
    public Collection<Module> loadModules(URI uri, Filter... filters) {
        return loadModules(uri, "", filters);
    }

    public Collection<Module> loadModules(URI uri, String root, Filter... filters) {
        return loadModules(Collections.singletonList(uri), root, filters);
    }

    public Collection<Module> loadModules(Collection<URI> uris, String root, Filter... filters) {
        final LinkedList<URI> copy = new LinkedList<>(uris);

        // 1. Find modules using the URI
        final THashSet<String> names = new THashSet<>();
        final Collection<ModuleLoader.ClassEntry> candidates = loader.getCandidates(getClassNames(copy, root, names));

        // 2. Filter the results
        Iterator<ModuleLoader.ClassEntry> iterator;

        for (Filter filter : filters) {
            iterator = candidates.iterator();

            while (iterator.hasNext()) {
                final ModuleLoader.ClassEntry classEntry = iterator.next();

                if (!filter.retain(classEntry)) {
                    iterator.remove();
                }
            }
        }

        // 3. Create a topological list of dependencies
        Map<ModuleLoader.ClassEntry, TopologicalSortedList.Node<ModuleLoader.ClassEntry>> nodes = new THashMap<>();
        final TopologicalSortedList<ModuleLoader.ClassEntry> sortedCandidates = new TopologicalSortedList<>();

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

        // 4. Sort the list and account for errors
        final Collection<Module> modules = new LinkedList<>();

        try {
            sortedCandidates.sort();

        } catch (TopologicalSortedList.CycleException e) {
            LOG.log(Level.WARNING, "Error sorting module load order, found dependency cycle", e);
            return modules;
        }

        // 5. Load all, sorted modules using our loader
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

    private boolean addUriToRealm(URI uri) {
        try {
            getLoader().getRealm().addURL(uri.toURL());
            classpath.add(uri);
            return true;

        } catch (MalformedURLException e) {
            LOG.log(Level.SEVERE, "Could not load modules from malformed URL: " + uri, e);
        }

        return false;
    }

    private Collection<String> getClassNames(Collection<URI> uris, String packageName, Collection<String> classNames) {
        // Example for a JAR URI:
        //
        // jar:file:/Users/spaceemotion/Development/bladekit/target/bladekit-commons-1.0-SNAPSHOT.jar!/net/mountainblade
        //          |--------------------------------- path ----------------------------------------|  |-- pkg root ---|
        // ^^^^---- is jar                                                      get index of this ---^^

        // Example for a collection of given mixed URIs:
        //
        // modules/
        //  - moduleA.jar!/          <-- root (jar)
        //     - net                 <-- package start
        //        - mountainblade
        //           - Test.class    <-- class
        //  - moduleB.jar!/ ....
        //  - demo/
        //     - moduleC.jar!/ ...
        // development
        //  - src                    <-- root (folder)
        //     - net                 <-- package start
        //        - mountainblade
        //           - Test.class    <-- class

        for (URI uri : uris) {
            final File file;

            // If the uri does not seem to be a jar file, do the directory walk
            if (!uri.getScheme().equalsIgnoreCase("jar") && !uri.getSchemeSpecificPart().endsWith(".jar")) {
                walkDirectory(null, new File(uri), packageName, classNames);
                continue;
            }

            // Add the JAR to the realm
            addUriToRealm(uri);

            // Check if we already have a cached version of the JAR file
            final Collection<String> cache = JAR_CACHE.get(uri);
            if (cache != null) {
                for (String name : cache) {
                    if (name.startsWith(packageName)) {
                        classNames.add(name);
                    }
                }

                continue;
            }

            // Get the proper JAR file or folder from the URI
            final Collection<String> classes = new LinkedList<>();
            final String scheme = uri.getSchemeSpecificPart();
            final int divider = scheme.indexOf("!/");
            file = new File(divider < 0 ? scheme : scheme.substring(0, divider));

            // Get appropriate class names by removing trailing .class and convert the file name to a usable class name
            try (ZipInputStream zip = new ZipInputStream(new FileInputStream(file))) {
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    String name = entry.getName();
                    if (!entry.isDirectory() && name.endsWith(".class")) {
                        name = getProperClassName(name);
                        classes.add(name);

                        if (name.startsWith(packageName)) {
                            classNames.add(name);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not fetch JAR file contents: " + uri, e);
            }

            // Add processed classes to the cache
            JAR_CACHE.put(uri, classes);
        }

        return classNames;
    }

    private String getProperClassName(String name) {
        return name.substring(0, name.length() - ".class".length()).replace("\\", "/").replace("/", ".");
    }

    private void walkDirectory(String rootPath, File parent, String packageName, Collection<String> classNames) {
        final File[] listFiles = parent.isDirectory() ? parent.listFiles() : null;

        if (listFiles != null) {
            loop: for (File file : listFiles) {
                final String name = file.getName();

                for (String blacklisted : BLACKLIST) {
                    if (name.equalsIgnoreCase(blacklisted)) {
                        continue loop;
                    }
                }

                // Check if the current file is a directory, and if it is, check if its a classpath (and thus a root)
                if (file.isDirectory()) {
                    walkDirectory(classpath.contains(parent.toURI()) ?
                            parent.getAbsolutePath() : rootPath, file, packageName, classNames);
                    continue;
                }

                // If we have a root path check if we're in the right package
                final String path = file.getAbsolutePath();
                final String substring = rootPath != null ? path.substring(rootPath.length() + 1) : null;
                if (substring != null && !substring.startsWith(packageName)) {
                    continue;
                }

                // Check for JAR files and do the whole thing over again
                final URI uri = file.toURI();
                if (name.endsWith(".jar")) {
                    getClassNames(Collections.singleton(uri), packageName, classNames);
                    continue;
                }

                // Only add class files
                if (name.endsWith(".class")) {
                    classNames.add(getProperClassName(substring != null ? substring : path));
                }
            }
        }
    }


    // -------------------------------- General getters --------------------------------

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


    // -------------------------------- Miscellaneous --------------------------------

    protected void shutdown(Iterator<Module> iterator) {
        while (iterator.hasNext()) {
            final Module module = iterator.next();

            // Get module entry
            final ModuleRegistry.Entry entry = registry.getEntry(loader.getClassEntry(module.getClass()).getModule());
            if (entry == null) {
                LOG.warning("Unable to set state to shut down: Could not find entry for module: " + module);
                continue;
            }

            // Skip already shut down modules
            final ModuleInformation information = entry.getInformation();
            if (ModuleState.SHUTDOWN.equals(information.getState())) {
                continue;
            }

            // Call shutdown function
            try {
                LOG.fine("Shutting down " + module.getClass().getName());
                Annotations.call(module, Shutdown.class, 0, new Class[]{ModuleManager.class}, this);

            } catch (IllegalAccessException | InvocationTargetException e) {
                LOG.log(Level.WARNING, "Could not invoke shutdown method on module: " + module, e);
            }

            // Set state to "shutdown"
            if (information instanceof ModuleInformationImpl) {
                ((ModuleInformationImpl) information).setState(ModuleState.SHUTDOWN);
            }
        }

        // And destroy what we can
        for (Destroyable destroyable : destroyables) {
            destroyable.destroy();
        }
    }

    public static ClassRealm newRealm(ClassRealm parent) {
        try {
            final String name = UUID.randomUUID().toString();
            return parent != null ? parent.createChildRealm(name) : CLASS_WORLD.newRealm(name);

        } catch (DuplicateRealmException e) {
            // Hopefully this never happens... would be weird, right? Right?!?
            throw new RuntimeException("Created duplicate realm even though we're using random UUIDs!", e);
        }
    }

    public static void blacklist(String name) {
        BLACKLIST.add(name);
    }

}
