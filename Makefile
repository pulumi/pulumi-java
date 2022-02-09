# Composite targets simplify local dev scenarios

build::	ensure build_go build_sdk

ensure::	ensure_go ensure_sdk

PKG_FILES := $(shell  find pkg -name '*.go' -type f)

# Go project rooted at `pkg/` implements Pulumi JVM language plugin
# and Java go as a Go library.

build_go::	ensure_go
	cd pkg && go build -v all

test_go:: build_go
	cd pkg && go test -test.v ./...

ensure_go::
	cd pkg && go mod tidy

bin/pulumi-language-jvm: ${PKG_FILES}
	mkdir -p bin
	cd pkg && go build -o ../bin github.com/pulumi/pulumi-java/pkg/cmd/pulumi-language-jvm

bin/pulumi-java-gen: ${PGK_FILES}
	mkdir -p bin
	cd pkg && go build -o ../bin github.com/pulumi/pulumi-java/pkg/cmd/pulumi-java-gen


# Java SDK is a gradle project rooted at `sdk/jvm`

install_sdk::
	cd sdk/jvm && make install

build_sdk::
	cd sdk/jvm && make build

ensure_sdk::
	cd sdk/jvm && make ensure

# pulumi-random provider Java SDKs built from providers/pulumi-random:

ensure_random::

build_random::	bin/pulumi-java-gen
	cd providers/pulumi-random && make build

install_random::
	cd providers/pulumi-random && make install

define generate_sdk
	rm -rf ./providers/./$(1)
	mkdir ./providers/./$(1)
	curl $(2) -o ./bin/$(1)-schema.json
	./bin/pulumi-java-gen -schema ./bin/$(1)-schema.json -out ./providers/$(1)
	cd ./providers/$(1) && gradle build
endef

.PHONY: lint_pkg
lint:: lint_pkg
lint_pkg:
	cd pkg && golangci-lint run -c ../.golangci.yml --timeout 5m


providers_all: aws-native kubernetes aws-native azure-native google-native

aws-native: bin/pulumi-java-gen install_sdk
	-$(call generate_sdk,aws-native,https://raw.githubusercontent.com/pulumi/pulumi-aws-native/master/provider/cmd/pulumi-resource-aws-native/schema.json)

kubernetes: bin/pulumi-java-gen install_sdk
	-$(call generate_sdk,kubernetes,https://raw.githubusercontent.com/pulumi/pulumi-kubernetes/master/provider/cmd/pulumi-resource-kubernetes/schema.json)

azure-native: bin/pulumi-java-gen install_sdk
	-$(call generate_sdk,azure-native,https://raw.githubusercontent.com/pulumi/pulumi-azure-native/master/provider/cmd/pulumi-resource-azure-native/schema.json)

google-native: bin/pulumi-java-gen install_sdk
	-$(call generate_sdk,google-native,https://raw.githubusercontent.com/pulumi/pulumi-google-native/master/provider/cmd/pulumi-resource-google-native/schema.json)

# Integration tests will use PULUMI_ACCESS_TOKEN to provision tests
# stacks in Pulumi service.
integration_tests::	bin/pulumi-language-jvm ensure_tests install_random
	cd tests/examples && PATH=${PATH}:${PWD}/bin go test -run TestJava -test.v

ensure_tests::
	pulumi plugin install resource random v4.3.1
