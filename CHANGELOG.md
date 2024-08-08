CHANGELOG
=========

## 0.14.0 (2024-08-08)

### Improvements

- Add support for parameterized providers
- Add support for local SDK generation

### Bug fixes

- Correctly escape special characters in generated Javadocs
- Fully qualify `java.lang` types in codegen

## 0.13.0 (2024-07-29)

### Improvements

- Codegen: pluginDownloadURL is now supported through the automatically generated build.gradle
- Update pulumi/pulumi to v3.121.0

### Bug Fixes

- Unable to rehydrate a resource that has required inputs

## 0.12.0 (2024-05-24)

### Improvements

- Plugin: will now automatically use the Gradle executor if build.gradle.kts is present
- Codegen: support for overlays

### Bug Fixes

- Generated Utilities.java will use the correct basePackage

## 0.11.0 (2024-05-09)

### Bug Fixes

* Escape javadoc special characters in generated code.
* Fail fast rather than emitting `PANIC`s when attempting to generate code for ill-formed programs.

## 0.10.0 (2024-03-27)

### Improvements

- Emit comments/trivia for resource and local variable declarations in program-gen
- Support `stringAsset` function in program-gen from PCL
- Turn not yet implemented error into a warning in program-gen

## 0.9.9 (2023-12-27)

### Improvements

- Update pulumi/pulumi to v3.99.0.

## 0.9.8 (2023-10-05)

### Improvements

- Adds `MissingRequiredPropertyException` to the main java SDK to be used later in the generated provider SDKs.

### Bug Fixes

- Fixes `builder()` implementation for result types where the identifier of the local variable defined for the result type collides with one of the fields of the result type.
- Adds `options.encoding = "UTF-8"` to the `compileJava` options so that it can handle non-ASCII characters in the source code, especially in the documentation comments of fields.

## 0.9.7 (2023-08-29)

### Improvements

- Plugin: clean up resources and exit cleanly on receiving SIGINT or CTRL_BREAK.

## 0.9.6 (2023-08-23)

