package io.pulumi.serialization.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Value;
import io.pulumi.core.Archive;
import io.pulumi.core.Asset;
import io.pulumi.core.AssetOrArchive;
import io.pulumi.core.Tuples;
import io.pulumi.core.Tuples.Tuple2;
import io.pulumi.core.internal.Constants;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.resources.DependencyResource;
import io.pulumi.resources.Resource;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.protobuf.Value.KindCase.*;
import static io.pulumi.serialization.internal.Structs.*;

/**
 * Also @see {@link Serializer}
 */
public class Deserializer {

    public InputOutputData<Object> deserialize(Value value) {
        Objects.requireNonNull(value);
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

    private <T> InputOutputData<T> deserializeCore(Value value, Function<Value, InputOutputData<T>> func) {
        var secret = unwrapSecret(value);
        boolean isSecret = secret.t2;
        value = secret.t1;

        if (value.getKindCase() == STRING_VALUE && Constants.UnknownValue.equals(value.getStringValue())) {
            // always deserialize unknown as the null value.
            return deserializeUnknown(isSecret);
        }

        var assetOrArchive = tryDeserializeAssetOrArchive(value);
        if (assetOrArchive.isPresent()) {
            //noinspection unchecked
            return InputOutputData.ofNullable(ImmutableSet.of(), (T) assetOrArchive.get(), true, isSecret);
        }

        var resource = tryDeserializeResource(value);
        if (resource.isPresent()) {
            //noinspection unchecked
            return InputOutputData.ofNullable(ImmutableSet.of(), (T) resource.get(), true, isSecret);
        }

        var innerData = func.apply(value);
        return InputOutputData.ofNullable(
                innerData.getResources(),
                innerData.getValueOptional().orElse(null),
                innerData.isKnown(),
                isSecret || innerData.isSecret()
        );
    }

    public <T> InputOutputData<T> deserializeUnknown(boolean isSecret) {
        return InputOutputData.ofNullable(ImmutableSet.of(), null, false, isSecret);
    }

    private InputOutputData<Void> deserializeEmpty(@SuppressWarnings("unused") Value unused) {
        return InputOutputData.empty();
    }

    private InputOutputData<Boolean> deserializeBoolean(Value value) {
        return deserializePrimitive(value, BOOL_VALUE, Value::getBoolValue);
    }

    private InputOutputData<String> deserializeString(Value value) {
        return deserializePrimitive(value, STRING_VALUE, Value::getStringValue);
    }

    private InputOutputData<Double> deserializeDouble(Value value) {
        return deserializePrimitive(value, NUMBER_VALUE, Value::getNumberValue);
    }

    private <T> InputOutputData<T> deserializePrimitive(Value value, Value.KindCase kind, Function<Value, T> func) {
        return deserializeOneOf(value, kind, v -> InputOutputData.of(ImmutableSet.of(), func.apply(v)));
    }

    private InputOutputData<List<?>> deserializeList(Value value) {
        return deserializeOneOf(value, LIST_VALUE, v -> {
            var resources = new HashSet<Resource>();
            var result = new LinkedList<Optional<?>>();
            var isKnown = true;
            var isSecret = false;

            for (var element : v.getListValue().getValuesList()) {
                var elementData = deserialize(element);
                resources.addAll(elementData.getResources());
                result.add(elementData.getValueOptional());
                isKnown = isKnown && elementData.isKnown();
                isSecret = isSecret || elementData.isSecret();
            }

            return InputOutputData.ofNullable(
                    ImmutableSet.copyOf(resources),
                    ImmutableList.copyOf(result),
                    isKnown,
                    isSecret
            );
        });
    }

    private InputOutputData<Map<String, Optional<?>>> deserializeStruct(Value value) {
        return deserializeOneOf(value, STRUCT_VALUE, v -> {
            var resources = new HashSet<Resource>();
            var result = new HashMap<String, Optional<?>>();
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
                resources.addAll(elementData.getResources());
                result.put(key, elementData.getValueOptional());
                isKnown = isKnown && elementData.isKnown();
                isSecret = isSecret || elementData.isSecret();
            }

            return InputOutputData.ofNullable(
                    ImmutableSet.copyOf(resources),
                    ImmutableMap.copyOf(result),
                    isKnown, isSecret
            );
        });
    }


