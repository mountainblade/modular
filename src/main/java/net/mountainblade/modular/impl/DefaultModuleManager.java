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

import gnu.trove.set.hash.THashSet;
import net.mountainblade.modular.Module;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the default implementation of a {@link net.mountainblade.modular.ModuleManager}.
 *
 * Use this if you want to have the simples of all implementations.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class DefaultModuleManager extends BaseModuleManager {

    /**
     * Creates a new module manager instance.
     *
     * This will initialize a blank new manager, registry and loader, contained inside a new class realm.
     * Modules loaded inside this manager will be kept hidden to other managers.
     *
     * @see HierarchicModuleManager
     */
    public DefaultModuleManager() {
        this(null);
    }

    /**
     * Creates a new module manager instance with a custom parent class loader.
     *
     * @param loader    The custom class loader
     * @see #DefaultModuleManager()
     */
    public DefaultModuleManager(ClassLoader loader) {
        super(new ModuleRegistry(
                new ConcurrentHashMap<Class<? extends Module>, ModuleRegistry.Entry>(), new THashSet<Module>()
        ), null, loader);
    }

}
