#!/usr/bin/env python3
"""Synthetic scheme regression with live MySQL source changes. Isolated QA only."""
import argparse,concurrent.futures,json,os,subprocess,time,urllib.error,urllib.parse,urllib.request,uuid
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url',default='http://127.0.0.1:48081')
parser.add_argument('--allow-test-fixtures',action='store_true',required=True)
parser.add_argument('--output-dir',type=Path,required=True)
parser.add_argument('--mysql-container',default='codex-ruoyi-mysql')
parser.add_argument('--database',default='hrm_payroll_collection_20261004b')
args=parser.parse_args();args.output_dir.mkdir(parents=True,exist_ok=True)
if not args.database.startswith('hrm_payroll_collection_'):parser.error('Use an isolated payroll collection QA database')
roles={'admin':(1,'HRM_SMOKE_TOKEN'),'reader':(1,'HRM_SCHEME_READER_TOKEN'),'editor':(1,'HRM_SCHEME_EDITOR_TOKEN'),'reviewer':(1,'HRM_SCHEME_REVIEWER_TOKEN'),
       'schemeonly':(1,'HRM_SCHEME_SCHEMEONLY_TOKEN'),'partial':(1,'HRM_SCHEME_PARTIAL_TOKEN'),'none':(1,'HRM_UNGRANTED_TOKEN'),'tenantb':(999,'HRM_TENANT_B_TOKEN')}
for _,env in roles.values():
 if not os.environ.get(env):parser.error('Missing verification credential: '+env)
base=args.base_url.rstrip('/')+'/admin-api/hrm/payroll/schemes';checks=[];opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def call(path,method='GET',data=None,params=None,role='admin'):
 tenant,env=roles.get(role,(1,None));headers={'tenant-id':str(tenant),'Content-Type':'application/json'}
 if env:headers['Authorization']='Bearer '+os.environ[env]
 req=urllib.request.Request(base+path+('?' + urllib.parse.urlencode(params) if params else ''),method=method,headers=headers,data=None if data is None else json.dumps(data,ensure_ascii=False).encode())
 try:r=opener.open(req,timeout=30)
 except urllib.error.HTTPError as e:r=e
 return json.load(r)
def success(path,**kw):
 r=call(path,**kw);assert r.get('code')==0,(path,r.get('code'));return r.get('data')
