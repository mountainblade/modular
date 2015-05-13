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
