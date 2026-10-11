import request from '@/config/axios'
const url = '/hrm/payroll/work'
export const employees = () =>
  request.get<{ id: number; name: string; jobNumber: string }[]>({ url: `${url}/employees` })
export const rules = (employeeId: number) =>
  request.get<any[]>({ url: `${url}/rules`, params: { employeeId } })
export const entries = (params: object) => request.get<any[]>({ url: `${url}/entries`, params })
export const summary = (params: object) => request.get<any>({ url: `${url}/summary`, params })
export const ledger = (employeeId: number) =>
  request.get<any[]>({ url: `${url}/time-off`, params: { employeeId } })
export const createRule = (data: object) => request.post({ url: `${url}/rule`, data })
export const approveRule = (id: number) =>
  request.post({ url: `${url}/rule/approve`, params: { id } })
export const confirm = (data: object) => request.post<number[]>({ url: `${url}/confirm`, data })
export const reserve = (params: object) => request.post({ url: `${url}/time-off/use`, params })
export const consume = (debitId: number) =>
  request.post({ url: `${url}/time-off/consume`, params: { debitId } })
export const refund = (params: object) => request.post({ url: `${url}/time-off/refund`, params })
export const settle = (params: object) => request.post({ url: `${url}/cycle/settle`, params })
