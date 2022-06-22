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

- [sdk] [#478](https://github.com/pulumi/pulumi-java/issues/478):
  support stack transformations with new `Pulumi.withOptions` API

- [provider] [#576](https://github.com/pulumi/pulumi-java/issues/576)
  Added Gradle support for subprojects. For example, given this
  directory structure:

  ```
  proj/settings.gradle
  proj/subproj/build.gradle
  proj/subproj2/build.gradle
  ```

  Running `cd proj/subproj && pulumi up` will now locate the project
  root and run `gradle :subproj:run` from within `proj`. This makes sure
  Gradle features such as plugins work as expeceted.


### Bug Fixes
