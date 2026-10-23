# T07 反馈提示候选交接（内部）

2026-10-23。实际缺陷原件为browser/010：ROOT1430签署成功后，1426 APPROVE被正确拒绝，取消对话框仍并列显示旧成功。008文件名错误，实际1426待批、1407有效；原件不改名/覆盖。

最小修复仅governance/index.vue的14处入口新增`success.value = ''`：root/decision/action打开及提交、load/history/precheck/retryPublish/readRelease/createJob/refreshJob/claim。发布入口委托retryPublish。保留原失败说明、依据/正文、发布请求号及现有错误处理，不改授权或后端重叠规则。

固定候选：

- SFC SHA256：`663519c7dd73e8d001e2c2a911eebbf7e81d3c16b942c1bdca6850c7b016cdde`
- 新脚本`script/hrm-payroll-governance-feedback.test.cjs` SHA256：`8e61d3f6307c7039f226cf4020f286429509318dbb8cae3881276d1a1f094eb8`

新测试使用Vue实际ref/computed，从实际SFC脚本通过TypeScript AST只移除imports后执行真实handler；API/router/生命周期边界stub，不复制handler，不挂载DOM、不连网络或业务DB。15个拟执行实例覆盖SIGN成功→APPROVE拒绝→取消仍保留失败/无旧成功，3打开入口不隐藏失败，3提交在await前清旧成功且保留完整输入并可重试，发布请求身份保留，7读取/下载入口清过期成功。测试未执行，不写PASS。

executor明确请求gpt-6-luna / max，保留实际模型核实状态。仓库根最小命令：

```bash
/opt/homebrew/opt/node@22/bin/node --test script/hrm-payroll-governance-feedback.test.cjs
```

随后使用UI项目现有依赖执行该SFC parse/compileScript及Prettier check，保存命令、退出码、输出和候选SHA。失败原件保留、返回coder，不由executor改候选。自动化通过后主线程可见回归：先取得真实新成功，再打开新动作并拒绝，依据保留、取消后仅有本次失败；不将批准重叠正确拒绝改成成功。

本轮coder已写完并固定SFC，不再改候选直至executor结果。因未提前与主线程串行交接，SFC热更新曾重置主线程未修改的补充草稿对话框；主线程确认无新数据丢失并暂停页面写入，后续产品编辑先交接再操作。此次未执行编译/测试/业务DB/浏览器，未提交、未改最终fix报告。coder请求gpt-6.1-sol / medium，实际模型未核实。
