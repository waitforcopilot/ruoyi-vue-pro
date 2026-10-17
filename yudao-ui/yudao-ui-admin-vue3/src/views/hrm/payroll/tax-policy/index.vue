<template>
  <ContentWrap>
    <RequirementContext module="TAX" />
    <el-form inline>
      <el-form-item label="纳税主体"
        ><el-select v-model="entityId" style="width: 240px"
          ><el-option
            v-for="entity in entities"
            :key="entity.id"
            :value="entity.id"
            :label="entity.name" /></el-select
      ></el-form-item>
      <el-form-item label="年度"
        ><el-input-number v-model="year" :min="2019" :max="2099" :precision="0"
      /></el-form-item>
      <el-button :loading="loading" @click="load">查询</el-button>
      <el-button v-hasPermi="['hrm:payroll:tax:update']" :disabled="!entityId" @click="open"
        >提交年度政策版本</el-button
      >
    </el-form>
    <el-alert
      type="info"
      :closable="false"
      title="政策版本须填写正式适用出处并独立批准。政策试算用于核对参数；员工实际累计、扣除与发放来源按税务档案确认。"
    />
    <el-table ref="policyTableRef" :data="rows" class="mt-10px">
      <el-table-column prop="year" label="年度" width="90" /><el-table-column
        prop="version"
        label="版本"
        width="80"
      />
      <el-table-column prop="name" label="政策名称" min-width="180" /><el-table-column
        prop="taxRegistrationNumber"
        label="纳税登记号"
        min-width="180"
      />
      <el-table-column prop="monthlyBasicDeduction" label="月减除费用" width="120" />
      <el-table-column label="状态" width="110"
        ><template #default="{ row }">{{
          states[row.status] || row.status
        }}</template></el-table-column
      >
      <el-table-column prop="officialReference" label="正式适用出处" min-width="200" />
      <el-table-column label="操作" width="260"
        ><template #default="{ row }">
          <el-button link @click="inspect(row)">参数与依据</el-button
          ><el-button link @click="history(row.id)">履历</el-button
          ><el-button link @click="openTrial(row)">试算</el-button>
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
        </template></el-table-column
      >
    </el-table>
    <el-divider>员工年度税务身份与历史累计</el-divider>
    <TaxProfiles
      :entity-id="entityId"
      :year="year"
      :policy-ready="policyReady"
      :policy-hint="policyHint"
      @review-policy="policyTableRef?.$el.scrollIntoView({ behavior: 'smooth', block: 'center' })"
    />
    <TaxFilings :entity-id="entityId" :year="year" />
  </ContentWrap>
  <Dialog v-model="visible" title="年度政策版本（待独立审批）" width="850px">
    <el-form label-width="150px">
      <el-form-item label="政策名称"
        ><el-input v-model="draft.name" maxlength="100"
      /></el-form-item>
      <el-form-item label="原批准版本"
        ><span>{{
          draft.priorPolicyId ? `引用政策 ${draft.priorPolicyId}` : '初始版本'
        }}</span></el-form-item
      >
      <el-form-item label="月减除费用"
        ><el-input-number v-model="draft.monthlyBasicDeduction" :min="0" :precision="2"
      /></el-form-item>
      <el-form-item label="允许奖金单独计税"
        ><el-switch v-model="draft.separateBonusAllowed"
      /></el-form-item>
      <el-form-item label="单独计税政策截止"
        ><el-date-picker v-model="draft.separateBonusValidTo" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-tabs
        ><el-tab-pane v-for="table in tables" :key="table.key" :label="table.label">
          <el-table :data="draft[table.key] || []">
            <el-table-column label="级距上限"
              ><template #default="{ row }"
                ><el-input-number
                  v-if="row.upper != null"
                  v-model="row.upper"
                  :min="0.01"
                  :precision="2"
                /><span v-else>无上限</span></template
              ></el-table-column
            >
            <el-table-column label="税率（0～1）"
              ><template #default="{ row }"
                ><el-input-number
                  v-model="row.rate"
                  :min="0"
                  :max="1"
                  :precision="6"
                  :step="0.01" /></template
            ></el-table-column>
            <el-table-column label="速算扣除数"
              ><template #default="{ row }"
                ><el-input-number v-model="row.quickDeduction" :min="0" :precision="2" /></template
            ></el-table-column>
          </el-table> </el-tab-pane
      ></el-tabs>
      <el-form-item label="正式适用出处"
        ><el-input
          v-model="draft.officialReference"
          type="textarea"
          maxlength="1000"
          placeholder="正式文件名称、适用年度、条款或可追溯出处"
      /></el-form-item>
      <el-form-item label="维护与核实依据"
        ><el-input v-model="draft.evidence" type="textarea" maxlength="1000"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button :loading="saving" type="primary" @click="save">提交待审版本</el-button></template
    >
  </Dialog>
  <Dialog v-model="trialVisible" title="年度政策试算" width="750px">
    <el-alert
      :title="`政策 ${trialDraft.policyId} · ${trialStatus}；工资收入填截至本次的计税工资累计，未减除专项扣除且不含本次年终奖。`"
      type="info"
      :closable="false"
    />
    <el-form label-width="220px" class="mt-10px">
      <el-form-item label="符合条件的减除费用月数"
        ><el-input-number v-model="trialDraft.basicMonths" :min="0" :max="12" :precision="0"
      /></el-form-item>
      <el-form-item v-for="field in trialFields" :key="field.key" :label="field.label"
        ><el-input-number v-model="trialDraft[field.key]" :min="0" :precision="2"
      /></el-form-item>
      <el-form-item label="年终奖计税策略"
        ><el-select v-model="trialDraft.bonusMode"
          ><el-option value="NONE" label="无本次年终奖" /><el-option
            value="MERGED"
            label="合并工资累计计税" /><el-option
            value="SEPARATE"
            label="符合政策时单独计税" /></el-select
      ></el-form-item>
      <el-form-item label="本年度已用单独计税"
        ><el-switch v-model="trialDraft.separateBonusAlreadyUsed"
      /></el-form-item>
    </el-form>
    <el-button :loading="calculating" type="primary" @click="calculate">核对试算结果</el-button>
    <el-descriptions v-if="quote" :column="2" border class="mt-10px">
      <el-descriptions-item label="累计减除费用">{{ quote.basicDeduction }}</el-descriptions-item
      ><el-descriptions-item label="工资累计应纳税所得">{{
        quote.salaryTaxable
      }}</el-descriptions-item>
      <el-descriptions-item label="工资累计应纳税额">{{
        quote.cumulativeSalaryTax
      }}</el-descriptions-item
      ><el-descriptions-item label="本次工资预扣">{{
        quote.currentSalaryWithholding
      }}</el-descriptions-item>
      <el-descriptions-item label="奖金单独计税">{{ quote.separateBonusTax }}</el-descriptions-item
      ><el-descriptions-item label="本次预扣合计">{{
        quote.currentWithholding
      }}</el-descriptions-item>
      <el-descriptions-item label="待更正税额（不自动退还）">{{
        quote.creditToReconcile
      }}</el-descriptions-item>
    </el-descriptions>
  </Dialog>
  <Dialog v-model="proofVisible" title="政策参数及审批履历" width="850px">
    <pre style="white-space: pre-wrap; word-break: break-all">{{ proof }}</pre>
  </Dialog>
