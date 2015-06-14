package net.mountainblade.modular.filters;

import net.mountainblade.modular.Filter;
import net.mountainblade.modular.impl.ModuleLoader;

/**
 * Represents a filter which returns true if one of the filters returns true as well.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class Or implements Filter {
    private final Filter[] filters;

    /**
     * Creates a new "or" filter which returns true if one of the filters returns true as well.
     *
     * @param filters    The filters to check the output from
     */
    public Or(Filter... filters) {
        this.filters = filters;
    }

    @Override
    public boolean retain(ModuleLoader.ClassEntry candidate) {
        for (Filter filter : filters) {
            if (filter.retain(candidate)) {
                return true;
            }
        }

        return false;
    }

}
