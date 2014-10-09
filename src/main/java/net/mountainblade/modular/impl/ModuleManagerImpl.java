package net.mountainblade.modular.impl;

import com.google.common.base.Optional;
import gnu.trove.set.hash.THashSet;
import lombok.Getter;
import lombok.extern.java.Log;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleManager;
import net.mountainblade.modular.UriHelper;
import net.mountainblade.modular.impl.locator.ClassLocator;
import net.mountainblade.modular.impl.locator.ClasspathLocator;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the default implementation of a {@link net.mountainblade.modular.ModuleManager}.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Log
public final class ModuleManagerImpl implements ModuleManager {
    @Getter
    private final ModuleRegistry registry;

    private final ModuleLoader loader;

    @Getter
    private final Collection<ClassLocator> locators;

    private final ClassWorld classWorld;

    private final JarCache jarCache;


    public ModuleManagerImpl() {
        classWorld = new ClassWorld();

        registry   = new ModuleRegistry();
        loader     = new ModuleLoader(classWorld, registry);
        locators   = new THashSet<>();
        jarCache   = new JarCache();

        // Add defaults
        getLocators().add(new ClasspathLocator());
    }

    @Override
    public Collection<Module> loadModules(URI uri) {
        // Detect pattern stuff
        Pattern pattern = null;

        if (!UriHelper.everything().equals(uri)) {
            pattern = Pattern.compile((uri.getAuthority() + uri.getPath()).replace("**", ".+").replace("*", "[^\\.]*"));
        }

        // Locate stuff from URI - using different providers (class pat, file, ...)
        Collection<ClasspathLocation> located = new LinkedList<>();

        for (ClassLocator locator : locators) {
            if (!locator.handles(uri)) {
                continue;
            }

            // Register all found locations
            for (ClasspathLocation location : locator.discover(uri)) {
                registerLocation(location);
                located.add(location);
            }
        }

        // Collect a bunch of classes that are Modules, plus the interface they're implementing
        Collection<Class<? extends Module>> moduleClasses = new LinkedList<>();

        for (ClasspathLocation location : located) {
            Collection<String> classNames = findModulesIn(location);

            // Check our possible modules
            for (String className : classNames) {
                if (pattern != null) {
                    Matcher matcher = pattern.matcher(className);

                    if (!matcher.matches()) {
                        continue;
                    }
                }

                Class<? extends Module> moduleClass = loader.loadModuleClass(location, className);
                if (moduleClass != null) {
                    moduleClasses.add(moduleClass);
                }
            }
        }

        // Topologically sort the list so we don't have trouble with dependencies
        // TODO

        // Instantiate the classes and inject their dependencies
        Collection<Module> modules = new LinkedList<>();

        for (Class<? extends Module> moduleClass : moduleClasses) {
            Module module = loader.loadModule(moduleClass);

            if (module != null) {
                modules.add(module);
            }
        }

        return modules;
    }

    @Override
    public final <M extends Module> Optional<M> getModule(Class<M> module) {
        return Optional.fromNullable(registry.getModule(module));
    }

    @Override
    public void destroy() {
        for (Module module : registry.getModules()) {
            // TODO call shutdown hook using injector
        }
    }

    public void registerLocation(ClasspathLocation location) {
        try {
            classWorld.newRealm(location.getRealm(), getClass().getClassLoader()).addURL(location.getUrl());

        } catch (DuplicateRealmException ignore) {
            // Happens for #classpath realms ...
        }
    }

    private Collection<String> findModulesIn(ClasspathLocation location) {
        String canonicalName = Module.class.getCanonicalName();

        LinkedList<String> subClasses = new LinkedList<>();
        JarCache.Entry cacheEntry = null;

        if (location.isJarFile()) {
            cacheEntry = jarCache.getEntry(location.getUri());
        }

        try {
            ClassLoader classLoader = classWorld.getRealm(location.getRealm());
            Collection<String> classNames = location.listClassNames();

            for (String className : classNames) {
                try {
                    Class<?> aClass = Class.forName(className, false, classLoader);

                    if (aClass.isInterface()) {
                        continue;
                    }

                    if (Module.class.isAssignableFrom(aClass) && !canonicalName.equals(aClass.getCanonicalName())) {
                        subClasses.add(className);
                    }

                } catch (ClassNotFoundException e) {
                    log.log(Level.WARNING, "Could not find class although it should exist: " + className, e);
                }
            }

        } catch (NoSuchRealmException e) {
            e.printStackTrace();
        }

        if (cacheEntry != null) {
            cacheEntry.getSubclasses().put(canonicalName, subClasses);
        }

        return subClasses;
    }

}
