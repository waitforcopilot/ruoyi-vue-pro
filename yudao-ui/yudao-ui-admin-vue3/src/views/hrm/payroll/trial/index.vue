<template>
  <div class="trial-page">
    <ContentWrap>
      <div class="heading"
        ><div
          ><h2>批次资料核验与试算</h2
          ><p>按明确主体、期间和人员核对工资资料，保存可回放的计算版本。</p></div
        ><el-button v-if="canMaintain" type="primary" @click="editor?.open()"
          >登记试算批次</el-button
        ></div
      >
      <el-alert
        title="试算须匹配已确认的人员资格和计算规则。实发须等于应发减扣款减个税；本页保存试算并进行两级版本复核与冻结，发放另行处理。"
        type="info"
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
      <div class="toolbar"
        ><el-input
          v-model="query.entityCode"
          placeholder="主体编号（精确）"
          aria-label="查询批次主体"
          @keyup.enter="search"
        /><el-input
          v-model="query.search"
          placeholder="批次名称"
          aria-label="查询批次名称"
          @keyup.enter="search"
        /><el-select v-model="query.status" clearable placeholder="全部状态"
          ><el-option v-for="(s, i) in Api.states" :key="i" :label="s" :value="i" /></el-select
        ><el-button @click="search">查询</el-button><el-button @click="reset">重置</el-button></div
      >
      <el-table v-loading="loading" :data="rows" border row-key="id" data-testid="trial-table">
        <el-table-column label="批次" min-width="210"
          ><template #default="{ row }"
            >{{ row.title }}<small>{{ row.code }}</small></template
          ></el-table-column
        >
        <el-table-column label="主体" min-width="190"
          ><template #default="{ row }"
            >{{ row.entityName }}<small>{{ row.entityCode }}</small></template
          ></el-table-column
        >
        <el-table-column label="期间" min-width="225"
          ><template #default="{ row }"
            >{{ row.periodStart }} 至 {{ row.periodEnd }}</template
          ></el-table-column
        >
        <el-table-column label="状态 / 人数" min-width="150"
          ><template #default="{ row }"
            ><el-tag
              :type="
                row.status >= 3 || row.status === 1
                  ? 'success'
                  : row.status === 2
                    ? 'info'
                    : 'warning'
              "
              >{{ Api.states[row.status] }}</el-tag
            >
            · {{ row.personCount }} 人</template
          ></el-table-column
        >
        <el-table-column prop="ownerName" label="负责人" min-width="120" />
        <el-table-column label="操作" min-width="185"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="select(row.id)">核验与版本</el-button
            ><el-button
              v-if="canMaintain && row.status <= 1"
              link
              type="primary"
              @click="editor?.open(row.id)"
              >编辑</el-button
            ></template
          ></el-table-column
        >
      </el-table>
      <Pagination
        v-model:page="query.pageNo"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="load"
      />
    </ContentWrap>
    <ContentWrap v-if="selected" v-loading="detailLoading">
      <div class="heading"
        ><div
          ><h3>{{ selected.title }} · {{ selected.code }}</h3
          ><p
            >{{ selected.entityName }} · {{ selected.periodStart }} 至 {{ selected.periodEnd }}</p
          ></div
        ><div class="actions"
          ><el-button :loading="checking" :disabled="executing" @click="verify">核验资料</el-button
          ><el-button
            v-if="canExecute && !pendingCommand"
            type="primary"
            :loading="executing"
            :disabled="selected.status! > 1 || !check?.ready || checking || detailLoading"
            @click="execute"
            >保存试算版本</el-button
          ><el-button
            v-if="canExecute && pendingCommand && selected.status! <= 1"
            type="primary"
            :loading="executing"
            @click="execute"
            >重试保存试算</el-button
          ></div
        ></div
      >
      <el-alert
        v-if="pendingCommand && !executing"
        title="上次请求尚未确认结果；重试会使用同一个请求编号，避免重复创建试算版本。"
        type="warning"
        :closable="false"
      />
      <div v-if="check" data-testid="trial-check"
        ><el-alert
          :title="
            check.ready
              ? selected.status! > 1
                ? '资料核验通过，当前处于复核或冻结阶段'
                : '资料核验通过，可以保存试算版本'
              : '资料尚未就绪，请处理阻断项'
          "
          :type="check.ready ? 'success' : 'warning'"
          :closable="false"
        /><p
          >纳入 {{ check.includedCount }} 人 · 排除 {{ check.excludedCount }} 人 · 阻断
          {{ check.blockedCount }} 人 · {{ check.checkedAt }}</p
        ><ul v-if="check.issues.length"
          ><li v-for="i in check.issues" :key="i.code">{{ i.message }}</li></ul
        >
        <el-table :data="check.people" border
          ><el-table-column label="人员" min-width="210"
            ><template #default="{ row }"
              >{{ row.name }}<small>HRM #{{ row.employeeId }}</small></template
            ></el-table-column
          ><el-table-column label="资格 / 状态" min-width="160"
            ><template #default="{ row }"
              >{{
                row.state === 'EXCLUDED' ? '排除计薪' : row.state === 'READY' ? '核验通过' : '阻断'
              }}<small v-if="row.eligibilityVersion"
                >资格 V{{ row.eligibilityVersion }}</small
              ></template
            ></el-table-column
          ><el-table-column label="待处理事项" min-width="320"
            ><template #default="{ row }"
              ><p v-for="i in row.issues" :key="i.code">{{ i.message }}</p
              ><span v-if="!row.issues.length">{{
                row.state === 'EXCLUDED' ? '不生成工资金额' : '已核对资格、来源与金额'
              }}</span></template
            ></el-table-column
          ></el-table
        >
      </div>
      <ReviewPanel
        :key="selected.id"
        :batch="selected"
        :runs="runs"
        class="mt-6"
        @changed="reviewChanged"
      />
      <el-tabs v-model="tab" class="mt-5">
        <el-tab-pane label="试算版本" name="runs"
          ><div class="version-toolbar"
            ><el-select
              v-model="runId"
              clearable
              placeholder="选择已保存试算版本"
              aria-label="查看试算版本"
              @change="loadRun"
              ><el-option
                v-for="r in runs"
                :key="r.id"
                :value="r.id"
                :label="`V${r.runVersion} · ${r.executedAt}`" /></el-select
            ><span v-if="selected.currentRunId">当前有效试算版本已保存</span
            ><span v-else-if="selected.latestRunId"
              >草稿已改变，请核验后生成新版本；旧版本保留</span
            ></div
          ><div v-loading="runLoading"
            ><TrialRun v-if="run" :run="run" /><el-empty
              v-else
              description="尚未选择试算版本" /></div
        ></el-tab-pane>
        <el-tab-pane label="版本比较" name="compare"
          ><div class="version-toolbar"
            ><el-select v-model="leftId" placeholder="比较基准版本" aria-label="比较基准版本"
              ><el-option
                v-for="r in runs"
                :key="r.id"
                :value="r.id"
                :label="'V' + r.runVersion" /></el-select
            ><el-select v-model="rightId" placeholder="比较目标版本" aria-label="比较目标版本"
              ><el-option
                v-for="r in runs"
                :key="r.id"
                :value="r.id"
                :label="'V' + r.runVersion" /></el-select
            ><el-button :loading="comparing" :disabled="!leftId || !rightId" @click="compare"
              >比较版本</el-button
            ></div
          >
          <div v-if="comparison" data-testid="trial-comparison"
            ><p
              >V{{ comparison.left.runVersion }} → V{{ comparison.right.runVersion }} ·
              {{
                comparison.ruleChanged
                  ? '计算规则、方案或来源绑定有变化'
                  : '计算规则、方案与来源绑定一致'
              }}</p
            ><div class="diff-totals"
              ><p v-for="(label, role) in Api.roleLabels" :key="role"
                >{{ label }}合计变化 · CNY<strong>{{
                  comparison.totalDifferences[role]
                }}</strong></p
              ></div
            ><el-table :data="comparison.people" border
              ><el-table-column label="人员" min-width="200"
                ><template #default="{ row }"
                  >{{ row.name }}<small>HRM #{{ row.employeeId }}</small></template
                ></el-table-column
              ><el-table-column label="变化" min-width="145"
                ><template #default="{ row }">{{
                  Api.changes[row.change]
                }}</template></el-table-column
              ><el-table-column label="原实发" min-width="130" align="right"
                ><template #default="{ row }">{{
                  row.leftAmounts?.net ?? '—'
                }}</template></el-table-column
              ><el-table-column label="新实发" min-width="130" align="right"
                ><template #default="{ row }">{{
                  row.rightAmounts?.net ?? '—'
                }}</template></el-table-column
              ><el-table-column
                v-for="(label, role) in Api.roleLabels"
                :key="role"
                :label="label + '差额'"
                min-width="130"
                align="right"
                ><template #default="{ row }">{{
                  row.differences?.[role] ?? '—'
                }}</template></el-table-column
              ></el-table
            ><p class="note">差額按目标版本减基准版本；新增、移除或排除人员不以零工资替代。</p></div
          >
        </el-tab-pane>
        <el-tab-pane label="操作历史" name="history"
          ><el-table :data="history" border data-testid="trial-history"
            ><el-table-column label="操作" min-width="130"
              ><template #default="{ row }">{{
                actionLabels[row.action] || row.action
              }}</template></el-table-column
            ><el-table-column prop="actorName" label="操作人" min-width="150" /><el-table-column
              prop="reason"
              label="说明"
              min-width="380"
            /><el-table-column label="资料版本" min-width="130"
              ><template #default="{ row }"
                >{{ row.fromVersion ?? '初始' }} → {{ row.toVersion }}</template
              ></el-table-column
            ></el-table
          ></el-tab-pane
        >
      </el-tabs>
    </ContentWrap>
    <TrialEditor ref="editor" @saved="saved" />
  </div>
