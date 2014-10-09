package net.mountainblade.modular.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents the Inject.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Target(value = { ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {

    boolean optional() default false;

}
