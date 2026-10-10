import request from '@/config/axios'

// MES 产品收货单明细 VO
export interface WmProductRecptDetailVO {
  id: number
  lineId: number
  recptId: number
  itemId: number
  itemCode: string
  itemName: string
  specification: string
  unitMeasureName: string
  quantity: number
  batchId: number
  warehouseId: number
  warehouseName: string
  locationId: number
  locationName: string
  areaId: number
  areaName: string
  remark: string
  createTime: string
}

// MES 产品收货单明细 API
export const WmProductRecptDetailApi = {
  // 查询产品收货单明细详情
  getProductRecptDetail: async (id: number) => {
    return await request.get({ url: '/mes/wm/product-recpt-detail/get?id=' + id })
  },

  // 查询产品收货单明细列表（按行编号）
  getProductRecptDetailListByLineId: async (lineId: number) => {
    return await request.get({ url: '/mes/wm/product-recpt-detail/list-by-line?lineId=' + lineId })
  },

  // 新增产品收货单明细
  createProductRecptDetail: async (data: WmProductRecptDetailVO) => {
    return await request.post({ url: '/mes/wm/product-recpt-detail/create', data })
  },

  // 修改产品收货单明细
  updateProductRecptDetail: async (data: WmProductRecptDetailVO) => {
    return await request.put({ url: '/mes/wm/product-recpt-detail/update', data })
  },

  // 删除产品收货单明细
  deleteProductRecptDetail: async (id: number) => {
    return await request.delete({ url: '/mes/wm/product-recpt-detail/delete?id=' + id })
  }
}

// Upstream receipt APIs coexist with legacy recpt APIs.


// MES 产品入库单明细 VO
export interface WmProductReceiptDetailVO {
  id: number
  lineId: number
  receiptId: number
  itemId: number
  itemCode: string
  quantity: number
  batchId: number
  warehouseId: number
  warehouseName: string
  locationId: number
  locationName: string
  areaId: number
  areaName: string
  remark: string
  createTime: string
}

// MES 产品入库单明细 API
export const WmProductReceiptDetailApi = {
  // 查询产品入库单明细列表
  getProductReceiptDetailList: async (params: any) => {
    return await request.get({ url: '/mes/wm/product-receipt-detail/list', params })
  },

  // 根据行项目ID查询产品入库单明细列表
  getProductReceiptDetailListByLineId: async (lineId: number) => {
    return await request.get({
      url: '/mes/wm/product-receipt-detail/list-by-line',
      params: { lineId }
    })
  },

  // 查询产品入库单明细详情
  getProductReceiptDetail: async (id: number) => {
    return await request.get({ url: '/mes/wm/product-receipt-detail/get?id=' + id })
  },

  // 新增产品入库单明细
  createProductReceiptDetail: async (data: WmProductReceiptDetailVO) => {
    return await request.post({ url: '/mes/wm/product-receipt-detail/create', data })
  },

  // 修改产品入库单明细
  updateProductReceiptDetail: async (data: WmProductReceiptDetailVO) => {
    return await request.put({ url: '/mes/wm/product-receipt-detail/update', data })
  },

  // 删除产品入库单明细
  deleteProductReceiptDetail: async (id: number) => {
    return await request.delete({ url: '/mes/wm/product-receipt-detail/delete?id=' + id })
  }
}
