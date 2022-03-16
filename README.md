# pulumi-java

Pulumi Java SDK


## Repo Structure

There are no long-term plans to merge Java SDK, language provider, and
codegen into `pulumi/pulumi`, instead it will continue evolving in
this repo.

When going public, the release cadence of the Java SDK and language
provider may be tied to that of the other SDKs like Node, Python, .NET
and Go that live in `pulumi/pulumi`. Its TBD how that will be
accomplished technically but options include Git submodules and
stitching releases via cross-repo GitHub Actions.

The dependecy loop between `pulumi/pulumi` and `pulumi/java` should be
avoided in favor of `pulumi/java` build-depending on `pulumi/pulumi`.
Layers of indirection similar to plugin acquisition will need to be
introduced in places where `pulumi/pulumi` currently build-depends on
`pulumi/java`, for example:

- `pkg/cmd/pulumi/import.go`: import command support needs
  jvm.GenerateProgram; instead figure out dynamic loading of program
  generators

- `pkg/cmd/pulumi/new.go`: new command needs jvm.Build to support
  dispatching the right build commands for a java project; instead
  figure out dynamic dispatch of builders per language

## Adding Examples

Copy `tests/examples/random` to `tests/examples/your-example`.

Update `rootProject.name` in `settings.gradle`.

Update `name` in `Pulumi.yaml`.

Update `mainClass` in `app/build.gradle`.

Reference any providers your example needs in `dependencies` section
of `app/build.gradle`.

Edit sources in `app/src/main`.

To run the example:

```
cd ~/pulumi-java
make bin/pulumi-language-jvm
export PATH=$PWD/bin:$PATH

# For every provider you need:
make provider.azure-native.install # installs into ~/.m2 maven repo
pulumi plugin install resource azure-native 1.56.0

# Now it should work:
cd tests/examples/your-example
pulumi preview
```

Once it is working you can add it to `java_examples_test.go` to run
under `ProgramTest` framework, and write some assert in Go against the
expected stack outputs.

## Git History

This repo adapts code from `VirtusLab/pulumi` fork of `pulumi/pulumi`
starting with the `jvm-lang-codegen-rewrite` branch. The upstream
history is preserved so that updates can be merged via Git.
