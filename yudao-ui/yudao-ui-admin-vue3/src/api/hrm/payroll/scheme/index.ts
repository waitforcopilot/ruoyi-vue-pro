import request from '@/config/axios'
export interface Group {
  id: number
  name: string
  salaryStandard?: string
  changeRule?: string
  taxRuleId?: number
}
export interface TaxRule {
  id: number
  name: string
  type: number
  taxEnabled: boolean
  threshold?: string
  decimalScale?: number
  cycleType?: number
}
export interface Option {
  id: number
  code: number
  parentCode: number
  name: string
  remark?: string
  type: number
  visible: boolean
  enabled: boolean
  taxEnabled: boolean
  calculateEnabled: boolean
  templateId?: number
  systemFlag: boolean
}
export interface Snapshot {
  schemaVersion: number
  group: Group
  taxRule?: TaxRule
  options: Option[]
  issues: string[]
}
export interface Scheme {
  id?: number
  groupId?: number
  groupName?: string
  title: string
  schemeVersion?: number
  revision?: number
  status?: number
  ownerName?: string
  reference?: string
  effectiveFrom?: string
  effectiveTo?: string
  optionCount?: number
  sourceHash?: string
  capturedAt?: string
  reviewedByName?: string
  reviewedTime?: string
  evidence?: string
  snapshot?: Snapshot
  expectedSourceHash?: string
}
export interface Change {
  path: string
  label: string
  left?: string
  right?: string
  kind: string
}
export interface Comparison {
  left: Scheme
  right: Scheme
  changes: Change[]
}
const root = '/hrm/payroll/schemes'
export const states = ['草稿', '已确认', '已停用']
export const groups = (search?: string): Promise<Group[]> =>
  request.get({ url: root + '/groups', params: { search } })
export const capture = (groupId: number): Promise<Scheme> =>
  request.get({ url: root + '/capture', params: { groupId } })
export const page = (params: object): Promise<{ list: Scheme[]; total: number }> =>
  request.get({ url: root + '/page', params })
export const get = (id: number): Promise<Scheme> =>
  request.get({ url: root + '/get', params: { id } })
export const create = (data: Scheme): Promise<number> =>
  request.post({ url: root + '/create', data })
export const update = (data: Scheme) => request.put({ url: root + '/update', data })
export const newVersion = (id: number, revision: number): Promise<number> =>
  request.post({ url: root + '/new-version', params: { id, revision } })
export const review = (data: { id: number; revision: number; action: string; evidence: string }) =>
  request.post({ url: root + '/review', data })
export const history = (id: number) => request.get({ url: root + '/history', params: { id } })
export const compare = (leftId: number, rightId: number): Promise<Comparison> =>
  request.get({ url: root + '/compare', params: { leftId, rightId } })
export const resolve = (groupId: number, start: string, end: string): Promise<Scheme> =>
  request.get({ url: root + '/resolve', params: { groupId, start, end } })
