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
package net.mountainblade.modular.examples;

import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleManager;
import net.mountainblade.modular.annotations.*;

@Implementation
public class ExampleModule implements Module {
    public static final String PREFIX = '[' + ExampleModule.class.getSimpleName() + "] ";

    @Inject
    private Example2Module example2Module;


    @Initialize
    public void init(ModuleManager moduleManager) {
        System.out.println(PREFIX + "Hello World, using a lightweight module system!");
        System.out.println(PREFIX + "Dynamically injected method parameter: " + moduleManager);
        System.out.println(PREFIX + "Dynamically injected field: " + example2Module);
    }

    @Shutdown
    public void destroy() {
        System.out.println(PREFIX + "Bye bye");
    }

}
