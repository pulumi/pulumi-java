package com.pulumi.provider.internal;

import com.pulumi.asset.Asset;
import com.pulumi.asset.Archive;
import com.pulumi.asset.FileAsset;
import com.pulumi.asset.FileArchive;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.internal.Internal;
import com.pulumi.resources.ResourceArgs;

import com.pulumi.test.internal.PulumiTestInternal;
import com.pulumi.provider.internal.properties.PropertyValue;
import com.pulumi.provider.internal.properties.PropertyValueSerializer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PropertyValueSerializerTest {

    static class BasicArgs extends ResourceArgs {
        @Import(name="intValue", required=true)
        private Output<Integer> intValue;

        @Import(name="doubleValue", required=true)
        private Output<Double> doubleValue;

        @Import(name="boolValue", required=true)
        private Output<Boolean> boolValue;

        @Import(name="stringValue", required=true)
        private Output<String> stringValue;

        @Import(name="numberValue", required=true)
        private Output<Number> numberValue;

        @Import(name="valueWithOverridenName")
        private Output<String> otherStringValue;

        public Output<Integer> intValue() {
            return this.intValue;
        }

        public Output<Double> doubleValue() {
            return this.doubleValue;
        }

        public Output<Boolean> boolValue() {
            return this.boolValue;
        }

        public Output<String> stringValue() {
            return this.stringValue;
        }

        public Output<Number> numberValue() {
            return this.numberValue;
        }

        public Output<String> otherStringValue() {
            return this.otherStringValue;
        }
    
        private BasicArgs() {}
    }

    @Test
    void testDeserializingBasicArgs() {
        var data = object(
            pair("intValue", PropertyValue.of(42)),
            pair("doubleValue", PropertyValue.of(3.14)),
            pair("boolValue", PropertyValue.of(true)),
            pair("stringValue", PropertyValue.of("hello")),
            pair("numberValue", PropertyValue.of(123.456)),
            pair("valueWithOverridenName", PropertyValue.of("other"))
        );

        var basicArgs = PropertyValueSerializer.deserialize(data, BasicArgs.class);
        
        var intValue = Internal.of(basicArgs.intValue()).getValueNullable().join();
        assertThat(intValue).isEqualTo(42);
        
        var doubleValue = Internal.of(basicArgs.doubleValue()).getValueNullable().join();
        assertThat(doubleValue).isEqualTo(3.14);
        
        var boolValue = Internal.of(basicArgs.boolValue()).getValueNullable().join();
        assertThat(boolValue).isTrue();
        
        var stringValue = Internal.of(basicArgs.stringValue()).getValueNullable().join();
        assertThat(stringValue).isEqualTo("hello");
        
        var numberValue = Internal.of(basicArgs.numberValue()).getValueNullable().join();
        assertThat(numberValue).isEqualTo(123.456);

        var otherStringValue = Internal.of(basicArgs.otherStringValue()).getValueNullable().join();
        assertThat(otherStringValue).isEqualTo("other");
    }

    static class SecretArgs extends ResourceArgs {
        @Import(name="password", required=true)
        private Output<String> password;

        @Import(name="tags", required=true)
        private Output<List<String>> tags;

        public Output<String> password() {
            return this.password;
        }

        public Output<List<String>> tags() {
            return this.tags;
        }

        private SecretArgs() {}
    }

    @Test
    void testDeserializingSecretArgsWorks() {
        var data = object(
            pair("password", PropertyValue.ofSecret(PropertyValue.of("PW"))),
            pair("tags", PropertyValue.ofSecret(array(
                PropertyValue.of("secret"),
                PropertyValue.of("sensitive"),
                PropertyValue.of("confidential")
            )))
        );

        var secretArgs = PropertyValueSerializer.deserialize(data, SecretArgs.class);
        
        var passwordData = PulumiTestInternal.extractOutputData(secretArgs.password());
        assertThat(passwordData.isSecret()).isTrue();
        assertThat(passwordData.isKnown()).isTrue();
        assertThat(passwordData.getValueNullable()).isEqualTo("PW");

        var tagsData = PulumiTestInternal.extractOutputData(secretArgs.tags());
        assertThat(tagsData.isSecret()).isTrue();
        assertThat(tagsData.isKnown()).isTrue();
        var tagsList = (List<String>)tagsData.getValueNullable();
        assertThat(tagsList).containsExactly("secret", "sensitive", "confidential");
    }

    static class PlainArgs extends ResourceArgs {
        @Import(name="intValue", required=true)
        private Integer intValue;

        @Import(name="doubleValue", required=true)
        private Double doubleValue;

        @Import(name="boolValue", required=true)
        private Boolean boolValue;

        @Import(name="stringValue", required=true)
        private String stringValue;

        @Import(name="numberValue", required=true)
        private Number numberValue;

        public Integer intValue() {
            return this.intValue;
        }

        public Double doubleValue() {
            return this.doubleValue;
        }

        public Boolean boolValue() {
            return this.boolValue;
        }

        public String stringValue() {
            return this.stringValue;
        }

        public Number numberValue() {
            return this.numberValue;
        }
    
        private PlainArgs() {}
    }

    @Test
    void testDeserializingPlainArgsWorks() {
        var data = object(
            pair("intValue", PropertyValue.of(42)),
            pair("doubleValue", PropertyValue.of(3.14)),
            pair("boolValue", PropertyValue.of(true)),
            pair("stringValue", PropertyValue.of("hello")),
            pair("numberValue", PropertyValue.of(123.456))
        );

        var plainArgs = PropertyValueSerializer.deserialize(data, PlainArgs.class);
        
        assertThat(plainArgs.intValue()).isEqualTo(42);
        assertThat(plainArgs.doubleValue()).isEqualTo(3.14);
        assertThat(plainArgs.boolValue()).isTrue();
        assertThat(plainArgs.stringValue()).isEqualTo("hello");
        assertThat(plainArgs.numberValue()).isEqualTo(123.456);
    }

    static class UsingListArgs extends ResourceArgs {
        @Import(name="first")
        private Output<List<String>> first;

        @Import(name="second")
        private Output<Set<String>> second;

        public Output<List<String>> first() {
            return this.first;
        }

        public Output<Set<String>> second() {
            return this.second;
        }

        private UsingListArgs() {}
    }

    @Test
    void testDeserializingListTypesWorks() {
        var array = array(
            PropertyValue.of("one"),
            PropertyValue.of("two"),
            PropertyValue.of("three"));

        var data = object(
            pair("first", array),
            pair("second", array));

        var args = PropertyValueSerializer.deserialize(data, UsingListArgs.class);

        var firstData = PulumiTestInternal.extractOutputData(args.first());
        var secondData = PulumiTestInternal.extractOutputData(args.second());

        var elements = Arrays.asList("one", "two", "three");
        assertThat(firstData.getValueNullable()).isEqualTo(elements);
        assertThat(secondData.getValueNullable()).isEqualTo(elements);
    }

    static class RequiredIntArgs extends ResourceArgs {
        @Import(name="property", required=true)
        private Output<Integer> property;

        public Output<Integer> property() {
            return this.property;
        }

        private RequiredIntArgs() {}
    }

    @Test
    void testDeserializingEmptyValuesIntoRequiredPropertyShouldFail() {
        var emptyObject = object();

        assertThatThrownBy(() -> PropertyValueSerializer.deserialize(emptyObject, RequiredIntArgs.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Missing required field property");
    }

    enum TestEnum { Allow, Default }
    static class EnumArgs extends ResourceArgs {
        @Import(name="enumInput")
        private Output<TestEnum> enumInput;

        public Output<TestEnum> enumInput() {
            return this.enumInput;
        }

        private EnumArgs() {}
    }

    @Test
    void testDeserializingEnumWorks() {
        var data = object(pair("enumInput", PropertyValue.of(1)));
        var args = PropertyValueSerializer.deserialize(data, EnumArgs.class);
        var enumValue = PulumiTestInternal.extractOutputData(args.enumInput());
        assertThat(enumValue.getValueNullable()).isEqualTo(TestEnum.Default);
    }

    static class StringArgs extends ResourceArgs {
        @Import(name="stringValue")
        private Output<String> stringValue;

        public Output<String> stringValue() {
            return this.stringValue;
        }

        private StringArgs() {}
    }

    @Test
    void testDeserializingUnknownInputsWorks() {
        var data = object(pair("stringValue", PropertyValue.COMPUTED));
        var deserialized = PropertyValueSerializer.deserialize(data, StringArgs.class);
        var outputData = PulumiTestInternal.extractOutputData(deserialized.stringValue());
        assertThat(outputData.getValueNullable()).isNull();
        assertThat(outputData.isSecret()).isFalse();
        assertThat(outputData.isKnown()).isFalse();
    }

    static class NestedArgs extends ResourceArgs {
        @Import(name="name")
        private Output<String> name;

        @Import(name="basic")
        private Output<BasicArgs> basic;

        @Import(name="children")
        private Output<List<NestedArgs>> children;

        public Output<String> name() {
            return this.name;
        }

        public Output<BasicArgs> basic() {
            return this.basic;
        }

        public Output<List<NestedArgs>> children() {
            return this.children;
        }

        private NestedArgs() {}
    }

    @Test
    void testDeserializingNestedArgsWorks() {
        var basicData = object(
            pair("intValue", PropertyValue.of(42)),
            pair("doubleValue", PropertyValue.of(3.14)),
            pair("boolValue", PropertyValue.of(true)),
            pair("stringValue", PropertyValue.of("hello")),
            pair("numberValue", PropertyValue.of(123.456))
        );

        var childData = object(
            pair("name", PropertyValue.of("child"))
        );

        var nestedData = object(
            pair("name", PropertyValue.of("parent")),
            pair("basic", basicData),
            pair("children", PropertyValue.of(Arrays.asList(childData)))
        );

        var args = PropertyValueSerializer.deserialize(nestedData, NestedArgs.class);

        // Verify name
        var nameData = PulumiTestInternal.extractOutputData(args.name());
        assertThat(nameData.getValueNullable()).isEqualTo("parent");

        // Verify basic args
        var basicArgs = Internal.of(args.basic()).getValueNullable().join();
        var basicIntValue = Internal.of(basicArgs.intValue()).getValueNullable().join();
        assertThat(basicIntValue).isEqualTo(42);

        // Verify children
        var childrenData = PulumiTestInternal.extractOutputData(args.children());
        var children = (List<NestedArgs>)childrenData.getValueNullable();
        assertThat(children).hasSize(1);
        var childNameData = PulumiTestInternal.extractOutputData(children.get(0).name());
        assertThat(childNameData.getValueNullable()).isEqualTo("child");
    }

    static class AssetArgs extends ResourceArgs {
        @Import(name="file")
        private Output<Asset> file;

        @Import(name="archive")
        private Output<Archive> archive;

        public Output<Asset> file() {
            return this.file;
        }

        public Output<Archive> archive() {
            return this.archive;
        }

        private AssetArgs() {}
    }

    @Test
    void testDeserializingAssetAndArchiveWorks() {
        var fileAsset = new FileAsset("path/to/file.txt");
        var fileArchive = new FileArchive("path/to/archive.zip");

        var data = object(
            pair("file", PropertyValue.of(fileAsset)),
            pair("archive", PropertyValue.of(fileArchive))
        );

        var args = PropertyValueSerializer.deserialize(data, AssetArgs.class);
        
        var fileData = PulumiTestInternal.extractOutputData(args.file());
        assertThat(fileData.isKnown()).isTrue();
        assertThat(fileData.getValueNullable()).isInstanceOf(FileAsset.class);
        var asset = (FileAsset)fileData.getValueNullable();
        assertThat(asset).isEqualTo(fileAsset);

        var archiveData = PulumiTestInternal.extractOutputData(args.archive());
        assertThat(archiveData.isKnown()).isTrue();
        assertThat(archiveData.getValueNullable()).isInstanceOf(FileArchive.class);
        var archive = (FileArchive)archiveData.getValueNullable();
        assertThat(archive).isEqualTo(fileArchive);
    }

    @SafeVarargs
    private static PropertyValue object(Map.Entry<String, PropertyValue>... pairs) {
        var builder = new HashMap<String, PropertyValue>();
        for (var pair : pairs) {
            builder.put(pair.getKey(), pair.getValue());
        }
        return PropertyValue.of(builder);
    }

    private PropertyValue array(PropertyValue... values) {
        return PropertyValue.of(Arrays.asList(values));
    }

    private Map.Entry<String, PropertyValue> pair(String key, PropertyValue value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }
}
