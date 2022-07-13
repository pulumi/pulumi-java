# Composite targets simplify local dev scenarios

build::	ensure build_go build_sdk

ensure::	ensure_sdk

PKG_FILES := $(shell  find pkg -name '*.go' -type f)
BLD_FILES := $(shell  find build -name '*.go' -type f)

# Go project rooted at `pkg/` implements Pulumi Java language plugin
# and Java go as a Go library.

build_go::
	cd pkg && go build -v all

test_go:: build_go submodule_update
	cd pkg && go test ./...

bin/pulumi-language-java: ${PKG_FILES}
	mkdir -p bin
	cd pkg && go build -o ../bin github.com/pulumi/pulumi-java/pkg/cmd/pulumi-language-java

bin/pulumi-java-gen: ${PKG_FILES}
	mkdir -p bin
	cd pkg && go build -o ../bin -ldflags "-X github.com/pulumi/pulumi-java/pkg/version.Version=$(shell pulumictl get version --tag-pattern '^pkg')" github.com/pulumi/pulumi-java/pkg/cmd/pulumi-java-gen

bin/pulumi-java-builder: ${BLD_FILES}
	mkdir -p bin
	cd build && go build -o ../bin github.com/pulumi/pulumi-java/build/cmd/pulumi-java-builder

# Java SDK is a gradle project rooted at `sdk/java`

install_sdk::
	cd sdk/java && make install

build_sdk::
	cd sdk/java && make build

ensure_sdk::
	cd sdk/java && make ensure

sdk.publish:	bin/pulumi-java-builder
	cd sdk/java && ../../bin/pulumi-java-builder publish --dir ./pulumi --pkg com.pulumi.pulumi

.PHONY: lint_pkg
lint:: lint_pkg
lint_pkg:
	cd pkg && golangci-lint run -c ../.golangci.yml --timeout 5m

# Run a custom integration test or example.
# Example: make test_example.aws-java-webserver
test_example.%:	bin/pulumi-language-java
	cd tests/examples && PATH="${PATH}:${PWD}/bin" go test -run "TestExamples/^$*$$" -test.v

# Test a single template, e.g.:
#     make test_template.java-gradle
test_template.%: bin/pulumi-language-java
	cd tests/templates && PATH="${PATH}:${PWD}/bin" go test -run "TestTemplates/^$*$$" -test.v

test_templates: bin/pulumi-language-java
	cd tests/templates && PATH="${PATH}:${PWD}/bin" go test -test.v

# Test a single integration, s.g.:
#     make test_integration.stack-reference
test_integration.%: bin/pulumi-language-java
	cd tests/integration && PATH="${PATH}:${PWD}/bin" go test -run "TestIntegrations/^$*$$" -test.v

test_integrations: bin/pulumi-language-java
	cd tests/integration && PATH="${PATH}:${PWD}/bin" go test -test.v

codegen_tests::
	cd ./pkg/codegen/java && go test ./...

submodule_update::
	git submodule update --init --recursive

# Borrows test case schemas from pulumi/pulumi repo linked in via git
# submodule as symlinks.
borrow_schemas:: submodule_update
	find pulumi/pkg/codegen/testing -name "schema.*" -exec ./scripts/borrow-schema.sh "{}" ";"

# Runs `go mod tidy` on every Go project.
tidy::
	./scripts/tidy.sh
