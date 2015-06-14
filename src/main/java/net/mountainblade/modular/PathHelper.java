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

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * Represents a helper class to assist with loading modules from various locations.
 *
 * @author spaceemotion
 * @version 1.0
 */
public final class PathHelper {

    private PathHelper() {
        // Private constructor as this is a helper class
    }

    /**
     * Returns the package name ready to be used when trying to load modules
     * from the same package (and below) as the given class.
     *
     * The output string will have the package name suffixed by a dot (".").
     *
     * @param aClass    The class to get the package from
     * @return A package name
     */
    public static String near(Class aClass) {
        return aClass.getPackage().getName() + ".";
    }

    /**
     * Returns an URI representing the parent directory of the given URL.
     *
     * @param url    The url
     * @return The parent directory's URI
     */
    public static URI inside(URL url) {
        return inside(url.getFile());
    }

    /**
     * Returns an URI representing the parent directory of the given path.
     *
     * @param file    The path to current directory
     * @return The parent directory's URI
     */
    public static URI inside(String file) {
        return new File(file).getParentFile().toURI();
    }

}
