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
package net.mountainblade.modular.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation that marks method(s) that should be executed when a module gets loaded.
 * The method signature can also be varied, so you can easily get the manager's instance when loading a module:
 *
 * <pre>
 *     &#64;Initialize
 *     public void init(ModuleManager manager) {
 *         System.out.println("This is running v" + manager.getInformation(getClass()).get().getVersion());
 *     }
 * </pre>
 *
 * @author spaceemotion
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Initialize {
}
