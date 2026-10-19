# S1验收证据索引

日期：2026-10-19。候选代码文件及SHA256见 [candidate-files.json](candidate-files.json)。基准HEAD为 `e7fbed579348298a0f53bafde83d9f39996982f8`。本目录不保存凭据、token、税务密钥及数据库原始备份。所有新增业务材料均为TEST合成夹具，不是正式业务批准。

| 验收 | 结果与证据类别 | 证据 |
| --- | --- | --- |
| E1 | 通过：真实浏览器甲9、乙10各首次46、再次0；真实API新甲13/乙14各46，四并发添加数0/46/0/46，无重复，编号相同而ID不同 | [甲初始化](browser-a-initialized.txt)、[乙初始化](browser-b-initialized.txt)、[冷启动并发API](api-r4.json) |
| E2 | 通过：真实浏览器修改甲COL-01、切乙、切甲、整页刷新；退出后从项目深链接登录自动返回甲9。真实API比较乙整批行不变 | [刷新保留](browser-a-persisted.txt)、[重登](browser-relogin.txt)、[API](api-r4.json) |
| E3 | 通过：真实浏览器比较、取消后仍有差异、保留COL-01原文、采纳COL-02转评审中，仅补COL-04；新增CUSTOM-0001。真实API并发CUSTOM、失败回滚；DB合成REMOVED记录采纳不复活 | [差异](browser-template-differences.txt)、[采纳及补缺](browser-template-result.txt)、[最终CUSTOM](browser-final.txt)、[移除夹具API](templates.json)、[失败回滚](failure-rollback.json) |
| E4 | 通过：真实浏览器改名范围版本不变、普通账号改期间子集失配、扩主体、修正显式子集并重开回显。真实API在两个独立项目15/16分别从十月已确认夹具执行扩主体/改期间，当前签署清空、旧签署历史完全保留 | [普通账号失配](browser-operator-scope.txt)、[子集重开](browser-subset-roundtrip.txt)、[固定范围夹具](scope-final.json)、[名称版本API](api-r4.json) |
| E5 | 通过：真实API拒绝旧scopeVersion、篡改项目/对象、外项目无权访问/历史/保存/初始化/导出、创建人无范围授权、超授权上限；拒绝不新增历史/项目事件。普通账号251浏览器在有效项目授权内修订成功；全局角色未扩权 | [API](api-r4.json)、[范围](scope-final.json)、[事件不变](final-export.json)、[普通账号页面](browser-operator-scope.png) |
| E6 | 通过：真实API逐一核对列表/保存/确认/分派/历史/初始化/导出边界，旧无projectId列表只读历史项目，省略projectId的外项目对象访问仍拒绝。真实浏览器实际下载COL筛选文件，5行均项目9；API整批甲48/乙46、新甲46/新乙46，逐行核对projectId | [隔离API](api-r4.json)、[范围确认API](scope-final.json)、[导出逐行结果](final-export.json)、[历史页面](browser-history.txt) |
| E7 | 通过：真实MySQL当前迁移重复执行；私有完整备份恢复到独立hrm_s1_restore_20261019后执行迁移两次。47旧需求、76历史、1旧项目、1814原角色菜单逐字段比较，含中文UTF8与恢复副本再次比对；原43模板逐字相等，加COL三项共46 | [基准](baseline.json)、[恢复](restore.json)、[最终数据核对](final-preservation.json) |
| E8 | 通过：真实内嵌浏览器完整主链，含普通账号成功操作、刷新/重登、历史及下载；51后端测试、32前端回归、全量vue-tsc、ESLint/Prettier、打包及后端UP | [最终截图](browser-final.png)、[最终页面](browser-final.txt)、[自动检查](checks.json)、[真实浏览器错误](browser-errors.json) |

甲9后来增加合成模板COL-04及CUSTOM-0001，因此最终48项；乙10保持46项。这个增量不改变首次初始化46项的验收证据。模板升级只修改甲9专用TEST目录；DEFAULT目录未修改。`api-r4.json` 的 `browserAcceptance: NOT_RUN` 表示该脚本自身不执行浏览器；页面证据独立列出，不将接口测试改名为浏览器测试。

已保留的真实失败：初次并发初始化在MySQL REPEATABLE_READ下出现死锁；改为项目行锁、当前读和READ_COMMITTED后，新空项目R3/R4均通过。浏览器暴露日期数组回显、范围输入回填和历史范围列取错对象，均已修复并回归。默认Node堆不足导致类型检查退出，8192MB重跑成功。后端重新打包/重启期间的一次请求超时仍保留在浏览器错误记录；最终运行候选的保存、下载、重登及刷新成功。不能据此声称整个会话零错误。

原始API、测试、构建、恢复脚本/日志与浏览器下载文件在仓库外 `~/.hrm-payroll-local/stage1-20261019/`。恢复数据库仅为隔离测试副本；本轮未在实际开发库执行回滚或基础SQL重建。
