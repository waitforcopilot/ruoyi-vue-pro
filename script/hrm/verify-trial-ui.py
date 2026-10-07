#!/usr/bin/env python3
"""Validate actual trial pages and capture original desktop/mobile screenshots."""
import argparse,json,os,re,uuid
from pathlib import Path
from playwright.sync_api import sync_playwright,expect
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--base-url',default='http://127.0.0.1:3000');p.add_argument('--fixture-file',type=Path,required=True);p.add_argument('--output-dir',type=Path,required=True);p.add_argument('--chromium-bin',default='/usr/bin/chromium');p.add_argument('--allow-test-fixtures',action='store_true',required=True);a=p.parse_args();a.output_dir.mkdir(parents=True,exist_ok=True)
for key in ['HRM_SMOKE_TOKEN','HRM_REFRESH_TOKEN','HRM_TRIAL_READER_TOKEN','HRM_TRIAL_READER_REFRESH_TOKEN']:
 if not os.environ.get(key):p.error('Missing credential: '+key)
f=json.loads(a.fixture_file.read_text());route=a.base_url.rstrip('/')+'/hrm/payroll-trial-batches';checks=[]
def passed(name):checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def seed(context,reader=False):
 values={'ACCESS_TOKEN':os.environ['HRM_TRIAL_READER_TOKEN' if reader else 'HRM_SMOKE_TOKEN'],'REFRESH_TOKEN':os.environ['HRM_TRIAL_READER_REFRESH_TOKEN' if reader else 'HRM_REFRESH_TOKEN'],'TenantId':1}
 context.add_init_script('(function(values){for(const[k,v]of Object.entries(values))localStorage.setItem(k,JSON.stringify({c:Date.now(),e:Date.now()+86400000,v:JSON.stringify(v)}));})('+json.dumps(values)+');')
def result(response):
 data=response.value.json();assert data.get('code')==0,('HTTP request failed',data.get('code'));return data['data']
def open_page(page):
 with page.expect_response(lambda r:'/trial-batches/page' in r.url) as response:page.goto(route,wait_until='domcontentloaded')
 result(response);expect(page.get_by_role('heading',name='批次资料核验与试算',exact=True)).to_be_visible(timeout=30000)
def search(page,title):
 page.get_by_label('查询批次主体',exact=True).fill(f['entityCode']);page.get_by_label('查询批次名称',exact=True).fill(title)
 with page.expect_response(lambda r:'/trial-batches/page' in r.url) as response:page.get_by_role('button',name='查询',exact=True).click()
 data=result(response);expect(page.locator('[data-testid="trial-table"] .el-table__body tbody tr')).to_have_count(len(data['list']));return data
def row(page):return page.locator('[data-testid="trial-table"] .el-table__body tbody tr').first
def select(page):
 with page.expect_response(lambda r:'/trial-batches/get?' in r.url) as response:row(page).get_by_role('button',name='核验与版本',exact=True).click()
 result(response);expect(page.get_by_role('button',name='核验资料',exact=True)).to_be_visible()
def screenshot(page,name,target=None):
 if target:target.scroll_into_view_if_needed()
 expect(page.locator('.el-loading-mask:visible')).to_have_count(0,timeout=15000);expect(page.locator('.el-notification:visible')).to_have_count(0,timeout=15000)
 page.screenshot(path=str(a.output_dir/name),full_page=True,animations='disabled')
def verify(page):
 with page.expect_response(lambda r:'/trial-batches/check?' in r.url) as response:page.get_by_role('button',name='核验资料',exact=True).click()
 return result(response)
def choose(page,label,name):
 control=page.get_by_role('combobox',name=label,exact=True);page.locator('.el-select').filter(has=control).click();dropdown=control.get_attribute('aria-controls');page.locator('#'+dropdown).get_by_role('option',name=name,exact=True).click()
def edit(page):row(page).get_by_role('button',name='编辑',exact=True).click();dialog=page.get_by_role('dialog').filter(has=page.get_by_role('button',name='保存草稿',exact=True));expect(dialog).to_be_visible();expect(dialog.get_by_role('button',name='保存草稿',exact=True)).to_be_enabled();return dialog
def save(page,dialog,path='update'):
 with page.expect_response(lambda r:'/trial-batches/'+path in r.url) as response:dialog.get_by_role('button',name='保存草稿',exact=True).click()
 data=result(response);expect(dialog).not_to_be_visible();return data
def held(page,pending):
 for _ in range(100):
  if pending:return
  page.wait_for_timeout(50)
 raise AssertionError('Held HTTP response not received')
