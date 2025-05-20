package com.pulumi.provider.internal;

import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.pulumi.asset.*;
import com.pulumi.core.internal.Constants;
import com.pulumi.provider.internal.properties.PropertyValue;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PropertyValueTests {

    // Helper method for comparing asset/archive internals
    private void assertAssetOrArchiveEquals(AssetOrArchive original, AssetOrArchive unmarshaled) {
        var originalInternal = AssetOrArchive.AssetOrArchiveInternal.from(original);
        var unmarshaledInternal = AssetOrArchive.AssetOrArchiveInternal.from(unmarshaled);
        assertEquals(originalInternal.getSigKey(), unmarshaledInternal.getSigKey());
        assertEquals(originalInternal.getPropName(), unmarshaledInternal.getPropName());
        assertEquals(originalInternal.getValue(), unmarshaledInternal.getValue());
    }

    @Test
    void testMarshalUnmarshalBasicTypes() {
        // Test null
        var nullValue = PropertyValue.NULL;
        var marshaledNull = nullValue.marshal();
        assertEquals(Value.KindCase.NULL_VALUE, marshaledNull.getKindCase());
        var unmarshaledNull = PropertyValue.unmarshal(marshaledNull);
        assertEquals(nullValue, unmarshaledNull);

        // Test boolean
        var boolValue = PropertyValue.of(true);
        var marshaledBool = boolValue.marshal();
        assertEquals(Value.KindCase.BOOL_VALUE, marshaledBool.getKindCase());
        assertTrue(marshaledBool.getBoolValue());
        var unmarshaledBool = PropertyValue.unmarshal(marshaledBool);
        assertEquals(boolValue, unmarshaledBool);

        // Test number
        var numberValue = PropertyValue.of(42.0);
        var marshaledNumber = numberValue.marshal();
        assertEquals(Value.KindCase.NUMBER_VALUE, marshaledNumber.getKindCase());
        assertEquals(42.0, marshaledNumber.getNumberValue(), 0.001);
        var unmarshaledNumber = PropertyValue.unmarshal(marshaledNumber);
        assertEquals(numberValue, unmarshaledNumber);

        // Test string
        var stringValue = PropertyValue.of("test");
        var marshaledString = stringValue.marshal();
        assertEquals(Value.KindCase.STRING_VALUE, marshaledString.getKindCase());
        assertEquals("test", marshaledString.getStringValue());
        var unmarshaledString = PropertyValue.unmarshal(marshaledString);
        assertEquals(stringValue, unmarshaledString);
    }

    @Test
    void testMarshalUnmarshalArrays() {
        var arrayValue = PropertyValue.of(Arrays.asList(
            PropertyValue.of("first"),
            PropertyValue.of(42.0),
            PropertyValue.of(true),
            PropertyValue.NULL,
            PropertyValue.COMPUTED
        ));

        var marshaledArray = arrayValue.marshal();
        assertEquals(Value.KindCase.LIST_VALUE, marshaledArray.getKindCase());
        
        ListValue list = marshaledArray.getListValue();
        assertEquals(5, list.getValuesCount());
        assertEquals("first", list.getValues(0).getStringValue());
        assertEquals(42.0, list.getValues(1).getNumberValue(), 0.001);
        assertTrue(list.getValues(2).getBoolValue());

        var unmarshaledArray = PropertyValue.unmarshal(marshaledArray);
        assertEquals(arrayValue, unmarshaledArray);
    }

    @Test
    void testMarshalUnmarshalObjects() {
        Map<String, PropertyValue> map = new HashMap<>();
        map.put("string", PropertyValue.of("value"));
        map.put("number", PropertyValue.of(42.0));
        map.put("bool", PropertyValue.of(true));
        map.put("null", PropertyValue.NULL);
        map.put("computed", PropertyValue.COMPUTED);
        map.put("nested", PropertyValue.of(Map.of("key", PropertyValue.of("nested-value"))));
        
        var objectValue = PropertyValue.of(map);
        var marshaledObject = objectValue.marshal();
        assertEquals(Value.KindCase.STRUCT_VALUE, marshaledObject.getKindCase());
        
        Struct struct = marshaledObject.getStructValue();
        assertEquals(6, struct.getFieldsCount());
        assertEquals("value", struct.getFieldsMap().get("string").getStringValue());
        assertEquals(42.0, struct.getFieldsMap().get("number").getNumberValue(), 0.001);
        assertTrue(struct.getFieldsMap().get("bool").getBoolValue());

        var unmarshaledObject = PropertyValue.unmarshal(marshaledObject);
        assertEquals(objectValue, unmarshaledObject);
    }

    @Test
    void testMarshalUnmarshalAssets() {
        // Test all asset types
        var assets = new PropertyValue[]{
            PropertyValue.of(new FileAsset("path/to/file")),
            PropertyValue.of(new StringAsset("asset content\nwith newlines")),
            PropertyValue.of(new RemoteAsset("https://example.com/asset?param=value")),
            PropertyValue.of(new RemoteAsset("https://example.com/asset"))
        };

        for (var asset : assets) {
            var marshaled = asset.marshal();
            var unmarshaled = PropertyValue.unmarshal(marshaled);
            assertAssetOrArchiveEquals(asset.getAssetValue(), unmarshaled.getAssetValue());
        }
    }

    @Test
    void testMarshalUnmarshalArchives() {
        // Test all archive types
        var fileArchive = PropertyValue.of(new FileArchive("path/to/archive.zip"));
        var remoteArchive = PropertyValue.of(new RemoteArchive("https://example.com/archive.tar.gz"));

        // Test basic archives
        for (var archive : new PropertyValue[]{fileArchive, remoteArchive}) {
            var marshaled = archive.marshal();
            var unmarshaled = PropertyValue.unmarshal(marshaled);
            assertAssetOrArchiveEquals(archive.getArchiveValue(), unmarshaled.getArchiveValue());
        }

        // Test AssetArchive with mixed content
        Map<String, AssetOrArchive> assets = new HashMap<>();
        assets.put("file1", new FileAsset("path/to/file1"));
        assets.put("file2", new StringAsset("content\nwith newlines"));
        assets.put("nested", new FileArchive("path/to/nested.zip"));
        var assetArchive = PropertyValue.of(new AssetArchive(assets));
        
        var marshaledAssetArchive = assetArchive.marshal();
        var unmarshaledAssetArchive = PropertyValue.unmarshal(marshaledAssetArchive);
        
        var originalMap = (Map<String, AssetOrArchive>)AssetOrArchive.AssetOrArchiveInternal
            .from(assetArchive.getArchiveValue()).getValue();
        var unmarshaledMap = (Map<String, AssetOrArchive>)AssetOrArchive.AssetOrArchiveInternal
            .from(unmarshaledAssetArchive.getArchiveValue()).getValue();
        
        assertEquals(originalMap.size(), unmarshaledMap.size());
        
        for (Map.Entry<String, AssetOrArchive> entry : originalMap.entrySet()) {
            assertAssetOrArchiveEquals(entry.getValue(), unmarshaledMap.get(entry.getKey()));
        }
    }

    @Test
    void testMarshalUnmarshalSecrets() {
        var secret = PropertyValue.ofSecret(PropertyValue.of("secret value"));
        var marshaledSecret = secret.marshal();
        var unmarshaledSecret = PropertyValue.unmarshal(marshaledSecret);
        assertEquals(secret, unmarshaledSecret);
    }

    @Test
    void testMarshalUnmarshalComputed() {
        var computed = PropertyValue.COMPUTED;
        var marshaledComputed = computed.marshal();
        var unmarshaledComputed = PropertyValue.unmarshal(marshaledComputed);
        assertEquals(computed, unmarshaledComputed);
    }

    @Test
    void testMarshalUnmarshalOutput() {
        Set<String> dependencies = new HashSet<>(Arrays.asList("dep1", "dep2", "dep3"));
        
        // Test different output variants
        var outputs = new PropertyValue[]{
            PropertyValue.of(new PropertyValue.OutputReference(PropertyValue.of("test value"), dependencies)),
            PropertyValue.of(new PropertyValue.OutputReference(null, dependencies)),
            PropertyValue.of(new PropertyValue.OutputReference(PropertyValue.COMPUTED, dependencies)),
            PropertyValue.of(new PropertyValue.OutputReference(
                PropertyValue.of(Map.of("nested", PropertyValue.of("value"))), 
                dependencies
            ))
        };

        for (var output : outputs) {
            var marshaled = output.marshal();
            var unmarshaled = PropertyValue.unmarshal(marshaled);
            assertEquals(output, unmarshaled);
        }
    }

    @Test
    void testMarshalUnmarshalResource() {
        var resources = new PropertyValue[]{
            // Full resource
            PropertyValue.of(new PropertyValue.ResourceReference(
                "urn:pulumi:stack::project::type::name",
                PropertyValue.of("resource-id"),
                "1.0.0"
            )),
            // Minimal resource
            PropertyValue.of(new PropertyValue.ResourceReference(
                "urn:pulumi:stack::project::type::name",
                null,
                ""
            )),
            // Resource with computed ID
            PropertyValue.of(new PropertyValue.ResourceReference(
                "urn:pulumi:stack::project::type::name",
                PropertyValue.COMPUTED,
                "2.0.0"
            ))
        };

        for (var resource : resources) {
            var marshaled = resource.marshal();
            var unmarshaled = PropertyValue.unmarshal(marshaled);
            assertEquals(resource, unmarshaled);
        }
    }

    @Test
    void testMarshalUnmarshalProperties() {
        // Create a test properties map
        Map<String, PropertyValue> properties = new HashMap<>();
        properties.put("string", PropertyValue.of("value"));
        properties.put("number", PropertyValue.of(42.0));
        properties.put("bool", PropertyValue.of(true));
        properties.put("null", PropertyValue.NULL);
        properties.put("computed", PropertyValue.COMPUTED);
        
        // Test marshaling
        var marshaledStruct = PropertyValue.marshalProperties(properties);
        
        // Test unmarshaling
        var unmarshaledProperties = PropertyValue.unmarshalProperties(marshaledStruct);
        
        // Verify contents
        assertEquals(properties.size(), unmarshaledProperties.size());
        for (Map.Entry<String, PropertyValue> entry : properties.entrySet()) {
            assertTrue(unmarshaledProperties.containsKey(entry.getKey()));
            assertEquals(entry.getValue(), unmarshaledProperties.get(entry.getKey()));
        }
    }

    @Test
    void testUnmarshalInvalidStruct() {
        // Test invalid special signature
        Map<String, Value> fields = new HashMap<>();
        fields.put(Constants.SpecialSigKey, Value.newBuilder().setStringValue("invalid").build());
        Struct struct = Struct.newBuilder().putAllFields(fields).build();
        assertThrows(IllegalArgumentException.class, () -> PropertyValue.unmarshal(Value.newBuilder().setStructValue(struct).build()));
    }

    @Test
    void testUnmarshalInvalidSecret() {
        // Test secret without value field
        Map<String, Value> fields = new HashMap<>();
        fields.put(Constants.SpecialSigKey, Value.newBuilder().setStringValue(Constants.SpecialSecretSig).build());
        Struct struct = Struct.newBuilder().putAllFields(fields).build();
        assertThrows(IllegalArgumentException.class, 
            () -> PropertyValue.unmarshal(Value.newBuilder().setStructValue(struct).build()));
    }

    @Test
    void testPropertyValueEquality() {
        // Test equality of different types
        assertNotEquals(PropertyValue.of(true), PropertyValue.of(42.0));
        assertNotEquals(PropertyValue.of("test"), PropertyValue.of(true));
        assertNotEquals(PropertyValue.of(Arrays.asList()), PropertyValue.of(new HashMap<>()));
        
        // Test equality of arrays with different lengths
        assertNotEquals(
            PropertyValue.of(Arrays.asList(PropertyValue.of("test"))),
            PropertyValue.of(Arrays.asList())
        );

        // Test equality of objects with different keys
        Map<String, PropertyValue> map1 = new HashMap<>();
        map1.put("key1", PropertyValue.of("value1"));
        Map<String, PropertyValue> map2 = new HashMap<>();
        map2.put("key2", PropertyValue.of("value1"));
        assertNotEquals(PropertyValue.of(map1), PropertyValue.of(map2));
    }

    @Test
    void testEquality() {
        // Test null equality
        assertEquals(PropertyValue.NULL, PropertyValue.NULL);
        assertNotEquals(PropertyValue.NULL, PropertyValue.of(true));
        
        // Test computed equality
        assertEquals(PropertyValue.COMPUTED, PropertyValue.COMPUTED);
        assertNotEquals(PropertyValue.COMPUTED, PropertyValue.NULL);
        
        // Test same type but different values
        assertNotEquals(PropertyValue.of(true), PropertyValue.of(false));
        assertNotEquals(PropertyValue.of(42.0), PropertyValue.of(43.0));
        assertNotEquals(PropertyValue.of("test1"), PropertyValue.of("test2"));
        
        // Test array equality with same content
        var array1 = PropertyValue.of(Arrays.asList(PropertyValue.of("test")));
        var array2 = PropertyValue.of(Arrays.asList(PropertyValue.of("test")));
        assertEquals(array1, array2);
        
        // Test object equality with same content
        Map<String, PropertyValue> map1 = new HashMap<>();
        map1.put("key", PropertyValue.of("value"));
        Map<String, PropertyValue> map2 = new HashMap<>();
        map2.put("key", PropertyValue.of("value"));
        assertEquals(PropertyValue.of(map1), PropertyValue.of(map2));
        
        // Test non-PropertyValue objects
        assertNotEquals(PropertyValue.of(true), Boolean.TRUE);
    }

    @Test
    void testIsNull() {
        assertTrue(PropertyValue.NULL.isNull());
        assertFalse(PropertyValue.of(true).isNull());
        assertFalse(PropertyValue.of(42.0).isNull());
        assertFalse(PropertyValue.of("test").isNull());
        assertFalse(PropertyValue.of(Arrays.asList()).isNull());
        assertFalse(PropertyValue.of(new HashMap<>()).isNull());
        assertFalse(PropertyValue.of(new FileAsset("test")).isNull());
        assertFalse(PropertyValue.of(new FileArchive("test")).isNull());
        assertFalse(PropertyValue.COMPUTED.isNull());
    }

    @Test
    void testHashCode() {
        // Test that equal objects have equal hash codes
        assertEquals(
            PropertyValue.of("test").hashCode(),
            PropertyValue.of("test").hashCode()
        );
        
        assertEquals(
            PropertyValue.of(Arrays.asList(PropertyValue.of("test"))).hashCode(),
            PropertyValue.of(Arrays.asList(PropertyValue.of("test"))).hashCode()
        );
        
        Map<String, PropertyValue> map1 = new HashMap<>();
        map1.put("key", PropertyValue.of("value"));
        Map<String, PropertyValue> map2 = new HashMap<>();
        map2.put("key", PropertyValue.of("value"));
        assertEquals(
            PropertyValue.of(map1).hashCode(),
            PropertyValue.of(map2).hashCode()
        );

        // Test that different objects have different hash codes
        assertNotEquals(
            PropertyValue.of("test1").hashCode(),
            PropertyValue.of("test2").hashCode()
        );

        // Test Asset hashCode consistency
        var fileAsset = PropertyValue.of(new FileAsset("path"));
        assertEquals(fileAsset.hashCode(), fileAsset.hashCode());

        // Test Archive hashCode consistency
        var fileArchive = PropertyValue.of(new FileArchive("path"));
        assertEquals(fileArchive.hashCode(), fileArchive.hashCode());

        // Test Resource hashCode consistency
        var ref = new PropertyValue.ResourceReference("urn", null, "");
        var resource = PropertyValue.of(ref);
        assertEquals(resource.hashCode(), resource.hashCode());

        // Test Output hashCode consistency
        Set<String> deps = new HashSet<>(Arrays.asList("dep1"));
        var outputRef = new PropertyValue.OutputReference(PropertyValue.of("test"), deps);
        var output = PropertyValue.of(outputRef);
        assertEquals(output.hashCode(), output.hashCode());
    }

    @Test
    void testToString() {
        // Test basic types
        assertEquals("null", PropertyValue.NULL.toString());
        assertEquals("true", PropertyValue.of(true).toString());
        assertEquals("42.0", PropertyValue.of(42.0).toString());
        assertEquals("test", PropertyValue.of("test").toString());
        assertEquals("{unknown}", PropertyValue.COMPUTED.toString());
        
        // Test collections
        assertEquals("[1.1,2.2,null]", PropertyValue.of(Arrays.asList(
            PropertyValue.of(1.1),
            PropertyValue.of(2.2),
            PropertyValue.NULL
        )).toString());
        
        Map<String, PropertyValue> map = new HashMap<>();
        map.put("key1", PropertyValue.of("value1"));
        map.put("key2", PropertyValue.NULL);
        assertEquals("{key1:value1,key2:null}", PropertyValue.of(map).toString());
        
        // Test special types
        assertEquals("secret(test)", PropertyValue.ofSecret(PropertyValue.of("test")).toString());
        
        // Test assets and archives
        var fileAsset = PropertyValue.of(new FileAsset("path/to/file"));
        assertEquals(fileAsset.getAssetValue().toString(), fileAsset.toString());

        var fileArchive = PropertyValue.of(new FileArchive("path/to/archive"));
        assertEquals(fileArchive.getArchiveValue().toString(), fileArchive.toString());

        // Test resource and output
        var resourceRef = new PropertyValue.ResourceReference(
            "urn:pulumi:stack::project::type::name",
            PropertyValue.of("resource-id"),
            "1.0.0"
        );
        assertEquals(resourceRef.toString(), PropertyValue.of(resourceRef).toString());

        Set<String> deps = new HashSet<>(Arrays.asList("dep1", "dep2"));
        var outputRef = new PropertyValue.OutputReference(PropertyValue.of("test"), deps);
        assertEquals(outputRef.toString(), PropertyValue.of(outputRef).toString());
    }
}
