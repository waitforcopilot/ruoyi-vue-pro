#!/usr/bin/env python3
"""Creates synthetic fixtures and checks a live, isolated intake instance. Never use a production DB."""
import argparse, concurrent.futures, copy, json, os, time, urllib.error, urllib.parse, urllib.request, uuid
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url', default='http://127.0.0.1:48081')
parser.add_argument('--allow-test-fixtures', action='store_true', required=True)
parser.add_argument('--output-dir', type=Path, required=True)
args = parser.parse_args()
args.output_dir.mkdir(parents=True, exist_ok=True)
base = args.base_url.rstrip('/') + '/admin-api/hrm/payroll/intake'
roles = {'admin': (1, 'HRM_SMOKE_TOKEN'), 'reader': (1, 'HRM_READER_TOKEN'), 'editor': (1, 'HRM_EDITOR_TOKEN'),
         'reviewer': (1, 'HRM_REVIEWER_TOKEN'), 'none': (1, 'HRM_UNGRANTED_TOKEN'), 'tenantb': (999, 'HRM_TENANT_B_TOKEN')}
for _, env in roles.values():
    if not os.environ.get(env): parser.error('Missing verification credential: ' + env)
checks = []
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

def call(path, method='GET', data=None, params=None, role='admin', upload=None, raw=False):
    tenant, env = roles.get(role, (1, None))
    headers = {'tenant-id': str(tenant)}
    if env: headers['Authorization'] = 'Bearer ' + os.environ[env]
    payload = None
    if upload is not None:
        filename, content = upload
        boundary = 'hrmverify' + uuid.uuid4().hex
        body = []
        for key, value in data.items():
            body.append(('--' + boundary + '\r\nContent-Disposition: form-data; name="' + key + '"\r\n\r\n' + str(value) + '\r\n').encode())
        body.append(('--' + boundary + '\r\nContent-Disposition: form-data; name="file"; filename="' + filename + '"\r\nContent-Type: text/csv\r\n\r\n').encode())
        body.extend([content, ('\r\n--' + boundary + '--\r\n').encode()])
        payload = b''.join(body)
        headers['Content-Type'] = 'multipart/form-data; boundary=' + boundary
    elif data is not None:
        payload = json.dumps(data, ensure_ascii=False).encode()
        headers['Content-Type'] = 'application/json'
    url = base + path + ('?' + urllib.parse.urlencode(params) if params else '')
    request = urllib.request.Request(url, headers=headers, method=method, data=payload)
    try: response = opener.open(request, timeout=30)
    except urllib.error.HTTPError as e: response = e
    content = response.read()
    return (content, response.headers) if raw else json.loads(content)

def success(path, **kw):
    result = call(path, **kw)
    assert result.get('code') == 0, (path, result.get('code'))
    return result.get('data')

def passed(name, condition):
    if not condition: raise AssertionError(name)
    checks.append({'check': name, 'result': 'PASS'})
    print('PASS:', name, flush=True)

def get(id, role='admin'): return success('/contracts/get', params={'id': id}, role=role)
def review(id, action='confirm', role='reviewer'):
    row = get(id)
    return call('/contracts/review', method='POST', role=role, data={'id': id, 'revision': row['revision'], 'action': action,
        'evidence': '隔离测试评审：仅用于验证字段与接口，不代表真实业务确认', 'reviewedByName': 'FORGED'})
def preview(id, content, role='admin', scope='测试主体', filename='预检回归.csv', start='2026-10-01', end='2026-10-31'):
    return call('/batches/preview', method='POST', role=role, upload=(filename, content), data={
        'contractId': id, 'declaredScope': scope, 'periodStart': start, 'periodEnd': end})
def batch(id, role='admin'): return success('/batches/get', params={'id': id}, role=role)
def issue_codes(result):
    return {i['code'] for i in result['globalIssues']} | {i['code'] for row in result['rows'] for i in row['issues']}

passed('anonymous denied', call('/sources', role=None)['code'] == 401)
passed('ungranted account denied', call('/sources', role='none')['code'] == 403)
sources = success('/sources', role='editor')
passed('intake source options available with query permission', len(sources) >= 12 and all('requiredFields' not in s or not s['requiredFields'] for s in sources))
request = {'sourceId': sources[0]['id'], 'title': '回归样例：数据接入字段契约 ' + str(int(time.time())),
    'actualSystem': '隔离测试 CSV（演示）', 'ownerName': '回归数据负责人（演示）', 'applicableScope': '仅隔离测试主体，生产范围待确认',
    'schema': {'fields': [
        {'key': 'job', 'label': '员工工号', 'type': 'TEXT', 'required': True, 'maxLength': 64},
        {'key': 'date', 'label': '业务日期', 'type': 'DATE', 'required': True},
        {'key': 'scope', 'label': '核对主体', 'type': 'TEXT', 'required': True, 'maxLength': 120},
        {'key': 'amount', 'label': '演示金额', 'type': 'DECIMAL', 'required': True, 'scale': 2, 'unit': '元'},
        {'key': 'note', 'label': '样例说明', 'type': 'TEXT', 'required': False, 'maxLength': 120}],
        'keyFields': ['job', 'date'], 'periodField': 'date', 'subjectField': 'scope'}}
