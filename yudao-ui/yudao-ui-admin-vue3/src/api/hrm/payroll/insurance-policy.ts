import request from '@/config/axios'
export interface Project {
  code: string
  name: string
  type: number
  minimumBase?: number
  maximumBase?: number
  personalRate?: number
  corporateRate?: number
  personalFixed?: number
  corporateFixed?: number
}
export interface Policy {
  id: number
  entityId: number
  code: string
  city: string
  version: number
  effectiveFrom: string
  effectiveTo: string
  evidence: string
  projects: string
  status: string
  replacesId?: number
}
const root = '/hrm/payroll/insurance-policy'
export const policies = (entityId: number) =>
  request.get<Policy[]>({ url: `${root}/policies`, params: { entityId } })
export const bases = (entityId: number) =>
  request.get<any[]>({ url: `${root}/bases`, params: { entityId } })
export const history = (entityId: number) =>
  request.get<any[]>({ url: `${root}/history`, params: { entityId } })
export const options = (entityId: number) =>
  request.get<{ employees: any[]; users: any[] }>({ url: `${root}/options`, params: { entityId } })
export const createPolicy = (data: object) => request.post({ url: `${root}/policy`, data })
export const proposeBase = (data: object) => request.post({ url: `${root}/base`, data })
export const transition = (kind: 'policy' | 'base', action: string, params: object) =>
  request.post({ url: `${root}/${kind}/${action}`, params })
export const preview = (params: object) =>
  request.get<Record<string, any>>({ url: `${root}/preview`, params })

export interface ImportBatch {
  id: number
  entityId: number
  fileName: string
  status: 'VALIDATED' | 'FAILED' | 'COMMITTED'
  errors: string
  requests?: string
  proposalIds?: string
  creator: string
}
export const importList = (entityId: number) =>
  request.get<ImportBatch[]>({ url: `${root}/imports`, params: { entityId } })
export const importDetails = (id: number) =>
  request.get<ImportBatch>({ url: `${root}/import`, params: { id } })
export const validateImport = async (entityId: number, file: File) => {
  const data = new FormData()
  data.append('entityId', String(entityId))
  data.append('file', file)
  const response = await request.upload<{ data: ImportBatch }>({
    url: `${root}/import-preview`,
    data
  })
  return response.data
}
export const commitImport = (id: number) =>
  request.post<number[]>({ url: `${root}/import-commit`, params: { id } })
export const baseTemplate = (policyId: number) =>
  request.download({ url: `${root}/base-template`, params: { policyId } })
export const importErrors = (id: number) =>
  request.download({ url: `${root}/import-errors`, params: { id } })
