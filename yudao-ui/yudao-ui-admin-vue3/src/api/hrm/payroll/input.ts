import request from '@/config/axios'
const root = '/hrm/payroll/input'
export const readiness = (batchId: number) =>
  request.get<any[]>({ url: `${root}/readiness`, params: { batchId } })
export const list = (batchId: number) =>
  request.get<any[]>({ url: `${root}/list`, params: { batchId } })
export const inspect = (data: object) => request.post<any>({ url: `${root}/inspect`, data })
export const submit = (data: object) => request.post<number>({ url: `${root}/submit`, data })
export const review = (params: object) => request.post({ url: `${root}/review`, params })
export const importList = (batchId: number) =>
  request.get<any[]>({ url: `${root}/imports`, params: { batchId } })
export const importDetails = (id: number) =>
  request.get<any>({ url: `${root}/import`, params: { id } })
export const importPreview = async (batchId: number, file: File) => {
  const data = new FormData()
  data.append('batchId', String(batchId))
  data.append('file', file)
  const result = await request.upload<{ data: any }>({ url: `${root}/import-preview`, data })
  return result.data
}
export const importCommit = (id: number) =>
  request.post<number[]>({ url: `${root}/import-commit`, params: { id } })
export const importRevalidate = (id: number) =>
  request.post<any>({ url: `${root}/import-revalidate`, params: { id } })
export const importErrors = (id: number) =>
  request.download({ url: `${root}/import-errors`, params: { id } })
export const template = (params: object) => request.download({ url: `${root}/template`, params })
