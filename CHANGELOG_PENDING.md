### Improvements

- [sdk][codegen] [#704](https://github.com/pulumi/pulumi-java/pull/704)
  remove prefix `get` from getters in Resource class and inherited classes
  Old getters are preserved and marked deprecated and will be deleted in the future.
  New getters: urn, id, pulumiResourceType, pulumiResourceName, pulumiChildResources.

- [sdk] [#643](https://github.com/pulumi/pulumi-java/issues/643):
  Do not panic when the type of property in resource outputs doesn't match
  the one on the wire, while deserializing.
  The fix proceed with a null / default value and a warning in place of an error. 
  This makes some programs succeed.

- [codegen] [#759](https://github.com/pulumi/pulumi-java/pull/759)
  fixes code generation for exports with deeply nested generic types.
  Previously these types would generate compiling code but throw 
  exceptions at runtime when in use.

  Packages generated with this version of codegen will need to depend
  on this version of Java SDK or higher, so that user programs get the 
  upgraded Java SDK that can understand the new export annotation
  scheme and continue working without changes.

### Bug Fixes

- [codegen] [#771](https://github.com/pulumi/pulumi-java/issues/771):
  Fix import path for provider resources on `pulumi convert`.
