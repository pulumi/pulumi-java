package names

var JavaUtil = Ident("java").FQN().Dot("util")

var JavaLang = Ident("java").FQN().Dot("lang")

var Optional = JavaUtil.Dot("Optional")

var Pulumi = Ident("io").FQN().Dot("pulumi")

var PulumiCore = Pulumi.Dot("core")

var PulumiInternal = PulumiCore.Dot("internal")

var PulumiAnnotations = PulumiInternal.Dot("annotations")

var ResourceType = PulumiAnnotations.Dot("ResourceType")

var Input = PulumiCore.Dot("Input")

var Output = PulumiCore.Dot("Output")

var List = JavaUtil.Dot("List")

var Map = JavaUtil.Dot("Map")

var String = JavaLang.Dot("String")

var Boolean = JavaLang.Dot("Boolean")

var Integer = JavaLang.Dot("Integer")

var Double = JavaLang.Dot("Double")

var Object = JavaLang.Dot("Object")

var GSON = Ident("com").FQN().Dot("google").Dot("gson")

var JSONElement = GSON.Dot("JsonElement")

var Archive = PulumiCore.Dot("Archive")

var AssetOrArchive = PulumiCore.Dot("AssetOrArchive")

var Either = PulumiCore.Dot("Either")

var Nullable = Ident("javax").FQN().Dot("annotation").Dot("Nullable")

var Objects = JavaUtil.Dot("Objects")

var OutputCustomType = PulumiAnnotations.Dot("OutputCustomType")

var InputImport = PulumiAnnotations.Dot("InputImport")

var OutputExport = PulumiAnnotations.Dot("OutputExport")

var EnumType = PulumiAnnotations.Dot("EnumType")

var Alias = PulumiCore.Dot("Alias")

var PulumiDeployment = Pulumi.Dot("deployment")

var Deployment = PulumiDeployment.Dot("Deployment")

var InvokeOptions = PulumiDeployment.Dot("InvokeOptions")

var CompletableFuture = JavaUtil.Dot("concurrent").Dot("CompletableFuture")

var TypeShape = PulumiInternal.Dot("Reflection").Dot("TypeShape")

var StringJoiner = JavaUtil.Dot("StringJoiner")

var Optionals = PulumiInternal.Dot("Optionals")
