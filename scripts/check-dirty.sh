#!/usr/bin/env bash

set -euo pipefail

git status -s -- providers

if [[ -n $(git status -s -- providers) ]]; then
    echo "providers/ generated code is out of date"
    exit 1
fi
