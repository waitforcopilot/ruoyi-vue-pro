#!/usr/bin/env python3
"""Exercise actual scheme-bound trial editing and saved sources; capture original screenshots."""
import argparse,json,os,re
from pathlib import Path
from urllib.parse import urlsplit
from playwright.sync_api import sync_playwright,expect
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--base-url',default='http://127.0.0.1:3000');p.add_argument('--fixture-file',type=Path,required=True);p.add_argument('--output-dir',type=Path,required=True);p.add_argument('--chromium-bin',default='/usr/bin/chromium');p.add_argument('--allow-test-fixtures',action='store_true',required=True);a=p.parse_args()
if urlsplit(a.base_url).hostname not in ['127.0.0.1','localhost']:p.error('Use the loopback synthetic QA service')
f=json.loads(a.fixture_file.read_text());assert f.get('synthetic');a.output_dir.mkdir(parents=True,exist_ok=True);checks=[];page_errors=[]
for role in ['maker','reader']:
 for suffix in ['TOKEN','REFRESH_TOKEN']:
  if not os.environ.get('HRM_REVIEW_'+role.upper()+'_'+suffix):p.error('Missing '+role+' credential')
def passed(name):checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def seed(context,role):
 values={'ACCESS_TOKEN':os.environ['HRM_REVIEW_'+role.upper()+'_TOKEN'],'REFRESH_TOKEN':os.environ['HRM_REVIEW_'+role.upper()+'_REFRESH_TOKEN'],'TenantId':1}
 context.add_init_script('(function(values){for(const[k,v]of Object.entries(values))localStorage.setItem(k,JSON.stringify({c:Date.now(),e:Date.now()+86400000,v:JSON.stringify(v)}));})('+json.dumps(values)+');')
def result(response):
 r=response.value.json();assert r.get('code')==0,(r.get('code'),r.get('msg'));return r['data']
def open_page(page,id):
 page.on('pageerror',lambda e:page_errors.append(str(e)))
 page.goto(a.base_url+'/hrm/payroll-trial-batches?batchId='+str(id),wait_until='domcontentloaded');expect(page.get_by_role('button',name='核验资料',exact=True)).to_be_visible(timeout=30000)
 with page.expect_response(lambda r:'/trial-batches/check?' in r.url) as response:page.get_by_role('button',name='核验资料',exact=True).click()
 result(response);expect(page.get_by_test_id('trial-check')).to_be_visible()
def search(page,name):
 page.get_by_label('查询批次主体',exact=True).fill(f['entityCode']);page.get_by_label('查询批次名称',exact=True).fill(name)
 with page.expect_response(lambda r:'/trial-batches/page' in r.url) as response:page.get_by_role('button',name='查询',exact=True).click()
 return result(response)
def row(page,code):return page.locator('[data-testid="trial-table"] .el-table__body tbody tr').filter(has_text=code)
def edit(page,code):
 row(page,code).get_by_role('button',name='编辑',exact=True).click();d=page.get_by_role('dialog').filter(has=page.get_by_role('button',name='保存草稿',exact=True));expect(d).to_be_visible();expect(d.get_by_role('button',name='保存草稿',exact=True)).to_be_enabled(timeout=20000);return d
def choose(page,label,name,partial=False):
 c=page.get_by_role('combobox',name=label,exact=True);page.locator('.el-select').filter(has=c).click();options=page.locator('#'+c.get_attribute('aria-controls')).get_by_role('option',name=name,exact=not partial);expect(options).to_have_count(1,timeout=20000);options.click()
def save(page,d):
 with page.expect_response(lambda r:'/trial-batches/update' in r.url) as response:d.get_by_role('button',name='保存草稿',exact=True).click()
 result(response);expect(d).not_to_be_visible()
def execute(page):
 with page.expect_response(lambda r:'/trial-batches/check?' in r.url) as response:page.get_by_role('button',name='核验资料',exact=True).click()
 assert result(response)['ready']
 with page.expect_response(lambda r:'/trial-batches/execute' in r.url) as response:page.get_by_role('button',name='保存试算版本',exact=True).click()
 return result(response)
