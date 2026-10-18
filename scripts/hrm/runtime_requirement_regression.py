#!/usr/bin/env python3
"""Verify requirement confirmation, history and legacy repair through real local APIs."""
import argparse
import datetime
import json
from pathlib import Path
import secrets
from runtime_client import RuntimeClient

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--state-file', required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
c = RuntimeClient(args.state_file)
s = c.state
results = []
if 'legacyRequirementId' in s:
    legacy = next(row for row in c.ok('/hrm/payroll/requirement/list') if row['id'] == s['legacyRequirementId'])
    assert legacy['status'] == 'REVIEWING' and legacy['version'] == 2, legacy
    assert legacy.get('confirmedBy') is None and legacy.get('confirmedAt') is None
    history = c.ok('/hrm/payroll/requirement/history?id=' + str(legacy['id']))
    assert len(history) == 2
    assert json.loads(history[-1]['snapshot'])['status'] == 'CONFIRMED'
    assert history[0]['actorId'] == 0 and json.loads(history[0]['snapshot'])['validationReason']
    results.append({'case': 'legacy-confirmation-reopened-idempotently', 'id': legacy['id'], 'status': legacy['status'], 'version': legacy['version'], 'historyVersions': [row['version'] for row in history]})
admin = c.ok('/system/auth/get-permission-info')['user']['id']
if 'disabledRequirementUserId' not in s:
    s['disabledRequirementUserId'] = c.ok('/system/user/create', {'username': 'hrmacceptdisabled', 'nickname': '隔离停用负责人', 'password': secrets.token_urlsafe(12), 'remark': '仅供无效负责人负向验收', 'postIds': []})
    c.save()
c.ok('/system/user/update-status', {'id': s['disabledRequirementUserId'], 'status': 1}, method='PUT')
request = {'code': 'COL-9201', 'module': 'COL', 'description': '隔离需求确认与版本验收', 'priority': 2, 'status': 'PENDING', 'readiness': 'MISSING'}
if 'requirementRegressionId' in s:
    prior = next(row for row in c.ok('/hrm/payroll/requirement/list') if row['id'] == s['requirementRegressionId'])
    request.update(id=prior['id'], version=prior['version'])
id_ = c.ok('/hrm/payroll/requirement/save', request)
s['requirementRegressionId'] = id_
c.save()
record = next(row for row in c.ok('/hrm/payroll/requirement/list') if row['id'] == id_)
version = record['version']
history_before = len(c.ok('/hrm/payroll/requirement/history?id=' + str(id_)))
confirmation = {**request, 'id': id_, 'version': version, 'status': 'CONFIRMED', 'reviewerId': admin, 'sourceOwnerId': admin, 'sourceSystem': '虚构验收数据源', 'acceptance': '字段、责任人、版本及历史可验证', 'evidence': '隔离自动验收记录', 'fieldMapping': 'employeeNumber → 员工编号；baseSalary → 基本工资'}
assert c.request('/hrm/payroll/requirement/save', confirmation)['code'] == 1050810040
for name, changes, expected in [
    ('empty-mapping', {'fieldMapping': ''}, 1050810036),
    ('bare-exemption', {'fieldMapping': 'N/A'}, 1050810036),
    ('unknown-owner', {'sourceOwnerId': 99999991}, 1050810037),
    ('unknown-reviewer', {'reviewerId': 99999992}, 1050810037),
    ('disabled-owner', {'sourceOwnerId': s['disabledRequirementUserId']}, 1050810037),
    ('confirmation-by-other-actor', {'reviewerId': s['reviewerId']}, 1050810036),
]:
    # Persist candidate content first. Failed confirmation itself must not add history.
    candidate = {**confirmation, **changes, 'version': version, 'status': 'REVIEWING'}
    c.ok('/hrm/payroll/requirement/save', candidate)
    version += 1
    before_confirm = len(c.ok('/hrm/payroll/requirement/history?id=' + str(id_)))
    result = c.request('/hrm/payroll/requirement/confirm', {'id': id_, 'version': version})
    assert result['code'] == expected, (name, result)
    assert len(c.ok('/hrm/payroll/requirement/history?id=' + str(id_))) == before_confirm
    results.append({'case': name, 'code': result['code']})
history_before = len(c.ok('/hrm/payroll/requirement/history?id=' + str(id_)))
c.ok('/hrm/payroll/requirement/save', {**confirmation, 'version': version, 'status': 'REVIEWING'})
version += 1
c.ok('/hrm/payroll/requirement/confirm', {'id': id_, 'version': version})
record = next(row for row in c.ok('/hrm/payroll/requirement/list') if row['id'] == id_)
assert record['confirmedBy'] == admin and record['status'] == 'CONFIRMED' and record['readiness'] == 'MISSING'
assert record['version'] == version + 1
results.append({'case': 'explicit-confirmation-does-not-change-readiness', 'id': id_, 'version': record['version'], 'readiness': record['readiness']})
assert c.request('/hrm/payroll/requirement/confirm', {'id': id_, 'version': version})['code'] == 1050810002
version += 1
c.ok('/hrm/payroll/requirement/save', {**request, 'id': id_, 'version': version, 'status': 'REVIEWING'})
version += 1
reopened = next(row for row in c.ok('/hrm/payroll/requirement/list') if row['id'] == id_)
assert reopened.get('confirmedBy') is None and reopened.get('confirmedAt') is None
c.ok('/hrm/payroll/requirement/save', {**confirmation, 'version': version, 'status': 'REVIEWING', 'fieldMapping': '不适用：本需求为制度文字评审，无外部业务数据字段'})
version += 1
c.ok('/hrm/payroll/requirement/confirm', {'id': id_, 'version': version})
history = c.ok('/hrm/payroll/requirement/history?id=' + str(id_))
assert len(history) == history_before + 5
results.append({'case': 'explicit-exemption-reopening-stale-version-and-history', 'historyAdded': 5, 'latestVersion': history[0]['version']})
args.output.write_text(json.dumps({'capturedAt': datetime.datetime.now().isoformat(), 'cases': results}, ensure_ascii=False, indent=2))
print('Passed: confirmation validation, real users, legacy repair, history and optimistic versions')
