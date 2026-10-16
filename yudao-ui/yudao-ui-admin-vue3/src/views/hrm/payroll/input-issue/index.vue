<template>
  <ContentWrap>
    <el-alert
      title="先在对应业务中补正真实来源，再提交材料并由指定复核人确认。未关闭工单将阻断核算与冻结。"
      type="info"
      :closable="false"
    />
    <el-form inline class="mt-4">
      <el-form-item label="法人主体"
        ><el-select v-model="entityId" @change="changeEntity"
          ><el-option
            v-for="e in selectors.entities"
            :key="e.id"
            :value="e.id"
            :label="e.name" /></el-select
      ></el-form-item>
      <el-form-item label="工资批次"
        ><el-select v-model="batchId" filterable @change="loadBatch" style="width: 300px"
          ><el-option
            v-for="b in selectors.batches"
            :key="b.id"
            :value="b.id"
            :label="`${b.title} · ${b.id}`" /></el-select
      ></el-form-item>
      <el-button :disabled="!batchId" @click="loadBatch">刷新</el-button>
    </el-form>
    <el-form inline
      ><el-form-item label="检查员工"
        ><el-select v-model="employeeId" filterable @change="loadSignals"
          ><el-option
            v-for="e in employees"
            :key="e.id"
            :value="e.id"
            :label="`${e.name} · ${e.jobNumber || e.id}`" /></el-select></el-form-item
    ></el-form>
    <el-table v-if="signals.length" :data="signals" v-loading="loading">
      <el-table-column label="就绪项目"
        ><template #default="{ row }">{{ labels[row.kind] }}</template></el-table-column
      >
      <el-table-column label="当前状态" width="120"
        ><template #default="{ row }"
          ><el-tag :type="row.ready ? 'success' : 'warning'">{{
            row.ready ? '已就绪' : '待补正'
          }}</el-tag></template
        ></el-table-column
      >
      <el-table-column prop="reason" label="检查结论" min-width="280" />
      <el-table-column label="操作" width="120"
        ><template #default="{ row }"
          ><el-button
            v-if="!row.ready && editable"
            v-hasPermi="['hrm:payroll:issue:update']"
            link
            @click="openCreate(row)"
            >创建工单</el-button
          ></template
        ></el-table-column
      >
    </el-table>
    <h4>缺口处理工单</h4>
    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="id" label="编号" width="80" /><el-table-column label="员工"
        ><template #default="{ row }">{{ employeeName(row.employeeId) }}</template></el-table-column
      >
      <el-table-column label="项目"
        ><template #default="{ row }">{{ labels[row.kind] }}</template></el-table-column
      >
      <el-table-column label="状态"
        ><template #default="{ row }"
          >{{ states[row.status]
          }}<el-tag v-if="row.overdue" type="danger" class="ml-2">逾期</el-tag></template
        ></el-table-column
      >
      <el-table-column prop="ownerId" label="责任人编号" /><el-table-column
        prop="reviewerId"
        label="复核人编号"
      />
      <el-table-column prop="dueTime" label="截止时间" min-width="165" />
      <el-table-column label="来源状态"
        ><template #default="{ row }">{{
          row.sourceMode === 'FROZEN_HISTORY'
            ? '冻结时证据'
            : row.sourceReady
              ? '当前已就绪'
              : '当前待补正'
        }}</template></el-table-column
      >
      <el-table-column prop="reason" label="处理依据" show-overflow-tooltip />
      <el-table-column label="操作" width="300"
        ><template #default="{ row }">
          <el-button link @click="history(row.id)">履历</el-button>
          <template v-if="editable">
            <el-button
              v-if="row.canSubmit"
              v-hasPermi="['hrm:payroll:issue:update']"
              link
              @click="submit(row.id)"
              >提交材料</el-button
            >
            <el-button
              v-if="row.canReassign"
              v-hasPermi="['hrm:payroll:issue:update']"
              link
              @click="openAssign(row)"
              >交接</el-button
            >
            <template v-if="row.status === 'SUBMITTED' && row.canReview"
              ><el-button
                v-hasPermi="['hrm:payroll:issue:review']"
                link
                @click="review(row.id, true)"
                >核实关闭</el-button
              ><el-button
                v-hasPermi="['hrm:payroll:issue:review']"
                link
                @click="review(row.id, false)"
                >退回</el-button
              ></template
            >
            <el-button
              v-if="row.status === 'CLOSED' && !row.sourceReady && row.canReview"
              v-hasPermi="['hrm:payroll:issue:review']"
              link
              @click="reopen(row.id)"
              >重开</el-button
            >
          </template>
        </template></el-table-column
      >
    </el-table>
  </ContentWrap>
  <Dialog v-model="dialog" :title="draft.id ? '工单责任交接' : '创建输入缺口工单'" width="600px">
    <el-form label-width="100px"
      ><el-form-item label="缺口项目">{{ labels[draft.kind] }}</el-form-item>
      <el-form-item label="责任人"
        ><el-select v-model="draft.ownerId" filterable
          ><el-option
            v-for="u in people.owners"
            :key="u.id"
            :value="u.id"
            :label="`${u.name} · ${u.id}`" /></el-select
      ></el-form-item>
      <el-form-item label="独立复核人"
        ><el-select v-model="draft.reviewerId" filterable
          ><el-option
            v-for="u in people.reviewers.filter((u) => u.id !== draft.ownerId)"
            :key="u.id"
            :value="u.id"
            :label="`${u.name} · ${u.id}`" /></el-select
      ></el-form-item>
      <el-form-item label="截止时间"
        ><el-date-picker v-model="draft.dueTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
      /></el-form-item>
      <el-form-item label="处理依据"
        ><el-input
          v-model="draft.reason"
          type="textarea"
          maxlength="1000"
          show-word-limit /></el-form-item
    ></el-form>
    <template #footer
      ><el-button
        type="primary"
        :loading="saving"
        :disabled="
          !draft.ownerId ||
          !draft.reviewerId ||
          draft.ownerId === draft.reviewerId ||
          !draft.dueTime ||
          !draft.reason.trim()
        "
        @click="save"
        >提交</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="historyVisible" title="工单处理履历"
    ><el-table :data="events"
      ><el-table-column label="动作"
        ><template #default="{ row }">{{
          actions[row.action] || row.action
        }}</template></el-table-column
      ><el-table-column prop="actorId" label="操作人编号" /><el-table-column
        prop="reason"
        label="依据" /><el-table-column prop="createTime" label="时间" min-width="165" /></el-table
  ></Dialog>
