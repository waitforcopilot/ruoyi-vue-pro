// Fictional discussion constants, never tax advice or a filing/delivery implementation.
import { demo, result } from './demo'
export type TaxScopeKey = 'VALID' | 'WRONG_YEAR' | 'WRONG_SUBJECT' | 'MISSING_DEDUCTION'
export type TaxVersion = 'V1' | 'V2'
export interface TaxInputs {
  year: number
  subject: string
  priorIncome: number | null
  currentIncome: number | null
  priorPersonal: number | null
  currentPersonal: number | null
  allowance: number | null
  otherDeduction: number | null
  priorTax: number | null
  basis: string | null
}
export const taxScopes: Record<TaxScopeKey, string> = {
  VALID: '2026 / TEST主体匹配',
  WRONG_YEAR: '年度不匹配：2025',
  WRONG_SUBJECT: '主体不匹配：TEST-SUBJECT-OTHER',
  MISSING_DEDUCTION: '缺其他扣除来源'
}
export function taxInputs(scope: TaxScopeKey, version: TaxVersion): TaxInputs {
  return {
    year: scope === 'WRONG_YEAR' ? 2025 : 2026,
    subject: scope === 'WRONG_SUBJECT' ? 'TEST-SUBJECT-OTHER' : 'TEST-SUBJECT-001',
    priorIncome: demo.priorIncome,
    currentIncome: result.gross,
    priorPersonal: demo.priorInsurance,
    currentPersonal: result.personal,
    allowance: demo.cumulativeAllowance,
    otherDeduction:
      scope === 'MISSING_DEDUCTION' ? null : version === 'V2' ? 11000 : demo.cumulativeSpecial,
    priorTax: demo.priorTax,
    basis:
      version === 'V2'
        ? 'TEST-DEDUCT-CORRECTION-001：独立更正候选，累计其他扣除10000→11000；未回写主链'
        : 'TEST-TAX-V1：2026年TEST-SUBJECT-001虚构累计规则，线性3%仅讨论参数'
  }
}
const round = (n: number) => Math.round((n + Number.EPSILON) * 100) / 100
export function resolveTax(input: TaxInputs) {
  const known = (n: number | null): n is number => n !== null && Number.isFinite(n) && n >= 0
  const values = [
    input.priorIncome,
    input.currentIncome,
    input.priorPersonal,
    input.currentPersonal,
    input.allowance,
    input.otherDeduction,
    input.priorTax
  ]
  if (
    input.year !== 2026 ||
    input.subject !== 'TEST-SUBJECT-001' ||
    !input.basis ||
    !values.every(known)
  )
    return {
      income: null,
      personal: null,
      taxable: null,
      cumulativeTax: null,
      tax: null,
      net: null,
      reason: '年度/主体不匹配或扣除来源缺失：未知，不套0、不生成申报或投递金额。'
    }
  // Validation above preserves unknown. These local values are used only after all source checks.
  const income = round(input.priorIncome! + input.currentIncome!)
  const personal = round(input.priorPersonal! + input.currentPersonal!)
  const taxable = round(income - personal - input.allowance! - input.otherDeduction!)
  const cumulativeTax = round(Math.max(0, taxable) * demo.demoRate)
  const tax = round(cumulativeTax - input.priorTax!)
  if (tax < 0)
    return {
      income,
      personal,
      taxable,
      cumulativeTax,
      tax: null,
      net: null,
      reason: 'TEST累计税低于前期税：退款口径缺依据，本期税/净额未知，不自动退税。'
    }
  return {
    income,
    personal,
    taxable,
    cumulativeTax,
    tax,
    net: round(input.currentIncome! - input.currentPersonal! - tax),
    reason: 'TEST来源年度/主体匹配；累计所得×虚构3%－前期TEST税。非现行税法。'
  }
}
export type BonusChoice = 'UNSELECTED' | 'A' | 'B'
export const bonusPlans = {
  A: {
    label: 'TEST方案A：独立奖金参数3%',
    rate: 0.03,
    basis: 'TEST-BONUS-A：仅将独立奖金12000乘虚构3%；需业务讨论选择，不是法定单独计税'
  },
  B: {
    label: 'TEST方案B：独立奖金参数5%',
    rate: 0.05,
    basis: 'TEST-BONUS-B：仅将独立奖金12000乘虚构5%；不是实际并入综合所得计算或税务建议'
  }
}
export function resolveBonus(choice: BonusChoice, basisConfirmed = true) {
  if (choice === 'UNSELECTED' || !basisConfirmed)
    return {
      amount: 12000,
      tax: null,
      net: null,
      basis: '尚未选择TEST方案及依据；税额未知，不能自动套用工资3%。'
    }
  const plan = bonusPlans[choice]
  return {
    amount: 12000,
    tax: round(12000 * plan.rate),
    net: round(12000 * (1 - plan.rate)),
    basis: plan.basis
  }
}
export type TaxDelivery = 'NOT_SENT' | 'FAILED' | 'WAITING_RETRY' | 'SUCCESS' | 'WITHDRAWN'
export interface TaxSlipState {
  withdrawn: boolean
  delivery: TaxDelivery
  attempts: number
  notice: string
}
export type TaxSlipAction = 'FAIL' | 'QUEUE_RETRY' | 'SUCCESS' | 'WITHDRAW'
export const taxDeliveryLabels: Record<TaxDelivery, string> = {
  NOT_SENT: '未投递（仅TEST页面）',
  FAILED: 'TEST失败：虚构渠道不可达',
  WAITING_RETRY: 'TEST待重试（无后台任务）',
  SUCCESS: 'TEST投递成功（模拟，无实际发送）',
  WITHDRAWN: 'TEST已撤回，不可投递'
}
export const createTaxSlip = (): TaxSlipState => ({
  withdrawn: false,
  delivery: 'NOT_SENT',
  attempts: 0,
  notice: '本地候选工资条，未生成真实工资条或调用渠道。'
})
export function transitionTaxSlip(
  state: TaxSlipState,
  validBasis: boolean,
  action: TaxSlipAction
): TaxSlipState {
  const block = (notice: string) => ({ ...state, notice })
  if (state.withdrawn)
    return block('已拦截：当前TEST版本已撤回，不可投递或重试；切换版本重新讨论。')
  if (action === 'WITHDRAW')
    return {
      ...state,
      withdrawn: true,
      delivery: 'WITHDRAWN',
      notice: 'TEST候选版本已撤回；金额保留作前后对照，无真实撤回请求。'
    }
  if (!validBasis) return block('已拦截：年度/主体或扣除来源不匹配，候选税额未知，不模拟投递。')
  if (action === 'QUEUE_RETRY')
    return state.delivery === 'FAILED'
      ? {
          ...state,
          delivery: 'WAITING_RETRY',
          notice: 'TEST失败标记为待重试；没有后台任务或外发。'
        }
      : block('仅TEST失败版本可标记待重试。')
  if (state.delivery !== 'NOT_SENT' && state.delivery !== 'WAITING_RETRY')
    return block(
      state.delivery === 'SUCCESS'
        ? '当前版本已模拟成功；重复操作不新增投递次数。'
        : '先将TEST失败标记待重试。'
    )
  return {
    ...state,
    attempts: state.attempts + 1,
    delivery: action === 'FAIL' ? 'FAILED' : 'SUCCESS',
    notice:
      action === 'FAIL'
        ? 'TEST虚构渠道失败；可标记待重试，未发送任何消息。'
        : 'TEST成功状态仅在页面模拟，未实际发送工资条、邮件或短信。'
  }
}
