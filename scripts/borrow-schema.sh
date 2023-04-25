#!/usr/bin/env bash

set -euo pipefail

SRC="$1"
DEST=${SRC#pulumi/}
DEST_DIR=$(dirname "$DEST")

mkdir -p "$DEST_DIR"
rm -f "$DEST"

LN="ln"
if [[ "$(uname)" == "Darwin" ]]; then
    # macOS uses BSD ln, which doesn't support --relative.
    # Look for GNU ln, which is installed as gln by Homebrew,
    # or fail if it's not found.
    if command -v gln >/dev/null 2>&1; then
        LN=$(command -v gln)
    else
        echo >&2 "error: gln not found. Please install coreutils:"
        echo >&2 "  brew install coreutils"
        exit 1
    fi
fi

"$LN" --relative -s -t "$DEST_DIR" "$SRC"