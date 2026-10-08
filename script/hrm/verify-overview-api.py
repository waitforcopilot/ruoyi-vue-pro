#!/usr/bin/env python3
"""Verify overview against real native BPM; also rerun the prior review module in isolated loopback QA."""
import copy,json,runpy,uuid
from pathlib import Path

# Reuse the version-bound review verifier's explicit fixture/network/database guards and token inputs.
# It executes its complete 55-check regression before exposing the same synthetic QA helpers.
prior=runpy.run_path(str(Path(__file__).with_name('verify-review-api.py')),run_name='__main__')
a=prior['a'];ok=prior['ok'];call=prior['call'];sql=prior['sql'];create=prior['create'];trial=prior['trial'];review=prior['review'];entity=prior['entity'];checks=[]
overview='hrm/payroll/overview'
def passed(name,condition=True):
 assert condition,name;checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def page(role='reader',**params):return ok('/page',module=overview,role=role,params={'entityCode':entity,'search':'OVERVIEW',**params})
def detail(id,role='reader'):return ok('/batch',module=overview,role=role,params={'id':id})
def clone_draft(id,name):
 b=ok('/get',module=trial,params={'id':id},role='maker')
 body={k:copy.deepcopy(b[k]) for k in ['entityCode','entityName','periodType','periodStart','periodEnd','definitionId','ownerName','reference','configuration']}
 body.update(code='OVERVIEW-'+name+'-'+uuid.uuid4().hex[:8].upper(),title='OVERVIEW '+name)
 return ok('/create',module=trial,role='maker',method='POST',data=body)
saved=create('OVERVIEW-SAVED');draft=clone_draft(saved,'NO-VERSION')
hr=create('OVERVIEW-HR');prior['submit'](hr)
finance=create('OVERVIEW-FINANCE');prior['submit'](finance);prior['decision'](finance,'hr')
approved=create('OVERVIEW-APPROVED');prior['approve'](approved)
frozen=create('OVERVIEW-FROZEN');prior['approve'](frozen);prior['send'](prior['command'](frozen,'freeze'),'finance')
invalidated=create('OVERVIEW-INVALIDATED');old=ok('/get',module=trial,params={'id':invalidated},role='maker');edit=copy.deepcopy(old);edit['title']='OVERVIEW INVALIDATED';ok('/update',module=trial,role='maker',method='PUT',data=edit)
expected={'0':2,'1':1,'2':2,'3':1,'4':1}
r=page(pageSize=1);passed('all five status counts are exact and independent of pagination',r['total']==7 and r['batches']['total']==7 and len(r['batches']['list'])==1 and r['states']==expected)
passed('HR assigned count comes from current native task outside first page',page('hr',pageSize=1)['assignedHr']==1 and page('hr')['assignedFinance']==0)
passed('finance assigned count comes from native finance stage',page('finance')['assignedFinance']==1 and page('finance')['assignedHr']==0)
passed('read-only actor has no fabricated personal tasks',page()['assignedHr']==page()['assignedFinance']==0)
rows={b['id']:b for b in page(pageSize=100)['batches']['list']}
passed('batch stages reflect real HR and finance tasks',rows[hr]['stage']=='HR_REVIEW' and rows[finance]['stage']=='FINANCE_REVIEW' and rows[approved]['stage']=='CHECK_FREEZE' and rows[frozen]['stage']=='FROZEN')
passed('assignment badge uses the current logged-in actor',next(b for b in page('hr',pageSize=100)['batches']['list'] if b['id']==hr)['assignedToMe'] and not rows[hr]['assignedToMe'])
passed('status filter applies consistently to count and list',page(status=2)['total']==2 and page(status=2)['states']['2']==2 and all(b['status']==2 for b in page(status=2)['batches']['list']))
passed('exact complete period returns only matching batches',page(periodStart='2026-10-01',periodEnd='2026-10-31')['total']==7 and page(periodStart='2026-10-01',periodEnd='2026-10-30')['total']==0)
passed('unknown entity is a legitimate empty result',ok('/page',module=overview,role='reader',params={'entityCode':'OVERVIEW-NONE-'+uuid.uuid4().hex})['total']==0)
for params in [{'periodStart':'2026-10-01'},{'periodStart':'2026-10-31','periodEnd':'2026-10-01'},{'pageSize':-1},{'pageSize':201},{'status':5}]:
 passed('invalid query rejected '+','.join(params),call('/page',module=overview,role='reader',params=params)['code']!=0)
