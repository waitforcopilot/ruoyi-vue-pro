<template>
  <ProjectPanel :key="projectId || 0" :project-id="projectId" @changed="load" />
  <ContentWrap>
    <el-alert
      class="mb-4"
      :title="
        loading
          ? '正在加载需求及项目归属'
          : projectError
            ? '需求或项目归属加载失败，请刷新重试'
            : project
              ? `${project.name} · 项目编号 ${project.id} · ${project.projectScopeVersion == null ? (projectId == null ? '项目范围尚未定义，仅标识历史迁移归属' : '范围尚未定义，不能确认') : '范围版本 ' + project.projectScopeVersion}`
              : '尚无历史迁移项目，首次保存需求时建立归属'
      "
      :type="projectError ? 'error' : 'info'"
      :closable="false"
    />
    <el-alert
      title="业务确认状态与数据就绪状态分别维护；确认需有效负责人、字段映射、来源、验收条件及依据；请先保存，再由评审负责人本人点击“确认版本”。"
      type="info"
      :closable="false"
    />
    <el-space class="my-4" wrap>
      <el-button v-if="returnTarget" @click="router.push(returnTarget)">返回业务模块</el-button>
      <el-button v-else-if="businessTarget" @click="router.push(businessTarget)"
        >进入业务模块</el-button
      >
      <el-select
        v-model="module"
        clearable
        placeholder="全部模块"
        style="width: 180px"
        @change="changeModule"
      >
        <el-option v-for="(label, key) in modules" :key="key" :label="label" :value="key" />
      </el-select>
      <el-button @click="load">刷新</el-button>
      <el-button v-hasPermi="['hrm:payroll:requirement:update']" type="primary" @click="edit()"
        >新增需求</el-button
      >
      <el-button
        v-hasPermi="['hrm:payroll:requirement:update']"
        :loading="saving"
        @click="initialize"
        >初始化 PRD 清单</el-button
      >
      <el-button v-hasPermi="['hrm:payroll:requirement:export']" @click="exportList"
        >导出评审清单</el-button
      >
    </el-space>
    <el-collapse v-model="expandedSources" class="mb-4">
      <el-collapse-item
        name="sources"
        :title="`${module ? modules[module] : '全部模块'}候选数据源与字段`"
      >
        <div class="mb-2"
          >以下为候选目录；实际来源、字段映射、责任人及依据由评审人员维护，需求确认与数据就绪分别核实。</div
        >
        <el-table :data="sources" stripe>
          <el-table-column prop="id" label="来源编号" width="100" />
          <el-table-column prop="name" label="候选来源" min-width="180" />
          <el-table-column label="必要字段" min-width="320"
            ><template #default="{ row }">{{ row.fields.join('、') }}</template></el-table-column
          >
          <el-table-column prop="owner" label="候选确认方" min-width="160" />
        </el-table>
      </el-collapse-item>
    </el-collapse>
    <el-table data-testid="payroll-requirements" v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="需求ID" width="85" />
      <el-table-column prop="code" label="编号" width="110" />
      <el-table-column label="模块" width="130"
        ><template #default="{ row }">{{
          modules[row.module] || row.module
        }}</template></el-table-column
      >
      <el-table-column prop="description" label="功能" min-width="250" />
      <el-table-column label="评审状态" width="110"
        ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
      >
      <el-table-column label="数据就绪" width="110"
        ><template #default="{ row }">{{ readiness[row.readiness] }}</template></el-table-column
      >
      <el-table-column prop="sourceSystem" label="数据来源" min-width="160" />
      <el-table-column label="范围适配" width="150"
        ><template #default="{ row }">{{
          scopeStates[row.scopeState] || '尚未定义'
        }}</template></el-table-column
      >
      <el-table-column prop="version" label="版本" width="70" />
      <el-table-column label="操作" width="280" fixed="right"
        ><template #default="{ row }">
          <el-button
            v-hasPermi="['hrm:payroll:requirement:update']"
            link
            type="primary"
            :disabled="row.permittedActions && !row.permittedActions.includes('EDIT')"
            @click="edit(row)"
            >编辑</el-button
          >
          <el-button link @click="showHistory(row)">历史</el-button>
          <el-button
            v-hasPermi="['hrm:payroll:requirement:assign']"
            link
            type="primary"
            :disabled="
              loading ||
              saving ||
              confirming ||
              (row.permittedActions && !row.permittedActions.includes('ASSIGN'))
            "
            @click="openAssignment(row)"
            >分派</el-button
          >
          <el-button
            v-if="['PENDING', 'REVIEWING', 'DISPUTED'].includes(row.status)"
            v-hasPermi="['hrm:payroll:requirement:confirm']"
            link
            type="primary"
            :disabled="
              loading ||
              saving ||
              confirming ||
              row.reviewerId !== userStore.getUser.id ||
              (row.permittedActions && !row.permittedActions.includes('CONFIRM'))
            "
            @click="confirmVersion(row)"
            >确认版本</el-button
          >
        </template></el-table-column
      >
    </el-table>
  </ContentWrap>
  <Dialog v-model="assignmentVisible" title="分派评审负责人" width="min(540px, calc(100vw - 24px))">
    <p
      >需求ID {{ assignmentRow?.id }}（{{ assignmentRow?.code }}）· 已保存版本
      {{ assignmentRow?.version }}</p
    >
    <el-alert
      title="只修改评审负责人；已确认需求改派后须重新评审。清空将取消当前分派。不可分派给自己，目标须有需求查询和确认权限。"
      type="warning"
      :closable="false"
      class="mb-4"
    />
    <el-form label-width="100px">
      <el-form-item label="新评审负责人">
        <el-select
          v-model="assignmentReviewerId"
          filterable
          clearable
          placeholder="选择评审人；留空取消分派"
        >
          <el-option
            v-for="user in assignmentUsers"
            :key="user.id"
            :value="user.id"
            :label="user.nickname"
          />
        </el-select>
      </el-form-item>
      <el-button :disabled="saving" @click="assignmentReviewerId = undefined">清空负责人</el-button>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="assignmentVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitAssignment">保存分派</el-button>
    </template>
  </Dialog>
  <Dialog
    v-model="visible"
    title="需求评审"
    width="min(720px, calc(100vw - 24px))"
    top="16px"
    scroll
    max-height="min(650px, calc(100dvh - 190px))"
  >
    <el-alert
      v-if="reviewRevision"
      class="mb-4"
      title="已确认需求的关键内容已修改；本次保存将转为评审中并清除当前签字，请保存后再重新确认。"
      type="warning"
      :closable="false"
    />
    <el-form
      ref="formRef"
      class="requirement-form"
      :model="form"
      :rules="rules"
      label-width="120px"
      :label-position="width < 600 ? 'top' : 'right'"
    >
      <el-form-item label="编号" prop="code"
        ><el-input v-model="form.code" :disabled="!!projectId" placeholder="新项目编号由服务器分配"
      /></el-form-item>
      <el-form-item label="模块" prop="module"
        ><el-select v-model="form.module"
          ><el-option
            v-for="(label, key) in modules"
            :key="key"
            :label="label"
            :value="key" /></el-select
      ></el-form-item>
      <template v-if="projectId">
        <el-form-item label="范围方式"
          ><el-select v-model="form.scopeMode"
            ><el-option label="继承项目范围" value="INHERIT" /><el-option
              label="显式子集"
              value="SUBSET" /></el-select
        ></el-form-item>
        <ScopeFields v-if="form.scopeMode === 'SUBSET'" v-model="form.scopeJson" />
      </template>
      <el-form-item label="功能说明" prop="description"
        ><el-input v-model="form.description" type="textarea" :maxlength="1000"
      /></el-form-item>
      <el-form-item label="字段映射"
        ><el-input
          v-model="form.fieldMapping"
          type="textarea"
          :maxlength="4000"
          placeholder="来源字段 → 本项目字段；不适用请填写：不适用：具体理由"
      /></el-form-item>
      <el-form-item label="来源系统"
        ><el-input v-model="form.sourceSystem" :maxlength="1000"
      /></el-form-item>
      <el-form-item label="来源负责人"
        ><el-select v-model="form.sourceOwnerId" filterable clearable
          ><el-option
            v-for="user in users"
            :key="user.id"
            :value="user.id"
            :label="user.nickname" /></el-select
      ></el-form-item>
      <el-form-item label="评审负责人"
        ><el-select v-model="form.reviewerId" disabled filterable clearable
          ><el-option
            v-for="user in users"
            :key="user.id"
            :value="user.id"
            :label="user.nickname" /></el-select
      ></el-form-item>
      <el-alert
        class="mb-4"
        title="请先保存需求，再通过列表中的分派操作修改评审负责人；不可自派，被分派人须有需求查询和确认权限。"
        type="info"
        :closable="false"
      />
      <el-form-item label="优先级"
        ><el-select v-model="form.priority"
          ><el-option :value="1" label="高" /><el-option :value="2" label="中" /><el-option
            :value="3"
            label="低" /></el-select
      ></el-form-item>
      <el-form-item label="评审状态"
        ><el-select v-model="form.status"
          ><el-option
            v-for="(label, key) in states"
            :disabled="key === 'CONFIRMED'"
            :key="key"
            :label="label"
            :value="key" /></el-select
      ></el-form-item>
      <el-form-item label="数据就绪"
        ><el-select v-model="form.readiness"
          ><el-option
            v-for="(label, key) in readiness"
            :key="key"
            :label="label"
            :value="key" /></el-select
      ></el-form-item>
      <el-form-item label="验收条件"
        ><el-input v-model="form.acceptance" type="textarea" :maxlength="2000"
      /></el-form-item>
      <el-form-item label="依据 / 异议"
        ><el-input v-model="form.evidence" type="textarea" :maxlength="2000"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save">保存</el-button></template
    >
  </Dialog>
  <Dialog
    v-model="historyVisible"
    :title="
      historyIdentity
        ? `评审历史 · 需求ID ${historyIdentity.id} · ${historyIdentity.code}`
        : '评审历史'
    "
    width="min(800px, calc(100vw - 24px))"
    top="16px"
    scroll
    max-height="min(650px, calc(100dvh - 190px))"
  >
    <div v-if="historyIdentity" class="mb-3">
      当前项目编号：{{ historyIdentity.projectId ?? '尚未归属' }}。
      以下项目编号按当时快照展示；旧快照未记录时不推定归属。
    </div>
    <el-table :data="histories">
      <el-table-column label="范围适配" width="150"
        ><template #default="{ row }">{{
          scopeStates[row.review.scopeState] || '历史未记录'
        }}</template></el-table-column
      >
      <el-table-column prop="version" label="版本" width="70" />
      <el-table-column label="操作人" width="100"
        ><template #default="{ row }">{{
          row.actorId === 0
            ? '系统校验'
            : users.find((u) => u.id === row.actorId)?.nickname || row.actorId
        }}</template></el-table-column
      >
      <el-table-column label="时间" width="180"
        ><template #default="{ row }">{{ formatDate(row.createTime) }}</template></el-table-column
      >
      <el-table-column label="评审内容" min-width="300"
        ><template #default="{ row }">
          <div>快照需求ID：{{ row.review.id ?? '历史未记录' }}</div>
          <div>快照项目编号：{{ row.review.projectId ?? '历史未记录' }}</div>
          <div>当时编号：{{ row.review.code || '历史未记录' }}</div>
          <div
            >当时范围版本：{{ row.review.projectScopeVersion ?? '历史未定义' }} /
            {{ row.review.scopeMode || '历史未记录' }}</div
          >
          <div v-if="row.review.confirmedBy"
            >当时确认人：{{ row.review.confirmedBy }}；签署范围版本：{{
              row.review.confirmedScopeVersion ?? '历史未记录'
            }}</div
          >
          <div>{{ row.review.description }}</div
          ><div>{{ states[row.review.status] }} / {{ readiness[row.review.readiness] }}</div>
          <div>来源：{{ row.review.sourceSystem || '未填写' }}</div>
          <div>映射：{{ row.review.fieldMapping || '未填写' }}</div>
          <div>验收：{{ row.review.acceptance || '未填写' }}</div
          ><div>依据：{{ row.review.evidence || '未填写' }}</div>
          <div v-if="row.review.validationReason">系统校验：{{ row.review.validationReason }}</div>
        </template></el-table-column
      >
    </el-table>
    <template #footer><el-button @click="historyVisible = false">关闭评审历史</el-button></template>
  </Dialog>
