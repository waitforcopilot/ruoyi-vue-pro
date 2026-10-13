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

export const noOvertimeCases = (params: object) =>
  request.get<any[]>({ url: `${url}/no-overtime`, params })
export const submitNoOvertime = (data: object) =>
  request.post<number>({ url: `${url}/no-overtime`, data })
export const reviewNoOvertime = (params: object) =>
  request.post({ url: `${url}/no-overtime/review`, params })

const varianceUrl = `${url}/variance`
export const variancePreview = (params: object) =>
  request.get<any>({ url: `${varianceUrl}/preview`, params })
export const varianceList = (params: object) =>
  request.get<any[]>({ url: `${varianceUrl}/list`, params })
export const createVariance = (data: object) =>
  request.post<number>({ url: `${varianceUrl}/create`, data: payrollDateTimes(data, ['dueTime']) })
export const submitVariance = (params: object) =>
  request.post({ url: `${varianceUrl}/submit`, params })
export const reviewVariance = (params: object) =>
  request.post({ url: `${varianceUrl}/review`, params })
export const reopenVariance = (params: object) =>
  request.post({ url: `${varianceUrl}/reopen`, params })
export const varianceHistory = (id: number) =>
  request.get<any[]>({ url: `${varianceUrl}/history`, params: { id } })

export const reassignVariance = (data: object) =>
  request.post({ url: `${varianceUrl}/reassign`, data: payrollDateTimes(data, ['dueTime']) })
