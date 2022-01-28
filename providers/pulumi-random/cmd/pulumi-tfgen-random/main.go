package main

import (
	random "github.com/pulumi/pulumi-random/provider/v4"
	"github.com/pulumi/pulumi-random/provider/v4/pkg/version"
	"github.com/pulumi/pulumi-terraform-bridge/v3/pkg/tfgen"
)

func main() {
	tfgen.Main("random", version.Version, random.Provider())
}
