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
package net.mountainblade.modular.impl;

/**
 * Represents a destroyable object.
 *
 * <p>This is an abstract class instead of an interface since we only want
 * to have protected access to the {@link #destroy() destroy} method.</p>
 *
 * @author spaceemotion
 * @version 1.0
 */
public abstract class Destroyable {

    /**
     * Attempts to "destroy" the object by clearing memory (e.g. empty out lists and maps).
     */
    protected abstract void destroy();

}
