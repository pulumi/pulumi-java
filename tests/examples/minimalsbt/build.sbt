organization := "com.pulumi.example"
name := "minimalsbt"
version := "1.0-SNAPSHOT"
Compile / mainClass := Some(s"{organization.value}.{name.value}.App")
libraryDependencies += "com.pulumi" % "pulumi" % sys.env.getOrElse(
  "PULUMI_JAVA_SDK_VERSION",
  "0.0.1"
)
