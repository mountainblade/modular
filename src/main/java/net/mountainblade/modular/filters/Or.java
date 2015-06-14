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
 * Represents a filter which returns true if one of the filters returns true as well.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class Or implements Filter {
    private final Filter[] filters;

    /**
     * Creates a new "or" filter which returns true if one of the filters returns true as well.
     *
     * @param filters    The filters to check the output from
     */
    public Or(Filter... filters) {
        this.filters = filters;
    }

    @Override
    public boolean retain(ModuleLoader.ClassEntry candidate) {
        for (Filter filter : filters) {
            if (filter.retain(candidate)) {
                return true;
            }
        }

        return false;
    }

}
