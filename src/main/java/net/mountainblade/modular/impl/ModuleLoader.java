package net.mountainblade.modular.impl;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import lombok.Data;
import lombok.extern.java.Log;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleManager;
import net.mountainblade.modular.ModuleState;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import net.mountainblade.modular.annotations.Inject;
import net.mountainblade.modular.impl.location.ClassLocation;
import net.mountainblade.modular.impl.location.JarClassLocation;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a loader for modules.
 *
 * @author spaceemotion
 * @version 1.0
 */
// TODO check for overloading of modules (two implementations are not allowed)
@Log
class ModuleLoader implements Destroyable {
    private final ClassWorld classWorld;
    private final ModuleRegistry registry;
    private final Injector injector;

    private final Map<Class<?>, ClassEntry> classCache;
    private final Set<Class<?>> invalidCache;


    public ModuleLoader(ClassWorld classWorld, ModuleRegistry registry) {
        this.classWorld = classWorld;
        this.registry = registry;

        this.injector = new Injector(registry);

        this.classCache = new THashMap<>();
        this.invalidCache = new THashSet<>();
    }


    public Module loadModule(ModuleManager moduleManager, ClassEntry classEntry) {
        Implementation annotation = classEntry.getAnnotation();

        ModuleInformationImpl information = new ModuleInformationImpl(annotation.version(), annotation.authors());
        ModuleRegistry.Entry moduleEntry = registry.createEntry(classEntry.getModule(), information);

        try {
            // Instantiate module
            Constructor<? extends Module> constructor = classEntry.getImplementation().getDeclaredConstructor();
            constructor.setAccessible(true);

            Module module = constructor.newInstance();

            // Set to loading
            information.setState(ModuleState.LOADING);

            // Inject dependencies
            injector.inject(moduleEntry, module);

            // Set to loaded
            Annotations.callMethodWithAnnotation(module, Initialize.class, 0, new Class[]{ModuleManager.class},
                    moduleManager);

            // Set to ready and add to registry, but also add the instance in "ghost mode"
            information.setState(ModuleState.READY);
            moduleEntry.setModule(module);

            registry.addModule(classEntry.getModule(), moduleEntry, false);
            registry.addModule(classEntry.getImplementation(), moduleEntry, true);

            return module;

        } catch (NoSuchMethodException e) {
            log.log(Level.WARNING, "Could not find module constructor", e);

        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            log.log(Level.WARNING, "Could not instantiate module implementation", e);

        } catch (InjectFailedException e) {
            log.log(Level.WARNING, "Could not load module implementation", e);
        }

        return null;
    }

    public Class<?> loadClass(ClassLocation location, String className) throws ClassNotFoundException {
        if (location != null) {
            try {
                return classWorld.getRealm(location.getRealm()).loadClass(className);

            } catch (ClassNotFoundException | NoSuchRealmException e) {
                log.log(Level.WARNING, "Could not properly load class from realm: " + location.getUri(), e);
            }
        }

        return getClass().getClassLoader().loadClass(className);
    }

    public ClassEntry getClassEntry(Class<? extends Module> implClass) {
        // Early checking for null, against module, and if we already checked and saw that it's invalid
        if (implClass == null || Module.class.equals(implClass) || Implementation.Default.class.equals(implClass) ||
                Inject.Current.class.equals(implClass) || invalidCache.contains(implClass)) {
            return null;
        }

        // Check lookup first
        ClassEntry classEntry = classCache.get(implClass);

        // If we found nothing, take the hard route
        if (classEntry == null) {
            // We do not allow interface or annotation modules - that would not work and is thus just ... silly
            if (implClass.isInterface() || implClass.isAnnotation()) {
                invalidCache.add(implClass);
                return null;
            }

            // Return null for classes that don't have the annotation
            Implementation implementation = implClass.getAnnotation(Implementation.class);
            if (implementation == null) {
                invalidCache.add(implClass);
                return null;
            }

            // Get correct module class
            Class<? extends Module> module;

            if (!implementation.module().equals(Implementation.Default.class)) {
                // The developer already provided the wanted class so we'll gladly use that instead
                module = implementation.module();

            } else {
                // Well, no things found, so we have to discover the module class on our own
                module = getModuleClassRecursively(implClass);

                if (module == null) {
                    invalidCache.add(implClass);
                    return null;
                }
            }

            // Get dependencies via the injector, create new class entry and add to cache so we don't need to this again
            classEntry = new ClassEntry(module, implClass, implementation, injector.discover(implClass));
            classCache.put(implClass, classEntry);

            // Also add the module, so we can get our dependencies right
            classCache.put(module, classEntry);
        }

        return classEntry;
    }

    @Override
    public void destroy() {
        injector.destroy();

        invalidCache.clear();
        classCache.clear();
    }

    @SuppressWarnings("unchecked")
    Collection<ClassEntry> getCandidatesWithPattern(Pattern regex, Collection<ClassLocation> located, JarCache cache) {
        Collection<ClassEntry> moduleClasses = new LinkedList<>();

        for (ClassLocation location : located) {
            Collection<String> classNames = findModulesIn(cache, location);

            // Check our possible modules
            for (String className : classNames) {
                if (regex != null) {
                    Matcher matcher = regex.matcher(className);

                    if (!matcher.matches()) {
                        continue;
                    }
                }

                try {
                    Class<?> candidate = loadClass(location, className);
                    ClassEntry classEntry = getClassEntry((Class<? extends Module>) candidate);

                    // Add to our classes
                    if (classEntry != null) {
                        moduleClasses.add(classEntry);
                    }

                } catch (ClassNotFoundException e) {
                    log.log(Level.WARNING, "Could not properly load module candidate", e);
                }
            }
        }

        return moduleClasses;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Module> getModuleClassRecursively(Class<?> aClass) {
        Class<? extends Module> moduleClass = null;

        // Let's check interfaces first...
        Class<?>[] interfaces = aClass.getInterfaces();

        for (Class<?> anInterface : interfaces) {
            if (!Module.class.isAssignableFrom(anInterface)) {
                continue;
            }

            // Seems like we found something
            if (Module.class.equals(anInterface)) {
                // Use our class since it probably is a direct implementation
                moduleClass = (Class<? extends Module>) aClass;

            } else {
                moduleClass = (Class<? extends Module>) anInterface;
            }

            break;
        }

        // We still didn't find a proper module class search through our superclasses
        if (moduleClass == null) {
            // TODO
        }

        return moduleClass;
    }

    private Collection<String> findModulesIn(JarCache cache, ClassLocation location) {
        String canonicalName = Module.class.getCanonicalName();

        LinkedList<String> subClasses = new LinkedList<>();
        JarCache.Entry cacheEntry = null;

        if (location instanceof JarClassLocation) {
            cacheEntry = cache.getEntry(location.getUri());
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
            log.log(Level.WARNING, "Could not find realm", e);
        }

        if (cacheEntry != null) {
            cacheEntry.getSubclasses().put(canonicalName, subClasses);
        }

        return subClasses;
    }


    @Data
    public final static class ClassEntry {
        private final Class<? extends Module> module;
        private final Class<? extends Module> implementation;
        private final Implementation annotation;
        private final Collection<Injector.Entry> dependencies;
    }

}
