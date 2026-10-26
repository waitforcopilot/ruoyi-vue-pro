// One shared TEST flow for the toolbar, scenario panel and slip. No production APIs.
import { createDemoState, demoContextKey, type DemoContext } from './demo-state'
import { demo, result } from './demo'
export type CalcStage = 'READY' | 'CALCULATED' | 'REVIEWED' | 'PAYMENT' | 'ARCHIVED'
export interface CalcReturn {
  employee: string
  period: string
  amount: number
  status: 'SUCCESS' | 'FAILED'
  reason: string
}
export interface CalcState {
  context: DemoContext
  stage: CalcStage
  notice: string
  prepared: boolean
  missing: boolean
  adjustment: number
  calculatedAdjustment: number | null
  revision: number
  reviewedRevision: number | null
  frozen: boolean
  returns: CalcReturn[]
}
export type CalcAction =
  | 'PREPARE'
  | 'CALCULATE'
  | 'REVIEW'
  | 'REJECT'
  | 'PAYMENT'
  | 'RETURN_PARTIAL'
  | 'RETURN_INVALID'
  | 'ARCHIVE'
  | 'FREEZE'
  | 'UNFREEZE'
  | 'SET_MISSING'
  | 'SET_ADJUSTMENT'
export const calcStageLabels: Record<CalcStage, string> = {
  READY: '准备',
  CALCULATED: '试算完成',
  REVIEWED: '复核完成',
  PAYMENT: '发放讨论',
  ARCHIVED: '归档讨论（非业务结清）'
}
export function createCalcState(context: DemoContext): CalcState {
  return {
    ...createDemoState(context),
    prepared: false,
    missing: false,
    adjustment: 0,
    calculatedAdjustment: null,
    revision: 0,
    reviewedRevision: null,
    frozen: false,
    returns: []
  }
}
export const inCalcContext = (state: CalcState, context: DemoContext) =>
  demoContextKey(state.context) === demoContextKey(context) ? state : createCalcState(context)