passed('reader cannot create contract', call('/contracts/create', method='POST', data=request, role='reader')['code'] == 403)
id = success('/contracts/create', method='POST', data=request, role='editor')
row = get(id)
passed('new contract is draft and server allocates version', row['status'] == 0 and row['revision'] == 1 and row['contractVersion'] >= 1)
passed('draft template blocked', call('/contracts/template', params={'id': id})['code'] == 1050920004)
passed('editor cannot confirm contract', review(id, role='editor')['code'] == 403)
stale = row
changed = {**row, 'evidence': '隔离测试草稿修订'}
success('/contracts/update', method='PUT', data=changed, role='editor')
passed('stale draft edit rejected', call('/contracts/update', method='PUT', data=stale, role='editor')['code'] == 1050920002)
row = get(id)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    updates = list(pool.map(lambda label: call('/contracts/update', method='PUT', data={**row, 'evidence': label}, role='editor'), ['并发草稿 A', '并发草稿 B']))
passed('concurrent same-revision edits have one winner', sorted(r['code'] for r in updates) == [0, 1050920002])
passed('reviewer cannot edit contract', call('/contracts/update', method='PUT', data=get(id), role='reviewer')['code'] == 403)
passed('authorized reviewer confirms', review(id)['code'] == 0)
row = get(id)
passed('review actor is server stamped', row['status'] == 1 and row.get('reviewedTime') and row.get('reviewedByName') != 'FORGED')
passed('confirmed contract immutable', call('/contracts/update', method='PUT', data=row, role='editor')['code'] == 1050920003)
template, headers = call('/contracts/template', params={'id': id}, raw=True)
passed('template has UTF8 BOM exact contract marker and download headers', template.startswith(b'\xef\xbb\xbf') and
       ('#hrm-payroll-contract,' + str(id) + ',' + str(row['contractVersion'])).encode() in template and
       headers.get_content_type() == 'text/csv' and '.csv' in headers.get('Content-Disposition', ''))
rows = ('QAVR-001,2026-10-02,测试主体,0,零值有效\r\nQAVR-002,2026-10-02,测试主体,,必填缺失\r\n'
        'QAVR-003,2026-10-02,测试主体,10.999,小数位超出\r\nQAVR-004,2026-09-30,测试主体,1,期间越界\r\n'
        'QAVR-005,2026-10-02,测试主体,2,复合键重复一\r\nQAVR-005,2026-10-02,测试主体,3,复合键重复二\r\n').encode()
file = template + rows
passed('reader cannot upload', preview(id, file, role='reader')['code'] == 403)
main = preview(id, file)['data']
detail = batch(main)
passed('batch dates use ISO strings for frontend clients', detail['periodStart'] == '2026-10-01' and detail['periodEnd'] == '2026-10-31')
passed('row errors include missing precision period and both duplicates', detail['rowCount'] == 6 and detail['validCount'] == 1 and
       detail['errorCount'] == 5 and {'REQUIRED', 'TYPE', 'PERIOD', 'DUPLICATE'} <= issue_codes(detail['result']))
passed('zero differs from missing and personnel match is explicit', detail['result']['rows'][0]['values']['amount'] == '0' and
       detail['result']['rows'][1]['values']['amount'] is None and detail['result']['employeeMatchEnabled'] is False)
