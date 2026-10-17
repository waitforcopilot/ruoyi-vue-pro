#!/usr/bin/env python3
"""Verify mid-month salary approval, stale-source freeze rejection and bonus version history."""
import argparse
import calendar
import datetime
from decimal import Decimal, ROUND_HALF_UP
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
from runtime_client import RuntimeClient

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--base-state',required=True)
p.add_argument('--state-file',required=True)
p.add_argument('--output-dir',type=Path,required=True)
a=p.parse_args()
root=Path(__file__).resolve().parents[2]
state=Path(a.state_file).resolve()
if root in state.parents or state.is_symlink():p.error('Credentials must stay outside repository')
base=json.loads(Path(a.base_state).read_text())
if not state.exists():
    today=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).date()
    keep={'e2eTaxRuleId','e2eSalaryGroupId','e2eTaxPolicyId'}
    if today.year==base['fixtureYear']:keep.add('e2eCostRule')
    employee=base['scopeEmployeeIds'][0]
    keep.update(f'e2e{kind}{employee}' for kind in ('SalaryProposal','WorkRule','TaxProfile'))
    cloned={k:v for k,v in base.items() if not k.startswith('e2e') or k in keep}
    cloned['historyBatchId']=base['batchId'];cloned['historyFilingId']=base['taxFilingId']
    cloned.pop('batchId',None)
    cloned.update(fixtureYear=today.year,fixtureMonth=today.month,fixturePayrollYear=today.year,fixturePayrollMonth=today.month,fixturePaymentDate=today.isoformat(),batchTitle='隔离月中调薪与奖金验收工资表')
    fd=os.open(state,os.O_CREAT|os.O_EXCL|os.O_WRONLY,0o600)
    with os.fdopen(fd,'w') as out:json.dump(cloned,out)
c=RuntimeClient(state);s=c.state
entity=s['entityId'];employee=s['scopeEmployeeIds'][0]
assert len(s['scopeEmployeeIds'])==1
Y,M=s['fixtureYear'],s['fixtureMonth'];days=calendar.monthrange(Y,M)[1]
start=f'{Y}-{M:02d}-01';effect=f'{Y}-{M:02d}-16';end=f'{Y}-{M:02d}-{days:02d}'
review=c.login('hrmacceptreview','reviewerPassword');finance=c.login('hrmacceptoperator','browserPassword')
reason='虚构月中调薪/奖金金标及来源版本验收，不作正式规则签字或机构付款'
a.output_dir.mkdir(parents=True,exist_ok=True)
D=Decimal

def rounded(value):return value.quantize(D('0.01'),rounding=ROUND_HALF_UP)
def remember(key,fn):
    if key not in s:s[key]=fn();c.save()
    return s[key]
def q(path,params,token=review):return c.query(path,params,token)
def run_phase(phase):subprocess.run([sys.executable,str(root/'scripts/hrm/runtime_payroll_regression.py'),'--state-file',str(state),'--output-dir',str(a.output_dir),'--phase',phase],check=True)
def latest(batch):return c.ok(f'/hrm/payroll/batch/versions?batchId={batch}')[0]
def decision(batch,run,action,token):return c.ok('/hrm/payroll/batch/transition/'+action,{'batchId':batch,'runId':run,'reason':reason},token)
def approve_run(batch,run):
    for status,action,token in [(11,'submit',c.token),(12,'review',review),(13,'approve',finance)]:
        if c.ok(f'/hrm/salary/month-record/get?id={batch}')['status']==status:decision(batch,run,action,token)
def check(run,gross,tax,net):
    row=json.loads(run['resultSnapshot'])[0]
    assert D(str(row['expectedPaySalary']))==gross,row['expectedPaySalary']
    assert D(str(row['personalTax']))==tax,row['personalTax']
    assert D(str(row['realPaySalary']))==net,row['realPaySalary']
    return row

