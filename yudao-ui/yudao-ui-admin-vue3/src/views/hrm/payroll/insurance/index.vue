<template>
  <div class="policy-page">
    <ContentWrap>
      <div class="heading">
        <div
          ><h2>社保公积金本地政策</h2
          ><p>按城市、适用范围和项目保存政策版本、缴费参数及确认依据。</p></div
        >
        <el-button v-if="canMaintain" type="primary" @click="openEditor()">登记政策草稿</el-button>
      </div>
      <el-alert
        title="确认保留本地政策。声明基数核对不证明人员参保资格，也不生成缴费月账或工资扣款。"
        type="info"
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
      <div class="toolbar">
        <el-input
          v-model="query.search"
          placeholder="搜索政策名称"
          maxlength="160"
          @keyup.enter="search"
        />
        <el-input
          v-model="query.scopeCode"
          placeholder="适用范围编号"
          maxlength="64"
          @keyup.enter="search"
        />
        <el-select v-model="query.projectType" clearable placeholder="全部缴费项目"
          ><el-option v-for="p in projects" :key="p.type" :value="p.type" :label="p.name"
        /></el-select>
        <el-select v-model="query.status" clearable placeholder="全部状态"
          ><el-option v-for="(s, i) in Api.states" :key="i" :value="i" :label="s"
        /></el-select>
        <el-button @click="search">查询</el-button><el-button @click="reset">重置</el-button>
      </div>
      <el-table
        :data="rows"
        row-key="id"
        v-loading="loading"
        border
        data-testid="insurance-policies"
      >
        <el-table-column prop="title" label="政策名称" min-width="180" /><el-table-column
          prop="cityName"
          label="参保城市"
          width="110"
        />
        <el-table-column label="适用范围" min-width="170"
          ><template #default="{ row }"
            >{{ row.scopeName || '待说明'
            }}<p class="note compact">{{ row.scopeCode }}</p></template
          ></el-table-column
        >
        <el-table-column label="缴费项目" min-width="130"
          ><template #default="{ row }"
            >{{ row.projectName
            }}<p v-if="row.customProjectCode" class="note compact">{{
              row.customProjectCode
            }}</p></template
          ></el-table-column
        >
        <el-table-column label="版本 / 状态" width="145"
          ><template #default="{ row }"
            >V{{ row.policyVersion }} ·
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
        <el-table-column prop="ownerName" label="负责人" min-width="100" />
        <el-table-column label="操作" min-width="255"
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
            <el-button v-if="canMaintain" link type="primary" @click="copyVersion(row)"
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
        description="暂无本地政策。登记实际出处、适用范围和参数后提交评审。"
      />
    </ContentWrap>
    <ContentWrap>
      <h3>声明基数核对</h3
      ><p class="note"
        >选择版本并声明期间及基数。一次核对按该政策明确的单位、缴费方式和舍入步骤计算；不会自动调整超限基数或按天数分摊。</p
      >
      <div class="lookup-grid">
        <label
          >政策版本<el-select
            v-model="lookup.policyId"
            aria-label="核对政策版本"
            placeholder="选择政策版本"
            @change="clearPreview"
            ><el-option
              v-for="r in rows"
              :key="r.id"
              :value="r.id!"
              :label="versionLabel(r)" /></el-select
        ></label>
        <label
          >开始日期<el-date-picker
            v-model="lookup.start"
            aria-label="核对开始日期"
            type="date"
            value-format="YYYY-MM-DD"
            @change="clearPreview"
        /></label>
        <label
          >结束日期<el-date-picker
            v-model="lookup.end"
            aria-label="核对结束日期"
            type="date"
            value-format="YYYY-MM-DD"
            @change="clearPreview"
        /></label>
        <label
          >声明基数（元）<el-input
            v-model="lookup.baseAmount"
            aria-label="声明基数（元）"
            inputmode="decimal"
            placeholder="明确填写基数，包括有效零值"
            @input="clearPreview"
        /></label>
      </div>
      <div class="actions"
        ><el-button :loading="resolving" @click="resolve">查找期间政策</el-button
        ><el-button type="primary" :loading="previewing" @click="preview"
          >核对缴费参数</el-button
        ></div
      >
      <el-alert v-if="lookupError" :title="lookupError" type="error" :closable="false" />
      <div v-if="matched" class="matched" data-testid="insurance-policy-match"
        ><el-alert
          :title="versionLabel(matched) + ' 完整覆盖声明期间'"
          type="success"
          :closable="false"
        /><el-button link type="primary" @click="useMatched">使用此版本核对</el-button></div
      >
      <div v-if="calculation" class="calculation" data-testid="insurance-policy-preview">
        <h3>{{ versionLabel(calculation.policy) }}</h3
        ><p
          >声明基数：{{ calculation.baseAmount }} ·
          {{ Api.units[calculation.policy.config?.baseUnit || ''] }}</p
        >
        <el-descriptions :column="1" border>
          <el-descriptions-item label="单位参照金额（元）">{{
            calculation.corporateAmount
          }}</el-descriptions-item>
          <el-descriptions-item label="单位计算依据"
            >{{ calculation.corporateExpression }} =
            {{ calculation.corporateRawAmount }}（舍入前）</el-descriptions-item
          >
          <el-descriptions-item label="单位舍入步骤">{{
            calculation.corporateSteps
          }}</el-descriptions-item>
          <el-descriptions-item label="个人参照金额（元）">{{
            calculation.personalAmount
          }}</el-descriptions-item>
          <el-descriptions-item label="个人计算依据"
            >{{ calculation.personalExpression }} =
            {{ calculation.personalRawAmount }}（舍入前）</el-descriptions-item
          >
          <el-descriptions-item label="个人舍入步骤">{{
            calculation.personalSteps
          }}</el-descriptions-item>
          <el-descriptions-item label="舍入口径"
            >{{ calculation.policy.config?.amountScale }} 位小数 ·
            {{ Api.roundings[calculation.policy.config?.roundingMode || ''] }} ·
            {{ Api.stages[calculation.policy.config?.roundingStage || ''] }}</el-descriptions-item
          > </el-descriptions
        ><p class="note">{{ calculation.explanation }}</p>
      </div>
    </ContentWrap>
    <ContentWrap>
      <h3>历史政策对比</h3
      ><p class="note"
        >筛选同城市、同范围、同项目，再选择两个版本；比较保留的参数和出处，空白与零值分开展示。</p
      >
      <div class="compare-grid"
        ><el-select v-model="leftId" aria-label="左侧政策版本" placeholder="左侧版本"
          ><el-option
            v-for="r in rows"
            :key="r.id"
            :value="r.id!"
            :label="versionLabel(r)" /></el-select
        ><el-select v-model="rightId" aria-label="右侧政策版本" placeholder="右侧版本"
          ><el-option
            v-for="r in rows"
            :key="r.id"
            :value="r.id!"
            :label="versionLabel(r)" /></el-select
        ><el-button :loading="comparing" @click="compare">比较政策</el-button></div
      >
      <el-alert v-if="compareError" :title="compareError" type="error" :closable="false" />
      <div v-if="comparison" data-testid="insurance-policy-comparison"
        ><p
          >V{{ comparison.left.policyVersion }} → V{{ comparison.right.policyVersion }} ·
          {{ comparison.changes.length }} 项差异</p
        >
        <el-table v-if="comparison.changes.length" :data="comparison.changes" border
          ><el-table-column label="变化" width="90"
            ><template #default="{ row }">{{
              kinds[row.kind] || row.kind
            }}</template></el-table-column
          ><el-table-column prop="label" label="政策参数" min-width="190" /><el-table-column
            label="左侧内容"
            min-width="230"
            ><template #default="{ row }">{{
              displayChange(row.path, row.left)
            }}</template></el-table-column
          ><el-table-column label="右侧内容" min-width="230"
            ><template #default="{ row }">{{
              displayChange(row.path, row.right)
            }}</template></el-table-column
          ></el-table
        ><el-empty v-else description="两个版本的政策参数和出处一致" />
      </div>
    </ContentWrap>
    <el-dialog
      v-model="editorVisible"
      :title="form.id ? '维护政策草稿' : '登记本地政策'"
      width="min(1040px, 96%)"
    >
      <el-alert
        v-if="editorError"
        :title="editorError"
        type="error"
        :closable="false"
        class="mb-3"
      />
      <el-form label-position="top" @submit.prevent>
        <div class="form-grid">
          <el-form-item label="参保城市 *"
            ><AreaSelect
              v-model="form.cityAreaId"
              :disabled="!!form.id"
              :check-strictly="true"
              :selectable-levels="[2]"
              placeholder="选择参保城市"
          /></el-form-item>
          <el-form-item label="缴费项目 *"
            ><el-select v-model="form.projectType" :disabled="!!form.id" @change="changeProject"
              ><el-option
                v-for="p in projects"
                :key="p.type"
                :label="p.name"
                :value="p.type" /></el-select
          ></el-form-item>
          <el-form-item label="适用范围编号 *"
            ><el-input
              v-model="form.scopeCode"
              :disabled="!!form.id"
              maxlength="64"
              placeholder="稳定大写编号，如实际人群或缴费分类"
          /></el-form-item>
          <el-form-item v-if="customProject" label="自定义项目编号 *"
            ><el-input
              v-model="form.customProjectCode"
              :disabled="!!form.id"
              maxlength="64"
              placeholder="同类自定义项目的稳定大写编号"
          /></el-form-item>
          <el-form-item label="政策名称 *"
            ><el-input v-model="form.title" maxlength="160"
          /></el-form-item>
          <el-form-item label="适用范围说明（确认必填）"
            ><el-input
              v-model="form.scopeName"
              maxlength="120"
              placeholder="明确实际地区内适用人群及条件"
          /></el-form-item>
          <el-form-item label="负责人（确认必填）"
            ><el-input v-model="form.ownerName" maxlength="120"
          /></el-form-item>
          <el-form-item label="生效开始（确认必填）"
            ><el-date-picker v-model="form.effectiveFrom" type="date" value-format="YYYY-MM-DD"
          /></el-form-item>
          <el-form-item label="生效结束（可选）"
            ><el-date-picker v-model="form.effectiveTo" type="date" value-format="YYYY-MM-DD"
          /></el-form-item>
        </div>
        <el-form-item label="政策出处与依据（确认必填）"
          ><el-input
            v-model="form.reference"
            type="textarea"
            :rows="3"
            maxlength="5000"
            placeholder="官方文件、年度、适用条款及业务核对依据"
        /></el-form-item>
        <el-form-item label="政策链接（可选）"
          ><el-input
            v-model="form.sourceUrl"
            maxlength="2048"
            placeholder="HTTP(S) 文件或页面地址；核对使用本地保留的参数"
        /></el-form-item>
        <h3>缴费参数</h3
        ><p class="note"
          >金额以元、比例以百分数填写，8 表示
          8%。缺失保留空白，合法零值需明确填写；参数不得只引用演示数字。</p
        >
        <div class="form-grid">
          <el-form-item label="基数下限（元，确认必填）"
            ><el-input v-model="form.config!.lowerBase" inputmode="decimal"
          /></el-form-item>
          <el-form-item label="基数上限（元，确认必填）"
            ><el-input v-model="form.config!.upperBase" inputmode="decimal"
          /></el-form-item>
          <el-form-item label="基数单位及周期（确认必填）"
            ><el-select v-model="form.config!.baseUnit" clearable
              ><el-option
                v-for="(label, key) in Api.units"
                :key="key"
                :label="label"
                :value="key" /></el-select
          ></el-form-item>
          <el-form-item label="单位缴费方式（确认必填）"
            ><el-select v-model="form.config!.corporateMode" clearable @change="changeCorporateMode"
              ><el-option
                v-for="(label, key) in Api.modes"
                :key="key"
                :label="label"
                :value="key" /></el-select
          ></el-form-item>
          <el-form-item label="单位缴费比例（%）"
            ><el-input
              v-model="form.config!.corporateRatePercent"
              inputmode="decimal"
              :disabled="!needsRate(form.config?.corporateMode)"
          /></el-form-item>
          <el-form-item label="单位固定额（元）"
            ><el-input
              v-model="form.config!.corporateFixedAmount"
              inputmode="decimal"
              :disabled="!needsFixed(form.config?.corporateMode)"
          /></el-form-item>
          <el-form-item label="个人缴费方式（确认必填）"
            ><el-select v-model="form.config!.personalMode" clearable @change="changePersonalMode"
              ><el-option
                v-for="(label, key) in Api.modes"
                :key="key"
                :label="label"
                :value="key" /></el-select
          ></el-form-item>
          <el-form-item label="个人缴费比例（%）"
            ><el-input
              v-model="form.config!.personalRatePercent"
              inputmode="decimal"
              :disabled="!needsRate(form.config?.personalMode)"
          /></el-form-item>
          <el-form-item label="个人固定额（元）"
            ><el-input
              v-model="form.config!.personalFixedAmount"
              inputmode="decimal"
              :disabled="!needsFixed(form.config?.personalMode)"
          /></el-form-item>
          <el-form-item label="金额小数位数（确认必填）"
            ><el-select v-model="form.config!.amountScale" clearable
              ><el-option
                v-for="n in 5"
                :key="n"
                :label="String(n - 1) + ' 位'"
                :value="n - 1" /></el-select
          ></el-form-item>
          <el-form-item label="舍入方式（确认必填）"
            ><el-select v-model="form.config!.roundingMode" clearable
              ><el-option
                v-for="(label, key) in Api.roundings"
                :key="key"
                :label="label"
                :value="key" /></el-select
          ></el-form-item>
          <el-form-item label="舍入步骤（确认必填）"
            ><el-select v-model="form.config!.roundingStage" clearable
              ><el-option
                v-for="(label, key) in Api.stages"
                :key="key"
                :label="label"
                :value="key" /></el-select
          ></el-form-item>
        </div>
      </el-form>
      <template #footer
        ><el-button @click="editorVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template
      >
    </el-dialog>
    <el-dialog
      v-model="reviewVisible"
      :title="reviewAction === 'confirm' ? '确认本地政策' : '停用本地政策'"
      width="min(680px,96%)"
    >
      <p>{{ reviewTarget?.title }} · V{{ reviewTarget?.policyVersion }}</p
      ><p class="note"
        >确认需核对出处、范围、参数及有效期；停用保留原参数和历史。评审不改变工资或缴费月账。</p
      >
      <el-alert
        v-if="reviewError"
        :title="reviewError"
        type="error"
        :closable="false"
        class="mb-3"
      /><el-form label-position="top"
        ><el-form-item label="评审依据 *"
          ><el-input v-model="evidence" type="textarea" :rows="4" maxlength="5000" /></el-form-item
      ></el-form>
      <template #footer
        ><el-button @click="reviewVisible = false">取消</el-button
        ><el-button type="primary" :loading="reviewing" @click="review"
          >提交评审</el-button
        ></template
      >
    </el-dialog>
    <el-drawer v-model="detailVisible" title="政策参数及评审历史" size="min(1080px,100%)">
      <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" />
      <template v-if="detail"
        ><h3>{{ versionLabel(detail) }}</h3
        ><el-tag>{{ Api.states[detail.status ?? 0] }}</el-tag>
        <p
          >{{ detail.cityName }} · {{ detail.scopeName }}（{{ detail.scopeCode }}） ·
          {{ detail.projectName }}</p
        ><p
          >有效期：{{ detail.effectiveFrom || '待确认' }} 至
          {{ detail.effectiveTo || '未指定结束' }}</p
        ><p>负责人：{{ detail.ownerName || '待填写' }}</p>
        <p class="multiline">政策出处：{{ detail.reference || '待补齐' }}</p
        ><el-link
          v-if="detail.sourceUrl"
          :href="detail.sourceUrl"
          target="_blank"
          rel="noopener noreferrer"
          >查看政策出处链接</el-link
        >
        <h3>保留的缴费参数</h3><Parameters :config="detail.config" /> <h3>评审历史</h3
        ><p>{{ detail.reviewedByName || '待评审' }} · {{ time(detail.reviewedTime) }}</p
        ><p class="multiline">{{ detail.evidence }}</p>
        <el-collapse
          ><el-collapse-item
            v-for="h in histories"
            :key="h.id"
            :name="h.id"
            :title="
              (actions[h.action] || h.action) + ' · ' + h.actorName + ' · ' + time(h.createTime)
            "
            ><p class="multiline">{{ h.reason }}</p
            ><Parameters :config="auditConfig(h)" /></el-collapse-item
        ></el-collapse>
      </template>
    </el-drawer>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import * as Api from '@/api/hrm/payroll/insurance'
