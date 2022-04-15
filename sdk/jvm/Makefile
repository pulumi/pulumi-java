# TODO: use pulumictl to compute versions from repo tags
VERSION := 1.0.0

# Builds the project and runs unit tests.
build::		ensure
	gradle --console=plain build

ensure::
	java --version
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
#    implementation 'com.pulumi:pulumi:1.0.0'
# }
#
# Adjust 1.0.0 to the right version above.
install::	build
	gradle --console=plain -Pversion=${VERSION} publishToMavenLocal
