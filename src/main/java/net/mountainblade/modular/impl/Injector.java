/**
 * Copyright (C) 2014-2015 MountainBlade (http://mountainblade.net)
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a dependency Injector for various kinds of dependencies (only affects fields).
 *
 * @author spaceemotion
 * @version 1.0
 */
public class Injector extends Destroyable {
    private static final Logger LOG = Logger.getLogger(Injector.class.getName());

    private final Map<Class<? extends Module>, Collection<Entry>> cache;
    private final ModuleRegistry registry;
    private final List<Builder> builders;


    Injector(ModuleRegistry registry) {
        this.registry = registry;
        this.builders = new LinkedList<>();
        this.cache = new ConcurrentHashMap<>();

        // Destroy to initialize with defaults
        destroy();
    }

    /**
     * Creates a new injection builder to set up a new field injection configuration.
     *
     * @param fieldClass    The type of fields this configuration / builder should affect
     * @param <T>           The type of the field, used to limit instances to their proper type
     * @return A new type-safe builder
     */
    public <T> Builder<T> inject(Class<T> fieldClass) {
        return new Builder<>(fieldClass);
    }

    /**
     * Injects the given module registry entry with its dependencies.
     *
     * @param moduleEntry    The entry to inject
     * @throws InjectFailedException if the injection fails
     */
    public void inject(ModuleRegistry.Entry moduleEntry) throws InjectFailedException {
        inject(moduleEntry.getModule());
    }

    /**
     * Injects the given module instance with its dependencies.
     *
     * @param module    The module instance
     * @throws InjectFailedException if the injection fails
     */
    public void inject(Module module) throws InjectFailedException {
        for (Entry entry : discover(module.getClass())) {
            if (entry == null) {
                continue;
            }

            entry.apply(module);
        }
    }

    Collection<Entry> discover(Class<? extends Module> moduleClass) {
        Collection<Entry> entries = cache.get(moduleClass);

        if (entries == null) {
            entries = new LinkedList<>();

            // Discover normal class fields
            discover(moduleClass, entries, moduleClass.getDeclaredFields());

            // Also discover fields from superclass
            Class superClass = moduleClass.getSuperclass();

            while (superClass != null && !superClass.equals(Class.class)) {
                discover(moduleClass, entries, superClass.getDeclaredFields());
                superClass = superClass.getSuperclass();
            }

            // Add our entries to the cache
            cache.put(moduleClass, entries);
        }

        return entries;
    }

