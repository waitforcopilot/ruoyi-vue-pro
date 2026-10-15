<template>
  <ContentWrap>
    <el-alert
      title="实际采用已批准的冻结工资成本报表，按计薪期间归集；金额单位为元，编制单位为人。空白营收/预算/编制表示未提供，确认零值另行保存；分母缺失或为零时不计算比率。"
      type="info"
      :closable="false"
    />
    <el-form inline class="mt-4"
      ><el-form-item label="法人主体"
        ><el-select v-model="entityId" @change="changeScope" style="width: 240px"
          ><el-option
            v-for="e in options.entities"
            :key="e.id"
            :value="e.id"
            :label="e.name" /></el-select
      ></el-form-item>
      <el-form-item label="期间"
        ><el-date-picker
          v-model="period"
          type="month"
          value-format="YYYY-MM"
          @change="changeScope" /></el-form-item
      ><el-button :loading="loading" :disabled="!entityId || !period" @click="load">查询</el-button>
    </el-form>
    <el-tabs v-model="tab">
      <el-tab-pane label="工资结构及环比" name="structure" lazy
        ><WageStructure
          :entity-id="entityId"
          :period="period"
          :departments="options.departments || []"
          :categories="options.categories || []"
      /></el-tab-pane>
      <el-tab-pane label="预算与实际分析" name="analysis">
        <el-alert
          v-if="analysis"
          :title="
            analysis.sourceStatus +
            (analysis.missingBatchIds?.length
              ? '；缺少成本报表的批次：' + analysis.missingBatchIds.join('、')
              : '')
          "
          :type="analysis.ready ? 'success' : 'warning'"
          :closable="false"
        />
        <el-form inline class="mt-4"
          ><el-form-item label="分析范围"
            ><el-select v-model="dimension" style="width: 180px"
              ><el-option
                v-for="(label, key) in dimensions"
                :key="key"
                :value="key"
                :label="label" /></el-select
          ></el-form-item>
          <el-button
            v-hasPermi="['hrm:payroll:analysis:export']"
            :disabled="!analysis?.ready"
            @click="exportAnalysis"
            >导出当前范围</el-button
          >
        </el-form>
        <el-table :data="analysisRows" v-loading="loading"
          ><el-table-column prop="name" label="范围" fixed min-width="140" /><el-table-column
            prop="dimensionKey"
            label="编号"
            min-width="100"
          />
          <el-table-column v-for="c in columns" :key="c.key" :label="c.label" :min-width="120"
            ><template #default="{ row }">{{
              display(row[c.key], c.percent)
            }}</template></el-table-column
          >
          <el-table-column
            prop="sourceStatus"
            label="资料状态"
            min-width="260"
            show-overflow-tooltip
          /><el-table-column prop="planVersion" label="计划版本" width="100" />
        </el-table>
        <p class="mt-4"
          >实有人数在每种范围内按员工去重，跨部门/项目范围可能交叉，不能将各类范围叠加。成本报告依据编号：{{
            analysis?.reportIds?.join('、') || '暂无'
          }}</p
        >
      </el-tab-pane>
      <el-tab-pane label="预算、营收及编制版本" name="plans">
        <el-space class="mb-4"
          ><el-button
            v-hasPermi="['hrm:payroll:analysis:update']"
            :disabled="!entityId || !period"
            @click="openPlan()"
            >登记资料</el-button
          ><el-button
            v-hasPermi="['hrm:payroll:analysis:export']"
            :disabled="!entityId || !period"
            @click="template"
            >下载模板/当前批准资料</el-button
          ></el-space
        >
        <el-table :data="plans" v-loading="loading"
          ><el-table-column label="范围"
            ><template #default="{ row }"
              >{{ dimensions[row.dimension] }} · {{ row.dimensionKey }}</template
            ></el-table-column
          ><el-table-column prop="version" label="版本" width="70" />
          <el-table-column
            v-for="c in [
              { key: 'budget', label: '预算' },
              { key: 'revenue', label: '营收' },
              { key: 'headcount', label: '编制' }
            ]"
            :key="c.key"
            :label="c.label"
            ><template #default="{ row }">{{
              row[c.key] == null ? '未提供' : row[c.key]
            }}</template></el-table-column
          >
          <el-table-column label="状态"
            ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
          ><el-table-column prop="evidence" label="核实依据" show-overflow-tooltip />
          <el-table-column label="操作" width="280"
            ><template #default="{ row }"
              ><el-button link @click="showEvidence(row)">依据与来源</el-button
              ><el-button
                v-if="row.status === 'APPROVED'"
                v-hasPermi="['hrm:payroll:analysis:update']"
                link
                @click="openPlan(row)"
                >接续版本</el-button
              >
              <template v-if="row.status === 'PENDING'"
                ><el-button
                  v-hasPermi="['hrm:payroll:analysis:approve']"
                  link
                  @click="review(row.id, true)"
                  >批准</el-button
                ><el-button
                  v-hasPermi="['hrm:payroll:analysis:approve']"
                  link
                  @click="review(row.id, false)"
                  >驳回</el-button
                ></template
              >
            </template></el-table-column
          >
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="Excel 预校验与导入" name="import">
        <el-alert
          title="范围类型：ENTITY（主体，编号ALL）、DEPARTMENT（部门编号）、COST_CENTER、PROJECT、CATEGORY。模板保留接续批准版本编号；年度/月必须匹配所选期间。空白不自动转零，至少填写预算、营收或编制之一。"
          :closable="false"
          type="info"
        />
        <el-space class="my-4" wrap>
          <el-upload
            ref="uploadRef"
            :key="`${entityId}-${period}`"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :disabled="!entityId || !period"
            :on-change="chooseFile"
            :on-remove="removeFile"
            ><el-button v-hasPermi="['hrm:payroll:analysis:update']"
              >选择计划资料文件</el-button
            ></el-upload
          >
          <el-button
            v-hasPermi="['hrm:payroll:analysis:update']"
            :loading="saving"
            :disabled="!file"
            @click="preview"
            >预校验</el-button
          >
          <el-button
            v-hasPermi="['hrm:payroll:analysis:update']"
            :disabled="!previewRows.length || !file"
            @click="exportErrors"
            >下载逐行校验</el-button
          >
          <el-button
            v-hasPermi="['hrm:payroll:analysis:update']"
            type="primary"
            :loading="saving"
            :disabled="!previewRows.length || previewRows.some((r) => r.error) || !file"
            @click="importFile"
            >整批提交独立复核</el-button
          >
        </el-space>
        <el-table :data="previewRows"
          ><el-table-column prop="sheet" label="工作表" /><el-table-column
            prop="row"
            label="文件行"
            width="100"
          /><el-table-column label="范围类型"
            ><template #default="{ row }">{{
              dimensions[row.dimension] || row.dimension
            }}</template></el-table-column
          ><el-table-column prop="dimensionKey" label="范围编号" /><el-table-column
            label="校验结果"
            min-width="360"
            ><template #default="{ row }">{{
              row.error || '通过；提交时再次校验当前批准版本'
            }}</template></el-table-column
          ></el-table
        >
      </el-tab-pane>
      <el-tab-pane label="期间审批与导出履历" name="history"
        ><el-table :data="history"
          ><el-table-column prop="createTime" label="时间" /><el-table-column
            prop="planId"
            label="计划编号" /><el-table-column prop="actorId" label="操作人" /><el-table-column
            prop="action"
            label="动作" /><el-table-column prop="reason" label="依据" min-width="240" /></el-table
      ></el-tab-pane>
    </el-tabs>
  </ContentWrap>
  <Dialog v-model="visible" title="提交预算/营收/编制资料新版本" width="720px">
    <el-form label-width="130px"
      ><el-form-item label="范围类型"
        ><el-select
          v-model="draft.dimension"
          :disabled="!!draft.priorPlanId"
          @change="draft.dimensionKey = draft.dimension === 'ENTITY' ? 'ALL' : ''"
          ><el-option
            v-for="(label, key) in dimensions"
            :key="key"
            :value="key"
            :label="label" /></el-select
      ></el-form-item>
      <el-form-item label="范围"
        ><span v-if="draft.dimension === 'ENTITY'">全主体</span
        ><el-select
          v-else-if="draft.dimension === 'DEPARTMENT'"
          v-model="draft.dimensionKey"
          :disabled="!!draft.priorPlanId"
          filterable
          ><el-option
            v-for="d in options.departments"
            :key="d.id"
            :value="String(d.id)"
            :label="d.name" /></el-select
        ><el-input
          v-else
          v-model="draft.dimensionKey"
          :disabled="!!draft.priorPlanId"
          maxlength="100"
          placeholder="填写与正式数据一致的成本中心、项目编码或人员类别"
      /></el-form-item>
      <el-form-item
        v-for="c in [
          { key: 'budget', label: '成本预算' },
          { key: 'revenue', label: '营收' },
          { key: 'headcount', label: '编制人数' }
        ]"
        :key="c.key"
        :label="c.label"
        ><el-input-number
          v-model="draft[c.key]"
          :min="0"
          :precision="c.key === 'headcount' ? 0 : 2"
          placeholder="留空表示未提供"
          clearable
      /></el-form-item>
      <el-form-item label="来源编号"
        ><el-input
          v-model="draft.sourceReference"
          maxlength="200"
          placeholder="正式预算、营收或编制文件编号" /></el-form-item
      ><el-form-item label="核实依据"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000"
      /></el-form-item> </el-form
    ><template #footer
      ><el-button
        type="primary"
        :loading="saving"
        :disabled="
          !draft.dimensionKey ||
          !draft.evidence.trim() ||
          [draft.budget, draft.revenue, draft.headcount].every((v) => v == null)
        "
        @click="save"
        >提交独立复核</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="proofVisible" title="正式资料依据" width="650px"
    ><p>{{ proof?.evidence }}</p
    ><p>来源：{{ proof?.sourceReference || '人工核实资料' }}</p
    ><p>提交人：{{ proof?.submittedBy }}；复核人：{{ proof?.reviewedBy || '待复核' }}</p
    ><p>{{ proof?.reviewReason }}</p></Dialog
  >
