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
package net.mountainblade.modular;

import java.io.File;
import java.net.URI;

/**
 * Represents the UriHelper.
 *
 * @author spaceemotion
 * @version 1.0
 */
public final class UriHelper {

    private UriHelper() {
        // Private constructor, this is a helper class
    }

    public static String nearAndBelow(Class aClass) {
        return aClass.getPackage().getName() + ".";
    }

    public static String nearAndBelow(Package pkg) {
        return pkg.getName() + ".";
    }

    public static URI folderOf(String file) {
        return new File(file).getParentFile().toURI();
    }

}
