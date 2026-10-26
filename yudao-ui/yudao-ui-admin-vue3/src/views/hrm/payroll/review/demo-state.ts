// Pure TEST page state. No API, storage, employee file or external delivery side effects.
import type { PayrollModule } from '../requirements'

export interface DemoContext {
  projectId?: number
  module: PayrollModule
  period: string
  scenario: string
}
export interface DemoState {
  context: DemoContext
  stage: 'READY' | 'CALCULATED' | 'REVIEWED'
  notice: string
}
export type DemoAction = 'CALCULATE' | 'REVIEW'
export const demoContextKey = (c: DemoContext) =>
  JSON.stringify([c.projectId ?? null, c.module, c.period, c.scenario])
export function createDemoState(context: DemoContext): DemoState {
  return { context: { ...context }, stage: 'READY', notice: '示例尚未执行；不代表真实薪酬状态' }
}
export function inDemoContext(state: DemoState, context: DemoContext): DemoState {
  return demoContextKey(state.context) === demoContextKey(context)
    ? state
    : createDemoState(context)
}
export function transitionDemo(
  state: DemoState,
  context: DemoContext,
  action: DemoAction
): DemoState {
  const current = inDemoContext(state, context)
  if (!context.projectId || context.module !== 'CALC') {
    return { ...current, notice: '请在已选项目的工资核算视图体验试算和复核。' }
  }
  if (action === 'CALCULATE') {
    return {
      ...current,
      stage: 'CALCULATED',
      notice: '示例试算完成；重算后须重新复核。仅页面内存，无正式业务写入。'
    }
  }
  if (current.stage !== 'CALCULATED') {
    return {
      ...current,
      notice:
        current.stage === 'REVIEWED'
          ? '当前示例已经复核；如需再次复核，请先重算。'
          : '请先完成本项目、本视图示例试算，再演示复核。'
    }
  }
  return { ...current, stage: 'REVIEWED', notice: '演示复核完成，仅页面内存状态；未提交正式审批。' }
}
export const qualityLabel = (value: boolean | undefined) =>
  value === true ? '通过' : value === false ? '未通过或待核验' : '未知（接口未返回）'
