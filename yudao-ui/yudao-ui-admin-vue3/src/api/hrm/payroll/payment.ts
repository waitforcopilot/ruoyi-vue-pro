import request from '@/config/axios'

export interface PaymentBatch {
  id: number
  title: string
  status: number
  employeeCount: number
  realPaySalary: number
  runId: number
}
export interface Payment {
  id: number
  employeeName: string
  bankAccount: string
  bankName: string
  amount: number
  status: string
  failureReason?: string
  attempt: number
}
export interface Column {
  field: string
  label: string
}
export interface BankTemplate {
  id?: number
  name: string
  columns: Column[] | string
  returnColumns: Column[] | string
  successValue: string
  failedValue: string
}
export const batches = (params: any) =>
  request.get<{ list: PaymentBatch[]; total: number }>({
    url: '/hrm/payroll/payment/batches',
    params
  })
export const list = (batchId: number) =>
  request.get<Payment[]>({ url: '/hrm/payroll/payment/list', params: { batchId } })
export const prepare = (batchId: number, runId: number) =>
  request.post({ url: '/hrm/payroll/payment/prepare', params: { batchId, runId } })
export const retry = (id: number) =>
  request.post({ url: '/hrm/payroll/payment/retry', params: { id } })
export const exportFile = (batchId: number, templateId?: number) =>
  request.download({ url: '/hrm/payroll/payment/export', params: { batchId, templateId } })
export const importTemplate = () =>
  request.download({ url: '/hrm/payroll/payment/import-template' })
export const reconcile = (data: FormData) =>
  request.upload({ url: '/hrm/payroll/payment/reconcile', data })
export const templates = () =>
  request.get<BankTemplate[]>({ url: '/hrm/payroll/payment/templates' })
export const createTemplate = (data: BankTemplate) =>
  request.post<number>({ url: '/hrm/payroll/payment/template', data })

export interface Difference {
  row: number
  paymentId: number
  reason: string
}
export interface Receipt {
  id: number
  fileName: string
  rowCount: number
  actorId: number
  createTime: string
}
export const validateFile = (data: FormData) =>
  request.upload<Difference[]>({ url: '/hrm/payroll/payment/validate', data })
export const receipts = (batchId: number) =>
  request.get<Receipt[]>({ url: '/hrm/payroll/payment/receipts', params: { batchId } })
