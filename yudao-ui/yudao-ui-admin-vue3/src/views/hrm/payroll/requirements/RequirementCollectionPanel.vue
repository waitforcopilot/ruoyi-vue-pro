<template>
  <div class="collection">
    <ContentWrap>
      <div class="heading">
        <div
          ><h2>薪酬需求征集</h2
          ><p>逐项明确首期范围、验收条件和数据来源，保存每次评审的结论。</p></div
        >
        <el-button
          v-hasPermi="['hrm:payroll:requirements:create']"
          :loading="busy"
          @click="initialize"
          >登记原型候选</el-button
        >
      </div>
      <div class="metrics">
        <div
          ><span>候选需求</span><strong>{{ stats.total }}</strong
          ><small>首期纳入 {{ stats.mvp }} 项</small></div
        >
        <div
          ><span>已确认</span><strong>{{ stats.confirmed }}</strong
          ><small>确认率 {{ confirmRate }}%</small></div
        >
        <div
          ><span>待确认 / 异议</span><strong>{{ stats.pending }} / {{ stats.disputed }}</strong
          ><small>按实际评审记录统计</small></div
        >
        <div
          ><span>数据来源就绪</span
          ><strong>{{ stats.sourceReady }} / {{ stats.sourceTotal }}</strong
          ><small>与功能确认分别维护</small></div
        >
      </div>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false"
        ><el-button link @click="reload">重新加载</el-button></el-alert
      >
      <el-tabs v-model="tab">
        <el-tab-pane label="功能需求" name="requirements">
          <div class="module-picker">
            <el-button :type="moduleCode ? '' : 'primary'" @click="selectModule('')"
              >全部模块</el-button
            >
            <el-button
              v-for="module in Api.modules"
              :key="module.code"
              :type="moduleCode === module.code ? 'primary' : ''"
              @click="selectModule(module.code)"
              >{{ module.name }}</el-button
            >
          </div>
          <div class="filters">
            <el-input
              v-model="query.search"
              placeholder="搜索功能或编号"
              clearable
              @keyup.enter="search"
            />
            <el-select v-model="query.status" placeholder="评审状态" clearable @change="search"
              ><el-option
                v-for="(label, index) in Api.states"
                :key="index"
                :label="label"
                :value="index"
            /></el-select>
            <el-select v-model="query.priority" placeholder="优先级" clearable @change="search"
              ><el-option v-for="index in 3" :key="index" :label="'P' + index" :value="index"
            /></el-select>
            <el-select
              v-model="query.scopeDecision"
              placeholder="范围决定"
              clearable
              @change="search"
              ><el-option
                v-for="(label, index) in Api.scopes"
                :key="index"
                :label="label"
                :value="index"
            /></el-select>
            <el-button @click="search">查询</el-button>
            <el-button
              v-hasPermi="['hrm:payroll:requirements:create']"
              type="primary"
              @click="openRequirement()"
              >补充需求</el-button
            >
          </div>
          <el-tag v-if="query.sourceCode" closable class="source-filter" @close="clearSourceFilter"
            >关联来源：{{ query.sourceCode }}</el-tag
          >
          <el-table v-loading="loading" :data="rows" row-key="id" border>
            <el-table-column prop="code" label="编号" width="105" />
            <el-table-column label="模块" width="105"
              ><template #default="{ row }">{{
                moduleName(row.moduleCode)
              }}</template></el-table-column
            >
            <el-table-column prop="title" label="功能需求" min-width="280" show-overflow-tooltip />
            <el-table-column label="状态" width="95"
              ><template #default="{ row }"
                ><el-tag
                  :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'"
                  >{{ Api.states[row.status] }}</el-tag
                ></template
              ></el-table-column
            >
            <el-table-column prop="ownerName" label="负责人" width="120" />
            <el-table-column label="优先级" width="85"
              ><template #default="{ row }">{{
                row.priority ? 'P' + row.priority : '未设定'
              }}</template></el-table-column
            >
            <el-table-column label="范围" width="100"
              ><template #default="{ row }">{{
                Api.scopes[row.scopeDecision ?? 0]
              }}</template></el-table-column
            >
            <el-table-column label="数据来源" min-width="180"
              ><template #default="{ row }"
                ><el-button
                  v-for="code in row.sourceCodes"
                  :key="code"
                  link
                  type="primary"
                  @click="showSource(code)"
                  >{{ code }}</el-button
                ></template
              ></el-table-column
            >
            <el-table-column label="操作" width="220" fixed="right"
              ><template #default="{ row }">
                <el-button
                  v-hasPermi="['hrm:payroll:requirements:update']"
                  link
                  type="primary"
                  @click="openRequirement(row)"
                  >维护</el-button
                >
                <el-button
                  v-hasPermi="['hrm:payroll:requirements:review']"
                  link
                  type="primary"
                  @click="openReview(row)"
                  >评审</el-button
                >
                <el-button link @click="showHistory('requirement', row.id, row.code)"
                  >记录</el-button
                >
                <el-button
                  v-if="!row.builtIn"
                  v-hasPermi="['hrm:payroll:requirements:delete']"
                  link
                  type="danger"
                  @click="remove(row)"
                  >删除</el-button
                >
              </template></el-table-column
            >
          </el-table>
          <div v-if="!loading && !stats.total" class="empty-hint"
            >尚未登记候选需求。有登记权限的用户可导入原型清单；所有候选初始为待确认。</div
          >
          <Pagination
            v-model:page="query.pageNo"
            v-model:limit="query.pageSize"
            :total="total"
            @pagination="getList"
          />
        </el-tab-pane>
        <el-tab-pane label="数据来源" name="sources">
          <div class="tab-heading"
            ><p>登记实际系统、字段映射、负责人和核验依据；未核实的来源不会自动成为已就绪。</p
            ><el-button
              v-hasPermi="['hrm:payroll:source:update']"
              type="primary"
              @click="openSource()"
              >补充来源</el-button
            ></div
          >
          <el-table :data="sources" border row-key="id">
            <el-table-column prop="code" label="编号" width="110" /><el-table-column
              prop="name"
              label="数据来源"
              min-width="190"
            />
            <el-table-column prop="actualSystem" label="实际系统" min-width="140" /><el-table-column
              prop="ownerName"
              label="负责人"
              width="120"
            />
            <el-table-column label="就绪状态" width="110"
              ><template #default="{ row }"
                ><el-tag
                  :type="row.readiness === 1 ? 'success' : row.readiness === 2 ? 'warning' : 'info'"
                  >{{ Api.readinessLabels[row.readiness] }}</el-tag
                ></template
              ></el-table-column
            >
            <el-table-column
              prop="evidence"
              label="依据 / 缺口"
              min-width="180"
              show-overflow-tooltip
            />
            <el-table-column label="操作" width="240" fixed="right"
              ><template #default="{ row }"
                ><el-button
                  v-hasPermi="['hrm:payroll:source:update']"
                  link
                  type="primary"
                  @click="openSource(row)"
                  >维护</el-button
                ><el-button link @click="linkedRequirements(row.code)">关联需求</el-button
                ><el-button link @click="showHistory('source', row.id, row.code)"
                  >记录</el-button
                ></template
              ></el-table-column
            >
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="评审基线" name="baselines">
          <div class="tab-heading"
            ><p>生成基线会冻结当前全部需求及来源。下载旧基线始终保留当时的数据。</p
            ><el-button
              v-hasPermi="['hrm:payroll:requirements:export']"
              type="primary"
              :loading="busy"
              :disabled="!stats.total"
              @click="createBaseline"
              >生成评审基线</el-button
            ></div
          >
          <el-table :data="baselines" border>
            <el-table-column label="基线编号" width="140"
              ><template #default="{ row }">B{{ row.id }}</template></el-table-column
            >
            <el-table-column prop="requirementCount" label="需求数" /><el-table-column
              prop="sourceCount"
              label="来源数"
            />
            <el-table-column prop="exportedByName" label="生成人" /><el-table-column
              label="生成时间"
              min-width="180"
              ><template #default="{ row }">{{
                formatDate(row.createTime)
              }}</template></el-table-column
            >
            <el-table-column label="操作" width="160"
              ><template #default="{ row }"
                ><el-button
                  v-hasPermi="['hrm:payroll:requirements:export']"
                  link
                  type="primary"
                  :loading="downloading === row.id"
                  @click="exportBaseline(row.id)"
                  >下载 Excel</el-button
                ></template
              ></el-table-column
            >
          </el-table>
          <p class="muted">显示最近 100 个基线；下载含“需求评审”“数据来源”“基线说明”三个工作表。</p>
        </el-tab-pane>
      </el-tabs>
    </ContentWrap>

    <Dialog
      v-model="reqVisible"
      :title="reqForm.id ? '维护需求 · ' + reqForm.code : '补充需求'"
      width="min(900px, 95vw)"
      :scroll="true"
      :max-height="'65vh'"
    >
      <el-alert
        title="保存修改会进入待确认；原评审依据保留在版本记录中。"
        type="info"
        :closable="false"
        class="form-alert"
      />
      <el-form ref="reqFormRef" :model="reqForm" :rules="reqRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="稳定编号" prop="code"
            ><el-input
              v-model="reqForm.code"
              :disabled="!!reqForm.id"
              placeholder="CUSTOM-001"
              maxlength="64"
          /></el-form-item>
          <el-form-item label="所属模块" prop="moduleCode"
            ><el-select v-model="reqForm.moduleCode"
              ><el-option
                v-for="module in Api.modules"
                :key="module.code"
                :value="module.code"
                :label="module.name" /></el-select
          ></el-form-item>
          <el-form-item label="功能需求" prop="title" class="full"
            ><el-input
              v-model="reqForm.title"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
          /></el-form-item>
          <el-form-item label="补充说明" class="full"
            ><el-input v-model="reqForm.description" type="textarea" :rows="2" maxlength="4000"
          /></el-form-item>
          <el-form-item label="需求负责人"
            ><el-input v-model="reqForm.ownerName" maxlength="120"
          /></el-form-item>
          <el-form-item label="优先级"
            ><el-select v-model="reqForm.priority" clearable placeholder="由评审确定"
              ><el-option
                v-for="index in 3"
                :key="index"
                :value="index"
                :label="'P' + index" /></el-select
          ></el-form-item>
          <el-form-item label="范围决定"
            ><el-select v-model="reqForm.scopeDecision"
              ><el-option
                v-for="(label, index) in Api.scopes"
                :key="index"
                :value="index"
                :label="label" /></el-select
          ></el-form-item>
          <el-form-item label="适用主体与人群"
            ><el-input
              v-model="reqForm.applicableScope"
              placeholder="请按业务评审填写"
              maxlength="500"
          /></el-form-item>
          <el-form-item label="范围原因" class="full"
            ><el-input
              v-model="reqForm.scopeReason"
              type="textarea"
              :rows="2"
              placeholder="暂缓时必须填写原因"
              maxlength="2000"
          /></el-form-item>
          <el-form-item label="关联数据来源" class="full"
            ><el-select v-model="reqForm.sourceCodes" multiple filterable
              ><el-option
                v-for="source in sources"
                :key="source.code"
                :value="source.code"
                :label="source.code + ' · ' + source.name" /></el-select
          ></el-form-item>
          <el-form-item label="字段映射" class="full"
            ><el-input v-model="reqForm.fieldMapping" type="textarea" :rows="2" maxlength="4000"
          /></el-form-item>
          <el-form-item label="验收条件" class="full"
            ><el-input
              v-model="reqForm.acceptanceCriteria"
              type="textarea"
              :rows="3"
              placeholder="确认时必须填写可核对的业务结果"
              maxlength="4000"
          /></el-form-item>
          <el-form-item label="备注与未决问题" class="full"
            ><el-input v-model="reqForm.remark" type="textarea" :rows="2" maxlength="2000"
          /></el-form-item>
        </div>
        <p class="muted"
          >原始来源：{{ reqForm.origin || '业务补充'
          }}<span v-if="reqForm.id"> · 当前版本 v{{ reqForm.version }}</span></p
        >
      </el-form>
      <template #footer
        ><el-button @click="reqVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveRequirement"
          >保存需求</el-button
        ></template
      >
    </Dialog>

    <Dialog
      v-model="reviewVisible"
      :title="'评审需求 · ' + reviewForm.code"
      width="min(640px, 95vw)"
    >
      <el-form ref="reviewRef" :model="reviewForm" :rules="reviewRules" label-position="top">
        <p>{{ reviewForm.title }}</p
        ><el-form-item label="评审结论" prop="status"
          ><el-radio-group v-model="reviewForm.status"
            ><el-radio v-for="(label, index) in Api.states" :key="index" :value="index">{{
              label
            }}</el-radio></el-radio-group
          ></el-form-item
        >
        <el-form-item label="评审依据 / 未决问题" prop="evidence"
          ><el-input
            v-model="reviewForm.evidence"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
        /></el-form-item>
        <p class="muted"
          >确认需要负责人、验收条件和来源关联。异议需要处理负责人。评审人和时间由系统记录。</p
        > </el-form
      ><template #footer
        ><el-button @click="reviewVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveReview"
          >提交评审</el-button
        ></template
      >
    </Dialog>

    <Dialog
      v-model="sourceVisible"
      :title="sourceForm.id ? '数据来源 · ' + sourceForm.code : '补充数据来源'"
      width="min(800px, 95vw)"
      :scroll="true"
      :max-height="'65vh'"
    >
      <el-form ref="sourceRef" :model="sourceForm" :rules="sourceRules" label-position="top"
        ><div class="form-grid">
          <el-form-item label="稳定编号" prop="code"
            ><el-input
              v-model="sourceForm.code"
              :disabled="!!sourceForm.id"
              placeholder="DS-CUSTOM-001"
              maxlength="64"
          /></el-form-item>
          <el-form-item label="来源名称" prop="name"
            ><el-input v-model="sourceForm.name" maxlength="200"
          /></el-form-item>
          <el-form-item label="来源说明" class="full"
            ><el-input v-model="sourceForm.description" type="textarea" :rows="2" maxlength="4000"
          /></el-form-item>
          <el-form-item label="必需字段与唯一键" class="full"
            ><el-input
              v-model="sourceForm.requiredFields"
              type="textarea"
              :rows="2"
              maxlength="4000"
          /></el-form-item>
          <el-form-item label="实际系统"
            ><el-input v-model="sourceForm.actualSystem" maxlength="200" /></el-form-item
          ><el-form-item label="来源负责人"
            ><el-input v-model="sourceForm.ownerName" maxlength="120"
          /></el-form-item>
          <el-form-item label="字段映射、单位、模板及截止时间" class="full"
            ><el-input v-model="sourceForm.fieldMapping" type="textarea" :rows="4" maxlength="4000"
          /></el-form-item>
          <el-form-item label="就绪状态" class="full"
            ><el-radio-group v-model="sourceForm.readiness"
              ><el-radio
                v-for="(label, index) in Api.readinessLabels"
                :key="index"
                :value="index"
                >{{ label }}</el-radio
              ></el-radio-group
            ></el-form-item
          >
          <el-form-item label="核验依据 / 数据缺口" class="full"
            ><el-input v-model="sourceForm.evidence" type="textarea" :rows="3" maxlength="2000"
          /></el-form-item> </div
        ><p class="muted"
          >已就绪需要实际系统、负责人、字段映射和依据。待补齐需要说明缺口。</p
        ></el-form
      >
      <template #footer
        ><el-button @click="sourceVisible = false">关闭</el-button
        ><el-button
          v-hasPermi="['hrm:payroll:source:update']"
          type="primary"
          :loading="saving"
          @click="saveSource"
          >保存来源</el-button
        ></template
      >
    </Dialog>

    <el-drawer
      v-model="historyVisible"
      :title="'评审与变更记录 · ' + historyTitle"
      size="min(680px, 95vw)"
    >
      <div v-loading="historyLoading"
        ><el-empty v-if="!historyRows.length && !historyLoading" description="暂无记录" />
        <el-timeline
          ><el-timeline-item
            v-for="record in historyRows"
            :key="record.id"
            :timestamp="formatDate(record.createTime)"
          >
            <strong
              >{{ actionName(record.action) }} ·
              {{ record.fromVersion ? 'v' + record.fromVersion + ' → ' : '' }}v{{
                record.toVersion
              }}</strong
            >
            <p>{{ record.actorName }} · {{ record.reason }}</p>
            <el-collapse v-if="record.beforeSnapshot || record.afterSnapshot"
              ><el-collapse-item title="查看本次变更内容"
                ><el-table :data="changes(record)" size="small" border
                  ><el-table-column prop="field" label="字段" width="120" /><el-table-column
                    prop="before"
                    label="变更前"
                    min-width="140" /><el-table-column
                    prop="after"
                    label="变更后"
                    min-width="140" /></el-table></el-collapse-item
            ></el-collapse> </el-timeline-item
        ></el-timeline>
      </div>
    </el-drawer>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import * as Api from '@/api/hrm/payroll/requirements'
