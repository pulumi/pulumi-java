#!/usr/bin/env bash

# When committing new PRs, this script builds and commits:
# - the new output codegen tests
# - each provider in providers(if it's added to make providers_all)

set -euo pipefail

git pull 2&> /dev/null || true
base_branch="$(git branch --show-current)"

echo "$base_branch" | grep -v '^codegen/' || (echo 'ERROR currently on codegen branch' && exit 1)

codegen_branch="codegen/$(git branch --show-current)"

git checkout -b "$codegen_branch"

git checkout 'origin/main' 'providers'
make providers_all

for provider in providers/*/; do
    [[ -e "$provider" ]] || break  # handle the case of no directories
    echo adding "$provider"
    git add "$provider" 1>/dev/null || echo "git add returned $? for $provider"
    git commit -q -m "[codegen] built and added added $provider" || echo "git commit returned $? for $provider"
done

git push --set-upstream origin "$codegen_branch"
git checkout "$base_branch"
