#!/usr/bin/env python3
"""Run scheduled punches and independently reviewed attendance corrections on synthetic staff."""
import argparse
import datetime
from decimal import Decimal
import io
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.parse
import urllib.request
from openpyxl import load_workbook
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
    p.error('Credentials must stay outside the repository')
if not state.exists():
    base = json.loads(Path(a.base_state).read_text())
    cloned = {k: v for k, v in base.items() if not k.startswith('e2e')}
    fd = os.open(state, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    with os.fdopen(fd, 'w') as out:
        json.dump(cloned, out)
c = RuntimeClient(state)
s = c.state
review = c.login('hrmacceptreview', 'reviewerPassword')
owner = c.login('hrmacceptoperator', 'browserPassword')
reason = '虚构隔离考勤验收：排班打卡与补正工单，不作正式地区扣款规则签字'
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

def prepare_employee():
    # Only identity and HR dates are SQL fixtures; all attendance writes use normal APIs.
    return int(sql("INSERT INTO hrm_employee(name,job_number,user_id,dept_id,entry_status,status,type,entry_time,regular_time,post_name,work_city,remark,tenant_id) VALUES "
        f"('隔离考勤工单员工','ACCEPT_ATTENDANCE',NULL,103,1,1,1,'2025-01-01','2025-01-01','验收岗位','验收城市','虚构考勤员工',1);"
        "SELECT id FROM hrm_employee WHERE job_number='ACCEPT_ATTENDANCE';").strip())

employee = remember('attendanceEmployeeId', prepare_employee)
previous = datetime.date.fromisoformat(s['fixturePaymentDate']).replace(day=1) - datetime.timedelta(days=1)
first = previous.replace(day=1)
day = first + datetime.timedelta(days=(7 - first.weekday()) % 7)
dates = [(day + datetime.timedelta(days=n)).isoformat() for n in range(3)]
group = remember('attendanceGroupId', lambda: c.ok('/hrm/attendance/group/create', {
    'name': '隔离排班与缺卡工单验收', 'openWifiCard': True, 'openPointCard': False,
    'rest': False, 'deptIds': [], 'employeeIds': [employee], 'points': [],
    'wifis': [{'ssid': 'SYNTHETIC', 'mac': '02:00:00:00:00:01'}],
    'specialDates': [], 'shifts': [{'weeks': [1, 2, 3, 4, 5],
        'startTime': '09:00', 'endTime': '18:00', 'clockInStartTime': '08:00',
        'clockInEndTime': '12:00', 'clockOutStartTime': '15:00', 'clockOutEndTime': '22:00',
        'restStartTime': '12:00', 'restEndTime': '13:00', 'excludeRestTime': True}],
    'deductRule': {'lateMethod': 2, 'lateDeductMoney': 2, 'earlyMethod': 2,
        'earlyDeductMoney': 3, 'absenteeismMethod': 1, 'absenteeismDeductMoney': 100,
        'misscardMethod': 1, 'misscardDeductMoney': 20}}))

def clock(key, date, time, kind):
    return remember(key, lambda: c.ok('/hrm/attendance/clock/create', {
        'employeeId': employee, 'clockTime': date + 'T' + time + ':00', 'type': kind,
        'attendanceTime': date + ('T09:00:00' if kind == 1 else 'T18:00:00'), 'remark': reason}))

clock('attendanceNormalIn', dates[0], '09:00', 1)
clock('attendanceNormalOut', dates[0], '18:00', 2)
clock('attendanceLateIn', dates[1], '09:10', 1)
clock('attendanceEarlyOut', dates[1], '17:45', 2)
original_out = clock('attendanceMissingOut', dates[2], '18:00', 2)

def daily(date):
    return c.ok('/hrm/attendance/statistics/daily-detail?' + urllib.parse.urlencode({
        'employeeId': employee, 'attendanceTime': date + ' 00:00:00'}))

def period():
    return c.ok('/hrm/attendance/statistics/period-record-list?' + urllib.parse.urlencode({
        'employeeIds': employee, 'from': dates[0], 'to': dates[2]}))[0]

def check_period(row, missing):
    for key, expected in {'attendDays': 3, 'lateMinute': 10, 'lateCount': 1,
        'earlyMinute': 15, 'earlyCount': 1, 'misscardCount': missing}.items():
        assert row[key] == expected, (key, row[key], expected)
    for key, expected in {'lateDeductAmount': 20, 'earlyDeductAmount': 45,
        'misscardDeductAmount': 20 * missing, 'attendanceDeductAmount': 65 + 20 * missing}.items():
        assert Decimal(str(row[key])) == expected, (key, row[key], expected)

before = remember('attendanceBeforeSummary', period)
check_period(before, 1)
assert before['actualDays'] == 3
request = {'employeeId': employee, 'attendanceDate': dates[2], 'type': 'MISSCARD',
    'clockType': 1, 'ownerId': s['browserUserId'], 'reviewerId': s['reviewerId'],
    'requestKey': 'ACCEPT_ATTENDANCE_CORRECTION', 'dueTime': dates[2] + 'T19:00:00'}
issue = remember('attendanceIssueId', lambda: c.ok('/hrm/payroll/attendance/issue', request))
assert c.ok('/hrm/payroll/attendance/issue', request) == issue

def issue_row():
    return next(row for row in c.ok(f'/hrm/payroll/attendance/issues?employeeId={employee}') if row['id'] == issue)

def review_issue(approved):
    return c.query('/hrm/payroll/attendance/review', {'id': issue, 'approve': str(approved).lower(), 'reason': reason}, review)

def submit():
    return c.ok('/hrm/payroll/attendance/submit', {'id': issue, 'evidence': reason,
        'correctionTime': dates[2] + 'T09:00:00'}, owner)

if not s.get('attendanceCorrectionPassed'):
    submit()
    self_review = c.request('/hrm/payroll/attendance/review?' + urllib.parse.urlencode({
        'id': issue, 'approve': 'true', 'reason': reason}), {}, owner)
    assert self_review['code'] not in (0, 500), self_review
    review_issue(False)
    assert issue_row()['status'] == 'OPEN'
    assert daily(dates[2])['misscardCount'] == 1
    submit()
    review_issue(True)
    first_correction = issue_row()['correctionClockId']
    c.query('/hrm/payroll/attendance/close', {'id': issue, 'reason': reason}, review)
    assert issue_row()['status'] == 'CLOSED'
    check_period(period(), 0)
    c.query('/hrm/payroll/attendance/reopen', {'id': issue, 'reason': reason}, review)
    assert issue_row()['status'] == 'OPEN'
    assert daily(dates[2])['misscardCount'] == 1
    assert c.ok(f'/hrm/attendance/clock/get?id={first_correction}')['status'] == 3
    submit()
    review_issue(True)
    c.query('/hrm/payroll/attendance/close', {'id': issue, 'reason': reason}, review)
    s['attendanceCorrectionPassed'] = True
    c.save()
after = period()
check_period(after, 0)
month_rows = c.ok('/hrm/attendance/statistics/month-record-page?' + urllib.parse.urlencode({
    'employeeId': employee, 'year': previous.year, 'month': previous.month,
    'pageNo': 1, 'pageSize': 10}))
assert month_rows['total'] == 1
monthly = month_rows['list'][0]
expected_days = sum((first + datetime.timedelta(days=n)).weekday() < 5 for n in range(previous.day))
assert monthly['attendDays'] == expected_days
assert monthly['actualDays'] == 3
assert monthly['absenteeismDays'] == expected_days - 3
assert Decimal(str(monthly['attendanceDeductAmount'])) == (expected_days - 3) * 100 + 65
assert issue_row()['status'] == 'CLOSED'
events = c.ok(f'/hrm/payroll/attendance/history?id={issue}')
assert [row['action'] for row in events] == [
    'CREATE', 'SUBMIT', 'REJECT', 'SUBMIT', 'APPROVE', 'CLOSE', 'REOPEN', 'SUBMIT', 'APPROVE', 'CLOSE']
expected_time = datetime.datetime.fromisoformat(dates[2] + 'T18:00:00').replace(
    tzinfo=datetime.timezone(datetime.timedelta(hours=8)))
assert c.ok(f'/hrm/attendance/clock/get?id={original_out}')['clockTime'] == int(expected_time.timestamp() * 1000)
query = urllib.parse.urlencode({'employeeIds': employee, 'from': dates[0], 'to': dates[2]})
request = urllib.request.Request(c.base + '/hrm/attendance/statistics/period-record-export?' + query,
    headers={'tenant-id': '1', 'Authorization': 'Bearer ' + c.token})
with c.opener.open(request, timeout=60) as response:
    data = response.read()
rows = list(load_workbook(io.BytesIO(data), read_only=True, data_only=True).active.values)
assert rows[1][0] == employee and rows[1][9] == 10 and rows[1][11] == 15, rows
(a.output_dir / 'attendance-runtime.json').write_text(json.dumps({
    'employeeId': employee, 'groupId': group, 'dates': dates, 'before': before, 'after': after,
    'monthly': monthly, 'monthlyExpectedScheduledDays': expected_days,
    'issueId': issue, 'eventActions': [row['action'] for row in events], 'selfReviewRejected': True,
    'duplicateIssueIdempotent': True, 'reopenInvalidatesCorrection': True,
    'originalPunchUnchanged': True, 'actualExcelRead': True}, ensure_ascii=False, indent=2))
print('Passed: scheduled daily/period attendance, exact nonzero deductions, correction reject/approve/close/reopen and real Excel')
