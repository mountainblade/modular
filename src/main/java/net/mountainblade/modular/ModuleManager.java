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

import com.google.common.base.Optional;
import net.mountainblade.modular.impl.Injector;
import net.mountainblade.modular.impl.ModuleLoader;
import net.mountainblade.modular.impl.ModuleRegistry;

import java.io.File;
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
     * This will skip all injection and method calls.
     *
     * @param module the module
     * @see #provide(Module)
     */
    <T extends Module> T provideSimple(T module);

    /**
     * Stores the given module instance in the registry,
     * but also executes all injection magic and calls the usual methods.
     *
     * @param module the module
     * @see #provideSimple(Module)
     */
    <T extends Module> T provide(T module);

    /**
     * Load modules from a URI.
     *
     * This will load all JAR files (and their containing modules) within the URI
     * as well as any ".class" files assuming the given URI is their root package.
     *
     * @param uri        The URI to load the modules from
     * @param filters    An array of {@link net.mountainblade.modular.Filter filters} to use
     * @return A collection of all successfully loaded modules.
     */
    Collection<Module> loadModules(URI uri, Filter... filters);

    /**
     * Loads modules from a URI with support for a resource package filter.
     *
     * @param uri            The URI to load the modules from
     * @param packageName    The root package name to look for
     * @param filters        An array of {@link net.mountainblade.modular.Filter filters} to use
     * @return A collection of all successfully loaded modules.
     */
    Collection<Module> loadModules(URI uri, String packageName, Filter... filters);

    /**
     * Loads modules from the given file.
     *
     * If the file is a JAR file directly it will only load that JAR,
     * otherwise this will work the same as {@link #loadModules(URI, Filter...)}.
     *
     * @param file       The file to load the modules from
     * @param filters    An array of {@link net.mountainblade.modular.Filter filters} to use
     * @return A collection of all successfully loaded modules.
     */
    Collection<Module> loadModules(File file, Filter... filters);

    /**
     * Loads modules inside the current class path with the given string representing either
     * a class file directly or a package and thus functioning as a filter.
     *
     * @param resource    The resource string, can be the fully qualified class name or a package name
     * @param filters     An array of {@link net.mountainblade.modular.Filter filters} to use
     * @return A collection of all successfully loaded modules.
     */
    Collection<Module> loadModules(String resource, Filter... filters);

    /**
     * Loads only the given module class and its dependencies.
     *
     * @param moduleClass    The class of the module to load
     * @param filters        An array of {@link net.mountainblade.modular.Filter filters} to use
     * @param <M>            The module type
     * @return Only the given module object to allow for builder-like method chaining
     */
    <M extends Module> M loadModule(Class<M> moduleClass, Filter... filters);

    /**
     * Loads modules from a collection of URIs with support for a resource package filter.
     *
     * @param uris           A collection of URIs to load the modules from
     * @param packageName    The root package name to look for
     * @param filters        An array of {@link net.mountainblade.modular.Filter filters} to use
     * @return A collection of all successfully loaded modules.
     */
    Collection<Module> loadModules(Collection<URI> uris, String packageName, Filter... filters);

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
     * Gets the registry containing all registered (loaded) Modules.
     *
     * @return The underlying module registry
     */
    ModuleRegistry getRegistry();

    /**
     * Gets the underlying field injector.
     * Use this if you want to add support for custom field types.
     *
     * @return The injector instance
     */
    Injector getInjector();

    /**
     * Gets the used module loader instance.
     * Use this to set the loading strategy for example.
     *
     * @return The underlying module loader
     */
    ModuleLoader getLoader();

    /**
     * Tells the manager to shut down and destroy all loaded modules.
     * This can be useful for cases in which you want to have a clear, fresh start.
     */
    void shutdown();

}
