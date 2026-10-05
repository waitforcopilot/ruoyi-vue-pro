<template>
  <div class="calculation-page">
    <ContentWrap>
      <div class="heading"
        ><div
          ><h2>工资规则表达式与核对</h2
          ><p>保存明确输入、表达式、舍入步骤和业务样例，按版本核对结果。</p></div
        ><el-button v-if="canMaintain" type="primary" @click="edit()">登记计算草稿</el-button></div
      >
      <el-alert
        title="样例验算通过不替代 HR/财务签认。规则核对不生成工资月表、个人扣款或发放记录。"
        type="info"
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
      <div class="toolbar"
        ><el-input
          v-model="query.search"
          placeholder="搜索规则名称"
          maxlength="160"
          @keyup.enter="search"
        /><el-input
          v-model="query.scopeCode"
          placeholder="适用范围编号"
          maxlength="64"
          @keyup.enter="search"
        /><el-select v-model="query.status" clearable placeholder="全部状态"
          ><el-option
            v-for="(state, index) in Api.states"
            :key="index"
            :value="index"
            :label="state" /></el-select
        ><el-button @click="search">查询</el-button><el-button @click="reset">重置</el-button></div
      >
      <el-table
        :data="rows"
        row-key="id"
        border
        v-loading="loading"
        data-testid="calculation-definitions"
      >
        <el-table-column label="规则" min-width="190"
          ><template #default="{ row }"
            >{{ row.title }}<p class="note compact">{{ row.code }}</p></template
          ></el-table-column
        >
        <el-table-column label="范围" min-width="170"
          ><template #default="{ row }"
            >{{ row.applicableScope || '待说明'
            }}<p class="note compact">{{ row.scopeCode }}</p></template
          ></el-table-column
        >
        <el-table-column label="版本 / 状态" width="145"
          ><template #default="{ row }"
            >V{{ row.definitionVersion }} ·
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'">{{
              Api.states[row.status]
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="输入 / 项目 / 样例" width="145"
          ><template #default="{ row }"
            >{{ row.inputCount }} / {{ row.itemCount }} / {{ row.caseCount }}</template
          ></el-table-column
        >
        <el-table-column label="有效期" min-width="200"
          ><template #default="{ row }"
            >{{ row.effectiveFrom || '待确认'
            }}{{ row.effectiveFrom ? ' 至 ' + (row.effectiveTo || '未指定结束') : '' }}</template
          ></el-table-column
        >
        <el-table-column prop="ownerName" label="负责人" min-width="110" />
        <el-table-column label="操作" min-width="260"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="showDetail(row.id)">详情</el-button
            ><el-button
              v-if="canMaintain && row.status === 0"
              link
              type="primary"
              @click="edit(row.id)"
              >编辑</el-button
            ><el-button
              v-if="canReview && row.status === 0"
              link
              type="success"
              @click="openReview(row, 'confirm')"
              >确认</el-button
            ><el-button
              v-if="canReview && row.status === 1"
              link
              type="danger"
              @click="openReview(row, 'retire')"
              >停用</el-button
            ><el-button v-if="canMaintain" link type="primary" @click="copyVersion(row)"
              >另建版本</el-button
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
      <el-empty
        v-if="!loading && !error && !rows.length"
        description="暂无计算定义。登记输入、表达式和可逐项对账的样例后提交评审。"
      />
    </ContentWrap>
    <ContentWrap>
      <h3>明确输入核对</h3
      ><p class="note"
        >选择单个已确认版本和完整期间，逐项填写输入。金额以文本传递；缺值不填零，单位不自动换算。</p
      >
      <div class="lookup-grid"
        ><label
          >规则版本<el-select
            v-model="lookup.definitionId"
            aria-label="核对规则版本"
            placeholder="选择版本"
            @change="chooseDefinition"
            ><el-option
              v-for="row in rows"
              :key="row.id"
              :value="row.id!"
              :label="versionLabel(row)" /></el-select></label
        ><label
          >开始日期<el-date-picker
            v-model="lookup.start"
            type="date"
            value-format="YYYY-MM-DD"
            aria-label="核对开始日期" /></label
        ><label
          >结束日期<el-date-picker
            v-model="lookup.end"
            type="date"
            value-format="YYYY-MM-DD"
            aria-label="核对结束日期" /></label
      ></div>
      <div v-if="selected?.program" class="lookup-inputs"
        ><label v-for="field in selected.program.inputs" :key="field.key"
          >{{ field.label }} · {{ field.unit
          }}<el-input
            v-model="lookup.inputs[field.key]"
            inputmode="decimal"
            maxlength="40"
            :aria-label="'核对输入 ' + field.key"
            :placeholder="
              field.type === 'INTEGER'
                ? '明确填写整数，包括有效零值'
                : '明确填写十进制值，包括有效零值'
            " /></label
      ></div>
      <p v-if="selected" class="note"
        >范围：{{ selected.applicableScope || '待说明' }}；所选状态：{{
          Api.states[selected.status ?? 0]
        }}</p
      >
      <el-button
        type="primary"
        :disabled="!selected || selected.status !== 1"
        :loading="previewing"
        @click="preview"
        >执行规则核对</el-button
      >
      <div v-if="result" class="result" data-testid="calculation-preview"
        ><h3>核对结果 · V{{ result.definition.definitionVersion }}</h3
        ><p>{{ result.explanation }}</p
        ><p class="note">输入：{{ mapText(result.result.inputs) }}</p>
        <el-table :data="result.result.items" row-key="key" border
          ><el-table-column type="index" label="顺序" width="70" /><el-table-column
            label="项目"
            min-width="150"
            ><template #default="{ row }"
              >{{ row.label }}<p class="note compact">{{ row.key }}</p></template
            ></el-table-column
          ><el-table-column prop="expression" label="表达式" min-width="200" /><el-table-column
            prop="rawResult"
            label="舍入前结果"
            min-width="160"
          /><el-table-column prop="amount" label="结果" min-width="150" /><el-table-column
            prop="unit"
            label="单位"
            width="110"
          /><el-table-column label="舍入" min-width="160"
            ><template #default="{ row }"
              >{{ row.amountScale }} 位 / {{ Api.roundings[row.roundingMode] }}</template
            ></el-table-column
          ><el-table-column type="expand"
            ><template #default="{ row }"
              ><div class="steps"
                ><p>引用：{{ row.dependencies.join('、') || '常量' }}</p
                ><ol
                  ><li v-for="(step, index) in row.steps" :key="index">{{ step }}</li></ol
                ></div
              ></template
            ></el-table-column
          ></el-table
        >
        <p class="hash">规则指纹：{{ result.programHash }}</p>
      </div>
    </ContentWrap>
    <ContentWrap
      ><h3>版本对比</h3
      ><div class="compare-grid"
        ><el-select v-model="leftId" placeholder="左侧版本" aria-label="对比左侧版本"
          ><el-option
            v-for="row in rows"
            :key="row.id"
            :value="row.id!"
            :label="versionLabel(row)" /></el-select
        ><el-select v-model="rightId" placeholder="右侧版本" aria-label="对比右侧版本"
          ><el-option
            v-for="row in rows"
            :key="row.id"
            :value="row.id!"
            :label="versionLabel(row)" /></el-select
        ><el-button :loading="comparing" @click="compare">对比版本</el-button></div
      ><p class="note">仅对比同编号和范围；缺失值与明确的 0 分开显示。</p
      ><el-table
        v-if="comparison"
        :data="comparison.changes"
        border
        data-testid="calculation-comparison"
        ><el-table-column prop="label" label="变化项" width="180" /><el-table-column label="左侧"
          ><template #default="{ row }">
            <pre>{{ changeText(row.path, row.left) }}</pre>
          </template></el-table-column
        ><el-table-column label="右侧"
          ><template #default="{ row }">
            <pre>{{ changeText(row.path, row.right) }}</pre>
          </template></el-table-column
        ></el-table
      ><p v-if="comparison && !comparison.changes.length" class="note"
        >两个版本的规则内容一致。</p
      ></ContentWrap
    >
    <CalculationEditor ref="editor" @saved="load" />
    <Dialog v-model="detailVisible" title="计算规则详情" width="min(1060px, 96vw)"
      ><el-alert v-if="detailError" :title="detailError" type="error" :closable="false" /><template
        v-if="detail"
      >
        <el-descriptions :column="2" border
          ><el-descriptions-item label="规则"
            >{{ detail.title }} · {{ detail.code }}</el-descriptions-item
          ><el-descriptions-item label="版本状态"
            >V{{ detail.definitionVersion }} ·
            {{ Api.states[detail.status ?? 0] }}</el-descriptions-item
          ><el-descriptions-item label="适用范围"
            >{{ detail.applicableScope || '待说明' }} · {{ detail.scopeCode }}</el-descriptions-item
          ><el-descriptions-item label="负责人">{{
            detail.ownerName || '待填写'
          }}</el-descriptions-item
          ><el-descriptions-item label="依据">{{
            detail.reference || '待填写'
          }}</el-descriptions-item
          ><el-descriptions-item label="确认依据">{{
            detail.evidence || '待确认'
          }}</el-descriptions-item></el-descriptions
        >
        <el-tabs v-model="detailTab"
          ><el-tab-pane label="定义与舍入" name="definition"
            ><p>{{ detail.description }}</p
            ><p
              >每次除法：{{ detail.program?.divisionScale ?? '未声明' }} 位 /
              {{ detail.program?.divisionRoundingMode || '未声明' }}</p
            ><el-table :data="detail.program?.inputs" border
              ><el-table-column prop="key" label="输入编号" /><el-table-column
                prop="label"
                label="名称" /><el-table-column prop="unit" label="单位" /><el-table-column
                prop="type"
                label="类型" /><el-table-column prop="scale" label="小数位" /></el-table
            ><el-table :data="detail.program?.items" border class="mt-4"
              ><el-table-column prop="key" label="项目编号" /><el-table-column
                prop="label"
                label="名称" /><el-table-column
                prop="expression"
                label="表达式"
                min-width="220" /><el-table-column prop="unit" label="单位" /><el-table-column
                prop="amountScale"
                label="小数位" /><el-table-column prop="roundingMode" label="舍入方式" /></el-table
          ></el-tab-pane>
          <el-tab-pane label="样例与预期" name="cases"
            ><el-button @click="verifyCases">重新核对样例</el-button
            ><p v-if="verified"
              >{{ verified.passed }} / {{ verified.total }} 项通过 ·
              {{ verified.allPassed ? '全部匹配' : '尚未全部匹配，不能确认' }}</p
            ><el-table :data="verified?.cases" border data-testid="calculation-cases"
              ><el-table-column prop="title" label="样例" min-width="170" /><el-table-column
                label="预期"
                min-width="220"
                ><template #default="{ row }">{{
                  mapText(row.expected)
                }}</template></el-table-column
              ><el-table-column label="实际" min-width="220"
                ><template #default="{ row }">{{ mapText(row.actual) }}</template></el-table-column
              ><el-table-column label="结果" min-width="180"
                ><template #default="{ row }"
                  ><el-tag :type="row.passed ? 'success' : 'danger'">{{
                    row.passed ? '匹配' : '不匹配'
                  }}</el-tag
                  ><p>{{ row.error }}</p></template
                ></el-table-column
              ></el-table
            ><p v-if="verified" class="hash">规则指纹：{{ verified.programHash }}</p></el-tab-pane
          >
          <el-tab-pane label="评审历史" name="history"
            ><el-table :data="history" border
              ><el-table-column label="操作"
                ><template #default="{ row }">{{
                  actions[row.action] || row.action
                }}</template></el-table-column
              ><el-table-column prop="actorName" label="操作人" /><el-table-column label="修订"
                ><template #default="{ row }"
                  >{{ row.fromVersion ?? '初始' }} → {{ row.toVersion }}</template
                ></el-table-column
              ><el-table-column label="时间" min-width="165"
                ><template #default="{ row }">{{
                  row.createTime == null ? '' : formatDate(row.createTime)
                }}</template></el-table-column
              ><el-table-column
                prop="reason"
                label="依据"
                min-width="240" /></el-table></el-tab-pane
        ></el-tabs> </template
    ></Dialog>
    <Dialog
      v-model="reviewVisible"
      :title="reviewForm.action === 'confirm' ? '确认计算规则' : '停用计算规则'"
      width="580px"
      ><p>{{ reviewTitle }}</p
      ><el-alert
        v-if="reviewForm.action === 'confirm'"
        title="确认将冻结此版本。须有明确范围、负责人、依据和有效期，且样例逐项匹配。"
        type="info"
        :closable="false"
      /><el-input
        v-model="reviewForm.evidence"
        class="mt-4"
        type="textarea"
        :rows="4"
        maxlength="4000"
        placeholder="填写评审依据和结论"
        aria-label="规则评审依据"
      /><el-alert
        v-if="reviewError"
        :title="reviewError"
        type="error"
        :closable="false"
        class="mt-3"
      /><template #footer
        ><el-button @click="reviewVisible = false">取消</el-button
        ><el-button type="primary" :loading="reviewing" @click="review"
          >提交评审</el-button
        ></template
      ></Dialog
    >
  </div>
