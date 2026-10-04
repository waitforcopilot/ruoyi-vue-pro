import request from '@/config/axios'
import type { History } from '@/api/hrm/payroll/requirements'

export interface ContractField {
  key: string
  label: string
  type: 'TEXT' | 'INTEGER' | 'DECIMAL' | 'DATE'
  required: boolean
  unit?: string
  scale?: number
  maxLength?: number
}
export interface ContractSchema {
  fields: ContractField[]
  keyFields: string[]
  periodField?: string
  subjectField?: string
  employeeField?: string
  externalEmployeeField?: string
  employeeNamespace?: string
}
export interface Contract {
  id?: number
  sourceId?: number
  sourceCode?: string
  sourceName?: string
  contractVersion?: number
  revision?: number
  status?: number
  title: string
  actualSystem?: string
  ownerName?: string
  applicableScope?: string
  evidence?: string
  schema: ContractSchema
  fieldCount?: number
  reviewedByName?: string
  reviewedTime?: number
}
export interface SourceOption {
  id: number
  code: string
  name: string
  readiness: number
}
export interface Issue {
  line?: number
  field?: string
  code: string
  message: string
}
export interface PreviewRow {
  line: number
  values: Record<string, string | null>
  employeeId?: number
  employeeMapping?: import('@/api/hrm/payroll/identity').Match
  issues: Issue[]
}
export interface PreviewResult {
  rowCount: number
  validCount: number
  errorCount: number
  employeeMatchEnabled: boolean
  employeeMatchMode?: 'NONE' | 'HRM_JOB_NUMBER' | 'EXTERNAL_MAPPING'
  globalIssues: Issue[]
  rows: PreviewRow[]
}
export interface Batch {
  id: number
  contractId: number
  sourceCode: string
  contractVersion: number
  fileName: string
  fileHash: string
  declaredScope: string
  periodStart: string
  periodEnd: string
  rowCount: number
  validCount: number
  errorCount: number
  status: number
  createdByName: string
  createTime: number
}
export interface BatchDetail extends Batch {
  contractSnapshot: Contract
  result: PreviewResult
}
const url = '/hrm/payroll/intake'
export const states = ['草稿', '已确认', '已停用']
export const sources = () => request.get<SourceOption[]>({ url: url + '/sources' })
export const contracts = (params: object) =>
  request.get<{ list: Contract[]; total: number }>({ url: url + '/contracts/page', params })
export const getContract = (id: number) =>
  request.get<Contract>({ url: url + '/contracts/get', params: { id } })
export const createContract = (data: Contract) =>
  request.post<number>({ url: url + '/contracts/create', data })
export const updateContract = (data: Contract) =>
  request.put({ url: url + '/contracts/update', data })
export const reviewContract = (data: {
  id: number
  revision: number
  action: string
  evidence: string
}) => request.post({ url: url + '/contracts/review', data })
export const history = (id: number) =>
  request.get<History[]>({ url: url + '/contracts/history', params: { id } })
export const template = (id: number) =>
  request.download<Blob>({ url: url + '/contracts/template', params: { id } })
export const preview = (data: FormData) =>
  request.post<number>({ url: url + '/batches/preview', data, headersType: 'multipart/form-data' })
export const batches = (params: object) =>
  request.get<{ list: Batch[]; total: number }>({ url: url + '/batches/page', params })
export const batch = (id: number) =>
  request.get<BatchDetail>({ url: url + '/batches/get', params: { id } })
