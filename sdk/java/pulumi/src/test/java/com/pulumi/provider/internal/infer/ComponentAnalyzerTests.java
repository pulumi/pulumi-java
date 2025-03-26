package com.pulumi.provider.internal.infer;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.Import;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.provider.internal.schema.PackageSpec;
import com.pulumi.provider.internal.schema.ResourceSpec;
import com.pulumi.provider.internal.schema.PropertySpec;
import com.pulumi.provider.internal.schema.TypeSpec;
import com.pulumi.provider.internal.Metadata;
import com.pulumi.provider.internal.schema.ComplexTypeSpec;
import com.pulumi.asset.Archive;
import com.pulumi.asset.Asset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ComponentAnalyzerTests {
    @Test
    void testGenerateSchemaWithoutMetadata() {
        var schema = ComponentAnalyzer.generateSchema(SelfSignedCertificate.class);

        // Package name should be "infer" from the package "com.pulumi.provider.internal.infer"
        var expected = new PackageSpec()
            .setName("infer")
            .setDisplayName("infer")
            .setVersion(null)
            .setNamespace("pulumi");

        // Set up language settings
        Map<String, Object> languageSettings = new HashMap<>();
        languageSettings.put("respectSchemaVersion", true);
        expected.getLanguage().put("nodejs", languageSettings);
        expected.getLanguage().put("python", languageSettings);
        expected.getLanguage().put("csharp", languageSettings);
        expected.getLanguage().put("java", languageSettings);
        expected.getLanguage().put("go", languageSettings);

        // Add the component schema
        expected.setResources(Map.of(
            "infer:index:SelfSignedCertificate",
            new ResourceSpec(
                Map.of(
                    "algorithm", PropertySpec.ofBuiltin("string"),
                    "ecdsaCurve", PropertySpec.ofBuiltin("string"),
                    "bits", PropertySpec.ofBuiltin("integer")
                ),
                Set.of("algorithm"),
                Map.of(
                    "pem", PropertySpec.ofBuiltin("string"),
                    "privateKey", PropertySpec.ofBuiltin("string"),
                    "caCert", PropertySpec.ofBuiltin("string")
                ),
                Set.of()
            )
        ));

        assertEquals(expected, schema);
    }

    @Test
    void testGenerateSchemaWithNoClasses() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ComponentAnalyzer.generateSchema()
        );

        assertEquals(
            "At least one component class must be provided",
            exception.getMessage()
        );
    }

    private Metadata metadata = new Metadata("my-component", "0.0.1", "Test package");

    private PackageSpec createBasePackageSpec() {
        Map<String, Object> languageSettings = new HashMap<>();
        languageSettings.put("respectSchemaVersion", true);
        Map<String, Map<String, Object>> language = new HashMap<>();
        for (String lang : new String[]{"nodejs", "python", "csharp", "java", "go"}) {
            language.put(lang, languageSettings);
        }

        return new PackageSpec()
            .setName("my-component")
            .setVersion("0.0.1")
            .setDisplayName("Test package")
            .setLanguage(language);
    }

    public static class SelfSignedCertificateArgs extends ResourceArgs {
        @Import(required = true)
        private Output<String> algorithm;

        @Import(required = false)
        private Output<String> ecdsaCurve;

        @Import
        private Output<Integer> bits;
    }

    public static class SelfSignedCertificate extends ComponentResource {
        @Export private Output<String> pem;
        @Export private Output<String> privateKey;
        @Export private Output<String> caCert;

        public SelfSignedCertificate(SelfSignedCertificateArgs args) {
            super("my-component:index:SelfSignedCertificate", "test");
        }
    }

    @Test
    void testAnalyzeComponent() {
        var schema = ComponentAnalyzer.generateSchema(metadata, SelfSignedCertificate.class);

        var expected = createBasePackageSpec()
            .setResources(Map.of(
                "my-component:index:SelfSignedCertificate",
                new ResourceSpec(
                    Map.of(
                        "algorithm", PropertySpec.ofBuiltin("string"),
                        "ecdsaCurve", PropertySpec.ofBuiltin("string"),
                        "bits", PropertySpec.ofBuiltin("integer")
                    ),
                    Set.of("algorithm"),
                    Map.of(
                        "pem", PropertySpec.ofBuiltin("string"),
                        "privateKey", PropertySpec.ofBuiltin("string"),
                        "caCert", PropertySpec.ofBuiltin("string")
                    ),
                    Set.of()
                )
            ));

        assertEquals(expected, schema);
    }

    public static class NoArgsComponent extends ComponentResource {
        public NoArgsComponent() {
            super("my-component:index:NoArgsComponent", "test");
        }
    }

    @Test
    void testAnalyzeComponentNoArgs() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ComponentAnalyzer.generateSchema(metadata, NoArgsComponent.class)
        );

        assertEquals(
            "Component " + NoArgsComponent.class.getName() +
            " must have exactly one constructor parameter that extends ResourceArgs",
            exception.getMessage()
        );
    }

    public static class EmptyArgs extends ResourceArgs {
        // Empty args class
    }

    public static class EmptyComponent extends ComponentResource {
        public EmptyComponent(EmptyArgs args) {
            super("my-component:index:EmptyComponent", "test");
        }
    }

    @Test
    void testAnalyzeComponentEmpty() {
        var schema = ComponentAnalyzer.generateSchema(metadata, EmptyComponent.class);

        var expected = createBasePackageSpec()
            .setResources(Map.of(
                "my-component:index:EmptyComponent",
                new ResourceSpec(Map.of(), Set.of(), Map.of(), Set.of())
            ));

        assertEquals(expected, schema);
    }

    public static class ComplexTypeArgs extends ResourceArgs {
        @Import(required = false)
        private Output<List<String>> aInputListStr;

        @Import(required = true)
        private String aStr;

        public Output<List<String>> aInputListStr() {
            return aInputListStr;
        }

        public String aStr() {
            return aStr;
        }
    }

    public static class ComplexOutputType {
        @Export private Output<List<String>> anOutputListStr;
        @Export private String aStr;
    }

    // Add component args class with all types
    public static class PlainTypesArgs extends ResourceArgs {
        @Import(required = true)
        private Integer aInt;

        @Import(required = true)
        private String aStr;

        @Import(required = true)
        private Double aFloat;

        @Import(required = true)
        private Boolean aBool;

        @Import(required = false)
        private String aOptional;

        @Import(required = true)
        private List<String> aList;

        @Import(required = true)
        private Output<List<String>> aInputList;

        @Import(required = true)
        private List<Output<String>> aListInput;

        @Import(required = true)
        private Output<List<Output<String>>> aInputListInput;

        @Import(required = true)
        private Map<String, Integer> aDict;

        @Import(required = true)
        private Map<String, Output<Integer>> aDictInput;

        @Import(required = true)
        private Output<Map<String, Integer>> aInputDict;

        @Import(required = true)
        private Output<Map<String, Output<Integer>>> aInputDictInput;

        @Import(required = true)
        private ComplexTypeArgs aComplexType;

        @Import(required = true)
        private Output<ComplexTypeArgs> aInputComplexType;
    }

    public static class PlainTypesComponent extends ComponentResource {
        @Export
        private Integer aInt;

        @Export
        private String aStr;

        @Export
        private Double aFloat;

        @Export
        private Boolean aBool;

        @Export
        private Output<List<String>> aOutputList;

        @Export
        private Output<ComplexOutputType> aOutputComplex;

        public PlainTypesComponent(PlainTypesArgs args) {
            super("my-component:index:PlainTypesComponent", "test");
        }
    }

    @Test
    void testAnalyzeComponentPlainTypes() {
        var schema = ComponentAnalyzer.generateSchema(metadata, PlainTypesComponent.class);

        Map<String, PropertySpec> inputs = new HashMap<>();
        // Basic types
        inputs.put("aInt", PropertySpec.ofBuiltin("integer", true));
        inputs.put("aStr", PropertySpec.ofBuiltin("string", true));
        inputs.put("aFloat", PropertySpec.ofBuiltin("number", true));
        inputs.put("aBool", PropertySpec.ofBuiltin("boolean", true));
        inputs.put("aOptional", PropertySpec.ofBuiltin("string", true));
        // Lists
        inputs.put("aList", PropertySpec.ofArray(TypeSpec.ofBuiltin("string", true)));
        inputs.put("aInputList", PropertySpec.ofArray(TypeSpec.ofBuiltin("string")));
        inputs.put("aListInput", PropertySpec.ofArray(TypeSpec.ofBuiltin("string")));
        inputs.put("aInputListInput", PropertySpec.ofArray(TypeSpec.ofBuiltin("string")));
        // Maps
        inputs.put("aDict", PropertySpec.ofDict(TypeSpec.ofBuiltin("integer", true)));
        inputs.put("aDictInput", PropertySpec.ofDict(TypeSpec.ofBuiltin("integer")));
        inputs.put("aInputDict", PropertySpec.ofDict(TypeSpec.ofBuiltin("integer")));
        inputs.put("aInputDictInput", PropertySpec.ofDict(TypeSpec.ofBuiltin("integer")));
        // Complex types
        inputs.put("aComplexType", PropertySpec.ofRef("#/types/my-component:index:ComplexType", true));
        inputs.put("aInputComplexType", PropertySpec.ofRef("#/types/my-component:index:ComplexType", null));

        var expected = createBasePackageSpec()
            .setResources(Map.of(
                "my-component:index:PlainTypesComponent",
                new ResourceSpec(
                    inputs,
                    Set.of("aInt", "aStr", "aFloat", "aBool", "aList", "aInputList", "aListInput",
                          "aInputListInput", "aDict", "aDictInput", "aInputDict", "aInputDictInput",
                          "aComplexType", "aInputComplexType"),
                    Map.of(
                        "aInt", PropertySpec.ofBuiltin("integer", true),
                        "aStr", PropertySpec.ofBuiltin("string", true),
                        "aFloat", PropertySpec.ofBuiltin("number", true),
                        "aBool", PropertySpec.ofBuiltin("boolean", true),
                        "aOutputList", PropertySpec.ofArray(TypeSpec.ofBuiltin("string", null)),
                        "aOutputComplex", PropertySpec.ofRef("#/types/my-component:index:ComplexOutputType", null)
                    ),
                    Set.of()
                )
            ))
            .setTypes(Map.of(
                "my-component:index:ComplexType",
                ComplexTypeSpec.ofObject(
                    Map.of(
                        "aInputListStr", PropertySpec.ofArray(TypeSpec.ofBuiltin("string")),
                        "aStr", PropertySpec.ofBuiltin("string", true)
                    ),
                    Set.of("aStr")
                ),
                "my-component:index:ComplexOutputType",
                ComplexTypeSpec.ofObject(
                    Map.of(
                        "anOutputListStr", PropertySpec.ofArray(TypeSpec.ofBuiltin("string")),
                        "aStr", PropertySpec.ofBuiltin("string", true)
                    ),
                    Set.of()
                )
            ));

        assertEquals(expected, schema);
    }

    public static class ListTypesArgs extends ResourceArgs {
        @Import(required = true)
        private Output<List<String>> requiredList;

        @Import(required = false)
        private Output<List<String>> optionalList;

        @Import(required = true)
        private Output<List<ComplexListTypeArgs>> complexList;
    }

    public static class ComplexListTypeArgs extends ResourceArgs {
        @Import(required = false)
        private Output<List<String>> name;
    }

    public static class ComplexListType {
        @Export
        private Output<List<String>> name;
    }

    public static class ListTypesComponent extends ComponentResource {
        @Export
        private Output<List<String>> simpleList;

        @Export
        private Output<List<ComplexListType>> complexList;

        public ListTypesComponent(ListTypesArgs args) {
            super("my-component:index:ListTypesComponent", "test");
        }
    }

    @Test
    void testAnalyzeList() {
        var schema = ComponentAnalyzer.generateSchema(metadata, ListTypesComponent.class);

        var expected = createBasePackageSpec()
            .setResources(Map.of(
                "my-component:index:ListTypesComponent",
                new ResourceSpec(
                    Map.of(
                        "requiredList", PropertySpec.ofArray(TypeSpec.ofBuiltin("string")),
                        "optionalList", PropertySpec.ofArray(TypeSpec.ofBuiltin("string")),
                        "complexList", PropertySpec.ofArray(
                            TypeSpec.ofRef("#/types/my-component:index:ComplexListType")
                        )
                    ),
                    Set.of("requiredList", "complexList"),
                    Map.of(
                        "simpleList", PropertySpec.ofArray(TypeSpec.ofBuiltin("string")),
                        "complexList", PropertySpec.ofArray(
                            TypeSpec.ofRef("#/types/my-component:index:ComplexListType")
                        )
                    ),
                    Set.of()
                )
            ))
            .setTypes(Map.of(
                "my-component:index:ComplexListType",
                ComplexTypeSpec.ofObject(
                    Map.of("name", PropertySpec.ofArray(
                        TypeSpec.ofBuiltin("string")
                    )),
                    Set.of()
                )
            ));

        assertEquals(expected, schema);
    }

    public static class NonStringMapKeyArgs extends ResourceArgs {
        @Import(required = true)
        private Map<Integer, String> badDict;
    }

    public static class NonStringMapKeyComponent extends ComponentResource {
        public NonStringMapKeyComponent(NonStringMapKeyArgs args) {
            super("my-component:index:NonStringMapKeyComponent", "test");
        }
    }

    @Test
    void testAnalyzeMapNonStringKey() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ComponentAnalyzer.generateSchema(metadata, NonStringMapKeyComponent.class)
        );

        assertEquals(
            "map keys must be strings, got 'Integer' for 'NonStringMapKeyArgs.badDict'",
            exception.getMessage()
        );
    }

    public static class ComplexDictType {
        @Export
        private Output<Map<String, Integer>> name;
    }

    public static class ComplexDictTypeArgs extends ResourceArgs {
        @Import(required = false)
        private Output<Map<String, Integer>> name;
    }

    public static class DictTypesArgs extends ResourceArgs {
        @Import(required = true)
        private Output<Map<String, Integer>> dictInput;

        @Import(required = true)
        private Output<Map<String, ComplexDictTypeArgs>> complexDictInput;
    }

    public static class DictTypesComponent extends ComponentResource {
        @Export
        private Output<Map<String, Integer>> dictOutput;

        @Export
        private Output<Map<String, ComplexDictType>> complexDictOutput;

        public DictTypesComponent(DictTypesArgs args) {
            super("my-component:index:DictTypesComponent", "test");
        }
    }

    @Test
    void testAnalyzeDict() {
        var schema = ComponentAnalyzer.generateSchema(metadata, DictTypesComponent.class);

        var expected = createBasePackageSpec()
            .setResources(Map.of(
                "my-component:index:DictTypesComponent",
                new ResourceSpec(
                    Map.of(
                        "dictInput", PropertySpec.ofDict(TypeSpec.ofBuiltin("integer")),
                        "complexDictInput", PropertySpec.ofDict(TypeSpec.ofRef("#/types/my-component:index:ComplexDictType", null))
                    ),
                    Set.of("dictInput", "complexDictInput"),
                    Map.of(
                        "dictOutput", PropertySpec.ofDict(TypeSpec.ofBuiltin("integer")),
                        "complexDictOutput", PropertySpec.ofDict(TypeSpec.ofRef("#/types/my-component:index:ComplexDictType", null))
                    ),
                    Set.of()
                )
            ))
            .setTypes(Map.of(
                "my-component:index:ComplexDictType",
                ComplexTypeSpec.ofObject(
                    Map.of("name", PropertySpec.ofDict(TypeSpec.ofBuiltin("integer"))),
                    Set.of()
                )
            ));

        assertEquals(expected, schema);
    }

    // Add these new test classes after the existing test classes
    public static class ComplexTypeWithRequiredAndOptionalArgs extends ResourceArgs {
        @Import(required = true)
        private Output<String> value;

        @Import(required = false)
        private Output<Integer> optionalValue;
    }

    public static class ComplexTypeWithRequiredAndOptional {
        @Export
        private Output<String> value;

        @Export
        private Output<Integer> optionalValue;
    }

    public static class ComplexTypeComponentArgs extends ResourceArgs {
        @Import(required = true)
        private Output<ComplexTypeWithRequiredAndOptionalArgs> someComplexType;
    }

    public static class ComplexTypeComponent extends ComponentResource {
        @Export
        private Output<ComplexTypeWithRequiredAndOptional> complexOutput;

        public ComplexTypeComponent(ComplexTypeComponentArgs args) {
            super("my-component:index:ComplexTypeComponent", "test");
        }
    }

    @Test
    void testAnalyzeComponentComplexType() {
        var schema = ComponentAnalyzer.generateSchema(metadata, ComplexTypeComponent.class);

        var expected = createBasePackageSpec()
            .setResources(Map.of(
                "my-component:index:ComplexTypeComponent",
                new ResourceSpec(
                    Map.of(
                        "someComplexType", PropertySpec.ofRef("#/types/my-component:index:ComplexTypeWithRequiredAndOptional")
                    ),
                    Set.of("someComplexType"),
                    Map.of(
                        "complexOutput", PropertySpec.ofRef("#/types/my-component:index:ComplexTypeWithRequiredAndOptional")
                    ),
                    Set.of()
                )
            ))
            .setTypes(Map.of(
                "my-component:index:ComplexTypeWithRequiredAndOptional",
                ComplexTypeSpec.ofObject(
                    Map.of(
                        "value", PropertySpec.ofBuiltin("string"),
                        "optionalValue", PropertySpec.ofBuiltin("integer")
                    ),
                    Set.of("value")
                )
            ));

        assertEquals(expected, schema);
    }

    public static class ArchiveComponentArgs extends ResourceArgs {
        @Import(required = true)
        private Output<Archive> inputArchive;
    }

    public static class ArchiveComponent extends ComponentResource {
        @Export
        private Output<Archive> outputArchive;

        public ArchiveComponent(ArchiveComponentArgs args) {
            super("my-component:index:ArchiveComponent", "test");
        }
    }

    @Test
    void testAnalyzeArchive() {
        var schema = ComponentAnalyzer.generateSchema(metadata, ArchiveComponent.class);

        var expected = createBasePackageSpec()
            .setResources(Map.of(
                "my-component:index:ArchiveComponent",
                new ResourceSpec(
                    Map.of(
                        "inputArchive", PropertySpec.ofRef("pulumi.json#/Archive")
                    ),
                    Set.of("inputArchive"),
                    Map.of(
                        "outputArchive", PropertySpec.ofRef("pulumi.json#/Archive")
                    ),
                    Set.of()
                )
            ));

        assertEquals(expected, schema);
    }

    public static class AssetComponentArgs extends ResourceArgs {
        @Import(required = true)
        private Output<Asset> inputAsset;
    }

    public static class AssetComponent extends ComponentResource {
        @Export
        private Output<Asset> outputAsset;

        public AssetComponent(AssetComponentArgs args) {
            super("my-component:index:AssetComponent", "test");
        }
    }

    @Test
    void testAnalyzeAsset() {
        var schema = ComponentAnalyzer.generateSchema(metadata, AssetComponent.class);

        var expected = createBasePackageSpec()
            .setResources(Map.of(
                "my-component:index:AssetComponent",
                new ResourceSpec(
                    Map.of(
                        "inputAsset", PropertySpec.ofRef("pulumi.json#/Asset")
                    ),
                    Set.of("inputAsset"),
                    Map.of(
                        "outputAsset", PropertySpec.ofRef("pulumi.json#/Asset")
                    ),
                    Set.of()
                )
            ));

        assertEquals(expected, schema);
    }

    public static class MyResource extends CustomResource {
        public MyResource(String name) {
            super("my:index:MyResource", name, ResourceArgs.Empty, null);
        }
    }

    public static class ResourceRefComponentArgs extends ResourceArgs {
        @Import(required = true)
        private Output<MyResource> password;
    }

    public static class ResourceRefComponent extends ComponentResource {
        public ResourceRefComponent(ResourceRefComponentArgs args) {
            super("my-component:index:ResourceRefComponent", "test");
        }
    }

    @Test
    void testAnalyzeResourceRef() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ComponentAnalyzer.generateSchema(metadata, ResourceRefComponent.class)
        );

        assertEquals(
            "Resource references are not supported yet: found type 'MyResource' for 'ResourceRefComponentArgs.password'",
            exception.getMessage()
        );
    }

    public static class RecursiveTypeArgs extends ResourceArgs {
        @Import(required = false)
        private Output<RecursiveTypeArgs> rec;
    }

    public static class RecursiveType {
        @Export
        private Output<RecursiveType> rec;
    }

    public static class RecursiveComponentArgs extends ResourceArgs {
        @Import(required = true)
        private Output<RecursiveTypeArgs> rec;
    }

    public static class RecursiveComponent extends ComponentResource {
        @Export
        private Output<RecursiveType> rec;

        public RecursiveComponent(RecursiveComponentArgs args) {
            super("my-component:index:RecursiveComponent", "test");
        }
    }

    @Test
    void testAnalyzeComponentSelfRecursiveComplexType() {
        var schema = ComponentAnalyzer.generateSchema(metadata, RecursiveComponent.class);

        var expected = createBasePackageSpec()
            .setResources(Map.of(
                "my-component:index:RecursiveComponent",
                new ResourceSpec(
                    Map.of(
                        "rec", PropertySpec.ofRef("#/types/my-component:index:RecursiveType")
                    ),
                    Set.of("rec"),
                    Map.of(
                        "rec", PropertySpec.ofRef("#/types/my-component:index:RecursiveType")
                    ),
                    Set.of()
                )
            ))
            .setTypes(Map.of(
                "my-component:index:RecursiveType",
                ComplexTypeSpec.ofObject(
                    Map.of(
                        "rec", PropertySpec.ofRef("#/types/my-component:index:RecursiveType")
                    ),
                    Set.of()
                )
            ));

        assertEquals(expected, schema);
    }

    public static class RecursiveTypeAArgs extends ResourceArgs {
        @Import(required = false)
        private Output<RecursiveTypeBArgs> b;
    }

    public static class RecursiveTypeBArgs extends ResourceArgs {
        @Import(required = false)
        private Output<RecursiveTypeAArgs> a;
    }

    public static class RecursiveTypeA {
        @Export
        private Output<RecursiveTypeB> b;
    }

    public static class RecursiveTypeB {
        @Export
        private Output<RecursiveTypeA> a;
    }

    public static class MutuallyRecursiveComponentArgs extends ResourceArgs {
        @Import(required = true)
        private Output<RecursiveTypeAArgs> rec;
    }

    public static class MutuallyRecursiveComponent extends ComponentResource {
        @Export
        private Output<RecursiveTypeB> rec;

        public MutuallyRecursiveComponent(MutuallyRecursiveComponentArgs args) {
            super("my-component:index:MutuallyRecursiveComponent", "test");
        }
    }

    @Test
    void testAnalyzeComponentMutuallyRecursiveComplexTypes() {
        var schema = ComponentAnalyzer.generateSchema(metadata, MutuallyRecursiveComponent.class);

        var expected = createBasePackageSpec()
            .setResources(Map.of(
                "my-component:index:MutuallyRecursiveComponent",
                new ResourceSpec(
                    Map.of(
                        "rec", PropertySpec.ofRef("#/types/my-component:index:RecursiveTypeA")
                    ),
                    Set.of("rec"),
                    Map.of(
                        "rec", PropertySpec.ofRef("#/types/my-component:index:RecursiveTypeB")
                    ),
                    Set.of()
                )
            ))
            .setTypes(Map.of(
                "my-component:index:RecursiveTypeA",
                ComplexTypeSpec.ofObject(
                    Map.of(
                        "b", PropertySpec.ofRef("#/types/my-component:index:RecursiveTypeB")
                    ),
                    Set.of()  // b is optional
                ),
                "my-component:index:RecursiveTypeB",
                ComplexTypeSpec.ofObject(
                    Map.of(
                        "a", PropertySpec.ofRef("#/types/my-component:index:RecursiveTypeA")
                    ),
                    Set.of()  // a is optional
                )
            ));

        assertEquals(expected, schema);
    }
}
