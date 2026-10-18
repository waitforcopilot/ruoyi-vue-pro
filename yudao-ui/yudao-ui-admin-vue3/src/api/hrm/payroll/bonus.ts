import request from '@/config/axios'
const url = '/hrm/payroll/bonus'
export const list = (params: object) => request.get<any[]>({ url: `${url}/list`, params })
export const create = (data: object) => request.post<number>({ url: `${url}/create`, data })
export const review = (params: { id: number; approved: boolean; reason: string }) =>
  request.post({ url: `${url}/review`, params })
export const history = (id: number) => request.get<any[]>({ url: `${url}/history`, params: { id } })

export const employees = (batchId: number) =>
  request.get<any[]>({ url: `${url}/employees`, params: { batchId } })