# New-year membership, work and insurance coverage are independent approved sources.
def coverage(year):
    key=f'adjustCoverage{year}'
    if year==base['fixtureYear']:return
    if s.get(key):return
    member=remember(key+'Member',lambda:c.ok('/hrm/payroll/entity/assign',{'entityId':entity,'employeeId':employee,'deptId':103,'employeeCategory':'REGULAR','workCity':'验收城市','effectiveFrom':f'{year}-01-01','effectiveTo':f'{year}-12-31','evidence':reason}))
    if not s.get(key+'MemberApproved'):
        q('/hrm/payroll/entity/membership/approve',{'id':member,'reason':reason});s[key+'MemberApproved']=True;c.save()
    work=remember(key+'Work',lambda:c.ok('/hrm/payroll/work/rule',{'entityId':entity,'employeeId':employee,'systemType':'STANDARD','effectiveFrom':f'{year}-01-01','effectiveTo':f'{year}-12-31','legalBasis':reason,'cycleMinutes':9600,'cycleMonths':1,'timeOffExpiryDays':180,'limitMinutes':2160,'hourlyBase':50,'workdayRate':1.5,'restdayRate':2,'holidayRate':3}))
    if not s.get(key+'WorkApproved'):
        q('/hrm/payroll/work/rule/approve',{'id':work});s[key+'WorkApproved']=True;c.save()
    policy=remember(key+'Policy',lambda:c.ok('/hrm/payroll/insurance-policy/policy',{'entityId':entity,'code':f'ACCEPT_COVERAGE_{year}','city':'验收城市','effectiveFrom':f'{year}-01-01','effectiveTo':f'{year}-12-31','evidence':reason,'projects':[{'code':'PENSION','name':'虚构养老','type':1,'minimumBase':3000,'maximumBase':20000,'personalRate':8,'corporateRate':16}]}))
    if not s.get(key+'PolicyApproved'):
        q('/hrm/payroll/insurance-policy/policy/approve',{'id':policy,'reason':reason});s[key+'PolicyApproved']=True;c.save()
    base_id=remember(key+'Base',lambda:c.ok('/hrm/payroll/insurance-policy/base',{'entityId':entity,'employeeId':employee,'policyId':policy,'coverage':'INSURED','incomeYear':year-1,'effectiveFrom':f'{year}-01-01','effectiveTo':f'{year}-12-31','declaredBases':{'PENSION':5000},'evidence':reason}))
    for flag,path,token in [('Reviewed','review',review),('Approved','approve',finance)]:
        if not s.get(key+flag):q('/hrm/payroll/insurance-policy/base/'+path,{'id':base_id,'reason':reason},token);s[key+flag]=True;c.save()
    if year==Y:s[f'e2eWorkRule{employee}']=work
    s[key]=True;c.save()

coverage(Y)
if Y!=base['fixtureYear']:
    def policy_request():
        body=c.ok(f'/hrm/payroll/tax-policy/defaults?entityId={entity}&year={Y}');body.update(requestKey=f'ACCEPT_ADJUST_POLICY_{entity}_{Y}',officialReference=reason,evidence=reason)
        return c.ok('/hrm/payroll/tax-policy/create',body)
    policy=remember('adjustAnnualPolicy',policy_request)
    if not s.get('adjustAnnualPolicyApproved'):q('/hrm/payroll/tax-policy/review',{'id':policy,'approved':True,'reason':reason});s['adjustAnnualPolicyApproved']=True;c.save()
    opening=(datetime.date(Y,M,1)-datetime.timedelta(days=1)) if M>1 else datetime.date(Y,1,1)
    prior=M-1
    profile=remember('adjustAnnualProfile',lambda:c.ok('/hrm/payroll/tax-profile/create',{'entityId':entity,'employeeId':employee,'year':Y,'requestKey':f'ACCEPT_ADJUST_PROFILE_{employee}_{Y}','taxpayerName':'隔离历史工资员工','identifierType':'PASSPORT','identifierCountry':'CN','identitySourceProfileId':base[f'e2eTaxProfile{employee}'],'residentThisYear':True,'noPriorContinuousRemunerationBeforeEntry':False,'firstSalaryThisYear':False,'noPriorSalaryThisYear':False,'separateBonusUsedElsewhere':False,'openingMode':'DECLARE' if prior else 'ZERO','openingAsOfDate':opening.isoformat(),'openingIncome':6000*prior,'openingSocialDeduction':400*prior,'openingBasicDeduction':5000*prior,'openingWithheld':18*prior,'officialReference':reason,'evidence':reason}))
    if not s.get('adjustAnnualProfileApproved'):q('/hrm/payroll/tax-profile/review',{'id':profile,'approved':True,'reason':reason});s['adjustAnnualProfileApproved']=True;c.save()
    s['e2eTaxPolicyId']=policy;s[f'e2eTaxProfile{employee}']=profile;c.save()