import type {
  Requirement,
  Source,
  Baseline,
  History,
  Summary
} from '@/api/hrm/payroll/requirements'
import { useMessage } from '@/hooks/web/useMessage'
import { formatDate } from '@/utils/formatTime'
import download from '@/utils/download'

defineOptions({ name: 'HrmRequirementCollectionPanel' })
const props = withDefaults(defineProps<{ moduleCode?: string; sourceCode?: string }>(), {
  moduleCode: '',
  sourceCode: ''
})
const emit = defineEmits<{
  moduleChange: [moduleCode: string]
  sourceChange: [sourceCode: string]
}>()
const message = useMessage()
const tab = ref('requirements')
const loading = ref(false),
  busy = ref(false),
  saving = ref(false)
const error = ref(''),
  downloading = ref<number>()
const rows = ref<Requirement[]>([]),
  sources = ref<Source[]>([]),
  baselines = ref<Baseline[]>([])
const total = ref(0)
const stats = ref<Summary>({
  total: 0,
  confirmed: 0,
  pending: 0,
  disputed: 0,
  mvp: 0,
  sourceTotal: 0,
  sourceReady: 0
})
const confirmRate = computed(() =>
  stats.value.total ? Math.round((stats.value.confirmed / stats.value.total) * 100) : 0
)
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  search: '',
  sourceCode: props.sourceCode,
  status: undefined as number | undefined,
  priority: undefined as number | undefined,
  scopeDecision: undefined as number | undefined
})
const moduleName = (code: string) => Api.modules.find((m) => m.code === code)?.name || code
const selectModule = (code: string) => emit('moduleChange', code)
const getList = async () => {
  loading.value = true
  try {
    const result = await Api.page({ ...query, moduleCode: props.moduleCode || undefined })
    rows.value = result.list
    total.value = result.total
    error.value = ''
  } catch {
    rows.value = []
    total.value = 0
    error.value = '需求加载失败，请重试。'
  } finally {
    loading.value = false
  }
}
const reload = async () => {
  try {
    ;[stats.value, sources.value, baselines.value] = await Promise.all([
      Api.summary(),
      Api.sources(),
      Api.baselines()
    ])
    await getList()
  } catch {
    error.value = '征集数据加载失败，请重新加载。'
  }
}
const search = () => {
  query.pageNo = 1
  return getList()
}
const clearSourceFilter = () => {
  query.sourceCode = ''
  emit('sourceChange', '')
  return search()
}
watch(() => props.moduleCode, search)
watch(
  () => props.sourceCode,
  (source) => {
    query.sourceCode = source
    search()
  }
)
onMounted(reload)
const initialize = async () => {
  await message.confirm(
    '登记 43 个原型功能、3 个征集功能和 12 类来源，均为待确认。已有修改将保留。'
  )
  busy.value = true
  try {
    const result = await Api.initialize()
    message.success(`新增 ${result.createdRequirements} 个需求、${result.createdSources} 类来源`)
    await reload()
  } finally {
    busy.value = false
  }
}
const reqVisible = ref(false),
  reqFormRef = ref<FormInstance>()
