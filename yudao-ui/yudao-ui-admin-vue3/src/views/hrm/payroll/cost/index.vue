<template>
  <ContentWrap>
    <el-alert
      title="成本来源为冻结工资版本：应发工资 + 单位社保公积金 + 批准配置的单位成本。分摊使用原核算历史归属，独立复核后方可正式导出。"
      type="info"
      :closable="false"
    />
    <el-form inline class="mt-4"
      ><el-form-item label="法人主体"
        ><el-select v-model="entityId" @change="changeEntity" style="width: 240px"
          ><el-option
            v-for="e in options.entities"
            :key="e.id"
            :value="e.id"
            :label="e.name" /></el-select
      ></el-form-item>
      <el-form-item label="冻结工资批次"
        ><el-select v-model="batchId" filterable @change="loadReports" style="width: 320px"
          ><el-option
            v-for="b in options.batches"
            :key="b.id"
            :value="b.id"
            :label="`${b.title} · ${b.id}`" /></el-select
      ></el-form-item>
    </el-form>
    <el-tabs v-model="tab">
      <el-tab-pane label="成本报表" name="reports">
        <el-button :disabled="!batchId" :loading="loading" @click="loadReports">刷新</el-button>
        <el-button
          v-hasPermi="['hrm:payroll:cost:update']"
          :disabled="!batchId"
          :loading="saving"
          @click="build"
          >生成新版本</el-button
        >
        <el-table :data="reports" v-loading="loading"
          ><el-table-column prop="version" label="报表版本" width="100" /><el-table-column
            prop="runId"
            label="冻结核算编号"
            width="120"
          />
          <el-table-column label="状态"
            ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
          ><el-table-column prop="computedBy" label="编制人" /><el-table-column
            prop="reviewedBy"
            label="复核人"
          /><el-table-column prop="reason" label="编制依据" show-overflow-tooltip />
          <el-table-column label="操作" width="300"
            ><template #default="{ row }"
              ><el-button link @click="viewReport(row)">分摊明细</el-button
              ><el-button link @click="history('REPORT', row)">履历</el-button>
              <template v-if="row.status === 'PENDING'"
                ><el-button
                  v-hasPermi="['hrm:payroll:cost:approve']"
                  link
                  @click="review('report', row.id, true)"
                  >批准</el-button
                ><el-button
                  v-hasPermi="['hrm:payroll:cost:approve']"
                  link
                  @click="review('report', row.id, false)"
                  >驳回</el-button
                ></template
              >
              <el-button
                v-if="row.status === 'APPROVED'"
                v-hasPermi="['hrm:payroll:cost:export']"
                link
                @click="exportReport(row.id)"
                >导出</el-button
              >
            </template></el-table-column
          >
        </el-table>
        <template v-if="selectedReport"
          ><h4>版本 {{ selectedReport.version }} · 分摊成本 {{ total }}</h4
          ><el-table :data="rows" show-summary :summary-method="summaries">
            <el-table-column prop="employeeName" label="员工" min-width="100" /><el-table-column
              prop="employeeCategory"
              label="人员类别"
              min-width="100"
            /><el-table-column
              prop="sourceDeptId"
              label="历史部门编号"
              width="120"
            /><el-table-column prop="deptName" label="分摊部门" min-width="140" />
            <el-table-column prop="costCenter" label="成本中心" /><el-table-column
              prop="projectCode"
              label="项目"
            /><el-table-column prop="percent" label="比例%" />
            <el-table-column prop="gross" label="应发" /><el-table-column
              prop="insurance"
              label="单位社保"
            /><el-table-column prop="fund" label="单位公积金" /><el-table-column
              prop="employerCost"
              label="配置单位成本"
            /><el-table-column prop="total" label="总成本" /><el-table-column
              prop="ruleId"
              label="规则编号"
            /> </el-table
        ></template>
      </el-tab-pane>
      <el-tab-pane label="分摊规则与历史版本" name="rules">
        <el-button :disabled="!entityId" @click="loadRules">刷新</el-button
        ><el-button
          v-hasPermi="['hrm:payroll:cost:update']"
          :disabled="!entityId"
          @click="openRule()"
          >登记分摊规则</el-button
        >
        <el-table :data="rules" v-loading="loading"
          ><el-table-column label="员工"
            ><template #default="{ row }">{{
              employeeName(row.employeeId)
            }}</template></el-table-column
          ><el-table-column prop="version" label="版本" width="80" /><el-table-column
            prop="effectiveFrom"
            label="开始日期"
          /><el-table-column prop="effectiveTo" label="结束日期" /><el-table-column
            prop="employerCost"
            label="每常规批次单位成本"
            min-width="160"
          />
          <el-table-column label="状态"
            ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
          ><el-table-column label="操作" width="320"
            ><template #default="{ row }">
              <el-button link @click="history('RULE', row)">规则与履历</el-button
              ><el-button
                v-if="row.status === 'APPROVED'"
                v-hasPermi="['hrm:payroll:cost:update']"
                link
                @click="openRule(row)"
                >接续版本</el-button
              >
              <template v-if="row.status === 'PENDING'"
                ><el-button
                  v-hasPermi="['hrm:payroll:cost:approve']"
                  link
                  @click="review('rule', row.id, true)"
                  >批准</el-button
                ><el-button
                  v-hasPermi="['hrm:payroll:cost:approve']"
                  link
                  @click="review('rule', row.id, false)"
                  >驳回</el-button
                ></template
              >
            </template></el-table-column
          >
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
  <Dialog v-model="ruleVisible" title="提交分摊规则新版本" width="1000px">
    <el-form label-width="160px"
      ><el-form-item label="员工"
        ><el-select v-model="draft.employeeId" filterable :disabled="!!draft.priorRuleId"
          ><el-option
            v-for="e in options.employees"
            :key="e.id"
            :value="e.id"
            :label="`${e.name} (${e.jobNumber})`" /></el-select
      ></el-form-item>
      <el-form-item label="有效日期"
        ><el-date-picker v-model="draft.effectiveFrom" value-format="YYYY-MM-DD" /><span
          class="mx-2"
          >至</span
        ><el-date-picker v-model="draft.effectiveTo" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="每常规批次单位成本"
        ><el-input-number v-model="draft.employerCost" :min="0" :precision="2" /><span class="ml-2"
          >无需额外配置时明确填零；独立补发不重复计入。</span
        ></el-form-item
      >
      <el-form-item label="规则依据"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000"
      /></el-form-item>
    </el-form>
    <el-table :data="draft.parts"
      ><el-table-column label="分摊部门" min-width="180"
        ><template #default="{ row }"
          ><el-select v-model="row.deptId" filterable
            ><el-option
              v-for="d in options.departments"
              :key="d.id"
              :value="d.id"
              :label="d.name" /></el-select></template
      ></el-table-column>
      <el-table-column label="成本中心"
        ><template #default="{ row }"
          ><el-input v-model="row.costCenter" maxlength="100" /></template></el-table-column
      ><el-table-column label="项目"
        ><template #default="{ row }"
          ><el-input v-model="row.projectCode" maxlength="100" /></template
      ></el-table-column>
      <el-table-column label="比例%" width="190"
        ><template #default="{ row }"
          ><el-input-number
            v-model="row.percent"
            :min="0"
            :max="100"
            :precision="4" /></template></el-table-column
      ><el-table-column width="80"
        ><template #default="{ $index }"
          ><el-button link @click="draft.parts.splice($index, 1)">删除</el-button></template
        ></el-table-column
      > </el-table
    ><el-button
      class="mt-2"
      @click="draft.parts.push({ deptId: undefined, costCenter: '', projectCode: '', percent: 0 })"
      >增加分摊行</el-button
    ><span class="ml-4">比例合计 {{ percentTotal }}%</span>
    <template #footer
      ><el-button
        :loading="saving"
        :disabled="
          !draft.employeeId ||
          !draft.evidence.trim() ||
          !draft.effectiveFrom ||
          !draft.effectiveTo ||
          Math.abs(percentTotal - 100) > 0.00001 ||
          draft.parts.some((p) => !p.deptId || p.percent <= 0)
        "
        type="primary"
        @click="saveRule"
        >提交独立复核</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="historyVisible" title="审批依据与历史" width="950px"
    ><p>{{ proof?.evidence || proof?.reason }}</p
    ><el-table v-if="proof?.parts" :data="JSON.parse(proof.parts)"
      ><el-table-column prop="deptId" label="分摊部门编号" /><el-table-column
        prop="costCenter"
        label="成本中心" /><el-table-column prop="projectCode" label="项目" /><el-table-column
        prop="percent"
        label="比例%"
    /></el-table>
    <el-table :data="events"
      ><el-table-column prop="createTime" label="时间" /><el-table-column
        prop="actorId"
        label="操作人" /><el-table-column prop="action" label="动作" /><el-table-column
        prop="reason"
        label="依据"
    /></el-table>
  </Dialog>
