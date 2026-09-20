#!/bin/sh
set -eu

: "${PROPRACTIX_SITE_ADDRESS:?set an approved HTTPS site address}"
curl --fail --silent --show-error --retry 12 --retry-delay 5 "${PROPRACTIX_SITE_ADDRESS%/}/healthz"
curl --fail --silent --show-error --retry 12 --retry-delay 5 "${PROPRACTIX_SITE_ADDRESS%/}/readyz"
