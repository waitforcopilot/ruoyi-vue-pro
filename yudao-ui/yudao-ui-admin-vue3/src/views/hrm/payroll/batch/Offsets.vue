<template>
  <el-alert
    title="先分配并独立复核税款，再重新同步/复核累计个税输入、核算和审批发放。本批次将使用批准待结算基数避免重复扣税；成功回盘后才实际核销，失败保留待结算。"
    type="info"
    :closable="false"
  />
  <el-space class="my-4"
    ><el-button :loading="loading" @click="load">刷新</el-button
    ><el-button v-if="editable" v-hasPermi="['hrm:payroll:offset:update']" @click="open"
      >分配批准税款</el-button
    ></el-space
  >
  <el-table :data="rows" v-loading="loading"
    ><el-table-column prop="id" label="分配编号" width="100" /><el-table-column
      prop="employeeId"
      label="本批次员工"
    /><el-table-column prop="sourceId" label="批准税款结算" /><el-table-column label="工资净额调整"
      ><template #default="{ row }"
        >{{ Number(row.netAmount) > 0 ? '+' : '' }}{{ row.netAmount }}</template
      ></el-table-column
    ><el-table-column label="状态"
      ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
    ><el-table-column
      prop="sourceReason"
      label="来源结论"
      min-width="180"
      show-overflow-tooltip
    /><el-table-column prop="consumedPaymentId" label="实际结清付款" /><el-table-column
      prop="evidence"
      label="分配依据"
      show-overflow-tooltip
    /><el-table-column prop="reviewReason" label="复核意见" show-overflow-tooltip /><el-table-column
      label="操作"
      width="220"
      ><template #default="{ row }"
        ><el-button link @click="history(row.id)">履历</el-button
        ><template v-if="editable"
          ><template v-if="row.status === 'PENDING'"
            ><el-button
              v-hasPermi="['hrm:payroll:offset:review']"
              link
              @click="review(row.id, true)"
              >批准</el-button
            ><el-button
              v-hasPermi="['hrm:payroll:offset:review']"
              link
              @click="review(row.id, false)"
              >驳回</el-button
            ></template
          ><el-button
            v-if="['PENDING', 'APPROVED'].includes(row.status)"
            v-hasPermi="['hrm:payroll:offset:update']"
            link
            @click="cancel(row.id)"
            >撤销分配</el-button
          ></template
        ></template
      ></el-table-column
    ></el-table
  >
  <Dialog v-model="visible" title="指定工资批次退补税" width="650px"
    ><el-form label-width="135px"
      ><el-form-item label="已批准税款结算"
        ><el-select v-model="draft.sourceId" filterable style="width: 100%"
          ><el-option
            v-for="s in options.taxSettlements"
            :key="s.id"
            :value="s.id"
            :label="`结算 ${s.id} · 原员工 ${s.employeeId} · ${s.year}-${s.month} · ${s.kind === 'REFUND' ? '退还' : '补收'} ${s.amount}`" /></el-select></el-form-item
      ><el-form-item label="本批次收付员工"
        ><el-select v-model="draft.employeeId" filterable style="width: 100%"
          ><el-option
            v-for="e in options.employees"
            :key="e.id"
            :value="e.id"
            :label="`${e.name} · ${e.jobNumber || e.id}`" /></el-select></el-form-item
      ><el-alert
        title="本批次发放月份须晚于原申报月份。重新入职可选择同主体、同纳税身份的新员工记录。新增或撤销分配会将旧试算改为待重新核算。"
        :closable="false"
        type="info" /><el-form-item label="分配依据" class="mt-4"
        ><el-input
          v-model="draft.evidence"
          type="textarea"
          maxlength="1000" /></el-form-item></el-form
    ><template #footer
      ><el-button
        type="primary"
        :loading="saving"
        :disabled="!draft.sourceId || !draft.employeeId || !draft.evidence.trim()"
        @click="save"
        >提交独立复核</el-button
      ></template
    ></Dialog
  >
  <Dialog v-model="historyVisible" title="工资退补税履历"
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
import request from '@/config/axios'
const props = defineProps<{ batchId: number; status: number }>(),
  emit = defineEmits(['changed']),
  message = useMessage(),
  base = '/hrm/payroll/offset',
  rows = ref<any[]>([]),
  events = ref<any[]>([]),
  options = reactive<any>({ employees: [], taxSettlements: [] }),
  draft = reactive<any>({ evidence: '' }),
  loading = ref(false),
  saving = ref(false),
  visible = ref(false),
  historyVisible = ref(false)
const states = {
  PENDING: '待独立复核',
  APPROVED: '批准待工资结算',
  REJECTED: '已驳回',
  CANCELLED: '已撤销分配',
  CONSUMED: '已随工资实际结清'
}
const editable = computed(() => [5, 11].includes(props.status))
async function load() {
  loading.value = true
  try {
    rows.value = await request.get({ url: base + '/list', params: { batchId: props.batchId } })
  } finally {
    loading.value = false
  }
}
async function open() {
  Object.assign(
    options,
    await request.get({ url: base + '/options', params: { batchId: props.batchId } })
  )
  Object.assign(draft, {
    batchId: props.batchId,
    sourceType: 'TAX',
    sourceId: undefined,
    employeeId: undefined,
    evidence: '',
    requestKey: crypto.randomUUID()
  })
  visible.value = true
}
async function save() {
  saving.value = true
  try {
    await request.post({ url: base + '/create', data: { ...draft } })
    visible.value = false
    message.success('已提交，独立复核后重新同步累计个税输入并核算')
    await load()
    emit('changed')
  } finally {
    saving.value = false
  }
}
async function note(title: string) {
  const { value } = await message.prompt(title, '依据', {
    inputType: 'textarea',
    inputValidator: (v: string) => (!!v?.trim() && v.length <= 1000) || '请填写1～1000字的依据'
  })
  return value
}
async function review(id: number, approved: boolean) {
  const reason = await note(approved ? '请填写独立核实税款与本批次收付人的依据' : '请填写驳回原因')
  await request.post({ url: base + '/review', data: { id, approved, reason } })
  await load()
  emit('changed')
}
async function cancel(id: number) {
  const reason = await note('请说明撤销本次工资分配的原因，原批准税款结算保留')
  await request.post({ url: base + '/cancel', data: { id, reason } })
  await load()
  emit('changed')
}
async function history(id: number) {
  events.value = await request.get({ url: base + '/history', params: { id } })
  historyVisible.value = true
}
watch(() => props.batchId, load, { immediate: true })
</script>
