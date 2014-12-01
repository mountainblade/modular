package net.mountainblade.modular;

import net.mountainblade.modular.impl.ModuleLoader;

import java.lang.annotation.Annotation;

/**
 * Represents a module filter.
 * Filters are used during the loading process and can decide whether or not a module should be loaded.
 *
 * @author spaceemotion
 * @version 1.0
 */
public interface Filter {

    /**
     * Determines whether or not the given candidate should be retained in the loading process.
     * If the function returns {@code true}, it will be retained, if {@code false} it will be removed (filtered out).
     *
     * @param candidate    The candidate we check against
     * @return True if the candidate should be retained or false if not
     */
    boolean retain(ModuleLoader.ClassEntry candidate);


    class AnnotationPresent implements Filter {
        private Class<? extends Annotation> annotation;


        public AnnotationPresent(Class<? extends Annotation> annotation) {
            this.annotation = annotation;
        }

        @Override
        public boolean retain(ModuleLoader.ClassEntry candidate) {
            return candidate.getImplementation().isAnnotationPresent(annotation);
        }

    }

    class InstanceOf implements Filter {
        private final Class<?> assignableClass;


        public InstanceOf(Class assignableClass) {
            this.assignableClass = assignableClass;
        }

        @Override
        public boolean retain(ModuleLoader.ClassEntry candidate) {
            return assignableClass.isAssignableFrom(candidate.getImplementation());
        }

    }

}
