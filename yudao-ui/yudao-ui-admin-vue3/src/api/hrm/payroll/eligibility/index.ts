import request from '@/config/axios'
import type { Mapping } from '@/api/hrm/payroll/identity'
import type { History } from '@/api/hrm/payroll/requirements'
export interface Eligibility extends Omit<
  Mapping,
  'sourceId' | 'sourceCode' | 'namespace' | 'externalCode' | 'mappingVersion'
> {
  entityCode: string
  entityName: string
  eligibilityVersion?: number
  qualification?: 'INCLUDED' | 'EXCLUDED'
  reason?: string
}
export interface LookupRequest {
  entityCode: string
  employeeId?: number
  start: string
  end: string
}
export interface Match {
  matched: boolean
  issueCode?: string
  explanation: string
  eligibility?: Eligibility
  personChanged: boolean
}
const url = '/hrm/payroll/employee-eligibilities'
export const states = ['草稿', '已确认', '已停用']
export const qualification = (value?: string) =>
  value === 'INCLUDED' ? '纳入计薪' : value === 'EXCLUDED' ? '排除计薪' : '尚未核定'
export const employee = (id: number) =>
  request.get<Mapping>({ url: url + '/employee', params: { id } })
export const page = (params: object) =>
  request.get<{ list: Eligibility[]; total: number }>({ url: url + '/page', params })
export const get = (id: number) => request.get<Eligibility>({ url: url + '/get', params: { id } })
export const create = (data: Eligibility) => request.post<number>({ url: url + '/create', data })
export const update = (data: Eligibility) => request.put({ url: url + '/update', data })
export const newVersion = (id: number, revision: number) =>
  request.post<number>({ url: url + '/new-version', params: { id, revision } })
export const review = (data: { id: number; revision: number; action: string; evidence: string }) =>
  request.post({ url: url + '/review', data })
export const history = (id: number) =>
  request.get<History[]>({ url: url + '/history', params: { id } })
export const lookup = (data: LookupRequest) => request.post<Match>({ url: url + '/lookup', data })
