package net.mountainblade.modular.impl;

/**
 * Represents an exception that gets thrown when an injection failed.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class InjectFailedException extends Exception {

    public InjectFailedException(String message) {
        super(message);
    }

}
