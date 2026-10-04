#!/usr/bin/env python3
"""Exercise actual UI flows and save unmodified Chromium screenshots in an isolated instance."""
import argparse, json, os, time
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url', default='http://127.0.0.1:3000')
parser.add_argument('--output-dir', type=Path, required=True)
parser.add_argument('--fixture-file', type=Path, required=True)
parser.add_argument('--chromium-bin', default='/usr/bin/chromium')
parser.add_argument('--allow-test-fixtures', action='store_true', required=True)
args = parser.parse_args()
for name in ['HRM_SMOKE_TOKEN', 'HRM_REFRESH_TOKEN', 'HRM_READER_TOKEN', 'HRM_READER_REFRESH_TOKEN']:
    if not os.environ.get(name): parser.error('Missing verification credential: ' + name)
args.output_dir.mkdir(parents=True, exist_ok=True)
fixture = json.loads(args.fixture_file.read_text())
main_version = (args.fixture_file.parent / 'intake-preview-sample.csv').read_text(encoding='utf-8-sig').splitlines()[0].split(',')[2]
base = args.base_url.rstrip('/')
checks = []

def passed(name):
    checks.append({'check': name, 'result': 'PASS'})
    print('PASS:', name, flush=True)

def seed(context, token, refresh):
    values = {'ACCESS_TOKEN': os.environ[token], 'REFRESH_TOKEN': os.environ[refresh], 'TenantId': 1}
    context.add_init_script('const v=' + json.dumps(values) + '; for(const[k,x]of Object.entries(v))localStorage.setItem(k,JSON.stringify({c:Date.now(),e:Date.now()+86400000,v:JSON.stringify(x)}));')

def result(response):
    data = response.value.json()
    assert data.get('code') == 0, ('request failed', data.get('code'))
    return data['data']

def contract_row(page, title):
    row = page.locator('[data-testid="contracts-table"]').get_by_role('row').filter(has=page.get_by_text(title, exact=True))
    if title == fixture['contractTitle']:
        row = row.filter(has=page.get_by_role('cell', name='V' + main_version, exact=True))
    return row

def search(page, title):
    page.get_by_placeholder('搜索契约名称或来源编号').fill(title)
    with page.expect_response(lambda r: '/intake/contracts/page' in r.url and r.request.method == 'GET') as response:
        page.get_by_role('button', name='查询', exact=True).click()
    result(response)
    expect(contract_row(page, title)).to_be_visible()

def ready_screenshot(page, filename):
    expect(page.locator('.el-loading-mask:visible')).to_have_count(0, timeout=15000)
    page.screenshot(path=str(args.output_dir / filename), full_page=True, animations='disabled')

def dates(page):
    page.get_by_placeholder('由提交人核实并填写').fill('测试主体')
    page.get_by_placeholder('开始日期').fill('2026-10-01')
    page.get_by_placeholder('结束日期').fill('2026-10-31')
    page.get_by_placeholder('结束日期').press('Enter')
    page.get_by_role('heading', name='薪酬数据接入准备', exact=True).click()

