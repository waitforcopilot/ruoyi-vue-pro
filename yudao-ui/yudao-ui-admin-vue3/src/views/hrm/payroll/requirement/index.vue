<template>
  <ContentWrap>
    <el-alert
      title="业务确认状态与数据就绪状态分别维护；确认需求需填写负责人、来源、验收条件及依据。"
      type="info"
      :closable="false"
    />
    <el-space class="my-4" wrap>
      <el-select
        v-model="module"
        clearable
        placeholder="全部模块"
        style="width: 180px"
        @change="load"
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
    <el-table v-loading="loading" :data="rows" stripe>
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
        ><el-input v-model="form.fieldMapping" type="textarea" :maxlength="4000"
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
          users.find((u) => u.id === row.actorId)?.nickname || row.actorId
        }}</template></el-table-column
      >
      <el-table-column prop="createTime" label="时间" width="180" />
      <el-table-column label="评审内容" min-width="300"
        ><template #default="{ row }">
          <div>{{ row.review.description }}</div
          ><div>{{ states[row.review.status] }} / {{ readiness[row.review.readiness] }}</div>
          <div>来源：{{ row.review.sourceSystem || '未填写' }}</div>
          <div>映射：{{ row.review.fieldMapping || '未填写' }}</div>
          <div>验收：{{ row.review.acceptance || '未填写' }}</div
          ><div>依据：{{ row.review.evidence || '未填写' }}</div>
        </template></el-table-column
      >
    </el-table>
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/requirement'
import type { PayrollRequirement } from '@/api/hrm/payroll/requirement'
import download from '@/utils/download'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
defineOptions({ name: 'HrmPayrollRequirement' })
const message = useMessage()
const users = ref<UserVO[]>([])
const modules: Record<string, string> = {
  OV: '薪酬总览',
  PLAN: '薪酬方案',
  CALC: '工资核算',
  TAX: '个税与工资条',
  ATT: '考勤',
  OT: '加班',
  HOUR: '工时',
  INS: '社保公积金',
  RPT: '报表分析',
  COL: '需求征集'
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
const module = ref<string>()
const loading = ref(false),
  saving = ref(false),
  visible = ref(false),
  historyVisible = ref(false)
const histories = ref<any[]>([]),
  formRef = ref()
const empty = (): PayrollRequirement => ({
  code: '',
  module: 'OV',
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
const edit = (row?: PayrollRequirement) => {
  form.value = row ? { ...row } : empty()
  visible.value = true
}
const save = async () => {
  await formRef.value.validate()
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
