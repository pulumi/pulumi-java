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
