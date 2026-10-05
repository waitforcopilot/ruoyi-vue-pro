#!/usr/bin/env python3
"""Live policy regression. All monetary samples are synthetic; isolated QA only."""
import argparse, concurrent.futures, copy, json, os, subprocess, time, urllib.error, urllib.parse, urllib.request, uuid
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url', default='http://127.0.0.1:48081')
parser.add_argument('--allow-test-fixtures', action='store_true', required=True)
parser.add_argument('--output-dir', type=Path, required=True)
parser.add_argument('--database', default='hrm_payroll_collection_20261004b')
parser.add_argument('--mysql-container', default='codex-ruoyi-mysql')
args = parser.parse_args(); args.output_dir.mkdir(parents=True, exist_ok=True)
if not args.database.startswith('hrm_payroll_collection_'): parser.error('Use an isolated collection QA database')
roles = {'admin': (1, 'HRM_SMOKE_TOKEN'), 'reader': (1, 'HRM_POLICY_READER_TOKEN'), 'editor': (1, 'HRM_POLICY_EDITOR_TOKEN'),
         'reviewer': (1, 'HRM_POLICY_REVIEWER_TOKEN'), 'none': (1, 'HRM_UNGRANTED_TOKEN'), 'tenantb': (999, 'HRM_TENANT_B_TOKEN')}
for _, env in roles.values():
    if not os.environ.get(env): parser.error('Missing verification credential: ' + env)
base = args.base_url.rstrip('/') + '/admin-api/hrm/payroll/insurance-policies'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({})); checks = []
def call(path, method='GET', data=None, params=None, role='admin'):
    tenant, env = roles.get(role, (1, None)); headers = {'tenant-id': str(tenant), 'Content-Type': 'application/json'}
    if env: headers['Authorization'] = 'Bearer ' + os.environ[env]
    request = urllib.request.Request(base + path + ('?' + urllib.parse.urlencode(params) if params else ''), method=method,
        headers=headers, data=None if data is None else json.dumps(data, ensure_ascii=False).encode())
    try: response = opener.open(request, timeout=30)
    except urllib.error.HTTPError as error: response = error
    return json.load(response)
def success(path, **kw):
    result = call(path, **kw); assert result.get('code') == 0, (path, result.get('code')); return result.get('data')
def passed(name, condition):
    assert condition, name; checks.append({'check': name, 'result': 'PASS'}); print('PASS:', name, flush=True)
