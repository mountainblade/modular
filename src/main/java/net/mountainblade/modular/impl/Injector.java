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
@Log
class Injector extends Destroyable {
    private final ModuleRegistry registry;

    private final Map<Class<? extends Module>, Collection<Entry>> cache;


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
                    entries.add(new Entry(module, annotation, field));
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

    @Override
    void destroy() {
        cache.clear();
    }


    public final class Entry {
        private final Module module;
        private final Inject annotation;
        private final Field field;


        protected Entry(Module module, Inject annotation, Field field) {
            this.module = module;
            this.annotation = annotation;
            this.field = field;
        }

        public Module getModule() {
            return module;
        }

        public Inject getAnnotation() {
            return annotation;
        }

        public Field getField() {
            return field;
        }

        @SuppressWarnings("unchecked")
        boolean apply() {
            field.setAccessible(true);

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
                Module module;

                do {
                    module = registry.getModule((Class<? extends Module>) superclass);
                    superclass = fieldType.getSuperclass();

                    // Exit when the superclass is not a module anymore
                    if (superclass == null || !Module.class.isAssignableFrom(superclass)) {
                        break;
                    }

                } while (module == null);

                if (module != null) {
                    try {
                        getField().set(getModule(), module);
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
