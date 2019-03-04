package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;

import java.util.Optional;

/**
 * Utility class for doing Reflection stuff like instantiating the {@link com.github.macgregor.alexandria.remotes.Remote}
 * implementation at runtime based on user configured fully qualified class name.
 */
public class Reflection {

    /**
     * Check whether or not a target object implmenets a class or interface.
     *
     * @param target  target object in question
     * @param interfaceClazz  interface or class to check for
     * @return  true if the object is assignable from the {@code interfaceClazz}
     */
    public static boolean implementsInterface(Object target, Class<?> interfaceClazz){
        return interfaceClazz.isAssignableFrom(target.getClass());
    }

    /**
     * Checks if a target class implements a class or interface, returning the object cast to that type as a convenience.
     *
     * @param target  target object in question
     * @param interfaceClazz  interface or class to check for
     * @param <T>  type of the interface or class used to return a type safe cast of the target object
     * @return  target cast to T if it implements {@code interfaceClazz} or Optional.empty() if it doesnt
     */
    public static <T> Optional<T> maybeImplementsInterface(Object target, Class<T> interfaceClazz){
        if(implementsInterface(target, interfaceClazz)){
            return Optional.of((T)target);
        }
        return Optional.empty();
    }

    /**
     * Instantiate a class form a fully qualified class name, wrapping any exceptions into an {@link AlexandriaException}.
     *
     * @param fullyQualifiedClassName  fully qualified package and class name of the desired class
     * @param <T>  class type returned
     * @return  instantiated object
     * @throws AlexandriaException  there was a problem preventing the class from being created such as the class not being found
     */
    public static <T> T create(String fullyQualifiedClassName) throws AlexandriaException {
        try {
            Class typeInstance = Class.forName(fullyQualifiedClassName);
            return (T)typeInstance.newInstance();
        } catch (ClassNotFoundException |IllegalAccessException | InstantiationException e) {
            throw new AlexandriaException.Builder()
                    .withMessage("Unable to instantiate class " + fullyQualifiedClassName)
                    .causedBy(e)
                    .build();
        }
    }
}
