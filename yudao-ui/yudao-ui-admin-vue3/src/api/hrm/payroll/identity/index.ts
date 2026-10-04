import request from '@/config/axios'
import type { History } from '@/api/hrm/payroll/requirements'
export interface Source {
  id: number
  code: string
  name: string
}
export interface Mapping {
  id?: number
  sourceId?: number
  sourceCode?: string
  namespace: string
  externalCode: string
  employeeId?: number
  employeeFingerprint?: string
  revision?: number
  mappingVersion?: number
  status?: number
  ownerName?: string
  reference?: string
  effectiveFrom?: string
  effectiveTo?: string
  snapshotName?: string
  snapshotJobNumber?: string
  snapshotDeptId?: number
  snapshotUserId?: number
  snapshotEntryTime?: string
  snapshotLeaveTime?: string
  snapshotEmployeeStatus?: number
  snapshotCapturedAt?: string
  reviewedByName?: string
  reviewedTime?: string
  evidence?: string
}
export interface Match {
  externalCode: string
  mappingId?: number
  mappingVersion?: number
  employeeId?: number
  snapshotCapturedAt?: string
  issueCode?: string
  message?: string
}
const url = '/hrm/payroll/identity'
export const states = ['草稿', '已确认', '已停用']
export const sources = () => request.get<Source[]>({ url: url + '/sources' })
export const employee = (id: number) =>
  request.get<Mapping>({ url: url + '/employee', params: { id } })
export const page = (params: object) =>
  request.get<{ list: Mapping[]; total: number }>({ url: url + '/page', params })
export const get = (id: number) => request.get<Mapping>({ url: url + '/get', params: { id } })
export const create = (data: Mapping) => request.post<number>({ url: url + '/create', data })
export const update = (data: Mapping) => request.put({ url: url + '/update', data })
export const newVersion = (id: number, revision: number) =>
  request.post<number>({ url: url + '/new-version', params: { id, revision } })
export const review = (data: { id: number; revision: number; action: string; evidence: string }) =>
  request.post({ url: url + '/review', data })
export const history = (id: number) =>
  request.get<History[]>({ url: url + '/history', params: { id } })
export const resolve = (
  sourceId: number,
  namespace: string,
  data: { externalCode: string; start: string; end: string }
) => request.post<Match>({ url: url + '/resolve', params: { sourceId, namespace }, data })
