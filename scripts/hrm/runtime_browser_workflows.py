#!/usr/bin/env python3
"""Exercise real HRM UI writes with private isolated fixtures and record browser evidence."""
import argparse
import asyncio
import json
import os
import re
from pathlib import Path
import time
from decimal import Decimal
import urllib.parse
from playwright.async_api import async_playwright, expect
from runtime_client import RuntimeClient

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--state-file', required=True)
parser.add_argument('--base-url', default='http://127.0.0.1:4173')
parser.add_argument('--output-dir', type=Path, required=True)
parser.add_argument('--case', choices=['confirmation', 'context', 'mobile', 'payroll', 'tax', 'versions', 'attendance'], default='confirmation')
args = parser.parse_args()
if urllib.parse.urlparse(args.base_url).hostname not in ('127.0.0.1', 'localhost'):
    parser.error('Only local isolated frontends are supported')
c = RuntimeClient(args.state_file)
args.output_dir.mkdir(parents=True, exist_ok=True)



async def context_workflow(page, errors):
    cases = [
        ('overview', 'OV', '薪酬总览', 'DS-01'), ('formula', 'PLAN', '薪酬方案', 'DS-05'),
        ('batch', 'CALC', '工资核算', 'DS-12'), ('tax-policy', 'TAX', '个税与工资条', 'DS-07'),
        ('attendance', 'ATT', '考勤', 'DS-02'), ('work', 'OT', '加班', 'DS-03'),
        ('work', 'HOUR', '工时', 'DS-04'), ('insurance-policy', 'INS', '社保公积金', 'DS-06'),
        ('analysis', 'RPT', '报表分析', 'DS-10'),
    ]
    results = []
    for path, module, label, source in cases:
        await page.goto(args.base_url + '/hrm-payroll/' + path, wait_until='networkidle')
        await page.get_by_role('button', name=f'本模块需求（{label}）', exact=True).click()
        await page.wait_for_url(lambda url: '/requirement?' in url and 'module=' + module in url)
        await page.wait_for_load_state('networkidle')
        expected = [row['code'] for row in c.ok('/hrm/payroll/requirement/list?module=' + module)]
        cells = page.locator('[data-testid="payroll-requirements"] .el-table__body tbody tr td:first-child .cell')
        await expect(cells).to_have_text(expected, timeout=15000)
        codes = await cells.all_text_contents()
        assert codes == expected, (module, codes, expected)
        await page.get_by_text(label + '候选数据源与字段', exact=True).click()
        await page.get_by_text(source, exact=True).first.wait_for(state='visible')
        if module == 'PLAN':
            await page.get_by_role('button', name='新增需求', exact=True).click()
            dialog = page.locator('.el-dialog:visible')
            assert label in await dialog.locator('.el-form-item').filter(has_text='模块').inner_text()
            await dialog.get_by_role('button', name='取消', exact=True).click()
            await dialog.wait_for(state='hidden')
            await page.screenshot(path=str(args.output_dir / 'f06-context-browser.png'), full_page=True)
        await page.get_by_role('button', name='返回业务模块', exact=True).click()
        await page.wait_for_url(lambda url: urllib.parse.urlparse(url).path == '/hrm-payroll/' + path)
        results.append({'module': module, 'route': path, 'requirementCodes': codes, 'candidateSourceChecked': source, 'returnSucceeded': True})
    await page.goto(args.base_url + '/hrm-payroll/requirement?module=UNKNOWN&from=https://example.invalid/', wait_until='networkidle')
    assert await page.get_by_role('button', name='返回业务模块', exact=True).count() == 0
    assert not errors, errors
    (args.output_dir / 'f06-browser.json').write_text(json.dumps({'cases': results, 'untrustedReturnTargetIgnored': True, 'pageErrors': errors}, ensure_ascii=False, indent=2))
    print('Passed: all nine module contexts, source dictionaries, form defaults and return navigation')

