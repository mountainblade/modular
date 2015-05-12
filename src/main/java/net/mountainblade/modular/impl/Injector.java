package net.mountainblade.modular.impl;

import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.annotations.Inject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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
public final class Injector extends Destroyable {
    private static final Logger LOG = Logger.getLogger(Injector.class.getName());

    private final Map<Class<? extends Module>, Collection<Entry>> cache;
    private final List<RegistryEntry> supports;

    private final ModuleRegistry registry;


    Injector(ModuleRegistry registry) {
        this.registry = registry;

        this.supports = new LinkedList<>();
        this.cache = new ConcurrentHashMap<>();

        addSupport(new EntryConstructor() {
            @Override
            public Entry construct(Inject annotation, Class<? extends Module> module, Field field) {
                return new LoggerEntry(annotation, module, field);
            }
        }, Logger.class, true);

        addSupport(new EntryConstructor() {
            @Override
            public Entry construct(Inject annotation, Class<? extends Module> module, Field field) {
                return new InformationEntry(annotation, module, field);
            }
        }, ModuleInformation.class, true);

        addSupport(new EntryConstructor() {
            @Override
            @SuppressWarnings("unchecked")
            public Entry construct(Inject annotation, Class<? extends Module> module, Field field) {
                return new ModuleEntry(annotation, module, field, (Class<? extends Module>) field.getType());
            }
        }, Module.class, false);
    }

    public void addSupport(EntryConstructor entry, Class classToMatch, boolean exactMatch) {
        supports.add(new RegistryEntry(entry, classToMatch, exactMatch));
    }

    public Collection<Entry> discover(Class<? extends Module> implementationClass) {
        Collection<Entry> entries = cache.get(implementationClass);

        if (entries == null) {
            entries = new LinkedList<>();

            // Discover normal class fields
            discover(implementationClass, entries, implementationClass.getDeclaredFields());

            // Also discover fields from superclass
            Class superClass = implementationClass.getSuperclass();

            while (superClass != null && !superClass.equals(Class.class)) {
                discover(implementationClass, entries, superClass.getDeclaredFields());
                superClass = superClass.getSuperclass();
            }

            // Add our entries to the cache
            cache.put(implementationClass, entries);
        }

        return entries;
    }

    @SuppressWarnings("unchecked")
    private void discover(Class<? extends Module> implementationClass, Collection<Entry> entries, Field[] fields) {
        // Loop through the fields
        for (Field field : fields) {
            // We do not want static fields
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            // Check inject annotation
            final Inject annotation = field.getAnnotation(Inject.class);
            if (annotation == null) {
                continue;
            }

            // Fetch dependency and do some checks beforehand
            final Class<?> fieldType = field.getType();

            try {
                if (fieldType.equals(Module.class)) {
                    throw new InjectFailedException("Cannot inject field with raw Module type");
                }

                if (fieldType.equals(implementationClass.getClass())) {
                    throw new InjectFailedException("Cannot inject field with itself (Why would you do that?)");
                }

                // Loop through our supports in reverse order
                final ListIterator<RegistryEntry> iterator = supports.listIterator(supports.size());
                boolean added = false;

                while (iterator.hasPrevious()) {
                    final RegistryEntry support = iterator.previous();

                    // Check if we got a match
                    if (!(support.exactMatch ? support.classEntry.equals(fieldType) :
                                                    support.classEntry.isAssignableFrom(fieldType))) {
                        continue;
                    }

                    // We found an injector, let's use that
                    entries.add(support.constructor.construct(annotation, implementationClass, field));
                    added = true;
                }

                if (!added) {
                    // We did not process the field correctly, so throw an error
                    throw new InjectFailedException("Dependency is not a module or special type: " + fieldType);
                }

            } catch (InjectFailedException e) {
                LOG.log(Level.WARNING, "Error with dependency entry for implementation, injects will fail", e);
            }
        }
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
    protected void destroy() {
        cache.clear();
    }


    private static class RegistryEntry {
        private final EntryConstructor constructor;
        private final Class classEntry;
        private final boolean exactMatch;


        private RegistryEntry(EntryConstructor constructor, Class classEntry, boolean exactMatch) {
            this.constructor = constructor;
            this.exactMatch = exactMatch;
            this.classEntry = classEntry;
        }
    }

    public interface EntryConstructor {
        Entry construct(Inject annotation, Class<? extends Module> module, Field field);
    }

    public static abstract class Entry {
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

        protected abstract boolean apply(ModuleRegistry.Entry moduleEntry, Module module);

        protected boolean injectField(Module module, Object object) {
            if (object != null) {
                try {
                    field.setAccessible(true);
                    getField().set(module, object);

                    return true;

                } catch (IllegalAccessException e) {
                    LOG.log(Level.SEVERE, "Could not inject module with " + type, e);
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
        protected boolean apply(ModuleRegistry.Entry moduleEntry, Module module) {
            Logger logger = moduleEntry.getLogger();

            if (logger == null) {
                logger = Logger.getLogger(module.getClass().getName());
                moduleEntry.setLogger(logger);
            }

            return injectField(module, logger);
        }

    }

    public final class InformationEntry extends Entry {

        protected InformationEntry(Inject annotation, Class<? extends Module> module, Field field) {
            super("information object", annotation, module, field);
        }

        @Override
        protected boolean apply(ModuleRegistry.Entry moduleEntry, Module module) {
            return injectField(module, moduleEntry.getInformation());
        }

    }

    public final class ModuleEntry extends Entry {
        private final Class<? extends Module> dependency;


        protected ModuleEntry(Inject annotation, Class<? extends Module> module, Field field,
                              Class<? extends Module> dependency) {
            super("module dependency", annotation, module, field);

            this.dependency = dependency;
        }

        public Class<? extends Module> getDependency() {
            return dependency;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected boolean apply(ModuleRegistry.Entry moduleEntry, Module module) {
            final Class<?> fieldType = getField().getType();

            if (!Module.class.isAssignableFrom(fieldType)) {
                return false;
            }

            Class<?> superclass = fieldType;
            Module dependency;

            do {
                dependency = registry.getModule((Class<? extends Module>) superclass);

                // Exit when the superclass is not a module anymore
                final Class<?> fieldTypeSuperclass = fieldType.getSuperclass();
                if (fieldTypeSuperclass == null || !Module.class.isAssignableFrom(fieldTypeSuperclass)) {
                    break;
                }

                // Continue with new superclass
                superclass = fieldTypeSuperclass;

            } while (dependency == null);

            // Inject the dependency
            return injectField(module, dependency);

        }

    }

}
