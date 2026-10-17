<template>
  <ContentWrap>
    <RequirementContext module="CALC" />
    <el-alert
      title="独立收回核定的税前多发工资。须有员工确认、按原到账年度批准的收入更正及真实退款凭证；原银行付款和已扣税额保留，补退税另行结清。"
      :closable="false"
      type="info"
    />
    <el-form inline class="mt-4"
      ><el-form-item label="法人主体"
        ><el-select v-model="entityId" @change="load"
          ><el-option
            v-for="e in entities"
            :key="e.id"
            :value="e.id"
            :label="e.name" /></el-select></el-form-item
      ><el-button :disabled="!entityId" @click="load">刷新</el-button
      ><el-button
        v-hasPermi="['hrm:payroll:recovery:update']"
        :disabled="!entityId"
        @click="openCreate"
        >登记独立收回</el-button
      ></el-form
    >
    <el-table :data="rows" v-loading="loading"
      ><el-table-column prop="id" label="编号" width="75" /><el-table-column
        prop="paymentId"
        label="原付款编号"
      /><el-table-column prop="employeeId" label="员工编号" /><el-table-column
        prop="originalIncome"
        label="核定前收入"
      /><el-table-column prop="correctedIncome" label="核定后收入" /><el-table-column
        prop="amount"
        label="收回金额"
      /><el-table-column label="状态"
        ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
      ><el-table-column prop="valueDate" label="实际退款日" min-width="120" /><el-table-column
        prop="employeeAcknowledgement"
        label="员工确认"
        show-overflow-tooltip
      /><el-table-column label="操作" width="280"
        ><template #default="{ row }">
          <el-button link @click="view(row)">依据及履历</el-button
          ><template v-if="row.status === 'PENDING'"
            ><el-button
              v-hasPermi="['hrm:payroll:recovery:review']"
              link
              @click="review(row.id, true)"
              >批准</el-button
            ><el-button
              v-hasPermi="['hrm:payroll:recovery:review']"
              link
              @click="review(row.id, false)"
              >驳回</el-button
            ></template
          >
          <el-button
            v-if="row.status === 'APPROVED'"
            v-hasPermi="['hrm:payroll:recovery:confirm']"
            link
            @click="openReceipt(row)"
            >核实退款</el-button
          ><el-button
            v-if="['PENDING', 'APPROVED'].includes(row.status)"
            v-hasPermi="['hrm:payroll:recovery:confirm']"
            link
            @click="cancel(row)"
            >撤销</el-button
          >
        </template></el-table-column
      ></el-table
    >
  </ContentWrap>
  <Dialog v-model="createVisible" title="登记独立工资收回" width="900px">
    <el-form label-width="130px"
      ><el-form-item label="原常规工资付款"
        ><el-select v-model="draft.paymentId" filterable style="width: 100%" @change="loadBaseline"
          ><el-option
            v-for="p in paymentOptions"
            :key="p.id"
            :value="p.id"
            :label="p.name" /></el-select
      ></el-form-item>
      <template v-if="baseline"
        ><el-form-item label="原实际到账日">{{ baseline.originalCashDate }}</el-form-item
        ><el-form-item label="接续批准差额"
          >{{ baseline.priorRetroId || '首次核定' }}；当前核定收入
          {{ baseline.income }}</el-form-item
        ><el-form-item label="核定后税前收入"
          ><el-input-number v-model="draft.correctedIncome" :min="0" :precision="2" /></el-form-item
        ><el-form-item label="应收回金额">{{ recoveryAmount.toFixed(2) }} 元</el-form-item>
        <el-alert
          title="选择已独立批准、尚未被收回申请占用的税务收入更正。跨年或多次补发时，可分配到本条更正链上的多个真实付款；合计必须等于收回金额。"
          type="info"
          :closable="false"
        />
        <el-table
          :data="baseline.taxAllocations"
          row-key="correctionId"
          @selection-change="selection"
          ><el-table-column type="selection" width="55" /><el-table-column
            prop="paymentId"
            label="付款编号" /><el-table-column
            prop="cashYear"
            label="原到账年度" /><el-table-column
            prop="correctionId"
            label="批准税务更正" /><el-table-column
            prop="originalIncome"
            label="更正前计税收入" /><el-table-column
            prop="correctedIncome"
            label="更正后计税收入" /><el-table-column
            prop="incomeReduction"
            label="收入减额" /></el-table
        ><p>所选税务减额合计 {{ allocationTotal.toFixed(2) }} 元</p> </template
      ><el-form-item label="员工确认资料"
        ><el-input
          v-model="draft.employeeAcknowledgement"
          type="textarea"
          maxlength="1000" /></el-form-item
      ><el-form-item label="核定及收回依据"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000" /></el-form-item
    ></el-form>
    <template #footer
      ><el-button
        type="primary"
        :loading="saving"
        :disabled="
          !draft.paymentId ||
          !baseline ||
          recoveryAmount <= 0 ||
          Math.abs(recoveryAmount - allocationTotal) > 0.00001 ||
          !draft.employeeAcknowledgement.trim() ||
          !draft.evidence.trim()
        "
        @click="create"
        >提交独立复核</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="receiptVisible" title="核实实际工资退款" width="620px"
    ><el-form label-width="130px"
      ><el-form-item label="工单编号">{{ receipt.id }}</el-form-item
      ><el-form-item label="核定收回金额">{{ receipt.amount }} 元</el-form-item
      ><el-form-item label="实际到账日"
        ><el-date-picker
          v-model="receipt.valueDate"
          type="date"
          value-format="YYYY-MM-DD" /></el-form-item
      ><el-form-item label="唯一退款流水"
        ><el-input v-model="receipt.reference" maxlength="100" /></el-form-item
      ><el-form-item label="实际到账凭证"
        ><el-input
          v-model="receipt.evidence"
          type="textarea"
          maxlength="1000" /></el-form-item></el-form
    ><template #footer
      ><el-button
        type="primary"
        :loading="saving"
        :disabled="!receipt.valueDate || !receipt.reference.trim() || !receipt.evidence.trim()"
        @click="confirm"
        >确认真实到账</el-button
      ></template
    ></Dialog
  >
  <Dialog v-model="historyVisible" title="工资收回依据及履历" width="950px"
    ><template v-if="selected"
      ><el-descriptions :column="2" border
        ><el-descriptions-item label="核定收回"
          >{{ selected.originalIncome }} → {{ selected.correctedIncome }}，收回
          {{ selected.amount }}</el-descriptions-item
        ><el-descriptions-item label="员工确认">{{
          selected.employeeAcknowledgement
        }}</el-descriptions-item
        ><el-descriptions-item label="原核定依据">{{ selected.evidence }}</el-descriptions-item
        ><el-descriptions-item label="复核意见">{{ selected.reviewReason }}</el-descriptions-item
        ><el-descriptions-item label="实际退款凭证"
          >{{ selected.receiptReference }} · {{ selected.receiptEvidence }}</el-descriptions-item
        ></el-descriptions
      ><el-table :data="allocations"
        ><el-table-column prop="correctionId" label="批准税务更正" /><el-table-column
          prop="paymentId"
          label="付款编号" /><el-table-column prop="cashYear" label="原现金年度" /><el-table-column
          prop="incomeReduction"
          label="收入减额" /></el-table></template
    ><el-table :data="events"
      ><el-table-column prop="action" label="动作" /><el-table-column
        prop="actorId"
        label="操作人编号" /><el-table-column prop="reason" label="依据" /><el-table-column
        prop="createTime"
        label="时间"
        min-width="165" /></el-table
  ></Dialog>
