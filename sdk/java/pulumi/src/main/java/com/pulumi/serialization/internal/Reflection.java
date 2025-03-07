package com.pulumi.serialization.internal;

import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.ClassPath;
import com.pulumi.core.annotations.PolicyPackMethod;
import com.pulumi.core.annotations.PolicyPackType;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.Resource;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public class Reflection
{
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
                                       Type typeToCheck)
    {
        return targetType.isAssignableFrom(getRawType(typeToCheck));
    }

    public static Type resolveGenericType(Type context,
                                          Type type)
    {
        if (type == null)
        {
            return null;
        }

        if (type instanceof Class<?>)
        {
            return type;
        }

        if (type instanceof ParameterizedType)
        {
            return type;
        }

        if (type instanceof TypeVariable<?>)
        {
            TypeVariable<?> type2 = (TypeVariable<?>) type;

            Object genDecl = type2.getGenericDeclaration();
            if (genDecl instanceof Class<?>)
            {
                Class<?> genDecl2 = (Class<?>) genDecl;

                TypeVariable<?>[] typeParams = genDecl2.getTypeParameters();
                for (int i = 0; i < typeParams.length; i++)
                {
                    if (typeParams[i].equals(type2))
                    {
                        if (context instanceof ParameterizedType)
                        {
                            ParameterizedType parameterizedType = (ParameterizedType) context;

                            Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
                            if (i < actualTypeArgs.length)
                            {
                                return actualTypeArgs[i];
                            }
                        }
                    }
                }
            }

            if (genDecl instanceof Method)
            {
                Method genDecl2 = (Method) genDecl;

                TypeVariable<?>[] typeParams = genDecl2.getTypeParameters();
                for (int i = 0; i < typeParams.length; i++)
                {
                    if (typeParams[i].equals(type2))
                    {
                        if (context instanceof ParameterizedType)
                        {
                            ParameterizedType parameterizedType = (ParameterizedType) context;

                            Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
                            if (i < actualTypeArgs.length)
                            {
                                return actualTypeArgs[i];
                            }
                        }
                    }
                }
            }

            Type[] bounds = type2.getBounds();

            if (bounds.length == 1)
            {
                return bounds[0];
            }
        }

        if (context != null)
        {
            throw new RuntimeException(String.format("Can't determine real type of %s [in context: %s]", type, context));
        }
        else
        {
            throw new RuntimeException(String.format("Can't determine real type of %s", type));
        }
    }

    public static <T> Class<T> getRawType(Type context,
                                          Type type)
    {
        if (type == null)
        {
            return null;
        }

        type = resolveGenericType(context, type);

        if (type instanceof Class<?>)
        {
            @SuppressWarnings("unchecked") Class<T> result = (Class<T>) type;
            return result;
        }

        if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            @SuppressWarnings("unchecked") Class<T> result = (Class<T>) parameterizedType.getRawType();
            return result;
        }

        if (context != null)
        {
            throw new RuntimeException(String.format("Can't determine real type of %s [in context: %s]", type, context));
        }
        else
        {
            throw new RuntimeException(String.format("Can't determine real type of %s", type));
        }
    }

    public static <T> Class<T> getRawType(Type type)
    {
        return getRawType(null, type);
    }
}
