SCHEMA_REF = master

export PATH:=./bin:$(PATH)

define generate_sdk
	rm -rf bin/$(1)
	mkdir -p bin/$(1)
	curl https://raw.githubusercontent.com/pulumi/pulumi-$(1)/master/provider/cmd/pulumi-resource-$(1)/schema.json -o bin/$(1)/schema.json
	bin/pulumi-java-gen -schema bin/$(1)/schema.json -out bin/$(1)
endef

build::	bin/aws-native bin/kubernetes bin/aws-native bin/google-native bin/azure-native

bin/aws-native: bin/pulumi-java-gen
	$(call generate_sdk,aws-native)

bin/kubernetes: bin/pulumi-java-gen
	$(call generate_sdk,kubernetes)

bin/azure-native: bin/pulumi-java-gen
	$(call generate_sdk,azure-native)

bin/google-native: bin/pulumi-java-gen
	$(call generate_sdk,google-native)

bin/pulumi-java-gen: bin
	cd ../pkg && go build -o ../providers/bin/ github.com/pulumi/pulumi-java/pkg/cmd/pulumi-java-gen

bin:
	mkdir -p bin