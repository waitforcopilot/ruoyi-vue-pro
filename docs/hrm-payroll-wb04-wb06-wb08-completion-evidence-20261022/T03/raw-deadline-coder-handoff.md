# T03期限原始输入修复候选

日期：2026-10-23。coder期望/请求gpt-6.1-sol / medium，实际模型未核实。本文件为内部交接，不修改最终fix报告。主线程报告的失败现场021/022/023原件保留：issue3531输入2026-13-40被ElDatePicker归一化为2027-02-09并成功重开cycle2；021文件名含rejected不代表成功拒绝。本轮不回滚该历史，不执行DB/浏览器或提交。

## 最小修复

- `WorkbenchPanel.vue`创建`draft.issue.dueDate`及REOPEN的`draft.dueDate`均改为ElInput文本绑定，保持原始YYYY-MM-DD字符串；没有date-picker/date value-format/自动trim/formatter/parser/maxlength（避免长无效输入被裁成合法日）。创建与重开均展示对应字段错误，并用实际aria-label聚焦，失败保持draft与弹窗。
- `issueValidation.ts`新增`validIssueDeadline`：要求字符串精确4-2-2位数字，再计算月份天数及公历闰年；拒绝2026-13-40、2026-02-30、非闰年2-29等，不用dayjs宽松解析或先构造会滚动进位的Date。deadline先判断缺失，只有非空不合法才显示“须为有效的YYYY-MM-DD日期”，故缺失与无效可区分。
- 服务端权限、错误码、问题状态机/历史保持不变。创建端目前`issueValid`直接LocalDate.parse异常路径没有本轮真实服务缺陷证据，未扩展修改；恶意请求边界仍由服务测试处理。

## 有区分力的新增回归

`scripts/hrm/frontend_workbench_issue_deadline_regression.cjs`解析实际WorkbenchPanel SFC模板，定位创建与重开两条真实v-model表达式，要求它们是无归一化设置的ElInput；编译这两个实际模板节点，用真实Vue runtime及真实Element Plus ElInput组件身份产生VNode，调用编译器生成的onUpdate:modelValue，断言draft保留精确原始值。若重新使用ElDatePicker或提前裁剪，绑定检查失败。

随后调用实际SFC script中的createIssue/issueAction/save，使用真实共享validator与Vue refs，只mock传输与页面依赖。计划36例：两模式各13种缺失/无效输入（undefined/null/空、2026-13-40/2026-02-30/2026-02-29/1900-02-29、月日零值、4-31、非补零、超长、两端空格），均无请求、弹窗/原串保留且可改有效2028-02-29提交；两模式各4种有效日期（普通日、2024与2000闰日、2-28）原样提交；两模式各1种后端版本拒绝保留日期。36是待执行计划数，不是通过数。

该集成检查覆盖实际组件类型/v-model原始字符串→共享validator→真实save→传输payload，并非仅纯函数测试；它没有挂载原生DOM/完整ElInput内部事件，不冒称AX输入或实际浏览器通过。改用明确文本输入避免日期picker解析阶段的已知归一化源，仍需主线程可见浏览器对相同无效字符串重演验证。

## 冻结与执行建议

SHA256：

```text
ad48036f300e1b6a190f7df52d1b150a54b0961cd11097be7589d7bb1fc96c77  WorkbenchPanel.vue
7849171aa44e16816ca3ff0e4cce676133ea9d01dd1195d523da4d4f62f37d69  issueValidation.ts
ee24848992b63f43cbedf64959a94e4ff9e84bbe49262be0277a525a4529432d  frontend_workbench_issue_deadline_regression.cjs
```

本轮只编码及git diff空白检查（退出0），没有运行脚本、格式检查、编译/测试或业务操作。T04冻结候选保持不变。

Luna在仓库根目录执行：

```bash
node scripts/hrm/frontend_workbench_issue_deadline_regression.cjs
node scripts/hrm/frontend_review_confirmation_regression.cjs
```

前端目录执行目标Prettier检查与ESLint（沿用项目现有正确命令，只限定WorkbenchPanel.vue与issueValidation.ts）；若formatter指出本轮格式问题，返回coder最小调整。脚本不是隐藏浏览器，不调用HTTP/DB。前端脚本成功后由主线程真实UI复验创建和重开：缺期限、2026-13-40、2026-02-30均不能提交且原输入保留；有效闰日补齐后才成功；以新cycle/新问题或适当当前状态执行，不能篡改旧失败cycle历史。保存拒绝前后issue版本/cycle/历史与网络是否有写入的原件，再交executor复核，不因脚本PASS宣布T03闭包。
