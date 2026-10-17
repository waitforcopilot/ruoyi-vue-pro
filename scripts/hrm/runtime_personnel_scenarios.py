#!/usr/bin/env python3
"""Prepare isolated personnel or historical tax fixtures without rewriting archived wages."""
import argparse
import calendar
import datetime
from decimal import Decimal, ROUND_HALF_UP
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
from runtime_client import RuntimeClient

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--base-state',required=True)
p.add_argument('--state-file',required=True)
p.add_argument('--database',required=True)
p.add_argument('--output-dir',type=Path)
p.add_argument('--kind',choices=['three-person','tax-history'],default='three-person')
a=p.parse_args()
if not re.fullmatch(r'hrm_remediation_[A-Za-z0-9_]+',a.database):p.error('Only isolated remediation databases are supported')
state=Path(a.state_file).resolve()
root=Path(__file__).resolve().parents[2]
if root in state.parents or state.is_symlink():p.error('Keep credentials outside the repository')
if not state.exists():
    original=json.loads(Path(a.base_state).read_text())
    cloned={k:v for k,v in original.items() if not k.startswith('e2e') and k not in ('entityId','scopeEmployeeIds','scopePolicyId','batchId','financialPlanId')}
    previous=datetime.date.fromisoformat(original['fixturePaymentDate']).replace(day=1)-datetime.timedelta(days=1)
    cloned['fixturePayrollYear']=previous.year;cloned['fixturePayrollMonth']=previous.month
    if a.kind=='tax-history':
        cloned['fixtureYear']=previous.year;cloned['fixtureMonth']=previous.month;cloned['fixturePaymentDate']=previous.isoformat()
    fd=os.open(state,os.O_CREAT|os.O_EXCL|os.O_WRONLY,0o600)
    with os.fdopen(fd,'w') as out:json.dump(cloned,out)
c=RuntimeClient(state)
s=c.state
tax_year=s['fixtureYear']
year,month=s.get('fixturePayrollYear',s['fixtureYear']),s.get('fixturePayrollMonth',s['fixtureMonth'])
start=f'{year}-{month:02d}-01'
mid=f'{year}-{month:02d}-16'
end=f'{tax_year}-12-31'
days=calendar.monthrange(year,month)[1]
review=c.login('hrmacceptreview','reviewerPassword')
finance=c.login('hrmacceptoperator','browserPassword')
evidence='虚构隔离人员金标；按周期自然日加权，归属日期内全勤；非正式业务签字'
historical=a.kind=='tax-history'
subject_code='ACCEPT_TAX_HISTORY' if historical else 'ACCEPT_THREE_SCENARIOS'
subject_name='隔离历史工资申报主体（虚构）' if historical else '隔离三类人员验收主体（虚构）'

def remember(key,create):
    if key not in s:s[key]=create();c.save()
    return s[key]

entity=remember('entityId',lambda:c.ok('/hrm/payroll/entity/create',{'code':subject_code,'name':subject_name,'taxRegistrationNumber':('TEST'+subject_code.replace('_',''))[:20],'evidence':evidence}))
if next(row['status'] for row in c.ok('/hrm/payroll/entity/list') if row['id']==entity)!='APPROVED':
    c.query('/hrm/payroll/entity/approve',{'id':entity,'reason':evidence},review)
c.ok('/hrm/payroll/entity/grant',{'entityId':entity,'userId':s['browserUserId'],'actions':['READ','MANAGE','CALCULATE','REVIEW','APPROVE','EXPORT'],'effectiveFrom':(datetime.date.today()-datetime.timedelta(days=1)).isoformat(),'effectiveTo':(datetime.date.today()+datetime.timedelta(days=30)).isoformat(),'reason':evidence})
if not historical and not s.get('scenarioReaderCreated'):
    s['readerUsername']='hrmacceptscenario';s['readerPassword']=secrets.token_urlsafe(12)
    s['readerId']=c.ok('/system/user/create',{'username':s['readerUsername'],'nickname':'隔离三类人员工资条验收','password':s['readerPassword'],'postIds':[],'remark':evidence})
    s['scenarioReaderCreated']=True;c.save()
c.ok('/system/permission/assign-user-role',{'userId':s['readerId'],'roleIds':[s['readerRoleId']]})
if 'scopeEmployeeIds' not in s:
    if historical:
        sql=("INSERT INTO hrm_employee(name,job_number,user_id,dept_id,entry_status,status,type,entry_time,regular_time,post_name,work_city,remark,tenant_id) VALUES "
             f"('隔离历史工资员工','ACCEPT_TAX_HISTORY',NULL,103,1,1,1,'{year}-01-01','{year}-01-01','验收岗位','验收城市','虚构历史工资',1);"
             "SELECT id FROM hrm_employee WHERE job_number='ACCEPT_TAX_HISTORY';")
    else:
        sql=("INSERT INTO hrm_employee(name,job_number,user_id,dept_id,entry_status,status,type,entry_time,regular_time,post_name,work_city,remark,tenant_id) VALUES "
             f"('隔离普通工资员工','ACCEPT_SCENE_STANDARD',NULL,103,1,1,1,'{year}-01-01','{year}-01-01','验收岗位','验收城市','虚构三类人员',1),"
             f"('隔离月中转正员工','ACCEPT_SCENE_REGULAR',{int(s['readerId'])},104,1,1,1,'{start}','{mid}','验收岗位','验收城市','虚构三类人员',1),"
             f"('隔离月中入职员工','ACCEPT_SCENE_ENTRY',NULL,104,1,1,1,'{mid}','{mid}','验收岗位','验收城市','虚构三类人员',1);"
             "SELECT id FROM hrm_employee WHERE job_number IN ('ACCEPT_SCENE_STANDARD','ACCEPT_SCENE_REGULAR','ACCEPT_SCENE_ENTRY') ORDER BY id;")
    result=subprocess.check_output(['docker','exec','-i','codex-ruoyi-mysql','mysql','--default-character-set=utf8mb4','--batch','--skip-column-names',a.database],input=sql,text=True)
    s['scopeEmployeeIds']=[int(row) for row in result.splitlines()];c.save()
