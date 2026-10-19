#!/usr/bin/env python3
"""Local TEST-only member/action regression; never substitutes for browser acceptance.

Use a private RuntimeClient state containing apiProjectId and the three dedicated
S2 credentials and a fresh TEST project with active creator draft access.
Bootstrap registration is external; this script cannot create or resurrect roots.
"""
import argparse
import concurrent.futures
import json
from pathlib import Path
from runtime_client import RuntimeClient

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--state-file', required=True)
p.add_argument('--output', type=Path, required=True)
a = p.parse_args()
c = RuntimeClient(a.state_file)
s = c.state
project = s['apiProjectId']
manager = c.login(s['managerUsername'], 'managerPassword')
editor = c.login(s['editorUsername'], 'editorPassword')
reviewer = c.login(s['reviewerUsername'], 'reviewerPassword')
base = '/hrm/payroll/requirement'
projects = base + '-project'
meta = next(x for x in c.ok(projects + '/list', token=manager) if x['id'] == project)
assert meta['projectKey'].startswith('TEST_S2_API_')
assert any(x['id'] == project for x in c.ok(projects + '/list')), 'Use a fresh TEST fixture with active creator access'
scope = {'entities': [101], 'organizations': [201], 'workerTypes': ['TEST'],
         'scenarios': ['TEST'], 'from': '2026-10-01', 'to': '2026-10-31',
         'explanation': 'TEST S2 isolated API case'}
if meta.get('projectScopeVersion') is None:
    c.ok(projects + '/scope', {'projectId': project, 'scopeJson': json.dumps(scope),
                             'reason': 'TEST initial scope'}, token=manager)
version = next(x for x in c.ok(projects + '/list', token=manager)
               if x['id'] == project)['projectScopeVersion']

def rows(token=manager):
    return c.ok(base + '/list?projectId=' + str(project), token=token)

def denied(path, data=None, token=editor):
    result = c.request(path, data, token=token)
    assert isinstance(result, dict) and result.get('code') in {401, 403, 1050810002, 1050810041, 1050810042, 1050810043, 1050810044, 1050810045}, path
    return result['code']

def grant(user, actions, modules=None, ids=None, delegable=''):
    return c.ok(projects + '/member-grant', {
        'projectId': project, 'userId': user, 'actions': actions,
        'delegableActions': delegable, 'fullProject': False,
        'modulesJson': json.dumps(modules or []),
        'requirementIdsJson': json.dumps(ids or []), 'ceilingJson': json.dumps(scope),
        'validFrom': '2026-10-01', 'validTo': '2026-12-31',
        'evidence': 'TEST synthetic delegation; no formal business approval'}, token=manager)

def revoke(gid, expected=1):
    return c.ok(projects + '/member-revoke', {'projectId': project, 'grantId': gid,
                'version': expected, 'reason': 'TEST revoke'}, token=manager)

def payload(row):
    return {k: row.get(k) for k in ['id', 'projectId', 'projectScopeVersion', 'code',
        'module', 'description', 'status', 'readiness', 'priority', 'version',
        'scopeMode', 'scopeJson', 'sourceSystem', 'sourceOwnerId', 'fieldMapping',
        'acceptance', 'evidence', 'reviewerId']}

# Fresh cases are appended, never modifying preceding runs or original records.
new = []
for module in ['COL', 'TAX']:
    new.append(c.ok(base + '/save', {'projectId': project,
        'projectScopeVersion': version, 'code': 'CUSTOM-0000', 'module': module,
        'description': 'TEST S2 API ' + module, 'status': 'PENDING',
        'readiness': 'MISSING', 'priority': 2, 'scopeMode': 'SUBSET',
        'scopeJson': json.dumps(scope), 'sourceSystem': 'TEST synthetic',
        'sourceOwnerId': s['editorId'], 'fieldMapping': 'TEST input -> TEST output',
        'acceptance': 'TEST reproducible case', 'evidence': 'TEST evidence'}))
rid, hidden = new
edit = grant(s['editorId'], 'READ,EDIT,EXPORT', ['COL'], [rid])
read = grant(s['editorId'], 'READ', ['TAX'], [hidden])
review = grant(s['reviewerId'], 'READ,CONFIRM', ['COL'], [rid])
assert {x['id'] for x in rows(editor)} == {rid, hidden}
r = next(x for x in rows(editor) if x['id'] == rid)
h = next(x for x in rows(editor) if x['id'] == hidden)
denied(base + '/save', {**payload(h), 'description': 'TEST should reject'})
denied(base + '/save', {**payload(r), 'module': 'TAX'})
denied(projects + '/metadata', {'id': project, 'projectKey': meta['projectKey'],
                               'name': 'TEST reject', 'metadataVersion': 1})
