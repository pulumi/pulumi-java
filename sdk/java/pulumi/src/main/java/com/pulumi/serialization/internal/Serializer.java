package com.pulumi.serialization.internal;

import com.google.gson.JsonElement;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.pulumi.Log;
import com.pulumi.asset.Archive;
import com.pulumi.asset.Archive.InvalidArchive;
import com.pulumi.asset.Asset;
import com.pulumi.asset.Asset.InvalidAsset;
import com.pulumi.asset.AssetOrArchive;
import com.pulumi.core.Either;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.EnumType;
import com.pulumi.core.internal.CompletableFutures;
import com.pulumi.core.internal.Constants;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.InputArgs;
import com.pulumi.resources.Resource;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.pulumi.core.internal.CompletableFutures.ignoreNullMapValues;
import static com.pulumi.core.internal.CompletableFutures.joinMapValues;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class Serializer {

    public final Set<Resource> dependentResources;

    private final Log log;

    public Serializer(Log log) {
        this.log = requireNonNull(log);
        this.dependentResources = new HashSet<>();
    }

    /**
     * Takes in an arbitrary object and serializes it into a uniform form that can converted
     * trivially to a protobuf to be passed to the Pulumi engine.
     * <p>
     * The allowed 'basis' forms that can be serialized are:
     * <ul>
     * <li>{@code null}</li>
     * <li>{@code bool} or {@code Boolean}</li>
     * <li>{@code int} or {@code Integer}</li>
     * <li>{@code double} or {@code Double}</li>
     * <li>{@code String}</li>
     * <li>@see {@link Asset}</li>
     * <li>@see {@link Archive}</li>
     * <li>@see {@link Resource}</li>
     * <li>@see {@link com.pulumi.resources.ResourceArgs}</li>
     * <li>@see {@link com.google.gson.JsonElement}</li>
     * </ul>
     * <p>
     * Additionally, other more complex objects can be serialized as long as they are built out of serializable objects.
     * <br>
     * These complex objects include:
     * <ul>
     * <li>@see {@link com.pulumi.core.Output}. As long as they are an Output of a serializable type.</li>
     * <li>@see {@link java.util.List}. As long as all elements in the list are serializable.</li>
     * <li>@see {@link java.util.Map}. As long as the key of the dictionary are {@code String} and as long as the value are all serializable.</li>
     * <li>@see {@link com.pulumi.core.Either}. As long as both left and right are serializable.</li>
     * </ul>
     * <p>
     * As the final complex type, an {@link Enum} is allowed, but there are special requirements as follows.<br>
     * It must be a standard enum (with {@code ordinal} value only) or:
     * <ul>
     *     <li>Have a constructor that takes a single parameter of {@code int}, {@code Integer}, {@code double}, {@code Double} or {@code String}. The constructor can and <b>should</b> be private.</li>
     *     <li>Have an underlying type of {@code int}, {@code Integer}, {@code double}, {@code Double} or {@code String}.</li>
     *     <li>Overriding {@code toString} isn't required, but is recommended and is what our codegen does.</li>
     * </ul>
     * <p>
     * <b>No other forms are allowed.</b>
     * <p>
     * <p>
     * This function will only return values of a very specific shape. Specifically, the
     * result values returned will <b>only</b> be one of:
     * <ul>
     * <li>{@code null}</li>
     * <li>{@code bool}</li>
     * <li>{@code int}</li>
     * <li>{@code double}</li>
     * <li>{@code String}</li>
     * <li>An {@link java.util.List} containing only these result value types.</li>
     * <li>An {@link java.util.Map} where the keys are {@code String}s and the values are only these result value types.</li>
     * </ul>
     * <p>
     * No other result type are allowed to be returned.
     */
    public CompletableFuture</* @Nullable */ Object> serializeAsync(String ctx, @Nullable Object prop, boolean keepResources) {
        requireNonNull(ctx);

        // IMPORTANT:
        // IMPORTANT: Keep this in sync with serializesPropertiesSync in invoke.ts
        // IMPORTANT:
        if (prop == null ||
                prop instanceof Boolean ||
                prop instanceof Integer ||
                prop instanceof Double ||
                prop instanceof String) {
            log.excessive(String.format("Serialize property[%s]: primitive=%s", ctx, prop));
            return CompletableFuture.completedFuture(/* @Nullable */ prop);
        }

        if (prop instanceof Optional) {
            //noinspection unchecked
            var optional = (Optional<Object>) prop;
            log.excessive(String.format("Serialize property[%s]: Recursion into Optional", ctx));

            return serializeAsync(ctx, optional.orElse(null), keepResources);
        }

        if (prop instanceof InputArgs) {
            var args = (InputArgs) prop;
            //noinspection RedundantCast
            return serializeInputArgsAsync(ctx, args, keepResources).thenApply(m -> (Object) m);
        }

        if (prop instanceof AssetOrArchive) {
            var assetOrArchive = (AssetOrArchive) prop;
            //noinspection RedundantCast
            return serializeAssetOrArchiveAsync(ctx, assetOrArchive, keepResources).thenApply(m -> (Object) m);
        }

        if (prop instanceof CompletableFuture) {
            throw new IllegalArgumentException(
                    String.format("CompletableFutures are not allowed for serialization. Please wrap your CompletableFutures in an Output:\n\t%s", ctx));
        }

        if (prop instanceof Either) {
            //noinspection unchecked
            var either = (Either<Object, Object>) prop;
            log.excessive(String.format("Serialize property[%s]: Recursion into Either", ctx));

            return serializeAsync(ctx, either.either(Function.identity(), Function.identity()), keepResources);
        }

        if (prop instanceof JsonElement) {
            var element = (JsonElement) prop;
            log.excessive(String.format("Serialize property[%s]: Recursion into JsonElement", ctx));

            return CompletableFuture.completedFuture(serializeJson(ctx, element));
        }

        if (prop instanceof Output) {
            //noinspection unchecked
            var output = (Output<Object>) prop;
            log.excessive(String.format("Serialize property[%s]: Recursion into InputOutput", ctx));

            return Internal.of(output).getDataAsync().thenCompose(
                    (OutputData<Object> data) -> {
                        this.dependentResources.addAll(data.getResources());

                        // When serializing an InputOutput, we will either
                        // serialize it as its resolved value or the "unknown value" sentinel.
                        // We will do the former for all outputs created directly by user code (such outputs always
                        // resolve isKnown to true) and for any resource outputs that were resolved with known values.
                        var isKnown = data.isKnown();
                        var isSecret = data.isSecret();

                        if (!isKnown) {
                            return CompletableFuture.completedFuture(Constants.UnknownValue);
                        }

                        if (isSecret) {
                            return serializeAsync(String.format("%s.id", ctx), data.getValueNullable(), keepResources).thenApply(
                                    /* @Nullable */ value -> {
                                        var result = new HashMap<String, /* @Nullable */ Object>();
                                        result.put(Constants.SpecialSigKey, Constants.SpecialSecretSig);
                                        result.put(Constants.SecretValueName, value);
                                        return result;
                                    }
                            );
                        }
                        return serializeAsync(String.format("%s.id", ctx), data.getValueNullable(), keepResources);
                    }
            );
        }

        if (prop instanceof CustomResource) {
            var customResource = (CustomResource) prop;
            // Resources aren't serializable; instead, we serialize them as references to the ID property.
            log.excessive(String.format("Serialize property[%s]: Encountered CustomResource", ctx));

            this.dependentResources.add(customResource);

            return serializeAsync(String.format("%s.id", ctx), customResource.id(), keepResources).thenCompose(
                    /* @Nullable */ id -> {
                        if (keepResources) {
                            //noinspection ConstantConditions
                            return serializeAsync(String.format("%s.urn", ctx), customResource.urn(), keepResources).thenApply(
                                    /* @Nullable */ urn -> {
                                        var result = new HashMap<String, /* @Nullable */ Object>();
                                        result.put(Constants.SpecialSigKey, Constants.SpecialResourceSig);
                                        result.put(Constants.ResourceUrnName, urn);
                                        result.put(Constants.ResourceIdName, Objects.equals(id, Constants.UnknownValue) ? "" : id);
                                        return result;
                                    }
                            );
                        }
                        return CompletableFuture.completedFuture(id);
                    }
            );
        }

        if (prop instanceof ComponentResource) {
            var componentResource = (ComponentResource) prop;
            // Component resources often can contain cycles in them. For example, an AWS {@code SecurityGroupRule}
            // can point a the AWS {@code SecurityGroup}, which in turn can point
            // back to its rules through its {@code egressRules} and {@code ingressRules} properties.
            // If serializing out the {@code SecurityGroup} resource ends up trying to serialize out
            // those properties, a deadlock will happen, due to waiting on the child, which is
            // waiting on the parent.
            //
            // Practically, there is no need to actually serialize out a component.  It doesn't
            // represent a real resource, nor does it have normal properties that need to be
            // tracked for differences (since changes to its properties don't represent changes
            // to resources in the real world).
            //
            // So, to avoid these problems, while allowing a flexible and simple programming
            // model, we just serialize out the component as its urn.  This allows the component
            // to be identified and tracked in a reasonable manner, while not causing us to
            // compute or embed information about it that is not needed, and which can lead to
            // deadlocks.
            log.excessive(String.format("Serialize property[%s]: Encountered ComponentResource", ctx));

            return serializeAsync(String.format("%s.urn", ctx), componentResource.urn(), keepResources).thenApply(
                    /* @Nullable */ urn -> {
                        if (keepResources) {
                            var result = new HashMap<String, /* @Nullable */ Object>();
                            result.put(Constants.SpecialSigKey, Constants.SpecialResourceSig);
                            result.put(Constants.ResourceUrnName, urn);
                            return result;
                        }
                        return urn;
                    }
            );
        }

        if (prop instanceof Map) {
            //noinspection unchecked
            var map = (Map<Object, /* @Nullable */ Object>) prop;
            //noinspection RedundantCast
            return serializeMapAsync(ctx, map, keepResources).thenApply(m -> (Object) m);
        }

        if (prop instanceof List) {
            //noinspection unchecked
            var list = (List</* @Nullable */ Object>) prop;
            //noinspection RedundantCast
            return serializeListAsync(ctx, list, keepResources).thenApply(l -> (Object) l);
        }

        if (prop instanceof Enum) {
            var shape = TypeShape.of(prop.getClass());
            var converter = shape.getAnnotatedMethod(EnumType.Converter.class);
            try {
                var value = converter.invoke(prop);
                if (value instanceof Double || value instanceof String) {
                    return CompletableFuture.completedFuture(value);
                } else {
                    throw new UnsupportedOperationException(
                            String.format(
                                    "Serialize property[%s]: Type '%s' is not a supported argument type." +
                                            " Expected an Enum with a converter method annotated with '%s' and return type of Double or String, got a converted type: '%s'",
                                    ctx,
                                    prop.getClass().getTypeName(),
                                    EnumType.Converter.class,
                                    value.getClass().getTypeName()
                            )
                    );
                }
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException(String.format("Unexpected exception: %s", ex.getMessage()), ex);
            }
        }

        throw new UnsupportedOperationException(String.format(
                "Serialize property[%s]: Type '%s' is not a supported argument type.",
                ctx, prop.getClass().getSimpleName()
        ));
    }

    @Nullable
    private Object serializeJson(String ctx, JsonElement element) {
        requireNonNull(ctx);
        requireNonNull(element);

        if (element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            var primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return primitive.getAsString();
            }
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            if (primitive.isNumber()) {
                return primitive.getAsDouble();
            }
            throw new UnsupportedOperationException(String.format("unsupported JSON primitive type: '%s'", primitive));
        }
        if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            var result = new ArrayList</* @Nullable */>(array.size());
            var index = 0;
            for (var child : array) {
                result.add(serializeJson(String.format("%s[%s]", ctx, index), child));
                index++;
            }
            return result; // can contain null elements!
        }
        if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            var result = new HashMap<String, /* @Nullable */ Object>();
            for (var entry : object.entrySet()) {
                result.put(entry.getKey(), serializeJson(String.format("%s.%s", ctx, entry.getKey()), entry.getValue()));
            }
            return result; // can contain null values!
        }
        throw new UnsupportedOperationException(String.format("Unknown JsonElement: '%s'", element));
    }

    private CompletableFuture<Map<String, /* @Nullable */ Object>> serializeAssetOrArchiveAsync(String ctx, AssetOrArchive assetOrArchive, boolean keepResources) {
        log.excessive(String.format("Serialize property[%s]: asset/archive=%s", ctx, assetOrArchive.getClass().getSimpleName()));

        if (assetOrArchive instanceof InvalidAsset) {
            throw new UnsupportedOperationException("Cannot serialize invalid asset");
        }
        if (assetOrArchive instanceof InvalidArchive) {
            throw new UnsupportedOperationException("Cannot serialize invalid archive");
        }

        var internalAssetOrArchive = Internal.from(assetOrArchive);
        var propName = internalAssetOrArchive.getPropName();
        return serializeAsync(ctx + "." + propName, internalAssetOrArchive.getValue(), keepResources).thenApply(
                /* @Nullable */ value -> {
                    var result = new HashMap<String, /* @Nullable */ Object>();
                    result.put(Constants.SpecialSigKey, internalAssetOrArchive.getSigKey());
                    result.put(internalAssetOrArchive.getPropName(), value);
                    return result;
                }
        );
    }

    private CompletableFuture<Map<String, /* @Nullable */ Object>> serializeInputArgsAsync(String ctx, InputArgs args, boolean keepResources) {
        log.excessive(String.format("Serialize property[%s]: Recursion into ResourceArgs", ctx));

        return Internal.from(args).toNullableMapAsync(this.log).thenCompose(
                map -> serializeMapAsync(ctx, map, keepResources)
        );
    }

    private CompletableFuture<ArrayList</* @Nullable */ Object>> serializeListAsync(String ctx, List</* @Nullable */ Object> list, boolean keepResources) {
        log.excessive(String.format("Serialize property[%s]: Hit list", ctx));

        var resultFutures = new ArrayList<CompletableFuture</* @Nullable */ Object>>(list.size());
        for (int i = 0, n = list.size(); i < n; i++) {
            log.excessive(String.format("Serialize property[%s]: array[%d] element", ctx, i));
            resultFutures.add(serializeAsync(String.format("%s[%s]", ctx, i), list.get(i), keepResources));
        }

        // ArrayList will preserve the null's
        return CompletableFutures.flatAllOf(resultFutures).thenApply(
                completed -> completed.stream()
                        .map(CompletableFuture::join)
                        .collect(toCollection(ArrayList::new))
        );
    }

    private CompletableFuture<Map<String, Object>> serializeMapAsync(String ctx, Map<Object, /* @Nullable */ Object> rawMap, boolean keepResources) {
        log.excessive(String.format("Serialize property[%s]: Hit dictionary", ctx));
        var map = stringKeysOrThrow(rawMap);
        var resultFutures = new HashMap<String, CompletableFuture</* @Nullable */ Object>>();
        for (var key : map.keySet()) {
            log.excessive(String.format("Serialize property[%s]: object.%s", ctx, key));
            resultFutures.put(key, serializeAsync(String.format("%s.%s", ctx, key), map.get(key), keepResources));
        }

        // We treat entries with null values as if they do not exist.
        return CompletableFutures.flatAllOf(resultFutures).thenApply(
                completed -> completed.entrySet().stream()
                        .filter(ignoreNullMapValues())
                        .map(joinMapValues())
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    /**
     * Given a @see {@link Map} produced by @see #serializeAsync
     * produces the equivalent @see {@link Struct} that can be passed to the Pulumi engine.
     */
    @InternalUse
    public static Struct createStruct(Map<String, Object> serializedMap) {
        var result = Struct.newBuilder();
        serializedMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(e -> result.putFields(e.getKey(), createValue(e.getValue())));
        return result.build();
    }

    /**
     * Internal use only. Creates a gRPC Protobuf Value.
     */
    @InternalUse
    public static Value createValue(@Nullable Object value) {
        var builder = Value.newBuilder();
        if (value == null) {
            return builder.setNullValue(NullValue.NULL_VALUE).build();
        }
        if (value instanceof Integer) {
            return builder.setNumberValue(((Integer) value)).build();
        }
        if (value instanceof Double) {
            return builder.setNumberValue(((Double) value)).build();
        }
        if (value instanceof Boolean) {
            return builder.setBoolValue((Boolean) value).build();
        }
        if (value instanceof String) {
            return builder.setStringValue((String) value).build();
        }
        if (value instanceof List) {
            //noinspection unchecked
            var list = ((List<Object>) value).stream()
                    .map(Serializer::createValue)
                    .collect(collectingAndThen(toList(),
                            l -> ListValue.newBuilder().addAllValues(l))
                    );
            return builder.setListValue(list).build();
        }
        if (value instanceof Map) {
            //noinspection rawtypes
            Map<String, Object> map = stringKeysOrThrow((Map) value);
            return builder.setStructValue(createStruct(map)).build();
        }
        throw new UnsupportedOperationException(String.format(
                "Unsupported value when converting to protobuf, type: '%s'",
                value.getClass().getTypeName()
        ));
    }

    /**
     * @throws UnsupportedOperationException if the given map key is not String
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, Object> stringKeysOrThrow(Map map) {
        map.keySet().stream().findFirst().ifPresent(key -> {
            if (!(key instanceof String)) {
                throw new UnsupportedOperationException(String.format(
                        "Expected a 'String' key, got: %s",
                        key.getClass().getTypeName()
                ));
            }
        });
        return (Map<String, Object>) map;
    }
}
