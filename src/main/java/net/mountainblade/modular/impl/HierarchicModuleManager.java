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
    private final BaseModuleManager parent;

    /**
     * Creates a new hierarchic module manager instance.
     *
     * This will initialize a new manager while keeping access to the given parent.
     * The new manager will have a custom registry and loader and be contained inside
     * a its own class realm, but will be able to fetch modules from its parent.
     *
     * @param parent    The parent manager to use as a reference
     * @see DefaultModuleManager
     */
    public HierarchicModuleManager(BaseModuleManager parent) {
        this(parent, null);
    }

    /**
     * Creates a new hierarchic module manager instance with a custom parent class loader.
     *
     * @param parent    The parent manager to use as a reference
     * @param loader    The custom class loader
     * @see #HierarchicModuleManager(BaseModuleManager)
     */
    public HierarchicModuleManager(BaseModuleManager parent, ClassLoader loader) {
        super(new HierarchicModuleRegistry(parent.getRegistry()), newRealm(parent.getLoader().getRealm(), loader));

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

    /**
     * Shuts down this module manager with the option to only shut down itself or with the parent.
     *
     * @param withParent    True if the parent should also be shut down, false if not
     */
    public void shutdown(boolean withParent) {
        final HierarchicModuleRegistry registry = (HierarchicModuleRegistry) getRegistry();
        shutdown(withParent ? registry.getModules().iterator() : registry.getChildModules());
    }

}
