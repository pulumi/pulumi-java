### Improvements

- [codegen] [#759](https://github.com/pulumi/pulumi-java/pull/759)
  fixes code generation for exports with deeply nested generic types.
  Previously these types would generate compiling code but throw 
  exceptions at runtime when in use.

  Packages generated with this version of codegen will need to depend
  on this version of Java SDK or higher, so that user programs get the 
  upgraded Java SDK that can understand the new export annotation
  scheme and continue working without changes.

### Bug Fixes
