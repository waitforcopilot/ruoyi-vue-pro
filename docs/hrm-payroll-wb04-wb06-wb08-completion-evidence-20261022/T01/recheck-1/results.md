# T01 复验第 1 轮

日期：2026-10-22。期望/请求模型：`gpt-6-luna / max`；实际运行模型：未核实。候选 HEAD：`234dca9b7e2e328c9fb107b539b9d45d78b8621f`。版本：Node 22.23.3、Prettier 3.8.3、`@vue/compiler-sfc` 3.5.34。

| 检查 | 执行命令与工作目录 | 退出码 | 结果与原始输出 |
|---|---|---:|---|
| 34 例前端回归 | `/opt/homebrew/opt/node@22/bin/node scripts/hrm/frontend_review_confirmation_regression.cjs`；仓库根目录 | 0 | PASS，34 条；[`automation.stdout.txt`](automation.stdout.txt) |
| 两个文件的 Prettier | `/opt/homebrew/opt/node@22/bin/node node_modules/prettier/bin/prettier.cjs --check src/views/hrm/payroll/review/validation.ts src/views/hrm/payroll/review/index.vue`；`yudao-ui/yudao-ui-admin-vue3/` | 0 | PASS，两个文件均符合格式；[`prettier.stdout.txt`](prettier.stdout.txt) |
| Vue SFC parse / compileScript | Node 22 调用 `@vue/compiler-sfc` 3.5.34，对 `index.vue` 执行 `parse()` 与 `compileScript()`；实际命令见前轮 [T01 记录](../automation-checks.md) | 0 | PASS；107 个 bindings；[`sfc-compile.stdout.txt`](sfc-compile.stdout.txt) |
| Git whitespace check | `git diff --check`；仓库根目录 | 0 | PASS，无输出；[`git-diff-check.stdout.txt`](git-diff-check.stdout.txt) |

## 最终三文件 SHA-256

复验前后哈希一致，执行期间未改源码或回归脚本。

| 文件 | SHA-256 |
|---|---|
| `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/validation.ts` | `82903174d28f1743aca049b787dd6099da63aa5647e95ed5e1d32f2076b87471` |
| `yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue` | `a5817f4eff90fe921238e04b4d6ca8d993b3a51b989e090923fb3cd302479bce` |
| `scripts/hrm/frontend_review_confirmation_regression.cjs` | `300b895151aac18f520646aa327b23c2b91b15e6bba7d641edcb695afc6282fd` |

本轮没有执行浏览器、业务、数据库或服务端操作。
