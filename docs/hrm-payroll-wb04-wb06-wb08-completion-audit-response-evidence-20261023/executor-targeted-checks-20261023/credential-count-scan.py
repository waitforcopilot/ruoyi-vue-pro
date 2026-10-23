#!/usr/bin/env python3
"""Scan only this candidate's new/delta text files and print counts, never values."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


ROOT = Path.cwd()
EVIDENCE = ROOT / "docs/hrm-payroll-wb04-wb06-wb08-completion-audit-response-evidence-20261023"
OUTPUT_DIR = EVIDENCE / "executor-targeted-checks-20261023"
IDENTITY = EVIDENCE / "candidate-identity-coder.json"
ALLOWLIST = EVIDENCE / "candidate-allowlist.json"
TEXT_SUFFIXES = {".ts", ".js", ".cjs", ".mjs", ".py", ".md", ".json", ".patch", ".txt", ".toml", ".yaml", ".yml", ".xml", ".csv", ".log"}
PATTERNS = (
    re.compile(r"-----BEGIN (?:RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----"),
    re.compile(r"\bAKIA[0-9A-Z]{16}\b"),
    re.compile(r"\bgh[pousr]_[A-Za-z0-9]{20,}\b"),
    re.compile(r"\bsk-[A-Za-z0-9_-]{20,}\b"),
    re.compile(r"\beyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b"),
    re.compile(r"(?i)\bBearer\s+[A-Za-z0-9._~+/-]{20,}"),
    re.compile(r"(?i)\b(?:password|passwd|secret|api[_-]?key|access[_-]?key|client[_-]?secret|token|authorization|cookie)\b\s*[:=]\s*[\"']([A-Za-z0-9+/_.=-]{16,})[\"']"),
)


def main() -> int:
    identity = json.loads(IDENTITY.read_text(encoding="utf-8"))
    allowlist = json.loads(ALLOWLIST.read_text(encoding="utf-8"))
    paths = {ROOT / item["path"] for item in identity["files"]}
    paths.update(ROOT / item for item in allowlist["paths"])
    paths.update(
        path for path in EVIDENCE.iterdir() if path.is_file()
    )
    paths = {path for path in paths if path.suffix.lower() in TEXT_SUFFIXES and path.is_file()}
    # Also include the directly edited product source and the original audit object.
    paths.add(ROOT / "yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts")
    paths.add(ROOT / "docs/hrm-payroll-wb04-wb06-wb08-completion-fix-report-audit-20261023.md")

    candidate_files = 0
    candidate_matches = 0
    for path in paths:
        if not path.is_file() or path.suffix.lower() not in TEXT_SUFFIXES:
            continue
        content = path.read_text(encoding="utf-8", errors="replace")
        matched = sum(len(pattern.findall(content)) for pattern in PATTERNS)
        if matched:
            candidate_files += 1
            candidate_matches += matched

    print(f"text_files_scanned={len(paths)}")
    print(f"files_with_secret_like_candidates={candidate_files}")
    print(f"secret_like_candidate_count={candidate_matches}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
