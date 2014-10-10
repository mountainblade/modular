package net.mountainblade.modular.impl;

import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.ModuleState;

import java.util.Properties;

/**
 * Represents the MavenModuleInformation.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class MavenModuleInformation implements ModuleInformation {

    @Override
    public String[] getAuthors() {
        return new String[0];
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public ModuleState getState() {
        return ModuleState.READY;
    }

    @Override
    public Properties getProperties() {
        return null;
    }

}
