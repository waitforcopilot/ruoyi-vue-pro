#!/usr/bin/env python3
"""Live synthetic identity/version/ACL/CSV regression. Requires an isolated QA database."""
import argparse, concurrent.futures, copy, json, os, subprocess, time, urllib.error, urllib.parse, urllib.request, uuid
from pathlib import Path

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url',default='http://127.0.0.1:48081')
parser.add_argument('--allow-test-fixtures',action='store_true',required=True)
parser.add_argument('--output-dir',type=Path,required=True)
parser.add_argument('--mysql-container',default='codex-ruoyi-mysql')
parser.add_argument('--database',default='hrm_payroll_collection_20261004b')
args=parser.parse_args();args.output_dir.mkdir(parents=True,exist_ok=True)
if not args.database.startswith('hrm_payroll_collection_'):parser.error('Only an isolated payroll collection QA database is supported')
roles={'admin':(1,'HRM_SMOKE_TOKEN'),'reader':(1,'HRM_MAPPING_READER_TOKEN'),'editor':(1,'HRM_MAPPING_EDITOR_TOKEN'),
       'reviewer':(1,'HRM_MAPPING_REVIEWER_TOKEN'),'dept':(1,'HRM_MAPPING_DEPT_TOKEN'),'self':(1,'HRM_MAPPING_SELF_TOKEN'),
       'noemployee':(1,'HRM_MAPPING_NOEMPLOYEE_TOKEN'),'none':(1,'HRM_UNGRANTED_TOKEN'),'tenantb':(999,'HRM_TENANT_B_TOKEN')}
for _,env in roles.values():
 if not os.environ.get(env):parser.error('Missing verification credential: '+env)
base=args.base_url.rstrip('/')+'/admin-api/hrm/payroll';checks=[]
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def call(path,method='GET',data=None,params=None,role='admin',upload=None,raw=False):
 tenant,env=roles.get(role,(1,None));headers={'tenant-id':str(tenant)};payload=None
 if env:headers['Authorization']='Bearer '+os.environ[env]
 if upload is not None:
  boundary='mappingverify'+uuid.uuid4().hex;parts=[]
  for k,v in data.items():parts.append(('--'+boundary+'\r\nContent-Disposition: form-data; name="'+k+'"\r\n\r\n'+str(v)+'\r\n').encode())
  parts.append(('--'+boundary+'\r\nContent-Disposition: form-data; name="file"; filename="mapping-qa.csv"\r\nContent-Type: text/csv\r\n\r\n').encode())
  parts.extend([upload,('\r\n--'+boundary+'--\r\n').encode()]);payload=b''.join(parts);headers['Content-Type']='multipart/form-data; boundary='+boundary
 elif data is not None:payload=json.dumps(data,ensure_ascii=False).encode();headers['Content-Type']='application/json'
 req=urllib.request.Request(base+path+('?' + urllib.parse.urlencode(params) if params else ''),method=method,headers=headers,data=payload)
 try:r=opener.open(req,timeout=30)
 except urllib.error.HTTPError as e:r=e
 return r.read() if raw else json.load(r)
def success(path,**kw):
 r=call(path,**kw);assert r.get('code')==0,(path,r.get('code'));return r.get('data')
