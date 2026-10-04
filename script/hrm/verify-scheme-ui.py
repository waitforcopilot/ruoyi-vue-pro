#!/usr/bin/env python3
"""Live scheme snapshots, comparison, review and permission states with original screenshots."""
import argparse,json,os,re,time
from datetime import date as calendar_date,timedelta
from pathlib import Path
from playwright.sync_api import sync_playwright,expect
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url',default='http://127.0.0.1:3000')
parser.add_argument('--fixture-file',type=Path,required=True)
parser.add_argument('--output-dir',type=Path,required=True)
parser.add_argument('--chromium-bin',default='/usr/bin/chromium')
parser.add_argument('--allow-test-fixtures',action='store_true',required=True)
args=parser.parse_args();args.output_dir.mkdir(parents=True,exist_ok=True)
for name in ['HRM_SMOKE_TOKEN','HRM_REFRESH_TOKEN','HRM_SCHEME_READER_TOKEN','HRM_SCHEME_READER_REFRESH_TOKEN','HRM_SCHEME_SCHEMEONLY_TOKEN','HRM_SCHEME_SCHEMEONLY_REFRESH_TOKEN']:
 if not os.environ.get(name):parser.error('Missing verification credential: '+name)
fixture=json.loads(args.fixture_file.read_text());base=args.base_url.rstrip('/');route=base+'/hrm/payroll-schemes';checks=[]
def passed(name):checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def seed(context,token='HRM_SMOKE_TOKEN',refresh='HRM_REFRESH_TOKEN'):
 values={'ACCESS_TOKEN':os.environ[token],'REFRESH_TOKEN':os.environ[refresh],'TenantId':1}
 context.add_init_script('(function(values){for(const[k,v]of Object.entries(values))localStorage.setItem(k,JSON.stringify({c:Date.now(),e:Date.now()+86400000,v:JSON.stringify(v)}));})('+json.dumps(values)+');')
def result(response):
 data=response.value.json();assert data.get('code')==0,('request failed',data.get('code'));return data['data']
def open_page(page):
 with page.expect_response(lambda r:'/schemes/page' in r.url) as response:page.goto(route,wait_until='domcontentloaded')
 result(response);expect(page.get_by_role('heading',name='薪酬方案配置版本',exact=True)).to_be_visible(timeout=30000);expect(page.locator('.el-loading-mask:visible')).to_have_count(0)
def search(page):
 page.locator('.toolbar .el-select').first.click();page.get_by_role('option',name=fixture['groupName']+' · #'+str(fixture['groupId']),exact=True).click();page.get_by_placeholder('搜索方案名称').fill(fixture['title'])
 with page.expect_response(lambda r:'/schemes/page' in r.url and 'groupId='+str(fixture['groupId']) in r.url) as response:page.get_by_role('button',name='查询',exact=True).click()
 return result(response)
def row_for(page,version):return page.locator('[data-testid="scheme-table"]').get_by_role('row').filter(has=page.get_by_role('cell',name=re.compile(r'^V'+str(version)+r'\s*·')))
def screenshot(page,name):
 expect(page.locator('.el-loading-mask:visible')).to_have_count(0,timeout=15000);expect(page.locator('.el-notification:visible')).to_have_count(0,timeout=10000)
 page.screenshot(path=str(args.output_dir/name),full_page=True,animations='disabled')
def select_version(page,index,version):
 selector=page.locator('.compare-grid .el-select').nth(index)
 selector.click()
 dropdown_id=selector.get_by_role('combobox').get_attribute('aria-controls')
 page.locator('#'+dropdown_id).get_by_role('option',name=re.compile(r'^V'+str(version)+r' ·')).click()
