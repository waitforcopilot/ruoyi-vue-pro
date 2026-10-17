#!/usr/bin/env python3
"""Pending runtime matrix for OA approval, three work systems and time-off ledger.

Preparation and real OA submission were exercised. The first approval exposed the
optional-mobile notification blocker. The user moved execution to local machines
before a post-fix rerun; this script is NOT evidence that the full matrix passed.
"""
import argparse
import datetime
from decimal import Decimal
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import time
import urllib.parse
from runtime_client import RuntimeClient

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--base-state', required=True)
p.add_argument('--state-file', required=True)
p.add_argument('--database', required=True)
p.add_argument('--output-dir', type=Path, required=True)
a = p.parse_args()
if not re.fullmatch(r'hrm_remediation_[A-Za-z0-9_]+', a.database):
    p.error('Only isolated remediation databases are supported')
state = Path(a.state_file).resolve()
root = Path(__file__).resolve().parents[2]
if root in state.parents or state.is_symlink():
    p.error('Keep credentials outside repository')
if not state.exists():
    base = json.loads(Path(a.base_state).read_text())
    fd = os.open(state, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    with os.fdopen(fd, 'w') as out:
        json.dump({k: v for k, v in base.items() if not k.startswith('e2e')}, out)
c = RuntimeClient(state)
s = c.state
review = c.login('hrmacceptreview', 'reviewerPassword')
reason = '虚构隔离三类工时验收，缩短周期基准仅供独立金额验算，非正式工时批准文件'
a.output_dir.mkdir(parents=True, exist_ok=True)

def remember(key, create):
    if key not in s:
        s[key] = create()
        c.save()
    return s[key]

def sql(text):
    return subprocess.check_output(['docker', 'exec', '-i', 'codex-ruoyi-mysql', 'mysql',
        '--default-character-set=utf8mb4', '--batch', '--skip-column-names', a.database],
        input=text, text=True)

# Actual deploy and task approval, never SQL status updates or fabricated process IDs.
xml = '''<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
 xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://synthetic.local/hrm">
 <process id="oa_overtime_apply" name="隔离加班独立审批" isExecutable="true">
  <startEvent id="start"/><sequenceFlow id="f1" sourceRef="start" targetRef="review"/>
  <userTask id="review" name="隔离独立复核" flowable:candidateStrategy="30" flowable:candidateParam="1"/>
  <sequenceFlow id="f2" sourceRef="review" targetRef="end"/><endEvent id="end"/>
 </process>
</definitions>'''
model = remember('workOvertimeModel', lambda: c.ok('/bpm/model/create', {
    'key': 'oa_overtime_apply', 'name': '隔离加班独立审批', 'category': '1', 'type': 10,
    'formType': 20, 'formCustomCreatePath': '/oa/overtime-apply',
    'formCustomViewPath': '/oa/overtime-apply/detail', 'visible': True,
    'managerUserIds': [1], 'bpmnXml': xml, 'allowCancelRunningProcess': True}))
if not s.get('workOvertimeModelDeployed'):
    c.query('/bpm/model/deploy', {'id': model})
    s['workOvertimeModelDeployed'] = True
    c.save()
entity = remember('workEntityId', lambda: c.ok('/hrm/payroll/entity/create', {
    'code': 'ACCEPT_WORK_SYSTEMS', 'name': '隔离三类工时主体（虚构）',
    'taxRegistrationNumber': 'TESTWORKSYSTEMS123', 'evidence': reason}))
if not s.get('workEntityApproved'):
    c.query('/hrm/payroll/entity/approve', {'id': entity, 'reason': reason}, review)
    s['workEntityApproved'] = True
    c.save()
payment = datetime.date.fromisoformat(s['fixturePaymentDate'])
last = payment.replace(day=1) - datetime.timedelta(days=1)
previous = last.replace(day=1) - datetime.timedelta(days=1)
cycle_start = previous.replace(day=1)
month_start = last.replace(day=1)
def monday(first):
    return first + datetime.timedelta(days=(7 - first.weekday()) % 7)
mon1, mon2 = monday(cycle_start), monday(month_start)
end = f'{payment.year}-12-31'
systems = ['STANDARD', 'COMPREHENSIVE', 'IRREGULAR']
employees = {}
tokens = {}
rules = {}
for system in systems:
    password_key = 'workPassword' + system
    remember(password_key, lambda: secrets.token_urlsafe(12))
    username = 'hrmacceptwork' + system.lower()
    user = remember('workUser' + system, lambda: c.ok('/system/user/create', {
        'username': username, 'nickname': '隔离工时' + system, 'password': s[password_key],
        'postIds': [], 'remark': reason}))
    c.ok('/system/permission/assign-user-role', {'userId': user, 'roleIds': [1]})
    tokens[system] = c.login(username, password_key)
    def create_employee():
        return int(sql("INSERT INTO hrm_employee(name,job_number,user_id,dept_id,entry_status,status,type,entry_time,regular_time,post_name,work_city,remark,tenant_id) VALUES "
            f"('隔离{system}员工','ACCEPT_WORK_{system}',{user},103,1,1,1,'{cycle_start}','{cycle_start}','验收岗位','验收城市','虚构工时员工',1);"
            f"SELECT id FROM hrm_employee WHERE job_number='ACCEPT_WORK_{system}';").strip())
    employee = remember('workEmployee' + system, create_employee)
    employees[system] = employee
    member = remember('workMember' + system, lambda: c.ok('/hrm/payroll/entity/assign', {
        'entityId': entity, 'employeeId': employee, 'deptId': 103, 'employeeCategory': 'REGULAR',
        'workCity': '验收城市', 'effectiveFrom': cycle_start.isoformat(), 'effectiveTo': end, 'evidence': reason}))
    if not s.get('workMemberApproved' + system):
        c.query('/hrm/payroll/entity/membership/approve', {'id': member, 'reason': reason}, review)
        s['workMemberApproved' + system] = True
        c.save()
    rule = remember('workRule' + system, lambda: c.ok('/hrm/payroll/work/rule', {
        'entityId': entity, 'employeeId': employee, 'systemType': system,
        'effectiveFrom': cycle_start.isoformat(), 'effectiveTo': end, 'legalBasis': reason,
        'cycleMinutes': 960, 'cycleMonths': 2 if system == 'COMPREHENSIVE' else 1,
        'timeOffExpiryDays': 180, 'limitMinutes': 180, 'hourlyBase': 50,
        'workdayRate': 1.5, 'restdayRate': 2, 'holidayRate': 3}))
    if not s.get('workRuleApproved' + system):
        rejected = c.request('/hrm/payroll/work/rule/approve?id=' + str(rule), {})
        assert rejected['code'] not in (0, 500), rejected
        c.query('/hrm/payroll/work/rule/approve', {'id': rule}, review)
        s['workRuleApproved' + system] = True
        c.save()
    rules[system] = rule
group = remember('workAttendanceGroup', lambda: c.ok('/hrm/attendance/group/create', {
    'name': '隔离工时排班', 'openWifiCard': True, 'openPointCard': False, 'rest': False,
    'deptIds': [], 'employeeIds': list(employees.values()), 'points': [], 'specialDates': [],
    'wifis': [{'ssid': 'SYNTHETIC', 'mac': '02:00:00:00:00:02'}],
    'shifts': [{'weeks': [1, 2, 3, 4, 5], 'startTime': '09:00', 'endTime': '18:00',
        'clockInStartTime': '08:00', 'clockInEndTime': '20:00', 'clockOutStartTime': '10:00',
        'clockOutEndTime': '23:00', 'restStartTime': '12:00', 'restEndTime': '13:00', 'excludeRestTime': True}],
    'deductRule': {'lateMethod': 2, 'lateDeductMoney': 0, 'earlyMethod': 2,
        'earlyDeductMoney': 0, 'absenteeismMethod': 1, 'absenteeismDeductMoney': 0,
        'misscardMethod': 1, 'misscardDeductMoney': 0}}))
holiday = mon2 + datetime.timedelta(days=3)
remember('workHolidayId', lambda: c.ok('/hrm/attendance/holiday/create', {
    'date': holiday.isoformat() + 'T00:00:00', 'type': 3}))

def overtime(key, system, date, start, end_time, compensation='PAY', clock_start=None, clock_end=None):
    employee = employees[system]
    token = tokens[system]
    begin, finish = date.isoformat() + 'T' + start + ':00', date.isoformat() + 'T' + end_time + ':00'
    application = remember(key + 'Apply', lambda: c.ok('/oa/overtime-apply/create', {
        'title': '隔离工时验收' + key, 'urgency': 1, 'type': 1,
        'startTime': begin, 'endTime': finish, 'reason': reason}, token))
    if not s.get(key + 'Approved'):
        row = c.ok('/oa/overtime-apply/get?id=' + str(application), token=token)
        if not row.get('processInstanceId'):
            c.ok('/oa/overtime-apply/submit', {'id': application, 'startUserSelectAssignees': {}}, token)
            row = c.ok('/oa/overtime-apply/get?id=' + str(application), token=token)
        process = row['processInstanceId']
        tasks = []
        for _ in range(20):
            tasks = c.ok('/bpm/task/list-by-process-instance-id?processInstanceId=' + process)
            pending = [task for task in tasks if task.get('endTime') is None]
            if pending:
                break
            time.sleep(0.25)
        assert len(pending) == 1, tasks
        c.ok('/bpm/task/approve', {'id': pending[0]['id'], 'reason': reason, 'variables': {}}, method='PUT')
        assert c.ok('/oa/overtime-apply/get?id=' + str(application), token=token)['status'] == 2
        s[key + 'Approved'] = True
        c.save()
    clock_ids = []
    for kind, value in [(1, clock_start or start), (2, clock_end or end_time)]:
        clock_ids.append(remember(key + 'Clock' + str(kind), lambda: c.ok('/hrm/attendance/clock/create', {
            'employeeId': employee, 'clockTime': date.isoformat() + 'T' + value + ':00',
            'attendanceTime': date.isoformat() + ('T09:00:00' if kind == 1 else 'T18:00:00'),
            'type': kind, 'remark': reason})))
    body = {'entityId': entity, 'employeeId': employee, 'sourceSystem': 'OA',
        'sourceRecordId': str(application), 'clockInId': clock_ids[0], 'clockOutId': clock_ids[1],
        'evidence': reason, 'startTime': begin, 'endTime': finish, 'breakMinutes': 0,
        'dayType': 'WORKDAY', 'compensation': compensation}
    ids = remember(key + 'Entries', lambda: c.ok('/hrm/payroll/work/confirm', body))
    duplicate = c.request('/hrm/payroll/work/confirm', body)
    assert duplicate['code'] not in (0, 500), duplicate
    return ids

overtime('standardWorkday', 'STANDARD', mon2, '18:00', '20:00', clock_start='17:50', clock_end='20:10')
overtime('standardTimeOff', 'STANDARD', mon2 + datetime.timedelta(days=5), '09:00', '11:00', 'TIME_OFF')
overtime('comprehensiveFirst', 'COMPREHENSIVE', mon1, '18:00', '19:00', clock_start='09:00')
overtime('comprehensiveSecond', 'COMPREHENSIVE', mon2, '18:00', '19:00', clock_start='09:00')
overtime('irregularWorkday', 'IRREGULAR', mon2, '18:00', '20:00')
overtime('irregularHoliday', 'IRREGULAR', holiday, '09:00', '11:00')

def summary(system):
    return c.ok('/hrm/payroll/work/summary?' + urllib.parse.urlencode({
        'entityId': entity, 'employeeId': employees[system], 'from': cycle_start.isoformat(), 'to': last.isoformat()}))

standard = summary('STANDARD')
assert standard['recognizedMinutes'] == 240 and Decimal(str(standard['amount'])) == 150
assert standard['limitExceeded']
if not s.get('workTimeOffPassed'):
    assert standard['timeOffBalance'] == 120
    params = {'entityId': entity, 'employeeId': employees['STANDARD'], 'minutes': 60, 'key': 'ACCEPT_RESERVE', 'reason': reason}
    debit = c.query('/hrm/payroll/work/time-off/use', params)
    assert c.query('/hrm/payroll/work/time-off/use', params) == debit
    assert summary('STANDARD')['timeOffBalance'] == 60
    refund_params = {'entityId': entity, 'debitId': debit, 'key': 'ACCEPT_REFUND', 'reason': reason}
    refund = c.query('/hrm/payroll/work/time-off/refund', refund_params)
    assert c.query('/hrm/payroll/work/time-off/refund', refund_params) == refund
    assert summary('STANDARD')['timeOffBalance'] == 120
    debit2 = c.query('/hrm/payroll/work/time-off/use', dict(params, key='ACCEPT_CONSUME'))
    c.query('/hrm/payroll/work/time-off/consume', {'entityId': entity, 'debitId': debit2})
    c.query('/hrm/payroll/work/time-off/consume', {'entityId': entity, 'debitId': debit2})
    assert summary('STANDARD')['timeOffBalance'] == 60
    s['workTimeOffPassed'] = True
    c.save()
cycle_params = {'ruleId': rules['COMPREHENSIVE'], 'from': cycle_start.isoformat()}
cycle = c.query('/hrm/payroll/work/cycle/settle', cycle_params)
assert c.query('/hrm/payroll/work/cycle/settle', cycle_params) == cycle
comprehensive = summary('COMPREHENSIVE')
assert comprehensive['recognizedMinutes'] == 120 and Decimal(str(comprehensive['amount'])) == 0
settled = next(row for row in comprehensive['cycles'] if row['id'] == cycle)
assert settled['actualMinutes'] == 1080 and settled['recognizedMinutes'] == 120
assert settled['payableMinutes'] == 120 and Decimal(str(settled['amount'])) == 150
assert settled['sourceFresh']
irregular = summary('IRREGULAR')
assert irregular['recognizedMinutes'] == 240 and Decimal(str(irregular['amount'])) == 300
result = {'entityId': entity, 'employees': employees, 'rules': rules, 'groupId': group,
    'cycleFrom': cycle_start.isoformat(), 'cycleTo': last.isoformat(), 'standard': summary('STANDARD'),
    'comprehensive': comprehensive, 'irregular': irregular, 'realOaProcessApproved': True,
    'duplicateRecognitionRejected': True, 'ruleSelfApprovalRejected': True,
    'timeOffReserveRefundConsumeIdempotent': True, 'cycleSettlementIdempotent': True,
    'syntheticPolicy': reason}
(a.output_dir / 'oa-work-runtime.json').write_text(json.dumps(result, ensure_ascii=False, indent=2))
print('Passed: actual OA/BPM approvals, standard/irregular overtime, cross-month comprehensive settlement and time-off ledger')
