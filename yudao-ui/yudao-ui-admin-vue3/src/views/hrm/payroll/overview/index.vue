<template>
  <div class="overview-page">
    <ContentWrap>
      <div class="heading">
        <div
          ><h2>薪酬批次概览</h2><p>按主体和完整期间查询批次，查看真实复核进度与保存版本。</p></div
        >
        <el-button :loading="loading" @click="refresh">刷新概览</el-button>
      </div>
      <el-form :inline="true" class="filters" @submit.prevent="search">
        <el-form-item label="主体编号"
          ><el-input v-model="query.entityCode" aria-label="查询主体编号" clearable maxlength="64"
        /></el-form-item>
        <el-form-item label="批次名称"
          ><el-input v-model="query.search" aria-label="查询批次名称" clearable maxlength="160"
        /></el-form-item>
        <el-form-item label="完整期间"
          ><el-date-picker
            v-model="period"
            aria-label="查询完整期间"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="起日"
            end-placeholder="止日"
        /></el-form-item>
        <el-form-item label="批次状态"
          ><el-select
            v-model="query.status"
            aria-label="查询批次状态"
            clearable
            class="status-filter"
            ><el-option
              v-for="(label, i) in states"
              :key="i"
              :label="label"
              :value="i" /></el-select
        ></el-form-item>
        <el-form-item
          ><el-button type="primary" native-type="submit" :loading="loading">查询</el-button
          ><el-button @click="reset">重置</el-button></el-form-item
        >
      </el-form>
      <p class="note"
        >期间按批次起止日完整匹配。状态与待办只统计当前查询范围内有权查看的完整批次。</p
      >
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        :closable="false"
        data-testid="overview-error"
      />
      <template v-if="summary">
        <p class="note">更新于 {{ summary.generatedAt }} · {{ summary.total }} 个批次</p>
        <div class="state-cards" data-testid="overview-states">
          <div v-for="(label, i) in states" :key="i" class="card"
            ><span>{{ label }}</span
            ><strong :data-testid="'overview-state-' + i">{{ summary.states[i] ?? 0 }}</strong
            ><small>个批次</small></div
          >
        </div>
        <div class="todos" data-testid="overview-todos">
          <div
            ><h3>分配给我的复核任务</h3
            ><p
              >HR 复核 <strong data-testid="assigned-hr">{{ summary.assignedHr }}</strong> 项 ·
              财务复核
              <strong data-testid="assigned-finance">{{ summary.assignedFinance }}</strong> 项</p
            ><small>来自当前版本的实际审批待办；办理仍需具备本级权限。</small></div
          >
          <el-button @click="reviewBatches">查看复核批次</el-button>
        </div>
        <el-table
          :data="summary.batches.list"
          border
          data-testid="overview-table"
          v-loading="loading"
        >
          <el-table-column label="批次" min-width="220"
            ><template #default="{ row }"
              ><strong>{{ row.title }}</strong
              ><small>{{ row.code }}</small></template
            ></el-table-column
          >
          <el-table-column label="主体与期间" min-width="230"
            ><template #default="{ row }"
              >{{ row.entityName
              }}<small
                >{{ row.entityCode }} · {{ row.periodStart }} 至 {{ row.periodEnd }}</small
              ></template
            ></el-table-column
          >
          <el-table-column label="状态与进度" min-width="170"
            ><template #default="{ row }"
              ><el-tag>{{ states[row.status] || '待核对' }}</el-tag
              ><small>{{ stages[row.stage] || '需核对流程状态' }}</small
              ><el-tag v-if="row.assignedToMe" type="warning" size="small"
                >我的复核任务</el-tag
              ></template
            ></el-table-column
          >
          <el-table-column prop="personCount" label="登记人数" width="95" />
          <el-table-column label="查看" width="170"
            ><template #default="{ row }"
              ><el-button link type="primary" @click="select(row.id)">金额与资料核验</el-button
              ><el-button link @click="navigate(row.id)">进入批次核对</el-button></template
            ></el-table-column
          >
        </el-table>
        <el-empty
          v-if="!summary.total"
          description="当前范围没有可查看的批次。请核对主体、期间与权限范围。"
        />
        <Pagination
          :total="summary.batches.total"
          v-model:page="query.pageNo"
          v-model:limit="query.pageSize"
          @pagination="load"
        />
      </template>
      <el-empty v-else-if="!loading && !error" description="暂未取得批次状态，请刷新重试。" />
    </ContentWrap>
    <ContentWrap v-if="selectedId" v-loading="detailLoading" data-testid="overview-detail">
      <template v-if="detail">
        <div class="heading"
          ><div
            ><h3>{{ detail.batch.title }}</h3
            ><p
              >{{ detail.batch.entityName }} · {{ detail.batch.periodStart }} 至
              {{ detail.batch.periodEnd }} · {{ detail.batch.code }}</p
            ></div
          ><el-button type="primary" @click="navigate(detail.batch.id)"
            >进入批次核对</el-button
          ></div
        >
        <el-alert
          :title="availability[detail.availability]"
          :type="detail.availability === 'CURRENT' ? 'success' : 'warning'"
          :closable="false"
        />
        <p class="note" v-if="detail.runId"
          >保存版本 V{{ detail.runVersion }} · {{ detail.executedByName }} ·
          {{ detail.executedAt }} · 纳入 {{ detail.includedCount }} 人，明确排除
          {{ detail.excludedCount }} 人</p
        >
        <div class="amount-cards" data-testid="overview-amounts">
          <div v-for="(label, key) in roleLabels" :key="key" class="card"
            ><span>{{ label }} · CNY</span
            ><strong :data-testid="'amount-' + key">{{ detail.amounts?.[key] ?? '—' }}</strong
            ><small>所选保存版本的金额</small></div
          >
          <div class="card muted"
            ><span>法定成本</span><strong>—</strong
            ><small>单位缴费尚未关联，暂未生成成本汇总</small></div
          >
        </div>
        <p class="note"
          >同一人员或期间可能出现在多个批次，本页金额始终来自所选批次的同一保存版本。冻结不表示已付款或已完税。</p
        >
        <div class="source-grid">
          <div class="source-card"
            ><h3>当前资料核验</h3
            ><el-tag :type="detail.check.ready ? 'success' : 'danger'">{{
              detail.check.ready ? '试算资料已就绪' : '存在阻断项'
            }}</el-tag
            ><p
              >可计薪 {{ detail.check.includedCount }} 人 · 明确排除
              {{ detail.check.excludedCount }} 人 · 阻断 {{ detail.check.blockedCount }} 人</p
            ><small
              >当前人员、资格、规则与已登记输入的核验结果。资料变化须先核对并重新试算。</small
            ></div
          >
          <div class="source-card"
            ><h3>来源归集</h3><p>当前工资输入按批次及人员登记来源依据。</p
            ><ul
              ><li>考勤、加班与工时尚未自动关联</li
              ><li>社保与公积金缴费月账尚未绑定</li
              ><li>个税金额来自登记输入，申报与完税状态尚未接入</li></ul
            ><small>已登记输入及试算就绪，不代表这些来源已完成自动归集。</small></div
          >
        </div>
        <el-alert
          v-for="(issue, i) in detail.check.issues"
          :key="i"
          :title="issue.message"
          type="error"
          :closable="false"
          class="issue"
        />
        <el-table
          v-if="blockedPeople.length"
          :data="blockedPeople"
          border
          data-testid="overview-issues"
          class="mt-4"
          ><el-table-column prop="name" label="人员" min-width="150" /><el-table-column
            label="待核对内容"
            min-width="340"
            ><template #default="{ row }"
              ><div v-for="(issue, i) in row.issues" :key="i">{{ issue.message }}</div></template
            ></el-table-column
          ></el-table
        >
      </template>
      <p v-else-if="!detailLoading">所选批次暂未取得核验结果，请重新选择或刷新。</p>
    </ContentWrap>
  </div>
