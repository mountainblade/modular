package net.mountainblade.modular;

import com.google.common.base.Optional;

import java.net.URI;
import java.util.Collection;

/**
 * Represents a module manager.
 *
 * @author spaceemotion
 * @version 1.0
 */
public interface ModuleManager extends Module {

    /**
     * Load modules from a URI.
     *
     * @param uri    The URI to load the modules from
     * @return The collection of all successfully loaded modules.
     */
    Collection<Module> loadModules(URI uri);

    /**
     * Gets a specific module by its interface class.
     *
     * @param module    The module class
     * @return An optional for the module instance
     */
    <M extends Module> Optional<M> getModule(Class<M> module);

    /**
     * Tells the manager to shut down and destroy all loaded modules.
     * <br>
     * This can be useful for cases in which you want to have a clear, fresh start.
     */
    void destroy();

}
