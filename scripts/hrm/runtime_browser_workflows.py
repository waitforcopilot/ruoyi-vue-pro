#!/usr/bin/env python3
"""Exercise real HRM UI writes with private isolated fixtures and record browser evidence."""
import argparse
import asyncio
import json
import os
from pathlib import Path
import time
import urllib.parse
from playwright.async_api import async_playwright
from runtime_client import RuntimeClient

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--state-file', required=True)
parser.add_argument('--base-url', default='http://127.0.0.1:4173')
parser.add_argument('--output-dir', type=Path, required=True)
parser.add_argument('--case', choices=['confirmation', 'context', 'mobile'], default='confirmation')
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
        codes = await page.locator('[data-testid="payroll-requirements"] .el-table__body tbody tr td:first-child .cell').all_text_contents()
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

async def login(page):
    await page.goto(args.base_url + '/login?redirect=/hrm-payroll/requirement', wait_until='networkidle')
    await page.locator('input[placeholder="请输入租户名称"]:visible').fill('芋道源码')
    await page.locator('input[placeholder="请输入用户名"]:visible').fill('hrmacceptoperator')
    await page.locator('input[placeholder="请输入密码"]:visible').fill(c.state['browserPassword'])
    await page.get_by_role('button', name='登录', exact=True).click()
    await page.wait_for_url(lambda url: '/login' not in url, timeout=60000)

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
