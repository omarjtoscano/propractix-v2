#!/bin/sh
set -eu

: "${PROPRACTIX_SERVER_IMAGE:?set a server image pinned by digest}"
: "${PROPRACTIX_CLIENT_IMAGE:?set a client image pinned by digest}"
: "${PROPRACTIX_RELEASE_SHA:?set the full release SHA}"
: "${PROPRACTIX_SITE_ADDRESS:?set an approved HTTPS site address}"
: "${PROPRACTIX_DB_PASSWORD_FILE:?set the external database password file}"

validate_image() {
    reference="$1"
    digest="${reference##*@sha256:}"
    case "${reference}" in
        *@sha256:*) ;;
        *) echo "Image must be pinned by digest: ${reference}" >&2; exit 1 ;;
    esac
    if [ "${#digest}" -ne 64 ] || ! printf '%s' "${digest}" | grep -Eq '^[0-9a-f]{64}$'; then
        echo "Invalid image digest: ${reference}" >&2
        exit 1
    fi
}

validate_image "${PROPRACTIX_SERVER_IMAGE}"
validate_image "${PROPRACTIX_CLIENT_IMAGE}"

if [ "${#PROPRACTIX_RELEASE_SHA}" -ne 40 ] \
        || ! printf '%s' "${PROPRACTIX_RELEASE_SHA}" | grep -Eq '^[0-9a-f]{40}$'; then
    echo "PROPRACTIX_RELEASE_SHA must be a full Git SHA" >&2
    exit 1
fi

if [ ! -r "${PROPRACTIX_DB_PASSWORD_FILE}" ]; then
    echo "Database password file is not readable" >&2
    exit 1
fi

docker compose -f infra/staging/compose.yaml config --quiet
