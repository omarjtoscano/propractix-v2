#!/bin/sh
set -eu

docker compose -f infra/staging/compose.yaml --profile migration run --rm migrate
