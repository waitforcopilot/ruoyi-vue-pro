#!/usr/bin/env python3
"""Exercise actual qualification workflow and retain original desktop/mobile screenshots."""
import argparse,json,os,re,uuid
from pathlib import Path
from playwright.sync_api import sync_playwright,expect
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url',default='http://127.0.0.1:3000')
parser.add_argument('--fixture-file',type=Path,required=True)
parser.add_argument('--output-dir',type=Path,required=True)
parser.add_argument('--chromium-bin',default='/usr/bin/chromium')
parser.add_argument('--allow-test-fixtures',action='store_true',required=True)
args=parser.parse_args();args.output_dir.mkdir(parents=True,exist_ok=True)
for name in ['HRM_SMOKE_TOKEN','HRM_REFRESH_TOKEN','HRM_ELIG_READER_TOKEN','HRM_ELIG_READER_REFRESH_TOKEN']:
 if not os.environ.get(name):parser.error('Missing credential: '+name)
fixture=json.loads(args.fixture_file.read_text());route=args.base_url.rstrip('/')+'/hrm/payroll-employee-eligibility';checks=[]
def passed(name):checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def seed(context,reader=False):
 values={'ACCESS_TOKEN':os.environ['HRM_ELIG_READER_TOKEN' if reader else 'HRM_SMOKE_TOKEN'],'REFRESH_TOKEN':os.environ['HRM_ELIG_READER_REFRESH_TOKEN' if reader else 'HRM_REFRESH_TOKEN'],'TenantId':1}
 context.add_init_script('(function(values){for(const[k,v]of Object.entries(values))localStorage.setItem(k,JSON.stringify({c:Date.now(),e:Date.now()+86400000,v:JSON.stringify(v)}));})('+json.dumps(values)+');')
def result(response):
 data=response.value.json();assert data.get('code')==0,('request failed',data.get('code'));return data['data']
def open_page(page):
 with page.expect_response(lambda r:'/employee-eligibilities/page' in r.url) as response:page.goto(route,wait_until='domcontentloaded')
 result(response);expect(page.get_by_role('heading',name='计薪人员资格与期间',exact=True)).to_be_visible(timeout=30000)
 expect(page.locator('.el-loading-mask:visible')).to_have_count(0)
def search(page,code=None):
 page.get_by_placeholder('声明主体编号（精确）',exact=True).fill(code or fixture['entityCode'])
 with page.expect_response(lambda r:'/employee-eligibilities/page' in r.url) as response:page.get_by_role('button',name='查询',exact=True).click()
 data=result(response);expect(page.locator('[data-testid="eligibility-table"] .el-table__body tbody tr')).to_have_count(len(data['list']));return data
def row_for(page,version):return page.locator('[data-testid="eligibility-table"]').get_by_role('row').filter(has=page.get_by_role('cell',name=re.compile(r'^V'+str(version)+r'\s*·')))
def screenshot(page,name):
 expect(page.locator('.el-loading-mask:visible')).to_have_count(0,timeout=15000);expect(page.locator('.el-notification:visible')).to_have_count(0,timeout=15000)
 page.screenshot(path=str(args.output_dir/name),full_page=True,animations='disabled')
def await_held(page,pending):
 for _ in range(100):
  if pending:return
  page.wait_for_timeout(50)
 raise AssertionError("Held HTTP response did not arrive")
def editor(page):return page.get_by_role('dialog').filter(has=page.get_by_role('button',name='保存草稿',exact=True))
def close_dialog(dialog):dialog.press('Escape');expect(dialog).not_to_be_visible()
def dates(target,start,end,prefix='资格'):
 for label,value in [(prefix+'开始日期',start),(prefix+'结束日期',end)]:target.get_by_label(label,exact=True).fill(value);target.get_by_label(label,exact=True).press('Enter')
 target.get_by_label(prefix+'开始日期',exact=True).press('Tab')
def choose(target,name):
 control=target.locator('.el-select');control.click();dropdown=control.get_by_role('combobox').get_attribute('aria-controls');target.page.locator('#'+dropdown).get_by_role('option',name=name,exact=True).click()
