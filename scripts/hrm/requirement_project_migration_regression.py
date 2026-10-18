"""Run F-01a twice in a NEW isolated local MySQL fixture database; never drop databases."""
import argparse
import datetime
from pathlib import Path
import re
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--container', default='codex-ruoyi-mysql')
parser.add_argument('--database', default='hrm_f01a_test_' + datetime.datetime.now().strftime('%Y%m%d%H%M%S'))
args = parser.parse_args()
if not re.fullmatch(r'hrm_f01a_test_[0-9]+', args.database):
    parser.error('Only a new hrm_f01a_test_<digits> fixture database is allowed')

def mysql(query, database=None):
    command = ['docker', 'exec', '-i', args.container, 'mysql', '--batch', '--raw', '--skip-column-names']
    if database:
        command.append(database)
    result = subprocess.run(command, input=query, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError('Local fixture SQL failed; no production database is modified')
    return result.stdout

# CREATE without IF NOT EXISTS prevents accidentally reusing or overwriting fixtures.
mysql('CREATE DATABASE `' + args.database + '` CHARACTER SET utf8mb4;')
def sql(query):
    return mysql(query, args.database)
sql('''CREATE TABLE hrm_payroll_requirement (
 id bigint PRIMARY KEY, tenant_id bigint NOT NULL, code varchar(64) NOT NULL,
 version int NOT NULL, status varchar(32), confirmed_by bigint,
 deleted bit DEFAULT 0, create_time datetime, update_time datetime,
 UNIQUE (tenant_id,code));
CREATE TABLE hrm_payroll_requirement_history (id bigint PRIMARY KEY, requirement_id bigint,
 tenant_id bigint, version int, snapshot text);
INSERT INTO hrm_payroll_requirement VALUES
 (101,1,'PLAN-01',7,'CONFIRMED',9,0,'2026-01-01','2026-02-01'),
 (202,2,'PLAN-01',3,'PENDING',NULL,0,'2026-01-02','2026-02-02'),
 (203,2,'PLAN-02',4,'REVIEWING',NULL,1,'2026-01-03','2026-02-03');
INSERT INTO hrm_payroll_requirement_history VALUES
 (1,101,1,7,'{"id":101,"original":true}'),(2,202,2,3,'{"id":202,"original":true}');''')
read_rows = 'SELECT id,tenant_id,code,version,status,confirmed_by,deleted,create_time,update_time FROM hrm_payroll_requirement ORDER BY id;'
read_history = 'SELECT * FROM hrm_payroll_requirement_history ORDER BY id;'
before = (sql(read_rows), sql(read_history))
migration = (Path(__file__).resolve().parents[2] / 'sql/mysql/upgrade/20261018-hrm-requirement-legacy-project.sql').read_text()
projects = None
for replay in (1, 2):
    sql(migration)
    assert (sql(read_rows), sql(read_history)) == before, 'Old rows or snapshots changed'
    assert sql('SELECT COUNT(*) FROM hrm_payroll_requirement_project;').strip() == '2'
    assert sql('SELECT COUNT(*) FROM hrm_payroll_requirement r JOIN hrm_payroll_requirement_project p ON p.id=r.project_id WHERE r.tenant_id<>p.tenant_id;').strip() == '0'
    assert sql('SELECT COUNT(*) FROM hrm_payroll_requirement WHERE project_id IS NULL;').strip() == '0'
    current = sql('SELECT id,tenant_id,project_key,metadata_version,project_scope_version FROM hrm_payroll_requirement_project ORDER BY id;')
    if projects is not None:
        assert current == projects, 'Project identity changed on replay'
    projects = current
print('PASS two-tenant migration, deleted-row ownership, old fields/history preservation and replay:', args.database)
