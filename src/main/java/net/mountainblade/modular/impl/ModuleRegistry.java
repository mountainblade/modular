package net.mountainblade.modular.impl;

import gnu.trove.set.hash.THashSet;
import lombok.*;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleInformation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Represents the ModuleRegistry.
 *
 * @author spaceemotion
 * @version 1.0
 */
final class ModuleRegistry implements Destroyable {
    private final Map<Class<? extends Module>, Entry> registry;
    private final Collection<Module> modules;


    ModuleRegistry() {
        registry = new ConcurrentHashMap<>();
        modules = new THashSet<>();
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
        return Collections.unmodifiableCollection(modules);
    }

    void addGhostModule(Class<? extends Module> moduleClass, Module module, ModuleInformation information) {
        Entry entry = new Entry(information);
        entry.setModule(module);

        addModule(moduleClass, entry, true);
    }

    void addModule(Class<? extends Module> moduleClass, Entry entry, boolean ghost) {
        registry.put(moduleClass, entry);

        // If we're "ghosting" we just wanted to add the module to the registration, but it is not a "real" module
        if (!ghost) {
            modules.add(entry.getModule());
        }
    }

    Entry createEntry(Class<? extends Module> moduleClass, ModuleInformation information) {
        if (moduleClass == null) {
            return null;
        }

        Entry entry = new Entry(information);
        registry.put(moduleClass, entry);

        return entry;
    }

    Entry getEntry(Class<? extends Module> moduleClass) {
        return moduleClass == null ? null : registry.get(moduleClass);
    }

    @Override
    public void destroy() {
        registry.clear();
        modules.clear();
    }


    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    static class Entry {
        private final ModuleInformation information;

        @Setter(AccessLevel.PACKAGE)
        private Module module;

        @Setter
        private Logger logger;
    }

}