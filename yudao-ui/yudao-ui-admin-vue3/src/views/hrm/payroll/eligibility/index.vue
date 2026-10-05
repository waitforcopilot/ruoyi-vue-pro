<template>
  <div class="eligibility-page">
    <ContentWrap>
      <div class="heading"
        ><div
          ><h2>计薪人员资格与期间</h2
          ><p>明确声明主体、人员资格与有效期，保留核对时的档案快照和评审依据。</p></div
        >
        <el-button v-if="canMaintain" type="primary" @click="openEditor()">登记资格</el-button></div
      >
      <el-alert
        title="声明主体须按真实业务依据登记。档案部门与状态仅记录抓取时所见信息；资格由负责人明确核定，确认后仍须另行核验工资输入与计算准备。"
        type="info"
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
      <div class="toolbar">
        <el-input
          v-model="query.entityCode"
          placeholder="声明主体编号（精确）"
          maxlength="64"
          @keyup.enter="search"
        />
        <el-input
          v-model="query.search"
          placeholder="人员姓名或工号"
          maxlength="160"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" clearable placeholder="全部版本状态"
          ><el-option v-for="(s, i) in Api.states" :key="i" :value="i" :label="s"
        /></el-select>
        <el-select v-model="query.qualification" clearable placeholder="全部资格结论"
          ><el-option value="INCLUDED" label="纳入计薪" /><el-option
            value="EXCLUDED"
            label="排除计薪"
        /></el-select>
        <el-button @click="search">查询</el-button><el-button @click="reset">重置</el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        border
        data-testid="eligibility-table"
      >
        <el-table-column label="声明主体" min-width="190"
          ><template #default="{ row }"
            >{{ row.entityName }}<small>{{ row.entityCode }}</small></template
          ></el-table-column
        >
        <el-table-column label="人员档案快照" min-width="205"
          ><template #default="{ row }"
            >{{ row.snapshotName || '未登记姓名'
            }}<small
              >HRM #{{ row.employeeId }} · 工号 {{ row.snapshotJobNumber || '未登记'
              }}<br />档案抓取部门 {{ row.snapshotDeptId ?? '未登记' }}</small
            ></template
          ></el-table-column
        >
        <el-table-column label="资格结论" min-width="130"
          ><template #default="{ row }"
            ><el-tag
              :type="
                row.qualification === 'INCLUDED'
                  ? 'success'
                  : row.qualification === 'EXCLUDED'
                    ? 'danger'
                    : 'info'
              "
              >{{ Api.qualification(row.qualification) }}</el-tag
            ></template
          ></el-table-column
        >
        <el-table-column label="版本 / 状态" width="155"
          ><template #default="{ row }"
            >V{{ row.eligibilityVersion }} ·
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'">{{
              Api.states[row.status]
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="资格有效期" min-width="210"
          ><template #default="{ row }"
            >{{ row.effectiveFrom || '待填写开始' }} 至
            {{ row.effectiveTo || '待填写结束' }}</template
          ></el-table-column
        >
        <el-table-column prop="ownerName" label="负责人" min-width="125" />
        <el-table-column label="操作" min-width="265"
          ><template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
            <el-button
              v-if="canMaintain && row.status === 0"
              link
              type="primary"
              @click="openEditor(row.id)"
              >编辑</el-button
            >
            <el-button
              v-if="canReview && row.status === 0"
              link
              type="success"
              @click="openReview(row, 'confirm')"
              >确认</el-button
            >
            <el-button
              v-if="canReview && row.status === 1"
              link
              type="danger"
              @click="openReview(row, 'retire')"
              >停用</el-button
            >
            <el-button
              v-if="canMaintain"
              :disabled="copying"
              link
              type="primary"
              @click="copyVersion(row)"
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
        v-if="!loading && !error && !rows.length"
        description="暂无当前人员数据范围内的资格版本。请核对主体、人员与业务依据后登记。"
      />
    </ContentWrap>
    <ContentWrap>
      <h3>按主体、人员和期间核对资格</h3
      ><p class="note"
        >须由一个已确认版本覆盖完整期间。跨版本或资料缺失时请核实；人员档案变化后须重新核对。</p
      >
      <el-form label-position="top" class="lookup-grid">
        <el-form-item label="核对主体编号"
          ><el-input v-model="lookup.entityCode" maxlength="64"
        /></el-form-item>
        <el-form-item label="核对 HRM 人员 ID"
          ><el-input-number v-model="lookup.employeeId" :min="1" :precision="0" :controls="false"
        /></el-form-item>
        <el-form-item label="核对开始日期"
          ><el-date-picker v-model="lookup.start" type="date" value-format="YYYY-MM-DD"
        /></el-form-item>
        <el-form-item label="核对结束日期"
          ><el-date-picker v-model="lookup.end" type="date" value-format="YYYY-MM-DD"
        /></el-form-item>
        <el-form-item label=" "
          ><el-button :loading="matching" @click="resolve">核对资格</el-button></el-form-item
        >
      </el-form>
      <el-alert v-if="lookupError" :title="lookupError" type="error" :closable="false" />
      <div v-if="match" data-testid="eligibility-match"
        ><el-alert
          :type="
            match.matched && match.eligibility?.qualification === 'INCLUDED' ? 'success' : 'warning'
          "
          :title="match.explanation"
          :closable="false"
        />
        <p v-if="match.eligibility" class="note"
          >{{ match.eligibility.snapshotName }} ·
          {{ Api.qualification(match.eligibility.qualification) }} · V{{
            match.eligibility.eligibilityVersion
          }}
          · 确认快照 {{ match.eligibility.snapshotCapturedAt }}<br />资格理由：{{
            match.eligibility.reason
          }}<br />资格依据：{{ match.eligibility.reference }}</p
        >
      </div>
    </ContentWrap>
    <el-dialog
      v-model="editorVisible"
      :title="form.id ? '维护资格草稿' : '登记计薪资格'"
      width="min(900px, calc(100% - 24px))"
      :close-on-click-modal="false"
    >
      <el-alert
        title="确认前必须核定纳入或排除、理由、负责人、依据及完整有效期。请先核对人员档案；保存将保留当前核对快照。"
        type="info"
        :closable="false"
      />
      <el-form v-loading="editorLoading" label-position="top" class="mt-4"
        ><div class="form-grid">
          <el-form-item label="声明主体编号 *"
            ><el-input
              v-model="form.entityCode"
              :disabled="!!form.id"
              maxlength="64"
              placeholder="稳定英文编号"
          /></el-form-item>
          <el-form-item label="声明主体名称 *"
            ><el-input v-model="form.entityName" maxlength="160"
          /></el-form-item>
          <el-form-item label="HRM 人员 ID *"
            ><el-input-number
              v-model="form.employeeId"
              :disabled="!!form.id"
              :min="1"
              :precision="0"
              :controls="false"
          /></el-form-item>
          <el-form-item label="人员核对"
            ><el-button :loading="personLoading" :disabled="!form.employeeId" @click="loadPerson"
              >核对当前档案</el-button
            ></el-form-item
          >
          <el-form-item label="明确计薪资格"
            ><el-select v-model="form.qualification" clearable placeholder="请按业务依据核定"
              ><el-option value="INCLUDED" label="纳入计薪" /><el-option
                value="EXCLUDED"
                label="排除计薪" /></el-select
          ></el-form-item>
          <el-form-item label="资格负责人"
            ><el-input v-model="form.ownerName" maxlength="120"
          /></el-form-item>
          <el-form-item label="资格开始日期"
            ><el-date-picker v-model="form.effectiveFrom" type="date" value-format="YYYY-MM-DD"
          /></el-form-item>
          <el-form-item label="资格结束日期"
            ><el-date-picker v-model="form.effectiveTo" type="date" value-format="YYYY-MM-DD"
          /></el-form-item>
        </div>
        <el-alert
          v-if="person"
          :title="`${person.snapshotName || '未登记姓名'} · HRM #${person.employeeId} · 工号 ${person.snapshotJobNumber || '未登记'} · 档案抓取部门 ${person.snapshotDeptId ?? '未登记'}`"
          type="success"
          :closable="false"
          data-testid="eligibility-person"
        />
        <p v-if="person" class="note"
          >快照抓取：{{ person.snapshotCapturedAt }}。档案状态不自动决定资格。</p
        >
        <el-form-item label="资格理由"
          ><el-input
            v-model="form.reason"
            type="textarea"
            :rows="2"
            maxlength="2000"
            show-word-limit
        /></el-form-item>
        <el-form-item label="资格依据 / 参考材料"
          ><el-input
            v-model="form.reference"
            type="textarea"
            :rows="2"
            maxlength="2000"
            show-word-limit
        /></el-form-item>
      </el-form>
      <el-alert v-if="editorError" :title="editorError" type="error" :closable="false" />
      <template #footer
        ><el-button :disabled="saving" @click="editorVisible = false">取消</el-button
        ><el-button
          type="primary"
          :disabled="editorLoading || personLoading"
          :loading="saving"
          @click="save"
          >保存草稿</el-button
        ></template
      >
    </el-dialog>
    <el-dialog
      v-model="detailVisible"
      title="资格版本详情与评审历史"
      width="min(1000px, calc(100% - 24px))"
    >
      <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" />
      <div v-loading="detailLoading"
        ><template v-if="detail">
          <el-descriptions :column="2" border
            ><el-descriptions-item label="声明主体"
              >{{ detail.entityCode }} · {{ detail.entityName }}</el-descriptions-item
            ><el-descriptions-item label="版本状态"
              >V{{ detail.eligibilityVersion }} ·
              {{ Api.states[detail.status ?? 0] }}</el-descriptions-item
            >
            <el-descriptions-item label="资格结论">{{
              Api.qualification(detail.qualification)
            }}</el-descriptions-item
            ><el-descriptions-item label="人员快照"
              >{{ detail.snapshotName }} · HRM #{{ detail.employeeId }}</el-descriptions-item
            >
            <el-descriptions-item label="有效期"
              >{{ detail.effectiveFrom || '待填写' }} 至
              {{ detail.effectiveTo || '待填写' }}</el-descriptions-item
            ><el-descriptions-item label="负责人">{{
              detail.ownerName || '待填写'
            }}</el-descriptions-item>
            <el-descriptions-item label="档案抓取部门">{{
              detail.snapshotDeptId ?? '未登记'
            }}</el-descriptions-item
            ><el-descriptions-item label="快照抓取">{{
              detail.snapshotCapturedAt
            }}</el-descriptions-item>
            <el-descriptions-item label="资格理由" :span="2">{{
              detail.reason || '待填写'
            }}</el-descriptions-item
            ><el-descriptions-item label="资格依据" :span="2">{{
              detail.reference || '待填写'
            }}</el-descriptions-item
            ><el-descriptions-item label="评审依据" :span="2">{{
              detail.evidence || '尚未评审'
            }}</el-descriptions-item>
          </el-descriptions>
          <h3>评审历史</h3
          ><el-table :data="events" border
            ><el-table-column label="操作" width="140"
              ><template #default="{ row }">{{
                actions[row.action] || row.action
              }}</template></el-table-column
            ><el-table-column prop="actorName" label="操作人" min-width="130" /><el-table-column
              label="修订"
              width="130"
              ><template #default="{ row }"
                >{{ row.fromVersion ?? '初建' }} → {{ row.toVersion }}</template
              ></el-table-column
            ><el-table-column prop="reason" label="依据（按数据权限展示）" min-width="260"
          /></el-table> </template
      ></div>
    </el-dialog>
    <el-dialog
      v-model="reviewVisible"
      :title="reviewForm.action === 'confirm' ? '确认计薪资格' : '停用计薪资格'"
      width="min(620px, calc(100% - 24px))"
      :close-on-click-modal="false"
    >
      <p v-if="reviewRow"
        >{{ reviewRow.snapshotName }} · {{ reviewRow.entityName }} · V{{
          reviewRow.eligibilityVersion
        }}
        · {{ Api.qualification(reviewRow.qualification) }}</p
      >
      <el-alert
        title="确认须核对资格、有效期和当前人员档案。停用后保留历史证据，该版本不再参与期间匹配。"
        type="info"
        :closable="false"
      />
      <el-form label-position="top" class="mt-4"
        ><el-form-item label="评审依据 *"
          ><el-input
            v-model="reviewForm.evidence"
            type="textarea"
            :rows="4"
            maxlength="4000" /></el-form-item
      ></el-form>
      <el-alert v-if="reviewError" :title="reviewError" type="error" :closable="false" />
      <template #footer
        ><el-button :disabled="reviewing" @click="reviewVisible = false">取消</el-button
        ><el-button type="primary" :loading="reviewing" @click="review"
          >提交评审</el-button
        ></template
      >
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import * as Api from '@/api/hrm/payroll/eligibility'
import type { Mapping } from '@/api/hrm/payroll/identity'
import type { History } from '@/api/hrm/payroll/requirements'
import { checkPermi } from '@/utils/permission'
defineOptions({ name: 'HrmPayrollEligibility' })
const message = useMessage()
const canMaintain = computed(
  () =>
    checkPermi(['hrm:employee:query']) &&
    checkPermi(['hrm:payroll:eligibility:query']) &&
    checkPermi(['hrm:payroll:eligibility:maintain'])
)
const canReview = computed(
  () =>
    checkPermi(['hrm:employee:query']) &&
    checkPermi(['hrm:payroll:eligibility:query']) &&
    checkPermi(['hrm:payroll:eligibility:review'])
)
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  entityCode: '',
  search: '',
  status: undefined as number | undefined,
  qualification: undefined as string | undefined
})
const rows = ref<Api.Eligibility[]>([]),
  total = ref(0),
  loading = ref(false),
  error = ref('')