</template>
<script setup lang="ts">
import RequirementContext from '../components/RequirementContext.vue'
import dayjs from 'dayjs'
import * as api from '@/api/hrm/payroll/tax-policy'
import TaxProfiles from './TaxProfiles.vue'
import TaxFilings from './TaxFilings.vue'
defineOptions({ name: 'HrmPayrollTaxPolicy' })
const message = useMessage()
const entities = ref<any[]>([]),
  entityId = ref<number>(),
  year = ref(dayjs().year()),
  rows = ref<any[]>([])
const policyTableRef = ref()
const policyLoaded = ref(false)
const policyReady = computed(
  () => policyLoaded.value && !loading.value && rows.value.some((row) => row.status === 'APPROVED')
)
const policyHint = computed(() =>
  !entityId.value
    ? '请先选择纳税主体'
    : loading.value
      ? '正在核对年度税务政策，请稍候'
      : !policyLoaded.value
        ? '年度税务政策未加载成功，请点击上方查询重试'
        : `请先批准该主体 ${year.value} 年度税务政策，再提交员工年度档案`
)
const loading = ref(false),
  visible = ref(false),
  saving = ref(false),
  trialVisible = ref(false),
  calculating = ref(false),
  proofVisible = ref(false),
  proof = ref('')
const draft = ref<any>({}),
  trialDraft = ref<any>({}),
  trialStatus = ref(''),
  quote = ref<any>()
