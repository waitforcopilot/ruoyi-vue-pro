export const labels: Record<string, string> = {
  PAYMENT_PLAN: '发放日期',
  SALARY_GROUP: '薪资组与计税配置',
  SALARY_INFO: '生效薪资',
  INSURANCE: '社保公积金',
  TAX_POLICY: '年度税务政策',
  TAX_PROFILE: '年度税务资料',
  ATTENDANCE: '考勤',
  OVERTIME: '工时与加班',
  DEDUCTIONS: '累计专项扣除',
  CUMULATIVE_TAX: '已发税务累计'
}
export const states: Record<string, string> = {
  OPEN: '待补正',
  SUBMITTED: '待独立复核',
  CLOSED: '已关闭'
}
export const actions: Record<string, string> = {
  CREATE: '创建',
  SUBMIT: '补充材料',
  CLOSE: '复核关闭',
  REJECT: '退回补正',
  REASSIGN: '责任人交接',
  REOPEN: '来源失效重开'
}
