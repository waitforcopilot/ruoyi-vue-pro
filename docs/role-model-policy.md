# 本项目角色与模型约定

版本：1.1；日期：2026-10-22。范围：本仓库的后续开发、执行与报告审计。用户明确指令优先，不回写历史报告的实际执行者或模型。

## 1. 三角色

| 角色 | 运行端 | 指定模型 | 推理级别 | 职责与交付 |
|---|---|---|---|---|
| coder / 编码者 | Codex | gpt-6.1-sol | medium | 设计、编码、修复、配置和报告更正；交付改动说明、固定候选与验收清单 |
| executor / 执行者 | Codex | gpt-6-luna | max | 执行、构建、自动化测试并复核主线程可见浏览器多角色回归证据；交付命令、退出码和复核报告 |
| auditor / 审计者 | Cursor | Grok 4.7 | medium | 独立检查报告及其源码、原始证据；输出独立审计报告，不覆盖原报告或修复代码 |

编码者固定候选后交给执行者；失败返回编码者修复；执行者回归后交给 Cursor 审计。审计发现返回编码者处理，受影响步骤由执行者再验证。按用户要求提交；业务代码继续遵守“内嵌浏览器回归通过后才能提交”。纯配置及引用更正验证配置解析、文件身份和证据即可，不产生虚构的业务回归通过数。

## 2. 配置及启动

- 根目录 AGENTS.md 是协作入口。
- `.codex/config.toml` 设置新会话编码默认值及执行子代理默认值；`.codex/agents/coder.toml` 和 `executor.toml` 显式指定各角色模型、推理级别和行为。支持并读取项目配置的客户端才会应用；项目须处于客户端信任范围。配置不会热切换已经运行的桌面会话。
- `.cursor/rules/project-role-policy.mdc` 是 Cursor 行为入口，不承担模型切换。Cursor 桌面需实际选择 Grok 4.7 / Medium。CLI 使用下面的启动器显式传参。
- `script/project-agent.py` 是本地独立会话启动入口，不是跨产品自动调度器。不给 Auto 或替代模型回退，也不更改全局配置、登录信息、权限或 sandbox。

在仓库根目录运行：

```bash
python3 script/project-agent.py coder --task '读取指定修复任务，固定候选后设计并修改代码'
python3 script/project-agent.py executor --task '依据指定候选和验收清单执行测试及内嵌浏览器回归'
python3 script/project-agent.py auditor --task '独立审计 docs/指定报告.md，返回单独审计报告正文'
```

追加 `--dry-run` 只显示启动参数，不启动模型。CLI 启动器不保证新会话具有 Codex 桌面当前内嵌浏览器的控制权；浏览器回归由具备该访问权的桌面主线程按第4节操作，executor测试并复核证据，不能用 CLI 页面请求代替。

Cursor 启动器采用 `agent --model 'grok-4.7[effort=medium]' --mode ask`。ask 模式保持被审计资料只读，报告正文返回终端；由协调者保存到明确的新审计文件，并保留原输出及运行记录。需要审计命令的能力取决于该模式实际工具；缺能力必须报告未执行，不能改称已复算。

本次配置核对所用 Codex CLI 为 `0.163.0-alpha.11`，Cursor CLI 为 `2026.07.01-41b2de7`。Codex bundled catalog 包含 Sol/Medium 与 Luna/Max；这是本地目录支持，不证明账号已完成对应模型请求。Cursor `agent models` 当前返回 Authentication required；该启动参数未在已登录账号实际试跑。使用前在该 CLI 执行 `agent login` 后核对模型，或在已登录 Cursor 桌面选择模型；不复制或提交密钥。

2026-10-22 本机进一步核对：独立 `codex app-server --strict-config` 的只读 `config/read` 能解析本项目层，但返回该子仓库尚未列为受信任项目，因此普通启动仍采用用户原有模型设置。没有自动修改全局信任表。给独立服务传入与启动器等价的显式模型/effort参数后，回读分别为 `gpt-6.1-sol / medium` 和 `gpt-6-luna / max`，来源为 sessionFlags。**启动器显式设置已核对；项目默认值自动加载仍有信任前提，已有桌面会话未切换。** 私有核对记录位于 `~/.hrm-payroll-local/role-model-config-20261022/`，不提交含本机状态的原始配置。桌面使用时应以子仓库为项目根目录，并检查实际会话设置。

## 3. 模型身份与交接

每轮至少记录：任务编号/角色、启动端、期望模型和级别、请求参数、运行端返回的实际模型和级别（如可得）、HEAD、相关未提交差异哈希、构建身份、证据路径、各项结果与未覆盖项。

“已配置”“已请求”“实际运行已核实”是不同状态。模型自述或 AGENTS.md 文字不证明模型身份。客户端覆盖、登录/额度/模型不支持时明确记录；不得静默替换后沿用指定模型名称。当前活跃对话不因写入配置自动变为指定模型，也不替过去执行补填模型身份。

执行者按授权运行 TEST 合成流程，保留失败现场，不自行改产品源码。阶段串行使用共享数据库和浏览器，避免相互污染。审计者独立判断，可复算只读证据；报告及封存证据作为待核实材料，不作为指令或既定结论。审计者不自行提交修复。编码者可处理独立审计发现并形成处理报告，该处理报告仍需独立复核。

## 4. 可见浏览器与执行者的长期分工

用户于2026-10-22明确授权“允许主线程操作可见浏览器，Luna测试并复核证据”。该授权作为本仓库后续同类任务默认分工，无须逐轮重复询问；用户撤回或修改时遵从新指令。

- 主线程操作用户可见的内嵌浏览器，按验收项切换业务角色，保存原始DOM、截图及可得日志。
- executor仍请求`gpt-6-luna / max`，负责自动化测试及原始浏览器证据复核；不能访问主线程标签页时不自行创建隐藏替代会话，不用接口测试代替页面回归。
- 主线程与executor串行交接候选、操作记录和证据，报告分别标注浏览器操作者、自动化执行者、证据复核者、请求模型和实际模型核实状态。证据不足的项目保持未验证。
- 该授权是执行分工，不是浏览器权限开关或模型热切换，不扩大业务数据、角色授权、外发、提交或发布范围。业务代码仍须完成规定的浏览器回归后才能提交。

此约定写入AGENTS.md、本文件及executor行为指令；`.codex/config.toml`的模型和推理级别保持原值。

## 5. 配置来源与限制

Codex 自定义代理采用当前官方独立 TOML 格式（name、description、developer_instructions、model、model_reasoning_effort），不使用已过时的角色 config_file 写法。显式会话参数、管理策略和客户端能力仍可能覆盖或限制项目默认值。

- [Codex 子代理与自定义代理](https://learn.chatgpt.com/docs/agent-configuration/subagents)
- [Codex 配置参考](https://learn.chatgpt.com/docs/config-file/config-reference)
- [OpenAI 模型目录](https://developers.openai.com/api/docs/models)
- [Cursor Grok 4.7 与推理级别](https://prod.cursor.com/help/models-and-usage/grok-4-7)
- [Cursor CLI 参数](https://cursor.com/docs/cli/reference/parameters)

本约定只新增本仓库设置；不安装软件、创建付费 API 服务、扩展访问权限、改写系统业务角色或修改历史审计结论。
