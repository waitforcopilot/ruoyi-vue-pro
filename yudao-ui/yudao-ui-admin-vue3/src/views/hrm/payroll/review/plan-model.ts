// Fictional PLAN scenarios only. No API, storage or production approval side effects.
import { demo, result } from './demo'

export const planScope = Object.freeze({
  subject: 'TEST-SUBJECT-001 · 虚构主体甲',
  employee: demo.employee,
  gradeGroup: 'TEST-A',
  gradeCode: 'TEST-01',
  currentEffective: '2026-10-01',
  candidateEffective: '2026-11-01'
})
export const planAmountLimit = 1_000_000
export type AmountCheck = { valid: true; amount: number } | { valid: false; error: string }
export function checkPlanAmount(input: unknown): AmountCheck {
  if (typeof input !== 'string' && typeof input !== 'number') {
    return { valid: false, error: '金额未知，请输入候选基本工资。' }
  }
  const text = String(input).trim()
  if (!text) return { valid: false, error: '金额不能为空；未将空值当作0。' }
  if (!/^\d+(?:\.\d{1,2})?$/.test(text)) {
    return {
      valid: false,
      error: '金额须为非负十进制数，最多两位小数；不接受负数、非有限值或科学计数法。'
    }
  }
  const amount = Number(text)
  if (!Number.isFinite(amount) || amount > planAmountLimit) {
    return { valid: false, error: '候选金额须在0～1000000元之间（TEST输入上限，非业务政策）。' }
  }
  return { valid: true, amount }
}
const moneyDifference = (amount: number) =>
  (Math.round(amount * 100) - Math.round(demo.base * 100)) / 100
export const planScenarios = {
  NORMAL: {
    label: '正常：下期候选',
    effective: '2026-11-01',
    approval: 'TEST演示复核通过（非正式批准）',
    basis: '按完整月基本工资比较；仅候选，不改当前V1。',
    outcome: '可展示下期候选基本工资与差额；未发布、未写入档案。'
  },
  MID_MONTH: {
    label: '例外：月中生效',
    effective: '2026-10-16',
    approval: '折算依据待确认',
    basis: '计薪日/小时分母、分段日历和批准依据缺失。',
    outcome: '月中实际增减未知，保留V1；不能把完整月差额直接计入当期。'
  },
  RETRO: {
    label: '例外：追溯调整',
    effective: '2026-09-01',
    approval: '追溯依据待确认',
    basis: '历史方案、已发/冻结批次、追溯期间和独立批准依据待补。',
    outcome: '追溯补差未知，原历史记录保持不变；需补证后另版本核对。'
  },
  PENDING: {
    label: '例外：尚未批准',
    effective: '2026-11-01',
    approval: 'TEST待评审',
    basis: '候选提出人不能代替批准人；尚无批准依据。',
    outcome: '暂不应用候选；当前仍V1基本10000元。'
  },
  REJECTED: {
    label: '例外：已驳回',
    effective: '2026-11-01',
    approval: 'TEST演示驳回',
    basis: '驳回原因：适用人员及预算依据待补；需修改后重新评审。',
    outcome: '候选未生效；当前仍V1基本10000元，保留讨论意见。'
  }
} as const
export type PlanScenario = keyof typeof planScenarios
export function previewPlan(input: unknown, scenario: PlanScenario) {
  const checked = checkPlanAmount(input)
  const branch = planScenarios[scenario]
  return {
    ...branch,
    scenario,
    currentBase: demo.base,
    currentGross: result.gross,
    candidateBase: checked.valid === true ? checked.amount : null,
    fullMonthDifference: checked.valid === true ? moneyDifference(checked.amount) : null,
    applicableDifference:
      checked.valid === true && scenario === 'NORMAL' ? moneyDifference(checked.amount) : null,
    valid: checked.valid,
    error: checked.valid === true ? '' : checked.error
  }
}

export interface PlanImportRow {
  subject: string
  employee: string
  gradeGroup: string
  gradeCode: string
  amount: string
  unit: string
  validFrom: string
  validTo: string
}
const importRow = (): PlanImportRow => ({
  subject: 'TEST-SUBJECT-001',
  employee: 'TEST-EMP-001',
  gradeGroup: 'TEST-A',
  gradeCode: 'TEST-02',
  amount: '10500',
  unit: '元/月',
  validFrom: '2026-11-01',
  validTo: '2027-10-31'
})
export const planImportSamples = {
  VALID: { label: '有效样例', rows: [importRow()] },
  DUPLICATE: { label: '重复薪档样例', rows: [importRow(), { ...importRow(), amount: '10600' }] },
  AMOUNT: {
    label: '无效金额样例',
    rows: [
      { ...importRow(), amount: '' },
      { ...importRow(), gradeCode: 'TEST-03', amount: '10500.001' }
    ]
  },
  DATE: {
    label: '无效有效期样例',
    rows: [
      { ...importRow(), validFrom: '2026-02-30' },
      { ...importRow(), gradeCode: 'TEST-03', validFrom: '2027-11-01', validTo: '2026-11-01' }
    ]
  }
} as const
export type PlanImportSample = keyof typeof planImportSamples
function validDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const timestamp = Date.parse(`${value}T00:00:00Z`)
  return Number.isFinite(timestamp) && new Date(timestamp).toISOString().slice(0, 10) === value
}
export function validatePlanImport(rows: readonly PlanImportRow[]) {
  const keys = rows.map((row) => JSON.stringify([row.subject, row.gradeGroup, row.gradeCode]))
  return rows.map((row, index) => {
    const errors: string[] = []
    for (const [key, label] of [
      ['subject', '主体'],
      ['employee', '适用员工'],
      ['gradeGroup', '薪级'],
      ['gradeCode', '薪档']
    ] as const) {
      if (!row[key].trim()) errors.push(`缺少${label}`)
    }
    if (keys.filter((key) => key === keys[index]).length > 1)
      errors.push('同主体薪级/薪档重复；未覆盖或写入')
    const checked = checkPlanAmount(row.amount)
    if (checked.valid === false) errors.push(checked.error)
    if (row.unit !== '元/月') errors.push('单位须为元/月，其他单位须先确认转换规则')
    if (!validDate(row.validFrom) || !validDate(row.validTo))
      errors.push('有效期须为真实日期YYYY-MM-DD')
    else if (row.validFrom > row.validTo) errors.push('生效日不得晚于失效日')
    return { row: { ...row }, line: index + 1, valid: errors.length === 0, errors }
  })
}
