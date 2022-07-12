### Improvements

- [provider] [#728](https://github.com/pulumi/pulumi-java/pull/728):
  Added health checks to `pulumi-language-java` so that it never
  leaks as an orphan process when the managing `pulumi` process
  terminates

### Bug Fixes

- [codegen] [#735](https://github.com/pulumi/pulumi-java/issues/735)
  Fix a bug introduced in 0.4.1: `gradle publish` stopped generating
  resources required by Pulumi and published broken packages.
