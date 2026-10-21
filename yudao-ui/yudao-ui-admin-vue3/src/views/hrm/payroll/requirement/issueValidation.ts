/** Input guidance only; authorization and transitions remain server-owned. */
export function issueMissing(mode: string, draft: Record<string, any>) {
  const result: Record<string, string> = {}
  const need = (key: string, label: string, value: unknown) => {
    if (value == null || String(value).trim() === '') result[key] = label
  }
  if (mode !== 'issue' && mode !== 'action') return result
  need('reason', '本次操作原因', draft.reason)
  if (mode === 'action') {
    need('evidence', '处理或复核依据', draft.evidence)
    if (draft.action === 'REOPEN') need('dueDate', '新期限', draft.dueDate)
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
    need('dueDate', '期限', issue.dueDate)
    need('category', '问题类别', issue.category)
    if (
      !issue.wholeProject &&
      ![issue.requirementIds, issue.sourceIds, issue.relationIds].some((ids) => ids?.length)
    )
      result.requirementIds = '关联需求、来源或消费关系（至少一项）'
  }
  return result
}
