PROJECT_NAME    := Pulumi JVM Core SDK
LANGHOST_PKG    := github.com/pulumi/pulumi/sdk/v3/jvm/cmd/pulumi-language-jvm
VERSION         := $(shell cd ../../ && pulumictl get version)

PROJECT_PKGS    := $(shell go list ./cmd...)
TESTPARALLELISM := 10

include ../../build/common.mk

ensure:: # TODO

build_package::
	gradle build

build_plugin::
	go install -ldflags "-X github.com/pulumi/pulumi/sdk/v3/go/common/version.Version=${VERSION}" ${LANGHOST_PKG}

build:: build_package build_plugin

install_package:: build_package # TODO
	gradle -Pversion=${VERSION} publishToMavenLocal

install_plugin:: build_plugin
	GOBIN=$(PULUMI_BIN) go install -ldflags "-X github.com/pulumi/pulumi/sdk/v3/go/common/version.Version=${VERSION}" ${LANGHOST_PKG}

install:: install_package install_plugin

jvm_test:: install # TODO

test_fast:: jvm_test
	$(GO_TEST_FAST) ${PROJECT_PKGS}

test_all:: jvm_test
	$(GO_TEST) ${PROJECT_PKGS}

dist:: build
	go install -ldflags "-X github.com/pulumi/pulumi/sdk/v3/go/common/version.Version=${VERSION}" ${LANGHOST_PKG}

brew:: BREW_VERSION := $(shell ../../scripts/get-version HEAD)
brew::
	go install -ldflags "-X github.com/pulumi/pulumi/sdk/v3/go/common/version.Version=${BREW_VERSION}" ${LANGHOST_PKG}

publish:: build_package # TODO