#!/usr/bin/env python3
"""Install a NEW isolated MySQL database, boot real services and run payroll acceptance.

Requires an already built application and UI plus disposable local MySQL/Redis.
Never reuses or drops an existing database; credentials and server logs stay private.
"""
import argparse
import base64
import datetime
import json
import os
from pathlib import Path
import re
import secrets
import socket
import subprocess
import sys
import tempfile
import time
import urllib.request
import urllib.parse
import bcrypt

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--database', required=True)
p.add_argument('--mysql-port', type=int, default=13306)
p.add_argument('--redis-port', type=int, default=16380)
p.add_argument('--redis-database', type=int, default=13)
p.add_argument('--ui-dir', type=Path, required=True)
p.add_argument('--output-dir', type=Path, required=True)
a = p.parse_args()
if not re.fullmatch(r'hrm_remediation_[a-zA-Z0-9_]+', a.database):
    p.error('Use a new isolated hrm_remediation_ database')
root = Path(__file__).resolve().parents[2]
ui = root / 'yudao-ui/yudao-ui-admin-vue3'
if not (a.ui_dir / 'index.html').is_file():
    p.error('--ui-dir must contain the built frontend')
for port in (48080, 4173):
    with socket.socket() as connection:
        if connection.connect_ex(('127.0.0.1', port)) == 0:
            p.error(f'Port {port} is already in use; stop the owned verification service first')
# Separate databases must also have separate Redis caches and login sessions.
with socket.create_connection(('127.0.0.1', a.redis_port), timeout=5) as connection:
    index = str(a.redis_database)
    connection.sendall((f'*2\r\n$6\r\nSELECT\r\n${len(index)}\r\n{index}\r\n'
                        '*1\r\n$6\r\nDBSIZE\r\n').encode())
    reader = connection.makefile('rb')
    if reader.readline() != b'+OK\r\n' or reader.readline() != b':0\r\n':
        p.error('Use an empty isolated Redis database via --redis-database; existing caches are never flushed')
private = Path(tempfile.mkdtemp(prefix='hrm-runtime-gate-'))
private.chmod(0o700)
output = a.output_dir.resolve()
output.mkdir(parents=True, exist_ok=True)
state = private / 'session.json'
today = datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).date()
password = secrets.token_urlsafe(12)
state.write_text(json.dumps({'baseUrl':'http://127.0.0.1:48080/admin-api', 'adminPassword':password,
                            'fixtureYear':today.year, 'fixtureMonth':today.month, 'fixturePaymentDate':today.isoformat()}))
state.chmod(0o600)
env = dict(os.environ, HRM_SMOKE_PASSWORD=password, TZ='Asia/Shanghai')

def run(script, *args):
    subprocess.run([sys.executable, str(root / 'scripts/hrm' / script), *map(str,args)], cwd=root, env=env, check=True)