const newRequirement = (): Requirement => ({
  code: '',
  moduleCode: props.moduleCode || 'req',
  title: '',
  sourceCodes: [],
  scopeDecision: 0
})
const reqForm = ref<Requirement>(newRequirement())
const required = (name: string) => [{ required: true, message: `请填写${name}`, trigger: 'blur' }]
const reqRules: FormRules = {
  code: required('稳定编号'),
  moduleCode: required('所属模块'),
  title: required('功能需求')
}
const openRequirement = async (row?: Requirement) => {
  reqForm.value = row?.id ? await Api.get(row.id) : newRequirement()
  reqVisible.value = true
  reqFormRef.value?.clearValidate()
}
const saveRequirement = async () => {
  await reqFormRef.value?.validate()
  if (reqForm.value.scopeDecision === 2 && !reqForm.value.scopeReason?.trim()) {
    message.warning('暂缓必须填写范围原因')
    return
  }
  saving.value = true
  try {
    if (reqForm.value.id) await Api.update(reqForm.value)
    else await Api.create(reqForm.value)
    reqVisible.value = false
    message.success('需求已保存，待评审')
    await reload()
  } finally {
    saving.value = false
  }
}
const remove = async (row: Requirement) => {
  await message.confirm('删除该补充需求？原型候选通过范围决定维护，不能删除。')
  await Api.remove(row.id!, row.version!)
  message.success('已删除，变更记录已保留')
  await reload()
}
const reviewVisible = ref(false),
  reviewRef = ref<FormInstance>()
