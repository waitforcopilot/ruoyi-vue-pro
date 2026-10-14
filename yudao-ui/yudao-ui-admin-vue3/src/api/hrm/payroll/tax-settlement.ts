import request from '@/config/axios'
export const balances = (filingId: number) =>
  request.get({ url: '/hrm/payroll/tax-settlement/balances', params: { filingId } })
export const list = (filingId: number) =>
  request.get({ url: '/hrm/payroll/tax-settlement/list', params: { filingId } })
export const submit = (data: any) =>
  request.post({ url: '/hrm/payroll/tax-settlement/submit', data })
export const review = (id: number, approve: boolean, reason: string) =>
  request.post({ url: '/hrm/payroll/tax-settlement/review', params: { id, approve, reason } })
export const confirm = (data: any) =>
  request.post({ url: '/hrm/payroll/tax-settlement/confirm', data })
export const cancel = (id: number, reason: string, bankVoidEvidence?: string) =>
  request.post({
    url: '/hrm/payroll/tax-settlement/cancel',
    params: { id, reason, bankVoidEvidence }
  })
