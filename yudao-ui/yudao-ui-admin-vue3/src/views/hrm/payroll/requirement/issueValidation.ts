/** Check the raw text; date controls must not normalize invalid calendar input before this gate. */
export function validIssueDeadline(value: unknown) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const [year, month, day] = value.split('-').map(Number)
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  return month >= 1 && month <= 12 && day >= 1 && day <= days[month - 1]
}

/** Input guidance only; authorization and transitions remain server-owned. */
export function issueMissing(mode: string, draft: Record<string, any>) {
  const result: Record<string, string> = {}
  const need = (key: string, label: string, value: unknown) => {
    if (value == null || String(value).trim() === '') result[key] = label
  }
  const deadline = (label: string, value: unknown) => {
    need('dueDate', label, value)
    if (!result.dueDate && !validIssueDeadline(value))
      result.dueDate = `${label}（须为有效的YYYY-MM-DD日期）`
  }
  if (mode !== 'issue' && mode !== 'action') return result
  need('reason', '本次操作原因', draft.reason)
  if (mode === 'action') {
    need('evidence', '处理或复核依据', draft.evidence)
    if (draft.action === 'REOPEN') deadline('新期限', draft.dueDate)
  }
  const issue = mode === 'issue' ? draft.issue || {} : draft
  if (mode === 'issue' || draft.action === 'REASSIGN') {
    need('handlerId', '处理人', issue.handlerId)
    need('reviewerId', '复核人', issue.reviewerId)
    if (issue.handlerId != null && issue.handlerId === issue.reviewerId)
      result.reviewerId = '复核人（必须与处理人不同）'
  }
  if (mode === 'issue') {
    need('description', '问题描述', issue.description)
    deadline('处理期限', issue.dueDate)
    need('category', '问题类别', issue.category)
    if (
      !issue.wholeProject &&
      ![issue.requirementIds, issue.sourceIds, issue.relationIds].some((ids) => ids?.length)
    )
      result.requirementIds = '关联需求、来源或消费关系（至少一项）'
  }
  return result
}
