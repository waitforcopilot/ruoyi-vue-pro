#!/usr/bin/env python3
"""Prepare two synthetic departments, then verify real-login HRM department/self scope."""
import argparse
import datetime
import json
from pathlib import Path
import subprocess
from runtime_client import RuntimeClient

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--state-file', required=True)
parser.add_argument('--database', required=True)
parser.add_argument('--prepare-fixtures', action='store_true', help='Explicitly allow isolated employee fixture SQL and business API writes')
parser.add_argument('--output', type=Path)
args = parser.parse_args()
if not args.database.startswith('hrm_remediation_') or not args.database.replace('_', '').isalnum():
    parser.error('Requires an isolated hrm_remediation_ database')
c = RuntimeClient(args.state_file)
s = c.state
year, month = s.get('fixtureYear', 2026), s.get('fixtureMonth', 10)
start_date, end_date = f'{year}-01-01', f'{year}-12-31'
e = s['entityId']
review = c.login('hrmacceptreview', 'reviewerPassword')
finance = c.login('hrmacceptoperator', 'browserPassword')
if args.prepare_fixtures and 'scopeEmployeeIds' not in s:
    # Employee creation normally notifies contacts; direct SQL is only test fixture setup.
    statement = ("INSERT INTO hrm_employee(name,job_number,user_id,dept_id,entry_status,status,type,entry_time,post_name,work_city,remark,tenant_id) "
                 f"VALUES('隔离范围员工103','ACCEPT_SCOPE_103',{int(s['browserUserId'])},103,1,1,1,'{year}-01-01','验收岗位','验收城市','虚构隔离夹具',1),"
                 f"('隔离范围员工104','ACCEPT_SCOPE_104',{int(s['readerId'])},104,1,1,1,'{year}-01-01','验收岗位','验收城市','虚构隔离夹具',1); "
                 "SELECT id FROM hrm_employee WHERE job_number IN ('ACCEPT_SCOPE_103','ACCEPT_SCOPE_104') ORDER BY dept_id;")
    rows = subprocess.check_output(['docker', 'exec', '-i', 'codex-ruoyi-mysql', 'mysql', '--default-character-set=utf8mb4', '--batch', '--skip-column-names', args.database], input=statement, text=True)
    s['scopeEmployeeIds'] = [int(row) for row in rows.splitlines()]
    c.save()
    for employee, dept in zip(s['scopeEmployeeIds'], [103, 104]):
        member = c.ok('/hrm/payroll/entity/assign', {'entityId': e, 'employeeId': employee, 'deptId': dept, 'employeeCategory': 'REGULAR', 'workCity': '验收城市', 'effectiveFrom': start_date, 'effectiveTo': end_date, 'evidence': '虚构隔离归属'})
        c.query('/hrm/payroll/entity/membership/approve', {'id': member, 'reason': '独立复核隔离归属'}, review)
    policy = c.ok('/hrm/payroll/insurance-policy/policy', {'entityId': e, 'code': 'ACCEPT_SCOPE_POLICY', 'city': '验收城市', 'effectiveFrom': start_date, 'effectiveTo': end_date, 'evidence': '虚构参数，非真实地区政策', 'projects': [{'code': 'PENSION', 'name': '验收养老', 'type': 1, 'minimumBase': 3000, 'maximumBase': 20000, 'personalRate': 8, 'corporateRate': 16}]})
    c.query('/hrm/payroll/insurance-policy/policy/approve', {'id': policy, 'reason': '独立复核虚构参数'}, review)
    s['scopePolicyId'] = policy
    c.save()
    for employee in s['scopeEmployeeIds']:
        base = c.ok('/hrm/payroll/insurance-policy/base', {'entityId': e, 'employeeId': employee, 'policyId': policy, 'coverage': 'INSURED', 'incomeYear': year - 1, 'effectiveFrom': start_date, 'effectiveTo': end_date, 'declaredBases': {'PENSION': 5000}, 'evidence': '虚构缴费申报'})
        c.query('/hrm/payroll/insurance-policy/base/review', {'id': base, 'reason': '独立HR复核'}, review)
        c.query('/hrm/payroll/insurance-policy/base/approve', {'id': base, 'reason': '独立财务批准'}, finance)
    print('Prepared synthetic department fixtures')
    raise SystemExit(0)
