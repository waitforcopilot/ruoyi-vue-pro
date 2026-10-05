基于 Vue3 + element-plus 实现的管理后台。仓库地址：

* Gitee：<https://gitee.com/yudaocode/yudao-ui-admin-vue3>
* GitHub：<https://github.com/yudaocode/yudao-ui-admin-vue3>

本目录是后端仓库内的前端增量，缺少完整项目的 package.json，不能直接安装和启动。
薪酬需求征集的正式源码、构建兼容补丁和依赖构建配置保存在此目录；固定完整前端提交后，
使用 `script/hrm/apply-vue3-overlay.py` 应用。安装、迁移、回归和实际页面截图见
[薪酬需求征集模块交付记录](../../docs/hrm-payroll-requirements-module.md)。

PR 自动回归使用 JDK 8、Node 24.19.0、pnpm 11.19.0 和上述固定 Vue3 基准。
安装前通过 `script/hrm/normalize-vue3-lockfile.py` 仅将基准锁文件的镜像 tarball 地址
改为官方 registry，保留版本、完整性哈希及依赖图，再以 `--frozen-lockfile` 安装。
完整执行与验收说明见 [薪酬 CI 交付记录](../../docs/hrm-payroll-ci-module.md)。
