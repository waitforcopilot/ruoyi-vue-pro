import request from '@/config/axios'
export interface TaxCorrectionRequest {
  paymentId: number
  priorCorrectionId?: number
  income?: number
  socialDeduction?: number
  openingAlreadyIncludesCorrection?: boolean
  basicDeduction: number
  officialReference: string
  evidence: string
  reason: string
  requestKey: string
}
export const preview = (data: TaxCorrectionRequest) =>
  request.post({ url: '/hrm/payroll/tax-correction/preview', data })
export const submit = (data: TaxCorrectionRequest) =>
  request.post({ url: '/hrm/payroll/tax-correction/submit', data })
export const list = (paymentId: number) =>
  request.get({ url: '/hrm/payroll/tax-correction/list', params: { paymentId } })
export const review = (id: number, approve: boolean, reason: string) =>
  request.post({ url: '/hrm/payroll/tax-correction/review', params: { id, approve, reason } })
