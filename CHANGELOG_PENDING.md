### Improvements

- reduce verbosity of maven builds to WARN
 [#776](https://github.com/pulumi/pulumi-java/pull/776)

### Bug Fixes

- [sdk] [#774](https://github.com/pulumi/pulumi-java/issues/774) 
  fixes deserialization regression in new codegen specific part of SDK, 
  complex @CustomType's are deserializable now

- [codegen] [#702](https://github.com/pulumi/pulumi-java/issues/702): 
  Fixes `pulumi convert` (when targeting Java). Now a maven 
  configuration (`pom.xml` with dependencies) is created and
  the generated Java files are moved to a proper package. 
