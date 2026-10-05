#!/usr/bin/env python3
"""Normalize the pinned Vue3 lockfile's mirror URLs, preserving every package hash."""
import argparse
import subprocess
from pathlib import Path

PINNED_COMMIT = '0af03a93b6b6300f878e28add69b1c7a9f09ec34'
MIRROR = b'tarball: https://registry.npmmirror.com/'
OFFICIAL = b'tarball: https://registry.npmjs.org/'

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('frontend', type=Path)
args = parser.parse_args()
target = args.frontend.resolve()
head = subprocess.check_output(['git', '-C', str(target), 'rev-parse', 'HEAD'], text=True).strip()
if head != PINNED_COMMIT:
    parser.error('Only the verified complete Vue3 baseline may be normalized: ' + PINNED_COMMIT)
original = subprocess.check_output(['git', '-C', str(target), 'show', 'HEAD:pnpm-lock.yaml'])
normalized = original.replace(MIRROR, OFFICIAL)
lockfile = target / 'pnpm-lock.yaml'
if lockfile.read_bytes() not in (original, normalized):
    parser.error('Independent lockfile edits would be overwritten; resolve them before normalization')
if b'registry.npmmirror.com' in normalized:
    parser.error('Unexpected mirror URL format; refusing a partial normalization')
lockfile.write_bytes(normalized)
print('Normalized', original.count(MIRROR), 'tarball URLs; package versions, integrity hashes and dependency graph unchanged')
