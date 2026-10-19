#!/usr/bin/env python3
"""S3 real local API regression. Requires private, independently registered TEST fixtures.
Does not register roots, modify roles, reset historical projects, or contact external systems.
"""
import argparse
import copy
import json
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import urllib.parse
from runtime_client import RuntimeClient

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--state-file',required=True)
parser.add_argument('--output',required=True)
c=None;s=None;editor=None;reviewer=None
def configure(state_file):
 global c,s,editor,reviewer
 c=RuntimeClient(state_file);s=c.state
 editor=c.login(s['editorUsername'],'editorPassword');reviewer=c.login(s['reviewerUsername'],'reviewerPassword')
root='/hrm/payroll/workbench';rp='/hrm/payroll/requirement';pp='/hrm/payroll/requirement-project'
results={};FIXTURES={}
def get(path,params,token=None):return c.ok(path+'?'+urllib.parse.urlencode(params),token=editor if token is None else token)
def lists(p,kind,token=editor):return get(root+'/list',{'projectId':p,'kind':kind},token)
def detail(p,id,kind,token=editor):return get(root+'/detail',{'projectId':p,'id':id,'kind':kind},token)
def rows(p,token=editor):return get(rp+'/list',{'projectId':p},token)
def req(p,id,token=editor):return next(r for r in rows(p,token) if r['id']==id)
def ctx(p,id=None,kind=None,scope=1):
 d={'projectId':p,'projectScopeVersion':scope,'reason':'TEST S3 independent real API regression'}
 if id is not None:d.update(id=id,version=detail(p,id,kind)['version'])
 return d

def post(path,data,token=None):return c.ok(path,data,token=editor if token is None else token)
def deny(path,data,token=None):
 r=c.request(path,data,token=editor if token is None else token)
 assert isinstance(r,dict) and r.get('code') in [403,401,1050810003,1050810043,1050810044,1050810045,1050810002,1050810030,1050810031,1050810032,1050810033,1050810034,1050810035,1050810036,1050810037,1050810038,1050810039,1050810040,1050810041,1050810042],(path,r)
 return r['code']

def source_spec(title):
 return {'name':'TEST '+title,'module':'COL','scopeJson':json.dumps(s['scope']),'ownerId':s['editorId'],'technicalContact':'TEST IT','acquisition':'TEST virtual file','specification':'TEST SPEC V1','uniqueKey':'employeeId,period','employeeMatch':'TEST ID exact','frequency':'monthly','cutoff':'month end','volume':'2 synthetic rows','qualityRule':'TEST QS RULE V1','fields':[{'name':'hours','meaning':'TEST time','type':'decimal','unit':'minute','required':True}]}
def mapping(src,r):return {'sourceId':src,'requirementId':r,'consumptionRule':'TEST QC RULE V1','fields':[{'pageField':'workingHours','sourceField':'hours','meaning':'TEST working time','type':'decimal','unit':'hour','required':True,'transform':'hours / 60','nullRule':'reject','duplicateRule':'reject','periodRule':'same month','exceptionRule':'independent review'}]}
def verify(p,id,layer,result='PASS',token=editor):
 kind='SOURCE' if layer in ['QS','SOURCE_SEMANTIC'] else 'RELATION';o=detail(p,id,kind)
 return post(root+'/verify',{**ctx(p,id,kind),'layer':layer,'result':result,'reference':'TEST independent synthetic rule execution, not formal approval','sampleDigest':s['sampleDigest'],'expectedBinding':o['bindings'][layer]},token)
def available(p):return sum(bool(o['available']) for o in lists(p,'RELATION') if o['data']['active'])
def confirm(p,r):return post(rp+'/confirm',{'projectId':p,'projectScopeVersion':1,'id':r,'version':req(p,r)['version']},reviewer)
def save_source(p,src,change):
 o=detail(p,src,'SOURCE');data=copy.deepcopy(o['data']);change(data)
 return post(root+'/source/save',{**ctx(p,src,'SOURCE'),'source':data})
def save_mapping(p,rel,change):
 o=detail(p,rel,'RELATION');data=copy.deepcopy(o['data']);change(data)
 return post(root+'/relation/save',{**ctx(p,rel,'RELATION'),'relation':data})
