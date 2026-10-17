<template>
  <ContentWrap>
    <el-alert
      title="业务确认状态与数据就绪状态分别维护；确认需有效负责人、字段映射、来源、验收条件及依据，并由评审负责人本人操作。"
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
      <el-table-column prop="version" label="版本" width="70" />
      <el-table-column label="操作" width="140" fixed="right"
        ><template #default="{ row }">
          <el-button
            v-hasPermi="['hrm:payroll:requirement:update']"
            link
            type="primary"
            @click="edit(row)"
            >编辑</el-button
          >
          <el-button link @click="showHistory(row)">历史</el-button>
        </template></el-table-column
      >
    </el-table>
  </ContentWrap>
  <Dialog v-model="visible" title="需求评审" width="720px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="编号" prop="code"
        ><el-input v-model="form.code" placeholder="例如 PLAN-06"
      /></el-form-item>
      <el-form-item label="模块" prop="module"
        ><el-select v-model="form.module"
          ><el-option
            v-for="(label, key) in modules"
            :key="key"
            :label="label"
            :value="key" /></el-select
      ></el-form-item>
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
        ><el-select v-model="form.reviewerId" filterable clearable
          ><el-option
            v-for="user in users"
            :key="user.id"
            :value="user.id"
            :label="user.nickname" /></el-select
      ></el-form-item>
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
  <Dialog v-model="historyVisible" title="评审历史" width="800px">
    <el-table :data="histories">
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
  </Dialog>
</template>
<script setup lang="ts">
import { formatDate } from '@/utils/formatTime'
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
const message = useMessage()
const userStore = useUserStore()
const users = ref<UserVO[]>([])
const modules = payrollModules
const route = useRoute()
const router = useRouter()
const returnTarget = computed(() => payrollReturnTarget(route.query.from))
const businessTarget = computed(() =>
  module.value ? payrollModuleRoutes[module.value] : undefined
)
const expandedSources = ref<string[]>([])
const sources = computed(() =>
  sourcesForModules(module.value ? [module.value] : (Object.keys(modules) as PayrollModule[]))
)

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
  visible = ref(false),
  historyVisible = ref(false)
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
const load = async () => {
  loading.value = true
  try {
    rows.value = await api.listRequirements(module.value)
  } finally {
    loading.value = false
  }
}
const changeModule = () =>
  router.replace({ query: { ...route.query, module: module.value || undefined } })
watch(
  () => route.query.module,
  async (value) => {
    module.value = payrollModule(value)
    await load()
  }
)
const edit = (row?: PayrollRequirement) => {
  form.value = row ? { ...row } : empty()
  visible.value = true
}
const save = async () => {
  await formRef.value.validate()
  if (form.value.status === 'CONFIRMED') {
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
    await api.saveRequirement(form.value)
    visible.value = false
    await load()
    message.success('已保存')
  } finally {
    saving.value = false
  }
}
const initialize = async () => {
  saving.value = true
  try {
    const count = await api.initializeRequirements()
    message.success(`新增 ${count} 项需求`)
    await load()
  } finally {
    saving.value = false
  }
}
const exportList = async () => {
  download.excel(await api.exportRequirements(module.value), '薪酬需求评审.xlsx')
}
const showHistory = async (row: PayrollRequirement) => {
  histories.value = (await api.requirementHistory(row.id!)).map((item) => ({
    ...item,
    review: JSON.parse(item.snapshot)
  }))
  historyVisible.value = true
}
onMounted(async () => {
  await Promise.all([load(), getSimpleUserList().then((data) => (users.value = data))])
})
</script>
