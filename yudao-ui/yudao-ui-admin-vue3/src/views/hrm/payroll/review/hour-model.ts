// TEST discussion scenarios only; no approval, payroll, attendance or delivery API calls.
import { demo } from './demo'
export type HourRegime = 'STANDARD' | 'COMPREHENSIVE' | 'IRREGULAR'
export interface HourMonth {
  period: string
  scheduled: number | null
  attended: number | null
  paidLeave: number | null
  overtime: number | null
  closed: boolean
  schedule: string
}
export interface HourExample {
  regime: HourRegime
  label: string
  employee: string
  subject: string
  effectiveFrom: string
  effectiveTo: string
  basis: string | null
  expectedPeriods: readonly string[]
  months: readonly HourMonth[]
  explanation: string
}
const october: HourMonth = {
  period: demo.period,
  scheduled: demo.scheduledHours,
  attended: demo.attendedHours,
  paidLeave: demo.paidLeaveHours,
  overtime: demo.overtimeHours,
  closed: true,
  schedule: 'TEST排班20班×8小时；19班出勤＋1班带薪假。加班另列。'
}
const november: HourMonth = {
  period: '2026-11',
  scheduled: 160,
  attended: 160,
  paidLeave: 0,
  overtime: 2,
  closed: true,
  schedule: 'TEST排班20班×8小时；20班出勤。另有认可加班2小时。'
}
const baseScope = { employee: demo.employee, subject: 'TEST-SUBJECT-001 · 虚构主体甲' }
const comprehensive = {
  ...baseScope,
  regime: 'COMPREHENSIVE' as const,
  effectiveFrom: '2026-10-01',
  effectiveTo: '2026-11-30',
  basis: 'TEST-HOUR-COMP-001 · 虚构10–11月周期适用及复核依据，非真实批准',
  expectedPeriods: ['2026-10', '2026-11']
}
export const hourExamples = {
  STANDARD: {
    ...baseScope,
    regime: 'STANDARD',
    label: '标准：10月主样例',
    effectiveFrom: '2026-10-01',
    effectiveTo: '2026-10-31',
    basis: 'TEST-HOUR-STD-001 · 虚构标准制度适用依据，非真实批准',
    expectedPeriods: ['2026-10'],
    months: [october],
    explanation:
      '本期应计160；出勤152＋带薪假8＝认可基础160。加班8小时单列，不能加入基本工资分母或重复补偿。'
  },
  COMP_COMPLETE: {
    ...comprehensive,
    label: '综合：10–11月完整周期',
    months: [october, november],
    explanation:
      '独立TEST完整周期：应计320，出勤312＋带薪假8＝320；另列认可加班10。周期规则不是实际法律口径，不能改10月主链。'
  },
  COMP_OPEN: {
    ...comprehensive,
    label: '综合：跨月尚未结清',
    months: [
      october,
      {
        ...november,
        attended: 80,
        overtime: null,
        closed: false,
        schedule: '11月仅记录前10班80小时；尚未结清，剩余实际与加班待核验。'
      }
    ],
    explanation:
      '10月已结、11月尚未结；已知记录232小时出勤＋8小时假。周期认可、差异和加班合计未知，不能把剩余记录默认为0。'
  },
  COMP_SHORT: {
    ...comprehensive,
    label: '综合：周期不足（缺11月）',
    months: [october],
    explanation:
      '仅有10月记录，缺11月整月；已提供月的160小时不是完整周期总量。完整周期应计/实际/认可与加班合计保持未知。'
  },
  IRREGULAR_BASIS: {
    ...baseScope,
    regime: 'IRREGULAR',
    label: '不定时：有TEST适用依据',
    effectiveFrom: '2026-01-01',
    effectiveTo: '2026-12-31',
    basis: 'TEST-HOUR-IR-001 · 虚构人员/有效期适用依据，非真实批准',
    expectedPeriods: ['2026-10'],
    months: [
      {
        ...october,
        scheduled: null,
        attended: 144,
        paidLeave: 8,
        overtime: null,
        schedule: '记录虚构实际144小时及批准带薪假8小时；无标准排班分母。'
      }
    ],
    explanation:
      '适用依据存在仅说明讨论分支的制度适用；实际记录152小时不等于自动认可计薪工时。不套标准160分母，加班和补偿依据另行确认。'
  },
  IRREGULAR_MISSING: {
    ...baseScope,
    regime: 'IRREGULAR',
    label: '不定时：缺适用批准依据',
    effectiveFrom: '2026-01-01',
    effectiveTo: '2026-12-31',
    basis: null,
    expectedPeriods: ['2026-10'],
    months: [
      {
        ...october,
        scheduled: null,
        attended: 144,
        paidLeave: 8,
        overtime: null,
        schedule: '保留虚构实际144小时＋带薪假8小时；制度适用待核实。'
      }
    ],
    explanation:
      '缺批准依据，不推定适用、不套标准分母、不自动认定加班。原始实际记录保留，认可及工资影响均未知。'
  }
} satisfies Record<string, HourExample>
export type HourExampleKey = keyof typeof hourExamples
export const hourRegimes: Record<HourRegime, { label: string; options: HourExampleKey[] }> = {
  STANDARD: { label: '标准工时', options: ['STANDARD'] },
  COMPREHENSIVE: { label: '综合工时', options: ['COMP_COMPLETE', 'COMP_OPEN', 'COMP_SHORT'] },
  IRREGULAR: { label: '不定时工时', options: ['IRREGULAR_BASIS', 'IRREGULAR_MISSING'] }
}
export function sumKnownHours(values: readonly (number | null)[]): number | null {
  if (!values.length || values.some((x) => x === null || !Number.isFinite(x) || x < 0)) return null
  return Math.round(values.reduce<number>((sum, value) => sum + (value as number), 0) * 100) / 100
}
export function summarizeHours(example: HourExample) {
  const periods = example.months.map((month) => month.period)
  const completeCoverage =
    new Set(periods).size === periods.length &&
    periods.length === example.expectedPeriods.length &&
    example.expectedPeriods.every((period) => periods.includes(period))
  const closed = completeCoverage && example.months.every((month) => month.closed)
  const basisPresent = !!example.basis?.trim()
  const observed = {
    scheduled: sumKnownHours(example.months.map((month) => month.scheduled)),
    attended: sumKnownHours(example.months.map((month) => month.attended)),
    paidLeave: sumKnownHours(example.months.map((month) => month.paidLeave)),
    overtime: sumKnownHours(example.months.map((month) => month.overtime))
  }
  const recorded = sumKnownHours([observed.attended, observed.paidLeave])
  const scheduled = completeCoverage ? observed.scheduled : null
  // Irregular applicability alone never supplies a standard denominator or overtime rule.
  const canRecognize = basisPresent && closed && example.regime !== 'IRREGULAR'
  const regular = canRecognize ? recorded : null
  const overtime = canRecognize ? observed.overtime : null
  const difference =
    regular !== null && scheduled !== null ? Math.round((regular - scheduled) * 100) / 100 : null
  return {
    completeCoverage,
    closed,
    basisPresent,
    observed,
    recorded,
    scheduled,
    regular,
    overtime,
    difference,
    salaryDenominator:
      example.regime === 'STANDARD' && regular !== null && scheduled !== null && scheduled > 0
        ? scheduled
        : null,
    status: !basisPresent
      ? '缺适用批准依据：认可与工资影响未知'
      : !completeCoverage
        ? '周期不足：缺期数据保持未知'
        : !closed
          ? '跨月未结清：不生成最终认可结果'
          : example.regime === 'IRREGULAR'
            ? '仅有制度适用依据：计薪与加班规则待确认'
            : 'TEST记录已完整对账；非正式业务批准'
  }
}
export const hourLabel = (value: number | null) =>
  value === null ? '未知/不适用（不归零）' : `${value}小时`
