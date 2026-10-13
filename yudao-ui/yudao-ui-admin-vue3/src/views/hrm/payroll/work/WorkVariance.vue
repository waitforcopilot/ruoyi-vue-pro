<template>
  <el-alert
    title="核对计划、实际及认可工时。缺卡不自动当作零；工单处理依据由独立人员复核。"
    type="info"
    :closable="false"
  />
  <el-button
    class="mt-10px mb-10px"
    v-hasPermi="['hrm:payroll:work:confirm']"
    :disabled="!entityId || !employeeId"
    @click="open"
    >登记工时排班差异</el-button
  >
  <el-table :data="rows" v-loading="loading">
    <el-table-column prop="workDate" label="日期" width="115" />
    <el-table-column prop="reason" label="差异说明" min-width="180" />
    <el-table-column label="责任人" width="120"
      ><template #default="{ row }">{{ userName(row.ownerId) }}</template></el-table-column
    >
    <el-table-column label="独立复核人" width="120"
      ><template #default="{ row }">{{ userName(row.reviewerId) }}</template></el-table-column
    >
    <el-table-column label="期限" width="165"
      ><template #default="{ row }">{{
        dayjs(row.dueTime).format('YYYY-MM-DD HH:mm')
      }}</template></el-table-column
    >
    <el-table-column label="状态" width="130"
      ><template #default="{ row }"
        ><el-tag :type="row.overdue ? 'danger' : 'info'"
          >{{ statusNames[row.status] || row.status }}{{ row.overdue ? ' · 逾期' : '' }}</el-tag
        ></template
      ></el-table-column
    >
    <el-table-column label="来源" width="120"
      ><template #default="{ row }"
        ><el-tag :type="row.sourceFresh ? 'success' : 'warning'">{{
          !row.sourceReady ? '来源未就绪' : row.sourceFresh ? '来源一致' : '需重新核实'
        }}</el-tag></template
      ></el-table-column
    >
    <el-table-column label="逾期提醒" width="140"
      ><template #default="{ row }">
        <el-tooltip v-if="row.reminderError" :content="row.reminderError"
          ><el-tag type="warning">未全部送达</el-tag></el-tooltip
        >
        <span v-else>{{
          row.reminderState && row.reminderState !== '{}'
            ? '已记录站内提醒'
            : row.overdue
              ? '待提醒'
              : '—'
        }}</span>
      </template></el-table-column
    >
    <el-table-column label="操作" width="280"
      ><template #default="{ row }">
        <el-button link @click="showProof(row.submittedSnapshot || row.sourceSnapshot)"
          >依据</el-button
        >
        <el-button link @click="history(row.id)">履历</el-button>
        <el-button
          v-if="row.status !== 'CLOSED'"
          v-hasPermi="['hrm:payroll:work:confirm']"
          link
          @click="assign(row)"
          >调整责任人</el-button
        >
        <el-button
          v-if="row.status === 'OPEN' && row.ownerId === currentUserId"
          v-hasPermi="['hrm:payroll:work:confirm']"
          link
          @click="submit(row.id)"
          >提交核实</el-button
        >
        <el-button
          v-if="row.status === 'SUBMITTED' && row.reviewerId === currentUserId"
          v-hasPermi="['hrm:payroll:work:approve']"
          :disabled="!row.sourceFresh || !row.sourceReady"
          link
          @click="review(row.id, true)"
          >复核关闭</el-button
        >
        <el-button
          v-if="row.status === 'SUBMITTED' && row.reviewerId === currentUserId"
          v-hasPermi="['hrm:payroll:work:approve']"
          link
          @click="review(row.id, false)"
          >退回</el-button
        >
        <el-button
          v-if="row.status === 'CLOSED' && row.reviewerId === currentUserId"
          v-hasPermi="['hrm:payroll:work:approve']"
          link
          @click="reopen(row.id)"
          >重开</el-button
        >
      </template></el-table-column
    >
  </el-table>
  <Dialog
    v-model="visible"
    :title="assigning ? '调整核实职责与期限' : '登记工时排班差异'"
    width="700px"
  >
    <el-form label-width="120px">
      <el-form-item label="核对日期"
        ><el-date-picker
          v-model="draft.workDate"
          :disabled="assigning"
          value-format="YYYY-MM-DD"
          :disabled-date="(date: Date) => !dayjs(date).isBefore(dayjs().startOf('day'))"
      /></el-form-item>
      <el-form-item v-if="!assigning"
        ><el-button :loading="previewing" @click="capture">核对当日来源</el-button></el-form-item
      >
      <template v-if="preview">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="排班分钟">{{
            preview.plannedMinutes ?? '未知'
          }}</el-descriptions-item>
          <el-descriptions-item label="实际分钟">{{
            preview.actualMinutes ?? '未知'
          }}</el-descriptions-item>
          <el-descriptions-item label="认可分钟">{{
            preview.recognizedMinutes ?? '未知'
          }}</el-descriptions-item>
          <el-descriptions-item label="实际减计划">{{
            preview.differenceMinutes ?? '无法计算'
          }}</el-descriptions-item>
        </el-descriptions>
        <el-alert
          v-for="issue in preview.issues"
          :key="issue"
          :title="issue"
          type="warning"
          :closable="false"
          class="mt-5px"
        />
        <el-button link @click="showProof(JSON.stringify(preview))">查看原来源</el-button>
      </template>
      <el-form-item label="差异说明"
        ><el-input v-model="draft.reason" type="textarea" maxlength="1000"
      /></el-form-item>
      <el-form-item label="责任人"
        ><el-select v-model="draft.ownerId" filterable
          ><el-option
            v-for="user in users"
            :key="user.id"
            :label="user.nickname"
            :value="user.id" /></el-select
      ></el-form-item>
      <el-form-item label="独立复核人"
        ><el-select v-model="draft.reviewerId" filterable
          ><el-option
            v-for="user in users"
            :key="user.id"
            :label="user.nickname"
            :value="user.id" /></el-select
      ></el-form-item>
      <el-form-item label="处理期限"
        ><el-date-picker v-model="draft.dueTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button
        :loading="saving"
        :disabled="!assigning && !preview"
        type="primary"
        @click="create"
        >{{ assigning ? '保存职责调整' : '登记工单' }}</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="proofVisible" title="工时来源与处理履历" width="850px">
    <pre style="white-space: pre-wrap; word-break: break-all">{{ proof }}</pre>
  </Dialog>
