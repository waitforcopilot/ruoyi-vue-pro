<template>
  <ContentWrap>
    <RequirementContext module="INS" />
    <el-form inline>
      <el-form-item label="缴费主体"
        ><el-select v-model="entityId" filterable style="width: 280px" @change="load"
          ><el-option
            v-for="e in entities"
            :key="e.id"
            :label="`${e.code} ${e.name}`"
            :value="e.id" /></el-select
      ></el-form-item>
      <el-button :loading="loading" @click="load">刷新</el-button>
      <el-button
        v-if="manage"
        v-hasPermi="['hrm:payroll:insurance-bill:update']"
        @click="openUpload"
        >导入实缴账单</el-button
      >
      <el-button
        v-if="canExport"
        v-hasPermi="['hrm:payroll:insurance-bill:export']"
        @click="downloadTemplate"
        >下载账单模板</el-button
      >
    </el-form>
    <el-alert
      title="应缴来自批准的政策和年度申报。账单经 HR 复核、财务审批后建立对账台账；差异通过缴费或退款回执处理，保留原始账单。"
      type="info"
      :closable="false"
    />
    <el-table :data="bills"
      ><el-table-column prop="id" label="账单编号" width="90" /><el-table-column
        prop="version"
        label="预览版本"
        width="80"
      /><el-table-column label="缴费月份" width="120"
        ><template #default="{ row }"
          >{{ row.year }}-{{ String(row.month).padStart(2, '0') }}</template
        ></el-table-column
      ><el-table-column prop="reference" label="机构账单号" /><el-table-column
        prop="fileName"
        label="文件"
      /><el-table-column label="状态"
        ><template #default="{ row }">{{ statuses[row.status] }}</template></el-table-column
      ><el-table-column prop="evidence" label="核实依据" show-overflow-tooltip />
      <el-table-column label="操作" width="300"
        ><template #default="{ row }"
          ><el-button link @click="openBill(row.id)">明细及差异</el-button
          ><el-button
            v-if="manage && ['VALIDATED', 'FAILED', 'REJECTED'].includes(row.status)"
            v-hasPermi="['hrm:payroll:insurance-bill:update']"
            link
            @click="revalidate(row.id)"
            >新建来源预览</el-button
          ><el-button
            v-if="manage && row.status === 'VALIDATED'"
            v-hasPermi="['hrm:payroll:insurance-bill:update']"
            link
            @click="act('submit', row.id)"
            >提交复核</el-button
          ><el-button
            v-if="canReview && row.status === 'HR_REVIEW'"
            v-hasPermi="['hrm:payroll:insurance-bill:review']"
            link
            @click="act('review', row.id)"
            >HR复核</el-button
          ><el-button
            v-if="canApprove && row.status === 'FINANCE_REVIEW'"
            v-hasPermi="['hrm:payroll:insurance-bill:approve']"
            link
            @click="act('approve', row.id)"
            >财务审批</el-button
          ><el-button
            v-if="
              (canReview && row.status === 'HR_REVIEW') ||
              (canApprove && row.status === 'FINANCE_REVIEW')
            "
            v-hasPermi="['hrm:payroll:insurance-bill:review', 'hrm:payroll:insurance-bill:approve']"
            link
            @click="act('reject', row.id)"
            >退回</el-button
          ></template
        ></el-table-column
      >
    </el-table>
  </ContentWrap>
  <el-dialog
    :destroy-on-close="true"
    v-model="uploadVisible"
    title="导入并预校验实缴账单"
    width="650px"
  >
    <el-form ref="uploadRef" :model="uploadDraft" label-width="130px"
      ><el-form-item label="缴费月份" prop="period" :rules="required"
        ><el-date-picker
          v-model="uploadDraft.period"
          type="month"
          value-format="YYYY-MM" /></el-form-item
      ><el-form-item label="机构账单号" prop="reference" :rules="required"
        ><el-input v-model="uploadDraft.reference" maxlength="128" /></el-form-item
      ><el-form-item label="核实依据" prop="evidence" :rules="required"
        ><el-input v-model="uploadDraft.evidence" type="textarea" maxlength="1000" /></el-form-item
      ><el-form-item label="Excel账单"
        ><el-upload
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="(f) => (billFile = f.raw)"
          :on-remove="() => (billFile = undefined)"
          ><el-button>选择文件</el-button
          ><template #tip
            ><div>每位员工每个项目一行；金额单位元，零值填写0。</div></template
          ></el-upload
        ></el-form-item
      ></el-form
    >
    <template #footer
      ><el-button @click="uploadVisible = false">关闭</el-button
      ><el-button type="primary" :loading="saving" @click="upload">预校验</el-button></template
    >
  </el-dialog>
  <el-dialog v-model="detailVisible" title="应缴、实缴及差异处理" width="1250px">
    <template v-if="current">
      <el-descriptions :column="3" border
        ><el-descriptions-item label="账单">{{ current.reference }}</el-descriptions-item
        ><el-descriptions-item label="状态">{{ statuses[current.status] }}</el-descriptions-item
        ><el-descriptions-item label="原始依据">{{
          current.evidence
        }}</el-descriptions-item></el-descriptions
      >
      <el-form inline class="mt-12px"
        ><el-button
          v-if="canExport"
          v-hasPermi="['hrm:payroll:insurance-bill:export']"
          @click="downloadReport"
          >导出应实差异</el-button
        ><el-button
          v-if="canExport && rowErrors.length"
          v-hasPermi="['hrm:payroll:insurance-bill:export']"
          @click="downloadErrors"
          >下载错误行</el-button
        ></el-form
      >
      <el-table v-if="rowErrors.length" :data="rowErrors"
        ><el-table-column prop="row" label="文件行" width="90" /><el-table-column
          prop="jobNumber"
          label="工号" /><el-table-column prop="reason" label="错误说明"
      /></el-table>
      <el-tabs>
        <el-tab-pane label="应实对账"
          ><el-table :data="compared" border
            ><el-table-column prop="jobNumber" label="工号" /><el-table-column
              prop="employeeName"
              label="员工"
            /><el-table-column prop="projectName" label="项目" /><el-table-column
              prop="expectedPersonal"
              label="个人应缴"
            /><el-table-column prop="expectedCorporate" label="单位应缴" /><el-table-column
              label="原个人实缴"
              ><template #default="{ row }">{{
                actual(row.originalPersonal)
              }}</template></el-table-column
            ><el-table-column label="原单位实缴"
              ><template #default="{ row }">{{
                actual(row.originalCorporate)
              }}</template></el-table-column
            ><el-table-column label="核实后个人实缴"
              ><template #default="{ row }">{{
                actual(row.actualPersonal)
              }}</template></el-table-column
            ><el-table-column label="核实后单位实缴"
              ><template #default="{ row }">{{
                actual(row.actualCorporate)
              }}</template></el-table-column
            ><el-table-column label="个人差额"
              ><template #default="{ row }">{{
                difference(row.expectedPersonal, row.actualPersonal)
              }}</template></el-table-column
            ><el-table-column label="单位差额"
              ><template #default="{ row }">{{
                difference(row.expectedCorporate, row.actualCorporate)
              }}</template></el-table-column
            ><el-table-column label="状态"
              ><template #default="{ row }">{{ statuses[row.status] }}</template></el-table-column
            ><el-table-column label="处理" width="125"
              ><template #default="{ row }"
                ><el-button
                  v-if="manage && current.status === 'APPROVED' && row.status !== 'MATCHED'"
                  v-hasPermi="['hrm:payroll:insurance-bill:update']"
                  link
                  @click="openReceipt(row)"
                  >录入处置回执</el-button
                ></template
              ></el-table-column
            ></el-table
          ></el-tab-pane
        >
        <el-tab-pane label="补缴、退款与核实凭证"
          ><el-table :data="receipts"
            ><el-table-column prop="id" label="编号" width="80" /><el-table-column label="处置方式"
              ><template #default="{ row }">{{ modes[row.mode] }}</template></el-table-column
            ><el-table-column prop="lineId" label="对账明细编号" /><el-table-column
              prop="personal"
              label="个人金额"
            /><el-table-column prop="corporate" label="单位金额" /><el-table-column
              prop="reference"
              label="回执编号"
            /><el-table-column
              prop="evidence"
              label="核实依据"
              show-overflow-tooltip
            /><el-table-column label="状态"
              ><template #default="{ row }">{{ statuses[row.status] }}</template></el-table-column
            ><el-table-column label="操作" width="230"
              ><template #default="{ row }"
                ><el-button
                  v-if="canExport"
                  v-hasPermi="['hrm:payroll:insurance-bill:export']"
                  link
                  @click="downloadProof(row)"
                  >下载凭证</el-button
                ><el-button
                  v-if="canReview && row.status === 'HR_REVIEW'"
                  v-hasPermi="['hrm:payroll:insurance-bill:review']"
                  link
                  @click="act('settlement/review', row.id)"
                  >HR复核</el-button
                ><el-button
                  v-if="canApprove && row.status === 'FINANCE_REVIEW'"
                  v-hasPermi="['hrm:payroll:insurance-bill:approve']"
                  link
                  @click="act('settlement/approve', row.id)"
                  >财务审批</el-button
                ><el-button
                  v-if="
                    (canReview && row.status === 'HR_REVIEW') ||
                    (canApprove && row.status === 'FINANCE_REVIEW')
                  "
                  v-hasPermi="[
                    'hrm:payroll:insurance-bill:review',
                    'hrm:payroll:insurance-bill:approve'
                  ]"
                  link
                  @click="act('settlement/reject', row.id)"
                  >退回</el-button
                ></template
              ></el-table-column
            ></el-table
          ></el-tab-pane
        >
      </el-tabs>
    </template>
  </el-dialog>
  <el-dialog :destroy-on-close="true" v-model="receiptVisible" title="录入已办理的缴费、退款或补录回执" width="680px">
    <el-alert
      title="补缴填写追加缴费金额，退款填写已退回金额；未提供实缴记录时先补录凭证及实际总额。审批后更新台账，不重复扣减工资。"
      type="info"
      :closable="false"
    />
    <el-form ref="receiptRef" :model="receiptDraft" label-width="130px" class="mt-12px"
      ><el-form-item label="处置方式"
        ><el-select v-model="receiptDraft.mode"
          ><el-option
            v-for="mode in receiptModes"
            :key="mode"
            :value="mode"
            :label="modes[mode]" /></el-select></el-form-item
      ><el-form-item label="个人金额（元）" prop="personal" :rules="required"
        ><el-input-number
          v-model="receiptDraft.personal"
          :min="0"
          :precision="2"
          :controls="false" /></el-form-item
      ><el-form-item label="单位金额（元）" prop="corporate" :rules="required"
        ><el-input-number
          v-model="receiptDraft.corporate"
          :min="0"
          :precision="2"
          :controls="false" /></el-form-item
      ><el-form-item label="机构回执编号" prop="reference" :rules="required"
        ><el-input v-model="receiptDraft.reference" maxlength="128" /></el-form-item
      ><el-form-item label="核实依据" prop="evidence" :rules="required"
        ><el-input v-model="receiptDraft.evidence" type="textarea" maxlength="1000" /></el-form-item
      ><el-form-item label="原始回执"
        ><el-upload
          :auto-upload="false"
          :limit="1"
          accept=".pdf,.jpg,.jpeg,.png,.xls,.xlsx"
          :on-change="(f) => (proofFile = f.raw)"
          :on-remove="() => (proofFile = undefined)"
          ><el-button>选择凭证（不超过2MB）</el-button></el-upload
        ></el-form-item
      ></el-form
    >
    <template #footer
      ><el-button @click="receiptVisible = false">关闭</el-button
      ><el-button :loading="saving" type="primary" @click="submitReceipt"
        >提交复核</el-button
      ></template
    >
  </el-dialog>
