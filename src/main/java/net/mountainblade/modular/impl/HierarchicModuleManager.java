package net.mountainblade.modular.impl;

import com.google.common.base.Optional;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleInformation;

/**
 * Represents a hierarchical ModuleManager that inherits the modules from its parent, but allows scoped sub-modules.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class HierarchicModuleManager extends BaseModuleManager {
    private final DefaultModuleManager parent;


    public HierarchicModuleManager(DefaultModuleManager parent) {
        super(new HierarchicModuleRegistry(parent.getRegistry()), newRealm(parent.getLoader().getRealm()));

        this.parent = parent;
    }

    @Override
    public <M extends Module> Optional<M> getModule(Class<M> module) {
        Optional<M> parentModule = parent.getModule(module);
        return parentModule.isPresent() ? parentModule : super.getModule(module);
    }

    @Override
    public Optional<ModuleInformation> getInformation(Class<? extends Module> module) {
        Optional<ModuleInformation> parentInfo = parent.getInformation(module);
        return parentInfo.isPresent() ? parentInfo : super.getInformation(module);
    }

    @Override
    public void shutdown() {
        shutdown(false);
    }

    public void shutdown(boolean withParent) {
        final HierarchicModuleRegistry registry = (HierarchicModuleRegistry) getRegistry();
        shutdown(withParent ? registry.getModuleCollection().iterator() : registry.getChildModules());
    }

}
