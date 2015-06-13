package net.mountainblade.modular.filters;

import net.mountainblade.modular.Filter;
import net.mountainblade.modular.impl.ModuleLoader;

/**
 * Represents a filter that negates the output of another.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class Not implements Filter {
    private final Filter other;

    /**
     * Creates a new "not" filter which inverts the output of the given filter.
     *
     * @param other    The filter to negate the output from
     */
    public Not(Filter other) {
        this.other = other;
    }

    @Override
    public boolean retain(ModuleLoader.ClassEntry candidate) {
        return !other.retain(candidate);
    }

}
