"""Read-only full-field reconciliation for T09 release 1455 / job 1457.

Reads the local TEST RELEASE and JOB rows within a session read-only consistent
snapshot transaction. Reconstructs the full-project projection from the frozen
JSON and uses the existing verify_workbook.load() parser. Writes only a summary
JSON beside this script; it does not modify the workbook or the database.
"""
from __future__ import annotations

import base64
import hashlib
import importlib.util
import json
import os
import re
import subprocess
import tempfile
from datetime import date
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
EVIDENCE = ROOT / "docs/hrm-payroll-wb04-wb06-wb08-completion-evidence-20261022/T09"
WORKBOOK = EVIDENCE / "release1455-job1457-browser.xlsx"
IDENTITY = EVIDENCE / "job1457-download-identity.json"
WORKSTATION = Path.home() / ".hrm-payroll-local/application-workstation.yaml"
SCHEMA = "hrm_local_20261017"
PROJECT_ID, RELEASE_ID, JOB_ID, ACTOR_ID = 153, 1455, 1457, 266
SHEETS = ["评审版本说明", "功能需求清单", "数据源清单", "字段映射", "问题与待办", "范围与变更"]


def mysql_config():
    config = {}
    for doc in yaml.safe_load_all(WORKSTATION.read_text()):
        if not isinstance(doc, dict):
            continue
        master = doc.get("spring", {}).get("datasource", {}).get("dynamic", {}).get("datasource", {}).get("master", {})
        if isinstance(master, dict):
            config.update(master)
    assert re.fullmatch(r"jdbc:mysql://127\.0\.0\.1:13306/hrm_local_20261017(?:\?.*)?", config.get("url", ""))
    assert config.get("username")
    return config


def option_value(value):
    return '"' + str(value).replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n") + '"'


def read_release_and_job():
    config = mysql_config()
    fd, option_path = tempfile.mkstemp(prefix="hrm-readonly-", suffix=".cnf")
    os.fchmod(fd, 0o600)
    try:
        with os.fdopen(fd, "w") as out:
            out.write("[client]\nuser=" + option_value(config["username"]) + "\n")
            if config.get("password") is not None:
                out.write("password=" + option_value(config["password"]) + "\n")
            out.write("host=127.0.0.1\nport=13306\nprotocol=tcp\n")
        sql = (
            "SET SESSION TRANSACTION READ ONLY; START TRANSACTION WITH CONSISTENT SNAPSHOT; "
            "SELECT @@session.transaction_read_only, DATABASE(); "
            "SELECT id,project_id,kind,version,tenant_id,OCTET_LENGTH(data_json),SHA2(data_json,256),data_json "
            "FROM hrm_payroll_governance_record WHERE deleted=b'0' AND "
            "((id=1455 AND kind='RELEASE') OR (id=1457 AND kind='JOB')) ORDER BY id; ROLLBACK"
        )
        result = subprocess.run(
            ["/opt/homebrew/bin/mysql", "--defaults-extra-file=" + option_path, "--batch", "--raw",
             "--skip-column-names", "--database=" + SCHEMA, "--execute=" + sql],
            check=True, capture_output=True, text=True,
        )
    finally:
        os.unlink(option_path)
    lines = result.stdout.splitlines()
    assert lines and lines[0] == "1\t" + SCHEMA, "read-only session/schema assertion failed"
    rows = {}
    for line in lines[1:]:
        p = line.split("\t", 7)
        assert len(p) == 8
        row = {"id": int(p[0]), "projectId": int(p[1]), "kind": p[2], "rowVersion": int(p[3]),
               "tenantId": int(p[4]), "dataJsonBytes": int(p[5]), "dataJsonSha256": p[6], "dataJson": p[7]}
        rows[row["kind"]] = row
    assert set(rows) == {"RELEASE", "JOB"}
    return rows


def as_id(value):
    return None if value is None else int(str(value))


def as_text(value):
    if value is None:
        return ""
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def as_obj(value):
    if isinstance(value, dict):
        return value
    if value is None:
        return {}
    return json.loads(value) if isinstance(value, str) else {}


def as_list(value):
    return value if isinstance(value, list) else []


def display_name(value, names):
    return "" if value is None else names.get(as_id(value), "当时未记录显示名")


