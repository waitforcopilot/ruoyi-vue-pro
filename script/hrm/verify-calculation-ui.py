#!/usr/bin/env python3
"""Exercise actual rule editor, cases and execution pages; retain original Chromium screenshots."""
import argparse
import json
import os
import re
import uuid
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url', default='http://127.0.0.1:3000')
parser.add_argument('--fixture-file', type=Path, required=True)
parser.add_argument('--output-dir', type=Path, required=True)
parser.add_argument('--chromium-bin', default='/usr/bin/chromium')
parser.add_argument('--allow-test-fixtures', action='store_true', required=True)
args = parser.parse_args()
args.output_dir.mkdir(parents=True, exist_ok=True)
for name in ['HRM_SMOKE_TOKEN','HRM_REFRESH_TOKEN','HRM_CALC_READER_TOKEN','HRM_CALC_READER_REFRESH_TOKEN']:
    if not os.environ.get(name): parser.error('Missing verification credential: '+name)
fixture = json.loads(args.fixture_file.read_text())
route = args.base_url.rstrip('/')+'/hrm/payroll-calculation'
checks = []

def passed(name):
    checks.append({'check':name,'result':'PASS'})
    print('PASS:',name,flush=True)
def seed(context, token='HRM_SMOKE_TOKEN', refresh='HRM_REFRESH_TOKEN'):
    values = {'ACCESS_TOKEN':os.environ[token],'REFRESH_TOKEN':os.environ[refresh],'TenantId':1}
    context.add_init_script('(function(values){for(const[k,v]of Object.entries(values))localStorage.setItem(k,JSON.stringify({c:Date.now(),e:Date.now()+86400000,v:JSON.stringify(v)}));})('+json.dumps(values)+');')
def result(response):
    data = response.value.json()
    assert data.get('code') == 0, ('request failed',data.get('code'))
    return data['data']
def open_page(page):
    with page.expect_response(lambda r:'/calculation-definitions/page' in r.url) as response:
        page.goto(route,wait_until='domcontentloaded')
    result(response)
    expect(page.get_by_role('heading',name='工资规则表达式与核对',exact=True)).to_be_visible(timeout=30000)
    expect(page.locator('.el-loading-mask:visible')).to_have_count(0)
def search(page,scope=None,title=None):
    scope = scope or fixture['scopeCode']
    page.get_by_placeholder('适用范围编号',exact=True).fill(scope)
    page.get_by_placeholder('搜索规则名称',exact=True).fill(title or fixture['title'])
    with page.expect_response(lambda r:'/calculation-definitions/page' in r.url and 'scopeCode='+scope in r.url) as response:
        page.get_by_role('button',name='查询',exact=True).click()
    data = result(response)
    table = page.locator('[data-testid="calculation-definitions"]')
    expect(table.locator('.el-table__body tbody tr')).to_have_count(len(data['list']))
    expect(table.locator('.el-table__body tbody tr td:nth-child(2) .note')).to_have_text([row['scopeCode'] for row in data['list']])
    return data
def row_for(page,version,code=None):
    return page.locator('[data-testid="calculation-definitions"]').get_by_role('row').filter(
        has=page.get_by_text(code or fixture['code'],exact=True)).filter(
        has=page.get_by_role('cell',name=re.compile(r'^V'+str(version)+r'\s*·')))
def choose(page,selector,name):
    selector.click()
    dropdown = selector.get_by_role('combobox').get_attribute('aria-controls')
    page.locator('#'+dropdown).get_by_role('option',name=name,exact=not isinstance(name,re.Pattern)).click()
def lookup_version(page,version,code=None):
    pattern = re.compile(re.escape(code or fixture['code'])+r' · V'+str(version)+r' ·')
    with page.expect_response(lambda r:'/calculation-definitions/get?' in r.url) as response:
        choose(page,page.locator('.lookup-grid .el-select'),pattern)
    return result(response)
def lookup_dates(page,start,end):
    for label,value in [('核对开始日期',start),('核对结束日期',end)]:
        page.get_by_label(label,exact=True).fill(value)
        page.get_by_label(label,exact=True).press('Enter')
    page.get_by_role('heading',name='明确输入核对',exact=True).click()
