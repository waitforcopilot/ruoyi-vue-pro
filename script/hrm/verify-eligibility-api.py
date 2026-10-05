#!/usr/bin/env python3
"""Verify actual qualification APIs and permissions using explicitly authorized synthetic QA fixtures."""
import argparse, concurrent.futures, copy, json, os, subprocess, time, urllib.error, urllib.parse, urllib.request, uuid
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url',default='http://127.0.0.1:48081')
parser.add_argument('--allow-test-fixtures',action='store_true',required=True)
parser.add_argument('--output-dir',type=Path,required=True)
parser.add_argument('--database',default='hrm_payroll_collection_20261004b')
parser.add_argument('--mysql-container',default='codex-ruoyi-mysql')
args=parser.parse_args();args.output_dir.mkdir(parents=True,exist_ok=True)
if not args.database.startswith('hrm_payroll_collection_'):parser.error('Use the isolated collection QA database')
roles={'admin':(1,'HRM_SMOKE_TOKEN'),'none':(1,'HRM_UNGRANTED_TOKEN'),'tenantb':(999,'HRM_TENANT_B_TOKEN')}
roles.update({r:(1,'HRM_ELIG_'+r.upper()+'_TOKEN') for r in ['reader','editor','reviewer','maintainonly','reviewonly','nohrm','dept','self']})
for _,name in roles.values():
 if not os.environ.get(name):parser.error('Missing verification credential: '+name)
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}));base=args.base_url.rstrip('/')+'/admin-api/hrm/payroll/employee-eligibilities';checks=[]
def call(path,method='GET',data=None,params=None,role='admin'):
 tenant,name=roles.get(role,(1,None));headers={'tenant-id':str(tenant),'Content-Type':'application/json'}
 if name:headers['Authorization']='Bearer '+os.environ[name]
 req=urllib.request.Request(base+path+('?' + urllib.parse.urlencode(params) if params else ''),method=method,headers=headers,data=None if data is None else json.dumps(data,ensure_ascii=False).encode())
 try:response=opener.open(req,timeout=30)
 except urllib.error.HTTPError as error:response=error
 return json.load(response)
def ok(path,**kw):
 data=call(path,**kw);assert data.get('code')==0,(path,data.get('code'));return data['data']
