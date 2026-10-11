<template>
  <ContentWrap>
    <el-alert
      v-if="overdueCount"
      :title="`有 ${overdueCount} 项待处理工单已超过截止时间，请责任人提交依据或复核人完成处理。`"
      type="warning"
      :closable="false"
    />
    <el-form v-if="!isPortal" inline>
      <el-form-item label="员工"
        ><el-select v-model="employeeId" clearable filterable style="width: 240px"
          ><el-option
            v-for="e in employees"
            :key="e.id"
            :label="`${e.jobNumber} ${e.name}`"
            :value="e.id" /></el-select
      ></el-form-item>
      <el-form-item label="状态"
        ><el-select v-model="status" clearable style="width: 150px"
          ><el-option
            v-for="(label, key) in statuses"
            :key="key"
            :label="label"
            :value="key" /></el-select
      ></el-form-item>
      <el-button :loading="loading" @click="load">查询</el-button
      ><el-button v-hasPermi="['hrm:payroll:attendance:update']" @click="newIssue"
        >分配异常工单</el-button
      >
    </el-form>
    <el-button v-if="isPortal" @click="load">刷新我的工单</el-button>
    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="id" label="工单编号" width="95" /><el-table-column
        label="员工"
        width="150"
        ><template #default="{ row }">{{ row.employeeName || employeeName(row.employeeId) }}</template></el-table-column
      ><el-table-column prop="attendanceDate" label="业务日期" width="115" />
      <el-table-column label="异常" width="100"
        ><template #default="{ row }"
          >{{ types[row.type]
          }}{{ row.clockType ? (row.clockType === 1 ? '（上班）' : '（下班）') : '' }}</template
        ></el-table-column
      ><el-table-column label="状态" width="90"
        ><template #default="{ row }">{{ statuses[row.status] }}</template></el-table-column
      >
      <el-table-column label="责任人" width="100"
        ><template #default="{ row }">{{ row.ownerName || userName(row.ownerId) }}</template></el-table-column
      ><el-table-column label="复核人" width="100"
        ><template #default="{ row }">{{ row.reviewerName || userName(row.reviewerId) }}</template></el-table-column
      ><el-table-column prop="dueTime" label="处理截止" :formatter="formatTime" width="155" />
      <el-table-column
        prop="reviewOpinion"
        label="复核意见"
        show-overflow-tooltip
      /><el-table-column label="操作" width="240"
        ><template #default="{ row }"
          ><el-button link @click="showHistory(row.id)">履历</el-button
          ><el-button v-if="row.status === 'OPEN'" link @click="openSubmit(row)">提交依据</el-button
          ><template v-if="!isPortal"
            ><el-button
              v-if="row.status === 'SUBMITTED'"
              v-hasPermi="['hrm:payroll:attendance:review']"
              link
              @click="review(row, true)"
              >批准</el-button
            ><el-button
              v-if="row.status === 'SUBMITTED'"
              v-hasPermi="['hrm:payroll:attendance:review']"
              link
              @click="review(row, false)"
              >退回</el-button
            ><el-button
              v-if="row.status === 'APPROVED'"
              v-hasPermi="['hrm:payroll:attendance:review']"
              link
              @click="close(row)"
              >验证关闭</el-button
            ><el-button
              v-if="['APPROVED', 'CLOSED'].includes(row.status)"
              v-hasPermi="['hrm:payroll:attendance:review']"
              link
              @click="reopen(row)"
              >重开</el-button
            ></template
          ></template
        ></el-table-column
      >
    </el-table>
  </ContentWrap>
  <ContentWrap v-if="!isPortal">
    <el-form inline
      ><el-form-item label="实际计薪区间"
        ><el-date-picker
          v-model="period"
          type="daterange"
          value-format="YYYY-MM-DD" /></el-form-item
      ><el-button v-hasPermi="['hrm:attendance:statistics:query']" @click="loadPeriod"
        >考勤归集</el-button
      ><el-button v-hasPermi="['hrm:attendance:statistics:export']" @click="exportPeriod"
        >导出</el-button
      ></el-form
    >
    <el-table :data="periodRows"
      ><el-table-column prop="employeeName" label="员工" /><el-table-column
        prop="attendanceGroupName"
        label="考勤组" /><el-table-column prop="attendDays" label="应出勤天" /><el-table-column
        prop="actualDays"
        label="实际出勤天" /><el-table-column prop="leaveDays" label="请假天" /><el-table-column
        prop="misscardCount"
        label="缺卡次数" /><el-table-column prop="lateMinute" label="迟到分钟" /><el-table-column
        prop="earlyMinute"
        label="早退分钟" /><el-table-column prop="absenteeismDays" label="旷工天" /><el-table-column
        prop="attendanceDeductAmount"
        label="考勤扣款"
    /></el-table>
  </ContentWrap>
  <Dialog v-model="createVisible" title="分配真实考勤异常" width="650px"
    ><el-form label-width="140px">
      <el-form-item label="员工"
        ><el-select v-model="draft.employeeId" filterable
          ><el-option
            v-for="e in employees"
            :key="e.id"
            :label="`${e.jobNumber} ${e.name}`"
            :value="e.id" /></el-select></el-form-item
      ><el-form-item label="业务日期"
        ><el-date-picker v-model="draft.attendanceDate" value-format="YYYY-MM-DD" /></el-form-item
      ><el-form-item label="异常类型"
        ><el-select v-model="draft.type"
          ><el-option
            v-for="(label, key) in types"
            :key="key"
            :label="label"
            :value="key" /></el-select></el-form-item
      ><el-form-item v-if="draft.type === 'MISSCARD'" label="补卡类型"
        ><el-radio-group v-model="draft.clockType"
          ><el-radio :value="1">上班</el-radio><el-radio :value="2">下班</el-radio></el-radio-group
        ></el-form-item
      >
      <el-form-item label="责任人"
        ><el-select v-model="draft.ownerId" filterable
          ><el-option
            v-for="u in users"
            :key="u.id"
            :label="u.name"
            :value="u.id" /></el-select></el-form-item
      ><el-form-item label="HR 复核人"
        ><el-select v-model="draft.reviewerId" filterable
          ><el-option
            v-for="u in users"
            :key="u.id"
            :label="u.name"
            :value="u.id" /></el-select></el-form-item
      ><el-form-item label="处理截止"
        ><el-date-picker
          v-model="draft.dueTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item
      ><el-form-item label="来源或工单唯一编号"
        ><el-input v-model="draft.requestKey" maxlength="100"
      /></el-form-item>
      <el-alert
        title="服务端会核对真实考勤异常；责任人与复核人需不同。补卡保留原打卡，批准后才进入考勤。"
        type="info"
        :closable="false" /></el-form
    ><template #footer
      ><el-button @click="createVisible = false">取消</el-button
      ><el-button :loading="saving" type="primary" @click="create">创建</el-button></template
    ></Dialog
  >
  <Dialog v-model="submitVisible" title="提交补正依据"
    ><el-form label-width="130px"
      ><el-form-item v-if="selected?.type === 'MISSCARD'" label="经证明的打卡时间"
        ><el-date-picker
          v-model="submission.correctionTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item
      ><el-form-item label="主管或其他证明依据"
        ><el-input
          v-model="submission.evidence"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit /></el-form-item></el-form
    ><template #footer
      ><el-button @click="submitVisible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="submit">提交复核</el-button></template
    ></Dialog
  >
  <Dialog v-model="historyVisible" title="异常与证据履历" width="850px"
    ><el-table :data="events"
      ><el-table-column prop="action" label="动作" /><el-table-column
        prop="actorId"
        label="操作人编号"
      /><el-table-column prop="reason" label="依据与意见" /><el-table-column
        prop="createTime"
        label="时间"
        :formatter="formatTime"
        width="155"
      /><el-table-column label="快照"
        ><template #default="{ row }"
          ><el-button link @click="showSnapshot(row.snapshot)">查看</el-button></template
        ></el-table-column
      ></el-table
    ></Dialog
  >
  <Dialog v-model="snapshotVisible" title="保留快照">
    <pre style="white-space: pre-wrap; word-break: break-all">{{ snapshot }}</pre>
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/attendance'
import dayjs from 'dayjs'
import download from '@/utils/download'
const props = defineProps<{ isPortal?: boolean }>()
const message = useMessage(),
  loading = ref(false),
  saving = ref(false)
