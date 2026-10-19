import request from '@/config/axios'
export const reviewPage = (params: any) =>
  request.get<any>({ url: '/hrm/payroll/review/page', params })
export const reviewStatistics = (params: any) =>
  request.get<any>({ url: '/hrm/payroll/review/statistics', params })
export const reviewExport = (params: any) =>
  request.get<any>({ url: '/hrm/payroll/review/export', params })
export const scopeDecision = (data: any) =>
  request.post({ url: '/hrm/payroll/review/decision', data })
export const saveImplementation = (data: any) =>
  request.post({ url: '/hrm/payroll/review/implementation', data })
export const saveCollaboration = (data: any) =>
  request.post({ url: '/hrm/payroll/review/collaboration', data })
export const batchAssign = (data: any) =>
  request.post<any[]>({ url: '/hrm/payroll/review/batch-assign', data })

export const reviewReminders = (projectId: number) =>
  request.get<any[]>({ url: '/hrm/payroll/review/reminders', params: { projectId } })
