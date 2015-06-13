package net.mountainblade.modular.filters;

import net.mountainblade.modular.Filter;
import net.mountainblade.modular.impl.ModuleLoader;

import java.lang.annotation.Annotation;

/**
 * Represents a filter that only lets implementations with a specific annotation pass through.
 *
 * @author spaceemotion
 * @version 1.0
 */
public class AnnotationPresent implements Filter {
    private final Class<? extends Annotation> annotation;

    /**
     * Creates a new annotation filter.
     *
     * @param annotation    The annotation to check against
     */
    public AnnotationPresent(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean retain(ModuleLoader.ClassEntry candidate) {
        return candidate.getImplementation().isAnnotationPresent(annotation);
    }

}
