package net.mountainblade.modular.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an annotation that is used to mark implementation of modules.
 * <br>
 * The instantiation happens at runtime, using {@link net.mountainblade.modular.ModuleManager#loadModules(java.net.URI)}
 * to load the modules. If a module has no implementation marked with this annotation, nothing will happen.
 * <br>
 * If the implementation does not implement an interface that either is plugin or an extension of plugin.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Implementation {


}
