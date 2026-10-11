import request from '@/config/axios'
export interface Formula {
  id?: number
  optionCode: number
  salaryGroupId?: number
  expression: string
  effectiveFrom: string
  effectiveTo: string
  evidence: string
  status?: string
  approvedBy?: number
}
export interface Variable {
  code: number
  name: string
}
export const list = () => request.get<Formula[]>({ url: '/hrm/payroll/formula/list' })
export const variables = () => request.get<Variable[]>({ url: '/hrm/payroll/formula/variables' })
export const groups = () =>
  request.get<{ id: number; name: string }[]>({ url: '/hrm/payroll/formula/groups' })
export const create = (data: Formula) =>
  request.post<number>({ url: '/hrm/payroll/formula/create', data })
export const approve = (id: number) =>
  request.post({ url: '/hrm/payroll/formula/approve', params: { id } })
export const preview = (data: {
  id: number
  date: string
  salaryGroupId?: number
  inputs: Record<string, number>
}) => request.post<Record<string, number>>({ url: '/hrm/payroll/formula/preview', data })
