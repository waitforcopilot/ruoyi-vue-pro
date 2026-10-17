<template>
  <ContentWrap>
    <RequirementContext module="PLAN" />
    <el-alert
      title="薪级薪档和岗位系数按方案版本维护。应用已批准方案会提交定调薪申请，HR及财务批准后生效。"
      type="info"
      :closable="false"
      class="mb-4"
    />
    <el-button v-hasPermi="['hrm:payroll:grade:update']" type="primary" @click="openCreate()"
      >新增方案版本</el-button
    >
    <el-button @click="downloadTemplate">下载导入模板</el-button>
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="code" label="方案编码" /><el-table-column
        prop="name"
        label="方案名称"
      /><el-table-column prop="version" label="版本" width="70" />
      <el-table-column label="工资项"
        ><template #default="{ row }">{{
          catalog.find((item) => item.code === row.optionCode)?.name || row.optionCode
        }}</template></el-table-column
      >
      <el-table-column prop="effectiveFrom" label="生效日" /><el-table-column
        prop="effectiveTo"
        label="失效日"
      />
      <el-table-column label="状态"
        ><template #default="{ row }">{{
          row.status === 'APPROVED' ? '已批准' : '待审批'
        }}</template></el-table-column
      >
      <el-table-column label="操作" width="310"
        ><template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情/对比</el-button>
          <el-button link @click="exportPolicy(row)">导出</el-button>
          <el-button v-hasPermi="['hrm:payroll:grade:update']" link @click="openCreate(row)"
            >复制新版本</el-button
          >
          <el-button
            v-if="row.status === 'PENDING'"
            v-hasPermi="['hrm:payroll:grade:approve']"
            link
            type="primary"
            @click="approve(row)"
            >审批</el-button
          >
          <el-button
            v-if="row.status === 'APPROVED'"
            v-hasPermi="['hrm:payroll:grade:update']"
            link
            type="primary"
            @click="openApply(row)"
            >应用薪级</el-button
          >
        </template></el-table-column
      >
    </el-table>
  </ContentWrap>
  <Dialog v-model="createVisible" title="维护薪级方案版本" width="950px">
    <el-form ref="formRef" :model="draft" :rules="rules" label-width="110px">
      <el-form-item label="方案编码" prop="code"
        ><el-input v-model="draft.code" maxlength="64"
      /></el-form-item>
      <el-form-item label="方案名称" prop="name"
        ><el-input v-model="draft.name" maxlength="100"
      /></el-form-item>
      <el-form-item label="工资项" prop="optionCode"
        ><el-select v-model="draft.optionCode" filterable
          ><el-option
            v-for="item in catalog"
            :key="item.code"
            :value="item.code"
            :label="item.name" /></el-select
      ></el-form-item>
      <el-form-item label="生效日" prop="effectiveFrom"
        ><el-date-picker v-model="draft.effectiveFrom" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="失效日" prop="effectiveTo"
        ><el-date-picker v-model="draft.effectiveTo" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="政策依据" prop="evidence"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000"
      /></el-form-item>
      <el-form-item label="批量导入"
        ><el-upload
          v-model:file-list="uploadList"
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="chooseFile"
          :on-remove="clearFile"
          ><el-button>选择薪级/岗位标准文件</el-button></el-upload
        ></el-form-item
      >
    </el-form>
    <template v-if="!file">
      <h4>薪级薪档</h4
      ><el-button
        @click="
          draft.grades.push({ gradeCode: '', stepCode: '', minimum: 0, standard: 0, maximum: 0 })
        "
        >添加薪档</el-button
      >
      <el-table :data="draft.grades"
        ><el-table-column label="薪级"
          ><template #default="{ row }"
            ><el-input v-model="row.gradeCode" /></template></el-table-column
        ><el-table-column label="薪档"
          ><template #default="{ row }"><el-input v-model="row.stepCode" /></template
        ></el-table-column>
        <el-table-column v-for="(label, key) in amountFields" :key="key" :label="label" width="200"
          ><template #default="{ row }"
            ><el-input-number v-model="row[key]" :min="0.01" :precision="2" /></template
        ></el-table-column>
        <el-table-column width="70"
          ><template #default="{ $index }"
            ><el-button link type="danger" @click="draft.grades.splice($index, 1)"
              >删除</el-button
            ></template
          ></el-table-column
        >
      </el-table>
      <h4>岗位系数</h4
      ><el-button @click="draft.postCoefficients.push({ postName: '', coefficient: 1 })"
        >添加岗位</el-button
      >
      <el-table :data="draft.postCoefficients"
        ><el-table-column label="岗位名称（与员工档案一致）"
          ><template #default="{ row }"
            ><el-input v-model="row.postName" /></template></el-table-column
        ><el-table-column label="系数"
          ><template #default="{ row }"
            ><el-input-number
              v-model="row.coefficient"
              :min="0.01"
              :max="100"
              :precision="4" /></template></el-table-column
        ><el-table-column width="70"
          ><template #default="{ $index }"
            ><el-button link type="danger" @click="draft.postCoefficients.splice($index, 1)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </template>
    <el-alert
      v-else
      title="文件中的薪档及岗位系数替代下方明细，保存前校验所有行；有错误时不会创建方案。"
      type="info"
      :closable="false"
    />
    <el-table v-if="importErrors.length" :data="importErrors"
      ><el-table-column prop="row" label="文件行" width="100" /><el-table-column
        prop="reason"
        label="问题"
    /></el-table>
    <template #footer
      ><el-button :loading="saving" type="primary" @click="save"
        >保存待审批版本</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="detailVisible" title="方案详情与版本对比" width="850px">
    <p>{{ current?.name }} · v{{ current?.version }} · {{ current?.evidence }}</p>
    <el-select v-model="compareId" clearable placeholder="选择同方案的历史版本"
      ><el-option
        v-for="row in comparable"
        :key="row.id"
        :value="row.id"
        :label="`v${row.version} · ${row.effectiveFrom}`"
    /></el-select>
    <el-table :data="differences"
      ><el-table-column prop="name" label="薪级/薪档或岗位" /><el-table-column
        prop="before"
        label="对比版本"
        min-width="220" /><el-table-column prop="after" label="当前版本" min-width="220"
    /></el-table>
  </Dialog>
  <Dialog v-model="applyVisible" title="按薪级提交定调薪申请">
    <el-form label-width="110px">
      <el-form-item label="员工"
        ><el-select v-model="application.employeeId" filterable
          ><el-option
            v-for="employee in employees"
            :key="employee.id"
            :value="employee.id"
            :label="`${employee.name}（${employee.jobNumber}）`" /></el-select
      ></el-form-item>
      <el-form-item label="薪级/薪档"
        ><el-select v-model="selectedGrade"
          ><el-option
            v-for="grade in currentGrades"
            :key="`${grade.gradeCode}|${grade.stepCode}`"
            :value="`${grade.gradeCode}|${grade.stepCode}`"
            :label="`${grade.gradeCode}/${grade.stepCode} 标准 ${grade.standard}`" /></el-select
      ></el-form-item>
      <el-form-item label="生效日"
        ><el-date-picker v-model="application.effectiveDate" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="薪档基数"
        ><el-input-number v-model="application.baseAmount" :precision="2" /><span
          >留空采用标准金额</span
        ></el-form-item
      >
      <el-form-item label="试用期比例"
        ><el-input-number v-model="application.probationFactor" :min="0.8" :max="1" :precision="2"
      /></el-form-item>
    </el-form>
    <el-alert
      title="工资项金额 = 薪档基数 × 岗位系数。配置岗位映射后，未匹配员工岗位会阻断；不配置映射时系数为1。结果提交审批，不直接改写薪资。"
      type="info"
      :closable="false"
    />
    <template #footer
      ><el-button :loading="saving" type="primary" @click="apply">提交审批申请</el-button></template
    >
  </Dialog>
