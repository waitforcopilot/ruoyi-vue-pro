import request from '@/config/axios'
export interface ModuleCount {
  code: string
  name: string
  total: number
  pending: number
  confirmed: number
  disputed: number
  inScope: number
  inScopeUnconfirmed: number
  undecided: number
}
export interface SourceState {
  code: string
  name: string
  readiness: number
}
export interface Batch {
  id: number
  sourceCode: string
  contractVersion: number
  periodStart: string
  periodEnd: string
  rowCount: number
  validCount: number
  errorCount: number
  status: number
}
export interface Section {
  authorized: boolean
  counts: Record<string, number> | null
  modules?: ModuleCount[] | null
  sources?: SourceState[] | null
  recentBatches?: Batch[] | null
}
export interface Summary {
  generatedAt: string
  requirements: Section
  sources: Section
  contracts: Section
  rules: Section
  batches: Section
}
export const summary = () => request.get<Summary>({ url: '/hrm/payroll/preparation/summary' })
