import request from '@/config/axios'

export interface Requirement {
  id?: number
  version?: number
  code: string
  moduleCode: string
  title: string
  description?: string
  ownerName?: string
  priority?: number
  scopeDecision?: number
  scopeReason?: string
  applicableScope?: string
  sourceCodes: string[]
  fieldMapping?: string
  acceptanceCriteria?: string
  remark?: string
  builtIn?: boolean
  origin?: string
  status?: number
  reviewedByName?: string
  reviewedTime?: number
  evidence?: string
}
export interface Source {
  id?: number
  version?: number
  code: string
  name: string
  description?: string
  requiredFields?: string
  actualSystem?: string
  fieldMapping?: string
  ownerName?: string
  readiness: number
  evidence?: string
  confirmedByName?: string
  confirmedTime?: number
}
export interface Baseline {
  id: number
  requirementCount: number
  sourceCount: number
  exportedByName: string
  createTime: number
}
export interface History {
  id: number
  action: string
  fromVersion?: number
  toVersion: number
  actorName: string
  reason: string
  beforeSnapshot?: string
  afterSnapshot?: string
  createTime: number
}
export interface Summary {
  total: number
  confirmed: number
  pending: number
  disputed: number
  mvp: number
  sourceTotal: number
  sourceReady: number
}
export const modules = [
  { code: 'overview', name: '薪酬总览' },
  { code: 'plan', name: '薪酬方案' },
  { code: 'calc', name: '工资核算' },
  { code: 'tax', name: '个税工资条' },
  { code: 'attendance', name: '考勤数据' },
  { code: 'overtime', name: '加班管理' },
  { code: 'hours', name: '工时管理' },
  { code: 'insurance', name: '社保公积金' },
  { code: 'report', name: '报表分析' },
  { code: 'req', name: '需求征集' }
]
export const states = ['待确认', '已确认', '异议']
export const scopes = ['未决定', '首期纳入', '暂缓']
export const readinessLabels = ['待核实', '已就绪', '待补齐']
const url = '/hrm/payroll/requirements'
export const initialize = () =>
  request.post<{ createdRequirements: number; createdSources: number }>({
    url: url + '/initialize'
  })
export const page = (params: object) =>
  request.get<{ list: Requirement[]; total: number }>({ url: url + '/page', params })
export const get = (id: number) => request.get<Requirement>({ url: url + '/get', params: { id } })
export const summary = () => request.get<Summary>({ url: url + '/summary' })
export const create = (data: Requirement) => request.post<number>({ url: url + '/create', data })
export const update = (data: Requirement) => request.put({ url: url + '/update', data })
export const remove = (id: number, version: number) =>
  request.delete({ url: url + '/delete', params: { id, version } })
export const review = (data: { id: number; version: number; status: number; evidence: string }) =>
  request.post({ url: url + '/review', data })
export const sources = () => request.get<Source[]>({ url: url + '/sources/list' })
export const createSource = (data: Source) =>
  request.post<number>({ url: url + '/sources/create', data })
export const updateSource = (data: Source) => request.put({ url: url + '/sources/update', data })
export const history = (objectType: string, objectId: number) =>
  request.get<History[]>({ url: url + '/history', params: { objectType, objectId } })
export const baselines = () => request.get<Baseline[]>({ url: url + '/baselines/list' })
export const createBaseline = () => request.post<number>({ url: url + '/baselines/create' })
export const exportBaseline = (id: number) =>
  request.download<Blob>({ url: url + '/baselines/export', params: { id } })
