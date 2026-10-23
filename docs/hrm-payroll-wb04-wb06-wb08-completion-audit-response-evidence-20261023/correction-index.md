# 2026-10-23 追加纠正说明与证据索引

本说明处理只读的[独立审计原件](../hrm-payroll-wb04-wb06-wb08-completion-fix-report-audit-20261023.md)，不修改该原件、T09历史AX/PNG、final-automation-2或任何旧manifest。编码者期望/请求gpt-6.1-sol / medium，实际运行模型未核实。本说明是编码与报告事实更正，不是独立审计，也不是新候选验收通过报告。

## 1. 需求1845的原件重新定位

逐行定位对象1845，避免把同屏1844的评审中v40套用到1845。下表是本轮直接读取AX及查看原PNG的结果，AX内容与图片可见范围分别判断。

| T09原件 | AX实际内容 | PNG可见内容及边界 |
|---|---|---|
| 003-dispute-missing-fields-input-preserved | 第317行含“请补齐以下字段：异议责任人ID、异议下一步。输入已保留。”；第369行责任人stepper Value为1；下一步无值 | 弹窗中责任人框视觉空白，下一步空白，两处字段错误可见。与AX值1分别保留；不据此推定底层输入值为空，也不更改该原件 |
| 004-dispute-persisted-after-refresh | 第223～237行：1845 / CUSTOM-0002 / 有异议 / 版本10，即DISPUTED v10 | 只显示页面首屏和场景，需求清单在视口外；PNG自身不证明v10或v11。该原件不是“状态矛盾过渡” |
| 005-dispute-response-reviewing-persisted | 第223～237行：1845 / CUSTOM-0002 / 评审中 / 版本11，即REVIEWING v11 | 可见1845行评审中、版本11 |
| 006-dispute-response-history | 历史版本10到11，状态DISPUTED到REVIEWING，版本11操作人266；businessContextJson记录disputeOwnerId=266及nextStep“TEST：由266回应映射第三版说明，再交279本人重新确认。” | 可见DISPUTED到REVIEWING、10到11、版本11操作人266及对象id1845；完整责任/下一步以AX历史JSON为据 |
| 007-assigned279-reconfirmed-refresh | 第222～244行：1845已确认、版本12，本人确认按钮禁用 | 只显示页面首屏，需求行在视口外；不以PNG证明列表状态，也不再将1845已确认AX误称为过渡 |
| 008-confirmed279-history-independent-read | 版本12操作人279、CONFIRMED、confirmedBy279；确认元组scope5/semantic8/source3578 semantic1/relation3583 mapping3 | 可见11到12、REVIEWING到CONFIRMED、操作人279、confirmedBy279；完整元组以AX JSON为据 |

旧`final-automation-2/final-review-complete.md`第28行、`final-automation-summary.md`第34行把004写成REVIEWING v11，活跃fix报告第5节沿用了这句；这是旧执行者摘要/复核及报告的引用错误。三者不是三个独立证明。活跃报告现按004、005、006分工纠正；封存摘要/复核原文保留，后续引用须连同本说明。003的拒绝文案仍成立，但“责任人实值为空”不能由现有原件确定。007的列表确认状态与008的签署历史分别成立。

原件身份及旧manifest字节见[历史身份冻结](historical-source-identities.json)。本轮只追加说明，不重新计算并覆盖旧manifest中的报告哈希或候选HEAD。

## 2. 既有Git交付与本次候选分开

本轮只读Git确认HEAD `d5ff56dbb5b94fdcf069c90f064f332d31f6a6fb`，父HEAD `fdc503dca517b4dad4ea136f73bd9fbe92e11c59`；489个提交路径与旧allowlist的regularPaths+forceRequiredPaths逐路径相同，多出0、缺少0，见[提交与清单比较](delivery-commit-allowlist-comparison.json)。主线程fresh查询真实远端`refs/heads/work`返回d5ff、exit0，见[远端查询原件](delivery-remote-check.json)。这证明既有交付已提交并到达远端，不证明本次新改动已经提交或push。

活跃报告第2/7节已更新；第3节保留既有验收的父HEAD加差异及20文件历史身份，并单独指出本次demo.ts新字节。旧allowlist/manifest仍是提交前历史快照。本次处理提交号用所在Git提交定位，不在文件内预填自身提交哈希。

## 3. 两句产品文案最小追加修正

只修改`yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/demo.ts`的两句主题例外说明：

- RPT“当前清单不冒充S5六表发布快照”改为“当前筛选清单与发布时固定的六表快照分别查看”。
- COL“S5正式发布、六表快照和异步下载另验”改为“独立治理页提供TEST受控发布、固定快照及异步导出，操作成功不等于正式业务批准”。

导航、接口、权限及其余产品代码未改。旧T05/D-01通过范围仍限原计划点名的两处说明和导航，本次追加两句不回填为旧证据通过。

## 4. 串行交接与未完成项

本编码阶段没有测试、构建、浏览器、数据库、stage、commit或push。候选文件字节见[coder候选身份](candidate-identity-coder.json)，交接必要路径见[本轮文件清单](candidate-allowlist.json)；身份清单不自哈希，后续文档如更新须在新轮次重新绑定，不能修改旧T09 manifest。

执行者请求gpt-6-luna / max，后续应独立复核历史引用、哈希保全与文案差异，使用既有格式/lint及必要模块检查，不为两句文案造镜像测试或重复271项后端测试。主线程在可见IAB检查COL/RPT新文字和治理入口，保存AX/PNG；执行者再复核原件。新候选尚未验收，不能把既有工程9/9当成本次回归通过。最终处理报告待真实执行/可见回归结果后补充，正式B与其他原边界不变。
