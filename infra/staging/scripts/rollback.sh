#!/bin/sh
set -eu

: "${PROPRACTIX_PREVIOUS_ENV_FILE:?set the previous immutable release environment file}"

if [ ! -r "${PROPRACTIX_PREVIOUS_ENV_FILE}" ]; then
    echo "Previous release environment file is not readable" >&2
    exit 1
fi

docker compose --env-file "${PROPRACTIX_PREVIOUS_ENV_FILE}" \
    -f infra/staging/compose.yaml up -d --no-build postgres server client

echo "Image rollback requested. Applied Flyway migrations were not reverted."
