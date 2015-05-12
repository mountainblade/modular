package net.mountainblade.modular.impl;

import gnu.trove.set.hash.THashSet;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the default implementation of a {@link net.mountainblade.modular.ModuleManager}.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class DefaultModuleManager extends BaseModuleManager {

    public DefaultModuleManager() {
        super(new ModuleRegistry(
                new ConcurrentHashMap<Class<? extends Module>, ModuleRegistry.Entry>(), new THashSet<Module>()
        ), null);

        // Also register ourselves so other modules can use this as implementation via injection
        getRegistry().addGhostModule(ModuleManager.class, this, new MavenModuleInformation());
    }

    @Override
    public void shutdown() {
        // Send shut down signal to all registered modules
        shutdown(getRegistry().getModuleCollection().iterator());
    }

}
