build_codegen::	ensure_codegen
	cd pkg && go build -v all

test_codegen:: build_codegen
	cd pkg && go test -test.v ./...

ensure_codegen::
	cd pkg && go mod tidy
