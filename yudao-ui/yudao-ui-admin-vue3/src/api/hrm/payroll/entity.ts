import request from '@/config/axios'
export interface Entity {
  id: number
  code: string
  name: string
  taxRegistrationNumber: string
  evidence: string
  status: string
  allowedActions: string[]
}
const root = '/hrm/payroll/entity'
export const list = () => request.get<Entity[]>({ url: `${root}/list` })
export const create = (data: object) => request.post<number>({ url: `${root}/create`, data })
export const revise = (id: number, data: object) =>
  request.post({ url: `${root}/revise`, params: { id }, data })
export const approve = (params: object) => request.post({ url: `${root}/approve`, params })
export const grants = (id: number) => request.get<any[]>({ url: `${root}/grants`, params: { id } })
export const grant = (data: object) => request.post({ url: `${root}/grant`, data })
export const revoke = (params: object) => request.post({ url: `${root}/revoke`, params })
export const memberships = (id: number) =>
  request.get<any[]>({ url: `${root}/memberships`, params: { id } })
export const assign = (data: object) => request.post({ url: `${root}/assign`, data })
export const approveMembership = (params: object) =>
  request.post({ url: `${root}/membership/approve`, params })
export const rejectMembership = (params: object) =>
  request.post({ url: `${root}/membership/reject`, params })
export const history = (id: number) =>
  request.get<any[]>({ url: `${root}/history`, params: { id } })
export const options = (id: number) =>
  request.get<{ employees: any[]; users: any[] }>({ url: `${root}/options`, params: { id } })
