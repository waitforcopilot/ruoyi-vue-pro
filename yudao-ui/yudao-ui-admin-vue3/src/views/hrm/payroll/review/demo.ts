// Pure fictional review adapter. No production API or business mutation is imported.
import { payrollModules, type PayrollModule } from '../requirements'
export const reviewOrder = Object.keys(payrollModules) as PayrollModule[]
const money = (n: number) => Math.round((n + Number.EPSILON) * 100) / 100
export const demo = Object.freeze({
  employee: 'TEST-EMP-001 · 虚构员工甲',
  department: '虚构研发组',
  period: '2026-10',
  base: 10000,
  performance: 1000,
  scheduledHours: 160,
  attendedHours: 152,
  paidLeaveHours: 8,
  overtimeHours: 8,
  multiplier: 1.5,
  personalInsurance: 800,
  personalFund: 600,
  employerInsurance: 1500,
  employerFund: 600,
  priorIncome: 90000,
  priorInsurance: 12600,
  cumulativeAllowance: 50000,
  cumulativeSpecial: 10000,
  priorTax: 702,
  demoRate: 0.03,
  budget: 14000,
  revenue: 100000
})
export function calculateDemo() {
  const hourly = money(demo.base / demo.scheduledHours)
  const overtime = money(hourly * demo.overtimeHours * demo.multiplier)
  const gross = money(demo.base + demo.performance + overtime)
  const personal = demo.personalInsurance + demo.personalFund
  const cumulativeTaxable = money(
    demo.priorIncome +
      gross -
      demo.priorInsurance -
      personal -
      demo.cumulativeAllowance -
      demo.cumulativeSpecial
  )
  const cumulativeTax = money(cumulativeTaxable * demo.demoRate)
  const tax = money(cumulativeTax - demo.priorTax)
  const net = money(gross - personal - tax)
  const employer = demo.employerInsurance + demo.employerFund
  return Object.freeze({
    hourly,
    overtime,
    gross,
    personal,
    cumulativeTaxable,
    cumulativeTax,
    tax,
    net,
    employer,
    cost: money(gross + employer)
  })
}
export const result = calculateDemo()
export interface ReviewView {
  purpose: string
  field: string
  fieldLabel: string
  columns: string[]
  rows: (string | number)[][]
  notes: string[]
  related: PayrollModule[]
}
const employee = demo.employee
export const reviewViews: Record<PayrollModule, ReviewView> = {
  OV: {
    purpose: '讨论期间、人员、成本口径和准备进度，真实征集统计在下方独立显示。',
    field: 'cost',
    fieldLabel: '单位用工成本',
    columns: ['虚构期间', '人员', '应发(元)', '个人扣款(元)', '税(元)', '实发(元)', '单位成本(元)'],
    rows: [
      [demo.period, employee, result.gross, result.personal, result.tax, result.net, result.cost]
    ],
    notes: [
      '单位成本＝应发11750＋单位缴费2100＝13850；与实发区分。',
      '准备进度来自实际来源核验；上方虚构金额不计入评审统计。'
    ],
    related: ['CALC', 'RPT']
  },
  PLAN: {
    purpose: '讨论工资结构、适用人员、生效时间和追溯审批。',
    field: 'base',
    fieldLabel: '基本工资',
    columns: ['项目', '规则/单位', '金额', '示例生效', '变更处理'],
    rows: [
      ['基本工资', '固定元/月', 10000, '2026-10-01', '月中变化待确认'],
      ['绩效', '演示固定额，系数待评审', 1000, '2026-10-01', '真实规则不得默认'],
      ['加班费', '62.50元/小时×8小时×1.5', 750, '2026-10', '工资/调休二选一']
    ],
    notes: [
      '示例方案V1；V2可演示基本工资调整但不保存到正式薪酬方案。',
      '薪级/薪档示例：TEST-A/TEST-01；审批人及追溯规则通过流程入口征集。'
    ],
    related: ['HOUR', 'CALC']
  },
  CALC: {
    purpose: '讨论准备→试算→复核→发放→归档的输入、例外和冻结边界。',
    field: 'gross',
    fieldLabel: '应发工资',
    columns: ['员工', '基本', '绩效', '加班', '应发', '个人缴费', '个税', '实发'],
    rows: [[employee, 10000, 1000, 750, result.gross, result.personal, result.tax, result.net]],
    notes: [
      '步骤1准备：考勤152小时＋带薪假8小时＝160小时；无缺勤扣款。',
      '步骤2试算：10000＋1000＋750＝11750；步骤3演示复核只改变本页面示例状态。',
      '部分回盘失败示例：TEST-BANK-FAIL不产生付款事实；正式付款文件和审批接口均不调用。'
    ],
    related: ['ATT', 'OT', 'INS', 'TAX']
  },
  TAX: {
    purpose: '讨论累计口径、扣除来源、工资条可见字段、撤回更正及投递。',
    field: 'tax',
    fieldLabel: '本期税额',
    columns: [
      '员工',
      '累计收入',
      '累计个人缴费',
      '累计基础扣除',
      '累计其他扣除',
      '累计应纳税额',
      '前期税',
      '本期税',
      '工资条实发'
    ],
    rows: [
      [employee, 101750, 14000, 50000, 10000, result.cumulativeTax, 702, result.tax, result.net]
    ],
    notes: [
      '虚构税演算：累计应纳税所得27750×演示税率3%＝832.50；减前期702＝130.50。',
      '演示常量仅用于讨论，不能视为法定税率/已批准规则或真实税务申报。',
      '工资条：应发11750－个人缴费1400－税130.50＝10219.50；渠道仅页面预览，邮件/短信均不发送。',
      '年终奖示例单独分支暂不参与本月金额，计税选择和申报格式在需求中确认。'
    ],
    related: ['CALC', 'INS']
  },
  ATT: {
    purpose: '讨论打卡、请假、跨期归集、异常补正及工资影响。',
    field: 'hours',
    fieldLabel: '出勤时长',
    columns: ['员工', '应计小时', '打卡小时', '已批准带薪假小时', '缺勤小时', '状态'],
    rows: [
      [employee, 160, 152, 8, 0, '正常：152＋8＝160'],
      ['TEST-异常分支', '待核对', '缺卡', '单据待补', '未知', '未归零、未计薪']
    ],
    notes: [
      '演示考勤周期2026-10；截止点及节假日来源待业务评审。',
      '缺卡分支显示未知，不能默认0；补卡后需重新归集。'
    ],
    related: ['HOUR', 'OT', 'CALC']
  },
  OT: {
    purpose: '讨论申请、打卡、认可时长、倍率、工资/调休及重复计薪拦截。',
    field: 'hours',
    fieldLabel: '加班时长',
    columns: [
      '员工',
      '申请小时',
      '打卡小时',
      '认可小时',
      '倍率',
      '单价元/小时',
      '工资金额',
      '处理'
    ],
    rows: [
      [employee, 8, 9, 8, 1.5, 62.5, 750, '工资；不重复调休'],
      ['TEST-跨期例外', 2, '待补', '未确认', '待评审', '待评审', '未知', '先核对，未计入主样例']
    ],
    notes: [
      '工资单位：10000元÷160小时＝62.50元/小时；认可8×1.5×62.5＝750。',
      '倍率是虚构讨论参数，不作为法定或已批准规则；上限/节假日/跨日处理另征集。'
    ],
    related: ['ATT', 'HOUR', 'CALC']
  },
  HOUR: {
    purpose: '讨论标准/综合/不定时制度、适用依据、排班和周期差异。',
    field: 'hours',
    fieldLabel: '认可工时',
    columns: ['员工/分支', '制度', '周期应计', '出勤＋带薪假', '认可加班', '规则有效期'],
    rows: [
      [employee, 'TEST标准示例', 160, '152＋8', 8, '2026-10'],
      ['TEST综合分支', '周期结算', '待配置', '待汇总', '待核验', '依据待补'],
      ['TEST不定时分支', '适用人员审核', '不套标准分母', '记录实际', '不能自动认定', '批准依据待补']
    ],
    notes: [
      '主样例工时与考勤、加班一致；加班未并入基本工资分母重复计算。',
      '周期不足/跨月/补休情形明确为待讨论，不用示例批准适用。'
    ],
    related: ['ATT', 'OT', 'PLAN']
  },
  INS: {
    purpose: '讨论主体/城市、有效政策、基数比例及应缴/实缴对账。',
    field: 'deduction',
    fieldLabel: '个人缴费扣款',
    columns: ['城市/项目', '基数元', '个人比例', '个人元', '单位比例', '单位元', '应缴/实缴'],
    rows: [
      ['TEST城-社保', 10000, '演示8%', 800, '演示15%', 1500, '2300/待对账'],
      ['TEST城-公积金', 10000, '演示6%', 600, '演示6%', 600, '1200/待对账']
    ],
    notes: [
      '个人合计1400进入工资扣款；单位合计2100进入用工成本。',
      '基数上下限示例5000/30000，非真实城市政策；入离职/调基/退款和政策依据待评审。'
    ],
    related: ['CALC', 'TAX', 'RPT']
  },
  RPT: {
    purpose: '讨论成本分摊、结构、人均、预算与收入分母及导出列。',
    field: 'cost',
    fieldLabel: '分摊成本',
    columns: ['维度', '比例', '成本元', '分母/预算', '核对'],
    rows: [
      ['TEST项目甲', '70%', 9695, '成本总额13850', '与项目乙合计13850'],
      ['TEST项目乙', '30%', 4155, '成本总额13850', '无重复分摊'],
      ['TEST预算', '100%', 13850, 14000, '差额-150'],
      ['TEST人均/营收比', '1人/100000收入', 13850, '13.85%', '分母为空时暂无适用项']
    ],
    notes: [
      '总成本13850与总览一致，不用实发10219.50代替成本。',
      '导出预览仅讨论列定义；独立治理页提供TEST决议、受控发布及六表快照导出入口，示例不代表正式验收。'
    ],
    related: ['OV', 'INS', 'COL']
  },
  COL: {
    purpose: '集中评审功能、来源、规则、责任、范围决策、问题、实施和版本，返回原意见位置。',
    field: 'status',
    fieldLabel: '需求评审状态',
    columns: ['维度', '含义', '依据'],
    rows: [
      ['功能确认', '本人确认当前语义/范围/来源映射版本', '真实保存/签字，非示例状态'],
      ['来源可用', '供给＋语义＋QS/QC＋问题', '真实S3元组推导'],
      ['实施状态', '未排期/开发中/待验收/验收通过', '独立人工记录，证据待核实'],
      [
        '发布状态',
        '独立治理页提供TEST决议、受控发布及六表快照入口',
        '示例及预检查不代表正式批准或验收'
      ]
    ],
    notes: [
      '候选/纳入/暂缓/移除与待确认/评审中/已确认/异议分开。',
      '所有意见保留项目、模块及稳定页面锚点；历史可比较，不覆盖原事实。'
    ],
    related: ['OV', 'CALC']
  }
}

