organization := "com.pulumi.example"
name := "stacktransformation2"
version := "1.0-SNAPSHOT"
Compile / mainClass := Some(s"{organization.value}.{name.value}.App")
libraryDependencies ++= List(
  "com.pulumi" % "pulumi" % sys.env.getOrElse(
    "PULUMI_JAVA_SDK_VERSION",
    "0.0.1"
  ),
  "com.pulumi" % "random" % sys.env.getOrElse(
    "PULUMI_RANDOM_PROVIDER_SDK_VERSION",
    "4.6.0"
  )
)
