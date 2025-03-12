package com.pulumi.serialization.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Value;
import com.pulumi.Log;
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
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.Urn;
import com.pulumi.resources.DependencyResource;
import com.pulumi.resources.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.protobuf.Value.KindCase.BOOL_VALUE;
import static com.google.protobuf.Value.KindCase.LIST_VALUE;
import static com.google.protobuf.Value.KindCase.NUMBER_VALUE;
import static com.google.protobuf.Value.KindCase.STRING_VALUE;
import static com.google.protobuf.Value.KindCase.STRUCT_VALUE;
import static com.pulumi.serialization.internal.Structs.tryGetStringValue;
import static com.pulumi.serialization.internal.Structs.tryGetStructValue;
import static com.pulumi.serialization.internal.Structs.tryGetValue;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * @see Serializer
 */
public class Deserializer {
    public static class ResourceIdentity {
        public final String type;
        public final String version;
        public final String urn;

        public ResourceIdentity(String type, String version, String urn) {
            this.type = type;
            this.version = version;
            this.urn = urn;
        }
    }

    private final Log log;
    private final ResourcePackages resourcePackages;

    public Deserializer(Log log) {
        this.log = requireNonNull(log);
        this.resourcePackages = new ResourcePackages(log);
    }

    public OutputData<Object> deserialize(Value value) {
        requireNonNull(value, "Expected value to be non-null");

        return deserializeCore(value, v -> {
            switch (v.getKindCase()) {
                case NUMBER_VALUE:
                    //noinspection RedundantCast
                    return deserializeDouble(v).apply(a -> (Object) a);
                case STRING_VALUE:
                    //noinspection RedundantCast
                    return deserializeString(v).apply(a -> (Object) a);
                case BOOL_VALUE:
                    //noinspection RedundantCast
                    return deserializeBoolean(v).apply(a -> (Object) a);
                case STRUCT_VALUE:
                    //noinspection RedundantCast
                    return deserializeStruct(v).apply(a -> (Object) a);
                case LIST_VALUE:
                    //noinspection RedundantCast
                    return deserializeList(v).apply(a -> (Object) a);
                case NULL_VALUE:
                    //noinspection RedundantCast
                    return deserializeEmpty(v).apply(a -> (Object) a);
                case KIND_NOT_SET:
                    throw new UnsupportedOperationException("Should never get 'None' type when deserializing protobuf");
                default:
                    throw new UnsupportedOperationException("Unknown type when deserializing protobuf: " + v.getKindCase());
            }
        });
    }

    private <T> OutputData<T> deserializeCore(Value maybeSecret, Function<Value, OutputData<T>> func) {
        var unwrapped = unwrapSecret(maybeSecret);
        var isSecret = unwrapped.isSecret;
        var value = unwrapped.value;

        if (value.getKindCase() == STRING_VALUE && Constants.UnknownValue.equals(value.getStringValue())) {
            // always deserialize unknown as the null value.
            return deserializeUnknown(isSecret);
        }

        var assetOrArchive = tryDeserializeAssetOrArchive(value);
        if (assetOrArchive.isPresent()) {
            //noinspection unchecked
            return OutputData.ofNullable(ImmutableSet.of(), (T) assetOrArchive.get(), true, isSecret);
        }

        var resource = tryDeserializeResource(value);
        if (resource.isPresent()) {
            //noinspection unchecked
            return OutputData.ofNullable(ImmutableSet.of(), (T) resource.get(), true, isSecret);
        }

        var innerData = func.apply(value);
        return OutputData.ofNullable(
                innerData.getResources(),
                innerData.getValueNullable(),
                innerData.isKnown(),
                isSecret || innerData.isSecret()
        );
    }

    public <T> OutputData<T> deserializeUnknown(boolean isSecret) {
        return OutputData.ofNullable(ImmutableSet.of(), null, false, isSecret);
    }

