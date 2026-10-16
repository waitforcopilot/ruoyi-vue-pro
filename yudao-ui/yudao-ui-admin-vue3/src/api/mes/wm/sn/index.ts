import request from '@/config/axios'

// MES SN 码 VO
export interface WmSnVO {
  id: number
  uuid: string
  code: string
  unitName: string
  itemId: number
  itemCode: string
  itemName: string
  specification: string
  batchCode: string
  workOrderId: number
  createTime: string
}

export interface WmSnGroupVO extends Omit<WmSnVO, 'id' | 'code'> {
  count: number
}

// MES SN 码生成 VO
export interface WmSnGenerateVO {
  itemId: number
  batchCode?: string
  workOrderId?: number
  count: number
}

// MES SN 码 API
export const WmSnApi = {
  // 生成 SN 码
  generateSnCodes: async (data: WmSnGenerateVO) => {
    return await request.post({ url: '/mes/wm/sn/generate', data })
  },

  // 查询 SN 码分页
  getSnPage: async (params: any) => {
    return await request.get({ url: '/mes/wm/sn/group-page', params })
  },

  getSnListByUuid: async (uuid: string): Promise<WmSnVO[]> => {
    return await request.get({ url: '/mes/wm/sn/list-by-uuid', params: { uuid } })
  },

  // 批量删除 SN 码
  deleteSnBatch: async (uuid: string) => {
    return await request.delete({ url: '/mes/wm/sn/delete-batch', params: { uuid } })
  },

  // 导出 SN 码 Excel
  exportSnExcel: async (params: any) => {
    return await request.download({ url: '/mes/wm/sn/group-export-excel', params })
  }
}
