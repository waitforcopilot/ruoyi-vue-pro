<template>
  <div class="identity-page">
    <ContentWrap
      ><div class="heading"
        ><div
          ><h2>薪酬人员编号映射</h2
          ><p>明确来源编号对应哪位 HRM 人员，保留有效期、版本与主档快照。</p></div
        ><el-button
          v-hasPermi="['hrm:payroll:identity:maintain']"
          type="primary"
          @click="openEditor()"
          >登记映射</el-button
        ></div
      >
      <el-alert
        title="编号命名空间用于区分来源编号，需按真实系统登记。人员快照记录抓取时看到的主档；历史任职、主体及计薪资格仍需另行核定。"
        type="info"
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
      <div class="toolbar"
        ><el-select v-model="query.sourceId" clearable placeholder="全部来源" aria-label="筛选来源"
          ><el-option
            v-for="s in sources"
            :key="s.id"
            :value="s.id"
            :label="s.code + ' · ' + s.name"
        /></el-select>
        <el-input
          v-model="query.namespace"
          placeholder="命名空间（精确）"
          maxlength="64"
          @keyup.enter="search"
        /><el-input
          v-model="query.externalCode"
          placeholder="外部编号（精确）"
          maxlength="128"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" clearable placeholder="全部状态"
          ><el-option v-for="(s, i) in Api.states" :key="i" :value="i" :label="s" /></el-select
        ><el-button @click="search">查询</el-button><el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" border data-testid="identity-table">
        <el-table-column prop="sourceCode" label="来源" width="100" /><el-table-column
          prop="namespace"
          label="命名空间"
          min-width="155"
        /><el-table-column prop="externalCode" label="外部编号" min-width="150" />
        <el-table-column label="版本 / 状态" width="160"
          ><template #default="{ row }"
            >V{{ row.mappingVersion }} ·
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'">{{
              Api.states[row.status]
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="目标人员快照" min-width="210"
          ><template #default="{ row }"
            >{{ row.snapshotName || '未填写姓名'
            }}<small
              >HRM #{{ row.employeeId }} · 工号 {{ row.snapshotJobNumber || '未登记' }}<br />部门
              {{ row.snapshotDeptId ?? '未登记' }}</small
            ></template
          ></el-table-column
        >
        <el-table-column label="有效期" min-width="210"
          ><template #default="{ row }"
            >{{ row.effectiveFrom || '待确认'
            }}{{ row.effectiveFrom ? ' 至 ' + (row.effectiveTo || '未指定结束') : '' }}</template
          ></el-table-column
        >
        <el-table-column prop="ownerName" label="负责人" min-width="125" />
        <el-table-column label="操作" min-width="250"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
            <el-button
              v-if="row.status === 0"
              v-hasPermi="['hrm:payroll:identity:maintain']"
              link
              type="primary"
              @click="openEditor(row.id)"
              >编辑</el-button
            >
            <el-button
              v-if="row.status === 0"
              v-hasPermi="['hrm:payroll:identity:review']"
              link
              type="success"
              @click="openReview(row, 'confirm')"
              >确认</el-button
            >
            <el-button
              v-if="row.status === 1"
              v-hasPermi="['hrm:payroll:identity:review']"
              link
              type="danger"
              @click="openReview(row, 'retire')"
              >停用</el-button
            >
            <el-button
              v-hasPermi="['hrm:payroll:identity:maintain']"
              link
              type="primary"
              @click="copyVersion(row)"
              >另建版本</el-button
            >
          </template></el-table-column
        > </el-table
      ><Pagination
        v-model:page="query.pageNo"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="load"
      />
      <el-empty
        v-if="!loading && !rows.length"
        description="暂无当前人员数据范围内的映射，请明确来源、命名空间和 HRM 人员 ID 后登记。"
      />
    </ContentWrap>
    <ContentWrap
      ><h3>按外部编号和期间核对</h3
      ><p class="note"
        >单个版本须完整覆盖核对期间，不自动拼接版本。CSV
        配置期间日期列时按该行日期核对；未配置时按整个声明期间核对。</p
      >
      <el-form label-position="top" class="lookup-grid"
        ><el-form-item label="核对来源"
          ><el-select v-model="lookup.sourceId"
            ><el-option
              v-for="s in sources"
              :key="s.id"
              :value="s.id"
              :label="s.code + ' · ' + s.name" /></el-select
        ></el-form-item>
        <el-form-item label="核对命名空间"
          ><el-input v-model="lookup.namespace" maxlength="64" /></el-form-item
        ><el-form-item label="核对外部编号"
          ><el-input v-model="lookup.externalCode" maxlength="128"
        /></el-form-item>
        <el-form-item label="核对开始日期"
          ><el-date-picker
            v-model="lookup.start"
            type="date"
            value-format="YYYY-MM-DD" /></el-form-item
        ><el-form-item label="核对结束日期"
          ><el-date-picker v-model="lookup.end" type="date" value-format="YYYY-MM-DD"
        /></el-form-item>
        <el-form-item label=" "
          ><el-button :loading="resolving" @click="resolve">核对编号</el-button></el-form-item
        > </el-form
      ><el-alert v-if="lookupError" :title="lookupError" type="error" :closable="false" />
      <div v-if="match" data-testid="identity-match"
        ><el-alert
          :type="match.employeeId ? 'success' : 'warning'"
          :title="
            match.employeeId
              ? `找到 HRM 人员 #${match.employeeId}，映射 #${match.mappingId} · V${match.mappingVersion}`
              : match.message || '未找到可用映射'
          "
          :closable="false"
        />
        <p v-if="match.employeeId" class="note"
          >人员快照抓取：{{ match.snapshotCapturedAt }}。此结果核对编号身份，不确认计薪资格。</p
        >
      </div>
    </ContentWrap>
    <el-dialog
      v-model="editorVisible"
      :title="form.id ? '维护映射草稿' : '登记人员编号映射'"
      width="min(900px, calc(100% - 24px))"
      :close-on-click-modal="false"
    >
      <el-alert
        title="请核对目标 HRM 人员。保存会刷新人员快照；确认前主档若再次变化，服务端会要求重新核对。"
        type="info"
        :closable="false"
      />
      <el-form label-position="top" class="mt-4"
        ><div class="form-grid">
          <el-form-item label="数据来源 *"
            ><el-select v-model="form.sourceId" :disabled="!!form.id"
              ><el-option
                v-for="s in sources"
                :key="s.id"
                :value="s.id"
                :label="s.code + ' · ' + s.name" /></el-select
          ></el-form-item>
          <el-form-item label="编号命名空间 *"
            ><el-input
              v-model="form.namespace"
              :disabled="!!form.id"
              maxlength="64"
              placeholder="稳定英文编号，例如来源的编号域"
          /></el-form-item>
          <el-form-item label="外部人员编号 *"
            ><el-input
              v-model="form.externalCode"
              :disabled="!!form.id"
              maxlength="128"
              placeholder="区分大小写，保留前导零"
          /></el-form-item>
          <el-form-item label="HRM 人员 ID *"
            ><el-input-number
              v-model="form.employeeId"
              :min="1"
              :max="Number.MAX_SAFE_INTEGER"
              :precision="0"
              controls-position="right"
              @change="clearSelected"
          /></el-form-item>
          <el-form-item label="映射负责人（确认必填）"
            ><el-input v-model="form.ownerName" maxlength="120"
          /></el-form-item>
          <el-form-item label="生效开始（确认必填）"
            ><el-date-picker v-model="form.effectiveFrom" type="date" value-format="YYYY-MM-DD"
          /></el-form-item>
          <el-form-item label="生效结束（可选）"
            ><el-date-picker v-model="form.effectiveTo" type="date" value-format="YYYY-MM-DD"
          /></el-form-item> </div
        ><el-button :loading="checkingPerson" @click="checkPerson">核对 HRM 人员</el-button>
        <div v-if="selected" class="person-preview" data-testid="selected-person"
          ><strong>{{ selected.snapshotName }}</strong
          ><p
            >HRM #{{ selected.employeeId }} · 工号 {{ selected.snapshotJobNumber || '未登记' }} ·
            部门 {{ selected.snapshotDeptId ?? '未登记' }} · 后台用户
            {{ selected.snapshotUserId ?? '未绑定' }}</p
          >
          <p class="note"
            >入职记录：{{ selected.snapshotEntryTime || '未登记' }} · 离职记录：{{
              selected.snapshotLeaveTime || '未登记'
            }}</p
          ></div
        >
        <el-form-item label="来源或核对依据（确认必填）"
          ><el-input v-model="form.reference" type="textarea" :rows="3" maxlength="2000"
        /></el-form-item> </el-form
      ><el-alert v-if="editorError" :title="editorError" type="error" :closable="false" />
      <template #footer
        ><el-button @click="editorVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template
      >
    </el-dialog>
    <el-dialog
      v-model="reviewVisible"
      :title="reviewAction === 'confirm' ? '确认人员编号映射' : '停用人员编号映射'"
      width="min(560px, calc(100% - 24px))"
      :close-on-click-modal="false"
    >
      <p
        >{{ reviewTarget?.externalCode }} · V{{ reviewTarget?.mappingVersion }} ·
        {{ reviewTarget?.snapshotName }}</p
      ><el-alert
        title="核实来源依据、有效期及人员快照。确认版本不可修改，同一编号已确认的有效期不能重叠。"
        type="warning"
        :closable="false"
      />
      <el-form label-position="top" class="mt-3"
        ><el-form-item label="评审依据 *"
          ><el-input v-model="evidence" type="textarea" :rows="3" maxlength="2000" /></el-form-item
      ></el-form>
      <el-alert v-if="reviewError" :title="reviewError" type="error" :closable="false" /><template
        #footer
        ><el-button @click="reviewVisible = false">取消</el-button
        ><el-button type="primary" :loading="reviewing" @click="review"
          >提交评审</el-button
        ></template
      >
    </el-dialog>
    <el-drawer v-model="detailVisible" title="编号映射、主档快照与评审记录" size="min(920px, 100%)">
      <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" /><template
        v-if="detail"
      >
        <h3
          >{{ detail.sourceCode }} · {{ detail.namespace }} · {{ detail.externalCode }} · V{{
            detail.mappingVersion
          }}</h3
        ><el-tag>{{ Api.states[detail.status ?? 0] }}</el-tag>
        <p
          >有效期：{{ detail.effectiveFrom || '待确认' }} 至
          {{ detail.effectiveTo || '未指定结束' }}</p
        ><p>负责人：{{ detail.ownerName || '待填写' }}</p
        ><h3>主档抓取快照</h3>
        <el-descriptions :column="1" border
          ><el-descriptions-item label="人员"
            >{{ detail.snapshotName }} · HRM #{{ detail.employeeId }}</el-descriptions-item
          >
          <el-descriptions-item label="工号">{{
            detail.snapshotJobNumber || '未登记'
          }}</el-descriptions-item
          ><el-descriptions-item label="部门编号">{{
            detail.snapshotDeptId ?? '未登记'
          }}</el-descriptions-item
          ><el-descriptions-item label="后台用户编号">{{
            detail.snapshotUserId ?? '未绑定'
          }}</el-descriptions-item>
          <el-descriptions-item label="主档入职/离职记录"
            >{{ detail.snapshotEntryTime || '未登记' }} /
            {{ detail.snapshotLeaveTime || '未登记' }}</el-descriptions-item
          ><el-descriptions-item label="主档抓取时间">{{
            detail.snapshotCapturedAt
          }}</el-descriptions-item></el-descriptions
        >
        <p class="note"
          >此快照保存当时可见的主档，不推定所声明生效日期的历史任职或工资资格。确认后不会随当前主档变化而覆盖。</p
        ><h3>来源依据</h3><p class="multiline">{{ detail.reference || '待填写' }}</p
        ><p>评审人：{{ detail.reviewedByName || '待评审' }} · {{ detail.reviewedTime || '' }}</p
        ><p class="multiline">{{ detail.evidence }}</p> <h3>评审历史</h3
        ><p class="note"
          >人员范围受限时，历史仅显示操作元数据；前后个人快照及自由填写理由按权限隐藏。</p
        >
        <el-collapse
          ><el-collapse-item
            v-for="h in histories"
            :key="h.id"
            :title="`${h.action} · ${h.actorName} · ${new Date(h.createTime).toLocaleString('zh-CN')}`"
            :name="h.id"
            ><p>{{ h.reason || '历史快照按人员范围隐藏' }}</p
            ><pre v-if="h.afterSnapshot">{{ pretty(h.afterSnapshot) }}</pre
            ><pre v-if="h.beforeSnapshot">{{ pretty(h.beforeSnapshot) }}</pre>
          </el-collapse-item></el-collapse
        >
      </template>
    </el-drawer>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import * as Api from '@/api/hrm/payroll/identity'
