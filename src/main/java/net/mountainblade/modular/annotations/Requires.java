package net.mountainblade.modular.annotations;

import net.mountainblade.modular.Module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation that tells modular that we require the given module
 * even though it might not have been loading automatically (through the load functions).
 *
 * @author spaceemotion
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Requires {

    /** The class of the module we require */
    Class<? extends Module> value();

}
