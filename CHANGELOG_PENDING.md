### Improvements

- [sdk] #553: Added `com.pulumi.test` package with new idiomatic Java API for [unit testing Pulumi programs]
  (https://www.pulumi.com/docs/guides/testing/#unit-testing) against mocks.
  See ([example](https://github.com/pulumi/pulumi-java/tree/main/tests/examples/testing-unit-java)) of using the new API.

- [codegen] [#709](https://github.com/pulumi/pulumi-java/pull/709)
  Improved version handling in `pulumi-java-gen`: now `--version`
  argument is optional and the version of the generated package need
  not be known at SDK code generation time and is not spliced into the
  code. To set the version at build time use
  `gradle -Pversion=1.2.3 build` or `PACKAGE_VERSION=1.2.3 gradle build`.

### Bug Fixes