import type { History } from '@/api/hrm/payroll/requirements'
defineOptions({ name: 'HrmPayrollIdentity' })
const sources = ref<Api.Source[]>([]),
  rows = ref<Api.Mapping[]>([]),
  total = ref(0),
  loading = ref(false),
  error = ref('')
const query = reactive<{
  pageNo: number
  pageSize: number
  sourceId?: number
  namespace?: string
  externalCode?: string
  status?: number
}>({ pageNo: 1, pageSize: 10 })
const editorVisible = ref(false),
  form = ref<Api.Mapping>({ namespace: '', externalCode: '' }),
  selected = ref<Api.Mapping>(),
  editorError = ref(''),
  saving = ref(false),
  checkingPerson = ref(false)
const reviewVisible = ref(false),
  reviewTarget = ref<Api.Mapping>(),
  reviewAction = ref('confirm'),
  evidence = ref(''),
  reviewError = ref(''),
  reviewing = ref(false)
const detailVisible = ref(false),
  detail = ref<Api.Mapping>(),
  histories = ref<History[]>([]),
  detailError = ref('')
const lookup = reactive({
    sourceId: undefined as number | undefined,
    namespace: '',
    externalCode: '',
    start: '',
    end: ''
  }),
  match = ref<Api.Match>(),
  resolving = ref(false),
  lookupError = ref('')
