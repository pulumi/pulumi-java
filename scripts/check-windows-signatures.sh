#!/usr/bin/env bash

set -euo pipefail

ROOT_CA="$(dirname "$0")/microsoft-identity-verification-root-ca-2020.pem"
EXPECTED_SIGNER_CN="CN=PULUMI CORPORATION"
EXPECTED_SIGNER_O="O=PULUMI CORPORATION"

if [[ $# -eq 0 ]]; then
    echo "no binaries to check" >&2
    exit 1
fi

failed=0
for binary in "$@"; do
    if ! output="$(osslsigncode verify -CAfile "${ROOT_CA}" -TSA-CAfile "${ROOT_CA}" -in "${binary}" 2>&1)"; then
        echo "invalid signature: ${binary}"
        echo "${output}" | tail -5
        failed=1
    else
        subject="$(echo "${output}" | grep -m1 -A1 'Signer #0:' | grep -m1 'Subject:' || true)"
        if ! echo "${subject}" | grep -q "${EXPECTED_SIGNER_CN}" || ! echo "${subject}" | grep -q "${EXPECTED_SIGNER_O}"; then
            echo "unexpected signer: ${binary}"
            echo "${output}" | grep -m1 -A1 'Signer #0:' | head -2
            failed=1
        else
            echo "signed: ${binary}"
        fi
    fi
done

exit "${failed}"
