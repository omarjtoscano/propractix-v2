#!/bin/sh
set -eu

canonical_directory="server/src/main/resources/db/migration"
unexpected="$(find server/src -type f -name 'V*.sql' ! -path "${canonical_directory}/*" -print)"

if [ -n "${unexpected}" ]; then
    echo "Flyway migrations outside ${canonical_directory}:" >&2
    echo "${unexpected}" >&2
    exit 1
fi
