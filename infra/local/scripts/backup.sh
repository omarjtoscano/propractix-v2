#!/bin/sh
set -eu

source_database="${PROPRACTIX_BACKUP_SOURCE_DB:-propractix}"
output_file="${1:-.local/backups/propractix.dump}"

case "${source_database}" in
    *[!a-zA-Z0-9_]*) echo "Invalid source database name" >&2; exit 1 ;;
esac

output_directory="$(dirname "${output_file}")"
mkdir -p "${output_directory}"
umask 077
temporary_file="$(mktemp "${output_file}.tmp.XXXXXX")"
trap 'rm -f "${temporary_file}"' EXIT HUP INT TERM

docker compose -f infra/compose.yaml exec -T postgres \
    pg_dump --format=custom --no-owner --no-acl \
    -U propractix -d "${source_database}" > "${temporary_file}"

if [ ! -s "${temporary_file}" ]; then
    echo "Local PostgreSQL backup is empty" >&2
    exit 1
fi

mv "${temporary_file}" "${output_file}"
trap - EXIT HUP INT TERM
echo "Local synthetic backup written to ${output_file}"
