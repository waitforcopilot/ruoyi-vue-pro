#!/usr/bin/env python3
"""Read-only final S4 candidate checks on previously exercised local TEST fixtures.
Browser regression remains a separate gate. Credentials are never emitted.
"""
import argparse
import json
import urllib.parse
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo
from runtime_client import RuntimeClient

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--state-file', required=True)
parser.add_argument('--output', required=True)
args = parser.parse_args()
c = RuntimeClient(args.state_file)
s = c.state
editor = c.login(s['editorUsername'], 'editorPassword')
reviewer = c.login(s['reviewerUsername'], 'reviewerPassword')
restricted = c.login(s['restrictedUsername'], 'restrictedPassword')

def get(path, token=editor, **params):
    return c.ok(path + '?' + urllib.parse.urlencode(params), token=token)

def stats(project):
    return get('/hrm/payroll/review/statistics', projectId=project)

out = {'verifiedAt': datetime.now(ZoneInfo('Asia/Shanghai')).isoformat()}
out['fixedS0AfterRestoringCandidate'] = stats(s['projects']['s0'])['counts']
assert out['fixedS0AfterRestoringCandidate'] == dict(U=47, E=47, I=40, C=40, T=6, D=0)
out['independentS3'] = stats(s['projects']['s3'])
assert out['independentS3']['confirmation']['label'] == '39/46'
assert out['independentS3']['includedConfirmation']['label'] == '39/40'
out['empty'] = stats(s['projects']['empty'])
assert out['empty']['confirmation']['percent'] is None
out['sources'] = stats(s['projects']['sources'])
assert out['sources']['readiness']['label'] == '2/4'
assert out['sources']['sourceCount'] == 2 and out['sources']['exempt'] == 1
assert out['sources']['unresolvedIssues'] == 2 and out['sources']['overdueIssues'] == 1
assert out['sources']['formalReleaseGate'] == 'S5_NOT_IMPLEMENTED'
subset = get('/hrm/payroll/review/statistics', restricted, projectId=s['projects']['sources'])
export = get('/hrm/payroll/review/export', restricted, projectId=s['projects']['sources'])
assert subset['counts']['E'] == 1 and subset['sourceCount'] == 1
assert export['sourceCount'] == 0 and export['L'][0]['relationId'] is None
assert 'projectTotals' not in subset and 'projectTotals' not in export
out['restrictedSourceExportIntersection'] = True
denied = c.request('/hrm/payroll/review/reminders?projectId=' + str(s['projectId']), token=restricted)
assert denied['code'] == 1050810043
out['revokedReadDeniesReminders'] = denied['code']
inbox = get('/hrm/payroll/review/reminders', reviewer, projectId=s['projectId'])
assert inbox and all(x['channel'] == 'IN_APP_ONLY' for x in inbox)
assert not any('仅265' in x['reference'] for x in inbox)
out['recipientIsolation'] = True
rows = get('/hrm/payroll/requirement/list', projectId=s['projectId'])
contexts = []
for term, kind, anchor in [('最终场景锚点', 'FIELD', 'field-ot-02'),
                            ('操作上下文', 'OPERATION', 'operation-simulate'),
                            ('流程上下文', 'FLOW', 'flow-approval')]:
    row = next(x for x in rows if term in x['description'])
    context = json.loads(row['businessContextJson'])
    assert context['targetType'] == kind and context['anchor'] == anchor
    assert context['contextModule'] == 'OT' and context['page'] == '/hrm-payroll/review'
    assert get('/hrm/payroll/requirement/history', projectId=s['projectId'], id=row['id'])
    contexts.append({'id': row['id'], 'version': row['version'], 'kind': kind, 'anchor': anchor})
out['browserCreatedContextsPersisted'] = contexts
Path(args.output).write_text(json.dumps(out, ensure_ascii=False, indent=2) + '\n')
print('Final read-only statistics, authorization, recipient isolation and browser context checks passed')