def supply(p,src,state,replace=False):return post(root+'/source/supply',{**ctx(p,src,'SOURCE'),'supply':state,'replaceDelivery':replace,'deliveryDigest':s['sampleDigest'],'deliveryReference':'TEST D1 private virtual CSV'})
def fixture(label,baseline=True,allow_existing_sources=False):
 p=s['apiProjects'][label];meta=next(x for x in c.ok(pp+'/list',token=editor) if x['id']==p)
 assert meta['projectKey'].startswith('TEST_S3_') and not rows(p) and (allow_existing_sources or not lists(p,'SOURCE')),'Use a fresh TEST fixture; regression never resets a project'
 rs=[]
 for text in ['R1','R2']:
  r=post(rp+'/save',{'projectId':p,'projectScopeVersion':1,'scopeMode':'INHERIT','code':'CUSTOM-0000','module':'COL','description':'TEST '+label+' '+text,'priority':2,'status':'PENDING','readiness':'READY','sourceApplicability':'APPLICABLE','ruleVersion':'TEST RULE V1','acceptance':'TEST expected normal and exceptional results','evidence':'TEST synthetic review basis'})
  assert req(p,r)['readiness']!='READY'
  post(rp+'/assign',{'projectId':p,'projectScopeVersion':1,'id':r,'version':req(p,r)['version'],'reviewerId':s['reviewerId']});rs.append(r)
 src=post(root+'/source/save',{**ctx(p),'source':source_spec(label)})
 rels=[post(root+'/relation/save',{**ctx(p),'relation':mapping(src,r)}) for r in rs]
 if baseline:
  supply(p,src,'PROVIDED',True);verify(p,src,'SOURCE_SEMANTIC');verify(p,src,'QS')
  for rel in rels:verify(p,rel,'RELATION_SEMANTIC');verify(p,rel,'QC')
  for r in rs:confirm(p,r)
  assert available(p)==2
 FIXTURES[label]={'projectId':p,'requirements':rs,'source':src,'relations':rels}
 return p,rs,src,rels

def issue(p,rs,src,category='DATA_READINESS',whole=False):return post(root+'/issue/create',{**ctx(p),'issue':{'description':'TEST independent issue','requirementIds':[rs[0]],'sourceIds':[],'wholeProject':whole,'category':category,'handlerId':s['editorId'],'reviewerId':s['reviewerId'],'dueDate':'2026-10-17'}})
def ia(p,id,action,token=editor,extra=None):
 o=detail(p,id,'ISSUE');d={**ctx(p,id,'ISSUE'),'action':action,'evidence':'TEST independent '+action,'expectedBinding':o['binding']}
 if extra:d.update(extra)
 return post(root+'/issue/action',d,token)