</template>
<script setup lang="ts">
import ProjectPanel from './ProjectPanel.vue'
import ScopeFields from './ScopeFields.vue'
import { useWindowSize } from '@vueuse/core'
import { formatDate } from '@/utils/formatTime'
import { checkPermi } from '@/utils/permission'
import { useUserStore } from '@/store/modules/user'
import {
  payrollModules,
  payrollModule,
  payrollModuleRoutes,
  payrollReturnTarget,
  sourcesForModules,
  type PayrollModule
} from '../requirements'
import * as api from '@/api/hrm/payroll/requirement'
import type { PayrollRequirement } from '@/api/hrm/payroll/requirement'
import download from '@/utils/download'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
defineOptions({ name: 'HrmPayrollRequirement' })
const { width } = useWindowSize()
const message = useMessage()
const userStore = useUserStore()
const users = ref<UserVO[]>([])
const modules = payrollModules
const route = useRoute()
const router = useRouter()
const projectId = computed(() => {
  if (route.query.projectId == null) return undefined
  const n = Number(route.query.projectId)
  return Number.isSafeInteger(n) && n > 0 ? n : -1
})
const returnTarget = computed(() => payrollReturnTarget(route.query.from))
const businessTarget = computed(() =>
  module.value ? payrollModuleRoutes[module.value] : undefined
)
const expandedSources = ref<string[]>([])
const sources = computed(() =>
  sourcesForModules(module.value ? [module.value] : (Object.keys(modules) as PayrollModule[]))
)