</template>
<script setup lang="ts">
import request from '@/config/axios'
import download from '@/utils/download'
const message = useMessage(),
  base = '/hrm/payroll/cost'
const entityId = ref<number>(),
  batchId = ref<number>(),
  tab = ref('reports')
const options = ref<any>({ entities: [], employees: [], departments: [], batches: [] })
const reports = ref<any[]>([]),
  rules = ref<any[]>([]),
  rows = ref<any[]>([]),
  events = ref<any[]>([]),
  selectedReport = ref<any>(),
  proof = ref<any>()
const loading = ref(false),
  saving = ref(false),
  ruleVisible = ref(false),
  historyVisible = ref(false)
const states = { PENDING: '待复核', APPROVED: '已批准', REJECTED: '已驳回', SUPERSEDED: '已接续' }
const draft = reactive({
  employeeId: undefined as number | undefined,
  priorRuleId: undefined as number | undefined,
  effectiveFrom: '',
  effectiveTo: '',
  employerCost: 0,
  evidence: '',
  requestKey: '',
  parts: [] as any[]
})
const percentTotal = computed(() => draft.parts.reduce((sum, p) => sum + Number(p.percent || 0), 0))
const summaries = ({ columns, data }: any) =>
  columns.map((column: any, index: number) => {
    if (index === 0) return '合计'
    if (!['gross', 'insurance', 'fund', 'employerCost', 'total'].includes(column.property))
      return ''
    return data
      .reduce((sum: number, row: any) => sum + Number(row[column.property] || 0), 0)
      .toFixed(2)
  })
