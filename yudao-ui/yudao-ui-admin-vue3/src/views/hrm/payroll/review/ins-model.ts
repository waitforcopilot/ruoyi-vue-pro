// Fictional INS review fixtures. No policy service, payroll mutation or payment API.
import { demo, result } from './demo'
export interface InsPolicy {
  id: string
  city: string
  subject: string
  from: string
  through: string
  minimum: number
  maximum: number
  personalInsurance: number
  employerInsurance: number
  personalFund: number
  employerFund: number
  basis: string
}
export interface InsEnrollment {
  id: string
  from: string
  through: string
  eligible: boolean | null
  basis: string | null
  insuranceBase: number
  fundBase: number
  policy: InsPolicy
}
export interface InsSnapshot {
  period: string
  subject: string
  enrollments: InsEnrollment[]
  paidPersonal: number | null
  paidEmployer: number | null
  receipt: string | null
}
export interface InsExample {
  label: string
  explanation: string
  before: InsSnapshot
  after: InsSnapshot
}
export const insPolicyA: InsPolicy = {
  id: 'TEST-INS-A-V1',
  city: 'TEST城甲',
  subject: 'TEST-SUBJECT-001',
  from: '2026-01',
  through: '2026-12',
  minimum: 5000,
  maximum: 30000,
  personalInsurance: 0.08,
  employerInsurance: 0.15,
  personalFund: 0.06,
  employerFund: 0.06,
  basis: 'TEST-POLICY-A：虚构年内政策；基数与比例均非真实城市政策'
}
const enrollment: InsEnrollment = {
  id: 'TEST-ENROLL-A',
  from: '2026-01',
  through: '2026-12',
  eligible: true,
  basis: 'TEST-QUAL-A：本月具备全月计缴资格（仅演示，不推定法定折算）',
  insuranceBase: demo.base,
  fundBase: demo.base,
  policy: insPolicyA
}
const main: InsSnapshot = {
  period: demo.period,
  subject: 'TEST-SUBJECT-001',
  enrollments: [enrollment],
  paidPersonal: null,
  paidEmployer: null,
  receipt: null
}
const policyB: InsPolicy = {
  ...insPolicyA,
  id: 'TEST-INS-B-V1',
  city: 'TEST城乙',
  from: '2026-11',
  personalInsurance: 0.09,
  employerInsurance: 0.16,
  personalFund: 0.07,
  employerFund: 0.07,
  basis: 'TEST-POLICY-B：2026-11起虚构城乙；旧城10月终止，同月不双计'
}
export const insExamples = {
  MAIN: {
    label: '主例：应缴1400/2100，实缴未知',
    explanation: '应缴来自TEST基数与比例；缺实缴及凭据，不显示已对账。',
    before: main,
    after: main
  },
  ANNUAL: {
    label: '年度调基：10000 → 12000',
    explanation: '独立候选：TEST调基单批准自2026-10生效；候选基数不改主工资基本10000。',
    before: main,
    after: {
      ...main,
      enrollments: [
        {
          ...enrollment,
          insuranceBase: 12000,
          fundBase: 12000,
          basis: 'TEST-BASE-2026-10：虚构年度调基批准12000，2026-10生效'
        }
      ]
    }
  },
  CITY: {
    label: '城市变更：10月甲 → 11月乙',
    explanation:
      'TEST变更单：甲城资格截至10月，乙城11月开始；按快照月份只选择一个有效参保记录，不把两城相加。',
    before: main,
    after: {
      ...main,
      period: '2026-11',
      enrollments: [
        { ...enrollment, through: '2026-10' },
        {
          ...enrollment,
          id: 'TEST-ENROLL-B',
          from: '2026-11',
          policy: policyB,
          basis: 'TEST-CITY-CHANGE：11月乙城资格批准'
        }
      ]
    }
  },
  JOIN: {
    label: '入职：资格待核对 → TEST确认全月',
    explanation:
      'TEST入职日10月16日，前态资格缺失未知；后态由独立TEST资格单明确10月全月计缴，不按入职天数自动推法定规则。',
    before: { ...main, enrollments: [{ ...enrollment, eligible: null, basis: null }] },
    after: {
      ...main,
      enrollments: [
        {
          ...enrollment,
          from: '2026-10',
          basis: 'TEST-JOIN-001：10月16日入职，TEST决定本月全月计缴'
        }
      ]
    }
  },
  EXIT: {
    label: '离职：10月在保 → 11月终止',
    explanation:
      'TEST离职日10月31日，资格截至10月；11月明确无适用参保记录，依据TEST-EXIT-001计0，不代表真实离职折算。',
    before: main,
    after: {
      ...main,
      period: '2026-11',
      enrollments: [{ ...enrollment, through: '2026-10', basis: 'TEST-EXIT-001：资格截至2026-10' }]
    }
  },
  SUPPLEMENT: {
    label: '应缴大于实缴：补缴候选',
    explanation: 'TEST凭据实缴个人1300/单位2000；各差100元待补缴，不把候选差额当已缴。',
    before: main,
    after: { ...main, paidPersonal: 1300, paidEmployer: 2000, receipt: 'TEST-RECEIPT-SHORT-001' }
  },
  REFUND: {
    label: '实缴大于应缴：退款候选',
    explanation: 'TEST凭据实缴个人1500/单位2200；各差-100元待退款，不把退款当工资或已退。',
    before: main,
    after: { ...main, paidPersonal: 1500, paidEmployer: 2200, receipt: 'TEST-RECEIPT-OVER-001' }
  },
  MATCHED: {
    label: '有TEST凭据：金额核对一致',
    explanation: 'TEST凭据只证明本演示金额一致，不代表真实缴款或生产结清。',
    before: main,
    after: { ...main, paidPersonal: 1400, paidEmployer: 2100, receipt: 'TEST-RECEIPT-MATCH-001' }
  },
  NO_RECEIPT: {
    label: '已填实缴但缺凭据：未对账',
    explanation: '实缴数值可供差额预览；无凭据不能标金额核对完成。',
    before: main,
    after: { ...main, paidPersonal: 1400, paidEmployer: 2100 }
  },
  EXPIRED: {
    label: '有效期不匹配：应缴未知',
    explanation: '资格仍覆盖10月，但政策仅至9月；不可沿用过期TEST政策。',
    before: main,
    after: {
      ...main,
      enrollments: [{ ...enrollment, policy: { ...insPolicyA, through: '2026-09' } }]
    }
  },
  BASE_INVALID: {
    label: '基数超界：阻断测算',
    explanation: 'TEST范围5000～30000：候选社保4000/公积金31000超界，不静默裁剪、不套0。',
    before: main,
    after: { ...main, enrollments: [{ ...enrollment, insuranceBase: 4000, fundBase: 31000 }] }
  },
  DUPLICATE_CITY: {
    label: '两城资格重叠：阻断双计',
    explanation: '11月甲城未终止又开始乙城，重叠待核对；不把两城应缴相加。',
    before: main,
    after: {
      ...main,
      period: '2026-11',
      enrollments: [
        enrollment,
        { ...enrollment, id: 'TEST-ENROLL-B', from: '2026-11', policy: policyB }
      ]
    }
  }
} satisfies Record<string, InsExample>
export type InsExampleKey = keyof typeof insExamples
const round = (n: number) => Math.round((n + Number.EPSILON) * 100) / 100
const validMonth = (value: string) => /^\d{4}-(0[1-9]|1[0-2])$/.test(value)
const inPeriod = (period: string, from: string, through: string) =>
  validMonth(period) &&
  validMonth(from) &&
  validMonth(through) &&
  from <= period &&
  period <= through