</template>
<script setup lang="ts">
import * as Api from '@/api/hrm/payroll/trial'
import type { History } from '@/api/hrm/payroll/requirements'
import { checkPermi } from '@/utils/permission'
import TrialEditor from './TrialEditor.vue'
import TrialRun from './TrialRun.vue'
import ReviewPanel from './ReviewPanel.vue'
import { actionLabels as reviewActionLabels } from '@/api/hrm/payroll/review'
defineOptions({ name: 'HrmPayrollTrial' })
const dependencies = [
  'hrm:employee:query',
  'hrm:payroll:trial:query',
  'hrm:payroll:calculation:query',
  'hrm:payroll:eligibility:query'
]
const canMaintain = computed(
  () => dependencies.every((p) => checkPermi([p])) && checkPermi(['hrm:payroll:trial:maintain'])
)
const canExecute = computed(
  () => dependencies.every((p) => checkPermi([p])) && checkPermi(['hrm:payroll:trial:execute'])
)
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  entityCode: '',
  search: '',
  status: undefined as number | undefined
})
const rows = ref<Api.Batch[]>([]),
  total = ref(0),
  loading = ref(false),
  error = ref(''),
  selected = ref<Api.Batch>(),
  detailLoading = ref(false),
  check = ref<Api.Check>(),
  checking = ref(false),
  executing = ref(false),
  pendingCommand = ref<Api.Command>()
