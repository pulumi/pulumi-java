### Improvements

- [codegen] **Breaking**
  [#163](https://github.com/pulumi/pulumi-java/issues/163): function
  invokes now accept `Output<T>` in their arguments and return
  `Output<T>` instead of `CompletableFuture<T>`
  [#612](https://github.com/pulumi/pulumi-java/pull/612).

- [sdk] `Stack` resource is now considered internal and cannot be
  directly instantiated by the user. Instead of inheriting from
  `Stack`, please use the `Pulumi.run` form to define resources in
  your stack as shown in the README.

### Bug Fixes
