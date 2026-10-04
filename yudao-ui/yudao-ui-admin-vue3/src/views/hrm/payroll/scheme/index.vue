<template>
  <div class="scheme-page">
    <ContentWrap
      ><div class="heading"
        ><div
          ><h2>薪酬方案配置版本</h2
          ><p>复用现有薪资组、计税设置与共用工资项，抓取确认版本并比较历史变化。</p></div
        ><el-button
          v-hasPermi="['hrm:payroll:scheme:maintain']"
          type="primary"
          @click="openEditor()"
          >登记方案版本</el-button
        ></div
      >
      <el-alert
        title="确认保存配置快照，不启用工资计算。当前主档的人员范围、历史任职及计薪资格需另行核定。"
        type="info"
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
      <p class="note">查阅配置版本需同时具备薪资组、工资项和计税规则的查询权限。</p>
      <div class="toolbar"
        ><el-select
          v-model="query.groupId"
          clearable
          filterable
          remote
          :remote-method="findGroups"
          placeholder="按薪资组筛选"
          ><el-option v-for="g in groups" :key="g.id" :value="g.id" :label="g.name + ' · #' + g.id"
        /></el-select>
        <el-input
          v-model="query.search"
          placeholder="搜索方案名称"
          maxlength="160"
          @keyup.enter="search"
        /><el-select v-model="query.status" clearable placeholder="全部状态"
          ><el-option v-for="(s, i) in Api.states" :key="i" :value="i" :label="s" /></el-select
        ><el-button @click="search">查询</el-button><el-button @click="reset">重置</el-button></div
      >
      <el-table :data="rows" v-loading="loading" border data-testid="scheme-table">
        <el-table-column prop="title" label="方案名称" min-width="190" /><el-table-column
          prop="groupName"
          label="薪资组快照"
          min-width="180"
        />
        <el-table-column label="版本 / 状态" width="160"
          ><template #default="{ row }"
            >V{{ row.schemeVersion }} ·
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'">{{
              Api.states[row.status]
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="有效期" min-width="210"
          ><template #default="{ row }"
            >{{ row.effectiveFrom || '待确认'
            }}{{ row.effectiveFrom ? ' 至 ' + (row.effectiveTo || '未指定结束') : '' }}</template
          ></el-table-column
        >
        <el-table-column prop="optionCount" label="薪资项数" width="100" /><el-table-column
          prop="ownerName"
          label="负责人"
          min-width="120"
        />
        <el-table-column label="操作" min-width="270"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="showDetail(row.id)">详情</el-button
            ><el-button
              v-if="row.status === 0"
              v-hasPermi="['hrm:payroll:scheme:maintain']"
              link
              type="primary"
              @click="openEditor(row.id)"
              >编辑</el-button
            >
            <el-button
              v-if="row.status === 0"
              v-hasPermi="['hrm:payroll:scheme:review']"
              link
              type="success"
              @click="openReview(row, 'confirm')"
              >确认</el-button
            ><el-button
              v-if="row.status === 1"
              v-hasPermi="['hrm:payroll:scheme:review']"
              link
              type="danger"
              @click="openReview(row, 'retire')"
              >停用</el-button
            ><el-button
              v-hasPermi="['hrm:payroll:scheme:maintain']"
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
        v-if="!loading && !error && !rows.length"
        description="暂无方案版本。先配置既有薪资组及工资项，再抓取草稿核对。"
      />
    </ContentWrap>
    <ContentWrap
      ><h3>历史配置对比</h3
      ><p class="note"
        >先筛选薪资组，再选择当前列表中的两个版本。对比读取保留的配置快照，不改变版本或当前源配置。</p
      >
      <div class="compare-grid"
        ><el-select v-model="leftId" placeholder="左侧版本" aria-label="左侧版本"
          ><el-option
            v-for="r in rows"
            :key="r.id"
            :value="r.id!"
            :label="'V' + r.schemeVersion + ' · ' + r.title" /></el-select
        ><el-select v-model="rightId" placeholder="右侧版本" aria-label="右侧版本"
          ><el-option
            v-for="r in rows"
            :key="r.id"
            :value="r.id!"
            :label="'V' + r.schemeVersion + ' · ' + r.title" /></el-select
        ><el-button :loading="comparing" @click="compare">比较配置</el-button></div
      >
      <el-alert
        v-if="compareError"
        :title="compareError"
        type="error"
        :closable="false"
        class="mt-3"
      /><div v-if="comparison" data-testid="scheme-comparison"
        ><p
          >V{{ comparison.left.schemeVersion }}（{{
            comparison.left.effectiveFrom || '期间待确认'
          }}） → V{{ comparison.right.schemeVersion }}（{{
            comparison.right.effectiveFrom || '期间待确认'
          }}） · {{ comparison.changes.length }} 项差异</p
        >
        <el-table :data="comparison.changes" border
          ><el-table-column label="变化" width="100"
            ><template #default="{ row }">{{ kinds[row.kind] }}</template></el-table-column
          ><el-table-column prop="label" label="配置项" min-width="235" /><el-table-column
            label="左侧配置"
            min-width="180"
            ><template #default="{ row }">{{
              difference(row.path, row.left)
            }}</template></el-table-column
          ><el-table-column label="右侧配置" min-width="180"
            ><template #default="{ row }">{{
              difference(row.path, row.right)
            }}</template></el-table-column
          ></el-table
        >
        <el-empty v-if="!comparison.changes.length" description="两个版本的配置内容一致"
      /></div>
    </ContentWrap>
    <ContentWrap
      ><h3>按期间核对配置版本</h3
      ><p class="note"
        >单个确认版本须覆盖完整区间，不拼接版本。结果只说明已有配置快照，不确认人员资格或当前核算可执行。</p
      >
      <el-form label-position="top" class="lookup-grid"
        ><el-form-item label="核对薪资组 ID"
          ><el-input-number
            v-model="lookup.groupId"
            :min="1"
            :precision="0"
            controls-position="right" /></el-form-item
        ><el-form-item label="核对开始日期"
          ><el-date-picker
            v-model="lookup.start"
            type="date"
            value-format="YYYY-MM-DD" /></el-form-item
        ><el-form-item label="核对结束日期"
          ><el-date-picker
            v-model="lookup.end"
            type="date"
            value-format="YYYY-MM-DD" /></el-form-item
        ><el-form-item label=" "
          ><el-button :loading="resolving" @click="resolve">核对期间</el-button></el-form-item
        ></el-form
      >
      <el-alert v-if="lookupError" :title="lookupError" type="error" :closable="false" /><el-alert
        v-if="matched"
        :title="
          matched.groupName + ' · V' + matched.schemeVersion + ' · 抓取 ' + matched.capturedAt
        "
        type="success"
        :closable="false"
        data-testid="scheme-period-match"
      />
    </ContentWrap>
    <el-dialog
      v-model="editorVisible"
      :title="form.id ? '维护方案草稿' : '登记方案配置版本'"
      width="min(1100px, calc(100% - 24px))"
      :close-on-click-modal="false"
    >
      <el-alert
        title="保存会重新抓取源配置。已确认版本保留历史内容；源配置变化后请先抓取核对，再保存和确认。"
        type="info"
        :closable="false"
      />
      <el-form label-position="top" class="mt-4"
        ><div class="form-grid"
          ><el-form-item label="源薪资组 *"
            ><el-select
              v-model="form.groupId"
              :disabled="!!form.id"
              filterable
              remote
              :remote-method="findGroups"
              @change="clearCandidate"
              ><el-option
                v-for="g in groups"
                :key="g.id"
                :value="g.id"
                :label="g.name + ' · #' + g.id" /></el-select
          ></el-form-item>
          <el-form-item label="方案名称 *"
            ><el-input v-model="form.title" maxlength="160" /></el-form-item
          ><el-form-item label="负责人（确认必填）"
            ><el-input v-model="form.ownerName" maxlength="120" /></el-form-item
          ><el-form-item label="生效开始（确认必填）"
            ><el-date-picker
              v-model="form.effectiveFrom"
              type="date"
              value-format="YYYY-MM-DD" /></el-form-item
          ><el-form-item label="生效结束（可选）"
            ><el-date-picker v-model="form.effectiveTo" type="date" value-format="YYYY-MM-DD"
          /></el-form-item> </div
        ><el-form-item label="配置依据（确认必填）"
          ><el-input
            v-model="form.reference"
            type="textarea"
            :rows="2"
            maxlength="2000" /></el-form-item
      ></el-form>
      <el-button :loading="capturing" @click="capture">抓取源配置并核对</el-button
      ><p v-if="candidate" class="note"
        >快照抓取：{{ candidate.capturedAt }} · 共
        {{ candidate.optionCount }} 项薪资定义。人员名单和个人金额不纳入此快照。</p
      ><SnapshotView :snapshot="candidate?.snapshot" />
      <el-alert v-if="editorError" :title="editorError" type="error" :closable="false" /><template
        #footer
        ><el-button @click="editorVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template
      >
    </el-dialog>
    <el-dialog
      v-model="reviewVisible"
      :title="reviewAction === 'confirm' ? '确认方案配置版本' : '停用方案配置版本'"
      width="min(580px, calc(100% - 24px))"
      :close-on-click-modal="false"
      ><p>{{ reviewTarget?.title }} · V{{ reviewTarget?.schemeVersion }}</p
      ><el-alert
        title="核实实际配置依据及有效期。确认保留配置快照，不变更现有薪资组或工资计算。"
        type="warning"
        :closable="false"
      /><el-form label-position="top" class="mt-3"
        ><el-form-item label="评审依据 *"
          ><el-input
            v-model="evidence"
            type="textarea"
            :rows="3"
            maxlength="2000" /></el-form-item></el-form
      ><el-alert v-if="reviewError" :title="reviewError" type="error" :closable="false" /><template
        #footer
        ><el-button @click="reviewVisible = false">取消</el-button
        ><el-button type="primary" :loading="reviewing" @click="review"
          >提交评审</el-button
        ></template
      ></el-dialog
    >
    <el-drawer v-model="detailVisible" title="方案配置快照与评审记录" size="min(1120px, 100%)"
      ><el-alert v-if="detailError" :title="detailError" type="error" :closable="false" /><template
        v-if="detail"
        ><h3>{{ detail.title }} · V{{ detail.schemeVersion }}</h3
        ><el-tag>{{ Api.states[detail.status ?? 0] }}</el-tag
        ><p
          >有效期：{{ detail.effectiveFrom || '待确认' }} 至
          {{ detail.effectiveTo || '未指定结束' }}</p
        ><p>负责人：{{ detail.ownerName || '待填写' }} · 抓取：{{ detail.capturedAt }}</p
        ><p class="multiline">{{ detail.reference }}</p
        ><SnapshotView :snapshot="detail.snapshot" /><h3>评审历史</h3
        ><p>{{ detail.reviewedByName || '待评审' }} · {{ detail.reviewedTime }}</p
        ><p class="multiline">{{ detail.evidence }}</p
        ><el-collapse
          ><el-collapse-item
            v-for="h in histories"
            :key="h.id"
            :title="
              h.action +
              ' · ' +
              h.actorName +
              ' · ' +
              new Date(h.createTime).toLocaleString('zh-CN')
            "
            :name="h.id"
            ><p>{{ h.reason }}</p
            ><pre v-if="h.afterSnapshot">{{ pretty(h.afterSnapshot) }}</pre>
          </el-collapse-item></el-collapse
        ></template
      ></el-drawer
    >
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import * as Api from '@/api/hrm/payroll/scheme'
import type { History } from '@/api/hrm/payroll/requirements'
import SnapshotView from './Snapshot.vue'
defineOptions({ name: 'HrmPayrollScheme' })
const groups = ref<Api.Group[]>([]),
  rows = ref<Api.Scheme[]>([]),
  total = ref(0),
  loading = ref(false),
  error = ref('')
