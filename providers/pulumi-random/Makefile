# TODO this needs a decision on how we version the JVM SDK for
# pulumi-random, and likely needs to take into account the version of
# the pulumi-random provider itself, which is currently implied in
# go.mod refs and not synced with the number here.
VERSION := 4.0.0

SCHEMA_VERSION := 4.3.1

SCHEMA = https://raw.githubusercontent.com/pulumi/pulumi-random/v${SCHEMA_VERSION}/provider/cmd/pulumi-resource-random/schema.json

OUT = sdk/java

export PATH:=../../bin:$(PATH)


build::	generate
	cd ${OUT} && gradle build


generate::
	rm -rf ${OUT}
	pulumi-java-gen -schema ${SCHEMA} -out ${OUT}
	mkdir -p ${OUT}/src/main/resources/io/pulumi/random
	echo ${VERSION} > ${OUT}/src/main/resources/io/pulumi/random/version.txt


install::	build
	cd ${OUT} && gradle -Pversion=${VERSION} publishToMavenLocal