def passed(name,value):
 assert value,name;checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def sql(statement):
 out=subprocess.run(['docker','exec','-i',args.mysql_container,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 -N -uroot "$1"','sh',args.database],input=statement,text=True,capture_output=True)
 assert out.returncode==0,'Isolated QA SQL failed';return out.stdout
person=981100101;other=981100102
sql("INSERT INTO hrm_employee(id,name,job_number,dept_id,user_id,status,tenant_id,creator) VALUES(981100101,'资格合成人员 A','ELIG-QA',981100011,982000008,20,1,'eligibility-verification'),(981100102,'资格合成人员 B','ELIG-QA',981100022,982000001,30,1,'eligibility-verification') ON DUPLICATE KEY UPDATE name=VALUES(name),job_number=VALUES(job_number),dept_id=VALUES(dept_id),user_id=VALUES(user_id),status=VALUES(status),deleted=0;")
protected=['hrm_salary_employee_info','hrm_salary_month_record','hrm_salary_month_employee_record','hrm_salary_slip_send_record','hrm_insurance_month_record','hrm_insurance_month_employee_record'];known=set(sql('SHOW TABLES;').splitlines());protected=[t for t in protected if t in known];assert protected;before=sql('CHECKSUM TABLE '+','.join(protected)+';')
stamp=str(int(time.time()))+'-'+uuid.uuid4().hex[:4].upper();entity='ELIG-QA-'+stamp
request={'entityCode':entity,'entityName':'合成资格核对主体 '+stamp,'employeeId':person,'qualification':'INCLUDED','ownerName':'QA 资格负责人','reference':'合成资格验收资料，非真实主体或工资资格结论','reason':'合成纳入资格样例','effectiveFrom':'2026-10-01','effectiveTo':'2026-10-31'}
def get(id,role='admin'):return ok('/get',params={'id':id},role=role)
def update(id,**changes):return call('/update',method='PUT',data={**get(id),**changes},role='editor')
def version(id):return ok('/new-version',method='POST',params={'id':id,'revision':get(id)['revision']},role='editor')
def review(id,action='confirm',role='reviewer',revision=None):return call('/review',method='POST',role=role,data={'id':id,'revision':get(id)['revision'] if revision is None else revision,'action':action,'evidence':'合成资料核对，非正式业务签认','reviewedByName':'FORGED'})
def lookup(start='2026-10-01',end='2026-10-31',code=None,employee=None,role='reader'):return call('/lookup',method='POST',role=role,data={'entityCode':code or entity,'employeeId':employee or person,'start':start,'end':end})
def parallel(fn):
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:return list(pool.map(fn,range(2)))
passed('anonymous and ungranted denied',call('/page',params={'pageNo':1,'pageSize':10},role=None)['code']==401 and call('/page',params={'pageNo':1,'pageSize':10},role='none')['code']==403)
for role in ['maintainonly','reviewonly','nohrm']:
 passed(role+' cannot read without combined query permissions',call('/employee',params={'id':person},role=role)['code']==403)
 passed(role+' cannot mutate without combined query permissions',call('/create',method='POST',data=request,role=role)['code']==403)
id=ok('/create',method='POST',role='editor',data={**request,'id':987,'revision':99,'snapshotName':'FORGED','reviewedByName':'FORGED','status':1})
row=get(id);passed('server owns version snapshot and reviewer',id!=987 and row['revision']==1 and row['eligibilityVersion']==1 and row['status']==0 and row['snapshotName']=='资格合成人员 A' and not row.get('reviewedByName'))
passed('duplicate entity person denied',call('/create',method='POST',data=request,role='editor')['code']==1050980004)
passed('reader cannot maintain',call('/update',method='PUT',data=row,role='reader')['code']==403)
passed('editor cannot review',review(id,role='editor')['code']==403)
passed('reviewer cannot maintain',call('/update',method='PUT',data=row,role='reviewer')['code']==403)
passed('same job number is not identity',ok('/employee',params={'id':other})['employeeId']!=row['employeeId'])
passed('HTTP ISO dates and whitelist',isinstance(row['snapshotCapturedAt'],str) and row['effectiveFrom']=='2026-10-01' and not {'mobile','idNumber','salary'}&set(row))
passed('draft does not match period',not lookup()['data']['matched'])
passed('confirmation success',review(id)['code']==0)
matched=lookup()['data'];passed('full period included matched',matched['matched'] and matched['eligibility']['id']==id and matched['eligibility']['qualification']=='INCLUDED')
passed('confirmed immutable',update(id,reason='cannot')['code']==1050980003)
passed('cross tenant IDs and histories denied',all(call(path,params={'id':id},role='tenantb')['code']==1050980000 for path in ['/get','/history']))
passed('cross tenant create cannot bind tenant A person',call('/create',method='POST',role='tenantb',data=request)['code']==1050980000)
passed('cross tenant personnel unavailable',call('/employee',params={'id':person},role='tenantb')['code']==1050980000)
passed('cross tenant period unresolved',not lookup(role='tenantb')['data']['matched'])
passed('adjacent date outside period unresolved',not lookup(end='2026-11-01')['data']['matched'])
next=version(id);passed('new version draft clears review',get(next)['eligibilityVersion']==2 and get(next)['status']==0 and not get(next).get('evidence'))
passed('inclusive overlap blocked',review(next)['code']==1050980005)
passed('draft update clears optional fields',update(next,qualification=None,ownerName=None,reference=None,reason=None,effectiveFrom=None,effectiveTo=None)['code']==0 and get(next).get('qualification') is None and get(next).get('effectiveTo') is None)
passed('incomplete confirmation blocked',review(next)['code']==1050980001)
passed('explicit finite exclusion saved',update(next,qualification='EXCLUDED',ownerName=request['ownerName'],reference=request['reference'],reason='合成排除资格样例',effectiveFrom='2026-11-01',effectiveTo='2026-11-30')['code']==0 and review(next)['code']==0)
excluded=lookup('2026-11-01','2026-11-30')['data'];passed('excluded distinct from unknown',excluded['matched'] and excluded['issueCode']=='EXCLUDED' and lookup(code='MISSING-QA')['data']['issueCode']=='UNRESOLVED_QUALIFICATION')
passed('period does not join versions',not lookup(end='2026-11-30')['data']['matched'])
third=version(id);old=get(third);passed('revision update success',update(third,reason='更新理由')['code']==0);passed('stale update blocked',call('/update',method='PUT',data=old,role='editor')['code']==1050980002)
passed('stable employee identity blocked',update(third,employeeId=other)['code']==1050980001)
passed('empty unbounded end cannot confirm',update(third,effectiveTo=None)['code']==0 and review(third)['code']==1050980001)
passed('dept and self readers see authorized snapshot',get(id,'dept')['id']==id and get(id,'self')['id']==id)
passed('scoped history redacts freeform and snapshots',all(not e.get('beforeSnapshot') and not e.get('afterSnapshot') and not e.get('reason') for e in ok('/history',params={'id':id},role='dept')))
drift_draft=version(id)
try:
 sql('UPDATE hrm_employee SET status=30 WHERE id=981100101;')
 drift=lookup()['data'];passed('source drift stops confirmed matching',not drift['matched'] and drift['personChanged'] and drift['issueCode']=='PERSON_CHANGED' and drift['eligibility']['snapshotEmployeeStatus']==20)
 passed('stale snapshot blocks draft save',update(third)['code']==1050980006)
 passed('source drift blocks otherwise complete draft confirmation',review(drift_draft)['code']==1050980006)
 passed('historical confirmed snapshot not rewritten',get(id)['snapshotEmployeeStatus']==20)
 sql('UPDATE hrm_employee SET dept_id=981100022,user_id=982000001 WHERE id=981100101;')
 passed('captured plus current department and self guard',all(call('/get',params={'id':id},role=r)['code']==1050980000 for r in ['dept','self']))
 passed('scoped page hides moved personnel',all(ok('/page',params={'pageNo':1,'pageSize':100,'entityCode':entity},role=r)['total']==0 for r in ['dept','self']))
finally:sql('UPDATE hrm_employee SET status=20,dept_id=981100011,user_id=982000008 WHERE id=981100101;')
# Independent same-period confirmation requests serialize at the series root.
versions=parallel(lambda _:version(id));passed('concurrent version numbers unique',len({get(v)['eligibilityVersion'] for v in versions})==2)
for v in versions:assert update(v,effectiveFrom='2026-12-01',effectiveTo='2026-12-31')['code']==0
reviews=parallel(lambda i:review(versions[i])['code']);passed('concurrent overlap admits one confirmation',sorted(reviews)==[0,1050980005])
passed('retirement excludes lookup and keeps history',review(next,'retire')['code']==0 and not lookup('2026-11-01','2026-11-30')['data']['matched'] and any(e['action']=='retire' for e in ok('/history',params={'id':next})))
passed('legacy salary and insurance tables unchanged',before==sql('CHECKSUM TABLE '+','.join(protected)+';'))
fixture={'entityCode':entity,'entityName':request['entityName'],'employeeId':person,'confirmedId':id,'draftId':third,'retiredId':next,'reason':request['reason'],'reference':request['reference']}
(args.output_dir/'hrm-eligibility-ui-fixture.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2)+'\n')
(args.output_dir/'hrm-eligibility-api-results.json').write_text(json.dumps({'checks':checks,'total':len(checks),'passed':len(checks),'syntheticFixtures':True,'legacyTablesUnchanged':protected},ensure_ascii=False,indent=2)+'\n')
print('PASS:',len(checks),'live eligibility checks; no formal eligibility policies or wage results created')
