<template>
  <el-divider content-position="left">个税申报与完税对账</el-divider>
  <el-alert
    title="按实际到账月份归集已成功付款及冻结税务来源，独立复核后导出申报准备文件。正式申报按主管税务机关格式办理，回填申报凭据并核对正式缴税／退税凭证；本页不向税务机关提交或付款。"
    type="info"
    :closable="false"
  />
  <el-form inline class="mt-4">
    <el-form-item label="收入月份"
      ><el-select v-model="month" style="width: 110px"
        ><el-option v-for="m in 12" :key="m" :value="m" :label="`${m}月`" /></el-select
    ></el-form-item>
    <el-button :loading="loading" :disabled="!entityId" @click="load">查询</el-button>
    <el-button v-hasPermi="['hrm:payroll:tax:update']" :disabled="!entityId" @click="open()"
      >生成申报版本</el-button
    >
  </el-form>
  <el-table :data="rows" v-loading="loading">
    <el-table-column prop="version" label="版本" width="70" />
    <el-table-column label="状态"
      ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
    >
    <el-table-column prop="expectedTax" label="申报应缴税合计" />
    <el-table-column prop="filingReference" label="正式申报凭据" min-width="180" />
    <el-table-column prop="filedDate" label="申报日期" />
    <el-table-column label="操作" width="300"
      ><template #default="{ row }">
        <el-button link @click="show(row.id)">来源与对账</el-button>
        <el-button
          v-if="row.status === 'PENDING'"
          v-hasPermi="['hrm:payroll:tax:approve']"
          link
          @click="review(row.id, true)"
          >批准</el-button
        >
        <el-button
          v-if="row.status === 'PENDING'"
          v-hasPermi="['hrm:payroll:tax:approve']"
          link
          @click="review(row.id, false)"
          >驳回</el-button
        >
        <el-button
          v-if="['APPROVED', 'FILED', 'RECONCILED'].includes(row.status)"
          v-hasPermi="['hrm:payroll:tax:update']"
          link
          @click="open(row)"
          >接续更正</el-button
        >
      </template></el-table-column
    >
  </el-table>
  <Dialog v-model="createVisible" title="生成申报版本（待独立复核）" width="650px">
    <el-form label-width="120px">
      <el-form-item label="收入所属期">{{ year }}年 {{ month }}月</el-form-item>
      <el-form-item label="发生额声明"
        ><el-select v-model="draft.zeroDeclaration"
          ><el-option :value="false" label="按本月到账来源申报" /><el-option
            :value="true"
            label="经核实本月无到账，零申报" /></el-select
      ></el-form-item>
      <el-form-item label="核实依据"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button type="primary" :loading="saving" @click="create"
        >生成并提交复核</el-button
      ></template
    >
  </Dialog>
  <Dialog v-model="detailVisible" title="申报来源与完税对账" width="1100px">
    <template v-if="data">
      <el-alert
        v-if="!data.sourceFresh"
        title="来源已变更或存在阻断，须核实并生成接续版本；原申报与凭证保持留痕。"
        type="warning"
        :closable="false"
      />
      <el-descriptions :column="3" border class="mt-4">
        <el-descriptions-item label="状态">{{ states[data.filing.status] }}</el-descriptions-item>
        <el-descriptions-item label="申报应缴税">{{
          data.filing.expectedTax
        }}</el-descriptions-item>
        <el-descriptions-item label="正式缴退净额">{{ data.officialPaid }}</el-descriptions-item>
        <el-descriptions-item label="待核对差额">{{ data.difference }}</el-descriptions-item>
        <el-descriptions-item
          v-if="data.source.actualWithheldTotal !== undefined"
          label="实际预缴税"
          >{{ data.source.actualWithheldTotal }}</el-descriptions-item
        >
        <el-descriptions-item
          v-if="data.source.periodTaxDifference !== undefined"
          label="本期税差额"
          >{{ data.source.periodTaxDifference }}（正数待补扣，负数待核实退税）</el-descriptions-item
        >
        <el-descriptions-item label="纳税登记号">{{
          data.source.taxRegistrationNumber
        }}</el-descriptions-item>
        <el-descriptions-item label="来源状态">{{
          data.sourceFresh ? '与批准版本一致' : '需核实／更正'
        }}</el-descriptions-item>
      </el-descriptions>
      <div class="mt-4">
        <el-button
          v-if="data.filing.status === 'RECONCILED'"
          v-hasPermi="['hrm:payroll:tax:query']"
          @click="taxSettlementsRef?.open(data.filing.id)"
          >员工税款结算</el-button
        >
        <el-button
          v-if="
            data.sourceFresh && ['APPROVED', 'FILED', 'RECONCILED'].includes(data.filing.status)
          "
          v-hasPermi="['hrm:payroll:tax:private']"
          @click="downloadFiling"
          >导出申报准备文件</el-button
        >
        <el-button
          v-if="data.sourceFresh && data.filing.status === 'APPROVED'"
          v-hasPermi="['hrm:payroll:tax:update']"
          @click="openFiled"
          >登记正式申报</el-button
        >
        <el-button
          v-if="data.filing.filedDate"
          v-hasPermi="['hrm:payroll:tax:update']"
          @click="openReceipt"
          >登记缴税／退税凭证</el-button
        >
        <el-button
          v-if="data.filing.filedDate"
          v-hasPermi="['hrm:payroll:tax:update']"
          @click="template"
          >凭证导入模板</el-button
        >
        <el-upload
          v-if="data.filing.filedDate && checkPermi(['hrm:payroll:tax:update'])"
          class="inline-block ml-10px"
          accept=".xlsx"
          :show-file-list="false"
          :http-request="upload"
          ><el-button>导入凭证待复核</el-button></el-upload
        >
        <el-button @click="downloadReceipts">导出完税对账资料</el-button>
        <el-button
          v-if="data.sourceFresh && ['FILED', 'RECONCILED'].includes(data.filing.status)"
          v-hasPermi="['hrm:payroll:tax:approve']"
          @click="reconcile"
          >确认对账</el-button
        >
      </div>
      <el-tabs class="mt-4">
        <el-tab-pane label="申报来源"
          ><el-table :data="data.source.rows">
            <el-table-column prop="taxpayerName" label="纳税人" /><el-table-column
              prop="maskedIdentifier"
              label="脱敏证件"
            />
            <el-table-column label="收入类型"
              ><template #default="{ row }">{{
                row.mode === 'SEPARATE_BONUS' ? '年终奖单独计税' : '居民工资'
              }}</template></el-table-column
            >
            <el-table-column prop="income" label="本月收入" /><el-table-column
              prop="withheld"
              label="已预扣"
            />
            <el-table-column prop="cumulativeIncome" label="累计工资收入" /><el-table-column
              prop="cumulativeTax"
              label="累计工资应税额"
            />
            <el-table-column prop="cumulativeActualWithheld" label="累计实际工资预缴" width="160" />
            <el-table-column label="工资累计税差额" width="170"
              ><template #default="{ row }">{{
                row.cumulativeTaxBalance === undefined ? '—' : row.cumulativeTaxBalance
              }}</template></el-table-column
            >
            <el-table-column
              prop="expectedSalaryWithholding"
              label="本月工资应预扣税"
              width="170"
            />
            <el-table-column prop="expectedBonusWithholding" label="奖金应预扣税" width="140" />
            <el-table-column prop="bonusTaxBalance" label="奖金税差额" width="140" />
            <el-table-column label="更正履历" width="140"
              ><template #default="{ row }">{{
                row.taxCorrectionIds?.join('、') || '—'
              }}</template></el-table-column
            >
            <el-table-column label="到账明细"
              ><template #default="{ row }">{{
                row.paymentIds.join('、')
              }}</template></el-table-column
            >
          </el-table></el-tab-pane
        >
        <el-tab-pane label="完税凭证"
          ><el-table :data="data.receipts">
            <el-table-column prop="id" label="记录编号" /><el-table-column
              prop="reference"
              label="官方凭证编号"
            />
            <el-table-column label="类型"
              ><template #default="{ row }">{{
                row.kind === 'REFUND' ? '退税' : '缴税'
              }}</template></el-table-column
            >
            <el-table-column prop="amount" label="金额" /><el-table-column
              prop="paymentDate"
              label="实际日期"
            />
            <el-table-column prop="originalReceiptId" label="原缴税记录" />
            <el-table-column label="状态"
              ><template #default="{ row }">{{ states[row.status] }}</template></el-table-column
            >
            <el-table-column prop="evidence" label="核实依据" />
            <el-table-column label="操作" width="130"
              ><template #default="{ row }"
                ><template v-if="row.status === 'PENDING'"
                  ><el-button
                    v-hasPermi="['hrm:payroll:tax:approve']"
                    link
                    @click="reviewReceipt(row.id, true)"
                    >批准</el-button
                  ><el-button
                    v-hasPermi="['hrm:payroll:tax:approve']"
                    link
                    @click="reviewReceipt(row.id, false)"
                    >驳回</el-button
                  ></template
                ></template
              ></el-table-column
            >
          </el-table></el-tab-pane
        >
        <el-tab-pane label="履历"
          ><el-table :data="data.events"
            ><el-table-column prop="createTime" label="时间" /><el-table-column label="操作"
              ><template #default="{ row }">{{
                actions[row.action] || row.action
              }}</template></el-table-column
            ><el-table-column prop="actorId" label="操作人" /><el-table-column
              prop="reason"
              label="依据" /><el-table-column prop="receiptId" label="凭证记录" /></el-table
        ></el-tab-pane>
      </el-tabs>
    </template>
  </Dialog>
  <Dialog v-model="filedVisible" title="登记正式申报凭据" width="600px"
    ><el-form label-width="120px">
      <el-form-item label="正式申报编号"
        ><el-input v-model="filingProof.reference" maxlength="100"
      /></el-form-item>
      <el-form-item label="申报日期"
        ><el-date-picker v-model="filingProof.date" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="核实依据"
        ><el-input v-model="filingProof.evidence" type="textarea" maxlength="1000"
      /></el-form-item> </el-form
    ><template #footer
      ><el-button type="primary" :loading="saving" @click="filed"
        >保存正式申报凭据</el-button
      ></template
    ></Dialog
  >
  <Dialog v-model="receiptVisible" title="登记正式缴税／退税凭证" width="650px"
    ><el-form label-width="140px">
      <el-form-item label="凭证类型"
        ><el-select v-model="receiptDraft.kind"
          ><el-option value="PAYMENT" label="缴税" /><el-option
            value="REFUND"
            label="退税" /></el-select
      ></el-form-item>
      <el-form-item v-if="receiptDraft.kind === 'REFUND'" label="原缴税凭证记录"
        ><el-select v-model="receiptDraft.originalReceiptId"
          ><el-option
            v-for="row in refundable"
            :key="row.id"
            :value="row.id"
            :label="`${row.reference}（记录${row.id}）`" /></el-select
      ></el-form-item>
      <el-form-item label="官方凭证编号"
        ><el-input v-model="receiptDraft.reference" maxlength="100"
      /></el-form-item>
      <el-form-item label="金额"
        ><el-input-number v-model="receiptDraft.amount" :min="0.01" :precision="2"
      /></el-form-item>
      <el-form-item label="实际缴退日期"
        ><el-date-picker v-model="receiptDraft.paymentDate" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="核实依据"
        ><el-input v-model="receiptDraft.evidence" type="textarea" maxlength="1000"
      /></el-form-item> </el-form
    ><template #footer
      ><el-button type="primary" :loading="saving" @click="receipt"
        >提交独立复核</el-button
      ></template
    ></Dialog
  >
  <TaxSettlements ref="taxSettlementsRef" />
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/tax-filing'
import TaxSettlements from './TaxSettlements.vue'
const taxSettlementsRef = ref<InstanceType<typeof TaxSettlements>>()
import dayjs from 'dayjs'
import download from '@/utils/download'
import { checkPermi } from '@/utils/permission'
const props = defineProps<{ entityId?: number; year: number }>()
const message = useMessage(),
  month = ref(dayjs().subtract(1, 'month').month() + 1),
  rows = ref<any[]>([]),
  loading = ref(false),
  saving = ref(false),
  createVisible = ref(false),
  detailVisible = ref(false),
  filedVisible = ref(false),
  receiptVisible = ref(false),
  draft = ref<any>({}),
  data = ref<any>(),
  filingProof = ref<any>({}),
  receiptDraft = ref<any>({})
