package com.pulumi.serialization.internal;

import com.google.common.base.MoreObjects;
import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.function.BiConsumer;

public class Reflection {
    public static <T extends Annotation> void enumerateClassesWithAnnotation(Class<T> clz, BiConsumer<Class<?>, T> consumer) {
        var loader = MoreObjects.firstNonNull(
                Reflection.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()
        );
        final ClassPath classpath;
        try {
            classpath = ClassPath.from(loader);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Failed to read class path: %s", e.getMessage()), e);
        }

        for (var classInfo : classpath.getAllClasses()) {
            // exclude early our dependencies and common packages almost certain to not contain what we want
            if (!ResourcePackages.excludePackages(classInfo)) continue;

            Class<?> c;
            try {
                c = classInfo.load();
            } catch (LinkageError e) {
                throw new IllegalStateException(String.format(
                        "Failed to load class '%s' (package: '%s') from class path: %s",
                        classInfo, classInfo.getPackageName(), e.getMessage()
                ), e);
            }

            var anno = c.getAnnotation(clz);
            if (anno != null) consumer.accept(c, anno);
        }
    }

    public static boolean isStaticMethod(Method m) {
        return Modifier.isStatic(m.getModifiers());
    }

    public static Type getTypeArgument(Type type,
                                       int i) {
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] args = paramType.getActualTypeArguments();

            if (i >= 0 && i < args.length) {
                return args[i];
            }
        }

        return null;
    }

    public static boolean isSubclassOf(Class<?> targetType,
                                       Type typeToCheck) {
        return targetType.isAssignableFrom(getRawType(typeToCheck));
    }

    public static Type resolveGenericType(Type context,
                                          Type type) {
        if (type == null) {
            return null;
        }

        if (type instanceof Class<?>) {
            return type;
        }

        if (type instanceof ParameterizedType) {
            return type;
        }

        if (type instanceof TypeVariable<?>) {
            TypeVariable<?> type2 = (TypeVariable<?>) type;

            Object genDecl = type2.getGenericDeclaration();
            if (genDecl instanceof Class<?>) {
                Class<?> genDecl2 = (Class<?>) genDecl;

                TypeVariable<?>[] typeParams = genDecl2.getTypeParameters();
                for (int i = 0; i < typeParams.length; i++) {
                    if (typeParams[i].equals(type2)) {
                        if (context instanceof ParameterizedType) {
                            ParameterizedType parameterizedType = (ParameterizedType) context;

                            Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
                            if (i < actualTypeArgs.length) {
                                return actualTypeArgs[i];
                            }
                        }
                    }
                }
            }

            if (genDecl instanceof Method) {
                Method genDecl2 = (Method) genDecl;

                TypeVariable<?>[] typeParams = genDecl2.getTypeParameters();
                for (int i = 0; i < typeParams.length; i++) {
                    if (typeParams[i].equals(type2)) {
                        if (context instanceof ParameterizedType) {
                            ParameterizedType parameterizedType = (ParameterizedType) context;

                            Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
                            if (i < actualTypeArgs.length) {
                                return actualTypeArgs[i];
                            }
                        }
                    }
                }
            }

            Type[] bounds = type2.getBounds();

            if (bounds.length == 1) {
                return bounds[0];
            }
        }

        if (context != null) {
            throw new RuntimeException(String.format("Can't determine real type of %s [in context: %s]", type, context));
        } else {
            throw new RuntimeException(String.format("Can't determine real type of %s", type));
        }
    }

    public static <T> Class<T> getRawType(Type context,
                                          Type type) {
        if (type == null) {
            return null;
        }

        type = resolveGenericType(context, type);

        if (type instanceof Class<?>) {
            @SuppressWarnings("unchecked") Class<T> result = (Class<T>) type;
            return result;
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            @SuppressWarnings("unchecked") Class<T> result = (Class<T>) parameterizedType.getRawType();
            return result;
        }

        if (context != null) {
            throw new RuntimeException(String.format("Can't determine real type of %s [in context: %s]", type, context));
        } else {
            throw new RuntimeException(String.format("Can't determine real type of %s", type));
        }
    }

    public static <T> Class<T> getRawType(Type type) {
        return getRawType(null, type);
    }
}