async def mobile_workflow(page, errors, width):
    await page.goto(args.base_url + '/hrm-payroll/requirement', wait_until='networkidle')
    add = page.get_by_role('button', name='新增需求', exact=True)
    try:
        await add.tap(timeout=2500)
    except Exception:
        await page.touchscreen.tap(20, 25)
        try:
            await add.tap(timeout=2500)
        except Exception:
            await page.touchscreen.tap(width - 20, 450)
            await add.tap()
    dialog = page.locator('.el-dialog:visible')
    save = dialog.get_by_role('button', name='保存', exact=True)
    bounds = await dialog.bounding_box()
    footer = await save.bounding_box()
    assert bounds and bounds['x'] >= 0 and bounds['x'] + bounds['width'] <= width, bounds
    assert footer and footer['x'] >= 0 and footer['x'] + footer['width'] <= width, footer
    assert footer['y'] >= 0 and footer['y'] + footer['height'] <= 844, footer
    code = 'COL-' + str(time.time_ns())[-6:]
    await dialog.locator('.el-form-item').filter(has_text='编号').locator('input').fill(code)
    await dialog.locator('.el-form-item').filter(has_text='功能说明').locator('textarea').fill('隔离触控验收：自适应表单与可达保存区')
    await page.screenshot(path=str(args.output_dir / f'f07-form-{width}.png'))
    await save.tap()
    await dialog.wait_for(state='hidden')
    await page.reload(wait_until='networkidle')
    await page.get_by_text(code, exact=True).wait_for()
    record = next(row for row in c.ok('/hrm/payroll/requirement/list') if row['code'] == code)
    assert record['status'] == 'PENDING'
    assert not errors, errors
    return {'viewportWidth': width, 'hasTouch': True, 'dialogBounds': bounds,
            'initialSaveButtonBounds': footer, 'requirementCode': code,
            'persistedAfterRefresh': True, 'pageErrors': errors}

async def login(page, username='hrmacceptoperator', password_key='browserPassword'):
    await page.goto(args.base_url + '/login?redirect=/hrm-payroll/requirement', wait_until='networkidle')
    await page.locator('input[placeholder="请输入租户名称"]:visible').fill('芋道源码')
    await page.locator('input[placeholder="请输入用户名"]:visible').fill(username)
    await page.locator('input[placeholder="请输入密码"]:visible').fill(c.state[password_key])
    await page.get_by_role('button', name='登录', exact=True).click()
    await page.wait_for_url(lambda url: '/login' not in url, timeout=60000)

async def payroll_workflow(browser, page, errors):
    await page.goto(args.base_url + '/hrm-payroll/batch', wait_until='networkidle')
    await page.get_by_text(c.state.get('batchTitle','隔离全链路验收工资表'), exact=True).first.wait_for()
    await page.screenshot(path=str(args.output_dir / 'f08-archived-batch-browser.png'), full_page=True)
    original = c.ok(f"/system/permission/list-role-menus?roleId={c.state['readerRoleId']}")
    menus = c.ok('/system/menu/list')
    by_id = {row['id']: row for row in menus}
    selected = set(original)
    for row in menus:
        if row.get('component') == 'hrm/portal/salary/slip/index':
            while row:
                selected.add(row['id'])
                row = by_id.get(row.get('parentId'))
    try:
        c.ok('/system/permission/assign-role-menu', {'roleId': c.state['readerRoleId'], 'menuIds': sorted(selected)})
        context = await browser.new_context(viewport={'width':1440, 'height':1000})
        owner = await context.new_page()
        owner.on('pageerror', lambda error: errors.append(str(error)))
        await login(owner, c.state.get('readerUsername','hrmacceptread'), 'readerPassword')
        await owner.goto(args.base_url + '/hrm/portal/salary/slip', wait_until='networkidle')
        await owner.get_by_text('我的工资条', exact=True).first.wait_for()
        expected_net = Decimal(str(c.state.get('payrollScenarios',{}).get(str(c.state['scopeEmployeeIds'][1]),{}).get('expected',{}).get('net',5582)))
        await owner.get_by_text(f'¥ {expected_net:.2f}', exact=True).first.wait_for()
        await owner.screenshot(path=str(args.output_dir / 'f08-owner-slip-browser.png'), full_page=True)
        assert not errors, errors
        (args.output_dir / 'f08-browser.json').write_text(json.dumps({'archivedBatchVisible':True, 'ownerActualSalaryVisible':str(expected_net), 'realLogin':True, 'pageErrors':errors}, ensure_ascii=False, indent=2))
        await context.close()
    finally:
        c.ok('/system/permission/assign-role-menu', {'roleId': c.state['readerRoleId'], 'menuIds': original})
    print('Passed: real browser archived batch and owner salary slip')

