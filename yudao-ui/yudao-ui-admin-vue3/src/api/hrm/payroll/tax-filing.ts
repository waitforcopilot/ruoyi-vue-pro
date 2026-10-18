import request from '@/config/axios'
const url = '/hrm/payroll/tax-filing'
export const list = (params: object) => request.get<any[]>({ url: `${url}/list`, params })
export const create = (data: object) => request.post<number>({ url: `${url}/create`, data })
interface ReviewParams {
  id: number
  approved: boolean
  reason: string
}
export const review = (params: ReviewParams) => request.post({ url: `${url}/review`, params })
export const detail = (id: number) => request.get<any>({ url: `${url}/detail`, params: { id } })
export const exportFiling = (id: number) =>
  request.download({ url: `${url}/export`, params: { id } })
export const filed = (params: object) => request.post({ url: `${url}/filed`, params })
export const receipt = (data: object) => request.post({ url: `${url}/receipt`, data })
export const reviewReceipt = (params: ReviewParams) =>
  request.post({ url: `${url}/receipt-review`, params })
export const reconcile = (params: { id: number; reason: string }) =>
  request.post({ url: `${url}/reconcile`, params })
export const receiptTemplate = () => request.download({ url: `${url}/receipt-template` })
export const exportReceipts = (id: number) =>
  request.download({ url: `${url}/receipt-export`, params: { id } })
export const importReceipts = (id: number, file: File) => {
  const data = new FormData()
  data.append('file', file)
  data.append('id', String(id))
  return request.upload({ url: `${url}/receipt-import`, data })
}
