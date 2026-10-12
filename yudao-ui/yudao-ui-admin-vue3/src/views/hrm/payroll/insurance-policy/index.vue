<template>
  <ContentWrap>
    <el-form inline>
      <el-form-item label="缴费主体"
        ><el-select v-model="entityId" filterable style="width: 260px" @change="load"
          ><el-option
            v-for="e in entities"
            :key="e.id"
            :label="`${e.code} ${e.name}`"
            :value="e.id" /></el-select
      ></el-form-item>
      <el-button :loading="loading" @click="load">刷新</el-button>
      <el-button v-if="manage" v-hasPermi="['hrm:payroll:insurance:update']" @click="openPolicy()"
        >新建政策版本</el-button
      >
      <el-button v-if="manage" v-hasPermi="['hrm:payroll:insurance:update']" @click="openBase()"
        >申报或年度调基</el-button
      >
    </el-form>
    <el-alert
      title="政策记录项目上下限、个人及单位比例和生效月份，需独立审批。申报基数经 HR 复核、财务审批后生效；已批准的豁免也保留依据。"
      type="info"
      :closable="false"
    />
    <el-tabs>
      <el-tab-pane label="地区政策版本">
        <el-table :data="policies">
          <el-table-column prop="code" label="政策编码" /><el-table-column
            prop="city"
            label="城市"
          /><el-table-column prop="version" label="版本" width="65" /><el-table-column
            prop="effectiveFrom"
            label="开始月份"
          /><el-table-column prop="effectiveTo" label="截止月份" /><el-table-column
            prop="status"
            label="状态"
          /><el-table-column prop="evidence" label="正式政策依据" show-overflow-tooltip />
          <el-table-column label="操作" width="280"
            ><template #default="{ row }">
              <el-button link @click="showPolicy(row)">项目及比例</el-button>
              <el-button
                v-if="manage"
                v-hasPermi="['hrm:payroll:insurance:update']"
                link
                @click="openPolicy(row)"
                >复制版本</el-button
              >
              <el-button
                v-if="row.status === 'APPROVED' && manage"
                v-hasPermi="['hrm:payroll:insurance:update']"
                link
                @click="openPolicy(row, true)"
                >截止或更正</el-button
              >
              <template v-if="row.status === 'PENDING' && canApprove"
                ><el-button
                  v-hasPermi="['hrm:payroll:insurance:approve']"
                  link
                  @click="act('policy', 'approve', row.id)"
                  >批准</el-button
                ><el-button
                  v-hasPermi="['hrm:payroll:insurance:approve']"
                  link
                  @click="act('policy', 'reject', row.id)"
                  >退回</el-button
                ></template
              >
            </template></el-table-column
          >
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="年度申报与历史归属">
        <el-table :data="bases"
          ><el-table-column prop="id" label="申报编号" width="90" /><el-table-column
            prop="jobNumber"
            label="工号"
          /><el-table-column prop="employeeName" label="员工" /><el-table-column label="缴费方式"
            ><template #default="{ row }">{{
              row.coverage === 'EXEMPT' ? '核定豁免' : '正常缴费'
            }}</template></el-table-column
          ><el-table-column prop="incomeYear" label="收入依据年度" /><el-table-column
            prop="effectiveFrom"
            label="生效月份"
          /><el-table-column prop="effectiveTo" label="截止月份" /><el-table-column
            prop="status"
            label="审批状态"
          />
          <el-table-column label="操作" width="220"
            ><template #default="{ row }">
              <el-button link @click="showEvidence(row)">基数与依据</el-button>
              <el-button
                v-if="row.status === 'APPROVED' && manage"
                v-hasPermi="['hrm:payroll:insurance:update']"
                link
                @click="openBase(row)"
                >截止或更正</el-button
              >
              <el-button
                v-if="row.status === 'HR_REVIEW' && canReview"
                v-hasPermi="['hrm:payroll:insurance:review']"
                link
                @click="act('base', 'review', row.id)"
                >HR 复核</el-button
              >
              <el-button
                v-if="row.status === 'FINANCE_REVIEW' && canApprove"
                v-hasPermi="['hrm:payroll:insurance:approve']"
                link
                @click="act('base', 'approve', row.id)"
                >财务批准</el-button
              >
              <el-button
                v-if="
                  (row.status === 'HR_REVIEW' && canReview) ||
                  (row.status === 'FINANCE_REVIEW' && canApprove)
                "
                v-hasPermi="['hrm:payroll:insurance:review', 'hrm:payroll:insurance:approve']"
                link
                @click="act('base', 'reject', row.id)"
                >退回</el-button
              >
            </template></el-table-column
          >
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="应缴测算"
        ><el-form inline
          ><el-form-item label="缴费月份"
            ><el-date-picker v-model="month" type="month" value-format="YYYY-MM" /></el-form-item
          ><el-button @click="calculate">测算</el-button></el-form
        >
        <el-table :data="quotes"
          ><el-table-column prop="employeeId" label="员工编号" /><el-table-column
            prop="baseId"
            label="批准申报编号"
          /><el-table-column prop="policyVersion" label="政策版本" /><el-table-column
            prop="personalInsurance"
            label="个人社保"
          /><el-table-column prop="personalFund" label="个人公积金" /><el-table-column
            prop="corporateInsurance"
            label="单位社保"
          /><el-table-column prop="corporateFund" label="单位公积金" /><el-table-column label="依据"
            ><template #default="{ row }"
              ><el-button link @click="showEvidence(row)"
                >基数、上下限及项目金额</el-button
              ></template
            ></el-table-column
          ></el-table
        >
      </el-tab-pane>
      <el-tab-pane label="审计履历"
        ><el-table :data="events"
          ><el-table-column prop="action" label="动作" /><el-table-column
            prop="actorId"
            label="操作人"
          /><el-table-column prop="reason" label="依据" /><el-table-column label="快照"
            ><template #default="{ row }"
              ><el-button link @click="showEvidence(row.snapshot)">查看</el-button></template
            ></el-table-column
          ></el-table
        ></el-tab-pane
      >
      <el-tab-pane label="年度基数批量导入">
        <el-form inline>
          <el-form-item label="模板政策">
            <el-select v-model="templatePolicyId" filterable style="width: 300px">
              <el-option
                v-for="p in policies.filter((p) => p.status === 'APPROVED')"
                :key="p.id"
                :value="p.id"
                :label="`${p.code} v${p.version}`"
              />
            </el-select>
          </el-form-item>
          <el-button
            v-if="canExport"
            v-hasPermi="['hrm:payroll:insurance:export']"
            :disabled="!templatePolicyId"
            @click="downloadTemplate"
            >下载申报模板</el-button
          >
          <el-upload
            v-if="manage"
            v-hasPermi="['hrm:payroll:insurance:update']"
            :show-file-list="false"
            :auto-upload="false"
            accept=".xlsx,.xls"
            :on-change="uploadImport"
          >
            <el-button :loading="importBusy">上传并预校验</el-button>
          </el-upload>
        </el-form>
        <el-alert
          title="每位员工每个政策项目填写一行，金额单位为元。真实零值填写 0；豁免使用 EXEMPT 并填写依据。文件全部通过后提交 HR 复核，提交不会直接生效。"
          type="info"
          :closable="false"
        />
        <el-table :data="importBatches">
          <el-table-column prop="id" label="导入编号" width="90" /><el-table-column
            prop="fileName"
            label="文件"
          />
          <el-table-column label="状态"
            ><template #default="{ row }">{{
              importStatuses[row.status]
            }}</template></el-table-column
          >
          <el-table-column prop="creator" label="上传人" width="90" />
          <el-table-column label="操作"
            ><template #default="{ row }">
              <el-button link @click="inspectImport(row.id)">预览与错误</el-button>
              <el-button
                v-if="canExport && row.status === 'FAILED'"
                v-hasPermi="['hrm:payroll:insurance:export']"
                link
                @click="downloadErrors(row.id)"
                >下载错误行</el-button
              >
            </template></el-table-column
          >
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
  <el-dialog v-model="policyVisible" title="政策版本申请" width="1100px">
    <el-form ref="policyRef" :model="policyDraft" label-width="110px">
      <el-alert
        v-if="policyDraft.replacesId"
        :title="`替代版本 ${policyDraft.replacesId}；沿用原起始月份，不扩大原截止月份。原记录保留。`"
        :closable="false"
      />
      <el-form-item label="稳定政策编码" prop="code" :rules="required"
        ><el-input
          v-model="policyDraft.code"
          :disabled="!!policyDraft.replacesId"
          maxlength="64"
          placeholder="同一政策历次版本沿用编码"
      /></el-form-item>
      <el-form-item label="适用城市" prop="city" :rules="required"
        ><el-input v-model="policyDraft.city" maxlength="128"
      /></el-form-item>
      <el-form-item label="生效月份" prop="fromMonth" :rules="required"
        ><el-date-picker
          v-model="policyDraft.fromMonth"
          type="month"
          value-format="YYYY-MM"
          :disabled="!!policyDraft.replacesId"
      /></el-form-item>
      <el-form-item label="截止月份" prop="toMonth" :rules="required"
        ><el-date-picker v-model="policyDraft.toMonth" type="month" value-format="YYYY-MM"
      /></el-form-item>
      <el-form-item label="正式政策依据" prop="evidence" :rules="required"
        ><el-input
          v-model="policyDraft.evidence"
          type="textarea"
          maxlength="1000"
          placeholder="政策文号、有效期及核定资料"
      /></el-form-item>
      <el-button @click="policyDraft.projects.push({ code: '', name: '', type: 1 })"
        >新增项目</el-button
      >
      <el-table :data="policyDraft.projects"
        ><el-table-column label="项目编码" width="130"
          ><template #default="{ row }"
            ><el-input v-model="row.code" maxlength="32" /></template></el-table-column
        ><el-table-column label="项目名称" width="140"
          ><template #default="{ row }"
            ><el-input v-model="row.name" maxlength="64" /></template></el-table-column
        ><el-table-column label="项目类型" width="140"
          ><template #default="{ row }"
            ><el-select v-model="row.type"
              ><el-option
                v-for="(name, type) in types"
                :key="type"
                :label="name"
                :value="Number(type)" /></el-select></template
        ></el-table-column>
        <el-table-column v-for="field in fields" :key="field.key" :label="field.label" width="130"
          ><template #default="{ row }"
            ><el-input-number
              v-model="row[field.key]"
              :min="0"
              :max="field.rate ? 100 : undefined"
              :precision="field.rate ? 6 : 2"
              :controls="false"
              style="width: 110px" /></template
        ></el-table-column>
        <el-table-column label="操作" width="75"
          ><template #default="{ $index }"
            ><el-button link @click="policyDraft.projects.splice($index, 1)"
              >移除</el-button
            ></template
          ></el-table-column
        >
      </el-table> </el-form
    ><template #footer
      ><el-button @click="policyVisible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="savePolicy"
        >提交审批</el-button
      ></template
    >
  </el-dialog>
  <el-dialog v-model="baseVisible" title="申报基数与缴费主体申请" width="680px">
    <el-form ref="baseRef" :model="baseDraft" label-width="120px">
      <el-alert
        v-if="baseDraft.replacesId"
        :title="`替代申报 ${baseDraft.replacesId}，保留原记录；缴费主体转移须先审批原申报的截止版本。`"
        :closable="false"
      />
      <el-form-item label="员工" prop="employeeId" :rules="required"
        ><el-select v-model="baseDraft.employeeId" filterable :disabled="!!baseDraft.replacesId"
          ><el-option
            v-for="e in employees"
            :key="e.id"
            :label="`${e.jobNumber} ${e.name}`"
            :value="e.id" /></el-select
      ></el-form-item>
      <el-form-item label="缴费方式"
        ><el-radio-group v-model="baseDraft.coverage" @change="coverageChanged"
          ><el-radio label="INSURED">正常缴费</el-radio
          ><el-radio label="EXEMPT">核定豁免</el-radio></el-radio-group
        ></el-form-item
      >
      <template v-if="baseDraft.coverage === 'INSURED'">
        <el-form-item label="批准政策" prop="policyId" :rules="required"
          ><el-select v-model="baseDraft.policyId" @change="policyChanged"
            ><el-option
              v-for="p in policies.filter((p) => p.status === 'APPROVED')"
              :key="p.id"
              :label="`${p.code} v${p.version} ${p.city}`"
              :value="p.id" /></el-select
        ></el-form-item>
        <el-form-item label="收入依据年度" prop="incomeYear" :rules="required"
          ><el-input-number v-model="baseDraft.incomeYear" :min="2000" :max="2100" :precision="0"
        /></el-form-item>
        <el-form-item
          v-for="p in baseProjects"
          :key="p.code"
          :label="`${p.name}申报基数`"
          :prop="`declaredBases.${p.code}`"
          :rules="required"
          ><el-input-number
            v-model="baseDraft.declaredBases[p.code]"
            :min="0"
            :precision="2"
          /><span class="ml-2"
            >政策上下限 {{ p.minimumBase }}～{{ p.maximumBase }} 元</span
          ></el-form-item
        >
      </template>
      <el-form-item label="生效月份" prop="fromMonth" :rules="required"
        ><el-date-picker
          v-model="baseDraft.fromMonth"
          type="month"
          value-format="YYYY-MM"
          :disabled="!!baseDraft.replacesId" /></el-form-item
      ><el-form-item label="截止月份" prop="toMonth" :rules="required"
        ><el-date-picker
          v-model="baseDraft.toMonth"
          type="month"
          value-format="YYYY-MM" /></el-form-item
      ><el-form-item label="申报或豁免依据" prop="evidence" :rules="required"
        ><el-input v-model="baseDraft.evidence" type="textarea" maxlength="1000"
      /></el-form-item> </el-form
    ><template #footer
      ><el-button @click="baseVisible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="saveBase"
        >提交 HR 复核</el-button
      ></template
    >
  </el-dialog>
  <el-dialog v-model="proofVisible" title="政策项目或审计依据" width="850px"
    ><el-table v-if="detailProjects.length" :data="detailProjects"
      ><el-table-column prop="code" label="项目" /><el-table-column
        prop="minimumBase"
        label="下限" /><el-table-column prop="maximumBase" label="上限" /><el-table-column
        prop="personalRate"
        label="个人比例 %" /><el-table-column
        prop="corporateRate"
        label="单位比例 %" /><el-table-column
        prop="personalFixed"
        label="个人固定额" /><el-table-column prop="corporateFixed" label="单位固定额" /></el-table
    ><pre v-else style="white-space: pre-wrap">{{ proof }}</pre>
  </el-dialog>
  <el-dialog v-model="importVisible" title="年度基数导入预览" width="1000px">
    <template v-if="activeImport">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="文件">{{ activeImport.fileName }}</el-descriptions-item
        ><el-descriptions-item label="状态">{{
          importStatuses[activeImport.status]
        }}</el-descriptions-item
        ><el-descriptions-item label="申报人数">{{ importRequests.length }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="importRowErrors.length" :data="importRowErrors"
        ><el-table-column prop="row" label="文件行" width="80" /><el-table-column
          prop="jobNumber"
          label="工号"
          width="130" /><el-table-column prop="reason" label="错误说明"
      /></el-table>
      <el-table v-else :data="importRequests">
        <el-table-column prop="employeeId" label="员工编号" /><el-table-column
          prop="coverage"
          label="缴费方式"
        /><el-table-column prop="incomeYear" label="收入年度" /><el-table-column
          prop="effectiveFrom"
          label="生效日期"
        /><el-table-column prop="effectiveTo" label="截止日期" />
        <el-table-column label="申报基数"
          ><template #default="{ row }"
            ><div v-for="(amount, code) in row.declaredBases" :key="code"
              >{{ code }}：{{ amount }} 元</div
            ></template
          ></el-table-column
        ><el-table-column prop="evidence" label="来源依据" show-overflow-tooltip />
      </el-table>
      <el-alert
        v-if="activeImport.status === 'COMMITTED'"
        :title="`已提交审批申请：${activeImport.proposalIds}`"
        type="success"
        :closable="false"
      />
    </template>
    <template #footer
      ><el-button @click="importVisible = false">关闭</el-button
      ><el-button
        v-if="manage && activeImport?.status === 'VALIDATED'"
        v-hasPermi="['hrm:payroll:insurance:update']"
        type="primary"
        :loading="importBusy"
        @click="submitImport"
        >确认提交 HR 复核</el-button
      ></template
    >
  </el-dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/insurance-policy'
import * as EntityApi from '@/api/hrm/payroll/entity'
import dayjs from 'dayjs'
import download from '@/utils/download'
import type { UploadFile } from 'element-plus'
import { ElMessageBox } from 'element-plus'
defineOptions({ name: 'HrmPayrollInsurancePolicy' })
const message = useMessage()
const importBatches = ref<api.ImportBatch[]>([]),
  activeImport = ref<api.ImportBatch>(),
  importVisible = ref(false),
  importBusy = ref(false),
  templatePolicyId = ref<number>()
const importStatuses: Record<string, string> = {
  VALIDATED: '预校验通过，待提交',
  FAILED: '校验失败',
  COMMITTED: '已提交审批'
}
const importRequests = computed<any[]>(() => JSON.parse(activeImport.value?.requests || '[]'))
const importRowErrors = computed<any[]>(() => JSON.parse(activeImport.value?.errors || '[]'))
const entities = ref<EntityApi.Entity[]>([]),
  entityId = ref<number>(),
  policies = ref<api.Policy[]>([]),
  bases = ref<any[]>([]),
  events = ref<any[]>([]),
  employees = ref<any[]>([]),
  quotes = ref<any[]>([])
const loading = ref(false),
  saving = ref(false),
  policyVisible = ref(false),
  baseVisible = ref(false),
  proofVisible = ref(false),
  proof = ref(''),
  detailProjects = ref<any[]>([])
const policyDraft = ref<any>({ projects: [] }),
  baseDraft = ref<any>({ declaredBases: {} }),
  policyRef = ref(),
  baseRef = ref(),
  month = ref(dayjs().format('YYYY-MM'))
const selected = computed(() => entities.value.find((e) => e.id === entityId.value)),
  manage = computed(() => !!selected.value?.allowedActions.includes('MANAGE')),
  canExport = computed(() => !!selected.value?.allowedActions.includes('EXPORT')),
  canReview = computed(() => !!selected.value?.allowedActions.includes('REVIEW')),
  canApprove = computed(() => !!selected.value?.allowedActions.includes('APPROVE'))
const required = [{ required: true, message: '请填写必填项', trigger: 'blur' }]
const types: Record<number, string> = {
  1: '养老保险',
  2: '医疗保险',
  3: '失业保险',
  4: '工伤保险',
  5: '生育保险',
  6: '补充大病医疗',
  7: '补充养老',
  8: '残保金',
  9: '社保自定义',
  10: '公积金',
  11: '公积金自定义'
}
const fields = [
  { key: 'minimumBase', label: '基数下限（元）' },
  { key: 'maximumBase', label: '基数上限（元）' },
  { key: 'personalRate', label: '个人比例 %', rate: true },
  { key: 'corporateRate', label: '单位比例 %', rate: true },
  { key: 'personalFixed', label: '个人固定额（元）' },
  { key: 'corporateFixed', label: '单位固定额（元）' }
]
const baseProjects = computed<api.Project[]>(() => {
  const p = policies.value.find((p) => p.id === baseDraft.value.policyId)
  return p ? JSON.parse(p.projects) : []
})
let loadVersion = 0
const load = async () => {
  const id = entityId.value,
    version = ++loadVersion
  policies.value = []
  bases.value = []
  events.value = []
  quotes.value = []
  importBatches.value = []
  templatePolicyId.value = undefined
  activeImport.value = undefined
  importVisible.value = false
  if (!id) return
  loading.value = true
  try {
    const result = await Promise.all([
      api.policies(id),
      api.bases(id),
      api.history(id),
      api.importList(id)
    ])
    if (version === loadVersion)
      [policies.value, bases.value, events.value, importBatches.value] = result
  } finally {
    if (version === loadVersion) loading.value = false
  }
}
const monthDates = (draft: any) => ({
  effectiveFrom: dayjs(`${draft.fromMonth}-01`).format('YYYY-MM-DD'),
  effectiveTo: dayjs(`${draft.toMonth}-01`).endOf('month').format('YYYY-MM-DD')
})
const openPolicy = (row?: api.Policy, replacing = false) => {
  if (!entityId.value) return
  policyDraft.value = row
    ? {
        ...row,
        entityId: entityId.value,
        projects: JSON.parse(row.projects),
        fromMonth: dayjs(row.effectiveFrom).format('YYYY-MM'),
        toMonth: dayjs(row.effectiveTo).format('YYYY-MM'),
        replacesId: replacing ? row.id : undefined,
        evidence: ''
      }
    : { entityId: entityId.value, projects: [] }
  policyVisible.value = true
  nextTick(() => policyRef.value?.clearValidate())
}
const savePolicy = async () => {
  await policyRef.value.validate()
  if (!policyDraft.value.projects.length) {
    message.warning('至少配置一个政策项目')
    return
  }
  saving.value = true
  try {
    await api.createPolicy({ ...policyDraft.value, ...monthDates(policyDraft.value) })
    policyVisible.value = false
    await load()
    message.success('政策已提交审批')
  } finally {
    saving.value = false
  }
}
const openBase = async (row?: any) => {
  if (!entityId.value) return
  employees.value = (await api.options(entityId.value)).employees
  baseDraft.value = row
    ? {
        ...row,
        declaredBases: JSON.parse(row.declaredBases),
        fromMonth: dayjs(row.effectiveFrom).format('YYYY-MM'),
        toMonth: dayjs(row.effectiveTo).format('YYYY-MM'),
        replacesId: row.id,
        evidence: ''
      }
    : { entityId: entityId.value, coverage: 'INSURED', declaredBases: {} }
  baseVisible.value = true
  await nextTick()
  baseRef.value?.clearValidate()
}
const policyChanged = () => {
  const existing = baseDraft.value.declaredBases
  baseDraft.value.declaredBases = Object.fromEntries(
    baseProjects.value.map((p) => [p.code, existing[p.code]])
  )
}
const coverageChanged = () => {
  if (baseDraft.value.coverage === 'EXEMPT') {
    baseDraft.value.policyId = undefined
    baseDraft.value.incomeYear = undefined
    baseDraft.value.declaredBases = {}
  }
}
const saveBase = async () => {
  await baseRef.value.validate()
  saving.value = true
  try {
    await api.proposeBase({ ...baseDraft.value, ...monthDates(baseDraft.value) })
    baseVisible.value = false
    await load()
    message.success('申报已提交 HR 复核')
  } finally {
    saving.value = false
  }
}
const act = async (kind: 'policy' | 'base', action: string, id: number) => {
  const { value: reason } = await ElMessageBox.prompt('请填写复核、审批或退回依据', '确认操作', {
    inputValidator: (v: string) => !!v?.trim() && v.length <= 1000,
    inputErrorMessage: '请填写 1 至 1000 字依据'
  })
  await api.transition(kind, action, { id, reason })
  await load()
  message.success('操作成功')
}
const calculate = async () => {
  if (!entityId.value || !month.value) return
  const [year, m] = month.value.split('-').map(Number)
  quotes.value = Object.values(await api.preview({ entityId: entityId.value, year, month: m }))
}
const showEvidence = (value: any) => {
  detailProjects.value = []
  try {
    proof.value = JSON.stringify(typeof value === 'string' ? JSON.parse(value) : value, null, 2)
  } catch {
    proof.value = String(value)
  }
  proofVisible.value = true
}
const showPolicy = (row: api.Policy) => {
  detailProjects.value = JSON.parse(row.projects)
  proofVisible.value = true
}
const downloadTemplate = async () => {
  if (templatePolicyId.value)
    download.excel(await api.baseTemplate(templatePolicyId.value), '年度缴费基数申报模板.xlsx')
}
const downloadErrors = async (id: number) =>
  download.excel(await api.importErrors(id), '缴费申报错误行.xlsx')
const inspectImport = async (id: number) => {
  activeImport.value = await api.importDetails(id)
  importVisible.value = true
}
const uploadImport = async (file: UploadFile) => {
  if (!entityId.value || !file.raw) return
  if (file.raw.size > 10 * 1024 * 1024) {
    message.warning('文件不能超过 10MB')
    return
  }
  const id = entityId.value
  importBusy.value = true
  try {
    const batch = await api.validateImport(id, file.raw)
    if (entityId.value !== id) return
    importBatches.value = await api.importList(id)
    activeImport.value = batch
    importVisible.value = true
  } finally {
    importBusy.value = false
  }
}
const submitImport = async () => {
  if (!activeImport.value) return
  const id = activeImport.value.id
  await message.confirm('将该文件的申报提交 HR 复核？批准前不会参与工资核算。')
  importBusy.value = true
  try {
    const ids = await api.commitImport(id)
    await load()
    await inspectImport(id)
    message.success(`已提交 ${ids.length} 位员工的申报`)
  } finally {
    importBusy.value = false
  }
}
onMounted(async () => {
  entities.value = (await EntityApi.list()).filter((e) => e.status === 'APPROVED')
  entityId.value = entities.value[0]?.id
  await load()
})
</script>
