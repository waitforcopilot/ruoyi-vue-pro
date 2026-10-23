#!/usr/bin/env python3
"""Read-only round-2 candidate and historical-source integrity verification."""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path


ROOT = Path.cwd()
EVIDENCE = ROOT / "docs/hrm-payroll-wb04-wb06-wb08-completion-audit-response-evidence-20261023"
ROUND1_IDENTITY = EVIDENCE / "candidate-identity-coder.json"
ROUND2_IDENTITY = EVIDENCE / "coder-round2-format/candidate-identity-round2.json"
HISTORICAL = EVIDENCE / "historical-source-identities.json"


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def main() -> int:
    round1_identity = json.loads(ROUND1_IDENTITY.read_text(encoding="utf-8"))
    round2_identity = json.loads(ROUND2_IDENTITY.read_text(encoding="utf-8"))
    historical = json.loads(HISTORICAL.read_text(encoding="utf-8"))
    current_head = subprocess.run(
        ["git", "rev-parse", "HEAD"], check=True, capture_output=True, text=True
    ).stdout.strip()
    relative_path = round2_identity["source"]["path"]
    current_path = ROOT / relative_path
    current_bytes = current_path.read_bytes()
    current_hash = sha(current_bytes)
    current_size = len(current_bytes)

    base_bytes = subprocess.run(
        ["git", "show", f"{round2_identity['head']}:{relative_path}"],
        check=True,
        capture_output=True,
    ).stdout
    base_text = base_bytes.decode("utf-8")
    old_rpt = "['导出列口径', '讨论主体/期间/维度/金额/规则与字段单位', '当前清单不冒充S5六表发布快照']"
    new_rpt = "['导出列口径', '讨论主体/期间/维度/金额/规则与字段单位', '当前筛选清单与发布时固定的六表快照分别查看']"
    old_col = "'S5正式发布、六表快照和异步下载另验'"
    new_col = "'独立治理页提供TEST受控发布、固定快照及异步导出，操作成功不等于正式业务批准'"
    round1_text = base_text.replace(old_rpt, new_rpt, 1).replace(old_col, new_col, 1)
    round1_bytes = round1_text.encode("utf-8")

    current_identity_matches = (
        current_size == round2_identity["source"]["bytes"]
        and current_hash == round2_identity["source"]["sha256"]
    )
    round1_identity_matches = (
        sha(round1_bytes) == round1_identity["files"][0]["sha256"]
        == round2_identity["round1SourceSha256"]
    )
    whitespace_equivalent = re.sub(rb"\s+", b"", round1_bytes) == re.sub(
        rb"\s+", b"", current_bytes
    )
    historical_checked = 0
    historical_matches = 0
    for record in historical["files"]:
        historical_checked += 1
        path = ROOT / record["path"]
        if path.is_file():
            data = path.read_bytes()
            if len(data) == record["bytes"] and sha(data) == record["sha256"]:
                historical_matches += 1

    head_matches = current_head == round2_identity["head"]
    checks = {
        "round2_source_identity_matches": current_identity_matches,
        "round1_identity_reconstructed_from_HEAD": round1_identity_matches,
        "round1_round2_whitespace_equivalent": whitespace_equivalent,
        "head_matches_round2_record": head_matches,
        "historical_sources_checked": historical_checked,
        "historical_sources_matching": historical_matches,
    }
    for key, value in checks.items():
        print(f"{key}={int(value) if isinstance(value, bool) else value}")
    success = all(
        value
        for key, value in checks.items()
        if key not in {"historical_sources_checked", "historical_sources_matching"}
    ) and historical_checked == historical_matches
    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main())
