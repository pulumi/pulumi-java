CHANGELOG
=========

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
