<template>
  <div class="rules-page">
    <ContentWrap>
      <div class="heading"
        ><div><h2>薪酬规则台账</h2><p>核实业务口径，保存适用范围、版本与预期样例。</p></div
        ><el-tag type="warning">正式核算接入待完成</el-tag></div
      >
      <el-alert
        title="本页完成规则口径评审；正式核算接入还需确认人员范围、来源完整性及计算实现。业务样例须使用合成或脱敏数据。"
        type="info"
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
      <div class="toolbar">
        <el-input
          v-model="query.search"
          clearable
          placeholder="搜索规则编号或名称"
          aria-label="搜索规则"
          @keyup.enter="search"
        />
        <el-select v-model="query.category" clearable placeholder="全部分类"
          ><el-option v-for="c in Api.categories" :key="c.code" :value="c.code" :label="c.label"
        /></el-select>
        <el-select v-model="query.status" clearable placeholder="全部状态"
          ><el-option v-for="(s, i) in Api.states" :key="i" :value="i" :label="s"
        /></el-select>
        <el-button @click="search">查询</el-button><el-button @click="reset">重置</el-button>
        <el-button
          v-hasPermi="['hrm:payroll:rule:maintain']"
          :loading="initializing"
          @click="initialize"
          >登记待决口径</el-button
        >
        <el-button v-hasPermi="['hrm:payroll:rule:maintain']" type="primary" @click="newRule"
          >补充规则</el-button
        >
      </div>
      <el-table v-loading="loading" :data="rows" border data-testid="rule-table">
        <el-table-column prop="code" label="规则编号" min-width="140" />
        <el-table-column prop="title" label="规则名称" min-width="210" show-overflow-tooltip />
        <el-table-column label="版本" width="80"
          ><template #default="{ row }">V{{ row.ruleVersion }}</template></el-table-column
        >
        <el-table-column label="状态" width="120"
          ><template #default="{ row }"
            ><el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'">{{
              Api.states[row.status]
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="分类" width="120"
          ><template #default="{ row }">{{ category(row.category) }}</template></el-table-column
        >
        <el-table-column prop="ownerName" label="负责人" min-width="130" />
        <el-table-column prop="scopeCode" label="范围编号" min-width="135" />
        <el-table-column label="生效期间" min-width="205"
          ><template #default="{ row }"
            >{{ row.effectiveFrom || '待确认'
            }}{{ row.effectiveFrom ? ' 至 ' + (row.effectiveTo || '持续有效') : '' }}</template
          ></el-table-column
        >
        <el-table-column label="参数 / 样例" width="110"
          ><template #default="{ row }"
            >{{ row.parameterCount }} / {{ row.caseCount }}</template
          ></el-table-column
        >
        <el-table-column label="操作" min-width="250"
          ><template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
            <el-button
              v-if="row.status === 0"
              v-hasPermi="['hrm:payroll:rule:maintain']"
              link
              type="primary"
              @click="edit(row.id)"
              >编辑</el-button
            >
            <el-button
              v-if="row.status === 0"
              v-hasPermi="['hrm:payroll:rule:review']"
              link
              type="success"
              @click="openReview(row, 'confirm')"
              >确认</el-button
            >
            <el-button
              v-if="row.status === 1"
              v-hasPermi="['hrm:payroll:rule:review']"
              link
              type="danger"
              @click="openReview(row, 'retire')"
              >停用</el-button
            >
            <el-button
              v-hasPermi="['hrm:payroll:rule:maintain']"
              link
              type="primary"
              @click="newVersion(row)"
              >另建版本</el-button
            >
          </template></el-table-column
        >
      </el-table>
      <Pagination
        v-model:page="query.pageNo"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="load"
      />
      <el-empty
        v-if="!loading && !rows.length"
        description="暂无规则口径，可先登记 PRD 的 15 个待决项，再由业务负责人补齐"
      />
    </ContentWrap>
    <RuleEditor v-model="editorVisible" :rule="editing" @saved="load" />
    <el-dialog
      v-model="reviewVisible"
      :title="reviewAction === 'confirm' ? '确认业务口径' : '停用业务口径'"
      width="min(560px, calc(100% - 24px))"
      :close-on-click-modal="false"
    >
      <p>{{ reviewTarget?.code }} · V{{ reviewTarget?.ruleVersion }} · {{ reviewTarget?.title }}</p>
      <el-alert
        :title="
          reviewAction === 'confirm'
            ? '请核实负责人、范围、生效期间、制度出处及样例。相同编号和范围的已确认期间不得重叠。'
            : '停用后保留原版本与评审记录，历史证据不删除。'
        "
        type="warning"
        :closable="false"
      />
      <el-form label-position="top" class="mt-4"
        ><el-form-item label="评审依据 *"
          ><el-input
            v-model="reviewEvidence"
            type="textarea"
            :rows="4"
            maxlength="2000" /></el-form-item
      ></el-form>
      <el-alert v-if="reviewError" :title="reviewError" type="error" :closable="false" />
      <template #footer
        ><el-button @click="reviewVisible = false">取消</el-button
        ><el-button type="primary" :loading="reviewing" @click="review"
          >提交评审</el-button
        ></template
      >
    </el-dialog>
    <el-drawer v-model="detailVisible" title="规则口径、业务样例与评审记录" size="min(960px, 100%)">
      <div v-loading="detailLoading"
        ><el-alert v-if="detailError" :title="detailError" type="error" :closable="false" />
        <template v-if="detail">
          <div class="detail-heading"
            ><h3>{{ detail.code }} · V{{ detail.ruleVersion }}</h3
            ><el-tag>{{ Api.states[detail.status ?? 0] }}</el-tag></div
          ><h3>{{ detail.title }}</h3>
          <p
            >负责人：{{ detail.ownerName || '待确认' }} · 关联问题：{{
              detail.questionCode || '业务补充'
            }}</p
          >
          <p>范围：{{ detail.scopeCode || '待确认' }} · {{ detail.applicableScope || '待核实' }}</p>
          <p
            >生效期间：{{ detail.effectiveFrom || '待确认'
            }}{{ detail.effectiveFrom ? ' 至 ' + (detail.effectiveTo || '持续有效') : '' }}</p
          >
          <h3>完整规则口径</h3
          ><p class="multiline">{{ detail.definition || '待业务负责人填写' }}</p>
          <h3>制度 / 政策出处</h3><p class="multiline">{{ detail.reference || '待核实' }}</p>
          <h3>口径参数</h3
          ><el-table :data="detail.parameters" border
            ><el-table-column prop="key" label="标识" min-width="120" /><el-table-column
              prop="label"
              label="业务名称"
              min-width="130"
            /><el-table-column prop="type" label="类型" width="100" /><el-table-column
              label="值"
              min-width="150"
              ><template #default="{ row }"
                >{{
                  row.value === undefined || row.value === null || row.value === ''
                    ? '待确认'
                    : row.value
                }}
                {{ row.unit || '' }}</template
              ></el-table-column
            ><el-table-column label="小数位" width="100"
              ><template #default="{ row }">{{
                row.type === 'DECIMAL' ? (row.scale ?? '待确认') : '—'
              }}</template></el-table-column
            ></el-table
          >
          <h3>业务输入与预期样例</h3
          ><p class="note">样例由业务评审人确认；本页展示证据，不执行计算。</p>
          <div v-for="(sample, i) in detail.cases" :key="i" class="case"
            ><strong>{{ i + 1 }}. {{ sample.title }}</strong
            ><p>脱敏输入</p><pre>{{ pretty(sample.inputJson) }}</pre
            ><p>预期结果</p><p class="multiline">{{ sample.expectedResult }}</p></div
          >
          <el-empty v-if="!detail.cases.length" description="业务样例待补齐" />
          <h3>评审历史</h3
          ><el-timeline
            ><el-timeline-item v-for="h in history" :key="h.id"
              ><strong>{{ actionName(h.action) }} · 修订 {{ h.toVersion }}</strong
              ><p>{{ h.actorName }} · {{ time(h.createTime) }}</p
              ><p>{{ h.reason }}</p
              ><el-collapse
                ><el-collapse-item title="查看当时的规则与样例">
                  <pre>{{ pretty(h.afterSnapshot) }}</pre>
                </el-collapse-item></el-collapse
              ></el-timeline-item
            ></el-timeline
          >
        </template>
      </div>
    </el-drawer>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import * as Api from '@/api/hrm/payroll/rules'
