package net.mountainblade.modular.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents the Repeat test annotation.
 *
 * @author fappel
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.METHOD)
public @interface Repeat {
    int value();

}
