<template>
  <ContentWrap>
    <el-alert
      title="核算、HR 复核和财务审批由不同人员执行。冻结后不可编辑；需修改时先解冻，再重新核算并提交审批。"
      :closable="false"
      type="info"
    />
    <el-form :inline="true" class="mt-4">
      <el-form-item label="状态"
        ><el-select v-model="query.status" clearable style="width: 180px"
          ><el-option
            v-for="(label, key) in api.statuses"
            :key="key"
            :value="Number(key)"
            :label="label" /></el-select
      ></el-form-item>
      <el-button @click="search">查询</el-button>
    </el-form>
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="entityName" label="法人主体" min-width="140"
        ><template #default="{ row }">{{
          row.entityName || (row.entityId ? `主体 ${row.entityId}` : '未归属')
        }}</template></el-table-column
      >
      <el-table-column prop="title" label="批次" min-width="180" />
      <el-table-column label="状态" width="150"
        ><template #default="{ row }">{{ api.statuses[row.status] }}</template></el-table-column
      >
      <el-table-column prop="expectedPaymentDate" label="计划发放日" width="130" />
      <el-table-column prop="employeeCount" label="人数" width="80" />
      <el-table-column prop="expectedPaySalary" label="应发" /><el-table-column
        prop="realPaySalary"
        label="实发"
      />
      <el-table-column label="操作" width="150"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="open(row)">审批与版本</el-button></template
        ></el-table-column
      >
    </el-table>
    <Pagination
      v-model:page="query.pageNo"
      v-model:limit="query.pageSize"
      :total="total"
      @pagination="load"
    />
  </ContentWrap>
  <Dialog v-model="visible" :title="selected?.title || '核算批次'" width="1000px">
    <template v-if="selected">
      <el-tag>{{ api.statuses[selected.status!] }}</el-tag>
      <el-space class="ml-4" wrap>
        <el-button
          v-for="action in availableActions"
          :key="action.code"
          v-hasPermi="['hrm:payroll:batch:' + action.code]"
          :disabled="
            !runs.length ||
            (['submit', 'review', 'approve', 'freeze'].includes(action.code) &&
              runs[0]?.expectedPaymentDate !== selected.expectedPaymentDate)
          "
          @click="prepare(action)"
          >{{ action.label }}</el-button
        >
      </el-space>
      <el-form v-if="[5, 11].includes(selected.status!)" inline class="mt-4">
        <el-form-item label="计划发放日期"
          ><el-date-picker
            v-model="plannedDate"
            value-format="YYYY-MM-DD"
            placeholder="明确计划日期"
        /></el-form-item>
        <el-button
          v-hasPermi="['hrm:salary:month-record:update']"
          :disabled="!plannedDate"
          :loading="savingPlan"
          @click="savePlan"
          >保存发放安排</el-button
        >
      </el-form>
      <el-alert
        class="mt-4"
        title="计划日期由主体维护人员确认，并保存到核算版本。调整日期后须重新核算，原版本保留；实际到账日期仍以银行回盘为准。"
        type="info"
        :closable="false"
      />
      <el-tabs class="mt-4">
        <el-tab-pane label="核算版本与差异">
          <el-select v-model="selectedRunId" style="width: 300px"
            ><el-option
              v-for="run in runs"
              :key="run.id"
              :value="run.id"
              :label="`版本 ${run.version} · ${formatDate(run.createTime)}`"
          /></el-select>
          <el-tag class="ml-4"
            >本版本计划发放日：{{
              runs.find((run) => run.id === selectedRunId)?.expectedPaymentDate || '尚未确认'
            }}</el-tag
          >
          <el-alert
            class="my-4"
            title="差异相对于上一个核算版本；首次核算显示完整结果。审批始终针对最新版本。"
            type="info"
            :closable="false"
          />
          <el-table :data="differenceRows">
            <el-table-column prop="employeeName" label="员工" /><el-table-column
              prop="expectedPaySalary"
              label="应发"
            /><el-table-column prop="realPaySalary" label="实发" />
            <el-table-column prop="delta" label="实发变动" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="来源占用与防重">
          <el-alert
            title="冻结时占用来源，解冻释放未发放占用；成功回盘保留已发放占用。计薪日期用于防重，具体工资金额见冻结版本。"
            type="info"
            :closable="false"
          />
          <el-table :data="sourceClaims">
            <el-table-column prop="runId" label="核算版本编号" width="130" />
            <el-table-column prop="employeeId" label="员工编号" width="100" />
            <el-table-column label="来源类型"
              ><template #default="{ row }">{{
                claimKinds[row.kind] || row.kind
              }}</template></el-table-column
            >
            <el-table-column label="来源编号或期间"
              ><template #default="{ row }">{{ claimPeriod(row) }}</template></el-table-column
            >
            <el-table-column label="来源金额"
              ><template #default="{ row }">{{
                row.amount == null ? '见核算版本' : row.amount
              }}</template></el-table-column
            >
            <el-table-column label="占用状态"
              ><template #default="{ row }">{{
                claimStatuses[row.status] || row.status
              }}</template></el-table-column
            >
            <el-table-column prop="releaseReason" label="释放依据" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="审批记录">
          <el-table :data="history"
            ><el-table-column label="时间" width="180"
              ><template #default="{ row }">{{
                formatDate(row.createTime)
              }}</template></el-table-column
            ><el-table-column prop="actorId" label="操作人" width="90" /><el-table-column
              label="操作"
              width="100"
              ><template #default="{ row }">{{
                actionNames[row.action] || row.action
              }}</template></el-table-column
            ><el-table-column label="状态变化" min-width="220"
              ><template #default="{ row }"
                >{{ api.statuses[row.fromStatus] }} → {{ api.statuses[row.toStatus] }}</template
              ></el-table-column
            ><el-table-column prop="reason" label="依据" min-width="220"
          /></el-table>
        </el-tab-pane>
      </el-tabs>
    </template>
  </Dialog>
  <Dialog v-model="actionVisible" :title="pendingAction?.label" width="500px">
    <el-input
      v-model="reason"
      type="textarea"
      :maxlength="1000"
      placeholder="填写审批意见或操作依据"
    />
    <template #footer
      ><el-button @click="actionVisible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="perform">提交</el-button></template
    >
  </Dialog>
