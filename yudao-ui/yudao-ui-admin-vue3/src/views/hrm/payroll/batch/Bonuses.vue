<template>
  <el-alert
    title="奖金按批次独立复核；金额或计税方式更正须提交接续版本。单独计税资格还将在冻结时检查年度使用记录。"
    type="info"
    :closable="false"
  />
  <el-button class="mt-4" @click="load">刷新</el-button>
  <el-button v-if="[5, 11].includes(status)" v-hasPermi="['hrm:payroll:tax:update']" @click="open()"
    >登记年终奖</el-button
  >
  <el-table :data="rows" v-loading="loading">
    <el-table-column prop="employeeId" label="员工编号" />
    <el-table-column prop="amount" label="奖金金额" />
    <el-table-column label="计税方式"
      ><template #default="{ row }">{{ modes[row.mode] }}</template></el-table-column
    >
    <el-table-column prop="expectedPaymentDate" label="发放日期" />
    <el-table-column prop="version" label="版本" />
    <el-table-column label="状态"
      ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
    >
    <el-table-column label="操作" width="270"
      ><template #default="{ row }">
        <el-button link @click="proof(row)">依据与履历</el-button>
        <el-button
          v-if="row.status === 'APPROVED' && [5, 11].includes(status)"
          v-hasPermi="['hrm:payroll:tax:update']"
          link
          @click="open(row)"
          >更正</el-button
        >
        <template v-if="row.status === 'PENDING' && [5, 11].includes(status)">
          <el-button v-hasPermi="['hrm:payroll:tax:approve']" link @click="review(row.id, true)"
            >批准</el-button
          >
          <el-button v-hasPermi="['hrm:payroll:tax:approve']" link @click="review(row.id, false)"
            >驳回</el-button
          >
        </template>
      </template></el-table-column
    >
  </el-table>
  <Dialog v-model="visible" title="提交年终奖来源" width="650px">
    <el-form label-width="110px">
      <el-form-item label="奖金员工"
        ><el-select v-model="draft.employeeId" filterable :disabled="!!draft.priorBonusId"
          ><el-option
            v-for="employee in employeeOptions"
            :key="employee.id"
            :value="employee.id"
            :label="`${employee.name} (${employee.jobNumber})`" /></el-select
      ></el-form-item>
      <el-form-item label="奖金金额"
        ><el-input-number v-model="draft.amount" :min="0" :precision="2"
      /></el-form-item>
      <el-form-item label="计税方式"
        ><el-select v-model="draft.mode"
          ><el-option
            v-for="(label, key) in modes"
            :key="key"
            :value="key"
            :label="label" /></el-select
      ></el-form-item>
      <el-form-item label="批准依据"
        ><el-input
          v-model="draft.evidence"
          type="textarea"
          maxlength="1000"
          placeholder="奖金授予依据、金额及计税方式确认资料"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button type="primary" :loading="saving" @click="save">提交独立复核</el-button></template
    >
  </Dialog>
  <Dialog v-model="proofVisible" title="奖金依据与履历" width="850px">
    <pre style="white-space: pre-wrap; word-break: break-all">{{ proofText }}</pre>
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/bonus'
const props = defineProps<{ batchId: number; status: number }>()
const employeeOptions = ref<any[]>([])
const message = useMessage(),
  rows = ref<any[]>([]),
  loading = ref(false),
  visible = ref(false),
  saving = ref(false),
  draft = ref<any>({}),
  proofVisible = ref(false),
  proofText = ref('')
const modes: Record<string, string> = {
  NONE: '取消／本批次无奖金',
  MERGED: '并入累计工资',
  SEPARATE: '全年一次单独计税'
}
const states: Record<string, string> = {
  PENDING: '待独立复核',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  SUPERSEDED: '历史批准版本'
}
let sequence = 0
async function load() {
  const version = ++sequence
  loading.value = true
  rows.value = []
  employeeOptions.value = []
  try {
    const [data, employees] = await Promise.all([
      api.list({ batchId: props.batchId }),
      api.employees(props.batchId)
    ])
    if (version === sequence) {
      rows.value = data
      employeeOptions.value = employees
    }
  } finally {
    if (version === sequence) loading.value = false
  }
}
function open(row?: any) {
  draft.value = {
    batchId: props.batchId,
    employeeId: row?.employeeId,
    priorBonusId: row?.id,
    amount: row?.amount ?? 0,
    mode: row?.mode,
    requestKey: crypto.randomUUID(),
    evidence: ''
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
  const batch = props.batchId
  const reason = await message.prompt(approved ? '填写独立核对依据' : '填写驳回原因')
  if (!reason || batch !== props.batchId) return
  await api.review({ id, approved, reason })
  await load()
}
async function proof(row: any) {
  const version = sequence
  const history = await api.history(row.id)
  if (version !== sequence) return
  proofText.value = JSON.stringify({ source: row, history }, null, 2)
  proofVisible.value = true
}
watch(
  () => props.batchId,
  () => {
    visible.value = false
    proofVisible.value = false
    load()
  },
  { immediate: true }
)
onBeforeUnmount(() => sequence++)
</script>
