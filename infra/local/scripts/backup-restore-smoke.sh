#!/bin/sh
set -eu

source_database="propractix_h01_backup_source"
backup_file=".local/backups/h01-synthetic.dump"

cleanup() {
    docker compose -f infra/compose.yaml exec -T postgres \
        dropdb --if-exists -U propractix "${source_database}" >/dev/null
    rm -f "${backup_file}"
}
trap cleanup EXIT HUP INT TERM

cleanup
docker compose -f infra/compose.yaml exec -T postgres \
    createdb -U propractix "${source_database}"
docker compose -f infra/compose.yaml exec -T postgres \
    psql -v ON_ERROR_STOP=1 -U propractix -d "${source_database}" -c \
    "CREATE TABLE h01_backup_restore_probe (id integer PRIMARY KEY, marker text NOT NULL); INSERT INTO h01_backup_restore_probe VALUES (1, 'H01_SYNTHETIC_BACKUP_RESTORE_OK');"

PROPRACTIX_BACKUP_SOURCE_DB="${source_database}" \
    infra/local/scripts/backup.sh "${backup_file}"
infra/local/scripts/restore-smoke.sh "${backup_file}"

echo "Local H01 backup/restore smoke test passed"