    private OutputData<Void> deserializeEmpty(@SuppressWarnings("unused") Value unused) {
        return OutputData.ofNullable(null);
    }

    private OutputData<Boolean> deserializeBoolean(Value value) {
        return deserializePrimitive(value, BOOL_VALUE, Value::getBoolValue);
    }

    private OutputData<String> deserializeString(Value value) {
        return deserializePrimitive(value, STRING_VALUE, Value::getStringValue);
    }

    private OutputData<Double> deserializeDouble(Value value) {
        return deserializePrimitive(value, NUMBER_VALUE, Value::getNumberValue);
    }

    private <T> OutputData<T> deserializePrimitive(Value value, Value.KindCase kind, Function<Value, T> func) {
        return deserializeOneOf(value, kind, v -> OutputData.of(ImmutableSet.of(), func.apply(v)));
    }

    private OutputData<List<?>> deserializeList(Value value) {
        return deserializeOneOf(value, LIST_VALUE, v -> {
            var resources = new HashSet<Resource>();
            var result = new ArrayList<>(); // will hold nulls
            var isKnown = true;
            var isSecret = false;

            for (var element : v.getListValue().getValuesList()) {
                var elementData = deserialize(element);
                resources.addAll(elementData.getResources());
                result.add(elementData.getValueNullable());
                isKnown = isKnown && elementData.isKnown();
                isSecret = isSecret || elementData.isSecret();
            }

            if (isKnown) {
                return OutputData.ofNullable(
                        ImmutableSet.copyOf(resources),
                        unmodifiableList(result), // Can't be immutable because this contains null
                        true,
                        isSecret
                );
            } else {
                return OutputData.ofNullable(
                        ImmutableSet.copyOf(resources),
                        null,
                        false,
                        isSecret
                );
            }
        });
    }

