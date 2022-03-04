#!/usr/bin/env bash

set -euo pipefail


git pull
git checkout 'origin/main' 'providers'
git checkout 'origin/main' 'pkg/codegen/testing'



