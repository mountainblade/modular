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
package net.mountainblade.modular;

import java.util.Properties;

/**
 * Represents a holder for meta data and other information about a module.
 *
 * @author spaceemotion
 * @version 1.0
 */
public interface ModuleInformation {

    /**
     * Gets the author(s) of the module.
     *
     * @return The author(s)
     */
    String[] getAuthors();

    /**
     * Gets the module version.
     *
     * @return The version string
     */
    Version getVersion();

    /**
     * Gets the current state of the module.
     *
     * @return The state of the module
     */
    ModuleState getState();

    /**
     * Gets custom module properties.
     *
     * @return The properties
     */
    Properties getProperties();

}
