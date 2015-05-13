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
package net.mountainblade.modular.demo;

import net.mountainblade.modular.Module;
import net.mountainblade.modular.annotations.*;

import java.util.logging.Logger;

/**
 * Represents a module that is loaded using its jar file.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Implementation(authors = "spaceemotion", version = "1.0.2")
public class JarModule implements Module {
    @Inject
    private Logger logger;


    @Initialize
    public void initialize() {
        logger.info("Hello world!");
    }

    @Shutdown
    public void shutdown() {
        logger.info("Bye world!");
    }

}
