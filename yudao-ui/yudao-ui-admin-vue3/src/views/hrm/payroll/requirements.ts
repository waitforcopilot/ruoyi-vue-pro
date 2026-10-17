export const payrollModules = {
  OV: '薪酬总览',
  PLAN: '薪酬方案',
  CALC: '工资核算',
  TAX: '个税与工资条',
  ATT: '考勤',
  OT: '加班',
  HOUR: '工时',
  INS: '社保公积金',
  RPT: '报表分析',
  COL: '需求征集'
} as const
export type PayrollModule = keyof typeof payrollModules
export interface PayrollDataSource {
  id: string
  name: string
  fields: readonly string[]
  owner: string
}
// Candidate dictionary from original PRD v0.1, section 5.1; no runtime readiness is implied.
export const payrollDataSources: readonly PayrollDataSource[] = [
  {
    id: 'DS-01',
    name: 'HR 员工与组织档案',
    fields: ['员工唯一编号', '主体', '部门', '入离职日期', '计薪资格'],
    owner: 'HR'
  },
  {
    id: 'DS-02',
    name: '考勤系统',
    fields: ['员工编号', '周期', '班次', '应出勤', '实出勤', '请假', '缺勤', '异常状态'],
    owner: 'HR / 考勤管理员'
  },
  {
    id: 'DS-03',
    name: 'OA 加班 / 请假审批',
    fields: ['单据编号', '员工', '起止时间', '审批状态', '认可时长', '补休状态'],
    owner: '业务主管 / OA 负责人'
  },
  {
    id: 'DS-04',
    name: '工时制度与排班系统',
    fields: ['员工制度类型', '适用有效期', '结算周期', '排班', '实际工时', '差异'],
    owner: '部门主管 / HR / IT'
  },
  {
    id: 'DS-05',
    name: '薪酬方案与员工薪资档案',
    fields: ['方案版本', '适用人员', '薪资项', '金额 / 单价', '有效期'],
    owner: 'HR / 财务'
  },
  {
    id: 'DS-06',
    name: '社保公积金政策与缴费台账',
    fields: ['地区', '项目', '有效期', '基数上下限', '个人 / 单位比例', '实际缴费额'],
    owner: 'HR / 财务'
  },
  {
    id: 'DS-07',
    name: '税务及扣除资料',
    fields: ['纳税主体', '年度', '本年累计应税收入', '已扣税额', '专项扣除'],
    owner: '财务'
  },
  {
    id: 'DS-08',
    name: '业务需求征集',
    fields: ['需求编号', '模块', '功能', '字段', '来源', '负责人', '优先级', '状态', '备注'],
    owner: '需求负责人'
  },
  {
    id: 'DS-09',
    name: '绩效考核与薪级标准',
    fields: ['绩效周期', '基数', '系数', '职级 / 薪档', '岗位系数', '补贴标准'],
    owner: 'HR / 业务主管'
  },
  {
    id: 'DS-10',
    name: '财务预算与成本数据',
    fields: ['部门 / 成本中心 / 项目', '营收', '预算', '编制人数', '分摊比例'],
    owner: '财务'
  },
  {
    id: 'DS-11',
    name: '员工自助与联系方式',
    fields: ['员工编号', '接收渠道', '邮箱 / 手机', '工资条可见字段', '投递结果'],
    owner: 'HR / IT'
  },
  {
    id: 'DS-12',
    name: '银行代发与回盘',
    fields: ['批次', '员工', '账户', '应付金额', '文件格式', '回盘状态', '失败原因'],
    owner: '财务 / IT'
  }
]
const moduleSources: Record<PayrollModule, readonly string[]> = {
  OV: ['DS-01', 'DS-02', 'DS-03', 'DS-04', 'DS-05', 'DS-06', 'DS-07', 'DS-10'],
  PLAN: ['DS-01', 'DS-05', 'DS-09'],
  CALC: ['DS-01', 'DS-02', 'DS-03', 'DS-04', 'DS-05', 'DS-06', 'DS-07', 'DS-12'],
  TAX: ['DS-01', 'DS-07', 'DS-11'],
  ATT: ['DS-01', 'DS-02', 'DS-03'],
  OT: ['DS-01', 'DS-02', 'DS-03', 'DS-04'],
  HOUR: ['DS-01', 'DS-02', 'DS-03', 'DS-04'],
  INS: ['DS-01', 'DS-06'],
  RPT: ['DS-01', 'DS-05', 'DS-06', 'DS-10'],
  COL: ['DS-08']
}
export function sourcesForModules(modules: readonly PayrollModule[]) {
  const ids = new Set(modules.flatMap((module) => moduleSources[module]))
  return payrollDataSources.filter((source) => ids.has(source.id))
}
export function payrollModule(value: unknown): PayrollModule | undefined {
  return typeof value === 'string' && Object.prototype.hasOwnProperty.call(payrollModules, value)
    ? (value as PayrollModule)
    : undefined
}
export const payrollModuleRoutes: Partial<Record<PayrollModule, string>> = {
  OV: '/hrm-payroll/overview',
  PLAN: '/hrm-payroll/formula',
  CALC: '/hrm-payroll/batch',
  TAX: '/hrm-payroll/tax-policy',
  ATT: '/hrm-payroll/attendance',
  OT: '/hrm-payroll/work',
  HOUR: '/hrm-payroll/work',
  INS: '/hrm-payroll/insurance-policy',
  RPT: '/hrm-payroll/analysis'
}
const businessRoutes = new Set([
  ...Object.values(payrollModuleRoutes),
  ...[
    'entity',
    'cost',
    'grade',
    'salary-proposal',
    'payment',
    'input',
    'input-issue',
    'insurance-bill',
    'insurance-adjustment',
    'wage-recovery'
  ].map((name) => '/hrm-payroll/' + name)
])
export function payrollReturnTarget(value: unknown): string | undefined {
  return typeof value === 'string' && businessRoutes.has(value.split(/[?#]/)[0]) ? value : undefined
}