import type { History } from '@/api/hrm/payroll/requirements'
import AreaSelect from '@/views/system/area/components/AreaSelect.vue'
import { formatDate } from '@/utils/formatTime'
import { checkPermi } from '@/utils/permission'
import Parameters from './Parameters.vue'
defineOptions({ name: 'HrmPayrollInsurancePolicy' })
const canMaintain = computed(() => checkPermi(['hrm:payroll:insurance-policy:maintain']))
const canReview = computed(() => checkPermi(['hrm:payroll:insurance-policy:review']))
const rows = ref<Api.Policy[]>([]),
  projects = ref<Api.Project[]>([]),
  total = ref(0),
  loading = ref(false),
  error = ref('')
const query = reactive<{
  pageNo: number
  pageSize: number
  scopeCode?: string
  projectType?: number
  search?: string
  status?: number
}>({ pageNo: 1, pageSize: 10 })
const editorVisible = ref(false),
  form = ref<Api.Policy>({ title: '', config: {} }),
  editorError = ref(''),
  saving = ref(false)
const reviewVisible = ref(false),
  reviewTarget = ref<Api.Policy>(),
  reviewAction = ref('confirm'),
  evidence = ref(''),
  reviewError = ref(''),
  reviewing = ref(false)
const detailVisible = ref(false),
  detail = ref<Api.Policy>(),
  histories = ref<History[]>([]),
  detailError = ref('')
