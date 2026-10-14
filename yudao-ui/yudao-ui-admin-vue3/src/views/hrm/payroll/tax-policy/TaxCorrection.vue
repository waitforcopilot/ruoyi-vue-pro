<template>
  <Dialog v-model="visible" title="已发工资税务来源更正" width="1000px">
    <el-alert
      title="请依据正式更正资料核实减除费用及税务档案。已发工资、银行到账日期和实际预缴金额保留原始事实。"
      type="info"
      :closable="false"
    />
    <el-form class="mt-16px" label-width="120px" :disabled="busy">
      <el-form-item label="支付明细">{{ draft.paymentId }}</el-form-item>
      <el-form-item label="更正减除费用"
        ><el-input-number v-model="draft.basicDeduction" :min="0" :precision="2"
      /></el-form-item>
      <el-form-item label="正式资料编号"
        ><el-input v-model="draft.officialReference" maxlength="100"
      /></el-form-item>
      <el-form-item label="更正依据"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000"
      /></el-form-item>
      <el-form-item label="更正原因"
        ><el-input v-model="draft.reason" type="textarea" maxlength="1000"
      /></el-form-item>
      <el-form-item>
        <el-button v-hasPermi="['hrm:payroll:tax:update']" :loading="busy" @click="preview"
          >核对更正结果</el-button
        >
        <el-button
          v-hasPermi="['hrm:payroll:tax:update']"
          type="primary"
          :disabled="!prepared"
          :loading="busy"
          @click="submit"
          >提交独立复核</el-button
        >
      </el-form-item>
    </el-form>
    <el-descriptions v-if="prepared" :column="3" border>
      <el-descriptions-item label="实际到账日">{{ prepared.valueDate }}</el-descriptions-item>
      <el-descriptions-item label="扣缴单位">{{
        prepared.taxRegistrationNumber
      }}</el-descriptions-item>
      <el-descriptions-item label="税务政策版本">{{ prepared.policyVersion }}</el-descriptions-item>
      <el-descriptions-item label="工资收入">{{ prepared.income }}</el-descriptions-item>
      <el-descriptions-item label="专项扣除">{{ prepared.socialDeduction }}</el-descriptions-item>
      <el-descriptions-item label="更正减除费用">{{
        prepared.basicDeduction
      }}</el-descriptions-item>
      <el-descriptions-item label="工资实际预缴">{{
        prepared.actualSalaryWithheld
      }}</el-descriptions-item>
      <el-descriptions-item label="单独计税奖金">{{
        prepared.separateBonusIncome
      }}</el-descriptions-item>
      <el-descriptions-item label="奖金实际预缴">{{
        prepared.actualBonusWithheld
      }}</el-descriptions-item>
    </el-descriptions>
    <el-table :data="rows" v-loading="busy" class="mt-16px">
      <el-table-column label="版本" prop="version" width="70" />
      <el-table-column label="状态" width="110"
        ><template #default="{ row }">{{ statuses[row.status] }}</template></el-table-column
      >
      <el-table-column label="正式资料" prop="officialReference" min-width="150" />
      <el-table-column label="减除费用" prop="basicDeduction" width="120" />
      <el-table-column label="原因" prop="reason" min-width="150" />
      <el-table-column label="复核意见" prop="reviewOpinion" min-width="150" />
      <el-table-column label="操作" width="130"
        ><template #default="{ row }">
          <template v-if="row.status === 'PENDING'"
            ><el-button
              v-hasPermi="['hrm:payroll:tax:approve']"
              :disabled="busy"
              link
              type="primary"
              @click="review(row.id, true)"
              >批准</el-button
            ><el-button
              v-hasPermi="['hrm:payroll:tax:approve']"
              :disabled="busy"
              link
              type="danger"
              @click="review(row.id, false)"
              >驳回</el-button
            ></template
          >
        </template></el-table-column
      >
    </el-table>
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/tax-correction'
const message = useMessage()
const emit = defineEmits(['changed'])
const visible = ref(false),
  busy = ref(false),
  rows = ref<any[]>([]),
  prepared = ref<any>()
const draft = ref<api.TaxCorrectionRequest>({
  paymentId: 0,
  basicDeduction: 0,
  officialReference: '',
  evidence: '',
  reason: '',
  requestKey: ''
})
const statuses: Record<string, string> = {
  PENDING: '待复核',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  SUPERSEDED: '历史批准更正'
}
let sequence = 0
async function load() {
  const id = draft.value.paymentId,
    seq = ++sequence
  const result = await api.list(id)
  if (seq === sequence && draft.value.paymentId === id) rows.value = result
}
async function open(paymentId: number, basicDeduction: number) {
  sequence++
  draft.value = {
    paymentId,
    basicDeduction,
    officialReference: '',
    evidence: '',
    reason: '',
    requestKey: crypto.randomUUID()
  }
  rows.value = []
  prepared.value = undefined
  visible.value = true
  await load()
}
defineExpose({ open })
watch(
  draft,
  () => {
    prepared.value = undefined
  },
  { deep: true, flush: 'sync' }
)
async function preview() {
  if (
    !draft.value.officialReference.trim() ||
    !draft.value.evidence.trim() ||
    !draft.value.reason.trim()
  ) {
    message.warning('请填写正式更正资料和原因')
    return
  }
  busy.value = true
  const body = { ...draft.value }
  try {
    const result = await api.preview(body)
    if (JSON.stringify(body) === JSON.stringify(draft.value)) prepared.value = result
  } finally {
    busy.value = false
  }
}
async function submit() {
  if (!prepared.value) return
  busy.value = true
  try {
    await api.submit(draft.value)
    message.success('已提交独立复核')
    await load()
    prepared.value = undefined
    emit('changed')
  } finally {
    busy.value = false
  }
}
async function review(id: number, approve: boolean) {
  const prompt = await message.prompt('请输入复核意见')
  const reason = prompt.value?.trim()
  if (!reason) return
  busy.value = true
  try {
    await api.review(id, approve, reason)
    await load()
    emit('changed')
  } finally {
    busy.value = false
  }
}
</script>
