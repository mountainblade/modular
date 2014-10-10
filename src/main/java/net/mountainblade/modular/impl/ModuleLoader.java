package net.mountainblade.modular.impl;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import lombok.Data;
import lombok.extern.java.Log;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleState;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a loader for modules.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Log
public class ModuleLoader extends Destroyable {
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


    public Module loadModule(ClassEntry classEntry) {
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
            injector.inject(module);

            // Set to loaded
            AnnotationHelper.callMethodWithAnnotation(module, Initialize.class);

            // Also add to registry
            information.setState(ModuleState.READY);
            moduleEntry.setModule(module);

            registry.addModule(classEntry.getModule(), moduleEntry, false);

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

    public Class<?> loadClass(ClasspathLocation location, String className) throws ClassNotFoundException {
        if (location != null) {
            try {
                return classWorld.getRealm(location.getRealm()).loadClass(className);

            } catch (ClassNotFoundException | NoSuchRealmException e) {
                log.log(Level.WARNING, "Could not properly load class from realm: " + location.getUri(), e);
            }
        }

        return getClass().getClassLoader().loadClass(className);
    }

    public ClassEntry getClassEntry(Class<? extends Module> aClass) {
        // Early checking for null, against module, and if we already checked and saw that it's invalid
        if (aClass == null || Module.class.equals(aClass) || invalidCache.contains(aClass)) {
            return null;
        }

        // Check lookup first
        ClassEntry classEntry = classCache.get(aClass);

        // If we found nothing, take the hard route
        if (classEntry == null) {
            // We do not allow interface or annotation modules - that would not work and is thus just ... silly
            if (aClass.isInterface() || aClass.isAnnotation()) {
                invalidCache.add(aClass);
                return null;
            }

            // Return null for classes that don't have the annotation
            Implementation implementation = aClass.getAnnotation(Implementation.class);
            if (implementation == null) {
                invalidCache.add(aClass);
                return null;
            }

            // Get correct module class
            Class<? extends Module> module;

            if (!implementation.module().equals(Module.class)) {
                // The developer already provided the wanted class so we'll gladly use that instead
                module = implementation.module();

            } else {
                // Well, no things found, so we have to discover the module class on our own
                module = getModuleClassRecursively(aClass);

                if (module == null) {
                    invalidCache.add(aClass);
                    return null;
                }
            }

            // Add to cache so we don't need to do all this again (only if we found something)
            classEntry = new ClassEntry(module, aClass, implementation);
            classCache.put(aClass, classEntry);
        }

        return classEntry;
    }

    @Override
    void destroy() {
        injector.destroy();

        invalidCache.clear();
        classCache.clear();
    }

    @SuppressWarnings("unchecked")
    Collection<ClassEntry> resolveCandidatesWithPattern(Pattern pattern, Collection<ClasspathLocation> located,
                                                        JarCache cache) {
        Collection<ClassEntry> moduleClasses = new LinkedList<>();

        for (ClasspathLocation location : located) {
            Collection<String> classNames = findModulesIn(cache, location);

            // Check our possible modules
            for (String className : classNames) {
                if (pattern != null) {
                    Matcher matcher = pattern.matcher(className);

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

    private Collection<String> findModulesIn(JarCache cache, ClasspathLocation location) {
        String canonicalName = Module.class.getCanonicalName();

        LinkedList<String> subClasses = new LinkedList<>();
        JarCache.Entry cacheEntry = null;

        if (location.isJarFile()) {
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
    }

}