</template>
<script setup lang="ts">
import { formatDate } from '@/utils/formatTime'
import * as api from '@/api/hrm/payroll/batch'
import type { SalaryMonthRecordVO } from '@/api/hrm/salary/month-record'
defineOptions({ name: 'HrmPayrollBatch' })
const message = useMessage()
const sourceClaims = ref<any[]>([])
const claimKinds: Record<string, string> = {
  TAX_YEAR: '年度预扣串行占用',
  WAGE_DAY: '计薪日期',
  OVERTIME: '认可加班',
  WORK_CYCLE: '综合工时周期',
  INSURANCE_MONTH: '月度缴费'
}
const claimStatuses: Record<string, string> = {
  RESERVED: '已冻结占用',
  PAID: '已对账发放',
  RELEASED: '已解冻释放'
}
const claimPeriod = (row: any) => {
  const parts = row.sourceKey.split(':')
  if (row.kind === 'TAX_YEAR') return `核算版本 ${row.runId} · 同一扣缴单位/纳税人/年度`
  return row.kind === 'OVERTIME' ? `审批 ${parts[1]} · ${parts[2]}` : parts.slice(1).join(' 至 ')
}
const query = reactive({ pageNo: 1, pageSize: 10, status: undefined as number | undefined })
const rows = ref<SalaryMonthRecordVO[]>([]),
  total = ref(0),
  loading = ref(false),
  visible = ref(false),
  saving = ref(false),
  actionVisible = ref(false)
const selected = ref<SalaryMonthRecordVO>(),
  runs = ref<api.PayrollRun[]>([]),
  history = ref<api.BatchEvent[]>([]),
  selectedRunId = ref<number>()