const round = (n: number) => Math.round((n + Number.EPSILON) * 100) / 100
export function calcAmounts(adjustment: number) {
  return {
    gross: round(result.gross + adjustment),
    personal: result.personal,
    tax: round(result.tax + adjustment * demo.demoRate),
    net: round(result.net + adjustment * (1 - demo.demoRate)),
    cost: round(result.cost + adjustment)
  }
}
export function calcPaymentRows(state: CalcState) {
  const amounts = calcAmounts(state.calculatedAdjustment ?? 0)
  return [
    {
      employee: 'TEST-EMP-001',
      period: state.context.period,
      amount: amounts.net,
      account: 'TEST-ACCOUNT-001'
    },
    {
      employee: 'TEST-EMP-002',
      period: state.context.period,
      amount: 5000,
      account: 'TEST-ACCOUNT-002'
    }
  ]
}
export function validateCalcReturns(state: CalcState, rows: CalcReturn[]) {
  const expected = calcPaymentRows(state)
  return (
    rows.length === expected.length &&
    new Set(rows.map((r) => r.employee)).size === rows.length &&
    rows.every((row) =>
      expected.some(
        (e) => e.employee === row.employee && e.period === row.period && e.amount === row.amount
      )
    )
  )
}
export function calcBatchLabel(state: CalcState) {
  if (state.returns.length === 0) return '未载入有效TEST回盘，不能推定已发放'
  const success = state.returns.filter((r) => r.status === 'SUCCESS').length
  return success === state.returns.length
    ? 'TEST全成功（非真实付款）'
    : success === 0
      ? 'TEST全失败，未结清'
      : 'TEST部分成功：1成功/1失败，未结清'
}
export function transitionCalc(
  state: CalcState,
  context: DemoContext,
  action: CalcAction,
  value?: number | boolean | null
): CalcState {
  const current = inCalcContext(state, context)
  const block = (notice: string) => ({ ...current, notice })
  if (!context.projectId || context.module !== 'CALC')
    return block('请在已选项目的工资核算视图体验TEST流程。')
  if (action === 'UNFREEZE')
    return current.frozen
      ? {
          ...current,
          frozen: false,
          stage: 'CALCULATED',
          reviewedRevision: null,
          returns: [],
          notice: 'TEST已解冻；原复核及回盘失效，需重新复核。'
        }
      : block('当前未冻结。')
  if (current.frozen)
    return block('已拦截：TEST快照冻结，不可改输入、重算或推进；先解冻，原复核将失效。')
  if (action === 'SET_MISSING' || action === 'SET_ADJUSTMENT') {
    if (
      action === 'SET_ADJUSTMENT' &&
      (context.scenario !== 'ADJUSTMENT' ||
        typeof value !== 'number' ||
        !Number.isFinite(value) ||
        Math.abs(value) > 1000)
    )
      return block('已拦截：仅独立调整候选允许-1000～1000元TEST调整，主链不改。')
    return {
      ...current,
      missing: action === 'SET_MISSING' ? Boolean(value) : current.missing,
      adjustment: action === 'SET_ADJUSTMENT' ? (value as number) : current.adjustment,
      stage: 'READY',
      prepared: false,
      calculatedAdjustment: null,
      reviewedRevision: null,
      returns: [],
      notice: '输入已变化：原试算、复核与回盘失效，请重新核对准备。'
    }
  }
  if (action === 'PREPARE')
    return current.missing
      ? block('准备阻断：DS-02考勤认可记录缺失，责任TEST-HR-001；未知不套0。')
      : {
          ...current,
          prepared: true,
          notice: 'TEST准备通过：ATT152＋带薪假8＝160、OT8/750、个人1400/单位2100齐备。'
        }
  if (action === 'CALCULATE') {
    if (!current.prepared || current.missing)
      return block('请先核对TEST准备；缺来源或未知输入不能试算。')
    return {
      ...current,
      stage: 'CALCULATED',
      calculatedAdjustment: current.adjustment,
      revision: current.revision + 1,
      reviewedRevision: null,
      returns: [],
      notice: 'TEST试算完成；本次重算使此前复核、发放讨论与回盘失效。'
    }
  }
  if (action === 'REVIEW')
    return current.stage === 'CALCULATED'
      ? {
          ...current,
          stage: 'REVIEWED',
          reviewedRevision: current.revision,
          notice: 'TEST本版本复核完成；未提交真实审批。'
        }
      : block('请先完成当前上下文试算；已复核需先重算。')
  if (action === 'REJECT')
    return ['CALCULATED', 'REVIEWED'].includes(current.stage)
      ? {
          ...current,
          stage: 'READY',
          prepared: false,
          calculatedAdjustment: null,
          reviewedRevision: null,
          returns: [],
          notice: 'TEST复核驳回：金额依据待补；原试算失效，准备后重算。'
        }
      : block('没有可驳回的试算版本。')
  if (action === 'FREEZE')
    return ['CALCULATED', 'REVIEWED'].includes(current.stage)
      ? {
          ...current,
          frozen: true,
          notice: 'TEST当前试算快照已冻结；不可修改或重算，解冻将清除复核。'
        }
      : block('请先试算再冻结快照。')
  if (action === 'PAYMENT')
    return current.stage === 'REVIEWED' && current.reviewedRevision === current.revision
      ? { ...current, stage: 'PAYMENT', notice: '进入TEST发放讨论，未生成付款文件或发起付款。' }
      : block('当前版本尚未复核，不能进入发放讨论。')
  if (action === 'RETURN_PARTIAL' || action === 'RETURN_INVALID') {
    if (current.stage !== 'PAYMENT') return block('请先进入TEST发放讨论，再载入虚构回盘。')
    const rows: CalcReturn[] = calcPaymentRows(current).map((row, i) => ({
      employee: row.employee,
      period: row.period,
      amount: row.amount,
      status: i === 0 ? 'SUCCESS' : 'FAILED',
      reason: i === 0 ? 'TEST-BANK-OK' : 'TEST-BANK-FAIL：虚构账号校验失败，待处理'
    }))
    if (action === 'RETURN_INVALID')
      rows[0] = { ...rows[0], employee: 'TEST-WRONG-EMP', amount: rows[0].amount + 1 }
    if (!validateCalcReturns(current, rows))
      return block('已拦截：TEST回盘人员/期间/金额不匹配；未替换有效回盘，不能据此标付款。')
    return {
      ...current,
      returns: rows,
      notice: 'TEST一成一败回盘已匹配人员/期间/金额；批次部分成功，失败未结清。'
    }
  }
  if (action === 'ARCHIVE')
    return current.stage === 'PAYMENT' && current.returns.length > 0
      ? {
          ...current,
          stage: 'ARCHIVED',
          notice: '已记录TEST归档讨论；保留失败项待处理，批次仍部分成功，非正式归档或结清。'
        }
      : block('请先完成TEST发放讨论并载入有效回盘，再记录归档讨论。')
  return current
}
