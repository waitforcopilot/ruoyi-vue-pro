#!/usr/bin/env python3
"""Live rule execution regression using synthetic fixtures in an isolated QA database."""
import argparse
import concurrent.futures
import copy
import json
import os
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url', default='http://127.0.0.1:48081')
parser.add_argument('--allow-test-fixtures', action='store_true', required=True)
parser.add_argument('--output-dir', type=Path, required=True)
parser.add_argument('--database', default='hrm_payroll_collection_20261004b')
parser.add_argument('--mysql-container', default='codex-ruoyi-mysql')
args = parser.parse_args()
args.output_dir.mkdir(parents=True, exist_ok=True)
if not args.database.startswith('hrm_payroll_collection_'):
    parser.error('Use an isolated collection QA database')
roles = {'admin': (1, 'HRM_SMOKE_TOKEN'), 'none': (1, 'HRM_UNGRANTED_TOKEN'),
         'tenantb': (999, 'HRM_TENANT_B_TOKEN')}
roles.update({r: (1, 'HRM_CALC_'+r.upper()+'_TOKEN')
              for r in ['reader', 'editor', 'reviewer', 'maintainonly', 'reviewonly']})
for _, name in roles.values():
    if not os.environ.get(name): parser.error('Missing verification credential: '+name)
base = args.base_url.rstrip('/') + '/admin-api/hrm/payroll/calculation-definitions'
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
checks = []

def call(path, method='GET', data=None, params=None, role='admin'):
    tenant, name = roles.get(role, (1, None))
    headers = {'tenant-id': str(tenant), 'Content-Type': 'application/json'}
    if name: headers['Authorization'] = 'Bearer ' + os.environ[name]
    request = urllib.request.Request(base+path+('?' + urllib.parse.urlencode(params) if params else ''),
        method=method, headers=headers, data=None if data is None else json.dumps(data, ensure_ascii=False).encode())
    try: response = opener.open(request, timeout=30)
    except urllib.error.HTTPError as error: response = error
    return json.load(response)

def success(path, **kw):
    result = call(path, **kw)
    assert result.get('code') == 0, (path, result.get('code'))
    return result.get('data')

def passed(name, condition):
    assert condition, name
    checks.append({'check': name, 'result': 'PASS'})
    print('PASS:', name, flush=True)

