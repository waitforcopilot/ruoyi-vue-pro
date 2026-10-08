#!/usr/bin/env python3
"""Verify explicit scheme/item binding with real saved trials and BPM in guarded synthetic QA."""
import copy,json,runpy,uuid
from pathlib import Path
prior=runpy.run_path(str(Path(__file__).with_name('verify-overview-api.py')),run_name='__main__')
core=prior['prior'];a=prior['a'];ok=prior['ok'];call=prior['call'];sql=prior['sql'];trial=core['trial'];review=core['review'];entity=core['entity'];checks=[]
schemes='hrm/payroll/schemes';calculation='hrm/payroll/calculation-definitions';overview='hrm/payroll/overview'
def passed(name,condition=True):
 assert condition,name;checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
assert sql("SELECT COUNT(*) FROM hrm_salary_option WHERE id=950000002 AND tenant_id=1 AND creator='scheme-verification' AND enabled=1")=='1','prepare the synthetic scheme source catalogue first'
group=985000000+int(uuid.uuid4().hex[:5],16)
sql("INSERT INTO hrm_salary_group(id,name,salary_standard,change_rule,tax_rule_id,employee_ids,dept_ids,creator,updater,tenant_id) VALUES("+str(group)+",'绑定合成方案组',10.00,'合成配置依据',950000001,'[]','[]','binding-verification','binding-verification',1)")
scheme=ok('/create',module=schemes,method='POST',data={'groupId':group,'title':'绑定合成方案','ownerName':'QA','reference':'方案技术回归依据，非实际工资制度','effectiveFrom':'2026-10-01','effectiveTo':'2026-10-31'})
def scheme_review(id,action):
 row=ok('/get',module=schemes,params={'id':id});return ok('/review',module=schemes,method='POST',data={'id':id,'revision':row['revision'],'action':action,'evidence':'合成'+action+'依据'})
scheme_review(scheme,'confirm');snapshot=ok('/get',module=schemes,params={'id':scheme})
rule=ok('/get',module=calculation,params={'id':core['definition']})
def draft(name):
 body=copy.deepcopy(core['batch'](prior['saved']))
 body={k:body[k] for k in ['entityCode','entityName','periodType','periodStart','periodEnd','definitionId','ownerName','reference','configuration']}
 body.update(code='BINDING-'+name+'-'+uuid.uuid4().hex[:8].upper(),title='BINDING '+name,schemeId=scheme)
 body['configuration']['sourceBindings']=[{'inputKey':i['key'],'sourceType':'MANUAL','unit':i['unit'],'reference':'合成独立来源 '+i['label']} for i in rule['program']['inputs']]
 body['configuration']['sourceBindings'][0].update(sourceType='SCHEME_ITEM',optionId=950000002,reference='合成工资项金额由核定工资约定录入')
 return body
def create(name,run=True):
 id=ok('/create',module=trial,role='maker',method='POST',data=draft(name))
 if run:core['execute'](id)
 return id
def batch(id):return core['batch'](id)
def run(id):return ok('/run',module=trial,role='maker',params={'id':batch(id)['currentRunId']})
def inspect(id):return ok('/batch',module=overview,role='maker',params={'id':id})
def command(id,action):
 b=batch(id);v=ok('/get',module=review,role='maker',params={'batchId':id});return {'batchId':id,'runId':b.get('currentRunId') or b['latestRunId'],'revision':v['revision'],'cycleId':None if action=='submit' else v.get('activeReviewId'),'requestKey':'binding-'+uuid.uuid4().hex,'action':action,'evidence':'合成'+action+'依据'}
def submit(id,hr='hr'):
 return ok('/action',module=review,method='POST',role='maker',data={**command(id,'submit'),'hrReviewerId':core['ids'][hr],'financeReviewerId':core['ids']['finance']})
def decide(id,role):
 v=ok('/get',module=review,role=role,params={'batchId':id});return ok('/action',module=review,method='POST',role=role,data={**command(id,'approve'),'taskId':v['tasks'][0]['id']})
