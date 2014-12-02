package net.mountainblade.modular.impl;

import gnu.trove.set.hash.THashSet;
import net.mountainblade.modular.Module;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the DefaultModuleRegistry.
 *
 * @author spaceemotion
 * @version 1.0
 */
public final class DefaultModuleRegistry extends ModuleRegistry {

    public DefaultModuleRegistry() {
        super(new ConcurrentHashMap<Class<? extends Module>, Entry>(), new THashSet<Module>());
    }

}