const states: Record<string, string> = {
  PENDING: '待审批',
  APPROVED: '已批准',
  SUPERSEDED: '历史批准版本',
  REJECTED: '已驳回'
}
const tables = [
  { key: 'residentBands', label: '居民工资年度累计级距' },
  { key: 'bonusBands', label: '奖金按月折算级距（总奖金 ÷ 12）' }
]
const trialFields = [
  { key: 'salaryIncome', label: '工资累计收入（含本次）' },
  { key: 'socialDeduction', label: '累计专项扣除（社保公积金）' },
  { key: 'additionalDeduction', label: '累计专项附加扣除' },
  { key: 'otherDeduction', label: '累计其他准予扣除' },
  { key: 'alreadyWithheld', label: '工资累计已预缴（不含单独奖金）' },
  { key: 'bonusAmount', label: '本次年终奖金额' }
]
let sequence = 0,
  trialSequence = 0
async function load() {
  const version = ++sequence
  rows.value = []
  policyLoaded.value = false
  if (!entityId.value) return
  loading.value = true
  try {
    const result = await api.list({ entityId: entityId.value, year: year.value })
    if (version === sequence) {
      rows.value = result
      policyLoaded.value = true
    }
  } finally {
    if (version === sequence) loading.value = false
  }
}
async function open() {
  const selected = entityId.value,
    selectedYear = year.value
  const result = await api.defaults({ entityId: selected, year: selectedYear })
  if (selected !== entityId.value || selectedYear !== year.value) return
  draft.value = { ...result, requestKey: crypto.randomUUID() }
  visible.value = true
}
async function save() {
  saving.value = true
  try {
    await api.create(draft.value)
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function review(id: number, approved: boolean) {
  const { value } = await message.prompt(
    '请输入独立复核意见',
    approved ? '批准年度税务政策' : '驳回年度税务政策'
  )
  await api.review({ id, approved, reason: value })
  await load()
}
function inspect(row: any) {
  proof.value = JSON.stringify(
    {
      ...row,
      residentBands: JSON.parse(row.residentBands),
      bonusBands: JSON.parse(row.bonusBands)
    },
    null,
    2
  )
  proofVisible.value = true
}
async function history(id: number) {
  proof.value = JSON.stringify(await api.history(id), null, 2)
  proofVisible.value = true
}
function openTrial(row: any) {
  trialSequence++
  quote.value = undefined
  trialStatus.value = states[row.status] || row.status
  trialDraft.value = {
    policyId: row.id,
    basicMonths: 0,
    salaryIncome: 0,
    socialDeduction: 0,
    additionalDeduction: 0,
    otherDeduction: 0,
    alreadyWithheld: 0,
    bonusAmount: 0,
    bonusMode: 'NONE',
    separateBonusAlreadyUsed: false
  }
  trialVisible.value = true
}
async function calculate() {
  const version = ++trialSequence,
    data = { ...trialDraft.value }
  quote.value = undefined
  calculating.value = true
  try {
    const result = await api.trial(data)
    if (version === trialSequence) {
      quote.value = result
      trialStatus.value = states[result.policyStatus] || result.policyStatus
    }
  } finally {
    if (version === trialSequence) calculating.value = false
  }
}
watch(
  trialDraft,
  () => {
    trialSequence++
    quote.value = undefined
    calculating.value = false
  },
  { deep: true }
)
watch(
  () => [entityId.value, year.value],
  () => {
    visible.value = false
    trialVisible.value = false
    trialSequence++
    void load()
  }
)
onMounted(async () => {
  entities.value = (await api.entities()).filter(
    (entity) => entity.status === 'APPROVED' && entity.allowedActions.includes('READ')
  )
  entityId.value = entities.value[0]?.id
})
</script>