run('migrate.py', '--container', 'codex-ruoyi-mysql', '--database', a.database, '--fresh')
run('migrate.py', '--container', 'codex-ruoyi-mysql', '--database', a.database)
hash_value = bcrypt.hashpw(password.encode(), bcrypt.gensalt(rounds=10)).decode()
subprocess.run(['docker','exec','-i','codex-ruoyi-mysql','mysql',a.database], input=f"UPDATE system_users SET password='{hash_value}' WHERE id=1;", text=True, check=True)
url = f'jdbc:mysql://127.0.0.1:{a.mysql_port}/{a.database}?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&useUnicode=true&characterEncoding=UTF-8'
config = {
    'server': {'address':'127.0.0.1','port':48080},
    'spring': {'datasource': {'dynamic': {'datasource': {name: {'url':url,'username':'root','password':''} for name in ('master','slave')}}},
               'redis':{'host':'127.0.0.1','port':a.redis_port,'database':a.redis_database},
               'boot':{'admin':{'client':{'enabled':False}}}, 'quartz':{'auto-startup':False}},
    'flowable':{'async-executor-activate':False},
    'yudao':{'security':{'mock-enable':False},'captcha':{'enable':False},'hrm':{'tax':{
        'identity-encryption-key':base64.b64encode(os.urandom(32)).decode(),
        'identity-fingerprint-key':base64.b64encode(os.urandom(32)).decode()}}},
    'logging':{'level':{'cn.iocoder.yudao.module':'WARN', 'cn.iocoder.yudao.framework.apilog':'OFF', 'cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler':'OFF'}, 'file':{'name':str(private / 'server.log')}}
}
(private / 'application-runtime-gate.yaml').write_text(json.dumps(config))
backend = preview = None
try:
    with (private / 'backend-console.log').open('w') as log:
        backend = subprocess.Popen(['java','-Xms256m','-Xmx1536m','-Duser.timezone=Asia/Shanghai','-jar',str(root/'yudao-server/target/yudao-server.jar'), '--spring.profiles.active=local,runtime-gate', '--spring.config.additional-location=file:' + str(private) + '/'], cwd=private, stdout=log, stderr=subprocess.STDOUT, env=env)
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    for _ in range(180):
        if backend.poll() is not None:
            raise RuntimeError(f'Backend failed; private log: {private}/backend-console.log')
        try:
            with opener.open('http://127.0.0.1:48080/admin-api/system/tenant/get-id-by-name?name=' + urllib.parse.quote('芋道源码'), timeout=2) as response:
                if json.load(response).get('data') == 1:
                    break
        except Exception:
            pass
        time.sleep(1)
    else:
        raise RuntimeError('Backend readiness timed out')
    run('acceptance_workflows.py','--state-file',state,'--create-isolated-fixtures')
    run('smoke_api.py','--initialize')
    run('runtime_scope_regression.py','--state-file',state,'--database',a.database,'--prepare-fixtures')
    run('runtime_scope_regression.py','--state-file',state,'--database',a.database,'--output',output/'scope-runtime.json')
    for phase in ('sources','payroll','slips'):
        run('runtime_payroll_regression.py','--state-file',state,'--output-dir',output,'--phase',phase)
    personnel_state = private/'personnel.json'
    personnel_output = output/'personnel-scenarios'
    run('runtime_personnel_scenarios.py','--base-state',state,'--state-file',personnel_state,'--database',a.database,'--output-dir',personnel_output)
    for phase in ('sources','payroll','slips'):
        run('runtime_payroll_regression.py','--state-file',personnel_state,'--output-dir',personnel_output,'--phase',phase)
    tax_state = private/'tax-history.json'
    tax_output = output/'tax-history'
    run('runtime_personnel_scenarios.py','--base-state',state,'--state-file',tax_state,'--database',a.database,'--output-dir',tax_output,'--kind','tax-history')
    for phase in ('sources','payroll'):
        run('runtime_payroll_regression.py','--state-file',tax_state,'--output-dir',tax_output,'--phase',phase)
    run('runtime_tax_filing_regression.py','--state-file',tax_state,'--output-dir',tax_output)
    bonus_state = private/'salary-bonus.json'
    bonus_output = output/'salary-bonus'
    run('runtime_salary_bonus_regression.py','--base-state',tax_state,'--state-file',bonus_state,'--output-dir',bonus_output)
    attendance_state = private/'attendance.json'
    attendance_output = output/'attendance'
    run('runtime_attendance_regression.py','--base-state',state,'--state-file',attendance_state,'--database',a.database,'--output-dir',attendance_output)
    facts = json.loads((output/'f08-payment-cost-runtime.json').read_text())
    assert all(facts[key] for key in ('archived','concurrentReplayPassed','amountMismatchRejected','staleAttemptRejected','frozenRecomputeRejected')), facts
    with (private/'preview.log').open('w') as log:
        preview = subprocess.Popen(['node','node_modules/vite/bin/vite.js','preview','--outDir',str(a.ui_dir.resolve()),'--host','127.0.0.1','--port','4173','--strictPort'],cwd=ui,stdout=log,stderr=subprocess.STDOUT,env=env)
    for _ in range(30):
        if preview.poll() is not None:
            raise RuntimeError('Frontend preview failed')
        try:
            with opener.open('http://127.0.0.1:4173',timeout=2):
                break
        except Exception:
            time.sleep(1)
    else:
        raise RuntimeError('Frontend readiness timed out')
    for case in ('confirmation','context','mobile','payroll'):
        run('runtime_browser_workflows.py','--state-file',state,'--output-dir',output,'--case',case)
    run('runtime_browser_workflows.py','--state-file',personnel_state,'--output-dir',personnel_output,'--case','payroll')
    run('runtime_browser_workflows.py','--state-file',tax_state,'--output-dir',tax_output,'--case','tax')
    run('runtime_browser_workflows.py','--state-file',bonus_state,'--output-dir',bonus_output,'--case','versions')
    run('runtime_browser_workflows.py','--state-file',attendance_state,'--output-dir',attendance_output,'--case','attendance')
    (output/'gate-result.json').write_text(json.dumps({'passed':True,'database':a.database,'fixtureDate':today.isoformat(),'freshInstallAndRepeatUpgrade':True,'realAuthentication':True,'uiCases':['confirmation','context','mobile','payroll'],'personnelScenarios':['standard','mid-month regularization','mid-month entry'],'closedPeriodTaxFiling':'RECONCILED','salaryAndBonusVersions':2,'annualSeparateBonusDuplicateRejected':True},indent=2))
    print('PASSED: fresh MySQL, real authentication, payroll/payment/slips/cost and browser gate', flush=True)
finally:
    for process in (preview, backend):
        if process is not None and process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
    print(f'Private verification state retained outside repository: {private}', flush=True)
