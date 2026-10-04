#!/usr/bin/env bash
set -euo pipefail

# Run only in a new, isolated test container. No ports or host database access.
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
mysql_image=${MYSQL_TEST_IMAGE:-mysql:8.0.46}
container_name="hrm-baseline-test-${RANDOM}-${RANDOM}"
container_id=''
export MYSQL_ROOT_PASSWORD
MYSQL_ROOT_PASSWORD=$(python3 -c 'import secrets; print(secrets.token_hex(24))')

cleanup() {
  if [ -n "$container_id" ]; then
    docker rm -f "$container_id" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT
container_id=$(docker run --detach --name "$container_name" --network none \
  --env MYSQL_ROOT_PASSWORD "$mysql_image")

ready=false
for attempt in $(seq 1 120); do
  if docker exec "$container_id" sh -c \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e "SELECT 1" >/dev/null 2>&1'; then
    ready=true
    break
  fi
  sleep 1
done
if [ "$ready" != true ]; then
  echo 'MySQL test container did not become ready.' >&2
  exit 1
fi

mysql_exec() {
  docker exec -i "$container_id" sh -c \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 -uroot "$@"' sh "$@"
}

mysql_exec -e 'CREATE DATABASE hrm_baseline_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
migration="$repo_root/sql/mysql/upgrade/20261004-hrm-bpm-baseline.sql"
mysql_exec hrm_baseline_verify < "$migration"
mysql_exec hrm_baseline_verify -e \
  "INSERT INTO bpm_category (name, code, status, tenant_id) VALUES ('baseline-repeatability-check', 'baseline-test', 0, 1)"
mysql_exec hrm_baseline_verify < "$migration"
mysql_exec hrm_baseline_verify < "$repo_root/script/hrm/verify-mysql-baseline.sql"
echo 'PASS: MySQL HRM/BPM migration, repeatability, historical dates, precision and serialized values.'
