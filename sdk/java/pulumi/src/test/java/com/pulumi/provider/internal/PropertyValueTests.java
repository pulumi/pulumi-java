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
        var boolValue = new PropertyValue(true);
        var marshaledBool = boolValue.marshal();
        assertEquals(Value.KindCase.BOOL_VALUE, marshaledBool.getKindCase());
        assertTrue(marshaledBool.getBoolValue());
        var unmarshaledBool = PropertyValue.unmarshal(marshaledBool);
        assertEquals(boolValue, unmarshaledBool);

        // Test number
        var numberValue = new PropertyValue(42.0);
        var marshaledNumber = numberValue.marshal();
        assertEquals(Value.KindCase.NUMBER_VALUE, marshaledNumber.getKindCase());
        assertEquals(42.0, marshaledNumber.getNumberValue(), 0.001);
        var unmarshaledNumber = PropertyValue.unmarshal(marshaledNumber);
        assertEquals(numberValue, unmarshaledNumber);

        // Test string
        var stringValue = new PropertyValue("test");
        var marshaledString = stringValue.marshal();
        assertEquals(Value.KindCase.STRING_VALUE, marshaledString.getKindCase());
        assertEquals("test", marshaledString.getStringValue());
        var unmarshaledString = PropertyValue.unmarshal(marshaledString);
        assertEquals(stringValue, unmarshaledString);
    }

    @Test
    void testMarshalUnmarshalArrays() {
        var arrayValue = new PropertyValue(Arrays.asList(
            new PropertyValue("first"),
            new PropertyValue(42.0),
            new PropertyValue(true),
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
        map.put("string", new PropertyValue("value"));
        map.put("number", new PropertyValue(42.0));
        map.put("bool", new PropertyValue(true));
        map.put("null", PropertyValue.NULL);
        map.put("computed", PropertyValue.COMPUTED);
        map.put("nested", new PropertyValue(Map.of("key", new PropertyValue("nested-value"))));
        
        var objectValue = new PropertyValue(map);
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
            new PropertyValue(new FileAsset("path/to/file")),
            new PropertyValue(new StringAsset("asset content\nwith newlines")),
            new PropertyValue(new RemoteAsset("https://example.com/asset?param=value")),
            new PropertyValue(new RemoteAsset("https://example.com/asset"))
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
        var fileArchive = new PropertyValue(new FileArchive("path/to/archive.zip"));
        var remoteArchive = new PropertyValue(new RemoteArchive("https://example.com/archive.tar.gz"));

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
        var assetArchive = new PropertyValue(new AssetArchive(assets));
        
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
        var secret = new PropertyValue(new PropertyValue("secret value"));
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
            new PropertyValue(new PropertyValue.OutputReference(new PropertyValue("test value"), dependencies)),
            new PropertyValue(new PropertyValue.OutputReference(null, dependencies)),
            new PropertyValue(new PropertyValue.OutputReference(PropertyValue.COMPUTED, dependencies)),
            new PropertyValue(new PropertyValue.OutputReference(
                new PropertyValue(Map.of("nested", new PropertyValue("value"))), 
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
            new PropertyValue(new PropertyValue.ResourceReference(
                "urn:pulumi:stack::project::type::name",
                new PropertyValue("resource-id"),
                "1.0.0"
            )),
            // Minimal resource
            new PropertyValue(new PropertyValue.ResourceReference(
                "urn:pulumi:stack::project::type::name",
                null,
                ""
            )),
            // Resource with computed ID
            new PropertyValue(new PropertyValue.ResourceReference(
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
        properties.put("string", new PropertyValue("value"));
        properties.put("number", new PropertyValue(42.0));
        properties.put("bool", new PropertyValue(true));
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
        assertNotEquals(new PropertyValue(true), new PropertyValue(42.0));
        assertNotEquals(new PropertyValue("test"), new PropertyValue(true));
        assertNotEquals(new PropertyValue(Arrays.asList()), new PropertyValue(new HashMap<>()));
        
        // Test equality of arrays with different lengths
        assertNotEquals(
            new PropertyValue(Arrays.asList(new PropertyValue("test"))),
            new PropertyValue(Arrays.asList())
        );

        // Test equality of objects with different keys
        Map<String, PropertyValue> map1 = new HashMap<>();
        map1.put("key1", new PropertyValue("value1"));
        Map<String, PropertyValue> map2 = new HashMap<>();
        map2.put("key2", new PropertyValue("value1"));
        assertNotEquals(new PropertyValue(map1), new PropertyValue(map2));
    }

    @Test
    void testEquality() {
        // Test null equality
        assertEquals(PropertyValue.NULL, PropertyValue.NULL);
        assertNotEquals(PropertyValue.NULL, new PropertyValue(true));
        
        // Test computed equality
        assertEquals(PropertyValue.COMPUTED, PropertyValue.COMPUTED);
        assertNotEquals(PropertyValue.COMPUTED, PropertyValue.NULL);
        
        // Test same type but different values
        assertNotEquals(new PropertyValue(true), new PropertyValue(false));
        assertNotEquals(new PropertyValue(42.0), new PropertyValue(43.0));
        assertNotEquals(new PropertyValue("test1"), new PropertyValue("test2"));
        
        // Test array equality with same content
        var array1 = new PropertyValue(Arrays.asList(new PropertyValue("test")));
        var array2 = new PropertyValue(Arrays.asList(new PropertyValue("test")));
        assertEquals(array1, array2);
        
        // Test object equality with same content
        Map<String, PropertyValue> map1 = new HashMap<>();
        map1.put("key", new PropertyValue("value"));
        Map<String, PropertyValue> map2 = new HashMap<>();
        map2.put("key", new PropertyValue("value"));
        assertEquals(new PropertyValue(map1), new PropertyValue(map2));
        
        // Test non-PropertyValue objects
        assertNotEquals(new PropertyValue(true), Boolean.TRUE);
    }

    @Test
    void testIsNull() {
        assertTrue(PropertyValue.NULL.isNull());
        assertFalse(new PropertyValue(true).isNull());
        assertFalse(new PropertyValue(42.0).isNull());
        assertFalse(new PropertyValue("test").isNull());
        assertFalse(new PropertyValue(Arrays.asList()).isNull());
        assertFalse(new PropertyValue(new HashMap<>()).isNull());
        assertFalse(new PropertyValue(new FileAsset("test")).isNull());
        assertFalse(new PropertyValue(new FileArchive("test")).isNull());
        assertFalse(PropertyValue.COMPUTED.isNull());
    }

    @Test
    void testHashCode() {
        // Test that equal objects have equal hash codes
        assertEquals(
            new PropertyValue("test").hashCode(),
            new PropertyValue("test").hashCode()
        );
        
        assertEquals(
            new PropertyValue(Arrays.asList(new PropertyValue("test"))).hashCode(),
            new PropertyValue(Arrays.asList(new PropertyValue("test"))).hashCode()
        );
        
        Map<String, PropertyValue> map1 = new HashMap<>();
        map1.put("key", new PropertyValue("value"));
        Map<String, PropertyValue> map2 = new HashMap<>();
        map2.put("key", new PropertyValue("value"));
        assertEquals(
            new PropertyValue(map1).hashCode(),
            new PropertyValue(map2).hashCode()
        );

        // Test that different objects have different hash codes
        assertNotEquals(
            new PropertyValue("test1").hashCode(),
            new PropertyValue("test2").hashCode()
        );

        // Test Asset hashCode consistency
        var fileAsset = new PropertyValue(new FileAsset("path"));
        assertEquals(fileAsset.hashCode(), fileAsset.hashCode());

        // Test Archive hashCode consistency
        var fileArchive = new PropertyValue(new FileArchive("path"));
        assertEquals(fileArchive.hashCode(), fileArchive.hashCode());

        // Test Resource hashCode consistency
        var ref = new PropertyValue.ResourceReference("urn", null, "");
        var resource = new PropertyValue(ref);
        assertEquals(resource.hashCode(), resource.hashCode());

        // Test Output hashCode consistency
        Set<String> deps = new HashSet<>(Arrays.asList("dep1"));
        var outputRef = new PropertyValue.OutputReference(new PropertyValue("test"), deps);
        var output = new PropertyValue(outputRef);
        assertEquals(output.hashCode(), output.hashCode());

        // Test Secret hashCode consistency
        var secret = new PropertyValue(new PropertyValue("secret"));
        assertEquals(secret.hashCode(), secret.hashCode());
    }

    @Test
    void testToString() {
        // Test basic types
        assertEquals("null", PropertyValue.NULL.toString());
        assertEquals("true", new PropertyValue(true).toString());
        assertEquals("42.0", new PropertyValue(42.0).toString());
        assertEquals("test", new PropertyValue("test").toString());
        assertEquals("{unknown}", PropertyValue.COMPUTED.toString());
        
        // Test collections
        assertEquals("[1.1,2.2,null]", new PropertyValue(Arrays.asList(
            new PropertyValue(1.1),
            new PropertyValue(2.2),
            PropertyValue.NULL
        )).toString());
        
        Map<String, PropertyValue> map = new HashMap<>();
        map.put("key1", new PropertyValue("value1"));
        map.put("key2", PropertyValue.NULL);
        assertEquals("{key1:value1,key2:null}", new PropertyValue(map).toString());
        
        // Test special types
        assertEquals("secret(test)", new PropertyValue(new PropertyValue("test")).toString());
        
        // Test assets and archives
        var fileAsset = new PropertyValue(new FileAsset("path/to/file"));
        assertEquals(fileAsset.getAssetValue().toString(), fileAsset.toString());

        var fileArchive = new PropertyValue(new FileArchive("path/to/archive"));
        assertEquals(fileArchive.getArchiveValue().toString(), fileArchive.toString());

        // Test resource and output
        var resourceRef = new PropertyValue.ResourceReference(
            "urn:pulumi:stack::project::type::name",
            new PropertyValue("resource-id"),
            "1.0.0"
        );
        assertEquals(resourceRef.toString(), new PropertyValue(resourceRef).toString());

        Set<String> deps = new HashSet<>(Arrays.asList("dep1", "dep2"));
        var outputRef = new PropertyValue.OutputReference(new PropertyValue("test"), deps);
        assertEquals(outputRef.toString(), new PropertyValue(outputRef).toString());
    }
}
