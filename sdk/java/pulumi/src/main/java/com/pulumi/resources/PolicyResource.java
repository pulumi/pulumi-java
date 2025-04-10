package com.pulumi.resources;

import com.google.common.collect.ImmutableList;
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
import com.pulumi.core.UndeferrableValue;
import com.pulumi.core.internal.Constants;
import com.pulumi.serialization.internal.Deserializer;
import com.pulumi.serialization.internal.PolicyResourcePackages;
import com.pulumi.serialization.internal.Reflection;
import com.pulumi.serialization.internal.Structs;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
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

    public static <T extends PolicyResource> T deserialize(Struct args, Class<T> type, boolean asInput) {
        try {
            T result = type.getDeclaredConstructor().newInstance();
            var fields = com.pulumi.core.internal.Reflection.allFields(type);

            Function<String, Field> getField = (key) -> {
                for (var field : fields) {
                    if (field.getName().equals(key)) {
                        field.setAccessible(true);
                        return field;
                    }
                }

                return null;
            };

            for (var entry : args.getFieldsMap().entrySet()) {
                var field = getField.apply(entry.getKey());
                if (field == null) {
                    // Ignore missing fields
                    continue;
//                    throw new NoSuchFieldException(String.format("%s: %s", type.getName(), entry.getKey()));
                }

                var fieldType = field.getGenericType();

                if (Reflection.sameType(fieldType, UndeferrableValue.class)) {
                    fieldType = Reflection.getTypeArgument(type, 0);
                }

                var valueData = deserializeInner(entry.getValue(), fieldType, asInput);
                field.set(result, valueData);
            }

            return result;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static @Nonnull UndeferrableValue<?> deserializeInner(Value value, Type type, boolean asInput) {
        requireNonNull(value, "Expected value to be non-null");
        value = unwrapSecret(value);

        var kindCase = value.getKindCase();
        if (kindCase == KindCase.STRING_VALUE) {
            var stringValue = value.getStringValue();

            if (Constants.UnknownValue.equals(stringValue)) {
                return new UndeferrableValue<>();
            }

            if (type != String.class) {
                try {
                    var valueBuilder = Value.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(stringValue, valueBuilder);
                    value = valueBuilder.build();
                    kindCase = value.getKindCase();
                } catch (Exception ignored) {
                    return new UndeferrableValue<>();
                }
            }
        }

        var assetOrArchive = tryDeserializeAssetOrArchive(value);
        if (assetOrArchive.isPresent()) {
            return new UndeferrableValue<>(assetOrArchive.get());
        }

        var resource = tryDeserializeResource(value, asInput);
        if (resource.isPresent()) {
            return new UndeferrableValue<>(resource.get());
        }

        switch (kindCase) {
            case NUMBER_VALUE:
                return new UndeferrableValue<>(value.getNumberValue());

            case STRING_VALUE:
                return new UndeferrableValue<>(value.getStringValue());

            case BOOL_VALUE:
                return new UndeferrableValue<>(value.getBoolValue());

            case STRUCT_VALUE:
                var structValue = new HashMap<String, Object>();
                var structElementType = Reflection.getTypeArgument(type, 1);

                for (var entry : value.getStructValue().getFieldsMap().entrySet()) {
                    var key = entry.getKey();
                    var element = entry.getValue();

                    // Unilaterally skip properties considered internal by the Pulumi engine.
                    // These don't actually contribute to the exposed shape of the object, do
                    // not need to be passed back to the engine, and often will not match the
                    // expected type we are deserializing into.
                    if (key.startsWith("__")) {
                        continue;
                    }

                    var elementData = deserializeInner(element, structElementType, asInput);
                    structValue.put(key, elementData);
                }

                return new UndeferrableValue<>(ImmutableMap.copyOf(structValue));

            case LIST_VALUE:
                var listValue = new ArrayList<>(); // will hold nulls
                var listElementType = Reflection.getTypeArgument(type, 0);

                for (var element : value.getListValue().getValuesList()) {
                    var elementData = deserializeInner(element, listElementType, asInput);
                    if (elementData.isPresent()) {
                        // If any value of the list are unknown, the whole list should be unknown.
                        return new UndeferrableValue<>();
                    }

                    listValue.add(elementData.getValue(null));
                }

                return new UndeferrableValue<>(ImmutableList.copyOf(listValue));

            case NULL_VALUE:
                return new UndeferrableValue<>(null);

            case KIND_NOT_SET:
                throw new UnsupportedOperationException("Should never get 'None' type when deserializing protobuf");

            default:
                throw new UnsupportedOperationException("Unknown type when deserializing protobuf: " + kindCase);
        }
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

    private static <T extends PolicyResource> Optional<T> tryDeserializeResource(Value value, boolean asInput) {
        var id = Deserializer.tryDecodingResourceIdentity(value);
        if (id == null) {
            return Optional.empty();
        }

        if (asInput) {
            var resourceClass = PolicyResourcePackages.resolveInputType(id.type, id.version);
            if (resourceClass != null) {
                //noinspection unchecked
                T resource = (T) deserialize(value.getStructValue(), resourceClass, asInput);

                return Optional.of(resource);
            }
        } else {
            var resourceClass = PolicyResourcePackages.resolveOutputType(id.type, id.version);
            if (resourceClass != null) {
                //noinspection unchecked
                T resource = (T) deserialize(value.getStructValue(), resourceClass, asInput);

                return Optional.of(resource);
            }
        }

        throw new UnsupportedOperationException("Value was marked as a Resource, but did not map to any known resource type.");
    }
}