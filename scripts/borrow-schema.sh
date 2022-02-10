#!/usr/bin/env bash

set -euo pipefail


SRC="$1"
DEST=${SRC#pulumi/}
DEST_DIR=$(dirname "$DEST")

mkdir -p "$DEST_DIR"
rm -f "$DEST"
ln --relative -s -t "$DEST_DIR" "$SRC"
