package io.pulumi.core.internal;

import io.pulumi.Stack;
import io.pulumi.core.AssetOrArchive;
import io.pulumi.core.Output;
import io.pulumi.deployment.CallOptions;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.resources.*;
import io.pulumi.resources.ComponentResource.ComponentResourceInternal;
import io.pulumi.resources.CustomResource.CustomResourceInternal;
import io.pulumi.resources.Resource.ResourceInternal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.util.Objects.requireNonNull;

public class Internal {

    private Internal() {
        throw new UnsupportedOperationException("static class");
    }

    public static <T> OutputInternal<T> of(Output<T> output) {
        return OutputInternal.cast(output);
    }

    public static CallOptions.CallOptionsInternal from(CallOptions o) {
        return from(o, CallOptions.CallOptionsInternal.class);
    }

    public static InvokeOptions.InvokeOptionsInternal from(InvokeOptions o) {
        return from(o, InvokeOptions.InvokeOptionsInternal.class);
    }

    public static InputArgs.InputArgsInternal from(InputArgs a) {
        return from(a, InputArgs.InputArgsInternal.class);
    }

    public static Stack.StackInternal from(Stack s) {
        return from(s, Stack.StackInternal.class);
    }

    public static ProviderResource.ProviderResourceInternal from(ProviderResource r) {
        return from(r, ProviderResource.ProviderResourceInternal.class);
    }

    public static CustomResourceInternal from(CustomResource r) {
        return from(r, CustomResourceInternal.class);
    }

    public static ComponentResourceInternal from(ComponentResource r) {
        return from(r, ComponentResourceInternal.class);
    }

    public static ResourceInternal from(Resource r) {
        return from(r, ResourceInternal.class);
    }

    public static AssetOrArchive.AssetOrArchiveInternal from(AssetOrArchive a) {
        return from(a, AssetOrArchive.AssetOrArchiveInternal.class);
    }

    /**
     * @param value        a class instance to return in internal class from
     * @param internalType the upper bound of internal type
     * @param <T>          type of the class containing the internal class
     * @param <I>          the internal class
     * @return an internal field value with a type not greater than
     * (in inheritance hierarchy) the provided internalType
     */
    public static <T, I> I from(T value, Class<I> internalType) {
        requireNonNull(value, "'value' cannot be 'null'");
        var type = value.getClass();
        var fieldAnnotation = InternalField.class;
        java.lang.reflect.Field internal = Reflection.allFields(type).stream()
                .filter(f -> f.isAnnotationPresent(fieldAnnotation))
                .peek(f -> f.setAccessible(true))
                .reduce((f1, f2) -> {
                    var c1 = f1.getType();
                    var c2 = f2.getType();
                    if (c1.equals(internalType)) {
                        return f1;
                    }
                    if (c2.equals(internalType)) {
                        return f2;
                    }
                    // check if c1 is the same or a superclass of c2
                    if (c1.isAssignableFrom(c2)) {
                        return f1; // return superclass
                    }
                    // check if c2 is the same or is a superclass of c1
                    if (c2.isAssignableFrom(c1)) {
                        return f2; // return superclass
                    }
                    throw new IllegalStateException(String.format(
                            "Can't decide what type to return for '%s', options: '%s','%s'",
                            internalType.getTypeName(), c1.getTypeName(), c2.getTypeName()
                    ));
                })
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "Expected type '%s' to have at least one private field of type '%s' annotated with: '%s'",
                        type.getTypeName(), internalType.getTypeName(), fieldAnnotation
                )));
        try {
            var obj = internal.get(value);
            if (obj == null) {
                throw new IllegalStateException(String.format(
                        "Can't get the value of an internal field '%s.%s', the value is 'null'",
                        internal.getDeclaringClass().getTypeName(), internal.getName()
                ));
            }
            if (!internalType.isInstance(obj)) {
                throw new IllegalStateException(String.format(
                        "Can't cast the value of an internal field '%s.%s' of type '%s' to '%s'",
                        internal.getDeclaringClass().getTypeName(), internal.getName(),
                        internal.getType().getTypeName(), internalType.getTypeName()
                ));
            }
            //noinspection unchecked
            return (I) obj;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "Can't get the value of an internal field '%s.%s', error: %s",
                    type.getTypeName(), internal.getName(), e.getMessage()
            ), e);
        } finally {
            internal.setAccessible(false);
        }
    }

    /**
     * @see #from(Object, Class)
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface InternalField {
        /* Empty */
    }
}
