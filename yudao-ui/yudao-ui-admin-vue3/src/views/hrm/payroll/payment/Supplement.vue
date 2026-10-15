<template>
  <Dialog v-model="visible" title="创建独立工资补发批次" width="680px">
    <el-alert
      title="只发放已核定工资收入差额，原银行付款保持不变。创建后先到核算审批复核差额来源，再试算、审批、冻结并代发。"
      type="info"
      :closable="false"
    />
    <el-form label-width="130px" class="mt-4">
      <el-form-item label="原工资期间">{{ baseline.originalBatch }}</el-form-item>
      <el-form-item label="此前核定收入">{{ baseline.income }}</el-form-item>
      <el-form-item label="更正后收入"
        ><el-input-number v-model="draft.correctedIncome" :min="0" :precision="2"
      /></el-form-item>
      <el-form-item label="计划到账日期"
        ><el-date-picker v-model="draft.expectedPaymentDate" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="核定与员工确认"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000"
      /></el-form-item>
    </el-form>
    <el-alert
      v-if="created"
      :title="`已创建补发批次 ${created}，可在核算审批中查询原工资期间的独立补发。`"
      type="success"
      :closable="false"
    />
    <template #footer
      ><el-button
        v-if="!created"
        type="primary"
        :loading="saving"
        :disabled="
          !draft.expectedPaymentDate ||
          !draft.evidence.trim() ||
          draft.correctedIncome <= Number(baseline.income)
        "
        @click="save"
        >创建并提交差额复核</el-button
      ></template
    >
  </Dialog>
</template>
<script setup lang="ts">
import request from '@/config/axios'
const visible = ref(false),
  saving = ref(false),
  created = ref<number>()
const baseline = ref<any>({})
const draft = reactive({
  paymentId: 0,
  correctedIncome: 0,
  expectedPaymentDate: '',
  evidence: '',
  requestKey: '',
  priorRetroId: undefined as number | undefined
})
async function open(paymentId: number) {
  baseline.value = await request.get({
    url: '/hrm/payroll/retro/supplement-baseline',
    params: { paymentId }
  })
  Object.assign(draft, {
    paymentId,
    correctedIncome: Number(baseline.value.income),
    expectedPaymentDate: '',
    evidence: '',
    requestKey: crypto.randomUUID(),
    priorRetroId: baseline.value.priorRetroId
  })
  created.value = undefined
  visible.value = true
}
async function save() {
  saving.value = true
  try {
    created.value = await request.post({ url: '/hrm/payroll/retro/supplement', data: { ...draft } })
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
