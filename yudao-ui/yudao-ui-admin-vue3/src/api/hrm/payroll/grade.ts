import request from '@/config/axios'
export interface Grade {
  gradeCode: string
  stepCode: string
  minimum: number
  standard: number
  maximum: number
}
export interface PostCoefficient {
  postName: string
  coefficient: number
}
export interface Policy {
  id: number
  code: string
  name: string
  version: number
  optionCode: number
  effectiveFrom: string
  effectiveTo: string
  grades: string
  postCoefficients: string
  evidence: string
  status: string
}
export interface Draft {
  code: string
  name: string
  optionCode?: number
  effectiveFrom: string
  effectiveTo: string
  evidence: string
  grades: Grade[]
  postCoefficients: PostCoefficient[]
}
export const list = () => request.get<Policy[]>({ url: '/hrm/payroll/grade-policy/list' })
export const variables = () =>
  request.get<{ code: number; name: string }[]>({ url: '/hrm/payroll/grade-policy/variables' })
export const employees = () =>
  request.get<{ id: number; name: string; jobNumber: string }[]>({
    url: '/hrm/payroll/grade-policy/employees'
  })
export const create = (data: Draft) =>
  request.post<number>({ url: '/hrm/payroll/grade-policy/create', data })
export const approve = (id: number) =>
  request.post({ url: '/hrm/payroll/grade-policy/approve', params: { id } })
export const apply = (data: {
  policyId: number
  employeeId: number
  gradeCode: string
  stepCode: string
  effectiveDate: string
  baseAmount?: number
  probationFactor: number
}) => request.post<number>({ url: '/hrm/payroll/grade-policy/apply', data })
export const template = () => request.download({ url: '/hrm/payroll/grade-policy/import-template' })
export const exportPolicy = (id: number) =>
  request.download({ url: '/hrm/payroll/grade-policy/export', params: { id } })
export const validateImport = async (file: File) => {
  const data = new FormData()
  data.append('file', file)
  const response = await request.upload<{ data: { row: number; reason: string }[] }>({
    url: '/hrm/payroll/grade-policy/validate-import',
    data
  })
  return response.data
}
export const importPolicy = async (file: File, metadata: Draft) => {
  const data = new FormData()
  data.append('file', file)
  data.append('metadata', JSON.stringify(metadata))
  const response = await request.upload<{ data: number }>({
    url: '/hrm/payroll/grade-policy/import',
    data
  })
  return response.data
}
