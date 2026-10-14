<template>
  <el-divider content-position="left">专项附加扣除申报资料</el-divider>
  <el-form inline>
    <el-form-item label="报告月份"
      ><el-select v-model="month" style="width: 120px"
        ><el-option v-for="m in 12" :key="m" :value="m" :label="`${m}月`" /></el-select
    ></el-form-item>
    <el-button :loading="loading" :disabled="!employeeId" @click="load">查询扣除来源</el-button>
    <el-button v-hasPermi="['hrm:payroll:tax:update']" :disabled="!employeeId" @click="open"
      >提交申报资料</el-button
    >
  </el-form>
  <el-alert
    title="先选择员工。金额采用经授权的扣缴申报端本年累计资料，提交后需独立复核。工资输入可同步批准版本，资料变更后须重新复核。大病医疗仅年度汇算，不填入工资预扣资料。"
    type="info"
    :closable="false"
  />
  <el-table :data="rows" class="mt-10px">
    <el-table-column prop="version" label="版本" /><el-table-column
      prop="sourceDate"
      label="资料日期"
    />
    <el-table-column label="状态"
      ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
    >
    <el-table-column prop="officialReference" label="正式来源" min-width="220" />
    <el-table-column label="操作" width="240"
      ><template #default="{ row }">
        <el-button link @click="proof(row)">金额与履历</el-button>
        <el-button
          v-if="row.status === 'PENDING'"
          v-hasPermi="['hrm:payroll:tax:approve']"
          link
          @click="review(row.id, true)"
          >批准</el-button
        >
        <el-button
          v-if="row.status === 'PENDING'"
          v-hasPermi="['hrm:payroll:tax:approve']"
          link
          @click="review(row.id, false)"
          >驳回</el-button
        >
      </template></el-table-column
    >
  </el-table>
  <Dialog v-model="visible" title="提交累计扣除资料（待独立复核）" width="700px">
    <el-form label-width="190px">
      <el-form-item label="年度 / 报告月份">{{ year }}年 {{ month }}月</el-form-item>
      <el-form-item label="资料日期"
        ><el-date-picker v-model="draft.sourceDate" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="发生额声明"
        ><el-select v-model="draft.mode" placeholder="明确选择"
          ><el-option value="ZERO" label="依据资料明确无扣除" /><el-option
            value="DECLARE"
            label="依据扣缴申报资料登记" /></el-select
      ></el-form-item>
      <el-form-item v-for="field in fields" :key="field.code" :label="field.name"
        ><el-input-number
          v-model="draft.values[field.code]"
          :min="0"
          :precision="2"
          :disabled="draft.mode === 'ZERO'"
      /></el-form-item>
      <el-form-item label="授权资料正式来源"
        ><el-input
          v-model="draft.officialReference"
          type="textarea"
          maxlength="1000"
          placeholder="扣缴申报单位、报告年度/月及授权资料编号"
      /></el-form-item>
      <el-form-item label="复核依据"
        ><el-input
          v-model="draft.evidence"
          type="textarea"
          maxlength="1000"
          placeholder="身份、所属单位、有效申报月份及累计金额的核实依据"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button :loading="saving" type="primary" @click="save">提交独立复核</el-button></template
    >
  </Dialog>
  <Dialog v-model="proofVisible" title="扣除金额与版本履历" width="850px">
    <pre style="white-space: pre-wrap; word-break: break-all">{{ proofText }}</pre>
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/tax-deduction'
import dayjs from 'dayjs'
const props = defineProps<{ entityId?: number; employeeId?: number; year: number }>()
const message = useMessage(),
  month = ref(dayjs().month() + 1),
  rows = ref<any[]>([]),
  loading = ref(false),
  visible = ref(false),
  saving = ref(false),
  draft = ref<any>({ values: {} }),
  proofVisible = ref(false),
  proofText = ref('')
const states: Record<string, string> = {
  PENDING: '待独立复核',
  APPROVED: '已批准',
  SUPERSEDED: '历史批准版本',
  REJECTED: '已驳回'
}
const fields = [
  { code: '260101', name: '累计子女教育' },
  { code: '260102', name: '累计住房租金' },
  { code: '260103', name: '累计住房贷款利息' },
  { code: '260104', name: '累计赡养老人' },
  { code: '260105', name: '累计继续教育' },
  { code: '260106', name: '累计婴幼儿照护' }
]
let sequence = 0
async function load() {
  const version = ++sequence
  rows.value = []
  if (!props.entityId || !props.employeeId) return
  loading.value = true
  try {
    const data = await api.list({
      entityId: props.entityId,
      employeeId: props.employeeId,
      year: props.year,
      month: month.value
    })
    if (version === sequence) rows.value = data
  } finally {
    if (version === sequence) loading.value = false
  }
}
async function open() {
  await load()
  if (!props.entityId || !props.employeeId) return
  draft.value = {
    entityId: props.entityId,
    employeeId: props.employeeId,
    year: props.year,
    month: month.value,
    priorDeclarationId: rows.value.find((row) => row.status === 'APPROVED')?.id,
    requestKey: crypto.randomUUID(),
    values: {}
  }
  visible.value = true
}
async function save() {
  saving.value = true
  try {
    await api.create(draft.value)
    visible.value = false
    message.success('已提交独立复核')
    await load()
  } finally {
    saving.value = false
  }
}
async function review(id: number, approved: boolean) {
  const reason = await message.prompt(approved ? '填写独立核对依据' : '填写退回原因')
  if (!reason) return
  await api.review({ id, approved, reason })
  await load()
}
async function proof(row: any) {
  const version = sequence
  const events = await api.history(row.id)
  if (version !== sequence) return
  const amounts = JSON.parse(row.valuesSnapshot)
  proofText.value = JSON.stringify(
    {
      资料: row,
      金额: fields.map((field) => ({ 项目: field.name, 累计金额: amounts[field.code] })),
      履历: events
    },
    null,
    2
  )
  proofVisible.value = true
}
watch(
  () => [props.entityId, props.employeeId, props.year, month.value],
  () => {
    visible.value = false
    proofVisible.value = false
    draft.value = { values: {} }
    void load()
  },
  { immediate: true }
)
watch(
  () => draft.value.mode,
  (mode) => {
    for (const field of fields) draft.value.values[field.code] = mode === 'ZERO' ? 0 : undefined
  }
)
</script>
