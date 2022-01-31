# TODO this needs a decision on how we version the JVM SDK for
# pulumi-random, and likely needs to take into account the version of
# the pulumi-random provider itself, which is currently implied in
# go.mod refs and not synced with the number here.
VERSION := 4.0.0

export PATH:=./bin:$(PATH)

# build the java sdk
build:: ensure bin/pulumi-tfgen-random bin/pulumi-tfgen-external-language-java
	which pulumi-tfgen-random
	pulumi-tfgen-random external:java --out sdk/java
	mkdir -p sdk/java/src/main/resources/io/pulumi/random
	echo ${VERSION} > sdk/java/src/main/resources/io/pulumi/random/version.txt
	cd sdk/java && gradle build

install::	build
	cd sdk/java && gradle -Pversion=${VERSION} publishToMavenLocal

ensure::
	go mod tidy
	pulumi plugin install resource random v4.0.0

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
