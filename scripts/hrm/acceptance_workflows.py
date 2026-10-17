#!/usr/bin/env python3
"""Create synthetic local fixtures and exercise real payroll approvals and permission boundaries."""
import argparse
import datetime
import json
import os
from pathlib import Path
import secrets
import urllib.error
import urllib.parse
import urllib.request

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url',default='http://127.0.0.1:48080/admin-api')
parser.add_argument('--username',default='admin')
parser.add_argument('--password-env',default='HRM_SMOKE_PASSWORD')
parser.add_argument('--state-file',type=Path,required=True,help='Private fixture state OUTSIDE the repository')
parser.add_argument('--create-isolated-fixtures',action='store_true',help='Explicitly allow synthetic users, roles, subjects and financial plans in an isolated test database')
args=parser.parse_args()
if not args.create_isolated_fixtures:parser.error('Requires --create-isolated-fixtures; never use against business data')
if urllib.parse.urlparse(args.base_url).hostname not in ('localhost','127.0.0.1'):parser.error('Only a local isolated environment is supported')
password=os.environ.get(args.password_env)
if not password:parser.error(f'Set {args.password_env}')
session_file=args.state_file.resolve()
root=Path(__file__).resolve().parents[2]
if session_file==root or root in session_file.parents or args.state_file.is_symlink():parser.error('Keep fixture credentials outside the repository and avoid symlinks')
s=json.loads(session_file.read_text()) if session_file.exists() else {}
if s.get('baseUrl',args.base_url)!=args.base_url:parser.error('Use separate fixture state for each verification environment')
s['baseUrl']=args.base_url
year,month=s.get('fixtureYear',2026),s.get('fixtureMonth',10)
base=args.base_url.rstrip('/')
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def call(path,data=None,token=None,method=None):
 h={'tenant-id':'1','Content-Type':'application/json','Authorization':'Bearer '+(s.get('adminToken','') if token is None else token)}
 req=urllib.request.Request(base+path,data=None if data is None else json.dumps(data).encode(),headers=h,method=method)
 try:
  with opener.open(req,timeout=30) as r:return json.loads(r.read())
 except urllib.error.HTTPError as e:return json.loads(e.read())
def ok(path,data=None,token=None,method=None):
 r=call(path,data,token,method)
 if r.get('code')!=0:raise RuntimeError(f'{path.split("?")[0]} returned {r.get("code")}')
 return r.get('data')
