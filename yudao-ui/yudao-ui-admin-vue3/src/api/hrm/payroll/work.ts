import request from '@/config/axios'
import { payrollDateTimes } from './date-time'
const url = '/hrm/payroll/work'
export const employees = (params: object) =>
  request.get<{ id: number; name: string; jobNumber: string }[]>({
    url: `${url}/employees`,
    params
  })
export const rules = (entityId: number, employeeId: number) =>
  request.get<any[]>({ url: `${url}/rules`, params: { entityId, employeeId } })
export const entries = (params: object) => request.get<any[]>({ url: `${url}/entries`, params })
export const summary = (params: object) => request.get<any>({ url: `${url}/summary`, params })
export const ledger = (entityId: number, employeeId: number) =>
  request.get<any[]>({ url: `${url}/time-off`, params: { entityId, employeeId } })
export const createRule = (data: object) => request.post({ url: `${url}/rule`, data })
export const approveRule = (id: number) =>
  request.post({ url: `${url}/rule/approve`, params: { id } })
export const confirm = (data: object) =>
  request.post<number[]>({
    url: `${url}/confirm`,
    data: payrollDateTimes(data, ['startTime', 'endTime', 'clockStart', 'clockEnd'])
  })
export const reserve = (params: object) => request.post({ url: `${url}/time-off/use`, params })
export const consume = (entityId: number, debitId: number) =>
  request.post({ url: `${url}/time-off/consume`, params: { entityId, debitId } })
export const refund = (params: object) => request.post({ url: `${url}/time-off/refund`, params })
export const settle = (params: object) => request.post({ url: `${url}/cycle/settle`, params })

export const rejectRule = (id: number, reason: string) =>
  request.post({ url: `${url}/rule/reject`, params: { id, reason } })
