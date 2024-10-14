# Composite targets simplify local dev scenarios

build::	ensure build_go build_sdk

ensure::	ensure_sdk

PKG_FILES := $(shell  find pkg -name '*.go' -type f)

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

# Java SDK is a gradle project rooted at `sdk/java`

install_sdk::
	cd sdk/java && make install

build_sdk::
	cd sdk/java && make build

ensure_sdk::
	cd sdk/java && make ensure

.PHONY: lint_pkg
lint:: lint_pkg
lint_pkg: lint_pkg_dependencies
	cd pkg && golangci-lint run -c ../.golangci.yml --timeout 5m
.PHONY: lint_pkg_dependencies
lint_pkg_dependencies:
	@cd pkg || exit 1; \
	PKG=$$(grep -r '"github.com/pulumi/pulumi/pkg/v3/[^codegen]' .); \
	if [ "$$?" -eq 0 ] ; then \
		echo "Cannot use pkg except for codegen.";\
		echo "Found $$PKG";\
		exit 1; \
	fi

# Run a custom integration test or example.
# Example: make test_example.aws-java-webserver
test_example.%:	bin/pulumi-language-java
	cd tests/examples && PATH="${PWD}/bin:${PATH}" go test -run "TestExamples/^$*$$" -test.v

# Test a single template, e.g.:
#     make test_template.java-gradle
test_template.%: bin/pulumi-language-java
	cd tests/templates && PATH="${PWD}/bin:${PATH}" go test -run "TestTemplates/^$*$$" -test.v

test_templates: bin/pulumi-language-java
	cd tests/templates && PATH="${PWD}/bin:${PATH}" go test -test.v

# Test a single integration, s.g.:
#     make test_integration.stack-reference
test_integration.%: bin/pulumi-language-java
	cd tests/integration && PATH="${PWD}/bin:${PATH}" go test -run "TestIntegrations/^$*$$" -test.v

test_integrations: bin/pulumi-language-java
	cd tests/integration && PATH="${PWD}/bin:${PATH}" go test -test.v

codegen_tests::
	cd ./pkg/codegen/java && go test ./...

codegen_tests_update::
	cd ./pkg/codegen/java && PULUMI_ACCEPT=true go test ./...

submodule_update::
	git submodule update --init --recursive --remote

# Borrows test case schemas from pulumi/pulumi repo linked in via git
# submodule as symlinks.
borrow_schemas:: submodule_update
	find pulumi/pkg/codegen/testing -name "schema.*" -exec ./scripts/borrow-schema.sh "{}" ";"
	find pulumi/pkg/codegen/testing/test/testdata -maxdepth 1 -name "*-*.json" -exec ./scripts/borrow-schema.sh "{}" \;


# Runs `go mod tidy` on every Go project.
tidy::
	./scripts/tidy.sh

clone_templates::
	git clone https://github.com/pulumi/templates.git
	git checkout d73ca1d9175b5763a1ccd982930f4cea48c8d7c9

clone_examples::
	git clone https://github.com/pulumi/examples.git
	git checkout 14aa33704324571cc5807afaa71f2e1e007892dd
