<template>
  <ContentWrap>
    <el-alert
      title="员工定薪、调薪及批量导入先提交审批申请。提交人、HR复核人、财务审批人必须不同；批准后按生效日期应用，冻结工资批次保持原版本。"
      type="info"
      :closable="false"
      class="mb-4"
    />
    <el-select v-model="selectedStatus" clearable placeholder="全部状态"
      ><el-option v-for="(label, key) in statuses" :key="key" :value="key" :label="label"
    /></el-select>
    <el-button @click="load">刷新</el-button>
    <el-table v-loading="loading" :data="filtered">
      <el-table-column label="员工"
        ><template #default="{ row }">{{ employeeName(row.employeeId) }}</template></el-table-column
      >
      <el-table-column label="提交时间" width="180"
        ><template #default="{ row }">{{ formatDate(row.createTime) }}</template></el-table-column
      >
      <el-table-column label="状态" width="110"
        ><template #default="{ row }">{{
          statuses[row.status] || row.status
        }}</template></el-table-column
      >
      <el-table-column prop="proposedBy" label="提交人" width="90" /><el-table-column
        prop="reviewedBy"
        label="HR复核人"
        width="100"
      /><el-table-column prop="approvedBy" label="审批人" width="90" />
      <el-table-column label="操作" width="250"
        ><template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">申请详情</el-button>
          <el-button
            v-if="row.status === 'HR_REVIEW'"
            v-hasPermi="['hrm:payroll:salary-proposal:review']"
            link
            type="primary"
            @click="decide(row, 'review')"
            >HR复核</el-button
          >
          <el-button
            v-if="row.status === 'FINANCE_REVIEW'"
            v-hasPermi="['hrm:payroll:salary-proposal:approve']"
            link
            type="primary"
            @click="decide(row, 'approve')"
            >财务审批</el-button
          >
          <el-button
            v-if="['HR_REVIEW', 'FINANCE_REVIEW'].includes(row.status)"
            v-hasPermi="['hrm:payroll:salary-proposal:reject']"
            link
            type="danger"
            @click="decide(row, 'reject')"
            >退回</el-button
          >
        </template></el-table-column
      >
    </el-table>
  </ContentWrap>
  <Dialog v-model="detailVisible" title="定调薪申请详情" width="800px">
    <el-descriptions v-if="current" :column="2" border>
      <el-descriptions-item label="员工">{{
        employeeName(current.employeeId)
      }}</el-descriptions-item>
      <el-descriptions-item label="类型">{{
        requestData.recordType === 1 ? '定薪' : '调薪'
      }}</el-descriptions-item>
      <el-descriptions-item label="生效日期">{{
        requestData.effectTime ? formatDate(requestData.effectTime) : '首次定薪按申请时当月首日'
      }}</el-descriptions-item>
      <el-descriptions-item label="备注">{{ requestData.remark }}</el-descriptions-item>
      <el-descriptions-item label="HR复核意见">{{ current.reviewReason }}</el-descriptions-item>
      <el-descriptions-item label="财务审批/退回意见">{{
        current.approvalReason
      }}</el-descriptions-item>
    </el-descriptions>
    <h4>正式薪资变化</h4
    ><el-table :data="changes"
      ><el-table-column prop="name" label="工资项" /><el-table-column
        prop="before"
        label="申请前" /><el-table-column prop="after" label="申请值"
    /></el-table>
    <h4>试用期薪资</h4
    ><el-table :data="requestData.probationSalaryOptions || []"
      ><el-table-column prop="name" label="工资项" /><el-table-column prop="value" label="申请值"
    /></el-table>
    <el-alert
      title="调薪申请中未列出的工资项沿用原值。审批期间基线发生变化时需退回并重新提交；批准后未来生效的申请在到期后由原生效任务应用。"
      type="info"
      :closable="false"
    />
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/salary-proposal'
import { formatDate } from '@/utils/formatTime'
defineOptions({ name: 'HrmPayrollSalaryProposal' })
const message = useMessage(),
  loading = ref(false),
  rows = ref<api.SalaryProposal[]>([])
const employees = ref<{ id: number; name: string; jobNumber: string }[]>([])
const statuses: Record<string, string> = {
  HR_REVIEW: '待HR复核',
  FINANCE_REVIEW: '待财务审批',
  APPROVED: '已批准',
  REJECTED: '已退回'
}
const selectedStatus = ref<string>(),
  detailVisible = ref(false),
  current = ref<api.SalaryProposal>()
const filtered = computed(() =>
  rows.value.filter((row) => !selectedStatus.value || row.status === selectedStatus.value)
)
const employeeName = (id: number) => {
  const employee = employees.value.find((row) => row.id === id)
  return employee ? `${employee.name}（${employee.jobNumber}）` : String(id)
}
async function load() {
  loading.value = true
  try {
    rows.value = await api.list()
  } finally {
    loading.value = false
  }
}
async function decide(row: api.SalaryProposal, action: string) {
  const { value } = await message.prompt('请填写本次处理意见', '审批申请', {
    inputValidator: (value) => !!value.trim() || '处理意见不能为空'
  })
  await api.decide(action, row.id, value)
  await load()
}
function openDetail(row: api.SalaryProposal) {
  current.value = row
  detailVisible.value = true
}
interface Option {
  code: number
  name?: string
  value: number
}
const requestData = computed(() => (current.value ? JSON.parse(current.value.requestSnapshot) : {}))
const changes = computed(() => {
  const before: Option[] = current.value?.beforeSnapshot
    ? JSON.parse(current.value.beforeSnapshot)?.salaryOptions || []
    : []
  const after: Option[] = requestData.value.salaryOptions || []
  const codes = [...new Set([...before, ...after].map((row) => row.code))]
  return codes.map((code) => {
    const old = before.find((row) => row.code === code),
      next = after.find((row) => row.code === code)
    return {
      name: next?.name || old?.name || String(code),
      before: old?.value ?? '未设置',
      after: next?.value ?? '沿用原值'
    }
  })
})
onMounted(async () => {
  employees.value = await api.employees()
  await load()
})
</script>