const states: Record<string, string> = {
  PENDING: '待独立复核',
  APPROVED: '已批准',
  FILED: '已正式申报',
  RECONCILED: '已对账',
  SUPERSEDED: '历史版本',
  REJECTED: '已驳回'
}
const actions: Record<string, string> = {
  SUBMIT: '提交申报版本',
  APPROVE: '批准申报',
  REJECT: '驳回申报',
  SUPERSEDE: '接续版本',
  EXPORT: '导出申报准备文件',
  FILED: '登记正式申报',
  RECEIPT_SUBMIT: '登记完税凭证',
  RECEIPT_APPROVE: '批准完税凭证',
  RECEIPT_REJECT: '驳回完税凭证',
  RECONCILE: '确认对账',
  RECEIPT_EXPORT: '导出完税资料'
}
const refundable = computed(
  () =>
    data.value?.receipts.filter(
      (row: any) => row.kind === 'PAYMENT' && row.status === 'APPROVED'
    ) || []
)
let sequence = 0,
  detailSequence = 0
async function load() {
  const version = ++sequence
  rows.value = []
  if (!props.entityId) return
  loading.value = true
  try {
    const result = await api.list({
      entityId: props.entityId,
      year: props.year,
      month: month.value
    })
    if (version === sequence) rows.value = result
  } finally {
    if (version === sequence) loading.value = false
  }
}
function open(prior?: any) {
  draft.value = {
    entityId: props.entityId,
    year: props.year,
    month: month.value,
    zeroDeclaration: undefined,
    priorFilingId: prior?.id,
    requestKey: crypto.randomUUID(),
    evidence: ''
  }
  createVisible.value = true
}
async function create() {
  saving.value = true
  try {
    await api.create(draft.value)
    createVisible.value = false
    message.success('已生成待复核版本')
    await load()
  } finally {
    saving.value = false
  }
}
async function review(id: number, approved: boolean) {
  const version = sequence
  const reason = await message.prompt(approved ? '填写独立核对申报来源依据' : '填写驳回原因')
  if (!reason || version !== sequence) return
  await api.review({ id, approved, reason })
  await load()
}
async function show(id: number) {
  const version = ++detailSequence
  data.value = undefined
  detailVisible.value = true
  const result = await api.detail(id)
  if (version === detailSequence) data.value = result
}
async function refresh() {
  const id = data.value?.filing.id,
    version = detailSequence
  if (!id) return
  await load()
  if (version === detailSequence && data.value?.filing.id === id && detailVisible.value)
    await show(id)
}

