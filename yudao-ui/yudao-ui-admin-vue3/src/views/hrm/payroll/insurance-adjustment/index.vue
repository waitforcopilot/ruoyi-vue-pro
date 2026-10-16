<template>
  <ContentWrap>
    <el-alert
      title="按原工资个人扣款与最终核实缴费账单结算差额。个人补收为正、退还为负；单位差额单独调整历史成本。原年度扣除须先正式更正，税款另行结清。"
      type="info"
      :closable="false"
    />
    <el-form inline class="mt-4">
      <el-form-item label="法人主体"
        ><el-select v-model="entityId" @change="load"
          ><el-option v-for="e in entities" :key="e.id" :value="e.id" :label="e.name" /></el-select
      ></el-form-item>
      <el-button :disabled="!entityId" @click="load">刷新</el-button>
      <el-button
        v-hasPermi="['hrm:payroll:insurance-adjustment:update']"
        :disabled="!entityId"
        @click="openCreate"
        >登记工资缴费差额</el-button
      >
    </el-form>
    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="id" label="编号" width="75" /><el-table-column
        prop="paymentId"
        label="原工资付款"
      /><el-table-column prop="employeeId" label="员工编号" /><el-table-column
        prop="billId"
        label="最终账单"
      />
      <el-table-column prop="personalDelta" label="个人补收/退还" /><el-table-column
        prop="corporateDelta"
        label="单位成本差额"
      />
      <el-table-column label="状态"
        ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
      >
      <el-table-column prop="valueDate" label="结清日期" min-width="110" />
      <el-table-column label="操作" width="290"
        ><template #default="{ row }">
          <el-button link @click="history(row)">依据及履历</el-button>
          <template v-if="row.status === 'PENDING'"
            ><el-button
              v-hasPermi="['hrm:payroll:insurance-adjustment:review']"
              link
              @click="review(row, true)"
              >批准</el-button
            ><el-button
              v-hasPermi="['hrm:payroll:insurance-adjustment:review']"
              link
              @click="review(row, false)"
              >驳回</el-button
            ></template
          >
          <el-button
            v-if="row.status === 'APPROVED'"
            v-hasPermi="['hrm:payroll:insurance-adjustment:confirm']"
            link
            @click="openReceipt(row)"
            >{{
              Number(row.personalDelta) === 0
                ? '核实成本记账'
                : Number(row.personalDelta) > 0
                  ? '核实收款'
                  : '核实退款'
            }}</el-button
          >
          <el-button
            v-if="['PENDING', 'APPROVED'].includes(row.status)"
            v-hasPermi="['hrm:payroll:insurance-adjustment:confirm']"
            link
            @click="cancel(row)"
            >撤销</el-button
          >
        </template></el-table-column
      >
    </el-table>
  </ContentWrap>
  <Dialog v-model="createVisible" title="登记社保公积金工资差额" width="820px">
    <el-form label-width="145px">
      <el-form-item label="原常规工资付款"
        ><el-select v-model="draft.paymentId" filterable style="width: 100%" @change="resetProof"
          ><el-option
            v-for="p in options.payments"
            :key="p.id"
            :value="p.id"
            :label="p.name" /></el-select
      ></el-form-item>
      <el-form-item label="最终核实账单"
        ><el-select
          v-model="draft.billId"
          filterable
          style="width: 100%"
          @change="proof = undefined"
          ><el-option
            v-for="b in options.bills"
            :key="b.id"
            :value="b.id"
            :label="b.name" /></el-select
      ></el-form-item>
      <el-form-item label="原年度税务更正"
        ><el-select
          v-model="draft.correctionId"
          clearable
          style="width: 100%"
          @change="proof = undefined"
          ><el-option
            v-for="c in corrections"
            :key="c.id"
            :value="c.id"
            :label="c.name" /></el-select
      ></el-form-item>
      <el-form-item label="员工确认"
        ><el-input v-model="draft.employeeAcknowledgement" type="textarea" maxlength="1000"
      /></el-form-item>
      <el-form-item label="调整依据"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000"
      /></el-form-item>
      <el-form-item
        ><el-button :disabled="!ready" @click="preview">校验并计算差额</el-button></el-form-item
      >
      <template v-if="proof"
        ><el-form-item label="原缴费期间"
          >{{ proof.original.year }}-{{ proof.original.month }}</el-form-item
        ><el-form-item label="个人差额"
          >{{ proof.personalDelta }} 元（正数补收、负数退还）</el-form-item
        ><el-form-item label="单位成本调整">{{ proof.corporateDelta }} 元</el-form-item
        ><el-form-item label="更正后原年度扣除"
          >{{ proof.correctedSocial }} 元</el-form-item
        ></template
      >
    </el-form>
    <template #footer
      ><el-button :loading="saving" :disabled="!proof || !ready" type="primary" @click="create"
        >提交独立复核</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="receiptVisible" title="核实收退或成本记账" width="680px">
    <el-alert
      :title="
        Number(selected?.personalDelta) === 0
          ? '个人无收退：填写真实单位成本记账凭证，金额为0。'
          : Number(selected?.personalDelta) > 0
            ? '核实员工实际补缴收款，金额须为个人差额绝对值。'
            : '核实向员工实际退款，金额须为个人差额绝对值。'
      "
      :closable="false"
      type="info"
    />
    <el-form label-width="120px" class="mt-4"
      ><el-form-item label="核实金额（元）">{{ receipt.amount }}</el-form-item
      ><el-form-item label="实际日期"
        ><el-date-picker v-model="receipt.valueDate" value-format="YYYY-MM-DD" /></el-form-item
      ><el-form-item label="唯一凭证编号"
        ><el-input v-model="receipt.reference" maxlength="100" /></el-form-item
      ><el-form-item label="凭证核实依据"
        ><el-input v-model="receipt.evidence" type="textarea" maxlength="1000" /></el-form-item
    ></el-form>
    <template #footer
      ><el-button
        :loading="saving"
        :disabled="!receipt.valueDate || !receipt.reference?.trim() || !receipt.evidence?.trim()"
        type="primary"
        @click="confirm"
        >确认结清</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="historyVisible" title="依据及履历" width="850px"
    ><el-descriptions :column="1" border
      ><el-descriptions-item label="员工确认">{{
        selected?.employeeAcknowledgement
      }}</el-descriptions-item
      ><el-descriptions-item label="调整依据">{{ selected?.evidence }}</el-descriptions-item
      ><el-descriptions-item label="结清凭证"
        >{{ selected?.receiptReference }} {{ selected?.receiptEvidence }}</el-descriptions-item
      ></el-descriptions
    ><el-table :data="events"
      ><el-table-column prop="action" label="动作" /><el-table-column
        prop="actorId"
        label="操作人" /><el-table-column prop="reason" label="依据" /><el-table-column
        prop="createTime"
        label="时间"
        min-width="160" /></el-table
  ></Dialog>