### Improvements

 - [sdk] Bumps com.google.guava:guava from 31.1-jre to 32.1.2-jre.
   [#1188](https://github.com/pulumi/pulumi-java/pull/1188)


## 0.9.5 (2023-07-14)

### Bug Fixes

  - [sdk] Fix links to output docs.
  - [codegen] Remove a TODO from codegen output.

## 0.9.4 (2023-06-07)

### Bug Fixes

  - [codegen] Fixed missing gradle fields for 3rd-party providers.

## 0.9.3 (2023-05-09)

  - [sdk] Enable Zip64 jar files for jar task
  - [sdk] Increase maximum memory for compile step to 16g

## 0.9.2 (2023-04-18)

### Bug Fixes

  - [sdk] Enable Zip64 jar files for javadoc
  - [sdk] Fix exception in URN handling of nested parent URN types.

## 0.9.1 (2023-04-12)

### Bug Fixes

  - [sdk] Fix exception in URN handling of nested parent URN types.

## 0.9.0 (2023-04-03)

### Improvements

  - [codegen] Support the `retainOnDelete` resource option in PCL.

  - [sdk] Added `StackReference.outputDetailsAsync` to retrieve output values from stack references directly.

## 0.8.0 (2023-03-01)

### Improvements

  - [java/sdk] Delegate alias computation to the engine. [#966](https://github.com/pulumi/pulumi-java/pull/966)

### Bug Fixes

  - [codegen] Generate build.gradle files compatible with Gradle 8.0.

## 0.7.1 (2022-12-15)

### Bug Fixes

- [provider] [#913](https://github.com/pulumi/pulumi-java/pull/913):
  Windows releases now release as .tar.gz to match pulumi plugin ecosystem expectations.

## 0.7.0 (2022-12-13)

### Improvements

- [codegen] Internal engineering work.

## 0.6.0 (2022-09-13)

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

## 0.5.4 (2022-08-12)

### Improvements

- [provider] [#791](https://github.com/pulumi/pulumi-java/pull/791):
  Basic support for using [sbt](https://www.scala-sbt.org/) projects and using Scala.
  See the example in [`tests/examples/minimalsbt`](tests/examples/minimalsbt).


## 0.5.3 (2022-08-11)

### Improvements

- [provider] [#776](https://github.com/pulumi/pulumi-java/pull/776):
  Reduced verbosity of Maven builds to only surface WARN and ERROR
  logs to the Pulumi user when Pulumi invokes Maven under the hood

- [codegen] [#702](https://github.com/pulumi/pulumi-java/issues/702):
  Enabled `pulumi convert` to target Java. Now a maven configuration
  (`pom.xml` with dependencies) is created and the generated Java
  files are moved to a proper package.

### Bug Fixes

- [sdk] [#782](https://github.com/pulumi/pulumi-java/pull/782): Fixed
  a serializer regression that affected the releases of
  `com.pulumi:kubernetes` after `3.19.1`. Upgrade `com.pulumi:pulumi`
  dependency to fix the issue that manifested as:

  ```UnsupportedOperationException: Convert [...]: Error```

## 0.5.2 (2022-07-20)

### Bug Fixes

- [codegen] [#757](https://github.com/pulumi/pulumi-java/pull/757):
  Fixes a regression introduced in 0.5.1 where

      pulumi-java-gen --build gradle

  stopped automatically generating "com.pulumi:pulumi" references in
  the `build.gradle` file, making the generated project fail to
  compile.

## 0.5.1 (2022-07-19)

### Bug Fixes

- [codegen] [#390](https://github.com/pulumi/pulumi-java/issues/390)
  fixes code generation for output types with large (200+) number of
  parameters hitting the JVM limits. Previously these types would
  generate compiling code but throw exceptions at runtime when in use.

  Packages generated with this version of codegen will need to depend
  on `com.pulumi:pulumi` (Java SDK) versions `0.5.0` or higher, so
  that user programs get the upgraded Java SDK that can understand the
  new class annotation scheme and continue working without changes.

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

## 0.5.0 (2022-07-13)

### Improvements

- [provider] [#728](https://github.com/pulumi/pulumi-java/pull/728):
  Added health checks to `pulumi-language-java` so that it never
  leaks as an orphan process when the managing `pulumi` process
  terminates

- [codegen] [#717](https://github.com/pulumi/pulumi-java/issues/717):
  Simplified publishing generated provider code to Maven Central.

  Added a `gradleNexusPublishPluginVersion` option that installs
  [gradle-nexus/publish-plugin](https://github.com/gradle-nexus/publish-plugin)
  in the build code generated by `pulumi-java-gen`.

  The option can also be activated on the command line during code
  generation: `pulumi-java-gen --build gradle-nexus`.

  This enables one-step publishing of generated Java packages via
  Sonatype's OSSRH Nexus:

  ```
  gradle publishToSonatype closeAndReleaseSonatypeStagingRepository
  ```

### Bug Fixes

- [codegen] [#735](https://github.com/pulumi/pulumi-java/issues/735):
  Fix a bug introduced in `0.4.1`. `gradle publish` stopped generating
  resources required by Pulumi and published broken packages.

## 0.4.1 (2022-06-30)

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

## 0.4.0 (2022-06-22)

### Improvements

- [codegen] **Breaking**
  [#163](https://github.com/pulumi/pulumi-java/issues/163): function
  invokes now accept `Output<T>` in their arguments and return
  `Output<T>` instead of `CompletableFuture<T>`
  [#612](https://github.com/pulumi/pulumi-java/pull/612)

- [sdk] `Stack` resource is now considered internal and cannot be
  directly instantiated by the user. Instead of inheriting from
  `Stack`, please use the `Pulumi.run` form to define resources in
  your stack as shown in the README

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

## 0.3.0 (2022-06-01)

### Improvements

- [programgen] [#547](https://github.com/pulumi/pulumi-java/issues/547):
  Generate fully qualified imports

- [sdk] [#419](https://github.com/pulumi/pulumi-java/issues/419):
  Remove SDK dependency on Mockito

- [provider] Support for using [jbang](https://jbang.dev)

### Bug Fixes

- [codegen] [#627](https://github.com/pulumi/pulumi-java/issues/627):
  Fix malformed versions in generated build files by sanitizing
  version input in pulumi-java-gen

## 0.2.1 (2022-05-20)

### Improvements

- [provider] Add `pulumi-java-gen` binary encapsulating Java SDK
  generation to the GitHub releases.

## 0.2.0 (2022-05-18)

### Improvements

- [sdk]
  [#534](https://github.com/pulumi/pulumi-java/issues/534):
  `StackReferenceArgs` can now be instantiated using
  `StackReferenceArgs.builder()` and `StackReferenceArgs.Builder`,
  also `StackReferenceArgs.getName()` was deprecated in favour of
  `StackReferenceArgs.name()`
  [#537](https://github.com/pulumi/pulumi-java/pull/537)

- [sdk]
  [#430](https://github.com/pulumi/pulumi-java/issues/430):
  now you can pass both plain `Object` and
  `Output`-wrapped objects to `Output.format`, as in:
  `Output.format("Resource %s has ID %s", myResource, myResource.getId())`
  [#539](https://github.com/pulumi/pulumi-java/pull/539)

- [examples] Add examples from PulumiUp
  [#542](https://github.com/pulumi/pulumi-java/pull/542)

- [codegen] Make sure all overloads of function invokes have doc comments
  [#581](https://github.com/pulumi/pulumi-java/pull/581)

- [ci] GitHub Actions: use 'temurin' JDK instead of the deprecated
  'adopt' JDK

### Bug Fixes

- [programgen] Fix
  [#546](https://github.com/pulumi/pulumi-java/issues/546):
  generated Java programs from PCL now also generate custom resource options
  [#558](https://github.com/pulumi/pulumi-java/pull/558)

- [provider] Fix
  [#538](https://github.com/pulumi/pulumi-java/issues/538): provide
  detailed compilation failure information from `pulumi-language-java`
  in Pulumi CLI

- [sdk] Fix [#552](https://github.com/pulumi/pulumi-java/issues/552):
  `Output.all` will preserve list length and retain `null` elements

- [provider] Fix
  [#540](https://github.com/pulumi/pulumi-java/issues/540): language
  host will no longer fail when doing plugin discovery