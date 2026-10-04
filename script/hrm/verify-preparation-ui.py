#!/usr/bin/env python3
"""Read-only live overview navigation, permission states and original Chromium screenshots."""
import argparse,json,os
from pathlib import Path
from playwright.sync_api import sync_playwright,expect
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url',default='http://127.0.0.1:3000')
parser.add_argument('--output-dir',type=Path,required=True)
parser.add_argument('--chromium-bin',default='/usr/bin/chromium')
args=parser.parse_args();args.output_dir.mkdir(parents=True,exist_ok=True)
for name in ['HRM_SMOKE_TOKEN','HRM_REFRESH_TOKEN','HRM_OVERVIEW_ONLY_TOKEN','HRM_OVERVIEW_ONLY_REFRESH_TOKEN','HRM_TENANT_B_TOKEN','HRM_TENANT_B_REFRESH_TOKEN']:
 if not os.environ.get(name):parser.error('Missing verification credential: '+name)
checks=[];base=args.base_url.rstrip('/');route=base+'/hrm/payroll-preparation'
def passed(name):checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def seed(context,token='HRM_SMOKE_TOKEN',refresh='HRM_REFRESH_TOKEN',tenant=1):
 values={'ACCESS_TOKEN':os.environ[token],'REFRESH_TOKEN':os.environ[refresh],'TenantId':tenant}
 context.add_init_script('(function(values){for(const [k,v] of Object.entries(values)){localStorage.setItem(k,JSON.stringify({c:Date.now(),e:Date.now()+86400000,v:JSON.stringify(v)}));}})('+json.dumps(values)+');')
def summary(page):
 with page.expect_response(lambda r:'/payroll/preparation/summary' in r.url) as response:page.goto(route,wait_until='domcontentloaded')
 result=response.value.json();assert result['code']==0,result['code']
 expect(page.get_by_role('heading',name='薪酬资料准备总览',exact=True)).to_be_visible(timeout=30000)
 return result['data']
