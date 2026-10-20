# HRM Git变更文件审查与归档记录

日期：2026-10-20。本轮起点`work` / `50db82ea68291ac886eef1db4d48c13227dfea9e`。用户要求审计Git变更、提交必要文件及文档并push；本轮没有修改业务代码、数据库、权限或服务。

## 1. 审查结论及提交范围

起点仅有1份已跟踪交接文档的175行追加，以及62份未跟踪文档/合成证据。审查后全部属于本项目需求、历史验证和审计链，决定归档；另新增本记录，并在交接文档增加最新阅读入口。

| 分类 | 文件数 | 必要性与处理 |
| --- | --- | --- |
| 本地运行交接 | 1 | 第10/11节实际暂停与启动记录此前未归档；保留历史，新增第0节指向最新阶段/FA-01结果，避免执行旧初始化命令 |
| PRD、独立审计、整改及阶段提示词 | 13 | 补齐阶段报告引用的v0.4、原代码审计、S5/S6执行提示词及S1～S6修复审计/独立复核，原文保持不变 |
| 早期内嵌浏览器连续流程证据 | 30 | 合成主体/员工的页面AX、3张截图、时间线及哈希清单，支撑早期流程报告；保留失败现场，不改写为当前失败 |
| 早期API运行核实证据 | 19 | 虚构工资/考勤/工时/缴费/需求验证JSON，报告明确API与浏览器及未验边界，不包含会话或凭据 |
| 本次审查记录 | 1 | 说明归档判断、输入哈希及验证边界 |

合计本次提交64个文件（1份已有文档修改、62份既有未跟踪文件、1份本记录）。没有临时构建输出、下载原件、私有状态、数据库备份或配置进入提交。

## 2. 审计检查

- 审查交接文档完整diff、13份Markdown的结构/范围及历史状态标记、全部JSON结构和敏感字段；3张PNG均实际查看，显示合成流程/模板及错误现场，不包含登录输入。
- 全部JSON可以解析；本轮提交Markdown的本地目标链接均存在。浏览器manifest列出的29项证据字节SHA-256逐项匹配；未为通过检查改写原证据。
- 常见JWT、GitHub令牌、私钥格式扫描无命中；JSON非空password/accessToken/refreshToken/authorization/secret/privateKey/apiKey/encryptionKey类字段无命中。
- 对本机私有凭据/配置及会话中的已知口令、秘密值做精确比对，无命中；只记录结果，不输出这些值。未将仓库外私有目录加入Git。
- 原PRD、原审计和历史证据均不改正文；包含“当时未跟踪”“原候选/旧缺陷”的陈述保留其日期语境，不因本次归档篡改旧结论。
- 交接首段仍是最初编写时的说明，第0节明确历史范围与最新入口；禁止用旧`--fresh`或密钥生成步骤覆盖已有本机库。
- 提交前对白名单执行`git diff --cached --check`、核对文件类型和路径；Git交付命令成功及远端SHA在实际执行后核对，不预填自指提交SHA。

## 3. 验证边界及剩余状态

本轮是文档/证据归档，不是重新完成业务验收；未运行Maven、迁移、性能或浏览器业务写流程。133项测试及FA-01 IAB发布/重试的实际结果仍引用[上一轮报告](hrm-payroll-fa01-fix-report-20261020.md)，本轮没有将其记成新执行结果。

F-07真实授权及条款材料、F-15正式组合验收仍开放；旧2026-10-17浏览器报告中的定薪阻塞仅为当时现场，后续修复状态按[修复进度](hrm-payroll-fix-progress-20261017.md)及阶段报告查阅。所有归档工资金额、员工及外部回盘均为原报告声明的合成离线样本，不是正式工资/付款/申报材料。

## 4. 原未跟踪输入哈希

以下62项为归档前实际输入；提交前再次确认内容未变。本记录和增加最新入口的交接文档不在此只读输入集合内。

