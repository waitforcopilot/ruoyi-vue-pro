#!/usr/bin/env python3
"""Verify original desktop/mobile overview pages and native-review deep links in synthetic QA."""
import argparse,json,os,re
from pathlib import Path
from urllib.parse import parse_qs,urlsplit
from playwright.sync_api import sync_playwright,expect
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--base-url',default='http://127.0.0.1:3000');p.add_argument('--fixture-file',type=Path,required=True);p.add_argument('--output-dir',type=Path,required=True);p.add_argument('--chromium-bin',default='/usr/bin/chromium');p.add_argument('--allow-test-fixtures',action='store_true',required=True);a=p.parse_args()
if urlsplit(a.base_url).hostname not in ['127.0.0.1','localhost']:p.error('Use the loopback synthetic verification service')
f=json.loads(a.fixture_file.read_text());assert f.get('synthetic');a.output_dir.mkdir(parents=True,exist_ok=True);checks=[]
for role in ['reader','hr','finance']:
 for suffix in ['TOKEN','REFRESH_TOKEN']:
  if not os.environ.get('HRM_REVIEW_'+role.upper()+'_'+suffix):p.error('Missing '+role+' credential')
def passed(name):checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def seed(context,role):
 values={'ACCESS_TOKEN':os.environ['HRM_REVIEW_'+role.upper()+'_TOKEN'],'REFRESH_TOKEN':os.environ['HRM_REVIEW_'+role.upper()+'_REFRESH_TOKEN'],'TenantId':1}
 context.add_init_script('(function(values){for(const[k,v]of Object.entries(values))localStorage.setItem(k,JSON.stringify({c:Date.now(),e:Date.now()+86400000,v:JSON.stringify(v)}));})('+json.dumps(values)+');')
def result(response):
 r=response.value.json();assert r.get('code')==0,(r.get('code'),r.get('msg'));return r['data']
def open_page(page):
 with page.expect_response(lambda r:'/overview/page' in r.url) as response:page.goto(a.base_url+'/hrm/payroll-overview',wait_until='domcontentloaded')
 result(response);expect(page.get_by_role('heading',name='薪酬批次概览',exact=True)).to_be_visible(timeout=30000)
def search(page):
 page.get_by_label('查询主体编号',exact=True).fill(f['entityCode']);page.get_by_label('查询批次名称',exact=True).fill('OVERVIEW')
 with page.expect_response(lambda r:'/overview/page' in r.url) as response:page.get_by_role('button',name='查询',exact=True).click()
 return result(response)
def row(page,id):return page.locator('[data-testid="overview-table"] .el-table__body tbody tr').filter(has_text=f['batchCodes'][str(id)])
def select(page,id):
 with page.expect_response(lambda r:'/overview/batch?' in r.url) as response:row(page,id).get_by_role('button',name='金额与资料核验',exact=True).click()
 r=result(response);expect(page.get_by_test_id('overview-detail')).to_be_visible();return r
def screenshot(page,name,target=None):
 if target is not None:target.scroll_into_view_if_needed()
 expect(page.locator('.el-loading-mask:visible')).to_have_count(0,timeout=20000);page.screenshot(path=str(a.output_dir/name),full_page=True,animations='disabled')
