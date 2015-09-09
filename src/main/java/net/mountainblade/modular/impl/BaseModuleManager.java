/**
 * Copyright (C) 2014-2015 MountainBlade (http://mountainblade.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.mountainblade.modular.impl;

import com.google.common.base.Optional;
import gnu.trove.iterator.hash.TObjectHashIterator;
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
import java.net.URISyntaxException;
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
 * Represents the default implementation for module managers.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class BaseModuleManager implements ModuleManager {
    private static final Logger LOG = Logger.getLogger(DefaultModuleManager.class.getName());

    private static final ClassWorld CLASS_WORLD = new ClassWorld();
    private static final String JAVA_HOME = new File(System.getProperty("java.home")).getParent();
    private static final List<URI> LOCAL_CLASSPATH = new LinkedList<>();
    private static final Map<URI, Collection<String>> JAR_CACHE = new THashMap<>();
    private static final Collection<String> BLACKLIST = new THashSet<>();
    private static final Collection<URI> URI_BLACKLIST = new THashSet<>();
    private static boolean thoroughSearchEnabled;
    static boolean includedFullClassPath;

    static {
        Collections.addAll(BLACKLIST, ".git", ".idea");

        final String additionalBlacklist = System.getProperty("modular.blacklist");
        if (additionalBlacklist != null) {
            Collections.addAll(BLACKLIST, additionalBlacklist.split(File.pathSeparator));
        }

        enableThoroughSearch(System.getProperty("modular.thoroughSearch") != null);

        if (System.getProperty("modular.includeFullClassPath") != null) {
            includeFullClassPath();
        }
    }

    private final Collection<Destroyable> destroyables;
    private final Collection<URI> classpath;

    private final ModuleRegistry registry;
    private final Injector injector;
    private final ModuleLoader loader;

    /**
     * Creates a new module manager instance.
     *
     * <strong>It is not recommended to </strong>
     *
     * @param registry       A registry instance
     * @param parentRealm    The parent realm, can be left at {@code null}
     * @param classLoader    A parent class loader, can be left at {@code null}
     */
    public BaseModuleManager(ModuleRegistry registry, ClassRealm parentRealm, ClassLoader classLoader) {
        this(registry, newRealm(parentRealm, classLoader));
    }

    /**
     * Creates a new module manager instance.
     *
     * @param registry    A registry instance
     * @param realm       The class realm to load our modules in
     */
    public BaseModuleManager(ModuleRegistry registry, ClassRealm realm) {
        this.destroyables = new LinkedList<>();
        this.classpath = createClassPathSet(LOCAL_CLASSPATH);

        this.registry = registry;
        this.injector = new Injector(registry);
        this.loader = new ModuleLoader(realm, registry, injector);

        destroyables.add(registry);
        destroyables.add(injector);
        destroyables.add(loader);

        // Also register ourselves so other modules can use this as implementation via injection
        getRegistry().addGhostModule(ModuleManager.class, this, new MavenModuleInformation());
    }


    // -------------------------------- Basic getters and setters --------------------------------

    @Override
    public final ModuleRegistry getRegistry() {
        return registry;
    }

    @Override
    public final Injector getInjector() {
        return injector;
    }

    @Override
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
            loader.injectAndInitialize(this, module, information);
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
            // Just ignore this and assume it's a package name
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

    @Override
    public Collection<Module> loadModules(URI uri, String packageName, Filter... filters) {
        return loadModules(Collections.singletonList(uri), packageName, filters);
    }

    @Override
    public Collection<Module> loadModules(Collection<URI> uris, String root, Filter... filters) {
        final LinkedList<URI> copy = new LinkedList<>(uris);

        // 1. Find modules using the URI
        final THashMap<URI, Collection<String>> map = new THashMap<>();
        final Collection<String> list = new LinkedList<>();
        final Collection<ModuleLoader.ClassEntry> entries = loader.filter(this, getClasses(copy, root, map, list), list);

        // 2. Filter the results
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

        // 3. Create a topological list of dependencies
        Map<ModuleLoader.ClassEntry, TopologicalSortedList.Node<ModuleLoader.ClassEntry>> nodes = new THashMap<>();
        final TopologicalSortedList<ModuleLoader.ClassEntry> sortedCandidates = new TopologicalSortedList<>();

        for (ModuleLoader.ClassEntry classEntry : entries) {
            TopologicalSortedList.Node<ModuleLoader.ClassEntry> node = nodes.get(classEntry);

            if (node == null) {
                node = sortedCandidates.addNode(classEntry);
                nodes.put(classEntry, node);
            }

            for (Injector.Entry dependencyEntry : classEntry.getDependencies()) {
                addDependency(classEntry, node, dependencyEntry.getDependency(), nodes, sortedCandidates);
            }

            for (Class<? extends Module> moduleClass : classEntry.getRequirements()) {
                addDependency(classEntry, node, moduleClass, nodes, sortedCandidates);
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

    private void addDependency(ModuleLoader.ClassEntry classEntry,
                               TopologicalSortedList.Node<ModuleLoader.ClassEntry> node,
                               Class<? extends Module> dependency,
                               Map<ModuleLoader.ClassEntry, TopologicalSortedList.Node<ModuleLoader.ClassEntry>> nodes,
                               TopologicalSortedList<ModuleLoader.ClassEntry> sortedCandidates) {
        // Skip the ones we don't need
        if (dependency == null || dependency.equals(classEntry.getImplementation())) {
            return;
        }

        final ModuleLoader.ClassEntry depClassEntry = loader.getClassEntry(dependency);
        if (depClassEntry == null) {
            LOG.warning("Could not get class entry for dependency: " + dependency);
            return;
        }

        TopologicalSortedList.Node<ModuleLoader.ClassEntry> depNode = nodes.get(depClassEntry);
        if (depNode == null) {
            depNode = sortedCandidates.addNode(depClassEntry);
            nodes.put(depClassEntry, depNode);
        }

        depNode.isRequiredBefore(node);
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

    private Map<URI, Collection<String>> getClasses(Collection<URI> uris, String packageName,
                                                    Map<URI, Collection<String>> classNames, Collection<String> list) {
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
            if (URI_BLACKLIST.contains(uri)) {
                continue;
            }

            final File file;

            // If the uri does not seem to be a jar file, do the directory walk
            if (!uri.getScheme().equalsIgnoreCase("jar") && !uri.getSchemeSpecificPart().endsWith(".jar")) {
                final File parent = new File(uri);
                walkDirectory(parent, parent, packageName, classNames, list);
                continue;
            }

            // Add the JAR to the realm
            addUriToRealm(uri);

            // Check if we already have a cached version of the JAR file
            final Collection<String> cache = JAR_CACHE.get(uri);

            if (cache != null) {
                if (packageName.isEmpty()) {
                    classNames.put(uri, cache);

                } else {
                    final LinkedList<String> names = new LinkedList<>();
                    for (String name : cache) {
                        if (name.startsWith(packageName)) {
                            names.add(name);
                            list.add(name);
                        }
                    }

                    classNames.put(uri, names);
                }

                continue;
            }

            // Get the proper JAR file or folder from the URI
            final Collection<String> classes = new LinkedList<>();
            final String scheme = uri.getSchemeSpecificPart();
            final int divider = scheme.indexOf("!/");
            file = new File(divider < 0 ? scheme : scheme.substring(0, divider).replace("file:", ""));

            // Get appropriate class names by removing trailing .class and convert the file name to a usable class name
            try (ZipInputStream zip = new ZipInputStream(new FileInputStream(file))) {
                String name;
                String properName;
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    name = entry.getName();

                    if (!entry.isDirectory() && name.endsWith(".class")) {
                        properName = getProperClassName(name);
                        if (properName.endsWith("package-info")) {
                            continue;
                        }

                        classes.add(properName);

                        if (properName.startsWith(packageName) || name.startsWith(packageName)) {
                            list.add(properName);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not fetch JAR file contents: " + uri + " (using " + file + ')', e);
            }

            // Add processed classes to the cache
            classNames.put(uri, classes);
            JAR_CACHE.put(uri, classes);
        }

        return classNames;
    }

    private String getProperClassName(String name) {
        return name.substring(0, name.length() - ".class".length()).replace("\\", "/").replace("/", ".");
    }

    private void walkDirectory(File root, File parent, String packageName, Map<URI, Collection<String>> names,
                               Collection<String> list) {
        final File[] listFiles = parent.isDirectory() ? parent.listFiles() : null;
        if (listFiles == null) {
            return;
        }

        // Continue to look up valid files within the directory
        for (File file : listFiles) {
            final String name = file.getName();
            if (isBlacklisted(name)) {
                continue;
            }

            // Check if the current file is a directory, and if it is, check if its a classpath (and thus a root)
            if (file.isDirectory()) {
                walkDirectory(classpath.contains(parent.toURI()) ?
                        parent.getAbsoluteFile() : root, file, packageName, names, list);
                continue;
            }

            // Check for JAR files and do the whole thing over again
            final URI uri = file.toURI();
            if (name.endsWith(".jar")) {
                getClasses(Collections.singleton(uri), packageName, names, list);
                continue;
            }

            if (root == null) {
                root = parent;
            }

            // If we have a root path check if we're in the right package
            final String path = file.getAbsolutePath();
            final String substring = path.substring(root.getAbsolutePath().length() + 1);
            if (!substring.startsWith(packageName)) {
                continue;
            }

            // Only add class files
            if (name.endsWith(".class")) {
                final URI rootUri = root.toURI();
                Collection<String> classNames = names.get(rootUri);
                if (classNames == null) {
                    classNames = new LinkedList<>();
                    names.put(rootUri, classNames);
                }

                final String className = getProperClassName(substring);
                if (!className.equalsIgnoreCase("package-info")) {
                    classNames.add(className);
                    list.add(className);

                    addUriToRealm(rootUri);
                }
            }
        }
    }

    void blacklist(URI uri) {
        URI_BLACKLIST.add(uri);
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


    // -------------------------------- Miscellaneous --------------------------------

    @Override
    public void shutdown() {
        // Send shut down signal to all registered modules
        shutdown(getRegistry().getModules().iterator());
    }

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

    /**
     * Creates a new class realm.
     *
     * @param parent         The parent realm, if any - can be left at null
     * @param classLoader    The parent class loader, if any - can be left at null
     * @return A new class realm
     * @throws RuntimeException When a realm of the same name already exists,
     * as we're using UUIDs this <i>should</i> never happen...
     */
    public static ClassRealm newRealm(ClassRealm parent, ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = BaseModuleManager.class.getClassLoader();
        }

        try {
            final String name = UUID.randomUUID().toString();
            return parent != null ? parent.createChildRealm(name) : CLASS_WORLD.newRealm(name, classLoader);

        } catch (DuplicateRealmException e) {
            // Hopefully this never happens... would be weird, right? Right?!?
            throw new RuntimeException("Created duplicate realm even though we're using random UUIDs!", e);
        }
    }

    /**
     * Blacklists the given directory / file name so folders
     * with the same name will be ignored during the discovery process.
     *
     * @param name    The name of the directory or file
     */
    public static void blacklist(String name) {
        BLACKLIST.add(name.toLowerCase());
    }

    /**
     * Adds the given URIs to the local classpath which will be checked when loading modules.
     *
     * @param uris    The URIs to add
     */
    public static void include(URI... uris) {
        Collections.addAll(LOCAL_CLASSPATH, uris);
    }

    /**
     * Includes all elements in the current java class path as retrieved by the "java.class.path" system property.
     *
     * This should only be used when the application is using some black magic and changes the class loader on startup.
     * It is also recommended to only use this method without thorough search.
     */
    public static void includeFullClassPath() {
        final String[] paths = System.getProperty("java.class.path", "").split(File.pathSeparator);
        final URI[] uris = new URI[paths.length];

        for (int i = 0; i < paths.length; i++) {
            uris[i] = new File(paths[i]).toURI();
        }

        include(uris);
        includedFullClassPath = true;
    }

    /**
     * Toggles the state of the "thorough search" mode.
     *
     * This mode was an internal first solution to a problem that, when running the application
     * from inside an IDE, some modules might not work as expected because their requirements
     * weren't loaded beforehand.
     *
     * However, this will increase the loading time drastically.
     * This mode can also be enabled by commandline (using the {@code -Dmodular.thoroughSearch} property).
     *
     * @param toggle    True if the mode should be enabled, false if disabled
     */
    public static void enableThoroughSearch(boolean toggle) {
        thoroughSearchEnabled = toggle;
        LOCAL_CLASSPATH.clear();

        final Class<BaseModuleManager> baseClass = BaseModuleManager.class;
        final ClassLoader classLoader = baseClass.getClassLoader();

        addToLocalClassPath(classLoader.getResource("."), findClassLoaderRootURL(baseClass));

        if (toggle && classLoader instanceof URLClassLoader) {
            // Add full runtime classpath
            String path;
            for (URL url: ((URLClassLoader) classLoader).getURLs()) {
                path = url.getPath();
                if (path.startsWith(JAVA_HOME)) {
                    continue;
                }

                if (!isBlacklisted(path)) {
                    addToLocalClassPath(url);
                }
            }
        }
    }

    /**
     * Indicates whether or not the "thorough search mode" is enabled.
     *
     * @return True if enabled, false if not
     * @see #enableThoroughSearch(boolean)
     */
    public static boolean thoroughSearchEnabled() {
        return thoroughSearchEnabled;
    }

    /**
     * Adds the given url(s) as URIs to the local class path.
     * This needs to be done before a manager gets created as each manager holds a modified copy of the static list.
     *
     * @param urls    The url(s) to add
     */
    public static void addToLocalClassPath(URL... urls) {
        for (URL path : urls) {
            if (path == null) {
                continue;
            }

            try {
                LOCAL_CLASSPATH.add(path.toURI());

            } catch (URISyntaxException ignore) {}
        }
    }

    /**
     * Tries to find the root URL from the given class by getting the full URL to the class file itself first,
     * then removing the additional path again.
     *
     * @param aClass    The class to get the URL for
     * @return The class loader's root URL, or null if none could be found
     */
    public static URL findClassLoaderRootURL(Class aClass) {
        final String prefix = aClass.getCanonicalName().replace('.', '/') + ".class";
        final URL resource = aClass.getClassLoader().getResource(prefix);

        try {
            if (resource != null) {
                return new URL(resource.toString().replace(prefix, ""));
            }
        } catch (MalformedURLException ignore) {}

        return null;
    }

    private static Collection<URI> createClassPathSet(List<URI> uris) {
        final THashSet<URI> classPath = new THashSet<>(uris);
        final TObjectHashIterator<URI> iterator = classPath.iterator();

        URI next;
        String entry;
        while (iterator.hasNext()) {
            next = iterator.next();
            if (next == null) {
                continue;
            }

            entry = next.toString();
            if (entry.contains(JAVA_HOME)) {
                iterator.remove();
                continue;
            }

            if (isBlacklisted(entry)) {
                iterator.remove();
            }
        }

        return classPath;
    }

    private static boolean isBlacklisted(String name) {
        name = name.toLowerCase();

        for (String blacklisted : BLACKLIST) {
            if (name.contains(blacklisted)) {
                return true;
            }
        }

        return false;
    }

}
