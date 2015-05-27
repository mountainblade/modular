/**
 * Copyright (C) 2014 MountainBlade (http://mountainblade.net)
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
    private final DefaultModuleManager parent;


    public HierarchicModuleManager(DefaultModuleManager parent) {
        this(parent, null);
    }

    public HierarchicModuleManager(DefaultModuleManager parent, ClassLoader loader) {
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

    public void shutdown(boolean withParent) {
        final HierarchicModuleRegistry registry = (HierarchicModuleRegistry) getRegistry();
        shutdown(withParent ? registry.getModuleCollection().iterator() : registry.getChildModules());
    }

}
