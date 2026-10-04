#!/usr/bin/env python3
"""Read-only comparison of the overview with its authorized underlying APIs."""
import argparse,json,os,urllib.error,urllib.parse,urllib.request
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url',default='http://127.0.0.1:48081')
parser.add_argument('--output-dir',type=Path,required=True)
args=parser.parse_args();args.output_dir.mkdir(parents=True,exist_ok=True)
roles={'admin':(1,'HRM_SMOKE_TOKEN'),'reader':(1,'HRM_READER_TOKEN'),'none':(1,'HRM_UNGRANTED_TOKEN'),
       'tenantb':(999,'HRM_TENANT_B_TOKEN'),'overview':(1,'HRM_OVERVIEW_ONLY_TOKEN')}
for _,env in roles.values():
 if not os.environ.get(env):parser.error('Missing verification credential: '+env)
base=args.base_url.rstrip('/')+'/admin-api';opener=urllib.request.build_opener(urllib.request.ProxyHandler({}));checks=[]
def call(path,params=None,role='admin'):
 tenant,env=roles.get(role,(1,None));headers={'tenant-id':str(tenant)}
 if env:headers['Authorization']='Bearer '+os.environ[env]
 req=urllib.request.Request(base+path+('?' + urllib.parse.urlencode(params) if params else ''),headers=headers)
 try:response=opener.open(req,timeout=30)
 except urllib.error.HTTPError as e:response=e
 return json.load(response)
def get(path,params=None,role='admin'):
 r=call(path,params,role);assert r.get('code')==0,(path,r.get('code'));return r['data']
def passed(name,condition):
 if not condition:raise AssertionError(name)
 checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
path='/hrm/payroll/preparation/summary'
passed('anonymous overview denied',call(path,role=None)['code']==401)
passed('identity without overview permission denied',call(path,role='none')['code']==403)
summary=get(path);passed('authorized overview sections available',all(summary[k]['authorized'] for k in ['requirements','sources','contracts','rules','batches']))
underlying=get('/hrm/payroll/requirements/summary');counts=summary['requirements']['counts']
passed('requirement review and scope counts agree with collection API',all(counts[k]==underlying[k] for k in ['total','pending','confirmed','disputed']) and counts['inScope']==underlying['mvp'])
unconfirmed=sum(get('/hrm/payroll/requirements/page',{'pageNo':1,'pageSize':1,'scopeDecision':1,'status':status})['total'] for status in [0,2])
passed('in-scope unconfirmed is not inferred from global pending count',counts['inScopeUnconfirmed']==unconfirmed)
passed('module totals reconcile with tenant requirement total',sum(m['total'] for m in summary['requirements']['modules'])==counts['total'])
for module in summary['requirements']['modules']:
 page=get('/hrm/payroll/requirements/page',{'pageNo':1,'pageSize':1,'moduleCode':module['code']})
 assert module['total']==page['total'],module['code']
passed('module counts match individually filtered API queries',True)
sources=get('/hrm/payroll/requirements/sources/list');source_counts=summary['sources']['counts']
passed('source readiness remains independent from contract confirmation',source_counts['total']==len(sources) and source_counts['ready']==sum(s['readiness']==1 for s in sources) and source_counts['gap']==sum(s['readiness']==2 for s in sources))
passed('source table metadata matches authorized source catalogue',summary['sources']['sources']==[{k:s[k] for k in ['code','name','readiness']} for s in sorted(sources,key=lambda s:s['code'])])
for section,url in [('contracts','/hrm/payroll/intake/contracts/page'),('rules','/hrm/payroll/rules/page')]:
 actual=summary[section]['counts'];expected=get(url,{'pageNo':1,'pageSize':1})['total'];assert actual['total']==expected
 for status,key in enumerate(['draft','confirmed','retired']):assert actual[key]==get(url,{'pageNo':1,'pageSize':1,'status':status})['total']
 passed(section+' counts include all draft confirmed and retired versions',True)
for role in ['admin','reader','tenantb']:
 result=get(path,role=role);batches=get('/hrm/payroll/intake/batches/page',{'pageNo':1,'pageSize':5},role=role)
 passed(role+' only sees own batch count and recent five',result['batches']['counts']['total']==batches['total'] and [b['id'] for b in result['batches']['recentBatches']]==[b['id'] for b in batches['list']])
 for status,key in [(0,'passed'),(1,'failed')]:assert result['batches']['counts'][key]==get('/hrm/payroll/intake/batches/page',{'pageNo':1,'pageSize':1,'status':status},role=role)['total']
passed('batch dates use ISO strings without personal payload',all(isinstance(b['periodStart'],str) and len(b['periodStart'])==10 and not any(k in b for k in ['fileName','snapshot','fileHash','declaredScope','createdBy']) for b in summary['batches']['recentBatches']))
restricted=get(path,role='overview')
passed('overview-only identity sees authorization flags without counts or rows',all(not restricted[k]['authorized'] and restricted[k].get('counts') is None and restricted[k].get('modules') is None and restricted[k].get('sources') is None and restricted[k].get('recentBatches') is None for k in ['requirements','sources','contracts','rules','batches']))
other=get(path,role='tenantb');other_req=get('/hrm/payroll/requirements/summary',role='tenantb')
passed('tenant B counts match its own APIs instead of tenant A',other['requirements']['counts']['total']==other_req['total'] and other['requirements']['counts']['total']!=summary['requirements']['counts']['total'])
passed('refresh returns stable unchanged metadata',get(path)['requirements']==summary['requirements'])
passed('response never exposes input rules amounts or assessment snapshots',not any(word in json.dumps(summary) for word in ['parametersJson','casesJson','snapshot','fieldMapping','expectedResult','evidence']))
(args.output_dir/'hrm-preparation-api-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2))
print('All',len(checks),'read-only preparation API checks passed')