def run():
 p,rs,src,rels=fixture('contact');save_source(p,src,lambda x:x.update(name='TEST display rename',technicalContact='TEST contact 2'));assert available(p)==2 and all(req(p,r)['status']=='CONFIRMED' for r in rs);results['contact']={'available':2,'confirmationsRetained':True}
 p,rs,src,rels=fixture('owner');save_source(p,src,lambda x:x.update(ownerId=s['reviewerId']));assert available(p)==0 and all(req(p,r)['status']=='CONFIRMED' for r in rs);assert lists(p,'TODO');results['owner']={'available':0,'functionalConfirmationRetained':True,'handoffTodo':True}
 p,rs,src,rels=fixture('supply');supply(p,src,'PENDING');assert available(p)==0 and all(req(p,r)['status']=='CONFIRMED' for r in rs);supply(p,src,'PROVIDED');assert available(p)==2;results['supply']=[0,2]
 for label,field,value in [('unit','unit','hour'),('type','type','string'),('interface','specification','TEST SPEC V2')]:
  p,rs,src,rels=fixture(label)
  old=[copy.deepcopy(x) for x in lists(p,'EVIDENCE')]
  def change(x):
   if field=='specification':x[field]=value
   else:x['fields'][0][field]=value
  save_source(p,src,change);assert available(p)==0 and all(req(p,r)['status']=='REVIEWING' for r in rs)
  assert detail(p,src,'SOURCE')['data']['deliveryVersion']==1
  verify(p,src,'SOURCE_SEMANTIC')
  for r in rels:verify(p,r,'RELATION_SEMANTIC')
  seq=[available(p)];verify(p,src,'QS','FAIL');seq.append(available(p));verify(p,src,'QS');seq.append(available(p));verify(p,rels[0],'QC');seq.append(available(p));verify(p,rels[1],'QC');seq.append(available(p));assert seq==[0,0,0,1,2]
  now={o['id']:o for o in lists(p,'EVIDENCE')}
  assert all(now[o['id']]==o for o in old),'Old evidence must be immutable'
  results[label]={'sameDeliveryVersion':1,'qualityRecovery':seq,'oldEvidenceImmutable':True,'confirmationNotAutoRestored':all(req(p,r)['status']=='REVIEWING' for r in rs)}
 p,rs,src,rels=fixture('mapping');save_mapping(p,rels[0],lambda x:x['fields'][0].update(transform='round(hours / 60, 2)'));assert available(p)==1 and req(p,rs[1])['status']=='CONFIRMED';verify(p,rels[0],'RELATION_SEMANTIC');assert available(p)==1;verify(p,rels[0],'QC');assert available(p)==2;results['mapping']=[1,1,2]
 p,rs,src,rels=fixture('delivery');supply(p,src,'PROVIDED',True);assert available(p)==0 and all(req(p,r)['status']=='CONFIRMED' for r in rs);results['deliveryReplacement']={'available':0,'confirmationsRetained':True}
 p,rs,src,rels=fixture('qsRule');save_source(p,src,lambda x:x.update(qualityRule='TEST QS RULE V2'));assert available(p)==0 and all(req(p,r)['status']=='CONFIRMED' for r in rs);results['qsRule']={'available':0,'semanticVersion':detail(p,src,'SOURCE')['data']['semanticVersion']};assert results['qsRule']['semanticVersion']==1
 p,rs,src,rels=fixture('qcRule');save_mapping(p,rels[0],lambda x:x.update(consumptionRule='TEST QC RULE V2'));assert available(p)==1 and all(req(p,r)['status']=='CONFIRMED' for r in rs);results['qcRule']={'available':1,'confirmationsRetained':True}
 for label in ['expand','period','rename']:
  p,rs,src,rels=fixture(label)
  # R2 is an explicit subset; each operation starts from a separate baseline.
  r=req(p,rs[1]);r.update(scopeMode='SUBSET',scopeJson=json.dumps(s['scope']));post(rp+'/save',r);confirm(p,rs[1])
  # Scope binding changed; re-execute only the corresponding quality layers, original source/delivery retained.
  verify(p,rels[1],'RELATION_SEMANTIC');verify(p,rels[1],'QC');assert available(p)==2
  if label=='rename':post(pp+'/metadata',{'id':p,'projectKey':next(x['projectKey'] for x in c.ok(pp+'/list',token=editor) if x['id']==p),'name':'TEST renamed only','metadataVersion':1});assert available(p)==2 and all(req(p,r)['status']=='CONFIRMED' for r in rs)
  else:
   scope=copy.deepcopy(s['scope'])
   if label=='expand':scope['entities']=[101,102]
   else:scope['from']='2026-11-01';scope['to']='2026-11-30'
   post(pp+'/scope',{'projectId':p,'expectedVersion':1,'scopeJson':json.dumps(scope),'reason':'TEST independent '+label})
   assert available(p)==0 and all(req(p,r)['status']=='REVIEWING' for r in rs)
   assert detail(p,src,'SOURCE')['data']['semanticVersion']==1 and detail(p,src,'SOURCE')['data']['deliveryVersion']==1
   if label=='period':assert req(p,rs[1])['scopeState']=='SCOPE_MISMATCH' and json.loads(req(p,rs[1])['scopeJson'])['from']=='2026-10-01'
  results[label]={'available':available(p),'scopeVersion':req(p,rs[0])['projectScopeVersion']}
 p,rs,src,rels=fixture('applicability',False)
 r=req(p,rs[0]);r.update(sourceApplicability='NOT_APPLICABLE',applicabilityReason='');post(rp+'/save',r)
 d={'projectId':p,'projectScopeVersion':1,'id':rs[0],'version':req(p,rs[0])['version']};deny(rp+'/confirm',d,reviewer)
 r=req(p,rs[0]);r.update(applicabilityReason='仅调整菜单导航，不读取业务数据');post(rp+'/save',r);assert req(p,rs[0])['readiness']=='EXEMPT';confirm(p,rs[0]);assert not detail(p,rels[0],'RELATION')['data']['active']
 r=req(p,rs[0]);r.update(sourceApplicability='APPLICABLE');post(rp+'/save',r);assert req(p,rs[0])['status']=='REVIEWING';deny(rp+'/confirm',{'projectId':p,'projectScopeVersion':1,'id':rs[0],'version':req(p,rs[0])['version']},reviewer)
 # One request cannot impersonate a signature even with complete fields.
 r=req(p,rs[0]);r.update(status='CONFIRMED');deny(rp+'/save',r)
 results['applicability']={'emptyReasonDraftSavedConfirmationDenied':True,'validExemptionConfirmedWithoutSourceOwnerOrMapping':True,'switchDeactivatedOldRelation':True,'switchBackDoesNotReviveRelation':True}
 p,rs,src,rels=fixture('issues');i=issue(p,rs,src);assert available(p)==1;ia(p,i,'START');ia(p,i,'SUBMIT');d={**ctx(p,i,'ISSUE'),'action':'CLOSE','evidence':'TEST close','expectedBinding':detail(p,i,'ISSUE')['binding']};deny(root+'/issue/action',d,editor);ia(p,i,'REJECT',reviewer);ia(p,i,'SUBMIT');ia(p,i,'CLOSE',reviewer);assert available(p)==2;ia(p,i,'REOPEN',reviewer,{'dueDate':'2026-11-30'});assert available(p)==1
 old=detail(p,i,'ISSUE')['binding'];save_mapping(p,rels[0],lambda x:x['fields'][0].update(nullRule='TEST quarantine'));ia(p,i,'START');ia(p,i,'SUBMIT');deny(root+'/issue/action',{**ctx(p,i,'ISSUE'),'action':'CLOSE','evidence':'TEST stale','expectedBinding':old},reviewer)
 d={**ctx(p,i,'ISSUE'),'action':'RECLASSIFY','evidence':'TEST independent review','category':'ADVISORY','wholeProject':False,'requirementIds':[rs[0]],'sourceIds':[]};deny(root+'/issue/action',d,editor);post(root+'/issue/action',d,reviewer);assert all(req(p,r).get('confirmedBy') in [None,s['reviewerId']] for r in rs)
 results['issues']={'completeRejectCloseReopenFlow':True,'selfCloseDenied':True,'staleAssociationCloseDenied':True,'downgradeRequiresIndependentReview':True}
 p,rs,src,rels=fixture('copy');target=s['apiProjects']['security'];meta=next(x for x in c.ok(pp+'/list',token=editor) if x['id']==target);assert meta['projectKey'].startswith('TEST_S3_')
 cp=post(root+'/source/copy',{**ctx(target),'sourceProjectId':p,'sourceId':src,'sourceVersion':detail(p,src,'SOURCE')['version']});copied=detail(target,cp,'SOURCE');assert copied['data']['supply']=='UNINVENTORIED' and copied['data']['deliveryVersion']==0 and not copied['sourceSemanticVerified'];save_source(p,src,lambda x:x['fields'][0].update(unit='hour'));assert detail(target,cp,'SOURCE')['data']['fields'][0]['unit']=='minute'
 extra=post(root+'/source/save',{**ctx(p),'source':source_spec('second actual source')});extraRel=post(root+'/relation/save',{**ctx(p),'relation':mapping(extra,rs[0])});assert extraRel not in rels and len([o for o in lists(p,'RELATION') if o['data']['requirementId']==rs[0]])==2
 results['copy']={'independentSourceId':cp,'versionsResetSupplyAndEvidenceNotInherited':True,'originalMutationDoesNotAffectCopy':True,'manyToMany':True}
 return results

if __name__ == '__main__':
 a=parser.parse_args()
 configure(a.state_file)
 try:
  run()
  Path(a.output).write_text(json.dumps({'fixtures':FIXTURES,'results':results,'browserAcceptance':'SEPARATE_GATE','formalBusinessApproval':False},ensure_ascii=False,indent=2)+'\n')
  print(json.dumps({'completedCases':list(results),'fixtureCount':len(FIXTURES)}))
 except Exception:
  Path(a.output).write_text(json.dumps({'fixtures':FIXTURES,'results':results,'complete':False},ensure_ascii=False,indent=2)+'\n')
  raise
