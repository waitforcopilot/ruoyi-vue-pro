#!/usr/bin/env python3
import json, urllib.request, urllib.parse, urllib.error, concurrent.futures, time, zipfile, io, xml.etree.ElementTree as E
import argparse, os
from pathlib import Path
parser=argparse.ArgumentParser(description='Writes uniquely named regression fixtures: run only against an isolated validation database.')
parser.add_argument('--base-url', default='http://127.0.0.1:48081')
parser.add_argument('--allow-test-fixtures', action='store_true', required=True)
parser.add_argument('--output-dir', type=Path, required=True)
args=parser.parse_args()
args.output_dir.mkdir(parents=True, exist_ok=True)
base=args.base_url.rstrip('/')+'/admin-api/hrm/payroll/requirements'
required=['HRM_SMOKE_TOKEN','HRM_READER_TOKEN','HRM_EDITOR_TOKEN','HRM_REVIEWER_TOKEN','HRM_UNGRANTED_TOKEN','HRM_TENANT_B_TOKEN']
for name in required:
 if not os.environ.get(name): parser.error('Missing verification credential: '+name)
admin={'accessToken':os.environ['HRM_SMOKE_TOKEN']}
roles={key:{'tenant':tenant,'auth':{'accessToken':os.environ[env]}} for key,tenant,env in [('reader',1,'HRM_READER_TOKEN'),('editor',1,'HRM_EDITOR_TOKEN'),('reviewer',1,'HRM_REVIEWER_TOKEN'),('none',1,'HRM_UNGRANTED_TOKEN'),('tenantb',999,'HRM_TENANT_B_TOKEN')]}
checks=[]
def call(path, method='GET', data=None, params=None, role='admin', raw=False, with_headers=False):
 credential=admin if role=='admin' else roles[role]['auth'] if role else None
 headers={'Content-Type':'application/json','tenant-id':str(1 if role in ('admin',None) else roles[role]['tenant'])}
 if credential:headers['Authorization']='Bearer '+credential['accessToken']
 url=base+path+('?' + urllib.parse.urlencode(params) if params else '')
 request=urllib.request.Request(url,headers=headers,method=method,data=json.dumps(data).encode() if data is not None else None)
 try:r=urllib.request.urlopen(request,timeout=30)
 except urllib.error.HTTPError as e:r=e
 payload=r.read()
 result=payload if raw else json.loads(payload)
 return (result,r.headers) if with_headers else result
def passed(name, condition):
 if not condition:raise AssertionError(name)
 checks.append({'check':name,'result':'PASS'});print('PASS:',name)
def success(path,**kw):
 result=call(path,**kw)
 assert result.get('code')==0,(path,result)
 return result.get('data')
passed('candidate catalog initialized',success('/page',params={'pageNo':1,'pageSize':100})['total'] >= 46)
passed('anonymous query denied',call('/summary',role=None)['code']==401)
passed('ungranted account denied',call('/summary',role='none')['code']==403)
passed('reader can query',call('/page',params={'pageNo':1,'pageSize':10},role='reader')['code']==0)
initial_summary=success('/summary')
initial_count=initial_summary['total']
request={'code':'CUSTOM-REG-'+str(int(time.time())),'moduleCode':'req','title':'回归样例：需求评审与版本核验','ownerName':'回归测试负责人（演示）','scopeDecision':1,'sourceCodes':['DS-01'],'acceptanceCriteria':'保存、评审与基线导出结果一致；记录修改后必须重新确认','remark':'隔离验证库中的回归样例，不代表真实业务确认'}
passed('reader cannot create',call('/create',method='POST',data=request,role='reader')['code']==403)
id=success('/create',method='POST',data=request,role='editor')
row=success('/get',params={'id':id})
passed('reader cannot update',call('/update',method='PUT',data=row,role='reader')['code']==403)
review={'id':id,'version':row['version'],'status':1,'evidence':'回归样例：依据自动化验收记录确认，仅用于隔离测试'}
passed('editor cannot review',call('/review',method='POST',data=review,role='editor')['code']==403)
passed('editor cannot export',call('/baselines/create',method='POST',role='editor')['code']==403)
success('/review',method='POST',data=review,role='reviewer')
row=success('/get',params={'id':id})
passed('review records authorized actor and time',row['status']==1 and bool(row.get('reviewedByName')) and row.get('reviewedTime'))
passed('confirmation does not mark data ready',success('/summary')['sourceReady']==initial_summary['sourceReady'])
baseline=success('/baselines/create',method='POST')
original=row['title']
changed={**row,'title':'回归样例：确认后修改，重新评审'}
success('/update',method='PUT',data=changed,role='editor')
updated=success('/get',params={'id':id})
passed('edit invalidates previous confirmation',updated['status']==0 and not updated.get('evidence') and not updated.get('reviewedByName'))
passed('stale update rejected',call('/update',method='PUT',data=changed,role='editor')['code']==1050910002)
payloads=[{**updated,'remark':'并发核验 A'},{**updated,'remark':'并发核验 B'}]
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
 results=list(pool.map(lambda data:call('/update',method='PUT',data=data,role='editor'),payloads))