</template>
<script setup lang="ts">
import * as Api from '@/api/hrm/payroll/overview'
import { roleLabels, states } from '@/api/hrm/payroll/trial'
defineOptions({ name: 'HrmPayrollOverview' })
const router = useRouter()
const query = reactive({
  entityCode: '',
  search: '',
  status: undefined as number | undefined,
  pageNo: 1,
  pageSize: 10
})
const period = ref<[string, string] | null>(null)
const summary = ref<Api.Overview>()
const detail = ref<Api.Detail>()
const selectedId = ref<number>()
const loading = ref(false),
  detailLoading = ref(false),
  error = ref('')
let loadTicket = 0,
  detailTicket = 0
const stages: Record<string, string> = {
  CHECK_INPUTS: '待核验资料',
  SUBMIT_REVIEW: '待提交复核',
  CHECK_BPM: '需同步流程状态',
  CHECK_FREEZE: '待核对冻结',
  FROZEN: '保存版本已冻结',
  HR_REVIEW: '待 HR 复核',
  FINANCE_REVIEW: '待财务复核'
}
const availability: Record<string, string> = {
  NONE: '尚无保存的试算版本，金额未生成。',
  UNAVAILABLE: '保存版本暂不可用，需核对批次资料。',
  INVALIDATED: '当前试算已失效，下方旧版本金额仅作历史参考；须重新试算和复核。',
  SOURCE_CHANGED: '来源资料已变化，下方为原保存版本金额；须核对后重新试算。',
  CURRENT: '当前来源与所选保存版本一致。金额为试算结果，发放状态另行核对。'
}
const blockedPeople = computed(
  () => detail.value?.check.people.filter((p) => p.issues.length) || []
)
const clearDetail = () => {
  detailTicket++
  selectedId.value = undefined
  detail.value = undefined
  detailLoading.value = false
}
const load = async () => {
  const ticket = ++loadTicket
  clearDetail()
  summary.value = undefined
  error.value = ''
  loading.value = true
  const params = { ...query, periodStart: period.value?.[0], periodEnd: period.value?.[1] }
  try {
    const data = await Api.page(params)
    if (ticket === loadTicket) summary.value = data
  } catch (e: any) {
    if (ticket === loadTicket) error.value = e?.message || String(e)
  } finally {
    if (ticket === loadTicket) loading.value = false
  }
}
const select = async (id: number) => {
  const ticket = ++detailTicket
  selectedId.value = id
  detail.value = undefined
  detailLoading.value = true
  error.value = ''
  try {
    const data = await Api.batch(id)
    if (ticket === detailTicket && selectedId.value === id) detail.value = data
  } catch (e: any) {
    if (ticket === detailTicket) error.value = e?.message || String(e)
  } finally {
    if (ticket === detailTicket) detailLoading.value = false
  }
}
const search = () => {
  query.pageNo = 1
  load()
}
const reset = () => {
  Object.assign(query, { entityCode: '', search: '', status: undefined })
  period.value = null
  search()
}
const reviewBatches = () => {
  query.status = 2
  search()
}
const refresh = async () => {
  const id = selectedId.value
  await load()
  if (id && summary.value?.batches.list.some((b) => b.id === id)) await select(id)
}
const navigate = (id: number) =>
  router.push({ path: '/hrm/payroll-trial-batches', query: { batchId: String(id) } })