for role in [None,'none','submitonly','freezeonly','noelig']:
 passed(str(role)+' missing overview or combined permissions denied',call('/page',module=overview,role=role)['code'] in [401,403] and call('/batch',module=overview,role=role,params={'id':saved})['code'] in [401,403])
original=ok('/run',module=trial,params={'id':ok('/get',module=trial,params={'id':saved})['currentRunId']})
d=detail(saved);passed('single chosen version has exact stored decimal money without summing overlaps',d['availability']=='CURRENT' and d['runId']==original['id'] and d['runVersion']==1 and d['amounts']==original['result']['totals'])
passed('no saved trial displays missing money rather than invented zero',detail(draft)['availability']=='NONE' and not detail(draft).get('amounts') and not detail(draft).get('runId'))
passed('invalidated trial retains clearly marked historical amounts',detail(invalidated)['availability']=='INVALIDATED' and detail(invalidated)['runId']==old['currentRunId'] and detail(invalidated)['amounts']==original['result']['totals'])
passed('frozen workflow state still matches the original business sources',detail(frozen)['availability']=='CURRENT' and detail(frozen)['batch']['status']==4 and detail(frozen)['batch']['stage']=='FROZEN')
qualification=prior['qualification']
try:
 sql('UPDATE hrm_payroll_employee_eligibility SET reference=\'概览合成来源变化\' WHERE id='+str(qualification))
 changed=detail(saved);passed('source basis drift is detected even with identical money',changed['availability']=='SOURCE_CHANGED' and changed['check']['ready'] and changed['amounts']==d['amounts'])
finally:sql('UPDATE hrm_payroll_employee_eligibility SET reference=\'合成资格依据\' WHERE id='+str(qualification))
try:
 sql('UPDATE hrm_employee SET status=30 WHERE id=984100101')
 changed=detail(saved);passed('current personnel blocker preserves original saved money',changed['availability']=='SOURCE_CHANGED' and not changed['check']['ready'] and changed['check']['blockedCount']==1 and changed['amounts']==d['amounts'])
finally:sql('UPDATE hrm_employee SET status=20 WHERE id=984100101')
before=prior['state_count'](saved);legacy=prior['legacy_hash']();page();detail(saved);page();detail(saved)
passed('overview GET leaves revisions history and legacy ledgers unchanged',before==prior['state_count'](saved) and legacy==prior['legacy_hash']())
mixed=create('OVERVIEW-MIXED',True)
passed('self scope excludes whole mixed batch including explicitly excluded person',page('self',pageSize=100)['total']==7 and call('/batch',module=overview,role='self',params={'id':mixed})['code']!=0)
passed('other department cannot count or read unauthorized batches',page('dept')['total']==0 and page('dept')['assignedHr']==0 and call('/batch',module=overview,role='dept',params={'id':saved})['code']!=0)
passed('other tenant cannot read overview batch',call('/batch',module=overview,role='tenantb',params={'id':saved})['code']!=0)
passed('legacy wage tax insurance slip and bank tables remain unchanged',prior['before']==prior['legacy_hash']())
fixture={'synthetic':True,'realBpmEngine':True,'entityCode':entity,'savedId':saved,'draftId':draft,'hrId':hr,'financeId':finance,'frozenId':frozen,'invalidatedId':invalidated,'qualificationId':qualification,'periodStart':'2026-10-01','periodEnd':'2026-10-31','gross':d['amounts']['gross'],'net':d['amounts']['net'],'batchCodes':{str(id):ok('/get',module=trial,params={'id':id})['code'] for id in [saved,draft,hr,finance,frozen,invalidated]}}
(a.output_dir/'hrm-overview-browser-fixture.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2)+'\n')
(a.output_dir/'hrm-overview-api-results.json').write_text(json.dumps({'total':len(checks),'passed':len(checks),'checks':checks,'priorReviewChecks':55,'syntheticFixtures':True,'realBpmEngine':True,'legacyUnchanged':True},ensure_ascii=False,indent=2)+'\n')
print('PASS:',len(checks),'actual overview checks plus 55 prior review checks')
