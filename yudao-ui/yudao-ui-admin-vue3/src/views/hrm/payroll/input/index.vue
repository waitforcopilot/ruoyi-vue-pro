<template>
  <SalarySubjectBatchSelector v-model="batchId" @change="load" />
  <ContentWrap>
    <el-alert
      title="考勤、加班、专项附加扣除和历史累计需要明确来源并由独立人员复核。同步来源变化或新版本在审时，核算与冻结会阻断；缺失不按满勤或零金额处理。"
      type="info"
      :closable="false"
    />
    <el-form inline class="mt-12px"
      ><el-button :loading="loading" @click="load">刷新</el-button
      ><el-form-item label="模板类型"
        ><el-select v-model="templateKind" @change="templateMode = modesFor(templateKind)[0]"
          ><el-option
            v-for="(name, kind) in kinds"
            :key="kind"
            :label="name"
            :value="kind" /></el-select></el-form-item
      ><el-form-item label="处理方式"
        ><el-select v-model="templateMode"
          ><el-option
            v-for="mode in modesFor(templateKind)"
            :key="mode"
            :value="mode"
            :label="modes[mode]" /></el-select></el-form-item
      ><el-button
        v-if="canExport"
        v-hasPermi="['hrm:payroll:input:export']"
        @click="downloadTemplate"
        >下载模板</el-button
      ><el-upload
        v-if="manage"
        v-hasPermi="['hrm:payroll:input:update']"
        :auto-upload="false"
        :show-file-list="false"
        accept=".xlsx,.xls"
        :on-change="upload"
        ><el-button :loading="saving">上传预校验</el-button></el-upload
      ></el-form
    >
    <el-tabs>
      <el-tab-pane label="就绪与异常"
        ><el-table v-loading="loading" :data="readiness"
          ><el-table-column prop="jobNumber" label="工号" /><el-table-column
            prop="employeeName"
            label="员工"
          /><el-table-column label="输入类型"
            ><template #default="{ row }">{{ kinds[row.kind] }}</template></el-table-column
          ><el-table-column label="状态"
            ><template #default="{ row }">{{ statuses[row.status] }}</template></el-table-column
          ><el-table-column
            prop="reason"
            label="来源或阻断原因"
            show-overflow-tooltip
          /><el-table-column label="操作"
            ><template #default="{ row }"
              ><el-button
                v-if="manage"
                v-hasPermi="['hrm:payroll:input:update']"
                link
                :disabled="row.status === 'PENDING'"
                @click="open(row)"
                >准备或新建版本</el-button
              ></template
            ></el-table-column
          ></el-table
        ></el-tab-pane
      >
      <el-tab-pane label="确认版本与复核"
        ><el-table :data="records"
          ><el-table-column prop="id" label="编号" width="80" /><el-table-column
            prop="employeeId"
            label="员工编号"
          /><el-table-column label="数据类型"
            ><template #default="{ row }">{{ kinds[row.kind] }}</template></el-table-column
          ><el-table-column prop="version" label="版本" width="70" /><el-table-column
            label="来源方式"
            ><template #default="{ row }">{{ modes[row.mode] }}</template></el-table-column
          ><el-table-column label="状态"
            ><template #default="{ row }">{{ statuses[row.status] }}</template></el-table-column
          ><el-table-column
            prop="evidence"
            label="核实依据"
            show-overflow-tooltip
          /><el-table-column label="操作" width="240"
            ><template #default="{ row }"
              ><el-button link @click="showRecord(row)">数据与来源</el-button
              ><template v-if="canReview && row.status === 'PENDING'"
                ><el-button
                  v-hasPermi="['hrm:payroll:input:review']"
                  link
                  @click="review(row.id, true)"
                  >复核通过</el-button
                ><el-button
                  v-hasPermi="['hrm:payroll:input:review']"
                  link
                  @click="review(row.id, false)"
                  >退回</el-button
                ></template
              ></template
            ></el-table-column
          ></el-table
        ></el-tab-pane
      >
      <el-tab-pane label="导入与错误行"
        ><el-table :data="importBatches"
          ><el-table-column prop="id" label="编号" /><el-table-column
            prop="fileName"
            label="文件"
          /><el-table-column prop="version" label="预览版本" /><el-table-column label="状态"
            ><template #default="{ row }">{{ statuses[row.status] }}</template></el-table-column
          ><el-table-column label="操作"
            ><template #default="{ row }"
              ><el-button link @click="showImport(row.id)">预览与错误</el-button
              ><el-button
                v-if="manage && ['VALIDATED', 'FAILED'].includes(row.status)"
                v-hasPermi="['hrm:payroll:input:update']"
                link
                @click="revalidate(row.id)"
                >新建来源预览</el-button
              ><el-button
                v-if="canExport && row.status === 'FAILED'"
                v-hasPermi="['hrm:payroll:input:export']"
                link
                @click="downloadErrors(row.id)"
                >错误行下载</el-button
              ></template
            ></el-table-column
          ></el-table
        ></el-tab-pane
      >
    </el-tabs>
  </ContentWrap>
  <el-dialog v-model="visible" title="准备核算输入" width="700px" :destroy-on-close="true">
    <el-form ref="formRef" :model="draft" label-width="170px"
      ><el-form-item label="员工与数据类型"
        >{{ employeeName }} · {{ kinds[draft.kind] }}</el-form-item
      ><el-form-item label="处理方式"
        ><el-select v-model="draft.mode" @change="modeChanged"
          ><el-option
            v-for="mode in modesFor(draft.kind)"
            :key="mode"
            :label="modes[mode]"
            :value="mode" /></el-select></el-form-item
      ><el-form-item label="来源与核实依据" prop="evidence" :rules="required"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000" /></el-form-item
      ><template v-if="draft.mode === 'IMPORT'"
        ><el-form-item
          v-for="field in fields[draft.kind]"
          :key="field"
          :label="labels[field] || field"
          :prop="`values.${field}`"
          :rules="required"
          ><el-input-number
            v-model="draft.values[field]"
            :min="0"
            :precision="2"
            :controls="false" /></el-form-item></template
    ></el-form>
    <el-table v-if="previewValues.length" :data="previewValues"
      ><el-table-column prop="name" label="预校验字段" /><el-table-column prop="value" label="数值"
    /></el-table>
    <template #footer
      ><el-button @click="visible = false">关闭</el-button
      ><el-button :loading="saving" @click="inspect">预校验</el-button
      ><el-button :loading="saving" type="primary" @click="submit"
        >提交独立复核</el-button
      ></template
    >
  </el-dialog>
  <el-dialog v-model="recordVisible" title="确认的数值与来源依据" width="950px"
    ><el-table :data="recordValues"
      ><el-table-column prop="name" label="字段" /><el-table-column
        prop="value"
        label="数值" /></el-table
    ><el-descriptions :column="2" border
      ><el-descriptions-item label="提交人">{{ currentRecord?.creator }}</el-descriptions-item
      ><el-descriptions-item label="复核人">{{ currentRecord?.reviewedBy }}</el-descriptions-item
      ><el-descriptions-item label="来源依据">{{
        currentRecord?.evidence
      }}</el-descriptions-item></el-descriptions
    ><el-collapse
      ><el-collapse-item title="查看原始来源快照">
        <pre style="white-space: pre-wrap">{{ sourceProof }}</pre>
      </el-collapse-item></el-collapse
    ></el-dialog
  >
  <el-dialog v-model="importVisible" title="核算输入导入预览" width="1000px"
    ><template v-if="currentImport"
      ><el-descriptions :column="3" border
        ><el-descriptions-item label="文件">{{ currentImport.fileName }}</el-descriptions-item
        ><el-descriptions-item label="状态">{{
          statuses[currentImport.status]
        }}</el-descriptions-item
        ><el-descriptions-item label="来源组数">{{
          proposals.length
        }}</el-descriptions-item></el-descriptions
      ><el-table v-if="rowErrors.length" :data="rowErrors"
        ><el-table-column prop="row" label="文件行" width="90" /><el-table-column
          prop="jobNumber"
          label="工号" /><el-table-column prop="reason" label="错误说明" /></el-table
      ><el-table v-else :data="proposals"
        ><el-table-column prop="request.employeeId" label="员工编号" /><el-table-column label="类型"
          ><template #default="{ row }">{{ kinds[row.request.kind] }}</template></el-table-column
        ><el-table-column label="处理方式"
          ><template #default="{ row }">{{ modes[row.request.mode] }}</template></el-table-column
        ><el-table-column label="预校验值"
          ><template #default="{ row }"
            ><div v-for="(value, field) in row.prepared.values" :key="field"
              >{{ labels[field] || field }}：{{ value }}</div
            ></template
          ></el-table-column
        ><el-table-column
          prop="request.evidence"
          label="来源依据"
          show-overflow-tooltip /></el-table></template
    ><template #footer
      ><el-button @click="importVisible = false">关闭</el-button
      ><el-button
        v-if="manage && currentImport?.status === 'VALIDATED'"
        v-hasPermi="['hrm:payroll:input:update']"
        :loading="saving"
        type="primary"
        @click="commit"
        >确认提交独立复核</el-button
      ></template
    ></el-dialog
  >
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/input'
import * as EntityApi from '@/api/hrm/payroll/entity'
import * as BatchApi from '@/api/hrm/salary/month-record'
import SalarySubjectBatchSelector from '@/views/hrm/salary/month-record/components/SalarySubjectBatchSelector.vue'
import download from '@/utils/download'
import { ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
defineOptions({ name: 'HrmPayrollInput' })
const message = useMessage(),
  batchId = ref<number>(),
  loading = ref(false),
  saving = ref(false),
  readiness = ref<any[]>([]),
  records = ref<any[]>([]),
  importBatches = ref<any[]>([]),
  actions = ref<string[]>([])
const manage = computed(() => actions.value.includes('MANAGE')),
  canReview = computed(() => actions.value.includes('REVIEW')),
  canExport = computed(() => actions.value.includes('EXPORT'))
const kinds: Record<string, string> = {
  ATTENDANCE: '考勤及实际出勤',
  OVERTIME: '工时制度与加班',
  DEDUCTIONS: '专项附加扣除',
  CUMULATIVE_TAX: '本主体历史累计预扣'
}
const modes: Record<string, string> = {
  SYNC: '同步已核实来源',
  ZERO: '确认无发生额',
  IMPORT: '提供完整导入数据'
}
const statuses: Record<string, string> = {
  MISSING: '尚未准备',
  PENDING: '待独立复核',
  APPROVED: '已复核就绪',
  REJECTED: '已退回',
  STALE: '来源变更，需新版本',
  VALIDATED: '预校验通过',
  FAILED: '导入失败',
  COMMITTED: '已提交复核',
  SUPERSEDED: '已由新预览替代'
}
const fields: Record<string, string[]> = {
  ATTENDANCE: [
    'ACTUAL_DAYS',
    'NEED_DAYS',
    '190101',
    '190102',
    '190103',
    '190104',
    '190105',
    '190106'
  ],
  OVERTIME: ['180101'],
  DEDUCTIONS: ['260101', '260102', '260103', '260104', '260105'],
  CUMULATIVE_TAX: ['250101', '250102', '250103', '250105']
}
const labels: Record<string, string> = {
  ACTUAL_DAYS: '实际出勤（天）',
  NEED_DAYS: '应出勤（天）',
  '180101': '加班费（元）',
  '190101': '迟到扣款（元）',
  '190102': '早退扣款（元）',
  '190103': '旷工扣款（元）',
  '190104': '请假扣款（元）',
  '190105': '缺卡扣款（元）',
  '190106': '其他考勤扣款（元）',
  '260101': '累计子女教育（元）',
  '260102': '累计住房租金（元）',
  '260103': '累计房贷利息（元）',
  '260104': '累计赡养老人（元）',
  '260105': '累计继续教育（元）',
  '250101': '截至上期累计收入（元）',
  '250102': '截至上期减除费用（元）',
  '250103': '截至上期专项扣除（元）',
  '250105': '截至上期已预扣税额（元）'
}
const modesFor = (kind: string) =>
  kind === 'ATTENDANCE'
    ? ['SYNC', 'IMPORT']
    : kind === 'OVERTIME'
      ? ['SYNC', 'ZERO']
      : kind === 'DEDUCTIONS'
        ? ['ZERO', 'IMPORT']
        : ['SYNC', 'ZERO', 'IMPORT']
const templateKind = ref('ATTENDANCE'),
  templateMode = ref('SYNC'),
  visible = ref(false),
  draft = ref<any>({ values: {} }),
  employeeName = ref(''),
  formRef = ref(),
  previewValues = ref<any[]>([])
const required = [{ required: true, message: '请填写；真实零值明确填0', trigger: 'blur' }]
const recordVisible = ref(false),
  currentRecord = ref<any>(),
  recordValues = ref<any[]>([]),
  sourceProof = ref(''),
  importVisible = ref(false),
  currentImport = ref<any>()
const proposals = computed(() => JSON.parse(currentImport.value?.requests || '[]')),
  rowErrors = computed(() => JSON.parse(currentImport.value?.errors || '[]'))
const displayValues = (v: Record<string, any>) =>
  Object.entries(v).map(([key, value]) => ({ name: labels[key] || key, value }))
let version = 0
const load = async () => {
  const id = batchId.value,
    v = ++version
  readiness.value = []
  records.value = []
  importBatches.value = []
  actions.value = []
  visible.value = false
  importVisible.value = false
  recordVisible.value = false
  if (!id) return
  loading.value = true
  try {
    const [ready, history, files, batch, entities] = await Promise.all([
      api.readiness(id),
      api.list(id),
      api.importList(id),
      BatchApi.getSalaryMonthRecord(id),
      EntityApi.list()
    ])
    if (v === version) {
      readiness.value = ready
      records.value = history
      importBatches.value = files
      actions.value = entities.find((e) => e.id === batch.entityId)?.allowedActions || []
    }
  } finally {
    if (v === version) loading.value = false
  }
}
const open = (row: any) => {
  draft.value = {
    batchId: batchId.value,
    employeeId: row.employeeId,
    kind: row.kind,
    mode: modesFor(row.kind)[0],
    values: {}
  }
  employeeName.value = `${row.jobNumber} ${row.employeeName}`
  previewValues.value = []
  visible.value = true
  nextTick(() => formRef.value?.clearValidate())
}
const modeChanged = () => {
  draft.value.values = {}
  previewValues.value = []
}
const payload = () => ({
  ...draft.value,
  values: draft.value.mode === 'IMPORT' ? draft.value.values : undefined
})
const inspect = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    previewValues.value = displayValues((await api.inspect(payload())).values)
  } finally {
    saving.value = false
  }
}
const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    await api.submit(payload())
    visible.value = false
    await load()
    message.success('已提交独立复核')
  } finally {
    saving.value = false
  }
}
const review = async (id: number, approve: boolean) => {
  const { value: reason } = await ElMessageBox.prompt('请填写核实来源或退回依据', '独立复核', {
    inputValidator: (v) => !!v?.trim() && v.length <= 1000
  })
  await api.review({ id, approve, reason })
  await load()
}
const showRecord = (row: any) => {
  currentRecord.value = row
  recordValues.value = displayValues(JSON.parse(row.payload))
  sourceProof.value = JSON.stringify(JSON.parse(row.sourceSnapshot), null, 2)
  recordVisible.value = true
}
const showImport = async (id: number) => {
  currentImport.value = await api.importDetails(id)
  importVisible.value = true
}
const upload = async (file: UploadFile) => {
  if (!batchId.value || !file.raw) return
  if (file.raw.size > 10 * 1024 * 1024) {
    message.warning('文件不能超过10MB')
    return
  }
  const id = batchId.value
  saving.value = true
  try {
    const result = await api.importPreview(id, file.raw)
    if (id !== batchId.value) return
    importBatches.value = await api.importList(id)
    currentImport.value = result
    importVisible.value = true
  } finally {
    saving.value = false
  }
}
const commit = async () => {
  if (!currentImport.value) return
  const id = currentImport.value.id
  await message.confirm('确认提交全部通过校验的输入？独立复核前不会参与工资核算。')
  saving.value = true
  try {
    await api.importCommit(id)
    await load()
    await showImport(id)
    message.success('已提交独立复核')
  } finally {
    saving.value = false
  }
}
const revalidate = async (id: number) => {
  await message.confirm('按当前来源生成新预览？原预览和错误记录会保留。')
  const next = await api.importRevalidate(id)
  await load()
  await showImport(next.id)
}
const downloadErrors = async (id: number) =>
  download.excel(await api.importErrors(id), '核算输入错误行.xlsx')
const downloadTemplate = async () => {
  if (batchId.value)
    download.excel(
      await api.template({
        batchId: batchId.value,
        kind: templateKind.value,
        mode: templateMode.value
      }),
      '核算输入模板.xlsx'
    )
}
</script>