passed('concurrent same-version saves: exactly one succeeds',sorted(r['code'] for r in results)==[0,1050910002])
history=success('/history',params={'objectType':'requirement','objectId':id})
passed('audit retains old conclusion and before/after versions',len(history)==4 and any('确认后修改' in h['reason'] for h in history))
export=success('/baselines/list')
passed('baseline list does not expose snapshots',all(not b.get('snapshot') for b in export))
blob,response_headers=call('/baselines/export',params={'id':baseline},raw=True,with_headers=True)
assert response_headers.get_content_type()=='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
assert response_headers.get('Content-Disposition','').endswith('-B'+str(baseline)+'.xlsx')
(args.output_dir/'payroll-review-baseline.xlsx').write_bytes(blob)
ns={'s':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
with zipfile.ZipFile(io.BytesIO(blob)) as z:
 strings=E.fromstring(z.read('xl/sharedStrings.xml'))
 texts=[''.join(si.itertext()) for si in strings]
 for name in ['xl/worksheets/sheet1.xml','xl/worksheets/sheet2.xml','xl/worksheets/sheet3.xml']:
  texts.extend(t.text for t in E.fromstring(z.read(name)).findall('.//s:t',ns))
 workbook=E.fromstring(z.read('xl/workbook.xml'))
 sheet_names=[s.attrib['name'] for s in workbook.find('s:sheets',ns)]
 passed('export has three sheets and frozen pre-edit title',sheet_names==['需求评审','数据来源','基线说明'] and original in texts and changed['title'] not in texts)
 sheet=E.fromstring(z.read('xl/worksheets/sheet1.xml'))
 passed('export covers the complete catalog plus the new requirement',len(sheet.find('s:sheetData',ns))==initial_count+2)
passed('source-to-requirement query uses exact stable code',success('/page',params={'pageNo':1,'pageSize':100,'sourceCode':'DS-01'})['total']>0 and success('/page',params={'pageNo':1,'pageSize':100,'sourceCode':'DS-010'})['total']==0)
source=success('/sources/list')[0]
ready={**source,'readiness':1,'actualSystem':'回归测试 HR 主档（演示）','ownerName':'回归数据负责人（演示）','fieldMapping':'测试 employeeCode -> employee_id；生效日期为 YYYY-MM-DD；金额单位为元','evidence':'回归样例：脱敏样本字段已核对，生产契约仍待业务负责人确认'}
passed('editor cannot update data source',call('/sources/update',method='PUT',data=ready,role='editor')['code']==403)
success('/sources/update',method='PUT',data=ready)
passed('source readiness independently persists',success('/summary')['sourceReady']>=1 and success('/summary')['confirmed']==initial_summary['confirmed'])
passed('tenant B cannot read tenant A detail',call('/get',params={'id':id},role='tenantb')['code']==1050910000)
passed('tenant B cannot read tenant A history',call('/history',params={'objectType':'requirement','objectId':id},role='tenantb')['code']==1050910000)
passed('tenant B cannot export tenant A baseline',call('/baselines/export',params={'id':baseline},role='tenantb')['code']==1050910005)
passed('tenant B list remains empty',call('/summary',role='tenantb')['data']['total']==0)
passed('tenant B cannot modify tenant A record',call('/update',method='PUT',data=updated,role='tenantb')['code']==1050910000)
passed('prototype candidate cannot be deleted',call('/delete',method='DELETE',params={'id':success('/page',params={'pageNo':1,'pageSize':1})['list'][0]['id'],'version':1})['code']==1050910004)
deleted_request={**request,'code':request['code']+'-DELETED'}
deleted_id=success('/create',method='POST',data=deleted_request,role='editor')
success('/delete',method='DELETE',params={'id':deleted_id,'version':1},role='editor')
deleted_history=success('/history',params={'objectType':'requirement','objectId':deleted_id})
passed('deleted custom detail is hidden, code reserved, audit remains tenant-owned',
 call('/get',params={'id':deleted_id})['code']==1050910000
 and call('/create',method='POST',data=deleted_request,role='editor')['code']==1050910001
 and any(h['action']=='delete' for h in deleted_history)
 and call('/history',params={'objectType':'requirement','objectId':deleted_id},role='tenantb')['code']==1050910000)
(args.output_dir/'hrm-collection-api-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2))
(args.output_dir/'hrm-collection-browser-fixture.json').write_text(json.dumps({'customId':id,'customCode':request['code'],'baseline':baseline},ensure_ascii=False))
print('All',len(checks),'API/MySQL regression checks passed')
