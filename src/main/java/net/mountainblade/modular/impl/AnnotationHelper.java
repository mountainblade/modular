package net.mountainblade.modular.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;


class AnnotationHelper {

    public static <T extends Annotation> void callMethodWithAnnotation(Object object, Class<T> annotation,
                                                                       Object... args)
            throws InvocationTargetException, IllegalAccessException {

        for (Method method : object.getClass().getDeclaredMethods()) {
            T initialize = method.getAnnotation(annotation);

            if (initialize != null) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (!Arrays.equals(parameterTypes, getTypes(args))) {
                    continue;
                }

                method.setAccessible(true);
                method.invoke(object, args);
                break;
            }
        }
    }

    private static Class<?>[] getTypes(Object... args) {
        Class[] classes = new Class[args.length];

        for (int i = 0; i < args.length; i++) {
            classes[i] = args.getClass();
        }

        return classes;
    }

}
