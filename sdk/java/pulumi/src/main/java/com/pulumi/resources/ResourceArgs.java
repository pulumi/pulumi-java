package com.pulumi.resources;

import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.core.internal.OutputInternal;
import com.pulumi.serialization.internal.Deserializer;
import com.pulumi.serialization.internal.Reflection;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Base type for all resource argument classes.
 */
@ParametersAreNonnullByDefault
public abstract class ResourceArgs extends InputArgs {
    public static final ResourceArgs Empty = new ResourceArgs() {
        // Empty
    };

    @Override
    protected void validateMember(Class<?> memberType, String fullName) {
        // No validation. A member may or may not be Input.
    }

    public static <T extends ResourceArgs> T deserialize(com.google.protobuf.Struct args, Class<T> type) throws ReflectiveOperationException {
        var deserializer = new Deserializer(Log.ignore());

        for (var m : type.getMethods()) {
            if (Reflection.isStaticMethod(m) && m.getParameterCount() == 0 && m.getName().equals("builder")) {
                var builder = m.invoke(null);
                var builderClass = builder.getClass();

                for (var entry : args.getFieldsMap().entrySet()) {
                    String k = entry.getKey();

                    var valueData = deserializer.deserialize(entry.getValue());

                    var value = new OutputInternal<>(valueData).get();

                    var builderMethod = builderClass.getMethod(k, value.getClass());
                    builderMethod.invoke(builder, value);
                }

                var argsMethod = builderClass.getMethod("build");
                @SuppressWarnings("unchecked") var result = (T) argsMethod.invoke(builder);
                return result;
            }
        }

        return null;
    }
}
