<template>
  <ContentWrap>
    <RequirementContext module="OV" />
    <el-form inline>
      <el-form-item label="法人主体"
        ><el-select v-model="entityId" @change="reset"
          ><el-option v-for="e in entities" :key="e.id" :value="e.id" :label="e.name" /></el-select
      ></el-form-item>
      <el-form-item label="计薪年度"
        ><el-input-number v-model="year" :min="2019" :max="2099" :precision="0"
      /></el-form-item>
      <el-form-item label="月份"
        ><el-select v-model="month" clearable style="width: 100px"
          ><el-option v-for="m in 12" :key="m" :value="m" :label="`${m}月`" /></el-select
      ></el-form-item>
      <el-form-item label="批次类型"
        ><el-select v-model="type" clearable style="width: 130px"
          ><el-option value="REGULAR" label="常规工资" /><el-option
            value="SUPPLEMENT"
            label="独立补发" /></el-select
      ></el-form-item>
      <el-form-item label="状态"
        ><el-select v-model="status" clearable style="width: 150px"
          ><el-option
            v-for="(name, key) in states"
            :key="key"
            :value="Number(key)"
            :label="name" /></el-select
      ></el-form-item>
      <el-button type="primary" :disabled="!entityId" :loading="loading" @click="load"
        >查询历史</el-button
      >
    </el-form>
    <el-alert
      title="金额按计薪批次及所选人员汇总；到账金额来自该批次成功回盘，期间归属不代表实际到账月。试算金额以当前显示版本为准。"
      type="info"
      :closable="false"
    />
    <el-table
      class="mt-4"
      :data="rows"
      v-loading="loading"
      highlight-current-row
      @row-click="select"
    >
      <el-table-column prop="title" label="批次" min-width="180" /><el-table-column
        prop="batchId"
        label="编号"
        width="85"
      />
      <el-table-column label="计薪年月"
        ><template #default="{ row }"
          >{{ row.year }}-{{ String(row.month).padStart(2, '0') }}</template
        ></el-table-column
      >
      <el-table-column label="类型"
        ><template #default="{ row }">{{
          row.parentBatchId ? '独立补发' : '常规工资'
        }}</template></el-table-column
      >
      <el-table-column label="状态"
        ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
      >
      <el-table-column prop="version" label="核算版本" /><el-table-column label="应发工资"
        ><template #default="{ row }">{{ money(row.gross) }}</template></el-table-column
      ><el-table-column label="实发工资"
        ><template #default="{ row }">{{ money(row.net) }}</template></el-table-column
      >
      <el-table-column label="批准总成本"
        ><template #default="{ row }">{{ money(row.approvedCost) }}</template></el-table-column
      ><el-table-column prop="openIssues" label="未关闭工单" /><el-table-column
        label="查看"
        width="90"
        ><template #default="{ row }"
          ><el-button link @click.stop="select(row)">总览明细</el-button></template
        ></el-table-column
      >
    </el-table>
  </ContentWrap>
  <ContentWrap v-if="detail" v-loading="detailLoading">
    <h3
      >{{ detail.summary.title }} · 版本 {{ detail.summary.version || '未知' }} ·
      {{ detail.summary.sourceMode === 'FROZEN_SNAPSHOT' ? '冻结证据' : '当前检查与试算' }}</h3
    >
    <el-select
      v-model="employeeIds"
      multiple
      filterable
      clearable
      collapse-tags
      collapse-tags-tooltip
      placeholder="全部计薪人员"
      style="width: 420px"
      ><el-option
        v-for="e in detail.employees"
        :key="e.id"
        :value="e.id"
        :label="`${e.name} · ${e.jobNumber || e.id}`" /></el-select
    ><el-button class="ml-2" @click="loadDetail">应用人员范围</el-button>
    <el-alert class="my-4" :title="detail.summary.sourceStatus" type="info" :closable="false" />
    <el-descriptions :column="3" border>
      <el-descriptions-item label="核算版本编号">{{
        detail.summary.runId || '未知'
      }}</el-descriptions-item
      ><el-descriptions-item label="所选人数">{{
        detail.summary.employeeCount ?? '未知'
      }}</el-descriptions-item
      ><el-descriptions-item label="计划发放日期">{{
        detail.summary.expectedPaymentDate || '未确认'
      }}</el-descriptions-item>
      <el-descriptions-item v-for="(label, key) in amounts" :key="key" :label="label">{{
        money(detail.summary[key])
      }}</el-descriptions-item>
      <el-descriptions-item label="成功 / 待处理 / 失败"
        >{{ detail.summary.paymentSuccess ?? '未知' }} /
        {{ detail.summary.paymentPending ?? '未知' }} /
        {{ detail.summary.paymentFailed ?? '未知' }}</el-descriptions-item
      ><el-descriptions-item label="未关闭工单">{{
        detail.summary.openIssues
      }}</el-descriptions-item
      ><el-descriptions-item label="成本报告编号">{{
        detail.summary.costReportId || '未知'
      }}</el-descriptions-item>
    </el-descriptions>
    <el-alert
      class="my-4"
      :title="detail.summary.costStatus"
      :type="detail.summary.approvedCost == null ? 'warning' : 'success'"
      :closable="false"
    />
    <h4
      >{{ detail.summary.sourceMode === 'FROZEN_SNAPSHOT' ? '保存证据' : '当前来源就绪' }}：{{
        detail.readyCount
      }}
      / {{ detail.signalCount }}</h4
    >
    <el-form inline
      ><el-form-item label="检查项目"
        ><el-select v-model="kind" clearable
          ><el-option
            v-for="(label, key) in allLabels"
            :key="key"
            :value="key"
            :label="label" /></el-select></el-form-item
      ><el-checkbox v-model="onlyMissing">只看缺口或未知</el-checkbox></el-form
    >
    <el-table :data="visibleSignals"
      ><el-table-column label="员工"
        ><template #default="{ row }">{{
          row.employeeName || `员工 #${row.employeeId}`
        }}</template></el-table-column
      ><el-table-column prop="jobNumber" label="工号" /><el-table-column label="检查项目"
        ><template #default="{ row }">{{
          allLabels[row.kind] || row.kind
        }}</template></el-table-column
      ><el-table-column label="状态" width="140"
        ><template #default="{ row }"
          ><el-tag :type="row.ready ? 'success' : 'warning'">{{
            signalLabel(row)
          }}</el-tag></template
        ></el-table-column
      ><el-table-column prop="reason" label="结论" min-width="320" /><el-table-column
        prop="sourceId"
        label="来源编号"
    /></el-table>
  </ContentWrap>
