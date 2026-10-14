<template>
  <Dialog v-model="visible" title="员工税款补扣与退税结算" width="1100px">
    <el-alert
      title="依据已完成完税对账的批准申报核实员工差额。申请和复核不代表已付款；核实实际银行收付款后才结清。"
      type="info"
      :closable="false"
    />
    <el-button class="my-12px" :loading="loading" @click="load">刷新余额与记录</el-button>
    <el-table :data="balances" v-loading="loading">
      <el-table-column prop="taxpayerName" label="纳税人" min-width="120" />
      <el-table-column prop="maskedIdentifier" label="脱敏证件" min-width="120" />
      <el-table-column label="收入类型"
        ><template #default="{ row }">{{ modes[row.mode] }}</template></el-table-column
      >
      <el-table-column prop="originalWithheld" label="原实扣" />
      <el-table-column prop="expectedWithholding" label="应扣" />
      <el-table-column prop="settledNet" label="已结算净补扣" />
      <el-table-column prop="remaining" label="余额（负数退税）" width="160" />
      <el-table-column label="操作" width="100"
        ><template #default="{ row }">
          <el-button
            v-if="Number(row.remaining) !== 0"
            v-hasPermi="['hrm:payroll:tax:update']"
            link
            type="primary"
            :disabled="loading"
            @click="create(row)"
            >申请结算</el-button
          >
        </template></el-table-column
      >
    </el-table>
    <el-table class="mt-16px" :data="records" v-loading="loading">
      <el-table-column prop="id" label="结算编号" width="90" />
      <el-table-column prop="employeeId" label="员工编号" width="90" />
      <el-table-column label="方式" width="90"
        ><template #default="{ row }">{{
          row.kind === 'REFUND' ? '退还员工' : '员工补扣'
        }}</template></el-table-column
      >
      <el-table-column prop="amount" label="金额" width="100" />
      <el-table-column label="状态" width="110"
        ><template #default="{ row }">{{ statuses[row.status] }}</template></el-table-column
      >
      <el-table-column prop="reviewReason" label="复核意见" min-width="120" />
      <el-table-column prop="valueDate" label="实际收付款日" width="140" />
      <el-table-column prop="bankReference" label="银行流水" min-width="140" />
      <el-table-column label="操作" width="230"
        ><template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button
              v-hasPermi="['hrm:payroll:tax:approve']"
              link
              type="primary"
              :disabled="loading"
              @click="review(row.id, true)"
              >批准</el-button
            >
            <el-button
              v-hasPermi="['hrm:payroll:tax:approve']"
              link
              type="danger"
              :disabled="loading"
              @click="review(row.id, false)"
              >驳回</el-button
            >
          </template>
          <el-button
            v-if="row.status === 'APPROVED'"
            v-hasPermi="['hrm:payroll:payment:reconcile']"
            link
            type="primary"
            :disabled="loading"
            @click="openBank(row)"
            >确认银行凭据</el-button
          >
          <el-button
            v-if="['PENDING', 'APPROVED'].includes(row.status)"
            v-hasPermi="['hrm:payroll:tax:approve']"
            link
            type="danger"
            :disabled="loading"
            @click="cancel(row)"
            >作废</el-button
          >
        </template></el-table-column
      >
    </el-table>
  </Dialog>
  <Dialog v-model="draftVisible" title="税款结算申请" width="650px">
    <el-form label-width="140px" :disabled="saving">
      <el-form-item label="员工编号"
        ><el-select v-model="draft.employeeId"
          ><el-option
            v-for="id in candidateEmployees"
            :key="id"
            :value="id"
            :label="String(id)" /></el-select
      ></el-form-item>
      <el-form-item label="方向">{{
        draft.kind === 'REFUND' ? '退还员工' : '员工补扣'
      }}</el-form-item>
      <el-form-item label="结算金额"
        ><el-input-number v-model="draft.amount" :min="0.01" :precision="2"
      /></el-form-item>
      <el-form-item label="员工收付款确认"
        ><el-input
          v-model="draft.employeeAcknowledgement"
          type="textarea"
          maxlength="1000"
          placeholder="已核实的员工确认及收/付款依据"
      /></el-form-item>
      <el-form-item label="结算依据"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000"
      /></el-form-item>
      <el-form-item label="原因"
        ><el-input v-model="draft.reason" type="textarea" maxlength="1000"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button type="primary" :loading="saving" @click="submit"
        >提交独立复核</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="bankVisible" title="核实实际银行收付款" width="650px">
    <el-form label-width="140px" :disabled="saving">
      <el-form-item label="结算编号">{{ bank.id }}</el-form-item>
      <el-form-item label="实际金额"
        ><el-input-number v-model="bank.amount" :min="0.01" :precision="2"
      /></el-form-item>
      <el-form-item label="实际收付款日"
        ><el-date-picker v-model="bank.valueDate" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="银行及单笔流水"
        ><el-input
          v-model="bank.bankReference"
          maxlength="100"
          placeholder="银行标识及该笔交易的唯一流水号"
      /></el-form-item>
      <el-form-item label="银行核实依据"
        ><el-input v-model="bank.evidence" type="textarea" maxlength="1000"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button type="primary" :loading="saving" @click="confirm"
        >确认实际收付款</el-button
      ></template
    >
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/tax-settlement'
const message = useMessage()
const visible = ref(false),
  loading = ref(false),
  saving = ref(false),
  draftVisible = ref(false),
  bankVisible = ref(false)
