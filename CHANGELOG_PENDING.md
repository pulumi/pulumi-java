### Improvements

- [sdk][codegen] [#704](https://github.com/pulumi/pulumi-java/pull/704)
  remove prefix `get` from getters in Resource class and inherited classes
  Old getters are preserved and marked deprecated and will be deleted in the future.
  New getters: urn, id, pulumiResourceType, pulumiResourceName, pulumiChildResources.

### Bug Fixes

- [codegen] [#771](https://github.com/pulumi/pulumi-java/issues/771):
  Fix import path for provider resources on `pulumi convert`.
