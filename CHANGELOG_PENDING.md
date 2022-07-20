### Improvements

### Bug Fixes

- [codegen] [#757](https://github.com/pulumi/pulumi-java/pull/757):
  Fixes a regression introduced in 0.5.1 where

      pulumi-java-gen --build gradle

  stopped automatically generating "com.pulumi:pulumi" references in
  the `build.gradle` file, making the generated project fail to
  compile.
