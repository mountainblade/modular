package net.mountainblade.modular.impl;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TLinkedHashSet;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleManager;
import net.mountainblade.modular.ModuleState;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import net.mountainblade.modular.annotations.Inject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a loader for modules.
 *
 * @author spaceemotion
 * @version 1.0
 */
public final class ModuleLoader extends Destroyable {
    private static final Logger LOG = Logger.getLogger(ModuleLoader.class.getName());

    /** A map containing all meta data about the indexed classes */
    private static final Map<Class<?>, ClassEntry> CLASS_CACHE = new THashMap<>();

    /** A set of classes that have been skipped as they contain no information (and should be skipped in the future) */
    private static final Collection<Class<?>> INVALID_CACHE = new THashSet<>();

    private final ClassRealm realm;
    private final ModuleRegistry registry;
    private final Injector injector;

    private final Collection<Class<?>> ignores;


    public ModuleLoader(ClassRealm realm, ModuleRegistry registry, Injector injector) {
        this.realm = realm;
        this.registry = registry;
        this.injector = injector;

        ignores = new THashSet<>();
    }

    public ClassRealm getRealm() {
        return realm;
    }

    /**
     * Adds the given class to the list of ignored module superclasses / -interfaces.
     *
     * @param ignore    The module super-class to ignore during the loading process
     * @return True if the class could be added, false if not
     */
    public boolean ignoreModuleClass(Class<? extends Module> ignore) {
        return ignores.add(ignore);
    }

    @SuppressWarnings("unchecked")
    Collection<ClassEntry> getCandidates(Collection<String> classNames) {
        final Collection<Class<? extends Module>> candidates = new TLinkedHashSet<>();
        final Collection<ClassEntry> moduleClasses = new LinkedList<>();

        // Walk over each location first, then build the list of potential modules
        for (String className : classNames) {
            Class<?> aClass;

            try {
                try {
                    aClass = realm.loadClass(className);

                } catch (ClassNotFoundException e) {
                    try {
                        aClass = getClass().getClassLoader().loadClass(className);

                    } catch (ClassNotFoundException e1) {
                        LOG.log(Level.WARNING, "Could not load class: " + className, e1);
                        continue;
                    }
                }
            } catch (NoClassDefFoundError e) {
                LOG.log(Level.INFO, "Could not load class that was available at compile time for: " + className, e);
                continue;
            }

            // We can safely ignore any interfaces, since we only want to get implementations
            if (isValidModuleClass(aClass)) {
                candidates.add((Class<? extends Module>) aClass);
            }
        }

        // Go through each candidate to search if we got one that got overwritten by a subclass, that's the sole purpose
        // of all this ordering and looping - to detect if an implementation became obsolete by a sub-implementation
        for (Class<? extends Module> candidate : candidates) {
            if (candidateIsObsolete(candidate, candidates)) {
                continue;
            }

            // Try to get class entry and add to our classes
            final ClassEntry classEntry = getClassEntry(candidate);
            if (classEntry != null) {
                moduleClasses.add(classEntry);
            }
        }

        return moduleClasses;
    }

    boolean isValidModuleClass(Class<?> aClass) {
        return !aClass.isInterface() && !Module.class.equals(aClass) && Module.class.isAssignableFrom(aClass);
    }

    private boolean candidateIsObsolete(Class<? extends Module> candidate, Collection<Class<? extends Module>> others) {
        for (Class<? extends Module> other : others) {
            if (other.getSuperclass() == candidate) {
                return true;
            }
        }

        return false;
    }

    public Module loadModule(ModuleManager moduleManager, ClassEntry classEntry) {
        // Try to get "from cache" first. We do not allow two modules be activated at the same time, so lets use that
        Module module = registry.getModule(classEntry.getImplementation());
        if (module != null) {
            return module;
        }

        // Seems like we haven't loaded that module before, so let's get started
        final ModuleInformationImpl information = new ModuleInformationImpl(classEntry.getAnnotation());
        final ModuleRegistry.Entry moduleEntry = registry.createEntry(classEntry.getModule(), information);

        try {
            // Instantiate module
            final Constructor<? extends Module> constructor = classEntry.getImplementation().getDeclaredConstructor();
            constructor.setAccessible(true);

            module = constructor.newInstance();

            // Set to load and initialize the module
            injectAndInitialize(moduleManager, module, information, moduleEntry);

            // Set to ready and add to registry, but also add the instance in "ghost mode"
            registerEntry(classEntry, module, information, moduleEntry);

            return module;

        } catch (NoSuchMethodException e) {
            LOG.log(Level.WARNING, "Could not find module constructor", e);

        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOG.log(Level.WARNING, "Could not instantiate module implementation", e);
        }

        return null;
    }