passed('reupload returns frozen original despite different filename', preview(id, file, filename='renamed.csv')['data'] == main)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    batches = list(pool.map(lambda _: preview(id, template + b'QAVR-C,2026-10-02,' + '测试主体'.encode() + b',0,concurrent\n'), range(2)))
passed('concurrent identical uploads return one batch', all(r['code'] == 0 for r in batches) and batches[0]['data'] == batches[1]['data'])
own = preview(id, file, role='editor')['data']
passed('idempotency includes submitting owner', own != main)
passed('same-tenant reader cannot read other owner batch', call('/batches/get', params={'id': main}, role='reader')['code'] == 1050920005)
passed('super administrator also cannot read other owner batch', call('/batches/get', params={'id': own})['code'] == 1050920005)
passed('owned batch list excludes payload and foreign batches', all(b['id'] != main and 'snapshot' not in b for b in success('/batches/page', params={'pageNo': 1, 'pageSize': 100}, role='editor')['list']))
scope_id = preview(id, template + 'S,2026-10-02,其他主体,0,范围不符\n'.encode())['data']
passed('scope mismatch retained as row error', 'SCOPE' in issue_codes(batch(scope_id)['result']))
passed('reversed period rejected', preview(id, file, start='2026-11-01')['code'] == 1050920001)
passed('oversized file rejected', preview(id, b'a' * (1024 * 1024 + 1))['code'] == 1050920001)
for label, content, code in [('wrong header', template.replace(b'job,date,scope,amount,note', b'job,date,scope,amount,unknown') + rows, 'HEADERS'),
                              ('invalid encoding', b'\xc3(', 'ENCODING'), ('malformed quotes', template + b'"unclosed', 'CSV_FORMAT')]:
    bad = preview(id, content)['data']
    passed(label + ' saved as failed batch', batch(bad)['status'] == 1 and code in issue_codes(batch(bad)['result']))
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    versions = list(pool.map(lambda _: success('/contracts/create', method='POST', data=request, role='editor'), range(2)))
numbers = sorted(get(v)['contractVersion'] for v in versions)
passed('concurrent versions allocated sequentially under source lock', numbers == [row['contractVersion'] + 1, row['contractVersion'] + 2])
passed('new version requires its own confirmation', all(get(v)['status'] == 0 for v in versions))
assert review(versions[0])['code'] == 0
wrong_version = preview(versions[0], file)['data']
passed('old template rejected for new contract version', 'TEMPLATE_VERSION' in issue_codes(batch(wrong_version)['result']))
missing_precision = copy.deepcopy(request); missing_precision['schema']['fields'][3].pop('scale')
incomplete = success('/contracts/create', method='POST', data=missing_precision)
passed('confirm requires explicit decimal precision', review(incomplete)['code'] == 1050920001)
person = copy.deepcopy(request); person['schema']['employeeField'] = 'job'; person['title'] = '回归样例：工号匹配 ' + str(int(time.time()))
person_id = success('/contracts/create', method='POST', data=person); assert review(person_id)['code'] == 0
person_template = call('/contracts/template', params={'id': person_id}, raw=True)[0]
person_rows = ('QAVR-INTAKE-KNOWN,2026-10-02,测试主体,0,已知工号\nQAVR-INTAKE-MISSING,2026-10-02,测试主体,1,未知工号\n'
               'QAVR-INTAKE-DUP,2026-10-02,测试主体,1,重复主档\nQAVR-INTAKE-OTHER,2026-10-02,测试主体,1,其他租户\n').encode()
passed('employee lookup requires separate HRM query permission', preview(person_id, person_template + person_rows, role='editor')['code'] == 1050920006)
person_batch = preview(person_id, person_template + person_rows)['data']
person_detail = batch(person_batch)
passed('employee matching rejects missing ambiguous and foreign-tenant identities', person_detail['validCount'] == 1 and
       person_detail['result']['rows'][0]['employeeId'] == 920000001 and
       [r['issues'][0]['code'] for r in person_detail['result']['rows'][1:]] == ['UNKNOWN_EMPLOYEE', 'AMBIGUOUS_EMPLOYEE', 'UNKNOWN_EMPLOYEE'])
for endpoint in ['/contracts/get', '/contracts/history', '/contracts/template']:
    passed('tenant B denied ' + endpoint, call(endpoint, params={'id': id}, role='tenantb')['code'] == 1050920000)
passed('tenant B denied contract modification', call('/contracts/update', method='PUT', data=row, role='tenantb')['code'] == 1050920000)
passed('tenant B denied other tenant source contract creation', call('/contracts/create', method='POST', data=request, role='tenantb')['code'] == 1050920001)
passed('tenant B denied batch and upload', call('/batches/get', params={'id': main}, role='tenantb')['code'] == 1050920005 and preview(id, file, role='tenantb')['code'] == 1050920000)
passed('tenant B lists contain no tenant A data', success('/contracts/page', params={'pageNo': 1, 'pageSize': 100}, role='tenantb')['total'] == 0 and success('/sources', role='tenantb') == [])
passed('retirement succeeds and blocks new preview', review(versions[0], 'retire')['code'] == 0 and preview(versions[0], file)['code'] == 1050920004)
passed('retired contract batch retains confirmed snapshot', batch(wrong_version)['contractSnapshot']['status'] == 1)
history = success('/contracts/history', params={'id': id})
passed('contract history contains only successful authorized revisions', len(history) == 4 and history[0]['action'] == 'confirm')
(args.output_dir / 'hrm-intake-api-results.json').write_text(json.dumps(checks, ensure_ascii=False, indent=2))
(args.output_dir / 'intake-preview-sample.csv').write_bytes(file)
(args.output_dir / 'hrm-intake-browser-fixture.json').write_text(json.dumps({'contractId': id, 'contractTitle': request['title'],
    'mainBatchId': main, 'personContractId': person_id, 'personBatchId': person_batch, 'retiredContractId': versions[0]}, ensure_ascii=False))
print('All', len(checks), 'intake API/MySQL regression checks passed')
