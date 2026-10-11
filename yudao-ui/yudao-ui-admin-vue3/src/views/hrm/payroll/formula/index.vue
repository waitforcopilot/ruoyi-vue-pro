<template>
  <ContentWrap>
    <el-alert
      title="公式按核算期间起始日选取版本。已批准规则不可修改；变更请新增生效版本，由其他人员审批。"
      type="info"
      :closable="false"
      class="mb-4"
    />
    <el-button v-hasPermi="['hrm:payroll:formula:update']" type="primary" @click="openCreate"
      >新增公式版本</el-button
    >
    <el-table v-loading="loading" :data="rows">
      <el-table-column label="工资项"
        ><template #default="{ row }">{{ itemName(row.optionCode) }}</template></el-table-column
      >
      <el-table-column label="适用范围"
        ><template #default="{ row }">{{ groupName(row.salaryGroupId) }}</template></el-table-column
      >
      <el-table-column prop="expression" label="公式" min-width="220" />
      <el-table-column prop="effectiveFrom" label="生效日" width="120" />
      <el-table-column prop="effectiveTo" label="失效日" width="120" />
      <el-table-column prop="evidence" label="政策依据" show-overflow-tooltip />
      <el-table-column label="状态" width="100"
        ><template #default="{ row }">{{
          row.status === 'APPROVED' ? '已批准' : '待审批'
        }}</template></el-table-column
      >
      <el-table-column label="操作" width="150"
        ><template #default="{ row }">
          <el-button link type="primary" @click="openPreview(row)">试算</el-button>
          <el-button
            v-if="row.status === 'PENDING'"
            v-hasPermi="['hrm:payroll:formula:approve']"
            link
            type="primary"
            @click="approve(row)"
            >审批</el-button
          >
        </template></el-table-column
      >
    </el-table>
  </ContentWrap>
  <Dialog v-model="createVisible" title="新增公式版本">
    <el-form ref="formRef" :model="draft" :rules="rules" label-width="100px">
      <el-form-item label="工资项" prop="optionCode"
        ><el-select v-model="draft.optionCode" filterable
          ><el-option
            v-for="item in catalog"
            :key="item.code"
            :value="item.code"
            :label="item.name" /></el-select
      ></el-form-item>
      <el-form-item label="薪资组"
        ><el-select v-model="draft.salaryGroupId" clearable placeholder="全部薪资组"
          ><el-option
            v-for="group in salaryGroups"
            :key="group.id"
            :value="group.id"
            :label="group.name" /></el-select
      ></el-form-item>
      <el-form-item label="公式" prop="expression"
        ><el-input v-model="draft.expression" type="textarea" maxlength="1000"
      /></el-form-item>
      <el-form-item label="插入变量"
        ><el-select v-model="chosenVariable" clearable filterable @change="insertVariable"
          ><el-option
            v-for="item in catalog"
            :key="item.code"
            :value="`item_${item.code}`"
            :label="item.name" /><el-option
            value="actual_work_days"
            label="实际工作天数" /><el-option
            value="need_work_days"
            label="应工作天数" /></el-select
      ></el-form-item>
      <el-alert
        title="仅支持加减乘除、括号和数字。工资项通过变量选择插入；缺失输入或除零将阻断核算。特定薪资组的规则覆盖同工资项的通用规则。"
        type="info"
        :closable="false"
      />
      <el-form-item label="生效日" prop="effectiveFrom"
        ><el-date-picker v-model="draft.effectiveFrom" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="失效日" prop="effectiveTo"
        ><el-date-picker v-model="draft.effectiveTo" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="政策依据" prop="evidence"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button :loading="saving" type="primary" @click="save"
        >保存待审批版本</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="previewVisible" title="公式样例试算">
    <el-form label-width="150px">
      <el-form-item label="核算期间起始日"
        ><el-date-picker v-model="previewDate" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="薪资组"
        ><el-select v-model="previewGroup" clearable placeholder="全部薪资组"
          ><el-option
            v-for="group in salaryGroups"
            :key="group.id"
            :value="group.id"
            :label="group.name" /></el-select
      ></el-form-item>
      <el-form-item
        v-for="variable in previewVariables"
        :key="variable"
        :label="variableLabel(variable)"
        ><el-input-number v-model="sample[variable]" :precision="4"
      /></el-form-item>
    </el-form>
    <el-alert
      title="填写本次试算输入。结果仅供验证，不写入员工工资，也不改变审批状态。"
      type="info"
      :closable="false"
    />
    <el-table :data="outputs"
      ><el-table-column prop="name" label="工资项" /><el-table-column prop="value" label="试算结果"
    /></el-table>
    <template #footer
      ><el-button :loading="saving" type="primary" @click="runPreview">试算</el-button></template
    >
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/formula'
import type { FormInstance } from 'element-plus'
defineOptions({ name: 'HrmPayrollFormula' })
const message = useMessage()
const rows = ref<api.Formula[]>([]),
  catalog = ref<api.Variable[]>([])
