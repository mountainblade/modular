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
     * Simply provides a module instance and stores it in the registry.
     *
     * @param module the module
     * @see #provide(Module)
     */
    void provideSimple(Module module);

    /**
     * Stored the given module instance in the registry, but also executes all injection magic and calls the usual
     * methods.
     *
     * @param module the module
     * @see #provideSimple(Module)
     */
    void provide(Module module);

    /**
     * Load modules from a URI.
     *
     * @param uri    The URI to load the modules from
     * @return The collection of all successfully loaded modules.
     */
    Collection<Module> loadModules(URI uri);

    /**
     * Gets a specific module by its class.
     *
     * @param module    The module class
     * @return An optional for the module instance
     */
    <M extends Module> Optional<M> getModule(Class<M> module);

    /**
     * Gets information about a specific module.
     *
     * @param module    The module class
     * @return An optional for the information
     */
    Optional<ModuleInformation> getInformation(Class<? extends Module> module);

    /**
     * Gets a collection of all registered Modules.
     *
     * @return All registered modules in an unmodifiable collection
     */
    Collection<Module> getModules();

    /**
     * Tells the manager to shut down and destroy all loaded modules.
     * <br>
     * This can be useful for cases in which you want to have a clear, fresh start.
     */
    void shutdown();

}
