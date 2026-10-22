#!/usr/bin/env python3
"""Start a project role with explicit model settings; dry-run does not call a model."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[1]
ROLES = {
    "coder": ("codex", "gpt-6.1-sol", "medium"),
    "executor": ("codex", "gpt-6-luna", "max"),
    "auditor": ("agent", "grok-4.7", "medium"),
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("role", choices=ROLES)
    parser.add_argument("--task", required=True, help="Task text; do not include credentials")
    parser.add_argument("--dry-run", action="store_true", help="Show the command without starting a model")
    args = parser.parse_args()
    binary, model, effort = ROLES[args.role]
    prompt = (
        f"本次角色：{args.role}。先阅读 AGENTS.md 与 docs/role-model-policy.md。"
        f"按该角色边界处理以下任务：\n{args.task}"
    )
    if binary == "codex":
        command = [binary, "--cd", str(ROOT), "--model", model,
                   "--config", f'model_reasoning_effort="{effort}"', prompt]
    else:
        # Cursor's ask mode keeps the reviewed material read-only. Report text is
        # returned to the terminal; saving it is a separate, named artifact step.
        command = [binary, "--workspace", str(ROOT), "--model",
                   f"{model}[effort={effort}]", "--mode", "ask", prompt]
    if args.dry_run:
        print(json.dumps({"role": args.role, "requestedModel": model,
                          "requestedEffort": effort, "command": command,
                          "actualRuntimeModel": "NOT_STARTED"}, ensure_ascii=False, indent=2))
        return 0
    if shutil.which(binary) is None:
        print(f"Required executable unavailable: {binary}; no model fallback.", file=sys.stderr)
        return 2
    return subprocess.run(command, cwd=ROOT).returncode


if __name__ == "__main__":
    sys.exit(main())