def save(page,dialog,path):
 with page.expect_response(lambda r:'/employee-eligibilities/'+path in r.url) as response:dialog.get_by_role('button',name='保存草稿',exact=True).click()
 data=result(response);expect(dialog).not_to_be_visible();return data
def review(page,row):
 row.get_by_role('button',name='确认',exact=True).click();dialog=page.get_by_role('dialog').filter(has=page.get_by_role('button',name='提交评审',exact=True));dialog.get_by_label('评审依据 *',exact=True).fill('浏览器合成资格核对，非正式业务签认')
 with page.expect_response(lambda r:'/employee-eligibilities/review' in r.url) as response:dialog.get_by_role('button',name='提交评审',exact=True).click()
 result(response);expect(dialog).not_to_be_visible()
def set_lookup(page,code,start,end):
 page.get_by_label('核对主体编号',exact=True).fill(code);page.get_by_label('核对 HRM 人员 ID',exact=True).fill(str(fixture['employeeId']));dates(page,start,end,'核对');page.get_by_role('heading',name='按主体、人员和期间核对资格',exact=True).click()
def resolve(page):
 with page.expect_response(lambda r:'/employee-eligibilities/lookup' in r.url) as response:page.get_by_role('button',name='核对资格',exact=True).click()
 return result(response)
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path=args.chromium_bin,args=['--no-sandbox']);context=browser.new_context(viewport={'width':1512,'height':1100},locale='zh-CN');seed(context);page=context.new_page();open_page(page);search(page)
 screenshot(page,'payroll-eligibility-desktop.png');passed('actual authorized qualification list renders persisted versions')
 set_lookup(page,fixture['entityCode'],'2026-10-01','2026-10-31');assert resolve(page)['matched'];expect(page.locator('[data-testid="eligibility-match"]')).to_contain_text('已确认纳入资格');page.locator('[data-testid="eligibility-match"]').scroll_into_view_if_needed();screenshot(page,'payroll-eligibility-matched.png');passed('confirmed full-period qualification shown with retained evidence')
 page.get_by_label('核对结束日期',exact=True).fill('2026-11-30');page.get_by_label('核对结束日期',exact=True).press('Enter');expect(page.locator('[data-testid="eligibility-match"]')).to_have_count(0);assert not resolve(page)['matched'];passed('changed period clears old result and does not join versions')
 pending=[]
 def hold(route):response=route.fetch();pending.append((route,response))
 page.route('**/employee-eligibilities/lookup',hold);page.get_by_role('button',name='核对资格',exact=True).click();expect(page.get_by_role('button',name='核对资格',exact=True)).to_have_class(re.compile('is-loading'))
 page.get_by_label('核对主体编号',exact=True).fill('MISSING-QA');await_held(page,pending);pending[0][0].fulfill(response=pending[0][1]);page.unroute('**/employee-eligibilities/lookup',hold);expect(page.locator('[data-testid="eligibility-match"]')).to_have_count(0);passed('late period response cannot replace changed inputs')
 page.get_by_role('button',name='登记资格',exact=True).click();dialog=editor(page);expect(dialog.get_by_label('资格开始日期',exact=True)).to_have_value('');expect(dialog.get_by_label('资格结束日期',exact=True)).to_have_value('');expect(dialog.get_by_label('明确计薪资格',exact=True)).to_have_value('');passed('new draft has no default qualification or effective dates')
 pending=[];page.route('**/employee-eligibilities/employee?*',hold);dialog.get_by_label('HRM 人员 ID *',exact=True).fill(str(fixture['employeeId']));dialog.get_by_role('button',name='核对当前档案',exact=True).click();expect(dialog.get_by_role('button',name='核对当前档案',exact=True)).to_have_class(re.compile('is-loading'));dialog.get_by_label('HRM 人员 ID *',exact=True).fill('981100102');await_held(page,pending);pending[0][0].fulfill(response=pending[0][1]);page.unroute('**/employee-eligibilities/employee?*',hold);expect(dialog.locator('[data-testid="eligibility-person"]')).to_have_count(0);passed('late personnel response cannot bind a different person')
 dialog.get_by_label('HRM 人员 ID *',exact=True).fill(str(fixture['employeeId']))
 with page.expect_response(lambda r:'/employee-eligibilities/employee?' in r.url) as response:dialog.get_by_role('button',name='核对当前档案',exact=True).click()
 result(response);expect(dialog.locator('[data-testid="eligibility-person"]')).to_contain_text('资格合成人员 A');code='ELIG-UI-'+uuid.uuid4().hex[:8].upper();dialog.get_by_label('声明主体编号 *',exact=True).fill(code);dialog.get_by_label('声明主体名称 *',exact=True).fill('浏览器合成资格主体');choose(dialog,'排除计薪');dialog.get_by_label('资格负责人',exact=True).fill('浏览器 QA 负责人');dates(dialog,'2027-01-01','2027-01-31');dialog.get_by_label('资格理由',exact=True).fill('合成排除资格场景');dialog.get_by_label('资格依据 / 参考材料',exact=True).fill('合成材料，仅用于接口与页面验证');screenshot(page,'payroll-eligibility-editor.png');id=save(page,dialog,'create');search(page,code);passed('structured editor verifies HRM person and saves explicit exclusion draft')
 review(page,row_for(page,1));search(page,code);set_lookup(page,code,'2027-01-01','2027-01-31');data=resolve(page);assert data['matched'] and data['issueCode']=='EXCLUDED';page.locator('[data-testid="eligibility-match"]').scroll_into_view_if_needed();screenshot(page,'payroll-eligibility-excluded.png');passed('reviewed exclusion is distinct from unresolved qualification')
 row_for(page,1).get_by_role('button',name='另建版本',exact=True).click();page.get_by_role('dialog').filter(has=page.get_by_role('button',name='确定',exact=True)).get_by_role('button',name='确定',exact=True).click();dialog=editor(page);expect(dialog).to_be_visible();expect(dialog.get_by_label('声明主体编号 *',exact=True)).to_be_disabled();dates(dialog,'2027-02-01','2027-02-28');save(page,dialog,'update');search(page,code);review(page,row_for(page,2));search(page,code);passed('new version requires explicit review and preserves previous version')
 row_for(page,1).get_by_role('button',name='详情',exact=True).click();detail=page.get_by_role('dialog').filter(has=page.get_by_role('heading',name='评审历史',exact=True));expect(detail).to_contain_text('合成排除资格场景');expect(detail).to_contain_text('浏览器合成资格核对');screenshot(page,'payroll-eligibility-history.png');close_dialog(detail);passed('version detail retains qualification basis and review history')
 reader_context=browser.new_context(viewport={'width':1512,'height':1000},locale='zh-CN');seed(reader_context,True);reader=reader_context.new_page();open_page(reader);search(reader,code)
 for name in ['登记资格','编辑','确认','停用','另建版本']:expect(reader.get_by_role('button',name=name,exact=True)).to_have_count(0)
 screenshot(reader,'payroll-eligibility-readonly.png');passed('query role shows no maintain or review controls')
 mobile_context=browser.new_context(viewport={'width':390,'height':844},is_mobile=True,locale='zh-CN');seed(mobile_context);mobile=mobile_context.new_page();open_page(mobile);search(mobile,code);screenshot(mobile,'payroll-eligibility-mobile.png');mobile.get_by_role('heading',name='按主体、人员和期间核对资格',exact=True).scroll_into_view_if_needed();screenshot(mobile,'payroll-eligibility-mobile-lookup.png');assert mobile.evaluate('document.documentElement.scrollWidth <= window.innerWidth');passed('mobile forms stack and table scroll stays inside page')
 (args.output_dir/'hrm-eligibility-browser-results.json').write_text(json.dumps({'total':len(checks),'passed':len(checks),'checks':checks,'screenshots':sorted(p.name for p in args.output_dir.glob('*.png')),'syntheticFixtures':True},ensure_ascii=False,indent=2)+'\n');browser.close()
print('PASS:',len(checks),'browser checks')
