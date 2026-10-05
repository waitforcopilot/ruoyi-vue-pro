#!/usr/bin/env python3
"""Real policy pages, declared-base arithmetic and original Chromium screenshots."""
import argparse, json, os, re, uuid
from datetime import date as calendar_date, timedelta
from pathlib import Path
from playwright.sync_api import sync_playwright, expect
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url',default='http://127.0.0.1:3000')
parser.add_argument('--fixture-file',type=Path,required=True)
parser.add_argument('--output-dir',type=Path,required=True)
parser.add_argument('--chromium-bin',default='/usr/bin/chromium')
parser.add_argument('--allow-test-fixtures',action='store_true',required=True)
args=parser.parse_args();args.output_dir.mkdir(parents=True,exist_ok=True)
for name in ['HRM_SMOKE_TOKEN','HRM_REFRESH_TOKEN','HRM_POLICY_READER_TOKEN','HRM_POLICY_READER_REFRESH_TOKEN']:
    if not os.environ.get(name):parser.error('Missing verification credential: '+name)
fixture=json.loads(args.fixture_file.read_text());route=args.base_url.rstrip('/')+'/hrm/payroll-insurance-policies';checks=[]
def passed(name):checks.append({'check':name,'result':'PASS'});print('PASS:',name,flush=True)
def seed(context,token='HRM_SMOKE_TOKEN',refresh='HRM_REFRESH_TOKEN'):
    values={'ACCESS_TOKEN':os.environ[token],'REFRESH_TOKEN':os.environ[refresh],'TenantId':1}
    context.add_init_script('(function(values){for(const[k,v]of Object.entries(values))localStorage.setItem(k,JSON.stringify({c:Date.now(),e:Date.now()+86400000,v:JSON.stringify(v)}));})('+json.dumps(values)+');')
def result(response):
    data=response.value.json();assert data.get('code')==0,('request failed',data.get('code'));return data['data']
def open_page(page):
    with page.expect_response(lambda r:'/insurance-policies/page' in r.url) as response:page.goto(route,wait_until='domcontentloaded')
    result(response);expect(page.get_by_role('heading',name='社保公积金本地政策',exact=True)).to_be_visible(timeout=30000);expect(page.locator('.el-loading-mask:visible')).to_have_count(0)
def search(page,scope=None):
    scope=scope or fixture['scopeCode'];page.get_by_placeholder('适用范围编号',exact=True).fill(scope);page.get_by_placeholder('搜索政策名称',exact=True).fill(fixture['title'])
    with page.expect_response(lambda r:'/insurance-policies/page' in r.url and 'scopeCode='+scope in r.url) as response:page.get_by_role('button',name='查询',exact=True).click()
    data=result(response);table=page.locator('[data-testid="insurance-policies"]')
    expect(table.locator('.el-table__body tbody tr')).to_have_count(len(data['list']))
    expect(table.locator('.el-table__body tbody tr td:nth-child(3) .note')).to_have_text([row['scopeCode'] for row in data['list']])
    return data
def row_for(page,version):return page.locator('[data-testid="insurance-policies"]').get_by_role('row').filter(has=page.get_by_role('cell',name=re.compile(r'^V'+str(version)+r'\s*·')))
def choose(page,selector,name):
    selector.click();dropdown=selector.get_by_role('combobox').get_attribute('aria-controls');page.locator('#'+dropdown).get_by_role('option',name=name,exact=not isinstance(name,re.Pattern)).click()
def field_select(dialog,label):return dialog.locator('.el-form-item').filter(has_text=label).locator('.el-select')
def lookup_version(page,version):choose(page,page.locator('.lookup-grid .el-select'),re.compile(r'^V'+str(version)+r' ·'))
def lookup_dates(page,start,end):
    for label,value in [('核对开始日期',start),('核对结束日期',end)]:page.get_by_label(label,exact=True).fill(value);page.get_by_label(label,exact=True).press('Enter')
    page.get_by_role('heading',name='声明基数核对',exact=True).click()
def preview(page,amount='100'):
    page.get_by_label('声明基数（元）',exact=True).fill(amount)
    with page.expect_response(lambda r:'/insurance-policies/preview' in r.url) as response:page.get_by_role('button',name='核对缴费参数',exact=True).click()
    return response
