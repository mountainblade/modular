package net.mountainblade.modular.impl;

import net.mountainblade.modular.Module;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the ModuleRegistry.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class ModuleRegistry {
    private final Map<Class<? extends Module>, Module> registry;


    ModuleRegistry() {
        registry = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <M extends Module> M getModule(Class<M> moduleClass) {
        return (M) registry.get(moduleClass);
    }

    public Collection<Module> getModules() {
        return registry.values();
    }

}
