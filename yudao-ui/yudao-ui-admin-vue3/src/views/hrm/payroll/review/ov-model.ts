// TEST overview only: derive readiness from the existing discussion models, never business APIs.
import { demo, result, demoTrend } from './demo'
import { hourExamples, summarizeHours } from './hour-model'
import { attendanceExamples, summarizeAttendance } from './att-model'
import { otExamples, resolveOt } from './ot-model'
import { insExamples, calculateIns } from './ins-model'
export type OvPeriod = '2026-10' | '2026-11'
export type OvScope = 'MAIN' | 'MISSING_ATT'
export function resolveOverview(period: OvPeriod, scope: OvScope) {
  const current = period === demo.period
  const hour = current ? summarizeHours(hourExamples.STANDARD) : null
  const att = current
    ? summarizeAttendance(attendanceExamples[scope === 'MAIN' ? 'NORMAL' : 'TRAVEL_PENDING'])
    : null
  const ot = current ? resolveOt(otExamples.APPROVED) : null
  const ins = current ? calculateIns(insExamples.MAIN.after) : null
  const measurable = !!(
    hour &&
    att &&
    ot &&
    ins &&
    hour.salaryDenominator &&
    att.wageDeduction === 0 &&
    ot.wage !== null &&
    ins.personal !== null &&
    ins.employer !== null
  )
  const amounts = measurable
    ? {
        gross: result.gross,
        personal: ins!.personal,
        tax: result.tax,
        net: result.net,
        cost: ins!.candidateCost
      }
    : { gross: null, personal: null, tax: null, net: null, cost: null }
  const readiness = [
    {
      module: 'HOUR' as const,
      anchor: 'field-hour-02',
      object: current ? 'TEST-HOUR-STD-001' : 'TEST-HOUR-11缺来源',
      responsible: 'TEST主管',
      deadline: `${period}-28（讨论期限）`,
      status: hour?.status ?? '缺本期工时来源，未知',
      gap: !hour?.salaryDenominator
    },
    {
      module: 'ATT' as const,
      anchor: 'field-att-04',
      object: current
        ? scope === 'MAIN'
          ? 'TEST-ATT-10'
          : 'TEST-D03 / TEST出差待批'
        : 'TEST-ATT-11缺来源',
      responsible: 'TEST考勤HR',
      deadline: `${period}-28（讨论期限）`,
      status: att?.wageExplanation ?? '缺本期考勤来源，未知',
      gap: att?.wageDeduction !== 0
    },
    {
      module: 'OT' as const,
      anchor: 'field-ot-04',
      object: current ? otExamples.APPROVED.id : 'TEST-OT-11缺来源',
      responsible: 'TEST主管',
      deadline: `${period}-29（讨论期限）`,
      status: ot
        ? `认可${ot.hours}小时 / 工资${ot.wage}元；TEST上限提示${ot.overLimit ? '待讨论' : '无'}`
        : '缺本期加班来源，未知',
      gap: !ot || ot.wage === null || ot.overLimit
    },
    {
      module: 'INS' as const,
      anchor: 'field-ins-05',
      object: current ? 'TEST-INS-10 / 缴款凭据' : 'TEST-INS-11缺来源',
      responsible: 'TEST参保HR/财务',
      deadline: `${period}-29（讨论期限）`,
      status: ins
        ? `个人应缴${ins.personal} / 单位应缴${ins.employer}元；${ins.status}；应缴可测，实缴待核对`
        : '缺本期参保政策与资格来源，未知',
      gap: !ins?.verified
    }
  ]
  return {
    period,
    scope,
    employee: demo.employee,
    amounts,
    measurable,
    readiness,
    gaps: readiness.filter((r) => r.gap),
    trend: [
      ...demoTrend.map((row) => ({
        ...row,
        gross: row.period === period ? amounts.gross : row.gross,
        kind: row.period === period ? '当前选择' : '独立TEST历史/固定主例参照'
      })),
      ...(current ? [] : [{ period, gross: null, kind: '当前选择：缺来源' }])
    ]
  }
}
export interface OvRealState {
  loading: boolean
  error: string
  label?: string
  visibleRows: number
}
export function overviewRealState(state: OvRealState) {
  if (state.loading) return '真实评审数据加载中：结果未知，不能视为空或通过。'
  if (state.error) return '真实评审读取失败或无权：原因以共用错误提示为准，未返回结果不推定为空。'
  return `${state.label || '服务端未提供统计范围标签；不推定全项目权限'}；${state.visibleRows === 0 ? '当前筛选无可见行，不代表全项目为空' : `本页可见${state.visibleRows}行，隐藏对象和数量不推测`}。真实统计与来源问题请查看下方共用征集及受控摘要。`
}