def passed(name,condition):
 if not condition:raise AssertionError(name)
 checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def sql(statement):
 r=subprocess.run(['docker','exec','-i',args.mysql_container,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 -N -uroot "$1"','sh',args.database],input=statement,text=True,capture_output=True)
 assert r.returncode==0,'Isolated source fixture SQL failed';return r.stdout
def get(id,role='admin'):return success('/get',params={'id':id},role=role)
def review(id,action='confirm',revision=None,role='reviewer'):
 row=get(id);return call('/review',method='POST',role=role,data={'id':id,'revision':row['revision'] if revision is None else revision,'action':action,'evidence':'隔离 QA 合成配置评审，不代表业务制度','reviewedByName':'FORGED'})
def version(id):return success('/new-version',method='POST',role='editor',params={'id':id,'revision':get(id)['revision']})
def update(id,**changes):return success('/update',method='PUT',role='editor',data={**get(id),**changes})
def parallel(function):
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as p:return list(p.map(function,range(2)))
def page(role='admin'):return success('/page',params={'pageNo':1,'pageSize':100,'groupId':group},role=role)
def resolve(start,end,role='admin'):return call('/resolve',params={'groupId':group,'start':start,'end':end},role=role)
passed('anonymous scheme access denied',call('/groups',role=None)['code']==401)
passed('ungranted scheme access denied',call('/groups',role='none')['code']==403)
passed('scheme query alone cannot read underlying configuration',call('/groups',role='schemeonly')['code']==1050950007)
passed('partial legacy queries cannot read tax settings through schemes',call('/groups',role='partial')['code']==1050950007)
stamp=str(int(time.time()))+'-'+uuid.uuid4().hex[:5];group_name='合成方案配置回归 '+stamp
group=int(sql("INSERT INTO hrm_salary_group(name,salary_standard,change_rule,tax_rule_id,tenant_id,creator) VALUES('"+group_name+"',10.00,'合成变更规则（非业务制度）',950000001,1,'scheme-verification'); SELECT LAST_INSERT_ID();").strip())
groups=success('/groups',params={'search':stamp},role='reader')
passed('source selector returns filtered tenant-only metadata without personnel lists',len(groups)==1 and groups[0]['id']==group and not groups[0].get('employeeIds') and not groups[0].get('deptIds'))
captured=success('/capture',params={'groupId':group});snapshot=captured['snapshot']
passed('capture preserves explicit source values with no financial defaults',snapshot['group']['salaryStandard']=='10.00' and snapshot['taxRule']['taxEnabled'] is False and snapshot['taxRule'].get('threshold') is None and len(snapshot['options'])==2 and not snapshot['issues'])
passed('capture timestamps are nonempty ISO and fingerprint is stable',len(captured['capturedAt'])==19 and captured['sourceHash']==success('/capture',params={'groupId':group})['sourceHash'])
passed('capture excludes employee memberships amounts and other tenant settings',not any(word in json.dumps(snapshot,ensure_ascii=False) for word in ['employeeIds','deptIds','probationSalary','另一租户目录']))
passed('foreign tenant source groups cannot be captured',call('/capture',params={'groupId':950000003})['code']==1050950001)
request={'groupId':group,'title':'合成薪酬方案 '+stamp,'ownerName':'QA 合成配置负责人','reference':'隔离 QA 技术核对依据','effectiveFrom':'2026-10-01','effectiveTo':'2026-10-31','expectedSourceHash':captured['sourceHash']}
passed('read-only role cannot register a scheme',call('/create',method='POST',data=request,role='reader')['code']==403)
main=success('/create',method='POST',role='editor',data={**request,'id':987654321,'schemeVersion':98,'status':1,'tenantId':999,'snapshot':{'FORGED':True},'reviewedByName':'FORGED'})
row=get(main)
passed('server owns version status tenant and captured configuration',row['schemeVersion']==1 and row['status']==0 and row['revision']==1 and main!=987654321 and row['sourceHash']==captured['sourceHash'] and row['snapshot']==snapshot)
passed('initial version cannot be registered twice',call('/create',method='POST',data=request,role='editor')['code']==1050950004)
other_group=int(sql("INSERT INTO hrm_salary_group(name,salary_standard,change_rule,tax_rule_id,tenant_id,creator) VALUES('并发合成组 "+stamp+"',10.00,'合成规则',950000001,1,'scheme-verification'); SELECT LAST_INSERT_ID();").strip())
results=parallel(lambda _:call('/create',method='POST',role='editor',data={**request,'groupId':other_group,'expectedSourceHash':None}))
passed('concurrent initial registrations have one winner',sorted(r['code'] for r in results)==[0,1050950004])
row=get(main);results=parallel(lambda n:call('/update',method='PUT',role='editor',data={**row,'reference':'并发核对 '+str(n)}))
passed('concurrent same-revision edits have one winner',sorted(r['code'] for r in results)==[0,1050950002])
passed('review and maintenance permissions are separate',review(main,role='editor')['code']==403 and call('/update',method='PUT',role='reviewer',data=get(main))['code']==403)
passed('source group cannot change on edit',call('/update',method='PUT',role='editor',data={**get(main),'groupId':other_group})['code']==1050950001)
update(main,ownerName=None)
passed('confirmation requires responsible person',review(main)['code']==1050950001)
update(main,ownerName=request['ownerName']);revision=get(main)['revision'];results=parallel(lambda _:review(main,revision=revision))
passed('concurrent same-revision reviews have one winner',sorted(r['code'] for r in results)==[0,1050950002])
row=get(main);passed('confirmation stamps real actor and ISO review time',row['reviewedByName']=='方案回归 reviewer' and len(row['reviewedTime'])==19)
passed('confirmed scheme cannot be edited',call('/update',method='PUT',role='editor',data=row)['code']==1050950003)
passed('list omits large snapshots and freeform evidence',all(not r.get('snapshot') and not r.get('reference') and not r.get('evidence') for r in page()['list']))
history=success('/history',params={'id':main})
passed('audit retains exact actor and configuration snapshots',any(h['action']=='confirm' and h['actorId']==960000003 and h.get('beforeSnapshot') and h.get('afterSnapshot') for h in history))
passed('saved history also requires all underlying configuration queries',call('/get',params={'id':main},role='schemeonly')['code']==1050950007 and call('/history',params={'id':main},role='partial')['code']==1050950007)
passed('tenant B cannot read edit review version or compare tenant A IDs',all(r['code']==1050950000 for r in [call('/get',params={'id':main},role='tenantb'),call('/update',method='PUT',data=get(main),role='tenantb'),call('/review',method='POST',data={'id':main,'revision':get(main)['revision'],'action':'retire','evidence':'合成测试'},role='tenantb'),call('/new-version',method='POST',params={'id':main,'revision':get(main)['revision']},role='tenantb'),call('/compare',params={'leftId':main,'rightId':main},role='tenantb')]) and page(role='tenantb')['total']==0)
next_id=version(main);initial_next=get(next_id)
passed('new version is draft without inherited review conclusion',initial_next['schemeVersion']==2 and initial_next['status']==0 and not initial_next.get('reviewedByName'))
passed('inclusive overlapping effective interval is rejected',review(next_id)['code']==1050950005)
payroll_tables=['hrm_salary_employee_info','hrm_salary_change_record','hrm_salary_month_record','hrm_salary_month_employee_record','hrm_salary_slip_send_record']
existing=set(sql("SHOW TABLES LIKE 'hrm_salary_%';").splitlines());payroll_tables=[t for t in payroll_tables if t in existing];payroll_before=sql('CHECKSUM TABLE '+','.join(payroll_tables)+';')
try:
 sql("UPDATE hrm_salary_group SET salary_standard=12.00 WHERE id="+str(group)+" AND creator='scheme-verification';")
 passed('group source changes block confirming a stale snapshot',review(next_id)['code']==1050950006)
 passed('stale preview hash blocks replacing a draft source snapshot',call('/update',method='PUT',role='editor',data={**get(next_id),'expectedSourceHash':initial_next['sourceHash']})['code']==1050950006)
 update(next_id);sql("UPDATE hrm_salary_tax_rule SET threshold=0.00,decimal_scale=0 WHERE id=950000001 AND creator='scheme-verification';")
 passed('tax source changes block confirmation until explicitly recaptured',review(next_id)['code']==1050950006)
 update(next_id);sql("UPDATE hrm_salary_option SET enabled=0 WHERE id=950000002 AND creator='scheme-verification';")
 passed('shared catalogue changes block confirmation until explicitly recaptured',review(next_id)['code']==1050950006)
 update(next_id,effectiveFrom='2026-11-01',effectiveTo='2026-11-30');assert review(next_id)['code']==0
 passed('confirmed previous version preserves all original configuration',get(main)['snapshot']==snapshot)
 comparison=success('/compare',params={'leftId':main,'rightId':next_id});changes=comparison['changes']
 passed('comparison exposes actual standard zero threshold and false flag changes',any(c['path']=='group.salaryStandard' and c['right']=='12.00' for c in changes) and any(c['path']=='taxRule.threshold' and c['right']=='0.00' for c in changes) and any(c['path']=='options.1000000000.enabled' and c['right']=='false' for c in changes))
 passed('comparison labels are readable and do not contain employee inputs',all(c['label']!=c['path'] for c in changes) and 'employeeIds' not in json.dumps(comparison))
 passed('equal version comparison has no artificial capture-time differences',not success('/compare',params={'leftId':main,'rightId':main})['changes'])
 passed('single period chooses its own retained configuration version',resolve('2026-10-01','2026-10-31')['data']['id']==main and resolve('2026-11-01','2026-11-30')['data']['id']==next_id)
 passed('period resolution does not stitch adjacent versions',resolve('2026-10-31','2026-11-01')['code']==1050950001)
finally:
 sql("UPDATE hrm_salary_group SET salary_standard=10.00 WHERE id="+str(group)+" AND creator='scheme-verification'; UPDATE hrm_salary_tax_rule SET threshold=NULL,decimal_scale=NULL WHERE id=950000001 AND creator='scheme-verification'; UPDATE hrm_salary_option SET enabled=1 WHERE id=950000002 AND creator='scheme-verification';")
versions=parallel(lambda _:version(next_id));numbers=sorted(get(id)['schemeVersion'] for id in versions)
passed('concurrent version allocation is sequential and never changes older snapshots',numbers==[3,4] and get(next_id)['snapshot']['group']['salaryStandard']=='12.00')
for id in versions:update(id,effectiveFrom='2026-12-01',effectiveTo='2026-12-31')
results=parallel(lambda n:review(versions[n]))
passed('concurrent overlapping confirmations have one winner',sorted(r['code'] for r in results)==[0,1050950005])
other_id=success('/page',params={'pageNo':1,'pageSize':10,'groupId':other_group})['list'][0]['id']
passed('cross-group comparison is rejected',call('/compare',params={'leftId':main,'rightId':other_id})['code']==1050950001)
assert review(main,'retire')['code']==0
passed('retired version remains readable but no longer resolves a period',get(main)['status']==2 and get(main)['snapshot']==snapshot and resolve('2026-10-01','2026-10-31')['code']==1050950001)
passed('existing employee payroll and payment table checksums are unchanged',payroll_before==sql('CHECKSUM TABLE '+','.join(payroll_tables)+';'))
fixture={'groupId':group,'groupName':group_name,'title':request['title'],'leftId':main,'leftVersion':1,'rightId':next_id,'rightVersion':2,'periodStart':'2026-11-01','periodEnd':'2026-11-30'}
(args.output_dir/'hrm-scheme-api-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2));(args.output_dir/'hrm-scheme-ui-fixture.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2))
print('All',len(checks),'live scheme API checks passed')