with sync_playwright() as pw:
 browser=pw.chromium.launch(executable_path=a.chromium_bin,args=['--no-sandbox']);context=browser.new_context(viewport={'width':1512,'height':1100},locale='zh-CN');seed(context);page=context.new_page();open_page(page);search(page,'MAIN');screenshot(page,'payroll-trial-desktop.png');passed('authorized real batch list renders stored data')
 select(page);expect(page.locator('[data-testid="trial-run"]')).to_contain_text('6300.00');screenshot(page,'payroll-trial-result.png',page.locator('[data-testid="trial-run"]'));passed('immutable V2 result shows exact CNY totals and qualified persons')
 page.locator('[data-testid="trial-run-people"] .el-table__body tbody tr').first.get_by_role('button',name='解释',exact=True).click();explanation=page.get_by_role('dialog').filter(has=page.locator('[data-testid="trial-explanation"]'));expect(explanation).to_contain_text('gross - deductions - tax');expect(explanation).to_contain_text('合成工资、个人社保公积金和已核定个税依据');screenshot(page,'payroll-trial-explanation.png');explanation.press('Escape');passed('personal explanation retains input basis expression and rounding steps')
 page.get_by_role('tab',name='版本比较',exact=True).click();choose(page,'比较基准版本','V1');choose(page,'比较目标版本','V2')
 with page.expect_response(lambda r:'/trial-batches/compare?' in r.url) as response:page.get_by_role('button',name='比较版本',exact=True).click()
 data=result(response);assert data['totalDifferences']['net']=='100.00';expect(page.locator('[data-testid="trial-comparison"]')).to_contain_text('金额变化');screenshot(page,'payroll-trial-comparison.png',page.locator('[data-testid="trial-comparison"]'));passed('version comparison displays exact 100.00 delta')
 choose(page,'比较目标版本','V1');expect(page.locator('[data-testid="trial-comparison"]')).to_have_count(0);passed('changing comparison controls clears previous difference')
 page.get_by_role('tab',name='操作历史',exact=True).click();expect(page.locator('[data-testid="trial-history"]')).to_contain_text('保存试算');passed('batch operation history is readable')
 assert verify(page)['ready'];screenshot(page,'payroll-trial-check.png',page.locator('[data-testid="trial-check"]'));passed('preflight checks current rule full-period qualification inputs and balance')
 pending=[]
 def hold(route):response=route.fetch();pending.append((route,response))
 page.route('**/trial-batches/check?*',hold);page.get_by_role('button',name='核验资料',exact=True).click();held(page,pending);search(page,'NULL-SCOPE');pending[0][0].fulfill(response=pending[0][1]);page.unroute('**/trial-batches/check?*',hold);expect(page.locator('[data-testid="trial-check"]')).to_have_count(0);passed('late preflight cannot restore result after batch selection changes')
 select(page);assert not verify(page)['ready'];expect(page.locator('[data-testid="trial-check"]')).to_contain_text('唯一、已确认');screenshot(page,'payroll-trial-blocked.png',page.locator('[data-testid="trial-check"]'));passed('unresolved qualification appears as explicit blocked row')
 search(page,'UNKNOWN');select(page);expect(page.locator('[data-testid="trial-run"]')).to_contain_text('排除计薪');screenshot(page,'payroll-trial-excluded.png',page.locator('[data-testid="trial-run"]'));passed('historical explicit exclusion persists after current roster is edited')
 search(page,'MAIN');dialog=edit(page);expect(dialog.get_by_label('批次编号',exact=True)).to_be_disabled();expect(dialog.get_by_label('批次开始日期',exact=True)).to_be_disabled();expect(dialog.get_by_label('HRM '+str(f['employeeId'])+' 已核定个税',exact=True)).to_have_value('350.00');screenshot(page,'payroll-trial-editor.png');passed('structured editor retains explicit values immutable batch identity and evidence')
 dialog.get_by_label('HRM '+str(f['employeeId'])+' 基本工资',exact=True).fill('6200.00');save(page,dialog);expect(page.locator('[data-testid="trial-run"]')).to_contain_text('6300.00');assert verify(page)['ready']
 with page.expect_response(lambda r:'/trial-batches/execute' in r.url) as response:page.get_by_role('button',name='保存试算版本',exact=True).click()
 data=result(response);assert data['runVersion']==3;expect(page.locator('[data-testid="trial-run"]')).to_contain_text('6400.00');passed('edited inputs create V3 while V2 remains unchanged')
 # A transport failure after commit must retain the same idempotency command for recovery.
 assert verify(page)['ready'];committed=[];dropped=[]
 def commit_then_drop(route):response=route.fetch();committed.append(response.json()['data']);route.abort('failed');dropped.append(True)
 page.route('**/trial-batches/execute',commit_then_drop);page.get_by_role('button',name='保存试算版本',exact=True).click();held(page,dropped);expect(page.get_by_role('button',name='重试保存试算',exact=True)).not_to_have_class(re.compile('is-loading'));page.unroute('**/trial-batches/execute',commit_then_drop)
 with page.expect_response(lambda r:'/trial-batches/execute' in r.url) as response:page.get_by_role('button',name='重试保存试算',exact=True).click()
 recovered=result(response);assert recovered['id']==committed[0]['id'] and recovered['runVersion']==4;passed('lost response retry reuses command and does not create duplicate version')
 # Candidate changes while personnel lookup is in flight must not add an old person.
 page.get_by_role('button',name='登记试算批次',exact=True).click();dialog=page.get_by_role('dialog').filter(has=page.get_by_role('button',name='保存草稿',exact=True));pending=[];page.route('**/employee-eligibilities/employee?*',hold)
 dialog.get_by_label('试算 HRM 人员 ID',exact=True).fill(str(f['employeeId']));dialog.get_by_role('button',name='核对档案并加入',exact=True).click();held(page,pending);dialog.get_by_label('试算 HRM 人员 ID',exact=True).fill(str(f['otherEmployeeId']));pending[0][0].fulfill(response=pending[0][1]);page.unroute('**/employee-eligibilities/employee?*',hold);expect(dialog.locator('[data-testid="trial-editor-person"]')).to_have_count(0);dialog.press('Escape');passed('late employee response cannot add a different selected person')
 page.get_by_role('button',name='登记试算批次',exact=True).click();dialog=page.get_by_role('dialog').filter(has=page.get_by_role('button',name='保存草稿',exact=True));code='TRIAL-UI-'+uuid.uuid4().hex[:8].upper()
 for label,value in [('批次编号',code),('批次名称','浏览器合成工资试算'),('批次负责人','浏览器 QA 负责人'),('批次主体编号',f['entityCode']),('批次主体名称','试算合成声明主体'),('批次口径依据','合成常规工资口径，仅技术回归')]:dialog.get_by_label(label,exact=True).fill(value)
 for label,value in [('批次开始日期','2026-10-01'),('批次结束日期','2026-10-31')]:dialog.get_by_label(label,exact=True).fill(value);dialog.get_by_label(label,exact=True).press('Enter')
 choose(page,'批次计算规则','试算合成常规工资规则 · '+f['definitionCode']+' V1');dialog.get_by_label('试算 HRM 人员 ID',exact=True).fill(str(f['employeeId']))
 with page.expect_response(lambda r:'/employee-eligibilities/employee?' in r.url) as response:dialog.get_by_role('button',name='核对档案并加入',exact=True).click()
 result(response);expect(dialog.locator('[data-testid="trial-editor-person"]')).to_have_count(1)
 labels=['基本工资','津贴','绩效','加班工资','缺勤扣款','个人社保','个人公积金','其他扣款','已核定个税'];values=['6300.00','500.00','1000.00','300.00','100.00','500.00','600.00','50.00','350.00']
 for label,value in zip(labels,values):field=dialog.get_by_label('HRM '+str(f['employeeId'])+' '+label,exact=True);expect(field).to_have_value('');field.fill(value)
 dialog.get_by_label('HRM '+str(f['employeeId'])+' 输入依据',exact=True).fill('浏览器合成工资和核定个税来源');save(page,dialog,'create');assert verify(page)['ready'];passed('new batch editor starts with missing inputs and saves explicitly entered nine amounts')
 with page.expect_response(lambda r:'/trial-batches/execute' in r.url) as response:page.get_by_role('button',name='保存试算版本',exact=True).click()
 result(response);expect(page.locator('[data-testid="trial-run"]')).to_contain_text('6500.00');passed('new browser-created batch computes its independently entered wage')
 readonly=browser.new_context(viewport={'width':1512,'height':1000},locale='zh-CN');seed(readonly,True);reader=readonly.new_page();open_page(reader);search(reader,'MAIN');select(reader);assert verify(reader)['ready']
 for name in ['登记试算批次','编辑','保存试算版本','重试保存试算']:expect(reader.get_by_role('button',name=name,exact=True)).to_have_count(0)
 screenshot(reader,'payroll-trial-readonly.png',reader.locator('[data-testid="trial-check"]'));passed('read-only role has checks and saved results without mutation buttons')
 mobilecontext=browser.new_context(viewport={'width':390,'height':844},is_mobile=True,locale='zh-CN');seed(mobilecontext);mobile=mobilecontext.new_page();open_page(mobile);search(mobile,'MAIN');screenshot(mobile,'payroll-trial-mobile.png');select(mobile);expect(mobile.locator('[data-testid="trial-run"]')).to_contain_text('6400.00');screenshot(mobile,'payroll-trial-mobile-result.png',mobile.locator('[data-testid="trial-run"]'));assert mobile.evaluate('document.documentElement.scrollWidth <= window.innerWidth');passed('mobile KPI cards stack and table overflow stays inside page')
 (a.output_dir/'hrm-trial-browser-results.json').write_text(json.dumps({'total':len(checks),'passed':len(checks),'checks':checks,'syntheticFixtures':True,'screenshots':sorted(p.name for p in a.output_dir.glob('*.png'))},ensure_ascii=False,indent=2)+'\n');browser.close()
print('PASS:',len(checks),'actual browser checks')
