<template>
  <el-alert
    title="核定原普通工资计税收入后，差额进入本批工资；核定基线已剔除历史差额。批准不代表已发放；跨年收回需先办理原年度更正。"
    type="info"
    :closable="false"
  />
  <el-button class="mt-4" @click="load">刷新</el-button>
  <el-button v-if="[5, 11].includes(status)" v-hasPermi="['hrm:payroll:retro:update']" @click="open"
    >登记补发/收回</el-button
  >
  <el-table v-loading="loading" :data="rows">
    <el-table-column prop="employeeId" label="员工编号" />
    <el-table-column prop="paymentId" label="原付款编号" />
    <el-table-column prop="originalIncome" label="原核定收入" />
    <el-table-column prop="correctedIncome" label="更正后收入" />
    <el-table-column prop="delta" label="本次差额" />
    <el-table-column prop="version" label="版本" />
    <el-table-column label="状态"
      ><template #default="{ row }">{{
        states[row.status] || row.status
      }}</template></el-table-column
    >
    <el-table-column label="操作" width="230"
      ><template #default="{ row }">
        <el-button link @click="history(row)">依据与履历</el-button>
        <el-button
          v-if="row.status === 'APPROVED' && [5, 11].includes(status)"
          v-hasPermi="['hrm:payroll:retro:approve']"
          link
          @click="cancel(row.id)"
          >撤销</el-button
        >
        <template v-if="row.status === 'PENDING' && [5, 11].includes(status)">
          <el-button v-hasPermi="['hrm:payroll:retro:approve']" link @click="review(row.id, true)"
            >批准</el-button
          >
          <el-button v-hasPermi="['hrm:payroll:retro:approve']" link @click="review(row.id, false)"
            >驳回</el-button
          >
        </template>
      </template></el-table-column
    >
  </el-table>
  <Dialog v-model="visible" title="登记工资差额" width="700px">
    <el-form label-width="120px">
      <el-form-item label="原到账工资"
        ><el-select v-model="draft.paymentId" filterable style="width: 100%">
          <el-option
            v-for="p in candidates"
            :key="p.paymentId"
            :value="p.paymentId"
            :label="`${p.employeeName} · ${p.originalBatch} · ${p.valueDate} · 原收入 ${p.income}`"
          /> </el-select
      ></el-form-item>
      <el-form-item label="更正后收入"
        ><el-input-number v-model="draft.correctedIncome" :min="0" :precision="2"
      /></el-form-item>
      <el-form-item label="核定与员工确认"
        ><el-input
          v-model="draft.evidence"
          type="textarea"
          maxlength="1000"
          placeholder="填写核定明细及员工确认的依据编号；不含年终奖与缴费差额"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button
        :loading="saving"
        :disabled="!draft.paymentId || !draft.evidence.trim()"
        type="primary"
        @click="save"
        >提交独立复核</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="proofVisible" title="核定依据与履历" width="850px">
    <p>{{ evidence }}</p
    ><el-table :data="events"
      ><el-table-column prop="createTime" label="时间" /><el-table-column
        prop="action"
        label="动作" /><el-table-column prop="actorId" label="操作人" /><el-table-column
        prop="reason"
        label="依据"
    /></el-table>
  </Dialog>
</template>
<script setup lang="ts">
import request from '@/config/axios'
const props = defineProps<{ batchId: number; status: number }>()
const message = useMessage()
const rows = ref<any[]>([]),
  candidates = ref<any[]>([]),
  events = ref<any[]>([])
const loading = ref(false),
  saving = ref(false),
  visible = ref(false),
  proofVisible = ref(false),
  evidence = ref('')
const draft = reactive({
  paymentId: undefined as number | undefined,
  correctedIncome: 0,
  evidence: '',
  requestKey: ''
})
const states = { PENDING: '待复核', APPROVED: '已批准', REJECTED: '已驳回', CANCELLED: '已撤销' }
const base = '/hrm/payroll/retro'
async function load() {
  loading.value = true
  try {
    rows.value = await request.get({ url: base + '/list', params: { batchId: props.batchId } })
  } finally {
    loading.value = false
  }
}
async function open() {
  candidates.value = await request.get({
    url: base + '/candidates',
    params: { batchId: props.batchId }
  })
  Object.assign(draft, {
    paymentId: undefined,
    correctedIncome: 0,
    evidence: '',
    requestKey: crypto.randomUUID()
  })
  visible.value = true
}
async function save() {
  saving.value = true
  try {
    const p = candidates.value.find((p) => p.paymentId === draft.paymentId)
    await request.post({
      url: base + '/create',
      data: { ...draft, batchId: props.batchId, priorRetroId: p?.priorRetroId }
    })
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function review(id: number, approved: boolean) {
  const { value } = await message.prompt('填写独立复核依据', '工资差额复核')
  if (!value?.trim()) return
  await request.post({ url: base + '/review', params: { id, approved, reason: value } })
  await load()
}
async function cancel(id: number) {
  const { value } = await message.prompt('填写正式撤销依据', '撤销工资差额')
  if (!value?.trim()) return
  await request.post({ url: base + '/cancel', params: { id, reason: value } })
  await load()
}
async function history(row: any) {
  events.value = await request.get({ url: base + '/history', params: { id: row.id } })
  evidence.value = row.evidence
  proofVisible.value = true
}
onMounted(load)
</script>
