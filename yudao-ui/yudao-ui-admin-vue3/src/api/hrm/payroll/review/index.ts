import request from '@/config/axios'
export type Action =
  | 'submit'
  | 'approve'
  | 'reject'
  | 'cancel'
  | 'admin-cancel'
  | 'freeze'
  | 'unfreeze'
export const actionLabels: Record<Action, string> = {
  submit: '提交两级复核',
  approve: '通过本级复核',
  reject: '驳回本轮复核',
  cancel: '撤销本轮复核',
  'admin-cancel': '管理撤销复核',
  freeze: '冻结复核版本',
  unfreeze: '凭依据解冻'
}
export const cycleStates = ['复核中', '复核通过', '已驳回', '已撤销', '已冻结', '已解冻']
export interface Cycle {
  id: number
  runId: number
  cycleVersion: number
  status: number
  sourceHash: string
  processInstanceId: string
  processDefinitionId: string
  startedBy: number
  startedByName: string
  submitEvidence: string
  startedAt: string
  hrReviewerId: number
  hrReviewerName: string
  hrEvidence?: string
  hrReviewedAt?: string
  financeReviewerId: number
  financeReviewerName: string
  financeEvidence?: string
  financeReviewedAt?: string
  outcome?: string
  finishedAt?: string
  frozenBy?: number
  frozenByName?: string
  freezeEvidence?: string
  frozenAt?: string
  unfrozenBy?: number
  unfrozenByName?: string
  unfreezeEvidence?: string
  unfrozenAt?: string
}
export interface View {
  batchId: number
  revision: number
  batchStatus: number
  activeReviewId?: number
  frozenRunId?: number
  cycle?: Cycle
  cycles: Cycle[]
  tasks: { id: string; key: string; name: string; assigneeId?: number }[]
}
export interface Command {
  batchId: number
  runId: number
  revision: number
  requestKey: string
  action: Action
  evidence: string
  cycleId?: number
  taskId?: string
  hrReviewerId?: number
  financeReviewerId?: number
}
const root = '/hrm/payroll/trial-batches/review'
export const get = (batchId: number): Promise<View> =>
  request.get({ url: root + '/get', params: { batchId } })
export const action = (data: Command): Promise<View> =>
  request.post({ url: root + '/action', data })
export const sync = (batchId: number): Promise<View> =>
  request.post({ url: root + '/sync', params: { batchId } })