import type { History } from '@/api/hrm/payroll/requirements'
import RuleEditor from './RuleEditor.vue'
defineOptions({ name: 'HrmPayrollRules' })
const rows = ref<Api.Rule[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  search: '',
  category: undefined as string | undefined,
  status: undefined as number | undefined
})
const message = (e: unknown) => (e instanceof Error ? e.message : String(e))
const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const p = await Api.page(query)
    rows.value = p.list
    total.value = p.total
  } catch (e) {
    error.value = message(e)
  } finally {
    loading.value = false
  }
}
const search = () => {
  query.pageNo = 1
  load()
}
const reset = () => {
  query.search = ''
  query.category = undefined
  query.status = undefined
  search()
}
const initializing = ref(false)
const initialize = async () => {
  initializing.value = true
  try {
    await Api.initialize()
    await load()
  } catch (e) {
    error.value = message(e)
  } finally {
    initializing.value = false
  }
}
const category = (value: string) => Api.categories.find((c) => c.code === value)?.label || value
const editorVisible = ref(false)
const editing = ref<Api.Rule>()
const newRule = () => {
  editing.value = undefined
  editorVisible.value = true
}
const edit = async (id: number) => {
  try {
    editing.value = await Api.get(id)
    editorVisible.value = true
  } catch (e) {
    error.value = message(e)
  }
}
const newVersion = async (row: Api.Rule) => {
  if (!row.id || !row.revision) return
  try {
    const id = await Api.newVersion(row.id, row.revision)
    await load()
    await edit(id)
  } catch (e) {
    error.value = message(e)
  }
}
const reviewVisible = ref(false)
const reviewTarget = ref<Api.Rule>()
const reviewAction = ref('confirm')
const reviewEvidence = ref('')
const reviewError = ref('')
const reviewing = ref(false)
const openReview = (row: Api.Rule, action: string) => {
  reviewTarget.value = row
  reviewAction.value = action
  reviewEvidence.value = ''
  reviewError.value = ''
  reviewVisible.value = true
}
const review = async () => {
  const row = reviewTarget.value
  if (!row?.id || !row.revision || !reviewEvidence.value.trim()) {
    reviewError.value = '请填写评审依据。'
    return
  }
  reviewing.value = true
  try {
    await Api.review({
      id: row.id,
      revision: row.revision,
      action: reviewAction.value,
      evidence: reviewEvidence.value
    })
    reviewVisible.value = false
    await load()
  } catch (e) {
    reviewError.value = message(e)
  } finally {
    reviewing.value = false
  }
}
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<Api.Rule>()
const history = ref<History[]>([])
const showDetail = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = undefined
  detailError.value = ''
  try {
    const [r, h] = await Promise.all([Api.get(id), Api.history(id)])
    detail.value = r
    history.value = h
  } catch (e) {
    detailError.value = message(e)
  } finally {
    detailLoading.value = false
  }
}
const actionNames: Record<string, string> = {
  initialize: '登记待决口径',
  create: '补充草稿',
  update: '修订草稿',
  'new-version': '另建版本',
  confirm: '口径确认',
  retire: '停用'
}
const actionName = (value: string) => actionNames[value] || value
const time = (v: number) => new Date(v).toLocaleString()
const pretty = (v?: string) => {
  try {
    return JSON.stringify(JSON.parse(v || '{}'), null, 2)
  } catch {
    return v
  }
}
onMounted(load)
</script>
<style scoped>
.rules-page {
  min-width: 0;
}
.heading,
.detail-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.heading {
  margin-bottom: 16px;
}
h2 {
  font-size: 22px;
  margin: 0 0 8px;
}
.heading p,
.note {
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 16px 0;
}
.toolbar > .el-input {
  width: 240px;
}
.toolbar > .el-select {
  width: 145px;
}
.toolbar :deep(.el-button + .el-button) {
  margin-left: 0;
}
.case {
  padding: 12px;
  margin: 12px 0;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
}
pre,
.multiline {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.6;
}
pre {
  background: var(--el-fill-color-light);
  padding: 12px;
  font-size: 12px;
}
@media (max-width: 640px) {
  .heading {
    align-items: flex-start;
    flex-direction: column;
  }
  .toolbar > .el-input,
  .toolbar > .el-select {
    width: 100%;
  }
  :deep(.el-pagination) {
    flex-wrap: wrap;
  }
}
</style>
