package net.mountainblade.modular.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;


class Annotations {

    public static <T extends Annotation> Object callMethodWithAnnotation(Object object, Class<T> annotation,
                                                                         int required, Class<?>[] argTypes,
                                                                         Object... args)
            throws InvocationTargetException, IllegalAccessException {

        for (Method method : object.getClass().getDeclaredMethods()) {
            // Check if we got the correct annotation
            T methodAnnotation = method.getAnnotation(annotation);
            if (methodAnnotation == null) {
                continue;
            }

            // Get and check general parameter types
            Class<?>[] methodParameterTypes = method.getParameterTypes();
            if (methodParameterTypes.length < required) {
                continue;
            }

            // Slowly decrease number of optional parameters, so we always get the "most available" one
            int counter = argTypes.length;

            do {
                if (isApplicable(Arrays.copyOf(methodParameterTypes, counter), argTypes)) {
                    // Make accessible and invoke the method
                    method.setAccessible(true);
                    return method.invoke(object, Arrays.copyOf(args, counter));
                }

            } while ((counter--) >= required);
        }

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