</template>
<script setup lang="ts">
import RequirementContext from '../components/RequirementContext.vue'
import * as api from '@/api/hrm/payroll/insurance-bill'
import * as EntityApi from '@/api/hrm/payroll/entity'
import download from '@/utils/download'
import { ElMessageBox } from 'element-plus'
defineOptions({ name: 'HrmPayrollInsuranceBill' })
const message = useMessage()
const entities = ref<EntityApi.Entity[]>([]),
  entityId = ref<number>(),
  bills = ref<any[]>([]),
  loading = ref(false),
  saving = ref(false)
const selected = computed(() => entities.value.find((e) => e.id === entityId.value))
const manage = computed(() => !!selected.value?.allowedActions.includes('MANAGE')),
  canReview = computed(() => !!selected.value?.allowedActions.includes('REVIEW')),
  canApprove = computed(() => !!selected.value?.allowedActions.includes('APPROVE')),
  canExport = computed(() => !!selected.value?.allowedActions.includes('EXPORT'))
const uploadVisible = ref(false),
  uploadRef = ref(),
  uploadDraft = ref<any>({}),
  billFile = ref<File>(),
  detailVisible = ref(false),
  current = ref<any>(),
  compared = ref<any[]>([]),
  receipts = ref<any[]>([])
const rowErrors = computed(() => JSON.parse(current.value?.errors || '[]'))
const receiptVisible = ref(false),
  receiptRef = ref(),
  receiptDraft = ref<any>({}),
  receiptModes = ref<string[]>([]),
  proofFile = ref<File>()
