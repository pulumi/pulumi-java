#!/usr/bin/env bash

# Build zips for disributing private preview. These zips are
# self-contianed with the Go binary, local Maven provider, templates,
# examples and a README on how to install and use Java.

set -euo pipefail

add_example()
{
    example="$1"
    echo "systemProp.maven.repo.local=../../../.m2/repository" > "tests/examples/$example/gradle.properties"
    (cd tests && zip "$zip" -r "examples/$example")
    rm "tests/examples/$example/gradle.properties"
}

add_template()
{
    template="$1"
    zip "$zip" -r "templates/$template"
}

add_maven_repo() {
    path="$1"
    (cd ~ && zip "$zip" -r ".m2/repository/$path")
}

build_zip()
{
    os="$1"
    arch="$2"
    zip="$PWD/self-contained-zip/pulumi-java-$os-$arch.zip"

    echo "===== BUILDING ${zip} ====="

    rm -rf "$zip"

    (cd self-contained-zip && zip "$zip" README.md)

    (cd "dist/pulumi-java_${os}_${arch}" &&
         mkdir bin &&
         mv pulumi-language-jvm* bin/ &&
         zip "$zip" -r bin)

    add_template java-gradle

    add_maven_repo io/pulumi/pulumi

    add_maven_repo io/pulumi/aws
    add_maven_repo io/pulumi/aws-native
    add_maven_repo io/pulumi/azure-native
    add_maven_repo io/pulumi/docker
    add_maven_repo io/pulumi/gcp
    add_maven_repo io/pulumi/google-native
    add_maven_repo io/pulumi/kubernetes
    add_maven_repo io/pulumi/pulumi
    add_maven_repo io/pulumi/random

    add_example aws-java-webserver
    add_example aws-native-java-s3-folder
    add_example azure-java-appservice-sql
    add_example azure-java-static-website
    add_example gcp-java-gke-hello-world
    add_example minimal
}

goreleaser build --skip-validate --snapshot --rm-dist

build_zip darwin  amd64
build_zip darwin  arm64
build_zip linux   amd64
build_zip linux   arm64
build_zip windows amd64
build_zip windows arm64
