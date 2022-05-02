#!/usr/bin/env bash

set -euo pipefail

for provider in providers/pulumi-*
do
    provider=${provider#"providers/pulumi-"}
    make "provider.$provider.generate"
done
