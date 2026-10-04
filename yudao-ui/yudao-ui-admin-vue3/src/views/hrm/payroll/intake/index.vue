<template>
  <div class="intake-page">
    <ContentWrap>
      <div class="page-heading"
        ><div><h2>薪酬数据接入准备</h2><p>先确认来源字段，再上传样本预检，逐行核对问题。</p></div
        ><el-tag type="warning">薪酬核算口径待确认</el-tag></div
      >
      <el-alert
        title="本页保存字段契约与预检批次。格式通过后，仍需确认人员映射、数据完整性及薪酬规则。"
        type="info"
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
      <el-tabs v-model="tab" @tab-change="tabChanged">
        <el-tab-pane label="来源契约" name="contracts">
          <div class="toolbar">
            <el-input
              v-model="query.search"
              clearable
              placeholder="搜索契约名称或来源编号"
              aria-label="搜索契约"
              @keyup.enter="search"
            />
            <el-select v-model="query.status" clearable placeholder="全部状态" aria-label="契约状态"
              ><el-option v-for="(s, i) in Api.states" :key="i" :value="i" :label="s"
            /></el-select>
            <el-button @click="search">查询</el-button><el-button @click="reset">重置</el-button>
            <el-button
              v-hasPermi="['hrm:payroll:intake:contract']"
              type="primary"
              @click="newContract"
              >新建契约</el-button
            >
          </div>
          <el-table v-loading="loading" :data="rows" border data-testid="contracts-table">
            <el-table-column prop="sourceCode" label="来源" width="110" />
            <el-table-column prop="title" label="契约名称" min-width="210" show-overflow-tooltip />
            <el-table-column label="版本" width="85"
              ><template #default="{ row }">V{{ row.contractVersion }}</template></el-table-column
            >
            <el-table-column label="状态" width="105"
              ><template #default="{ row }"
                ><el-tag
                  :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'"
                  >{{ Api.states[row.status] }}</el-tag
                ></template
              ></el-table-column
            >
            <el-table-column prop="fieldCount" label="字段数" width="80" />
            <el-table-column prop="ownerName" label="来源负责人" min-width="140" />
            <el-table-column
              prop="actualSystem"
              label="实际系统"
              min-width="170"
              show-overflow-tooltip
            />
            <el-table-column label="操作" min-width="320"
              ><template #default="{ row }">
                <el-button link type="primary" @click="showContract(row.id)">详情</el-button>
                <el-button
                  v-if="row.status === 0"
                  v-hasPermi="['hrm:payroll:intake:contract']"
                  link
                  type="primary"
                  @click="editContract(row.id, false)"
                  >编辑</el-button
                >
                <el-button
                  v-if="row.status === 0"
                  v-hasPermi="['hrm:payroll:intake:confirm']"
                  link
                  type="success"
                  @click="openReview(row, 'confirm')"
                  >确认</el-button
                >
                <el-button
                  v-if="row.status === 1"
                  v-hasPermi="['hrm:payroll:intake:confirm']"
                  link
                  type="danger"
                  @click="openReview(row, 'retire')"
                  >停用</el-button
                >
                <el-button
                  v-hasPermi="['hrm:payroll:intake:contract']"
                  link
                  type="primary"
                  @click="editContract(row.id, true)"
                  >另建版本</el-button
                >
                <el-button
                  v-if="row.status === 1"
                  v-hasPermi="['hrm:payroll:intake:preview']"
                  link
                  type="primary"
                  @click="useContract(row.id)"
                  >预检</el-button
                >
              </template></el-table-column
            >
          </el-table>
          <Pagination
            v-model:page="query.pageNo"
            v-model:limit="query.pageSize"
            :total="total"
            @pagination="loadContracts"
          />
          <el-empty
            v-if="!loading && !rows.length"
            description="暂无字段契约，请先在需求征集登记来源，再新建契约"
          />
        </el-tab-pane>
        <el-tab-pane v-if="canPreview" label="上传预检" name="preview">
          <el-form label-position="top" class="preview-form">
            <div class="preview-grid">
              <el-form-item label="已确认契约 *"
                ><el-select
                  v-model="selectedId"
                  filterable
                  remote
                  :remote-method="findConfirmed"
                  placeholder="输入名称或来源编号检索"
                  @change="contractChanged"
                  @visible-change="selectorOpened"
                  ><el-option
                    v-for="c in confirmedOptions"
                    :key="c.id"
                    :value="c.id!"
                    :label="
                      c.sourceCode + ' · V' + c.contractVersion + ' · ' + c.title
                    " /></el-select
              ></el-form-item>
              <el-form-item label="本次声明范围 / 主体 *"
                ><el-input v-model="declaredScope" maxlength="120" placeholder="由提交人核实并填写"
              /></el-form-item>
              <el-form-item label="本次声明期间 *"
                ><el-date-picker
                  v-model="period"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
              /></el-form-item>
              <el-form-item label="UTF-8 CSV 文件 *"
                ><input
                  type="file"
                  accept=".csv,text/csv"
                  aria-label="选择 CSV 文件"
                  @change="fileChanged"
              /></el-form-item>
            </div>
            <div class="preview-actions"
              ><el-button :disabled="!selectedContract" @click="downloadTemplate"
                >下载所选版本模板</el-button
              ><el-button
                type="primary"
                :disabled="!selectedContract"
                :loading="previewing"
                @click="runPreview"
                >上传并预检</el-button
              ></div
            >
          </el-form>
          <el-alert v-if="selectedError" :title="selectedError" type="error" :closable="false" />
          <p class="hint"
            >保留模板前两行，从第 3 行填入数据。每批最多 1 MiB、500 行、32
            列；支持带引号的逗号和换行。原始文件不保留，结果中保存规范化后的有效值。</p
          >
          <p v-if="selectedContract" class="hint"
            >人员核对：{{
              selectedContract.schema.employeeField
                ? '按 HRM 工号字段 ' + selectedContract.schema.employeeField
                : '未启用，人员映射待核定'
            }}；期间列核对：{{ selectedContract.schema.periodField || '未启用' }}；主体列核对：{{
              selectedContract.schema.subjectField || '未启用'
            }}。</p
          >
          <BatchResult :batch="result" />
        </el-tab-pane>
        <el-tab-pane label="我的预检批次" name="batches">
          <div class="toolbar"
            ><el-select v-model="batchQuery.status" clearable placeholder="全部结果"
              ><el-option :value="0" label="格式通过" /><el-option
                :value="1"
                label="发现问题" /></el-select
            ><el-button @click="searchBatches">查询批次</el-button></div
          >
          <p class="hint">仅显示本人提交的批次，历史批次按提交时的契约与结果查看。</p>
          <el-table v-loading="batchLoading" :data="batchRows" border data-testid="batches-table">
            <el-table-column prop="id" label="批次" width="80" /><el-table-column
              prop="fileName"
              label="文件"
              min-width="200"
            />
            <el-table-column label="来源版本" min-width="140"
              ><template #default="{ row }"
                >{{ row.sourceCode }} · V{{ row.contractVersion }}</template
              ></el-table-column
            >
            <el-table-column prop="declaredScope" label="声明范围" min-width="160" />
            <el-table-column label="数据 / 无问题 / 问题数" width="195"
              ><template #default="{ row }"
                >{{ row.rowCount }} / {{ row.validCount }} / {{ row.errorCount }}</template
              ></el-table-column
            >
            <el-table-column label="结果" width="120"
              ><template #default="{ row }"
                ><el-tag :type="row.status === 0 ? 'success' : 'danger'">{{
                  row.status === 0 ? '格式通过' : '发现问题'
                }}</el-tag></template
              ></el-table-column
            >
            <el-table-column label="操作" width="100"
              ><template #default="{ row }"
                ><el-button link type="primary" @click="showBatch(row.id)"
                  >查看批次</el-button
                ></template
              ></el-table-column
            >
          </el-table>
          <Pagination
            v-model:page="batchQuery.pageNo"
            v-model:limit="batchQuery.pageSize"
            :total="batchTotal"
            @pagination="loadBatches"
          />
        </el-tab-pane>
      </el-tabs>
    </ContentWrap>
    <ContractEditor
      v-model="editorVisible"
      :contract="editing"
      :sources="sourceOptions"
      @saved="saved"
    />
    <el-dialog
      v-model="reviewVisible"
      :title="reviewAction === 'confirm' ? '确认来源契约' : '停用来源契约'"
      width="min(560px, calc(100% - 24px))"
      :close-on-click-modal="false"
    >
      <p>{{ reviewTarget?.title }} · V{{ reviewTarget?.contractVersion }}</p
      ><el-alert
        :title="
          reviewAction === 'confirm'
            ? '确认后可预检该版本文件。确认不代表来源完整或薪酬规则已核定。'
            : '停用后禁止新的预检，原批次保留。'
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
      <el-alert v-if="reviewError" :title="reviewError" type="error" :closable="false" /><template
        #footer
        ><el-button @click="reviewVisible = false">取消</el-button
        ><el-button type="primary" :loading="reviewing" @click="submitReview"
          >提交评审</el-button
        ></template
      >
    </el-dialog>
    <el-drawer v-model="detailVisible" title="来源契约与评审历史" size="min(880px, 100%)">
      <div v-loading="detailLoading"
        ><el-alert v-if="detailError" :title="detailError" type="error" :closable="false" />
        <template v-if="detail"
          ><h3>{{ detail.title }} · V{{ detail.contractVersion }}</h3
          ><el-tag>{{ Api.states[detail.status ?? 0] }}</el-tag
          ><p>{{ detail.sourceCode }} · {{ detail.sourceName }}</p
          ><p
            >实际系统：{{ detail.actualSystem || '待核实' }} · 负责人：{{
              detail.ownerName || '待确认'
            }}</p
          ><p>适用范围：{{ detail.applicableScope || '待核实' }}</p>
          <el-table :data="detail.schema.fields" border
            ><el-table-column prop="key" label="列名" min-width="120" /><el-table-column
              prop="label"
              label="业务名称"
              min-width="130"
            /><el-table-column prop="type" label="类型" width="100" /><el-table-column
              label="约束"
              min-width="175"
              ><template #default="{ row }"
                >{{ row.required ? '必填' : '可空' }} {{ row.unit || '' }}
                {{
                  row.type === 'DECIMAL'
                    ? '小数位：' + (row.scale ?? '待确认')
                    : row.type === 'TEXT'
                      ? '长度：' + (row.maxLength ?? '待确认')
                      : ''
                }}</template
              ></el-table-column
            ></el-table
          >
          <p>唯一键：{{ detail.schema.keyFields.join(' + ') }}</p
          ><p
            >人员核对：{{ detail.schema.employeeField || '未启用' }} · 期间列：{{
              detail.schema.periodField || '未启用'
            }}
            · 主体列：{{ detail.schema.subjectField || '未启用' }}</p
          >
          <el-button v-if="detail.status === 1" @click="downloadContractTemplate(detail)"
            >下载模板</el-button
          ><h3 class="mt-6">评审历史</h3>
          <el-timeline
            ><el-timeline-item v-for="h in contractHistory" :key="h.id"
              ><strong>{{ actionName(h.action) }} · 修订 {{ h.toVersion }}</strong
              ><p>{{ h.actorName }} · {{ time(h.createTime) }}</p
              ><p>{{ h.reason }}</p
              ><el-collapse
                ><el-collapse-item title="查看本次契约快照">
                  <pre>{{ pretty(h.afterSnapshot) }}</pre>
                </el-collapse-item></el-collapse
              ></el-timeline-item
            ></el-timeline
          >
        </template>
      </div>
    </el-drawer>
    <el-drawer v-model="batchVisible" title="预检批次快照" size="min(1100px, 100%)"
      ><div v-loading="batchDetailLoading"
        ><el-alert
          v-if="batchDetailError"
          :title="batchDetailError"
          type="error"
          :closable="false" /><BatchResult :batch="batchDetail" /></div
    ></el-drawer>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import * as Api from '@/api/hrm/payroll/intake'
