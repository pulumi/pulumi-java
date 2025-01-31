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
    // Property values will be one of these types.
    public enum PropertyValueType {
        Null,
        Bool,
        Number,
        String,
        Array,
        Object,
        Asset,
        Archive,
        Secret,
        Resource,
        Output,
        Computed
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
    public static final PropertyValue NULL = new PropertyValue(SpecialType.IsNull);
    public static final PropertyValue COMPUTED = new PropertyValue(SpecialType.IsComputed);

    private enum SpecialType {
        IsNull,
        IsComputed
    }

    // Fields for storing the actual value
    private final Boolean boolValue;
    private final Double numberValue;
    private final String stringValue;
    private final List<PropertyValue> arrayValue;
    private final Map<String, PropertyValue> objectValue;
    private final Asset assetValue;
    private final Archive archiveValue;
    private final PropertyValue secretValue;
    private final ResourceReference resourceValue;
    private final OutputReference outputValue;
    private final boolean isComputed;

    public PropertyValue(boolean value) {
        this.boolValue = value;
        this.numberValue = null;
        this.stringValue = null;
        this.arrayValue = null;
        this.objectValue = null;
        this.assetValue = null;
        this.archiveValue = null;
        this.secretValue = null;
        this.resourceValue = null;
        this.outputValue = null;
        this.isComputed = false;
    }

    public PropertyValue(double value) {
        this.boolValue = null;
        this.numberValue = value;
        this.stringValue = null;
        this.arrayValue = null;
        this.objectValue = null;
        this.assetValue = null;
        this.archiveValue = null;
        this.secretValue = null;
        this.resourceValue = null;
        this.outputValue = null;
        this.isComputed = false;
    }

    public PropertyValue(String value) {
        this.boolValue = null;
        this.numberValue = null;
        this.stringValue = value;
        this.arrayValue = null;
        this.objectValue = null;
        this.assetValue = null;
        this.archiveValue = null;
        this.secretValue = null;
        this.resourceValue = null;
        this.outputValue = null;
        this.isComputed = false;
    }

    public PropertyValue(List<PropertyValue> value) {
        this.boolValue = null;
        this.numberValue = null;
        this.stringValue = null;
        this.arrayValue = Collections.unmodifiableList(new ArrayList<>(value));
        this.objectValue = null;
        this.assetValue = null;
        this.archiveValue = null;
        this.secretValue = null;
        this.resourceValue = null;
        this.outputValue = null;
        this.isComputed = false;
    }

    public PropertyValue(Map<String, PropertyValue> value) {
        this.boolValue = null;
        this.numberValue = null;
        this.stringValue = null;
        this.arrayValue = null;
        this.objectValue = Collections.unmodifiableMap(new HashMap<>(value));
        this.assetValue = null;
        this.archiveValue = null;
        this.secretValue = null;
        this.resourceValue = null;
        this.outputValue = null;
        this.isComputed = false;
    }

    private PropertyValue(SpecialType type) {
        this.boolValue = null;
        this.numberValue = null;
        this.stringValue = null;
        this.arrayValue = null;
        this.objectValue = null;
        this.assetValue = null;
        this.archiveValue = null;
        this.secretValue = null;
        this.resourceValue = null;
        this.outputValue = null;
        this.isComputed = type == SpecialType.IsComputed;
    }

    public PropertyValueType getType() {
        if (boolValue != null) return PropertyValueType.Bool;
        if (numberValue != null) return PropertyValueType.Number;
        if (stringValue != null) return PropertyValueType.String;
        if (arrayValue != null) return PropertyValueType.Array;
        if (objectValue != null) return PropertyValueType.Object;
        if (assetValue != null) return PropertyValueType.Asset;
        if (archiveValue != null) return PropertyValueType.Archive;
        if (secretValue != null) return PropertyValueType.Secret;
        if (resourceValue != null) return PropertyValueType.Resource;
        if (outputValue != null) return PropertyValueType.Output;
        if (isComputed) return PropertyValueType.Computed;
        return PropertyValueType.Null;
    }

    public boolean isNull() {
        return boolValue == null &&
               numberValue == null &&
               stringValue == null &&
               arrayValue == null &&
               objectValue == null &&
               assetValue == null &&
               archiveValue == null &&
               secretValue == null &&
               resourceValue == null &&
               outputValue == null &&
               !isComputed;
    }

    public boolean isComputed() {
        return isComputed;
    }

    public Asset getAssetValue() {
        return assetValue;
    }

    public Archive getArchiveValue() {
        return archiveValue;
    }

    public Boolean getBoolValue() {
        return boolValue;
    }

    public Double getNumberValue() {
        return numberValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public List<PropertyValue> getArrayValue() {
        return arrayValue;
    }

    public Map<String, PropertyValue> getObjectValue() {
        return objectValue;
    }

    public PropertyValue getSecretValue() {
        return secretValue;
    }

    public ResourceReference getResourceValue() {
        return resourceValue;
    }

    public OutputReference getOutputValue() {
        return outputValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyValue)) return false;
        PropertyValue that = (PropertyValue) o;
        
        if (boolValue != null && that.boolValue != null) {
            return boolValue.equals(that.boolValue);
        }
        if (numberValue != null && that.numberValue != null) {
            return numberValue.equals(that.numberValue);
        }
        if (stringValue != null && that.stringValue != null) {
            return stringValue.equals(that.stringValue);
        }
        if (arrayValue != null && that.arrayValue != null) {
            if (arrayValue.size() != that.arrayValue.size()) {
                return false;
            }
            for (int i = 0; i < arrayValue.size(); i++) {
                if (!arrayValue.get(i).equals(that.arrayValue.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (objectValue != null && that.objectValue != null) {
            if (objectValue.size() != that.objectValue.size()) {
                return false;
            }
            for (Map.Entry<String, PropertyValue> entry : objectValue.entrySet()) {
                PropertyValue theirValue = that.objectValue.get(entry.getKey());
                if (theirValue == null || !entry.getValue().equals(theirValue)) {
                    return false;
                }
            }
            return true;
        }
        if (assetValue != null && that.assetValue != null) {
            return assetValue.equals(that.assetValue);
        }
        if (archiveValue != null && that.archiveValue != null) {
            return archiveValue.equals(that.archiveValue);
        }
        if (secretValue != null && that.secretValue != null) {
            return secretValue.equals(that.secretValue);
        }
        if (resourceValue != null && that.resourceValue != null) {
            return resourceValue.equals(that.resourceValue);
        }
        if (outputValue != null && that.outputValue != null) {
            return outputValue.equals(that.outputValue);
        }
        if (isComputed && that.isComputed) {
            return true;
        }        
        if (isNull() && that.isNull()) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (boolValue != null) return boolValue.hashCode();
        if (numberValue != null) return numberValue.hashCode();
        if (stringValue != null) return stringValue.hashCode();
        if (arrayValue != null) return arrayValue.hashCode();
        if (objectValue != null) return objectValue.hashCode();
        if (assetValue != null) return assetValue.hashCode();
        if (archiveValue != null) return archiveValue.hashCode();
        if (secretValue != null) {
            // Combine boolean true with secretValue to differentiate from non-secret value
            return Objects.hash(Boolean.TRUE, secretValue);
        }
        if (resourceValue != null) return resourceValue.hashCode();
        if (outputValue != null) return outputValue.hashCode();
        if (isComputed) return SpecialType.IsComputed.hashCode();
        return SpecialType.IsNull.hashCode();
    }

    public PropertyValue(Asset value) {
        this.boolValue = null;
        this.numberValue = null;
        this.stringValue = null;
        this.arrayValue = null;
        this.objectValue = null;
        this.assetValue = value;
        this.archiveValue = null;
        this.secretValue = null;
        this.resourceValue = null;
        this.outputValue = null;
        this.isComputed = false;
    }

    public PropertyValue(Archive value) {
        this.boolValue = null;
        this.numberValue = null;
        this.stringValue = null;
        this.arrayValue = null;
        this.objectValue = null;
        this.assetValue = null;
        this.archiveValue = value;
        this.secretValue = null;
        this.resourceValue = null;
        this.outputValue = null;
        this.isComputed = false;
    }

    public PropertyValue(PropertyValue secret) {
        this.boolValue = null;
        this.numberValue = null;
        this.stringValue = null;
        this.arrayValue = null;
        this.objectValue = null;
        this.assetValue = null;
        this.archiveValue = null;
        this.secretValue = secret;
        this.resourceValue = null;
        this.outputValue = null;
        this.isComputed = false;
    }

    public PropertyValue(ResourceReference value) {
        this.boolValue = null;
        this.numberValue = null;
        this.stringValue = null;
        this.arrayValue = null;
        this.objectValue = null;
        this.assetValue = null;
        this.archiveValue = null;
        this.secretValue = null;
        this.resourceValue = value;
        this.outputValue = null;
        this.isComputed = false;
    }

    public PropertyValue(OutputReference value) {
        this.boolValue = null;
        this.numberValue = null;
        this.stringValue = null;
        this.arrayValue = null;
        this.objectValue = null;
        this.assetValue = null;
        this.archiveValue = null;
        this.secretValue = null;
        this.resourceValue = null;
        this.outputValue = value;
        this.isComputed = false;
    }

    // Protobuf marshaling methods
    public static PropertyValue unmarshal(Value value) {
        switch (value.getKindCase()) {
            case NULL_VALUE:
                return PropertyValue.NULL;
            case BOOL_VALUE:
                return new PropertyValue(value.getBoolValue());
            case NUMBER_VALUE:
                return new PropertyValue(value.getNumberValue());
            case STRING_VALUE:
                // This could be the special unknown value
                if (Constants.UnknownValue.equals(value.getStringValue())) {
                    return PropertyValue.COMPUTED;
                }
                return new PropertyValue(value.getStringValue());
            case LIST_VALUE:
                List<PropertyValue> list = new ArrayList<>();
                for (Value v : value.getListValue().getValuesList()) {
                    list.add(unmarshal(v));
                }
                return new PropertyValue(list);
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
        return new PropertyValue(map);
    }

    private static PropertyValue unmarshalSecret(Map<String, Value> fields) {
        if (!fields.containsKey(Constants.SecretValueName)) {
            throw new IllegalArgumentException("Secrets must have a field called 'value'");
        }
        return new PropertyValue(unmarshal(fields.get(Constants.SecretValueName)));
    }

    private static PropertyValue unmarshalAsset(Map<String, Value> fields) {
        if (fields.containsKey(Constants.AssetOrArchivePathName)) {
            return new PropertyValue(new FileAsset(fields.get(Constants.AssetOrArchivePathName).getStringValue()));
        }
        if (fields.containsKey(Constants.AssetOrArchiveUriName)) {
            return new PropertyValue(new RemoteAsset(fields.get(Constants.AssetOrArchiveUriName).getStringValue()));
        }
        if (fields.containsKey(Constants.AssetTextName)) {
            return new PropertyValue(new StringAsset(fields.get(Constants.AssetTextName).getStringValue()));
        }
        throw new IllegalArgumentException("Asset must have either 'path', 'uri', or 'text' field");
    }

    private static PropertyValue unmarshalArchive(Map<String, Value> fields) {
        if (fields.containsKey(Constants.AssetOrArchivePathName)) {
            return new PropertyValue(new FileArchive(fields.get(Constants.AssetOrArchivePathName).getStringValue()));
        }
        if (fields.containsKey(Constants.AssetOrArchiveUriName)) {
            return new PropertyValue(new RemoteArchive(fields.get(Constants.AssetOrArchiveUriName).getStringValue()));
        }
        if (fields.containsKey(Constants.ArchiveAssetsName)) {
            Value assetsValue = fields.get(Constants.ArchiveAssetsName);
            if (assetsValue.getKindCase() != Value.KindCase.STRUCT_VALUE) {
                throw new IllegalArgumentException("Archive assets must be an object");
            }
            
            Map<String, AssetOrArchive> assets = new HashMap<>();
            for (Map.Entry<String, Value> entry : assetsValue.getStructValue().getFieldsMap().entrySet()) {
                PropertyValue innerValue = unmarshal(entry.getValue());
                if (innerValue.assetValue != null) {
                    assets.put(entry.getKey(), innerValue.assetValue);
                } else if (innerValue.archiveValue != null) {
                    assets.put(entry.getKey(), innerValue.archiveValue);
                } else {
                    throw new IllegalArgumentException("AssetArchive can only contain Assets or Archives");
                }
            }
            return new PropertyValue(new AssetArchive(Collections.unmodifiableMap(assets)));
        }
        throw new IllegalArgumentException("Archive must have either 'path', 'uri', or 'assets' field");
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
        return new PropertyValue(new ResourceReference(urn, id, version));
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

        PropertyValue output = new PropertyValue(new OutputReference(value, dependencies));
        return isSecret ? new PropertyValue(output) : output;
    }

    // Marshal methods
    public Value marshal() {
        switch (getType()) {
            case Null:
                return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
            case Bool:
                return Value.newBuilder().setBoolValue(boolValue).build();
            case Number:
                return Value.newBuilder().setNumberValue(numberValue).build();
            case String:
                return Value.newBuilder().setStringValue(stringValue).build();
            case Array:
                ListValue.Builder listBuilder = ListValue.newBuilder();
                for (PropertyValue item : arrayValue) {
                    listBuilder.addValues(item.marshal());
                }
                return Value.newBuilder().setListValue(listBuilder).build();
            case Object:
                return marshalObject();
            case Asset:
                return marshalAsset();
            case Archive:
                return marshalArchive();
            case Secret:
                return marshalSecret();
            case Resource:
                return marshalResource();
            case Output:
                return marshalOutput(outputValue, false);
            case Computed:
                return Value.newBuilder().setStringValue(Constants.UnknownValue).build();
            default:
                throw new IllegalStateException("Unknown property value type: " + getType());
        }
    }

    private Value marshalObject() {
        Struct.Builder structBuilder = Struct.newBuilder();
        for (Map.Entry<String, PropertyValue> entry : objectValue.entrySet()) {
            structBuilder.putFields(entry.getKey(), entry.getValue().marshal());
        }
        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    private Value marshalAsset() {
        var internal = AssetOrArchive.AssetOrArchiveInternal.from(assetValue);
        Struct.Builder structBuilder = Struct.newBuilder();
        structBuilder.putFields(Constants.SpecialSigKey, 
            Value.newBuilder().setStringValue(internal.getSigKey()).build());
        structBuilder.putFields(internal.getPropName(),
            Value.newBuilder().setStringValue((String)internal.getValue()).build());
        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    private Value marshalArchive() {
        var internal = AssetOrArchive.AssetOrArchiveInternal.from(archiveValue);
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
                    assetsBuilder.putFields(entry.getKey(), new PropertyValue((Asset)entry.getValue()).marshal());
                } else {
                    assetsBuilder.putFields(entry.getKey(), new PropertyValue((Archive)entry.getValue()).marshal());
                }
            }
            structBuilder.putFields(internal.getPropName(),
                Value.newBuilder().setStructValue(assetsBuilder).build());
        }
        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    private Value marshalSecret() {
        // Special case if our secret value is an output
        if (secretValue.getType() == PropertyValueType.Output) {
            return marshalOutput(secretValue.outputValue, true);
        }
        Struct.Builder structBuilder = Struct.newBuilder();
        structBuilder.putFields(Constants.SpecialSigKey,
            Value.newBuilder().setStringValue(Constants.SpecialSecretSig).build());
        structBuilder.putFields(Constants.SecretValueName, secretValue.marshal());
        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    private Value marshalResource() {
        Struct.Builder structBuilder = Struct.newBuilder();
        structBuilder.putFields(Constants.SpecialSigKey,
            Value.newBuilder().setStringValue(Constants.SpecialResourceSig).build());
        structBuilder.putFields(Constants.UrnPropertyName,
            Value.newBuilder().setStringValue(resourceValue.URN.toString()).build());
        
        if (resourceValue.id != null) {
            structBuilder.putFields(Constants.IdPropertyName, resourceValue.id.marshal());
        }
        
        if (!resourceValue.packageVersion.isEmpty()) {
            structBuilder.putFields(Constants.ResourceVersionName,
                Value.newBuilder().setStringValue(resourceValue.packageVersion).build());
        }

        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    private Value marshalOutput(OutputReference output, boolean isSecret) {
        Struct.Builder structBuilder = Struct.newBuilder();
        structBuilder.putFields(Constants.SpecialSigKey,
            Value.newBuilder().setStringValue(Constants.SpecialOutputSig).build());

        if (output.value != null) {
            structBuilder.putFields(Constants.SecretValueName, output.value.marshal());
        }

        ListValue.Builder depsBuilder = ListValue.newBuilder();
        List<String> sortedDeps = new ArrayList<>(output.dependencies);
        sortedDeps.sort(String.CASE_INSENSITIVE_ORDER);
        for (String dep : sortedDeps) {
            depsBuilder.addValues(Value.newBuilder().setStringValue(dep.toString()).build());
        }
        structBuilder.putFields(Constants.DependenciesName,
            Value.newBuilder().setListValue(depsBuilder).build());

        structBuilder.putFields(Constants.SecretName,
            Value.newBuilder().setBoolValue(isSecret).build());

        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    @Override
    public String toString() {
        switch (getType()) {
            case Null:
                return "null";
            case Bool:
                return boolValue.toString();
            case Number:
                return numberValue.toString();
            case String:
                return stringValue;
            case Array:
                return "[" + String.join(",", 
                    arrayValue.stream()
                        .map(Object::toString)
                        .collect(Collectors.toList())) + "]";
            case Object:
                return "{" + String.join(",", 
                    objectValue.entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue().toString())
                        .collect(Collectors.toList())) + "}";
            case Asset:
                return assetValue.toString();
            case Archive:
                return archiveValue.toString();
            case Secret:
                return "secret(" + secretValue.toString() + ")";
            case Resource:
                return resourceValue.toString();
            case Output:
                return outputValue.toString();
            case Computed:
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
