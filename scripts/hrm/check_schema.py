#!/usr/bin/env python3
"""Check HRM/OA/BPM entity columns against an explicitly selected test database."""
import argparse
import re
import subprocess
import tempfile
import shutil
import os
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

# javac parses comma declarators, initializers, generics and annotations correctly.
# No application classes are loaded; nested DTO members are deliberately excluded.
files = []
for module in ("hrm", "oa", "bpm"):
    directory = args.source_root / f"yudao-module-{module}/src/main/java/cn/iocoder/yudao/module/{module}/dal/dataobject"
    files.extend(str(file) for file in sorted(directory.rglob("*.java")))
files.append(str(args.source_root / "yudao-framework/yudao-spring-boot-starter-mybatis/src/main/java/cn/iocoder/yudao/framework/mybatis/core/dataobject/BaseDO.java"))
helper = Path(__file__).with_name("SchemaFields.java")
with tempfile.TemporaryDirectory(prefix="hrm-schema-") as compiled:
    compiler = shutil.which("javac")
    if not compiler:
        parser.error("Install a JDK and put javac on PATH")
    tools_jar = Path(compiler).resolve().parents[1] / "lib/tools.jar"
    classpath = os.pathsep.join([compiled, str(tools_jar)]) if tools_jar.exists() else compiled
    subprocess.run([compiler, "-cp", classpath, "-encoding", "UTF-8", "-d", compiled, str(helper)], check=True)
    declarations = subprocess.check_output(["java", "-cp", classpath, "SchemaFields", *files], text=True)
errors, tables, expected, inherited, tenant_ignored = [], set(), {}, set(), set()
for line in declarations.splitlines():
    name, column, file = line.split("\t")
    if name == "@base":
        if column != "-":
            inherited.add(column)
        continue
    tables.add(name)
    expected.setdefault(name, {})
    if column == "@tenant-ignore":
        tenant_ignored.add(name)
        continue
    if column != "-":
        expected[name][column] = file
if not inherited:
    raise RuntimeError("BaseDO fields were not parsed; refusing an incomplete check")
field_count = 0
for name in sorted(tables):
    for column in inherited:
        expected[name].setdefault(column, "BaseDO.java")
    # The tenant interceptor requires this storage column, independently of BaseDO.
    if name not in tenant_ignored:
        expected[name].setdefault("tenant_id", "tenant interceptor")
    field_count += len(expected[name])
    if name not in columns:
        errors.append(f"Missing table: {name}")
        continue
    for column, file in sorted(expected[name].items()):
        if column not in columns[name]:
            errors.append(f"Missing column: {name}.{column} ({file})")

for error in errors:
    print(error)
print(f"Checked {len(tables)} tables and {field_count} fields; {len(errors)} missing items")
raise SystemExit(bool(errors))
