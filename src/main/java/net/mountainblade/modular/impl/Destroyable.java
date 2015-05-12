package net.mountainblade.modular.impl;

/**
 * Represents a destroyable object.
 *
 * <p>This is an abstract class instead of an interface since we only want
 * to have protected access to the {@link #destroy() destroy} method.</p>
 *
 * @author spaceemotion
 * @version 1.0
 */
public abstract class Destroyable {

    /**
     * Attempts to "destroy" the object by clearing memory (e.g. empty out lists and maps).
     */
    protected abstract void destroy();

}