def preview(page,values=None):
    for key,value in (values or fixture['inputs']).items():
        page.get_by_label('核对输入 '+key,exact=True).fill(value)
    with page.expect_response(lambda r:'/calculation-definitions/preview' in r.url) as response:
        page.get_by_role('button',name='执行规则核对',exact=True).click()
    return response
def screenshot(page,name):
    expect(page.locator('.el-loading-mask:visible')).to_have_count(0,timeout=15000)
    expect(page.locator('.el-notification:visible')).to_have_count(0,timeout=10000)
    page.screenshot(path=str(args.output_dir/name),full_page=True,animations='disabled')
def close_dialog(dialog):
    dialog.press('Escape')
    expect(dialog).not_to_be_visible()
def editor(page): return page.get_by_role('dialog').filter(has=page.get_by_role('button',name='保存草稿',exact=True))
def review_dialog(page): return page.get_by_role('dialog').filter(has=page.get_by_role('button',name='提交评审',exact=True))
def save_editor(page,dialog,path):
    with page.expect_response(lambda r:'/calculation-definitions/'+path in r.url) as response:
        dialog.get_by_role('button',name='保存草稿',exact=True).click()
    data=result(response)
    expect(dialog).not_to_be_visible()
    return data

with sync_playwright() as p:
    browser = p.chromium.launch(executable_path=args.chromium_bin,headless=True,args=['--no-sandbox'])
    context = browser.new_context(viewport={'width':1720,'height':1200},locale='zh-CN',timezone_id='Asia/Shanghai')
    seed(context)
    page=context.new_page(); errors=[]
    page.on('pageerror',lambda e:errors.append(e.name))
    open_page(page); data=search(page)
    assert data['total'] >= 4
    expect(row_for(page,1).get_by_text('已确认',exact=True)).to_be_visible()
    screenshot(page,'payroll-calculation-desktop.png')
    passed('actual rule route renders filtered versions scope and approval states')
    definition=lookup_version(page,2)
    for key in fixture['inputs']: expect(page.get_by_label('核对输入 '+key,exact=True)).to_have_value('')
    lookup_dates(page,fixture['start'],fixture['end'])
    amounts=result(preview(page))
    assert {item['key']:item['amount'] for item in amounts['result']['items']} == fixture['expected']
    output=page.locator('[data-testid="calculation-preview"]')
    expect(output.get_by_role('cell',name='333.33333333',exact=True)).to_be_visible()
    expect(output.get_by_role('cell',name='0',exact=True).first).to_be_visible()
    output.locator('.el-table__expand-icon').first.click()
    expect(output).to_contain_text('除法 8 位 / HALF_UP')
    expect(output).to_contain_text('后续项目引用此舍入结果')
    output.scroll_into_view_if_needed(); screenshot(page,'payroll-calculation-preview.png')
    passed('browser shows exact ordered amounts valid zero raw result and calculation trace')
    page.get_by_label('核对输入 base',exact=True).fill('1001.00')
    expect(output).to_have_count(0)
    rejected=preview(page,{**fixture['inputs'],'cycleDays':'0'})
    assert rejected.value.json()['code']==1050970001
    expect(output).to_have_count(0)
    passed('input changes invalidate output and division by zero never leaves a previous result')
    choose(page,page.locator('.compare-grid .el-select').nth(0),re.compile(re.escape(fixture['code'])+r' · V1 ·'))
    choose(page,page.locator('.compare-grid .el-select').nth(1),re.compile(re.escape(fixture['code'])+r' · V2 ·'))
    with page.expect_response(lambda r:'/calculation-definitions/compare' in r.url) as response:
        page.get_by_role('button',name='对比版本',exact=True).click()
    result(response)
    comparison=page.locator('[data-testid="calculation-comparison"]')
    expect(comparison).to_contain_text('0 位 / HALF_UP')
    expect(comparison).to_contain_text('deduction = 0.00')
    expect(comparison).to_contain_text('deduction = 0')
    comparison.scroll_into_view_if_needed(); screenshot(page,'payroll-calculation-comparison.png')
    passed('version comparison displays explicit integer precision and retained decimal case expectations')
    row_for(page,2).get_by_role('button',name='详情',exact=True).click()
    detail=page.get_by_role('dialog').filter(has=page.get_by_text('计算规则详情',exact=True))
    expect(detail).to_be_visible()
    detail.get_by_role('tab',name='样例与预期',exact=True).click()
    with page.expect_response(lambda r:'/calculation-definitions/cases' in r.url) as response:
        detail.get_by_role('button',name='重新核对样例',exact=True).click()
    assert result(response)['allPassed']
    expect(detail.locator('[data-testid="calculation-cases"]')).to_contain_text('333.33')
    screenshot(page,'payroll-calculation-cases.png')
    detail.get_by_role('tab',name='评审历史',exact=True).click()
    expect(detail).to_contain_text('计算回归 reviewer')
    screenshot(page,'payroll-calculation-history.png'); close_dialog(detail)
    passed('detail rechecks saved cases and retains actual review actor and revision history')
    page.get_by_role('button',name='登记计算草稿',exact=True).click()
    dialog=editor(page); expect(dialog).to_be_visible()
    dialog.get_by_role('button',name='增加输入',exact=True).click()
    expect(dialog.locator('[data-testid="calculation-editor-inputs"] .el-select').nth(1)).to_contain_text('选择')
    dialog.get_by_role('button',name='增加项目',exact=True).click()
    expect(dialog.locator('[data-testid="calculation-editor-items"] .el-select').nth(0)).to_contain_text('选择')
    passed('new editor leaves financial precision and rounding explicitly unselected')
    dialog.get_by_role('button',name='载入合成运算样例',exact=True).click()
    page.get_by_role('dialog').filter(has=page.get_by_text('载入合成运算样例',exact=True)).get_by_role('button',name='确定',exact=True).click()
    expect(dialog.get_by_label('规则负责人',exact=True)).to_have_value('')
    expect(dialog.get_by_label('范围编号',exact=True)).to_have_value('')
    ui_code='UI-CALC-'+uuid.uuid4().hex[:12].upper()
    ui_scope=ui_code+'-SCOPE'
    ui_title=fixture['title']+' · 页面规则'
    for label,value in [('规则编号',ui_code),('规则名称',ui_title),('范围编号',ui_scope),('规则负责人','页面合成负责人'),
                        ('规则适用范围','页面合成技术范围'),('规则依据','仅用于页面运算验收的合成依据')]:
        dialog.get_by_label(label,exact=True).fill(value)
    for label,value in [('规则开始日期','2028-10-01'),('规则结束日期','2028-10-31')]:
        dialog.get_by_label(label,exact=True).fill(value); dialog.get_by_label(label,exact=True).press('Enter')
    dialog.get_by_role('tab',name='输入与表达式',exact=True).click()
    screenshot(page,'payroll-calculation-editor.png')
    dialog.get_by_role('tab',name='业务样例与预期',exact=True).click()
    dialog.get_by_placeholder('net',exact=True).fill('334.00')
    created=save_editor(page,dialog,'create')
    assert search(page,ui_scope,ui_title)['total']==1
    row_for(page,1,ui_code).get_by_role('button',name='确认',exact=True).click()
    review=review_dialog(page); review.get_by_label('规则评审依据',exact=True).fill('页面验证不匹配样例须阻断')
    with page.expect_response(lambda r:'/calculation-definitions/review' in r.url) as response:
        review.get_by_role('button',name='提交评审',exact=True).click()
    assert response.value.json()['code']==1050970006
    expect(review).to_be_visible(); screenshot(page,'payroll-calculation-blocked-case.png')
    review.get_by_role('button',name='取消',exact=True).click()
    passed('explicit synthetic sample retains blank policy metadata and mismatched expected result blocks approval')
    row_for(page,1,ui_code).get_by_role('button',name='编辑',exact=True).click()
    dialog=editor(page); expect(dialog).to_be_visible()
    expect(dialog.get_by_label('规则编号',exact=True)).to_be_disabled()
    dialog.get_by_role('tab',name='业务样例与预期',exact=True).click()
    dialog.get_by_placeholder('net',exact=True).fill('333.33')
    save_editor(page,dialog,'update')
    row_for(page,1,ui_code).get_by_role('button',name='确认',exact=True).click()
    review=review_dialog(page); review.get_by_label('规则评审依据',exact=True).fill('页面合成结果逐项匹配，不代表正式业务签认')
    with page.expect_response(lambda r:'/calculation-definitions/review' in r.url) as response:
        review.get_by_role('button',name='提交评审',exact=True).click()
    result(response); expect(review).not_to_be_visible()
    expect(row_for(page,1,ui_code).get_by_role('button',name='编辑',exact=True)).to_have_count(0)
    lookup_version(page,1,ui_code); lookup_dates(page,'2028-10-01','2028-10-31')
    assert result(preview(page))['definition']['id']==created
    passed('actual browser editing correcting and confirming a rule makes its exact saved definition executable')
    with page.expect_response(lambda r:'/calculation-definitions/new-version' in r.url) as response:
        row_for(page,1,ui_code).get_by_role('button',name='另建版本',exact=True).click()
    new_id=result(response)
    expect(row_for(page,2,ui_code)).to_be_visible()
    expect(output).to_have_count(0)
    lookup_version(page,2,ui_code)
    expect(page.get_by_role('button',name='执行规则核对',exact=True)).to_be_disabled()
    row_for(page,2,ui_code).get_by_role('button',name='编辑',exact=True).click()
    dialog=editor(page); expect(dialog).to_be_visible()
    dialog.get_by_label('规则名称',exact=True).fill(ui_title+' · 修订')
    save_editor(page,dialog,'update')
    expect(page.locator('.lookup-grid .el-select')).to_contain_text(ui_title+' · 修订')
    for key in fixture['inputs']: expect(page.get_by_label('核对输入 '+key,exact=True)).to_have_value('')
    passed('copy and edit refresh selected definition clear inputs and discard old execution output')
    row_for(page,1,ui_code).get_by_role('button',name='停用',exact=True).click()
    review=review_dialog(page); review.get_by_label('规则评审依据',exact=True).fill('页面验收停用合成版本并保留历史')
    with page.expect_response(lambda r:'/calculation-definitions/review' in r.url) as response:
        review.get_by_role('button',name='提交评审',exact=True).click()
    result(response); expect(review).not_to_be_visible()
    expect(row_for(page,1,ui_code).get_by_text('已停用',exact=True)).to_be_visible()
    passed('browser retirement preserves version visibility and removes execution eligibility')
    reader=browser.new_context(viewport={'width':1480,'height':1050},locale='zh-CN')
    seed(reader,'HRM_CALC_READER_TOKEN','HRM_CALC_READER_REFRESH_TOKEN')
    rp=reader.new_page(); rp.on('pageerror',lambda e:errors.append('reader: '+e.name))
    open_page(rp); search(rp)
    for label in ['登记计算草稿','编辑','确认','停用','另建版本']:
        expect(rp.get_by_role('button',name=label,exact=True)).to_have_count(0)
    lookup_version(rp,2); lookup_dates(rp,fixture['start'],fixture['end'])
    assert result(preview(rp))['result']['items'][1]['amount']=='0'
    rp.locator('[data-testid="calculation-preview"]').scroll_into_view_if_needed()
    screenshot(rp,'payroll-calculation-readonly.png'); reader.close()
    passed('query-only reader sees matching filtered rows and exact results without mutation controls')
    mobile=browser.new_context(viewport={'width':390,'height':844},is_mobile=True,has_touch=True,locale='zh-CN')
    seed(mobile); mp=mobile.new_page(); mp.on('pageerror',lambda e:errors.append('mobile: '+e.name))
    open_page(mp); search(mp)
    assert mp.evaluate('document.documentElement.scrollWidth<=window.innerWidth')
    screenshot(mp,'payroll-calculation-mobile.png')
    mp.get_by_role('button',name='登记计算草稿',exact=True).click()
    expect(editor(mp)).to_be_visible()
    assert mp.evaluate('document.documentElement.scrollWidth<=window.innerWidth')
    editor(mp).get_by_role('button',name='取消',exact=True).click(); mobile.close()
    passed('390px mobile contains table overflow and stacks input and editor metadata')
    assert not errors, errors
    passed('desktop reader and mobile have no uncaught browser errors')
    context.close(); browser.close()
(args.output_dir/'hrm-calculation-ui-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2)+'\n')
print('All',len(checks),'rule calculation browser checks passed')
