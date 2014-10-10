package net.mountainblade.modular.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation that, if attached, automatically fills fields with their dependencies when a module gets
 * loaded. This will also result in a topological sorting of modules to (hopefully) prevent any dependency issues.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {

    /** If specified, the dependency can be left at null if it cannot be found. */
    boolean optional() default false;

}
