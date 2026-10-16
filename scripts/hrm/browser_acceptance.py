#!/usr/bin/env python3
"""Read the payroll application through a real local browser login; no messages or payments are sent."""
import argparse
import json
import os
from pathlib import Path
import shutil
import urllib.parse
from playwright.sync_api import sync_playwright

ROUTES = ['overview', 'requirement', 'formula', 'grade', 'salary-proposal', 'batch', 'payment',
          'tax-policy', 'attendance', 'work', 'insurance-policy', 'insurance-bill', 'input',
          'input-issue', 'cost', 'analysis', 'entity', 'wage-recovery', 'insurance-adjustment']
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url', default='http://127.0.0.1:4173')
parser.add_argument('--username', required=True)
parser.add_argument('--tenant-name', default='芋道源码')
parser.add_argument('--password-env', default='HRM_BROWSER_PASSWORD')
parser.add_argument('--chromium-executable', default=shutil.which('chromium'))
parser.add_argument('--screenshots', type=Path, help='Optional local artifact directory')
args = parser.parse_args()
if urllib.parse.urlparse(args.base_url).hostname not in ('localhost', '127.0.0.1'):
    parser.error('Only local verification is supported')
password = os.environ.get(args.password_env)
if not password:
    parser.error(f'Set {args.password_env}')
if args.screenshots:
    args.screenshots.mkdir(parents=True, exist_ok=True)
with sync_playwright() as p:
    browser = p.chromium.launch(executable_path=args.chromium_executable, headless=True,
                               args=['--no-sandbox', '--disable-dev-shm-usage', '--disable-features=BackForwardCache'])
    try:
        page = browser.new_page(viewport={'width': 1600, 'height': 1100})
        errors, failures = [], []
        page.on('pageerror', lambda error: errors.append(str(error)))

        def response(reply):
            if '/admin-api/hrm/' not in reply.url:
                return
            try:
                data = reply.json()
            except Exception:
                return
            if isinstance(data, dict) and data.get('code') != 0:
                failures.append({'path': reply.url.split('/admin-api')[-1].split('?')[0], 'code': data.get('code')})

        page.on('response', response)
        page.goto(args.base_url.rstrip('/') + '/login?redirect=/hrm-payroll/overview', wait_until='networkidle', timeout=90000)
        page.locator('input[placeholder="请输入租户名称"]:visible').fill(args.tenant_name)
        page.locator('input[placeholder="请输入用户名"]:visible').fill(args.username)
        page.locator('input[placeholder="请输入密码"]:visible').fill(password)
        page.get_by_role('button', name='登录', exact=True).click()
        page.wait_for_url(lambda url: '/login' not in url, timeout=60000)
        page.wait_for_load_state('networkidle', timeout=60000)
        print('Passed: real browser login', flush=True)
        for route in ROUTES:
            page.goto(args.base_url.rstrip('/') + '/hrm-payroll/' + route, wait_until='networkidle', timeout=60000)
            body = page.locator('body').inner_text()
            assert '抱歉，您访问的页面不存在' not in body and '/login' not in page.url, route
            assert not errors, {'route': route, 'errors': errors}
            assert not failures, {'route': route, 'apiErrors': failures}
            if args.screenshots:
                page.screenshot(path=str(args.screenshots / (route + '.png')), full_page=True)
            print('Passed: /hrm-payroll/' + route, flush=True)
        print(json.dumps({'browserRoutes': len(ROUTES), 'uncaughtErrors': len(errors), 'hrmApiErrors': len(failures)}), flush=True)
    finally:
        browser.close()
