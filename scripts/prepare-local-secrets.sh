#!/bin/sh
set -eu

secret_directory="infra/local/secrets"
template_file="infra/local/templates/database.password.example"
secret_file="${secret_directory}/database.password"

if [ ! -r "${template_file}" ]; then
    echo "Missing synthetic local secret template: ${template_file}" >&2
    exit 1
fi

umask 077
mkdir -p "${secret_directory}"
chmod 700 "${secret_directory}"

temporary_file="${secret_file}.tmp"
trap 'rm -f "${temporary_file}"' EXIT HUP INT TERM

source_file="${secret_file}"
if [ ! -e "${secret_file}" ]; then
    source_file="${template_file}"
fi

secret_value=$(cat "${source_file}")
if [ -z "${secret_value}" ]; then
    echo "Synthetic local secret must not be empty" >&2
    exit 1
fi

printf '%s' "${secret_value}" > "${temporary_file}"
mv "${temporary_file}" "${secret_file}"

chmod 600 "${secret_file}"

if git ls-files --error-unmatch "${secret_file}" >/dev/null 2>&1; then
    echo "Local secret must never be tracked: ${secret_file}" >&2
    exit 1
fi

if ! git check-ignore -q "${secret_file}"; then
    echo "Local secret is not protected by .gitignore: ${secret_file}" >&2
    exit 1
fi