def port_full_projection(release):
    """Port the unfiltered, full-authorization projection branch."""
    snap = as_obj(release.get("snapshot"))
    all_req = as_list(snap.get("requirements"))
    nodes = as_list(snap.get("workbench"))
    reqs = {as_id(r.get("id")): r for r in all_req}
    node_data = {as_id(n.get("id")): as_obj(n.get("dataJson")) for n in nodes}
    sources = {as_id(n.get("id")): n for n in nodes if n.get("kind") == "SOURCE"}
    relations = {as_id(n.get("id")): n for n in nodes if n.get("kind") == "RELATION"}
    relation_exported, relation_visible, hidden_edges = set(), set(), set()
    for rid, node in relations.items():
        d = node_data[rid]
        req_id, source_id = as_id(d.get("requirementId")), as_id(d.get("sourceId"))
        if req_id not in reqs:
            continue
        if source_id in sources:
            relation_exported.add(rid)
            if d.get("active") is True:
                relation_visible.add(rid)
        else:
            hidden_edges.add(req_id)

    issue_visible = set()
    for node in nodes:
        if node.get("kind") != "ISSUE":
            continue
        iid = as_id(node.get("id")); d = node_data[iid]
        affected = {as_id(x) for x in as_list(d.get("requirementIds"))}
        for raw in as_list(d.get("relationIds")):
            rel = relations.get(as_id(raw))
            if rel:
                affected.add(as_id(node_data[as_id(rel.get("id"))].get("requirementId")))
        issue_sources = {as_id(x) for x in as_list(d.get("sourceIds"))}
        for rid in relations:
            if as_id(node_data[rid].get("sourceId")) in issue_sources:
                affected.add(as_id(node_data[rid].get("requirementId")))
        if d.get("wholeProject") is True:
            affected.update(as_id(r.get("id")) for r in all_req)
        if affected.isdisjoint(reqs):
            continue
        relation_ids = {as_id(x) for x in as_list(d.get("relationIds"))}
        if affected.issubset(reqs) and relation_ids.issubset(relation_exported) and issue_sources.issubset(sources):
            issue_visible.add(iid)
        else:
            hidden_edges.update(affected.intersection(reqs))

    names = {as_id(u.get("id")): str(u.get("nickname") or "") for u in as_list(snap.get("userFacts"))}
    basis_ref = "SUMMARY-1"
    sheets = {name: [] for name in SHEETS}
    E, I, C, T, D, L = set(), set(), set(), set(), set(), []
    exempt, placeholders = 0, {}

    for req_id, req in reqs.items():
        if req.get("scopeDecisionJson") is not None:
            sd = as_obj(req.get("scopeDecisionJson"))
            if sd.get("replacementId") is not None and as_id(sd.get("replacementId")) not in reqs:
                hidden_edges.add(req_id)
        redact = req_id in hidden_edges
        ref = "REQ-" + str(req_id)
        if redact:
            placeholders[req_id] = "受限关联-" + str(len(placeholders) + 1)
        business = as_obj(req.get("businessContextJson"))
        sheets[SHEETS[1]].append({
            "记录类型":"REQUIREMENT","记录号":ref,"需求ID":str(req_id),"编号":req.get("code"),"模块":req.get("module"),
            "功能":"关联依据受限，自由文本已裁剪" if redact else req.get("description"),
            "业务场景与字段":"受限" if redact else business,"功能评审":req.get("status"),
            "当前签字有效（发布时）":req.get("currentConfirmationValid") is True,"范围决策":req.get("scopeDecision"),
            "版本":req.get("version"),"语义版本":req.get("semanticVersion"),"来源适用性":req.get("sourceApplicability"),
            "不适用理由":"受限" if redact else req.get("applicabilityReason"),
            "评审人ID":as_text(req.get("reviewerId")),"评审人当时姓名":display_name(req.get("reviewerId"),names),
            "确认时间":req.get("confirmedAt"),"验收条件":"受限" if redact else req.get("acceptance"),
            "规则版本":"受限" if redact else req.get("ruleVersion"),"依据":"受限" if redact else req.get("evidence"),
            "确认引用元组":basis_ref if redact else req.get("confirmedTupleJson"),"发布依据关联":basis_ref,
            "受限关联":placeholders.get(req_id),"优先级":req.get("priority"),
            "实施状态与证据":"受限" if redact else req.get("implementationJson"),
            "协作历史":"受限" if redact else req.get("collaborationJson"),
        })
        histories = [h for h in as_list(snap.get("requirementHistories")) if as_id(h.get("requirementId")) == req_id]
        sheets[SHEETS[5]].append({
            "记录类型":"SCOPE","记录号":ref,"需求ID":str(req_id),"编号":req.get("code"),"范围方式":req.get("scopeMode"),
            "项目范围版本":req.get("projectScopeVersion"),"范围状态":req.get("scopeState"),
            "范围正文":"受限" if redact else req.get("scopeJson"),"范围决策":req.get("scopeDecision"),
            "排除及变更依据":"受限" if redact else req.get("scopeDecisionJson"),
            "页面上下文":"受限" if redact else req.get("businessContextJson"),
            "历史版本关联":"受限" if redact else histories,"发布依据关联":basis_ref,"受限关联":placeholders.get(req_id),
        })
        removed = req.get("scopeDecision") == "REMOVED"
        if not removed:
            E.add(req_id)
            if req.get("scopeDecision") == "INCLUDED": I.add(req_id)
            if req.get("scopeDecision") == "DEFERRED": T.add(req_id)
            if req.get("status") == "DISPUTED": D.add(req_id)
            if req.get("currentConfirmationValid") is True: C.add(req_id)
        reason = str(req.get("applicabilityReason") or "").strip()
        valid_exemption = bool(reason) and len(reason) >= 8 and reason.upper() not in ("N/A","NA","不适用")
        if req.get("sourceApplicability") == "NOT_APPLICABLE" and valid_exemption:
            if not removed: exempt += 1
            sheets[SHEETS[2]].append({
                "记录类型":"不适用声明","记录号":"NA-"+str(req_id),"需求关联":ref,"需求ID":str(req_id),
                "理由":"声明依据受限" if redact else req.get("applicabilityReason"),
                "确认人":as_text(req.get("confirmedBy")),"确认人当时姓名":display_name(req.get("confirmedBy"),names),
                "确认时间":req.get("confirmedAt"),"需求版本":req.get("version"),"声明状态":req.get("status"),"发布依据关联":basis_ref,
            })
            continue
        if removed:
            continue
        found, hidden = False, False
        for fact in as_list(snap.get("relationFacts")):
            if as_id(fact.get("requirementId")) != req_id:
                continue
            rid = as_id(fact.get("relationId"))
            if rid in relation_visible:
                L.append({"requirementId":req_id,"relationId":fact.get("relationId"),"available":fact.get("available")})
                found = True
            else:
                hidden = True
        if hidden or not found:
            L.append({"requirementId":req_id,"relationId":None,"available":False})

    for sid, node in sources.items():
        d = node_data[sid]
        history = [node_data[as_id(h.get("id"))] for h in nodes
                   if h.get("kind") in ("EVIDENCE","HISTORY")
                   and as_id(node_data[as_id(h.get("id"))].get("objectId")) == sid]
        sheets[SHEETS[2]].append({
            "记录类型":"SOURCE","记录号":"SRC-"+str(sid),"来源ID":str(sid),"名称":d.get("name"),
            "负责人ID":as_text(d.get("ownerId")),"负责人当时姓名":display_name(d.get("ownerId"),names),
            "来源规格及供给":dict(d),"冻结历史及核验证据":history,"语义版本":d.get("semanticVersion"),
            "交付版本":d.get("deliveryVersion"),"发布依据关联":basis_ref,
        })
    for rid in relation_exported:
        d = node_data[rid]
        if as_id(d.get("requirementId")) not in reqs:
            continue
        history = [node_data[as_id(h.get("id"))] for h in nodes
                   if h.get("kind") in ("EVIDENCE","HISTORY")
                   and as_id(node_data[as_id(h.get("id"))].get("objectId")) == rid]
        for i, raw in enumerate(as_list(d.get("fields")),1):
            ref = "REL-"+str(rid)+("" if i == 1 else "-F"+str(i))
            sheets[SHEETS[3]].append({
                "记录类型":"MAPPING","记录号":ref,"关系ID":str(rid),"需求关联":"REQ-"+str(d.get("requirementId")),
                "来源关联":"SRC-"+str(d.get("sourceId")),"关系当前生效":d.get("active"),
                "映射版本":d.get("mappingVersion"),"字段映射及异常规则":raw,"质量消费规则":d.get("consumptionRule"),
                "冻结语义质量及变更证据":history,"发布依据关联":basis_ref,
            })
    for req_id, placeholder in placeholders.items():
        sheets[SHEETS[2]].append({"记录类型":"受限关联","记录号":placeholder,"需求关联":"REQ-"+str(req_id),"说明":"关联内容受限"})
        sheets[SHEETS[3]].append({"记录类型":"受限关联","记录号":placeholder,"需求关联":"REQ-"+str(req_id),"来源关联":placeholder,"说明":"关联内容受限"})
        sheets[SHEETS[4]].append({"记录类型":"受限关联","记录号":placeholder,"需求关联":"REQ-"+str(req_id),"说明":"关联内容受限"})

    unresolved = overdue = 0
    created = date.fromisoformat(str(release.get("createdAt"))[:10])
    for node in nodes:
        iid = as_id(node.get("id"))
        if iid in issue_visible:
            d = node_data[iid]
            if d.get("state") != "CLOSED":
                unresolved += 1
                if created > date.fromisoformat(str(d.get("dueDate"))):
                    overdue += 1
            sheets[SHEETS[4]].append({"记录类型":"ISSUE","记录号":"ISSUE-"+str(iid),"问题ID":str(iid),
                                      "处理及复核":d,"发布依据关联":basis_ref})
    for node in nodes:
        if node.get("kind") not in ("TODO","PROPOSAL"):
            continue
        d = node_data[as_id(node.get("id"))]; target = as_id(d.get("objectId"))
        if target in sources or target in relation_exported or target in issue_visible:
            ref = "SRC-"+str(target) if target in sources else "REL-"+str(target) if target in relation_exported else "ISSUE-"+str(target)
            sheets[SHEETS[4]].append({"记录类型":node.get("kind"),"记录号":node.get("kind")+"-"+str(node.get("id")),
                                      "对象关联":ref,"待办及建议":d})

    ready = sum(x.get("available") is True for x in L)
    included_ready = sum(as_id(x.get("requirementId")) in I and x.get("available") is True for x in L)
    included_l = sum(as_id(x.get("requirementId")) in I for x in L)
    def ratio(n, d):
        percent = None if d == 0 else round(n * 100.0 / d + 1e-10, 2)
        return {"numerator":n,"denominator":d,"label":"暂无适用项（0/0）" if d == 0 else str(n)+"/"+str(d),"percent":percent}
    consumed_sources = {as_id(f.get("sourceId")) for f in as_list(snap.get("relationFacts"))
                        if as_id(f.get("requirementId")) in E and as_id(f.get("relationId")) in relation_visible}
    stats = {"counts":{"U":len(reqs),"E":len(E),"I":len(I),"C":len(C),"T":len(T),"D":len(D)},
             "confirmation":ratio(len(C),len(E)),"scopeDisposition":ratio(len(I)+len(T),len(E)),
             "includedConfirmation":ratio(len(I.intersection(C)),len(I)),"readiness":ratio(ready,len(L)),
             "includedReadiness":ratio(included_ready,included_l),"sourceCount":len(consumed_sources),
             "exempt":exempt,"unresolvedIssues":unresolved,"overdueIssues":overdue,"L":L}
    project = as_obj(snap.get("project"))
    sheets[SHEETS[0]].append({
        "记录类型":"SUMMARY","记录号":"SUMMARY-1","项目ID":str(PROJECT_ID),"项目当时名称":project.get("name"),
        "项目范围版本":project.get("projectScopeVersion"),"发布ID":str(RELEASE_ID),"发布版本":release.get("version"),
        "完整性":"本期完整确认需求基线","合成或正式":release.get("mode"),"发布时刻":release.get("createdAt"),
        "发布说明":release.get("reason"),"统计":stats,"筛选":"无","发布依据关联":basis_ref,
        "用途":"需求规格评审；不代表数据可投产",
    })
    gate = as_obj(snap.get("gate"))
    used_ids = {as_id(d.get("recordId")) for d in as_list(gate.get("decisions"))}
    decisions = as_list(snap.get("decisionVersions")) or as_list(gate.get("decisions"))
    for raw in decisions:
        decision = as_obj(raw.get("decision")); record_id = raw.get("recordId"); ref = "DEC-"+str(record_id)
        sheets[SHEETS[0]].append({"记录类型":"DECISION","记录号":ref,"决议ID":decision.get("decisionId"),
                                  "决议版本":decision.get("decisionVersion"),"本次发布使用":as_id(record_id) in used_ids,
                                  "条款及批准事实":decision,"发布时状态":raw.get("lifecycleAtAction"),
                                  "授权关联":"APPROVAL-GRANT-"+str(record_id)})
        sheets[SHEETS[0]].append({"记录类型":"GRANT","记录号":"APPROVAL-GRANT-"+str(record_id),
                                  "关联决议":ref,"当时批准授权闭包":decision.get("approvalAuthority")})
    sheets[SHEETS[0]].append({"记录类型":"GRANT","记录号":"GRANT-PUBLISH","当时发布授权闭包":release.get("publicationAuthority")})
    for root in as_list(snap.get("controlledRoots")):
        sheets[SHEETS[0]].append({"记录类型":"GRANT","记录号":"ROOT-"+str(root.get("id")),
                                  "授权根版本":root.get("version"),"授权根受控正文与签署":as_obj(root.get("dataJson"))})
    for anchor in as_list(snap.get("controlledAnchors")):
        sheets[SHEETS[0]].append({"记录类型":"GRANT","记录号":"ANCHOR-"+str(anchor.get("id")),
                                  "独立登记材料受控副本":as_obj(anchor.get("dataJson"))})
    for grant in as_list(snap.get("projectGrants")):
        rid = grant.get("id")
        root_ref = "历史技术访问授权，无正式根" if grant.get("rootRecordId") is None else "ROOT-"+str(grant.get("rootRecordId"))
        sheets[SHEETS[0]].append({"记录类型":"GRANT","记录号":"GRANT-"+str(rid),"授权事实":grant,"授权根关联":root_ref})
    sheets[SHEETS[0]].append({"记录类型":"SUMMARY","记录号":"PROJECT-HISTORY","项目范围及成员变更历史":snap.get("projectEvents")})
    return sheets