const lookup = reactive<Api.LookupRequest>({
  entityCode: '',
  employeeId: undefined,
  start: '',
  end: ''
})
const match = ref<Api.Match>(),
  lookupError = ref(''),
  matching = ref(false)
const editorVisible = ref(false),
  editorLoading = ref(false),
  editorError = ref(''),
  personLoading = ref(false),
  saving = ref(false)
const blank = (): Api.Eligibility => ({
  entityCode: '',
  entityName: '',
  employeeId: undefined,
  qualification: undefined,
  effectiveFrom: '',
  effectiveTo: '',
  ownerName: '',
  reason: '',
  reference: ''
})
const form = ref<Api.Eligibility>(blank()),
  person = ref<Partial<Mapping>>()
const detailVisible = ref(false),
  detailLoading = ref(false),
  detailError = ref(''),
  detail = ref<Api.Eligibility>(),
  events = ref<History[]>([])
const reviewVisible = ref(false),
  reviewError = ref(''),
  reviewing = ref(false),
  reviewRow = ref<Api.Eligibility>(),
  copying = ref(false)
const reviewForm = reactive({ id: 0, revision: 0, action: 'confirm', evidence: '' })
const actions: Record<string, string> = {
  create: '登记草稿',
  update: '更新草稿',
  'new-version': '另建版本',
  confirm: '确认',
  retire: '停用'
}
let loadSequence = 0,
  lookupSequence = 0,
  editorSequence = 0,
  personSequence = 0,
  detailSequence = 0
