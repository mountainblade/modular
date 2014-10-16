package net.mountainblade.modular.impl;

import lombok.extern.java.Log;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.annotations.Inject;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Represents the Injector for module dependencies.
 *
 * @author spaceemotion
 * @version 1.0
 */
// TODO support other stuff to inject: logger, information, ...
@Log
class Injector implements Destroyable {
    private final Map<Class<? extends Module>, Collection<Entry>> cache;

    private final ModuleRegistry registry;


    Injector(ModuleRegistry registry) {
        this.registry = registry;

        this.cache = new ConcurrentHashMap<>();
    }

    public Collection<Entry> discover(Class<? extends Module> implementationClass) {
        Collection<Entry> entries = cache.get(implementationClass);

        if (entries == null) {
            entries = new LinkedList<>();

            // Loop through the fields
            for (Field field : implementationClass.getDeclaredFields()) {
                Inject annotation = field.getAnnotation(Inject.class);

                if (annotation != null) {
                    try {
                        entries.add(new ModuleEntry(implementationClass, annotation, field));

                    } catch (InjectFailedException e) {
                        log.log(Level.WARNING, "Error with dependency entry for implementation, injects will fail", e);
                    }
                }
            }

            // Add our entries to the cache
            cache.put(implementationClass, entries);
        }

        return entries;
    }

    public void inject(Module implementation) throws InjectFailedException {
        Class<? extends Module> implementationClass = implementation.getClass();
        Collection<Entry> entries = discover(implementationClass);

        // Loop through the entries and inject the dependencies
        for (Entry entry : entries) {
            if (!entry.getModule().equals(implementationClass)) {
                continue;
            }

            if (!entry.apply(implementation)) {
                throw new InjectFailedException("Failed to inject dependencies: " + entry.getModule());
            }
        }
    }

    @Override
    public void destroy() {
        cache.clear();
    }


    public abstract class Entry {
        protected final Class<? extends Module> module;
        protected final Inject annotation;
        protected final Field field;


        protected Entry(Inject annotation, Class<? extends Module> module, Field field) {
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

        abstract boolean apply(Module module);
    }

    public final class ModuleEntry extends Entry {
        private final Class<? extends Module> dependency;


        @SuppressWarnings("unchecked")
        protected ModuleEntry(Class<? extends Module> module, Inject annotation, Field field) throws InjectFailedException {
            super(annotation, module, field);

            // Fetch dependency and do some checks beforehand
            Class<?> fieldType = getField().getType();

            if (fieldType.equals(Module.class)) {
                throw new InjectFailedException("Cannot inject field with raw Material type");
            }

            if (fieldType.equals(module.getClass())) {
                throw new InjectFailedException("Cannot inject field with itself (Why would you want to do that?)");
            }

            if (!Module.class.isAssignableFrom(fieldType)) {
                throw new InjectFailedException("Dependency is not a module: " + fieldType);
            }

            dependency = (Class<? extends Module>) fieldType;
        }

        public Class<? extends Module> getDependency() {
            return dependency;
        }

        @Override
        @SuppressWarnings("unchecked")
        boolean apply(Module module) {
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

                if (dependency != null) {
                    try {
                        field.setAccessible(true);
                        getField().set(module, dependency);

                        return true;

                    } catch (IllegalAccessException e) {
                        log.log(Level.SEVERE, "Could not inject module with dependency", e);
                    }
                }

                return getAnnotation().optional();
            }

            return false;
        }

    }

}
