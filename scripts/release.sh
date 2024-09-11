#!/usr/bin/env bash
#
# Example:
#
# ./scripts/release.sh v0.5.2

set -euo pipefail

VERSION="$1"

git tag ${VERSION}
git tag pkg/${VERSION}
git tag sdk/${VERSION}
git push origin ${VERSION}     # releases pulumi-language-java to GitHub Releases
git push origin pkg/${VERSION} # "releases" Go code so that go can link v0.5.0
git push origin sdk/${VERSION} # releases Java SDK to Maven Central