def date(dialog,label,value):
 field=dialog.get_by_label(label,exact=True);field.fill(value);field.press('Enter');dialog.get_by_text('保存会重新抓取源配置。已确认版本保留历史内容；源配置变化后请先抓取核对，再保存和确认。',exact=True).click()
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path=args.chromium_bin,headless=True,args=['--no-sandbox'])
 context=browser.new_context(viewport={'width':1680,'height':1250},locale='zh-CN',timezone_id='Asia/Shanghai');seed(context)
 page=context.new_page();errors=[];page.on('pageerror',lambda e:errors.append(e.name));open_page(page);data=search(page)
 assert data['total']>=4;expect(row_for(page,2).get_by_text('已确认',exact=True)).to_be_visible();expect(row_for(page,1).get_by_text('已停用',exact=True)).to_be_visible()
 screenshot(page,'payroll-schemes-desktop.png');passed('scheme route renders actual source group versions statuses and ISO periods')
 select_version(page,0,1);select_version(page,1,2)
 with page.expect_response(lambda r:'/schemes/compare' in r.url) as response:page.get_by_role('button',name='比较配置',exact=True).click()
 changes=result(response)['changes'];comparison=page.locator('[data-testid="scheme-comparison"]');expect(comparison).to_contain_text(str(len(changes))+' 项差异')
 expect(comparison.get_by_role('cell',name='12.00',exact=True)).to_be_visible();expect(comparison.get_by_role('cell',name='0.00',exact=True)).to_be_visible();expect(comparison.get_by_role('cell',name='否',exact=True)).to_be_visible();expect(comparison.get_by_role('cell',name='未设置',exact=True).first).to_be_visible()
 comparison.scroll_into_view_if_needed();screenshot(page,'payroll-schemes-comparison.png');passed('historical comparison renders actual values while separating zero false and missing')
 select_version(page,1,1)
 with page.expect_response(lambda r:'/schemes/compare' in r.url) as response:page.get_by_role('button',name='比较配置',exact=True).click()
 assert not result(response)['changes'];expect(comparison.get_by_text('两个版本的配置内容一致',exact=True)).to_be_visible();passed('equal configuration comparison has an explicit empty state')
 row_for(page,2).get_by_role('button',name='详情',exact=True).click();drawer=page.locator('.el-drawer:visible')
 expect(drawer.get_by_role('cell',name='12.00',exact=True)).to_be_visible();expect(drawer.get_by_role('cell',name='0.00 / 0',exact=True)).to_be_visible();expect(drawer.get_by_role('heading',name='租户共用薪资项目录快照',exact=True)).to_be_visible()
 frozen=drawer.locator('[data-testid="scheme-options"]').get_by_role('row').filter(has=page.get_by_role('cell',name='1000000000',exact=True));expect(frozen.get_by_role('cell',name='否',exact=True).first).to_be_visible()
 screenshot(page,'payroll-schemes-snapshot.png');passed('detail keeps the reviewed standard tax settings and shared catalogue after sources change')
 drawer.get_by_role('heading',name='评审历史',exact=True).scroll_into_view_if_needed()
 audit=drawer.locator('.el-collapse-item').first
 expect(audit).to_be_visible();audit.locator('.el-collapse-item__header').click()
 retained=audit.locator('pre');expect(retained).to_contain_text('"schemeVersion": 2');retained.scroll_into_view_if_needed()
 screenshot(page,'payroll-schemes-history.png');passed('native drawer scrolling exposes the exact retained configuration review snapshot')
 drawer.get_by_role('button',name='关闭此对话框',exact=True).click();row=row_for(page,2)
 with page.expect_response(lambda r:'/schemes/get?' in r.url) as get_response:
  with page.expect_response(lambda r:'/schemes/new-version' in r.url) as response:row.get_by_role('button',name='另建版本',exact=True).click()
 ui_id=result(response);new=result(get_response);ui_version=new['schemeVersion'];dialog=page.get_by_role('dialog').filter(has=page.get_by_text('维护方案草稿',exact=True));expect(dialog).to_be_visible()
 expect(dialog.get_by_role('cell',name='10.00',exact=True)).to_be_visible();expect(dialog.get_by_label('源薪资组 *',exact=True)).to_be_disabled();dialog.get_by_label('方案名称 *',exact=True).fill(fixture['title']+' · 页面版本')
 with page.expect_response(lambda r:'/schemes/update' in r.url) as response:dialog.get_by_role('button',name='保存草稿',exact=True).click()
 result(response);expect(dialog).not_to_be_visible();passed('new version captures current source configuration and saves a separate draft')
 ui_row=row_for(page,ui_version);ui_row.get_by_role('button',name='确认',exact=True).click();review=page.get_by_role('dialog').filter(has=page.get_by_text('确认方案配置版本',exact=True));review.get_by_role('button',name='提交评审',exact=True).click();expect(review.get_by_text('请填写评审依据。',exact=True)).to_be_visible()
 review.get_by_label('评审依据 *',exact=True).fill('页面回归：先验证期间重叠阻断')
 with page.expect_response(lambda r:'/schemes/review' in r.url) as response:review.get_by_role('button',name='提交评审',exact=True).click()
 assert response.value.json()['code']==1050950005;expect(page.get_by_text('该薪资组存在有效期重叠的已确认方案',exact=True)).to_be_visible();passed('browser requires evidence and rejects an overlapping confirmation')
 review.get_by_role('button',name='取消',exact=True).click();ui_row.get_by_role('button',name='编辑',exact=True).click();dialog=page.get_by_role('dialog').filter(has=page.get_by_text('维护方案草稿',exact=True))
 start=calendar_date(2030+ui_version//12,ui_version%12+1,1);end=(start+timedelta(days=32)).replace(day=1)-timedelta(days=1)
 dialog.get_by_label('生效结束（可选）',exact=True).fill('');dialog.get_by_label('生效结束（可选）',exact=True).press('Tab');date(dialog,'生效开始（确认必填）',start.isoformat());date(dialog,'生效结束（可选）',end.isoformat())
 with page.expect_response(lambda r:'/schemes/capture' in r.url) as response:dialog.get_by_role('button',name='抓取源配置并核对',exact=True).click()
 result(response)
 with page.expect_response(lambda r:'/schemes/update' in r.url) as response:dialog.get_by_role('button',name='保存草稿',exact=True).click()
 result(response);expect(dialog).not_to_be_visible();ui_row.get_by_role('button',name='确认',exact=True).click();review=page.get_by_role('dialog').filter(has=page.get_by_text('确认方案配置版本',exact=True));review.get_by_label('评审依据 *',exact=True).fill('页面回归：合成新期间配置确认，不触发工资计算')
 with page.expect_response(lambda r:'/schemes/review' in r.url) as response:review.get_by_role('button',name='提交评审',exact=True).click()
 result(response);expect(review).not_to_be_visible();expect(ui_row.get_by_role('button',name='编辑',exact=True)).to_have_count(0);passed('recaptured nonoverlapping version confirms and locks edits')
 page.get_by_label('核对薪资组 ID',exact=True).fill(str(fixture['groupId']));page.get_by_label('核对薪资组 ID',exact=True).press('Tab')
 for label,value in [('核对开始日期',start.isoformat()),('核对结束日期',end.isoformat())]:page.get_by_label(label,exact=True).fill(value);page.get_by_label(label,exact=True).press('Enter')
 page.get_by_role('heading',name='按期间核对配置版本',exact=True).click()
 with page.expect_response(lambda r:'/schemes/resolve' in r.url) as response:page.get_by_role('button',name='核对期间',exact=True).click()
 assert result(response)['id']==ui_id;expect(page.locator('[data-testid="scheme-period-match"]')).to_contain_text('V'+str(ui_version));screenshot(page,'payroll-schemes-period.png');passed('period lookup uses one actual confirmed configuration version')
 ui_row.get_by_role('button',name='停用',exact=True).click();review=page.get_by_role('dialog').filter(has=page.get_by_text('停用方案配置版本',exact=True));review.get_by_label('评审依据 *',exact=True).fill('页面回归停用合成配置版本，历史保留')
 with page.expect_response(lambda r:'/schemes/review' in r.url) as response:review.get_by_role('button',name='提交评审',exact=True).click()
 result(response);expect(review).not_to_be_visible();expect(ui_row.get_by_text('已停用',exact=True)).to_be_visible();passed('retirement preserves the version while removing its period availability')
 reader=browser.new_context(viewport={'width':1440,'height':1050},locale='zh-CN');seed(reader,'HRM_SCHEME_READER_TOKEN','HRM_SCHEME_READER_REFRESH_TOKEN');rp=reader.new_page();open_page(rp);search(rp)
 for name in ['登记方案版本','编辑','确认','停用','另建版本']:expect(rp.get_by_role('button',name=name,exact=True)).to_have_count(0)
 expect(row_for(rp,2).get_by_role('button',name='详情',exact=True)).to_be_visible();passed('read-only role keeps configuration views without mutation controls');reader.close()
 limited=browser.new_context(viewport={'width':1440,'height':1050},locale='zh-CN');seed(limited,'HRM_SCHEME_SCHEMEONLY_TOKEN','HRM_SCHEME_SCHEMEONLY_REFRESH_TOKEN');lp=limited.new_page()
 with lp.expect_response(lambda r:'/schemes/page' in r.url) as response:lp.goto(route,wait_until='domcontentloaded')
 assert response.value.json()['code']==1050950007;expect(lp.locator('[data-testid="scheme-table"]').get_by_role('cell')).to_have_count(0);expect(lp.get_by_text('操作未成功，请核对页面提示或刷新后重试。',exact=True)).to_be_visible();screenshot(lp,'payroll-schemes-restricted.png');passed('scheme-only permission cannot expose saved source tax or catalogue data');limited.close()
 mobile=browser.new_context(viewport={'width':390,'height':844},is_mobile=True,has_touch=True,locale='zh-CN');seed(mobile);mp=mobile.new_page();open_page(mp);search(mp);assert mp.evaluate('document.documentElement.scrollWidth<=window.innerWidth');screenshot(mp,'payroll-schemes-mobile.png')
 mp.get_by_role('button',name='登记方案版本',exact=True).click();dialog=mp.get_by_role('dialog').filter(has=mp.get_by_text('登记方案配置版本',exact=True));expect(dialog).to_be_visible();assert mp.evaluate('document.documentElement.scrollWidth<=window.innerWidth');dialog.get_by_role('button',name='保存草稿',exact=True).click();expect(dialog.get_by_text('请选择薪资组、填写名称并抓取源配置核对。',exact=True)).to_be_visible();dialog.get_by_role('button',name='取消',exact=True).click();passed('390px mobile stacks forms and contains table scrolling');mobile.close()
 assert not errors,errors;passed('browser has no uncaught page errors');context.close();browser.close()
(args.output_dir/'hrm-scheme-ui-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2));print('All',len(checks),'scheme browser checks passed')