const reviewForm = ref({ id: 0, version: 1, code: '', title: '', status: 0, evidence: '' })
const reviewRules: FormRules = { evidence: required('评审依据') }
const openReview = async (row: Requirement) => {
  const current = await Api.get(row.id!)
  reviewForm.value = {
    id: current.id!,
    version: current.version!,
    code: current.code,
    title: current.title,
    status: current.status ?? 0,
    evidence: ''
  }
  reviewVisible.value = true
  reviewRef.value?.clearValidate()
}
const saveReview = async () => {
  await reviewRef.value?.validate()
  saving.value = true
  try {
    await Api.review(reviewForm.value)
    reviewVisible.value = false
    message.success('评审结论已记录')
    await reload()
  } finally {
    saving.value = false
  }
}
const sourceVisible = ref(false),
  sourceRef = ref<FormInstance>()
const sourceForm = ref<Source>({ code: '', name: '', readiness: 0 })
const sourceRules: FormRules = { code: required('稳定编号'), name: required('来源名称') }
const openSource = (source?: Source) => {
  sourceForm.value = source ? { ...source } : { code: '', name: '', readiness: 0 }
  sourceVisible.value = true
  sourceRef.value?.clearValidate()
}
const showSource = (code: string) => {
  const source = sources.value.find((s) => s.code === code)
  if (source) openSource(source)
}
const saveSource = async () => {
  await sourceRef.value?.validate()
  saving.value = true
  try {
    if (sourceForm.value.id) await Api.updateSource(sourceForm.value)
    else await Api.createSource(sourceForm.value)
    sourceVisible.value = false
    message.success('来源记录已保存')
    await reload()
  } finally {
    saving.value = false
  }
}
const linkedRequirements = (code: string) => {
  query.sourceCode = code
  query.search = ''
  query.status = undefined
  query.priority = undefined
  query.scopeDecision = undefined
  tab.value = 'requirements'
  emit('sourceChange', code)
  search()
}
const createBaseline = async () => {
  await message.confirm('将当前所有候选需求和数据来源冻结为新基线，未决项也会保留。')
  busy.value = true
  try {
    const id = await Api.createBaseline()
    message.success(`评审基线 B${id} 已生成`)
    baselines.value = await Api.baselines()
  } finally {
    busy.value = false
  }
}
const exportBaseline = async (id: number) => {
  downloading.value = id
  try {
    download.excel(await Api.exportBaseline(id), `薪酬评审基线-B${id}.xlsx`)
  } finally {
    downloading.value = undefined
  }
}
const historyVisible = ref(false),
  historyLoading = ref(false),
  historyTitle = ref('')
