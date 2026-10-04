import request from '@/config/axios'
import type { History } from '@/api/hrm/payroll/requirements'
export interface Parameter {
  key: string
  label: string
  type: 'TEXT' | 'INTEGER' | 'DECIMAL' | 'BOOLEAN' | 'DATE'
  value?: string
  unit?: string
  scale?: number
}
export interface BusinessCase {
  title: string
  inputJson: string
  expectedResult: string
}
export interface Rule {
  id?: number
  revision?: number
  code: string
  title: string
  category: string
  questionCode?: string
  ruleVersion?: number
  status?: number
  builtIn?: boolean
  ownerName?: string
  scopeCode?: string
  applicableScope?: string
  effectiveFrom?: string
  effectiveTo?: string
  definition?: string
  reference?: string
  parameters: Parameter[]
  cases: BusinessCase[]
  parameterCount?: number
  caseCount?: number
  reviewedByName?: string
  reviewedTime?: number
  evidence?: string
}
export const categories = [
  { code: 'PERIOD', label: '周期与截止' },
  { code: 'PAYROLL', label: '薪资方案' },
  { code: 'ATTENDANCE', label: '考勤扣款' },
  { code: 'OVERTIME', label: '加班补休' },
  { code: 'HOURS', label: '工时制度' },
  { code: 'INSURANCE', label: '社保公积金' },
  { code: 'TAX', label: '税务口径' },
  { code: 'ROUNDING', label: '精度舍入' },
  { code: 'LIFECYCLE', label: '核算流程' },
  { code: 'SLIP', label: '工资条' },
  { code: 'ACCESS', label: '授权与保留' },
  { code: 'PAYMENT', label: '代发回盘' },
  { code: 'COST', label: '成本与预算' },
  { code: 'OTHER', label: '其他' }
]
export const states = ['待确认草稿', '口径已确认', '已停用']
const url = '/hrm/payroll/rules'
export const initialize = () => request.post<number>({ url: url + '/initialize' })
export const page = (params: object) =>
  request.get<{ list: Rule[]; total: number }>({ url: url + '/page', params })
export const get = (id: number) => request.get<Rule>({ url: url + '/get', params: { id } })
export const create = (data: Rule) => request.post<number>({ url: url + '/create', data })
export const update = (data: Rule) => request.put({ url: url + '/update', data })
export const newVersion = (id: number, revision: number) =>
  request.post<number>({ url: url + '/new-version', params: { id, revision } })
export const review = (data: { id: number; revision: number; action: string; evidence: string }) =>
  request.post({ url: url + '/review', data })
export const history = (id: number) =>
  request.get<History[]>({ url: url + '/history', params: { id } })
