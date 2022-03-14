# Java Language Provider for Pulumi

## Prerequisites

- Java 11+ JRE
- Gradle (tested with Gradle 7.3.3)
- Pulumi CLI (tested with v3.21.0)

## Installation

Unpack the correct zip archive for your OS and platform such as
`pulumi-java-darwin-amd64.zip`. Assume that shell commands below are
executed in the directory where the archive is unpacked to.

Add `bin/pulumi-language-jvm` to PATH. For example, in Bash:

```
export PATH=$PATH:$PWD/bin
```

## Running Examples

Install Pulumi resource plugins:

```
pulumi plugin install resource random        4.3.1
pulumi plugin install resource aws-native    0.12.0
pulumi plugin install resource azure-native  1.56.0
pulumi plugin install resource google-native 0.13.0
pulumi plugin install resource aws           4.37.3
pulumi plugin install resource gcp           6.15.1
pulumi plugin install resource kubernetes    3.15.2
pulumi plugin install resource docker        2.10.0
```

You can now deploy the examples, such as:

```
cd examples/aws-native-java-s3-folder
pulumi stack init dev
pulumi up
```

There might be additional instructions in per-example `README.md`.
