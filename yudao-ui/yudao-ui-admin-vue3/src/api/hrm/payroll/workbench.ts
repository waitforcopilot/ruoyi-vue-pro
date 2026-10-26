import request from '@/config/axios'

export interface WorkbenchRecord {
  id: number
  projectId: number
  kind: string
  version: number
  data: Record<string, any>
  bindings?: Record<string, string>
  binding?: string
  available?: boolean
  semanticVerified?: boolean
  sourceSemanticVerified?: boolean
  QSResult?: boolean
  dates?: { unresolved: boolean; overdue: boolean; wasOverdue: boolean }
  affectedRequirementIds?: number[]
  permittedActions?: string[]
}
const root = '/hrm/payroll/workbench'
export const listWorkbench = (projectId: number, kind: string) =>
  request.get<WorkbenchRecord[]>({ url: root + '/list', params: { projectId, kind } })
export const workbenchHistory = (projectId: number, kind: string, id: number) =>
  request.get<WorkbenchRecord[]>({ url: root + '/history', params: { projectId, kind, id } })
export const workbenchCommand = (path: string, data: Record<string, any>) =>
  request.post({ url: root + path, data })
