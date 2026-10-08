import request from '@/config/axios'
import type { Definition, Result } from '@/api/hrm/payroll/calculation'
import type { Eligibility } from '@/api/hrm/payroll/eligibility'
import type { History } from '@/api/hrm/payroll/requirements'
import type { Scheme } from '@/api/hrm/payroll/scheme'
export const roleLabels: Record<string, string> = {
  gross: '应发',
  deductions: '扣款',
  tax: '个税',
  net: '实发'
}
export const states = ['草稿', '已试算', '复核中', '复核通过', '已冻结']
export const changes: Record<string, string> = {
  ADDED: '新增人员',
  REMOVED: '移除人员',
  QUALIFICATION_CHANGED: '资格变化',
  AMOUNTS_CHANGED: '金额变化',
  INPUTS_CHANGED: '输入变化',
  SOURCE_CHANGED: '来源变化',
  UNCHANGED: '无变化'
}
export interface Person {
  employeeId: number
  employeeFingerprint?: string
  inputs: Record<string, string>
  inputReference?: string
  snapshotName?: string
  snapshotJobNumber?: string
  snapshotDeptId?: number
  snapshotUserId?: number
  snapshotCapturedAt?: string
}
export interface Batch {
  id?: number
  revision?: number
  code: string
  title: string
  entityCode: string
  entityName: string
  periodType: string
  periodStart: string
  periodEnd: string
  definitionId?: number
  schemeId?: number
  ownerName?: string
  reference?: string
  status?: number
  personCount?: number
  currentRunId?: number
  latestRunId?: number
  activeReviewId?: number
  frozenRunId?: number
  configuration: {
    roles: Record<string, string>
    people: Person[]
    sourceBindings?: SourceBinding[]
  }
}
export interface SourceBinding {
  inputKey: string
  sourceType: string
  optionId?: number
  unit: string
  reference: string
}
export interface Issue {
  code: string
  message: string
}
export interface Check {
  batchId: number
  revision: number
  ready: boolean
  sourceHash: string
  includedCount: number
  excludedCount: number
  blockedCount: number
  checkedAt: string
  issues: Issue[]
  people: {
    employeeId: number
    name: string
    state: string
    eligibilityId?: number
    eligibilityVersion?: number
    issues: Issue[]
  }[]
}
export interface Run {
  id: number
  batchId: number
  runVersion: number
  sourceHash: string
  expectedRevision: number
  includedCount: number
  excludedCount: number
  executedBy: number
  executedByName: string
  executedAt: string
  result?: {
    batch: Batch
    definition: Definition
    scheme?: Scheme
    programHash: string
    check: Check
    totals: Record<string, string>
    people: {
      input: Person
      eligibility?: Eligibility
      state: string
      amounts: Record<string, string>
      calculation?: { inputs: Record<string, string>; items: Result[] }
    }[]
  }
}
export interface Comparison {
  left: Run
  right: Run
  ruleChanged: boolean
  totalDifferences: Record<string, string>
  people: {
    employeeId: number
    name: string
    change: string
    leftAmounts?: Record<string, string>
    rightAmounts?: Record<string, string>
    differences?: Record<string, string>
    leftInputs?: Record<string, string>
    rightInputs?: Record<string, string>
  }[]
}
export interface Command {
  batchId: number
  revision: number
  sourceHash: string
  requestKey: string
}
const root = '/hrm/payroll/trial-batches'
export const page = (params: object): Promise<{ list: Batch[]; total: number }> =>
  request.get({ url: root + '/page', params })
export const get = (id: number): Promise<Batch> =>
  request.get({ url: root + '/get', params: { id } })
export const create = (data: Batch): Promise<number> =>
  request.post({ url: root + '/create', data })
export const update = (data: Batch) => request.put({ url: root + '/update', data })
export const check = (id: number): Promise<Check> =>
  request.get({ url: root + '/check', params: { id } })
export const execute = (data: Command): Promise<Run> =>
  request.post({ url: root + '/execute', data })
export const runs = (id: number): Promise<Run[]> =>
  request.get({ url: root + '/runs', params: { id } })
export const run = (id: number): Promise<Run> => request.get({ url: root + '/run', params: { id } })
export const compare = (leftId: number, rightId: number): Promise<Comparison> =>
  request.get({ url: root + '/compare', params: { leftId, rightId } })
export const history = (id: number): Promise<History[]> =>
  request.get({ url: root + '/history', params: { id } })