</template>
<script setup lang="ts">
import RequirementContext from '../components/RequirementContext.vue'
import request from '@/config/axios'
import { labels } from '../input-issue/labels'
defineOptions({ name: 'HrmPayrollOverview' })
const base = '/hrm/payroll/overview',
  entities = ref<any[]>([]),
  entityId = ref<number>(),
  year = ref(new Date().getFullYear()),
  month = ref<number>(),
  type = ref<string>(),
  status = ref<number>(),
  rows = ref<any[]>([]),
  loading = ref(false),
  detailLoading = ref(false),
  detail = ref<any>(),
  batchId = ref<number>(),
  employeeIds = ref<number[]>([]),
  kind = ref<string>(),
  onlyMissing = ref(false)
const states: Record<number, string> = {
  5: '未核算',
  10: '已归档',
  11: '已核算',
  12: 'HR复核中',
  13: '财务审批中',
  14: '已批准',
  15: '已冻结',
  16: '发放中',
  17: '已发放'
}
const amounts = {
  gross: '应发工资',
  tax: '本期预扣个税',
  net: '实发工资',
  personalInsurance: '个人社保',
  personalFund: '个人公积金',
  corporateInsurance: '单位社保',
  corporateFund: '单位公积金',
  approvedCost: '批准总成本',
  paidAmount: '成功回盘金额'
}
const allLabels = { ...labels, SUPPLEMENT: '独立补发证据', SETTLEMENT_OFFSETS: '工资退补税分配' }
const money = (v: any) =>
  v == null
    ? '未知'
    : Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const visibleSignals = computed(
  () =>
    detail.value?.signals.filter(
      (s) => (!kind.value || s.kind === kind.value) && (!onlyMissing.value || !s.ready)
    ) || []
)
const signalLabel = (row: any) =>
  row.status === 'NOT_APPLICABLE'
    ? '不适用（有依据）'
    : row.status === 'SNAPSHOT_PRESENT'
      ? '历史证据已保存'
      : row.status === 'SNAPSHOT_MISSING'
        ? '历史证据未知'
        : row.ready
          ? '已就绪'
          : '待核实'
function reset() {
  rows.value = []
  detail.value = undefined
  batchId.value = undefined
  employeeIds.value = []
}
async function load() {
  if (!entityId.value) return
  loading.value = true
  detail.value = undefined
  try {
    rows.value = await request.get({
      url: base + '/history',
      params: {
        entityId: entityId.value,
        year: year.value,
        month: month.value || undefined,
        type: type.value || undefined,
        status: status.value || undefined
      }
    })
  } finally {
    loading.value = false
  }
}
async function select(row: any) {
  batchId.value = row.batchId
  employeeIds.value = []
  kind.value = undefined
  await loadDetail()
}
async function loadDetail() {
  if (!batchId.value) return
  detailLoading.value = true
  try {
    detail.value = await request.get({
      url: base + '/detail',
      params: {
        batchId: batchId.value,
        employeeIds: employeeIds.value.length ? employeeIds.value.join(',') : undefined
      }
    })
  } finally {
    detailLoading.value = false
  }
}
onMounted(async () => {
  entities.value = await request.get({ url: base + '/entities' })
})
</script>