const filingId = ref<number>(),
  balances = ref<any[]>([]),
  records = ref<any[]>([]),
  candidateEmployees = ref<number[]>([])
const draft = ref<any>({}),
  bank = ref<any>({})
const modes: Record<string, string> = { RESIDENT_WAGE: '工资', SEPARATE_BONUS: '单独计税奖金' }
const statuses: Record<string, string> = {
  PENDING: '待复核',
  APPROVED: '待银行确认',
  SETTLED: '实际已结清',
  REJECTED: '已驳回',
  CANCELLED: '已作废'
}
let sequence = 0
async function open(id: number) {
  filingId.value = id
  balances.value = []
  records.value = []
  draftVisible.value = false
  bankVisible.value = false
  visible.value = true
  await load()
}
defineExpose({ open })
async function load() {
  const id = filingId.value,
    seq = ++sequence
  if (!id) return
  loading.value = true
  try {
    const [b, r] = await Promise.all([api.balances(id), api.list(id)])
    if (seq === sequence && id === filingId.value) {
      balances.value = b
      records.value = r
    }
  } finally {
    if (seq === sequence) loading.value = false
  }
}
function create(row: any) {
  const refund = Number(row.remaining) < 0
  const reserved = Number(refund ? row.refundReserved : row.recoveryReserved)
  const available = Math.abs(Number(row.remaining)) - reserved
  if (available <= 0) {
    message.warning('余额已由待处理结算预占')
    return
  }
  candidateEmployees.value = row.employeeIds || []
  draft.value = {
    filingId: filingId.value,
    employeeId: candidateEmployees.value[0],
    mode: row.mode,
    kind: refund ? 'REFUND' : 'RECOVERY',
    amount: available,
    employeeAcknowledgement: '',
    evidence: '',
    reason: '',
    requestKey: crypto.randomUUID()
  }
  draftVisible.value = true
}
async function submit() {
  if (
    !draft.value.employeeAcknowledgement?.trim() ||
    !draft.value.evidence?.trim() ||
    !draft.value.reason?.trim()
  ) {
    message.warning('请填写员工确认、结算依据和原因')
    return
  }
  saving.value = true
  try {
    await api.submit(draft.value)
    draftVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function review(id: number, approve: boolean) {
  const { value } = await message.prompt('请输入复核意见')
  if (!value?.trim()) return
  loading.value = true
  try {
    await api.review(id, approve, value.trim())
    await load()
  } finally {
    loading.value = false
  }
}
function openBank(row: any) {
  bank.value = {
    id: row.id,
    amount: Number(row.amount),
    valueDate: new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Shanghai' }).format(new Date()),
    bankReference: '',
    evidence: ''
  }
  bankVisible.value = true
}
async function confirm() {
  if (!bank.value.valueDate || !bank.value.bankReference?.trim() || !bank.value.evidence?.trim()) {
    message.warning('请填写银行流水和实际收付款依据')
    return
  }
  saving.value = true
  try {
    await api.confirm(bank.value)
    bankVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function cancel(row: any) {
  const { value: reason } = await message.prompt('请输入作废原因')
  if (!reason?.trim()) return
  let proof: string | undefined
  if (row.status === 'APPROVED') {
    const { value } = await message.prompt('请输入核实银行未执行或已作废的依据')
    if (!value?.trim()) return
    proof = value.trim()
  }
  loading.value = true
  try {
    await api.cancel(row.id, reason.trim(), proof)
    await load()
  } finally {
    loading.value = false
  }
}
</script>
