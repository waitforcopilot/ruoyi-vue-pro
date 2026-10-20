#!/usr/bin/env python3
"""Local TEST-only PRD 9.2 load: 120s warmup, 600s sample, 10 members, 20 full exports.
Credentials and raw results must remain outside the repository. No real business calls.
"""
import argparse
import concurrent.futures
import datetime
import hashlib
import json
import math
import threading
import time
import urllib.parse
from pathlib import Path
from runtime_client import RuntimeClient
from verify_governance_workbook import read_book, verify


def run(state_path, output):
    root = Path(__file__).resolve().parents[2]
    output = Path(output).resolve()
    assert root not in output.parents, 'Raw samples must stay private'
    output.mkdir(mode=0o700, parents=True, exist_ok=False)
    client = RuntimeClient(state_path)
    state = client.state
    project = state['s6Perf']
    workers = state['s6Workers']
    assert len(workers) == 10 and len({x['id'] for x in workers}) == 10
    editor = client.login(state['editorUsername'], 'editorPassword')
    listed = client.ok('/hrm/payroll/requirement-project/list', token=editor)
    assert any(p['id'] == project['projectId'] and p['projectKey'].startswith('TEST_S6_') for p in listed)
    gid = '/hrm/payroll/governance'
    release = project['performanceReleaseId']
    frozen = client.ok(gid + '/release?' + urllib.parse.urlencode({'projectId': project['projectId'], 'releaseId': release}), token=editor)
    assert frozen['restricted'] is False and len(frozen['sheets']['功能需求清单']) == 500
    assert len([r for r in frozen['sheets']['数据源清单'] if r['记录类型'] == 'SOURCE']) == 100
    assert len(frozen['sheets']['字段映射']) == 2000
    tokens = [client.login(x['username'], x['passwordKey']) for x in workers]
    lock = threading.Lock()
    samples, exports = [], []
    with (output / 'frozen-projection.json').open('w') as f:
        json.dump(frozen, f, ensure_ascii=False)
    raw = (output / 'samples.jsonl').open('x')
    barrier = threading.Barrier(11)
    start = [None]

    def record(row):
        with lock:
            samples.append(row)
            raw.write(json.dumps(row, ensure_ascii=False) + '\n')
            raw.flush()

    def request(path, data, token, kind, actor):
        begin = time.monotonic()
        stamp = datetime.datetime.now().astimezone().isoformat()
        phase = 'warmup' if begin < start[0] + 120 else 'sample'
        try:
            answer = client.request(path, data, token)
            code = answer.get('code') if isinstance(answer, dict) else 'non-JSON'
            value = answer.get('data') if isinstance(answer, dict) else None
            error = None if code == 0 else 'unexpected response'
        except Exception as ex:
            code, value, error = 'exception', None, type(ex).__name__
        duration = time.monotonic() - begin
        record({'kind': kind, 'actorId': actor, 'phase': phase, 'wallTime': stamp,
                'offsetSeconds': begin - start[0], 'seconds': duration, 'code': code, 'error': error})
        return value, code

    allowed = ['id', 'projectId', 'projectScopeVersion', 'scopeMode', 'scopeJson', 'sourceApplicability',
               'applicabilityReason', 'ruleVersion', 'code', 'module', 'description', 'fieldMapping',
               'sourceSystem', 'sourceOwnerId', 'reviewerId', 'priority', 'status', 'readiness',
               'acceptance', 'evidence', 'version', 'businessContextJson']

    def reviewer(index):
        token, worker = tokens[index], workers[index]
        barrier.wait()
        cycle = 0
        while time.monotonic() < start[0] + 720:
            tick = time.monotonic()
            value, code = request('/hrm/payroll/review/page?' + urllib.parse.urlencode(
                {'projectId': project['projectId'], 'pageNo': 1, 'pageSize': 20}), None,
                token, 'list', worker['id'])
            if code == 0:
                assert value['total'] == 500
                pending = [r for r in value['list'] if r['status'] != 'CONFIRMED']
                row = pending[index]
                command = {k: row.get(k) for k in allowed}
                command['description'] = 'TEST S6 load reviewer ' + str(index) + ' cycle ' + str(cycle)
                request('/hrm/payroll/requirement/save', command, token, 'save', worker['id'])
            cycle += 1
            # A scheduling choice, not an extra PRD pass criterion. All ten members
            # perform real concurrent cycles; no reset/dropping of slow samples.
            time.sleep(max(0, min(5 - (time.monotonic() - tick), start[0] + 720 - time.monotonic())))

    def export_twenty():
        for n in range(20):
            deadline = start[0] + 120 + n * 28
            delay = deadline - time.monotonic()
            if delay > 0:
                time.sleep(delay)
            began = time.monotonic()
            result = {'number': n + 1, 'offsetSeconds': began - start[0], 'success': False}
            try:
                job = client.ok(gid + '/export/create', {'projectId': project['projectId'], 'releaseId': release}, editor)
                result['jobId'] = job
                while True:
                    status = client.ok(gid + '/export/status?' + urllib.parse.urlencode({'projectId': project['projectId'], 'id': job}), token=editor)
                    if status['state'] != 'QUEUED':
                        break
                    if time.monotonic() - began > 60:
                        raise TimeoutError('export not ready in 60 seconds')
                    time.sleep(.2)
                assert status['state'] == 'READY', status['state']
                body = client.request(gid + '/export/download?' + urllib.parse.urlencode({'projectId': project['projectId'], 'id': job}), None, editor)
                assert isinstance(body, bytes) and body.startswith(b'PK'), 'download not XLSX'
                result['secondsToDownload'] = time.monotonic() - began
                path = output / ('export-' + str(n + 1).zfill(2) + '.xlsx')
                path.write_bytes(body)
                book = read_book(path)
                report = verify(frozen, book, book)
                result.update(success=True, sha256=hashlib.sha256(body).hexdigest(), checkedFields=report['checkedFields'],
                              businessRows=[x['businessRows'] for x in report['checks']], sixSheets=True,
                              noOrphanReferences=True, finishedOffset=time.monotonic() - start[0])
            except Exception as ex:
                result.update(error=type(ex).__name__ + ': ' + str(ex)[:250], secondsToFailure=time.monotonic() - began)
            exports.append(result)
            (output / 'exports.json').write_text(json.dumps(exports, ensure_ascii=False, indent=2) + '\n')

    with concurrent.futures.ThreadPoolExecutor(max_workers=11) as pool:
        futures = [pool.submit(reviewer, i) for i in range(10)]
        start[0] = time.monotonic()
        wall_start = datetime.datetime.now().astimezone().isoformat()
        barrier.wait()
        export_future = pool.submit(export_twenty)
        for future in futures:
            future.result()
        export_future.result()
    raw.close()
    metrics = {}
    for kind in ['list', 'save']:
        rows = [x for x in samples if x['kind'] == kind and x['phase'] == 'sample']
        values = sorted(r['seconds'] for r in rows)
        failures = sum(r['code'] != 0 for r in rows)
        metrics[kind] = {'samples': len(rows), 'successfulSamples': len(rows) - failures,
                         'unexpectedFailures': failures, 'failureRate': failures / len(rows) if rows else None,
                         'p95SecondsNearestRank': values[math.ceil(.95 * len(values)) - 1] if values else None,
                         'maxSeconds': max(values) if values else None}
    elapsed = time.monotonic() - start[0]
    passed = all(m['successfulSamples'] >= 500 and m['unexpectedFailures'] == 0 and m['p95SecondsNearestRank'] <= 2 for m in metrics.values())
    passed = passed and len(exports) == 20 and all(x['success'] and x['secondsToDownload'] <= 30 and 120 <= x['offsetSeconds'] < 720 for x in exports)
    summary = {'startedAt': wall_start, 'finishedAt': datetime.datetime.now().astimezone().isoformat(),
               'warmupSeconds': 120, 'sampleWindowSeconds': 600, 'elapsedSeconds': elapsed,
               'projectId': project['projectId'], 'releaseId': release,
               'load': {'requirements': 500, 'sources': 100, 'fieldMappingRows': 2000, 'reviewerUsers': 10, 'relations': 499},
               'schedule': '10 simultaneous independent members, 5-second cycles; 20 sequential exports scheduled 28 seconds apart during same sample window',
               'sampling': 'nearest-rank ceil(0.95*N); all started-in-window samples including failures/timeouts retained; no latency samples discarded',
               'metrics': metrics, 'exports': exports, 'pass': passed,
               'rawSamples': str(output / 'samples.jsonl'), 'negativeTestsIncluded': False}
    (output / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    if not passed:
        raise SystemExit(1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--state', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    run(args.state, args.output)
