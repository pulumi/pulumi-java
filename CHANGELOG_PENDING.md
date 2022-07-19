### Improvements

- [codegen] @CustomType builders now use the new @Builder and @Setter annotations
  this is a breaking change and the generated code depends on SDK 0.5.0

### Bug Fixes

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
