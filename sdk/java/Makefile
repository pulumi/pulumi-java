.PHONY: build ensure test_all test_auto install

# Builds the project and runs fast unit tests.
build::		ensure
	gradle --console=plain build

ensure::
	java --version
	gradle --version

# Builds the project and runs all unit tests, both fast and auto tests.
# A coverage report is generated at pulumi/build/reports/jacoco/allTests
test_all::		ensure
    gradle --console=plain build testAll

# Builds the project and runs only auto tests.
test_auto::		ensure
    gradle --console=plain build autoTest

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
	gradle --console=plain publishToMavenLocal
