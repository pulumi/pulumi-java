package com.pulumi.core;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.CheckReturnValue;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.pulumi.core.internal.PulumiCollectors.toSingleton;
import static java.util.stream.Collectors.joining;

public final class TypeShape<T> {

    public static final TypeShape<Void> Empty = TypeShape.of(Void.class);

    private final Class<? super T> type;
    private final ImmutableList<TypeShape<?>> parameters;

    private TypeShape(Class<? super T> type) {
        this(type, ImmutableList.of());
    }

    private TypeShape(Class<? super T> type, ImmutableList<TypeShape<?>> parameters) {
        this.type = Objects.requireNonNull(type);
        this.parameters = Objects.requireNonNull(parameters);
    }

    public Class<T> getType() {
        //noinspection unchecked
        return (Class<T>) this.type;
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

    public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationType) {
        return Optional.ofNullable(this.getType().getAnnotation(annotationType));
    }

    public <A extends Annotation> Constructor<?> getAnnotatedConstructor(Class<A> constructorAnnotation) {
        return Arrays.stream(this.type.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(constructorAnnotation))
                .peek(c -> c.setAccessible(true))
                .collect(toSingleton(cause -> new IllegalArgumentException(String.format(
                        "Expected target type '%s' to have exactly one constructor annotated with @%s, got: %s",
                        getTypeName(), constructorAnnotation.getSimpleName(), cause
                ))));
    }

    public <A extends Annotation> boolean hasAnnotatedConstructor(Class<A> constructorAnnotation) {
        return Arrays.stream(this.type.getDeclaredConstructors())
                .anyMatch(c -> c.isAnnotationPresent(constructorAnnotation));
    }

    public <A extends Annotation> Method getAnnotatedMethod(Class<A> methodAnnotation) {
        return Arrays.stream(this.getType().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(methodAnnotation))
                .peek(m -> m.setAccessible(true))
                .collect(toSingleton(cause -> new IllegalArgumentException(String.format(
                        "Expected target type '%s' to have exactly one method annotated with @%s, got: %s",
                        getTypeName(), methodAnnotation.getSimpleName(), cause
                ))));
    }

    public <A extends Annotation> Class<?> getAnnotatedClass(Class<A> classAnnotation) {
        return Arrays.stream(this.getType().getDeclaredClasses())
                .filter(c -> c.isAnnotationPresent(classAnnotation))
                .collect(toSingleton(cause -> new IllegalArgumentException(String.format(
                        "Expected target type '%s' to have exactly one declared class annotated with @%s, got: %s",
                        getTypeName(), classAnnotation.getSimpleName(), cause
                ))));
    }

    public <A extends Annotation> boolean hasAnnotatedClass(Class<A> classAnnotation) {
        return Arrays.stream(this.getType().getDeclaredClasses())
                .anyMatch(c -> c.isAnnotationPresent(classAnnotation));
    }

    /**
     * Returns true if the type and all the parameters are assignable from the given other shape.
     *
     * @see Class#isAssignableFrom(Class)
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public <U> boolean isAssignableFrom(TypeShape<U> other) {
        if (!this.type.isAssignableFrom(other.type)) {
            return false;
        }
        if (this.getParameterCount() != other.getParameterCount()) {
            return false;
        }
        for (int i = 0; i < this.getParameters().size(); i++) {
            TypeShape<?> thisParam = this.getParameters().get(i);
            TypeShape<?> otherParam = other.getParameters().get(i);
            if (!thisParam.isAssignableFrom(otherParam)) {
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
                .add("type", type)
                .add("parameters", parameters)
                .toString();
    }

    public static TypeShape<?> extract(Parameter parameter) {
        var parameterClass = parameter.getType();
        var token = TypeToken.of(parameter.getParameterizedType());
        return extract(parameterClass, token);
    }

    private static TypeShape<?> extract(Class<?> type, TypeToken<?> resolverToken) {
        var builder = TypeShape.builder(type);
        for (var param : type.getTypeParameters()) {
            var token = resolverToken.resolveType(param);
            builder.addParameter(extract(token.getRawType(), token));
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

        private Builder(Class<? super T> type) {
            this.type = Objects.requireNonNull(type);
            this.parameters = ImmutableList.builder();
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
            return new TypeShape<>(this.type, this.parameters.build());
        }
    }
}
