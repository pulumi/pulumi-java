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
	"io/fs"
	"os"
	"path/filepath"
)

func readOverlays(rootDir string, overlays []string) (map[string][]byte, error) {
	result := map[string][]byte{}
	for _, overlay := range overlays {
		overlayDir := filepath.Join(rootDir, overlay)
		err := filepath.WalkDir(overlayDir, func(path string, entry fs.DirEntry, err error) error {
			if err != nil {
				return err
			}
			if !entry.IsDir() {
				relativePath, err := filepath.Rel(overlayDir, path)
				if err != nil {
					return err
				}
				bytes, err := os.ReadFile(path)
				if err != nil {
					return err
				}
				result[relativePath] = bytes
			}
			return err
		})
		if err != nil {
			return nil, err
		}
	}
	return result, nil
}
