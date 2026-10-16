#!/usr/bin/env python3
"""Check requirement evidence and migration coverage; this does not replace business tests."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[2]
plan = (root / 'docs/hrm-payroll-audit-and-development-plan.md').read_text()
report = (root / 'docs/hrm-payroll-final-acceptance.md').read_text()
pattern = r'^\| ((?:OV|PLAN|CALC|TAX|ATT|OT|HOUR|INS|RPT|COL)-\d+) \|'
expected = re.findall(pattern, plan, re.M)
actual = re.findall(pattern, report, re.M)
assert len(expected) == len(set(expected)) == 46
assert len(actual) == len(set(actual)) == 46 and set(expected) == set(actual), 'Requirement coverage mismatch'
for target in re.findall(r'\]\((\.\./[^)]+|hrm-runtime-verification\.md)\)', report):
    assert (root / 'docs' / target).is_file(), f'Missing evidence file: {target}'
for line in report.splitlines():
    if not re.match(pattern, line):
        continue
    for view in line.split('|')[-2].strip().strip('`').split(','):
        path = root / 'yudao-ui/yudao-ui-admin-vue3/src/views/hrm'
        if not view.startswith(('salary/', 'portal/')):
            path /= 'payroll'
        assert (path / view / 'index.vue').is_file(), f'Missing view: {view}'
manifest = [line for line in (root / 'scripts/hrm/migrations.txt').read_text().splitlines() if line and not line.startswith('#')]
required = {str(p.relative_to(root)) for p in (root / 'sql/mysql/upgrade').glob('*hrm*.sql')}
required.update({'sql/mysql/hrm.sql', 'sql/mysql/bpm.sql', 'sql/mysql/oa.sql'})
assert len(manifest) == len(set(manifest)) and set(manifest) == required, 'Migration manifest mismatch'
for name in manifest:
    assert re.search(r'^SET NAMES utf8mb4;', (root / name).read_text(), re.M), f'Missing charset initialization: {name}'
print(f'Passed: 46 unique requirements, evidence files and views; {len(manifest)} ordered UTF-8 migrations')
