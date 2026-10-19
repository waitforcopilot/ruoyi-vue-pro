#!/usr/bin/env python3
"""Real localhost S4 API regression; private independently registered TEST fixtures only.
Never creates roles, grants or resets projects. Browser is a separate acceptance gate.
"""
import argparse,copy,json,urllib.parse
from pathlib import Path
from runtime_client import RuntimeClient
import runtime_workbench_stage3 as wb

def business(module='OT'):
 return {'page':'/hrm-payroll/review','contextModule':module,'anchor':'field-hours' if module in ['OT','ATT','HOUR'] else 'module','targetType':'FIELD' if module in ['OT','ATT','HOUR'] else 'MODULE','role':'TEST HR','scenario':'TEST review overtime','goal':'TEST collect requirement','expected':'TEST independent expected result','normalCase':'TEST 8 hours','exceptionCase':'TEST missing punch: unknown','calculation':'TEST discussion rule; not approved policy','unit':'TEST hour','precision':'TEST 2 decimals HALF_UP','validity':'TEST 2026-10','ruleException':'TEST independent correction review','disputeOwnerId':s['editorId'],'nextStep':'TEST review with business owner'}

def get(path,**params):return c.ok(path+'?'+urllib.parse.urlencode({k:v for k,v in params.items() if v is not None}),token=editor)
def post(path,data,token=None):return c.ok(path,data,token=editor if token is None else token)
def rows(p):return get('/hrm/payroll/requirement/list',projectId=p)
def row(p,id):return get('/hrm/payroll/review/detail',projectId=p,id=id)
def ctx(p,id):return {'projectId':p,'projectScopeVersion':1,'id':id,'version':row(p,id)['version'],'reason':'TEST S4 explicit independent basis'}
def stat(p,**kw):return get('/hrm/payroll/review/statistics',projectId=p,**kw)
def deny(path,data,token=None):
 r=c.request(path,data,token=editor if token is None else token);assert isinstance(r,dict) and r.get('code') not in [None,0],(path,r);return r['code']
def create(p,title,module='OT',app='NOT_APPLICABLE'):
 return post('/hrm/payroll/requirement/save',{'projectId':p,'projectScopeVersion':1,'scopeMode':'INHERIT','code':'CUSTOM-0000','module':module,'description':'TEST S4 '+title,'priority':2,'status':'PENDING','readiness':'READY','sourceApplicability':app,'applicabilityReason':'TEST only page discussion, no business data consumed' if app=='NOT_APPLICABLE' else '', 'ruleVersion':'TEST RULE V1','acceptance':'TEST normal and missing input outcomes','evidence':'TEST independently authorized synthetic review','businessContextJson':json.dumps(business(module))})
def assign(p,id,user=None):return post('/hrm/payroll/requirement/assign',{**ctx(p,id),'reviewerId':s['reviewerId'] if user is None else user})
def confirm(p,id):return post('/hrm/payroll/requirement/confirm',ctx(p,id),reviewer)
def decide(p,id,d,**kw):return post('/hrm/payroll/review/decision',{**ctx(p,id),'decision':d,**kw})
def edit(p,id,**kw):
 x=row(p,id);x.update(kw);return post('/hrm/payroll/requirement/save',x)
def expected(p,u,e,i,confirmed,t,d,ic):
 x=stat(p);assert x['counts']==dict(U=u,E=e,I=i,C=confirmed,T=t,D=d),x['counts']
 assert x['confirmation']['label']==f'{confirmed}/{e}';assert x['scopeDisposition']['label']==f'{i+t}/{e}';assert x['includedConfirmation']['label']==f'{ic}/{i}';assert x['formalReleaseGate']=='S5_NOT_IMPLEMENTED';return x

def fixed(p):
 assert not rows(p),'Use a fresh TEST fixture; this regression never clears or resets projects'
 result=[]
 for n in range(46):
  id=create(p,'fixed '+str(n),'OT' if n%2==0 else 'ATT');assign(p,id);result.append(id)
  if n<40:decide(p,id,'INCLUDED');assert row(p,id)['status']=='PENDING';confirm(p,id)
  else:decide(p,id,'DEFERRED',responsibleId=s['editorId'],reevaluate='TEST data supplied next review')
 return result

