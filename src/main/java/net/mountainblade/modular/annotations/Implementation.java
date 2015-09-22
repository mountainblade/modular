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

import net.mountainblade.modular.Module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation that is used to mark implementation of modules.
 *
 * <p>The instantiation happens at runtime, using
 * {@link net.mountainblade.modular.ModuleManager#loadModules(java.net.URI, net.mountainblade.modular.Filter...)
 * loadModules(URI uri)} to load the modules. If a module has no implementation marked with this annotation
 * or an interface has been marked instead, nothing will happen.</p>
 *
 * <p>If the annotated class extends some abstract base that implements the module, the system will discover this on its
 * own. If you want to be specific, specify the module using the {@link #module()} parameter.</p>
 *
 * <p>It is not possible that an implementation implements two different modules - even if they are interfaces. It is
 * also not possible to have multiple implementations of a module.</p>
 *
 * @author spaceemotion
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Implementation {

    /**
     * Represents the class for the module we want to implement.
     * If left at default value, the system will automatically resolve this.
     *
     * @return The implemented module class
     * @see Module
     */
    Class<? extends Module> module() default Default.class;

    /**
     * Holds a list of all authors that worked on the module, can be left empty
     *
     * @return An immutable array of authors
     */
    String[] authors() default {};

    /**
     * The module version or build ID.
     * As this contains a string, the defined version might be invalid!
     *
     * @return The module version represented as a string
     * @see net.mountainblade.modular.Version
     */
    String version() default "unknown";

    /**
     * Default implementation representing that an implementation should use its default implementation (current class).
     *
     * @see #module()
     */
    final class Default implements Module {}

}