// Each archived function has a concrete discussion scenario and its own stable anchor.
const topicScenarios: Record<PayrollModule, [string, string, string][]> = {
  OV: [
    [
      '汇总口径',
      '应发11750、实发10219.50、单位成本13850，期间和主体分别标识',
      '不可把实发当成本；无金额时显示未知'
    ],
    ['期间与历史', '2026-10主样例；8/9月趋势为独立虚构历史', '跨期更正另版本，不覆盖历史'],
    ['准备进度', '考勤/加班/工时/缴费进入来源语义及QS/QC核验', '功能确认与待供数可同时存在'],
    ['异常与提醒', '缺卡和未决质量问题分开确定责任及期限', '提示与站内待办不代表问题已关闭']
  ],
  PLAN: [
    [
      '工资结构',
      '固定10000＋绩效1000＋加班750，个人扣款另列',
      '浮动系数和正式规则未确认时不得默认批准'
    ],
    [
      '薪级薪档',
      'TEST-A/TEST-01对应当前基本10000，可讨论导入字段',
      '重复薪档、单位及生效期必须核对'
    ],
    ['定薪调薪', '当前10000；模拟下期10500，先讨论审批和追溯', '未生效模拟不改本期核算或工资档案'],
    ['方案版本', 'V1当前有效示例，V2下期候选；比较差额500', '候选版本不能覆盖当前已核对依据']
  ],
  CALC: [
    ['批次生命周期', '准备→试算→复核→发放讨论→归档讨论', '演示审批不调用真实审批/付款'],
    ['公式及调整', '10000＋1000＋750－1400－130.50＝10219.50', '缺关键输入标未知，不能自动归零'],
    ['异常拦截', '缺卡、来源缺口及质量失败分别反馈', '人工补正须留依据并重核验'],
    [
      '代发与回盘',
      'TEST-BANK-OK/TEST-BANK-FAIL展示成功与部分失败分支',
      '未确认回盘不能标全批已发放'
    ],
    ['工资条生成', '同一TEST员工、期间、应发与实发预览一致', '演示生成不创建真实工资条或发送通知']
  ],
  TAX: [
    [
      '累计税演算',
      '累计所得27750、演示税率3%、累计税832.50，前期702，本期130.50',
      '仅虚构规则，不作为真实税务政策或申报'
    ],
    [
      '扣除来源',
      '前期个人缴费12600＋本期1400；基础50000，其他10000',
      '来源/主体/年度不匹配须重核对'
    ],
    [
      '工资条模板投递',
      '应发、扣款、个税、实发分别展示；仅本页预览',
      '撤回/更正及可见字段另评审，短信邮件不外发'
    ],
    ['税务导出', '讨论主体、年度、累计列、格式与申报/完税关联', '当前评审导出不是税务申报文件'],
    [
      '年终奖分支',
      '单独讨论奖金金额、计税方式及选择依据，不并入10月主链',
      '未确认方案不能自动套用演示税率'
    ]
  ],
  ATT: [
    ['日月归集', '本周期排班160，打卡152，批准带薪假8', '缺卡不等于缺勤，截止和跨期规则待核定'],
    ['单据关联', 'TEST员工的请假8小时与月汇总关联', '出差、外出、补卡单未批准时不得覆盖原记录'],
    [
      '异常判定',
      '正常152＋8＝160；迟到/早退/缺卡另列异常分支',
      '阈值、旷工定义和例外依据不能用示例批准'
    ],
    ['异常闭环', '核对责任人补证，独立复核后关闭', '关闭工作台问题不自动确认需求或核验来源'],
    ['工资联动', '主样例无缺勤扣款；异常结果不得直接入核算', '补正后重新归集，防止重复扣款']
  ],
  OT: [
    ['申请与审批', '申请8、打卡9、认可8，三字段独立', '驳回和未批准时长不能自动计薪'],
    ['统计与倍率', '10000÷160＝62.50；认可8×演示倍率1.5＝750', '跨日/假期/跨期倍率和证据待核定'],
    [
      '加班进入核算',
      '750只进入应发一次；同一TEST员工实发10219.50',
      '来源重复、人工补差和重算须核对去重'
    ],
    ['调休补休', '本例选择加班工资，不同时产生8小时调休', '调休余额、补休截止和转工资规则另议'],
    ['上限提醒', '演示超限情景只提示核对依据和责任人', '不引用未经核定的法定上限或自动批准']
  ],
  HOUR: [
    [
      '制度适用',
      'TEST标准主链，综合/不定时列独立讨论分支',
      '员工适用、有效期及批准依据缺失不能推定'
    ],
    ['周期结算', '主链160小时，跨月周期需单独冻结边界', '周期不足和不定时制度不套标准分母'],
    ['报表与异常', '应计160/出勤152/带薪假8/认可加班8分别列示', '缺卡或跨期数据保持待核验'],
    ['工资联动', '加班8小时计750，基础分母仍160', '考勤扣款、补休和加班不能重复调整'],
    ['排班核对', '虚构排班20班×8＝160；19班出勤＋1班带薪假', '本排班不是自然工作日或法律口径']
  ],
  INS: [
    ['基数调基', 'TEST城基数10000；上下限示例5000/30000', '真实年度政策、补缴和入离职条件待核定'],
    ['比例及城市', '个人社保8%/单位15%、公积金各6%仅演示', '跨城市/主体/有效期变化须绑定政策依据'],
    ['个人扣款', '800＋600＝1400，扣入主样例一次', '已扣与实缴不一致必须列差额'],
    ['单位成本', '1500＋600＝2100；单位成本11750＋2100＝13850', '个人扣款不重复计为单位缴费'],
    ['缴费对账', '应缴社保2300、公积金1200；实缴待对账', '实缴未知不能显示已清偿；退款补缴另留依据']
  ],
  RPT: [
    ['成本分摊', '项目甲70%=9695，乙30%=4155，合计13850', '维度重复或分摊不等100%须拦截'],
    [
      '分母和成本率',
      '虚构1人、人均13850；营收100000，成本率13.85%',
      '空分母显示暂无适用项，不计算100%'
    ],
    ['结构与变动', '基本10000、绩效1000、加班750、单位缴费2100', '期间/结构变化先确定可比口径'],
    ['预算与实际', '预算14000、实际13850，差额-150', '预算版本及增减项须留依据'],
    [
      '导出列口径',
      '讨论主体/期间/维度/金额/规则与字段单位',
      '当前筛选清单与发布时固定的六表快照分别查看'
    ]
  ],
  COL: [
    [
      '范围与清单',
      '候选/纳入/暂缓/移除独立于功能评审和实施状态',
      '重复移除须同项目保留项，恢复要重审'
    ],
    ['评审与问题', '本人确认、分派资格、未决/逾期和来源缺口独立', '意见或会议记录不是正式业务决议'],
    [
      '输出与版本',
      '当前筛选清单按实时权限导出；原锚点和历史保留',
      '独立治理页提供TEST受控发布、固定快照及异步导出，操作成功不等于正式业务批准'
    ]
  ]
}
export const reviewTopics = Object.fromEntries(
  reviewOrder.map((module) => [
    module,
    topicScenarios[module].map(([title, normal, exception], i) => ({
      code: `${module}-${String(i + 1).padStart(2, '0')}`,
      title,
      normal,
      exception,
      anchor: `field-${module.toLowerCase()}-${String(i + 1).padStart(2, '0')}`
    }))
  ])
) as Record<
  PayrollModule,
  { code: string; title: string; normal: string; exception: string; anchor: string }[]
>
export const demoTrend = [
  { period: '2026-08', gross: 10000 },
  { period: '2026-09', gross: 11000 },
  { period: demo.period, gross: result.gross }
]
export const demoCostStructure = [
  { name: '基本工资', amount: demo.base },
  { name: '绩效', amount: demo.performance },
  { name: '加班', amount: result.overtime },
  { name: '单位缴费', amount: result.employer }
]
