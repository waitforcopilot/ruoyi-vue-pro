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
    title="历史累计只填写本扣缴单位所属年度金额。零累计须明确声明；居民身份、首次工资在新入职前的资格及外单位奖金使用情况须提供可核实依据。"
    type="info"
    :closable="false"
  />
  <el-table :data="rows" class="mt-10px">
    <el-table-column prop="taxpayerName" label="纳税人" /><el-table-column
      prop="maskedIdentifier"
      label="脱敏标识"
    /><el-table-column label="居民资格"
      ><template #default="{ row }">{{
        row.residentThisYear == null ? '待补充核实' : row.residentThisYear ? '居民' : '非居民'
      }}</template></el-table-column
    ><el-table-column prop="version" label="版本" width="70" />
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
        <el-button v-if="row.status === 'APPROVED'" link @click="openLedger(row)"
          >已发工资年度台账</el-button
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
  <TaxDeductions :entity-id="entityId" :employee-id="employeeId" :year="year" />
  <Dialog v-model="visible" title="年度税务档案（待独立复核）" width="750px">
    <el-form label-width="230px">
      <el-form-item label="纳税人姓名"
        ><el-input v-model="draft.taxpayerName" readonly
      /></el-form-item>
      <el-form-item v-if="savedIdentity?.id" label="已核实身份"
        ><el-checkbox v-model="reuseIdentity"
          >沿用 {{ savedIdentity.year }} 年批准身份
          {{ savedIdentity.maskedIdentifier }}</el-checkbox
        ></el-form-item
      >
      <el-form-item label="标识类型"
        ><el-select v-model="draft.identifierType" :disabled="reuseIdentity"
          ><el-option value="ID_CARD" label="居民身份证" /><el-option
            value="PASSPORT"
            label="护照" /><el-option value="OTHER" label="其他税务认可标识" /></el-select
      ></el-form-item>
      <el-form-item label="签发国家代码"
        ><el-input
          v-model="draft.identifierCountry"
          :disabled="reuseIdentity"
          maxlength="2"
          placeholder="CN 等两位大写代码"
      /></el-form-item>
      <el-form-item v-if="!reuseIdentity" label="完整税务标识"
        ><el-input
          v-model="draft.identifier"
          show-password
          type="password"
          autocomplete="off"
          maxlength="64"
      /></el-form-item>
      <el-form-item label="年度税收居民资格"
        ><el-select v-model="draft.residentThisYear" placeholder="须依据资料明确选择"
          ><el-option :value="true" label="已核实居民个人" /><el-option
            :value="false"
            label="非居民，不能套用居民累计模型" /></el-select
      ></el-form-item>
      <el-form-item label="本年首次工资优惠资格"
        ><el-switch v-model="draft.firstSalaryThisYear"
      /></el-form-item>
      <el-form-item label="本年新入职前未取得工资薪金"
        ><el-switch v-model="draft.noPriorSalaryThisYear"
      /></el-form-item>
      <el-form-item label="本年新入职前无连续性劳务"
        ><el-switch v-model="draft.noPriorContinuousRemunerationBeforeEntry"
      /></el-form-item>
      <el-form-item label="外单位已用奖金单独计税"
        ><el-switch v-model="draft.separateBonusUsedElsewhere"
      /></el-form-item>
      <el-form-item label="本单位历史累计状态"
        ><el-select v-model="draft.openingMode" placeholder="须明确选择"
          ><el-option value="ZERO" label="确认无额外历史累计资料，仍归集系统已发事实" /><el-option
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
  <Dialog v-model="ledgerVisible" title="按实际到账年度归集已发工资" width="1100px">
    <el-form inline
      ><el-form-item label="累计截止日"
        ><el-date-picker v-model="ledgerDate" value-format="YYYY-MM-DD" /></el-form-item
      ><el-button :loading="ledgerLoading" @click="loadLedger">核对年度台账</el-button
      ><el-button :loading="employmentLoading" @click="loadEmployment"
        >核对任职月份</el-button
      ></el-form
    >
    <el-alert
      title="引用成功回盘与冻结工资版本。历史累计覆盖截止日内已发金额，不重复相加；缺日期或税务来源时累计不可使用。此台账尚未替代工资核算的累计输入。"
      type="info"
      :closable="false"
    />
    <el-descriptions v-if="employmentData" :column="3" border class="mt-10px">
      <el-descriptions-item label="减除费用口径">{{
        employmentData.mode === 'EMPLOYMENT_MONTHS'
          ? '本单位当年任职月份'
          : '已核实首次工资，从一月起计'
      }}</el-descriptions-item>
      <el-descriptions-item label="计入月份">{{
        employmentData.months.map((month: number) => `${month}月`).join('、') || '本年无任职月份'
      }}</el-descriptions-item>
      <el-descriptions-item label="按批准政策应减除费用">{{
        employmentData.basicDeduction
      }}</el-descriptions-item>
    </el-descriptions>
    <template v-if="ledgerData">
      <el-alert
        v-for="reason in ledgerData.blockers"
        :key="reason"
        :title="reason"
        type="error"
        :closable="false"
        class="mt-10px"
      />
      <el-descriptions v-if="ledgerData.ready" :column="4" border class="mt-10px">
        <el-descriptions-item label="工资收入">{{ ledgerData.income }}</el-descriptions-item
        ><el-descriptions-item label="专项扣除">{{
          ledgerData.socialDeduction
        }}</el-descriptions-item
        ><el-descriptions-item label="减除费用">{{
          ledgerData.basicDeduction
        }}</el-descriptions-item
        ><el-descriptions-item label="工资已预缴">{{ ledgerData.withheld }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="ledgerData.rows" class="mt-10px">
        <el-table-column label="税务更正" width="130"
          ><template #default="{ row }">
            <el-button
              v-hasPermi="['hrm:payroll:tax:query']"
              link
              type="primary"
              @click="taxCorrectionRef?.open(row.paymentId, Number(row.basicDeduction || 0))"
              >{{ row.correctionId ? '更正履历' : '核实更正' }}</el-button
            >
          </template></el-table-column
        >
        <el-table-column prop="paymentId" label="回盘明细" /><el-table-column
          prop="batchId"
          label="工资批次"
        /><el-table-column prop="runId" label="冻结版本" /><el-table-column
          prop="valueDate"
          label="实际到账日"
          width="120"
        />
        <el-table-column label="归集状态" width="140"
          ><template #default="{ row }">{{ ledgerStates[row.status] }}</template></el-table-column
        >
        <el-table-column prop="income" label="本次工资收入" /><el-table-column
          prop="socialDeduction"
          label="专项扣除"
        /><el-table-column prop="basicDeduction" label="减除费用" /><el-table-column
          prop="withheld"
          label="工资预缴"
        /><el-table-column prop="separateBonusIncome" label="单独计税奖金" /><el-table-column
          prop="separateBonusTax"
          label="奖金税额"
        /><el-table-column prop="reason" label="说明" min-width="210" />
      </el-table>
    </template>
  </Dialog>
  <Dialog v-model="proofVisible" title="年度税务资料与复核履历" width="850px">
    <pre style="white-space: pre-wrap; word-break: break-all">{{ proofText }}</pre>
  </Dialog>
  <TaxCorrection
    :key="`${props.entityId}-${props.year}`"
    ref="taxCorrectionRef"
    @changed="loadLedger"
  />
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/tax-profile'
import dayjs from 'dayjs'
import TaxDeductions from './TaxDeductions.vue'
import TaxCorrection from './TaxCorrection.vue'
const taxCorrectionRef = ref<InstanceType<typeof TaxCorrection>>()
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
const savedIdentity = ref<any>(),
  reuseIdentity = ref(false)
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
const ledgerVisible = ref(false),
  ledgerLoading = ref(false),
  ledgerDate = ref(''),
  ledgerData = ref<any>(),
  ledgerEmployeeId = ref<number>()
const employmentData = ref<any>(),
  employmentLoading = ref(false)
let employmentSequence = 0
const ledgerStates: Record<string, string> = {
  COUNTED: '已归入累计',
  OPENING_COVERED: '历史资料已覆盖',
  UNDATED: '缺到账日期',
  TAX_CORRECTION_STALE: '更正来源需重新核实',
  TAX_PERIOD_MISMATCH: '到账税务期需更正',
  TAX_SUBJECT_MISMATCH: '扣缴主体需更正',
  SOURCE_INVALID: '税务来源待核实'
}
let ledgerSequence = 0
async function openLedger(row: any) {
  employmentSequence++
  employmentData.value = undefined
  employmentLoading.value = false
  ledgerEmployeeId.value = row.employeeId
  const end = `${props.year}-12-31`,
    today = dayjs().format('YYYY-MM-DD')
  ledgerDate.value = end < today ? end : today
  ledgerData.value = undefined
  ledgerVisible.value = true
  await loadLedger()
}
async function loadEmployment() {
  const version = ++employmentSequence,
    sourceVersion = ledgerSequence
  employmentData.value = undefined
  if (!props.entityId || !ledgerEmployeeId.value || !ledgerDate.value) return
  employmentLoading.value = true
  try {
    const data = await api.basicDeduction({
      entityId: props.entityId,
      employeeId: ledgerEmployeeId.value,
      payDate: ledgerDate.value
    })
    if (version === employmentSequence && sourceVersion === ledgerSequence)
      employmentData.value = data
  } finally {
    if (version === employmentSequence) employmentLoading.value = false
  }
}
async function loadLedger() {
  const version = ++ledgerSequence
  ledgerData.value = undefined
  if (!props.entityId || !ledgerEmployeeId.value || !ledgerDate.value) return
  ledgerLoading.value = true
  try {
    const data = await api.cashLedger({
      entityId: props.entityId,
      employeeId: ledgerEmployeeId.value,
      year: props.year,
      asOfDate: ledgerDate.value
    })
    if (version === ledgerSequence) ledgerData.value = data
  } finally {
    if (version === ledgerSequence) ledgerLoading.value = false
  }
}
watch(
  ledgerDate,
  () => {
    ledgerSequence++
    ledgerData.value = undefined
    ledgerLoading.value = false
    employmentSequence++
    employmentData.value = undefined
    employmentLoading.value = false
  },
  { flush: 'sync' }
)
watch(ledgerVisible, (shown) => {
  if (!shown) {
    ledgerSequence++
    ledgerData.value = undefined
    ledgerLoading.value = false
    employmentSequence++
    employmentData.value = undefined
    employmentLoading.value = false
  }
})
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
async function open() {
  const version = sequence
  const entityId = props.entityId,
    targetId = employeeId.value
  if (!entityId || !targetId) return
  savedIdentity.value = undefined
  reuseIdentity.value = false
  const source = await api.identitySource({ entityId, employeeId: targetId, year: props.year })
  if (version !== sequence || entityId !== props.entityId || targetId !== employeeId.value) return
  savedIdentity.value = source?.id ? source : undefined
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
    residentThisYear: prior?.residentThisYear,
    noPriorContinuousRemunerationBeforeEntry:
      prior?.noPriorContinuousRemunerationBeforeEntry ?? false,
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
  if (savedIdentity.value) {
    draft.value.identifierType = savedIdentity.value.identifierType
    draft.value.identifierCountry = savedIdentity.value.identifierCountry
    reuseIdentity.value = true
  }
  visible.value = true
}
watch(
  reuseIdentity,
  (enabled) => {
    if (enabled && savedIdentity.value?.id) {
      draft.value.identitySourceProfileId = savedIdentity.value.id
      draft.value.identifier = undefined
      draft.value.identifierType = savedIdentity.value.identifierType
      draft.value.identifierCountry = savedIdentity.value.identifierCountry
    } else {
      draft.value.identitySourceProfileId = undefined
      draft.value.identifier = undefined
    }
  },
  { flush: 'sync' }
)
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
    ledgerVisible.value = false
    ledgerSequence++
    ledgerData.value = undefined
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
  if (!shown) {
    draft.value.identifier = ''
    savedIdentity.value = undefined
    reuseIdentity.value = false
  }
})
</script>
