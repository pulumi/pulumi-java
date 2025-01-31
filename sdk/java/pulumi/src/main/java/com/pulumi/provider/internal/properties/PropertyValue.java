package com.pulumi.provider.internal.properties;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.NullValue;
import com.google.protobuf.ListValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.pulumi.asset.*;
import com.pulumi.core.internal.Constants;

public class PropertyValue {
    public enum ValueType {
        NULL(null),
        BOOL(Boolean.class),
        NUMBER(Double.class),
        STRING(String.class),
        ARRAY(List.class),
        OBJECT(Map.class),
        ASSET(Asset.class),
        ARCHIVE(Archive.class),
        SECRET(PropertyValue.class),
        RESOURCE(ResourceReference.class),
        OUTPUT(OutputReference.class),
        COMPUTED(null);

        private final Class<?> valueClass;

        ValueType(Class<?> valueClass) {
            this.valueClass = valueClass;
        }
    }

    public static class ResourceReference {
        private final String URN;
        private final PropertyValue id;
        private final String packageVersion;

        public ResourceReference(String urn, PropertyValue id, String version) {
            this.URN = urn;
            this.id = id;
            this.packageVersion = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ResourceReference)) return false;
            ResourceReference that = (ResourceReference) o;
            return Objects.equals(URN, that.URN) &&
                   Objects.equals(id, that.id) &&
                   Objects.equals(packageVersion, that.packageVersion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(URN, id, packageVersion);
        }
    }

    public static class OutputReference {
        private final PropertyValue value;
        private final Set<String> dependencies;

        public OutputReference(PropertyValue value, Set<String> dependencies) {
            // Normalize Computed to null
            this.value = (value != null && value.isComputed()) ? null : value;
            this.dependencies = Collections.unmodifiableSet(new HashSet<>(dependencies));
        }

        public PropertyValue getValue() {
            return value;
        }

        public Set<String> getDependencies() {
            return dependencies;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OutputReference)) return false;
            OutputReference that = (OutputReference) o;
            return Objects.equals(value, that.value) &&
                   Objects.equals(dependencies, that.dependencies);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, dependencies);
        }

        @Override
        public String toString() {
            return String.format("Output(%s, %s)", value, dependencies);
        }
    }

    // Static constants
    public static final PropertyValue NULL = new PropertyValue(ValueType.NULL,  null);
    public static final PropertyValue COMPUTED = new PropertyValue(ValueType.COMPUTED, null);

    private final ValueType type;
    private final Object value;

    private PropertyValue(ValueType type, Object value) {
        if (value != null && type.valueClass != null && !type.valueClass.isInstance(value)) {
            throw new IllegalArgumentException(String.format(
                "Value of type %s cannot be used for PropertyValue of type %s",
                value.getClass().getName(),
                type
            ));
        }
        this.type = type;
        this.value = value;
    }

    // Factory methods
    public static PropertyValue of(boolean value) {
        return new PropertyValue(ValueType.BOOL, value);
    }

    public static PropertyValue of(double value) {
        return new PropertyValue(ValueType.NUMBER, value);
    }

    public static PropertyValue of(String value) {
        return new PropertyValue(ValueType.STRING, value);
    }

    public static PropertyValue of(List<PropertyValue> value) {
        return new PropertyValue(ValueType.ARRAY, Collections.unmodifiableList(new ArrayList<>(value)));
    }

    public static PropertyValue of(Map<String, PropertyValue> value) {
        return new PropertyValue(ValueType.OBJECT, Collections.unmodifiableMap(new HashMap<>(value)));
    }

    public static PropertyValue of(Asset value) {
        return new PropertyValue(ValueType.ASSET, value);
    }

    public static PropertyValue of(Archive value) {
        return new PropertyValue(ValueType.ARCHIVE, value);
    }

    public static PropertyValue ofSecret(PropertyValue value) {
        return new PropertyValue(ValueType.SECRET, value);
    }

    public static PropertyValue of(ResourceReference value) {
        return new PropertyValue(ValueType.RESOURCE, value);
    }

    public static PropertyValue of(OutputReference value) {
        return new PropertyValue(ValueType.OUTPUT, value);
    }

    // Type-safe getters
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    public ValueType getType() {
        return type;
    }

    public boolean isNull() {
        return type == ValueType.NULL;
    }

    public boolean isComputed() {
        return type == ValueType.COMPUTED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyValue)) return false;
        PropertyValue that = (PropertyValue) o;
        
        if (type != that.type) return false;
        
        // Handle special cases first
        if (type == ValueType.NULL || type == ValueType.COMPUTED) {
            return true;
        }
        
        // For collections, we need special handling
        if (type == ValueType.ARRAY) {
            List<PropertyValue> thisList = getValue();
            List<PropertyValue> thatList = that.getValue();
            return thisList.equals(thatList);
        }
        
        if (type == ValueType.OBJECT) {
            Map<String, PropertyValue> thisMap = getValue();
            Map<String, PropertyValue> thatMap = that.getValue();
            return thisMap.equals(thatMap);
        }
        
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        if (type == ValueType.NULL || type == ValueType.COMPUTED) {
            return Objects.hash(type);
        }
        
        if (type == ValueType.SECRET) {
            // Combine with TRUE for secrets
            return Objects.hash(Boolean.TRUE, value);
        }
        
        // Special handling for collections to ensure deep hash
        if (type == ValueType.ARRAY) {
            List<PropertyValue> list = getValue();
            return Objects.hash(type, list);
        }
        
        if (type == ValueType.OBJECT) {
            Map<String, PropertyValue> map = getValue();
            return Objects.hash(type, map);
        }
        
        // Default case for all other types
        return Objects.hash(type, value);
    }

    // Protobuf marshaling methods
    public static PropertyValue unmarshal(Value value) {
        switch (value.getKindCase()) {
            case NULL_VALUE:
                return PropertyValue.NULL;
            case BOOL_VALUE:
                return of(value.getBoolValue());
            case NUMBER_VALUE:
                return of(value.getNumberValue());
            case STRING_VALUE:
                if (Constants.UnknownValue.equals(value.getStringValue())) {
                    return PropertyValue.COMPUTED;
                }
                return of(value.getStringValue());
            case LIST_VALUE:
                List<PropertyValue> list = new ArrayList<>();
                for (Value v : value.getListValue().getValuesList()) {
                    list.add(unmarshal(v));
                }
                return of(Collections.unmodifiableList(list));
            case STRUCT_VALUE:
                return unmarshalStruct(value.getStructValue());
            default:
                throw new IllegalArgumentException("Unexpected grpc value type: " + value.getKindCase());
        }
    }

    private static PropertyValue unmarshalStruct(Struct struct) {
        // Handle special types (Asset, Archive, Secret, Resource, Output)
        Map<String, Value> fields = struct.getFieldsMap();
        if (fields.containsKey(Constants.SpecialSigKey)) {
            String sig = fields.get(Constants.SpecialSigKey).getStringValue();
            switch (sig) {
                case Constants.SpecialSecretSig:
                    return unmarshalSecret(fields);
                case Constants.SpecialAssetSig:
                    return unmarshalAsset(fields);
                case Constants.SpecialArchiveSig:
                    return unmarshalArchive(fields);
                case Constants.SpecialResourceSig:
                    return unmarshalResource(fields);
                case Constants.SpecialOutputSig:
                    return unmarshalOutput(fields);
                default:
                    throw new IllegalArgumentException("Unknown special signature: " + sig);
            }
        }

        // Regular object
        Map<String, PropertyValue> map = new HashMap<>();
        for (Map.Entry<String, Value> entry : fields.entrySet()) {
            map.put(entry.getKey(), unmarshal(entry.getValue()));
        }
        return of(Collections.unmodifiableMap(map));
    }

    private static PropertyValue unmarshalSecret(Map<String, Value> fields) {
        if (!fields.containsKey(Constants.SecretValueName)) {
            throw new IllegalArgumentException("Secrets must have a field called 'value'");
        }
        return ofSecret(unmarshal(fields.get(Constants.SecretValueName)));
    }

    private static PropertyValue unmarshalAsset(Map<String, Value> fields) {
        Asset asset;
        if (fields.containsKey(Constants.AssetOrArchivePathName)) {
            asset = new FileAsset(fields.get(Constants.AssetOrArchivePathName).getStringValue());
        } else if (fields.containsKey(Constants.AssetOrArchiveUriName)) {
            asset = new RemoteAsset(fields.get(Constants.AssetOrArchiveUriName).getStringValue());
        } else if (fields.containsKey(Constants.AssetTextName)) {
            asset = new StringAsset(fields.get(Constants.AssetTextName).getStringValue());
        } else {
            throw new IllegalArgumentException("Asset must have either 'path', 'uri', or 'text' field");
        }
        return of(asset);
    }

    private static PropertyValue unmarshalArchive(Map<String, Value> fields) {
        Archive archive;
        if (fields.containsKey(Constants.AssetOrArchivePathName)) {
            archive = new FileArchive(fields.get(Constants.AssetOrArchivePathName).getStringValue());
        } else if (fields.containsKey(Constants.AssetOrArchiveUriName)) {
            archive = new RemoteArchive(fields.get(Constants.AssetOrArchiveUriName).getStringValue());
        } else if (fields.containsKey(Constants.ArchiveAssetsName)) {
            Value assetsValue = fields.get(Constants.ArchiveAssetsName);
            if (assetsValue.getKindCase() != Value.KindCase.STRUCT_VALUE) {
                throw new IllegalArgumentException("Archive assets must be an object");
            }
            
            Map<String, AssetOrArchive> assets = new HashMap<>();
            for (Map.Entry<String, Value> entry : assetsValue.getStructValue().getFieldsMap().entrySet()) {
                PropertyValue innerValue = unmarshal(entry.getValue());
                if (innerValue.getType() == ValueType.ASSET) {
                    assets.put(entry.getKey(), innerValue.<Asset>getValue());
                } else if (innerValue.getType() == ValueType.ARCHIVE) {
                    assets.put(entry.getKey(), innerValue.<Archive>getValue());
                } else {
                    throw new IllegalArgumentException("AssetArchive can only contain Assets or Archives");
                }
            }
            archive = new AssetArchive(Collections.unmodifiableMap(assets));
        } else {
            throw new IllegalArgumentException("Archive must have either 'path', 'uri', or 'assets' field");
        }
        return of(archive);
    }

    private static PropertyValue unmarshalResource(Map<String, Value> fields) {
        if (!fields.containsKey(Constants.UrnPropertyName)) {
            throw new IllegalArgumentException("Resource must have a 'urn' field");
        }
        String urn = fields.get(Constants.UrnPropertyName).getStringValue();
        String version = "";
        if (fields.containsKey(Constants.ResourceVersionName)) {
            version = fields.get(Constants.ResourceVersionName).getStringValue();
        }
        PropertyValue id = null;
        if (fields.containsKey(Constants.IdPropertyName)) {
            id = unmarshal(fields.get(Constants.IdPropertyName));
        }
        return of(new ResourceReference(urn, id, version));
    }

    private static PropertyValue unmarshalOutput(Map<String, Value> fields) {
        PropertyValue value = null;
        if (fields.containsKey(Constants.SecretValueName)) {
            value = unmarshal(fields.get(Constants.SecretValueName));
        }

        Set<String> dependencies = new HashSet<>();
        if (fields.containsKey(Constants.DependenciesName)) {
            Value depsValue = fields.get(Constants.DependenciesName);
            if (depsValue.getKindCase() != Value.KindCase.LIST_VALUE) {
                throw new IllegalArgumentException("Output dependencies must be an array");
            }
            for (Value dep : depsValue.getListValue().getValuesList()) {
                if (dep.getKindCase() != Value.KindCase.STRING_VALUE) {
                    throw new IllegalArgumentException("Output dependency must be a string");
                }
                dependencies.add(dep.getStringValue());
            }
        }

        boolean isSecret = false;
        if (fields.containsKey(Constants.SecretName)) {
            Value secretValue = fields.get(Constants.SecretName);
            if (secretValue.getKindCase() != Value.KindCase.BOOL_VALUE) {
                throw new IllegalArgumentException("Output secret must be a boolean");
            }
            isSecret = secretValue.getBoolValue();
        }

        PropertyValue output = of(new OutputReference(value, dependencies));
        return isSecret ? ofSecret(output) : output;
    }

    // Marshal methods
    public Value marshal() {
        switch (type) {
            case NULL:
                return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
            case BOOL:
                return Value.newBuilder().setBoolValue(getValue()).build();
            case NUMBER:
                return Value.newBuilder().setNumberValue(getValue()).build();
            case STRING:
                return Value.newBuilder().setStringValue(getValue()).build();
            case ARRAY:
                ListValue.Builder listBuilder = ListValue.newBuilder();
                List<PropertyValue> list = getValue();
                for (PropertyValue item : list) {
                    listBuilder.addValues(item.marshal());
                }
                return Value.newBuilder().setListValue(listBuilder).build();
            case OBJECT:
                return marshalObject();
            case ASSET:
                return marshalAsset();
            case ARCHIVE:
                return marshalArchive();
            case SECRET:
                return marshalSecret();
            case RESOURCE:
                return marshalResource();
            case OUTPUT:
                return marshalOutput(getValue(), false);
            case COMPUTED:
                return Value.newBuilder().setStringValue(Constants.UnknownValue).build();
            default:
                throw new IllegalStateException("Unknown property value type: " + type);
        }
    }

    private Value marshalObject() {
        Struct.Builder structBuilder = Struct.newBuilder();
        Map<String, PropertyValue> map = getValue();
        for (Map.Entry<String, PropertyValue> entry : map.entrySet()) {
            structBuilder.putFields(entry.getKey(), entry.getValue().marshal());
        }
        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    private Value marshalAsset() {
        Asset asset = getValue();
        var internal = AssetOrArchive.AssetOrArchiveInternal.from(asset);
        Struct.Builder structBuilder = Struct.newBuilder();
        structBuilder.putFields(Constants.SpecialSigKey, 
            Value.newBuilder().setStringValue(internal.getSigKey()).build());
        structBuilder.putFields(internal.getPropName(),
            Value.newBuilder().setStringValue((String)internal.getValue()).build());
        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    private Value marshalArchive() {
        Archive archive = getValue();
        var internal = AssetOrArchive.AssetOrArchiveInternal.from(archive);
        Struct.Builder structBuilder = Struct.newBuilder();
        structBuilder.putFields(Constants.SpecialSigKey,
            Value.newBuilder().setStringValue(internal.getSigKey()).build());

        if (internal.getValue() instanceof String) {
            structBuilder.putFields(internal.getPropName(),
                Value.newBuilder().setStringValue((String)internal.getValue()).build());
        } else {
            @SuppressWarnings("unchecked")
            Map<String, AssetOrArchive> assets = (Map<String, AssetOrArchive>)internal.getValue();
            Struct.Builder assetsBuilder = Struct.newBuilder();
            for (Map.Entry<String, AssetOrArchive> entry : assets.entrySet()) {
                if (entry.getValue() instanceof Asset) {
                    assetsBuilder.putFields(entry.getKey(), PropertyValue.of((Asset)entry.getValue()).marshal());
                } else {
                    assetsBuilder.putFields(entry.getKey(), PropertyValue.of((Archive)entry.getValue()).marshal());
                }
            }
            structBuilder.putFields(internal.getPropName(),
                Value.newBuilder().setStructValue(assetsBuilder).build());
        }
        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    private Value marshalSecret() {
        PropertyValue secretValue = getValue();
        // Special case if our secret value is an output
        if (secretValue.getType() == ValueType.OUTPUT) {
            return marshalOutput(secretValue.<OutputReference>getValue(), true);
        }
        Struct.Builder structBuilder = Struct.newBuilder();
        structBuilder.putFields(Constants.SpecialSigKey,
            Value.newBuilder().setStringValue(Constants.SpecialSecretSig).build());
        structBuilder.putFields(Constants.SecretValueName, secretValue.marshal());
        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    private Value marshalResource() {
        ResourceReference resource = getValue();
        Struct.Builder structBuilder = Struct.newBuilder();
        structBuilder.putFields(Constants.SpecialSigKey,
            Value.newBuilder().setStringValue(Constants.SpecialResourceSig).build());
        structBuilder.putFields(Constants.UrnPropertyName,
            Value.newBuilder().setStringValue(resource.URN).build());
        
        if (resource.id != null) {
            structBuilder.putFields(Constants.IdPropertyName, resource.id.marshal());
        }
        
        if (!resource.packageVersion.isEmpty()) {
            structBuilder.putFields(Constants.ResourceVersionName,
                Value.newBuilder().setStringValue(resource.packageVersion).build());
        }

        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    private Value marshalOutput(OutputReference output, boolean isSecret) {
        Struct.Builder structBuilder = Struct.newBuilder();
        structBuilder.putFields(Constants.SpecialSigKey,
            Value.newBuilder().setStringValue(Constants.SpecialOutputSig).build());

        if (output.getValue() != null) {
            structBuilder.putFields(Constants.SecretValueName, output.getValue().marshal());
        }

        ListValue.Builder depsBuilder = ListValue.newBuilder();
        List<String> sortedDeps = new ArrayList<>(output.getDependencies());
        sortedDeps.sort(String.CASE_INSENSITIVE_ORDER);
        for (String dep : sortedDeps) {
            depsBuilder.addValues(Value.newBuilder().setStringValue(dep).build());
        }
        structBuilder.putFields(Constants.DependenciesName,
            Value.newBuilder().setListValue(depsBuilder).build());

        structBuilder.putFields(Constants.SecretName,
            Value.newBuilder().setBoolValue(isSecret).build());

        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    @Override
    public String toString() {
        switch (type) {
            case NULL:
                return "null";
            case BOOL:
                return getValue().toString();
            case NUMBER:
                return getValue().toString();
            case STRING:
                return getValue().toString();
            case ARRAY:
                List<PropertyValue> list = getValue();
                return "[" + String.join(",", 
                    list.stream()
                        .map(Object::toString)
                        .collect(Collectors.toList())) + "]";
            case OBJECT:
                Map<String, PropertyValue> map = getValue();
                return "{" + String.join(",", 
                    map.entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue().toString())
                        .collect(Collectors.toList())) + "}";
            case ASSET:
                return getValue().toString();
            case ARCHIVE:
                return getValue().toString();
            case SECRET:
                return "secret(" + getValue().toString() + ")";
            case RESOURCE:
                return getValue().toString();
            case OUTPUT:
                return getValue().toString();
            case COMPUTED:
                return "{unknown}";
            default:
                return "unknown";
        }
    }

    // Add this static method
    public static Map<String, PropertyValue> unmarshalProperties(Struct properties) {
        Map<String, PropertyValue> result = new HashMap<>();
        for (Map.Entry<String, Value> entry : properties.getFieldsMap().entrySet()) {
            result.put(entry.getKey(), unmarshal(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    // Add this static method
    public static Struct marshalProperties(Map<String, PropertyValue> properties) {
        Struct.Builder builder = Struct.newBuilder();
        for (Map.Entry<String, PropertyValue> entry : properties.entrySet()) {
            builder.putFields(entry.getKey(), entry.getValue().marshal());
        }
        return builder.build();
    }
}
