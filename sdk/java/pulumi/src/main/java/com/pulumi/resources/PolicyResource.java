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
import com.pulumi.core.annotations.PolicyResourceProperty;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Base type for all resource policy classes.
 */
@ParametersAreNonnullByDefault
public abstract class PolicyResource {
    static class ValueAndFlag {
        final Field value;
        final Field flag;

        public ValueAndFlag(Field value, Field flag) {
            this.value = value;
            this.flag = flag;
        }
    }

    static class DeserializedValue {
        static final DeserializedValue empty = new DeserializedValue();

        final Object value;
        final boolean unknown;

        DeserializedValue() {
            this.value = null;
            this.unknown = true;
        }

        DeserializedValue(Object value) {
            this.value = value;
            this.unknown = false;
        }
    }

    private static final ConcurrentHashMap<Type, Map<String, ValueAndFlag>> s_lookupFields = new ConcurrentHashMap<>();

    private static Map<String, ValueAndFlag> getProperties(Type type) {
        return s_lookupFields.computeIfAbsent(type, k -> {
            var clz = Reflection.getRawType(type);
            var fields = com.pulumi.core.internal.Reflection.allFields(clz);

            var lookup = new HashMap<String, Field>();
            for (var field : fields) {
                lookup.put(field.getName(), field);
            }

            var properties = new HashMap<String, ValueAndFlag>();

            // Initialize all the fields
            for (var field : fields) {
                var anno = field.getAnnotation(PolicyResourceProperty.class);
                if (anno != null) {
                    Field flagField = lookup.get(anno.flag());

                    field.setAccessible(true);
                    flagField.setAccessible(true);

                    var prop = new ValueAndFlag(field, flagField);
                    properties.put(anno.name(), prop);
                }
            }

            return properties;
        });
    }

    //--//

    private String urn;

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public String getUrn() {
        return this.urn;
    }

    public static <T extends PolicyResource> T deserialize(Struct args, Class<T> type, boolean asInput) {
        return type.cast(deserializeRaw(args, type, asInput));
    }

    public static Object deserializeRaw(Struct args, Type type, boolean asInput) {
        try {
            var clz = Reflection.getRawType(type);
            var result = clz.getDeclaredConstructor().newInstance();
            var lookup = getProperties(type);

            for (var entry : args.getFieldsMap().entrySet()) {
                var pair = lookup.get(entry.getKey());
                if (pair == null) {
                    // Ignore missing fields
                    continue;
                }

                var fieldType = pair.value.getGenericType();

                var valueData = deserializeInner(entry.getValue(), fieldType, asInput);
                pair.value.set(result, valueData.value);
                pair.flag.set(result, valueData.unknown);
            }

            return result;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static @Nonnull DeserializedValue deserializeInner(Value value, Type type, boolean asInput) {
        requireNonNull(value, "Expected value to be non-null");
        value = unwrapSecret(value);

        var kindCase = value.getKindCase();
        if (kindCase == KindCase.STRING_VALUE) {
            var stringValue = value.getStringValue();

            if (Constants.UnknownValue.equals(stringValue)) {
                return DeserializedValue.empty;
            }

            if (type != String.class) {
                try {
                    var valueBuilder = Value.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(stringValue, valueBuilder);
                    value = valueBuilder.build();
                    kindCase = value.getKindCase();
                } catch (Exception ignored) {
                    return DeserializedValue.empty;
                }
            }
        }

        var assetOrArchive = tryDeserializeAssetOrArchive(value);
        if (assetOrArchive.isPresent()) {
            return new DeserializedValue(assetOrArchive.get());
        }

        var resource = tryDeserializeResource(value, asInput);
        if (resource.isPresent()) {
            return new DeserializedValue(resource.get());
        }

        switch (kindCase) {
            case NUMBER_VALUE:
                return new DeserializedValue(value.getNumberValue());

            case STRING_VALUE:
                return new DeserializedValue(value.getStringValue());

            case BOOL_VALUE:
                return new DeserializedValue(value.getBoolValue());

            case STRUCT_VALUE:
                var structValue = value.getStructValue();
                if (Reflection.sameType(type, Map.class)) {
                    var result = new HashMap<String, Object>();
                    var structElementType = Reflection.getTypeArgument(type, 1);

                    for (var entry : structValue.getFieldsMap().entrySet()) {
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
                        if (!elementData.unknown) {
                            result.put(key, elementData.value);
                        }
                    }

                    return new DeserializedValue(ImmutableMap.copyOf(result));
                } else {
                    var obj = deserializeRaw(structValue, Reflection.getRawType(type), asInput);
                    return new DeserializedValue(obj);
                }

            case LIST_VALUE:
                var listValue = new ArrayList<>(); // will hold nulls
                var listElementType = Reflection.getTypeArgument(type, 0);

                for (var element : value.getListValue().getValuesList()) {
                    var elementData = deserializeInner(element, listElementType, asInput);
                    if (elementData.unknown) {
                        // If any value of the list are unknown, the whole list should be unknown.
                        return DeserializedValue.empty;
                    }

                    listValue.add(elementData.value);
                }

                return new DeserializedValue(ImmutableList.copyOf(listValue));

            case NULL_VALUE:
                return new DeserializedValue(null);

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