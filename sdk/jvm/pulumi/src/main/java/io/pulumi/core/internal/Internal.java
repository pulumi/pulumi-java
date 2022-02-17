package io.pulumi.core.internal;

import io.pulumi.Stack;
import io.pulumi.deployment.CallOptions;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.resources.InputArgs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.pulumi.core.internal.PulumiCollectors.toSingleton;

public class Internal {

    private Internal() {
        throw new UnsupportedOperationException("static class");
    }

    public static CallOptions.Internal from(CallOptions o) {
        return from(o, CallOptions.Internal.class);
    }

    public static InvokeOptions.Internal from(InvokeOptions o) {
        return from(o, InvokeOptions.Internal.class);
    }

    public static InputArgs.Internal from(InputArgs a) {
        return from(a, InputArgs.Internal.class);
    }

    public static Stack.Internal from(Stack s) {
        return from(s, Stack.Internal.class);
    }

    public static <T, I> I from(T value, Class<I> internalType) {
        var type = value.getClass();
        var fieldAnnotation = Field.class;
        java.lang.reflect.Field internal = Reflection.allFields(type).stream()
                .filter(f -> f.isAnnotationPresent(fieldAnnotation))
                .peek(f -> f.setAccessible(true))
                .filter(f -> internalType.isAssignableFrom(f.getType()))
                .collect(toSingleton(cause -> new IllegalArgumentException(String.format(
                        "Expected type '%s' to have one private field of type '%s' annotated with: '%s', got: %s",
                        type.getTypeName(), internalType.getTypeName(), fieldAnnotation, cause
                ))));
        try {
            //noinspection unchecked
            return (I) internal.get(value);
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
    public @interface Field {
        /* Empty */
    }
}