    public void injectAndInitialize(ModuleManager manager, Module module, ModuleInformationImpl information,
                                    ModuleRegistry.Entry moduleEntry) {
        try {
            // Set to loading state
            information.setState(ModuleState.LOADING);

            // Inject dependencies
            injector.inject(moduleEntry, module);

            // Call initialize method
            Annotations.call(module, Initialize.class, 0, new Class[]{ModuleManager.class}, manager);

        } catch (InjectFailedException e) {
            LOG.log(Level.WARNING, "Could not load module implementation", e);

        } catch (InvocationTargetException | IllegalAccessException e) {
            LOG.log(Level.WARNING, "Could not call initialize method on module implementation", e);
        }
    }

    public void registerEntry(ClassEntry classEntry, Module module, ModuleInformationImpl information,
                              ModuleRegistry.Entry moduleEntry) {
        information.setState(ModuleState.READY);
        moduleEntry.setModule(module);

        registry.addModule(classEntry.getModule(), moduleEntry, false);
        registry.addModule(classEntry.getImplementation(), moduleEntry, true);
    }

    public ClassEntry getClassEntry(Class<? extends Module> implClass) {
        // Early checking for null, against module, and if we already checked and saw that it's invalid
        if (implClass == null || Module.class.equals(implClass) || Implementation.Default.class.equals(implClass) ||
                Inject.Current.class.equals(implClass) || INVALID_CACHE.contains(implClass)) {
            return null;
        }

        // Check lookup first
        ClassEntry classEntry = CLASS_CACHE.get(implClass);

        // If we found nothing, take the hard route
        if (classEntry == null) {
            // We do not allow interface or annotation modules - that would not work and is thus just ... silly
            if (implClass.isInterface() || implClass.isAnnotation()) {
                INVALID_CACHE.add(implClass);
                return null;
            }

            // Return null for classes that don't have the annotation
            final Implementation implementation = implClass.getAnnotation(Implementation.class);
            if (implementation == null) {
                INVALID_CACHE.add(implClass);
                return null;
            }

            // Get correct module class
            final Class<? extends Module> module;

            if (!implementation.module().equals(Implementation.Default.class)) {
                // The developer already provided the wanted class so we'll gladly use that instead
                // We also ignore the ignore list - ba dum tss - here, since the dev. explicitly specified it
                module = implementation.module();

            } else {
                // Well, no things found, so we have to discover the module class on our own
                module = getModuleClassRecursively(implClass);

                if (module == null) {
                    INVALID_CACHE.add(implClass);
                    return null;
                }
            }

            // Get dependencies via the injector, create new class entry and add to cache so we don't need to this again
            classEntry = new ClassEntry(module, implClass, implementation, injector.discover(implClass));
            CLASS_CACHE.put(implClass, classEntry);

            // Also add the module, so we can get our dependencies right
            CLASS_CACHE.put(module, classEntry);
        }

        return classEntry;
    }

    @Override
    public void destroy() {
        injector.destroy();
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Module> getModuleClassRecursively(Class<?> aClass) {
        // Let's check interfaces first...
        final Class<?>[] interfaces = aClass.getInterfaces();

        for (Class<?> anInterface : interfaces) {
            if (!Module.class.isAssignableFrom(anInterface)) {
                continue;
            }

            // Even though it was an ignored one, maybe the parent wasn't ignored
            if (ignores.contains(anInterface)) {
                final Class<? extends Module> recursiveLookup = getModuleClassRecursively(anInterface);

                if (recursiveLookup != null) {
                    return recursiveLookup;
                }
            }

            // Seems like we found something
            if (!aClass.isInterface() && Module.class.equals(anInterface)) {
                // Use our class since it probably is a direct implementation
                return (Class<? extends Module>) aClass;
            }

            return (Class<? extends Module>) anInterface;
        }

        // We still didn't find a proper module class search through our parents (superclass of superclass of super...)
        return getModuleClassRecursively(aClass.getSuperclass());
    }


    public final static class ClassEntry {
        private final Class<? extends Module> module;
        private final Class<? extends Module> implementation;
        private final Implementation annotation;
        private final Collection<Injector.Entry> dependencies;


        public ClassEntry(Class<? extends Module> module, Class<? extends Module> implementation,
                          Implementation annotation, Collection<Injector.Entry> dependencies) {
            this.module = module;
            this.implementation = implementation;
            this.annotation = annotation;
            this.dependencies = dependencies;
        }

        public Class<? extends Module> getModule() {
            return module;
        }

        public Class<? extends Module> getImplementation() {
            return implementation;
        }

        public Implementation getAnnotation() {
            return annotation;
        }

        public Collection<Injector.Entry> getDependencies() {
            return dependencies;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClassEntry)) return false;

            ClassEntry that = (ClassEntry) o;

            return annotation.equals(that.annotation) && dependencies.equals(that.dependencies) &&
                    implementation.equals(that.implementation) && module.equals(that.module);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * (31 * module.hashCode() + implementation.hashCode()) + annotation.hashCode()) +
                    dependencies.hashCode();
        }

        @Override
        public String toString() {
            return "ClassEntry{module=" + module + ", implementation=" + implementation + ", annotation=" + annotation +
                    ", dependencies=" + dependencies + '}';
        }

    }

}
