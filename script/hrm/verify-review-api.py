#!/usr/bin/env python3
"""Run version-bound review against real Yudao BPM in an expressly allowed isolated QA database."""
import argparse, concurrent.futures, copy, hashlib, json, os, subprocess, urllib.error, urllib.parse, urllib.request, uuid
from pathlib import Path
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--base-url', default='http://127.0.0.1:48081')
p.add_argument('--allow-test-fixtures', action='store_true', required=True)
p.add_argument('--output-dir', type=Path, required=True)
p.add_argument('--database', default='hrm_payroll_collection_20261004b')
p.add_argument('--mysql-container', default='codex-ruoyi-mysql')
a = p.parse_args()
if not a.database.startswith('hrm_payroll_collection_') or urllib.parse.urlparse(a.base_url).hostname not in ['127.0.0.1', 'localhost']:
    p.error('Use the isolated collection QA database and a loopback verification service')
a.output_dir.mkdir(parents=True, exist_ok=True)
roles = {'admin': (1, 'HRM_SMOKE_TOKEN'), 'none': (1, 'HRM_UNGRANTED_TOKEN'), 'tenantb': (999, 'HRM_TENANT_B_TOKEN')}
for role in ['maker', 'hr', 'finance', 'reader', 'submitonly', 'freezeonly', 'wronghr', 'self', 'dept', 'noelig', 'manager']:
    roles[role] = (1, 'HRM_REVIEW_' + role.upper() + '_TOKEN')
for _, key in roles.values():
    if not os.environ.get(key): p.error('Missing credential: ' + key)
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
root = a.base_url.rstrip('/') + '/admin-api/'
checks = []
review = 'hrm/payroll/trial-batches/review'
trial = 'hrm/payroll/trial-batches'
ids = {'maker':984000001, 'hr':984000002, 'finance':984000003, 'reader':984000004, 'wronghr':984000007, 'self':984000008, 'dept':984000009, 'noelig':984000010, 'manager':984000011, 'disabled':984000012}
def call(path, method='GET', data=None, params=None, role='admin', module=review):
    tenant, key = roles.get(role, (1, None))
    headers = {'tenant-id': str(tenant), 'Content-Type': 'application/json'}
    if key: headers['Authorization'] = 'Bearer ' + os.environ[key]
    req = urllib.request.Request(root + module + path + ('?' + urllib.parse.urlencode(params) if params else ''), method=method, headers=headers, data=None if data is None else json.dumps(data, ensure_ascii=False).encode())
    try: response = opener.open(req, timeout=60)
    except urllib.error.HTTPError as e: response = e
    return json.load(response)
def ok(path, **kw):
    r = call(path, **kw); assert r.get('code') == 0, (path, r.get('code'), r.get('msg')); return r['data']
def passed(name, condition=True):
    assert condition, name; checks.append({'check':name, 'result':'PASS'}); print('PASS:', name, flush=True)
