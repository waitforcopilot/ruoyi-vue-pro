#!/usr/bin/env python3
"""Creates synthetic ledger fixtures; requires an isolated database and dedicated verification identities."""
import argparse, concurrent.futures, copy, json, os, time, urllib.error, urllib.parse, urllib.request
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url',default='http://127.0.0.1:48081')
parser.add_argument('--allow-test-fixtures',action='store_true',required=True)
parser.add_argument('--output-dir',type=Path,required=True)
args=parser.parse_args();args.output_dir.mkdir(parents=True,exist_ok=True)
app=args.base_url.rstrip('/')+'/admin-api';base=app+'/hrm/payroll/rules'
roles={'admin':(1,'HRM_SMOKE_TOKEN'),'reader':(1,'HRM_READER_TOKEN'),'editor':(1,'HRM_EDITOR_TOKEN'),
       'reviewer':(1,'HRM_REVIEWER_TOKEN'),'none':(1,'HRM_UNGRANTED_TOKEN'),'tenantb':(999,'HRM_TENANT_B_TOKEN')}
for _,env in roles.values():
 if not os.environ.get(env):parser.error('Missing verification credential: '+env)
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}));checks=[]
def call(path,method='GET',data=None,params=None,role='admin',common=False):
 tenant,env=roles.get(role,(1,None));headers={'tenant-id':str(tenant),'Content-Type':'application/json'}
 if env:headers['Authorization']='Bearer '+os.environ[env]
 url=(app if common else base)+path+('?' + urllib.parse.urlencode(params) if params else '')
 req=urllib.request.Request(url,method=method,headers=headers,data=json.dumps(data,ensure_ascii=False).encode() if data is not None else None)
 try:response=opener.open(req,timeout=30)
 except urllib.error.HTTPError as e:response=e
 return json.load(response)
def success(path,**kw):
 r=call(path,**kw);assert r.get('code')==0,(path,r.get('code'));return r.get('data')
def passed(name,condition):
 if not condition:raise AssertionError(name)
 checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def get(id,role='admin'):return success('/get',params={'id':id},role=role)
def review(id,action='confirm',role='reviewer',revision=None):
 return call('/review',method='POST',role=role,data={'id':id,'revision':revision or get(id)['revision'],'action':action,
   'evidence':'规则台账隔离回归：仅验证资料和版本，不代表真实制度或计算结论','reviewedByName':'FORGED'})
def version(id,role='editor'):
 return success('/new-version',method='POST',params={'id':id,'revision':get(id)['revision']},role=role)
passed('anonymous query denied',call('/page',role=None)['code']==401)
passed('ungranted identity denied',call('/page',role='none')['code']==403)
passed('reader cannot initialize',call('/initialize',method='POST',role='reader')['code']==403)
before_tax=success('/hrm/salary/tax-rule/list',common=True)
success('/initialize',method='POST',role='editor')
catalog=[r for r in success('/page',params={'pageNo':1,'pageSize':100,'search':'RULE-Q'},role='reader')['list'] if r['builtIn'] and r['ruleVersion']==1]
passed('fifteen PRD rule questions initialized as undecided',len(catalog)==15 and all(r['status']==0 and not r.get('effectiveFrom') and r['caseCount']==0 and r['parameterCount']==0 for r in catalog))
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
 initialized=list(pool.map(lambda _:success('/initialize',method='POST',role='editor'),range(2)))
passed('concurrent catalog initialization idempotent',initialized==[0,0])
candidate=next(r for r in catalog if r['code']=='RULE-Q02');candidate=get(candidate['id'])
success('/update',method='PUT',data={**candidate,'ownerName':'回归补充负责人（演示）'},role='editor')
success('/initialize',method='POST',role='editor')
passed('initialization preserves business draft edits',get(candidate['id'])['ownerName']=='回归补充负责人（演示）')
stamp=str(int(time.time()))
request={'code':'RULE-CUSTOM-REG-'+stamp,'title':'回归样例：规则口径及业务样例 '+stamp,'category':'ROUNDING','questionCode':'Q-11',
 'ownerName':'回归规则负责人（演示）','scopeCode':'QA-ONLY','applicableScope':'仅合成验证样例，不用于生产工资',
 'effectiveFrom':'2026-10-01','effectiveTo':'2026-10-31','definition':'零值和缺失分开登记；样例仅验证类型和证据，未实现正式计算。',
 'reference':'自动化回归说明（合成资料，非实际制度或政策）','parameters':[{'key':'zero','label':'零值参数（演示）','type':'DECIMAL','value':'0.00','unit':'演示单位','scale':2},
 {'key':'flag','label':'布尔参数（演示）','type':'BOOLEAN','value':'false'}],
 'cases':[{'title':'合成零值与缺失核对','inputJson':'{"zero":0,"missing":null}',
 'expectedResult':'零值保留 0；缺失不得推定为零。仅用于验证资料登记，不代表实际金额计算。'}]}
passed('reader cannot create',call('/create',method='POST',data=request,role='reader')['code']==403)
passed('reserved question codes cannot be preempted',call('/create',method='POST',data={**request,'code':'RULE-Q02'})['code']==1050930001)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
 created=list(pool.map(lambda _:call('/create',method='POST',data=request,role='editor'),range(2)))
