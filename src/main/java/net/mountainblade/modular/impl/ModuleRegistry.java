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
import net.mountainblade.modular.ModuleState;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

/**
 * Represents a registry for modules. It also stores all additional instances that belong to that module.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class ModuleRegistry extends Destroyable {
    private final Map<Class<? extends Module>, Entry> registry;
    private final Collection<Module> modules;


    ModuleRegistry(Map<Class<? extends Module>, Entry> registry, Collection<Module> modules) {
        this.registry = registry;
        this.modules = modules;
    }

    /**
     * Gets a module by its class.
     * This also can fetch an implementation by its implementing module.
     *
     * @param moduleClass    The class of the module
     * @param <M>            The module type
     * @return The instance of the module or null
     */
    @SuppressWarnings("unchecked")
    public <M extends Module> M getModule(Class<M> moduleClass) {
        Entry entry = getEntry(moduleClass);
        return entry == null ? null : (M) entry.getModule();
    }

    /**
     * Gets the module information for the given module.
     *
     * @param moduleClass    The class of the module
     * @return The module information or null
     */
    public ModuleInformation getInformation(Class<? extends Module> moduleClass) {
        Entry entry = getEntry(moduleClass);
        return entry == null ? null : entry.getInformation();
    }

    /**
     * Gets a collection of all currently registered modules.
     *
     * @return A collection of all modules
     */
    public Collection<Module> getModules() {
        return Collections.unmodifiableCollection(modules);
    }

    /**
     * Gets a collection of all registered modules with the given state.
     *
     * @param state    The state to check against
     * @return A collection of the filtered modules
     */
    public Collection<Module> getModules(ModuleState state) {
        final Collection<Module> modules = new LinkedList<>();

        for (Module module : getModules()) {
            final ModuleInformation information = getInformation(module.getClass());
            if (state.equals(information.getState())) {
                modules.add(module);
            }
        }

        return Collections.unmodifiableCollection(modules);
    }

    /**
     * Adds a "ghost" module which has an entry but is not added to the loaded modules list
     * and thus will only be visible to the injector.
     *
     * @param moduleClass    The class of the module we're implementing
     * @param module         The implementation
     * @param information    The information instance
     */
    protected void addGhostModule(Class<? extends Module> moduleClass, Module module, ModuleInformation information) {
        final Entry entry = new Entry(information, moduleClass);
        entry.setModule(module);

        addModule(moduleClass, entry, true);
    }

    /**
     * Adds a module to the registry.
     *
     * @param moduleClass    The class of the module we're implementing
     * @param entry          The registry entry
     * @param ghost          False if it should be added to collection, true if it should be hidden
     */
    protected void addModule(Class<? extends Module> moduleClass, Entry entry, boolean ghost) {
        registry.put(moduleClass, entry);

        // If we're "ghosting" we just wanted to add the module to the registration, but it is not a "real" module
        if (!ghost) {
            modules.add(entry.getModule());
        }
    }

    /**
     * Creates a new registry entry for the given module.
     * Please keep in mind that this will already add the entry to the registry.
     *
     * @param moduleClass    The class of the module we're implementing
     * @param information    The module's information
     * @return A new registry entry
     */
    protected Entry createEntry(Class<? extends Module> moduleClass, ModuleInformation information) {
        if (moduleClass == null) {
            return null;
        }

        Entry entry = new Entry(information, moduleClass);
        registry.put(moduleClass, entry);

        return entry;
    }

    /**
     * Gets the registry entry for the given module.
     *
     * @param moduleClass    The class of the module we're implementing
     * @return An entry or null
     */
    protected Entry getEntry(Class<? extends Module> moduleClass) {
        return moduleClass == null ? null : registry.get(moduleClass);
    }

    @Override
    protected void destroy() {
        registry.clear();
        modules.clear();
    }

    /**
     * Returns the internal registry.
     *
     * @return An unmodifiable map representing the registry
     */
    protected Map<Class<? extends Module>, Entry> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }


    /**
     * Represents the registry entry DTO.
     */
    public static final class Entry {
        private final ModuleInformation information;
        private final Class<? extends Module> moduleClass;
        private Module module;


        private Entry(ModuleInformation information, Class<? extends Module> moduleClass) {
            this.information = information;
            this.moduleClass = moduleClass;
        }

        /**
         * Gets the module's information.
         *
         * @return The information object
         */
        public ModuleInformation getInformation() {
            return information;
        }

        /**
         * Returns the class of the module we're implementing.
         *
         * @return The module class
         */
        public Class<? extends Module> getModuleClass() {
            return moduleClass;
        }

        /**
         * Returns the module implementation instance.
         *
         * @return The instance
         */
        public Module getModule() {
            return module;
        }

        void setModule(Module module) {
            this.module = module;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            return information.equals(entry.information) &&
                    !(module != null ? !module.equals(entry.module) : entry.module != null) &&
                    moduleClass.equals(entry.moduleClass);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * (31 * information.hashCode() + moduleClass.hashCode()) +
                    (module != null ? module.hashCode() : 0));
        }

        @Override
        public String toString() {
            return "Entry{information=" + information + ", moduleClass=" + moduleClass + ", module=" + module + '}';
        }

    }

}
