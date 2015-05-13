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
package net.mountainblade.modular.annotations;

import net.mountainblade.modular.Module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Represents an annotation that, if attached, automatically fills fields with their dependencies when a module gets
 * loaded. This will also result in a topological sorting of modules to (hopefully) prevent any dependency issues.</p>
 *
 * <p>There are some special cases of "dependencies" that will get resolved automatically and handled differently. Some
 * of these special cases also take the {@link #from()} parameter into account, if you want to get objects from other
 * modules (cross-dependency injection).
 * <ul>
 *     <li>{@link java.util.logging.Logger Logger} - If the field is a logger this will be filled with a module-specific
 *     logger instance. <b>No cross-dependency injection</b></li>
 *     <li>{@link net.mountainblade.modular.ModuleInformation ModuleInformation} - Holds the current module's
 *     information or the information of the specified one.</li>
 * </ul></p>
 *
 * @author spaceemotion
 * @version 1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {

    /** If specified, the object won't be injected and can be left at null, if it cannot be found. */
    boolean optional() default false;

    /** If specified, this will get the object from another module instead (cross-dependency injection). */
    Class<? extends Module> from() default Current.class;

    /**
     * Default implementation representing that an injection should use the current model instance.
     *
     * @see #from()
     */
    final class Current implements Module {}

}