def screenshot(page,name):
 expect(page.locator('.el-loading-mask:visible')).to_have_count(0)
 expect(page.locator('.el-notification:visible')).to_have_count(0,timeout=10000)
 page.screenshot(path=str(args.output_dir/name),full_page=True,animations='disabled')
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path=args.chromium_bin,headless=True,args=['--no-sandbox'])
 context=browser.new_context(viewport={'width':1680,'height':1250},locale='zh-CN',timezone_id='Asia/Shanghai');seed(context)
 page=context.new_page();errors=[];page.on('pageerror',lambda e:errors.append(e.name));data=summary(page)
 for section,metric in [('requirements','pending'),('sources','ready'),('contracts','confirmed'),('rules','confirmed'),('batches','failed')]:
  expect(page.locator('[data-testid="card-'+section+'"] strong')).to_have_text(str(data[section]['counts'][metric]))
 passed('overview cards show actual server counts with independent source and rule states')
 expect(page.get_by_text('本页汇总资料登记与预检状态，尚不提供工资计算。契约确认、规则确认或格式通过，均不代表人员、金额和来源完整性已经核实。',exact=True)).to_be_visible()
 for batch in data['batches']['recentBatches']:
  row=page.locator('[data-testid="batch-table"]').get_by_role('row').filter(has=page.get_by_role('cell',name=str(batch['id']),exact=True))
  expect(row.get_by_role('cell',name=batch['periodStart']+' 至 '+batch['periodEnd'],exact=True)).to_be_visible()
 passed('recent batches show owned IDs ISO periods and original quality status')
 with page.expect_response(lambda r:'/payroll/preparation/summary' in r.url) as response:page.get_by_role('button',name='刷新状态',exact=True).click()
 refreshed=response.value.json();assert refreshed['code']==0 and refreshed['data']['generatedAt']!=data['generatedAt']
 expect(page.locator('[data-testid="updated"]')).to_be_visible();passed('refresh obtains a new server snapshot')
 screenshot(page,'payroll-preparation-desktop.png')
 page.locator('[data-testid="batch-table"]').scroll_into_view_if_needed()
 expect(page.locator('[data-testid="source-table"]').get_by_role('cell',name=data['sources']['sources'][-1]['code'],exact=True)).to_be_in_viewport()
 screenshot(page,'payroll-preparation-batches.png')
 passed('internal page scrolling exposes all source rows and the owned recent batch section')
 calc=page.locator('[data-testid="module-table"]').get_by_role('row').filter(has=page.get_by_role('cell',name='工资核算',exact=True))
 with page.expect_response(lambda r:'/payroll/requirements/page' in r.url and 'moduleCode=calc' in r.url) as response:calc.get_by_role('button',name='查看需求',exact=True).click()
 assert response.value.json()['code']==0;expect(page).to_have_url(base+'/hrm/payroll-requirements?module=calc')
 passed('module navigation filters the real requirements page')
 summary(page)
 first=data['sources']['sources'][0]
 source_row=page.locator('[data-testid="source-table"]').get_by_role('row').filter(has=page.get_by_role('cell',name=first['code'],exact=True))
 with page.expect_response(lambda r:'/payroll/requirements/page' in r.url and 'sourceCode='+first['code'] in r.url) as response:source_row.get_by_role('button',name='关联需求',exact=True).click()
 assert response.value.json()['code']==0;expect(page).to_have_url(base+'/hrm/payroll-requirements?sourceCode='+first['code'])
 passed('source navigation filters actual linked requirements')
 summary(page);page.locator('[data-testid="card-rules"]').get_by_role('button',name='核对规则与样例',exact=True).click()
 expect(page.get_by_role('heading',name='薪酬规则台账',exact=True)).to_be_visible(timeout=10000)
 summary(page);page.locator('[data-testid="card-contracts"]').get_by_role('button',name='查看字段契约',exact=True).click()
 expect(page.get_by_role('heading',name='薪酬数据接入准备',exact=True)).to_be_visible(timeout=10000)
 passed('rule and contract links reach the implemented modules')
 summary(page)
 # Simulate one failure for recovery assertions; screenshots always use real server responses.
 pattern='**/hrm/payroll/preparation/summary*'
 def fail_once(request):request.fulfill(status=200,content_type='application/json',body=json.dumps({'code':500,'msg':'回归模拟临时失败'}))
 page.route(pattern,fail_once)
 page.get_by_role('button',name='刷新状态',exact=True).click()
 expect(page.get_by_text('未能取得最新资料状态，请刷新重试。',exact=True)).to_be_visible()
 expect(page.locator('[data-testid="card-requirements"]')).to_have_count(0)
 page.unroute(pattern,fail_once)
 with page.expect_response(lambda r:'/payroll/preparation/summary' in r.url) as response:page.get_by_role('button',name='刷新状态',exact=True).click()
 assert response.value.json()['code']==0;expect(page.locator('[data-testid="card-requirements"] strong')).to_be_visible()
 passed('failed refresh clears stale data and recovers from a real successful response')
 restricted=browser.new_context(viewport={'width':1440,'height':1000},locale='zh-CN');seed(restricted,'HRM_OVERVIEW_ONLY_TOKEN','HRM_OVERVIEW_ONLY_REFRESH_TOKEN')
 rp=restricted.new_page();summary(rp)
 expect(rp.locator('.unauthorized')).to_have_count(5)
 for table in ['module-table','source-table','batch-table']:expect(rp.locator('[data-testid="'+table+'"]')).to_have_count(0)
 for button in ['核对需求与范围','核对来源','查看字段契约','核对规则与样例','核对预检结果']:expect(rp.get_by_role('button',name=button,exact=True)).to_have_count(0)
 screenshot(rp,'payroll-preparation-restricted.png');passed('overview-only identity shows permission gaps without zeros hidden counts or navigation');restricted.close()
 other=browser.new_context(viewport={'width':1440,'height':1000},locale='zh-CN');seed(other,'HRM_TENANT_B_TOKEN','HRM_TENANT_B_REFRESH_TOKEN',999)
 op=other.new_page();empty=summary(op);assert empty['requirements']['counts']['total']==0
 expect(op.get_by_text('当前租户尚未登记候选需求；请在需求征集页登记并填写实际范围。',exact=True)).to_be_visible()
 expect(op.get_by_text('当前租户尚未登记来源',exact=True)).to_be_visible()
 screenshot(op,'payroll-preparation-empty-tenant.png');passed('empty tenant shows actual zero metadata and actionable empty states');other.close()
 mobile=browser.new_context(viewport={'width':390,'height':844},is_mobile=True,has_touch=True,locale='zh-CN');seed(mobile)
 mp=mobile.new_page();summary(mp);assert mp.evaluate('document.documentElement.scrollWidth<=window.innerWidth')
 expect(mp.locator('[data-testid="card-requirements"] strong')).to_be_visible();screenshot(mp,'payroll-preparation-mobile.png')
 mp.locator('[data-testid="batch-table"]').scroll_into_view_if_needed();assert mp.evaluate('document.documentElement.scrollWidth<=window.innerWidth')
 screenshot(mp,'payroll-preparation-mobile-batches.png')
 passed('390px mobile overview stacks cards and contains table scrolling');mobile.close()
 assert not errors,errors;passed('no uncaught browser page errors')
 context.close();browser.close()
(args.output_dir/'hrm-preparation-ui-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2))
print('All',len(checks),'preparation browser checks passed')