import type { History } from '@/api/hrm/payroll/requirements'
import { checkPermi } from '@/utils/permission'
import ContractEditor from './ContractEditor.vue'
import BatchResult from './BatchResult.vue'
defineOptions({ name: 'HrmPayrollIntake' })
const tab = ref('contracts')
const canPreview = computed(() => checkPermi(['hrm:payroll:intake:preview']))
const error = ref('')
const loading = ref(false)
const rows = ref<Api.Contract[]>([])
const total = ref(0)
const sourceOptions = ref<Api.SourceOption[]>([])
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  search: '',
  status: undefined as number | undefined
})
const message = (e: unknown) => (e instanceof Error ? e.message : String(e))
const loadContracts = async () => {
  loading.value = true
  error.value = ''
  try {
    const p = await Api.contracts(query)
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
  loadContracts()
}
const reset = () => {
  query.search = ''
  query.status = undefined
  search()
}
const editorVisible = ref(false)
const editing = ref<Api.Contract>()
const newContract = () => {
  editing.value = undefined
  editorVisible.value = true
}
const editContract = async (id: number, copy: boolean) => {
  try {
    const c = await Api.getContract(id)
    if (copy) {
      c.id = undefined
      c.revision = undefined
    }
    editing.value = c
    editorVisible.value = true
  } catch (e) {
    error.value = message(e)
  }
}
const saved = async () => {
  await loadContracts()
  await findConfirmed('')
}
const reviewVisible = ref(false)
const reviewTarget = ref<Api.Contract>()
const reviewAction = ref('confirm')
const reviewEvidence = ref('')
const reviewError = ref('')
const reviewing = ref(false)
const openReview = (c: Api.Contract, action: string) => {
  reviewTarget.value = c
  reviewAction.value = action
  reviewEvidence.value = ''
  reviewError.value = ''
  reviewVisible.value = true
}
const submitReview = async () => {
  const c = reviewTarget.value
  if (!c?.id || !c.revision || !reviewEvidence.value.trim()) {
    reviewError.value = '请填写评审依据。'
    return
  }
  reviewing.value = true
  try {
    await Api.reviewContract({
      id: c.id,
      revision: c.revision,
      action: reviewAction.value,
      evidence: reviewEvidence.value
    })
    reviewVisible.value = false
    await saved()
  } catch (e) {
    reviewError.value = message(e)
  } finally {
    reviewing.value = false
  }
}
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<Api.Contract>()
const contractHistory = ref<History[]>([])
const showContract = async (id: number) => {
  detailVisible.value = true
  detail.value = undefined
  detailError.value = ''
  detailLoading.value = true
  try {
    const [c, h] = await Promise.all([Api.getContract(id), Api.history(id)])
    detail.value = c
    contractHistory.value = h
  } catch (e) {
    detailError.value = message(e)
  } finally {
    detailLoading.value = false
  }
}
const actionLabels: Record<string, string> = {
  create: '登记草稿',
  update: '修改草稿',
  confirm: '确认',
  retire: '停用'
}
const actionName = (a: string) => actionLabels[a] || a
const time = (v: number) => new Date(v).toLocaleString()
const pretty = (v?: string) => {
  try {
    return JSON.stringify(JSON.parse(v || '{}'), null, 2)
  } catch {
    return v
  }
}
const confirmedOptions = ref<Api.Contract[]>([])
const selectedId = ref<number>()
const selectedContract = ref<Api.Contract>()
const selectedError = ref('')
const declaredScope = ref('')
const period = ref<string[]>([])
const file = ref<File>()
const result = ref<Api.BatchDetail>()
const previewing = ref(false)
let lookupSequence = 0
const findConfirmed = async (search: string) => {
  const sequence = ++lookupSequence
  try {
    const p = await Api.contracts({ pageNo: 1, pageSize: 20, status: 1, search })
    if (sequence === lookupSequence) {
      confirmedOptions.value = p.list
      if (selectedContract.value && !p.list.some((c) => c.id === selectedId.value))
        confirmedOptions.value.unshift(selectedContract.value)
    }
  } catch (e) {
    error.value = message(e)
  }
}
const selectorOpened = (v: boolean) => {
  if (v) findConfirmed('')
}
let selectionSequence = 0
const contractChanged = async () => {
  const sequence = ++selectionSequence
  result.value = undefined
  selectedError.value = ''
  selectedContract.value = undefined
  if (!selectedId.value) return
  try {
    const c = await Api.getContract(selectedId.value)
    if (sequence !== selectionSequence) return
    if (c.status !== 1) {
      selectedError.value = '所选契约已停用或尚未确认，请重新选择。'
      return
    }
    selectedContract.value = c
  } catch (e) {
    if (sequence === selectionSequence) selectedError.value = message(e)
  }
}
const useContract = async (id: number) => {
  selectedId.value = id
  await contractChanged()
  if (selectedContract.value) {
    confirmedOptions.value = [selectedContract.value]
    tab.value = 'preview'
  }
}
const fileChanged = (e: Event) => {
  file.value = (e.target as HTMLInputElement).files?.[0]
  result.value = undefined
  selectedError.value = ''
  if (file.value && file.value.size > 1024 * 1024)
    selectedError.value = '文件超过 1 MiB 上限，请拆分后上传。'
}
const downloadContractTemplate = async (c: Api.Contract) => {
  if (!c.id) return
  try {
    const blob = await Api.template(c.id)
    const href = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = href
    a.download = `payroll-${c.sourceCode}-v${c.contractVersion}.csv`
    a.click()
    setTimeout(() => URL.revokeObjectURL(href), 1000)
  } catch (e) {
    error.value = message(e)
  }
}
const downloadTemplate = () => {
  if (selectedContract.value) downloadContractTemplate(selectedContract.value)
}
const runPreview = async () => {
  selectedError.value = ''
  if (
    !selectedId.value ||
    !selectedContract.value ||
    !declaredScope.value.trim() ||
    period.value?.length !== 2 ||
    !file.value
  ) {
    selectedError.value = '请填写契约、声明范围、期间并选择 CSV 文件。'
    return
  }
  if (file.value.size > 1024 * 1024) {
    selectedError.value = '文件超过 1 MiB 上限，请拆分后上传。'
    return
  }
  previewing.value = true
  result.value = undefined
  try {
    const data = new FormData()
    data.append('contractId', String(selectedId.value))
    data.append('declaredScope', declaredScope.value)
    data.append('periodStart', period.value[0])
    data.append('periodEnd', period.value[1])
    data.append('file', file.value)
    const id = await Api.preview(data)
    result.value = await Api.batch(id)
  } catch (e) {
    selectedError.value = message(e)
  } finally {
    previewing.value = false
  }
}
const batchRows = ref<Api.Batch[]>([])
const batchLoading = ref(false)
const batchTotal = ref(0)
const batchQuery = reactive({ pageNo: 1, pageSize: 10, status: undefined as number | undefined })
const loadBatches = async () => {
  batchLoading.value = true
  error.value = ''
  try {
    const p = await Api.batches(batchQuery)
    batchRows.value = p.list
    batchTotal.value = p.total
  } catch (e) {
    error.value = message(e)
  } finally {
    batchLoading.value = false
  }
}
const searchBatches = () => {
  batchQuery.pageNo = 1
  loadBatches()
}
const tabChanged = (name: string | number) => {
  if (name === 'batches') loadBatches()
  if (name === 'preview') findConfirmed('')
}
const batchVisible = ref(false)
const batchDetailLoading = ref(false)
const batchDetailError = ref('')
const batchDetail = ref<Api.BatchDetail>()
const showBatch = async (id: number) => {
  batchVisible.value = true
  batchDetail.value = undefined
  batchDetailError.value = ''
  batchDetailLoading.value = true
  try {
    batchDetail.value = await Api.batch(id)
  } catch (e) {
    batchDetailError.value = message(e)
  } finally {
    batchDetailLoading.value = false
  }
}
onMounted(async () => {
  await loadContracts()
  try {
    sourceOptions.value = await Api.sources()
  } catch (e) {
    error.value = message(e)
  }
})
</script>
<style scoped>
.intake-page {
  min-width: 0;
}
.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
h2 {
  margin: 0 0 8px;
  font-size: 22px;
}
.page-heading p,
.hint {
  margin: 8px 0;
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
  width: 260px;
}
.toolbar > .el-select {
  width: 170px;
}
.toolbar :deep(.el-button + .el-button) {
  margin-left: 0;
}
.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 24px;
}
.preview-form {
  margin: 16px 0;
}
.preview-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.preview-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
.preview-grid :deep(.el-select),
.preview-grid :deep(.el-date-editor) {
  width: 100%;
}
.preview-grid input[type='file'] {
  max-width: 100%;
}
pre {
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
@media (max-width: 640px) {
  .page-heading {
    flex-direction: column;
    align-items: flex-start;
  }
  .preview-grid {
    grid-template-columns: minmax(0, 1fr);
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