</template>
<script setup lang="ts">
import * as Api from '@/api/hrm/payroll/calculation'
import { checkPermi } from '@/utils/permission'
import { formatDate } from '@/utils/formatTime'
import CalculationEditor from './CalculationEditor.vue'
defineOptions({ name: 'HrmPayrollCalculation' })
const canMaintain = computed(
  () =>
    checkPermi(['hrm:payroll:calculation:query']) &&
    checkPermi(['hrm:payroll:calculation:maintain'])
)
const canReview = computed(
  () =>
    checkPermi(['hrm:payroll:calculation:query']) && checkPermi(['hrm:payroll:calculation:review'])
)
const error = ref(''),
  loading = ref(false),
  rows = ref<Api.Definition[]>([]),
  total = ref(0)
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  search: '',
  scopeCode: '',
  status: undefined as number | undefined
})
const editor = ref<InstanceType<typeof CalculationEditor>>()
const lookup = reactive({
  definitionId: undefined as number | undefined,
  start: '',
  end: '',
  inputs: {} as Record<string, string>
})
const selected = ref<Api.Definition>(),
  result = ref<Api.Preview>(),
  previewing = ref(false)
const leftId = ref<number>(),
  rightId = ref<number>(),
  comparison = ref<Api.Comparison>(),
  comparing = ref(false)