const query = reactive<{
  pageNo: number
  pageSize: number
  groupId?: number
  search?: string
  status?: number
}>({ pageNo: 1, pageSize: 10 })
const editorVisible = ref(false),
  form = ref<Api.Scheme>({ title: '' }),
  candidate = ref<Api.Scheme>(),
  editorError = ref(''),
  saving = ref(false),
  capturing = ref(false)
const detailVisible = ref(false),
  detail = ref<Api.Scheme>(),
  histories = ref<History[]>([]),
  detailError = ref('')
const reviewVisible = ref(false),
  reviewTarget = ref<Api.Scheme>(),
  reviewAction = ref('confirm'),
  evidence = ref(''),
  reviewError = ref(''),
  reviewing = ref(false)
const leftId = ref<number>(),
  rightId = ref<number>(),
  comparison = ref<Api.Comparison>(),
  compareError = ref(''),
  comparing = ref(false),
  kinds: Record<string, string> = { ADDED: '新增', REMOVED: '移除', CHANGED: '变更' }
const lookup = reactive({ groupId: undefined as number | undefined, start: '', end: '' }),
  matched = ref<Api.Scheme>(),
  lookupError = ref(''),
  resolving = ref(false)
const message = (e: unknown) =>
  e instanceof Error ? e.message : '操作未成功，请核对页面提示或刷新后重试。'
