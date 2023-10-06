package names

var JavaUtil = Ident("java").FQN().Dot("util")

var JavaLang = Ident("java").FQN().Dot("lang")

var Optional = JavaUtil.Dot("Optional")

var Pulumi = Ident("com").FQN().Dot("pulumi")

var PulumiExceptions = Pulumi.Dot("exceptions")

var PulumiMissingRequiredPropertyException = PulumiExceptions.Dot("MissingRequiredPropertyException")

var PulumiCore = Pulumi.Dot("core")

var PulumiAsset = Pulumi.Dot("asset")

var PulumiInternal = PulumiCore.Dot("internal")

var PulumiAnnotations = PulumiCore.Dot("annotations")

var ResourceType = PulumiAnnotations.Dot("ResourceType")

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

var Archive = PulumiAsset.Dot("Archive")

var AssetOrArchive = PulumiAsset.Dot("AssetOrArchive")

var Either = PulumiCore.Dot("Either")

var Nullable = Ident("javax").FQN().Dot("annotation").Dot("Nullable")

var Objects = JavaUtil.Dot("Objects")

var CustomType = PulumiAnnotations.Dot("CustomType")

var Import = PulumiAnnotations.Dot("Import")

var Export = PulumiAnnotations.Dot("Export")

var EnumType = PulumiAnnotations.Dot("EnumType")

var Alias = PulumiCore.Dot("Alias")

var PulumiDeployment = Pulumi.Dot("deployment")

var InvokeArgs = Pulumi.Dot("resources").Dot("InvokeArgs")

var Deployment = PulumiDeployment.Dot("Deployment")

var InvokeOptions = PulumiDeployment.Dot("InvokeOptions")

var CompletableFuture = JavaUtil.Dot("concurrent").Dot("CompletableFuture")

var TypeShape = PulumiCore.Dot("TypeShape")

var StringJoiner = JavaUtil.Dot("StringJoiner")

var Optionals = PulumiInternal.Dot("Optionals")

var Void = JavaLang.Dot("Void")

var Codegen = PulumiInternal.Dot("Codegen")