</template>
<script setup lang="ts">
import dayjs from 'dayjs'
import * as api from '@/api/hrm/payroll/work'
import * as userApi from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'
const currentUserId = computed(() => useUserStore().getUser.id)
const props = defineProps<{ entityId?: number; employeeId?: number }>()
const message = useMessage()
const rows = ref<any[]>([]),
  users = ref<userApi.UserVO[]>([])
const loading = ref(false),
  visible = ref(false),
  saving = ref(false),
  previewing = ref(false)
const assigning = ref(false)
const preview = ref<any>(),
  draft = ref<any>({}),
  proofVisible = ref(false),
  proof = ref('')
const statusNames: Record<string, string> = {
  OPEN: '待核实',
  SUBMITTED: '待独立复核',
  CLOSED: '已关闭'
}
const userName = (id: number) =>
  users.value.find((user) => user.id === id)?.nickname || `用户 ${id}`
let sequence = 0,
  previewSequence = 0
async function load() {
  const version = ++sequence
  rows.value = []
  if (!props.entityId || !props.employeeId) return
  loading.value = true
  try {
    const result = await api.varianceList({
      entityId: props.entityId,
      employeeId: props.employeeId
    })
    if (version === sequence) rows.value = result
  } finally {
    if (version === sequence) loading.value = false
  }
}
function open() {
  assigning.value = false
  previewSequence++
  preview.value = undefined
  draft.value = {
    entityId: props.entityId,
    employeeId: props.employeeId,
    workDate: dayjs().subtract(1, 'day').format('YYYY-MM-DD'),
    dueTime: dayjs().add(2, 'day').hour(17).minute(0).second(0).format('YYYY-MM-DDTHH:mm:ss'),
    requestKey: crypto.randomUUID()
  }
  visible.value = true
}
function assign(row: any) {
  previewSequence++
  preview.value = undefined
  assigning.value = true
  draft.value = {
    id: row.id,
    workDate: row.workDate,
    ownerId: row.ownerId,
    reviewerId: row.reviewerId,
    dueTime: dayjs().add(2, 'day').hour(17).minute(0).second(0).format('YYYY-MM-DDTHH:mm:ss'),
    reason: ''
  }
  visible.value = true
}
async function capture() {
  const version = ++previewSequence
  preview.value = undefined
  if (!draft.value.workDate) return
  previewing.value = true
  try {
    const result = await api.variancePreview({
      entityId: props.entityId,
      employeeId: props.employeeId,
      date: draft.value.workDate
    })
    if (version === previewSequence) preview.value = result
  } finally {
    if (version === previewSequence) previewing.value = false
  }
}
async function create() {
  saving.value = true
  try {
    if (assigning.value) await api.reassignVariance(draft.value)
    else await api.createVariance(draft.value)
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function submit(id: number) {
  const { value } = await message.prompt('请输入事实核实及处理依据', '提交工时差异核实')
  await api.submitVariance({ id, evidence: value })
  await load()
}
async function review(id: number, approved: boolean) {
  const { value } = await message.prompt('请输入独立复核意见', approved ? '复核关闭' : '退回核实')
  await api.reviewVariance({ id, approved, reason: value })
  await load()
}
async function reopen(id: number) {
  const { value } = await message.prompt('请输入重开依据', '重开工时差异')
  await api.reopenVariance({ id, reason: value })
  await load()
}
function showProof(snapshot: string) {
  proof.value = JSON.stringify(JSON.parse(snapshot), null, 2)
  proofVisible.value = true
}
async function history(id: number) {
  proof.value = JSON.stringify(await api.varianceHistory(id), null, 2)
  proofVisible.value = true
}
watch(
  () => [props.entityId, props.employeeId],
  () => {
    previewSequence++
    visible.value = false
    preview.value = undefined
    void load()
  },
  { immediate: true }
)
watch(
  () => draft.value.workDate,
  () => {
    previewSequence++
    preview.value = undefined
    previewing.value = false
  }
)
onMounted(async () => {
  users.value = await userApi.getSimpleUserList()
})
</script>