const errorText = (e: unknown) =>
  typeof e === 'string' ? e : e instanceof Error ? e.message : '操作失败，请刷新或核对资料后重试'
const clearMatch = () => {
  lookupSequence++
  match.value = undefined
  lookupError.value = ''
  matching.value = false
}
watch(() => JSON.stringify(lookup), clearMatch)
watch(editorVisible, (open) => {
  if (!open) {
    editorSequence++
    personSequence++
    editorLoading.value = false
    personLoading.value = false
  }
})
watch(
  () => form.value.employeeId,
  () => {
    if (!form.value.id) {
      personSequence++
      person.value = undefined
      form.value.employeeFingerprint = undefined
      personLoading.value = false
    }
  }
)
watch(detailVisible, (open) => {
  if (!open) {
    detailSequence++
    detailLoading.value = false
  }
})
const load = async () => {
  const sequence = ++loadSequence
  loading.value = true
  error.value = ''
  clearMatch()
  try {
    const data = await Api.page({ ...query })
    if (sequence !== loadSequence) return
    rows.value = data.list
    total.value = data.total
  } catch (e) {
    if (sequence === loadSequence) {
      rows.value = []
      total.value = 0
      error.value = errorText(e)
    }
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}
const search = () => {
  query.pageNo = 1
  load()
}
const reset = () => {
  Object.assign(query, {
    pageNo: 1,
    entityCode: '',
    search: '',
    status: undefined,
    qualification: undefined
  })
  load()
}
const openEditor = async (id?: number) => {
  const sequence = ++editorSequence
  personSequence++
  form.value = blank()
  person.value = undefined
  editorError.value = ''
  editorVisible.value = true
  editorLoading.value = !!id
  if (!id) return
  try {
    const data = await Api.get(id)
    if (sequence !== editorSequence || !editorVisible.value) return
    form.value = data
    person.value = data
  } catch (e) {
    if (sequence === editorSequence) editorError.value = errorText(e)
  } finally {
    if (sequence === editorSequence) editorLoading.value = false
  }
}
const loadPerson = async () => {
  const id = form.value.employeeId
  if (!id) return
  const sequence = ++personSequence,
    editor = editorSequence
  personLoading.value = true
  editorError.value = ''
  try {
    const data = await Api.employee(id)
    if (
      sequence !== personSequence ||
      editor !== editorSequence ||
      !editorVisible.value ||
      id !== form.value.employeeId
    )
      return
    person.value = data
    form.value.employeeFingerprint = data.employeeFingerprint
  } catch (e) {
    if (sequence === personSequence && editor === editorSequence) {
      person.value = undefined
      form.value.employeeFingerprint = undefined
      editorError.value = errorText(e)
    }
  } finally {
    if (sequence === personSequence) personLoading.value = false
  }
}
const save = async () => {
  if (
    !form.value.entityCode ||
    !form.value.entityName.trim() ||
    !form.value.employeeId ||
    !form.value.employeeFingerprint
  ) {
    editorError.value = '请填写声明主体、人员 ID，并核对人员档案'
    return
  }
  saving.value = true
  editorError.value = ''
  try {
    const payload = {
      ...form.value,
      qualification: form.value.qualification || undefined,
      effectiveFrom: form.value.effectiveFrom || undefined,
      effectiveTo: form.value.effectiveTo || undefined
    }
    if (form.value.id) await Api.update(payload)
    else await Api.create(payload)
    editorVisible.value = false
    message.success('资格草稿已保存，尚未确认')
    await load()
  } catch (e) {
    editorError.value = errorText(e)
  } finally {
    saving.value = false
  }
}
const copyVersion = async (row: Api.Eligibility) => {
  try {
    await message.confirm(
      '另建版本将抓取当前人员档案。请重新核对资格与有效期，评审结论须重新确认。'
    )
    copying.value = true
    const id = await Api.newVersion(row.id!, row.revision!)
    await load()
    await openEditor(id)
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') error.value = errorText(e)
  } finally {
    copying.value = false
  }
}
const openReview = (row: Api.Eligibility, action: string) => {
  reviewRow.value = row
  Object.assign(reviewForm, { id: row.id, revision: row.revision, action, evidence: '' })
  reviewError.value = ''
  reviewVisible.value = true
}
const review = async () => {
  if (!reviewForm.evidence.trim()) {
    reviewError.value = '必须填写评审依据'
    return
  }
  reviewing.value = true
  reviewError.value = ''
  try {
    await Api.review({ ...reviewForm })
    reviewVisible.value = false
    message.success('评审已保存')
    await load()
  } catch (e) {
    reviewError.value = errorText(e)
  } finally {
    reviewing.value = false
  }
}
const showDetail = async (id: number) => {
  const sequence = ++detailSequence
  detail.value = undefined
  events.value = []
  detailError.value = ''
  detailVisible.value = true
  detailLoading.value = true
  try {
    const [row, history] = await Promise.all([Api.get(id), Api.history(id)])
    if (sequence !== detailSequence || !detailVisible.value) return
    detail.value = row
    events.value = history
  } catch (e) {
    if (sequence === detailSequence) detailError.value = errorText(e)
  } finally {
    if (sequence === detailSequence) detailLoading.value = false
  }
}
const resolve = async () => {
  clearMatch()
  if (!lookup.entityCode || !lookup.employeeId || !lookup.start || !lookup.end) {
    lookupError.value = '请填写主体、人员 ID 和完整核对期间'
    return
  }
  const sequence = ++lookupSequence
  matching.value = true
  try {
    const result = await Api.lookup({ ...lookup })
    if (sequence === lookupSequence) match.value = result
  } catch (e) {
    if (sequence === lookupSequence) lookupError.value = errorText(e)
  } finally {
    if (sequence === lookupSequence) matching.value = false
  }
}
onMounted(load)
</script>
<style scoped>
.heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
h2 {
  margin: 0 0 8px;
  font-size: 22px;
}
h3 {
  margin: 0 0 12px;
}
p,
.note {
  color: var(--el-text-color-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
small {
  display: block;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
  margin-top: 4px;
}
.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin: 18px 0;
}
.toolbar .el-input,
.toolbar .el-select {
  width: 205px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}
.lookup-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}
.form-grid :deep(.el-select),
.form-grid :deep(.el-input-number),
.form-grid :deep(.el-date-editor),
.lookup-grid :deep(.el-input-number),
.lookup-grid :deep(.el-date-editor) {
  width: 100%;
  min-width: 0;
}
@media (max-width: 800px) {
  .lookup-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 520px) {
  .form-grid,
  .lookup-grid {
    grid-template-columns: 1fr;
  }
  .toolbar .el-input,
  .toolbar .el-select {
    width: 100%;
  }
}
</style>
