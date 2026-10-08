#!/usr/bin/env python3
"""Verify real payroll review pages with distinct actors and capture original desktop/mobile screenshots."""
import argparse,json,os,re
from urllib.parse import parse_qs,urlsplit
from pathlib import Path
from playwright.sync_api import sync_playwright,expect
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--base-url',default='http://127.0.0.1:3000');p.add_argument('--fixture-file',type=Path,required=True);p.add_argument('--output-dir',type=Path,required=True);p.add_argument('--chromium-bin',default='/usr/bin/chromium');p.add_argument('--allow-test-fixtures',action='store_true',required=True);a=p.parse_args();a.output_dir.mkdir(parents=True,exist_ok=True)
for role in ['maker','hr','finance','reader']:
 for suffix in ['TOKEN','REFRESH_TOKEN']:
  key='HRM_REVIEW_'+role.upper()+'_'+suffix
  if not os.environ.get(key):p.error('Missing credential: '+key)
f=json.loads(a.fixture_file.read_text());assert f.get('synthetic');route=a.base_url.rstrip('/')+'/hrm/payroll-trial-batches';checks=[]
def passed(name):checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def seed(context,role):
 values={'ACCESS_TOKEN':os.environ['HRM_REVIEW_'+role.upper()+'_TOKEN'],'REFRESH_TOKEN':os.environ['HRM_REVIEW_'+role.upper()+'_REFRESH_TOKEN'],'TenantId':1}
 context.add_init_script('(function(values){for(const[k,v]of Object.entries(values))localStorage.setItem(k,JSON.stringify({c:Date.now(),e:Date.now()+86400000,v:JSON.stringify(v)}));})('+json.dumps(values)+');')
def result(response):
 data=response.value.json();assert data.get('code')==0,('HTTP failure',data.get('code'),data.get('msg'));return data['data']
def open_page(page):
 with page.expect_response(lambda r:'/trial-batches/page' in r.url) as response:page.goto(route,wait_until='domcontentloaded')
 result(response);expect(page.get_by_role('heading',name='批次资料核验与试算',exact=True)).to_be_visible(timeout=30000)
def search(page,title='BROWSER'):
 page.get_by_label('查询批次主体',exact=True).fill(f['entityCode']);page.get_by_label('查询批次名称',exact=True).fill(title)
 with page.expect_response(lambda r:'/trial-batches/page' in r.url) as response:page.get_by_role('button',name='查询',exact=True).click()
 result(response)
def row(page,code):return page.locator('[data-testid="trial-table"] .el-table__body tbody tr').filter(has_text=code)
def select(page,code=None):
 with page.expect_response(lambda r:'/review/get?' in r.url) as response:row(page,code or f['batchCode']).get_by_role('button',name='核验与版本',exact=True).click()
 view=result(response);expect(page.locator('[data-testid="payroll-review-panel"]')).to_be_visible();return view

def panel(page):return page.locator('[data-testid="payroll-review-panel"]')
def screenshot(page,name,target=None):
 if target is not None:target.scroll_into_view_if_needed()
 expect(page.locator('.el-loading-mask:visible')).to_have_count(0,timeout=20000)
 expect(page.locator('.el-notification:visible')).to_have_count(0,timeout=20000)
 page.screenshot(path=str(a.output_dir/name),full_page=True,animations='disabled')
def choose(page,label,name):
 control=page.get_by_role('combobox',name=label,exact=True);page.locator('.el-select').filter(has=control).click();dropdown=control.get_attribute('aria-controls');page.locator('#'+dropdown).get_by_role('option',name=name,exact=True).click()
def action(page,label,evidence):
 panel(page).get_by_label('复核操作依据',exact=True).fill(evidence)
 with page.expect_response(lambda r:'/review/action' in r.url) as response:panel(page).get_by_role('button',name=label,exact=True).click()
 data=result(response);expect(panel(page).locator('.el-loading-mask:visible')).to_have_count(0,timeout=20000);return data

def refresh(page):
 with page.expect_response(lambda r:'/review/get?' in r.url) as response:panel(page).get_by_role('button',name='刷新并重新核对',exact=True).click()
 result(response);expect(panel(page).locator('.el-loading-mask:visible')).to_have_count(0,timeout=20000)