const total = computed(() => rows.value.reduce((sum, r) => sum + Number(r.total), 0).toFixed(2))
const employeeName = (id: number) =>
  options.value.employees.find((e) => e.id === id)?.name || `员工 ${id}`
async function changeEntity() {
  rows.value = []
  reports.value = []
  rules.value = []
  batchId.value = undefined
  selectedReport.value = undefined
  if (!entityId.value) return
  options.value = await request.get({
    url: base + '/options',
    params: { entityId: entityId.value }
  })
  await loadRules()
}
async function loadRules() {
  if (!entityId.value) return
  loading.value = true
  try {
    rules.value = await request.get({ url: base + '/rules', params: { entityId: entityId.value } })
  } finally {
    loading.value = false
  }
}
async function loadReports() {
  rows.value = []
  selectedReport.value = undefined
  if (!batchId.value) return
  loading.value = true
  try {
    reports.value = await request.get({
      url: base + '/reports',
      params: { batchId: batchId.value }
    })
  } finally {
    loading.value = false
  }
}
async function build() {
  if (!batchId.value) return
  const { value } = await message.prompt('填写成本报表编制依据', '生成报表版本')
  if (!value?.trim()) return
  saving.value = true
  try {
    await request.post({ url: base + '/build', params: { batchId: batchId.value, reason: value } })
    await loadReports()
  } finally {
    saving.value = false
  }
}
async function viewReport(report: any) {
  rows.value = await request.get({ url: base + '/rows', params: { id: report.id } })
  selectedReport.value = report
}
async function review(kind: string, id: number, approved: boolean) {
  const { value } = await message.prompt('填写独立复核依据', '成本复核')
  if (!value?.trim()) return
  await request.post({ url: base + '/review-' + kind, params: { id, approved, reason: value } })
  await (kind === 'rule' ? loadRules() : loadReports())
}
async function exportReport(id: number) {
  const blob = await request.download({ url: base + '/export', params: { id } })
  download.excel(blob, '薪酬成本分摊.xlsx')
}
async function openRule(rule?: any) {
  Object.assign(draft, {
    employeeId: rule?.employeeId,
    priorRuleId: rule?.id,
    effectiveFrom: rule?.effectiveFrom || '',
    effectiveTo: rule?.effectiveTo || '',
    employerCost: Number(rule?.employerCost || 0),
    evidence: '',
    requestKey: crypto.randomUUID(),
    parts: rule
      ? JSON.parse(rule.parts)
      : [{ deptId: undefined, costCenter: '', projectCode: '', percent: 100 }]
  })
  ruleVisible.value = true
}
async function saveRule() {
  saving.value = true
  try {
    await request.post({ url: base + '/rule', data: { ...draft, entityId: entityId.value } })
    ruleVisible.value = false
    await loadRules()
  } finally {
    saving.value = false
  }
}
async function history(kind: string, row: any) {
  events.value = await request.get({ url: base + '/history', params: { kind, id: row.id } })
  proof.value = row
  historyVisible.value = true
}
onMounted(async () => {
  options.value = await request.get({ url: base + '/options' })
})
</script>
