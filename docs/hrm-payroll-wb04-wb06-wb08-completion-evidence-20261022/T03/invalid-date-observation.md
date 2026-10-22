# T03 日期输入失败观察（只读）

日期：2026-10-23。范围仅为复核封存的 `T03/browser/021`、`022-FAIL`、`023-FAIL` AX/PNG 及日期校验源码。没有运行服务/测试、操作浏览器或数据库，也没有修改产品代码或其他封存证据。

## 结论

此处不是“无效日期被拒绝”，而是一个失败的浏览器用例：主线程操作记录说明重开问题3531时输入 `2026-13-40`；日期组件在请求提交前将其规范化为 `2027-02-09`，表单因此提交成功。服务端收到的是合法日期，无法从规范化后的值识别用户原输入无效。不得把 `021-reopen-invalid-date-rejected` 这个文件名或先前服务测试结果当作该页面负例通过证据。

用户输入的原始字符串 `2026-13-40` 来自主线程本轮操作说明；这三份封存AX/PNG没有显示该原始文本或请求payload，独立可见的是规范化后的值和成功结果。报告区分了操作记录与原件能直接证明的事实。

## 原件能证明什么

- `021-reopen-invalid-date-rejected.txt/png`：AX树显示问题3531，动作REOPEN，`新期限`控件值为`2027-02-09`，并捕获到“已保存，状态按当前证据重新计算”。PNG画面已回到问题列表，截止日显示`2027-02-09`。这与文件名中的“rejected”相反，不能证明拒绝。
- `022-FAIL-invalid-date-normalized-and-reopened.txt/png`：问题3531显示“待处理 / ADVISORY / 2027-02-09”，处理人269、复核人279。PNG显示重开后的列表项。记录为明确FAIL。
- `023-FAIL-normalized-date-cycle2-history.txt/png`：问题3531对象历史版本7的事件为`ISSUE_REOPEN`，操作者279；快照中`dueDate=2027-02-09`、`state=OPEN`、`cycle=2`，第一周期字段按重开流程清空。历史PNG显示工作台对象历史。记录为明确FAIL。

三张截图均为1269×1190像素；AX和PNG内容相互一致。以上原件只读保留，未重命名或覆盖。

## 源码与测试边界

当前表单在 `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/requirement/WorkbenchPanel.vue`（约406–412行）以 `el-date-picker` 将控件值绑定到`draft.dueDate`并使用`value-format="YYYY-MM-DD"`。`src/views/hrm/payroll/requirement/issueValidation.ts`（约11–12行）只对REOPEN日期做非空检查。服务端 `HrmPayrollWorkbenchService.issueAction`（约257行）再对已绑定的字符串执行`LocalDate.parse`；`2027-02-09`是有效值，服务端看不到并无法校验组件接收的原始输入`2026-13-40`。

当前39例T03服务契约中，`reopenDeadlineRejectsWithoutDestroyingFirstCycle`直接向服务传入缺失、空值和`2026-02-30`；这些测试证明服务端在收到无效日历字符串时拒绝并保留第一周期数据。T04的23例也是服务/状态/并发契约测试。二者都不驱动Vue的日期控件，也不检验用户输入在组件绑定前的解析/规范化，故不能证明浏览器对原始无效日期安全。39例与23例仍各自保留原测试通过结论，但这条UI边界需要新候选修复后的单独页面复测。

## 当前结论边界

浏览器证据显示3531在捕获时已由REOPEN成功进入cycle 2 OPEN，期限为2027-02-09。没有从SQL读取或写入该状态；本报告只根据封存页面原件记录。coder已接手日期输入修复；待新候选冻结后，应由执行者复验前端静态/自动化，再由主线程通过可见页面重新输入非法日期，确认被拒绝且3531仍保持旧版本/关闭状态；有效日期正例通过后再继续完整周期回归。实际运行模型未核实。
