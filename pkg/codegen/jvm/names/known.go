package names

var JavaUtil = Ident("java").FQN().Dot("util")

var JavaLang = Ident("java").FQN().Dot("lang")

var Optional = JavaUtil.Dot("Optional")

var Pulumi = Ident("io").FQN().Dot("pulumi")

var PulumiCore = Pulumi.Dot("core")

var PulumiAnnotations = PulumiCore.Dot("internal").Dot("annotations")

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

var JsonElement = GSON.Dot("JsonElement")

var Archive = PulumiCore.Dot("Archive")

var AssetOrArchive = PulumiCore.Dot("AssetOrArchive")

var Either = PulumiCore.Dot("Either")
