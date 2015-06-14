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
package net.mountainblade.modular.filters;

import net.mountainblade.modular.Filter;
import net.mountainblade.modular.impl.ModuleLoader;

/**
 * Represents a filter that only lets implementations of the given parent class pass through.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class InstanceOf implements Filter {
    private final Class<?> assignableClass;

    /**
     * Creates a new "instanceof" filter.
     *
     * @param assignableClass    The class to check the implementation against
     */
    public InstanceOf(Class assignableClass) {
        this.assignableClass = assignableClass;
    }

    @Override
    public boolean retain(ModuleLoader.ClassEntry candidate) {
        return assignableClass.isAssignableFrom(candidate.getImplementation());
    }

}
