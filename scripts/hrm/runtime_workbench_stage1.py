#!/usr/bin/env python3
"""Local synthetic S1 API regression. Browser acceptance remains a separate gate."""
import argparse,json,concurrent.futures
from pathlib import Path
from runtime_client import RuntimeClient
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--state-file',required=True)
parser.add_argument('--output',type=Path,required=True)
parser.add_argument('--run-id',default='R3')
a=parser.parse_args();c=RuntimeClient(a.state_file)
# Preserve original records: only projects named TEST_S1_20261019_* are written.
projects=c.ok('/hrm/payroll/requirement-project/list')
def project(key,token=None):
    visible=c.ok('/hrm/payroll/requirement-project/list',token=token)
    old=next((p for p in visible if p['projectKey']==key),None)
    return old['id'] if old else c.ok('/hrm/payroll/requirement-project/create',{'projectKey':key,'name':key+' 合成验收'},token=token)
assert a.run_id.isalnum() and len(a.run_id)<=8
pa=project('TEST_S1_20261019_A_'+a.run_id);pb=project('TEST_S1_20261019_B_'+a.run_id)
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    added=list(pool.map(lambda p:c.ok('/hrm/payroll/requirement/initialize?projectId='+str(p),{}),[pa,pa,pb,pb]))
def rows(p,token=None):return c.ok('/hrm/payroll/requirement/list?projectId='+str(p),token=token)
ra,rb=rows(pa),rows(pb)
assert len(ra)==len(rb)==46 and len({r['templateItemId'] for r in ra})==46
assert {r['id'] for r in ra}.isdisjoint({r['id'] for r in rb})
assert {r['code'] for r in ra}=={r['code'] for r in rb}
def save_payload(r):
    fields=['id','projectId','projectScopeVersion','code','module','description','fieldMapping','sourceSystem','sourceOwnerId','reviewerId','priority','status','readiness','acceptance','evidence','version','scopeMode','scopeJson']
    return {k:r.get(k) for k in fields}
r=next(x for x in ra if x['code']=='OV-01');before_b=rows(pb)
payload=save_payload(r);payload['description']='TEST S1 API manual description retained'
c.ok('/hrm/payroll/requirement/save',payload)
assert rows(pb)==before_b
# Cross-project body mismatch and history ID substitution are rejected.
invalid={**payload,'projectId':pb}
assert c.request('/hrm/payroll/requirement/save',invalid)['code']!=0
assert c.request('/hrm/payroll/requirement/history?id='+str(r['id'])+'&projectId='+str(pb))['code']==1050810043
assert c.request('/hrm/payroll/requirement/confirm',{'id':r['id'],'version':r['version'],'projectId':pb})['code']==1050810043
assert c.request('/hrm/payroll/requirement/assign',{'id':r['id'],'version':r['version'],'projectId':pb,'reviewerId':249})['code']==1050810043
operator=c.login('hrmacceptoperator','browserPassword')
assert not any(x['id']==pa for x in c.ok('/hrm/payroll/requirement-project/list',token=operator))
assert c.request('/hrm/payroll/requirement/list?projectId='+str(pa),token=operator)['code']==1050810043
assert c.request('/hrm/payroll/requirement/history?id='+str(r['id']),token=operator)['code']==1050810043
assert c.request('/hrm/payroll/requirement/save',payload,token=operator)['code']==1050810043
assert c.request('/hrm/payroll/requirement/initialize?projectId='+str(pa),{},token=operator)['code']==1050810043
# Existing ordinary editor creates only its own synthetic project, no role grant.
pc=project('TEST_S1_20261019_EDITOR',operator)
create={'projectId':pc,'code':'CUSTOM-0000','module':'COL','description':'TEST S1 ordinary editor own project','status':'PENDING','readiness':'MISSING','priority':2,'scopeMode':'INHERIT'}
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    ids=list(pool.map(lambda _:c.ok('/hrm/payroll/requirement/save',create,token=operator),range(3)))
assert len(set(ids))==3
cr=rows(pc,operator);assert len({x['code'] for x in cr})==len(cr)
# Ownership never grants business scope authority, even to technical admin.
scope={'entities':[101],'organizations':[201],'workerTypes':['TEST'],'scenarios':['TEST'],'from':'2026-10-01','to':'2026-10-31','explanation':'TEST synthetic only'}
assert c.request('/hrm/payroll/requirement-project/scope',{'projectId':pa,'scopeJson':json.dumps(scope),'reason':'TEST'},token=None)['code']==1050810043
metadata=next(x for x in c.ok('/hrm/payroll/requirement-project/list') if x['id']==pa)
c.ok('/hrm/payroll/requirement-project/metadata',{'id':pa,'projectKey':metadata['projectKey'],'name':'TEST S1 A renamed','metadataVersion':metadata['metadataVersion']})
updated=next(x for x in c.ok('/hrm/payroll/requirement-project/list') if x['id']==pa)
assert updated['metadataVersion']==metadata['metadataVersion']+1 and updated.get('projectScopeVersion')==metadata.get('projectScopeVersion')
# Old unscoped list must remain legacy-only.
legacy=c.ok('/hrm/payroll/requirement/list');assert not any(x['projectId'] in [pa,pb,pc] for x in legacy)
result={'projects':{'A':pa,'B':pb,'ordinaryEditor':pc},'projectARequirements':len(ra),'projectBRequirements':len(rb),'parallelInitializationAdded':added,'sameCodesDistinctIds':True,'crossProjectEditsAndHistoryDenied':True,'ordinaryEditorForeignProjectDenied':True,'ordinaryEditorOwnProjectSaves':len(ids),'customUnique':True,'metadataOnlyChange':True,'ownerScopeAuthorityDenied':True,'legacyListIsolated':True,'browserAcceptance':'NOT_RUN'}
a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2));print(json.dumps(result,ensure_ascii=False))