    @SuppressWarnings("unchecked")
    private void discover(Class<? extends Module> implementationClass, Collection<Entry> entries, Field[] fields) {
        fields: for (Field field : fields) {
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

                for (ListIterator<Builder> iterator = builders.listIterator(builders.size()); iterator.hasPrevious();) {
                    final Builder builder = iterator.previous();

                    // Check if we got a match
                    if (!(builder.exactMatch ? builder.fieldClass.equals(fieldType) :
                            builder.fieldClass.isAssignableFrom(fieldType))) {
                        continue;
                    }

                    // Check requirements (god I hate type erasure)
                    for (Class<? extends Annotation> annotationClass :
                            (Collection<Class<? extends Annotation>>) builder.annotationClasses) {
                        if (field.getAnnotation(annotationClass) == null) {
                            continue fields;
                        }
                    }

                    for (Annotation annotationImpl : (Collection<Annotation>) builder.annotations) {
                        final Annotation fieldAnnotation = field.getAnnotation(annotationImpl.annotationType());
                        if (!annotationImpl.equals(fieldAnnotation)) {
                            continue fields;
                        }
                    }

                    // We found an injector, let's use it
                    final Class<? extends Module> from = annotation.from();
                    final boolean useFrom = !from.equals(Inject.Current.class);
                    if (useFrom) {
                        checkModuleField(implementationClass, from);
                    }

                    entries.add(new Entry(useFrom ? from : Module.class.isAssignableFrom(fieldType) ?
                            (Class<? extends Module>) fieldType : implementationClass,
                            builder, annotation, field, useFrom));
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

    @Override
    protected void destroy() {
        cache.clear();
        builders.clear();

        // Reset default injectors
        inject(Logger.class).with(new Constructor<Logger>() {
            @Override
            public Logger construct(Inject annotation, Class<? extends Logger> type, Module module) {
                return Logger.getLogger(module.getClass().getName());
            }
        });
        inject(Module.class).with(new Constructor<Module>() {
            @Override
            public Module construct(Inject annotation, Class<? extends Module> type, Module module) {
                return registry.getModule(type);
            }
        });
        inject(ModuleInformation.class).with(new Constructor<ModuleInformation>() {
            @Override
            public ModuleInformation construct(Inject annotation, Class<? extends ModuleInformation> type, Module module) {
                return registry.getInformation(module.getClass());
            }
        });
    }


    class Entry {
        private final Class<? extends Module> dependency;
        private final Builder builder;
        private final Inject annotation;
        private final Field field;
        private final boolean useFrom;


        Entry(Class<? extends Module> dependency, Builder builder, Inject annotation, Field field, boolean useFrom) {
            this.dependency = dependency;
            this.builder = builder;
            this.annotation = annotation;
            this.field = field;
            this.useFrom = useFrom;
        }

        public Class<? extends Module> getDependency() {
            return dependency;
        }

        @SuppressWarnings("unchecked")
        private void apply(Module module) throws InjectFailedException {
            final Module theModule = useFrom ? registry.getModule(dependency) : module;
            if (theModule == null) {
                throw new InjectFailedException("Could not get module for " + dependency);
            }

            final Object object = builder.constructor.construct(annotation, field.getType(), theModule);
            if (object == null) {
                if (!annotation.optional() && !builder.nullable) {
                    final int modifiers = field.getModifiers();
                    final Type fieldType = field.getGenericType();
                    throw new InjectFailedException("Could not inject " + theModule.getClass() + " with null object " +
                            "for field \"" + (((modifiers == 0) ? "" : (Modifier.toString(modifiers) + " ")) +
                            fieldType.getTypeName()) + " " + field.getName() + '"');
                }

                return;
            }

            try {
                field.setAccessible(true);
                field.set(module, object);

            } catch (IllegalAccessException e) {
               throw new InjectFailedException("Could not inject " + dependency + " with " + field.getType(), e);
            }
        }

    }

    /**
     * Represents an injection builder.
     *
     * @param <T>    The type of instances we provide / construct
     */
    public class Builder<T> {
        private final Class<T> fieldClass;
        private final Collection<Class<? extends Annotation>> annotationClasses;
        private final Collection<Annotation> annotations;
        private Constructor<? extends T> constructor;
        private boolean nullable;
        private boolean exactMatch;


        Builder(Class<T> fieldClass) {
            this.fieldClass = fieldClass;
            this.annotationClasses = new LinkedList<>();
            this.annotations = new LinkedList<>();
        }

        /**
         * Sets the injected instance to always be the given one.
         *
         * @param instance    The instance we should populate the field with
         */
        public void with(final T instance) {
            with(new Constructor<T>() {
                @Override
                public T construct(Inject annotation, Class<? extends T> type, Module module) {
                    return instance;
                }
            });
        }

        /**
         * Sets the constructor to the given instance and adds this builder to the registry.
         *
         * @param constructor    The instance constructor
         */
        public void with(Constructor<? extends T> constructor) {
            this.constructor = constructor;
            builders.add(this);
        }

        /**
         * Marks this configuration as "nullable".
         * It will not throw an exception, even if the returned instance is null.
         *
         * @return This builder to allow for method chaining
         */
        public Builder<T> nullable() {
            this.nullable = true;
            return this;
        }

        /**
         * Marks this configuration to restrict the field type to be exactly the given one.
         *
         * If this builder's field type was set to {@code MyPlugin.class} and the "exactly" flag set to true
         * this will only affect fields that have its type set to that, and will ignore fields using subclasses
         * of the plugin's class.
         *
         * @return This builder to allow for method chaining
         */
        public Builder<T> exactly() {
            this.exactMatch = true;
            return this;
        }

        /**
         * Adds the given annotation instance to the required annotations.
         * This method can be called multiple times to add multiple annotation requirements.
         *
         * @param annotation    An annotation implementation to check against
         * @return This builder to allow for method chaining
         */
        public Builder<T> marked(Annotation annotation) {
            this.annotations.add(annotation);
            return this;
        }

        /**
         * Adds the given annotation type to the required annotations.
         * This method can be called multiple times to add multiple annotation requirements.
         *
         * @param annotation    An annotation class
         * @return This builder to allow for method chaining
         */
        public Builder<T> marked(Class<? extends Annotation> annotation) {
            this.annotationClasses.add(annotation);
            return this;
        }

    }

    /**
     * Represents an instance constructor.
     * Is used when injecting fields with instances.
     *
     * @param <T>    The type of the instance
     */
    public interface Constructor<T> {
        /**
         * Constructs (or provides) an instance of the set type.
         *
         * @param annotation    The {@link Inject} field annotation.
         * @param type          The field's type
         * @param module        The module instance
         * @return An instance, but also allows null
         */
        T construct(Inject annotation, Class<? extends T> type, Module module);
    }

}