const plannedDate = ref<string>(),
  savingPlan = ref(false)
let detailSequence = 0
const actionNames: Record<string, string> = {
  SET_PAYMENT_PLAN: '调整计划发放日',
  submit: '提交复核',
  review: 'HR 复核通过',
  approve: '财务审批通过',
  reject: '退回',
  freeze: '冻结',
  unfreeze: '解冻',
  archive: '归档',
  pay: '开始发放',
  paid: '发放完成',
  retry: '重试失败项'
}
const actionsByStatus: Record<number, string[]> = {
  11: ['submit'],
  12: ['review', 'reject'],
  13: ['approve', 'reject'],
  14: ['freeze', 'reject'],
  15: ['unfreeze'],
  17: ['archive']
}
const availableActions = computed(() =>
  (actionsByStatus[selected.value?.status || 0] || []).map((code) => ({
    code,
    label: actionNames[code]
  }))
)
const pendingAction = ref<{ code: string; label: string }>(),
  reason = ref('')
const load = async () => {
  loading.value = true
  try {
    const data = await api.page(query)
    rows.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const search = async () => {
  query.pageNo = 1
  await load()
}
const savePlan = async () => {
  const batchId = selected.value?.id,
    date = plannedDate.value
  if (!batchId || !date) return
  const reason = await message.prompt('填写计划发放安排依据；日期变更后须重新核算')
  if (!reason || selected.value?.id !== batchId || plannedDate.value !== date) return
  savingPlan.value = true
  try {
    await api.paymentPlan(batchId, date, reason)
    message.success('已保存计划发放日期')
    await load()
    if (selected.value?.id === batchId) {
      const row = rows.value.find((item) => item.id === batchId)
      if (row) await open(row)
      else visible.value = false
    }
  } finally {
    savingPlan.value = false
  }
}
watch(visible, (shown) => {
  if (!shown) detailSequence++
})
const open = async (row: SalaryMonthRecordVO) => {
  const version = ++detailSequence
  plannedDate.value = row.expectedPaymentDate
  runs.value = []
  history.value = []
  sourceClaims.value = []
  selected.value = row
  const data = await Promise.all([
    api.versions(row.id!),
    api.events(row.id!),
    api.sourceClaims(row.id!)
  ])
  if (version !== detailSequence || selected.value?.id !== row.id) return
  runs.value = data[0]
  history.value = data[1]
  sourceClaims.value = data[2]
  selectedRunId.value = runs.value[0]?.id
  visible.value = true
}
const differenceRows = computed(() => {
  const index = runs.value.findIndex((r) => r.id === selectedRunId.value)
  if (index < 0) return []
  const current = JSON.parse(runs.value[index].resultSnapshot),
    previous = index + 1 < runs.value.length ? JSON.parse(runs.value[index + 1].resultSnapshot) : []
  const all = new Map<number, any>()
  for (const row of previous)
    all.set(row.employeeId, {
      ...row,
      expectedPaySalary: 0,
      realPaySalary: 0,
      delta: -Number(row.realPaySalary || 0)
    })
  for (const row of current) {
    const before = previous.find((item: any) => item.employeeId === row.employeeId)
    all.set(row.employeeId, {
      ...row,
      delta: (Number(row.realPaySalary || 0) - Number(before?.realPaySalary || 0)).toFixed(2)
    })
  }
  return [...all.values()]
})
const prepare = (action: { code: string; label: string }) => {
  pendingAction.value = action
  reason.value = ''
  actionVisible.value = true
}
const perform = async () => {
  if (!reason.value.trim()) {
    message.warning('请填写操作依据')
    return
  }
  saving.value = true
  try {
    await api.transition(
      pendingAction.value!.code,
      selected.value!.id!,
      runs.value[0].id,
      reason.value
    )
    actionVisible.value = false
    await load()
    const updated = rows.value.find((r) => r.id === selected.value!.id)
    if (updated) await open(updated)
    else visible.value = false
    message.success('已提交')
  } finally {
    saving.value = false
  }
}
onMounted(load)
</script>