async def tax_workflow(page, errors):
    await page.goto(args.base_url + '/hrm-payroll/tax-policy', wait_until='networkidle')
    entity=next(row for row in c.ok('/hrm/payroll/entity/list') if row['id']==c.state['entityId'])
    await page.locator('.el-form-item').filter(has_text='纳税主体').locator('.el-select').click()
    await page.locator('.el-select-dropdown:visible').get_by_text(entity['name'],exact=True).click()
    await page.locator('.el-select-dropdown:visible').wait_for(state='hidden')
    await page.locator('.el-form-item').filter(has_text='年度').locator('input').fill(str(c.state['fixtureYear']))
    await page.get_by_role('button',name='查询',exact=True).first.click()
    month_form=page.locator('.el-form').filter(has_text='收入月份')
    await month_form.locator('.el-select').click()
    await page.locator('.el-select-dropdown:visible').get_by_text(f"{c.state['fixtureMonth']}月",exact=True).click()
    await page.locator('.el-select-dropdown:visible').wait_for(state='hidden')
    await month_form.get_by_role('button',name='查询',exact=True).click()
    row=page.get_by_role('row').filter(has_text=f"OFFLINE_TEST_FILING_{c.state['taxFilingId']}")
    await expect(row).to_contain_text('已对账')
    await row.get_by_role('button',name='来源与对账',exact=True).click()
    dialog=page.locator('.el-dialog:visible')
    await dialog.get_by_text('与批准版本一致',exact=True).wait_for()
    values=await dialog.locator('.el-descriptions__content').all_text_contents()
    assert values[:4]==['已对账','18','18','0'],values
    assert not errors,errors
    await page.screenshot(path=str(args.output_dir/'tax-filing-browser.png'),full_page=True)
    (args.output_dir/'tax-filing-browser.json').write_text(json.dumps({'entityId':c.state['entityId'],'filingId':c.state['taxFilingId'],'status':'已对账','expectedTax':18,'paid':18,'difference':0,'pageErrors':errors},ensure_ascii=False,indent=2))
    print('Passed: real browser closed-period tax filing and reconciled zero difference')

async def versions_workflow(page, errors):
    await page.goto(args.base_url+'/hrm-payroll/batch',wait_until='networkidle')
    row=page.get_by_role('row').filter(has_text=c.state['batchTitle'])
    await row.get_by_role('button',name='审批与版本',exact=True).click()
    dialog=page.locator('.el-dialog:visible')
    await dialog.get_by_role('tab',name='核算版本与差异',exact=True).click()
    pane=dialog.get_by_role('tabpanel',name='核算版本与差异')
    first=pane.locator('.el-table__body tbody tr').first
    name=await first.locator('td').first.inner_text()
    if not name.strip():
        await page.screenshot(path=str(args.output_dir/'salary-version-before-fix.png'),full_page=True)
    assert name.strip()=='隔离历史工资员工',repr(name)
    runs=c.ok('/hrm/payroll/batch/versions?batchId='+str(c.state['batchId']))
    results=[]
    for index,run in enumerate(runs):
        await pane.locator('.el-select').click()
        await page.locator('.el-select-dropdown:visible').get_by_text(re.compile('^版本 '+str(run['version'])+' ·')).click()
        await page.locator('.el-select-dropdown:visible').wait_for(state='hidden')
        record=json.loads(run['resultSnapshot'])[0]
        await expect(first.locator('td').nth(1)).to_have_text(str(record['expectedPaySalary']))
        await expect(first.locator('td').nth(2)).to_have_text(str(record['realPaySalary']))
        await expect(first.locator('td').first).to_have_text('隔离历史工资员工')
        prior_net=json.loads(runs[index+1]['resultSnapshot'])[0]['realPaySalary'] if index+1<len(runs) else 0
        delta=Decimal(str(record['realPaySalary']))-Decimal(str(prior_net))
        await expect(first.locator('td').nth(3)).to_have_text(f'{delta:.2f}')
        results.append({'version':run['version'],'gross':record['expectedPaySalary'],'net':record['realPaySalary'],'employeeNameVisible':True})
    assert not errors,errors
    await page.screenshot(path=str(args.output_dir/'salary-version-browser.png'),full_page=True)
    (args.output_dir/'salary-version-browser.json').write_text(json.dumps({'batchId':c.state['batchId'],'versions':results,'pageErrors':errors},ensure_ascii=False,indent=2))
    print('Passed: real browser selects both immutable salary/bonus versions with historical employee names')

async def attendance_workflow(page, errors):
    await page.goto(args.base_url + '/hrm-payroll/attendance', wait_until='networkidle')
    row = page.get_by_role('row').filter(has_text='隔离考勤工单员工')
    await expect(row).to_contain_text('已关闭')
    await expect(row).to_contain_text('隔离薪酬验收操作人')
    await expect(row).to_contain_text('隔离验收复核人')
    await row.get_by_role('button', name='履历', exact=True).click()
    dialog = page.locator('.el-dialog:visible')
    actions = dialog.locator('.el-table__body tbody tr td:first-child .cell')
    expected = ['CREATE', 'SUBMIT', 'REJECT', 'SUBMIT', 'APPROVE', 'CLOSE', 'REOPEN', 'SUBMIT', 'APPROVE', 'CLOSE']
    await expect(actions).to_have_text(expected)
    assert not errors, errors
    await page.screenshot(path=str(args.output_dir / 'attendance-browser.png'), full_page=True)
    (args.output_dir / 'attendance-browser.json').write_text(json.dumps({
        'issueId': c.state['attendanceIssueId'], 'status': '已关闭', 'ownerVisible': True,
        'reviewerVisible': True, 'historyActions': expected, 'pageErrors': errors}, ensure_ascii=False, indent=2))
    print('Passed: actual attendance correction status, responsible users and full history in browser')

