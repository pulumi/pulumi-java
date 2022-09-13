### Improvements

- [sdk] [#704](https://github.com/pulumi/pulumi-java/pull/704): Remove
  the `get` prefix from getters in `Resource` class and inherited
  classes Old getters are preserved and marked deprecated. New
  getters: `urn`, `id`, `pulumiResourceType`, `pulumiResourceName`,
  `pulumiChildResources`.

- [sdk] [#643](https://github.com/pulumi/pulumi-java/issues/643): Do
  not panic when the type of property in resource outputs does not
  match the one on the wire, while deserializing. The fix proceed with
  a null / default value and a warning in place of an error.

- [codegen] [#759](https://github.com/pulumi/pulumi-java/pull/759):
  Fix code generation for exports with deeply nested generic types.
  Previously these types would generate compiling code but throw
  exceptions at runtime when in use.

  Packages generated with this version of codegen will need to depend
  on this version of Java SDK or higher, so that user programs get the
  upgraded Java SDK that can understand the new export annotation
  scheme and continue working without changes.

- [provider] [#785](https://github.com/pulumi/pulumi-java/issues/785):
  The Pulumi CLI will no longer display seemingly duplicate stack
  traces. The language host was modified to hide error messages from
  an optional process of plugin discovery through classpath
  introspection. Also `plugin about` will no longer display errors
  from plugin discovery. To display errors from plugin discovery, use
  `-v=9` with a value of 1 or greater.

### Bug Fixes

- [codegen] [#771](https://github.com/pulumi/pulumi-java/issues/771):
  Fix import path for provider resources on `pulumi convert`.

- [sdk] [#778](https://github.com/pulumi/pulumi-java/pull/778): Fix a
  bug that prevented user from specifying both parent and child
  aliases when refactoring component resources.

- [sdk] [#840](https://github.com/pulumi/pulumi-java/pull/840): Fix a
  regression introduced after 0.5.2 when Pulumi Java SDK stopped
  tolerating missing fields from providers that manifested as
  InvocationTargetException caused by a NullPointerException.
