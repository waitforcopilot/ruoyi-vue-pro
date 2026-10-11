import request from '@/config/axios'
export interface SalaryProposal {
  id: number
  employeeId: number
  requestSnapshot: string
  beforeSnapshot?: string
  status: string
  proposedBy?: number
  reviewedBy?: number
  approvedBy?: number
  reviewReason?: string
  approvalReason?: string
  changeRecordId?: number
  createTime: number
}
export const list = () =>
  request.get<SalaryProposal[]>({ url: '/hrm/payroll/salary-proposal/list' })
export const employees = () =>
  request.get<{ id: number; name: string; jobNumber: string }[]>({
    url: '/hrm/payroll/salary-proposal/employees'
  })
export const decide = (action: string, id: number, reason: string) =>
  request.post({ url: `/hrm/payroll/salary-proposal/${action}`, data: { id, reason } })
