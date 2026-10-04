#!/usr/bin/env bash
set -euo pipefail
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
container_id=''
export MYSQL_ROOT_PASSWORD
MYSQL_ROOT_PASSWORD=$(python3 -c 'import secrets; print(secrets.token_hex(24))')
cleanup() { if [ -n "$container_id" ]; then docker rm -f "$container_id" >/dev/null 2>&1 || true; fi; }
trap cleanup EXIT
container_id=$(docker run --detach --network none --env MYSQL_ROOT_PASSWORD "${MYSQL_TEST_IMAGE:-mysql:8.0.46}")
ready=false
for attempt in $(seq 1 120); do
  if docker exec "$container_id" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e "SELECT 1" >/dev/null 2>&1'; then ready=true; break; fi
  sleep 1
done
if [ "$ready" != true ]; then echo 'Isolated MySQL did not become ready.' >&2; exit 1; fi
mysql_exec() { docker exec -i "$container_id" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 -uroot "$@"' sh "$@"; }
mysql_exec -e 'CREATE DATABASE collection_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
# Use the actual system menu schema, without loading accounts or business seed data.
python3 - "$repo_root/sql/mysql/ruoyi-vue-pro.sql" <<'PY' | mysql_exec collection_verify
import re, sys
source=open(sys.argv[1]).read()
print(re.search(r'CREATE TABLE `system_menu`\s*\([\s\S]*?;',source).group())
PY
migration="$repo_root/sql/mysql/upgrade/20261004-hrm-payroll-requirements.sql"
mysql_exec collection_verify < "$migration"
mysql_exec collection_verify -e "INSERT INTO hrm_payroll_requirement(code,module_code,title,source_codes,tenant_id) VALUES ('CUSTOM-MIGRATION','req','重复迁移保留样例','[\"DS-01\"]',1)"
mysql_exec collection_verify < "$migration"
mysql_exec collection_verify < "$repo_root/script/hrm/verify-collection-mysql.sql"
echo 'PASS: collection DDL, repeatability, menu uniqueness, large snapshots and tenant-scoped codes.'