const scopeStates: Record<string, string> = {
  MATCH: '匹配',
  UNDEFINED: '尚未定义',
  SCOPE_MISMATCH: '范围不匹配'
}
const states: Record<string, string> = {
  PENDING: '待确认',
  REVIEWING: '评审中',
  CONFIRMED: '已确认',
  DISPUTED: '有异议',
  DEFERRED: '暂缓'
}
const readiness: Record<string, string> = {
  MISSING: '未就绪',
  PARTIAL: '部分就绪',
  READY: '已就绪',
  EXEMPT: '不适用'
}
const rows = ref<PayrollRequirement[]>([])
const module = ref<PayrollModule | undefined>(payrollModule(route.query.module))
const loading = ref(false),
  saving = ref(false),
  confirming = ref(false),
  visible = ref(false),
  historyVisible = ref(false)
const historyIdentity = ref<Pick<PayrollRequirement, 'id' | 'code' | 'projectId'>>()
const histories = ref<any[]>([]),
  formRef = ref()
const empty = (): PayrollRequirement => ({
  code: '',
  module: module.value || 'OV',
  description: '',
  priority: 2,
  status: 'PENDING',
  readiness: 'MISSING'
})
const form = ref<PayrollRequirement>(empty())
const rules = {
  code: [
    { required: true, message: '填写编号' },
    { pattern: /^[A-Z][A-Z0-9]*-[0-9]{2,6}$/, message: '编号格式如 PLAN-06' }
  ],
  module: [{ required: true, message: '选择模块' }],
  description: [{ required: true, message: '填写功能说明' }]
}
const project = ref<api.PayrollRequirementProject | null>(null)
const projectError = ref(false)
let loadVersion = 0
const load = async () => {
  const ticket = ++loadVersion,
    selectedProject = projectId.value,
    selectedModule = module.value
  loading.value = true
  try {
    projectError.value = false
    const [data, metadata] = await Promise.all([
      api.listRequirements(selectedModule, selectedProject),
      selectedProject
        ? api.listRequirementProjects().then((items) => {
            const p = items.find((x) => x.id === selectedProject)
            if (!p) throw new Error('项目不可访问')
            return p
          })
        : api.getLegacyRequirementProject()
    ])
    if (ticket !== loadVersion || selectedProject !== projectId.value) return
    rows.value = data
    project.value = metadata
  } catch (error) {
    if (ticket !== loadVersion || selectedProject !== projectId.value) return
    projectError.value = true
    project.value = null
    rows.value = []
    throw error
  } finally {
    if (ticket === loadVersion) loading.value = false
  }
}
const changeModule = () =>
  router.replace({ query: { ...route.query, module: module.value || undefined } })
