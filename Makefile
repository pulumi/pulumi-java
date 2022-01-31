# Composite targets simplify local dev scenarios

build::	ensure build_codegen build_sdk

ensure::	ensure_codegen ensure_sdk


# Java codegen is a Go project rooted at `pkg/`.

build_codegen::	ensure_codegen
	cd pkg && go build -v all

test_codegen:: build_codegen
	cd pkg && go test -test.v ./...

ensure_codegen::
	cd pkg && go mod tidy


# Java SDK is a gradle project rooted at `sdk/jvm`

install_sdk::
	cd sdk/jvm && make install

build_sdk::
	cd sdk/jvm && make build

ensure_sdk::
	cd sdk/jvm && make ensure
