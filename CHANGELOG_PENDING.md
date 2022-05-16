### Improvements

- Fix #534: StackReferenceArgs can now be instantiated using StackReferenceArgs.builder() and StackReferenceArgs.Builder, 
  also StackReferenceArgs.getName() was deprecated in favour of StackReferenceArgs.name()
[#537](https://github.com/pulumi/pulumi-java/pull/537)
- Fix #430: now you can pass both plain Object and Output-wrapped objects to Output.format, as in: Output.format("Resource %s has ID %s", myResource, myResource.getId())
[#539](https://github.com/pulumi/pulumi-java/pull/539)
- Fix #476: add examples from PulumiUp
[#542](https://github.com/pulumi/pulumi-java/pull/542)
- Fix #546: generated Java programs from PCL now also generate custom resource options
[#558](https://github.com/pulumi/pulumi-java/pull/558)
- Fix #547: Implement fully qualified imports for generated programs from PCL
[#596](https://github.com/pulumi/pulumi-java/pull/596)
- [codegen] make sure all overloads of function invokes have doc comments
[#581](https://github.com/pulumi/pulumi-java/pull/581)

- GitHub Actions: JDK 'adopt' -> 'temurin'

### Bug Fixes

- Programgen adapted to latest changes in SDK
[#545](https://github.com/pulumi/pulumi-java/pull/545)
- Fix #538: provide detailed compilation failure information from pulumi-language-java in pulumi CLI