const editor = ref<InstanceType<typeof TrialEditor>>(),
  runs = ref<Api.Run[]>([]),
  run = ref<Api.Run>(),
  runId = ref<number>(),
  runLoading = ref(false),
  leftId = ref<number>(),
  rightId = ref<number>(),
  comparison = ref<Api.Comparison>(),
  comparing = ref(false),
  history = ref<History[]>([]),
  tab = ref('runs')
const actionLabels: Record<string, string> = {
  create: '登记草稿',
  update: '维护草稿',
  execute: '保存试算',
  ...reviewActionLabels,
  'bpm-approved': '流程复核通过',
  'bpm-rejected': '流程驳回',
  'bpm-cancelled': '流程撤销'
}
let listTicket = 0,
  selectionTicket = 0,
  checkTicket = 0,
  runTicket = 0,
  compareTicket = 0
const load = async () => {
  const ticket = ++listTicket
  loading.value = true
  error.value = ''
  try {
    const data = await Api.page(query)
    if (ticket !== listTicket) return
    rows.value = data.list
    total.value = data.total
  } catch (e: any) {
    if (ticket === listTicket) error.value = e?.message || String(e)
  } finally {
    if (ticket === listTicket) loading.value = false
  }
}
const clearDetail = () => {
  selectionTicket++
  checkTicket++
  runTicket++
  compareTicket++
  selected.value = undefined
  check.value = undefined
  run.value = undefined
  runId.value = undefined
  runs.value = []
  history.value = []
  comparison.value = undefined
  leftId.value = undefined
  rightId.value = undefined
  pendingCommand.value = undefined
  checking.value = false
  runLoading.value = false
  comparing.value = false
}
const search = () => {
  query.pageNo = 1
  clearDetail()
  load()
}
const reset = () => {
  Object.assign(query, { entityCode: '', search: '', status: undefined })
  search()
}
const select = async (id: number) => {
  clearDetail()
  const ticket = selectionTicket
  detailLoading.value = true
  error.value = ''
  try {
    const [batch, versions, audits] = await Promise.all([
      Api.get(id),
      Api.runs(id),
      Api.history(id)
    ])
    if (ticket !== selectionTicket) return
    selected.value = batch
    runs.value = versions
    history.value = audits
    tab.value = 'runs'
    runId.value = batch.currentRunId || batch.latestRunId
    if (runId.value) await loadRun()
  } catch (e: any) {
    if (ticket === selectionTicket) error.value = e?.message || String(e)
  } finally {
    if (ticket === selectionTicket) detailLoading.value = false
  }
}
const verify = async () => {
  if (!selected.value) return
  const id = selected.value.id!,
    selection = selectionTicket,
    ticket = ++checkTicket
  check.value = undefined
  checking.value = true
  error.value = ''
  try {
    const data = await Api.check(id)
    if (ticket !== checkTicket || selection !== selectionTicket) return
    check.value = data
  } catch (e: any) {
    if (ticket === checkTicket && selection === selectionTicket)
      error.value = e?.message || String(e)
  } finally {
    if (ticket === checkTicket) checking.value = false
  }
}
const execute = async () => {
  if (!selected.value || !canExecute.value) return
  if (!pendingCommand.value) {
    if (!check.value?.ready) return
    pendingCommand.value = {
      batchId: selected.value.id!,
      revision: check.value.revision,
      sourceHash: check.value.sourceHash,
      requestKey: crypto.randomUUID()
    }
  }
  const command = { ...pendingCommand.value },
    selection = selectionTicket
  executing.value = true
  error.value = ''
  try {
    const data = await Api.execute(command)
    if (selection !== selectionTicket) return
    pendingCommand.value = undefined
    check.value = undefined
    await select(command.batchId)
    if (selected.value?.id === command.batchId) {
      runId.value = data.id
      run.value = data
      await load()
    }
  } catch (e: any) {
    if (selection === selectionTicket) error.value = e?.message || String(e)
  } finally {
    executing.value = false
  }
}
const loadRun = async () => {
  const id = runId.value,
    ticket = ++runTicket,
    selection = selectionTicket
  run.value = undefined
  if (!id) {
    runLoading.value = false
    return
  }
  runLoading.value = true
  try {
    const data = await Api.run(id)
    if (ticket === runTicket && selection === selectionTicket) run.value = data
  } catch (e: any) {
    if (ticket === runTicket) error.value = e?.message || String(e)
  } finally {
    if (ticket === runTicket) runLoading.value = false
  }
}
watch([leftId, rightId], () => {
  compareTicket++
  comparison.value = undefined
  comparing.value = false
})
const compare = async () => {
  if (!leftId.value || !rightId.value) return
  const ticket = ++compareTicket,
    selection = selectionTicket
  comparison.value = undefined
  comparing.value = true
  try {
    const data = await Api.compare(leftId.value, rightId.value)
    if (ticket === compareTicket && selection === selectionTicket) comparison.value = data
  } catch (e: any) {
    if (ticket === compareTicket) error.value = e?.message || String(e)
  } finally {
    if (ticket === compareTicket) comparing.value = false
  }
}
const reviewChanged = async (id: number) => {
  const selection = selectionTicket
  if (selected.value?.id !== id) return
  try {
    const [batch, versions, records] = await Promise.all([
      Api.get(id),
      Api.runs(id),
      Api.history(id)
    ])
    if (selection !== selectionTicket || selected.value?.id !== id) return
    selected.value = batch
    runs.value = versions
    history.value = records
    check.value = undefined
    await load()
  } catch (e: any) {
    if (selection === selectionTicket) error.value = e?.message || String(e)
  }
}
const saved = async (id: number) => {
  await load()
  await select(id)
}
const route = useRoute()
watch(
  () => [route.path, route.query.batchId],
  ([path, value]) => {
    if (path !== '/hrm/payroll-trial-batches') {
      clearDetail()
      return
    }
    if (
      typeof value === 'string' &&
      /^[1-9]\d*$/.test(value) &&
      Number.isSafeInteger(Number(value))
    )
      select(Number(value))
    else clearDetail()
  }
)
onMounted(async () => {
  await load()
  if (route.path !== '/hrm/payroll-trial-batches') return
  const id = route.query.batchId
  if (typeof id === 'string' && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id)))
    await select(Number(id))
})
</script>
<style scoped>
.trial-page {
  min-width: 0;
}
.heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.heading h2 {
  font-size: 22px;
  margin: 0 0 8px;
}
.heading p,
.note,
small {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}
small {
  display: block;
  margin-top: 5px;
}
.toolbar,
.version-toolbar,
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin: 18px 0;
}
.toolbar > .el-input,
.toolbar > .el-select,
.version-toolbar > .el-select {
  width: 240px;
}
.diff-totals {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.diff-totals p {
  padding: 14px;
  background: #f3f6fb;
  font-size: 13px;
}
.diff-totals strong {
  display: block;
  font-size: 21px;
  color: #1e40af;
  margin-top: 8px;
}
@media (max-width: 720px) {
  .toolbar > .el-input,
  .toolbar > .el-select,
  .version-toolbar > .el-select {
    width: 100%;
  }
  .diff-totals {
    grid-template-columns: 1fr;
  }
  .heading {
    align-items: flex-start;
  }
  .actions {
    width: 100%;
  }
}
</style>
