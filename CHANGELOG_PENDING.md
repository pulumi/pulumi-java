### Improvements

- reduce verbosity of maven builds to WARN
 [#776](https://github.com/pulumi/pulumi-java/pull/776)

### Bug Fixes

- [codegen] [#702](https://github.com/pulumi/pulumi-java/issues/702): 
  Fixes `pulumi convert` (when targeting Java). Now a maven 
  configuration (`pom.xml` with dependencies) is created and
  the generated Java files are moved to a proper package. 

- [codegen] [#771](https://github.com/pulumi/pulumi-java/issues/771):
  Fix import path for provider resources on `pulumi convert`.