const findGroups = async (search?: string) => {
  try {
    groups.value = await Api.groups(search)
  } catch (e) {
    error.value = message(e)
  }
}
const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const data = await Api.page({ ...query, search: query.search || undefined })
    rows.value = data.list
    total.value = data.total
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
  query.groupId = undefined
  query.search = undefined
  query.status = undefined
  return search()
}
const clearCandidate = () => {
  candidate.value = undefined
  form.value.expectedSourceHash = undefined
}
const openEditor = async (id?: number) => {
  editorError.value = ''
  clearCandidate()
  try {
    form.value = id ? await Api.get(id) : { title: '' }
    if (id) {
      candidate.value = form.value
      form.value.expectedSourceHash = form.value.sourceHash
    }
    editorVisible.value = true
  } catch (e) {
    error.value = message(e)
  }
}
const capture = async () => {
  editorError.value = ''
  if (!form.value.groupId) {
    editorError.value = '请选择源薪资组。'
    return
  }
  capturing.value = true
  try {
    candidate.value = await Api.capture(form.value.groupId)
    form.value.expectedSourceHash = candidate.value.sourceHash
  } catch (e) {
    editorError.value = message(e)
  } finally {
    capturing.value = false
  }
}
const save = async () => {
  editorError.value = ''
  if (!form.value.groupId || !form.value.title.trim() || !candidate.value?.snapshot) {
    editorError.value = '请选择薪资组、填写名称并抓取源配置核对。'
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
const copyVersion = async (row: Api.Scheme) => {
  try {
    const id = await Api.newVersion(row.id!, row.revision!)
    await load()
    await openEditor(id)
  } catch (e) {
    error.value = message(e)
  }
}
const openReview = (row: Api.Scheme, action: string) => {
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
const compare = async () => {
  compareError.value = ''
  comparison.value = undefined
  if (!leftId.value || !rightId.value) {
    compareError.value = '请选择两个版本。'
    return
  }
  comparing.value = true
  try {
    comparison.value = await Api.compare(leftId.value, rightId.value)
  } catch (e) {
    compareError.value = message(e)
  } finally {
    comparing.value = false
  }
}
const difference = (path: string, v?: string) => {
  if (v === null || v === undefined) return '未设置'
  if (v === 'true' || v === 'false') return v === 'true' ? '是' : '否'
  if (path === 'taxRule.type')
    return (
      ({ '1': '工资薪金所得税', '2': '劳务报酬所得税', '3': '不计税' } as Record<string, string>)[
        v
      ] || v
    )
  if (path === 'taxRule.cycleType')
    return (
      ({ '1': '上年 12 月至本年 11 月', '2': '本年 1 月至 12 月' } as Record<string, string>)[v] ||
      v
    )
  if (path.startsWith('options.') && path.endsWith('.type'))
    return ['减项', '加项', '计算项'][Number(v)] || v
  return v
}
const resolve = async () => {
  lookupError.value = ''
  matched.value = undefined
  if (!lookup.groupId || !lookup.start || !lookup.end) {
    lookupError.value = '请填写薪资组 ID 和核对起止日期。'
    return
  }
  resolving.value = true
  try {
    matched.value = await Api.resolve(lookup.groupId, lookup.start, lookup.end)
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
  await findGroups()
  await load()
})
</script>
<style scoped>
.heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  gap: 12px;
  flex-wrap: wrap;
  margin: 18px 0;
}
.toolbar .el-input,
.toolbar .el-select {
  width: 220px;
}
.form-grid,
.compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}
.compare-grid {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  margin-bottom: 16px;
}
.lookup-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
  gap: 12px 16px;
}
.el-input-number,
.el-date-editor.el-input {
  width: 100%;
}
.multiline,
pre {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
pre {
  font-size: 12px;
}
@media (max-width: 640px) {
  .form-grid,
  .lookup-grid,
  .compare-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .toolbar .el-input,
  .toolbar .el-select {
    width: 100%;
  }
}
</style>
