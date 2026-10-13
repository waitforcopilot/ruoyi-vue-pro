<template>
  <el-form inline>
    <el-form-item label="员工"
      ><el-select v-model="employeeId" filterable clearable style="width: 240px"
        ><el-option
          v-for="employee in employees"
          :key="employee.id"
          :value="employee.id"
          :label="`${employee.jobNumber} ${employee.name}`" /></el-select
    ></el-form-item>
    <el-button :loading="loading" @click="load">查询税务档案</el-button>
    <el-button v-hasPermi="['hrm:payroll:tax:update']" :disabled="!employeeId" @click="open"
      >提交年度档案</el-button
    >
  </el-form>
  <el-alert
    title="历史累计只填写本扣缴单位所属年度金额。零累计须明确声明；首次工资资格及外单位单独奖金使用情况须提供可核实依据。"
    type="info"
    :closable="false"
  />
  <el-table :data="rows" class="mt-10px">
    <el-table-column prop="taxpayerName" label="纳税人" /><el-table-column
      prop="maskedIdentifier"
      label="脱敏标识"
    /><el-table-column prop="version" label="版本" width="70" />
    <el-table-column label="状态"
      ><template #default="{ row }">{{
        states[row.status] || row.status
      }}</template></el-table-column
    >
    <el-table-column prop="openingAsOfDate" label="期初资料截止日" /><el-table-column
      prop="openingIncome"
      label="历史累计收入"
    /><el-table-column prop="openingWithheld" label="历史预缴" />
    <el-table-column label="操作" width="310"
      ><template #default="{ row }">
        <el-button link @click="proof(row)">依据</el-button
        ><el-button link @click="history(row.id)">履历</el-button>
        <el-button v-hasPermi="['hrm:payroll:tax:private']" link @click="identifier(row.id)"
          >核实完整标识</el-button
        >
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
  <Dialog v-model="visible" title="年度税务档案（待独立复核）" width="750px">
    <el-form label-width="230px">
      <el-form-item label="纳税人姓名"
        ><el-input v-model="draft.taxpayerName" readonly
      /></el-form-item>
      <el-form-item label="标识类型"
        ><el-select v-model="draft.identifierType"
          ><el-option value="ID_CARD" label="居民身份证" /><el-option
            value="PASSPORT"
            label="护照" /><el-option value="OTHER" label="其他税务认可标识" /></el-select
      ></el-form-item>
      <el-form-item label="签发国家代码"
        ><el-input v-model="draft.identifierCountry" maxlength="2" placeholder="CN 等两位大写代码"
      /></el-form-item>
      <el-form-item label="完整税务标识"
        ><el-input
          v-model="draft.identifier"
          show-password
          type="password"
          autocomplete="off"
          maxlength="64"
      /></el-form-item>
      <el-form-item label="本年度首次取得工资"
        ><el-switch v-model="draft.firstSalaryThisYear"
      /></el-form-item>
      <el-form-item label="资格核实前未取得其他工资"
        ><el-switch v-model="draft.noPriorSalaryThisYear"
      /></el-form-item>
      <el-form-item label="外单位已用奖金单独计税"
        ><el-switch v-model="draft.separateBonusUsedElsewhere"
      /></el-form-item>
      <el-form-item label="本单位历史累计状态"
        ><el-select v-model="draft.openingMode" placeholder="须明确选择"
          ><el-option value="ZERO" label="声明本单位当年无历史累计" /><el-option
            value="DECLARE"
            label="依据资料登记历史累计" /></el-select
      ></el-form-item>
      <el-form-item label="资料截止日"
        ><el-date-picker v-model="draft.openingAsOfDate" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item v-for="field in amounts" :key="field.key" :label="field.label"
        ><el-input-number
          v-model="draft[field.key]"
          :disabled="draft.openingMode === 'ZERO'"
          :min="0"
          :precision="2"
      /></el-form-item>
      <el-form-item label="正式资料出处"
        ><el-input
          v-model="draft.officialReference"
          type="textarea"
          maxlength="1000"
          placeholder="申报单位、年度、实名资料及累计记录出处"
      /></el-form-item>
      <el-form-item label="核实依据"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button :loading="saving" type="primary" @click="save">提交独立复核</el-button></template
    >
  </Dialog>
  <Dialog v-model="proofVisible" title="年度税务资料与复核履历" width="850px">
    <pre style="white-space: pre-wrap; word-break: break-all">{{ proofText }}</pre>
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/tax-profile'
import dayjs from 'dayjs'
const props = defineProps<{ entityId?: number; year: number }>()
const message = useMessage()
const employees = ref<any[]>([]),
  employeeId = ref<number>(),
  rows = ref<any[]>([]),
  loading = ref(false),
  visible = ref(false),
  saving = ref(false),
  proofVisible = ref(false),
  proofText = ref(''),
  draft = ref<any>({})
