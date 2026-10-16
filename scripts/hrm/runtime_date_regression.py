#!/usr/bin/env python3
"""Verify ISO dates, real approvals and rejected date inputs through the running backend."""
import argparse
import datetime
import json
from pathlib import Path
from runtime_client import RuntimeClient

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--state-file', required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
c = RuntimeClient(args.state_file)
s = c.state
review = c.login('hrmacceptreview', 'reviewerPassword')
results = []
if 'dateContractBatchId' not in s:
    s['dateContractBatchId'] = c.ok('/hrm/salary/month-record/create', {'entityId': s['entityId'], 'year': 2026, 'month': 11, 'title': '隔离ISO日期契约验收', 'startTime': '2026-11-01T00:00:00', 'endTime': '2026-11-30T23:59:59'})
    c.save()
batch = c.ok('/hrm/salary/month-record/get?id=' + str(s['dateContractBatchId']))
zone = datetime.timezone(datetime.timedelta(hours=8))
expected = int(datetime.datetime(2026, 11, 1, tzinfo=zone).timestamp() * 1000)
assert batch['startTime'] == expected, batch['startTime']
results.append({'case': 'iso-input-persists-correct-period', 'batchId': batch['id'], 'expectedStartMillis': expected, 'actualStartMillis': batch['startTime']})
for value in ('invalid-date', True, 1.5, {}):
    rejected = c.request('/hrm/salary/month-record/create', {'entityId': s['entityId'], 'year': 2026, 'month': 12, 'startTime': value})
    assert rejected['code'] == 400, rejected
    results.append({'case': 'invalid-date-input-rejected', 'input': value, 'code': rejected['code']})
rejected = c.request('/hrm/salary/month-record/create', {'entityId': s['entityId'], 'year': 2026, 'month': 12, 'startTime': 0, 'endTime': 0})
assert rejected['code'] not in (0, 500), rejected
results.append({'case': 'epoch-outside-payroll-period-rejected', 'code': rejected['code']})
if 'dateContractFormulaId' not in s:
    variables = c.ok('/hrm/payroll/formula/variables')
    target = next((row['code'] for row in variables if row['name'] == '新库正常API工资项'), None)
    if target is None:
        created = c.ok('/hrm/salary/option/create', {'parentCode': 10, 'name': '新库正常API工资项', 'remark': '虚构日期验收'})
        target = next(row['code'] for row in c.ok('/hrm/payroll/formula/variables') if row['id'] == created)
    s['dateContractFormulaId'] = c.ok('/hrm/payroll/formula/create', {'optionCode': target, 'expression': 'item_10101 * 2', 'effectiveFrom': '2027-01-01', 'effectiveTo': '2027-12-31', 'evidence': '虚构日期契约回归；2027有效，不改变2026普通工资'})
    c.save()
fid = s['dateContractFormulaId']
formula = next(row for row in c.ok('/hrm/payroll/formula/list') if row['id'] == fid)
if formula['status'] != 'APPROVED':
    self_review = c.request('/hrm/payroll/formula/approve?id=' + str(fid), {})
    assert self_review['code'] == 1050810005
    c.query('/hrm/payroll/formula/approve', {'id': fid}, review)
    formula = next(row for row in c.ok('/hrm/payroll/formula/list') if row['id'] == fid)
assert formula['effectiveFrom'] == '2027-01-01'
assert formula['effectiveTo'] == '2027-12-31'
assert formula['status'] == 'APPROVED'
results.append({'case': 'approved-formula-iso-dates', 'id': fid, 'effectiveFrom': formula['effectiveFrom'], 'effectiveTo': formula['effectiveTo'], 'status': formula['status']})
args.output.write_text(json.dumps({'capturedAt': datetime.datetime.now().isoformat(), 'cases': results}, ensure_ascii=False, indent=2))
print('Passed: ISO dates, correct date-time persistence, invalid input and approval write paths')
