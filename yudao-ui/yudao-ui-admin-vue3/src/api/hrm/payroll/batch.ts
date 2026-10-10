import request from '@/config/axios'
import type { SalaryMonthRecordVO } from '@/api/hrm/salary/month-record'

export interface PayrollRun {
  id: number
  version: number
  computedBy: number
  createTime: string
  inputSnapshot: string
  ruleSnapshot: string
  resultSnapshot: string
}
export interface BatchEvent {
  id: number
  runId: number
  action: string
  fromStatus: number
  toStatus: number
  actorId: number
  reason: string
  createTime: string
}
export const page = (params: any) =>
  request.get<{ list: SalaryMonthRecordVO[]; total: number }>({
    url: '/hrm/payroll/batch/page',
    params
  })
export const versions = (batchId: number) =>
  request.get<PayrollRun[]>({ url: '/hrm/payroll/batch/versions', params: { batchId } })
export const events = (batchId: number) =>
  request.get<BatchEvent[]>({ url: '/hrm/payroll/batch/events', params: { batchId } })
export const transition = (action: string, batchId: number, runId: number, reason: string) =>
  request.post({ url: `/hrm/payroll/batch/transition/${action}`, data: { batchId, runId, reason } })
export const statuses: Record<number, string> = {
  5: '草稿',
  10: '已归档',
  11: '已试算',
  12: '待 HR 复核',
  13: '待财务审批',
  14: '审批通过',
  15: '已冻结',
  16: '发放中',
  17: '已发放'
}
