# T04 / T06 / T07 / T08 内部 closure 建议

日期：2026-10-23（Asia/Shanghai）。仅为最终 fix 报告提供可引用的内部 closure 结论；不是 Cursor 独立审计、正式业务批准或最终 fix 报告。候选为 HEAD `fdc503dca517b4dad4ea136f73bd9fbe92e11c59`，核心候选 SHA 与 final-automation-2 执行前后相同；实际模型未核实。

| 任务 | 建议 | 可引用的可见/自动化证据 | 结论边界 |
|---|---|---|---|
| T04 | **可关闭（TEST范围）** | 联合 Maven Surefire `HrmPayrollT04IssueConsistencyTest` 23/23；browser/020–027：020 issue3590 当前mapping2提交待复核；021/022真实列表均显示3590已关闭、类别DATA_READINESS、期限10/31，数据仍未就绪；023是第二轮 `待复核`；024提交原因绑定当前mapping2；025映射更新为v3；026提交旧mapping2绑定被拒，提示当前版本/范围已变化、输入保留；027刷新后仍是 `待复核`。 | 021原名“close-rejected-input-retained”错误；原始PNG/AX不重命名、不改写，结论应更正为“实际关闭成功”。它不是负例，也没有证明数据已交付。026才是旧绑定拒绝；027保持review。分类/问题关闭不能推断来源供数/QS/QC完成，也不扩张为正式业务验收。 |
| T06 | **可关闭（TEST导出与已打开原件范围）** | 联合测试 `HrmPayrollT06ExportFixtureTest` 1/1；`TEST-FIXTURE-PROVENANCE.txt` 说明长/空数据经真实生产 `workbook()` exporter生成，POI字段重建通过。现有 native-boundary1429 长/空文件原始identity `6e3c5ead…adecbaad` / `632eb7f…e06fe16` 与当时逐表Excel PNG/AX绑定。job1454本轮IAB下载件 `d32e9c9e…a975a3e5` 有六表Excel实际阅读，POI独立对账覆盖126业务记录/1130字段，6表均与冻结projection和job1429业务记录精确相同。 | 新一轮T09自动化再次生成的长/空POI fixture SHA不同（`0e90e7fe…b3223976cc` / `f6d8005d…b3223976cc`），但这只是另一次自动化测试输出；它们未替换旧Excel原件，也不冒充已打开文件。两个文件身份须在报告中分开。job1454关闭native时 `saved=false`、`printed=false`；本结论不声称全文件逐单元格肉眼检查、执行打印或正式业务批准。 |
| T07 | **可关闭（待1455 v4下载链落盘身份通过）** | 联合测试 `HrmPayrollT07GovernanceConsistencyTest` 31/31；browser/023–028历史链：旧mapping2 request在草稿依赖变化后受阻、输入保留；新v3请求发布1452且重试复用1452；此前有效release 1407/1433保留，1426仍为待批准。T09/008以真实历史读证明1845 v12由279本人确认，scope5/semantic8/source3578 semantic1/relation3583/mapping3元组；T09/011可见冻结release1455 v4读取同一v12并标出当前签字有效（发布时）。 | 1452是v12之前的版本，不能替代1455 v4。主线程已报告1455 v4已发布并正执行真实IAB下载；将1455实际下载路径、HTTP 200、bytes/SHA和六表全字段冻结对账补齐后，关闭最后的冻结/交付链。不要把TEST有效签字/发布写成正式业务批准。 |
| T08 | **可关闭（TEST授权/撤权/导出边界）** | 联合测试 `HrmPayrollT08FrozenExportContractTest` 16/16；browser/001–010证明权限受限用户无法读取/完整发布，限定节选1441可成功；root1442激活后可生成1445，撤权后旧job领取被拒、撤权后新job创建被拒；其他有效成员267按自身权限创建/下载job1447成功，文件SHA及实际响应独立留档。受限projection JSON验证只暴露1658/3341/3344等范围内对象。 | 只支持这组TEST账号、项目和对象的可见IAB/API结果，不扩张为全权限矩阵、生产多租户/MySQL验收或其他用户权限。HTTP业务错误响应与“前端隐藏按钮”分开引用。 |

## 后端与前端运行身份

运行后端PID 55351从当前 `yudao-server/target/yudao-server.jar` 启动；jar启动前mtime、当前SHA和嵌入HRM exporter `.class` 与target/classes字节比对见 `runtime-artifact-identity.json`。当前Vite dev PID 86218在5173端口服务；实际GET治理页模块返回200，正文含当前SFC行为标记，原件及headers已保存。联合自动化的全部候选源文件、HEAD和tracked diff前后未变。运行端实际模型仍为 `unverified`。

## 最终闭环状态

T04、T06、T08建议关闭；T07的1455 v4实际浏览器下载、文件身份和冻结六表重建仍待主线程补齐。只有该项通过后，才建议把四项一起写成关闭。T09/003缺字段时输入保留、006历史记录异议v10至回应v11、008确认v12、011冻结v4都已复核；最终v12发布的1455证据已经入库，保供/映射等TEST业务状态不代表正式业务批准。
