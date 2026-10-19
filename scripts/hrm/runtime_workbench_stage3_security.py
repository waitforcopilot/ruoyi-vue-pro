#!/usr/bin/env python3
"""Real S3 permission, revocation and transaction races on fresh TEST fixtures only."""
import argparse,copy,json,urllib.parse
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import runtime_workbench_stage3 as w
ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--state-file',required=True);ap.add_argument('--output',required=True);a=ap.parse_args();w.configure(a.state_file)
c,s=w.c,w.s;editor,reviewer=w.editor,w.reviewer;result={}
p,rs,src,rels=w.fixture('security',allow_existing_sources=True)
restricted=c.login(s['restrictedUsername'],'restrictedPassword')
base={'projectId':p,'userId':s['restrictedId'],'actions':'READ','delegableActions':'','ceilingJson':json.dumps(s['scope']),'modulesJson':'["COL"]','requirementIdsJson':json.dumps([rs[0]]),'sourceIdsJson':'[]','fullProject':False,'evidence':'TEST explicitly bounded synthetic delegation','validFrom':'2026-10-19','validTo':'2026-10-19'}
bare=w.post(w.pp+'/member-grant',base)
# Controller routes are fixed below; no root registration happens in this script.
assert w.rows(p,restricted)[0]['id']==rs[0]
assert not w.lists(p,'SOURCE',restricted) and not w.lists(p,'RELATION',restricted)
h=w.get(w.rp+'/history',{'projectId':p,'id':rs[0]},restricted)
for x in h:
 r=json.loads(x['snapshot'])
 if r.get('confirmedTupleJson'):
  t=json.loads(r['confirmedTupleJson']);assert not t['sources'] and 'authority' not in t
result['requirementReadDoesNotImplySourceRead']=True
expanded={**base,'actions':'READ,EDIT,SOURCE_EDIT','sourceIdsJson':json.dumps([src])}
grant=w.post(w.pp+'/member-grant',expanded)
assert len(w.lists(p,'SOURCE',restricted))==1 and len(w.lists(p,'RELATION',restricted))==1
assert all(o['data'].get('requirementId',rs[0])!=rs[1] for o in w.lists(p,'RELATION',restricted))
before=w.detail(p,src,'SOURCE');changed=copy.deepcopy(before['data']);changed['fields'][0]['unit']='hour'
w.deny(w.root+'/source/save',{**w.ctx(p,src,'SOURCE'),'source':changed},restricted)
assert w.detail(p,src,'SOURCE')==before
w.post(w.root+'/source/propose',{**w.ctx(p,src,'SOURCE'),'suggestion':'TEST request contract change from authorized owner'},restricted)
assert w.lists(p,'PROPOSAL')
result['sharedSemanticCoverageAndProposalPath']=True
wrong_project=s['apiProjects']['copy']
w.deny(w.root+'/source/supply',{**w.ctx(wrong_project),'id':src,'version':before['version'],'supply':'PENDING','replaceDelivery':False})
w.deny(w.root+'/source/copy',{**w.ctx(wrong_project),'sourceProjectId':p,'sourceId':src,'sourceVersion':before['version']},restricted)
w.deny(w.root+'/source/copy',{**w.ctx(wrong_project),'sourceProjectId':p,'sourceId':src,'sourceVersion':before['version']},reviewer)
w.deny(w.pp+'/member-grant',{**expanded,'sourceIdsJson':json.dumps([w.FIXTURES.get('copy',{}).get('source',99999999)])})
w.deny(w.pp+'/member-grant',{**expanded,'actions':'READ,UNKNOWN_S3_ACTION'})
w.deny(w.rp+'/save',w.req(p,rs[0]),reviewer)
result['crossProjectCopyBothEndsAndUnknownActionsDenied']=True
# Cross-tenant header cannot reuse tenant1 membership or IDs.
import urllib.request,urllib.error
request=urllib.request.Request(c.base+w.root+'/detail?'+urllib.parse.urlencode({'projectId':p,'kind':'SOURCE','id':src}),headers={'tenant-id':'2','Authorization':'Bearer '+editor})
try:
 with c.opener.open(request,timeout=30) as response:cross=json.loads(response.read())