def sql(statement):
    result = subprocess.run(['docker', 'exec', '-i', args.mysql_container, 'sh', '-c',
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -N -uroot "$1"', 'sh', args.database], input=statement, text=True, capture_output=True)
    assert result.returncode == 0, 'Isolated checksum query failed'; return result.stdout
def get(id, role='admin'): return success('/get', params={'id': id}, role=role)
def update(id, **changes): return success('/update', method='PUT', data={**get(id), **changes}, role='editor')
def version(id): return success('/new-version', method='POST', params={'id': id, 'revision': get(id)['revision']}, role='editor')
def review(id, action='confirm', revision=None, role='reviewer'):
    return call('/review', method='POST', role=role, data={'id': id, 'revision': get(id)['revision'] if revision is None else revision,
        'action': action, 'evidence': '隔离 QA 合成政策评审，不代表官方政策', 'reviewedBy': 123, 'reviewedByName': 'FORGED'})
def preview(id, amount, start='2026-10-01', end='2026-10-31', role='reader'):
    return call('/preview', method='POST', role=role, data={'policyId': id, 'start': start, 'end': end, 'baseAmount': amount})
def parallel(fn):
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool: return list(pool.map(fn, range(2)))
passed('anonymous policy access denied', call('/projects', role=None)['code'] == 401)
passed('ungranted policy access denied', call('/projects', role='none')['code'] == 403)
projects = success('/projects', role='reader')
passed('project catalogue reuses all existing types and custom classification', len(projects) == 11 and next(p for p in projects if p['type'] == 9)['custom'] is True)
tables = ['hrm_insurance_scheme', 'hrm_insurance_scheme_project', 'hrm_insurance_employee_info', 'hrm_insurance_month_record',
          'hrm_insurance_month_employee_record', 'hrm_salary_employee_info', 'hrm_salary_month_record', 'hrm_salary_month_employee_record', 'hrm_salary_slip_send_record']
known = set(sql('SHOW TABLES;').splitlines()); tables = [t for t in tables if t in known]; before = sql('CHECKSUM TABLE ' + ','.join(tables) + ';')
stamp = str(int(time.time())) + '-' + uuid.uuid4().hex[:4]; scope = ('INS-QA-' + stamp).upper()
config = {'lowerBase': '0', 'upperBase': '10000', 'baseUnit': 'YUAN_MONTH', 'corporateMode': 'RATE', 'personalMode': 'RATE',
          'corporateRatePercent': '16', 'personalRatePercent': '8', 'amountScale': 2, 'roundingMode': 'HALF_UP', 'roundingStage': 'TOTAL'}
request = {'cityAreaId': 330100, 'scopeCode': scope, 'scopeName': '合成技术人群，非真实参保范围', 'projectType': 1,
           'title': '合成本地政策 ' + stamp, 'ownerName': 'QA 政策负责人', 'reference': '合成技术样例出处，非官方政策',
           'sourceUrl': 'https://example.test/policy', 'effectiveFrom': '2026-10-01', 'effectiveTo': '2026-10-31', 'config': config}
empty = success('/create', method='POST', role='editor', data={**request, 'scopeCode': scope+'-EMPTY', 'config': {}})
passed('empty draft keeps missing values rather than policy defaults', not get(empty)['config'].get('lowerBase') and not get(empty)['config'].get('roundingMode') and review(empty)['code'] == 1050960001)
passed('reader cannot create or review policy records', call('/create', method='POST', data=request, role='reader')['code'] == 403 and review(empty, role='reader')['code'] == 403)
main = success('/create', method='POST', role='editor', data={**request, 'id': 987654321, 'status': 1, 'policyVersion': 98, 'tenantId': 999, 'configSchemaVersion': 99})
row = get(main); original = copy.deepcopy(row['config'])
passed('server owns tenant initial version status and configuration schema', main != 987654321 and row['policyVersion'] == 1 and row['revision'] == 1 and row['status'] == 0 and row['configSchemaVersion'] == 1)
passed('money and percentage fields are exact decimal strings', row['config']['lowerBase'] == '0.00' and row['config']['corporateRatePercent'] == '16.0000')
passed('district and city cannot register duplicate policy identity', call('/create', method='POST', role='editor', data={**request, 'cityAreaId': 330102})['code'] == 1050960004)
results = parallel(lambda _: call('/create', method='POST', role='editor', data={**request, 'scopeCode': scope+'-RACE'}))
passed('concurrent initial registration has one winner', sorted(r['code'] for r in results) == [0, 1050960004])
row = get(main); results = parallel(lambda n: call('/update', method='PUT', role='editor', data={**row, 'reference': '合成并发核对 ' + str(n)}))
passed('concurrent same-revision editing has one winner', sorted(r['code'] for r in results) == [0, 1050960002])
passed('maintenance and review roles are separate', review(main, role='editor')['code'] == 403 and call('/update', method='PUT', role='reviewer', data=get(main))['code'] == 403)
passed('city scope and project identity cannot be edited', call('/update', method='PUT', role='editor', data={**get(main), 'scopeCode': scope+'-CHANGED'})['code'] == 1050960001)
update(main, ownerName=None); passed('confirmation requires responsible person', review(main)['code'] == 1050960001); update(main, ownerName=request['ownerName'])
revision = get(main)['revision']; results = parallel(lambda _: review(main, revision=revision))
passed('concurrent same-revision review has one winner', sorted(r['code'] for r in results) == [0, 1050960002])
row = get(main)
passed('review actor and timestamp are stamped by the server', row['reviewedByName'] == '政策回归 reviewer' and isinstance(row['reviewedTime'], int) and row['reviewedTime'] > 0)
passed('confirmed policy cannot be edited', call('/update', method='PUT', role='editor', data=row)['code'] == 1050960003)
calculation = preview(main, '5000')['data']
passed('live percentage calculation returns exact amounts and explanation', calculation['corporateAmount'] == '800.00' and calculation['personalAmount'] == '400.00' and '16.0000%' in calculation['corporateExpression'] and '不生成月账' in calculation['explanation'])
passed('valid zero and inclusive upper boundary are accepted', preview(main, '0')['data']['personalAmount'] == '0.00' and preview(main, '10000')['data']['corporateAmount'] == '1600.00')
passed('out-of-range base is blocked without clamping', preview(main, '10000.01')['code'] == 1050960006)
passed('wrong declared period does not use a policy implicitly', preview(main, '5000', start='2026-09-30')['code'] == 1050960001)
page = success('/page', params={'pageNo':1, 'pageSize':100, 'scopeCode':scope})
passed('policy lists omit parameters and freeform evidence', page['total'] == 1 and all(not r.get('config') and not r.get('reference') and not r.get('evidence') for r in page['list']))
history = success('/history', params={'id':main})
passed('audit retains exact actor and before-after policy parameters', any(h['action'] == 'confirm' and h['actorId'] == 970000003 and h.get('beforeSnapshot') and 'configJson' in h['afterSnapshot'] for h in history))
foreign = [call('/get', params={'id':main}, role='tenantb'), call('/history', params={'id':main}, role='tenantb'),
    call('/update', method='PUT', data=get(main), role='tenantb'), call('/new-version', method='POST', params={'id':main,'revision':row['revision']}, role='tenantb'),
    call('/review', method='POST', data={'id':main,'revision':row['revision'],'action':'retire','evidence':'合成验证'}, role='tenantb'),
    call('/resolve', params={'id':main,'start':'2026-10-01','end':'2026-10-31'}, role='tenantb'),
    call('/compare', params={'leftId':main,'rightId':main}, role='tenantb'), preview(main,'5000',role='tenantb')]
passed('tenant B cannot access IDs history edits reviews copies periods or arithmetic', all(r['code'] == 1050960000 for r in foreign) and success('/page', params={'pageNo':1,'pageSize':100,'scopeCode':scope}, role='tenantb')['total'] == 0)
next_id = version(main); copied = get(next_id)
passed('new policy version copies parameters and clears review conclusions', copied['policyVersion'] == 2 and copied['status'] == 0 and copied['config'] == original and not copied.get('reviewedByName'))
passed('overlapping confirmed policy intervals are rejected', review(next_id)['code'] == 1050960005)
changed = {**copied['config'], 'corporateMode':'RATE_PLUS_FIXED', 'corporateRatePercent':'1.5', 'corporateFixedAmount':'0.50',
           'personalRatePercent':'0', 'amountScale':0, 'roundingStage':'COMPONENT'}
update(next_id, config=changed, effectiveFrom='2026-11-01', effectiveTo='2026-11-30'); assert review(next_id)['code'] == 0
calculation = preview(next_id, '100', '2026-11-01', '2026-11-30')['data']
passed('component rounding and explicit zero match the confirmed version', calculation['corporateAmount'] == '3' and calculation['personalAmount'] == '0' and calculation['policy']['config']['amountScale'] == 0 and calculation['corporateSteps'] == '比例部分 1.5 → 2；固定部分 0.5 → 1；合计 3' and calculation['personalSteps'] == '比例部分 0 → 0')
total_id = success('/create', method='POST', role='editor', data={**request,'scopeCode':scope+'-TOTAL','config':{**changed,'roundingStage':'TOTAL'}})
assert review(total_id)['code'] == 0
total_calculation = preview(total_id,'100')['data']
passed('total rounding differs from component rounding as declared', total_calculation['corporateAmount'] == '2' and total_calculation['corporateSteps'] == '比例部分 1.5 + 固定部分 0.5 = 2；合计舍入 → 2')
passed('changing a new version preserves all old parameters', get(main)['config'] == original)
comparison = success('/compare', params={'leftId':main,'rightId':next_id})
passed('historical comparison preserves zero missing and rounding changes', any(c['path']=='personalRatePercent' and c['right']=='0.0000' for c in comparison['changes']) and any(c['path']=='corporateFixedAmount' and c.get('left') is None and c['right']=='0.50' for c in comparison['changes']))
passed('equal version comparison has no artificial timestamp differences', not success('/compare', params={'leftId':main,'rightId':main})['changes'])
passed('adjacent periods resolve their own policy versions', success('/resolve', params={'id':main,'start':'2026-10-01','end':'2026-10-31'})['id'] == main and success('/resolve', params={'id':main,'start':'2026-11-01','end':'2026-11-30'})['id'] == next_id)
passed('period selection never stitches adjacent versions', call('/resolve', params={'id':main,'start':'2026-10-31','end':'2026-11-01'})['code'] == 1050960001)
missing = success('/create', method='POST', role='editor', data={**request,'scopeCode':scope+'-MISSING','config':{**config,'personalRatePercent':None}})
passed('missing active input blocks confirmation rather than becoming zero', review(missing)['code'] == 1050960001)
zero_copy = version(missing); update(zero_copy, config={**config,'personalRatePercent':'0'})
passed('missing-to-zero remains visible in saved policy comparison', any(c['path']=='personalRatePercent' and c.get('left') is None and c['right']=='0.0000' for c in success('/compare', params={'leftId':missing,'rightId':zero_copy})['changes']))
fixed = success('/create', method='POST', role='editor', data={**request,'scopeCode':scope+'-FIXED','config':{**config,'corporateMode':'FIXED','corporateRatePercent':None,'corporateFixedAmount':'12.50','personalMode':'FIXED','personalRatePercent':None,'personalFixedAmount':'0'}})
assert review(fixed)['code'] == 0
passed('fixed amounts and valid zero are not confused with missing inputs', preview(fixed,'0')['data']['corporateAmount'] == '12.50' and preview(fixed,'0')['data']['personalAmount'] == '0.00')
round_results = []
for mode in ['HALF_UP','HALF_EVEN','DOWN','UP']:
    id = success('/create', method='POST', role='editor', data={**request,'scopeCode':scope+'-'+mode,'config':{**config,'corporateRatePercent':'50','amountScale':0,'roundingMode':mode}})
    assert review(id)['code'] == 0; round_results.append(preview(id,'1')['data']['corporateAmount'])
passed('all four rounding modes survive live JSON and database roundtrips', round_results == ['1','0','0','1'])
large = success('/create', method='POST', role='editor', data={**request,'scopeCode':scope+'-LARGE','config':{**config,'upperBase':'9999999999.99','corporateRatePercent':'1.2345','amountScale':4}})
assert review(large)['code'] == 0
passed('large base and four-place percentage retain exact arithmetic', preview(large,'9999999999.99')['data']['corporateAmount'] == '123449999.9999')
for name,bad in [('precision',{'config':{**config,'lowerBase':'1.001'}}),('upper',{'config':{**config,'lowerBase':'3','upperBase':'2'}}),('url',{'sourceUrl':'javascript:alert(1)'}),('mode',{'config':{**config,'corporateFixedAmount':'0'}}),('custom',{'projectType':9,'customProjectCode':None})]:
    passed('invalid policy '+name+' rejected before registration', call('/create', method='POST', role='editor', data={**request,'scopeCode':scope+'-BAD-'+name.upper(),**bad})['code'] != 0)
versions = parallel(lambda _: version(next_id))
passed('concurrent version allocation is sequential', sorted(get(id)['policyVersion'] for id in versions) == [3,4])
for id in versions: update(id,effectiveFrom='2026-12-01',effectiveTo='2026-12-31')
results = parallel(lambda n: review(versions[n]))
passed('concurrent overlapping reviews have one winner', sorted(r['code'] for r in results) == [0,1050960005])
passed('cross-policy series comparison is rejected', call('/compare', params={'leftId':main,'rightId':fixed})['code'] == 1050960001)
assert review(main,'retire')['code'] == 0
passed('retired policy retains parameters and history but cannot calculate', get(main)['config'] == original and get(main)['status'] == 2 and preview(main,'5000')['code'] == 1050960001 and bool(success('/history',params={'id':main})))
passed('retired series still resolves another applicable confirmed version', success('/resolve', params={'id':main,'start':'2026-11-01','end':'2026-11-30'})['id'] == next_id and call('/resolve', params={'id':main,'start':'2026-10-01','end':'2026-10-31'})['code'] == 1050960001)
passed('legacy insurance salary and delivery table checksums stay unchanged', before == sql('CHECKSUM TABLE '+','.join(tables)+';'))
fixture = {'scopeCode':scope,'title':request['title'],'leftId':main,'rightId':next_id,'leftVersion':1,'rightVersion':2,'cityAreaId':330100,
           'start':'2026-11-01','end':'2026-11-30','baseAmount':'100','corporateAmount':'3','personalAmount':'0'}
(args.output_dir/'hrm-insurance-api-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2)+'\n')
(args.output_dir/'hrm-insurance-ui-fixture.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2)+'\n')
print('All',len(checks),'live insurance policy API checks passed')
