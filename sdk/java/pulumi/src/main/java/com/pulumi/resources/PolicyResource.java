package com.pulumi.resources;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.Value.KindCase;
import com.google.protobuf.util.JsonFormat;
import com.pulumi.asset.Archive;
import com.pulumi.asset.Asset;
import com.pulumi.asset.AssetArchive;
import com.pulumi.asset.AssetOrArchive;
import com.pulumi.asset.FileArchive;
import com.pulumi.asset.FileAsset;
import com.pulumi.asset.RemoteArchive;
import com.pulumi.asset.RemoteAsset;
import com.pulumi.asset.StringAsset;
import com.pulumi.core.internal.Constants;
import com.pulumi.serialization.internal.Deserializer;
import com.pulumi.serialization.internal.PolicyResourcePackages;
import com.pulumi.serialization.internal.Reflection;
import com.pulumi.serialization.internal.Structs;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Base type for all resource policy classes.
 */
@ParametersAreNonnullByDefault
public abstract class PolicyResource {
    private String urn;

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public String getUrn() {
        return this.urn;
    }

    public static <T extends PolicyResource> T deserialize(Struct args, Class<T> type) {
        try {
            T result = type.getDeclaredConstructor().newInstance();

            for (var entry : args.getFieldsMap().entrySet()) {
                try {
                    Field field = type.getField(entry.getKey());

                    var valueData = deserializeInner(entry.getValue(), field.getGenericType());
                    if (valueData instanceof String && field.getType() != String.class) {
                        var valueBuilder = Value.newBuilder();
                        JsonFormat.parser().ignoringUnknownFields().merge((String) valueData, valueBuilder);
                        valueData = deserializeInner(valueBuilder.build(), field.getGenericType());
                    }

                    if (valueData != null) {
                        field.set(result, valueData);
                    }

                } catch (NoSuchFieldException e) {
                    // Ignore missing fields
                }
            }

            return result;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object deserializeInner(Value value, Type type) {
        requireNonNull(value, "Expected value to be non-null");

        return deserializeCore(value, v -> {
            switch (v.getKindCase()) {
                case NUMBER_VALUE:
                    return deserializeDouble(v);

                case STRING_VALUE:
                    return deserializeString(v);

                case BOOL_VALUE:
                    return deserializeBoolean(v);

                case STRUCT_VALUE:
                    return deserializeStruct(v, Reflection.getTypeArgument(type, 1));

                case LIST_VALUE:
                    return deserializeList(v, Reflection.getTypeArgument(type, 0));

                case NULL_VALUE:
                    return null;

                case KIND_NOT_SET:
                    throw new UnsupportedOperationException("Should never get 'None' type when deserializing protobuf");
                default:
                    throw new UnsupportedOperationException("Unknown type when deserializing protobuf: " + v.getKindCase());
            }
        });
    }

    private static <T> T deserializeCore(Value maybeSecret, Function<Value, T> func) {
        var value = unwrapSecret(maybeSecret);

        if (value.getKindCase() == KindCase.STRING_VALUE && Constants.UnknownValue.equals(value.getStringValue())) {
            // always deserialize unknown as the null value.
            return null;
        }

        var assetOrArchive = tryDeserializeAssetOrArchive(value);
        if (assetOrArchive.isPresent()) {
            //noinspection unchecked
            return (T) assetOrArchive.get();
        }

        var resource = tryDeserializeResource(value);
        //noinspection OptionalIsPresent
        if (resource.isPresent()) {
            //noinspection unchecked
            return (T) resource.get();
        }

        return func.apply(value);
    }

    private static boolean deserializeBoolean(Value value) {
        return deserializePrimitive(value, KindCase.BOOL_VALUE, Value::getBoolValue);
    }

    private static String deserializeString(Value value) {
        return deserializePrimitive(value, KindCase.STRING_VALUE, Value::getStringValue);
    }

    private static double deserializeDouble(Value value) {
        return deserializePrimitive(value, KindCase.NUMBER_VALUE, Value::getNumberValue);
    }

    private static <T> T deserializePrimitive(Value value, Value.KindCase kind, Function<Value, T> func) {
        return deserializeOneOf(value, kind, func);
    }

    private static List<?> deserializeList(Value value, Type type) {
        return deserializeOneOf(value, KindCase.LIST_VALUE, v -> {
            var result = new ArrayList<>(); // will hold nulls

            for (var element : v.getListValue().getValuesList()) {
                var elementData = deserializeInner(element, type);
                result.add(elementData);
            }

            return result;
        });
    }

    private static Map<String, ?> deserializeStruct(Value value, Type type) {
        return deserializeOneOf(value, KindCase.STRUCT_VALUE, v -> {
            var result = new HashMap<String, Object>();

            for (var entry : v.getStructValue().getFieldsMap().entrySet()) {
                var key = entry.getKey();
                var element = entry.getValue();

                // Unilaterally skip properties considered internal by the Pulumi engine.
                // These don't actually contribute to the exposed shape of the object, do
                // not need to be passed back to the engine, and often will not match the
                // expected type we are deserializing into.
                if (key.startsWith("__")) {
                    continue;
                }

                var elementData = deserializeInner(element, type);
                if (elementData == null) {
                    continue; // skip null early, because most collections cannot handle null values
                }
                result.put(key, elementData);
            }

            return ImmutableMap.copyOf(result);
        });
    }

    private static <T> T deserializeOneOf(Value value, Value.KindCase kind, Function<Value, T> func) {
        return deserializeCore(value, v -> {
            if (v.getKindCase() != kind) {
                throw new UnsupportedOperationException(String.format("Trying to deserialize '%s' as a '%s'", v.getKindCase(), kind));
            }

            return func.apply(v);
        });
    }

    private static Value unwrapSecret(Value value) {
        var sig = checkSpecialStruct(value);
        if (sig.isPresent() && Constants.SpecialSecretSig.equals(sig.get())) {
            var secretValue = Structs.tryGetValue(value.getStructValue(), Constants.SecretValueName)
                    .orElseThrow(() -> new UnsupportedOperationException("Secrets must have a field called 'value'"));

            return unwrapSecret(secretValue);
        }

        return value;
    }

    /**
     * @return signature of the special Struct or empty if not a special Struct
     */
    private static Optional<String> checkSpecialStruct(Value value) {
        if (value.getKindCase() == KindCase.STRUCT_VALUE) {
            for (Map.Entry<String, Value> entry : value.getStructValue().getFieldsMap().entrySet()) {
                if (entry.getKey().equals(Constants.SpecialSigKey)) {
                    if (entry.getValue().getKindCase() == KindCase.STRING_VALUE) {
                        String stringValue = entry.getValue().getStringValue();
                        return Optional.of(stringValue);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<AssetOrArchive> tryDeserializeAssetOrArchive(Value value) {
        var sig = checkSpecialStruct(value);
        if (sig.isPresent()) {
            if (Constants.SpecialAssetSig.equals(sig.get())) {
                return Optional.of(deserializeAsset(value));
            }
            if (Constants.SpecialArchiveSig.equals(sig.get())) {
                return Optional.of(deserializeArchive(value));
            }
        }

        return Optional.empty();
    }

    private static Archive deserializeArchive(Value value) {
        if (value.getKindCase() != KindCase.STRUCT_VALUE) {
            throw new IllegalArgumentException("Expected Value kind of Struct, got: " + value.getKindCase());
        }

        var path = Structs.tryGetStringValue(value.getStructValue(), Constants.AssetOrArchivePathName);
        if (path.isPresent()) {
            return new FileArchive(path.get());
        }

        var uri = Structs.tryGetStringValue(value.getStructValue(), Constants.AssetOrArchiveUriName);
        if (uri.isPresent()) {
            return new RemoteArchive(uri.get());
        }

        var assets = Structs.tryGetStructValue(value.getStructValue(), Constants.ArchiveAssetsName);
        if (assets.isPresent()) {
            final Function<Value, AssetOrArchive> assetArchiveOrThrow = v ->
                    tryDeserializeAssetOrArchive(v)
                            .orElseThrow(() -> new UnsupportedOperationException("AssetArchive contained an element that wasn't itself an Asset or Archive."));
            return new AssetArchive(
                    assets.get().getFieldsMap().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    assetArchiveOrThrow.compose(Map.Entry::getValue)
                            ))
            );
        }

        throw new UnsupportedOperationException("Value was marked as Archive, but did not conform to required shape.");
    }

    private static Asset deserializeAsset(Value value) {
        if (value.getKindCase() != KindCase.STRUCT_VALUE) {
            throw new IllegalArgumentException("Expected Value kind of Struct, got: " + value.getKindCase());
        }

        var path = Structs.tryGetStringValue(value.getStructValue(), Constants.AssetOrArchivePathName);
        if (path.isPresent()) {
            return new FileAsset(path.get());
        }

        var uri = Structs.tryGetStringValue(value.getStructValue(), Constants.AssetOrArchiveUriName);
        if (uri.isPresent()) {
            return new RemoteAsset(uri.get());
        }

        var text = Structs.tryGetStringValue(value.getStructValue(), Constants.AssetTextName);
        if (text.isPresent()) {
            return new StringAsset(text.get());
        }

        throw new UnsupportedOperationException("Value was marked as Asset, but did not conform to required shape.");
    }

    private static <T extends PolicyResource> Optional<T> tryDeserializeResource(Value value) {
        var id = Deserializer.tryDecodingResourceIdentity(value);
        if (id == null) {
            return Optional.empty();
        }

        var resourceClass = PolicyResourcePackages.resolveType(id.type, id.version);
        if (resourceClass == null) {
            throw new UnsupportedOperationException("Value was marked as a Resource, but did not map to any known resource type.");
        }

        //noinspection unchecked
        T resource = (T) deserialize(value.getStructValue(), resourceClass);

        return Optional.of(resource);
    }
}

