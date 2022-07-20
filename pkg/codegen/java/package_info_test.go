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

package java

import (
	"testing"

	"github.com/blang/semver"
	"github.com/stretchr/testify/assert"
)

func TestWithJavaSdkDependencyDefault(t *testing.T) {
	sdk := "com.pulumi:pulumi"
	v010 := semver.MustParse("0.1.0")
	v050 := semver.MustParse("0.5.0")

	assert.Equal(t, v010.String(),
		PackageInfo{}.
			WithJavaSdkDependencyDefault(v010).
			Dependencies[sdk])

	assert.Equal(t, v050.String(),
		PackageInfo{Dependencies: map[string]string{sdk: v050.String()}}.
			WithJavaSdkDependencyDefault(v010).
			Dependencies[sdk])
}
