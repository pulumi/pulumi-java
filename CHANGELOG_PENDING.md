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

- [cli] [#785](https://github.com/pulumi/pulumi-java/issues/785):
  The Pulumi CLI will no longer display, what look like a duplicate stack traces.
  The language host was modified to hide error messages from an optional process
  of plugin discovery through classpath introspection.
  Also `plugin about` will no longer display errors from plugin discovery.
  To display errors from plugin discovery, use config option `runtime.options.v`
  with a value of 1 or greater.

### Bug Fixes

- [codegen] [#771](https://github.com/pulumi/pulumi-java/issues/771):
  Fix import path for provider resources on `pulumi convert`.

- [sdk] Fixes a bug that prevented user from specifying both parent
  and child aliases when refactoring component resources.