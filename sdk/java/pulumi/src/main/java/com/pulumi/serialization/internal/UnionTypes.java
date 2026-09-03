package com.pulumi.serialization.internal;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.pulumi.asset.Archive;
import com.pulumi.asset.Asset;
import com.pulumi.asset.AssetOrArchive;
import com.pulumi.core.Either;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.ConstValue;
import com.pulumi.core.annotations.CustomType;
import com.pulumi.core.annotations.EnumType;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.annotations.UnionType;
import com.pulumi.core.internal.Optionals;
import com.pulumi.core.internal.Reflection;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.Resource;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Reads the {@link UnionType} annotations of a generated union interface and selects the member
 * that a wire value belongs to.
 */
@InternalUse
public final class UnionTypes {

    /**
     * A wire value the runtime cannot examine. It matches every member.
     */
    public static final Object UNKNOWN = new Object();

    private UnionTypes() {
        throw new UnsupportedOperationException("static class");
    }

    /**
     * One member of a union interface.
     */
    public static final class Member {
        private final Class<?> type;
        private final TypeShape<?> wire;

        private Member(Class<?> type, TypeShape<?> wire) {
            this.type = type;
            this.wire = wire;
        }

        /**
         * @return the class the runtime produces for this member
         */
        public Class<?> type() {
            return this.type;
        }

        /**
         * @return the type the wire value is converted to before it is wrapped
         */
        public TypeShape<?> wire() {
            return this.wire;
        }

        /**
         * @return true when {@link #type()} is a case class that wraps a value of {@link #wire()}
         */
        public boolean isCase() {
            return !this.type.equals(this.wire.getType());
        }

        @Override
        public String toString() {
            return this.isCase()
                    ? String.format("%s(%s)", this.type.getSimpleName(), this.wire.asString())
                    : this.type.getTypeName();
        }
    }

    /**
     * @return the union annotation of {@code type}, if it is a generated union interface
     */
    public static Optional<UnionType> of(Class<?> type) {
        return Optional.ofNullable(type.getAnnotation(UnionType.class));
    }

    /**
     * @return the members of the union {@code type}, in declaration order
     */
    public static ImmutableList<Member> membersOf(Class<?> type) {
        var members = ImmutableList.<Member>builder();
        for (var unionCase : type.getAnnotationsByType(UnionType.Case.class)) {
            var wire = unionCase.refs().length == 0
                    ? TypeShape.of(unionCase.type())
                    : TypeShape.fromTree(unionCase.refs(), unionCase.tree());
            members.add(new Member(unionCase.type(), wire));
        }
        return members.build();
    }

    /**
     * Selects the member of the union {@code type} that {@code value} belongs to.
     *
     * @param value a wire value: null, a String, a Number, a Boolean, a List, a Map, an
     *              {@link AssetOrArchive}, a {@link Resource}, or {@link #UNKNOWN}
     * @return the member, or a message explaining why no single member fits
     */
    public static Either<String, Member> select(Class<?> type, @Nullable Object value) {
        var members = membersOf(type);
        if (members.isEmpty()) {
            return Either.ofLeft(String.format(
                    "Expected '%s' annotated with @%s to also carry at least one @%s.Case, found none",
                    type.getTypeName(), UnionType.class.getSimpleName(), UnionType.class.getSimpleName()
            ));
        }

        var candidates = members.stream()
                .filter(member -> matches(value, member.wire()))
                .collect(toList());
        if (candidates.size() == 1) {
            return Either.ofRight(candidates.get(0));
        }

        var pool = candidates.isEmpty() ? members : candidates;
        return Either.ofLeft(String.format(
                "Expected a value that fits one member of '%s', got %s that fits %s: %s",
                type.getTypeName(), describe(value), candidates.isEmpty() ? "none of" : "several of",
                pool.stream().map(Member::toString).collect(joining(", "))
        ));
    }