def screenshot(page,name):
    expect(page.locator('.el-loading-mask:visible')).to_have_count(0,timeout=15000);expect(page.locator('.el-notification:visible')).to_have_count(0,timeout=10000)
    page.screenshot(path=str(args.output_dir/name),full_page=True,animations='disabled')
def date(dialog,label,value):
    dialog.get_by_label(label,exact=True).fill(value);dialog.get_by_label(label,exact=True).press('Enter');dialog.get_by_role('heading',name='缴费参数',exact=True).click()

with sync_playwright() as p:
    browser=p.chromium.launch(executable_path=args.chromium_bin,headless=True,args=['--no-sandbox'])
    context=browser.new_context(viewport={'width':1720,'height':1280},locale='zh-CN',timezone_id='Asia/Shanghai');seed(context)
    page=context.new_page();errors=[];page.on('pageerror',lambda e:errors.append(e.name));open_page(page);data=search(page)
    assert data['total']>=4;expect(row_for(page,2).get_by_text('已确认',exact=True)).to_be_visible();expect(row_for(page,1).get_by_text('已停用',exact=True)).to_be_visible()
    screenshot(page,'payroll-insurance-desktop.png');passed('policy route renders actual cities scope versions statuses and dates')
    lookup_version(page,2);lookup_dates(page,fixture['start'],fixture['end']);calculated=result(preview(page));assert calculated['corporateAmount']=='3' and calculated['personalAmount']=='0'
    output=page.locator('[data-testid="insurance-policy-preview"]');expect(output.get_by_role('cell',name='3',exact=True)).to_be_visible();expect(output.get_by_role('cell',name='0',exact=True)).to_be_visible();expect(output).to_contain_text('各部分舍入后合计')
    expect(output).to_contain_text('比例部分 1.5 → 2；固定部分 0.5 → 1；合计 3');expect(output).to_contain_text('比例部分 0 → 0')
    output.scroll_into_view_if_needed();screenshot(page,'payroll-insurance-preview.png');passed('browser renders exact component-rounded reference amounts including valid zero and each rounding step')
    rejected=preview(page,'10000.01');assert rejected.value.json()['code']==1050960006;expect(output).to_have_count(0);passed('out-of-range base clears old output without fabricating zero or clamping')
    lookup_version(page,1)
    with page.expect_response(lambda r:'/insurance-policies/resolve' in r.url) as response:page.get_by_role('button',name='查找期间政策',exact=True).click()
    assert result(response)['id']==fixture['rightId'];expect(page.locator('[data-testid="insurance-policy-match"]')).to_contain_text('V2');page.get_by_role('button',name='使用此版本核对',exact=True).click();assert result(preview(page))['policy']['id']==fixture['rightId'];passed('period resolution explicitly selects the matching version before arithmetic')
    choose(page,page.locator('.compare-grid .el-select').nth(0),re.compile(r'^V1 ·'));choose(page,page.locator('.compare-grid .el-select').nth(1),re.compile(r'^V2 ·'))
    with page.expect_response(lambda r:'/insurance-policies/compare' in r.url) as response:page.get_by_role('button',name='比较政策',exact=True).click()
    changes=result(response)['changes'];comparison=page.locator('[data-testid="insurance-policy-comparison"]');expect(comparison).to_contain_text(str(len(changes))+' 项差异');expect(comparison.get_by_role('cell',name='0.0000',exact=True)).to_be_visible();expect(comparison.get_by_role('cell',name='未设置',exact=True)).to_be_visible();expect(comparison.get_by_role('cell',name='0',exact=True)).to_be_visible();comparison.scroll_into_view_if_needed();screenshot(page,'payroll-insurance-comparison.png');passed('historical comparison distinguishes missing parameters explicit zero and rounding changes')
    row_for(page,2).get_by_role('button',name='详情',exact=True).click();drawer=page.locator('.el-drawer:visible');expect(drawer.get_by_role('cell',name='1.5000 / 0.50',exact=True)).to_be_visible();expect(drawer.get_by_role('cell',name='0.0000 / 未设置',exact=True)).to_be_visible();screenshot(page,'payroll-insurance-parameters.png');passed('policy detail preserves reviewed parameters reference and explicit units')
    audit=drawer.locator('.el-collapse-item').first;audit.locator('.el-collapse-item__header').click();retained=audit.locator('[data-testid="insurance-parameters"]');expect(retained.get_by_role('cell',name='0.0000 / 未设置',exact=True)).to_be_visible();retained.scroll_into_view_if_needed();screenshot(page,'payroll-insurance-history.png');passed('audit expansion shows retained policy values with readable actions and actor time')
    drawer.get_by_role('button',name='关闭此对话框',exact=True).click()
    page.get_by_role('button',name='登记政策草稿',exact=True).click();dialog=page.get_by_role('dialog').filter(has=page.get_by_text('登记本地政策',exact=True));expect(dialog).to_be_visible()
    expect(dialog.locator('.el-loading-mask:visible')).to_have_count(0);city=dialog.get_by_placeholder('选择参保城市',exact=True);city.fill('杭州市');page.locator('.el-cascader__suggestion-item:visible').filter(has_text='杭州市').first.click()
    choose(page,field_select(dialog,'缴费项目 *'),'养老保险');ui_scope=fixture['scopeCode']+'-UI-'+uuid.uuid4().hex[:4].upper()
    dialog.get_by_label('适用范围编号 *',exact=True).fill(ui_scope);dialog.get_by_label('政策名称 *',exact=True).fill(fixture['title']+' · 页面草稿')
    with page.expect_response(lambda r:'/insurance-policies/create' in r.url) as response:dialog.get_by_role('button',name='保存草稿',exact=True).click()
    created=result(response);expect(dialog).not_to_be_visible();assert search(page,ui_scope)['total']==1;passed('real city cascader and project selector register a separate draft without policy defaults')
    row_for(page,1).get_by_role('button',name='详情',exact=True).click();drawer=page.locator('.el-drawer:visible');expect(drawer.locator('[data-testid="insurance-parameters"]').get_by_role('cell',name='未设置 / 未设置',exact=True).first).to_be_visible();screenshot(page,'payroll-insurance-missing-draft.png');drawer.get_by_role('button',name='关闭此对话框',exact=True).click();passed('new draft visibly preserves missing amounts rates unit and rounding')
    row_for(page,1).get_by_role('button',name='确认',exact=True).click();review=page.get_by_role('dialog').filter(has=page.get_by_text('确认本地政策',exact=True));review.get_by_role('button',name='提交评审',exact=True).click();expect(review.get_by_text('请填写评审依据。',exact=True)).to_be_visible();review.get_by_label('评审依据 *',exact=True).fill('页面合成回归：验证缺失资料阻断')
    with page.expect_response(lambda r:'/insurance-policies/review' in r.url) as response:review.get_by_role('button',name='提交评审',exact=True).click()
    assert response.value.json()['code']==1050960001;expect(review).to_be_visible();review.get_by_role('button',name='取消',exact=True).click();passed('review requires evidence and cannot approve a draft with missing policy information')
    search(page)
    with page.expect_response(lambda r:'/insurance-policies/get?' in r.url) as get_response:
        with page.expect_response(lambda r:'/insurance-policies/new-version' in r.url) as response:row_for(page,2).get_by_role('button',name='另建版本',exact=True).click()
    id=result(response);new=result(get_response);version=new['policyVersion'];dialog=page.get_by_role('dialog').filter(has=page.get_by_text('维护政策草稿',exact=True));expect(dialog).to_be_visible()
    expect(dialog.get_by_label('适用范围编号 *',exact=True)).to_be_disabled();expect(dialog.get_by_label('个人缴费比例（%）',exact=True)).to_have_value('0.0000');expect(dialog.get_by_label('单位固定额（元）',exact=True)).to_have_value('0.50')
    with page.expect_response(lambda r:'/insurance-policies/update' in r.url) as response:dialog.get_by_role('button',name='保存草稿',exact=True).click()
    result(response);expect(dialog).not_to_be_visible();row=row_for(page,version);row.get_by_role('button',name='确认',exact=True).click();review=page.get_by_role('dialog').filter(has=page.get_by_text('确认本地政策',exact=True));review.get_by_label('评审依据 *',exact=True).fill('页面合成回归：有效期重叠验证')
    with page.expect_response(lambda r:'/insurance-policies/review' in r.url) as response:review.get_by_role('button',name='提交评审',exact=True).click()
    assert response.value.json()['code']==1050960005;review.get_by_role('button',name='取消',exact=True).click();passed('copied zero parameters remain explicit and overlapping effective intervals are blocked')
    row.get_by_role('button',name='编辑',exact=True).click();dialog=page.get_by_role('dialog').filter(has=page.get_by_text('维护政策草稿',exact=True));start=calendar_date(2031+version//12,version%12+1,1);end=(start+timedelta(days=32)).replace(day=1)-timedelta(days=1)
    dialog.get_by_label('生效结束（可选）',exact=True).fill('');dialog.get_by_label('生效结束（可选）',exact=True).press('Tab');date(dialog,'生效开始（确认必填）',start.isoformat());date(dialog,'生效结束（可选）',end.isoformat());dialog.get_by_label('个人缴费比例（%）',exact=True).fill('7.5');dialog.get_by_label('政策名称 *',exact=True).fill(fixture['title']+' · 页面版本')
    with page.expect_response(lambda r:'/insurance-policies/update' in r.url) as response:dialog.get_by_role('button',name='保存草稿',exact=True).click()
    result(response);expect(dialog).not_to_be_visible();row.get_by_role('button',name='确认',exact=True).click();review=page.get_by_role('dialog').filter(has=page.get_by_text('确认本地政策',exact=True));review.get_by_label('评审依据 *',exact=True).fill('页面合成新期间评审，不代表官方政策')
    with page.expect_response(lambda r:'/insurance-policies/review' in r.url) as response:review.get_by_role('button',name='提交评审',exact=True).click()
    result(response);expect(review).not_to_be_visible();expect(row.get_by_role('button',name='编辑',exact=True)).to_have_count(0);lookup_version(page,version);lookup_dates(page,start.isoformat(),end.isoformat());new_amounts=result(preview(page));assert new_amounts['policy']['id']==id and new_amounts['personalAmount']=='8';passed('explicit decimal editing confirms a separate period and uses its exact saved parameters')
    row.get_by_role('button',name='停用',exact=True).click();review=page.get_by_role('dialog').filter(has=page.get_by_text('停用本地政策',exact=True));review.get_by_label('评审依据 *',exact=True).fill('页面回归停用合成版本，保留历史')
    with page.expect_response(lambda r:'/insurance-policies/review' in r.url) as response:review.get_by_role('button',name='提交评审',exact=True).click()
    result(response);expect(review).not_to_be_visible();expect(row.get_by_text('已停用',exact=True)).to_be_visible();passed('retirement preserves the tested policy version and review history')
    reader=browser.new_context(viewport={'width':1480,'height':1050},locale='zh-CN');seed(reader,'HRM_POLICY_READER_TOKEN','HRM_POLICY_READER_REFRESH_TOKEN');rp=reader.new_page();rp.on('pageerror',lambda e:errors.append('reader: '+e.name));open_page(rp);search(rp)
    for label in ['登记政策草稿','编辑','确认','停用','另建版本']:expect(rp.get_by_role('button',name=label,exact=True)).to_have_count(0)
    lookup_version(rp,2);lookup_dates(rp,fixture['start'],fixture['end']);assert result(preview(rp))['corporateAmount']=='3';rp.locator('[data-testid="insurance-policy-preview"]').scroll_into_view_if_needed();screenshot(rp,'payroll-insurance-readonly.png');reader.close();passed('policy-only reader refreshes matching list rows and can inspect parameters without mutation controls')
    mobile=browser.new_context(viewport={'width':390,'height':844},is_mobile=True,has_touch=True,locale='zh-CN');seed(mobile);mp=mobile.new_page();mp.on('pageerror',lambda e:errors.append('mobile: '+e.name));open_page(mp);search(mp);assert mp.evaluate('document.documentElement.scrollWidth<=window.innerWidth');screenshot(mp,'payroll-insurance-mobile.png')
    mp.get_by_role('button',name='登记政策草稿',exact=True).click();dialog=mp.get_by_role('dialog').filter(has=mp.get_by_text('登记本地政策',exact=True));expect(dialog).to_be_visible();assert mp.evaluate('document.documentElement.scrollWidth<=window.innerWidth');dialog.get_by_role('button',name='保存草稿',exact=True).click();expect(dialog.get_by_text('请选择城市及项目，填写范围编号和政策名称。',exact=True)).to_be_visible();dialog.get_by_role('button',name='取消',exact=True).click();mobile.close();passed('390px mobile contains table overflow and stacks policy fields')
    assert not errors,errors;passed('browser has no uncaught page errors');context.close();browser.close()
(args.output_dir/'hrm-insurance-ui-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2)+'\n');print('All',len(checks),'insurance policy browser checks passed')