except urllib.error.HTTPError as error:cross=json.loads(error.read())
assert cross.get('code') in [401,403,1050810043],cross
result['crossTenantDenied']=True
w.post(w.pp+'/member-revoke',{'projectId':p,'grantId':grant,'version':1,'reason':'TEST revoke source delegation'})
assert not w.lists(p,'SOURCE',restricted)
w.deny(w.root+'/source/supply',{**w.ctx(p,src,'SOURCE'),'supply':'PENDING','replaceDelivery':False},restricted)
assert w.rows(p,restricted) and w.lists(p,'SOURCE')
result['sameTokenRevocationAndIndependentReadGrant']=True

p,rs,src,rels=w.fixture('concurrency')
allowed={0,1050810043,1050810044,1050810045,1050810002,1050810003,1050810040}
def race(label,left,right):
 def run(x):return c.request(x[0],x[1],token=x[2])
 with ThreadPoolExecutor(max_workers=2) as pool:out=list(pool.map(run,[left,right]))
 assert all(isinstance(x,dict) and x.get('code') in allowed for x in out),(label,out)
 assert out[1]['code']==0,(label,out)
 result[label]=[x['code'] for x in out]
def verification(id,layer):
 kind='SOURCE' if layer in ['QS','SOURCE_SEMANTIC'] else 'RELATION';o=w.detail(p,id,kind)
 return (w.root+'/verify',{**w.ctx(p,id,kind),'layer':layer,'result':'PASS','reference':'TEST concurrent verification execution','sampleDigest':s['sampleDigest'],'expectedBinding':o['bindings'][layer]},editor)
def restore():
 w.verify(p,src,'SOURCE_SEMANTIC');w.verify(p,src,'QS')
 for rel in rels:w.verify(p,rel,'RELATION_SEMANTIC');w.verify(p,rel,'QC')
 for r in rs:
  if w.req(p,r)['status']!='CONFIRMED':w.confirm(p,r)
 assert w.available(p)==2
change=copy.deepcopy(w.detail(p,src,'SOURCE')['data']);change['specification']='TEST concurrent SPEC V2'
race('sourceVsQS',verification(src,'QS'),(w.root+'/source/save',{**w.ctx(p,src,'SOURCE'),'source':change},editor));assert w.available(p)==0
restore()
change=copy.deepcopy(w.detail(p,rels[0],'RELATION')['data']);change['fields'][0]['nullRule']='TEST quarantine'
race('mappingVsQC',verification(rels[0],'QC'),(w.root+'/relation/save',{**w.ctx(p,rels[0],'RELATION'),'relation':change},editor));assert w.available(p)==1
restore()
race('deliveryVsQC',verification(rels[0],'QC'),(w.root+'/source/supply',{**w.ctx(p,src,'SOURCE'),'supply':'PROVIDED','replaceDelivery':True,'deliveryDigest':s['sampleDigest'],'deliveryReference':'TEST replacement D2'},editor));assert w.available(p)==0
restore()
# Make confirmation eligible, then race a new source semantic change.
w.save_mapping(p,rels[0],lambda x:x['fields'][0].update(transform='TEST round(hours/60,2)'))
change=copy.deepcopy(w.detail(p,src,'SOURCE')['data']);change['specification']='TEST concurrent SPEC V3'
request=(w.rp+'/confirm',{'projectId':p,'projectScopeVersion':1,'id':rs[0],'version':w.req(p,rs[0])['version']},reviewer)
race('sourceVsConfirmation',request,(w.root+'/source/save',{**w.ctx(p,src,'SOURCE'),'source':change},editor));assert w.req(p,rs[0])['status']=='REVIEWING'
restore()
# Issue-close, verify and scope changes share the project lock. Scope-first old close is rejected.
i=w.issue(p,rs,src);w.ia(p,i,'START');w.ia(p,i,'SUBMIT')
close=(w.root+'/issue/action',{**w.ctx(p,i,'ISSUE'),'action':'CLOSE','evidence':'TEST independent close','expectedBinding':w.detail(p,i,'ISSUE')['binding']},reviewer)
scope=copy.deepcopy(s['scope']);scope['entities']=[101,102]
race('scopeVsIssueClose',close,(w.pp+'/scope',{'projectId':p,'expectedVersion':1,'scopeJson':json.dumps(scope),'reason':'TEST concurrent scope change'},editor));assert w.available(p)==0
w.deny(w.root+'/verify',{**verification(src,'SOURCE_SEMANTIC')[1],'projectScopeVersion':1},editor)
w.deny(w.root+'/issue/action',{**close[1],'version':w.detail(p,i,'ISSUE')['version']},reviewer)
result['scopeVsOldVerificationAndCloseDenied']=True
result['fixtures']=w.FIXTURES
Path(a.output).write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'securityAndRaceCases':list(result)}))