def postq(path,params,token=None):return ok(path+'?'+urllib.parse.urlencode(params),{},token)
def save():
 fd=os.open(str(session_file),os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
 os.fchmod(fd,0o600)
 with os.fdopen(fd,'w') as f:json.dump(s,f)
s['adminToken']=ok('/system/auth/login',{'username':args.username,'password':password},token='')['accessToken'];save()
assert all(row['code'].startswith('ACCEPT_') and '隔离' in row['name'] for row in ok('/hrm/payroll/entity/list')), 'This workflow requires an isolated fixture-only HRM database'
if 'reviewerId' not in s:
 s['reviewerPassword']=secrets.token_urlsafe(12)
 s['reviewerId']=ok('/system/user/create',{'username':'hrmacceptreview','nickname':'隔离验收复核人','password':s['reviewerPassword'],'remark':'仅供隔离库薪酬验收，无真实人员信息','postIds':[]})
 save();ok('/system/permission/assign-user-role',{'userId':s['reviewerId'],'roleIds':[1]})
ok('/system/permission/assign-user-role',{'userId':s['reviewerId'],'roleIds':[1]})
s['reviewerToken']=ok('/system/auth/login',{'username':'hrmacceptreview','password':s['reviewerPassword']})['accessToken'];save()
if 'entityId' not in s:
 s['entityId']=ok('/hrm/payroll/entity/create',{'code':'ACCEPT_20261016','name':'隔离验收主体（虚构）','taxRegistrationNumber':'TESTENTITY20260001','evidence':'仅供隔离测试的虚构登记，不作真实业务依据'})
 save()
entity=s['entityId'];reviewer=s['reviewerToken']
rows=ok('/hrm/payroll/entity/list')
if next(row for row in rows if row['id']==entity)['status']!='APPROVED':
 assert call('/hrm/payroll/entity/approve?'+urllib.parse.urlencode({'id':entity,'reason':'本人不可批准'}),{})['code']!=0
 postq('/hrm/payroll/entity/approve',{'id':entity,'reason':'独立核实虚构测试主体'},reviewer)
print('Passed: real entity creation; maker cannot approve; independent actor approval')
if 'readerId' not in s:
 s['readerPassword']=secrets.token_urlsafe(12);s['readerId']=ok('/system/user/create',{'username':'hrmacceptread','nickname':'隔离验收只读人','password':s['readerPassword'],'remark':'仅供权限验收','postIds':[]});save()
reader=ok('/system/auth/login',{'username':'hrmacceptread','password':s['readerPassword']})['accessToken'];s['readerToken']=reader;save()
ok('/system/permission/assign-user-role',{'userId':s['readerId'],'roleIds':[]})
reader=ok('/system/auth/login',{'username':'hrmacceptread','password':s['readerPassword']})['accessToken']
assert call('/hrm/payroll/overview/entities',token=reader)['code']==403
if 'readerRoleId' not in s:
 s['readerRoleId']=ok('/system/role/create',{'name':'薪酬隔离验收只读','code':'hrmacceptread','sort':99,'status':0,'remark':'隔离测试角色'});save()
menus=ok('/system/menu/list');selected=[m['id'] for m in menus if m.get('permission')=='hrm:payroll:overview:query']
assert selected
ok('/system/permission/assign-role-menu',{'roleId':s['readerRoleId'],'menuIds':selected})
ok('/system/permission/assign-user-role',{'userId':s['readerId'],'roleIds':[s['readerRoleId']]})
reader=ok('/system/auth/login',{'username':'hrmacceptread','password':s['readerPassword']})['accessToken'];s['readerToken']=reader;save()
for grant in ok(f'/hrm/payroll/entity/grants?id={entity}'):
 if grant['userId']==s['readerId']:
  postq('/hrm/payroll/entity/revoke',{'id':grant['id'],'reason':'重复隔离验收先恢复无主体权限状态'})
assert ok('/hrm/payroll/overview/entities',token=reader)==[]
assert call(f'/hrm/payroll/overview/history?entityId={entity}&year={year}',token=reader)['code']!=0
today=datetime.date.today();ok('/hrm/payroll/entity/grant',{'entityId':entity,'userId':s['readerId'],'actions':['READ'],'effectiveFrom':(today-datetime.timedelta(days=1)).isoformat(),'effectiveTo':(today+datetime.timedelta(days=30)).isoformat(),'reason':'验收主体只读范围'})
assert len(ok('/hrm/payroll/overview/entities',token=reader))==1
assert call('/hrm/payroll/financial/plans?'+urllib.parse.urlencode({'entityId':entity,'year':year,'month':month}),token=reader)['code']==403
print('Passed: real menu denial; subject scope denial; explicit read grant; writer API denial')
if 'financialPlanId' not in s:
 request={'entityId':entity,'year':year,'month':month,'dimension':'ENTITY','dimensionKey':'ALL','budget':100000,'revenue':500000,'headcount':20,'evidence':'隔离验收预算、营收及编制虚构数据','sourceReference':'ACCEPTANCE-LOCAL','requestKey':'runtime-accept-plan-v1'}
 s['financialPlanId']=ok('/hrm/payroll/financial/create',request);save();assert ok('/hrm/payroll/financial/create',request)==s['financialPlanId']
if next(p for p in ok(f'/hrm/payroll/financial/plans?entityId={entity}&year={year}&month={month}') if p['id']==s['financialPlanId'])['status']!='APPROVED':
 assert call('/hrm/payroll/financial/review?'+urllib.parse.urlencode({'id':s['financialPlanId'],'approved':'true','reason':'本人不能复核'}),{})['code']!=0
 postq('/hrm/payroll/financial/review',{'id':s['financialPlanId'],'approved':'true','reason':'独立复核隔离测试计划'},reviewer)
analysis=ok(f'/hrm/payroll/financial/analysis?entityId={entity}&year={year}&month={month}')
print('Passed: real financial-plan idempotency, independent review and analysis')
for path in [f'/hrm/payroll/entity/options?id={entity}',f'/hrm/payroll/overview/history?entityId={entity}&year={year}',f'/hrm/payroll/insurance-policy/options?entityId={entity}',f'/hrm/payroll/insurance-policy/policies?entityId={entity}',f'/hrm/payroll/insurance-policy/bases?entityId={entity}',f'/hrm/payroll/insurance-bill/list?entityId={entity}',f'/hrm/payroll/work/employees?entityId={entity}&from={year}-01-01&to={year}-12-31',f'/hrm/payroll/work/rules?entityId={entity}',f'/hrm/payroll/cost/options?entityId={entity}',f'/hrm/payroll/wage-recovery/list?entityId={entity}',f'/hrm/payroll/insurance-adjustment/list?entityId={entity}',f'/hrm/payroll/financial/wage-structure?entityId={entity}&year={year}&month={month}']:
 ok(path);print('Passed:',path.split('?')[0])

if 'browserUserId' not in s:
 s['browserPassword']=secrets.token_urlsafe(12);s['browserUserId']=ok('/system/user/create',{'username':'hrmacceptoperator','nickname':'隔离薪酬验收操作人','password':s['browserPassword'],'remark':'仅供隔离库页面巡检，无真实人员','postIds':[]});save()
if 'browserRoleId' not in s:
 s['browserRoleId']=ok('/system/role/create',{'name':'薪酬模块隔离验收','code':'hrmacceptoperator','sort':99,'status':0,'remark':'仅HRM菜单及权限的隔离角色'});save()
menus=ok('/system/menu/list');byid={m['id']:m for m in menus};selected={m['id'] for m in menus if (m.get('permission') or '').startswith('hrm:') or (m.get('component') or '').startswith('hrm/') or m.get('path')=='/hrm-payroll'}
for i in list(selected):
 m=byid[i]
 while m.get('parentId') in byid:
  selected.add(m['parentId']);m=byid[m['parentId']]
ok('/system/permission/assign-role-menu',{'roleId':s['browserRoleId'],'menuIds':sorted(selected)})
ok('/system/permission/assign-user-role',{'userId':s['browserUserId'],'roleIds':[s['browserRoleId']]})
today=datetime.date.today();ok('/hrm/payroll/entity/grant',{'entityId':s['entityId'],'userId':s['browserUserId'],'actions':['READ','MANAGE','CALCULATE','REVIEW','APPROVE','EXPORT'],'effectiveFrom':(today-datetime.timedelta(days=1)).isoformat(),'effectiveTo':(today+datetime.timedelta(days=30)).isoformat(),'reason':'隔离验收主体的完整薪酬职责范围'})
print('Prepared isolated HRM operator with',len(selected),'menu and permission entries')
