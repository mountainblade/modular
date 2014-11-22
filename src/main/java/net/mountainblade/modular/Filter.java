package net.mountainblade.modular;

import net.mountainblade.modular.impl.ModuleLoader;

/**
 * Represents a module filter.
 * Filters are used during the loading process and can decide whether or not a module should be loaded.
 *
 * @author spaceemotion
 * @version 1.0
 */
public interface Filter {

    /**
     * Determines whether or not the given candidate should be retained in the loading process.
     * If the function returns {@code true}, it will be retained, if {@code false} it will be removed (filtered out).
     *
     * @param candidate    The candidate we check against
     * @return True if the candidate should be retained or false if not
     */
    boolean retain(ModuleLoader.ClassEntry candidate);

}
