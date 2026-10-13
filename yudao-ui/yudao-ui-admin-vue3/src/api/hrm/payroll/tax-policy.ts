import request from '@/config/axios'
const url = '/hrm/payroll/tax-policy'
export const entities = () => request.get<any[]>({ url: `${url}/entities` })
export const list = (params: object) => request.get<any[]>({ url: `${url}/list`, params })
export const defaults = (params: object) => request.get<any>({ url: `${url}/defaults`, params })
export const create = (data: object) => request.post<number>({ url: `${url}/create`, data })
export const review = (params: object) => request.post({ url: `${url}/review`, params })
export const trial = (data: object) => request.post<any>({ url: `${url}/trial`, data })
export const history = (id: number) => request.get<any[]>({ url: `${url}/history`, params: { id } })
