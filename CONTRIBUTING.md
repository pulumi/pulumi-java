# Contributing to Pulumi Java

Thank you for contributing to help make Pulumi Java support better. We
appreciate the help! This repository is one of many across the Pulumi
ecosystem and we welcome contributions to them all.

## Code of Conduct

Please make sure to read and observe our
[Contributor Code of Conduct](https://github.com/pulumi/pulumi/blob/master/CODE-OF-CONDUCT.md).

## Communications

You are welcome to join the [Pulumi Community Slack](https://slack.pulumi.com/)
for questions and a community of like-minded folks.
We discuss features and file bugs on GitHub via
[Issues](https://github.com/pulumi/pulumi-java/issues) as well as
[Discussions](https://github.com/pulumi/pulumi/discussions).

### Issues

Feel free to pick up any existing issue that looks interesting to you
or fix a bug you stumble across while using Pulumi with Java. No
matter the size, we welcome all improvements.

### Feature Work

For larger features, we'd appreciate it if you open a
[new issue](https://github.com/pulumi/pulumi-java/issues/new)
before investing a lot of time so we can discuss the feature together.

Please also be sure to browse
[current issues](https://github.com/pulumi/pulumi-java/issues)
to make sure your issue is unique, to lighten the triage burden on our maintainers.

## Developing

### Setting up your development environment

You will want to install the following on your machine:

- JDK 17
- Gradle Build Tool 8.8
- Apache Maven 3.8.4
- Go 1.22
- Pulumi CLI 3.30.0 or higher

### Preparing a pull request

1. Ensure running `make` passes with no issues.
2. Run `make codegen_tests_update` if anything related to codegen has been changed.

### Understanding the repo structure

- `sdk/java/` is a Gradle project that hosts the core Java SDK

- `pkg/cmd/pulumi-language-java` is a Go program that defines the
  Pulumi Java Language Provider

- `pkg/codegen` hosts Java SDK and program generation Go packages

- `tests/examples` contain a few example programs and Go test
  automation that exercises them regularly in a CI context

- `templates` define starter project templates

- `pulumi` is a Git submodule reference to a copy of `pulumi/pulumi`
  with the purpose of consuming protobuf definitions that declare SDK
  and provider interfaces

- `providers` contain Java SDKs built for major cloud providers using
  the Pulumi codegen technology; the generated code is checked in to
  assist reviewing changes through standard git diff tools

### Working with local dependencies

There are Make targets that help rebuild all Java and Go packages from
source. For correct cross-referencing in examples and tests, Java
packages need to be installed into the local `~/.m2` Maven repo.

The targets do not yet understand dependencies accurately, so you may
need to re-run to make sure changes to Java SDK or providers are
rebuilt.

As for Go changes, Pulumi CLI will respect the version of
`pulumi-language-java` it finds in `PATH` over the default version it
ships with. When testing changes to the language provider, you
therefore might need to manipulate `PATH` to prefer the locally build
version.

```shell
make submodule_update # downloads pulumi/pulumi for protobuf definitions
make install_sdk # installs com.pulumi:pulumi into ~/.m2 maven repo
make providers_all # installs all com.pulumi:pulumi-* providers into ~/.m2 maven repo
make bin/pulumi-language-java # builds bin/pulumi-language-java language host
export PATH="${PATH}:${PWD}/bin" # add bin/pulumi-language-java to PATH
```

### Updating generated test code

The `pkg/codegen/testing/test/testdata` directory holds generated code used in tests.
Tests will begin failing if the code in this directory is out-of-date.

Use `make codegen_tests_update` to update code it if you change any codegen logic.

### Working with published dependencies

Release builds of the Java SDK are published to Maven Central as
`com.pulumi:pulumi`.

### Adding examples and testing them locally

Every example is a valid Pulumi program that can be tested by manually
doing `pulumi up` in the right folder.

```
cd tests/exmaples/aws-java-webserver
pulumi up
```

Manual testing sometimes requires setting per-example settings using
`pulumi config` and is documented in every individual example
`README.md`.

For automated testing, examples are added to `java_examples_test.go`
to run under `ProgramTest` framework. There is a Make helper to run
the automated test:

```
make test_example.aws-java-webserver
```

### Testing templates locally

See `templates/README.md` on how to manually test template changes
locally.

Similarly to examples, Make helper targets are provided to
automatically test templates, for example to test
`templates/java-gradle` run:

```
make test_template.java-gradle
```

## Getting Help

We are sure there are rough edges and we appreciate you helping out.
If you want to talk with other folks in the Pulumi community
(including members of the Pulumi team) come hang out in the
`#contribute` channel on the
[Pulumi Community Slack](https://slack.pulumi.com/).
