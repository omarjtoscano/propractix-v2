#!/bin/sh
set -eu

gate_script="infra/staging/scripts/require-deployment-gate.sh"
workflow_file=".github/workflows/deploy-staging.yml"

if CL0_CLOUD_STAGING=PENDING "${gate_script}" >/dev/null 2>&1; then
    echo "PENDING must block deployment" >&2
    exit 1
fi

CL0_CLOUD_STAGING=READY_FOR_APPLICATION "${gate_script}"
CL0_CLOUD_STAGING=CLOSED "${gate_script}"

if CL0_CLOUD_STAGING=UNKNOWN "${gate_script}" >/dev/null 2>&1; then
    echo "Unknown gate states must fail closed" >&2
    exit 1
fi

grep -Fq "vars.CL0_CLOUD_STAGING == 'READY_FOR_APPLICATION' || vars.CL0_CLOUD_STAGING == 'CLOSED'" "${workflow_file}"
