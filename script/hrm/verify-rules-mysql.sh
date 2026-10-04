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
mysql_exec -e 'CREATE DATABASE rules_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
python3 - "$repo_root/sql/mysql/ruoyi-vue-pro.sql" <<'PY' | mysql_exec rules_verify
import re,sys
print(re.search(r'CREATE TABLE `system_menu`\s*\([\s\S]*?;',open(sys.argv[1]).read()).group())
PY
mysql_exec rules_verify < "$repo_root/sql/mysql/upgrade/20261004-hrm-payroll-requirements.sql"
mysql_exec rules_verify < "$repo_root/sql/mysql/upgrade/20261004-hrm-payroll-rules.sql"
mysql_exec rules_verify -e "INSERT INTO hrm_payroll_rule(code,title,category,rule_version,revision,parameters_json,cases_json,parameter_count,case_count,tenant_id) VALUES('RULE-CUSTOM-MIGRATION','保留草稿','OTHER',1,1,'[]','[]',0,0,1)"
mysql_exec rules_verify < "$repo_root/sql/mysql/upgrade/20261004-hrm-payroll-rules.sql"
mysql_exec rules_verify < "$repo_root/script/hrm/verify-rules-mysql.sql"
echo 'PASS: rule migration repeatability, three permissions, tenant/version unique keys and Unicode MEDIUMTEXT.'
