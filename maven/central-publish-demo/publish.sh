#!/usr/bin/env bash

set -euo pipefail

export SIGNING_KEY_ID="2EC87BB8"
export SIGNING_PASSWORD=$(pass gpg/support@pulumi.com/passphrase)
export SIGNING_KEY=$(cat private-key.asc)

export PULUMI_JAVA_DEMO_SDK_VERSION=0.0.1-SNAPSHOT
export OSSRH_REPO_URL=https://s01.oss.sonatype.org/content/repositories/snapshots/
export OSSRH_USERNAME=t0yv0
export OSSRH_PASSWORD=$(pass issues.sonatype.org/t0yv0/anton@pulumi.com)

gradle publish --stacktrace