const required = [{ required: true, message: '请填写必填项', trigger: 'blur' }]
const statuses: Record<string, string> = {
  VALIDATED: '预校验通过',
  FAILED: '文件校验失败',
  HR_REVIEW: '待HR复核',
  FINANCE_REVIEW: '待财务审批',
  APPROVED: '已批准，待处置差异',
  RECONCILED: '对账完成',
  REJECTED: '已退回',
  SUPERSEDED: '已被新预览替代',
  MISSING: '未提供实缴记录',
  MATCHED: '应实相符',
  UNDERPAID: '少缴',
  OVERPAID: '多缴',
  MIXED: '个人及单位差异方向不同'
}
const modes: Record<string, string> = { PAY: '补缴', REFUND: '退款', RECORD: '补录实缴记录' }
const actual = (v: any) => (v == null ? '未提供' : v)
const difference = (expected: any, paid: any) =>
  paid == null ? '不可计算' : (Number(expected) - Number(paid)).toFixed(2)
let loadVersion = 0
const load = async () => {
  const id = entityId.value,
    version = ++loadVersion
  bills.value = []
  current.value = undefined
  detailVisible.value = false
  uploadVisible.value = false
  receiptVisible.value = false
  if (!id) return
  loading.value = true
  try {
    const result = await api.list(id)
    if (version === loadVersion) bills.value = result
  } finally {
    if (version === loadVersion) loading.value = false
  }
}
const openUpload = () => {
  uploadDraft.value = {}
  billFile.value = undefined
  uploadVisible.value = true
  nextTick(() => uploadRef.value?.clearValidate())
}
const upload = async () => {
  await uploadRef.value.validate()
  if (!entityId.value || !billFile.value) {
    message.warning('请选择账单文件')
    return
  }
  if (billFile.value.size > 10 * 1024 * 1024) {
    message.warning('账单不能超过10MB')
    return
  }
  const id = entityId.value
  const [year, month] = uploadDraft.value.period.split('-').map(Number)
  saving.value = true
  try {
    const b = await api.preview(
      {
        entityId: id,
        year,
        month,
        reference: uploadDraft.value.reference,
        evidence: uploadDraft.value.evidence
      },
      billFile.value
    )
    if (entityId.value !== id) return
    uploadVisible.value = false
    bills.value = await api.list(id)
    await openBill(b.id)
  } finally {
    saving.value = false
  }
}
const openBill = async (id: number) => {
  const selectedEntity = entityId.value
  const b = await api.get(id)
  if (entityId.value !== selectedEntity) return
  current.value = b
  if (['APPROVED', 'RECONCILED'].includes(b.status)) {
    const result = await Promise.all([api.lines(id), api.settlements(id)])
    ;[compared.value, receipts.value] = result
  } else {
    compared.value = JSON.parse(b.comparison)
    receipts.value = []
  }
  detailVisible.value = true
}
const act = async (action: string, id: number) => {
  let reason: string | undefined
  if (action === 'submit') await message.confirm('确认提交该账单复核？')
  else {
    const result = await ElMessageBox.prompt('请填写核验或退回依据', '确认操作', {
      inputValidator: (v) => !!v?.trim() && v.length <= 1000,
      inputErrorMessage: '请填写1至1000字依据'
    })
    reason = result.value
  }
  const billId = current.value?.id
  await api.action(action, id, reason)
  await load()
  if (billId) await openBill(billId)
  message.success('操作成功')
}
const revalidate = async (id: number) => {
  await message.confirm('按当前批准来源生成新的预览版本？原预览及错误记录会保留。')
  const revised = await api.revalidate(id)
  await load()
  await openBill(revised.id)
  message.success('新预览已生成')
}
const openReceipt = (row: any) => {
  receiptModes.value =
    row.status === 'MISSING'
      ? ['RECORD']
      : row.status === 'UNDERPAID'
        ? ['PAY']
        : row.status === 'OVERPAID'
          ? ['REFUND']
          : ['PAY', 'REFUND']
  receiptDraft.value = { lineId: row.id, mode: receiptModes.value[0] }
  proofFile.value = undefined
  receiptVisible.value = true
  nextTick(() => receiptRef.value?.clearValidate())
}
const submitReceipt = async () => {
  await receiptRef.value.validate()
  if (!proofFile.value) {
    message.warning('请选择原始回执')
    return
  }
  if (proofFile.value.size > 2 * 1024 * 1024) {
    message.warning('凭证不能超过2MB')
    return
  }
  const billId = current.value.id
  saving.value = true
  try {
    await api.propose(receiptDraft.value, proofFile.value)
    receiptVisible.value = false
    await openBill(billId)
    message.success('回执已提交HR复核')
  } finally {
    saving.value = false
  }
}
const downloadTemplate = async () => {
  if (entityId.value) download.excel(await api.template(entityId.value), '实缴账单模板.xlsx')
}
const downloadReport = async () =>
  download.excel(await api.exportBill(current.value.id), '缴费应实差异.xlsx')
const downloadErrors = async () =>
  download.excel(await api.errors(current.value.id), '账单错误行.xlsx')
const downloadProof = async (row: any) => {
  const blob = await api.proof(row.id)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = row.proofName || 'receipt'
  a.click()
  URL.revokeObjectURL(url)
}
onMounted(async () => {
  entities.value = (await EntityApi.list()).filter((e) => e.status === 'APPROVED')
  entityId.value = entities.value[0]?.id
  await load()
})
</script>
