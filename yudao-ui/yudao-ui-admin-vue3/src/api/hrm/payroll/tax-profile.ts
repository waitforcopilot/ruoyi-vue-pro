import request from '@/config/axios'
const url = '/hrm/payroll/tax-profile'
export const employees = (params: object) => request.get<any[]>({ url: `${url}/employees`, params })
export const list = (params: object) => request.get<any[]>({ url: `${url}/list`, params })
export const create = (data: object) => request.post<number>({ url: `${url}/create`, data })
export const review = (params: object) => request.post({ url: `${url}/review`, params })
export const history = (id: number) => request.get<any[]>({ url: `${url}/history`, params: { id } })
export const privateIdentifier = (id: number) =>
  request.get<string>({ url: `${url}/private-identifier`, params: { id } })

export const cashLedger = (params: object) =>
  request.get<any>({ url: `${url}/cash-ledger`, params })

export const basicDeduction = (params: object) =>
  request.get<any>({ url: `${url}/basic-deduction`, params })

export const identitySource = (params: object) =>
  request.get<any>({ url: `${url}/identity-source`, params })
