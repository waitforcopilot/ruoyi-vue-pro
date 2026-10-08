import request from '@/config/axios'
import type { Check } from '@/api/hrm/payroll/trial'

export interface BatchRow {
  id: number
  code: string
  title: string
  entityCode: string
  entityName: string
  periodStart: string
  periodEnd: string
  status: number
  revision: number
  personCount: number
  ownerName?: string
  stage: string
  assignedToMe: boolean
}
export interface Overview {
  generatedAt: string
  total: number
  states: Record<number, number>
  assignedHr: number
  assignedFinance: number
  batches: { list: BatchRow[]; total: number }
}
export interface Detail {
  batch: BatchRow
  availability: 'NONE' | 'UNAVAILABLE' | 'INVALIDATED' | 'SOURCE_CHANGED' | 'CURRENT'
  runId?: number
  runVersion?: number
  executedByName?: string
  executedAt?: string
  includedCount?: number
  excludedCount?: number
  amounts?: Record<string, string>
  check: Check
}
const root = '/hrm/payroll/overview'
export const page = (params: object): Promise<Overview> =>
  request.get({ url: root + '/page', params })
export const batch = (id: number): Promise<Detail> =>
  request.get({ url: root + '/batch', params: { id } })
