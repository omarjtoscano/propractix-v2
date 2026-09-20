#!/bin/sh
set -eu

backup_file="${1:-.local/backups/h01-synthetic.dump}"
restore_database="propractix_h01_restore_smoke"
expected_marker="H01_SYNTHETIC_BACKUP_RESTORE_OK"

if [ ! -s "${backup_file}" ]; then
    echo "Backup file is missing or empty: ${backup_file}" >&2
    exit 1
fi

cleanup() {
    docker compose -f infra/compose.yaml exec -T postgres \
        dropdb --if-exists -U propractix "${restore_database}" >/dev/null
}
trap cleanup EXIT HUP INT TERM

cleanup
docker compose -f infra/compose.yaml exec -T postgres \
    createdb -U propractix "${restore_database}"
docker compose -f infra/compose.yaml exec -T postgres \
    pg_restore --exit-on-error --no-owner --no-acl \
    -U propractix -d "${restore_database}" < "${backup_file}"

restored_marker="$(docker compose -f infra/compose.yaml exec -T postgres \
    psql --tuples-only --no-align -U propractix -d "${restore_database}" \
    -c 'SELECT marker FROM h01_backup_restore_probe WHERE id = 1')"

if [ "${restored_marker}" != "${expected_marker}" ]; then
    echo "Restored synthetic marker does not match" >&2
    exit 1
fi

echo "Local synthetic backup restored and verified"
