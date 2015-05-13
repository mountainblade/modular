/**
 * Copyright (C) 2014 MountainBlade (http://mountainblade.net)
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
    private final List<Support> supports;

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
                return new ModuleEntry(annotation, (Class<? extends Module>) field.getType(), field);
            }
        }, Module.class, false);
    }

    public void addSupport(EntryConstructor entry, Class classToMatch, boolean exactMatch) {
        supports.add(new Support(entry, classToMatch, exactMatch));
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
                checkModuleField(implementationClass, fieldType);

                // Loop through our supports in reverse order to account for class overwrites
                boolean added = false;

                for (ListIterator<Support> iterator = supports.listIterator(supports.size()); iterator.hasPrevious();) {
                    final Support support = iterator.previous();

                    // Check if we got a match
                    if (!(support.exactMatch ? support.classEntry.equals(fieldType) :
                                                    support.classEntry.isAssignableFrom(fieldType))) {
                        continue;
                    }

                    // We found an injector, let's use that
                    final Class<? extends Module> from = annotation.from();
                    final boolean useFrom = !from.equals(Inject.Current.class);
                    if (useFrom) {
                        checkModuleField(implementationClass, from);
                    }

                    entries.add(support.constructor.construct(annotation, useFrom ? from : implementationClass, field));
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

    private void checkModuleField(Class<? extends Module> implementationClass, Class<?> fieldType)
            throws InjectFailedException {
        if (fieldType.equals(Module.class)) {
            throw new InjectFailedException("Cannot inject field with raw Module type");
        }

        if (fieldType.equals(implementationClass.getClass())) {
            throw new InjectFailedException("Cannot inject field with itself (Why would you do that?)");
        }
    }

    public void inject(ModuleRegistry.Entry moduleEntry, Module module, ModuleLoader loader)
            throws InjectFailedException {
        final Class<? extends Module> implementationClass = module.getClass();

        // Loop through the entries and inject the dependencies
        for (Entry entry : discover(implementationClass)) {
            if (!entry.apply(moduleEntry, module, loader)) {
                throw new InjectFailedException("Failed to inject dependencies: " + entry.getModule());
            }
        }
    }

    @Override
    protected void destroy() {
        cache.clear();
    }


    private static class Support {
        private final EntryConstructor constructor;
        private final Class classEntry;
        private final boolean exactMatch;


        private Support(EntryConstructor constructor, Class classEntry, boolean exactMatch) {
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

        public final Class<? extends Module> getModule() {
            return module;
        }

        public final Inject getAnnotation() {
            return annotation;
        }

        public final Field getField() {
            return field;
        }

        protected abstract boolean apply(ModuleRegistry.Entry moduleEntry, Module module, ModuleLoader loader);

        protected boolean injectField(Module module, Object object) {
            if (object != null) {
                try {
                    field.setAccessible(true);
                    field.set(module, object);
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
        protected boolean apply(ModuleRegistry.Entry moduleEntry, Module module, ModuleLoader loader) {
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
            super("module information", annotation, module, field);
        }

        @Override
        protected boolean apply(ModuleRegistry.Entry moduleEntry, Module module, ModuleLoader loader) {
            return injectField(module, (!getAnnotation().from().equals(Inject.Current.class) ?
                    registry.getEntry(getModule()) : moduleEntry).getInformation());
        }

    }

    public final class ModuleEntry extends Entry {

        protected ModuleEntry(Inject annotation, Class<? extends Module> module, Field field) {
            super("module dependency", annotation, module, field);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected boolean apply(ModuleRegistry.Entry moduleEntry, Module module, ModuleLoader loader) {
            Class<?> superclass = getField().getType();
            Module dependency;

            do {
                dependency = registry.getModule((Class<? extends Module>) superclass);

                // Exit when the superclass is not a module anymore
                final Class<?> fieldTypeSuperclass = superclass.getSuperclass();
                if (fieldTypeSuperclass == null || !Module.class.isAssignableFrom(fieldTypeSuperclass)) {
                    break;
                }

                // Continue with new superclass
                superclass = fieldTypeSuperclass;

            } while (dependency == null);

            return injectField(module, dependency);
        }

    }

}
