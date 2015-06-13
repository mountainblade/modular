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
package net.mountainblade.modular;

import net.mountainblade.modular.impl.ModuleLoader;

/**
 * Represents a module filter.
 * Filters are used during the loading process and can decide whether or not a module should be loaded.
 *
 * @author spaceemotion
 * @version 1.0
 */
public interface Filter {

    /**
     * Determines whether or not the given candidate should be retained in the loading process.
     * If the function returns {@code true}, it will be retained, if {@code false} it will be removed (filtered out).
     *
     * @param candidate    The candidate we check against
     * @return True if the candidate should be retained or false if not
     */
    boolean retain(ModuleLoader.ClassEntry candidate);

}