export function calculateIns(snapshot: InsSnapshot) {
  let personalInsurance: number | null = null
  let personalFund: number | null = null
  let employerInsurance: number | null = null
  let employerFund: number | null = null
  let reason = '资格、主体、政策有效期、基数或比例缺失/不匹配：应缴未知。'
  // Same record ID and identical payload is counted once; conflicting payloads remain unknown.
  const records = new Map<string, InsEnrollment>()
  let conflict = false
  for (const row of snapshot.enrollments) {
    if (records.has(row.id) && JSON.stringify(records.get(row.id)) !== JSON.stringify(row))
      conflict = true
    records.set(row.id, row)
  }
  const active = [...records.values()].filter((row) =>
    inPeriod(snapshot.period, row.from, row.through)
  )
  if (active.length > 1 || conflict) reason = '重叠/冲突参保记录：阻断重复计缴，待核对城市与资格。'
  else if (
    active.length === 0 &&
    records.size > 0 &&
    validMonth(snapshot.period) &&
    [...records.values()].every(
      (row) =>
        row.basis &&
        row.eligible !== null &&
        row.policy.subject === snapshot.subject &&
        validMonth(row.from) &&
        validMonth(row.through) &&
        row.from <= row.through
    )
  ) {
    personalInsurance = personalFund = employerInsurance = employerFund = 0
    reason = 'TEST资格有效期外，明确无本月适用参保记录；本例应缴0。'
  } else if (active.length === 1) {
    const row = active[0],
      policy = row.policy
    const valid = (n: number) => Number.isFinite(n) && n >= 0
    const validBases = [row.insuranceBase, row.fundBase].every(
      (n) => valid(n) && n >= policy.minimum && n <= policy.maximum
    )
    const validRates = [
      policy.personalInsurance,
      policy.employerInsurance,
      policy.personalFund,
      policy.employerFund
    ].every((n) => valid(n) && n <= 1)
    if (
      !conflict &&
      row.basis &&
      row.eligible !== null &&
      policy.basis &&
      policy.subject === snapshot.subject &&
      inPeriod(snapshot.period, policy.from, policy.through) &&
      valid(policy.minimum) &&
      valid(policy.maximum) &&
      policy.minimum <= policy.maximum &&
      validBases &&
      validRates
    ) {
      personalInsurance = row.eligible ? round(row.insuranceBase * policy.personalInsurance) : 0
      personalFund = row.eligible ? round(row.fundBase * policy.personalFund) : 0
      employerInsurance = row.eligible ? round(row.insuranceBase * policy.employerInsurance) : 0
      employerFund = row.eligible ? round(row.fundBase * policy.employerFund) : 0
      reason = 'TEST主体、资格、政策有效期和基数范围匹配；同记录仅计一次。'
    }
  }
  const personal =
    personalInsurance === null || personalFund === null
      ? null
      : round(personalInsurance + personalFund)
  const employer =
    employerInsurance === null || employerFund === null
      ? null
      : round(employerInsurance + employerFund)
  const knownPaid = (n: number | null) => n !== null && Number.isFinite(n) && n >= 0
  const personalDelta =
    personal !== null && knownPaid(snapshot.paidPersonal)
      ? round(personal - snapshot.paidPersonal!)
      : null
  const employerDelta =
    employer !== null && knownPaid(snapshot.paidEmployer)
      ? round(employer - snapshot.paidEmployer!)
      : null
  const verified = Boolean(snapshot.receipt) && personalDelta !== null && employerDelta !== null
  const status = !verified
    ? '未对账：缺有效应缴、实缴或缴款凭据'
    : personalDelta === 0 && employerDelta === 0
      ? 'TEST金额核对一致（非真实缴款完成）'
      : '有TEST凭据但差额待处理：补缴/退款均未执行'
  return {
    personalInsurance,
    personalFund,
    employerInsurance,
    employerFund,
    personal,
    employer,
    personalDelta,
    employerDelta,
    verified,
    status,
    reason,
    activeCount: active.length,
    candidateCost: employer === null ? null : round(result.gross + employer)
  }
}
export const insDeltaLabel = (n: number | null) =>
  n === null
    ? '未知，待核对'
    : n > 0
      ? `+${n.toFixed(2)}元，待补缴`
      : n < 0
        ? `${n.toFixed(2)}元，待退款`
        : '0.00元，无金额差额'