with sync_playwright() as p:
    browser = p.chromium.launch(executable_path=args.chromium_bin, headless=True, args=['--no-sandbox'])
    context = browser.new_context(viewport={'width': 1680, 'height': 1350}, locale='zh-CN', timezone_id='Asia/Shanghai')
    seed(context, 'HRM_SMOKE_TOKEN', 'HRM_REFRESH_TOKEN')
    page = context.new_page(); errors = []
    page.on('pageerror', lambda e: errors.append(e.name))
    page.goto(base + '/hrm/payroll-intake', wait_until='domcontentloaded')
    expect(page.get_by_role('heading', name='薪酬数据接入准备', exact=True)).to_be_visible(timeout=30000)
    search(page, fixture['contractTitle'])
    passed('authenticated intake page uses actual contract API')
    ready_screenshot(page, 'payroll-intake-contracts.png')
    contract_row(page, fixture['contractTitle']).get_by_role('button', name='详情', exact=True).click()
    drawer = page.locator('.el-drawer:visible')
    expect(drawer.get_by_role('heading', name='评审历史', exact=True)).to_be_visible()
    expect(drawer.get_by_text('员工工号', exact=True)).to_be_visible()
    with page.expect_download() as download:
        drawer.get_by_role('button', name='下载模板', exact=True).click()
    template_path = args.output_dir / 'ui-downloaded-template.csv'
    download.value.save_as(str(template_path))
    assert template_path.read_bytes().startswith(b'\xef\xbb\xbf#hrm-payroll-contract,')
    passed('contract detail audit and real CSV template download')
    ready_screenshot(page, 'payroll-intake-contract-history.png')
    drawer.get_by_role('button', name='关闭此对话框', exact=True).click()
    contract_row(page, fixture['contractTitle']).get_by_role('button', name='另建版本', exact=True).click()
    dialog = page.get_by_role('dialog').filter(has=page.get_by_text('新建契约版本', exact=True))
    expect(dialog).to_be_visible()
    ui_title = fixture['contractTitle'] + ' · 页面回归 ' + str(int(time.time()))
    dialog.get_by_label('契约名称 *', exact=True).fill(ui_title)
    with page.expect_response(lambda r: '/intake/contracts/create' in r.url) as response:
        dialog.get_by_role('button', name='保存草稿', exact=True).click()
    ui_id = result(response)
    expect(dialog).not_to_be_visible()
    expect(contract_row(page, ui_title)).to_be_visible()
    passed('copying confirmed contract creates separate draft version through UI')
    contract_row(page, ui_title).get_by_role('button', name='编辑', exact=True).click()
    edit_dialog = page.get_by_role('dialog').filter(has=page.get_by_text('维护契约草稿', exact=True))
    edit_dialog.get_by_label('适用范围（确认必填）', exact=True).fill('页面回归：仅隔离测试主体，不用于正式核算')
    with page.expect_response(lambda r: '/intake/contracts/update' in r.url) as response:
        edit_dialog.get_by_role('button', name='保存草稿', exact=True).click()
    result(response); expect(edit_dialog).not_to_be_visible()
    passed('draft field contract edits persist via backend')
    contract_row(page, ui_title).get_by_role('button', name='确认', exact=True).click()
    review_dialog = page.get_by_role('dialog').filter(has=page.get_by_text('确认来源契约', exact=True))
    review_dialog.get_by_role('button', name='提交评审', exact=True).click()
    expect(review_dialog.get_by_text('请填写评审依据。', exact=True)).to_be_visible()
    review_dialog.get_by_label('评审依据 *', exact=True).fill('页面回归：使用合成样例确认字段定义，生产口径待确认')
    with page.expect_response(lambda r: '/intake/contracts/review' in r.url) as response:
        review_dialog.get_by_role('button', name='提交评审', exact=True).click()
    result(response); expect(review_dialog).not_to_be_visible()
    expect(contract_row(page, ui_title).get_by_role('button', name='编辑', exact=True)).to_have_count(0)
    passed('review requires evidence then locks confirmed contract in UI')
    contract_row(page, ui_title).get_by_role('button', name='预检', exact=True).click()
    expect(page.get_by_role('tab', name='上传预检', exact=True)).to_have_attribute('aria-selected', 'true')
    page.get_by_role('button', name='上传并预检', exact=True).click()
    expect(page.get_by_text('请填写契约、声明范围、期间并选择 CSV 文件。', exact=True)).to_be_visible()
    expect(page.get_by_role('button', name='上传并预检', exact=True)).to_be_enabled()
    with page.expect_download() as download:
        page.get_by_role('button', name='下载所选版本模板', exact=True).click()
    own_template = args.output_dir / 'ui-valid-sample.csv'
    download.value.save_as(str(own_template))
    own_template.write_bytes(own_template.read_bytes() + 'UI-ZERO,2026-10-02,测试主体,0,页面回归零值\n'.encode())
    dates(page)
    page.locator('input[type="file"]').set_input_files(str(own_template))
    with page.expect_response(lambda r: '/intake/batches/preview' in r.url) as response:
        page.get_by_role('button', name='上传并预检', exact=True).click()
    ui_batch = result(response)
    expect(page.get_by_text('格式预检通过', exact=True)).to_be_visible()
    expect(page.get_by_text('人员映射未启用。格式预检通过后，仍需确认人员映射、来源完整性和薪酬规则。', exact=True)).to_be_visible()
    passed('validation remains retryable and valid zero upload passes without implying payroll readiness')
    page.get_by_role('tab', name='来源契约', exact=True).click()
    contract_row(page, fixture['contractTitle']).get_by_role('button', name='预检', exact=True).click()
    page.locator('input[type="file"]').set_input_files(str(args.fixture_file.parent / 'intake-preview-sample.csv'))
    with page.expect_response(lambda r: '/intake/batches/preview' in r.url) as response:
        page.get_by_role('button', name='上传并预检', exact=True).click()
    assert result(response) == fixture['mainBatchId']
    display = page.locator('[data-testid="batch-result"]').filter(visible=True)
    expect(display.get_by_text('缺失', exact=True)).to_be_visible()
    expect(display.get_by_text('0', exact=True)).to_be_visible()
    expect(display.get_by_text('(DUPLICATE)', exact=True)).to_have_count(2)
    passed('UI shows zero missing precision period and duplicate errors from original owned batch')
    ready_screenshot(page, 'payroll-intake-precheck.png')
    display.locator('.el-switch').click()
    expect(display.get_by_text('零值有效', exact=True)).to_have_count(0)
    expect(display.get_by_text('必填缺失', exact=True)).to_be_visible()
    passed('problem-only filter excludes the valid zero row')
    page.get_by_role('tab', name='我的预检批次', exact=True).click()
    expect(page.locator('[data-testid="batches-table"]').get_by_role('cell', name=str(ui_batch), exact=True)).to_be_visible()
    passed('own batch history lists persisted uploads')
    ready_screenshot(page, 'payroll-intake-batches.png')
    page.get_by_role('tab', name='来源契约', exact=True).click()
    contract_row(page, ui_title).get_by_role('button', name='停用', exact=True).click()
    retire_dialog = page.get_by_role('dialog').filter(has=page.get_by_text('停用来源契约', exact=True))
    retire_dialog.get_by_label('评审依据 *', exact=True).fill('页面回归：停用测试版本，核验历史结果保留')
    with page.expect_response(lambda r: '/intake/contracts/review' in r.url) as response:
        retire_dialog.get_by_role('button', name='提交评审', exact=True).click()
    result(response); expect(retire_dialog).not_to_be_visible()
    expect(contract_row(page, ui_title).get_by_role('button', name='预检', exact=True)).to_have_count(0)
    page.get_by_role('tab', name='我的预检批次', exact=True).click()
    batch_row = page.locator('[data-testid="batches-table"]').get_by_role('row').filter(has=page.get_by_role('cell', name=str(ui_batch), exact=True))
    batch_row.get_by_role('button', name='查看批次', exact=True).click()
    expect(page.locator('.el-drawer:visible').get_by_text('格式预检通过', exact=True)).to_be_visible()
    passed('retirement removes new upload action but preserved batch remains viewable')
    page.locator('.el-drawer:visible').get_by_role('button', name='关闭此对话框', exact=True).click()
    reader = browser.new_context(viewport={'width': 1440, 'height': 1000}, locale='zh-CN')
    seed(reader, 'HRM_READER_TOKEN', 'HRM_READER_REFRESH_TOKEN')
    rp = reader.new_page(); rp.goto(base + '/hrm/payroll-intake', wait_until='domcontentloaded')
    expect(rp.get_by_role('heading', name='薪酬数据接入准备', exact=True)).to_be_visible(timeout=30000)
    for name in ['新建契约', '另建版本', '确认', '编辑', '预检']:
        expect(rp.get_by_role('button', name=name, exact=True)).to_have_count(0)
    expect(rp.get_by_role('tab', name='上传预检', exact=True)).to_have_count(0)
    rp.get_by_role('tab', name='我的预检批次', exact=True).click()
    expect(rp.locator('[data-testid="batches-table"]').get_by_role('row')).to_have_count(1)
    passed('reader sees permitted metadata and no mutation controls or other owners batches')
    reader.close()
    mobile = browser.new_context(viewport={'width': 390, 'height': 844}, is_mobile=True, has_touch=True, locale='zh-CN')
    seed(mobile, 'HRM_SMOKE_TOKEN', 'HRM_REFRESH_TOKEN')
    mp = mobile.new_page(); mp.goto(base + '/hrm/payroll-intake', wait_until='domcontentloaded')
    expect(mp.get_by_role('heading', name='薪酬数据接入准备', exact=True)).to_be_visible(timeout=30000)
    assert mp.evaluate('document.documentElement.scrollWidth <= window.innerWidth')
    ready_screenshot(mp, 'payroll-intake-mobile.png')
    mp.get_by_role('button', name='新建契约', exact=True).click()
    md = mp.get_by_role('dialog').filter(has=mp.get_by_text('新建契约版本', exact=True))
    expect(md).to_be_visible()
    expect(md.get_by_label('契约名称 *', exact=True)).to_be_visible()
    assert mp.evaluate('document.documentElement.scrollWidth <= window.innerWidth')
    ready_screenshot(mp, 'payroll-intake-mobile-form.png')
    passed('mobile page and stacked field editor fit a 390px viewport')
    mobile.close()
    assert not errors, 'Browser page errors: ' + ','.join(errors)
    passed('browser runtime has no uncaught page errors')
    context.close(); browser.close()

(args.output_dir / 'hrm-intake-ui-results.json').write_text(json.dumps(checks, ensure_ascii=False, indent=2))
(args.output_dir / 'hrm-intake-ui-fixture.json').write_text(json.dumps({'uiContractId': ui_id, 'uiBatchId': ui_batch}, ensure_ascii=False))
print('All', len(checks), 'intake browser regression checks passed')