</template>
<script setup lang="ts">
import RequirementContext from '../components/RequirementContext.vue'
import * as api from '@/api/hrm/payroll/grade'
import download from '@/utils/download'
import type { FormInstance, UploadFile, UploadUserFile } from 'element-plus'
defineOptions({ name: 'HrmPayrollGrade' })
const message = useMessage(),
  rows = ref<api.Policy[]>([]),
  catalog = ref<{ code: number; name: string }[]>([]),
  employees = ref<{ id: number; name: string; jobNumber: string }[]>([])
const loading = ref(false),
  saving = ref(false),
  createVisible = ref(false),
  detailVisible = ref(false),
  applyVisible = ref(false),
  current = ref<api.Policy>(),
  compareId = ref<number>()
const uploadList = ref<UploadUserFile[]>([])
const file = ref<File>(),
  importErrors = ref<{ row: number; reason: string }[]>([]),
  formRef = ref<FormInstance>()
const draft = reactive<api.Draft>({
  code: '',
  name: '',
  effectiveFrom: '',
  effectiveTo: '',
  evidence: '',
  grades: [],
  postCoefficients: []
})
const rules = Object.fromEntries(
  ['code', 'name', 'optionCode', 'effectiveFrom', 'effectiveTo', 'evidence'].map((key) => [
    key,
    [{ required: true, message: '请填写此项', trigger: 'change' }]
  ])
)
const amountFields = { minimum: '最低金额', standard: '标准金额', maximum: '最高金额' }
async function load() {
  loading.value = true
  try {
    rows.value = await api.list()
  } finally {
    loading.value = false
  }
}
function openCreate(row?: api.Policy) {
  Object.assign(
    draft,
    row
      ? {
          code: row.code,
          name: row.name,
          optionCode: row.optionCode,
          effectiveFrom: row.effectiveFrom,
          effectiveTo: row.effectiveTo,
          evidence: row.evidence,
          grades: JSON.parse(row.grades),
          postCoefficients: JSON.parse(row.postCoefficients)
        }
      : {
          code: '',
          name: '',
          optionCode: undefined,
          effectiveFrom: '',
          effectiveTo: '',
          evidence: '',
          grades: [],
          postCoefficients: []
        }
  )
  uploadList.value = []
  file.value = undefined
  importErrors.value = []
  createVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}
