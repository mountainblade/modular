package net.mountainblade.modular.impl;

import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleInformation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

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

    @SuppressWarnings("unchecked")
    public <M extends Module> M getModule(Class<M> moduleClass) {
        Entry entry = getEntry(moduleClass);
        return entry == null ? null : (M) entry.getModule();
    }

    public ModuleInformation getInformation(Class<? extends Module> moduleClass) {
        Entry entry = getEntry(moduleClass);
        return entry == null ? null : entry.getInformation();
    }

    public Collection<Module> getModules() {
        return Collections.unmodifiableCollection(getModuleCollection());
    }

    protected void addGhostModule(Class<? extends Module> moduleClass, Module module, ModuleInformation information) {
        Entry entry = new Entry(information, moduleClass);
        entry.setModule(module);

        addModule(moduleClass, entry, true);
    }

    protected void addModule(Class<? extends Module> moduleClass, Entry entry, boolean ghost) {
        getRegistry().put(moduleClass, entry);

        // If we're "ghosting" we just wanted to add the module to the registration, but it is not a "real" module
        if (!ghost) {
            getModuleCollection().add(entry.getModule());
        }
    }

    protected  Entry createEntry(Class<? extends Module> moduleClass, ModuleInformation information) {
        if (moduleClass == null) {
            return null;
        }

        Entry entry = new Entry(information, moduleClass);
        getRegistry().put(moduleClass, entry);

        return entry;
    }

    protected Entry getEntry(Class<? extends Module> moduleClass) {
        return moduleClass == null ? null : getRegistry().get(moduleClass);
    }

    @Override
    protected void destroy() {
        getRegistry().clear();
        getModuleCollection().clear();
    }

    public Map<Class<? extends Module>, Entry> getRegistry() {
        return registry;
    }

    protected Collection<Module> getModuleCollection() {
        return modules;
    }


    public static final class Entry {
        private final ModuleInformation information;
        private final Class<? extends Module> moduleClass;
        private Module module;
        private Logger logger;


        protected Entry(ModuleInformation information, Class<? extends Module> moduleClass) {
            this.information = information;
            this.moduleClass = moduleClass;
        }

        public ModuleInformation getInformation() {
            return information;
        }

        public Class<? extends Module> getModuleClass() {
            return moduleClass;
        }

        public Module getModule() {
            return module;
        }

        protected void setModule(Module module) {
            this.module = module;
        }

        public Logger getLogger() {
            return logger;
        }

        protected void setLogger(Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            return information.equals(entry.information) &&
                    !(logger != null ? !logger.equals(entry.logger) : entry.logger != null) &&
                    !(module != null ? !module.equals(entry.module) : entry.module != null) &&
                    moduleClass.equals(entry.moduleClass);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * (31 * information.hashCode() + moduleClass.hashCode()) +
                    (module != null ? module.hashCode() : 0)) + (logger != null ? logger.hashCode() : 0);
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "information=" + information +
                    ", moduleClass=" + moduleClass +
                    ", module=" + module +
                    '}';
        }

    }

}