def main():
    identity = json.loads(IDENTITY.read_text())
    file_bytes = WORKBOOK.read_bytes()
    file_sha = hashlib.sha256(file_bytes).hexdigest()
    assert len(file_bytes) == identity["bytes"] and file_sha == identity["sha256"]
    rows = read_release_and_job()
    release_row, job_row = rows["RELEASE"], rows["JOB"]
    release, job = json.loads(release_row["dataJson"]), json.loads(job_row["dataJson"])
    stored_bytes = base64.b64decode(job["bytes"])
    stored_sha = hashlib.sha256(stored_bytes).hexdigest()
    assert (release_row["id"],release_row["projectId"],release_row["kind"]) == (RELEASE_ID,PROJECT_ID,"RELEASE")
    assert (job_row["id"],job_row["projectId"],job_row["kind"]) == (JOB_ID,PROJECT_ID,"JOB")
    assert job.get("releaseId") == RELEASE_ID and job.get("actorId") == ACTOR_ID and job.get("state") == "READY"
    assert stored_sha == file_sha and len(stored_bytes) == len(file_bytes)
    assert hashlib.sha256(('"' + job["bytes"] + '"').encode()).hexdigest() == job.get("fileDigest")
    snap = as_obj(release.get("snapshot"))
    req = next(r for r in as_list(snap.get("requirements")) if as_id(r.get("id")) == 1845)
    confirmed = as_obj(req.get("confirmedTupleJson"))
    source_tuple = as_list(confirmed.get("sources"))[0]
    assert as_id(req.get("confirmedBy")) == 279 and as_id(req.get("confirmedScopeVersion")) == 5
    assert as_id(confirmed.get("projectScopeVersion")) == 5 and as_id(confirmed.get("semanticVersion")) == 8
    assert as_id(source_tuple.get("sourceId")) == 3578 and as_id(source_tuple.get("semanticVersion")) == 1
    assert as_id(source_tuple.get("relationId")) == 3583 and as_id(source_tuple.get("mappingVersion")) == 3

    loader_path = ROOT / "docs/hrm-payroll-wb04-wb06-wb08-fix-evidence-20261021/verify_workbook.py"
    spec = importlib.util.spec_from_file_location("verify_workbook", loader_path)
    loader = importlib.util.module_from_spec(spec); spec.loader.exec_module(loader)
    actual, layout = loader.load(WORKBOOK)
    expected = port_full_projection(release)
    sheets = {}; total_records = total_fields = 0
    for name in SHEETS:
        expected_by_ref = {row["记录号"]:row for row in expected[name]}
        actual_by_ref = {k:v for k,v in actual[name].items() if k != "EXPORT-METADATA"}
        missing = sorted(set(expected_by_ref)-set(actual_by_ref))
        extra = sorted(set(actual_by_ref)-set(expected_by_ref))
        mismatches = []
        for ref in sorted(set(expected_by_ref)&set(actual_by_ref)):
            field_names = sorted(set(expected_by_ref[ref])|set(actual_by_ref[ref]))
            bad = [field for field in field_names if expected_by_ref[ref].get(field) != actual_by_ref[ref].get(field)]
            if bad:
                mismatches.append({"record":ref,"fieldNames":bad})
        fields = sum(len(row) for row in expected_by_ref.values())
        sheets[name] = {"expectedBusinessRecords":len(expected_by_ref),"actualBusinessRecords":len(actual_by_ref),
                        "expectedBusinessFields":fields,"missingRecords":missing,"extraRecords":extra,
                        "mismatches":mismatches,"exactEquality":not missing and not extra and not mismatches}
        total_records += len(expected_by_ref); total_fields += fields
    passed = all(x["exactEquality"] for x in sheets.values())
    result = {
        "task":"T09 release1455 / job1457 full six-sheet reconciliation",
        "method":"local frozen RELEASE JSON read in a session read-only consistent snapshot; full-project projection reconstructed from frozen objects; existing verify_workbook.py load() parser; only EXPORT-METADATA parent excluded",
        "database":{"schema":SCHEMA,"sessionTransactionReadOnly":True,"consistentSnapshot":True,"mutatingStatements":0,"rollbackIssued":True,
                    "releaseRow":{"id":release_row["id"],"projectId":release_row["projectId"],"kind":release_row["kind"],
                                  "rowVersion":release_row["rowVersion"],"tenantId":release_row["tenantId"],
                                  "dataJsonBytes":release_row["dataJsonBytes"],"dataJsonSha256":release_row["dataJsonSha256"],
                                  "releaseVersion":release.get("version"),"mode":release.get("mode"),
                                  "snapshotScopeVersion":as_obj(snap.get("project")).get("projectScopeVersion")},
                    "jobRow":{"id":job_row["id"],"projectId":job_row["projectId"],"kind":job_row["kind"],
                              "state":job.get("state"),"actorId":job.get("actorId"),"releaseId":job.get("releaseId"),
                              "storedBytes":len(stored_bytes),"storedBytesSha256":stored_sha,"matchesBrowserFile":stored_sha==file_sha}},
        "confirmedRequirement1845":{"confirmedBy":as_id(req.get("confirmedBy")),"scopeVersion":as_id(req.get("confirmedScopeVersion")),
                                    "semanticVersion":as_id(confirmed.get("semanticVersion")),"sourceId":as_id(source_tuple.get("sourceId")),
                                    "sourceSemanticVersion":as_id(source_tuple.get("semanticVersion")),
                                    "relationId":as_id(source_tuple.get("relationId")),"mappingVersion":as_id(source_tuple.get("mappingVersion"))},
        "browserDownload":{"taskId":identity.get("taskId"),"releaseId":identity.get("releaseId"),"actor":identity.get("actor"),
                           "method":identity.get("method"),"bytes":len(file_bytes),"sha256":file_sha,
                           "httpStatus":identity.get("httpStatus")},
        "comparisonScope":"every parent record and every field in all six business sheets; only the per-job EXPORT-METADATA parent and its fields excluded",
        "comparedBusinessRecords":total_records,"comparedBusinessFields":total_fields,"sheets":sheets,"layout":layout,
        "loaderChecks":"six ordered sheet names, eight-column headers, no formulas, unique parent rows, closed detail references, complete ordered fragments, bounded row heights",
        "overall":"PASS_FULL_FIELD_RECONCILIATION" if passed else "FAIL_FULL_FIELD_RECONCILIATION",
    }
    output = HERE / "release1455-job1457-full-field-reconciliation.json"
    output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n")
    print(json.dumps({k:v for k,v in result.items() if k not in ("sheets","layout")},ensure_ascii=False,indent=2))
    if not passed:
        for name, item in sheets.items():
            if not item["exactEquality"]:
                print(name,json.dumps({k:item[k] for k in ("missingRecords","extraRecords","mismatches")},ensure_ascii=False))
        raise SystemExit(1)


if __name__ == "__main__":
    main()