const rows = ref<any[]>([]),
  employees = ref<{ id: number; name: string; jobNumber: string }[]>([]),
  users = ref<{ id: number; name: string }[]>([])
const employeeId = ref<number>(),
  status = ref<string>()
const statuses: Record<string, string> = {
  OPEN: '待补正',
  SUBMITTED: '待HR复核',
  APPROVED: '待验证关闭',
  CLOSED: '已关闭'
}
const types: Record<string, string> = {
  MISSCARD: '缺卡',
  LATE: '迟到',
  EARLY: '早退',
  ABSENT: '旷工'
}
const overdueCount = computed(
  () => rows.value.filter((r) => r.status !== 'CLOSED' && dayjs(r.dueTime).isBefore(dayjs())).length
)
const formatTime = (_row: unknown, _column: unknown, value: any) =>
  value ? dayjs(value).format('YYYY-MM-DD HH:mm') : ''
const employeeName = (id: number) => employees.value.find((e) => e.id === id)?.name || `员工 ${id}`
const userName = (id: number) => users.value.find((u) => u.id === id)?.name || `用户 ${id}`
const createVisible = ref(false),
  submitVisible = ref(false),
  historyVisible = ref(false),
  snapshotVisible = ref(false),
  snapshot = ref('')
const draft = ref<any>({}),
  selected = ref<any>(),
  submission = ref<any>({}),
  events = ref<any[]>([])