old_run=latest(s['historyBatchId'])
old_hash=hashlib.sha256(old_run['resultSnapshot'].encode()).hexdigest()
proposal=remember('adjustProposalId',lambda:c.ok('/hrm/salary/employee-info/update',{'employeeId':employee,'recordType':2,'changeReason':6,'effectTime':effect+'T00:00:00','remark':reason,'salaryOptions':[{'code':10101,'value':9000}],'probationSalaryOptions':[{'code':10101,'value':9000}]},method='PUT'))
status=next(row['status'] for row in c.ok('/hrm/payroll/salary-proposal/list') if row['id']==proposal)
if status=='HR_REVIEW':c.ok('/hrm/payroll/salary-proposal/review',{'id':proposal,'reason':reason},review);status='FINANCE_REVIEW'
if status=='FINANCE_REVIEW':c.ok('/hrm/payroll/salary-proposal/approve',{'id':proposal,'reason':reason},finance)
batch=remember('batchId',lambda:c.ok('/hrm/salary/month-record/create',{'entityId':entity,'year':Y,'month':M,'title':s['batchTitle'],'startTime':start+'T00:00:00','endTime':end+'T23:59:59'}))
plan=remember('adjustFinancialPlan',lambda:c.ok('/hrm/payroll/financial/create',{'entityId':entity,'year':Y,'month':M,'dimension':'ENTITY','dimensionKey':'ALL','budget':100000,'revenue':500000,'headcount':20,'evidence':reason,'sourceReference':'ACCEPT_ADJUST','requestKey':f'ACCEPT_ADJUST_PLAN_{batch}'}))
if not s.get('adjustPlanApproved'):q('/hrm/payroll/financial/review',{'id':plan,'approved':True,'reason':reason});s['adjustPlanApproved']=True;c.save()
gross=rounded((D(6000)*15+D(9000)*(days-15))/days)
regular_tax=rounded((gross-D(5400))*D('0.03'))
regular_net=gross-400-regular_tax
s['payrollScenarios']={str(employee):{'entryDate':base['payrollScenarios'][str(employee)]['entryDate'],'expected':{'gross':str(gross),'tax':str(regular_tax),'net':str(regular_net)}}}
c.save()
if 'adjustInitialRun' not in s:
    run_phase('sources')
    q('/hrm/salary/month-record/compute',{'id':batch},c.token)
    initial=latest(batch);check(initial,gross,regular_tax,regular_net)
    s['adjustInitialRun']=initial['id'];s['adjustInitialResultHash']=hashlib.sha256(initial['resultSnapshot'].encode()).hexdigest();c.save()
bonus=remember('adjustBonusId',lambda:c.ok('/hrm/payroll/bonus/create',{'batchId':batch,'employeeId':employee,'amount':36000,'mode':'SEPARATE','requestKey':f'ACCEPT_BONUS_{batch}','evidence':reason}))
if not s.get('adjustBonusApproved'):q('/hrm/payroll/bonus/review',{'id':bonus,'approved':True,'reason':reason});s['adjustBonusApproved']=True;c.save()
if not s.get('adjustStaleFreezeRejected'):
    approve_run(batch,s['adjustInitialRun'])
    rejected=c.request('/hrm/payroll/batch/transition/freeze',{'batchId':batch,'runId':s['adjustInitialRun'],'reason':reason})
    assert rejected['code'] not in (0,500),rejected
    assert c.ok(f'/hrm/salary/month-record/get?id={batch}')['status']==14
    decision(batch,s['adjustInitialRun'],'reject',finance)
    s['adjustStaleFreezeRejected']=True;c.save()
if 'adjustBonusRun' not in s:
    q('/hrm/salary/month-record/compute',{'id':batch},c.token)
    changed=latest(batch);check(changed,gross+36000,regular_tax+1080,regular_net+34920)
    s['adjustBonusRun']=changed['id'];c.save()
