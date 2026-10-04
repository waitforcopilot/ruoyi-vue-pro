#!/usr/bin/env python3
"""Read-only startup regression for an authorized HRM/BPM verification instance."""
import argparse
import json
import os
import time
import urllib.error
import urllib.request


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://127.0.0.1:48081')
    parser.add_argument('--tenant-id', default='1')
    parser.add_argument('--wait-seconds', type=int, default=60)
    args = parser.parse_args()
    token = os.environ.get('HRM_SMOKE_TOKEN')
    if not token:
        parser.error('Set HRM_SMOKE_TOKEN to an authorized verification token')
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    headers = {'Authorization': 'Bearer ' + token, 'tenant-id': args.tenant_id}

    def get(path, authenticated=True):
        request = urllib.request.Request(args.base_url.rstrip('/') + path,
                                         headers=headers if authenticated else {'tenant-id': args.tenant_id})
        try:
            with opener.open(request, timeout=15) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            return json.load(error)

    deadline = time.monotonic() + args.wait_seconds
    while True:
        try:
            docs = get('/v3/api-docs/hrm')
            if not docs.get('openapi', '').startswith('3.'):
                raise RuntimeError('HRM OpenAPI group is not available')
            break
        except (OSError, ValueError, RuntimeError):
            if time.monotonic() >= deadline:
                raise RuntimeError('HRM verification instance did not become ready')
            time.sleep(1)

    results = []
    for group in ('hrm', 'bpm'):
        docs = get('/v3/api-docs/' + group)
        if not docs.get('openapi', '').startswith('3.') or not docs.get('paths'):
            raise RuntimeError(group + ' API group is missing')
        results.append({'group': group, 'routes': len(docs['paths'])})

    paths = [
        '/admin-api/hrm/employee/page?pageNo=1&pageSize=10',
        '/admin-api/hrm/salary/month-record/page?pageNo=1&pageSize=10',
        '/admin-api/hrm/salary/tax-rule/list',
        '/admin-api/hrm/salary/slip-send-record/page?pageNo=1&pageSize=10',
        '/admin-api/hrm/attendance/statistics/month-record-page?pageNo=1&pageSize=10&year=2026&month=10',
        '/admin-api/hrm/insurance/month-record/list',
        '/admin-api/bpm/category/page?pageNo=1&pageSize=10',
        '/admin-api/bpm/model/list',
        '/admin-api/system/user/page?pageNo=1&pageSize=10',
        '/admin-api/infra/config/page?pageNo=1&pageSize=10',
    ]
    for path in paths:
        response = get(path)
        if response.get('code') != 0:
            raise RuntimeError(path.split('?')[0] + ' returned code ' + str(response.get('code')))
        results.append({'path': path.split('?')[0], 'code': 0})

    response = get('/admin-api/hrm/salary/month-record/page?pageNo=1&pageSize=10', authenticated=False)
    if response.get('code') not in (401, 403):
        raise RuntimeError('Anonymous payroll access was not rejected')
    results.append({'check': 'anonymous payroll access denied', 'code': response['code']})
    print(json.dumps(results, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
