### Improvements

- Fix #534: StackReferenceArgs can now be instantiated using StackReferenceArgs.builder() and StackReferenceArgs.Builder, 
  also StackReferenceArgs.getName() was deprecated in favour of StackReferenceArgs.name()
[#537](https://github.com/pulumi/pulumi-java/pull/537)
- Fix #430: now you can pass both plain Object and Output-wrapped objects to Output.format, as in: Output.format("Resource %s has ID %s", myResource, myResource.getId())
[#539](https://github.com/pulumi/pulumi-java/pull/539)
- Fix #476: add examples from PulumiUp
[#542](https://github.com/pulumi/pulumi-java/pull/542)

- GitHub Actions: JDK 'adopt' -> 'temurin'

### Bug Fixes

- Programgen adapted to latest changes in SDK
[#545](https://github.com/pulumi/pulumi-java/pull/545)

