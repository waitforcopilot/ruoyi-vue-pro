import request from '@/config/axios'
export interface Config {
  lowerBase?: string | null
  upperBase?: string | null
  baseUnit?: string | null
  corporateMode?: string | null
  personalMode?: string | null
  corporateRatePercent?: string | null
  personalRatePercent?: string | null
  corporateFixedAmount?: string | null
  personalFixedAmount?: string | null
  amountScale?: number | null
  roundingMode?: string | null
  roundingStage?: string | null
}
export interface Policy {
  id?: number
  title: string
  cityAreaId?: number
  cityName?: string
  scopeCode?: string
  scopeName?: string
  projectType?: number
  projectCode?: string
  projectName?: string
  customProjectCode?: string
  policyVersion?: number
  revision?: number
  status?: number
  ownerName?: string
  reference?: string
  sourceUrl?: string
  effectiveFrom?: string
  effectiveTo?: string
  configSchemaVersion?: number
  config?: Config
  reviewedByName?: string
  reviewedTime?: number
  evidence?: string
  createTime?: number
}
export interface Project {
  type: number
  name: string
  custom: boolean
}
export interface Preview {
  policy: Policy
  baseAmount: string
  corporateRawAmount: string
  personalRawAmount: string
  corporateAmount: string
  personalAmount: string
  corporateExpression: string
  personalExpression: string
  corporateSteps: string
  personalSteps: string
  explanation: string
}
export interface Change {
  path: string
  label: string
  left?: string
  right?: string
  kind: string
}
export interface Comparison {
  left: Policy
  right: Policy
  changes: Change[]
}
export const states = ['草稿', '已确认', '已停用']
export const modes: Record<string, string> = {
  RATE: '按比例',
  FIXED: '固定金额',
  RATE_PLUS_FIXED: '比例加固定额'
}
export const units: Record<string, string> = {
  YUAN_MONTH: '元 / 月',
  YUAN_YEAR: '元 / 年',
  YUAN_DECLARED: '元 / 一次声明'
}
export const roundings: Record<string, string> = {
  HALF_UP: '四舍五入',
  HALF_EVEN: '四舍六入、五成双',
  DOWN: '向零舍入',
  UP: '远离零舍入'
}
export const stages: Record<string, string> = { TOTAL: '合计后舍入', COMPONENT: '各部分舍入后合计' }
const root = '/hrm/payroll/insurance-policies'
export const projects = (): Promise<Project[]> => request.get({ url: root + '/projects' })
export const page = (params: object): Promise<{ list: Policy[]; total: number }> =>
  request.get({ url: root + '/page', params })
export const get = (id: number): Promise<Policy> =>
  request.get({ url: root + '/get', params: { id } })
export const create = (data: object): Promise<number> =>
  request.post({ url: root + '/create', data })
export const update = (data: object) => request.put({ url: root + '/update', data })
export const newVersion = (id: number, revision: number): Promise<number> =>
  request.post({ url: root + '/new-version', params: { id, revision } })
export const review = (data: { id: number; revision: number; action: string; evidence: string }) =>
  request.post({ url: root + '/review', data })
export const history = (id: number) => request.get({ url: root + '/history', params: { id } })
export const compare = (leftId: number, rightId: number): Promise<Comparison> =>
  request.get({ url: root + '/compare', params: { leftId, rightId } })
export const resolve = (id: number, start: string, end: string): Promise<Policy> =>
  request.get({ url: root + '/resolve', params: { id, start, end } })
export const preview = (data: {
  policyId: number
  start: string
  end: string
  baseAmount: string
}): Promise<Preview> => request.post({ url: root + '/preview', data })
