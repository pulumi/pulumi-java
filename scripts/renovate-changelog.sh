#!/usr/bin/env bash

# Generate a changie fragment for a dependency bump that Renovate applied.
#
# Invoked as `make renovate DEP=<name> VERSION=<new version> FILE=<package file>` from a
# renovate postUpgradeTasks hook (see renovate.json5), which fills the arguments from its
# {{{depName}}}/{{{newVersion}}}/{{{packageFile}}} template variables, once per updated
# dependency. Only released artifacts get entries: the Go module at the repo root (the
# language plugin) and the Java SDK under sdk/java/. No-op for anything else (CI,
# integration tests, test fixtures).
#
# Runs before the PR exists, so fragments carry no PR number; the `dependencies` kind in
# .changie.yaml is formatted accordingly.

set -euo pipefail

dep="$1"
version="$2"
file="$3"

case "$file" in
    go.mod) component=runtime ;;
    sdk/java/*test*) exit 0 ;;
    sdk/java/*) component=sdk ;;
    *) exit 0 ;;
esac

slug=$(printf '%s-%s' "$dep" "$version" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-')
slug=${slug#-}
slug=${slug%-}

# Pinned to the changie version in .mise.toml; `go run` because the renovate container
# has Go but not our mise toolchain.
go run github.com/miniscruff/changie@v1.24.2 new \
    --dry-run \
    --component "$component" \
    --kind dependencies \
    --body "Update $dep to $version" \
    > ".changes/unreleased/dependencies-$slug.yaml"
