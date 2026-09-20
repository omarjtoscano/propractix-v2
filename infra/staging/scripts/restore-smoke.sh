#!/bin/sh
set -eu

infra/staging/scripts/require-deployment-gate.sh

: "${PROPRACTIX_BACKUP_URI:?set the approved backup object URI}"
: "${PROPRACTIX_DB_USER:=propractix}"
restore_database="propractix_restore_smoke_$(date +%s)"

cleanup() {
    docker compose -f infra/staging/compose.yaml exec -T postgres \
        dropdb --if-exists -U "${PROPRACTIX_DB_USER}" "${restore_database}"
}
trap cleanup EXIT INT TERM

docker compose -f infra/staging/compose.yaml exec -T postgres \
    createdb -U "${PROPRACTIX_DB_USER}" "${restore_database}"
aws s3 cp "${PROPRACTIX_BACKUP_URI}" - \
    | docker compose -f infra/staging/compose.yaml exec -T postgres \
        pg_restore --exit-on-error --no-owner --no-acl \
        -U "${PROPRACTIX_DB_USER}" -d "${restore_database}"

echo "Encrypted backup restored successfully into an isolated smoke database"
