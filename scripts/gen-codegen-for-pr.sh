#!/usr/bin/env bash

set -euo pipefail

git pull 2&> /dev/null || true
base_branch="$(git branch --show-current)"

echo $base_branch | grep -v '^codegen/' || (echo 'ERROR currently on codegen branch' && exit -1)

codegen_branch="codegen/$(git branch --show-current)"

git checkout -b "$codegen_branch"

git checkout 'origin/main' 'pkg/codegen/testing'
PULUMI_ACCEPT=true make install_sdk codegen_tests
git add -u 'pkg/codegen/testing'
git commit -m "[codegen] built and added added codegen_tests"

git checkout 'origin/main' 'providers'
make providers_all

for provider in `ls providers/*/ -d`; do
    echo adding $provider
    git add -u "$provider" > /dev/null
    git commit -m "[codegen] built and added added $provider"
done;

git push --set-upstream origin "$codegen_branch"
git checkout "$base_branch"