</template>
<script setup lang="ts">
import request from '@/config/axios'
import dayjs from 'dayjs'
import { labels, states, actions } from './labels'
defineOptions({ name: 'HrmPayrollInputIssue' })
const base = '/hrm/payroll/input-issue',
  message = useMessage()
const entityId = ref<number>(),
  batchId = ref<number>(),
  employeeId = ref<number>(),
  loading = ref(false),
  saving = ref(false),
  dialog = ref(false),
  historyVisible = ref(false)
const selectors = reactive<any>({ entities: [], batches: [] }),
  employees = ref<any[]>([]),
  signals = ref<any[]>([]),
  rows = ref<any[]>([]),
  events = ref<any[]>([]),
  people = reactive<any>({ owners: [], reviewers: [] }),
  draft = reactive<any>({ reason: '' })
const editable = computed(() =>
  [5, 11, 12, 13, 14].includes(selectors.batches.find((b) => b.id === batchId.value)?.status)
)
const employeeName = (id: number) => {
  const e = employees.value.find((e) => e.id === id)
  return e ? `${e.name} · ${e.jobNumber || id}` : id
}
async function changeEntity() {
  batchId.value = undefined
  employeeId.value = undefined
  rows.value = []
  signals.value = []
  employees.value = []
  Object.assign(
    selectors,
    await request.get({ url: base + '/selectors', params: { entityId: entityId.value } })
  )
}
async function loadBatch() {
  if (!batchId.value) return
  loading.value = true
  try {
    const [a, b] = await Promise.all([
      request.get({ url: base + '/list', params: { batchId: batchId.value } }),
      request.get({ url: base + '/employees', params: { batchId: batchId.value } })
    ])
    rows.value = a
    employees.value = b
    employeeId.value = undefined
    signals.value = []
  } finally {
    loading.value = false
  }
}
async function loadSignals() {
  signals.value = []
  if (!employeeId.value || !batchId.value) return
  loading.value = true
  try {
    signals.value = await request.get({
      url: base + '/signals',
      params: { batchId: batchId.value, employeeId: employeeId.value }
    })
  } finally {
    loading.value = false
  }
}
async function options(id: number) {
  Object.assign(
    people,
    await request.get({
      url: base + '/options',
      params: { batchId: batchId.value, employeeId: id }
    })
  )
}
async function openCreate(row: any) {
  await options(row.employeeId)
  Object.assign(draft, {
    id: undefined,
    batchId: batchId.value,
    employeeId: row.employeeId,
    kind: row.kind,
    ownerId: undefined,
    reviewerId: undefined,
    dueTime: dayjs().add(2, 'day').format('YYYY-MM-DDTHH:mm:ss'),
    reason: '',
    requestKey: crypto.randomUUID()
  })
  dialog.value = true
}
async function openAssign(row: any) {
  await options(row.employeeId)
  Object.assign(draft, {
    ...row,
    dueTime: dayjs().add(2, 'day').format('YYYY-MM-DDTHH:mm:ss'),
    reason: ''
  })
  dialog.value = true
}
async function save() {
  saving.value = true
  try {
    const data = draft.id
      ? {
          id: draft.id,
          ownerId: draft.ownerId,
          reviewerId: draft.reviewerId,
          dueTime: draft.dueTime,
          reason: draft.reason
        }
      : { ...draft }
    await request.post({ url: base + (draft.id ? '/reassign' : '/create'), data })
    dialog.value = false
    message.success('工单已保存')
    await loadBatch()
  } finally {
    saving.value = false
  }
}
async function note(title: string) {
  const { value } = await message.prompt(title, '处理依据', {
    inputType: 'textarea',
    inputValidator: (v: string) => (!!v?.trim() && v.length <= 1000) || '请填写1～1000字的依据'
  })
  return value
}
async function submit(id: number) {
  const evidence = await note('请提供补正资料编号或说明，不要填写工资、证件或银行卡明文')
  await request.post({ url: base + '/submit', data: { id, evidence } })
  await loadBatch()
}
async function review(id: number, approved: boolean) {
  const reason = await note(approved ? '请填写独立核实来源的依据' : '请填写退回补正原因')
  await request.post({ url: base + '/review', data: { id, approved, reason } })
  await loadBatch()
}
async function reopen(id: number) {
  const reason = await note('请说明来源再次失效的原因')
  await request.post({ url: base + '/reopen', data: { id, reason } })
  await loadBatch()
}
async function history(id: number) {
  events.value = await request.get({ url: base + '/history', params: { id } })
  historyVisible.value = true
}
onMounted(async () => Object.assign(selectors, await request.get({ url: base + '/selectors' })))
</script>