const detailVisible = ref(false),
  detail = ref<Api.Definition>(),
  detailError = ref(''),
  detailTab = ref('definition'),
  verified = ref<Api.Cases>(),
  history = ref<any[]>([])
const reviewVisible = ref(false),
  reviewTitle = ref(''),
  reviewError = ref(''),
  reviewing = ref(false)
const reviewForm = reactive({ id: 0, revision: 0, action: '', evidence: '' })
const actions: Record<string, string> = {
  create: '登记草稿',
  update: '维护草稿',
  'new-version': '另建版本',
  confirm: '确认规则',
  retire: '停用规则'
}
const versionLabel = (row: Api.Definition) =>
  `${row.title} · ${row.code} · V${row.definitionVersion} · ${Api.states[row.status ?? 0]}`
const mapText = (values?: Record<string, string>) =>
  values
    ? Object.entries(values)
        .map(([key, value]) => `${key} = ${value ?? '缺失'}`)
        .join('；')
    : '缺失'
const changeText = (path: string, value?: string) => {
  if (value == null) return '缺失'
  if (path !== 'program') return value
  try {
    const program = JSON.parse(value) as Api.Program
    return [
      '输入：' +
        program.inputs
          .map((x) => `${x.key}（${x.label}，${x.unit}，${x.scale ?? '缺失'} 位）`)
          .join('；'),
      '项目：' +
        program.items
          .map(
            (x) => `${x.key} = ${x.expression}（${x.amountScale ?? '缺失'} 位 / ${x.roundingMode}）`
          )
          .join('；'),
      `每次除法：${program.divisionScale ?? '未声明'} 位 / ${program.divisionRoundingMode ?? '未声明'}`,
      '样例：' +
        program.cases
          .map((x) => `${x.title}：${mapText(x.inputs)}；预期 ${mapText(x.expected)}`)
          .join('\n')
    ].join('\n')
  } catch {
    return value
  }
}
let loadSequence = 0,
  selectionSequence = 0,
  previewSequence = 0,
  detailSequence = 0,
  comparisonSequence = 0
