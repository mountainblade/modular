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
package net.mountainblade.modular.impl;

import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.ModuleState;
import net.mountainblade.modular.Version;
import net.mountainblade.modular.annotations.Implementation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

class ModuleInformationImpl implements ModuleInformation {
    private final Properties properties;
    private final Collection<String> authors;
    private final Version version;

    private ModuleState state;


    ModuleInformationImpl(Implementation annotation) {
        this(annotation.version(), annotation.authors());
    }

    ModuleInformationImpl(String version, String... authors) {
        this.version = getVersion(version);
        this.authors = Arrays.asList(authors);

        this.properties = new Properties();
        this.state = ModuleState.UNKNOWN;
    }

    @Override
    public String[] getAuthors() {
        return authors.toArray(new String[authors.size()]);
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public ModuleState getState() {
        return state;
    }

    public void setState(ModuleState state) {
        this.state = state;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    private static Version getVersion(String version) {
        try {
            return new Version(version);

        } catch (IllegalArgumentException e) {
            if (version != null && !version.equals("unknown")) {
                Logger.getLogger(ModuleInformationImpl.class.getName()).log(Level.INFO,
                        "Could not parse version \"" + version + "\", will assign 1.0.0 instead", e);
            }

            return new Version(1);
        }
    }

}