const leftId = ref<number>(),
  rightId = ref<number>(),
  comparison = ref<Api.Comparison>(),
  compareError = ref(''),
  comparing = ref(false)
const lookup = reactive({
    policyId: undefined as number | undefined,
    start: '',
    end: '',
    baseAmount: ''
  }),
  matched = ref<Api.Policy>(),
  calculation = ref<Api.Preview>(),
  lookupError = ref(''),
  previewing = ref(false),
  resolving = ref(false)
const kinds: Record<string, string> = { ADDED: '新增', REMOVED: '移除', CHANGED: '变更' },
  actions: Record<string, string> = {
    create: '登记草稿',
    update: '维护草稿',
    'new-version': '另建版本',
    confirm: '确认',
    retire: '停用'
  }
const message = (e: unknown) =>
  e instanceof Error ? e.message : '操作未成功，请核对页面提示或刷新后重试。'
const time = (v?: number) => (v === null || v === undefined ? '' : formatDate(v))
const versionLabel = (r: Api.Policy) => 'V' + r.policyVersion + ' · ' + r.title
const customProject = computed(
  () => projects.value.find((p) => p.type === form.value.projectType)?.custom
)
const needsRate = (mode?: string | null) => mode === 'RATE' || mode === 'RATE_PLUS_FIXED'
const needsFixed = (mode?: string | null) => mode === 'FIXED' || mode === 'RATE_PLUS_FIXED'
const changeProject = () => {
  form.value.customProjectCode = undefined
}
const changeCorporateMode = () => {
  const c = form.value.config!
  if (!needsRate(c.corporateMode)) c.corporateRatePercent = undefined
  if (!needsFixed(c.corporateMode)) c.corporateFixedAmount = undefined
}
const changePersonalMode = () => {
  const c = form.value.config!
  if (!needsRate(c.personalMode)) c.personalRatePercent = undefined
  if (!needsFixed(c.personalMode)) c.personalFixedAmount = undefined
}
const clearPreview = () => {
  matched.value = undefined
  calculation.value = undefined
  lookupError.value = ''
}
const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const data = await Api.page({
      ...query,
      scopeCode: query.scopeCode || undefined,
      search: query.search || undefined
    })
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
  query.scopeCode = undefined
  query.projectType = undefined
  query.search = undefined
  query.status = undefined
  return search()
}
const openEditor = async (id?: number) => {
  editorError.value = ''
  try {
    form.value = id ? await Api.get(id) : { title: '', config: {} }
    form.value.config ||= {}
    editorVisible.value = true
  } catch (e) {
    error.value = message(e)
  }
}
const payload = () => {
  const f = form.value,
    c = { ...f.config }
  for (const key of [
    'lowerBase',
    'upperBase',
    'corporateRatePercent',
    'personalRatePercent',
    'corporateFixedAmount',
    'personalFixedAmount'
  ] as const) {
    const value = c[key]
    if (value === null || value === undefined || value.trim() === '') c[key] = null
    else {
      if (!/^\d+(\.\d+)?$/.test(value.trim()))
        throw new Error('金额和比例须填写非负十进制数，缺失可留空。')
      c[key] = value.trim()
    }
  }
  for (const key of [
    'baseUnit',
    'corporateMode',
    'personalMode',
    'roundingMode',
    'roundingStage'
  ] as const)
    if (!c[key]) c[key] = null
  return {
    id: f.id,
    revision: f.revision,
    cityAreaId: f.cityAreaId,
    scopeCode: f.scopeCode,
    scopeName: f.scopeName,
    projectType: f.projectType,
    customProjectCode: f.customProjectCode || null,
    title: f.title,
    ownerName: f.ownerName,
    reference: f.reference,
    sourceUrl: f.sourceUrl,
    effectiveFrom: f.effectiveFrom || null,
    effectiveTo: f.effectiveTo || null,
    config: c
  }
}
const save = async () => {
  editorError.value = ''
  if (
    !form.value.cityAreaId ||
    !form.value.scopeCode ||
    !form.value.projectType ||
    !form.value.title.trim()
  ) {
    editorError.value = '请选择城市及项目，填写范围编号和政策名称。'
    return
  }
  saving.value = true
  try {
    const data = payload()
    if (form.value.id) await Api.update(data)
    else await Api.create(data)
    editorVisible.value = false
    await load()
  } catch (e) {
    editorError.value = message(e)
  } finally {
    saving.value = false
  }
}
const copyVersion = async (row: Api.Policy) => {
  try {
    const id = await Api.newVersion(row.id!, row.revision!)
    await load()
    await openEditor(id)
  } catch (e) {
    error.value = message(e)
  }
}
const openReview = (row: Api.Policy, action: string) => {
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
  detail.value = undefined
  histories.value = []
  detailError.value = ''
  detailVisible.value = true
  try {
    const [record, history] = await Promise.all([Api.get(id), Api.history(id)])
    detail.value = record
    histories.value = history
  } catch (e) {
    detailError.value = message(e)
  }
}
const auditConfig = (h: History): Api.Config | undefined => {
  if (!h.afterSnapshot) return
  try {
    return JSON.parse(JSON.parse(h.afterSnapshot).configJson)
  } catch {
    return undefined
  }
}
const compare = async () => {
  compareError.value = ''
  comparison.value = undefined
  if (!leftId.value || !rightId.value) {
    compareError.value = '请选择两个政策版本。'
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
const displayChange = (path: string, value?: string) => {
  if (value === undefined || value === null) return '未设置'
  const names: Record<string, Record<string, string>> = {
    baseUnit: Api.units,
    corporateMode: Api.modes,
    personalMode: Api.modes,
    roundingMode: Api.roundings,
    roundingStage: Api.stages
  }
  return names[path]?.[value] || value
}
const resolve = async () => {
  lookupError.value = ''
  matched.value = undefined
  if (!lookup.policyId || !lookup.start || !lookup.end) {
    lookupError.value = '请选择政策系列的一个版本，并明确声明开始和结束日期。'
    return
  }
  resolving.value = true
  try {
    matched.value = await Api.resolve(lookup.policyId, lookup.start, lookup.end)
  } catch (e) {
    lookupError.value = message(e)
  } finally {
    resolving.value = false
  }
}
const useMatched = () => {
  if (matched.value) {
    lookup.policyId = matched.value.id
    calculation.value = undefined
    lookupError.value = ''
  }
}
const preview = async () => {
  lookupError.value = ''
  calculation.value = undefined
  if (
    !lookup.policyId ||
    !lookup.start ||
    !lookup.end ||
    !/^\d+(\.\d+)?$/.test(lookup.baseAmount.trim())
  ) {
    lookupError.value = '请选择明确版本及期间，填写声明基数；有效零值填写 0。'
    return
  }
  previewing.value = true
  try {
    calculation.value = await Api.preview({
      policyId: lookup.policyId,
      start: lookup.start,
      end: lookup.end,
      baseAmount: lookup.baseAmount.trim()
    })
  } catch (e) {
    lookupError.value = message(e)
  } finally {
    previewing.value = false
  }
}
onMounted(async () => {
  await load()
  try {
    projects.value = await Api.projects()
  } catch (e) {
    error.value = message(e)
  }
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
.compact {
  margin: 4px 0;
}
.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin: 18px 0;
}
.toolbar .el-input,
.toolbar .el-select {
  width: 190px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}
.compare-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 12px;
  margin: 16px 0;
}
.lookup-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.lookup-grid label {
  display: grid;
  gap: 8px;
}
.el-select,
.el-date-editor.el-input,
.el-cascader {
  width: 100%;
}
.actions {
  display: flex;
  gap: 12px;
  margin: 16px 0;
}
.matched {
  margin-top: 14px;
}
.calculation {
  margin-top: 20px;
}
.multiline {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
h3 {
  margin: 20px 0 12px;
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
  .actions {
    flex-wrap: wrap;
  }
}
</style>