const clearPreview = () => {
  previewSequence++
  result.value = undefined
}
watch(() => [lookup.start, lookup.end, JSON.stringify(lookup.inputs)], clearPreview)
watch([leftId, rightId], () => {
  comparisonSequence++
  comparison.value = undefined
  comparing.value = false
})
watch(detailVisible, (value) => {
  if (!value) detailSequence++
})
const load = async () => {
  const sequence = ++loadSequence
  loading.value = true
  error.value = ''
  selectionSequence++
  selected.value = undefined
  lookup.inputs = {}
  clearPreview()
  comparisonSequence++
  comparison.value = undefined
  try {
    const data = await Api.page({ ...query })
    if (sequence !== loadSequence) return
    rows.value = data.list
    total.value = data.total
    if (lookup.definitionId && !rows.value.some((row) => row.id === lookup.definitionId))
      lookup.definitionId = undefined
    if (lookup.definitionId) await chooseDefinition()
    if (leftId.value && !rows.value.some((row) => row.id === leftId.value)) leftId.value = undefined
    if (rightId.value && !rows.value.some((row) => row.id === rightId.value))
      rightId.value = undefined
  } catch (e: any) {
    if (sequence === loadSequence) error.value = e?.message || String(e)
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}
const search = () => {
  query.pageNo = 1
  return load()
}
const reset = () => {
  query.search = ''
  query.scopeCode = ''
  query.status = undefined
  return search()
}
const edit = async (id?: number) => {
  try {
    await editor.value?.open(id)
  } catch (e: any) {
    error.value = e?.message || String(e)
  }
}
const chooseDefinition = async () => {
  const sequence = ++selectionSequence,
    id = lookup.definitionId
  selected.value = undefined
  lookup.inputs = {}
  clearPreview()
  error.value = ''
  if (!id) return
  try {
    const data = await Api.get(id)
    if (sequence !== selectionSequence || id !== lookup.definitionId) return
    selected.value = data
    lookup.inputs = Object.fromEntries((data.program?.inputs || []).map((field) => [field.key, '']))
  } catch (e: any) {
    if (sequence === selectionSequence) error.value = e?.message || String(e)
  }
}
const preview = async () => {
  clearPreview()
  const sequence = previewSequence
  previewing.value = true
  error.value = ''
  try {
    if (!lookup.definitionId || !lookup.start || !lookup.end)
      throw new Error('请选择版本并明确开始、结束日期')
    const data = await Api.preview({
      definitionId: lookup.definitionId,
      start: lookup.start,
      end: lookup.end,
      inputs: { ...lookup.inputs }
    })
    if (sequence === previewSequence) result.value = data
  } catch (e: any) {
    if (sequence === previewSequence) error.value = e?.message || String(e)
  } finally {
    previewing.value = false
  }
}
const showDetail = async (id: number) => {
  const sequence = ++detailSequence
  error.value = ''
  try {
    const [data, events] = await Promise.all([Api.get(id), Api.history(id)])
    if (sequence !== detailSequence) return
    detail.value = data
    detailError.value = ''
    verified.value = data.verifiedCases
    history.value = events
    detailTab.value = 'definition'
    detailVisible.value = true
  } catch (e: any) {
    if (sequence === detailSequence) error.value = e?.message || String(e)
  }
}
const verifyCases = async () => {
  const sequence = detailSequence,
    id = detail.value?.id
  detailError.value = ''
  try {
    if (id) {
      const data = await Api.cases(id)
      if (sequence === detailSequence && detailVisible.value && id === detail.value?.id)
        verified.value = data
    }
  } catch (e: any) {
    if (sequence === detailSequence) detailError.value = e?.message || String(e)
  }
}
const copyVersion = async (row: Api.Definition) => {
  error.value = ''
  try {
    await Api.newVersion(row.id!, row.revision!)
    await load()
  } catch (e: any) {
    error.value = e?.message || String(e)
  }
}
const openReview = (row: Api.Definition, action: string) => {
  Object.assign(reviewForm, { id: row.id, revision: row.revision, action, evidence: '' })
  reviewTitle.value = versionLabel(row)
  reviewError.value = ''
  reviewVisible.value = true
}
const review = async () => {
  reviewing.value = true
  reviewError.value = ''
  try {
    await Api.review({ ...reviewForm })
    reviewVisible.value = false
    selected.value = undefined
    clearPreview()
    await load()
  } catch (e: any) {
    reviewError.value = e?.message || String(e)
  } finally {
    reviewing.value = false
  }
}
const compare = async () => {
  const sequence = ++comparisonSequence
  comparison.value = undefined
  comparing.value = true
  error.value = ''
  try {
    if (!leftId.value || !rightId.value) throw new Error('请选择两个版本')
    const data = await Api.compare(leftId.value, rightId.value)
    if (sequence === comparisonSequence) comparison.value = data
  } catch (e: any) {
    if (sequence === comparisonSequence) error.value = e?.message || String(e)
  } finally {
    if (sequence === comparisonSequence) comparing.value = false
  }
}
onMounted(load)
</script>
<style scoped>
.heading {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
}
.heading h2 {
  margin: 0 0 8px;
}
.heading p,
.note {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
.compact {
  margin: 4px 0 0;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin: 20px 0;
}
.toolbar :deep(.el-input),
.toolbar :deep(.el-select) {
  max-width: 230px;
}
.lookup-grid,
.lookup-inputs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  margin: 18px 0;
}
.lookup-grid label,
.lookup-inputs label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
}
.lookup-grid :deep(.el-select),
.lookup-grid :deep(.el-date-editor) {
  width: 100%;
}
.compare-grid {
  display: flex;
  gap: 14px;
}
.compare-grid :deep(.el-select) {
  width: 320px;
}
.result {
  margin-top: 24px;
  padding-top: 8px;
  border-top: 1px solid #e2e8f0;
}
.hash {
  color: #64748b;
  font-size: 12px;
  word-break: break-all;
}
.steps {
  padding: 12px 30px;
  line-height: 1.8;
}
pre {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  line-height: 1.7;
}
@media (max-width: 760px) {
  .heading,
  .toolbar,
  .compare-grid {
    flex-wrap: wrap;
  }
  .lookup-grid,
  .lookup-inputs {
    grid-template-columns: 1fr;
  }
  .toolbar :deep(.el-input),
  .toolbar :deep(.el-select),
  .compare-grid :deep(.el-select) {
    max-width: 100%;
    width: 100%;
  }
}
</style>