def source(page):
 s=page.get_by_test_id('trial-scheme-snapshot')
 if not s.is_visible():page.get_by_text('方案版本与输入来源绑定',exact=True).click()
 expect(s).to_be_visible();return s
def screenshot(page,name,target=None):
 if target is not None:target.scroll_into_view_if_needed()
 expect(page.locator('.el-loading-mask:visible')).to_have_count(0,timeout=20000);page.screenshot(path=str(a.output_dir/name),full_page=True,animations='disabled')
with sync_playwright() as pw:
 browser=pw.chromium.launch(executable_path=a.chromium_bin,args=['--no-sandbox']);context=browser.new_context(viewport={'width':1512,'height':1100},locale='zh-CN');seed(context,'maker');page=context.new_page();open_page(page,f['batchId']);expect(page.get_by_test_id('trial-run')).to_contain_text(f['net']);s=source(page);expect(s).to_contain_text(f['schemeTitle']+' · V2');expect(s).to_contain_text(f['optionName']);expect(s.locator('.source-row')).to_have_count(9);screenshot(page,'payroll-binding-snapshot.png',s);passed('saved version displays exact money and nine original scheme/manual source associations')
 search(page,'BINDING BROWSER');d=edit(page,f['batchCode']);editor=d.get_by_test_id('trial-scheme-editor');expect(editor.locator('.source-card')).to_have_count(9);expect(editor.locator('.el-select').filter(has=page.get_by_role('combobox',name='baseSalary 来源类型',exact=True))).to_contain_text('方案工资项关联');expect(editor.locator('.el-select').filter(has=page.get_by_role('combobox',name='baseSalary 工资项',exact=True))).to_contain_text(f['optionName']+' · #'+str(f['optionId']));expect(editor).to_contain_text('此批次已有关联方案');expect(editor.locator('.el-select__clear')).to_have_count(0);screenshot(page,'payroll-binding-editor.png',editor);passed('structured editor loads explicit selected item and preserves historical access requirement')
 expect(editor.locator('.el-select').filter(has=page.get_by_role('combobox',name='withheldTax 来源类型',exact=True))).to_contain_text('独立录入来源');expect(editor.get_by_label('withheldTax 工资项',exact=True)).to_have_count(0);passed('manual source has declared basis and no implied scheme item')
 basis=editor.get_by_label('baseSalary 来源依据',exact=True);old_basis=basis.input_value();basis.fill(' ')
 with page.expect_response(lambda r:'/trial-batches/update' in r.url) as response:d.get_by_role('button',name='保存草稿',exact=True).click()
 assert response.value.json()['code']!=0;expect(d).to_be_visible();expect(page.get_by_role('alert').filter(has_text='不能为空')).to_be_visible();screenshot(page,'payroll-binding-missing-basis.png',editor);basis.fill('浏览器核对后的合成工资项来源');save(page,d);expect(page.get_by_test_id('trial-run')).to_contain_text('试算 V1');passed('missing source basis stays in editor and valid edit retains previous immutable result')
 r=execute(page);assert r['runVersion']==2 and r['result']['totals']['net']==f['net'];expect(page.get_by_test_id('trial-run')).to_contain_text('试算 V2');s=source(page);expect(s).to_contain_text('浏览器核对后的合成工资项来源');screenshot(page,'payroll-binding-retrial.png',s);passed('editing source basis creates V2 with saved policy association and equal wages')
 page.get_by_role('tab',name='版本比较',exact=True).click();choose(page,'比较基准版本','V1');choose(page,'比较目标版本','V2')
 with page.expect_response(lambda r:'/trial-batches/compare?' in r.url) as response:page.get_by_role('button',name='比较版本',exact=True).click()
 diff=result(response);assert diff['ruleChanged'] and all(v=='0.00' for v in diff['totalDifferences'].values());expect(page.get_by_test_id('trial-comparison')).to_contain_text('方案或来源绑定有变化');screenshot(page,'payroll-binding-comparison.png',page.get_by_test_id('trial-comparison'));passed('source changes are visible separately from unchanged wage amounts')
 page.get_by_role('tab',name='试算版本',exact=True).click();choose(page,'查看试算版本','V1 ·',True);s=source(page);expect(s).to_contain_text(old_basis);expect(s).not_to_contain_text('浏览器核对后的合成工资项来源');screenshot(page,'payroll-binding-history.png',s);passed('older trial shows its own original source basis rather than current draft')
 search(page,'BINDING UI-UNBOUND');d=edit(page,f['unboundBatchCode']);choose(page,'试算方案版本',f['schemeTitle']+' · 绑定合成方案组 V2');editor=d.get_by_test_id('trial-scheme-editor');expect(editor.locator('.source-card')).to_have_count(9)
 for key in f['inputKeys']:
  choose(page,key+' 来源类型','方案工资项关联' if key=='baseSalary' else '独立录入来源')
  if key=='baseSalary':choose(page,key+' 工资项',f['optionName']+' · #'+str(f['optionId']))
  editor.get_by_label(key+' 来源依据',exact=True).fill('浏览器明确登记 '+key+' 合成来源')
 screenshot(page,'payroll-binding-new-associations.png',editor);save(page,d);r=execute(page);assert r['result']['scheme']['id']==f['schemeId'] and r['result']['totals']['net']==f['net'];passed('browser can associate an independent draft with a selected version and explicitly declare all sources')
 restricted=browser.new_context(viewport={'width':1512,'height':1100},locale='zh-CN');seed(restricted,'reader');reader=restricted.new_page();reader.goto(a.base_url+'/hrm/payroll-trial-batches',wait_until='domcontentloaded');expect(reader.get_by_role('heading',name='批次资料核验与试算',exact=True)).to_be_visible(timeout=30000);assert search(reader,'BINDING')['total']==0;screenshot(reader,'payroll-binding-restricted.png');passed('query-only reader without policy access cannot count or view bound batches')
 oldpage=context.new_page();open_page(oldpage,f['oldBatchId']);expect(oldpage.get_by_test_id('trial-check')).to_contain_text('方案须已确认');s=source(oldpage);expect(s).to_contain_text(f['schemeTitle']+' · V1');expect(oldpage.get_by_test_id('trial-run')).to_contain_text(f['net']);screenshot(oldpage,'payroll-binding-retired.png',s);passed('retired scheme blocks current qualification while original saved policy and wage result stay readable')
 mobilecontext=browser.new_context(viewport={'width':390,'height':844},is_mobile=True,locale='zh-CN');seed(mobilecontext,'maker');mobile=mobilecontext.new_page();open_page(mobile,f['batchId']);s=source(mobile);expect(s).to_contain_text('浏览器核对后的合成工资项来源');assert mobile.evaluate('document.documentElement.scrollWidth<=window.innerWidth');screenshot(mobile,'payroll-binding-mobile-snapshot.png',s);search(mobile,'BINDING BROWSER');d=edit(mobile,f['batchCode']);editor=d.get_by_test_id('trial-scheme-editor');expect(editor.locator('.source-card')).to_have_count(9);assert mobile.evaluate('document.documentElement.scrollWidth<=window.innerWidth');screenshot(mobile,'payroll-binding-mobile-editor.png',editor);passed('390px source cards and structured editor remain readable without document overflow')
 capture=context.new_page();open_page(capture,f['batchId']);snapshot=source(capture);expect(snapshot.locator('.source-row')).to_have_count(9)
 capture.wait_for_function("() => !document.querySelector('.el-collapse-item__wrap')?.classList.contains('el-collapse-transition')")
 snapshot.screenshot(path=str(a.output_dir/'payroll-binding-all-sources.png'),animations='disabled')
 assert not page_errors,page_errors;passed('browser has no uncaught page errors')
 (a.output_dir/'hrm-binding-browser-results.json').write_text(json.dumps({'total':len(checks),'passed':len(checks),'checks':checks,'syntheticFixtures':True,'realBpmEngine':True,'pageErrors':page_errors,'screenshots':sorted(p.name for p in a.output_dir.glob('*.png'))},ensure_ascii=False,indent=2)+'\n');browser.close()
print('PASS:',len(checks),'actual binding browser checks')