const period = ref([
    dayjs().startOf('month').format('YYYY-MM-DD'),
    dayjs().endOf('month').format('YYYY-MM-DD')
  ]),
  periodRows = ref<any[]>([])
async function load() {
  loading.value = true
  try {
    rows.value = props.isPortal
      ? await api.mine()
      : await api.issues({ employeeId: employeeId.value, status: status.value })
  } finally {
    loading.value = false
  }
}
function newIssue() {
  draft.value = {
    employeeId: employeeId.value,
    attendanceDate: dayjs().subtract(1, 'day').format('YYYY-MM-DD'),
    type: 'MISSCARD',
    clockType: 1,
    dueTime: dayjs().add(2, 'day').hour(17).minute(0).second(0).format('YYYY-MM-DD HH:mm:ss'),
    requestKey: ''
  }
  createVisible.value = true
}
async function create() {
  saving.value = true
  try {
    await api.create(draft.value)
    createVisible.value = false
    message.success('工单已分配')
    await load()
  } finally {
    saving.value = false
  }
}
function openSubmit(row: any) {
  selected.value = row
  submission.value = {
    id: row.id,
    evidence: row.evidence || '',
    correctionTime: row.correctionTime
      ? dayjs(row.correctionTime).format('YYYY-MM-DD HH:mm:ss')
      : undefined
  }
  submitVisible.value = true
}
async function submit() {
  saving.value = true
  try {
    await api.submit(submission.value, props.isPortal)
    submitVisible.value = false
    message.success('已提交复核')
    await load()
  } finally {
    saving.value = false
  }
}
async function review(row: any, approve: boolean) {
  const { value } = await message.prompt(
    '请核实证明材料并填写复核意见',
    approve ? '批准补正' : '退回补正'
  )
  await api.review({ id: row.id, approve, reason: value })
  await load()
}
async function close(row: any) {
  const { value } = await message.prompt(
    '服务端将重新计算并验证异常消除，请填写关闭依据',
    '验证关闭'
  )
  await api.close({ id: row.id, reason: value })
  await load()
}
async function reopen(row: any) {
  const { value } = await message.prompt(
    '请填写重开依据。原批准补卡将标记无效，保留记录和履历；相关未冻结工资需重算。',
    '重开工单'
  )
  await api.reopen({ id: row.id, reason: value })
  await load()
}
async function showHistory(id: number) {
  events.value = await api.history(id, props.isPortal)
  historyVisible.value = true
}
function showSnapshot(value: string) {
  snapshot.value = JSON.stringify(JSON.parse(value), null, 2)
  snapshotVisible.value = true
}
function params() {
  if (!employeeId.value || period.value?.length !== 2) {
    message.warning('请选择员工和实际计薪区间')
    return
  }
  return { employeeIds: String(employeeId.value), from: period.value[0], to: period.value[1] }
}
async function loadPeriod() {
  const p = params()
  if (p) periodRows.value = await api.period(p)
}
async function exportPeriod() {
  const p = params()
  if (p) download.excel(await api.exportPeriod(p), '计薪期间考勤.xlsx')
}
onMounted(async () => {
  if (!props.isPortal)
    [employees.value, users.value] = await Promise.all([api.employees(), api.users()])
  await load()
})
</script>
