package net.mountainblade.modular.impl;

import lombok.extern.java.Log;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.annotations.Inject;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the Injector for module dependencies.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Log
class Injector implements Destroyable {
    private final Map<Class<? extends Module>, Collection<Entry>> cache;

    private final ModuleRegistry registry;


    Injector(ModuleRegistry registry) {
        this.registry = registry;

        this.cache = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public Collection<Entry> discover(Class<? extends Module> implementationClass) {
        Collection<Entry> entries = cache.get(implementationClass);

        if (entries == null) {
            entries = new LinkedList<>();

            // Loop through the fields
            for (Field field : implementationClass.getDeclaredFields()) {
                Inject annotation = field.getAnnotation(Inject.class);
                if (annotation == null) {
                    continue;
                }

                // Fetch dependency and do some checks beforehand
                Class<?> fieldType = field.getType();

                try {
                    if (fieldType.equals(Module.class)) {
                        throw new InjectFailedException("Cannot inject field with raw Material type");
                    }

                    if (fieldType.equals(implementationClass.getClass())) {
                        throw new InjectFailedException("Cannot inject field with itself (Why would you do that?)");
                    }

                    if (Logger.class.equals(fieldType)) {
                        entries.add(new LoggerEntry(annotation, implementationClass, field));

                    } else if (ModuleInformation.class.equals(fieldType)) {
                        entries.add(new InformationEntry(annotation, implementationClass, field));

                    } else if (Module.class.isAssignableFrom(fieldType)) {
                        // Our to normal module injection
                        entries.add(new ModuleEntry(annotation, implementationClass, field,
                                (Class<? extends Module>) fieldType));

                    } else {
                        throw new InjectFailedException("Dependency is not a module or special type: " + fieldType);
                    }

                } catch (InjectFailedException e) {
                    log.log(Level.WARNING, "Error with dependency entry for implementation, injects will fail", e);
                }
            }

            // Add our entries to the cache
            cache.put(implementationClass, entries);
        }

        return entries;
    }

    public void inject(ModuleRegistry.Entry moduleEntry, Module implementation) throws InjectFailedException {
        Class<? extends Module> implementationClass = implementation.getClass();
        Collection<Entry> entries = discover(implementationClass);

        // Loop through the entries and inject the dependencies
        for (Entry entry : entries) {
            if (!entry.getModule().equals(implementationClass)) {
                continue;
            }

            if (!entry.apply(moduleEntry, implementation)) {
                throw new InjectFailedException("Failed to inject dependencies: " + entry.getModule());
            }
        }
    }

    @Override
    public void destroy() {
        cache.clear();
    }


    public abstract class Entry {
        private final String type;

        private final Class<? extends Module> module;
        private final Inject annotation;
        private final Field field;


        protected Entry(String type, Inject annotation, Class<? extends Module> module, Field field) {
            this.type = type;

            this.annotation = annotation;
            this.module = module;
            this.field = field;
        }

        public Class<? extends Module> getModule() {
            return module;
        }

        public Inject getAnnotation() {
            return annotation;
        }

        public Field getField() {
            return field;
        }

        abstract boolean apply(ModuleRegistry.Entry moduleEntry, Module module);

        protected boolean injectField(Module module, Object object) {
            if (object != null) {
                try {
                    field.setAccessible(true);
                    getField().set(module, object);

                    return true;

                } catch (IllegalAccessException e) {
                    log.log(Level.SEVERE, "Could not inject module with " + type, e);
                }
            }

            return getAnnotation().optional();
        }

    }

    public final class LoggerEntry extends Entry {

        protected LoggerEntry(Inject annotation, Class<? extends Module> module, Field field) {
            super("logger", annotation, module, field);
        }

        @Override
        boolean apply(ModuleRegistry.Entry moduleEntry, Module module) {
            Logger logger = Logger.getLogger(module.getClass().getName());
            moduleEntry.setLogger(logger);

            return injectField(module, logger);
        }

    }

    public final class InformationEntry extends Entry {

        protected InformationEntry(Inject annotation, Class<? extends Module> module, Field field) {
            super("information object", annotation, module, field);
        }

        @Override
        boolean apply(ModuleRegistry.Entry moduleEntry, Module module) {
            return injectField(module, moduleEntry.getInformation());
        }

    }

    public final class ModuleEntry extends Entry {
        private final Class<? extends Module> dependency;


        protected ModuleEntry(Inject annotation, Class<? extends Module> module, Field field,
                              Class<? extends Module> dependency) throws InjectFailedException {
            super("module dependency", annotation, module, field);

            this.dependency = dependency;
        }

        public Class<? extends Module> getDependency() {
            return dependency;
        }

        @Override
        @SuppressWarnings("unchecked")
        boolean apply(ModuleRegistry.Entry moduleEntry, Module module) {
            Class<?> fieldType = getField().getType();

            if (fieldType.equals(Module.class)) {
                log.log(Level.WARNING, "Cannot inject field with raw Material type");
                return false;
            }

            if (fieldType.equals(getModule().getClass())) {
                log.log(Level.WARNING, "Cannot inject field with itself (Why would you want to do that?)");
                return false;
            }

            if (Module.class.isAssignableFrom(fieldType)) {
                Class<?> superclass = fieldType;
                Module dependency;

                do {
                    dependency = registry.getModule((Class<? extends Module>) superclass);

                    // Exit when the superclass is not a module anymore
                    Class<?> fieldTypeSuperclass = fieldType.getSuperclass();
                    if (fieldTypeSuperclass == null || !Module.class.isAssignableFrom(fieldTypeSuperclass)) {
                        break;
                    }

                    // Continue with new superclass
                    superclass = fieldTypeSuperclass;

                } while (dependency == null);

                // Inject the dependency
                return injectField(module, dependency);
            }

            return false;
        }

    }

}