onMounted(load)
onBeforeUnmount(() => {
  loadTicket++
  clearDetail()
})
</script>
<style scoped>
.overview-page {
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
.heading h3 {
  margin: 0 0 8px;
}
.heading p,
.note,
small {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
small {
  display: block;
  margin-top: 5px;
}
.filters {
  margin-top: 18px;
}
.status-filter {
  width: 145px;
}
.state-cards,
.amount-cards {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0;
}
.card {
  border: 1px solid #dce4f0;
  border-radius: 8px;
  padding: 16px;
  background: #f5f8fd;
  min-width: 0;
}
.card > span {
  color: #64748b;
}
.card strong {
  display: block;
  margin-top: 12px;
  font-size: 25px;
  color: #234ac9;
  overflow-wrap: anywhere;
}
.amount-cards .card strong {
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}
.muted {
  background: #f8fafc;
}
.muted strong {
  color: #94a3b8;
}
.todos {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #dce4f0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.todos h3 {
  margin: 0;
  font-size: 16px;
}
.source-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}
.source-card {
  border: 1px solid #dce4f0;
  padding: 16px;
  border-radius: 8px;
  line-height: 1.7;
}
.source-card h3 {
  margin: 0 0 10px;
  font-size: 16px;
}
.source-card ul {
  padding-left: 20px;
  color: #64748b;
}
.issue {
  margin-top: 12px;
}
@media (max-width: 1100px) {
  .state-cards,
  .amount-cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 640px) {
  .state-cards,
  .amount-cards,
  .source-grid {
    grid-template-columns: 1fr;
  }
  .filters :deep(.el-form-item),
  .filters :deep(.el-form-item__content),
  .filters :deep(.el-date-editor) {
    width: 100%;
    min-width: 0;
  }
  .filters :deep(.el-form-item) {
    margin-right: 0;
  }
}
</style>
