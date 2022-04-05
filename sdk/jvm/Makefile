# Builds the project and runs unit tests.
build::		ensure
	gradle build

ensure::
	gradle --version

# Installs the package into a local Maven repository - `~/.m2` by
# default.
#
# To reference this build from `build.gradle`:
#
# repositories {
#    ...
#    mavenLocal()
# }
#
# dependencies {
#    implementation 'io.pulumi:pulumi:1.0.0'
# }
#
install::	build
	gradle publishToMavenLocal