def run():
 p=s['projects']['s0'];ids=fixed(p);out['S0']=expected(p,46,46,40,40,6,0,40)
 extra=create(p,'disputed candidate');assign(p,extra);edit(p,extra,status='DISPUTED');out['S1']=expected(p,47,47,40,40,6,1,40)
 history_before=get('/hrm/payroll/requirement/history',projectId=p,id=extra)
 decide(p,extra,'REMOVED',removalKind='DUPLICATE',replacementId=ids[0]);out['S2']=expected(p,47,46,40,40,6,0,40)
 assert len(get('/hrm/payroll/requirement/history',projectId=p,id=extra))==len(history_before)+1
 deny('/hrm/payroll/review/decision',{**ctx(p,ids[0]),'decision':'REMOVED','removalKind':'DUPLICATE','replacementId':extra})
 old=ctx(p,extra);decide(p,extra,'CANDIDATE');assert row(p,extra)['status']=='REVIEWING' and not row(p,extra)['currentConfirmationValid']
 deny('/hrm/payroll/review/decision',{**old,'decision':'INCLUDED'})
 filtered=stat(p,decision='INCLUDED');assert filtered['counts']['E']==40 and filtered['projectTotals']['counts']['E']==47;out['filteredWholeProjectUnchanged']=True
 page=get('/hrm/payroll/review/page',projectId=p,pageNo=2,pageSize=10,sort='-version');assert len(page['list'])==10 and page['total']==47
 ex=get('/hrm/payroll/review/export',projectId=p,decision='INCLUDED');assert len(ex['requirements'])==40 and ex['counts']['E']==40;out['paginationFilterExport']=True
 p=s['projects']['s3'];ids3=fixed(p);changed=business('OT');changed['scenario']='TEST changed semantic scenario';edit(p,ids3[0],businessContextJson=json.dumps(changed));out['S3']=expected(p,46,46,40,39,6,0,39)
 p=s['projects']['empty'];x=stat(p);assert all(x[k]['denominator']==0 and x[k]['percent'] is None and '0/0' in x[k]['label'] for k in ['confirmation','scopeDisposition','includedConfirmation','readiness','includedReadiness']);out['S4']=x
 p=s['projects']['scopeOnly'];id=create(p,'scope alias');deny('/hrm/payroll/review/decision',{**ctx(p,id),'decision':'INCLUDED'});out['scopeDoesNotImplyScopeDecide']=True
 # Immutable review association and controlled in-app notes; confirmation independent.
 p=s['projects']['s3'];id=ids3[2];before=row(p,id);assert before['currentConfirmationValid']
 post('/hrm/payroll/review/implementation',{**ctx(p,id),'state':'ACCEPTED','page':'TEST local review page','api':'TEST API reference','task':'TEST TASK S4','evidence':'TEST unverified evidence reference'})
 after=row(p,id);assert after['currentConfirmationValid'] and after['confirmedBy']==before['confirmedBy'] and after['semanticVersion']==before['semanticVersion'] and json.loads(after['implementationJson'])['conclusionVerified'] is False
 for kind in ['MEETING','REMINDER']:post('/hrm/payroll/review/collaboration',{**ctx(p,id),'kind':kind,'reference':'TEST '+kind,'recipientId':s['reviewerId']})
 assert len(json.loads(row(p,id)['collaborationJson']))==2;out['implementationAndCollaborationIndependent']=True
 # Real partial assignment: first succeeds, second uses stale version and fails.
 a,b=ids3[4:6];old=ctx(p,b);decide(p,b,'INCLUDED');result=None
 # Assigner cannot self-assign, use another independent current reviewer. Same reviewer no-op is legal.
 result=post('/hrm/payroll/review/batch-assign',{'projectId':p,'projectScopeVersion':1,'reviewerId':s['reviewerId'],'items':[ctx(p,a),old]});assert [x['success'] for x in result]==[True,False];out['batchPartialFailure']=result
 # S3 real supply/semantic/QS/QC is the only source of availability, not front-end READY.
 p=s['projects']['sources'];assert not rows(p)
 rs=[create(p,'R1','COL','APPLICABLE'),create(p,'R2','COL','APPLICABLE')]
 for id in rs:assign(p,id)
 src=post(wb.root+'/source/save',{**wb.ctx(p),'source':wb.source_spec('S4 source')});rels=[post(wb.root+'/relation/save',{**wb.ctx(p),'relation':wb.mapping(src,id)}) for id in rs]
 for id in rs:confirm(p,id)
 assert stat(p)['readiness']['label']=='0/2'
 wb.supply(p,src,'PROVIDED',True);wb.verify(p,src,'SOURCE_SEMANTIC');wb.verify(p,src,'QS')
 for rel in rels:wb.verify(p,rel,'RELATION_SEMANTIC');wb.verify(p,rel,'QC')
 assert stat(p)['readiness']['label']=='2/2'
 gap=create(p,'missing source','COL','APPLICABLE');assert stat(p)['readiness']['label']=='2/3'
 decide(p,gap,'DEFERRED',responsibleId=s['editorId'],reevaluate='TEST supply then review');assert stat(p)['readiness']['label']=='2/3'
 invalid=create(p,'invalid exemption','COL');edit(p,invalid,applicabilityReason='');assert stat(p)['readiness']['label']=='2/4'
 edit(p,invalid,applicabilityReason='TEST no business source because navigation only');assert stat(p)['readiness']['label']=='2/3'
 post(wb.root+'/relation/save',{**wb.ctx(p),'relation':wb.mapping(src,gap)});assert stat(p)['readiness']['label']=='2/3'
 src2=post(wb.root+'/source/save',{**wb.ctx(p),'source':wb.source_spec('S4 second source')});post(wb.root+'/relation/save',{**wb.ctx(p),'relation':wb.mapping(src2,rs[0])})
 decide(p,rs[0],'INCLUDED');x=stat(p);assert x['readiness']['label']=='2/4' and x['includedReadiness']['label']=='1/2' and x['sourceCount']==2 and x['exempt']==1
 out['sourceRelationships']=x
 # Deliberately omit signatures in one legacy-labelled row is tested via unit fixture; API never fabricates signatures.
 out['protectedBusinessWriteMode']='review-only; full table checksums independently compared after browser'
 out['browserAcceptance']='SEPARATE_GATE';out['formalRelease']='S5_NOT_IMPLEMENTED';out['fixtureIds']={'s0':s['projects']['s0'],'s3':s['projects']['s3'],'empty':s['projects']['empty'],'source':src,'relations':rels}

if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--state-file',required=True);parser.add_argument('--output',required=True);args=parser.parse_args()
 wb.configure(args.state_file);c,s,editor,reviewer=wb.c,wb.s,wb.editor,wb.reviewer;out={};run();Path(args.output).write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'completedCases':list(out),'formalApproval':False}))
