#!/usr/bin/env python3
"""Exercise trial HTTP APIs in the expressly allowed isolated synthetic QA database."""
import argparse, concurrent.futures, copy, hashlib, json, os, subprocess, urllib.error, urllib.parse, urllib.request, uuid
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--base-url',default='http://127.0.0.1:48081');p.add_argument('--allow-test-fixtures',action='store_true',required=True);p.add_argument('--output-dir',type=Path,required=True);p.add_argument('--database',default='hrm_payroll_collection_20261004b');p.add_argument('--mysql-container',default='codex-ruoyi-mysql');a=p.parse_args()
if not a.database.startswith('hrm_payroll_collection_'):p.error('Use the isolated collection QA database')
a.output_dir.mkdir(parents=True,exist_ok=True);checks=[]
roles={'admin':(1,'HRM_SMOKE_TOKEN'),'none':(1,'HRM_UNGRANTED_TOKEN'),'tenantb':(999,'HRM_TENANT_B_TOKEN')}
roles.update({r:(1,'HRM_TRIAL_'+r.upper()+'_TOKEN') for r in ['reader','editor','executor','maintainonly','executeonly','nohrm','nocalc','dept','self','noelig']})
for _,key in roles.values():
 if not os.environ.get(key):p.error('Missing credential: '+key)
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}));root=a.base_url.rstrip('/')+'/admin-api/hrm/payroll/';trial='trial-batches'
def call(path,method='GET',data=None,params=None,role='admin',module=trial):
 tenant,key=roles.get(role,(1,None));headers={'tenant-id':str(tenant),'Content-Type':'application/json'}
 if key:headers['Authorization']='Bearer '+os.environ[key]
 request=urllib.request.Request(root+module+path+('?' + urllib.parse.urlencode(params) if params else ''),method=method,headers=headers,data=None if data is None else json.dumps(data,ensure_ascii=False).encode())
 try:r=opener.open(request,timeout=40)
 except urllib.error.HTTPError as e:r=e
 return json.load(r)
def ok(path,**kw):
 r=call(path,**kw);assert r.get('code')==0,(path,r.get('code'));return r['data']