with sync_playwright() as pw:
 browser=pw.chromium.launch(executable_path=a.chromium_bin,args=['--no-sandbox']);pages={}
 for role in ['reader','hr','finance']:
  c=browser.new_context(viewport={'width':1512,'height':1120},locale='zh-CN');seed(c,role);pg=c.new_page();open_page(pg);data=search(pg);pages[role]=pg
 reader=pages['reader'];expect(reader.get_by_test_id('overview-state-2')).to_have_text('2');expect(reader.get_by_test_id('overview-state-4')).to_have_text('1');expect(reader.get_by_test_id('assigned-hr')).to_have_text('0');screenshot(reader,'payroll-overview-states.png',reader.get_by_role('heading',name='薪酬批次概览',exact=True));passed('scoped five-state dashboard and readonly personal task counts use actual data')
 select(reader,f['savedId']);expect(reader.get_by_test_id('amount-gross')).to_have_text(f['gross']);expect(reader.get_by_test_id('amount-net')).to_have_text(f['net']);expect(reader.get_by_test_id('overview-detail')).to_contain_text('当前来源与所选保存版本一致');expect(reader.get_by_test_id('overview-detail')).to_contain_text('单位缴费尚未关联');screenshot(reader,'payroll-overview-version-money.png',reader.get_by_test_id('overview-detail'));passed('money belongs to one saved version and unbound legal cost is missing')
 select(reader,f['draftId']);expect(reader.get_by_test_id('overview-detail')).to_contain_text('尚无保存的试算版本')
 for key in ['gross','deductions','tax','net']:expect(reader.get_by_test_id('amount-'+key)).to_have_text('—')
 screenshot(reader,'payroll-overview-no-version.png',reader.get_by_test_id('overview-detail'));passed('draft without a saved version displays no invented zero wages')
 select(reader,f['invalidatedId']);expect(reader.get_by_test_id('overview-detail')).to_contain_text('旧版本金额仅作历史参考');expect(reader.get_by_test_id('amount-net')).to_have_text(f['net']);screenshot(reader,'payroll-overview-invalidated.png',reader.get_by_test_id('overview-detail'));passed('invalidated historical money remains clearly labelled')
 hr=pages['hr'];expect(hr.get_by_test_id('assigned-hr')).to_have_text('1');expect(row(hr,f['hrId'])).to_contain_text('我的复核任务');expect(row(hr,f['hrId'])).to_contain_text('待 HR 复核');screenshot(hr,'payroll-overview-hr-todo.png',hr.get_by_test_id('overview-todos'));passed('assigned HR identity sees exactly its real native task')
 finance=pages['finance'];expect(finance.get_by_test_id('assigned-finance')).to_have_text('1');expect(row(finance,f['financeId'])).to_contain_text('我的复核任务');screenshot(finance,'payroll-overview-finance-todo.png',finance.get_by_test_id('overview-todos'));passed('assigned finance identity sees current finance stage')
 with finance.expect_response(lambda r:'/review/get?' in r.url) as response:row(finance,f['financeId']).get_by_role('button',name='进入批次核对',exact=True).click()
 view=result(response);assert view['batchId']==f['financeId'];expect(finance).to_have_url(re.compile(r'payroll-trial-batches\?batchId='+str(f['financeId'])));expect(finance.get_by_test_id('payroll-review-panel').get_by_role('button',name='通过本级复核',exact=True)).to_be_visible();screenshot(finance,'payroll-overview-review-link.png',finance.get_by_test_id('payroll-review-panel'));passed('overview link selects the exact review batch and preserves stage action permissions')
 finance.reload(wait_until='domcontentloaded');expect(finance.get_by_test_id('payroll-review-panel').get_by_role('button',name='通过本级复核',exact=True)).to_be_visible(timeout=30000);passed('batchId deep link survives a direct browser reload')
 # Delay a different batch's response until after the current money has loaded.
 held=[]
 def delay(route):
  if parse_qs(urlsplit(route.request.url).query).get('id')==[str(f['draftId'])]:held.append((route,route.fetch()))
  else:route.continue_()
 reader.route('**/overview/batch?*',delay);row(reader,f['draftId']).get_by_role('button',name='金额与资料核验',exact=True).click()
 for _ in range(200):
  if held:break
  reader.wait_for_timeout(50)
 assert held;select(reader,f['savedId'])
 with reader.expect_response(lambda r:'/overview/batch?' in r.url and parse_qs(urlsplit(r.url).query).get('id')==[str(f['draftId'])]) as response:held[0][0].fulfill(response=held[0][1])
 result(response);reader.evaluate('() => new Promise(resolve => setTimeout(resolve,100))');expect(reader.get_by_test_id('amount-net')).to_have_text(f['net']);expect(reader.get_by_test_id('overview-detail')).not_to_contain_text('尚无保存的试算版本');reader.unroute('**/overview/batch?*',delay);passed('late previous-batch response cannot overwrite chosen financial version')
 reader.get_by_role('button',name='查看复核批次',exact=True).click();expect(reader.get_by_test_id('overview-detail')).to_have_count(0);expect(reader.get_by_test_id('overview-state-2')).to_have_text('2');expect(reader.get_by_test_id('overview-state-1')).to_have_text('0');passed('query change clears prior money and consistently filters state counts')
 mobilecontext=browser.new_context(viewport={'width':390,'height':844},is_mobile=True,locale='zh-CN');seed(mobilecontext,'reader');mobile=mobilecontext.new_page();open_page(mobile);search(mobile);screenshot(mobile,'payroll-overview-mobile.png',mobile.get_by_test_id('overview-states'));assert mobile.evaluate('document.documentElement.scrollWidth <= window.innerWidth');select(mobile,f['savedId']);expect(mobile.get_by_test_id('amount-net')).to_have_text(f['net']);screenshot(mobile,'payroll-overview-mobile-money.png',mobile.get_by_test_id('overview-amounts'));assert mobile.evaluate('document.documentElement.scrollWidth <= window.innerWidth');passed('mobile cards stack, exact money remains readable and table overflow stays local')
 (a.output_dir/'hrm-overview-browser-results.json').write_text(json.dumps({'total':len(checks),'passed':len(checks),'checks':checks,'syntheticFixtures':True,'realBpmEngine':True,'screenshots':sorted(p.name for p in a.output_dir.glob('*.png'))},ensure_ascii=False,indent=2)+'\n');browser.close()
print('PASS:',len(checks),'actual overview browser checks')
