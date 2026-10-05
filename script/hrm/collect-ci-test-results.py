#!/usr/bin/env python3
"""Fail CI if HRM/BPM tests were skipped or their reports are missing."""
import argparse
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def collect(root):
    groups = {}
    for path in sorted(root.glob('**/target/surefire-reports/TEST-*.xml')):
        suite = ET.parse(path).getroot()
        if suite.tag != 'testsuite':
            raise ValueError('Unexpected Surefire report format: ' + str(path))
        counts = {key: int(suite.attrib[key]) for key in ('tests', 'failures', 'errors', 'skipped')}
        if any(value < 0 for value in counts.values()) or counts['skipped'] > counts['tests']:
            raise ValueError('Invalid test counts: ' + str(path))
        module = path.relative_to(root).parts[:-3]
        name = '/'.join(module)
        total = groups.setdefault(name, dict(tests=0, failures=0, errors=0, skipped=0, suites=0))
        for key, value in counts.items():
            total[key] += value
        total['suites'] += 1
    if not groups:
        raise ValueError('No Surefire reports found; tests must run before collecting results')
    for name in ('yudao-module-hrm', 'yudao-module-bpm'):
        counts = groups.get(name)
        if not counts or counts['tests'] - counts['skipped'] <= 0:
            raise ValueError(name + ': no executed tests; HRM profile or test execution is missing')
    if groups['yudao-module-hrm']['skipped']:
        raise ValueError('HRM tests must all execute; skipped HRM tests are not accepted')
    total = {key: sum(group[key] for group in groups.values()) for key in ('tests', 'failures', 'errors', 'skipped', 'suites')}
    if total['failures'] or total['errors']:
        raise ValueError('Test failures/errors exist; regression cannot pass')
    return {'fullReactor': total, 'hrm': groups['yudao-module-hrm'], 'bpm': groups['yudao-module-bpm'], 'modules': groups}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--summary', type=Path)
    args = parser.parse_args()
    try:
        result = collect(args.root.resolve())
    except (ValueError, KeyError, ET.ParseError, OSError) as error:
        print('CI regression rejected:', error, file=sys.stderr)
        return 1
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    if args.summary:
        rows = ['| Scope | Tests | Failures | Errors | Skipped |', '| --- | ---: | ---: | ---: | ---: |']
        for name in ('fullReactor', 'hrm', 'bpm'):
            values = result[name]
            rows.append('| {} | {} | {} | {} | {} |'.format(name, *(values[key] for key in ('tests', 'failures', 'errors', 'skipped'))))
        with args.summary.open('a', encoding='utf-8') as summary:
            summary.write('\n'.join(rows) + '\n')
    print('PASS: HRM/BPM tests executed; complete reactor reports have zero failures and errors')
    return 0


if __name__ == '__main__':
    sys.exit(main())
