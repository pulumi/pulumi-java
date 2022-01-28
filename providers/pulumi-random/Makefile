export PATH:=./bin:$(PATH)

# build the java sdk
build_java:: ensure bin/pulumi-tfgen-random bin/pulumi-tfgen-external-language-java
	which pulumi-tfgen-random
	pulumi-tfgen-random external:java --out sdk/java

ensure::
	go mod tidy

bin/pulumi-tfgen-external-language-java:
	mkdir -p bin
	cd ../../pkg/cmd/pulumi-tfgen-external-language-java && go build -o ../../../providers/pulumi-random/bin

# Using the trick to rebuild `pulumi-tfgen-random` from source.
# Consider in the future publishing the binary from
# `pulumi/pulumi-random` and reusing here.
#
# Supporting files to do the trick:
#
# - `go.mod` defines which Git hash to grab
#
# - `cmd/pulumi-tfgen-random/main.go` copy-pasted from
#   `pulumi/pulumi-random`
bin/pulumi-tfgen-random:
	mkdir -p bin
	go build -o bin \
		-ldflags "-X github.com/pulumi/pulumi-random/provider/v4/pkg/version.Version=4.3.0-alpha.1643302200" \
		github.com/pulumi-java/providers/pulumi-random/cmd/pulumi-tfgen-random

clean::
	rm -rf bin
