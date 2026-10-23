# T04 可见浏览器操作内部记录

日期：2026-10-23（Asia/Shanghai）。仅整理主线程此次真实UI操作及browser/001–008原件，不是最终fix报告、Luna复核结论或独立审计。本记录编写者未重新操作浏览器。浏览器操作者为主线程；executor已反馈下述004/007图文对应限制；本记录不代其完成全部8组原件的复核，实际模型均未由本记录核实。

本机页面为 `http://127.0.0.1:5173`，项目153「TEST WB completion 20261022 A · 非正式批准」、范围版本5，关联需求1844。问题3569标题为「TEST_WBC_T04_BASELINE_OVERDUE_20261023：仅本机合成，验证全项目阻断与逾期」，处理人266，独立复核人279。下表以原件编号排列，每个编号均有同名txt（AX文本）及png（截图），不是把重复DOM当多次独立观察。

| 顺序与原件（browser目录） | 操作/原件可支持的观察 | 边界 |
|---|---|---|
| 001-created-overdue-whole-project-3569 | 266成功创建3569，BASELINE_BLOCK、到期2026-10-22；本机日期2026-10-23，列表显示「待处理…已逾期」。007的创建快照另证wholeProject=true、requirementIds=[1844]、sourceIds=[] | 全项目与关联需求同时存在，不是无任何关联对象的全项目独立创建正例 |
| 002-baseline-whole-project-precheck-blocked | 266到治理页面执行预检，显示「当前范围的强制决议覆盖不完整或冲突」「本期纳入集合为空」「存在相关未关闭基线阻断问题」 | 多项条件同时不齐；不能写成仅因3569导致完整发布失败的单变量后端实验。AX是页面文案，不含原始BASELINE_ISSUES_OPEN响应code |
| 003-handler-processing-overdue-no-reclassify-control | 266开始处理；列表为「处理中 / BASELINE_BLOCK / 2026-10-22 已逾期」，处理人界面没有复核分类控件 | 控件缺失仅证明本角色页面表现，不代替后端越权调用负例 |
| 004-handler-submitted-overdue-baseline | 004 AX显示「待复核 / BASELINE_BLOCK / 2026-10-22 已逾期」；提交成功以004 AX和007历史ISSUE_SUBMIT v3交叉支持 | executor早先将004 PNG判为处理中/提交复核属于误报；主线程及文档编写者重新查看均确认当前PNG为待复核，未覆盖原件。提交不等于关闭 |
| 005-reviewer-reclassify-no-evidence-rejected | 切换279，RECLASSIFY选择ADVISORY但不填处理或复核依据；前端显示「请补齐以下字段：处理或复核依据。输入已保留。」原因及分类仍在表单，底层列表仍BASELINE_BLOCK | 这是有AX和截图的前端校验拒绝；未据此推论请求已到服务或后端精确拒绝code |
| 006-reviewer-reclassified-advisory-still-overdue | 279补独立复核依据后保存，列表变「待复核 / ADVISORY / 2026-10-22 已逾期」 | 分类改变未关闭问题、未抹去逾期，未证明数据就绪 |
| 007-reclassification-v4-history-279 | 对象历史保留ISSUE_CREATE v1/START v2/SUBMIT v3（actor266）及RECLASSIFY v4（actor279），含依据、关联需求1844和wholeProject=true；v2–v4 wasOverdue=true，v4 state=REVIEW/category=ADVISORY | v4/actor279由007 AX支持，007 PNG首屏历史仅到v3，不声称截图可见v4。历史回读支持持久化及两个操作者的分工；快照中的time是系统保存值，不反推截图精确捕获时间或运行制品身份 |
| 008-advisory-precheck-baseline-block-removed | 266重新登录并回治理页面预检，仅仍显示强制决议覆盖缺失/冲突和本期纳入集合为空，原基线阻断问题提示消失 | 仍不允许完整发布；不能写成发布正例、全部阻断已清或T04整体关闭 |

本次核对保留007的证据层次更正：v4由AX支持，PNG首屏仅到v3。executor重新查看原件后确认此前把003状态误套到004并撤回差异结论；主线程及文档编写者亦确认004 AX/PNG相符；当前004 PNG确为待复核，原件未覆盖，SHA256为`59757942ebe2c8786f29a9eea804a069f760a625b86515f68c20c8c01901a65b`。007 PNG SHA256为`f95ce95881ae5c922d38fd15d4bf3b7b858e7e24fe54710409a76d8bd9c7f7ea`。

首次创建尝试和switch控制有额外操作限制：主线程报告首次未关联需求时服务拒绝且输入保留，随后选择1844再创建；该次拒绝未保存独立失败截图/AX/响应，不能冒称已有完整失败证据或精确服务code。主线程报告native AX switch click最初未生效，随后依据截图坐标启用；最终wholeProject=true由007的持久化创建快照支持。001–008没有封存该switch第一次点击失败的前后完整原件，不把点击动作本身当状态成功证据。

007保存的事件值依次为2026-10-23T07:51:06.808、07:51:47.357、07:51:55.171、07:52:37.364。v1 wasOverdue=false是创建保存字段；001按当前日期已显示逾期，v2起wasOverdue=true，二者不能混为同一字段的矛盾或隐去差异。

本轮可支持的子集为：隔离TEST问题的页面逾期、BASELINE_BLOCK预检提示、处理/提交、独立复核缺依据前端拒绝及补齐后ADVISORY分类、历史保留、重新登录后的分类阻断提示消失。尚未证明本轮完整发布正例、来源/数据可用、DATA_READINESS各分支、全部版本竞争、全T04关闭或正式业务交付。自动化确定性时钟/并发及服务权限证据须另由executor与本轮页面证据绑定，不由本记录补填。

本轮仅新增本内部记录、T09独立scope-note并更正本轮新增T08 handoff的启动参数文字；未改历史计划或最终fix报告，未提交。记录本身不确认运行服务对应哪一源码/包体；候选及制品身份应由主线程统一证据另行绑定。
