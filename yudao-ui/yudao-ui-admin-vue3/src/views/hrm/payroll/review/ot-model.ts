// TEST review scenarios only. Hours and money are separate; no approval/payroll API.
import { demo, result } from './demo'
export type OtDecision = 'PENDING' | 'APPROVED' | 'REJECTED'
export type OtMode = 'WAGE' | 'LEAVE'
export interface OtExample {
  label: string
  id: string
  requested: number
  punched: number | null
  recognized: number | null
  decision: OtDecision
  multiplier: number | null
  basis: string | null
  slices: { period: string; interval: string; hours: number }[]
}
const main: OtExample = {
  label: '批准：申请8 / 打卡9 / 认可8',
  id: 'TEST-OT-001',
  requested: 8,
  punched: 9,
  recognized: 8,
  decision: 'APPROVED',
  multiplier: demo.multiplier,
  basis: 'TEST-RULE-OT-V1：取申请、打卡与主管认可的最小值；工资倍率1.5',
  slices: [{ period: '2026-10', interval: '2026-10-12 18:00～次日02:00（TEST）', hours: 8 }]
}
export const otExamples = {
  APPROVED: main,
  PENDING: { ...main, label: '待批准：认可与工资未知', decision: 'PENDING', recognized: null },
  REJECTED: { ...main, label: '驳回：认可0，原始打卡9保留', decision: 'REJECTED', recognized: 0 },
  CROSS_DAY: {
    ...main,
    label: '跨日：两段合计8，不重复计整单',
    slices: [
      { period: '2026-10', interval: '2026-10-12 18:00～24:00（TEST）', hours: 6 },
      { period: '2026-10', interval: '2026-10-13 00:00～02:00（TEST）', hours: 2 }
    ]
  },
  CROSS_PERIOD: {
    ...main,
    label: '跨期：缺归属依据，金额未知',
    requested: 2,
    punched: null,
    recognized: null,
    basis: null,
    slices: [
      { period: '2026-10', interval: '2026-10-31 23:00～24:00（TEST）', hours: 1 },
      { period: '2026-11', interval: '2026-11-01 00:00～01:00（TEST）', hours: 1 }
    ]
  },
  HOLIDAY: {
    ...main,
    label: '假期：独立TEST倍率2，非日历/法律认定',
    multiplier: 2,
    basis: 'TEST-HOLIDAY-V1：假设此段为虚构假期，倍率2；不代表真实节假日规则'
  }
} satisfies Record<string, OtExample>
export type OtExampleKey = keyof typeof otExamples
export function resolveOt(example: OtExample) {
  const valid = (n: number | null): n is number => n !== null && Number.isFinite(n) && n >= 0
  let hours: number | null = null
  if (example.decision === 'REJECTED') hours = 0
  else if (
    example.decision === 'APPROVED' &&
    example.basis &&
    valid(example.requested) &&
    valid(example.punched) &&
    valid(example.recognized) &&
    example.slices.every((s) => valid(s.hours)) &&
    example.slices.reduce((n, s) => n + s.hours, 0) === example.recognized
  ) {
    hours = Math.min(example.requested, example.punched, example.recognized)
  }
  const wage: number | null =
    hours === 0
      ? 0
      : hours !== null && valid(example.multiplier)
        ? Math.round(hours * result.hourly * example.multiplier * 100) / 100
        : null
  // Fictional monthly warning: prior recognized30 + this order; cap36 is a discussion parameter.
  const monthly: number | null = hours === null ? null : 30 + hours
  return { hours, wage, monthly, overLimit: monthly !== null && monthly > 36 }
}
export interface OtLedger {
  orderId: string | null
  mode: OtMode | null
  wage: number
  granted: number
  used: number
  usageIds: string[]
}
export function emptyOtLedger(): OtLedger {
  return { orderId: null, mode: null, wage: 0, granted: 0, used: 0, usageIds: [] }
}
export function compensateOt(state: OtLedger, example: OtExample, mode: OtMode) {
  const resolved = resolveOt(example)
  if (state.orderId !== null)
    return {
      state,
      message:
        state.mode === mode
          ? '已拦截：同一加班单重复补偿，工资与调休余额均未增加。'
          : '已拦截：该单已选择另一补偿方式，不能工资与调休双计。'
    }
  if (resolved.hours === null || resolved.hours <= 0 || (mode === 'WAGE' && resolved.wage === null))
    return { state, message: '已拦截：未获有效认可或规则依据不足，不生成补偿。' }
  return {
    state: {
      ...state,
      orderId: example.id,
      mode,
      wage: mode === 'WAGE' ? resolved.wage! : 0,
      granted: mode === 'LEAVE' ? resolved.hours : 0
    },
    message: 'TEST补偿已登记；同单只登记一次，未写入正式工资。'
  }
}
export function useOtLeave(state: OtLedger, usageId: string, hours: number) {
  if (state.usageIds.includes(usageId))
    return { state, message: '已拦截：同一调休使用单重复提交，余额未重复扣减。' }
  if (
    state.mode !== 'LEAVE' ||
    !Number.isFinite(hours) ||
    hours <= 0 ||
    hours > state.granted - state.used
  )
    return { state, message: '已拦截：无调休余额或使用超过余额。' }
  return {
    state: { ...state, used: state.used + hours, usageIds: [...state.usageIds, usageId] },
    message: 'TEST使用单登记4小时；再次点击同一使用单将被拦截。'
  }
}