    private <T> InputOutputData<T> deserializeOneOf(Value value, Value.KindCase kind, Function<Value, InputOutputData<T>> func) {
        return deserializeCore(value, v -> {
            if (v.getKindCase() == kind) {
                return func.apply(v);
            } else {
                throw new UnsupportedOperationException(
                        String.format("Trying to deserialize '%s' as a '%s'", v.getKindCase(), kind));
            }
        });
    }

    private static Tuple2<Value, Boolean> unwrapSecret(Value value) {
        return innerUnwrapSecret(value, false);
    }

    private static Tuple2<Value, Boolean> innerUnwrapSecret(Value value, boolean isSecret) {
        var sig = isSpecialStruct(value);
        if (sig.isPresent() && Constants.SpecialSecretSig.equals(sig.get())) {
            var secretValue = tryGetValue(value.getStructValue(), Constants.SecretValueName)
                    .orElseThrow(() -> {
                        throw new UnsupportedOperationException("Secrets must have a field called 'value'");
                    });

            return innerUnwrapSecret(secretValue, true);
        }

        return Tuples.of(value, isSecret);
    }

    /**
     * @return signature of the special Struct or empty if not a special Struct
     */
    private static Optional<String> isSpecialStruct(Value value) {
        return Stream.of(value)
                .filter(v -> v.getKindCase() == STRUCT_VALUE)
                .flatMap(v -> v.getStructValue().getFieldsMap().entrySet().stream())
                .filter(entry -> entry.getKey().equals(Constants.SpecialSigKey))
                .filter(entry -> entry.getValue().getKindCase() == STRING_VALUE)
                .map(entry -> entry.getValue().getStringValue())
                .findFirst();
    }

    private static Optional<AssetOrArchive> tryDeserializeAssetOrArchive(Value value) {
        var sig = isSpecialStruct(value);
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
            return new Archive.FileArchive(path.get());
        }

        var uri = tryGetStringValue(value.getStructValue(), Constants.AssetOrArchiveUriName);
        if (uri.isPresent()) {
            return new Archive.RemoteArchive(uri.get());
        }

        var assets = tryGetStructValue(value.getStructValue(), Constants.ArchiveAssetsName);
        if (assets.isPresent()) {
            final Function<Value, AssetOrArchive> assetArchiveOrThrow = v ->
                    tryDeserializeAssetOrArchive(v)
                            .orElseThrow(() -> {
                                throw new UnsupportedOperationException(
                                        "AssetArchive contained an element that wasn't itself an Asset or Archive.");
                            });
            return new Archive.AssetArchive(
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
            return new Asset.FileAsset(path.get());
        }

        var uri = tryGetStringValue(value.getStructValue(), Constants.AssetOrArchiveUriName);
        if (uri.isPresent()) {
            return new Asset.RemoteAsset(uri.get());
        }

        var text = tryGetStringValue(value.getStructValue(), Constants.AssetTextName);
        if (text.isPresent()) {
            return new Asset.StringAsset(text.get());
        }

        throw new UnsupportedOperationException("Value was marked as Asset, but did not conform to required shape.");
    }

    private static Optional<Resource> tryDeserializeResource(Value value) {
        var sig = isSpecialStruct(value);
        if (sig.isEmpty() || !Constants.SpecialResourceSig.equals(sig.get())) {
            return Optional.empty();
        }

        var struct = value.getStructValue();

        var urn = tryGetStringValue(struct, Constants.ResourceUrnName)
                .orElseThrow(() -> {
                    throw new UnsupportedOperationException(
                            "Value was marked as a Resource, but did not conform to required shape.");
                });

        var version = tryGetStringValue(struct, Constants.ResourceVersionName)
                .orElse("");

        // TODO: a good candidate for some new URN methods with good unit tests
        var urnParts = urn.split("::");
        var qualifiedType = urnParts[2];
        var qualifiedTypeParts = qualifiedType.split("\\$");
        var type = qualifiedTypeParts[qualifiedTypeParts.length - 1];

        var resource = ResourcePackages.tryConstruct(type, version, urn);
        if (resource.isPresent()) {
            return resource;
        }

        return Optional.of(new DependencyResource(urn));
    }
}
