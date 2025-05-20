// Copyright 2016-2022, Pulumi Corporation.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"fmt"
	"os"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"
)

func main() {
	fmt.Printf("pulumi-java-gen is deprecated, use `pulumi package gen-sdk --language java` instead\n")
	if err := newPulumiJavaGenCmd().Execute(); err != nil {
		_, err := fmt.Fprintf(os.Stderr, "An error occurred: %v\n", err)
		contract.IgnoreError(err)
		os.Exit(1)
	}
}
