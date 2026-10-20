# 638字段校验：离线可复算证据补充

此目录补齐独立审计指出的投影输入缺失，不改旧报告。数据均为项目105/快照1352的TEST合成评审内容；无凭据、无真实工资或付款材料。

| 文件 | 来源及含义 |
| --- | --- |
| projection-1352.json | 本轮实际API读取1352的当前全可见冻结投影；不是宣称找回上一轮未归档的内存字节 |
| release-1352-job-1361.xlsx | 上一轮实际IAB下载原件的逐字节副本，哈希与旧workbook.json一致；本轮未重新下载 |
| verifier-raw-output.json | 本轮直接运行现有核验器的完整原始输出；相同文件路径传入两次，其重导出字段不可用作历史相等证据 |
| single-file-result.json | 单文件638字段、73长字段、六表及显式关联结果；标明未比较历史重导出，移除有歧义的逐表重导出字段 |
| prior-commit-files.json | 从git show读取上一实现22条路径及回执2条路径，补明进度附记遗漏 |
| manifest.json | 输入审计、核验器、投影、XLSX及结果SHA-256、取数方式和语义边界；本目录说明文件自身不属于原取数时点的manifest条目 |

在仓库根目录离线复算（标准库，无需运行服务或输入凭据）：

```bash
python3 scripts/hrm/verify_governance_workbook.py \
  docs/hrm-payroll-fa01-export-evidence-supplement-20261020/projection-1352.json \
  docs/hrm-payroll-fa01-export-evidence-supplement-20261020/release-1352-job-1361.xlsx \
  docs/hrm-payroll-fa01-export-evidence-supplement-20261020/release-1352-job-1361.xlsx \
  --output /tmp/hrm-1352-single-file-verification.json
```

预期退出0、checkedFields=638、longFieldsWithContinuation=73、sixSheetsExactly=true、noOrphanExplicitReferences=true。CLI需要两个文件参数，此处故意为同一路径，reexportOrderedCellsIdentical仅为自比，**不证明两次历史重导出一致**。本轮不重跑135项Maven测试，不新做浏览器回归、原生打开或性能采样。