</template>
<script setup lang="ts">
import RequirementContext from '../components/RequirementContext.vue'
import request from '@/config/axios'
defineOptions({ name: 'HrmPayrollWageRecovery' })
const base = '/hrm/payroll/wage-recovery',
  message = useMessage(),
  entityId = ref<number>(),
  entities = ref<any[]>([]),
  paymentOptions = ref<any[]>([]),
  rows = ref<any[]>([]),
  events = ref<any[]>([]),
  baseline = ref<any>(),
  selected = ref<any>(),
  allocations = ref<any[]>([]),
  chosen = ref<any[]>([]),
  loading = ref(false),
  saving = ref(false),
  createVisible = ref(false),
  receiptVisible = ref(false),
  historyVisible = ref(false)
const states = {
  PENDING: '待独立复核',
  APPROVED: '待实际退款',
  REJECTED: '已驳回',
  CANCELLED: '已撤销',
  SETTLED: '已结清'
}
const draft = reactive<any>({ employeeAcknowledgement: '', evidence: '' }),
  receipt = reactive<any>({ reference: '', evidence: '' })
const recoveryAmount = computed(
    () => Number(baseline.value?.income || 0) - Number(draft.correctedIncome || 0)
  ),
  allocationTotal = computed(() =>
    chosen.value.reduce((sum, a) => sum + Number(a.incomeReduction), 0)
  )
