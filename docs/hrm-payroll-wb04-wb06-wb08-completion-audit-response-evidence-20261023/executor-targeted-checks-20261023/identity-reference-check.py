#!/usr/bin/env python3
"""Read-only candidate identity and Markdown reference check."""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path


ROOT = Path.cwd()
EVIDENCE = ROOT / "docs/hrm-payroll-wb04-wb06-wb08-completion-audit-response-evidence-20261023"
IDENTITY = EVIDENCE / "candidate-identity-coder.json"
HISTORICAL = EVIDENCE / "historical-source-identities.json"
REPORT = ROOT / "docs/hrm-payroll-wb04-wb06-wb08-completion-fix-report-20261022.md"
CORRECTION = EVIDENCE / "correction-index.md"


def digest(path: Path) -> tuple[int, str]:
    data = path.read_bytes()
    return len(data), hashlib.sha256(data).hexdigest()


def local_links(markdown_path: Path) -> list[Path]:
    body = markdown_path.read_text(encoding="utf-8")
    links: list[Path] = []
    for match in re.finditer(r"(?<!!)\[[^\]]*\]\(([^)]+)\)", body):
        destination = match.group(1).split()[0].strip("<>")
        lowered = destination.lower()
        if lowered.startswith(("https://", "http://", "mailto:", "data:", "javascript:")) or destination.startswith("#"):
            continue
        destination = destination.split("#", 1)[0]
        if not destination:
            target = markdown_path
        else:
            target = (markdown_path.parent / destination).resolve()
        links.append(target)
    return links


def main() -> int:
    identity = json.loads(IDENTITY.read_text(encoding="utf-8"))
    historical = json.loads(HISTORICAL.read_text(encoding="utf-8"))
    head = subprocess.run(
        ["git", "rev-parse", "HEAD"], check=True, capture_output=True, text=True
    ).stdout.strip()
    candidate_checked = 0
    candidate_matches = 0
    for record in identity["files"]:
        candidate_checked += 1
        path = ROOT / record["path"]
        if path.is_file() and digest(path) == (record["bytes"], record["sha256"]):
            candidate_matches += 1

    historical_checked = 0
    historical_matches = 0
    for record in historical["files"]:
        historical_checked += 1
        path = ROOT / record["path"]
        if path.is_file() and digest(path) == (record["bytes"], record["sha256"]):
            historical_matches += 1

    checked_links = 0
    broken_links = 0
    for markdown_path in (REPORT, CORRECTION):
        for target in local_links(markdown_path):
            checked_links += 1
            if not target.exists():
                broken_links += 1

    head_matches = head == identity["head"]
    print(f"candidate_identity_files_checked={candidate_checked}")
    print(f"candidate_identity_files_matching={candidate_matches}")
    print(f"historical_source_files_checked={historical_checked}")
    print(f"historical_source_files_matching={historical_matches}")
    print(f"head_matches_candidate={int(head_matches)}")
    print(f"active_report_and_correction_links_checked={checked_links}")
    print(f"broken_local_links={broken_links}")

    success = (
        candidate_checked == candidate_matches
        and historical_checked == historical_matches
        and head_matches
        and broken_links == 0
    )
    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main())
