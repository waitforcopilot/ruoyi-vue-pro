# T03 raw期限静态复验 2

- 冻结身份：`WorkbenchPanel.vue` SHA-256 `f115485d3201beaa23c84e76c561d994a3ccac8ee030ae6bc57487a099d06650`；`issueValidation.ts` SHA-256 `7849171aa44e16816ca3ff0e4cce676133ea9d01dd1195d523da4d4f62f37d69`。日期回归脚本仍为 SHA-256 `ee24848992b63f43cbedf64959a94e4ff9e84bbe49262be0277a525a4529432d`。检查前后哈希见对应记录，均未变化。
- Prettier 对两文件 `--check`：exit 0，全部匹配项目格式。
- ESLint 对两文件：exit 0，无输出。
- 36例 raw deadline 集成回归与34例既有确认回归沿用前轮通过结果，本轮按指令未重跑。
- 格式差异依据：coder交接报告临时Prettier输出仅调整两个节点布局、无语义变化；旧候选在前轮只保存哈希、没有保存源文件或差异，因此本执行者无法独立重算旧/新逐字节差异。详细 provenance 在 `format-delta-provenance.md`。
- 未执行浏览器、MySQL、后端测试、打包或重启。请求模型为 gpt-6-luna/max；实际模型未核实。