function chooseFile(upload: UploadFile) {
  file.value = upload.raw
  importErrors.value = []
}
function clearFile() {
  file.value = undefined
  importErrors.value = []
}
async function save() {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    if (file.value) {
      importErrors.value = await api.validateImport(file.value)
      if (importErrors.value.length) return
      await api.importPolicy(file.value, draft)
    } else await api.create(draft)
    createVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function approve(row: api.Policy) {
  await message.confirm('批准此薪级方案版本？生效期间不能与同方案的其他批准版本重叠。')
  await api.approve(row.id)
  await load()
}
async function downloadTemplate() {
  download.excel(await api.template(), '薪级薪档与岗位系数模板.xlsx')
}
async function exportPolicy(row: api.Policy) {
  download.excel(await api.exportPolicy(row.id), `${row.name}-v${row.version}.xlsx`)
}
function openDetail(row: api.Policy) {
  current.value = row
  compareId.value = undefined
  detailVisible.value = true
}
const comparable = computed(() =>
  rows.value.filter((row) => row.code === current.value?.code && row.id !== current.value?.id)
)
function policyValues(policy?: api.Policy) {
  const values = new Map<string, string>()
  if (policy) {
    ;(JSON.parse(policy.grades) as api.Grade[]).forEach((row) =>
      values.set(
        `${row.gradeCode}/${row.stepCode}`,
        `最低${row.minimum}，标准${row.standard}，最高${row.maximum}`
      )
    )
    ;(JSON.parse(policy.postCoefficients) as api.PostCoefficient[]).forEach((row) =>
      values.set(`岗位：${row.postName}`, `系数${row.coefficient}`)
    )
  }
  return values
}
const differences = computed(() => {
  const before = policyValues(rows.value.find((row) => row.id === compareId.value)),
    after = policyValues(current.value)
  return [...new Set([...before.keys(), ...after.keys()])].map((name) => ({
    name,
    before: before.get(name) || '未设置',
    after: after.get(name) || '未设置'
  }))
})
const application = reactive<{
  policyId: number
  employeeId?: number
  effectiveDate: string
  baseAmount?: number
  probationFactor: number
}>({ policyId: 0, effectiveDate: '', probationFactor: 1 })
const selectedGrade = ref(''),
  currentGrades = computed<api.Grade[]>(() =>
    current.value ? JSON.parse(current.value.grades) : []
  )
async function openApply(row: api.Policy) {
  current.value = row
  selectedGrade.value = ''
  Object.assign(application, {
    policyId: row.id,
    employeeId: undefined,
    effectiveDate: row.effectiveFrom,
    baseAmount: undefined,
    probationFactor: 1
  })
  employees.value = await api.employees()
  applyVisible.value = true
}
async function apply() {
  if (!application.employeeId || !selectedGrade.value || !application.effectiveDate) {
    message.warning('请选择员工、薪级薪档和生效日')
    return
  }
  const [gradeCode, stepCode] = selectedGrade.value.split('|')
  saving.value = true
  try {
    await api.apply({ ...application, employeeId: application.employeeId, gradeCode, stepCode })
    message.success('定调薪申请已提交，等待HR与财务审批')
    applyVisible.value = false
  } finally {
    saving.value = false
  }
}
onMounted(async () => {
  catalog.value = await api.variables()
  await load()
})
</script>