</template>
<script setup lang="ts">
import WageStructure from './WageStructure.vue'
import request from '@/config/axios'
import download from '@/utils/download'
import dayjs from 'dayjs'
const message = useMessage(),
  base = '/hrm/payroll/financial'
const entityId = ref<number>(),
  period = ref(dayjs().format('YYYY-MM')),
  tab = ref('analysis'),
  dimension = ref('ENTITY')
const options = ref<any>({ entities: [], departments: [] }),
  analysis = ref<any>(),
  plans = ref<any[]>([]),
  history = ref<any[]>([]),
  previewRows = ref<any[]>([]),
  proof = ref<any>()
const uploadRef = ref<any>()
const file = ref<File>(),
  requestKey = ref(''),
  loading = ref(false),
  saving = ref(false),
  visible = ref(false),
  proofVisible = ref(false)
const dimensions = {
  ENTITY: '主体',
  DEPARTMENT: '部门',
  COST_CENTER: '成本中心',
  PROJECT: '项目',
  CATEGORY: '人员类别'
}
const states = { PENDING: '待复核', APPROVED: '已批准', REJECTED: '已驳回', SUPERSEDED: '已接续' }
const columns = [
  { key: 'actualCost', label: '实际成本' },
  { key: 'budget', label: '批准预算' },
  { key: 'budgetVariance', label: '预算差额' },
  { key: 'budgetVarianceRate', label: '预算差异率%', percent: true },
  { key: 'revenue', label: '营收' },
  { key: 'costRevenueRate', label: '成本营收率%', percent: true },
  { key: 'employees', label: '实有人数' },
  { key: 'headcount', label: '批准编制' },
  { key: 'staffingVariance', label: '编制差额' },
  { key: 'costPerEmployee', label: '人均成本' },
  { key: 'costPerPosition', label: '编制单位成本' }
]
const draft = reactive<any>({
  dimension: 'ENTITY',
  dimensionKey: 'ALL',
  budget: undefined,
  revenue: undefined,
  headcount: undefined,
  evidence: '',
  sourceReference: '',
  requestKey: '',
  priorPlanId: undefined
})
const analysisRows = computed(
  () => analysis.value?.rows?.filter((r) => r.dimension === dimension.value) || []
)
const params = () => {
  const [year, month] = period.value.split('-').map(Number)
  return { entityId: entityId.value, year, month }
}
const display = (value: any, percent = false) =>
  value == null
    ? '不可计算'
    : Number(value).toLocaleString('zh-CN', { maximumFractionDigits: percent ? 4 : 4 })