    private OutputData<Map<String, ?>> deserializeStruct(Value value) {
        return deserializeOneOf(value, STRUCT_VALUE, v -> {
            var resources = new HashSet<Resource>();
            var result = new HashMap<String, Object>();
            var isKnown = true;
            var isSecret = false;

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

                var elementData = deserialize(element);
                // Unknown values will be unwrapped at this point to a 'null'.
                // To keep track of unknown values we aggregate here, before we skip the 'null' value.
                // An example of this situation would be a map with only unknown elements.
                isKnown = isKnown && elementData.isKnown();

                var valueOrNull = elementData.getValueNullable();
                if (valueOrNull == null) {
                    continue; // skip null early, because most collections cannot handle null values
                }
                result.put(key, valueOrNull);

                resources.addAll(elementData.getResources());
                isSecret = isSecret || elementData.isSecret();
            }

            return OutputData.ofNullable(
                    ImmutableSet.copyOf(resources),
                    isKnown ? ImmutableMap.copyOf(result) : null,
                    isKnown,
                    isSecret
            );
        });
    }


    private <T> OutputData<T> deserializeOneOf(Value value, Value.KindCase kind, Function<Value, OutputData<T>> func) {
        return deserializeCore(value, v -> {
            if (v.getKindCase() == kind) {
                return func.apply(v);
            } else {
                throw new UnsupportedOperationException(
                        String.format("Trying to deserialize '%s' as a '%s'", v.getKindCase(), kind));
            }
        });
    }

    private static UnwrappedSecret unwrapSecret(Value value) {
        return innerUnwrapSecret(value, false);
    }

    private static UnwrappedSecret innerUnwrapSecret(Value value, boolean isSecret) {
        var sig = checkSpecialStruct(value);
        if (sig.isPresent() && Constants.SpecialSecretSig.equals(sig.get())) {
            var secretValue = tryGetValue(value.getStructValue(), Constants.SecretValueName)
                    .orElseThrow(() -> new UnsupportedOperationException("Secrets must have a field called 'value'"));

            return innerUnwrapSecret(secretValue, true);
        }

        return UnwrappedSecret.of(value, isSecret);
    }

    private static final class UnwrappedSecret {
        public final Value value;
        public final boolean isSecret;

        private UnwrappedSecret(Value value, boolean isSecret) {
            this.value = requireNonNull(value, "Expected value to be non-null");
            this.isSecret = isSecret;
        }

        private static UnwrappedSecret of(Value value, boolean isSecret) {
            return new UnwrappedSecret(value, isSecret);
        }
    }

    /**
     * @return signature of the special Struct or empty if not a special Struct
     */
    private static Optional<String> checkSpecialStruct(Value value) {
        return Stream.of(value)
                .filter(v -> v.getKindCase() == STRUCT_VALUE)
                .flatMap(v -> v.getStructValue().getFieldsMap().entrySet().stream())
                .filter(entry -> entry.getKey().equals(Constants.SpecialSigKey))
                .filter(entry -> entry.getValue().getKindCase() == STRING_VALUE)
                .map(entry -> entry.getValue().getStringValue())
                .findFirst();
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
        if (value.getKindCase() != STRUCT_VALUE) {
            throw new IllegalArgumentException("Expected Value kind of Struct, got: " + value.getKindCase());
        }

        var path = tryGetStringValue(value.getStructValue(), Constants.AssetOrArchivePathName);
        if (path.isPresent()) {
            return new FileArchive(path.get());
        }

        var uri = tryGetStringValue(value.getStructValue(), Constants.AssetOrArchiveUriName);
        if (uri.isPresent()) {
            return new RemoteArchive(uri.get());
        }

        var assets = tryGetStructValue(value.getStructValue(), Constants.ArchiveAssetsName);
        if (assets.isPresent()) {
            final Function<Value, AssetOrArchive> assetArchiveOrThrow = v ->
                    tryDeserializeAssetOrArchive(v)
                            .orElseThrow(() -> new UnsupportedOperationException(
                                    "AssetArchive contained an element that wasn't itself an Asset or Archive."));
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
        if (value.getKindCase() != STRUCT_VALUE) {
            throw new IllegalArgumentException("Expected Value kind of Struct, got: " + value.getKindCase());
        }

        var path = tryGetStringValue(value.getStructValue(), Constants.AssetOrArchivePathName);
        if (path.isPresent()) {
            return new FileAsset(path.get());
        }

        var uri = tryGetStringValue(value.getStructValue(), Constants.AssetOrArchiveUriName);
        if (uri.isPresent()) {
            return new RemoteAsset(uri.get());
        }

        var text = tryGetStringValue(value.getStructValue(), Constants.AssetTextName);
        if (text.isPresent()) {
            return new StringAsset(text.get());
        }

        throw new UnsupportedOperationException("Value was marked as Asset, but did not conform to required shape.");
    }

    public static ResourceIdentity tryDecodingResourceIdentity(Value value) {
        var sig = checkSpecialStruct(value);
        if (sig.isEmpty() || !Constants.SpecialResourceSig.equals(sig.get())) {
            return null;
        }

        var struct = value.getStructValue();

        var urn = tryGetStringValue(struct, Constants.ResourceUrnName)
                .orElseThrow(() -> new UnsupportedOperationException("Value was marked as a Resource, but did not conform to required shape."));

        var version = tryGetStringValue(struct, Constants.ResourceVersionName)
                .orElse("");

        var urnParsed = Urn.parse(urn);
        var type = urnParsed.qualifiedType.type.asString();

        return new ResourceIdentity(type, version, urn);

    }

    private Optional<Resource> tryDeserializeResource(Value value) {
        var id = tryDecodingResourceIdentity(value);
        if (id == null) {
            return Optional.empty();
        }

        var resource = this.resourcePackages.tryConstruct(id.type, id.version, id.urn);
        if (resource.isPresent()) {
            return resource;
        }

        return Optional.of(new DependencyResource(id.urn));
    }
}