const historyRows = ref<History[]>([])
const showHistory = async (type: string, id: number, title: string) => {
  historyTitle.value = title
  historyRows.value = []
  historyVisible.value = true
  historyLoading.value = true
  try {
    historyRows.value = await Api.history(type, id)
  } finally {
    historyLoading.value = false
  }
}
const actionName = (action: string) =>
  ({ initialize: '登记候选', create: '新增', update: '维护', review: '评审', delete: '删除' })[
    action
  ] || action
const fields: Record<string, string> = {
  title: '功能需求',
  description: '说明',
  name: '来源名称',
  moduleCode: '模块',
  ownerName: '负责人',
  priority: '优先级',
  scopeDecision: '范围决定',
  scopeReason: '范围原因',
  applicableScope: '适用范围',
  sourceCodes: '数据来源',
  fieldMapping: '字段映射',
  acceptanceCriteria: '验收条件',
  remark: '备注 / 未决问题',
  status: '评审状态',
  evidence: '依据 / 缺口',
  actualSystem: '实际系统',
  requiredFields: '必需字段',
  readiness: '就绪状态'
}
const display = (key: string, value: unknown) => {
  if (value === undefined || value === null || value === '') return '—'
  if (key === 'status') return Api.states[Number(value)]
  if (key === 'scopeDecision') return Api.scopes[Number(value)]
  if (key === 'readiness') return Api.readinessLabels[Number(value)]
  if (key === 'priority') return 'P' + value
  if (key === 'moduleCode') return moduleName(String(value))
  return Array.isArray(value) ? value.join(', ') : String(value)
}
const changes = (record: History) => {
  const before = JSON.parse(record.beforeSnapshot || '{}'),
    after = JSON.parse(record.afterSnapshot || '{}')
  return Object.keys(fields)
    .filter((key) => JSON.stringify(before[key]) !== JSON.stringify(after[key]))
    .map((key) => ({
      field: fields[key],
      before: display(key, before[key]),
      after: display(key, after[key])
    }))
}
</script>
<style scoped>
h2 {
  margin: 0 0 8px;
  font-size: 22px;
}
p {
  line-height: 1.6;
}
.heading,
.tab-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.heading p,
.tab-heading p,
.muted {
  color: var(--el-text-color-secondary);
}
.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin: 20px 0;
}
.metrics > div {
  padding: 16px 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}
.metrics span,
.metrics small {
  display: block;
  color: var(--el-text-color-secondary);
}
.metrics strong {
  display: block;
  font-size: 28px;
  margin: 8px 0;
}
.module-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 4px 0 20px;
}
.module-picker .el-button + .el-button {
  margin-left: 0;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}
.filters .el-input {
  width: 240px;
}
.filters .el-select {
  width: 145px;
}
.source-filter,
.form-alert {
  margin-bottom: 16px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 20px;
}
.form-grid .full {
  grid-column: 1 / -1;
}
.form-grid .el-select {
  width: 100%;
}
.empty-hint {
  padding: 16px;
  color: var(--el-text-color-secondary);
  text-align: center;
}
@media (max-width: 768px) {
  .heading,
  .tab-heading {
    align-items: flex-start;
    flex-direction: column;
  }
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }
  .metrics > div {
    padding: 12px;
  }
  .metrics strong {
    font-size: 24px;
  }
  .filters .el-input {
    width: 100%;
  }
  .filters .el-select {
    width: calc(50% - 5px);
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .module-picker {
    gap: 6px;
  }
}
</style>