async function load() {
  rows.value = []
  if (!entityId.value) return
  loading.value = true
  try {
    rows.value = await request.get({ url: base + '/list', params: { entityId: entityId.value } })
  } finally {
    loading.value = false
  }
}
async function openCreate() {
  paymentOptions.value = await request.get({
    url: base + '/payments',
    params: { entityId: entityId.value }
  })
  baseline.value = undefined
  chosen.value = []
  Object.assign(draft, {
    paymentId: undefined,
    priorRetroId: undefined,
    correctedIncome: undefined,
    taxCorrectionIds: [],
    employeeAcknowledgement: '',
    evidence: '',
    requestKey: crypto.randomUUID()
  })
  createVisible.value = true
}
async function loadBaseline() {
  baseline.value = undefined
  chosen.value = []
  if (!draft.paymentId) return
  baseline.value = await request.get({
    url: base + '/baseline',
    params: { paymentId: draft.paymentId }
  })
  draft.priorRetroId = baseline.value.priorRetroId
  draft.correctedIncome = Number(baseline.value.income)
}
function selection(values: any[]) {
  chosen.value = values
  draft.taxCorrectionIds = values.map((v) => v.correctionId)
}
async function create() {
  saving.value = true
  try {
    await request.post({ url: base + '/create', data: { ...draft } })
    createVisible.value = false
    message.success('已提交独立复核')
    await load()
  } finally {
    saving.value = false
  }
}
async function reason(title: string) {
  const { value } = await message.prompt(title, '核实依据', {
    inputType: 'textarea',
    inputValidator: (v: string) => (!!v?.trim() && v.length <= 1000) || '请填写1～1000字的依据'
  })
  return value
}
async function review(id: number, approved: boolean) {
  const note = await reason(
    approved ? '请填写独立核实工资差额、员工确认及税务分配的依据' : '请填写驳回原因'
  )
  await request.post({ url: base + '/review', data: { id, approved, reason: note } })
  await load()
}
function openReceipt(row: any) {
  Object.assign(receipt, {
    id: row.id,
    amount: Number(row.amount),
    valueDate: undefined,
    reference: '',
    evidence: ''
  })
  receiptVisible.value = true
}
async function confirm() {
  saving.value = true
  try {
    await request.post({ url: base + '/confirm', data: { ...receipt } })
    receiptVisible.value = false
    message.success('实际退款已核实')
    await load()
  } finally {
    saving.value = false
  }
}
async function cancel(row: any) {
  const note = await reason('请填写撤销原因')
  const evidence =
    row.status === 'APPROVED' ? await reason('请填写已批准退款指令的作废证明') : undefined
  await request.post({ url: base + '/cancel', data: { id: row.id, reason: note, evidence } })
  await load()
}
async function view(row: any) {
  events.value = await request.get({ url: base + '/history', params: { id: row.id } })
  selected.value = row
  allocations.value = JSON.parse(row.allocationSnapshot || '[]')
  historyVisible.value = true
}
onMounted(async () => {
  entities.value = await request.get({ url: base + '/entities' })
})
</script>