def sql(statement):
    r = subprocess.run(['docker', 'exec', '-i', a.mysql_container, 'sh', '-c', 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 -N -uroot "$1"', 'sh', a.database], input=statement, text=True, capture_output=True, check=True)
    return r.stdout.strip()
protected = ['hrm_salary_month_record', 'hrm_salary_month_employee_record', 'hrm_salary_slip', 'hrm_salary_slip_send_record', 'hrm_insurance_month_record', 'hrm_insurance_month_employee_record', 'hrm_employee_salary_card']
def legacy_hash():
    result = sql('CHECKSUM TABLE ' + ','.join(protected) + ' EXTENDED;'); rows = [line.split('\t') for line in result.splitlines()]
    assert len(rows) == len(protected) and all(len(row)==2 and row[1].isdigit() for row in rows)
    return hashlib.sha256(result.encode()).hexdigest()
before = legacy_hash()
# Existing model APIs deploy the bundled template; production deployment remains an explicit administrator operation.
key = 'hrm_payroll_trial_review'
models = ok('/list', module='bpm/model')
existing = next((m for m in models if m['key'] == key), None)
xml = (Path(__file__).resolve().parents[2] / 'yudao-module-hrm/src/main/resources/bpmn/payroll-review.bpmn20.xml').read_text()
model = {'key':key, 'name':'薪酬试算两级复核', 'category':'', 'type':10, 'formType':20, 'formCustomCreatePath':'/hrm/payroll-trial-batches', 'formCustomViewPath':'/hrm/payroll-trial-batches', 'visible':False, 'managerUserIds':[1], 'allowCancelRunningProcess':True, 'allowWithdrawTask':False, 'autoApprovalType':0, 'titleSetting':{'enable':False}, 'summarySetting':{'enable':False}, 'bpmnXml':xml}
if existing:
    model['id'] = existing['id']; ok('/update', module='bpm/model', method='PUT', data=model); modelid=existing['id']
else: modelid=ok('/create', module='bpm/model', method='POST', data=model)
ok('/deploy', module='bpm/model', method='POST', params={'id':modelid})
passed('bundled two-stage definition deployed through existing BPM model API')
suffix = uuid.uuid4().hex[:8].upper(); entity = 'REVIEW-QA-' + suffix
person, other = 984100101, 984100102
sql('UPDATE hrm_employee SET status=20,dept_id=984100011,user_id=984000008 WHERE id=984100101; UPDATE hrm_employee SET status=30,dept_id=984100022,user_id=984000020 WHERE id=984100102;')
program = ok('/wage-template', module='hrm/payroll/calculation-definitions')
definition = ok('/create', module='hrm/payroll/calculation-definitions', method='POST', data={'code':'REVIEW-RULE-'+suffix,'title':'复核合成工资规则','scopeCode':entity,'applicableScope':'隔离 QA 合成主体','ownerName':'QA','reference':'合成工资口径，非正式工资政策','effectiveFrom':'2026-10-01','effectiveTo':'2026-10-31','program':program})
ok('/review', module='hrm/payroll/calculation-definitions', method='POST', data={'id':definition,'revision':1,'action':'confirm','evidence':'合成规则核对'})
def qualify(employee, decision):
    id = ok('/create', module='hrm/payroll/employee-eligibilities', method='POST', data={'entityCode':entity,'entityName':'复核合成主体','employeeId':employee,'qualification':decision,'ownerName':'QA','reference':'合成资格依据','reason':'技术验证声明资格','effectiveFrom':'2026-10-01','effectiveTo':'2026-10-31'})
    ok('/review', module='hrm/payroll/employee-eligibilities', method='POST', data={'id':id,'revision':1,'action':'confirm','evidence':'合成资格核对'}); return id
qualification = qualify(person, 'INCLUDED'); qualify(other, 'EXCLUDED')
def create(name, mixed=False):
    people = [{'employeeId':person,'inputs':copy.deepcopy(program['cases'][0]['inputs']),'inputReference':'合成工资与核定个税依据'}]
    if mixed: people.append({'employeeId':other,'inputs':{},'inputReference':'明确排除计薪'})
    id = ok('/create', module=trial, role='maker', method='POST', data={'code':'REVIEW-'+name+'-'+suffix,'title':'复核合成批次 '+name,'entityCode':entity,'entityName':'复核合成主体','periodType':'MONTHLY','periodStart':'2026-10-01','periodEnd':'2026-10-31','definitionId':definition,'ownerName':'合成核算负责人','reference':'合成复核技术验证，非实际工资','configuration':{'roles':{'gross':'gross','deductions':'deductions','tax':'tax','net':'net'},'people':people}})
    execute(id); return id

def batch(id): return ok('/get', module=trial, params={'id':id}, role='maker')
def view(id, role='reader'): return ok('/get', params={'batchId':id}, role=role)
def execute(id):
    c=ok('/check',module=trial,params={'id':id},role='maker');assert c['ready'], c['issues']
    return ok('/execute',module=trial,role='maker',method='POST',data={'batchId':id,'revision':c['revision'],'sourceHash':c['sourceHash'],'requestKey':'trial-'+uuid.uuid4().hex})
def command(id, action, evidence=None):
    b=batch(id);v=view(id);return {'batchId':id,'runId':b.get('currentRunId') or b['latestRunId'],'revision':v['revision'],'cycleId':None if action=='submit' else v.get('activeReviewId'),'requestKey':'review-'+uuid.uuid4().hex,'action':action,'evidence':evidence or '合成'+action+'依据'}
def send(cmd, role): return ok('/action', method='POST', data=cmd, role=role)
def submit(id, hr='hr', finance='finance'):
    return send({**command(id,'submit'),'hrReviewerId':ids[hr],'financeReviewerId':ids[finance]},'maker')
def decision(id, role, action='approve'):
    v=view(id);return send({**command(id,action),'taskId':v['tasks'][0]['id']},role)
def approve(id): submit(id); decision(id,'hr'); return decision(id,'finance')
def state_count(id):return sql('SELECT revision,status,COALESCE(current_run_id,0) FROM hrm_payroll_trial_batch WHERE id='+str(id)+'; SELECT COUNT(*) FROM hrm_payroll_review WHERE object_type=\'trial-batch\' AND object_id='+str(id)+';')
main=create('MAIN'); original=ok('/run',module=trial,params={'id':batch(main)['currentRunId']})
count=state_count(main); first=view(main);view(main);passed('review GET is read-only',first['cycles']==[] and state_count(main)==count)
for role in [None,'none','submitonly','freezeonly','noelig']:
    passed(str(role)+' missing combined permissions is denied',call('/get',params={'batchId':main},role=role)['code'] in [401,403] and call('/action',method='POST',data=command(main,'submit'),role=role)['code'] in [401,403])
for hr, finance in [('hr','hr'),('maker','finance'),('hr','maker'),('hr','disabled')]:
    passed('invalid reviewer combination '+hr+'/'+finance+' rejected',call('/action',method='POST',role='maker',data={**command(main,'submit'),'hrReviewerId':ids[hr],'financeReviewerId':ids[finance]})['code']==1050991000)
for hr in ['reader','noelig','dept']:
    passed('reviewer permissions or full scope required '+hr,call('/action',method='POST',role='maker',data={**command(main,'submit'),'hrReviewerId':ids[hr],'financeReviewerId':ids['finance']})['code']==1050991001)
passed('missing evidence cannot start BPM',call('/action',method='POST',role='maker',data={**command(main,'submit'),'hrReviewerId':ids['hr'],'financeReviewerId':ids['finance'],'evidence':' '})['code']!=0)
start={**command(main,'submit'),'hrReviewerId':ids['hr'],'financeReviewerId':ids['finance']}; submitted=send(start,'maker')
cycle=submitted['cycle']; pid=cycle['processInstanceId']; runid=cycle['runId']
passed('submission binds real BPM deployment run and fingerprint',submitted['batchStatus']==2 and cycle['processDefinitionId'].startswith(key+':') and cycle['sourceHash'] and cycle['runId']==original['id'] and submitted['tasks'][0]['key']=='hrReview' and submitted['tasks'][0]['assigneeId']==ids['hr'])
passed('retry submission returns original receipt with ISO timestamps',send(start,'maker')==submitted and len(view(main)['cycles'])==1 and isinstance(cycle['startedAt'],str) and not cycle['startedAt'].startswith('1970-'))
passed('changed request body cannot reuse key',call('/action',method='POST',role='maker',data={**start,'evidence':'改变依据'})['code']==1050991004)
for path,role,data,method,module in [('/approve','hr',{'id':submitted['tasks'][0]['id'],'reason':'通用审批绕过尝试','variables':{}},'PUT','bpm/task'),('/reject','hr',{'id':submitted['tasks'][0]['id'],'reason':'通用驳回绕过尝试'},'PUT','bpm/task'),('/transfer','hr',{'id':submitted['tasks'][0]['id'],'assigneeUserId':ids['finance'],'reason':'通用转办绕过尝试'},'PUT','bpm/task'),('/cancel-by-start-user','maker',{'id':pid,'reason':'通用撤销绕过尝试'},'DELETE','bpm/process-instance'),('/cancel-by-admin','admin',{'id':pid,'reason':'管理员通用撤销绕过尝试'},'DELETE','bpm/process-instance'),('/create','admin',{'processDefinitionId':cycle['processDefinitionId'],'variables':{},'startUserSelectAssignees':{'hrReview':[ids['hr']],'financeReview':[ids['finance']]}},'POST','bpm/process-instance')]:
    passed('generic BPM cannot bypass payroll guard '+path,call(path,module=module,method=method,role=role,data=data)['code']==1050991001)
passed('nonassigned HR and premature finance cannot approve',all(call('/action',method='POST',data={**command(main,'approve'),'taskId':submitted['tasks'][0]['id']},role=r)['code']==1050991001 for r in ['wronghr','finance']))
passed('falsified task ID cannot approve',call('/action',method='POST',data={**command(main,'approve'),'taskId':'fake-task'},role='hr')['code']==1050991002)
passed('freeze before two approvals is rejected',call('/action',method='POST',data=command(main,'freeze'),role='finance')['code']==1050991000)
edit=batch(main)
passed('reviewed batch cannot be edited',call('/update',module=trial,method='PUT',data=edit,role='maker')['code']==1050990001)
c=ok('/check',module=trial,params={'id':main});passed('reviewed batch cannot be recalculated',call('/execute',module=trial,method='POST',role='maker',data={'batchId':main,'revision':c['revision'],'sourceHash':c['sourceHash'],'requestKey':'blocked-'+uuid.uuid4().hex})['code']==1050990001)
hrcmd={**command(main,'approve'),'taskId':view(main)['tasks'][0]['id']};hr=send(hrcmd,'hr')
passed('HR approval advances to assigned finance task',hr['batchStatus']==2 and hr['cycle']['hrEvidence'] and hr['tasks'][0]['key']=='financeReview' and hr['tasks'][0]['assigneeId']==ids['finance'])
passed('approval retry returns original task receipt without repeating engine operation',send(hrcmd,'hr')==hr)
finance=decision(main,'finance');passed('native finance completion synchronizes approved business status',finance['batchStatus']==3 and finance['cycle']['status']==1 and finance['cycle']['outcome']=='APPROVED' and finance['tasks']==[])
passed('real BPM completion works without a mandatory phone',sql("SELECT COUNT(*) FROM system_users WHERE id=984000001 AND (mobile IS NULL OR mobile='')")=='1')
passed('actual native BPM historic status is approved',sql("SELECT LONG_ FROM ACT_HI_VARINST WHERE PROC_INST_ID_='"+pid+"' AND NAME_='PROCESS_STATUS'")=='2')
passed('BPM holds control metadata without payroll amounts or protected review evidence',not sql("SELECT NAME_ FROM ACT_HI_VARINST WHERE PROC_INST_ID_='"+pid+"' AND (NAME_ IN ('baseSalary','net','gross','evidence') OR TEXT_ LIKE '%合成approve依据%')"))
passed('core readonly role cannot freeze',call('/action',method='POST',data=command(main,'freeze'),role='reader')['code']==1050991001)
frozen=send(command(main,'freeze'),'finance');passed('freeze binds approved immutable run and evidence',frozen['batchStatus']==4 and frozen['frozenRunId']==runid and frozen['cycle']['freezeEvidence'] and frozen['cycle']['frozenBy']==ids['finance'])
passed('freeze changes neither exact amounts nor calculation trace',ok('/run',module=trial,params={'id':runid})['result']==original['result'])
passed('audit snapshot records actual final approved cycle',json.loads(next(r['afterSnapshot'] for r in ok('/history',module=trial,params={'id':main}) if r['action']=='approve' and r['actorId']==ids['finance']))['status']==1)
unfrozen=send(command(main,'unfreeze'),'finance');passed('unfreeze preserves frozen evidence and requires a new review round',unfrozen['batchStatus']==1 and not unfrozen.get('activeReviewId') and not unfrozen.get('frozenRunId') and unfrozen['cycles'][0]['status']==5 and unfrozen['cycles'][0]['freezeEvidence'] and unfrozen['cycles'][0]['unfreezeEvidence'])
second=submit(main);passed('new review round cannot reuse previous approval',second['cycle']['cycleVersion']==2 and not second['cycle'].get('hrEvidence') and not second['cycle'].get('financeEvidence'))
send(command(main,'cancel'),'maker');passed('starter cancellation invalidates current run and preserves history',batch(main)['status']==0 and not batch(main).get('currentRunId') and batch(main)['latestRunId']==runid and len(view(main)['cycles'])==2)
passed('cancelled round needs a new trial before resubmission',call('/action',method='POST',role='maker',data={**command(main,'submit'),'hrReviewerId':ids['hr'],'financeReviewerId':ids['finance']})['code']==1050991002)
# Native rejection preserves the original run and invalidates the current pointer.
rejected=create('REJECTED');submit(rejected);rejection=decision(rejected,'hr','reject')
passed('HR rejection closes real BPM and invalidates current trial',rejection['batchStatus']==0 and rejection['cycle']['outcome']=='REJECTED' and not batch(rejected).get('currentRunId') and sql("SELECT LONG_ FROM ACT_HI_VARINST WHERE PROC_INST_ID_='"+rejection['cycle']['processInstanceId']+"' AND NAME_='PROCESS_STATUS'")=='3')
execute(rejected);renewed=submit(rejected);passed('retrial after rejection binds a new run and new cycle',renewed['cycle']['cycleVersion']==2 and renewed['cycle']['runId']!=rejection['cycle']['runId'] and len(ok('/runs',module=trial,params={'id':rejected}))==2)
send(command(rejected,'admin-cancel'),'manager');passed('independent management cancellation is auditable and closes BPM',view(rejected)['cycle']['outcome']=='CANCELLED' and any(r['action']=='admin-cancel' and r['actorId']==ids['manager'] and r['reason']=='合成admin-cancel依据' for r in ok('/history',module=trial,params={'id':rejected})))
# Source drift is never silently accepted by an approval or freeze.
drift=create('DRIFT');submit(drift)
sql('UPDATE hrm_employee SET status=30 WHERE id=984100101;')
passed('person drift blocks approval without recording a passed stage',call('/action',method='POST',role='hr',data={**command(drift,'approve'),'taskId':view(drift)['tasks'][0]['id']})['code']==1050991003 and not view(drift)['cycle'].get('hrEvidence'))
send(command(drift,'cancel'),'maker');sql('UPDATE hrm_employee SET status=20 WHERE id=984100101;')
freeze_drift=create('FREEZE-DRIFT');approve(freeze_drift);sql('UPDATE hrm_employee SET status=30 WHERE id=984100101;')
passed('person drift after approval blocks freeze while retaining approvals',call('/action',method='POST',role='finance',data=command(freeze_drift,'freeze'))['code']==1050991003 and view(freeze_drift)['batchStatus']==3 and view(freeze_drift)['cycle']['financeEvidence'])
sql('UPDATE hrm_employee SET status=20 WHERE id=984100101;')
# Full historical footprint protection also applies to review records, including excluded people.
mixed=create('MIXED',True);submit(mixed)
for role in ['self','dept']:
    passed(role+' cannot read a mixed-scope review or synchronize it',call('/get',params={'batchId':mixed},role=role)['code']==1050990000 and call('/sync',method='POST',params={'batchId':mixed},role=role)['code']==1050990000)
selfbatch=create('SELF');sv=submit(selfbatch,'self');sr=decision(selfbatch,'self')
passed('reviewer self-scope uses selected reviewer identity rather than starter',sv['tasks'][0]['assigneeId']==ids['self'] and sr['cycle']['hrEvidence'])
send(command(selfbatch,'cancel'),'maker')
passed('cross-tenant review read action and sync denied',call('/get',params={'batchId':main},role='tenantb')['code']==1050990000 and call('/action',method='POST',data=command(main,'submit'),role='tenantb')['code']==1050990000 and call('/sync',method='POST',params={'batchId':main},role='tenantb')['code']==1050990000)
# Two simultaneous lost-response retries create one process and one command receipt.
concurrent_id=create('CONCURRENT');concurrentcmd={**command(concurrent_id,'submit'),'hrReviewerId':ids['hr'],'financeReviewerId':ids['finance']}
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool: replies=list(pool.map(lambda _:send(concurrentcmd,'maker'),range(2)))
passed('simultaneous submit retry creates one cycle and one native BPM instance',replies[0]==replies[1] and len(view(concurrent_id)['cycles'])==1 and sql('SELECT COUNT(*) FROM hrm_payroll_review_command WHERE batch_id='+str(concurrent_id))=='1')
send(command(concurrent_id,'cancel'),'maker')
# A late/repeated event cannot advance an old cycle; explicit sync never trusts client event fields.
rev=view(concurrent_id)['revision'];synced=ok('/sync',method='POST',params={'batchId':concurrent_id},role='reader')
passed('explicit reconciliation is idempotent on completed history',synced['revision']==rev and synced['batchStatus']==0)
# Separate batches remain ready for real browser submission, approval and cancellation.
ui=create('BROWSER'); cancel_ui=create('BROWSER-CANCEL');readonly_ui=create('BROWSER-READONLY')
fixture={'synthetic':True,'entityCode':entity,'batchId':ui,'batchCode':batch(ui)['code'],'cancelBatchId':cancel_ui,'cancelBatchCode':batch(cancel_ui)['code'],'readonlyBatchId':readonly_ui,'readonlyBatchCode':batch(readonly_ui)['code'],'definitionId':definition,'employeeId':person,'hrReviewerId':ids['hr'],'financeReviewerId':ids['finance'],'makerId':ids['maker'],'closedBatchId':main}
passed('legacy wage tax insurance slip and bank tables unchanged',before==legacy_hash())
(a.output_dir/'hrm-review-browser-fixture.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2)+'\n')
(a.output_dir/'hrm-review-api-results.json').write_text(json.dumps({'total':len(checks),'passed':len(checks),'checks':checks,'syntheticFixtures':True,'realBpmEngine':True,'legacyProtectedTables':protected,'legacyUnchanged':before==legacy_hash()},ensure_ascii=False,indent=2)+'\n')
print('PASS:',len(checks),'actual HTTP/BPM checks')
