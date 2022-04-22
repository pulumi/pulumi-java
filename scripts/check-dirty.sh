#!/usr/bin/env bash

set -euo pipefail

git status -s -- providers

[[ -n $(git status -s -- providers) ]] && echo "providers/ generated code is out of date" && exit 1
