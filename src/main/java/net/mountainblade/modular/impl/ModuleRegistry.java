package net.mountainblade.modular.impl;

import gnu.trove.set.hash.THashSet;
import lombok.*;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleInformation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the ModuleRegistry.
 *
 * @author spaceemotion
 * @version 1.0
 */
public final class ModuleRegistry extends Destroyable {
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
        addModule(moduleClass, new Entry(module, information), true);
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

        Entry entry = new Entry(null, information);
        registry.put(moduleClass, entry);

        return entry;
    }

    Entry getEntry(Class<? extends Module> moduleClass) {
        return moduleClass == null ? null : registry.get(moduleClass);
    }

    @Override
    void destroy() {
        registry.clear();
        modules.clear();
    }


    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    static class Entry {
        @Setter(AccessLevel.PACKAGE)
        private Module module;
        private final ModuleInformation information;
    }

}
