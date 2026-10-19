#!/usr/bin/env python3
"""Final S4 candidate impact and authorization regression on prepared TEST fixtures.
No grants, roles, payroll or external services are written here. Browser proof is separate.
"""
import argparse,json,urllib.parse
from pathlib import Path
from datetime import datetime
from zoneinfo import ZoneInfo
from runtime_client import RuntimeClient
import runtime_workbench_stage3 as wb
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--state-file',required=True);parser.add_argument('--output',required=True);args=parser.parse_args();wb.configure(args.state_file);c,s,ed,rv=wb.c,wb.s,wb.editor,wb.reviewer
restricted=c.login(s['restrictedUsername'],'restrictedPassword')
def get(path,token=ed,**params):return c.ok(path+'?'+urllib.parse.urlencode(params),token=token)
def detail(p,id,token=ed):return get('/hrm/payroll/review/detail',token,projectId=p,id=id)
def stats(p,token=ed,**params):return get('/hrm/payroll/review/statistics',token,projectId=p,**params)
def ctx(p,id):return {'projectId':p,'projectScopeVersion':1,'id':id,'version':detail(p,id)['version'],'reason':'TEST S4 final candidate impact'}
def deny(path,data,token=ed):
 r=c.request(path,data,token=token);assert isinstance(r,dict) and r.get('code') not in [None,0],(path,r);return r['code']
def deny_get(path,token=restricted,**params):return deny(path+'?'+urllib.parse.urlencode(params),None,token)
out={'verifiedAt':datetime.now(ZoneInfo('Asia/Shanghai')).isoformat(),'candidatePhase':'final backend after reference clipping and scope action checks'}
x=stats(s['projects']['s0']);assert x['counts']==dict(U=47,E=47,I=40,C=40,T=6,D=0);assert x['includedConfirmation']['label']=='40/40';out['S0AfterRestoredCandidate']=x['counts']
x=stats(s['projects']['s3']);assert x['confirmation']['label']=='39/46' and x['includedConfirmation']['label']=='39/40';out['independentS3']=x['counts']
x=stats(s['projects']['empty']);assert x['confirmation']['percent'] is None;out['emptyIsUnknownNotHundredPercent']=True
p=s['projectId'];r=s['browserRequirementId'];restricted_current=stats(p,restricted)
assert restricted_current['counts']['U']==1 and 'projectTotals' not in restricted_current
assert len(get('/hrm/payroll/review/export',restricted,projectId=p)['requirements'])==1
out['hiddenDetailDenied']=deny_get('/hrm/payroll/review/detail',projectId=p,id=619)
out['crossProjectDenied']=deny_get('/hrm/payroll/review/detail',projectId=s['projects']['other'],id=r)
out['scopeDecideDeniedDespitePlatformPermission']=deny('/hrm/payroll/review/decision',{**ctx(p,r),'decision':'INCLUDED'},restricted)
out['implementationAfterRevocationDenied']=deny('/hrm/payroll/review/implementation',{**ctx(p,r),'state':'DEVELOPING','page':'TEST','api':'TEST','task':'TEST'},restricted)
old={**ctx(p,r),'version':1,'state':'DEVELOPING','page':'TEST','api':'TEST','task':'TEST'};out['staleImplementationDenied']=deny('/hrm/payroll/review/implementation',old)
history=get('/hrm/payroll/requirement/history',restricted,projectId=p,id=r)
old_removed=[json.loads(h['snapshot']) for h in history if json.loads(h['snapshot']).get('scopeDecision')=='REMOVED']
assert old_removed and all('replacementId' not in json.loads(h['scopeDecisionJson']) and json.loads(h['scopeDecisionJson'])['replacementRestricted'] for h in old_removed);out['hiddenHistoricalReplacementClipped']=True
p=s['projects']['sources'];r=s['restrictedSourceRequirementId'];before=stats(p);assert before['readiness']['label']=='2/4'
view=stats(p,restricted);assert view['counts']['E']==1 and view['sourceCount']==1 and len(view['L'])==1
export=get('/hrm/payroll/review/export',restricted,projectId=p);assert export['sourceCount']==0 and len(export['L'])==1 and export['L'][0]['relationId'] is None
assert 'projectTotals' not in view and 'projectTotals' not in export
out['readAndExportSourceIntersection']={'visibleReadRelationships':len(view['L']),'visibleReadSources':view['sourceCount'],'exportSources':export['sourceCount'],'exportRelationshipIds':[i['relationId'] for i in export['L']]}
rows=get('/hrm/payroll/requirement/list',projectId=p)
missing=next(x for x in rows if 'missing source' in x['description']);mismatch=dict(s['scope']);mismatch['entities']=[102];missing.update(scopeMode='SUBSET',scopeJson=json.dumps(mismatch));c.ok('/hrm/payroll/requirement/save',missing,token=ed)
x=stats(p);assert x['readiness']['label']=='2/4' and detail(p,missing['id'])['scopeState']=='SCOPE_MISMATCH';out['scopeMismatchKeepsRelationshipDenominator']=True
base=detail(p,r);base['status']='DEFERRED';out['reviewDeferredCannotBypassScopeDecision']=deny('/hrm/payroll/requirement/save',base)
# Real S3 issue records drive unresolved/overdue and readiness rather than invented counters.
assert not wb.lists(p,'ISSUE'),'Use a source fixture without prior final impact issues'
def issue(category,req,due):return c.ok(wb.root+'/issue/create',{**wb.ctx(p),'issue':{'description':'TEST S4 '+category+' statistics','requirementIds':[req],'sourceIds':[],'wholeProject':False,'category':category,'handlerId':s['editorId'],'reviewerId':s['reviewerId'],'dueDate':due}},token=ed)
a=issue('ADVISORY',r,'2026-10-16');x=stats(p);assert x['unresolvedIssues']==1 and x['overdueIssues']==1 and x['readiness']['label']=='2/4'
other=next(x for x in rows if 'R2' in x['description']);data=issue('DATA_READINESS',other['id'],'2026-11-30');x=stats(p);assert x['unresolvedIssues']==2 and x['readiness']['label']=='1/4'
wb.ia(p,data,'START',ed);wb.ia(p,data,'SUBMIT',ed);wb.ia(p,data,'CLOSE',rv);x=stats(p);assert x['unresolvedIssues']==1 and x['readiness']['label']=='2/4'
block=issue('BASELINE_BLOCK',r,'2026-11-30');x=stats(p);assert block in x['baselineIssueIds'] and x['formalReleaseGate']=='S5_NOT_IMPLEMENTED'
filtered=stats(p,module='OT');assert filtered['counts']['E']==0 and block in filtered['projectTotals']['baselineIssueIds']
out['issues']={'advisoryId':a,'closedDataId':data,'baselineId':block,'unresolved':x['unresolvedIssues'],'overdue':x['overdueIssues'],'ready':x['readiness']['label'],'filterCannotHideProjectBlock':True}
out['finalSourceStatistics']=x;out['formalRelease']='S5_NOT_IMPLEMENTED';Path(args.output).write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'completedCases':list(out),'formalApproval':False}))