const states: Record<string, string> = {
  PENDING: '待复核',
  APPROVED: '已批准',
  SUPERSEDED: '历史批准档案',
  REJECTED: '已驳回'
}
const amounts = [
  { key: 'openingIncome', label: '本单位历史工资收入' },
  { key: 'openingSocialDeduction', label: '本单位历史专项扣除' },
  { key: 'openingBasicDeduction', label: '本单位历史减除费用' },
  { key: 'openingWithheld', label: '本单位历史预缴（工资）' }
]
let sequence = 0,
  employeeSequence = 0
async function load() {
  const version = ++sequence
  rows.value = []
  if (!props.entityId) return
  loading.value = true
  try {
    const result = await api.list({
      entityId: props.entityId,
      year: props.year,
      employeeId: employeeId.value
    })
    if (version === sequence) rows.value = result
  } finally {
    if (version === sequence) loading.value = false
  }
}
function open() {
  const employee = employees.value.find((row) => row.id === employeeId.value)
  if (!employee) return
  const prior = rows.value.find(
    (row) => row.employeeId === employee.id && row.status === 'APPROVED'
  )
  draft.value = {
    entityId: props.entityId,
    employeeId: employee.id,
    year: props.year,
    priorProfileId: prior?.id,
    requestKey: crypto.randomUUID(),
    taxpayerName: employee.name,
    identifierType: 'ID_CARD',
    identifierCountry: 'CN',
    identifier: '',
    firstSalaryThisYear: false,
    noPriorSalaryThisYear: false,
    separateBonusUsedElsewhere: false,
    openingAsOfDate: `${props.year}-01-01`,
    openingMode: undefined,
    openingIncome: undefined,
    openingSocialDeduction: undefined,
    openingBasicDeduction: undefined,
    openingWithheld: undefined,
    officialReference: '',
    evidence: ''
  }
  visible.value = true
}
async function save() {
  saving.value = true
  try {
    await api.create(draft.value)
    visible.value = false
    draft.value.identifier = ''
    await load()
  } finally {
    saving.value = false
  }
}
async function review(id: number, approved: boolean) {
  const { value } = await message.prompt(
    '请输入实名、年度及本扣缴单位来源复核意见',
    approved ? '批准年度税务档案' : '驳回税务档案'
  )
  await api.review({ id, approved, reason: value })
  await load()
}
function proof(row: any) {
  proofText.value = JSON.stringify(row, null, 2)
  proofVisible.value = true
}
async function history(id: number) {
  const version = sequence
  const result = await api.history(id)
  if (version === sequence) {
    proofText.value = JSON.stringify(result, null, 2)
    proofVisible.value = true
  }
}
async function identifier(id: number) {
  const version = sequence
  const value = await api.privateIdentifier(id)
  if (version === sequence) await message.alert(value, '完整税务标识（按授权核实）')
}
watch(
  () => [props.entityId, props.year],
  async () => {
    const version = ++employeeSequence
    sequence++
    employeeId.value = undefined
    employees.value = []
    visible.value = false
    draft.value.identifier = ''
    rows.value = []
    if (!props.entityId) return
    const result = await api.employees({ entityId: props.entityId, year: props.year })
    if (version === employeeSequence) employees.value = result
    await load()
  },
  { immediate: true }
)
watch(employeeId, () => {
  void load()
})
watch(
  () => draft.value.openingMode,
  (mode) => {
    for (const field of amounts) draft.value[field.key] = mode === 'ZERO' ? 0 : undefined
  }
)
watch(visible, (shown) => {
  if (!shown) draft.value.identifier = ''
})
</script>
