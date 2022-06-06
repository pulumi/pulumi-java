### Improvements

- Fix #547: Implement fully qualified imports for generated programs from PCL
[#596](https://github.com/pulumi/pulumi-java/pull/596)

- Fix #163: function invokes now return `Output<T>` instead of `CompletableFuture<T>`
[#612](https://github.com/pulumi/pulumi-java/pull/612)

- Fix #419: Remove SDK dependency on Mockito

- Support for using [jbang](https://jbang.dev)

- Stack resource is now considered internal and cannot be directly instantiated by the user. 
  The TestResult returned in tests is now an interface and no longer exposes fields.

### Bug Fixes

Fix #627 - sanitize codegen sdk version input in pulumi-java-gen
