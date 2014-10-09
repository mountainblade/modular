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
 * Represents the Injector.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Log
class Injector {
    private final ModuleRegistry registry;

    private final Map<Class<? extends Module>, Collection<Entry>> cache; // TODO support multiple annotations


    Injector(ModuleRegistry registry) {
        this.registry = registry;

        this.cache = new ConcurrentHashMap<>();
    }

    public Collection<Entry> discover(Module module) {
        Class<? extends Module> moduleClass = module.getClass();
        Collection<Entry> entries = cache.get(moduleClass);

        if (entries == null) {
            entries = new LinkedList<>();

            // Loop through the fields
            for (Field field : moduleClass.getDeclaredFields()) {
                Inject annotation = field.getAnnotation(Inject.class);

                if (annotation != null) {
                    entries.add(new FieldEntry(module, annotation, field));
                }
            }

            // Add our entries to the cache
            cache.put(moduleClass, entries);
        }

        return entries;
    }

    public void inject(Module module) throws InjectFailedException {
        Collection<Entry> entries = discover(module);

        // Loop through the entries and inject through "apply"
        for (Entry entry : entries) {
            if (!entry.apply()) {
                throw new InjectFailedException("Failed to inject dependencies: " + entry.getModule().getClass());
            }
        }
    }


    public static abstract class Entry {
        private final Module module;
        private final Inject annotation;


        protected Entry(Module module, Inject annotation) {
            this.module = module;
            this.annotation = annotation;
        }

        public Module getModule() {
            return module;
        }

        public Inject getAnnotation() {
            return annotation;
        }

        abstract boolean apply();

    }


    public class FieldEntry extends Entry {
        private final Field field;


        private FieldEntry(Module module, Inject annotation, Field field) {
            super(module, annotation);

            this.field = field;
        }

        @Override
        @SuppressWarnings("unchecked")
        boolean apply() {
            field.setAccessible(true);

            Class<?> declaringClass = field.getDeclaringClass();

            if (Module.class.isAssignableFrom(declaringClass)) {
                Module module = registry.getModule((Class<? extends Module>) declaringClass);

                if (module != null) {
                    try {
                        field.set(getModule(), module);
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
