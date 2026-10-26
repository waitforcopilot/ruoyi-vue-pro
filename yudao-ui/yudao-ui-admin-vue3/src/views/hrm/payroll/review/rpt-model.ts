// TEST report discussion only; no payroll/file delivery API.
import { demo, result, demoAllocation, demoMetrics, demoCostStructure } from './demo'
export type RptDimension = 'DEPARTMENT' | 'COST_CENTER' | 'PROJECT'
export interface RptAllocation {
  key: string
  label: string
  percent: number
}
export const rptDimensions: Record<RptDimension, { label: string; rows: RptAllocation[] }> = {
  DEPARTMENT: {
    label: '部门',
    rows: [{ key: 'TEST-DEPT-001', label: demo.department, percent: 100 }]
  },
  COST_CENTER: {
    label: '成本中心',
    rows: [{ key: 'TEST-CC-001', label: 'TEST研发成本中心', percent: 100 }]
  },
  PROJECT: {
    label: '项目',
    rows: [
      { key: 'TEST-PROJECT-A', label: 'TEST项目甲', percent: 70 },
      { key: 'TEST-PROJECT-B', label: 'TEST项目乙', percent: 30 }
    ]
  }
}
const round = (n: number) => Math.round((n + Number.EPSILON) * 100) / 100
export interface RptAllocatedRow extends RptAllocation {
  employee: string
  period: string
  cost: number
}
export interface RptAllocationResult {
  valid: boolean
  total: number | null
  rows: RptAllocatedRow[]
  reason: string
}
export function allocateReport(rows: readonly RptAllocation[]): RptAllocationResult {
  const invalid =
    rows.length === 0 ||
    rows.some(
      (r) => !r.key.trim() || !Number.isFinite(r.percent) || r.percent <= 0 || r.percent > 100
    )
  const duplicate = new Set(rows.map((r) => r.key)).size !== rows.length
  const totalPercent = rows.reduce((sum, r) => sum + r.percent, 0)
  if (invalid || duplicate || Math.abs(totalPercent - 100) > 0.000001)
    return {
      valid: false,
      total: null,
      rows: [],
      reason: duplicate
        ? '重复分配键：已阻断，不能重复分摊'
        : '分配键/比例无效或合计不等100%：已阻断'
    }
  const allocated = rows.map((r, index) => ({
    ...r,
    employee: demo.employee,
    period: demo.period,
    cost:
      index === rows.length - 1
        ? round(
            result.cost -
              rows
                .slice(0, index)
                .reduce((sum, prior) => sum + round((result.cost * prior.percent) / 100), 0)
          )
        : round((result.cost * r.percent) / 100)
  }))
  return {
    valid: true,
    total: round(allocated.reduce((sum, r) => sum + r.cost, 0)),
    rows: allocated,
    reason: '同一TEST员工按当前单一维度分摊；跨维度是替代视图，不叠加成本或人数'
  }
}
export function reportMetrics(headcount: number | null, revenue: number | null) {
  const positive = (value: number | null): value is number =>
    value !== null && Number.isFinite(value) && value > 0
  return {
    average:
      positive(headcount) && Number.isInteger(headcount) ? round(result.cost / headcount) : null,
    costRate: positive(revenue) ? round((result.cost / revenue) * 100) : null
  }
}
export interface ReportBudget {
  period: string
  subject: string
  version: string | null
  amount: number | null
}
export const rptBudget: ReportBudget = {
  period: demo.period,
  subject: 'TEST-SUBJECT-001',
  version: 'TEST-BUDGET-V1',
  amount: demo.budget
}
export function compareReportBudget(budget: ReportBudget) {
  const comparable =
    budget.period === demo.period &&
    budget.subject === 'TEST-SUBJECT-001' &&
    budget.version === rptBudget.version &&
    budget.amount !== null &&
    Number.isFinite(budget.amount) &&
    budget.amount >= 0
  return {
    comparable,
    delta: comparable ? round(result.cost - budget.amount!) : null,
    reason: comparable
      ? '同主体/期间及TEST-BUDGET-V1；差额＝成本－预算'
      : '缺批准版本/金额或主体、期间、预算版本不匹配：不可比，差额未知'
  }
}
export const rptReferences = {
  allocation: demoAllocation,
  metrics: demoMetrics,
  structure: demoCostStructure
}
