#!/usr/bin/env python3
"""Read-only XLSX verification against a frozen governance projection (stdlib only)."""
import argparse
import hashlib
import json
import re
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {'s': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
SHEETS = ['评审版本说明', '功能需求清单', '数据源清单', '字段映射', '问题与待办', '范围与变更']


def read_book(path):
    with zipfile.ZipFile(path) as book:
        strings = []
        if 'xl/sharedStrings.xml' in book.namelist():
            strings = [''.join(x.itertext()) for x in ET.fromstring(book.read('xl/sharedStrings.xml')).findall('s:si', NS)]
        names = [x.attrib['name'] for x in ET.fromstring(book.read('xl/workbook.xml')).findall('s:sheets/s:sheet', NS)]
        assert names == SHEETS, names
        result = {}
        for n, name in enumerate(names, 1):
            rows = []
            root = ET.fromstring(book.read(f'xl/worksheets/sheet{n}.xml'))
            for row in root.findall('s:sheetData/s:row', NS):
                values = []
                for cell in row.findall('s:c', NS):
                    index = 0
                    for c in re.match(r'[A-Z]+', cell.attrib['r'])[0]:
                        index = index * 26 + ord(c) - 64
                    values.extend([None] * (index - len(values)))
                    kind = cell.attrib.get('t')
                    text = cell.findtext('s:v', default='', namespaces=NS)
                    if kind == 's':
                        value = strings[int(text)]
                    elif kind == 'inlineStr':
                        value = ''.join(cell.find('s:is', NS).itertext())
                    elif kind == 'b':
                        value = text == '1'
                    elif kind in ('str', 'e'):
                        value = text
                    else:
                        value = float(text) if text else None
                    assert cell.find('s:f', NS) is None, 'Unexpected formula'
                    values[index - 1] = value
                rows.append(values)
            width = max(map(len, rows), default=0)
            result[name] = [r + [None] * (width - len(r)) for r in rows]
        return result


def verify(expected, first, second):
    checked = long_fields = 0
    ids = {}
    results = []
    for name in SHEETS:
        a, b = first[name], second[name]
        clean = lambda rows: [r for r in rows if len(r) < 2 or r[1] != 'EXPORT-METADATA']
        assert clean(a) == clean(b), f'Re-export changed business cells: {name}'
        headers, records = a[0], clean(a[1:])
        assert len(records) == len(expected['sheets'][name]), name
        ids[name] = {r[1] for r in records}
        assert len(ids[name]) == len(records), f'Duplicate record IDs: {name}'
        for record, projected in zip(records, expected['sheets'][name]):
            for field, value in projected.items():
                if value is None:
                    continue
                indexes = [i for i, h in enumerate(headers) if re.sub(r'（续\d+）$', '', str(h)) == field]
                assert indexes, (name, field)
                if isinstance(value, (int, float, bool)):
                    actual = record[indexes[0]]
                    # POI preserves Long values (IDs/epoch milliseconds) as exact text.
                    if isinstance(actual, str) and not isinstance(value, bool):
                        assert actual == str(value), (name, field, 'numeric text precision')
                        actual = value
                else:
                    actual = ''.join(str(record[i]) if record[i] is not None else '' for i in indexes)
                    if isinstance(value, (dict, list)):
                        actual = json.loads(actual)
                assert actual == value, (name, projected.get('记录号'), field)
                checked += 1
                long_fields += len(indexes) > 1
        results.append({'sheet': name, 'businessRows': len(records), 'columns': len(headers), 'reexportOrderedCellsIdentical': True, 'fieldsReconstructedExactly': True})
    for rows in expected['sheets'].values():
        for row in rows:
            for key in ['发布依据关联', '授权关联', '授权根关联', '关联决议']:
                value = row.get(key)
                if isinstance(value, str) and re.match(r'^(SUMMARY-|GRANT-|APPROVAL-GRANT-|ROOT-|ANCHOR-|DEC-)', value):
                    assert value in ids[SHEETS[0]], (key, value)
            for key, target in [('需求关联', SHEETS[1]), ('来源关联', SHEETS[2])]:
                if row.get(key):
                    assert row[key] in ids[target], (key, row[key])
            if row.get('对象关联'):
                assert any(row['对象关联'] in v for v in ids.values()), row['对象关联']
    return {'sixSheetsExactly': True, 'checkedFields': checked, 'longFieldsWithContinuation': long_fields,
            'noOrphanExplicitReferences': True, 'onlyExportMetadataExcludedFromComparison': True, 'checks': results}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('projection', type=Path)
    p.add_argument('first', type=Path)
    p.add_argument('second', type=Path)
    p.add_argument('--output', type=Path)
    args = p.parse_args()
    report = verify(json.loads(args.projection.read_text()), read_book(args.first), read_book(args.second))
    report['files'] = [{'file': x.name, 'sha256': hashlib.sha256(x.read_bytes()).hexdigest()} for x in [args.first, args.second]]
    text = json.dumps(report, ensure_ascii=False, indent=2) + '\n'
    if args.output:
        args.output.write_text(text)
    print(text)


if __name__ == '__main__':
    main()
