#!/bin/sh
set -eu

infra/staging/scripts/require-deployment-gate.sh

: "${PROPRACTIX_BACKUP_URI:?set the approved external backup destination}"
: "${PROPRACTIX_BACKUP_KMS_KEY_ID:?set the approved backup encryption key}"
: "${PROPRACTIX_DB_USER:=propractix}"
: "${PROPRACTIX_DB_NAME:=propractix}"

backup_file="$(mktemp /tmp/propractix-backup.XXXXXX)"
trap 'rm -f "${backup_file}"' EXIT HUP INT TERM
chmod 600 "${backup_file}"

docker compose -f infra/staging/compose.yaml exec -T postgres \
    pg_dump --format=custom --no-owner --no-acl -U "${PROPRACTIX_DB_USER}" -d "${PROPRACTIX_DB_NAME}" \
    > "${backup_file}"

aws s3 cp "${backup_file}" "${PROPRACTIX_BACKUP_URI}" \
    --sse aws:kms --sse-kms-key-id "${PROPRACTIX_BACKUP_KMS_KEY_ID}"