watch(
  () => [route.query.module, route.query.projectId],
  async () => {
    module.value = payrollModule(route.query.module)
    visible.value = false
    assignmentVisible.value = false
    await load()
  }
)
const originalReview = ref<PayrollRequirement>()
const reviewFields = [
  'scopeMode',
  'scopeJson',
  'code',
  'module',
  'description',
  'fieldMapping',
  'sourceSystem',
  'sourceOwnerId',
  'reviewerId',
  'acceptance',
  'evidence',
  'priority'
] as const
const reviewRevision = computed(
  () =>
    originalReview.value?.status === 'CONFIRMED' &&
    reviewFields.some(
      (field) => (originalReview.value?.[field] ?? null) !== (form.value[field] ?? null)
    )
)
const edit = (row?: PayrollRequirement) => {
  originalReview.value = row ? { ...row } : undefined
  form.value = row
    ? { ...row }
    : {
        ...empty(),
        projectId: projectId.value,
        scopeMode: projectId.value ? 'INHERIT' : undefined,
        projectScopeVersion: project.value?.projectScopeVersion
      }
  if (projectId.value && !row) form.value.code = 'CUSTOM-0000'
  visible.value = true
}
const save = async () => {
  await formRef.value.validate()
  if (form.value.status === 'CONFIRMED' && originalReview.value?.status !== 'CONFIRMED') {
    message.warning('请先保存需求，再使用列表中的确认版本操作')
    return
  }
  if (form.value.status === 'CONFIRMED' && !reviewRevision.value) {
    if (!form.value.fieldMapping?.trim()) {
      message.warning('请填写字段映射；不适用时需说明具体理由')
      return
    }
    if (String(form.value.reviewerId) !== String(userStore.getUser.id)) {
      message.warning('请由所选评审负责人登录后本人确认')
      return
    }
  }
  saving.value = true
  try {
    const revising = reviewRevision.value && form.value.status === 'CONFIRMED'
    await api.saveRequirement(revising ? { ...form.value, status: 'REVIEWING' } : form.value)
    visible.value = false
    await load()
    message.success(revising ? '修订已保存为评审中，请重新确认' : '已保存')
  } finally {
    saving.value = false
  }
}
const assignmentVisible = ref(false)
const assignmentRow = ref<PayrollRequirement>()
const assignmentReviewerId = ref<number>()
let assignmentContext: { path: string; module?: string } | undefined
const eligibleReviewers = ref<number[] | undefined>()
const assignmentUsers = computed(() =>
  users.value.filter((u) => !eligibleReviewers.value || eligibleReviewers.value.includes(u.id))
)
const openAssignment = async (row: PayrollRequirement) => {
  if (
    !confirmationActive ||
    loading.value ||
    saving.value ||
    confirming.value ||
    !row.id ||
    !row.version ||
    !checkPermi(['hrm:payroll:requirement:assign'])
  )
    return
  eligibleReviewers.value = projectId.value
    ? await api.eligibleRequirementReviewers(projectId.value, row.id)
    : undefined
  assignmentRow.value = { ...row }
  assignmentReviewerId.value = row.reviewerId
  assignmentContext = { path: route.fullPath, module: module.value }
  assignmentVisible.value = true
}
const submitAssignment = async () => {
  const row = assignmentRow.value
  const current = () =>
    confirmationActive &&
    assignmentVisible.value &&
    assignmentContext?.path === route.fullPath &&
    assignmentContext?.module === module.value &&
    rows.value.some((item) => item.id === row?.id && item.version === row?.version)
  if (!row?.id || !row.version || loading.value || saving.value || confirming.value || !current())
    return
  const reviewerId = assignmentReviewerId.value
  if (reviewerId === userStore.getUser.id && reviewerId !== row.reviewerId) {
    message.warning('不能分派给自己')
    return
  }
  saving.value = true
  try {
    await api.assignRequirement(
      row.id,
      row.version,
      reviewerId,
      projectId.value,
      project.value?.projectScopeVersion
    )
    if (!current()) return
    assignmentVisible.value = false
    await load()
    message.success('分派已保存；请由当前评审负责人确认版本')
  } finally {
    saving.value = false
  }
}
let confirmationActive = true
onActivated(() => {
  confirmationActive = true
})
onDeactivated(() => {
  confirmationActive = false
  assignmentVisible.value = false
})
onBeforeUnmount(() => {
  confirmationActive = false
  assignmentVisible.value = false
})
const confirmVersion = async (row: PayrollRequirement) => {
  if (
    !confirmationActive ||
    loading.value ||
    saving.value ||
    confirming.value ||
    !row.id ||
    !row.version ||
    row.reviewerId !== userStore.getUser.id ||
    !['PENDING', 'REVIEWING', 'DISPUTED'].includes(row.status)
  )
    return
  const id = row.id,
    version = row.version,
    selectedModule = module.value,
    path = route.fullPath
  const current = () =>
    confirmationActive &&
    path === route.fullPath &&
    selectedModule === module.value &&
    rows.value.some(
      (item) =>
        item.id === id &&
        item.version === version &&
        item.status === row.status &&
        item.reviewerId === row.reviewerId
    )
  confirming.value = true
  try {
    try {
      await message.confirm(
        `确认需求ID ${id}（${row.code}）的已保存版本 ${version}？确认不会保存表单中的其他修改。`,
        '确认需求版本'
      )
    } catch (error) {
      if (error === 'cancel' || error === 'close') return
      throw error
    }
    if (!current()) return
    await api.confirmRequirement(id, version, projectId.value, project.value?.projectScopeVersion)
    if (!current()) return
    message.success('该版本已确认')
    await load()
  } finally {
    confirming.value = false
  }
}
const initialize = async () => {
  saving.value = true
  try {
    const count = await api.initializeRequirements(projectId.value)
    message.success(`新增 ${count} 项需求`)
    await load()
  } finally {
    saving.value = false
  }
}
const exportList = async () => {
  download.excel(await api.exportRequirements(module.value, projectId.value), '薪酬需求评审.xlsx')
}
const showHistory = async (row: PayrollRequirement) => {
  const identity = { id: row.id, code: row.code, projectId: row.projectId }
  histories.value = (await api.requirementHistory(row.id!, projectId.value)).map((item) => ({
    ...item,
    review: JSON.parse(item.snapshot)
  }))
  historyIdentity.value = identity
  historyVisible.value = true
}
onMounted(async () => {
  await Promise.all([load(), getSimpleUserList().then((data) => (users.value = data))])
})
</script>

<style scoped>
.requirement-form :deep(.el-select) {
  width: 100%;
}
.requirement-form {
  padding-right: 12px;
}
</style>
