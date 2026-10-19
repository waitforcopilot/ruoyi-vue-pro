import request from '@/config/axios'
const root = '/hrm/payroll/governance'
export const get = (path: string, params?: any) => request.get<any>({ url: root + path, params })
export const post = (path: string, data: any) => request.post<any>({ url: root + path, data })
export const download = (params: any) =>
  request.download({ url: root + '/export/download', params })
