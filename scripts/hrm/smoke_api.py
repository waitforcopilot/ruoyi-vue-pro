#!/usr/bin/env python3
"""Exercise committed payroll APIs against an isolated local test environment."""
import argparse
import json
import os
import urllib.request
import urllib.parse
import urllib.error

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--base-url", default="http://127.0.0.1:48080/admin-api")
parser.add_argument("--tenant-id", default="1")
parser.add_argument("--username", default="admin")
parser.add_argument("--password-env", default="HRM_SMOKE_PASSWORD")
parser.add_argument("--initialize", action="store_true", help="Initialize the PRD list in the test database")
args = parser.parse_args()
if urllib.parse.urlparse(args.base_url).hostname not in ("127.0.0.1", "localhost"):
    parser.error("This smoke test only supports a local test environment")
password = os.environ.get(args.password_env)
if not password:
    parser.error(f"Set {args.password_env} for the test account")
headers = {"tenant-id": args.tenant_id, "Content-Type": "application/json"}

def call(path, data=None, method=None, authenticated=True):
    selected = headers if authenticated else {"tenant-id": args.tenant_id}
    request = urllib.request.Request(
        args.base_url.rstrip("/") + path,
        data=json.dumps(data).encode() if data is not None else None,
        headers=selected,
        method=method,
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.read()
    except urllib.error.HTTPError as error:
        return error.read()

def result(path, data=None, method=None):
    response = json.loads(call(path, data, method))
    if response.get("code") != 0:
        raise RuntimeError(f"{path}: result code {response.get('code')}")
    return response.get("data")

headers["Authorization"] = "Bearer test1"
assert json.loads(call("/hrm/payroll/requirement/list")).get("code") == 401, "Mock authentication must be disabled"
headers.pop("Authorization")
login = result("/system/auth/login", {"username": args.username, "password": password})
headers["Authorization"] = "Bearer " + login["accessToken"]
result("/system/auth/get-permission-info")
for path in (
    "/hrm/payroll/requirement/list",
    "/hrm/payroll/batch/page?pageNo=1&pageSize=10",
    "/hrm/payroll/payment/batches?pageNo=1&pageSize=10",
    "/hrm/payroll/payment/templates",
    "/hrm/payroll/formula/list",
    "/hrm/payroll/formula/groups",
    "/hrm/payroll/formula/variables",
    "/hrm/payroll/grade-policy/list",
    "/hrm/payroll/grade-policy/variables",
    "/hrm/payroll/salary-proposal/list",
    "/hrm/payroll/salary-proposal/employees",
    "/hrm/payroll/entity/list",
    "/hrm/payroll/cost/options",
    "/hrm/payroll/financial/options",
    "/hrm/payroll/input-issue/selectors",
    "/hrm/payroll/tax-policy/entities",
    "/hrm/payroll/overview/entities",
    "/hrm/payroll/wage-recovery/entities",
    "/hrm/payroll/insurance-adjustment/entities",
):
    result(path)
    print(f"Passed: {path.split('?')[0]}")
if args.initialize:
    result("/hrm/payroll/requirement/initialize", {}, "POST")
    assert result("/hrm/payroll/requirement/initialize", {}, "POST") == 0
    assert len(result("/hrm/payroll/requirement/list")) >= 43
    print("Passed: idempotent PRD initialization")
assert call("/hrm/payroll/requirement/export")[:2] == b"PK"
unauthenticated = json.loads(call("/hrm/payroll/requirement/list", authenticated=False))
assert unauthenticated.get("code") == 401
print("Passed: real Excel export and unauthenticated access rejection")

assert call("/hrm/payroll/grade-policy/import-template")[:2] == b"PK"
original_tenant = headers["tenant-id"]
try:
    headers["tenant-id"] = "2" if original_tenant != "2" else "1"
    assert json.loads(call("/hrm/payroll/requirement/list")).get("code") == 403
finally:
    headers["tenant-id"] = original_tenant
print("Passed: mock-token rejection, wrong-tenant rejection and grade Excel template")
