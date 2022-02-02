SCHEMA_REF = master
SCHEMA_URL = https://raw.githubusercontent.com/pulumi/pulumi-azure-native/${SCHEMA_REF}/provider/cmd/pulumi-resource-azure-native/schema.json

export PATH:=./bin:$(PATH)

build::	bin/pulumi-java-gen bin/schema.json
	pulumi-java-gen -schema bin/schema.json -out sdk/java

bin/schema.json:
	mkdir -p bin
	curl ${SCHEMA_URL} -o bin/schema.json

bin/pulumi-java-gen:
	mkdir -p bin
	cd ../../pkg/cmd/pulumi-java-gen && go build -o ../../../providers/pulumi-azure-native/bin

clean::
	rm -rf bin