    /**
     * Wraps a value converted to {@code member.wire()} in the member's case class, or returns it
     * unchanged when the member implements the union interface itself.
     */
    public static Object wrap(Member member, Object converted) {
        if (!member.isCase()) {
            return converted;
        }
        var constructors = member.type().getDeclaredConstructors();
        if (constructors.length != 1 || constructors[0].getParameterCount() != 1) {
            throw new IllegalArgumentException(String.format(
                    "Expected union case '%s' to declare exactly one constructor with one parameter, got: %s",
                    member.type().getTypeName(), Arrays.toString(constructors)
            ));
        }
        var constructor = constructors[0];
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(converted);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(String.format(
                    "Failed to construct union case '%s': %s", member.type().getTypeName(), e.getMessage()
            ), e);
        } finally {
            constructor.setAccessible(false);
        }
    }

    /**
     * Reports whether {@code value} can belong to {@code type} on the wire.
     */
    static boolean matches(@Nullable Object value, TypeShape<?> type) {
        if (value == UNKNOWN) {
            return true;
        }
        var clazz = type.getType();

        if (Optional.class.isAssignableFrom(clazz)) {
            return value == null || matches(value, parameter(type, 0));
        }
        if (Output.class.isAssignableFrom(clazz)) {
            return matches(value, parameter(type, 0));
        }
        if (value == null) {
            return false;
        }

        if (Object.class.equals(clazz) || JsonElement.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (String.class.equals(clazz)) {
            return value instanceof String;
        }
        if (boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
            return value instanceof Boolean;
        }
        if (isNumber(clazz)) {
            return value instanceof Number;
        }
        if (clazz.isEnum()) {
            return enumAdmits(type, value);
        }
        if (Either.class.isAssignableFrom(clazz)) {
            return matches(value, parameter(type, 0)) || matches(value, parameter(type, 1));
        }
        if (List.class.isAssignableFrom(clazz)) {
            var element = parameter(type, 0);
            return value instanceof List
                    && ((List<?>) value).stream().allMatch(v -> matches(v, element));
        }
        if (Map.class.isAssignableFrom(clazz)) {
            var element = parameter(type, 1);
            return value instanceof Map
                    && ((Map<?, ?>) value).values().stream().allMatch(v -> matches(v, element));
        }
        if (Archive.class.isAssignableFrom(clazz)) {
            return value instanceof Archive;
        }
        if (AssetOrArchive.class.isAssignableFrom(clazz)) {
            return value instanceof Asset;
        }
        if (Resource.class.isAssignableFrom(clazz)) {
            return clazz.isInstance(value);
        }
        if (of(clazz).isPresent()) {
            return membersOf(clazz).stream().anyMatch(member -> matches(value, member.wire()));
        }

        var properties = propertiesOf(clazz);
        if (properties.isPresent()) {
            return value instanceof Map && objectMatches((Map<?, ?>) value, properties.get());
        }
        return clazz.isInstance(value);
    }

    private static boolean objectMatches(Map<?, ?> value, List<Property> properties) {
        var declared = properties.stream().map(p -> p.name).collect(toList());
        if (!declared.containsAll(value.keySet())) {
            return false;
        }
        for (var property : properties) {
            var v = value.get(property.name);
            if (v == null) {
                if (property.required) {
                    return false;
                }
                continue;
            }
            if (property.constant != null && !constantAdmits(property, v)) {
                return false;
            }
            if (!matches(v, property.type)) {
                return false;
            }
        }
        return true;
    }

    private static boolean constantAdmits(Property property, Object value) {
        if (value == UNKNOWN) {
            return true;
        }
        var clazz = unwrap(property.type).getType();
        if (isNumber(clazz)) {
            return value instanceof Number
                    && ((Number) value).doubleValue() == Double.parseDouble(property.constant);
        }
        if (boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
            return Boolean.valueOf(property.constant).equals(value);
        }
        return property.constant.equals(value);
    }

    private static boolean enumAdmits(TypeShape<?> type, Object value) {
        var converter = type.getAnnotatedMethod(EnumType.Converter.class);
        for (var constant : type.getType().getEnumConstants()) {
            try {
                var literal = converter.invoke(constant);
                if (literal instanceof Number && value instanceof Number) {
                    if (((Number) literal).doubleValue() == ((Number) value).doubleValue()) {
                        return true;
                    }
                } else if (Objects.equals(literal, value)) {
                    return true;
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(String.format("Unexpected exception: %s", e.getMessage()), e);
            }
        }
        return false;
    }

    /**
     * A property of an object member: a builder setter of a {@link CustomType}, or an
     * {@link Import} field of an args class.
     */
    private static final class Property {
        private final String name;
        private final boolean required;
        private final TypeShape<?> type;
        @Nullable
        private final String constant;

        private Property(String name, boolean required, TypeShape<?> type, @Nullable ConstValue constant) {
            this.name = name;
            this.required = required;
            this.type = type;
            this.constant = constant == null ? null : constant.value();
        }
    }

    private static Optional<List<Property>> propertiesOf(Class<?> clazz) {
        var shape = TypeShape.of(clazz);
        if (shape.hasAnnotatedClass(CustomType.Builder.class)) {
            var builder = shape.getAnnotatedClass(CustomType.Builder.class);
            var properties = Converter.processSetters(builder, method -> {
                var parameter = Converter.extractSetterParameter(method);
                return new Property(
                        Converter.extractSetterName(method),
                        !parameter.isAnnotationPresent(Nullable.class),
                        TypeShape.extract(parameter),
                        method.getAnnotation(ConstValue.class)
                );
            });
            return Optional.of(ImmutableList.copyOf(properties.values()));
        }

        var fields = Reflection.allFields(clazz).stream()
                .filter(field -> field.isAnnotationPresent(Import.class))
                .collect(toList());
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fields.stream().map(field -> {
            var annotation = field.getAnnotation(Import.class);
            return new Property(
                    Optionals.ofBlank(annotation.name()).orElse(field.getName()),
                    annotation.required(), TypeShape.extract(field), field.getAnnotation(ConstValue.class)
            );
        }).collect(toList()));
    }

    private static TypeShape<?> unwrap(TypeShape<?> type) {
        var clazz = type.getType();
        if (Optional.class.isAssignableFrom(clazz) || Output.class.isAssignableFrom(clazz)) {
            return unwrap(parameter(type, 0));
        }
        return type;
    }

    private static TypeShape<?> parameter(TypeShape<?> type, int index) {
        return type.getParameter(index).orElseGet(() -> TypeShape.of(Object.class));
    }

    private static boolean isNumber(Class<?> clazz) {
        return int.class.equals(clazz) || Integer.class.equals(clazz)
                || double.class.equals(clazz) || Double.class.equals(clazz);
    }

    private static String describe(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value == UNKNOWN) {
            return "an unknown value";
        }
        if (value instanceof Map) {
            return "an object with keys " + new TreeSet<>(((Map<?, ?>) value).keySet());
        }
        return "a " + value.getClass().getSimpleName();
    }
}
