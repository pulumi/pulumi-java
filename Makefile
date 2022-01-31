# Composite targets simplify local dev scenarios

build::	ensure build_go build_sdk

ensure::	ensure_go ensure_sdk


# Go project rooted at `pkg/` implements Pulumi JVM language plugin
# and Java go as a Go library.

build_go::	ensure_go
	cd pkg && go build -v all

test_go:: build_go
	cd pkg && go test -test.v ./...

ensure_go::
	cd pkg && go mod tidy

bin/pulumi-language-jvm:	pkg
	mkdir -p bin
	cd pkg && go build -o ../bin github.com/pulumi/pulumi-java/pkg/cmd/pulumi-language-jvm

# Java SDK is a gradle project rooted at `sdk/jvm`

install_sdk::
	cd sdk/jvm && make install

build_sdk::
	cd sdk/jvm && make build

ensure_sdk::
	cd sdk/jvm && make ensure

# pulumi-random provider Java SDKs built from providers/pulumi-random:

ensure_random::
	cd providers/pulumi-random && make ensure

build_random::
	cd providers/pulumi-random && make build

install_random::
	cd providers/pulumi-random && make install

# Integration tests will use PULUMI_ACCESS_TOKEN to provision tests
# stacks in Pulumi service.
integration_tests::	bin/pulumi-language-jvm
	cd tests/examples && PATH=${PATH}:${PWD}/bin go test -run TestJava -test.v