passed('concurrent same code creation has one winner',sorted(r['code'] for r in created)==[0,1050930004])
id=next(r['data'] for r in created if r['code']==0);row=get(id)
passed('new rule version and revision allocated by server',row['ruleVersion']==1 and row['revision']==1 and row['status']==0)
passed('editor cannot review',review(id,role='editor')['code']==403)
passed('reviewer cannot edit',call('/update',method='PUT',data=row,role='reviewer')['code']==403)
success('/update',method='PUT',data={**row,'definition':request['definition']+' 补充评审资料。'},role='editor')
passed('stale update and review rejected',call('/update',method='PUT',data=row,role='editor')['code']==1050930002 and review(id,revision=1)['code']==1050930002)
row=get(id)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
 updates=list(pool.map(lambda suffix:call('/update',method='PUT',data={**row,'definition':request['definition']+suffix},role='editor'),[' 并发 A',' 并发 B']))
passed('concurrent same revision edits have one winner',sorted(r['code'] for r in updates)==[0,1050930002])
passed('authorized reviewer confirms complete evidence',review(id)['code']==0)
row=get(id)
passed('actor stamped by server and values normalized',row['reviewedByName']!='FORGED' and row['reviewedTime'] and row['parameters'][0]['value']=='0' and row['parameters'][1]['value']=='false')
passed('effective dates use ISO strings',row['effectiveFrom']=='2026-10-01' and row['effectiveTo']=='2026-10-31')
passed('confirmed rule cannot be edited',call('/update',method='PUT',data=row,role='editor')['code']==1050930003)
second=version(id);copied=get(second)
passed('new version retains inputs but no old confirmation',copied['ruleVersion']==2 and copied['status']==0 and not copied.get('reviewedByName') and not copied.get('evidence') and copied['cases']==row['cases'])
passed('overlap with existing confirmed interval rejected',review(second)['code']==1050930005)
success('/update',method='PUT',data={**copied,'effectiveFrom':'2026-11-01','effectiveTo':'2026-11-30'},role='editor')
passed('adjacent nonoverlapping rule interval confirms',review(second)['code']==0)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
 versions=list(pool.map(lambda _:version(id),range(2)))
passed('concurrent new versions allocated sequentially',sorted(get(v)['ruleVersion'] for v in versions)==[3,4])
for v in versions:
 success('/update',method='PUT',role='editor',data={**get(v),'scopeCode':'QA-CONCURRENT','effectiveFrom':'2026-11-01','effectiveTo':'2026-11-30'})
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
 conclusions=list(pool.map(lambda v:review(v),versions))
passed('concurrent overlapping confirmations have one winner',sorted(r['code'] for r in conclusions)==[0,1050930005])
incomplete=copy.deepcopy(request);incomplete['code']='RULE-CUSTOM-INCOMPLETE-'+stamp;incomplete['cases']=[]
incomplete_id=success('/create',method='POST',data=incomplete,role='editor')
passed('business expected case required for confirmation',review(incomplete_id)['code']==1050930001)
bad=copy.deepcopy(request);bad['code']='RULE-CUSTOM-BADJSON-'+stamp;bad['cases'][0]['inputJson']='[1,2]'
passed('nonobject business input rejected',call('/create',method='POST',data=bad,role='editor')['code']==1050930001)
precision=copy.deepcopy(request);precision['code']='RULE-CUSTOM-PRECISION-'+stamp;precision['parameters'][0]['value']='0.001'
precision_id=success('/create',method='POST',data=precision,role='editor')
passed('confirmation rejects precision excess without rounding',review(precision_id)['code']==1050930001 and get(precision_id)['status']==0)
passed('metadata list omits parameter and case payload',all(not r.get('parameters') and not r.get('cases') and not r.get('definition') for r in success('/page',params={'pageNo':1,'pageSize':100})['list']))
for endpoint in ['/get','/history']:
 passed('tenant B denied '+endpoint,call(endpoint,params={'id':id},role='tenantb')['code']==1050930000)
passed('tenant B denied edit review and version allocation',call('/update',method='PUT',data=row,role='tenantb')['code']==1050930000 and review(id,role='tenantb')['code']==1050930000 and call('/new-version',method='POST',params={'id':id,'revision':row['revision']},role='tenantb')['code']==1050930000)
passed('tenant B list remains empty',success('/page',params={'pageNo':1,'pageSize':100},role='tenantb')['total']==0)
history=success('/history',params={'id':id})
passed('successful revision history freezes original business sample',len(history)==4 and '合成零值与缺失核对' in history[0]['afterSnapshot'] and history[0]['action']=='confirm')
passed('retire preserves old confirmed evidence',review(id,'retire')['code']==0 and get(id)['status']==2 and '"status":1' in success('/history',params={'id':id})[0]['beforeSnapshot'])
passed('legacy calculation tax configuration unchanged',success('/hrm/salary/tax-rule/list',common=True)==before_tax)
(args.output_dir/'hrm-rule-api-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2))
(args.output_dir/'hrm-rule-browser-fixture.json').write_text(json.dumps({'code':request['code'],'title':request['title'],'originalId':id,'currentId':second,'currentVersion':2,'incompleteId':incomplete_id},ensure_ascii=False))
print('All',len(checks),'rule API/MySQL regression checks passed')