assert len(s['scopeEmployeeIds']) == 2
own, outside = s['scopeEmployeeIds']
menu = c.ok('/system/menu/list')
original = c.ok('/system/permission/list-role-menus?roleId=' + str(s['readerRoleId']))
role = c.ok('/system/role/get?id=' + str(s['readerRoleId']))
selected = [row['id'] for row in menu if row.get('permission') in ('hrm:payroll:overview:query', 'hrm:payroll:insurance:query', 'hrm:payroll:entity:query', 'hrm:payroll:attendance:query')]
results = []
try:
    c.ok('/system/permission/assign-role-menu', {'roleId': s['readerRoleId'], 'menuIds': selected})
    for scope, depts, expected in [(2, [103], own), (5, [], outside)]:
        c.ok('/system/permission/assign-role-data-scope', {'roleId': s['readerRoleId'], 'dataScope': scope, 'dataScopeDeptIds': depts})
        reader = c.login('hrmacceptread', 'readerPassword')
        quote = c.ok(f'/hrm/payroll/insurance-policy/preview?entityId={e}&year={year}&month={month}', token=reader)
        assert set(quote) == {str(expected)}, quote
        assert quote[str(expected)]['personalInsurance'] == 400
        bases = c.ok(f'/hrm/payroll/insurance-policy/bases?entityId={e}', token=reader)
        assert {row['employeeId'] for row in bases} == {expected}, bases
        attendance = c.ok('/hrm/payroll/attendance/employees', token=reader)
        assert {row['id'] for row in attendance} == {expected}, attendance
        results.append({'dataScope': scope, 'departments': depts, 'expectedEmployeeId': expected, 'previewEmployeeIds': list(quote), 'baseEmployeeIds': [row['employeeId'] for row in bases], 'attendanceEmployeeIds': [row['id'] for row in attendance]})
finally:
    c.ok('/system/permission/assign-role-menu', {'roleId': s['readerRoleId'], 'menuIds': original})
    c.ok('/system/permission/assign-role-data-scope', {'roleId': s['readerRoleId'], 'dataScope': role['dataScope'], 'dataScopeDeptIds': role.get('dataScopeDeptIds') or []})
# A role with real management permissions must also respect the department boundary.
operator_role = c.ok('/system/role/get?id=' + str(s['browserRoleId']))
if 'batchId' not in s:
    s['batchId'] = c.ok('/hrm/salary/month-record/create', {'entityId': e, 'year': year, 'month': month, 'title': '隔离全链路验收工资表', 'startTime': f'{year}-{month:02d}-01T00:00:00', 'endTime': ((datetime.date(year, month, 28) + datetime.timedelta(days=4)).replace(day=1) - datetime.timedelta(days=1)).isoformat() + 'T00:00:00'})
    c.save()
try:
    c.ok('/system/permission/assign-role-data-scope', {'roleId': s['browserRoleId'], 'dataScope': 2, 'dataScopeDeptIds': [103]})
    operator = c.login('hrmacceptoperator', 'browserPassword')
    refused = c.request('/hrm/payroll/insurance-policy/base', {'entityId': e, 'employeeId': outside, 'policyId': s['scopePolicyId'], 'coverage': 'INSURED', 'incomeYear': year - 1, 'effectiveFrom': start_date, 'effectiveTo': end_date, 'declaredBases': {'PENSION': 6000}, 'evidence': '越权负向验收'}, token=operator)
    assert refused['code'] not in (0, 500), refused
    batch = c.request('/hrm/salary/month-record/get?id=' + str(s['batchId']), token=operator)
    assert (batch['code'] == 0 and batch.get('data') is None) or batch['code'] not in (0, 500), batch
    page = c.ok('/hrm/payroll/batch/page?pageNo=1&pageSize=100', token=operator)
    assert s['batchId'] not in {row['id'] for row in page['list']}, page
    results.append({'case': 'manager-cannot-write-outside-department', 'code': refused['code']})
    results.append({'case': 'mixed-department-batch-hidden-from-detail-and-list', 'batchId': s['batchId'], 'detailCode': batch['code']})
finally:
    c.ok('/system/permission/assign-role-data-scope', {'roleId': s['browserRoleId'], 'dataScope': operator_role['dataScope'], 'dataScopeDeptIds': operator_role.get('dataScopeDeptIds') or []})
if args.output:
    args.output.write_text(json.dumps({'capturedAt': datetime.datetime.now().isoformat(), 'database': args.database, 'cases': results}, ensure_ascii=False, indent=2))
print('Passed: real custom-department and self scope across amounts, bases and employee candidates')
