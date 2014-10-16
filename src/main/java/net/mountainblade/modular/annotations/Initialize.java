package net.mountainblade.modular.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation that marks method(s) that should be executed when a module gets loaded.
 *
 * <p>The method signature can also be varied, so you can easily get the manager's instance when loading a module:
 * <pre>
 *     &#64;Initialize
 *     public void init(ModuleManager manager) {
 *         System.out.println("This is running v" + manager.getInformation(getClass()).get().getVersion());
 *     }
 * </pre></p>
 *
 * @author spaceemotion
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Initialize {
}