main=create('MAIN');saved=run(main);passed('confirmed scheme version and every typed source are saved with exact wage result',saved['result']['scheme']['id']==scheme and len(saved['result']['batch']['configuration']['sourceBindings'])==9 and saved['result']['totals']['net']=='6200.00')
passed('saved ISO scheme dates survive persistent read and current manifest',saved['result']['scheme']==snapshot and inspect(main)['availability']=='CURRENT' and not snapshot['capturedAt'].startswith('1970-'))
negative=[]
body=draft('NO-SOURCES');body['configuration'].pop('sourceBindings');negative.append(('missing source bindings',body))
body=draft('UNKNOWN');body['configuration']['sourceBindings'][0]['inputKey']='unknown';negative.append(('unknown input binding',body))
body=draft('DUPLICATE');body['configuration']['sourceBindings'][1]['inputKey']='baseSalary';negative.append(('duplicate input binding',body))
body=draft('DISABLED');body['configuration']['sourceBindings'][0]['optionId']=999999999;negative.append(('foreign or missing scheme item',body))
body=draft('DUP-ITEM');body['configuration']['sourceBindings'][1].update(sourceType='SCHEME_ITEM',optionId=950000002);negative.append(('repeated scheme item',body))
body=draft('UNIT');body['configuration']['sourceBindings'][0]['unit']='USD';negative.append(('wrong source unit',body))
body=draft('BASIS');body['configuration']['sourceBindings'][0]['reference']=' ';negative.append(('missing source basis',body))
body=draft('MANUAL-ID');body['configuration']['sourceBindings'][1]['optionId']=950000002;negative.append(('manual source carries item ID',body))
body=draft('ALL-MANUAL');body['configuration']['sourceBindings'][0].update(sourceType='MANUAL',optionId=None);negative.append(('nominal scheme without any item association',body))
body=draft('NO-SCHEME');body.pop('schemeId');negative.append(('source bindings without scheme',body))
body=draft('OTHER-ENTITY');body['entityCode']='BINDING-OTHER';negative.append(('definition entity mismatch',body))
body=draft('PARTIAL-PERIOD');body.update(periodType='CUSTOM',periodStart='2026-09-30');negative.append(('scheme does not cover whole period',body))
for name,body in negative:passed(name+' cannot create',call('/create',module=trial,role='maker',method='POST',data=body)['code']!=0)
for role in ['reader','wronghr']:
 passed(role+' lacking policy permission cannot read batch or saved run',call('/get',module=trial,role=role,params={'id':main})['code']==1050990006 and call('/run',module=trial,role=role,params={'id':saved['id']})['code']==1050990006)
 for path,module in [('/page',trial),('/page',overview)]:
  result=ok(path,module=module,role=role,params={'entityCode':entity,'search':'BINDING'});passed(role+' cannot count bound batches via '+module,result['total']==0)
passed('legacy independent input batch remains readable without policy permission',ok('/get',module=trial,role='reader',params={'id':prior['saved']})['id']==prior['saved'])
passed('whole historical employee scope remains required',call('/get',module=trial,role='dept',params={'id':main})['code']==1050990000)
passed('foreign tenant cannot read captured scheme via trial',call('/run',module=trial,role='tenantb',params={'id':saved['id']})['code']==1050990000)
passed('inadequate reviewer policy access prevents native process creation',call('/action',module=review,role='maker',method='POST',data={**command(main,'submit'),'hrReviewerId':core['ids']['wronghr'],'financeReviewerId':core['ids']['finance']})['code']==1050991001)
submitted=submit(main);passed('real BPM submission validates the persistent scheme manifest',submitted['batchStatus']==2 and submitted['cycle']['sourceHash'] and submitted['cycle']['runId']==saved['id'])
hr=decide(main,'hr');passed('bound scheme proceeds through actual HR and finance tasks',hr['tasks'][0]['key']=='financeReview')
finance=decide(main,'finance');passed('native terminal event approves captured scheme version',finance['batchStatus']==3)
frozen=ok('/action',module=review,role='finance',method='POST',data=command(main,'freeze'));passed('freeze retains stable scheme and exact input results',frozen['batchStatus']==4 and run(main)['result']==saved['result'] and inspect(main)['availability']=='CURRENT')
retire_batch=create('RETIRE');retire_original=run(retire_batch);retire_approved=create('RETIRE-FREEZE');submit(retire_approved);decide(retire_approved,'hr');decide(retire_approved,'finance');pending=create('RETIRE-APPROVE');submit(pending)
source=sql("SELECT name,CAST(enabled AS UNSIGNED),type FROM hrm_salary_option WHERE id=950000002 AND tenant_id=1").split('\t')
try:
 sql("UPDATE hrm_salary_option SET name='源目录后来变化',enabled=0,type=0 WHERE id=950000002 AND tenant_id=1")
 passed('mutable catalogue changes preserve the selected confirmed snapshot',inspect(retire_batch)['availability']=='CURRENT' and run(retire_batch)['result']['scheme']==snapshot)