def passed(name,condition):
 if not condition:raise AssertionError(name)
 checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def sql(statement):
 r=subprocess.run(['docker','exec','-i',args.mysql_container,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 -N -uroot "$1"','sh',args.database],input=statement,text=True,capture_output=True)
 assert r.returncode==0,'Isolated fixture SQL failed';return r.stdout
def get(id,role='admin'):return success('/identity/get',params={'id':id},role=role)
def page(code=None,role='admin'):
 params={'pageNo':1,'pageSize':100,'namespace':'QA-MAPPING'}
 if code is not None:params['externalCode']=code
 return success('/identity/page',params=params,role=role)
def review(id,action='confirm',role='reviewer',revision=None):
 row=get(id);return call('/identity/review',method='POST',role=role,data={'id':id,'revision':row['revision'] if revision is None else revision,'action':action,'evidence':'隔离 QA 合成评审依据，仅测试技术行为','reviewedByName':'FORGED'})
def version(id,employee=None):
 params={'id':id,'revision':get(id)['revision']}
 if employee:params['employeeId']=employee
 return success('/identity/new-version',method='POST',params=params,role='editor')
def update(id,**changes):
 data={**get(id),**changes};data.pop('employeeFingerprint',None)
 return success('/identity/update',method='PUT',data=data,role='editor')
def resolve(code,start='2026-10-01',end='2026-10-31',role='admin',namespace='QA-MAPPING'):
 return success('/identity/resolve',method='POST',params={'sourceId':source,'namespace':namespace},data={'externalCode':code,'start':start,'end':end},role=role)
def preview(contract,file,role='admin'):
 return call('/intake/batches/preview',method='POST',role=role,upload=file,data={'contractId':contract,'declaredScope':'隔离 QA 演示范围','periodStart':'2026-10-01','periodEnd':'2026-10-31'})
def batch(id):return success('/intake/batches/get',params={'id':id})
def parallel(function):
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:return list(pool.map(function,range(2)))
def codes(result):return {i['code'] for row in result['rows'] for i in row['issues']}|{i['code'] for i in result['globalIssues']}

passed('anonymous identity APIs denied',call('/identity/sources',role=None)['code']==401)
passed('ungranted identity denied',call('/identity/sources',role='none')['code']==403)
passed('mapping query alone cannot read people or mappings',call('/identity/sources',role='noemployee')['code']==403 and call('/identity/page',params={'pageNo':1,'pageSize':10},role='noemployee')['code']==403)
sources=success('/identity/sources',role='reader');source=sources[0]['id']
passed('tenant sources return only narrow metadata',len(sources)>=12 and all(not s.get('actualSystem') and not s.get('ownerName') for s in sources))
person=success('/identity/employee',params={'id':930000001},role='editor')
passed('exact person lookup returns ISO snapshot and fingerprint without unrelated PII',person['employeeId']==930000001 and person['snapshotName']=='编号映射合成员工 A' and len(person['snapshotCapturedAt'])==19 and person['snapshotEntryTime']=='2026-01-01T00:00:00' and len(person['employeeFingerprint'])==64 and not any(k in person for k in ['phone','idNumber','bankAccount','salary']))
passed('foreign tenant person cannot be selected',call('/identity/employee',params={'id':930000004})['code']==1050940000)
passed('department scope cannot select another department person',call('/identity/employee',params={'id':930000002},role='dept')['code']==1050940000)
stamp=str(int(time.time()))+'-'+uuid.uuid4().hex[:5]
request={'sourceId':source,'namespace':'QA-MAPPING','externalCode':'MAP-'+stamp+'-001','employeeId':930000001,
 'employeeFingerprint':person['employeeFingerprint'],'ownerName':'QA 合成数据负责人','reference':'隔离 QA 来源登记，不代表真实业务核定','effectiveFrom':'2026-10-01','effectiveTo':'2026-10-31'}
def create(code=None,employee=930000001,**changes):
 data={**request,'externalCode':code or 'MAP-'+stamp+'-'+uuid.uuid4().hex[:6],'employeeId':employee,**changes};data.pop('employeeFingerprint',None)
 return success('/identity/create',method='POST',data=data,role='editor')
passed('reader cannot maintain mappings',call('/identity/create',method='POST',data=request,role='reader')['code']==403)
main=success('/identity/create',method='POST',data={**request,'id':987654321,'status':1,'mappingVersion':99,'tenantId':999,'snapshotName':'FORGED','reviewedByName':'FORGED'},role='editor');row=get(main)
passed('server allocates draft ID version and personal snapshot',main!=987654321 and row['status']==0 and row['mappingVersion']==1 and row['revision']==1 and row['snapshotName']==person['snapshotName'])
passed('same identity requires a new version rather than duplicate registration',call('/identity/create',method='POST',data=request,role='editor')['code']==1050940004)
duplicate={**request,'externalCode':'DUP-'+stamp}
results=parallel(lambda _:call('/identity/create',method='POST',data=duplicate,role='editor'))
passed('concurrent first registrations have exactly one winner',sorted(r['code'] for r in results)==[0,1050940004])
stale=get(main);success('/identity/update',method='PUT',data={**stale,'reference':'已重新核对合成人员'},role='editor')
passed('stale draft edits rejected',call('/identity/update',method='PUT',data=stale,role='editor')['code']==1050940002)
row=get(main);results=parallel(lambda n:call('/identity/update',method='PUT',data={**row,'reference':'并发合成依据 '+str(n)},role='editor'))
passed('concurrent same-revision edits have one winner',sorted(r['code'] for r in results)==[0,1050940002])
passed('source namespace and external identity cannot change on edit',call('/identity/update',method='PUT',data={**get(main),'externalCode':'CHANGED'},role='editor')['code']==1050940001)
passed('review and maintenance permissions are separated',review(main,role='editor')['code']==403 and call('/identity/update',method='PUT',data=get(main),role='reviewer')['code']==403)
incomplete=create(ownerName='',reference='',effectiveFrom=None,effectiveTo=None)
passed('confirmation requires responsible person reference and effective dates',review(incomplete)['code']==1050940001)
passed('confirmation requires review evidence',call('/identity/review',method='POST',role='reviewer',data={'id':main,'revision':get(main)['revision'],'action':'confirm','evidence':''})['code']!=0)
review_revision=get(main)['revision'];results=parallel(lambda _:review(main,revision=review_revision))
passed('concurrent same-revision reviews have one winner',sorted(r['code'] for r in results)==[0,1050940002])
row=get(main)
passed('confirmed mapping stamps real actor and freezes edits',row['status']==1 and row['reviewedByName']=='映射回归 reviewer' and row.get('reviewedTime') and call('/identity/update',method='PUT',data=row,role='editor')['code']==1050940003)
passed('leading-zero external code resolves an exact confirmed version despite duplicate HRM job numbers',resolve(request['externalCode'])['employeeId']==930000001)
history=success('/identity/history',params={'id':main})
passed('audit preserves server actor and before-after snapshots',any(h['action']=='confirm' and h['actorId']==940000003 and h.get('beforeSnapshot') and h.get('afterSnapshot') for h in history))
passed('department reader audit redacts reasons and all personal snapshots',all(not h.get('beforeSnapshot') and not h.get('afterSnapshot') and not h.get('reason') for h in success('/identity/history',params={'id':main},role='dept')))
passed('self scope uses bound HRM person rather than job number',page(request['externalCode'],role='self')['total']==1 and call('/identity/employee',params={'id':930000002},role='self')['code']==1050940000)
for suffix in ['Aa','aa','001','1']:
 id=create('CASE-'+stamp+'-'+suffix);assert review(id)['code']==0
passed('external codes preserve case and leading zeros in MySQL lookup',all(page('CASE-'+stamp+'-'+suffix)['total']==1 for suffix in ['Aa','aa','001','1']))
other_namespace=create(request['externalCode'],namespace='QA-OTHER');assert review(other_namespace)['code']==0
passed('same external code can belong to a distinct namespace',resolve(request['externalCode'],namespace='QA-OTHER')['mappingId']==other_namespace)
versions=parallel(lambda _:version(main));numbers=sorted(get(id)['mappingVersion'] for id in versions)
passed('concurrent new versions are sequential and leave review conclusion empty',numbers==[2,3] and all(get(id)['status']==0 and not get(id).get('reviewedBy') for id in versions))
passed('inclusive effective boundary overlap blocks confirmation',review(versions[0])['code']==1050940005)
schedule_code='SCHEDULE-'+stamp;early=create(schedule_code,effectiveTo='2026-10-15');assert review(early)['code']==0
late=version(early,930000002);update(late,effectiveFrom='2026-10-16',effectiveTo='2026-10-31');assert review(late)['code']==0
passed('adjacent periods resolve their own versions but do not stitch a whole interval',resolve(schedule_code,'2026-10-15','2026-10-15')['employeeId']==930000001 and resolve(schedule_code,'2026-10-16','2026-10-16')['employeeId']==930000002 and resolve(schedule_code).get('issueCode')=='UNRESOLVED_MAPPING')
open_id=create(effectiveTo=None);assert review(open_id)['code']==0
passed('open-ended periods are explicit and resolve future dates',resolve(get(open_id)['externalCode'],'2027-01-01','2027-01-31')['mappingId']==open_id)
race=create();race_other=version(race)
results=parallel(lambda n:review([race,race_other][n]))
passed('concurrent overlapping confirmations have one winner',sorted(r['code'] for r in results)==[0,1050940005])
department_b=create('DEPT-B-'+stamp,employee=930000002);assert review(department_b)['code']==0
passed('department pagination totals and resolution exclude unauthorized people',page('DEPT-B-'+stamp,role='dept')['total']==0 and resolve('DEPT-B-'+stamp,role='dept').get('issueCode')=='UNRESOLVED_MAPPING' and call('/identity/get',params={'id':department_b},role='dept')['code']==1050940000)
passed('tenant B cannot read update review or version tenant A identities',all(r['code']==1050940000 for r in [call('/identity/get',params={'id':main},role='tenantb'),call('/identity/update',method='PUT',data=get(main),role='tenantb'),call('/identity/review',method='POST',data={'id':main,'revision':get(main)['revision'],'action':'retire','evidence':'隔离测试'},role='tenantb'),call('/identity/new-version',method='POST',params={'id':main,'revision':get(main)['revision']},role='tenantb')]) and page(request['externalCode'],role='tenantb')['total']==0)

salary_tables=['hrm_salary_tax_rule','hrm_salary_employee_info','hrm_salary_change_record','hrm_salary_month_record','hrm_salary_month_employee_record','hrm_salary_slip_record']
existing=set(sql("SHOW TABLES LIKE 'hrm_salary_%';").splitlines());salary_tables=[t for t in salary_tables if t in existing]
payroll_before=sql('CHECKSUM TABLE '+','.join(salary_tables)+';')
try:
 sql("UPDATE hrm_employee SET name='主档变化合成员工',dept_id=940000001,user_id=NULL,deleted=0 WHERE id=930000003 AND creator='identity-verification'; UPDATE hrm_employee SET deleted=0 WHERE id=930000005 AND creator='identity-verification';")
 drift=create(employee=930000003);before=get(drift)
 sql("UPDATE hrm_employee SET name='已更新主档合成员工' WHERE id=930000003 AND creator='identity-verification';")
 passed('master-record drift blocks confirmation',review(drift)['code']==1050940006)
 passed('stale employee picker fingerprint blocks saving a changed main record',call('/identity/update',method='PUT',data=before,role='editor')['code']==1050940006)
 update(drift);assert review(drift)['code']==0;frozen=get(drift)
 sql("UPDATE hrm_employee SET name='主档再次变化合成员工',dept_id=940000002 WHERE id=930000003 AND creator='identity-verification';")
 passed('confirmed name department and capture time stay frozen after main record changes',all(get(drift)[k]==frozen[k] for k in ['snapshotName','snapshotDeptId','snapshotCapturedAt','employeeFingerprint']))
 passed('both captured and current department must be visible',page(get(drift)['externalCode'],role='dept')['total']==0 and call('/identity/get',params={'id':drift},role='dept')['code']==1050940000)
 deleted=create(employee=930000005);assert review(deleted)['code']==0
 sql("UPDATE hrm_employee SET deleted=1 WHERE id=930000005 AND creator='identity-verification';")
 passed('deleted main record cannot resolve but all-scope history remains readable',resolve(get(deleted)['externalCode']).get('issueCode')=='UNRESOLVED_MAPPING' and get(deleted)['status']==1 and bool(success('/identity/history',params={'id':deleted})))
 passed('deleted main record also disappears from scoped count',page(get(deleted)['externalCode'],role='dept')['total']==0)
 assert review(deleted,'retire')['code']==0
finally:
 sql("UPDATE hrm_employee SET name='主档变化合成员工',dept_id=940000001,user_id=NULL,deleted=0 WHERE id=930000003 AND creator='identity-verification'; UPDATE hrm_employee SET deleted=0 WHERE id=930000005 AND creator='identity-verification';")

contract_request={'sourceId':source,'title':'编号映射预检合成契约 '+stamp,'actualSystem':'隔离 QA CSV','ownerName':'QA 合成负责人','applicableScope':'隔离 QA 演示范围',
 'schema':{'fields':[{'key':'external','label':'外部人员编号','type':'TEXT','required':True,'maxLength':128},{'key':'date','label':'业务日期','type':'DATE','required':True},{'key':'note','label':'样例说明','type':'TEXT','required':False,'maxLength':120}],
 'keyFields':['external','date'],'externalEmployeeField':'external','employeeNamespace':'QA-MAPPING','periodField':'date'}}
def contract(data):
 id=success('/intake/contracts/create',method='POST',role='editor',data=data);row=success('/intake/contracts/get',params={'id':id})
 r=call('/intake/contracts/review',method='POST',role='reviewer',data={'id':id,'revision':row['revision'],'action':'confirm','evidence':'隔离 QA 字段契约核对'});assert r['code']==0,r['code'];return id
invalid=copy.deepcopy(contract_request);invalid['schema']['employeeField']='external'
passed('contract cannot combine external mapping and direct HRM job matching',call('/intake/contracts/create',method='POST',data=invalid)['code']==1050920001)
invalid=copy.deepcopy(contract_request);invalid['schema']['employeeNamespace']=''
passed('external matching requires explicit namespace',call('/intake/contracts/create',method='POST',data=invalid)['code']==1050920001)
contract_id=contract(contract_request);template=call('/intake/contracts/template',params={'id':contract_id},raw=True)
content=template+(request['externalCode']+',2026-10-02,合成已知编号\nMISSING-'+stamp+',2026-10-02,合成未知编号\n').encode()
first=preview(contract_id,content);assert first['code']==0,first['code'];first=first['data'];result=batch(first)['result']
passed('CSV uses mapping identity even when HRM job numbers are duplicated',result['employeeMatchMode']=='EXTERNAL_MAPPING' and result['rows'][0]['employeeId']==930000001 and result['rows'][0]['employeeMapping']['mappingId']==main and result['rows'][0]['employeeMapping']['mappingVersion']==1)
passed('CSV unknown mappings return row issues without HRM fallback',result['validCount']==1 and result['errorCount']==1 and 'UNRESOLVED_MAPPING' in codes(result))
passed('same file and visible mapping outcomes reuse the same immutable batch',preview(contract_id,content)['data']==first)
assert review(main,'retire')['code']==0
retired_batch=preview(contract_id,content)['data'];passed('retirement produces a new batch with unresolved identity',retired_batch!=first and batch(retired_batch)['result']['validCount']==0)
rebound=version(main,930000002);assert review(rebound)['code']==0
new_batch=preview(contract_id,content)['data'];new_result=batch(new_batch)['result']
passed('publishing a replacement mapping produces a new batch with its version reference',new_batch not in [first,retired_batch] and new_result['rows'][0]['employeeMapping']['mappingId']==rebound and new_result['rows'][0]['employeeId']==930000002)
passed('old CSV result retains its original frozen person and mapping version',batch(first)['result']==result)
passed('unknown mapped row contains no target employee or mapping reference',batch(preview(contract_id,content)['data'])['result']['rows'][1].get('employeeId') is None and batch(preview(contract_id,content)['data'])['result']['rows'][1].get('employeeMapping') is None)
scoped_id=preview(contract_id,content,role='dept')['data'];scoped=success('/intake/batches/get',params={'id':scoped_id},role='dept')['result']
passed('mapped CSV applies department scope before choosing a target',scoped['validCount']==0 and scoped['rows'][0].get('employeeMapping') is None and 'UNRESOLVED_MAPPING' in codes(scoped))
whole=copy.deepcopy(contract_request);whole['schema'].pop('periodField');whole_id=contract(whole);whole_template=call('/intake/contracts/template',params={'id':whole_id},raw=True)
whole_result=batch(preview(whole_id,whole_template+(schedule_code+',2026-10-02,不可跨版本拼接\n').encode())['data'])['result']
passed('CSV without date matching requires one version for the entire declared interval','UNRESOLVED_MAPPING' in codes(whole_result))
dated_result=batch(preview(contract_id,template+(schedule_code+',2026-10-15,前半期间\n'+schedule_code+',2026-10-16,后半期间\n').encode())['data'])['result']
passed('CSV date matching retains the different period versions per row',dated_result['validCount']==2 and [r['employeeMapping']['mappingId'] for r in dated_result['rows']]==[early,late])
passed('retired mapping history is retained and never physically deleted',get(main)['status']==2 and any(h['action']=='retire' for h in success('/identity/history',params={'id':main})))
passed('identity and preview operations leave existing payroll data unchanged',payroll_before==sql('CHECKSUM TABLE '+','.join(salary_tables)+';'))
fixture={'sourceId':source,'namespace':'QA-MAPPING','externalCode':request['externalCode'],'mappingId':rebound,'mappingVersion':get(rebound)['mappingVersion'],'employeeA':930000001,'employeeB':930000002,'contractId':contract_id,'contractTitle':contract_request['title'],'batchId':new_batch,'frozenBatchId':first,'scheduleCode':schedule_code,'earlyId':early,'lateId':late,'periodStart':'2026-10-01','periodEnd':'2026-10-31'}
(args.output_dir/'hrm-identity-api-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2))
(args.output_dir/'hrm-identity-ui-fixture.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2))
(args.output_dir/'mapping-preview.csv').write_bytes(content)
print('All',len(checks),'live identity API checks passed')