</template>
<script setup lang="ts">
import request from '@/config/axios'
defineOptions({ name: 'HrmPayrollInsuranceAdjustment' })
const base = '/hrm/payroll/insurance-adjustment',
  message = useMessage()
const entityId = ref<number>(),
  entities = ref<any[]>([]),
  rows = ref<any[]>([]),
  options = ref<any>({ payments: [], bills: [], corrections: [] }),
  events = ref<any[]>([]),
  proof = ref<any>(),
  selected = ref<any>()
const loading = ref(false),
  saving = ref(false),
  createVisible = ref(false),
  receiptVisible = ref(false),
  historyVisible = ref(false)
const states = {
  PENDING: '待独立复核',
  APPROVED: '待结清',
  SETTLED: '已结清',
  CANCELLED: '已撤销',
  REJECTED: '已驳回'
}
const draft = reactive<any>({}),
  receipt = reactive<any>({})
const corrections = computed(() =>
  options.value.corrections.filter((c: any) => c.paymentId === draft.paymentId)
)
const ready = computed(
  () =>
    draft.paymentId &&
    draft.billId &&
    draft.employeeAcknowledgement?.trim() &&
    draft.evidence?.trim()
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
  options.value = await request.get({
    url: base + '/options',
    params: { entityId: entityId.value }
  })
  Object.assign(draft, {
    paymentId: undefined,
    billId: undefined,
    correctionId: undefined,
    employeeAcknowledgement: '',
    evidence: '',
    requestKey: crypto.randomUUID()
  })
  proof.value = undefined
  createVisible.value = true
}
function resetProof() {
  draft.correctionId = undefined
  proof.value = undefined
}
async function preview() {
  proof.value = undefined
  proof.value = await request.post({ url: base + '/preview', data: { ...draft } })
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
  return (
    await message.prompt(title, '核实依据', {
      inputType: 'textarea',
      inputValidator: (v: string) => (!!v?.trim() && v.length <= 1000) || '请填写1～1000字的依据'
    })
  ).value
}
async function review(row: any, approved: boolean) {
  await request.post({
    url: base + '/review',
    data: { id: row.id, approved, reason: await reason(approved ? '独立复核批准' : '驳回依据') }
  })
  await load()
}
async function cancel(row: any) {
  const basis = await reason('撤销依据'),
    evidence = row.status === 'APPROVED' ? await reason('已批准收退或记账指令作废依据') : undefined
  await request.post({ url: base + '/cancel', data: { id: row.id, reason: basis, evidence } })
  await load()
}
function openReceipt(row: any) {
  selected.value = row
  Object.assign(receipt, {
    id: row.id,
    amount: Math.abs(Number(row.personalDelta)),
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
    message.success('已核实结清')
    await load()
  } finally {
    saving.value = false
  }
}
async function history(row: any) {
  selected.value = row
  events.value = await request.get({ url: base + '/history', params: { id: row.id } })
  historyVisible.value = true
}
onMounted(async () => {
  entities.value = await request.get({ url: base + '/entities' })
  if (entities.value.length === 1) {
    entityId.value = entities.value[0].id
    await load()
  }
})
</script>
