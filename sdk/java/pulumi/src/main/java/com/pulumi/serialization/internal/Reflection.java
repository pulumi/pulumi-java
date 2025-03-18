package com.pulumi.serialization.internal;

import com.google.common.base.MoreObjects;
import com.google.common.reflect.ClassPath;
import com.pulumi.core.internal.Exceptions;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.function.BiConsumer;

public class Reflection {
    public static abstract class TypeReference<T> {
        public final Type type;

        protected TypeReference() {
            Type superClass = this.getClass().getGenericSuperclass();
            if (superClass instanceof Class) {
                throw new IllegalArgumentException("Internal error: TypeReference constructed without actual type information");
            } else {
                this.type = ((ParameterizedType)superClass).getActualTypeArguments()[0];
            }
        }
    }

    public static <T extends Annotation> void enumerateClassesWithAnnotation(Class<T> clz,
                                                                             BiConsumer<Class<?>, T> consumer) {
        var loader = MoreObjects.firstNonNull(Reflection.class.getClassLoader(), Thread.currentThread()
                .getContextClassLoader());
        final ClassPath classpath;
        try {
            classpath = ClassPath.from(loader);
        } catch (IOException e) {
            throw Exceptions.newIllegalState(e, "Failed to read class path: %s", e.getMessage());
        }

        for (var classInfo : classpath.getAllClasses()) {
            // exclude early our dependencies and common packages almost certain to not contain what we want
            if (!ResourcePackages.excludePackages(classInfo)) continue;

            Class<?> c;
            try {
                c = classInfo.load();
            } catch (LinkageError e) {
                throw Exceptions.newIllegalState(e, "Failed to load class '%s' (package: '%s') from class path: %s",
                        classInfo, classInfo.getPackageName(), e.getMessage());
            }

            var anno = c.getAnnotation(clz);
            if (anno != null) consumer.accept(c, anno);
        }
    }

    public static boolean isStaticMethod(Method m) {
        return Modifier.isStatic(m.getModifiers());
    }

    public static Type getTypeArgument(Type type, int i) {
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] args = paramType.getActualTypeArguments();

            if (i >= 0 && i < args.length) {
                return args[i];
            }
        }

        return null;
    }

    public static boolean isSubclassOf(Type typeToCheck, Class<?> targetType) {
        return targetType.isAssignableFrom(getRawType(typeToCheck));
    }

    public static boolean isSubclassOf(Type typeToCheck, Class<?> targetType, Type... typeParameters) {
        if (!isSubclassOf(typeToCheck, targetType)) {
            return false;
        }

        int index = 0;
        for (Type t : typeParameters) {
            var sub = getTypeArgument(typeToCheck, index++);
            if (sub == null || !isSubclassOf(t, getRawType(sub))) {
                return false;
            }
        }

        return true;
    }

    public static Type makeType(Type genericClass, TypeVariable...typeParameters) {
        return getRawType(genericClass);
    }

    public static Type resolveGenericType(Type context, Type type) {
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
            throw Exceptions.newRuntime(null, "Can't determine real type of %s [in context: %s]", type, context);
        } else {
            throw Exceptions.newRuntime(null, "Can't determine real type of %s", type);
        }
    }

    public static <T> Class<T> getRawType(Type context, Type type) {
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
            throw Exceptions.newRuntime(null, "Can't determine real type of %s [in context: %s]", type, context);
        } else {
            throw Exceptions.newRuntime(null, "Can't determine real type of %s", type);
        }
    }

    public static <T> Class<T> getRawType(Type type) {
        return getRawType(null, type);
    }
}
