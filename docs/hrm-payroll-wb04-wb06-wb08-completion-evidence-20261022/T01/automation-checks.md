# T01 自动化检查记录

日期：2026-10-22。角色：executor。期望/请求模型：`gpt-6-luna / max`；实际运行模型：未核实。

候选基线：HEAD `234dca9b7e2e328c9fb107b539b9d45d78b8621f`。目标源码当时已有工作区差异；本次检查只写入本目录的证据文件，没有修改源码、格式、数据库或运行服务。

## 结果

| 检查 | 命令与工作目录 | 退出码 | 结果 |
|---|---|---:|---|
| 前端回归脚本 | `/opt/homebrew/opt/node@22/bin/node scripts/hrm/frontend_review_confirmation_regression.cjs`；仓库根目录 | 0 | PASS，34 条检查；完整 stdout 见 [`automation.stdout.txt`](automation.stdout.txt) |
| pnpm 调用 Prettier | `pnpm exec prettier --check src/views/hrm/payroll/review/validation.ts src/views/hrm/payroll/review/index.vue`；`yudao-ui/yudao-ui-admin-vue3/` | 1 | 未能启动格式检查。pnpm 报 `ERR_PNPM_ABORTED_REMOVE_MODULES_DIR_NO_TTY`，在无 TTY 环境中中止其 modules 目录处理；stdout/stderr 见 [`prettier-pnpm-attempt.stdout.txt`](prettier-pnpm-attempt.stdout.txt) |
| 直接调用 Prettier | `/opt/homebrew/opt/node@22/bin/node node_modules/prettier/bin/prettier.cjs --check src/views/hrm/payroll/review/validation.ts src/views/hrm/payroll/review/index.vue`；`yudao-ui/yudao-ui-admin-vue3/` | 1 | 格式检查发现 `index.vue` 有格式差异；`validation.ts` 未报告格式问题。没有运行 `--write`。stdout 见 [`prettier-check.stdout.txt`](prettier-check.stdout.txt) |
| Vue SFC parse / compileScript | 使用 Node 22.23.3 调用 `@vue/compiler-sfc` 3.5.34，对 `src/views/hrm/payroll/review/index.vue` 执行 `parse()` 和 `compileScript()` | 0 | PASS；解析无错误，检测到 `<script setup>`，生成 107 个 bindings。stdout 见 [`sfc-compile.stdout.txt`](sfc-compile.stdout.txt) |

SFC 检查实际命令：

```sh
/opt/homebrew/opt/node@22/bin/node -e 'const fs=require("node:fs");const {parse,compileScript}=require("./node_modules/.pnpm/@vue+compiler-sfc@3.5.34/node_modules/@vue/compiler-sfc");const filename="src/views/hrm/payroll/review/index.vue";const {descriptor,errors}=parse(fs.readFileSync(filename,"utf8"),{filename});if(errors.length)throw new Error("parse errors: "+errors.map(String).join("; "));const compiled=compileScript(descriptor,{id:"hrm-payroll-review"});process.stdout.write(JSON.stringify({status:"PASS",filename,hasScriptSetup:Boolean(descriptor.scriptSetup),bindingCount:Object.keys(compiled.bindings).length},null,2)+"\n")'
```

## 候选文件身份

Node 22.23.3、Prettier 3.8.3、`@vue/compiler-sfc` 3.5.34。

| 文件 | 本次操作前与操作后的 SHA-256 |
|---|---|
| `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/validation.ts` | `82903174d28f1743aca049b787dd6099da63aa5647e95ed5e1d32f2076b87471` |
| `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue` | `497c91c1e4c1059d8bef8f8bebe9a4da7534616de0bb7ad1471ccbe70157d704` |
| `scripts/hrm/frontend_review_confirmation_regression.cjs` | `300b895151aac18f520646aa327b23c2b91b15e6bba7d641edcb695afc6282fd` |

Prettier 未通过，需编码者处理格式后再复验。此记录只覆盖自动化回归、指定文件格式及 SFC 语法；不代表内嵌浏览器或服务端验收通过。