with sync_playwright() as pw:
 browser=pw.chromium.launch(executable_path=a.chromium_bin,args=['--no-sandbox'])
 contexts={};pages={}
 for role in ['maker','hr','finance','reader']:
  c=browser.new_context(viewport={'width':1512,'height':1050},locale='zh-CN');seed(c,role);pg=c.new_page();open_page(pg);search(pg);contexts[role]=c;pages[role]=pg
 maker=pages['maker'];initial=select(maker);expect(panel(maker).get_by_role('button',name='提交两级复核',exact=True)).to_be_visible();screenshot(maker,'payroll-review-ready.png',panel(maker));passed('current run ready for explicitly assigned two-stage review')
 panel(maker).get_by_role('button',name='提交两级复核',exact=True).click();expect(panel(maker)).to_contain_text('请填写本次核对意见与依据');passed('empty evidence rejected before submit')
 choose(maker,'HR 复核人','合成 HR 复核人');choose(maker,'财务复核人','合成财务复核人');panel(maker).get_by_label('复核操作依据',exact=True).fill('浏览器合成资料核对：应发、扣款、个税及实发已核对')
 committed=[];dropped=[];bodies=[]
 def lose(route):
  bodies.append(route.request.post_data_json);response=route.fetch();data=response.json();assert data['code']==0;committed.append(data['data']);route.abort('failed');dropped.append(True)
 maker.route('**/review/action',lose);panel(maker).get_by_role('button',name='提交两级复核',exact=True).click()
 for _ in range(200):
  if dropped:break
  maker.wait_for_timeout(50)
 assert dropped
 retry=panel(maker).get_by_role('button',name='重试提交两级复核',exact=True);expect(retry).to_be_visible();expect(retry).not_to_have_class(re.compile('is-loading'));maker.unroute('**/review/action',lose)
 maker.on('request',lambda r:bodies.append(r.post_data_json) if '/review/action' in r.url else None)
 with maker.expect_response(lambda r:'/review/action' in r.url) as response:retry.click()
 recovered=result(response);assert recovered==committed[0] and bodies[0]==bodies[1];expect(panel(maker)).to_contain_text('复核中');expect(row(maker,f['batchCode']).get_by_role('button',name='编辑',exact=True)).to_have_count(0);expect(maker.get_by_role('button',name='保存试算版本',exact=True)).to_be_disabled();screenshot(maker,'payroll-review-submitted.png',panel(maker));passed('lost submit response reuses identical command and creates one cycle')
 hr=pages['hr'];hv=select(hr);expect(panel(hr).get_by_role('button',name='通过本级复核',exact=True)).to_be_visible();expect(panel(hr).get_by_role('button',name='冻结复核版本',exact=True)).to_have_count(0);screenshot(hr,'payroll-review-hr.png',panel(hr));passed('assigned HR sees stage actions without freeze permission')
 reader=pages['reader'];select(reader)
 for name in ['提交两级复核','通过本级复核','驳回本轮复核','冻结复核版本','凭依据解冻','撤销本轮复核','管理撤销复核']:expect(panel(reader).get_by_role('button',name=name,exact=True)).to_have_count(0)
 screenshot(reader,'payroll-review-readonly.png',panel(reader));passed('read-only actor sees bound version and reviewers without mutations')
 finance=pages['finance'];select(finance);expect(panel(finance).get_by_role('button',name='通过本级复核',exact=True)).to_have_count(0);passed('finance cannot act before HR completes')
 hrview=action(hr,'通过本级复核','浏览器 HR 合成复核：人员资格及工资口径通过');assert hrview['batchStatus']==2;expect(panel(hr)).to_contain_text('等待');passed('HR evidence saved against original run without closing finance stage')
 refresh(finance);expect(panel(finance).get_by_role('button',name='通过本级复核',exact=True)).to_be_visible();screenshot(finance,'payroll-review-finance.png',panel(finance))
 approved=action(finance,'通过本级复核','浏览器财务合成复核：四项金额及来源通过');assert approved['batchStatus']==3;expect(panel(finance).get_by_role('button',name='冻结复核版本',exact=True)).to_be_visible();screenshot(finance,'payroll-review-approved.png',panel(finance));passed('native final approval produces reviewed state and independent freeze action')
 frozen=action(finance,'冻结复核版本','浏览器合成冻结：保留本轮已复核 V1');assert frozen['batchStatus']==4;expect(panel(finance)).to_contain_text('版本已冻结');expect(finance.get_by_role('button',name='保存试算版本',exact=True)).to_have_count(0);refresh(maker);expect(maker.get_by_role('button',name='保存试算版本',exact=True)).to_be_disabled();screenshot(finance,'payroll-review-frozen.png',panel(finance));passed('freeze locks exact run while editing and recalculation stay disabled')
 mobilecontext=browser.new_context(viewport={'width':390,'height':844},is_mobile=True,locale='zh-CN');seed(mobilecontext,'reader');mobile=mobilecontext.new_page();open_page(mobile);search(mobile);select(mobile);expect(panel(mobile)).to_contain_text('已冻结');screenshot(mobile,'payroll-review-mobile-frozen.png',panel(mobile));assert mobile.evaluate('document.documentElement.scrollWidth <= window.innerWidth');passed('mobile stages stack and overflow remains inside page')
 unfrozen=action(finance,'凭依据解冻','浏览器合成解冻：重新核对本轮依据');assert unfrozen['batchStatus']==1 and not unfrozen.get('activeReviewId');expect(panel(finance)).to_contain_text('已解冻');panel(finance).get_by_role('button',name=re.compile('第 1 轮')).click();screenshot(finance,'payroll-review-unfrozen.png',panel(finance));passed('unfreeze retains both old decisions and frozen evidence')
 # Submit another browser batch, then cancel it through the starter path with a recorded reason.
 select(maker,f['cancelBatchCode']);choose(maker,'HR 复核人','合成 HR 复核人');choose(maker,'财务复核人','合成财务复核人');action(maker,'提交两级复核','浏览器合成撤销场景资料核对');expect(panel(maker).get_by_role('button',name='撤销本轮复核',exact=True)).to_be_visible()
 cancelled=action(maker,'撤销本轮复核','浏览器合成撤销：需要重新登记试算输入');assert cancelled['batchStatus']==0;expect(panel(maker)).to_contain_text('已撤销');screenshot(maker,'payroll-review-cancelled.png',panel(maker));passed('starter cancellation preserves history and invalidates current trial')
 # A held response for another batch must not repaint the selected batch's review panel.
 hold=[]
 def delay(route):
  if parse_qs(urlsplit(route.request.url).query).get('batchId')==[str(f['readonlyBatchId'])]:
   hold.append((route,route.fetch()))
  else:route.continue_()
 reader.route('**/review/get?*',delay);row(reader,f['readonlyBatchCode']).get_by_role('button',name='核验与版本',exact=True).click()
 for _ in range(200):
  if hold:break
  reader.wait_for_timeout(50)
 assert hold
 with reader.expect_response(lambda r:'/review/get?' in r.url) as response:row(reader,f['batchCode']).get_by_role('button',name='核验与版本',exact=True).click()
 result(response)
 with reader.expect_response(lambda r:'/review/get?' in r.url and parse_qs(urlsplit(r.url).query).get('batchId')==[str(f['readonlyBatchId'])]) as delayed:
  hold[0][0].fulfill(response=hold[0][1])
 result(delayed);reader.evaluate('() => new Promise(resolve => setTimeout(resolve, 100))')
 expect(panel(reader)).to_contain_text('已解冻');expect(panel(reader)).not_to_contain_text('尚未提交复核');passed('late prior-batch response cannot overwrite current review history')
 reader.unroute('**/review/get?*',delay)
 (a.output_dir/'hrm-review-browser-results.json').write_text(json.dumps({'total':len(checks),'passed':len(checks),'checks':checks,'syntheticFixtures':True,'realBpmEngine':True,'screenshots':sorted(p.name for p in a.output_dir.glob('*.png'))},ensure_ascii=False,indent=2)+'\n');browser.close()
print('PASS:',len(checks),'actual browser checks')
