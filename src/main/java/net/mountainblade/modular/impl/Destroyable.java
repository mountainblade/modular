package net.mountainblade.modular.impl;

/**
 * Represents a destroyable object.
 *
 * @author spaceemotion
 * @version 1.0
 */
public abstract class Destroyable {

    /**
     * "Destroys" the object by clearing memory (e.g. empty out lists and maps).
     */
    protected abstract void destroy();

}