async function downloadFiling() {
  const id = data.value?.filing.id
  if (!id) return
  const result = await api.exportFiling(id)
  download.excel(result, '个税申报准备.xlsx')
}
function openFiled() {
  filingProof.value = { id: data.value.filing.id }
  filedVisible.value = true
}
async function filed() {
  saving.value = true
  try {
    await api.filed(filingProof.value)
    filedVisible.value = false
    await refresh()
  } finally {
    saving.value = false
  }
}
function openReceipt() {
  receiptDraft.value = { filingId: data.value.filing.id, requestKey: crypto.randomUUID() }
  receiptVisible.value = true
}
async function receipt() {
  saving.value = true
  try {
    await api.receipt(receiptDraft.value)
    receiptVisible.value = false
    await refresh()
  } finally {
    saving.value = false
  }
}
async function reviewReceipt(id: number, approved: boolean) {
  const version = detailSequence
  const reason = await message.prompt(approved ? '填写独立核对正式凭证的依据' : '填写驳回原因')
  if (!reason || version !== detailSequence) return
  await api.reviewReceipt({ id, approved, reason })
  await refresh()
}
async function reconcile() {
  const id = data.value?.filing.id,
    version = detailSequence
  const reason = await message.prompt('填写申报金额与正式缴退税凭证的对账依据')
  if (!reason || version !== detailSequence || !id) return
  await api.reconcile({ id, reason })
  await refresh()
}
async function template() {
  download.excel(await api.receiptTemplate(), '完税凭证导入模板.xlsx')
}
async function upload(options: any) {
  const id = data.value?.filing.id
  if (!id) return
  try {
    await api.importReceipts(id, options.file)
    options.onSuccess?.({})
    message.success('已导入待复核凭证')
    await refresh()
  } catch (error) {
    options.onError?.(error)
    throw error
  }
}
async function downloadReceipts() {
  const id = data.value?.filing.id
  if (id) download.excel(await api.exportReceipts(id), '完税对账凭证.xlsx')
}
watch(
  () => [props.entityId, props.year, month.value],
  () => {
    detailSequence++
    data.value = undefined
    detailVisible.value = false
    createVisible.value = false
    filedVisible.value = false
    receiptVisible.value = false
    load()
  },
  { immediate: true }
)
watch(detailVisible, (visible) => {
  if (!visible) detailSequence++
})
onBeforeUnmount(() => {
  sequence++
  detailSequence++
})
</script>
