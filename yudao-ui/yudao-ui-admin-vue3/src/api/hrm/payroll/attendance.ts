import request from '@/config/axios'
import { payrollDateTimes } from './date-time'
const root = '/hrm/payroll/attendance'
const portal = '/hrm/portal/payroll/attendance'
export const employees = () =>
  request.get<{ id: number; name: string; jobNumber: string }[]>({ url: `${root}/employees` })
export const users = () => request.get<{ id: number; name: string }[]>({ url: `${root}/users` })
export const issues = (params: object) => request.get<any[]>({ url: `${root}/issues`, params })
export const mine = () => request.get<any[]>({ url: `${portal}/mine` })
export const overdue = () => request.get<any[]>({ url: `${portal}/overdue` })
export const history = (id: number, isPortal = false) =>
  request.get<any[]>({ url: `${isPortal ? portal : root}/history`, params: { id } })
export const create = (data: object) =>
  request.post<number>({ url: `${root}/issue`, data: payrollDateTimes(data, ['dueTime']) })
export const submit = (data: object, isPortal = false) =>
  request.post({
    url: `${isPortal ? portal : root}/submit`,
    data: payrollDateTimes(data, ['correctionTime'])
  })
export const review = (params: object) => request.post({ url: `${root}/review`, params })
export const close = (params: object) => request.post({ url: `${root}/close`, params })
export const reopen = (params: object) => request.post({ url: `${root}/reopen`, params })
export const period = (params: object) =>
  request.get<any[]>({ url: '/hrm/attendance/statistics/period-record-list', params })
export const exportPeriod = (params: object) =>
  request.download({ url: '/hrm/attendance/statistics/period-record-export', params })
