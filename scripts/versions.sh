#!/usr/bin/env bash

set -euo pipefail

echo "PULUMI_JAVA_SDK_VERSION=$(pulumictl get version --tag-pattern '^sdk')"

for provider in providers/pulumi-*
do
    provider=${provider#"providers/pulumi-"}

    # Detect prerelease tags.
    #
    # Example:
    #
    #    GITHUB_REF_TYPE=tag
    #    GITHUB_REF_NAME=awsx/v1.0.0-beta.1
    prerelease=""
    if [[ "$GITHUB_REF_TYPE" == "tag" ]]; then
        if [[ "$GITHUB_REF_NAME" == "${provider}/v"*"-"* ]]; then
            prerelease="--is-prerelease"
        fi
    fi

    tag="^${provider}/"
    var="PULUMI_${provider^^}_PROVIDER_SDK_VERSION"
    var="${var/-/_}"
    val=$(pulumictl get version --tag-pattern "$tag" $prerelease)
    echo "$var=$val"
done
