import request from '@/config/axios'

export interface Input {
  key: string
  label: string
  type: string
  unit: string
  scale?: number | null
}
export interface Item {
  key: string
  label: string
  unit: string
  expression: string
  amountScale?: number | null
  roundingMode: string
}
export interface BusinessCase {
  title: string
  inputs: Record<string, string>
  expected: Record<string, string>
}
export interface Program {
  inputs: Input[]
  items: Item[]
  cases: BusinessCase[]
  divisionScale?: number | null
  divisionRoundingMode?: string | null
}
export interface Cases {
  programHash: string
  total: number
  passed: number
  allPassed: boolean
  cases: {
    title: string
    passed: boolean
    error?: string
    expected: Record<string, string>
    actual: Record<string, string>
  }[]
}
export interface Definition {
  id?: number
  revision?: number
  code: string
  title: string
  scopeCode: string
  ownerName?: string
  applicableScope?: string
  description?: string
  reference?: string
  effectiveFrom?: string
  effectiveTo?: string
  definitionVersion?: number
  schemaVersion?: number
  status?: number
  inputCount?: number
  itemCount?: number
  caseCount?: number
  program?: Program
  verifiedCases?: Cases
  reviewedByName?: string
  reviewedTime?: number
  evidence?: string
}
export interface Result {
  key: string
  label: string
  unit: string
  expression: string
  dependencies: string[]
  rawResult: string
  amount: string
  amountScale: number
  roundingMode: string
  steps: string[]
}
export interface Preview {
  definition: Definition
  programHash: string
  start: string
  end: string
  result: { inputs: Record<string, string>; items: Result[] }
  explanation: string
}
export interface Comparison {
  left: Definition
  right: Definition
  changes: { path: string; label: string; left?: string; right?: string; kind: string }[]
}
export const states = ['草稿', '已确认', '已停用']
export const roundings: Record<string, string> = {
  HALF_UP: '四舍五入',
  HALF_EVEN: '四舍六入、五成双',
  DOWN: '向零舍入',
  UP: '远离零舍入'
}
const root = '/hrm/payroll/calculation-definitions'
export const page = (params: object): Promise<{ list: Definition[]; total: number }> =>
  request.get({ url: root + '/page', params })
export const get = (id: number): Promise<Definition> =>
  request.get({ url: root + '/get', params: { id } })
export const create = (data: Definition): Promise<number> =>
  request.post({ url: root + '/create', data })
export const update = (data: Definition) => request.put({ url: root + '/update', data })
export const newVersion = (id: number, revision: number): Promise<number> =>
  request.post({ url: root + '/new-version', params: { id, revision } })
export const review = (data: { id: number; revision: number; action: string; evidence: string }) =>
  request.post({ url: root + '/review', data })
export const cases = (id: number): Promise<Cases> =>
  request.get({ url: root + '/cases', params: { id } })
export const history = (id: number) => request.get({ url: root + '/history', params: { id } })
export const compare = (leftId: number, rightId: number): Promise<Comparison> =>
  request.get({ url: root + '/compare', params: { leftId, rightId } })
export const preview = (data: {
  definitionId: number
  start: string
  end: string
  inputs: Record<string, string>
}): Promise<Preview> => request.post({ url: root + '/preview', data })

export const wageTemplate = (): Promise<Program> => request.get({ url: root + '/wage-template' })
