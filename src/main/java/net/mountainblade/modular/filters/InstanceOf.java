package net.mountainblade.modular.filters;

import net.mountainblade.modular.Filter;
import net.mountainblade.modular.impl.ModuleLoader;

/**
 * Represents a filter that only lets implementations of the given parent class pass through.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class InstanceOf implements Filter {
    private final Class<?> assignableClass;

    /**
     * Creates a new "instanceof" filter.
     *
     * @param assignableClass    The class to check the implementation against
     */
    public InstanceOf(Class assignableClass) {
        this.assignableClass = assignableClass;
    }

    @Override
    public boolean retain(ModuleLoader.ClassEntry candidate) {
        return assignableClass.isAssignableFrom(candidate.getImplementation());
    }

}
