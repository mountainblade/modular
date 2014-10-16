package net.mountainblade.modular.annotations;

import net.mountainblade.modular.Module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation that is used to mark implementation of modules.
 *
 * <p>The instantiation happens at runtime, using {@link net.mountainblade.modular.ModuleManager#loadModules(java.net.URI)
 * loadModules(URI uri)}*
 * to load the modules. If a module has no implementation marked with this annotation or an interface has been marked
 * instead, nothing will happen.</p>
 *
 * <p>If the annotated class extends some abstract base that implements the module, the system will discover this on its
 * own. If you want to be specific, specify the module using the {@link #module()} parameter.</p>
 *
 * <p>It is not possible that an implementation implements two different modules - even if they are interfaces. It is
 * also not possible to have multiple implementations of a module.</p>
 *
 * @author spaceemotion
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Implementation {

    /**
     * The class for the module we want to implement.
     * If left at default value, the system will automatically resolve this.
     */
    Class<? extends Module> module() default Default.class;

    /** Holds a list of all authors that worked on the module, can be left empty */
    String[] authors() default {};

    /** The module version or build ID */
    String version() default "unknown";

    /**
     * Default implementation representing that an implementation should use its default implementation (current class).
     *
     * @see #module()
     */
    final class Default implements Module {}

}
