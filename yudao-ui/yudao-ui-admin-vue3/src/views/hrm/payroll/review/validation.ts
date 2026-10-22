/** UI guidance only. Authorization, versions and business gates remain server-owned. */
export const businessLabels: Record<string, string> = {
  role: '业务角色',
  scenario: '触发场景',
  goal: '业务目标',
  expected: '期望行为',
  normalCase: '正常样例',
  exceptionCase: '异常样例',
  calculation: '计算或业务规则',
  unit: '单位',
  precision: '精度与舍入',
  validity: '有效期或适用依据',
  ruleException: '规则例外与处理'
}
export function opinionMissing(row: any, business: Record<string, any>, confirming = false) {
  const missing: Record<string, string> = {}
  const need = (key: string, label: string, value: unknown) => {
    if (value == null || String(value).trim() === '') missing[key] = label
  }
  need('description', '功能说明', row.description)
  if (confirming || row.status === 'REVIEWING') {
    for (const key of confirming ? Object.keys(businessLabels) : ['scenario', 'expected'])
      need(key, businessLabels[key], business[key])
  }
  if (confirming || row.status === 'DISPUTED') {
    need('reviewerId', '评审负责人（请先分派）', row.reviewerId)
    need('evidence', '依据或异议说明', row.evidence)
  }
  if (row.status === 'DISPUTED') {
    need('disputeOwnerId', '异议责任人ID', business.disputeOwnerId)
    need('nextStep', '异议下一步', business.nextStep)
  }
  if (confirming) {
    need('ruleVersion', '规则引用版本', row.ruleVersion)
    need('acceptance', '验收条件', row.acceptance)
    if (row.sourceApplicability === 'NOT_APPLICABLE') {
      // Match validExemption on the server; incomplete reasons remain valid drafts.
      const reason = String(row.applicabilityReason ?? '').trim()
      if (reason.length < 8 || ['N/A', 'NA', '不适用'].includes(reason.toUpperCase()))
        missing.applicabilityReason = '不适用理由（至少8个字符，具体说明为何不消费业务数据）'
    }
  }
  return missing
}
export function failureGuidance(error: unknown) {
  const message = error instanceof Error ? error.message : typeof error === 'string' ? error : ''
  // Only display controlled field guidance; never echo arbitrary infrastructure errors.
  if (/^请补齐以下字段：|^请修正新期限：/.test(message)) return `${message}。输入已保留。`
  if (/版本|记录已被更新|范围.*变|范围.*失效/.test(message))
    return '当前版本或范围已变化。输入已保留；请读取最新版本，人工合并后重试。'
  if (/权限|本人|分派|确认人/.test(message))
    return '当前账号没有此操作权限，或不具备该动作所需的分派身份。输入已保留；请核对当前项目及分派。'
  return '请求未完成，输入已保留；请检查连接及当前业务状态后重试。'
}
