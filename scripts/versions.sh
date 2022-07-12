#!/usr/bin/env bash

set -euo pipefail

echo "PULUMI_JAVA_SDK_VERSION=$(pulumictl get version --tag-pattern '^sdk')"
