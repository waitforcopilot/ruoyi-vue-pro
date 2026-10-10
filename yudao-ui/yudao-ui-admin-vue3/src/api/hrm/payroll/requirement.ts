import request from '@/config/axios'

export interface PayrollRequirement {
  id?: number
  code: string
  module: string
  description: string
  fieldMapping?: string
  sourceSystem?: string
  sourceOwnerId?: number
  reviewerId?: number
  priority: number
  status: string
  readiness: string
  acceptance?: string
  evidence?: string
  version?: number
  confirmedBy?: number
  confirmedAt?: string
}
export const listRequirements = (module?: string) =>
  request.get<PayrollRequirement[]>({ url: '/hrm/payroll/requirement/list', params: { module } })
export const saveRequirement = (data: PayrollRequirement) =>
  request.post<number>({ url: '/hrm/payroll/requirement/save', data })
export const initializeRequirements = () =>
  request.post<number>({ url: '/hrm/payroll/requirement/initialize' })
export const requirementHistory = (id: number) =>
  request.get({ url: '/hrm/payroll/requirement/history', params: { id } })
export const exportRequirements = (module?: string) =>
  request.download({ url: '/hrm/payroll/requirement/export', params: { module } })