finally:
 sql("UPDATE hrm_salary_option SET name='"+source[0].replace("'","''")+"',enabled="+source[1]+",type="+source[2]+" WHERE id=950000002 AND tenant_id=1")
scheme_review(scheme,'retire')
passed('retirement blocks new trial but preserves old immutable amounts',not ok('/check',module=trial,role='maker',params={'id':retire_batch})['ready'] and inspect(retire_batch)['availability']=='SOURCE_CHANGED' and run(retire_batch)['result']==retire_original['result'])
passed('retirement blocks a later approval in existing BPM',call('/action',module=review,role='hr',method='POST',data={**command(pending,'approve'),'taskId':ok('/get',module=review,role='hr',params={'batchId':pending})['tasks'][0]['id']})['code']==1050991003)
passed('retirement blocks freeze after both stages passed',call('/action',module=review,role='finance',method='POST',data=command(retire_approved,'freeze'))['code']==1050991003)
for id in [pending]:ok('/action',module=review,role='maker',method='POST',data=command(id,'cancel'))
new_scheme=ok('/new-version',module=schemes,method='POST',params={'id':scheme,'revision':ok('/get',module=schemes,params={'id':scheme})['revision']});scheme_review(new_scheme,'confirm')
body=batch(retire_batch);body['schemeId']=new_scheme;body['configuration']['sourceBindings'][0]['reference']='新方案项目口径的合成依据';ok('/update',module=trial,role='maker',method='PUT',data=body)
passed('changing scheme clears current run and preserves historical selected version',inspect(retire_batch)['availability']=='INVALIDATED' and ok('/run',module=trial,role='maker',params={'id':retire_original['id']})['result']['scheme']['id']==scheme)
core['execute'](retire_batch);new_run=run(retire_batch);comparison=ok('/compare',module=trial,role='maker',params={'leftId':retire_original['id'],'rightId':new_run['id']})
passed('new trial stores selected scheme V2 and identifies policy changes with equal wages',new_run['result']['scheme']['id']==new_scheme and new_run['result']['scheme']['schemeVersion']==2 and comparison['ruleChanged'] and all(v=='0.00' for v in comparison['totalDifferences'].values()))
cleared=batch(retire_batch);cleared['schemeId']=None;cleared['configuration']['sourceBindings']=None
passed('binding cannot be removed to bypass old snapshot permissions',call('/update',module=trial,role='maker',method='PUT',data=cleared)['code']!=0)
before_read=core['state_count'](retire_batch);inspect(retire_batch);run(retire_batch);passed('bound GET keeps business state and audits unchanged',before_read==core['state_count'](retire_batch))
passed('all seven existing salary insurance slip and bank tables stay unchanged',core['before']==core['legacy_hash']())
ui_body=draft('BROWSER');ui_body['schemeId']=new_scheme;ui=ok('/create',module=trial,role='maker',method='POST',data=ui_body);core['execute'](ui)
unbound_body=draft('UI-UNBOUND');unbound_body.pop('schemeId');unbound_body['configuration'].pop('sourceBindings');unbound_ui=ok('/create',module=trial,role='maker',method='POST',data=unbound_body)
fixture={'synthetic':True,'realBpmEngine':True,'entityCode':entity,'batchId':ui,'batchCode':batch(ui)['code'],'schemeId':new_scheme,'schemeTitle':ok('/get',module=schemes,params={'id':new_scheme})['title'],'schemeVersion':2,'oldBatchId':main,'oldRunId':saved['id'],'newBatchId':retire_batch,'oldSchemeId':scheme,'optionId':950000002,'optionName':next(o['name'] for o in snapshot['snapshot']['options'] if o['id']==950000002),'gross':'7800.00','net':'6200.00'}
fixture.update(unboundBatchId=unbound_ui,unboundBatchCode=batch(unbound_ui)['code'],employeeId=core['person'],inputKeys=[i['key'] for i in rule['program']['inputs']])
(a.output_dir/'hrm-binding-browser-fixture.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2)+'\n')
(a.output_dir/'hrm-binding-api-results.json').write_text(json.dumps({'total':len(checks),'passed':len(checks),'checks':checks,'priorReviewChecks':55,'priorOverviewChecks':30,'syntheticFixtures':True,'realBpmEngine':True,'legacyUnchanged':True},ensure_ascii=False,indent=2)+'\n')
print('PASS:',len(checks),'binding API checks plus 55 review and 30 overview checks')
