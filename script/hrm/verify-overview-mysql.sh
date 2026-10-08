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
mysql_exec -e 'CREATE DATABASE overview_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
python3 - "$repo_root/sql/mysql/ruoyi-vue-pro.sql" <<'PY' | mysql_exec overview_verify
import re,sys
s=open(sys.argv[1]).read()
for name in ['system_menu','system_role_menu']:
 print(re.search(r'CREATE TABLE `'+name+r'`\s*\([\s\S]*?;',s).group())
print("INSERT INTO system_menu(id,name,type,parent_id,path) VALUES(1,'隔离 HRM 菜单',1,0,'/hrm');")
print("INSERT INTO system_role_menu(role_id,menu_id,tenant_id) VALUES(1,1,1);")
PY
mysql_exec overview_verify < "$repo_root/sql/mysql/upgrade/20261008-hrm-payroll-overview.sql"
mysql_exec overview_verify < "$repo_root/sql/mysql/upgrade/20261008-hrm-payroll-overview.sql"
result=$(mysql_exec -N overview_verify -e "SELECT COUNT(*) FROM system_menu WHERE permission='hrm:payroll:overview:query' AND component='hrm/payroll/overview/index' AND component_name='HrmPayrollOverview' AND deleted=0; SELECT COUNT(*) FROM system_role_menu; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE 'hrm_%';")
test "$result" = $'1\n1\n0'
echo 'PASS: repeated overview migration registers one entry, preserves existing role grants and creates no payroll data tables.'