ids=s['scopeEmployeeIds']
assert len(ids)==(1 if historical else 3)
D=Decimal
mixed=((D(4500)*15+D(6000)*(days-15))/days).quantize(D('0.01'),rounding=ROUND_HALF_UP)
entry=(D(6000)*(days-15)/days).quantize(D('0.01'),rounding=ROUND_HALF_UP)
s['payrollScenarios']={str(ids[0]):{'name':'历史普通工资' if historical else '普通工资','entryDate':f'{year}-01-01','salaryEffectDate':start,'expected':{'gross':'6000','tax':'18','net':'5582'}}}
if not historical:
    s['payrollScenarios'].update({
        str(ids[1]):{'name':'月中转正且年度首次工资','entryDate':start,'salaryEffectDate':start,'probationSalary':4500,'regularSalary':6000,'firstSalary':True,'expected':{'gross':str(mixed),'tax':'0','net':str(mixed-400)}},
        str(ids[2]):{'name':'月中入职且年度首次工资','entryDate':mid,'salaryEffectDate':mid,'firstSalary':True,'actualDays':11,'needDays':11,'expected':{'gross':str(entry),'tax':'0','net':str(entry-400)}}})
s['batchTitle']='隔离历史工资申报验收工资表' if historical else '隔离三类人员验收工资表';c.save()
if a.output_dir:
    a.output_dir.mkdir(parents=True,exist_ok=True)
    (a.output_dir/'personnel-expected.json').write_text(json.dumps({'period':f'{year}-{month:02d}','paymentDate':s['fixturePaymentDate'],'policy':evidence,'scenarios':s['payrollScenarios']},ensure_ascii=False,indent=2))
for employee,dept in zip(ids,[103,104,104]):
    begin=s['payrollScenarios'][str(employee)]['entryDate']
    member=remember(f'scenarioMembership{employee}',lambda:c.ok('/hrm/payroll/entity/assign',{'entityId':entity,'employeeId':employee,'deptId':dept,'employeeCategory':'REGULAR','workCity':'验收城市','effectiveFrom':begin,'effectiveTo':end,'evidence':evidence}))
    if not s.get(f'scenarioMemberApproved{employee}'):
        c.query('/hrm/payroll/entity/membership/approve',{'id':member,'reason':evidence},review)
        s[f'scenarioMemberApproved{employee}']=True;c.save()
policy=remember('scopePolicyId',lambda:c.ok('/hrm/payroll/insurance-policy/policy',{'entityId':entity,'code':subject_code+'_POLICY','city':'验收城市','effectiveFrom':f'{year}-01-01','effectiveTo':end,'evidence':evidence,'projects':[{'code':'PENSION','name':'虚构养老','type':1,'minimumBase':3000,'maximumBase':20000,'personalRate':8,'corporateRate':16}]}))
if not s.get('scenarioPolicyApproved'):
    c.query('/hrm/payroll/insurance-policy/policy/approve',{'id':policy,'reason':evidence},review);s['scenarioPolicyApproved']=True;c.save()
for employee in ids:
    base=remember(f'scenarioInsurance{employee}',lambda:c.ok('/hrm/payroll/insurance-policy/base',{'entityId':entity,'employeeId':employee,'policyId':policy,'coverage':'INSURED','incomeYear':year-1,'effectiveFrom':s['payrollScenarios'][str(employee)]['entryDate'][:7]+'-01','effectiveTo':end,'declaredBases':{'PENSION':5000},'evidence':evidence}))
    if not s.get(f'scenarioInsuranceReviewed{employee}'):
        c.query('/hrm/payroll/insurance-policy/base/review',{'id':base,'reason':evidence},review);s[f'scenarioInsuranceReviewed{employee}']=True;c.save()
    if not s.get(f'scenarioInsuranceApproved{employee}'):
        c.query('/hrm/payroll/insurance-policy/base/approve',{'id':base,'reason':evidence},finance);s[f'scenarioInsuranceApproved{employee}']=True;c.save()
plan=remember('financialPlanId',lambda:c.ok('/hrm/payroll/financial/create',{'entityId':entity,'year':year,'month':month,'dimension':'ENTITY','dimensionKey':'ALL','budget':100000,'revenue':500000,'headcount':20,'evidence':evidence,'sourceReference':'ACCEPT_SCENARIOS','requestKey':f'ACCEPT_{entity}_FINANCIAL_SCENARIO'}))
if not s.get('scenarioPlanApproved'):
    c.query('/hrm/payroll/financial/review',{'id':plan,'approved':True,'reason':evidence},review);s['scenarioPlanApproved']=True;c.save()
remember('batchId',lambda:c.ok('/hrm/salary/month-record/create',{'entityId':entity,'year':year,'month':month,'title':s['batchTitle'],'startTime':start+'T00:00:00','endTime':f'{year}-{month:02d}-{days:02d}T23:59:59'}))
print(f'Prepared {len(ids)} synthetic personnel scenarios; all business sources use normal APIs')
