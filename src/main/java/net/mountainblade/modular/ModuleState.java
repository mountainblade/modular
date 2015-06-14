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

/**
 * Represents the state a module can be currently in.
 *
 * @author spaceemotion
 * @version 1.0
 */
public enum ModuleState {
    /** The module has been recognized and is currently loading */
    LOADING,

    /** The module is ready, the fields injected and its initialize method called (if there is one) */
    READY,

    /** The module was running at least once and its shutdown method has been called (if there is one) */
    SHUTDOWN,

    /** The state is currently unknown, can happen when a module has just been recognized by the system */
    UNKNOWN
}