```text
6baf46ce80fc0669666f738e9a429417fccbad628c0886c7175cb964b489dbd7  docs/hrm-browser-flow-20261017/01-entity-created.txt
8b77103b6444ef35978b7219d8e870ab7bff14373bf72f4a143a94f726f9f397  docs/hrm-browser-flow-20261017/02-entity-self-approval-rejected.txt
ebac81e1c8a8cd95aebdb102049ed20da8f0453168ea4f4311dfdbe194857551  docs/hrm-browser-flow-20261017/04-entity-approved.txt
56f5d655486f6f4078bd511191c0d56187a8c048803e244713444b0e051e3fed  docs/hrm-browser-flow-20261017/06-employee-created.txt
4bbc7010f511f1ebfc271a896a6473b0fef09677e4f0ab13ec2e169d0cac083f  docs/hrm-browser-flow-20261017/08-batch-created.txt
7e42f5501dee959932f8ca3f7d6a134ed7d370625465e338763ced35ef33a6cf  docs/hrm-browser-flow-20261017/11-entity-row-selection.txt
f13141726ff488784b1da3511da85a683116c7245036588e659dd5c09ee86629  docs/hrm-browser-flow-20261017/12-membership-submitted.txt
6ce32146282afcde96d9fb3e7cde57ee6529eabfca2d553aca3f87cdb3511f1e  docs/hrm-browser-flow-20261017/13-membership-approved.txt
f840ac12ee8315c84bd976ea31dcdfa4fa6a57773ccb4a27d2c78e0a36b3cfbe  docs/hrm-browser-flow-20261017/14-salary-proposal-submitted.txt
b7ad6321808879385ec696e8b6035110087e7034b20c98b6be2981c93c65e845  docs/hrm-browser-flow-20261017/15-work-rule-submitted.txt
e1476cdec51ab49c084a75d288a6b0db9a8396f2d0b248969a71dc555219ddee  docs/hrm-browser-flow-20261017/16-insurance-policy-submitted.txt
1c8199de1d2e2c198da652e3b8b91cacaafb1c3f1a8a203470764a368903f99f  docs/hrm-browser-flow-20261017/17-tax-policy-submitted.txt
46c3229cfa9e50812c122dfbf6e43126f491e222f76d9e0240b3a21d604dda68  docs/hrm-browser-flow-20261017/18-tax-profile-submitted.txt
6ca0e378dad3c2414713837a1b318d6a18a2a96d0f717c0664b656fcc09a1601  docs/hrm-browser-flow-20261017/19-tax-policy-approved.txt
fe14d9789ec2e815c7785cabc92d7be3e90e4ec18fd0add0beb3a21e065f8ffc  docs/hrm-browser-flow-20261017/20-salary-hr-reviewed.txt
a834cf69faed83e79529a34c5787ba370ef8c0e16c823668352bb534dc667984  docs/hrm-browser-flow-20261017/21-insurance-policy-approved.txt
411ed214166bda903269767f58cecf6574cffc0ad098eea981bae34d67422817  docs/hrm-browser-flow-20261017/22-work-rule-approved.txt
2ca0d0a7a43e743ff6cdbb2f613a058cbce5b9c11d9f6fa5b6d3f979d5c4d64a  docs/hrm-browser-flow-20261017/23-salary-finance-result.png
256bbb43643a5dbeaaab515d81e6b19c2baf258499d8f8f3243ebf160f273445  docs/hrm-browser-flow-20261017/23-salary-finance-result.txt
6beaa5692da1fb30a7b4b30997e393dce813eddd68c942a14f42cefa830c0ed6  docs/hrm-browser-flow-20261017/24-salary-approval-blocker-detail.txt
81f29e74eda9ce1180ee3e670c0f3f9e5323521ae5cd1e37e0e8136263e107fa  docs/hrm-browser-flow-20261017/25-salary-rejected-template.txt
989deca9dcf97ca40c0e4679667b09c221a583b04ad81f980783ef8357448d1d  docs/hrm-browser-flow-20261017/27-basic-salary-template-created.png
02731d4c0eb5604743696d52ff6866f00d0ef8e84a907f8ee13bd05051c5e78e  docs/hrm-browser-flow-20261017/27-basic-salary-template-created.txt
509d339d4f8a5d64a1b2c7784906ec314acdd828565c4cccb5697c175680152c  docs/hrm-browser-flow-20261017/28-salary-template-options-empty.txt
86e30eb361035868d98e59ef34b481e556e8f699b84e9565874a1fe8cd8ffbdf  docs/hrm-browser-flow-20261017/29-salary-template-refresh-empty.txt
42015da01a44e3b731d8dbefe903058a68e4bf17e78cc8707f82c5e485d99586  docs/hrm-browser-flow-20261017/30-salary-template-reopen-empty.png
363b6fc4c32628a9394f2f535b983b0f421afc3ea53a3758e1bed3d3861eb6d5  docs/hrm-browser-flow-20261017/30-salary-template-reopen-empty.txt
74bc5b71778b87203ddbb279d1c2c514942a2fcec1b0fbebbd06ec9475676d66  docs/hrm-browser-flow-20261017/browser-errors.json
4f3a15f314e7d9b3702db589e5bacc8fa4690f7e3c83eb73695003f777b1bde2  docs/hrm-browser-flow-20261017/manifest.json
6dabf5ffdd1c32eac4c7ec8a0c9f2551215e9e0782391bf7de51b135e7679436  docs/hrm-browser-flow-20261017/timeline.json
4ec5d75a7c3dbe9cba8646fe3c4732631192aff453ebad89dc2a5dc375b13b58  docs/hrm-live-verification-20261017/attendance-runtime.json
902537643b063d79217d18374662053db84295f41b25a7f5c07944b020221b77  docs/hrm-live-verification-20261017/bonus-salary-bonus-runtime.json
16043c2d8a480bd95efab2d5719834211e8dea9b8766886ad0c66b039f221cee  docs/hrm-live-verification-20261017/f08-payment-cost-runtime.json
2407a288afc59cce3d91a19ed5f54db3ef89b9f83ae8c06737fbb6cfae3233c7  docs/hrm-live-verification-20261017/f08-payroll-runtime.json
ac82f80396d147693a5842e2ed75192a8c1d37ec80bd4dcaa6397fecc3082bdb  docs/hrm-live-verification-20261017/f08-report-exports.json
e9f7e27fab78c3d6820af2c29053e3dfb2eaee8176654f339dc6e1de9cd46443  docs/hrm-live-verification-20261017/f08-slip-runtime.json
115e56a1e6464c7b3c0f2740c12eb0d8be3adafab8c1db3359f18861d3a00ffe  docs/hrm-live-verification-20261017/f08-source-readiness.json
95b0f1f09f5cb6ca5f9915238b99be3eb4d40c342c4e38b18dff070530085f08  docs/hrm-live-verification-20261017/future-draft-interference.json
ad9a787ed424fad624b6544c28042bc56c9b364a1a172a901f1d0111fca10992  docs/hrm-live-verification-20261017/grade-and-export-runtime.json
257647e310478b2830b6d23d91ce015651d799d653a87ecb90911b8818a1898d  docs/hrm-live-verification-20261017/history-tax-filing-runtime.json
5ed49416449376eebe5511657c29b0e7b848750a98cdd00d43c3ce74ee4658c3  docs/hrm-live-verification-20261017/insurance-bill-runtime.json
16032603c10bbcd3cebe21fa0a1d8f1a3e64319018920720f4c903c9adee0ef6  docs/hrm-live-verification-20261017/manifest.json
c55062bf7d3a14a85968e8ee11330a10f563ce6ecce424c24fc15485d09b8056  docs/hrm-live-verification-20261017/precheck.json
957df13da6bcb4236c86e462f7cf23cd41e7254c46ca7df97180ed943b35188d  docs/hrm-live-verification-20261017/requirement-prd04-runtime.json
5217ce60fad4a6511bde29d0d9f0ab53d03131ec30ba6e96f8571a51ac625dec  docs/hrm-live-verification-20261017/requirement-runtime.json
c27d2070199b1dc9a316dca32fead7750c94e3776c17ec44ab5d2cd4dbb1c5d7  docs/hrm-live-verification-20261017/scope-runtime.json
a603a71fb11fdf119689715d90febcd070a333027c907235c973d79351034e55  docs/hrm-live-verification-20261017/wage-structure-response.json
cb7fe460ed9cd1d057389d22c07f55b0eeb11230af69529585f6a94bf60ecbc3  docs/hrm-live-verification-20261017/work-standard-summary.json
a0f5e5b930e5331d612b21a4467af3cf4188d2945c6cd2a695a7210ad0ca96a7  docs/hrm-live-verification-20261017/work-subcases-runtime.json
f5763f21f363875035eb191db9b93f083dffcd904f1131732b6f12e1a9dce6da  docs/hrm-payroll-browser-flow-20261017.md
6432faee40606ccd2bbd989b39363a4058fff97957465deac16dfee5775860a1  docs/hrm-payroll-functional-verification-20261017.md
7350322dd7d729275d2e424cecc04961f3c672edd86e29043a7502151523e985  docs/hrm-payroll-requirements-workbench-prd-20261017.md
453b05dbd7edcfdfcd95ab560ea004ce2852ab6b93620b33c744da4108483bab  docs/hrm-payroll-requirements-workbench-prd-audit-remediation-20261017.md
c492679fab8ed22d558046944fbe73f4f9fd6547bec3af004778b01fd1c5a6f5  docs/hrm-payroll-requirements-workbench-prd-independent-audit-20261017.md
81a24cbc38ac887affedb82962dcae008ac1eac3b2cca6f060b0dfb4f6cc017a  docs/hrm-payroll-requirements-workbench-prd-independent-reaudit-v03-20261017.md
4cec46b8e092d44d8519f435452a50dea669d3312596bc7581c909a396937ceb  docs/hrm-payroll-requirements-workbench-prd-reaudit-v04-remediation-20261017.md
9e17cfc0c3af83d7cf10d5c9ee60862fd70169cbf14cb19861a617ec935e92d5  docs/hrm-payroll-stage-1-6-fix-audit-20261020.md
2a178eda302ef423ba690d02a15160ae4df60e33e145562141c0b87e4271cc6e  docs/hrm-payroll-stage-1-6-fix-audit-reaudit-20261020.md
02a3fb5f3fa3a593d170162032a5ca7485adacf51dc6a309b5c6377800c4cda8  docs/hrm-payroll-stage-2-6-prompts-audit-20261019.md
1a54d3f83030b54bdab571f818576483033a59f7b9f3e533ef166b60bdb51f6d  docs/hrm-payroll-stage-5-execution-prompt-20261019.md
9447a9dce2fbd6501e4f08c8cccb72739f382c0b73d56cec3771b411d2fe70a1  docs/hrm-payroll-stage-6-execution-prompt-20261019.md
9a09b4d3a83d69e01063f6291a5847ade0e1c3ecb41b69a4eb5967f533c86393  docs/hrm-payroll-workbench-code-audit-v04-20261017.md
```
