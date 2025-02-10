# Composite targets simplify local dev scenarios

build::	ensure build_go build_sdk

ensure::	ensure_go ensure_sdk

PKG_FILES := $(shell  find pkg -name '*.go' -type f)

# Go project rooted at `pkg/` implements Pulumi JVM language plugin
# and Java go as a Go library.

build_go::	ensure_go
	cd pkg && go build -v all

test_go:: build_go submodule_update
	cd pkg && go test ./...

ensure_go::
	cd pkg && go mod tidy

bin/pulumi-language-jvm: ${PKG_FILES}
	mkdir -p bin
	cd pkg && go build -o ../bin github.com/pulumi/pulumi-java/pkg/cmd/pulumi-language-jvm

bin/pulumi-java-gen: ${PKG_FILES}
	mkdir -p bin
	cd pkg && go build -o ../bin github.com/pulumi/pulumi-java/pkg/cmd/pulumi-java-gen


# Java SDK is a gradle project rooted at `sdk/jvm`

install_sdk::
	cd sdk/jvm && make install

build_sdk::
	cd sdk/jvm && make build

ensure_sdk::
	cd sdk/jvm && make ensure

providers_generate_all: provider.random.generate provider.aws.generate provider.aws-native.generate provider.docker.generate provider.kubernetes.generate provider.azure-native.generate provider.google-native.generate provider.gcp.generate provider.eks.generate

providers_all: provider.random.install provider.aws.install provider.aws-native.install provider.docker.install provider.kubernetes.install provider.azure-native.install provider.google-native.install provider.gcp.install provider.eks.install

provider.eks.build: provider.kubernetes.install provider.aws.install

# Example: make provider.random.build
provider.%.build:	provider.%.generate
	cd providers/pulumi-$*/sdk/java && gradle build

# Example: make provider.random.generate
provider.%.generate:	bin/pulumi-java-gen
	./bin/pulumi-java-gen -config providers/pulumi-$*/pulumi-java-gen.yaml

.PHONY: lint_pkg
lint:: lint_pkg
lint_pkg:
	cd pkg && golangci-lint run -c ../.golangci.yml --timeout 5m

# Example: make provider.random.install
provider.%.install:	provider.%.build
	cd providers/pulumi-$*/sdk/java && gradle publishToMavenLocal

# Integration tests will use PULUMI_ACCESS_TOKEN to provision tests
# stacks in Pulumi service.
integration_tests::	bin/pulumi-language-jvm ensure_tests

# Run a custom integration test or example.
# Example: make test_example.aws-java-webserver
test_example.%:	bin/pulumi-language-jvm ensure_tests provider.random.install
	cd tests/examples && PATH=${PATH}:${PWD}/bin go test -run "TestExamples/^$*" -test.v

ensure_plugins::
	pulumi plugin install resource aws v4.37.3
	pulumi plugin install resource aws-native v0.12.0
	pulumi plugin install resource azure-native v1.56.0
	pulumi plugin install resource kubernetes v3.15.1
	pulumi plugin install resource gcp v6.11.0
	pulumi plugin install resource random v4.3.1
	pulumi plugin install resource eks v0.37.1

ensure_tests::	submodule_update ensure_plugins

codegen_tests::	ensure_tests
	cd ./pkg/codegen/jvm && go test ./...

submodule_update::
	git submodule update --init --recursive

# Borrows test case schemas from pulumi/pulumi repo linked in via git
# submodule as symlinks.
borrow_schemas:: submodule_update
	find pulumi/pkg/codegen/testing -name "schema.*" -exec ./scripts/borrow-schema.sh "{}" ";"

# Runs `go mod tidy` on every Go project.
tidy::
	./scripts/tidy.sh
