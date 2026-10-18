<template>
  <ContentWrap>
    <el-alert
      title="请先补正对应业务资料，再提交资料编号或说明。指定复核人核实真实来源后关闭工单。"
      type="info"
      :closable="false"
    />
    <el-button class="my-4" :loading="loading" @click="load">刷新</el-button>
    <el-alert
      v-if="loadError"
      title="工单加载失败，当前状态尚未确认。请点击刷新重试后再处理。"
      type="error"
      :closable="false"
      class="mb-4"
    />
    <el-table
      :data="rows"
      v-loading="loading"
      :empty-text="loading ? '正在加载工单' : loadError ? '工单未加载，请刷新重试' : '暂无工单'"
      ><el-table-column prop="id" label="工单编号" /><el-table-column
        prop="batchId"
        label="工资批次编号"
      /><el-table-column prop="employeeId" label="员工编号" /><el-table-column label="项目"
        ><template #default="{ row }">{{ labels[row.kind] }}</template></el-table-column
      ><el-table-column label="状态"
        ><template #default="{ row }"
          >{{ states[row.status] }}<el-tag v-if="row.overdue" type="danger">逾期</el-tag></template
        ></el-table-column
      ><el-table-column label="截止时间" min-width="165">
        <template #default="{ row }">{{ displayTime(row.dueTime) }}</template> </el-table-column
      ><el-table-column prop="reason" label="处理要求" show-overflow-tooltip /><el-table-column
        prop="reviewOpinion"
        label="复核意见"
        show-overflow-tooltip
      /><el-table-column label="操作" width="180"
        ><template #default="{ row }"
          ><el-button v-if="row.canSubmit" link :disabled="submitting" @click="submit(row.id)"
            >补充材料</el-button
          ><el-button link @click="history(row.id)">履历</el-button></template
        ></el-table-column
      ></el-table
    >
  </ContentWrap>
  <Dialog
    v-model="historyVisible"
    :title="`工单处理履历 #${historyIssueId || '—'}`"
    width="min(1000px, calc(100vw - 32px))"
    ><el-table :data="events" max-height="65vh"
      ><el-table-column label="动作" width="120"
        ><template #default="{ row }">{{
          actions[row.action] || row.action
        }}</template></el-table-column
      ><el-table-column prop="actorId" label="操作人编号" width="100" /><el-table-column
        prop="reason"
        label="依据"
        min-width="320"
      /><el-table-column label="时间" width="165">
        <template #default="{ row }">{{ displayTime(row.createTime) }}</template>
      </el-table-column></el-table
    ></Dialog
  >
</template>
<script setup lang="ts">
import request from '@/config/axios'
import dayjs from 'dayjs'
import { ElMessageBox } from 'element-plus'
import { labels, states, actions } from '@/views/hrm/payroll/input-issue/labels'
defineOptions({ name: 'HrmPortalPayrollInputIssue' })
const base = '/hrm/portal/payroll-input-issue',
  message = useMessage(),
  loading = ref(false),
  loadError = ref(false),
  submitting = ref(false),
  historyVisible = ref(false),
  historyIssueId = ref<number>(),
  rows = ref<any[]>([]),
  events = ref<any[]>([])
const displayTime = (value: string | number | null | undefined) => {
  if (value == null || value === '') return '未知'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : '未知'
}
let active = true
let listVersion = 0
let historyVersion = 0
function clearHistory() {
  historyVersion++
  historyVisible.value = false
  historyIssueId.value = undefined
  events.value = []
}
let submitVersion = 0
let ownsPrompt = false
function invalidateSubmit() {
  submitVersion++
  submitting.value = false
  if (ownsPrompt) {
    ownsPrompt = false
    ElMessageBox.close()
  }
}
async function load() {
  clearHistory()
  invalidateSubmit()
  const version = ++listVersion
  rows.value = []
  loadError.value = false
  loading.value = true
  try {
    const result = await request.get({ url: base + '/list' })
    if (active && version === listVersion) rows.value = result
  } catch {
    if (active && version === listVersion) loadError.value = true
  } finally {
    if (active && version === listVersion) loading.value = false
  }
}
async function submit(id: number) {
  if (!active || loading.value || submitting.value) return
  const row = rows.value.find((row) => row.id === id)
  if (!row || row.status !== 'OPEN' || row.canSubmit !== true) return
  const version = ++submitVersion
  const scope = listVersion
  const batchId = row.batchId
  const employeeId = row.employeeId
  const current = () =>
    active &&
    version === submitVersion &&
    scope === listVersion &&
    rows.value.some(
      (row) =>
        row.id === id &&
        row.batchId === batchId &&
        row.employeeId === employeeId &&
        row.status === 'OPEN' &&
        row.canSubmit === true
    )
  submitting.value = true
  ownsPrompt = true
  try {
    let value: string
    try {
      const result = await message.prompt(
        '请提供补正资料编号或说明，不要填写工资、证件或银行卡明文',
        '补充材料',
        {
          inputType: 'textarea',
          inputValidator: (v: string) =>
            (!!v?.trim() && v.length <= 1000) || '请填写1～1000字的依据'
        }
      )
      value = result.value
    } catch (error) {
      if (error === 'cancel' || error === 'close') return
      throw error
    }
    if (!current()) return
    ownsPrompt = false
    await request.post({ url: base + '/submit', data: { id, evidence: value } })
    if (!current()) return
    message.success('材料已提交，等待指定复核人核实')
    await load()
  } finally {
    if (version === submitVersion) {
      submitting.value = false
      ownsPrompt = false
    }
  }
}
async function history(id: number) {
  clearHistory()
  if (!active || loading.value) return
  const row = rows.value.find((row) => row.id === id)
  if (!row) return
  const version = historyVersion
  const scope = listVersion
  const batchId = row.batchId
  const employeeId = row.employeeId
  const result = await request.get({ url: base + '/history', params: { id } })
  if (
    !active ||
    version !== historyVersion ||
    scope !== listVersion ||
    !rows.value.some(
      (row) => row.id === id && row.batchId === batchId && row.employeeId === employeeId
    )
  )
    return
  events.value = result
  historyIssueId.value = id
  historyVisible.value = true
}
function leavePage() {
  active = false
  listVersion++
  loading.value = false
  clearHistory()
  invalidateSubmit()
}
onMounted(load)
onActivated(() => {
  if (active) return
  active = true
  return load()
})
onDeactivated(leavePage)
onBeforeUnmount(leavePage)
</script>
