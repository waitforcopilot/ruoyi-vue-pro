#!/usr/bin/env python3
"""Apply the ordered HRM migrations to an explicitly selected MySQL database."""
import argparse
from pathlib import Path
import re
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--container', required=True)
parser.add_argument('--database', required=True)
parser.add_argument('--fresh', action='store_true', help='Create a NEW isolated database and install the upstream base schema; refuses existing databases')
args = parser.parse_args()
if not re.fullmatch(r'[A-Za-z0-9_-]+', args.database):
    parser.error('Invalid database name')
root = Path(__file__).resolve().parents[2]
manifest = Path(__file__).with_name('migrations.txt')
paths = [root / line for line in manifest.read_text().splitlines() if line and not line.startswith('#')]
if len(paths) != len(set(paths)) or any(not p.is_file() for p in paths):
    parser.error('Invalid or incomplete migration manifest')

def sql(query):
    return subprocess.check_output(['docker', 'exec', args.container, 'mysql', '--batch', '--skip-column-names', '-e', query], text=True)

if args.fresh:
    exists = sql(f"SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='{args.database}'").strip()
    if exists != '0':
        parser.error('--fresh refuses to overwrite an existing database')
    sql(f'CREATE DATABASE `{args.database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci')
    paths.insert(0, root / 'sql/mysql/ruoyi-vue-pro.sql')
else:
    if sql(f"SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='{args.database}'").strip() != '1':
        parser.error('Database does not exist; use --fresh only for a new isolated installation')
for path in paths:
    # Deliberately start with latin1 to ensure each committed script initializes its own charset.
    with path.open('rb') as data:
        subprocess.run(['docker', 'exec', '-i', args.container, 'mysql', '--default-character-set=latin1', args.database], stdin=data, check=True, stdout=subprocess.DEVNULL)
    print(f'Applied: {path.relative_to(root)}', flush=True)