def passed(name,condition):
 assert condition,name;checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def sql(statement):
 r=subprocess.run(['docker','exec','-i',a.mysql_container,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 -N -uroot "$1"','sh',a.database],input=statement,text=True,capture_output=True,check=True);return r.stdout.strip()
protected=['hrm_salary_month_record','hrm_salary_month_employee_record','hrm_salary_slip','hrm_salary_slip_send_record','hrm_insurance_month_record','hrm_insurance_month_employee_record','hrm_employee_salary_card']
def legacy_hash():
 result=sql('CHECKSUM TABLE '+','.join(protected)+' EXTENDED;');rows=[line.split('\t') for line in result.splitlines()]
 assert len(rows)==len(protected) and all(len(row)==2 and row[1].isdigit() for row in rows),'Missing legacy checksum evidence'
 return hashlib.sha256(result.encode()).hexdigest()
before=legacy_hash();suffix=uuid.uuid4().hex[:8].upper();entity='TRIAL-QA-'+suffix;person=983100101;other=983100102;nullperson=983100103
sql("UPDATE hrm_employee SET status=20,dept_id=983100011,user_id=983000009 WHERE id=983100101; UPDATE hrm_employee SET status=30,dept_id=983100022,user_id=983000020 WHERE id=983100102;")
program=ok('/wage-template',module='calculation-definitions',role='reader');passed('editable template has explicit money inputs and two cases',len(program['inputs'])==9 and len(program['cases'])==2)
definition=ok('/create',module='calculation-definitions',method='POST',data={'code':'TRIAL-RULE-'+suffix,'title':'试算合成常规工资规则','scopeCode':entity,'applicableScope':'隔离 QA 声明主体','ownerName':'合成规则负责人','reference':'技术回归合成口径，非正式工资政策','effectiveFrom':'2026-10-01','effectiveTo':'2026-10-31','program':program})
ok('/review',module='calculation-definitions',method='POST',data={'id':definition,'revision':1,'action':'confirm','evidence':'合成规则样例核对'})
def qualify(employee,decision):
 id=ok('/create',module='employee-eligibilities',method='POST',data={'entityCode':entity,'entityName':'试算合成声明主体','employeeId':employee,'qualification':decision,'ownerName':'合成资格负责人','reference':'QA 合成依据','reason':'技术验证明确声明资格','effectiveFrom':'2026-10-01','effectiveTo':'2026-10-31'})
 ok('/review',module='employee-eligibilities',method='POST',data={'id':id,'revision':1,'action':'confirm','evidence':'合成资格核对'});return id
qualification=qualify(person,'INCLUDED')
def inputrow(employee,inputs=None):return {'employeeId':employee,'inputs':copy.deepcopy(program['cases'][0]['inputs'] if inputs is None else inputs),'inputReference':'合成工资、个人社保公积金和已核定个税依据'}
def draft(code,people=None):return {'code':code+'-'+suffix,'title':'试算合成工资批次 '+code,'entityCode':entity,'entityName':'试算合成声明主体','periodType':'MONTHLY','periodStart':'2026-10-01','periodEnd':'2026-10-31','definitionId':definition,'ownerName':'合成工资负责人','reference':'隔离 QA 工资口径，仅技术验证','configuration':{'roles':{'gross':'gross','deductions':'deductions','tax':'tax','net':'net'},'people':people or [inputrow(person)]}}
def get(id,role='admin'):return ok('/get',params={'id':id},role=role)
def update(id,**overrides):return ok('/update',method='PUT',data={**get(id),**overrides},role='editor')
def command(id,key=None):
 c=ok('/check',params={'id':id});assert c['ready'],c.get('issues');return {'batchId':id,'revision':c['revision'],'sourceHash':c['sourceHash'],'requestKey':key or 'qa-'+uuid.uuid4().hex}
def execute(id,role='executor',cmd=None):return ok('/execute',method='POST',data=cmd or command(id),role=role)
def issue(id,code):
 c=ok('/check',params={'id':id});return not c['ready'] and code in [i['code'] for i in c['issues']]+[i['code'] for row in c['people'] for i in row['issues']]
request=draft('MAIN')
passed('anonymous and ungranted denied',call('/page',params={'pageNo':1,'pageSize':10},role=None)['code']==401 and call('/page',params={'pageNo':1,'pageSize':10},role='none')['code']==403)
for role in ['maintainonly','executeonly','nohrm','nocalc','noelig']:
 passed(role+' lacks required combined permissions',call('/page',params={'pageNo':1,'pageSize':10},role=role)['code']==403 and call('/create',method='POST',data=request,role=role)['code']==403)
id=ok('/create',method='POST',data={**request,'id':9999,'revision':99,'status':1},role='editor');row=get(id)
passed('server owns identity revision and snapshots',id!=9999 and row['status']==0 and row['revision']==1 and row['configuration']['people'][0]['snapshotName']=='试算合成人员 A' and row['configuration']['people'][0]['employeeFingerprint'])
passed('HTTP dates are ISO strings',row['periodStart']=='2026-10-01' and isinstance(row['configuration']['people'][0]['snapshotCapturedAt'],str))
passed('duplicate batch code rejected',call('/create',method='POST',data=request,role='editor')['code']==1050990003)
passed('reader cannot edit and executor cannot edit',all(call('/update',method='PUT',data=row,role=r)['code']==403 for r in ['reader','executor']))
cmd=command(id);passed('preflight stable and exact included count',cmd['sourceHash']==ok('/check',params={'id':id},role='reader')['sourceHash'])
passed('editor cannot execute',call('/execute',method='POST',data=cmd,role='editor')['code']==403)
first=execute(id,cmd=cmd);passed('executor stores balanced exact amounts',first['result']['totals']=={'gross':'7800.00','deductions':'1250.00','tax':'350.00','net':'6200.00'} and first['runVersion']==1)
saved=copy.deepcopy(first['result']);passed('full personal expression and source trace retained',len(saved['people'][0]['calculation']['items'])==4 and saved['people'][0]['calculation']['items'][3]['steps'] and saved['people'][0]['eligibility']['id']==qualification)
passed('retry returns same version even after revision advances',execute(id,cmd=cmd)['id']==first['id'] and len(ok('/runs',params={'id':id}))==1)
passed('request key conflict rejected',call('/execute',method='POST',data=command(id,cmd['requestKey']),role='executor')['code']==1050990005)
row=get(id);row['configuration']['people'][0]['inputs']['baseSalary']='6100.00';ok('/update',method='PUT',data=row,role='editor');changed=get(id)
passed('edit clears current result while retaining latest historical run',changed['status']==0 and not changed.get('currentRunId') and changed['latestRunId']==first['id'])
second=execute(id);diff=ok('/compare',params={'leftId':first['id'],'rightId':second['id']})
passed('immutable first result and exact version delta',ok('/run',params={'id':first['id']})['result']==saved and diff['totalDifferences']['net']=='100.00' and diff['people'][0]['change']=='AMOUNTS_CHANGED')
passed('lightweight page and run list omit sensitive payloads',not ok('/page',params={'pageNo':1,'pageSize':100,'entityCode':entity})['list'][0].get('configuration') and not ok('/runs',params={'id':id})[0].get('result'))
for path,params in [('/get',{'id':id}),('/check',{'id':id}),('/runs',{'id':id}),('/history',{'id':id}),('/run',{'id':first['id']}),('/compare',{'leftId':first['id'],'rightId':second['id']})]:passed('cross tenant denied '+path,call(path,params=params,role='tenantb')['code']==1050990000)
passed('cross tenant page does not reveal batch',ok('/page',params={'pageNo':1,'pageSize':100,'entityCode':entity},role='tenantb')['total']==0)
for value in [123.45,None,True]:
 req=draft('WIRE-'+str(value).upper());req['configuration']['people'][0]['inputs']['withheldTax']=value;passed('wire value rejected '+str(value),call('/create',method='POST',data=req,role='editor')['code']!=0)
missing=draft('MISSING');del missing['configuration']['people'][0]['inputs']['withheldTax'];missingid=ok('/create',method='POST',data=missing,role='editor');passed('missing tax is blocked instead of zero',issue(missingid,'INPUT_OR_AMOUNT_INVALID'))
row=get(missingid);row['configuration']['people'][0]['inputs']['withheldTax']='0.00';ok('/update',method='PUT',data=row,role='editor');passed('explicit zero is valid',execute(missingid)['result']['totals']['net']=='6550.00')
negative=draft('NEGATIVE');negative['configuration']['people'][0]['inputs']['withheldTax']='-0.01';negativeid=ok('/create',method='POST',data=negative,role='editor');passed('negative regular wage input blocked',issue(negativeid,'INPUT_OR_AMOUNT_INVALID'))
metaid=ok('/create',method='POST',data={**draft('META'),'ownerName':None,'reference':None},role='editor');passed('batch owner and evidence block execution when missing',issue(metaid,'BATCH_BASIS_MISSING'))
custom=ok('/create',method='POST',data={**draft('CUSTOM'),'periodType':'CUSTOM','periodStart':'2026-10-02'},role='editor');passed('custom explicit period supported',ok('/check',params={'id':custom})['ready'])
passed('partial natural month rejected',call('/create',method='POST',data={**draft('PARTIAL'),'periodStart':'2026-10-02'},role='editor')['code']==1050990001)
binding=draft('BINDING');binding['configuration']['roles']['net']='tax';bindid=ok('/create',method='POST',data=binding,role='editor');passed('binding collision blocks preflight',issue(bindid,'OUTPUT_BINDING_INVALID'))
unknownid=ok('/create',method='POST',data=draft('UNKNOWN',[inputrow(person),inputrow(other,{})]),role='editor');passed('unknown qualification blocks whole batch',issue(unknownid,'UNRESOLVED_QUALIFICATION'))
excludedqualification=qualify(other,'EXCLUDED');exclude=execute(unknownid);passed('excluded person has no computed zero and no contribution',exclude['excludedCount']==1 and exclude['result']['people'][1]['amounts']=={} and not exclude['result']['people'][1].get('calculation') and exclude['result']['totals']['net']=='6200.00')
allExcluded=ok('/create',method='POST',data=draft('ALL-EXCLUDED',[inputrow(other,{})]),role='editor');passed('all excluded blocks empty run',issue(allExcluded,'NO_INCLUDED_PEOPLE'))
for role in ['dept','self']:
 passed(role+' may read own complete batch',ok('/get',params={'id':id},role=role)['id']==id)
 passed(role+' cannot read batch with unrelated excluded person',call('/get',params={'id':unknownid},role=role)['code']==1050990000)
row=get(unknownid);row['configuration']['people']=[row['configuration']['people'][0]];ok('/update',method='PUT',data=row,role='editor')
passed('removing unrelated person does not expose old salary history',all(call('/get',params={'id':unknownid},role=r)['code']==1050990000 for r in ['dept','self']))
nullid=ok('/create',method='POST',data=draft('NULL-SCOPE',[inputrow(nullperson,{})]),role='editor');passed('null dept and user cannot leak through SQL scope',all(call('/get',params={'id':nullid},role=r)['code']==1050990000 for r in ['dept','self']) and all(nullid not in [b['id'] for b in ok('/page',params={'pageNo':1,'pageSize':100,'entityCode':entity},role=r)['list']] for r in ['dept','self']))
concurrent_id=ok('/create',method='POST',data=draft('CONCURRENT'),role='editor');concurrentcmd=command(concurrent_id)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:replies=list(pool.map(lambda _:execute(concurrent_id,cmd=concurrentcmd),range(2)))
passed('simultaneous retry creates one version',replies[0]['id']==replies[1]['id'] and len(ok('/runs',params={'id':concurrent_id}))==1)
stale=ok('/create',method='POST',data=draft('STALE'),role='editor');stalecmd=command(stale);row=get(stale);row['title']='合成修改批次';ok('/update',method='PUT',data=row,role='editor');passed('stale revision blocks execution',call('/execute',method='POST',data=stalecmd,role='executor')['code']==1050990002)
hashid=ok('/create',method='POST',data=draft('HASH'),role='editor');hashcmd=command(hashid)
sql("UPDATE hrm_payroll_employee_eligibility SET reference='合成来源变化' WHERE id="+str(qualification))
passed('source hash change with same batch revision blocks commit',call('/execute',method='POST',data=hashcmd,role='executor')['code']==1050990002)
sql("UPDATE hrm_employee SET status=30 WHERE id=983100101")
passed('current person drift blocks new trial',issue(id,'PERSON_CHANGED'))
passed('old immutable result replays after source drift',ok('/run',params={'id':first['id']})['result']==saved and execute(id,cmd=cmd)['id']==first['id'])
sql("UPDATE hrm_employee SET status=20 WHERE id=983100101")
passed('legacy protected tables are unchanged',before==legacy_hash())
fixture={'entityCode':entity,'batchId':id,'firstRunId':first['id'],'secondRunId':second['id'],'definitionId':definition,'definitionCode':'TRIAL-RULE-'+suffix,'employeeId':person,'otherEmployeeId':other,'excludedBatchId':unknownid,'unknownBatchId':nullid,'synthetic':True}
(a.output_dir/'hrm-trial-browser-fixture.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2)+'\n')
(a.output_dir/'hrm-trial-api-results.json').write_text(json.dumps({'total':len(checks),'passed':len(checks),'checks':checks,'syntheticFixtures':True,'legacyProtectedTables':protected,'legacyUnchanged':before==legacy_hash()},ensure_ascii=False,indent=2)+'\n')
print('PASS:',len(checks),'actual HTTP checks')