async def main():
    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch(executable_path=os.environ.get('HRM_CHROMIUM_PATH', '/usr/bin/chromium'), headless=True, args=['--no-sandbox', '--disable-dev-shm-usage'])
        if args.case == 'mobile':
            results = []
            for width in (390, 768, 1440):
                context = await browser.new_context(viewport={'width': width, 'height': 844}, has_touch=True, is_mobile=width < 600)
                page = await context.new_page()
                page.set_default_timeout(15000)
                errors = []
                page.on('pageerror', lambda error: errors.append(str(error)))
                await login(page)
                results.append(await mobile_workflow(page, errors, width))
                await context.close()
            (args.output_dir / 'f07-browser.json').write_text(json.dumps({'cases': results}, ensure_ascii=False, indent=2))
            await browser.close()
            print('Passed: 390/768/1440 touch create, visible save footer and persisted reload')
            return
        context = await browser.new_context(viewport={'width': 1440, 'height': 1000})
        page = await context.new_page()
        page.set_default_timeout(15000)
        errors = []
        page.on('pageerror', lambda error: errors.append(str(error)))
        await login(page)
        if args.case == 'attendance':
            await attendance_workflow(page, errors)
            await browser.close()
            return
        if args.case == 'versions':
            await versions_workflow(page, errors)
            await browser.close()
            return
        if args.case == 'tax':
            await tax_workflow(page, errors)
            await browser.close()
            return
        if args.case == 'payroll':
            await payroll_workflow(browser, page, errors)
            await browser.close()
            return
        if args.case == 'context':
            await context_workflow(page, errors)
            await browser.close()
            return
        await page.goto(args.base_url + '/hrm-payroll/requirement', wait_until='networkidle')
        await page.get_by_role('button', name='新增需求', exact=True).click()
        dialog = page.locator('.el-dialog:visible')

        def field(label):
            return dialog.locator('.el-form-item').filter(has_text=label)

        async def select(label, value):
            await field(label).locator('.el-select').click()
            await page.locator('.el-select-dropdown:visible').get_by_text(value, exact=True).click()
            await page.locator('.el-select-dropdown:visible').wait_for(state='hidden')

        code = 'COL-' + str(int(time.time()) % 1000000).zfill(6)
        await field('编号').locator('input').fill(code)
        await select('模块', '需求征集')
        await field('功能说明').locator('textarea').fill('隔离浏览器真实确认与校验验收')
        await select('评审状态', '已确认')
        await dialog.get_by_role('button', name='保存', exact=True).click()
        await page.get_by_text('请填写字段映射；不适用时需说明具体理由', exact=True).wait_for()
        await field('字段映射').locator('textarea').fill('employeeId → 员工编号；salary → 工资金额')
        await dialog.get_by_role('button', name='保存', exact=True).click()
        await page.get_by_text('请由所选评审负责人登录后本人确认', exact=True).wait_for()
        await field('来源系统').locator('input').fill('虚构浏览器验收系统')
        await select('来源负责人', '隔离薪酬验收操作人')
        await select('评审负责人', '隔离薪酬验收操作人')
        await field('验收条件').locator('textarea').fill('刷新后可见已确认，数据就绪保持独立')
        await field('依据 / 异议').locator('textarea').fill('隔离浏览器操作记录，不作为正式业务签字')
        await dialog.get_by_role('button', name='保存', exact=True).click()
        await dialog.wait_for(state='hidden', timeout=15000)
        await page.reload(wait_until='networkidle')
        row = page.get_by_role('row').filter(has_text=code)
        await row.wait_for()
        assert await row.get_by_text('已确认', exact=True).count() == 1
        assert await row.get_by_text('未就绪', exact=True).count() == 1
        assert not errors, errors
        await row.scroll_into_view_if_needed()
        await page.screenshot(path=str(args.output_dir / 'f04-confirmation-browser.png'), full_page=True)
        record = next(row for row in c.ok('/hrm/payroll/requirement/list') if row['code'] == code)
        assert record['confirmedBy'] == c.state['browserUserId']
        (args.output_dir / 'f04-browser.json').write_text(json.dumps({'case': 'real-browser-confirmation', 'requirementCode': code, 'confirmedBy': record['confirmedBy'], 'status': record['status'], 'readiness': record['readiness'], 'missingMappingBlocked': True, 'wrongReviewerBlocked': True, 'pageErrors': errors}, ensure_ascii=False, indent=2))
        await browser.close()
    print('Passed: browser mapping/reviewer validation, actual confirmation, reload and independent readiness')

asyncio.run(main())
