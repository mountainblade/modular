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
package net.mountainblade.modular.examples;

import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import net.mountainblade.modular.annotations.Inject;

import java.util.logging.Logger;

@Implementation(version = "2.0-develop")
public class Example3Module implements Module {
    @Inject
    private Logger logger;

    @Inject
    private ModuleInformation info;


    @Initialize
    private void init() {
        logger.info("Running example 3 in version " + info.getVersion());
    }

}
