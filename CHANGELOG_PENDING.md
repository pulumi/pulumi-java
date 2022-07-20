### Improvements

### Bug Fixes

- [codegen] [#390](https://github.com/pulumi/pulumi-java/issues/390)
  fixes code generation for output types with large (200+) number of
  parameters hitting the JVM limits. Previously these types would
  generate compiling code but throw exceptions at runtime when in use.

  Packages generated with this version of codegen will need to depend
  on `com.pulumi:pulumi` (Java SDK) versions `0.5.0` or higher, so
  that user programs get the upgraded Java SDK that can understand the
  new class annotation scheme and continue working without changes.

- [codegen] [#739](https://github.com/pulumi/pulumi-java/issues/739)
  fixes ambiguity in the "packages" option by adding a new
  "dependencies" option in Java extensions to Package Schema. With
  this change, both package name overrides and desired Maven
  dependencies in the generated build files can now be specified:

      packages:
        "admissionregistration.k8s.io/v1: "admissionregistration.v1"

      build: gradle

	  dependencies:
	    "com.pulumi:aws": "5.4.0"
