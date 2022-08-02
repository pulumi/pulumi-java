### Improvements

- reduce verbosity of maven builds to WARN
 [#776](https://github.com/pulumi/pulumi-java/pull/776)

### Bug Fixes

- [codegen] #702: Fixes `pulumi convert` when targeting Java. 
  This was done by generating maven configuration (`pom.xml`) and moving the generated Java files to a proper package. 
