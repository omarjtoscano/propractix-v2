#!/bin/sh
set -eu

compose_file="infra/compose.yaml"
local_profile="server/src/main/resources/application-local.yaml"
secret_file="infra/local/secrets/database.password"

if grep -Eq 'POSTGRES_PASSWORD[[:space:]]*:' "${compose_file}"; then
    echo "Inline PostgreSQL passwords are forbidden in local Compose" >&2
    exit 1
fi

grep -Fq 'POSTGRES_PASSWORD_FILE: /run/secrets/database.password' "${compose_file}"
grep -Fq '127.0.0.1:8025:8025' "${compose_file}"
grep -Fq '127.0.0.1:1025:1025' "${compose_file}"
if grep -Eq 'MP_(SMTP_RELAY|SMTP_FORWARD)' "${compose_file}"; then
    echo "Mailpit relay and forwarding must remain disabled locally" >&2
    exit 1
fi
grep -Fq 'configtree:' "${local_profile}"
grep -Fq 'password: ${database.password}' "${local_profile}"
grep -Fq 'host: localhost' "${local_profile}"
grep -Fq 'port: 1025' "${local_profile}"

if git ls-files 'infra/local/secrets/*' | grep -Ev '\.example$' | grep -q .; then
    echo "Only .example local secret templates may be tracked" >&2
    exit 1
fi

if [ -e "${secret_file}" ] && ! git check-ignore -q "${secret_file}"; then
    echo "Generated local secret is not ignored" >&2
    exit 1
fi