denied(projects + '/member-grant', {'projectId': project, 'userId': s['reviewerId'],
                                  'actions': 'READ', 'delegableActions': '', 'fullProject': False,
                                  'modulesJson': '["COL"]', 'requirementIdsJson': json.dumps([rid]),
                                  'ceilingJson': json.dumps(scope), 'validFrom': '2026-10-01',
                                  'validTo': '2026-10-31', 'evidence': 'TEST unauthorized member request'})
# Missing projectId cannot route a current object through legacy authorization.
denied(base + '/save', {**payload(r), 'projectId': None})
denied(base + '/list', token=editor)
denied(base + '/export', token=editor)
c.ok(base + '/save', {**payload(r), 'description': 'TEST S2 edited by minimal editor'}, token=editor)
r = next(x for x in rows() if x['id'] == rid)
# Assignee must have current object READ and CONFIRM, even if platform role is valid.
assign = {'id': rid, 'projectId': project, 'projectScopeVersion': version, 'version': r['version']}
denied(base + '/assign', {**assign, 'reviewerId': s['editorId']}, token=manager)
denied(base + '/assign', {**assign, 'reviewerId': s['managerId']}, token=manager)
c.ok(base + '/assign', {**assign, 'reviewerId': s['reviewerId']}, token=manager)
r = next(x for x in rows() if x['id'] == rid)
c.ok(base + '/confirm', {'id': rid, 'projectId': project,
     'projectScopeVersion': version, 'version': r['version']}, token=reviewer)
signed = next(x for x in rows() if x['id'] == rid)
assert signed['status'] == 'CONFIRMED' and signed['confirmedBy'] == s['reviewerId']
history_before = c.ok(base + '/history?id=' + str(rid), token=reviewer)
# Concurrent saves of the same row version serialize: exactly one winner.
r = next(x for x in rows(editor) if x['id'] == rid)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    results = list(pool.map(lambda i: c.request(base + '/save',
        {**payload(r), 'description': 'TEST race ' + str(i)}, token=editor), [1, 2]))
assert sum(x['code'] == 0 for x in results) == 1
# Revoke races with a save; then the SAME token is denied on all later operations.
r = next(x for x in rows(editor) if x['id'] == rid)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    save_job = pool.submit(c.request, base + '/save', payload(r), editor)
    revoke_job = pool.submit(revoke, edit)
    race_save = save_job.result()
    assert revoke_job.result() is True
assert race_save['code'] in (0, 1050810043)
# The independent TAX read grant survives; EDIT cannot be manufactured from it.
assert {x['id'] for x in rows(editor)} == {hidden}
denied(base + '/history?id=' + str(rid), token=editor)
denied(base + '/save', payload(r), token=editor)
denied(base + '/export?projectId=' + str(project), token=editor)
revoke(read)
denied(base + '/list?projectId=' + str(project), token=editor)
revoke(review)
for path, data in [(base + '/list?projectId=' + str(project), None),
                   (base + '/history?id=' + str(rid), None),
                   (base + '/confirm', {'id': rid, 'version': r['version'],
                      'projectId': project, 'projectScopeVersion': version})]:
    denied(path, data, reviewer)
after = c.ok(base + '/history?id=' + str(rid), token=manager)
assert all(x in after for x in history_before)
# Reassignment remains possible for other currently authorized members.
replacement = grant(s['editorId'], 'READ,CONFIRM', ['COL'], [rid])
# Editor lacks platform CONFIRM; a project grant alone cannot qualify them.
assert s['editorId'] not in c.ok(projects + '/eligible-reviewers?projectId=' + str(project)
                                + '&requirementId=' + str(rid), token=manager)
revoke(replacement)
# Stale lifecycle versions reject without resurrecting anything.
denied(projects + '/member-revoke', {'projectId': project, 'grantId': edit,
       'version': 1, 'reason': 'TEST stale'}, manager)
result = {'projectId': project, 'requirements': new, 'actionScopeIntersection': True,
          'legacyUnscopedDenied': True, 'crossModuleWriteDenied': True,
          'validMinimalAssignmentAndConfirmation': True, 'concurrentSaveWinners': 1,
          'revocationRaceSaveCode': race_save['code'], 'sameTokenAfterRevokeDenied': True,
          'otherGrantReadPreserved': True, 'originalSignatureHistoryRetained': True,
          'platformAndProjectReviewerQualifications': True, 'browserAcceptance': 'SEPARATE_GATE'}
a.output.parent.mkdir(parents=True, exist_ok=True)
a.output.write_text(json.dumps(result, ensure_ascii=False, indent=2))
print(json.dumps(result, ensure_ascii=False))
