package net.mountainblade.modular.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a helper for Annotations.
 *
 * @author spaceemotion
 * @version 1.0
 */
public final class Annotations {
    /**
     * Represents the void. Nothing to see here. Move along. Now. Please.
     * <br>
     * This is used when we want to return something, but it cannot be a "known" object. And since java does not allow
     * instantiation of the {@link java.lang.Void} type...
     */
    private static final Object NOTHING = new Object();


    private Annotations() {
        // Private constructor
    }

    /**
     * Calls a method with a specific annotation and parameters.
     *
     * @param object     The object
     * @param annotation The annotation class
     * @param required   The number of required parameters (at least)
     * @param argTypes   The parameter types
     * @param args       The parameter objects
     * @return An object that the method might return
     * @throws InvocationTargetException The invocation target exception
     * @throws IllegalAccessException The illegal access exception
     */
    public static Object call(Object object, Class<? extends Annotation> annotation, int required, Class<?>[] argTypes,
                                                     Object... args)
            throws InvocationTargetException, IllegalAccessException {

        List<Method> declaredMethods = Arrays.asList(object.getClass().getDeclaredMethods());
        for (Method method : declaredMethods) {
            Object returnValue = callMethod(object, annotation, required, argTypes, method, args);

            if (!NOTHING.equals(returnValue)) {
                return returnValue;
            }
        }

        for (Method method : object.getClass().getMethods()) {
            if (declaredMethods.contains(method)) {
                continue;
            }

            Object returnValue = callMethod(object, annotation, required, argTypes, method, args);
            if (!NOTHING.equals(returnValue)) {
                return returnValue;
            }
        }

        return null;
    }

    private static Object callMethod(Object object, Class<? extends Annotation> annotation, int required,
                                                          Class<?>[] argTypes, Method method, Object[] args)
            throws IllegalAccessException, InvocationTargetException {
        // Check if we got the correct annotation
        if (method.getAnnotation(annotation) == null) {
            return NOTHING;
        }

        // Get and check general parameter types
        Class<?>[] methodParameterTypes = method.getParameterTypes();
        if (methodParameterTypes.length < required) {
            return NOTHING;
        }

        // Slowly decrease number of optional parameters, so we always get the "most available" one
        int counter = argTypes.length;

        do {
            if (isApplicable(Arrays.copyOf(methodParameterTypes, counter), argTypes)) {
                // Make accessible and invoke the method
                method.setAccessible(true);

                if (counter > 0) {
                    return method.invoke(object, Arrays.copyOf(args, counter));
                }

                return method.invoke(object);
            }

        } while ((counter--) >= required);

        return null;
    }

    private static boolean isApplicable(Class<?>[] method, Class<?>[] parameters) {
        for (int i = method.length - 1; i >= 0; i--) {
            Class<?> aClass = method[i];

            if (aClass == null || !aClass.isAssignableFrom(parameters[i])) {
                return false;
            }
        }

        return true;
    }

}
