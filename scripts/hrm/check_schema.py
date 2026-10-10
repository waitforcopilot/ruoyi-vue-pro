#!/usr/bin/env python3
"""Check HRM/OA/BPM entity columns against an explicitly selected test database."""
import argparse
import re
import subprocess
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--container", required=True, help="MySQL test container")
parser.add_argument("--database", required=True)
parser.add_argument("--source-root", type=Path, default=Path(__file__).resolve().parents[2])
args = parser.parse_args()
if not re.fullmatch(r"[A-Za-z0-9_-]+", args.database):
    parser.error("Invalid database name")
query = (
    "SELECT table_name,column_name FROM information_schema.columns "
    f"WHERE table_schema='{args.database}'"
)
data = subprocess.check_output(
    ["docker", "exec", args.container, "mysql", "--batch", "--skip-column-names", "-e", query],
    text=True,
)
columns = {}
for line in data.splitlines():
    table, column = line.split("\t")
    columns.setdefault(table, set()).add(column)

errors, tables, field_count = [], set(), 0
for module in ("hrm", "oa", "bpm"):
    directory = args.source_root / f"yudao-module-{module}/src/main/java/cn/iocoder/yudao/module/{module}/dal/dataobject"
    for file in directory.rglob("*.java"):
        source = file.read_text()
        table = re.search(r'@TableName\((?:value\s*=\s*)?"([^"]+)"', source)
        if not table:
            continue
        name = table[1]
        tables.add(name)
        if name not in columns:
            errors.append(f"Missing table: {name}")
            continue
        # Keep fields of the outer data object, excluding nested DTO properties.
        code = re.sub(r"/\*[\s\S]*?\*/|//[^\n]*", "", source)
        for match in re.finditer(r"private\s+([\w<>., ?\[\]]+)\s+(\w+)\s*;", code):
            prefix = code[:match.start()]
            if prefix.count("{") - prefix.count("}") != 1 or "static" in match[1].split():
                continue
            annotations = prefix[prefix.rfind(";") + 1:]
            if re.search(r"@TableField\([^)]*exist\s*=\s*false", annotations):
                continue
            override = re.search(r'@TableField\((?:value\s*=\s*)?"([^"]+)"', annotations)
            column = override[1] if override else re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", match[2]).lower()
            field_count += 1
            if column not in columns[name]:
                errors.append(f"Missing column: {name}.{column} ({file.name})")

for error in errors:
    print(error)
print(f"Checked {len(tables)} tables and {field_count} fields; {len(errors)} missing items")
raise SystemExit(bool(errors))
