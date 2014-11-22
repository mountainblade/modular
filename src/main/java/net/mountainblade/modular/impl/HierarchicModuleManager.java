package net.mountainblade.modular.impl;

import com.google.common.base.Optional;
import net.mountainblade.modular.Filter;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.ModuleManager;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Represents a hierarchical ModuleManager.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class HierarchicModuleManager implements ModuleManager {
    private final DefaultModuleManager parent;

    private final Collection<Destroyable> destroyables;
    private final ModuleRegistry registry;
    private final ModuleLoader loader;


    public HierarchicModuleManager(DefaultModuleManager parent) {
        this.parent = parent;

        // Re-create certain instances so we can keep them separate from the parent
        registry = new ModuleRegistry();
        loader = new ModuleLoader(parent.getClassWorld(), registry, new Injector(registry));
        destroyables = new LinkedList<>();

        // Add our destroyable methods
        destroyables.add(registry);
        destroyables.add(loader);
    }

    @Override
    public <T extends Module> T provideSimple(T module) {
        return parent.provide(module, false, registry, loader);
    }

    @Override
    public <T extends Module> T provide(T module) {
        return parent.provide(module, true, registry, loader);
    }

    @Override
    public Collection<Module> loadModules(URI uri, Filter... filters) {
        return parent.loadModules(uri, loader, filters);
    }

    @Override
    public <M extends Module> Optional<M> getModule(Class<M> module) {
        Optional<M> parentModule = parent.getModule(module);
        return parentModule.isPresent() ? parentModule : Optional.fromNullable(registry.getModule(module));
    }

    @Override
    public Optional<ModuleInformation> getInformation(Class<? extends Module> module) {
        Optional<ModuleInformation> parentInfo = parent.getInformation(module);
        return parentInfo.isPresent() ? parentInfo : Optional.fromNullable(registry.getInformation(module));
    }

    @Override
    public Collection<Module> getModules() {
        return CollectionHelper.combine(parent.getModules(), registry.getModules());
    }

    @Override
    public void shutdown() {
        shutdown(false);
    }

    public void shutdown(boolean withParent) {
        parent.shutdown(registry, loader);

        if (withParent) {
            parent.shutdown();
        }

        // Also destroy our own objects
        for (Destroyable destroyable : destroyables) {
            destroyable.destroy();
        }
    }

}