const salaryGroups = ref<{ id: number; name: string }[]>([])
const loading = ref(false),
  saving = ref(false),
  createVisible = ref(false),
  previewVisible = ref(false)
const formRef = ref<FormInstance>(),
  chosenVariable = ref<string>()
const draft = reactive<api.Formula>({
  optionCode: 0,
  expression: '',
  effectiveFrom: '',
  effectiveTo: '',
  evidence: ''
})
const rules = Object.fromEntries(
  ['optionCode', 'expression', 'effectiveFrom', 'effectiveTo', 'evidence'].map((key) => [
    key,
    [{ required: true, message: '请填写此项', trigger: 'change' }]
  ])
)
const itemName = (code: number) =>
  catalog.value.find((item) => item.code === code)?.name || String(code)
const groupName = (id?: number) =>
  id ? salaryGroups.value.find((group) => group.id === id)?.name || String(id) : '全部薪资组'
const variableLabel = (name: string) =>
  name === 'actual_work_days'
    ? '实际工作天数'
    : name === 'need_work_days'
      ? '应工作天数'
      : itemName(Number(name.replace('item_', '')))
async function load() {
  loading.value = true
  try {
    rows.value = await api.list()
  } finally {
    loading.value = false
  }
}
function openCreate() {
  Object.assign(draft, {
    optionCode: undefined,
    salaryGroupId: undefined,
    expression: '',
    effectiveFrom: '',
    effectiveTo: '',
    evidence: ''
  })
  createVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}
function insertVariable(value: string) {
  if (value) draft.expression += value
  chosenVariable.value = undefined
}
async function save() {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    await api.create(draft)
    createVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function approve(row: api.Formula) {
  await message.confirm('批准后该公式版本将用于生效期间的核算，是否继续？')
  await api.approve(row.id!)
  await load()
}
const previewRule = ref<api.Formula>(),
  previewDate = ref(''),
  previewGroup = ref<number>()
const sample = reactive<Record<string, number>>({}),
  previewVariables = ref<string[]>([]),
  outputs = ref<{ name: string; value: number }[]>([])
function openPreview(row: api.Formula) {
  previewRule.value = row
  previewDate.value = row.effectiveFrom
  previewGroup.value = row.salaryGroupId
  outputs.value = []
  Object.keys(sample).forEach((key) => delete sample[key])
  previewVariables.value = [
    ...new Set(
      rows.value
        .filter((rule) => rule.status === 'APPROVED' || rule.id === row.id)
        .flatMap((rule) => rule.expression.match(/[A-Za-z_][A-Za-z0-9_]*/g) || [])
    )
  ]
  previewVisible.value = true
}
async function runPreview() {
  if (!previewDate.value) {
    message.warning('请选择核算期间起始日')
    return
  }
  saving.value = true
  try {
    const result = await api.preview({
      id: previewRule.value!.id!,
      date: previewDate.value,
      salaryGroupId: previewGroup.value,
      inputs: sample
    })
    outputs.value = Object.entries(result)
      .filter(([name]) => name.startsWith('item_'))
      .map(([name, value]) => ({ name: variableLabel(name), value }))
  } finally {
    saving.value = false
  }
}
onMounted(async () => {
  ;[catalog.value, salaryGroups.value] = await Promise.all([api.variables(), api.groups()])
  await load()
})
</script>
