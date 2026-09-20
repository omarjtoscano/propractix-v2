#!/bin/sh
set -eu

case "${CL0_CLOUD_STAGING:-PENDING}" in
    READY_FOR_APPLICATION|CLOSED)
        ;;
    PENDING)
        echo "CL0_CLOUD_STAGING is PENDING; deployment and AWS evidence are blocked" >&2
        exit 1
        ;;
    *)
        echo "Unknown CL0_CLOUD_STAGING state: ${CL0_CLOUD_STAGING}" >&2
        exit 1
        ;;
esac