function removeFile() {
  file.value = undefined
  previewRows.value = []
  requestKey.value = ''
  uploadRef.value?.clearFiles()
}
async function changeScope() {
  removeFile()
  analysis.value = undefined
  plans.value = []
  history.value = []
  if (!entityId.value || !period.value) return
  options.value = await request.get({
    url: base + '/options',
    params: { entityId: entityId.value }
  })
  await load()
}
async function load() {
  if (!entityId.value || !period.value) return
  loading.value = true
  try {
    const p = params()
    const data = await Promise.all([
      request.get({ url: base + '/analysis', params: p }),
      request.get({ url: base + '/plans', params: p }),
      request.get({ url: base + '/history', params: p })
    ])
    if (
      entityId.value !== p.entityId ||
      period.value !== `${p.year}-${String(p.month).padStart(2, '0')}`
    )
      return
    ;[analysis.value, plans.value, history.value] = data
  } finally {
    loading.value = false
  }
}
function openPlan(row?: any) {
  Object.assign(draft, {
    dimension: row?.dimension || 'ENTITY',
    dimensionKey: row?.dimensionKey || 'ALL',
    priorPlanId: row?.id,
    budget: row?.budget == null ? undefined : Number(row.budget),
    revenue: row?.revenue == null ? undefined : Number(row.revenue),
    headcount: row?.headcount == null ? undefined : Number(row.headcount),
    evidence: '',
    sourceReference: '',
    requestKey: crypto.randomUUID()
  })
  visible.value = true
}
async function save() {
  saving.value = true
  try {
    await request.post({ url: base + '/create', data: { ...draft, ...params() } })
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function review(id: number, approved: boolean) {
  const { value } = await message.prompt('填写正式复核依据', '预算/营收/编制复核')
  if (!value?.trim()) return
  await request.post({ url: base + '/review', params: { id, approved, reason: value } })
  await load()
}
function showEvidence(row: any) {
  proof.value = row
  proofVisible.value = true
}
async function template() {
  const blob = await request.download({ url: base + '/template', params: params() })
  download.excel(blob, '预算营收编制模板.xlsx')
}
async function exportAnalysis() {
  const blob = await request.download({
    url: base + '/export',
    params: { ...params(), dimension: dimension.value }
  })
  download.excel(blob, '预算与成本分析.xlsx')
  await load()
}
function chooseFile(upload: any) {
  if (!upload.raw) return
  if (upload.raw.size > 10 * 1024 * 1024) {
    message.warning('文件不能超过10MB')
    removeFile()
    return
  }
  file.value = upload.raw
  requestKey.value = crypto.randomUUID()
  previewRows.value = []
}
function form() {
  const data = new FormData()
  Object.entries(params()).forEach(([key, value]) => data.append(key, String(value)))
  data.append('file', file.value!)
  data.append('requestKey', requestKey.value)
  return data
}
async function preview() {
  saving.value = true
  try {
    const result = await request.upload<{ data: any[] }>({ url: base + '/preview', data: form() })
    previewRows.value = result.data
  } finally {
    saving.value = false
  }
}
async function importFile() {
  saving.value = true
  try {
    const result = await request.upload<{ data: number[] }>({ url: base + '/import', data: form() })
    message.success(`已提交 ${result.data.length} 条待复核资料`)
    removeFile()
    await load()
    tab.value = 'plans'
  } finally {
    saving.value = false
  }
}
async function exportErrors() {
  const blob = await request.postOriginal({
    url: base + '/preview-errors',
    data: form(),
    headersType: 'multipart/form-data',
    responseType: 'blob'
  })
  download.excel(blob, '计划导入逐行校验.xlsx')
}
onMounted(async () => {
  options.value = await request.get({ url: base + '/options' })
})
</script>