def sql(statement):
    result = subprocess.run(['docker', 'exec', '-i', args.mysql_container, 'sh', '-c',
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -N -uroot "$1"', 'sh', args.database],
        input=statement, text=True, capture_output=True)
    assert result.returncode == 0, 'Isolated checksum query failed'
    return result.stdout

def get(id, role='admin'): return success('/get', params={'id': id}, role=role)
def update(id, **changes): return success('/update', method='PUT', data={**get(id), **changes}, role='editor')
def version(id): return success('/new-version', method='POST', params={'id': id, 'revision': get(id)['revision']}, role='editor')
def review(id, action='confirm', role='reviewer', revision=None):
    return call('/review', method='POST', role=role, data={'id': id,
        'revision': get(id)['revision'] if revision is None else revision, 'action': action,
        'evidence': '隔离 QA 合成运算评审，不代表正式薪资政策', 'reviewedBy': 123, 'reviewedByName': 'FORGED'})
def preview(id, inputs=None, start='2026-10-01', end='2026-10-31', role='reader'):
    return call('/preview', method='POST', role=role, data={'definitionId': id, 'start': start, 'end': end,
        'inputs': {'base': '1000.00', 'days': '1', 'cycleDays': '3'} if inputs is None else inputs})
def parallel(fn):
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool: return list(pool.map(fn, range(2)))

passed('anonymous and ungranted access denied', call('/page', params={'pageNo':1,'pageSize':10}, role=None)['code'] == 401
       and call('/page', params={'pageNo':1,'pageSize':10}, role='none')['code'] == 403)
tables = ['hrm_salary_employee_info', 'hrm_salary_month_record', 'hrm_salary_month_employee_record',
          'hrm_salary_slip_send_record', 'hrm_insurance_month_record', 'hrm_insurance_month_employee_record']
known = set(sql('SHOW TABLES;').splitlines())
tables = [t for t in tables if t in known]
assert tables, 'Expected existing wage tables in QA'
before = sql('CHECKSUM TABLE '+','.join(tables)+';')
stamp = str(int(time.time()))+'-'+uuid.uuid4().hex[:4].upper()
code = 'CALC-QA-'+stamp
scope = 'SYNTHETIC-'+stamp
program = {
    'inputs': [{'key': 'base', 'label': '合成基础金额', 'type': 'DECIMAL', 'unit': '合成元', 'scale': 2},
               {'key': 'days', 'label': '合成参与天数', 'type': 'INTEGER', 'unit': '天', 'scale': 0},
               {'key': 'cycleDays', 'label': '合成期间天数', 'type': 'INTEGER', 'unit': '天', 'scale': 0}],
    'items': [{'key': key, 'label': label, 'unit': '合成元', 'expression': expression,
               'amountScale': 2, 'roundingMode': 'HALF_UP'} for key, label, expression in
              [('net', '合成结果', 'prorated - deduction'), ('prorated', '合成折算额', 'base * days / cycleDays'), ('deduction', '合成调整', '0')]],
    'divisionScale': 8, 'divisionRoundingMode': 'HALF_UP',
    'cases': [{'title': '合成三分之一运算', 'inputs': {'base': '1000.00', 'days': '1', 'cycleDays': '3'},
               'expected': {'net': '333.33', 'prorated': '333.33', 'deduction': '0.00'}}]}
request = {'code': code, 'title': '合成规则核对 '+stamp, 'scopeCode': scope,
           'ownerName': 'QA 运算负责人', 'applicableScope': '合成技术范围，非真实工资人群',
           'description': '用于验收规则依赖、精度和对账能力', 'reference': '合成数学样例，非 HR/财务签认政策',
           'effectiveFrom': '2026-10-01', 'effectiveTo': '2026-10-31', 'program': program}
passed('reader cannot create definitions', call('/create', method='POST', role='reader', data=request)['code'] == 403)
passed('maintenance and review permissions each require query permission',
       call('/create', method='POST', role='maintainonly', data=request)['code'] == 403 and
       call('/review', method='POST', role='reviewonly', data={'id':1,'revision':1,'action':'confirm','evidence':'合成依据'})['code'] == 403)
main = success('/create', method='POST', role='editor', data={**request, 'id':987654321,'revision':98,
        'definitionVersion':98,'schemaVersion':99,'status':1,'tenantId':999,'reviewedBy':123})
row = get(main)
passed('server owns initial tenant version revision schema and draft status', main != 987654321 and row['definitionVersion'] == 1
       and row['revision'] == 1 and row['status'] == 0 and row['schemaVersion'] == 1 and not row.get('reviewedByName'))
passed('saved JSON preserves explicit zero precision and decimal text', row['program']['inputs'][1]['scale'] == 0
       and row['program']['cases'][0]['inputs']['base'] == '1000.00' and row['program']['cases'][0]['expected']['deduction'] == '0.00')
passed('reader accesses cases and history but cannot update copy or review', success('/cases',params={'id':main},role='reader')['allPassed']
       and bool(success('/history',params={'id':main},role='reader')) and call('/update',method='PUT',role='reader',data=row)['code'] == 403
       and call('/new-version',method='POST',role='reader',params={'id':main,'revision':1})['code'] == 403 and review(main,role='reader')['code'] == 403)
passed('maintenance and review duties are separate', review(main,role='editor')['code'] == 403 and call('/update',method='PUT',role='reviewer',data=row)['code'] == 403)
passed('draft cannot execute declared inputs', preview(main)['code'] == 1050970001)
passed('same tenant and code cannot register another first version', call('/create',method='POST',role='editor',data=request)['code'] == 1050970004)
results = parallel(lambda _: call('/create',method='POST',role='editor',data={**request,'code':code+'-RACE'}))
passed('concurrent initial registration has one winner', sorted(r['code'] for r in results) == [0,1050970004])
row = get(main)
results = parallel(lambda n: call('/update',method='PUT',role='editor',data={**row,'description':'合成并发修改 '+str(n)}))
passed('concurrent same-revision editing has one winner', sorted(r['code'] for r in results) == [0,1050970002])
passed('rule and scope identities cannot be changed', call('/update',method='PUT',role='editor',data={**get(main),'scopeCode':scope+'-OTHER'})['code'] == 1050970001)
update(main, ownerName=None, description=None)
passed('nullable metadata clears and missing owner blocks confirmation', get(main).get('ownerName') is None and get(main).get('description') is None and review(main)['code'] == 1050970001)
update(main, ownerName=request['ownerName'])
revision = get(main)['revision']
results = parallel(lambda _: review(main, revision=revision))
passed('concurrent same-revision confirmation has one winner', sorted(r['code'] for r in results) == [0,1050970002])
row = get(main)
passed('review actor and time are stamped by the server', row['reviewedByName'] == '计算回归 reviewer' and isinstance(row['reviewedTime'],int) and row['reviewedTime'] > 0)
passed('confirmed rules are immutable', call('/update',method='PUT',role='editor',data=row)['code'] == 1050970003)
calculated = preview(main)['data']
items = calculated['result']['items']
passed('dependencies execute before dependents and use rounded predecessor amounts', [i['key'] for i in items] == ['prorated','deduction','net']
       and [i['amount'] for i in items] == ['333.33','0.00','333.33'] and items[0]['rawResult'] == '333.33333333')
passed('exact strings and rounding steps accompany every result', all(isinstance(i['amount'],str) and i['steps'] for i in items)
       and any('除法 8 位 / HALF_UP' in step for step in items[0]['steps']) and '不生成工资月表' in calculated['explanation']
       and len(calculated['programHash']) == 64)
passed('explicit zero input is accepted', all(i['amount'] == '0.00' for i in preview(main, {'base':'0','days':'1','cycleDays':'3'})['data']['result']['items']))
invalid_inputs = [('missing',{'base':'1000.00','days':'1'}),('extra',{'base':'1000.00','days':'1','cycleDays':'3','other':'0'}),
                  ('blank',{'base':'','days':'1','cycleDays':'3'}),('null',{'base':None,'days':'1','cycleDays':'3'}),
                  ('excess-scale',{'base':'1.001','days':'1','cycleDays':'3'}),('integer-decimal',{'base':'1','days':'1.0','cycleDays':'3'}),
                  ('exponent',{'base':'1e3','days':'1','cycleDays':'3'}),('divide-zero',{'base':'1','days':'1','cycleDays':'0'}),
                  ('JSON-number',{'base':1000,'days':'1','cycleDays':'3'}),('JSON-boolean',{'base':True,'days':'1','cycleDays':'3'})]
for name, values in invalid_inputs:
    passed('invalid input '+name+' rejected without coercion or implicit zero', preview(main,values)['code'] != 0)
passed('declared period must be valid and fully covered by selected version', preview(main,start='2026-09-30')['code'] == 1050970001
       and preview(main,start='2026-10-31',end='2026-10-01')['code'] == 1050970001)
page = success('/page',params={'pageNo':1,'pageSize':100,'code':code},role='reader')
passed('list omits expressions cases and freeform evidence', page['total'] == 1 and all(not r.get('program') and not r.get('verifiedCases')
       and not r.get('description') and not r.get('reference') and not r.get('evidence') for r in page['list']))
events = success('/history',params={'id':main})
passed('audit retains before and after definitions and actual actor', any(e['action'] == 'confirm' and e['actorName'] == '计算回归 reviewer'
       and 'programJson' in e['beforeSnapshot'] and 'programJson' in e['afterSnapshot'] for e in events))
foreign = [call('/get',params={'id':main},role='tenantb'),call('/cases',params={'id':main},role='tenantb'),
           call('/history',params={'id':main},role='tenantb'),call('/compare',params={'leftId':main,'rightId':main},role='tenantb'),
           call('/update',method='PUT',data=row,role='tenantb'),call('/new-version',method='POST',params={'id':main,'revision':row['revision']},role='tenantb'),
           call('/review',method='POST',role='tenantb',data={'id':main,'revision':row['revision'],'action':'retire','evidence':'合成隔离验收'}),preview(main,role='tenantb')]
passed('all reads writes histories and arithmetic are tenant isolated', all(r['code'] == 1050970000 for r in foreign)
       and success('/page',params={'pageNo':1,'pageSize':100,'code':code},role='tenantb')['total'] == 0)
next_id = version(main)
copied = get(next_id)
passed('new version copies definitions and clears approval', copied['definitionVersion'] == 2 and copied['revision'] == 1
       and copied['status'] == 0 and copied['program'] == program and not copied.get('reviewedByName') and not copied.get('evidence'))
passed('overlapping confirmation periods are blocked', review(next_id)['code'] == 1050970005)
changed = copy.deepcopy(program)
changed['items'][2]['amountScale'] = 0
changed['cases'][0]['expected']['deduction'] = '0'
update(next_id, program=changed, effectiveFrom='2026-11-01', effectiveTo='2026-11-30', description='合成调整明确使用 0 位小数')
assert review(next_id)['code'] == 0
passed('zero-place result precision survives JSON database and execution', preview(next_id,start='2026-11-01',end='2026-11-30')['data']['result']['items'][1]['amount'] == '0'
       and get(next_id)['program']['items'][2]['amountScale'] == 0)
passed('old version stays unchanged after new precision is approved', get(main)['program'] == program)
comparison = success('/compare',params={'leftId':main,'rightId':next_id})
passed('comparison preserves exact precision and period changes', any(c['path'] == 'program' and json.loads(c['right'])['items'][2]['amountScale'] == 0
       and json.loads(c['left'])['items'][2]['amountScale'] == 2 for c in comparison['changes'])
       and not success('/compare',params={'leftId':main,'rightId':main})['changes'])
passed('period execution does not silently join adjacent versions', preview(main,start='2026-10-31',end='2026-11-01')['code'] == 1050970001)
bad_cases = copy.deepcopy(program)
bad_cases['cases'][0]['expected']['net'] = '334.00'
failed = success('/create',method='POST',role='editor',data={**request,'code':code+'-FAILED','program':bad_cases})
passed('mismatched business cases remain inspectable drafts and block approval', not get(failed)['verifiedCases']['allPassed'] and review(failed)['code'] == 1050970006)
bad_cases['cases'][0]['expected']['net'] = 'invalid'
update(failed,program=bad_cases)
passed('invalid expected decimal cannot become a passed case', not success('/cases',params={'id':failed})['allPassed'] and review(failed)['code'] == 1050970006)
empty_cases = copy.deepcopy(program); empty_cases['cases'] = []
empty = success('/create',method='POST',role='editor',data={**request,'code':code+'-EMPTY','program':empty_cases})
passed('zero business cases cannot approve a rule', get(empty)['verifiedCases']['total'] == 0 and not get(empty)['verifiedCases']['allPassed'] and review(empty)['code'] == 1050970006)
for name, mutate in [('cycle',lambda p:p['items'][1].update(expression='net')),
                     ('unknown',lambda p:p['items'][1].update(expression='undeclared')),
                     ('script',lambda p:p['items'][1].update(expression='T(java.lang.Runtime).getRuntime()')),
                     ('missing-precision',lambda p:p['items'][0].pop('amountScale')),
                     ('float-precision',lambda p:p['items'][0].update(amountScale=2.5)),
                     ('string-precision',lambda p:p['items'][0].update(amountScale='2')),
                     ('missing-division',lambda p:p.pop('divisionScale')),
                     ('numeric-case',lambda p:p['cases'][0]['inputs'].update(base=1000))]:
    invalid = copy.deepcopy(program); mutate(invalid)
    passed('invalid definition '+name+' rejected before persistence', call('/create',method='POST',role='editor',data={**request,'code':code+'-'+name.upper(),'program':invalid})['code'] != 0)
versions = parallel(lambda _: version(next_id))
passed('concurrent version allocation remains sequential in MySQL', sorted(get(id)['definitionVersion'] for id in versions) == [3,4])
for id in versions: update(id,effectiveFrom='2026-12-01',effectiveTo='2026-12-31')
results = parallel(lambda n: review(versions[n]))
passed('concurrent overlapping version approvals have one winner', sorted(r['code'] for r in results) == [0,1050970005])
confirmed = next(id for id, response in zip(versions, results) if response['code'] == 0)
assert review(confirmed,'retire')['code'] == 0
passed('retirement retains definitions and audit while disabling execution', get(confirmed)['status'] == 2 and get(confirmed)['program'] == changed
       and preview(confirmed,start='2026-12-01',end='2026-12-31')['code'] == 1050970001 and bool(success('/history',params={'id':confirmed})))
passed('different rule series cannot be compared', call('/compare',params={'leftId':main,'rightId':failed})['code'] == 1050970001)
passed('existing salary insurance and delivery checksums stay unchanged', before == sql('CHECKSUM TABLE '+','.join(tables)+';'))
fixture = {'code':code,'scopeCode':scope,'title':request['title'],'leftId':main,'rightId':next_id,'failedId':failed,
           'start':'2026-11-01','end':'2026-11-30','inputs':{'base':'1000.00','days':'1','cycleDays':'3'},
           'expected':{'net':'333.33','prorated':'333.33','deduction':'0'}}
(args.output_dir/'hrm-calculation-api-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2)+'\n')
(args.output_dir/'hrm-calculation-ui-fixture.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2)+'\n')
print('All',len(checks),'live rule calculation checks passed')
