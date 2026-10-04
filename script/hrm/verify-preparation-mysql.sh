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
mysql_exec -e 'CREATE DATABASE preparation_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
python3 - "$repo_root/sql/mysql/ruoyi-vue-pro.sql" <<'PY' | mysql_exec preparation_verify
import re,sys
print(re.search(r'CREATE TABLE `system_menu`\s*\([\s\S]*?;',open(sys.argv[1]).read()).group())
PY
mysql_exec preparation_verify < "$repo_root/sql/mysql/upgrade/20261004-hrm-payroll-requirements.sql"
mysql_exec preparation_verify < "$repo_root/sql/mysql/upgrade/20261004-hrm-payroll-preparation.sql"
mysql_exec preparation_verify -e "UPDATE system_menu SET name='保留已配置名称' WHERE permission='hrm:payroll:preparation:query'; INSERT INTO hrm_payroll_requirement(code,module_code,title,tenant_id) VALUES('CUSTOM-MIGRATION','req','保留已有资料',1)"
mysql_exec preparation_verify < "$repo_root/sql/mysql/upgrade/20261004-hrm-payroll-preparation.sql"
mysql_exec preparation_verify <<'SQL'
DELIMITER //
CREATE PROCEDURE assert_preparation_migration()
BEGIN
 IF (SELECT COUNT(*) FROM system_menu WHERE permission='hrm:payroll:preparation:query' AND deleted=0) <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate preparation menu';
 END IF;
 IF (SELECT COUNT(*) FROM system_menu WHERE name='保留已配置名称' AND component='hrm/payroll/preparation/index' AND component_name='HrmPayrollPreparation' AND path='payroll-preparation') <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Menu was overwritten or route is wrong';
 END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_requirement WHERE title='保留已有资料' AND tenant_id=1) <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Read-only overview migration changed business records';
 END IF;
 IF (SELECT COUNT(*) FROM hrm_payroll_review) <> 0 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Overview migration created review conclusions';
 END IF;
END//
DELIMITER ;
CALL assert_preparation_migration();
DROP PROCEDURE assert_preparation_migration;
SQL
echo 'PASS: preparation menu repeatability, configured name preservation, route and unchanged business/review records'
