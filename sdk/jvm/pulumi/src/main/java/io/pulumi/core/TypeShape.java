package io.pulumi.core;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.internal.Reflection;
import io.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.pulumi.core.internal.PulumiCollectors.toSingleton;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public final class TypeShape<T> {

    public static final TypeShape<Void> Empty = TypeShape.of(Void.class);

    private final Class<T> type;
    private final ImmutableList<TypeShape<?>> parameters;
    private final boolean nullable;

    private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private TypeShape(Class<T> type) {
        this(type, ImmutableList.of());
    }

    private TypeShape(Class<T> type, ImmutableList<TypeShape<?>> parameters) {
        this.type = requireNonNull(type);
        this.parameters = requireNonNull(parameters);
        this.nullable = true;
    }

    private TypeShape(Class<T> type, ImmutableList<TypeShape<?>> parameters, boolean nullable) {
        this.type = requireNonNull(type);
        this.parameters = requireNonNull(parameters);
        this.nullable = nullable;
    }

    public Class<T> getType() {
        return this.type;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getTypeName() {
        return this.type.getTypeName();
    }

    public Optional<TypeShape<?>> getParameter(int index) {
        if (index < this.parameters.size()) {
            return Optional.of(this.parameters.get(index));
        }
        return Optional.empty();
    }

    public ImmutableList<TypeShape<?>> getParameters() {
        return parameters;
    }

    public int getParameterCount() {
        return this.parameters.size();
    }

    public boolean hasParameters() {
        return this.parameters.size() > 0 || this.getType().getTypeParameters().length > 0;
    }

    @InternalUse
    public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationType) {
        return Optional.ofNullable(this.getType().getAnnotation(annotationType));
    }

    @InternalUse
    public <A extends Annotation> AnnotatedConstructor<T, A> getAnnotatedConstructor(Class<A> constructorAnnotation) {
        return Arrays.stream(this.type.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(constructorAnnotation))
                .peek(c -> c.setAccessible(true))
                .map(c -> {
                    try {
                        return new AnnotatedConstructor<>(
                                this,
                                lookup.in(this.getType()).unreflectConstructor(c),
                                c.getAnnotation(constructorAnnotation),
                                extract(c.getParameterTypes(), c.getAnnotatedParameterTypes())
                        );
                    } catch (IllegalAccessException e) {
                        throw new IllegalArgumentException(String.format(
                                "Expected target type '%s' constructor annotated with '@%s; to be accessible, got: %s",
                                getTypeName(), constructorAnnotation.getSimpleName(), e.getMessage()
                        ), e);
                    } finally {
                        c.setAccessible(false);
                    }
                })
                .collect(toSingleton(cause -> new IllegalArgumentException(String.format(
                        "Expected target type '%s' to have an accessible constructor annotated with @%s, got: %s",
                        getTypeName(), constructorAnnotation.getSimpleName(), cause
                ))));
    }

    @InternalUse
    public static final class AnnotatedConstructor<T, A extends Annotation> {

        private final TypeShape<T> type;
        private final MethodHandle constructor;
        private final A annotation;
        private final ImmutableList<TypeShape<?>> parameters;

        public AnnotatedConstructor(TypeShape<T> type, MethodHandle constructor, A annotation, ImmutableList<TypeShape<?>> parameters) {
            this.type = requireNonNull(type);
            this.constructor = requireNonNull(constructor);
            this.annotation = requireNonNull(annotation);
            this.parameters = requireNonNull(parameters);

            var length = this.constructor.type().parameterArray().length;
            if (parameters.size() != length) {
                throw new IllegalStateException(String.format(
                        "Expected MethodHandle parameters to match reflection parameters, got %d and %d respectively.",
                        parameters.size(), length
                ));
            }
        }

        public A getAnnotation() {
            return this.annotation;
        }

        public ImmutableList<TypeShape<?>> getParameters() {
            return this.parameters;
        }

        public T invoke(Object... args) {
            if (this.parameters.size() != args.length) {
                throw new IllegalArgumentException(String.format(
                        "Expected constructor '%s' to be given %d arguments, got: %d",
                        this.constructor, this.parameters.size(), args.length
                ));
            }
            if (!Reflection.isAssignablePrimitiveFrom(this.type.type, this.constructor.type().returnType())) {
                throw new IllegalStateException(String.format(
                        "Expected the constructor '%s' return type '%s' match type shape '%s'",
                        this.constructor, this.constructor.type().returnType(), this.type.getTypeName()
                ));
            }
            for (int i = 0; i < this.parameters.size(); i++) {
                TypeShape<?> p = this.parameters.get(i);
                if (!Reflection.isAssignablePrimitiveFrom(p.type, args[i].getClass())) {
                    throw new IllegalArgumentException(String.format(
                            "Expected constructor '%s' to be given argument #%s (from 0) of type '%s', got '%s'",
                            this.constructor, i, p.type.getTypeName(), args[i].getClass().getTypeName()
                    ));
                }
            }
            try {
                //noinspection unchecked
                return (T) this.constructor.invokeWithArguments(args);
            } catch (Throwable e) {
                throw new IllegalStateException(String.format(
                        "Unexpected exception while invoking constructor '%s': %s",
                        this.constructor, e.getMessage()
                ), e);
            }
        }
    }

    @InternalUse
    public <A extends Annotation> AnnotatedMethod<A> getAnnotatedMethod(Class<A> methodAnnotation) {
        return Arrays.stream(this.getType().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(methodAnnotation))
                .peek(m -> m.setAccessible(true))
                .map(m -> {
                    try {
                        return new AnnotatedMethod<>(
                                lookup.in(this.getType()).unreflect(m),
                                m.getAnnotation(methodAnnotation),
                                // the first parameter is the self-reference type
                                ImmutableList.<TypeShape<?>>builder()
                                        .add(this)
                                        .addAll(extract(m.getParameterTypes(), m.getAnnotatedParameterTypes()))
                                        .build(),
                                extract(m.getReturnType(), m.getAnnotatedReturnType())
                        );
                    } catch (IllegalAccessException e) {
                        throw new IllegalArgumentException(String.format(
                                "Expected target type '%s' to have an accessible method annotated with @%s, got error: %s",
                                getTypeName(), methodAnnotation.getSimpleName(), e.getMessage()
                        ), e);
                    } finally {
                        m.setAccessible(false);
                    }
                })
                .collect(toSingleton(cause -> new IllegalArgumentException(String.format(
                        "Expected target type '%s' to have exactly one method annotated with @%s, got: %s",
                        getTypeName(), methodAnnotation.getSimpleName(), cause
                ))));
    }

    @InternalUse
    public static final class AnnotatedMethod<A extends Annotation> {
        private final MethodHandle method;
        private final A annotation;
        private final ImmutableList<TypeShape<?>> parameters;
        private final TypeShape<?> returnType;

        public AnnotatedMethod(
                MethodHandle method, A annotation, ImmutableList<TypeShape<?>> parameters, TypeShape<?> returnType
        ) {
            this.method = requireNonNull(method);
            this.annotation = requireNonNull(annotation);
            this.parameters = requireNonNull(parameters);

            var length = this.method.type().parameterArray().length;
            if (parameters.size() != length) {
                throw new IllegalStateException(String.format(
                        "Expected MethodHandle parameters to match reflection parameters, got %d and %d respectively.",
                        parameters.size(), length
                ));
            }

            this.returnType = requireNonNull(returnType);
        }

        public A getAnnotation() {
            return this.annotation;
        }

        public ImmutableList<TypeShape<?>> getParameters() {
            return this.parameters;
        }

        public TypeShape<?> getReturnType() {
            return this.returnType;
        }

        // First argument is the self-reference
        public Object invoke(Object... args) {
            if (this.parameters.size() != args.length) {
                throw new IllegalArgumentException(String.format(
                        "Expected method '%s' to be given %d arguments, got: %d",
                        this.method, this.parameters.size(), args.length
                ));
            }
            if (!Reflection.isAssignablePrimitiveFrom(this.returnType.type, this.method.type().returnType())) {
                throw new IllegalStateException(String.format(
                        "Expected the method '%s' return type '%s' match return type shape '%s'",
                        this.method, this.method.type().returnType(), this.returnType.getTypeName()
                ));
            }
            for (int i = 0; i < this.parameters.size(); i++) {
                TypeShape<?> p = this.parameters.get(i);
                if (!Reflection.isAssignablePrimitiveFrom(p.type, args[i].getClass())) {
                    throw new IllegalArgumentException(String.format(
                            "Expected method '%s' to be given argument #%s (from 0) of type '%s', got '%s'",
                            this.method, i, p.type.getTypeName(), args[i].getClass().getTypeName()
                    ));
                }
            }
            try {
                return this.method.invokeWithArguments(args);
            } catch (Throwable e) {
                throw new IllegalStateException(String.format(
                        "Unexpected exception while invoking method '%s': %s",
                        this.method, e.getMessage()
                ), e);
            }
        }
    }

    /**
     * Returns true if the type and all of the parameters are assignable from the given other shape.
     *
     * @see Class#isAssignableFrom(Class)
     */
    @InternalUse
    public boolean isAssignablePrimitiveFrom(TypeShape<?> other) {
        if (!Reflection.isAssignablePrimitiveFrom(this.type, other.type)) {
            return false;
        }
        if (this.getParameterCount() != other.getParameterCount()) {
            return false;
        }
        for (int i = 0; i < this.getParameters().size(); i++) {
            TypeShape<?> thisParam = this.getParameters().get(i);
            TypeShape<?> otherParam = other.getParameters().get(i);
            if (!thisParam.isAssignablePrimitiveFrom(otherParam)) {
                return false;
            }
        }
        return true;
    }

    public String asString() {
        var builder = new StringBuilder();
        builder.append(getTypeName());
        if (hasParameters()) {
            builder.append("<")
                    .append(parameters.stream().map(TypeShape::asString).collect(joining(",")))
                    .append(">");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", this.type)
                .add("parameters", this.parameters)
                .add("nullable", this.nullable)
                .toString();
    }

    @InternalUse
    private static ImmutableList<TypeShape<?>> extract(Class<?>[] typeClasses, AnnotatedType[] types) {
        if (typeClasses.length != types.length) {
            throw new IllegalStateException("Unexpected mismatch between class and type arrays.");
        }

        var builder = ImmutableList.<TypeShape<?>>builder();
        for (int i = 0; i < typeClasses.length; i++) {
            builder.add(extract(typeClasses[i], types[i]));
        }
        return builder.build();
    }

    @SuppressWarnings("UnstableApiUsage")
    @InternalUse
    private static TypeShape<?> extract(Class<?> typeClass, AnnotatedType type) {
        var token = TypeToken.of(type.getType());
        var nullable = type.isAnnotationPresent(Nullable.class);
        return extract(typeClass, token, nullable);
    }

    @SuppressWarnings("UnstableApiUsage")
    @InternalUse
    private static TypeShape<?> extract(Class<?> type, TypeToken<?> resolverToken, boolean nullable) {
        var builder = TypeShape.builder(type);
        builder.setNullable(nullable);
        for (var param : type.getTypeParameters()) {
            var nullableParam = param.isAnnotationPresent(Nullable.class);
            var token = resolverToken.resolveType(param);
            builder.addParameter(extract(token.getRawType(), token, nullableParam));
        }
        return builder.build();
    }

    @InternalUse
    public com.google.gson.reflect.TypeToken<?> toGSON() {
        var paramTokens = this.parameters.stream()
                .map(ts -> ts.toGSON().getType())
                .toArray(Type[]::new);
        return com.google.gson.reflect.TypeToken.getParameterized(this.type, paramTokens);
    }

    public static <T> TypeShape<T> of(Class<T> type) {
        return new TypeShape<>(type);
    }

    public static <T> TypeShape<Optional<T>> optional(Class<T> valueClass) {
        return TypeShape.<Optional<T>>builder(Optional.class)
                .addParameter(valueClass)
                .build();
    }

    public static <L, R> TypeShape<Either<L, R>> either(Class<L> leftClass, Class<R> rightClass) {
        return TypeShape.<Either<L, R>>builder(Either.class)
                .addParameter(leftClass)
                .addParameter(rightClass)
                .build();
    }

    public static <E> TypeShape<List<E>> list(Class<E> elementClass) {
        return TypeShape.<List<E>>builder(List.class)
                .addParameter(elementClass)
                .build();
    }

    public static <K, V> TypeShape<Map<K, V>> map(Class<K> keyClass, Class<V> valueClass) {
        return TypeShape.<Map<K, V>>builder(Map.class)
                .addParameter(keyClass)
                .addParameter(valueClass)
                .build();
    }

    public static <T> Builder<T> builder(Class<? super T> type) {
        return new Builder<>(type);
    }

    public final static class Builder<T> {

        private final Class<? super T> type;
        private final ImmutableList.Builder<TypeShape<?>> parameters;
        private boolean isNullable = true;

        private Builder(Class<? super T> type) {
            this.type = requireNonNull(type);
            this.parameters = ImmutableList.builder();
        }

        @CanIgnoreReturnValue
        public Builder<T> setNullable(boolean isNullable) {
            this.isNullable = isNullable;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder<T> addParameters(Class<?>... types) {
            for (var type : types) {
                parameters.add(TypeShape.of(type));
            }
            return this;
        }

        @CanIgnoreReturnValue
        public Builder<T> addParameter(Class<?> type) {
            parameters.add(TypeShape.of(type));
            return this;
        }

        @CanIgnoreReturnValue
        public Builder<T> addParameter(TypeShape<?> shape) {
            parameters.add(shape);
            return this;
        }

        @CheckReturnValue
        public TypeShape<T> build() {
            //noinspection unchecked
            return new TypeShape<>((Class<T>) this.type, this.parameters.build(), this.isNullable);
        }
    }
}
