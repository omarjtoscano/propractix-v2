#!/bin/sh
set -eu

infra/staging/scripts/require-deployment-gate.sh

infra/staging/scripts/preflight.sh
docker compose -f infra/staging/compose.yaml pull postgres server client
infra/staging/scripts/migrate.sh
docker compose -f infra/staging/compose.yaml up -d --no-build postgres server client

if ! infra/staging/scripts/smoke.sh; then
    if [ -n "${PROPRACTIX_PREVIOUS_ENV_FILE:-}" ]; then
        infra/staging/scripts/rollback.sh
    fi
    exit 1
fi

echo "Staging release passed readiness and smoke checks"
