package net.mountainblade.modular;

import java.util.Properties;

/**
 * Represents a holder for meta data and other information about a module.
 *
 * @author spaceemotion
 * @version 1.0
 */
public interface ModuleInformation {

    /**
     * Gets the author(s) of the module.
     *
     * @return The author(s)
     */
    String[] getAuthors();

    /**
     * Gets the module version.
     *
     * @return The version string
     */
    String getVersion();

    /**
     * Gets the current state of the module.
     *
     * @return The state of the module
     */
    ModuleState getState();

    /**
     * Gets custom module properties.
     *
     * @return The properties
     */
    Properties getProperties();

}