const message = (e: unknown) =>
  e instanceof Error ? e.message : '操作未成功，请核对页面提示或刷新后重试。'
const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const result = await Api.page({
      ...query,
      namespace: query.namespace || undefined,
      externalCode: query.externalCode || undefined
    })
    rows.value = result.list
    total.value = result.total
  } catch (e) {
    rows.value = []
    total.value = 0
    error.value = message(e)
  } finally {
    loading.value = false
  }
}
const search = () => {
  query.pageNo = 1
  return load()
}
const reset = () => {
  query.sourceId = undefined
  query.namespace = undefined
  query.externalCode = undefined
  query.status = undefined
  return search()
}
const openEditor = async (id?: number) => {
  editorError.value = ''
  selected.value = undefined
  try {
    form.value = id ? await Api.get(id) : { namespace: '', externalCode: '' }
    editorVisible.value = true
  } catch (e) {
    error.value = message(e)
  }
}
const clearSelected = () => {
  selected.value = undefined
  form.value.employeeFingerprint = undefined
}
const checkPerson = async () => {
  editorError.value = ''
  if (!form.value.employeeId) {
    editorError.value = '请填写 HRM 人员 ID。'
    return
  }
  checkingPerson.value = true
  selected.value = undefined
  try {
    selected.value = await Api.employee(form.value.employeeId)
    form.value.employeeFingerprint = selected.value.employeeFingerprint
  } catch (e) {
    editorError.value = message(e)
  } finally {
    checkingPerson.value = false
  }
}
const save = async () => {
  editorError.value = ''
  if (
    !form.value.sourceId ||
    !form.value.namespace ||
    !form.value.externalCode.trim() ||
    !selected.value ||
    selected.value.employeeId !== form.value.employeeId
  ) {
    editorError.value = '请填写来源、命名空间、外部编号，并核对 HRM 人员。'
    return
  }
  saving.value = true
  try {
    if (form.value.id) await Api.update(form.value)
    else await Api.create(form.value)
    editorVisible.value = false
    await load()
  } catch (e) {
    editorError.value = message(e)
  } finally {
    saving.value = false
  }
}
const copyVersion = async (row: Api.Mapping) => {
  try {
    const id = await Api.newVersion(row.id!, row.revision!)
    await load()
    await openEditor(id)
  } catch (e) {
    error.value = message(e)
  }
}
const openReview = (row: Api.Mapping, action: string) => {
  reviewTarget.value = row
  reviewAction.value = action
  evidence.value = ''
  reviewError.value = ''
  reviewVisible.value = true
}
const review = async () => {
  reviewError.value = ''
  if (!evidence.value.trim()) {
    reviewError.value = '请填写评审依据。'
    return
  }
  reviewing.value = true
  try {
    await Api.review({
      id: reviewTarget.value!.id!,
      revision: reviewTarget.value!.revision!,
      action: reviewAction.value,
      evidence: evidence.value
    })
    reviewVisible.value = false
    await load()
  } catch (e) {
    reviewError.value = message(e)
  } finally {
    reviewing.value = false
  }
}
const showDetail = async (id: number) => {
  detailVisible.value = true
  detail.value = undefined
  histories.value = []
  detailError.value = ''
  try {
    const result = await Promise.all([Api.get(id), Api.history(id)])
    detail.value = result[0]
    histories.value = result[1]
  } catch (e) {
    detailError.value = message(e)
  }
}
const resolve = async () => {
  lookupError.value = ''
  match.value = undefined
  if (
    !lookup.sourceId ||
    !lookup.namespace ||
    !lookup.externalCode ||
    !lookup.start ||
    !lookup.end
  ) {
    lookupError.value = '请填写来源、命名空间、外部编号和起止日期。'
    return
  }
  resolving.value = true
  try {
    match.value = await Api.resolve(lookup.sourceId, lookup.namespace, lookup)
  } catch (e) {
    lookupError.value = message(e)
  } finally {
    resolving.value = false
  }
}
const pretty = (s: string) => {
  try {
    return JSON.stringify(JSON.parse(s), null, 2)
  } catch {
    return s
  }
}
onMounted(async () => {
  try {
    sources.value = await Api.sources()
  } catch (e) {
    error.value = message(e)
    return
  }
  await load()
})
</script>
<style scoped>
.heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.heading h2 {
  margin: 0;
}
.heading p,
.note {
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 18px 0;
}
.toolbar .el-input,
.toolbar .el-select {
  width: 210px;
}
.lookup-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 16px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}
small {
  display: block;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
.person-preview {
  margin: 16px 0;
  padding: 14px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}
.multiline {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
pre {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 12px;
}
.el-date-editor.el-input,
.el-input-number {
  width: 100%;
}
@media (max-width: 640px) {
  .form-grid,
  .lookup-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .toolbar .el-input,
  .toolbar .el-select {
    width: 100%;
  }
}
</style>
