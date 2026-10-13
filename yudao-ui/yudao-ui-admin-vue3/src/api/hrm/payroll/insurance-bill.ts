import request from '@/config/axios'
const root = '/hrm/payroll/insurance-bill'
export const list = (entityId: number) =>
  request.get<any[]>({ url: `${root}/list`, params: { entityId } })
export const get = (id: number) => request.get<any>({ url: `${root}/get`, params: { id } })
export const lines = (id: number) => request.get<any[]>({ url: `${root}/lines`, params: { id } })
export const settlements = (id: number) =>
  request.get<any[]>({ url: `${root}/settlements`, params: { id } })
export const preview = async (metadata: Record<string, any>, file: File) => {
  const data = new FormData()
  Object.entries(metadata).forEach(([k, v]) => data.append(k, String(v)))
  data.append('file', file)
  const result = await request.upload<{ data: any }>({ url: `${root}/preview`, data })
  return result.data
}
export const action = (action: string, id: number, reason?: string) =>
  request.post({ url: `${root}/${action}`, params: { id, reason } })
export const propose = async (metadata: Record<string, any>, proof: File) => {
  const data = new FormData()
  data.append('metadata', JSON.stringify(metadata))
  data.append('proof', proof)
  const result = await request.upload<{ data: number }>({ url: `${root}/settlement`, data })
  return result.data
}
export const template = (entityId: number) =>
  request.download({ url: `${root}/template`, params: { entityId } })
export const errors = (id: number) => request.download({ url: `${root}/errors`, params: { id } })
export const exportBill = (id: number) =>
  request.download({ url: `${root}/export`, params: { id } })
export const proof = (id: number) =>
  request.download({ url: `${root}/settlement/proof`, params: { id } })

export const revalidate = (id: number) =>
  request.post<any>({ url: `${root}/revalidate`, params: { id } })
