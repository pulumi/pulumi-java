#!/usr/bin/env bash

set -euo pipefail

echo "PULUMI_JAVA_SDK_VERSION=$(pulumictl get version --tag-pattern '^sdk')"

for provider in providers/pulumi-*
do
    provider=${provider#"providers/pulumi-"}
    tag="^${provider}/"
    var="PULUMI_${provider^^}_PROVIDER_SDK_VERSION"
    var="${var/-/_}"
    val=$(pulumictl get version --tag-pattern "$tag")
    echo "$var=$val"
done
