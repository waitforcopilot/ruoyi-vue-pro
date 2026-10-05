#!/usr/bin/env python3
"""Apply this repository's Vue3 sources to the pinned complete frontend checkout."""
import argparse
import shutil
import subprocess
from pathlib import Path

PINNED_COMMIT = '0af03a93b6b6300f878e28add69b1c7a9f09ec34'
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('frontend', type=Path, help='Complete yudao-ui-admin-vue3 Git checkout')
args = parser.parse_args()
target = args.frontend.resolve()
repo = Path(__file__).resolve().parents[2]
overlay = repo / 'yudao-ui/yudao-ui-admin-vue3'
if not (target / 'package.json').is_file():
    parser.error('Target must be the complete Vue3 project, not the partial directory in this repository')
head = subprocess.check_output(['git', '-C', str(target), 'rev-parse', 'HEAD'], text=True).strip()
if head != PINNED_COMMIT:
    parser.error('Checkout the documented frontend commit first: ' + PINNED_COMMIT)
patch = overlay / 'patches/vue3-build-compat.patch'
command = ['git', '-C', str(target), 'apply']
pending = []
for segment in patch.read_bytes().split(b'diff --git ')[1:]:
    fragment = b'diff --git ' + segment
    forward = subprocess.run(command + ['--check', '-'], input=fragment, capture_output=True)
    reverse = subprocess.run(command + ['--reverse', '--check', '-'], input=fragment, capture_output=True)
    if forward.returncode and reverse.returncode:
        parser.error('Compatibility patch conflicts with local edits: ' + fragment.splitlines()[0].decode())
    if forward.returncode == 0:
        pending.append(fragment)
# Refuse to overwrite any file with independent local edits. Reapplying our overlay is allowed.
files = [overlay / 'pnpm-workspace.yaml'] + sorted((overlay / 'src').rglob('*'))
for source in files:
    if not source.is_file():
        continue
    relative = source.relative_to(overlay)
    destination = target / relative
    if not destination.exists() or destination.read_bytes() == source.read_bytes():
        continue
    original = subprocess.run(['git', '-C', str(target), 'show', 'HEAD:' + relative.as_posix()], capture_output=True)
    if original.returncode or destination.read_bytes() != original.stdout:
        parser.error('Independent edits would be overwritten: ' + relative.as_posix())
if pending:
    subprocess.run(command + ['-'], input=b''.join(pending), check=True)
count = 0
for source in files:
    if source.is_file():
        destination = target / source.relative_to(overlay)
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        count += 1
print('Applied', count, 'overlay files and build compatibility fixes to', target)
