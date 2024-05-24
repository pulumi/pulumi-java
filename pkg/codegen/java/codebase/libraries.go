package codebase

func isImplicitlyImportedPackage(pkg string) bool {
	if pkg == "java.lang" {
		return true
	}

	return false
}

var JString = NewSymbol("java.lang", "String")

var JList = NewSymbol("java.util", "List")

var JArrayList = NewSymbol("java.util", "ArrayList")

var JMap = NewSymbol("java.util", "Map")

var JFiles = NewSymbol("java.nio.file", "Files")

var JPaths = NewSymbol("java.nio.file", "Paths")

var JPulumi = NewSymbol("com.pulumi", "Pulumi")

var JPulumiContext = NewSymbol("com.pulumi", "Context")

var JPulumiFileArchive = NewSymbol("com.pulumi.asset", "FileArchive")

var JPulumiFileAsset = NewSymbol("com.pulumi.asset", "FileAsset")

var JPulumiStringAsset = NewSymbol("com.pulumi.asset", "StringAsset")

var JPulumiOutput = NewSymbol("com.pulumi.core", "Output")

var JPulumiDeployment = NewSymbol("com.pulumi.deployment", "Deployment")

var JPulumiCustomResourceOptions = NewSymbol("com.pulumi.resources", "CustomResourceOptions")

var JPulumiFiles = NewSymbol("com.pulumi.codegen.internal", "Files")

var JPulumiKeyedValue = NewSymbol("com.pulumi.codegen.internal", "KeyedValue")

var JPulumiSerialization = NewSymbol("com.pulumi.codegen.internal", "Serialization")
