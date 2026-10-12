# GitHub 构建通知调查与修复

核实日期：2026-10-12。截图邮件对应提交 `752ba1b752`。

## 已核实原因

旧 Java 与前端工作流都会在每次 `master` 推送时触发，没有文件范围筛选。持续按模块提交、推送后，失败运行及 GitHub 自动邮件通知累积。此次查询的最近 8 次运行均失败，每次推送对应 Java、前端两条运行；这不是对邮箱邮件数量的统计。

- [Java 失败运行](https://github.com/waitforcopilot/ruoyi-vue-pro/actions/runs/38240244980)：JDK 设置步骤使用 `actions/setup-java@v2`，日志明确报 `Cache service responded with 400`。Maven 构建步骤被跳过。JDK 8、17 任务失败，11 任务随矩阵取消。
- [前端失败运行](https://github.com/waitforcopilot/ruoyi-vue-pro/actions/runs/38240244867)：使用旧 Node 14/16、pnpm 6/Yarn 配置，缓存指向不存在的 `yudao-ui-admin/yarn.lock`；日志报 `Some specified paths were not resolved, unable to cache dependencies.`。实际 Vue3 工程位于 `yudao-ui/yudao-ui-admin-vue3`，使用 `pnpm-lock.yaml`。

两条工作流都在准备环境/缓存时失败，尚未执行应用构建。远端失败通知不能代替本地代码测试结果；同样，本地测试成功也不能代表 GitHub 工作流成功。

## 修复

- 更新 Checkout、Java、Node、pnpm Actions，使用当前缓存接口。
- Java CI 使用已验证的 JDK 21 环境；项目编译 source/target 仍保持原有 1.8。执行全部 HRM 回归，再打包启用模块和主服务，不再跳过全部测试。
- Maven CI 从 Maven Central 解析依赖，避免构建依赖未验证的地区镜像可用性。
- 前端使用 Node 24、pnpm 10.30.0，工作目录及缓存锁文件指向实际 Vue3 工程，冻结锁文件安装并执行生产构建。
- 后端、前端分别按相关文件变化触发；文档提交不触发构建。保留手动运行及 PR 验证，同分支旧运行可取消。
- 两条工作流均只请求读取源码权限。HRM 测试报告保存为构建产物。

本次提交前验证：暂存版本的 652 项 HRM 测试通过，主服务及启用模块打包通过；冻结锁文件检查和 Vue3 生产构建通过。修复后 GitHub 运行结果须在本次推送后单独核验。