s['payrollScenarios'][str(employee)]['expected']={'gross':str(gross+36000),'tax':str(regular_tax+1080),'net':str(regular_net+34920)}
c.save()
run_phase('payroll')
# Another batch may use merged assessment, but cannot reuse the paid annual separate quota.
if not s.get('adjustQuotaControlPassed'):
    following=(datetime.date(Y,M,28)+datetime.timedelta(days=4)).replace(day=1)
    coverage(following.year)
    candidate=remember('adjustQuotaBatch',lambda:c.ok('/hrm/salary/month-record/create',{'entityId':entity,'year':following.year,'month':following.month,'title':'隔离年度奖金重复占用对照批次'}))
    c.ok('/hrm/payroll/batch/payment-plan',{'batchId':candidate,'expectedPaymentDate':s['fixturePaymentDate'],'reason':reason})
    for kind,mode,values in [('ATTENDANCE','IMPORT',dict(ACTUAL_DAYS=22,NEED_DAYS=22,**{str(code):0 for code in range(190101,190107)})),('OVERTIME','ZERO',{}),('DEDUCTIONS','SYNC',{}),('CUMULATIVE_TAX','CASH',{})]:
        input_id=remember('adjustQuotaInput'+kind,lambda:c.ok('/hrm/payroll/input/submit',{'batchId':candidate,'employeeId':employee,'kind':kind,'mode':mode,'values':values,'evidence':reason}))
        item=next(row for row in c.ok(f'/hrm/payroll/input/list?batchId={candidate}') if row['id']==input_id)
        if item['status']!='APPROVED':q('/hrm/payroll/input/review',{'id':input_id,'approve':True,'reason':reason})
    duplicate=remember('adjustDuplicateBonus',lambda:c.ok('/hrm/payroll/bonus/create',{'batchId':candidate,'employeeId':employee,'amount':6000,'mode':'SEPARATE','requestKey':f'ACCEPT_DUPLICATE_BONUS_{candidate}','evidence':reason}))
    reviewed=c.request(f'/hrm/payroll/bonus/review?id={duplicate}&approved=true&reason=synthetic-quota-test',{},review)
    if reviewed['code']==0:
        blocked=c.request(f'/hrm/salary/month-record/compute?id={candidate}',{})
        assert blocked['code']==1050810020,blocked
        prior=duplicate
    else:
        assert reviewed['code']==1050810020,reviewed
        q('/hrm/payroll/bonus/review',{'id':duplicate,'approved':False,'reason':reason})
        prior=None
    merged=remember('adjustMergedControl',lambda:c.ok('/hrm/payroll/bonus/create',{'batchId':candidate,'employeeId':employee,'priorBonusId':prior,'amount':6000,'mode':'MERGED','requestKey':f'ACCEPT_MERGED_CONTROL_{candidate}','evidence':reason}))
    q('/hrm/payroll/bonus/review',{'id':merged,'approved':True,'reason':reason})
    q('/hrm/salary/month-record/compute',{'id':candidate},c.token)
    check(latest(candidate),D(15000),D(438),D(14162))
    s['adjustQuotaControlPassed']=True;c.save()
versions=c.ok(f'/hrm/payroll/batch/versions?batchId={batch}')
assert len(versions)==2
initial=next(row for row in versions if row['id']==s['adjustInitialRun'])
assert hashlib.sha256(initial['resultSnapshot'].encode()).hexdigest()==s['adjustInitialResultHash']
assert hashlib.sha256(latest(s['historyBatchId'])['resultSnapshot'].encode()).hexdigest()==old_hash
assert c.ok(f"/hrm/payroll/tax-filing/detail?id={s['historyFilingId']}")['sourceFresh']
assert c.ok(f"/hrm/salary/month-record/get?id={s['adjustQuotaBatch']}")['status']==11
assert c.ok(f"/hrm/payroll/payment/list?batchId={s['adjustQuotaBatch']}")==[]
claims=c.ok(f'/hrm/payroll/batch/source-claims?batchId={batch}')
assert any(row['kind']=='BONUS_YEAR' and row['status']=='PAID' for row in claims),claims
(a.output_dir/'salary-bonus-runtime.json').write_text(json.dumps({'batchId':batch,'employeeId':employee,'effectDate':effect,'regularBefore':6000,'regularAfter':9000,'periodDays':days,'salaryBeforeBonus':str(gross),'taxBeforeBonus':str(regular_tax),'netBeforeBonus':str(regular_net),'approvedBonus':36000,'separateBonusTax':1080,'salaryAfterBonus':str(gross+36000),'taxAfterBonus':str(regular_tax+1080),'netAfterBonus':str(regular_net+34920),'initialRunId':s['adjustInitialRun'],'bonusRunId':s['adjustBonusRun'],'staleFreezeRejected':True,'priorRunUnchanged':True,'priorPaidBatchUnchanged':True,'priorFilingStillFresh':True,'annualBonusClaim':'PAID','duplicateSeparateBonusRejected':s['adjustQuotaControlPassed'],'quotaControlBatchId':s['adjustQuotaBatch'],'quotaControlStatus':c.ok(f"/hrm/salary/month-record/get?id={s['adjustQuotaBatch']}")['status'],'mergedControlGross':15000,'mergedControlTax':438,'mergedControlNet':14162},ensure_ascii=False,indent=2))
print('Passed: approved mid-month adjustment, stale-source freeze rejection, bonus re-computation, paid annual quota and immutable prior versions')
