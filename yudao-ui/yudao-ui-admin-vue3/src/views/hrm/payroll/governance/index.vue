<template>
  <ContentWrap class="governance-page">
    <h1>业务决议、发布与快照导出</h1>
    <el-alert
      title="TEST材料及合成签署仅验证系统实现，不构成真实业务批准。正式授权根必须有实际有权业务人员提供的受控签署材料。"
      type="warning"
      :closable="false"
    />
    <div class="toolbar">
      <el-select aria-label="治理项目" :model-value="projectId" @change="switchProject">
        <el-option v-if="anchors.length" label="租户项目创建授权" :value="0" />
        <el-option v-for="p in projects" :key="p.id" :label="`${p.id} · ${p.name}`" :value="p.id" />
      </el-select>
      <el-button :loading="loading" @click="load">刷新决议与发布</el-button>
      <el-button v-if="projectId" @click="goReview">返回十视图评审</el-button>
      <el-button
        v-if="projectId"
        @click="
          router.push({ path: '/hrm-payroll/requirement', query: { projectId, module: 'COL' } })
        "
        >需求、成员及实际来源</el-button
      >
    </div>
    <p
      >{{ project?.name || (projectId === 0 ? '租户创建授权' : '选择有权项目') }} · 范围版本
      {{ project?.projectScopeVersion ?? '未定义' }}</p
    >
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-alert v-if="success" :title="success" type="success" :closable="false" />
    <section aria-label="独立业务授权根">
      <h2>独立业务授权根</h2>
      <p>登记与签署分开。授权依据读权独立，旧测试授权不会自动升级为正式授权。</p>
      <el-button v-if="anchors.length && canPlatform('root-register')" @click="openRoot"
        >登记授权根材料</el-button
      >
      <p v-if="!anchors.length && !roots.length"
        >当前没有可见的独立登记材料；技术管理身份不能替代业务签署。</p
      >
      <div class="wide-table" tabindex="0" aria-label="授权根表，可横向滚动">
        <el-table :data="roots" row-key="id">
          <el-table-column prop="id" label="授权根ID" width="105" />
          <el-table-column prop="version" label="记录版本" width="100" />
          <el-table-column prop="data.materialVersion" label="材料版本" min-width="150" />
          <el-table-column prop="data.mode" label="材料性质" width="100" />
          <el-table-column prop="data.state" label="签署状态" width="170" />
          <el-table-column prop="data.userId" label="被授权人ID" width="115" />
          <el-table-column label="操作" min-width="310"
            ><template #default="{ row }">
              <el-button @click="showRecord(row)">查看受控材料</el-button>
              <el-button
                v-if="row.data.state === 'PENDING_SIGNATURE' && canPlatform('root-sign')"
                @click="openAction('root', row, 'SIGN')"
                >独立签署</el-button
              >
              <el-button
                v-if="row.data.state === 'ACTIVE' && canPlatform('root-sign')"
                @click="openAction('root', row, 'REVOKE')"
                >撤销授权根</el-button
              >
            </template></el-table-column
          >
        </el-table>
      </div>
    </section>
    <section v-if="projectId" aria-label="业务决议版本">
      <h2>业务决议版本与独立批准</h2>
      <p
        >必须覆盖Q-01/02/04/05/07/10。Q-03/06/08/09另行决议或明确分类；正文修订建立新版本，批准和撤销保留原事实。</p
      >
      <el-button v-if="caps.DECISION_EDIT && canPlatform('decision-edit')" @click="openDecision()"
        >起草决议版本</el-button
      >
      <p v-if="!caps.DECISION_READ">当前没有独立决议读权，不能依据需求读权查看决议正文。</p>
      <div class="wide-table" tabindex="0" aria-label="决议表，可横向滚动"
        ><el-table :data="decisions" row-key="id">
          <el-table-column prop="id" label="版本记录ID" width="115" />
          <el-table-column prop="data.title" label="决议标题" min-width="220" />
          <el-table-column prop="data.decisionVersion" label="决议版本" width="100" />
          <el-table-column prop="data.questions" label="覆盖问题" min-width="200" />
          <el-table-column prop="lifecycle" label="当前生命周期" width="170" />
          <el-table-column prop="data.approverId" label="指定批准人ID" width="130" />
          <el-table-column label="操作" min-width="510"
            ><template #default="{ row }">
              <el-button @click="showRecord(row)">查看版本正文</el-button>
              <el-button @click="history(row)">决议事件历史</el-button>
              <el-button
                v-if="caps.DECISION_EDIT && canPlatform('decision-edit')"
                @click="openDecision(row)"
                >基于此版本修订</el-button
              >
              <el-button
                v-if="
                  caps.DECISION_EDIT && row.data.state === 'DRAFT' && canPlatform('decision-edit')
                "
                @click="openAction('decision', row, 'SUBMIT')"
                >提交批准</el-button
              >
              <el-button
                v-if="
                  caps.DECISION_APPROVE &&
                  row.data.state === 'PENDING_APPROVAL' &&
                  canPlatform('decision-approve')
                "
                @click="openAction('decision', row, 'APPROVE')"
                >本人独立批准</el-button
              >
              <el-button
                v-if="
                  caps.DECISION_APPROVE &&
                  row.data.state === 'APPROVED' &&
                  canPlatform('decision-approve')
                "
                @click="openAction('decision', row, 'REVOKE')"
                >撤销决议</el-button
              >
            </template></el-table-column
          >
        </el-table></div
      >
    </section>
    <section v-if="projectId" aria-label="发布检查与固定快照">
      <h2>发布检查与固定快照</h2>
      <p
        >规格基线与数据可投产分开；供数待提供不会自动阻止规格明确的需求基线。正式发布须完整闭包可见及当前有效决议。</p
      >
      <el-button :disabled="busy || loading" @click="precheck">执行发布预检查</el-button>
      <p v-if="gate"
        >{{ gate.allowed ? '当前发布条件齐备，尚未因此发布' : '发布条件未齐备' }} · 范围版本
        {{ gate.projectScopeVersion }}</p
      >
      <ul v-if="gate"
        ><li v-for="(b, i) in gate.blocks" :key="i"
          >{{ blockLabels[b.code] || b.code }}{{ b.requirementId ? ` · 需求${b.requirementId}` : ''
          }}{{ b.question ? ` · ${b.question}` : '' }}</li
        ></ul
      >
      <el-form label-position="top">
        <el-form-item label="发布依据或草稿说明"
          ><el-input
            v-model="publishReason"
            type="textarea"
            :rows="3"
            aria-label="发布依据或草稿说明"
        /></el-form-item>
        <el-form-item label="幂等请求号"
          ><el-input v-model="requestKey" aria-label="幂等请求号"
        /></el-form-item>
      </el-form>
      <div class="toolbar">
        <el-button
          :loading="busy"
          :disabled="!gate?.allowed || !caps.BASELINE_PUBLISH || !publishReason"
          @click="publish(false)"
          >发布本期完整需求基线</el-button
        >
        <el-button
          :loading="busy"
          :disabled="!gate || !caps.EXPORT || !publishReason"
          @click="publish(true)"
          >保存固定草稿快照</el-button
        >
        <el-button v-if="lastPublish" :disabled="busy" @click="retryPublish"
          >重试相同发布请求</el-button
        >
      </div>
      <div class="wide-table" tabindex="0" aria-label="发布快照表，可横向滚动"
        ><el-table :data="releases" row-key="id">
          <el-table-column prop="id" label="固定快照ID" width="120" />
          <el-table-column prop="version" label="发布版本" width="100" />
          <el-table-column prop="mode" label="合成与正式" min-width="220" />
          <el-table-column label="类型" width="110"
            ><template #default="{ row }">{{
              row.draft ? '固定草稿' : '需求基线'
            }}</template></el-table-column
          >
          <el-table-column prop="createdAt" label="创建时间" min-width="200" />
          <el-table-column label="操作" min-width="320"
            ><template #default="{ row }">
              <el-button @click="readRelease(row.id)">查看冻结六表</el-button>
              <el-button v-if="caps.EXPORT" @click="createJob(row.id)">生成六表下载</el-button>
            </template></el-table-column
          >
        </el-table></div
      >
      <el-form inline
        ><el-form-item label="导出筛选需求ID（留空为全量可见）"
          ><el-input
            v-model="exportIds"
            aria-label="导出筛选需求ID"
            placeholder="例如 101,102" /></el-form-item
      ></el-form>
      <h3>本轮下载任务</h3>
      <p>任务创建、生成、结果交付及领取均按当前权限检查；旧地址不能替代重新鉴权。</p>
      <el-table :data="jobs" row-key="id"
        ><el-table-column prop="id" label="任务ID" /><el-table-column
          prop="releaseId"
          label="快照ID"
        /><el-table-column prop="state" label="状态" /><el-table-column
          prop="label"
          label="文件范围"
        /><el-table-column prop="error" label="提示" /><el-table-column label="操作" min-width="210"
          ><template #default="{ row }"
            ><el-button @click="refreshJob(row)">刷新下载任务</el-button
            ><el-button v-if="row.state === 'READY'" @click="claim(row)"
              >领取XLSX</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </section>
    <section v-if="frozen" aria-label="冻结六表内容">
      <h2>快照{{ frozen.releaseId }} · {{ frozen.label }}</h2>
      <el-tabs v-model="selectedSheet"
        ><el-tab-pane
          v-for="(rows, name) in frozen.sheets"
          :key="name"
          :label="String(name)"
          :name="String(name)"
        >
          <div class="wide-table" tabindex="0"
            ><el-table :data="rows"
              ><el-table-column prop="记录类型" label="记录类型" width="140" /><el-table-column
                prop="记录号"
                label="快照关联号"
                width="180"
              /><el-table-column label="内容" min-width="500"
                ><template #default="{ row }"
                  ><el-button @click="showRecord(row)">展开冻结记录</el-button
                  ><span>{{
                    row['功能'] || row['名称'] || row['完整性'] || row['说明'] || row['编号'] || ''
                  }}</span></template
                ></el-table-column
              ></el-table
            ></div
          >
        </el-tab-pane></el-tabs
      >
    </section>
    <el-dialog
      v-model="rootOpen"
      title="登记授权根材料"
      width="min(900px, 94vw)"
      class="governance-dialog"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="独立登记材料"
          ><el-select v-model="rootForm.anchorId" aria-label="独立登记材料"
            ><el-option
              v-for="a in anchors"
              :key="a.id"
              :label="`${a.id} · ${a.data.mode} · ${a.data.materialVersion}`"
              :value="a.id" /></el-select
        ></el-form-item>
        <el-form-item label="被授权人ID"
          ><el-input-number v-model="rootForm.userId" aria-label="被授权人ID" :min="1"
        /></el-form-item>
        <el-form-item label="许可动作"
          ><el-checkbox-group v-model="rootForm.actions" aria-label="许可动作"
            ><el-checkbox v-for="(name, key) in actionLabels" :key="key" :label="key">{{
              name
            }}</el-checkbox></el-checkbox-group
          ></el-form-item
        >
        <el-form-item label="可继续委派动作"
          ><el-select v-model="rootForm.delegableActions" multiple aria-label="可继续委派动作"
            ><el-option
              v-for="key in rootForm.actions"
              :key="key"
              :label="actionLabels[key]"
              :value="key" /></el-select
        ></el-form-item>
        <el-form-item v-for="f in rootFields" :key="f.key" :label="f.label"
          ><el-input
            v-model="rootForm[f.key]"
            :type="f.long ? 'textarea' : 'text'"
            :rows="f.long ? 5 : undefined"
            :aria-label="f.label"
        /></el-form-item> </el-form
      ><template #footer
        ><el-button @click="rootOpen = false">取消登记</el-button
        ><el-button :loading="busy" @click="saveRoot">保存待签署材料</el-button></template
      >
    </el-dialog>
    <el-dialog
      v-model="decisionOpen"
      title="起草不可变决议版本"
      width="min(950px, 94vw)"
      class="governance-dialog"
      :close-on-click-modal="false"
    >
      <p
        >当前项目{{ projectId }}，范围版本{{
          decisionForm.projectScopeVersion
        }}。保存为新版本，不覆盖旧正文或签署。</p
      >
      <el-form label-position="top">
        <el-form-item label="决议标题"
          ><el-input v-model="decisionForm.title" aria-label="决议标题"
        /></el-form-item>
        <el-form-item label="覆盖问题"
          ><el-checkbox-group v-model="decisionForm.questions" aria-label="覆盖问题"
            ><el-checkbox v-for="q in questions" :key="q" :label="q">{{
              q
            }}</el-checkbox></el-checkbox-group
          ></el-form-item
        >
        <el-form-item v-for="f in decisionFields" :key="f.key" :label="f.label"
          ><el-input
            v-model="decisionForm[f.key]"
            :type="f.long ? 'textarea' : 'text'"
            :rows="f.long ? 6 : undefined"
            :aria-label="f.label"
        /></el-form-item>
        <el-form-item label="指定独立批准人ID"
          ><el-input-number
            v-model="decisionForm.approverId"
            :min="1"
            aria-label="指定独立批准人ID"
        /></el-form-item>
        <el-form-item label="其他未决问题分类"
          ><div v-for="q in otherQuestions" :key="q"
            ><label>{{ q }}</label
            ><el-select v-model="decisionForm.otherQuestions[q]" :aria-label="`${q}分类`"
              ><el-option label="基线阻断" value="BASELINE_BLOCK" /><el-option
                label="数据准备"
                value="DATA_READINESS" /><el-option
                label="建议"
                value="ADVISORY" /></el-select></div
        ></el-form-item>
        <el-checkbox v-model="decisionForm.semanticChange"
          >本版本改变业务语义或验收条件</el-checkbox
        > </el-form
      ><template #footer
        ><el-button @click="decisionOpen = false">取消起草</el-button
        ><el-button :loading="busy" @click="saveDecision">保存决议草稿</el-button></template
      >
    </el-dialog>
    <el-dialog
      v-model="actionOpen"
      title="记录本人动作与依据"
      width="min(700px, 94vw)"
      class="governance-dialog"
      :close-on-click-modal="false"
      ><p
        >{{ actionForm.kind }} · 记录{{ actionForm.id }} · {{ actionForm.action }} · 版本{{
          actionForm.version
        }}</p
      ><el-input
        v-model="actionForm.reason"
        type="textarea"
        :rows="6"
        aria-label="本人动作依据"
      /><template #footer
        ><el-button @click="actionOpen = false">取消本人动作</el-button
        ><el-button :loading="busy" :disabled="!actionForm.reason.trim()" @click="saveAction"
          >提交本人动作</el-button
        ></template
      ></el-dialog
    >
    <el-dialog
      v-model="recordOpen"
      title="冻结记录与受控正文"
      width="min(1100px, 94vw)"
      class="governance-dialog"
    >
      <pre class="record">{{ recordText }}</pre
      ><template #footer
        ><el-button @click="recordOpen = false">关闭记录</el-button></template
      ></el-dialog
    >
  </ContentWrap>
</template>

<script setup lang="ts">
import * as api from '@/api/hrm/payroll/governance'
import { listRequirementProjects, requirementCapabilities } from '@/api/hrm/payroll/requirement'
import download from '@/utils/download'
import { checkPermi } from '@/utils/permission'
const canPlatform = (action: string) => checkPermi(['hrm:payroll:requirement:' + action])
const router = useRouter(),
  route = useRoute()
const projectId = computed(() =>
  route.query.projectId == null ? undefined : Number(route.query.projectId)
)
const projects = ref<any[]>([]),
  anchors = ref<any[]>([]),
  roots = ref<any[]>([]),
  decisions = ref<any[]>([]),
  releases = ref<any[]>([])
const caps = ref<any>({}),
  gate = ref<any>(),
  frozen = ref<any>(),
  jobs = ref<any[]>([])
const project = computed(() => projects.value.find((p) => p.id === projectId.value))
const loading = ref(false),
  busy = ref(false),
  error = ref(''),
  success = ref(''),
  publishReason = ref(''),
  requestKey = ref(crypto.randomUUID()),
  exportIds = ref(''),
  lastPublish = ref<any>()
const rootOpen = ref(false),
  decisionOpen = ref(false),
  actionOpen = ref(false),
  recordOpen = ref(false),
  recordText = ref(''),
  selectedSheet = ref('评审版本说明')
const rootForm = ref<any>({}),
  decisionForm = ref<any>({}),
  actionForm = ref<any>({ reason: '' })
const questions = Array.from({ length: 10 }, (_, n) => `Q-${String(n + 1).padStart(2, '0')}`),
  otherQuestions = ['Q-03', 'Q-06', 'Q-08', 'Q-09']
const actionLabels: Record<string, string> = {
  READ: '读取项目',
  META: '修改项目名称',
  EDIT: '编辑需求',
  ASSIGN: '分派评审',
  CONFIRM: '本人确认需求',
  EXPORT: '导出',
  MEMBER: '管理成员',
  SCOPE: '修订项目范围',
  SCOPE_DECIDE: '需求范围取舍',
  SOURCE_EDIT: '维护来源',
  SOURCE_VERIFY: '核验来源质量',
  ISSUE_MANAGE: '管理问题',
  ISSUE_HANDLE: '处理问题',
  ISSUE_REVIEW: '独立复核问题',
  DECISION_READ: '读取决议正文',
  GRANT_READ: '读取授权依据',
  PERSON_READ: '读取关联人员显示名',
  DECISION_EDIT: '起草决议',
  DECISION_APPROVE: '独立批准决议',
  BASELINE_PUBLISH: '发布基线',
  PROJECT_CREATE: '创建项目'
}
const rootFields = [
  { key: 'scopeJson', label: '授权范围正文', long: true },
  { key: 'validFrom', label: '授权开始日期' },
  { key: 'validTo', label: '授权结束日期' },
  { key: 'materialVersion', label: '受控材料版本' },
  { key: 'body', label: '受控签署材料正文', long: true },
  { key: 'reason', label: '登记依据', long: true }
]
const decisionFields = [
  { key: 'clauses', label: '覆盖条款', long: true },
  { key: 'clauseVersion', label: '条款引用版本' },
  { key: 'body', label: '决议受控正文', long: true },
  { key: 'scopeJson', label: '决议适用范围正文', long: true },
  { key: 'validFrom', label: '决议开始日期' },
  { key: 'validTo', label: '决议结束日期' },
  { key: 'approvalConditions', label: '批准条件', long: true },
  { key: 'reason', label: '起草或修订依据', long: true }
]
const blockLabels: Record<string, string> = {
  PROJECT_ARCHIVED: '项目已归档',
  FULL_CLOSURE_READ_REQUIRED: '缺少完整快照闭包读权',
  BASELINE_PUBLISH_REQUIRED: '缺少独立发布权限',
  CURRENT_CONTROLLED_PUBLISH_AUTHORITY_REQUIRED: '缺少当前有效的受控发布授权',
  CURRENT_DECISION_COVERAGE_REQUIRED: '当前范围的强制决议覆盖不完整或冲突',
  UNRESOLVED_QUESTION_CLASSIFICATION: '其他未决问题尚未明确分类',
  UNRESOLVED_BASELINE_QUESTION: '仍有基线阻断问题',
  EMPTY_INCLUDED_SET: '本期纳入集合为空',
  SCOPE_MISMATCH: '需求范围与当前项目不匹配',
  CURRENT_CONFIRMATION_REQUIRED: '需要当前有效的本人确认',
  CONFIRMATION_GOVERNANCE_FACT_REQUIRED: '旧确认缺少S5批准及授权事实，需重新确认',
  SPECIFICATION_NOT_VERIFIED: '来源或映射规格尚未核验',
  EXCLUSION_BASIS_INCOMPLETE: '排除理由、责任人或重新评估条件不齐备',
  DUPLICATE_TARGET_INVALID: '重复移除的保留项无效',
  BASELINE_ISSUES_OPEN: '存在相关未关闭基线阻断问题'
}
let ticket = 0
async function load() {
  success.value = ''
  const own = ++ticket,
    id = projectId.value
  loading.value = true
  error.value = ''
  roots.value = []
  decisions.value = []
  releases.value = []
  caps.value = {}
  gate.value = undefined
  frozen.value = undefined
  try {
    const [ps, as] = await Promise.all([listRequirementProjects(), api.get('/anchors')])
    if (own !== ticket) return
    projects.value = ps
    anchors.value = as
    if (id == null) return
    const loadedRoots = await api.get('/roots', { projectId: id })
    if (own !== ticket) return
    roots.value = loadedRoots
    if (id) {
      const c = await requirementCapabilities(id)
      const [ds, rs] = await Promise.all([
        c.DECISION_READ ? api.get('/decisions', { projectId: id }) : [],
        api.get('/releases', { projectId: id })
      ])
      if (own !== ticket) return
      caps.value = c
      decisions.value = ds
      releases.value = rs
    }
  } catch {
    if (own === ticket)
      error.value = '真实决议或发布数据未加载：请核对当前权限或重试，未返回内容不推定为空或通过。'
  } finally {
    if (own === ticket) loading.value = false
  }
}
function switchProject(id: number) {
  lastPublish.value = undefined
  jobs.value = []
  success.value = ''
  rootOpen.value = decisionOpen.value = actionOpen.value = false
  router.replace({ path: route.path, query: { projectId: id } })
}
function goReview() {
  router.push({ path: '/hrm-payroll/review', query: { projectId: projectId.value, module: 'COL' } })
}
function showRecord(row: any) {
  recordText.value = JSON.stringify(row, null, 2)
  recordOpen.value = true
}
async function history(row: any) {
  success.value = ''
  showRecord(await api.get('/decision/history', { projectId: projectId.value, id: row.id }))
}
function openRoot() {
  success.value = ''
  rootForm.value = {
    anchorId: anchors.value[0]?.id,
    userId: undefined,
    actions: [],
    delegableActions: [],
    scopeJson: project.value?.scopeJson || '',
    validFrom: '',
    validTo: '',
    materialVersion: '',
    body: '',
    reason: ''
  }
  rootOpen.value = true
}
function openDecision(row?: any) {
  success.value = ''
  decisionForm.value = row
    ? {
        ...JSON.parse(JSON.stringify(row.data)),
        previousId: row.id,
        reason: '',
        projectScopeVersion: project.value?.projectScopeVersion
      }
    : {
        projectScopeVersion: project.value?.projectScopeVersion,
        title: '',
        questions: [],
        clauses: '',
        clauseVersion: '',
        body: '',
        scopeJson: project.value?.scopeJson || '',
        validFrom: '',
        validTo: '',
        approverId: undefined,
        approvalConditions: '',
        semanticChange: false,
        otherQuestions: {},
        reason: ''
      }
  decisionOpen.value = true
}
async function saveRoot() {
  success.value = ''
  busy.value = true
  error.value = ''
  try {
    const id = await api.post('/root/register', { ...rootForm.value, projectId: projectId.value })
    rootOpen.value = false
    await load()
    success.value = `已登记待签署授权根${id}，尚未生效`
  } catch {
    error.value = '登记未完成；当前输入已保留，请核对独立材料、人员、动作和范围。'
  } finally {
    busy.value = false
  }
}
async function saveDecision() {
  success.value = ''
  busy.value = true
  error.value = ''
  try {
    const id = await api.post('/decision/draft', {
      ...decisionForm.value,
      projectId: projectId.value
    })
    decisionOpen.value = false
    await load()
    success.value = `已保存决议草稿记录${id}，尚未批准`
  } catch {
    error.value = '决议草稿未保存，输入已保留；请核对必填项、当前范围和独立授权。'
  } finally {
    busy.value = false
  }
}
function openAction(kind: string, row: any, action: string) {
  success.value = ''
  actionForm.value = { kind, id: row.id, version: row.version, action, reason: '' }
  actionOpen.value = true
}
async function saveAction() {
  success.value = ''
  busy.value = true
  error.value = ''
  try {
    await api.post(`/${actionForm.value.kind}/action`, {
      ...actionForm.value,
      projectId: projectId.value
    })
    actionOpen.value = false
    await load()
    success.value = '本人动作已保存，生命周期按有效期和当前状态计算'
  } catch {
    error.value = '本人动作被拒绝或版本已变；依据输入已保留，不视为批准成功。'
  } finally {
    busy.value = false
  }
}
async function precheck() {
  success.value = ''
  const own = ticket,
    target = projectId.value
  busy.value = true
  error.value = ''
  try {
    const result = await api.get('/precheck', { projectId: target })
    if (own === ticket && target === projectId.value) gate.value = result
  } catch {
    gate.value = undefined
    error.value = '发布预检查未完成；不能把未知结果视为通过。'
  } finally {
    busy.value = false
  }
}
async function publish(draft: boolean) {
  lastPublish.value = {
    projectId: projectId.value,
    projectScopeVersion: gate.value.projectScopeVersion,
    requestKey: requestKey.value,
    dependencyToken: gate.value.dependencyToken,
    reason: publishReason.value,
    draft
  }
  await retryPublish()
}
async function retryPublish() {
  success.value = ''
  busy.value = true
  error.value = ''
  try {
    const id = await api.post('/publish', lastPublish.value)
    await load()
    success.value = `固定快照ID：${id}。相同请求重试仍须当前资格有效。`
  } catch {
    error.value = '发布或重试未完成，说明及请求号已保留；请重新检查当前授权、决议、范围和依赖版本。'
  } finally {
    busy.value = false
  }
}
async function readRelease(releaseId: number) {
  success.value = ''
  const own = ticket,
    target = projectId.value
  try {
    const result = await api.get('/release', { projectId: target, releaseId })
    if (own !== ticket || target !== projectId.value) return
    frozen.value = result
    selectedSheet.value = '评审版本说明'
  } catch {
    frozen.value = undefined
    error.value = '当前权限不能读取该快照，未展示旧缓存内容。'
  }
}
async function createJob(releaseId: number) {
  success.value = ''
  const own = ticket,
    target = projectId.value
  try {
    const ids = exportIds.value.trim()
      ? exportIds.value.split(',').map((x) => Number(x.trim()))
      : undefined
    if (ids?.some((x) => !Number.isSafeInteger(x) || x <= 0)) throw new Error('invalid ids')
    const id = await api.post('/export/create', {
      projectId: target,
      releaseId,
      requirementIds: ids
    })
    if (own !== ticket || target !== projectId.value) return
    const row = { id, projectId: target, releaseId, state: 'QUEUED', label: '', error: '' }
    jobs.value.push(row)
    await refreshJob(row)
  } catch {
    error.value = '导出任务未创建，请核对筛选需求ID及当前导出权限。'
  }
}
async function refreshJob(row: any) {
  success.value = ''
  try {
    const result = await api.get('/export/status', { projectId: row.projectId, id: row.id })
    jobs.value = jobs.value.map((job) => (job.id === row.id ? { ...job, ...result } : job))
  } catch {
    jobs.value = jobs.value.map((job) =>
      job.id === row.id
        ? { ...job, state: 'UNKNOWN', error: '当前权限或版本已变化，无法交付旧任务。' }
        : job
    )
  }
}
async function claim(row: any) {
  success.value = ''
  try {
    const blob = await api.download({ projectId: row.projectId, id: row.id })
    download.excel(blob, `薪酬评审-快照${row.releaseId}-任务${row.id}.xlsx`)
  } catch {
    frozen.value = undefined
    gate.value = undefined
    jobs.value = jobs.value.map((job) =>
      job.id === row.id
        ? { ...job, state: 'DENIED', error: '领取被拒绝；旧任务或地址不能绕过当前权限。' }
        : job
    )
  }
}
watch(() => route.query.projectId, load, { immediate: true })
onBeforeUnmount(() => {
  ticket++
})
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 14px 0;
}

.toolbar :deep(.el-select) {
  min-width: 230px;
}

section {
  margin-top: 30px;
}

.wide-table {
  max-width: 100%;
  overflow-x: auto;
}

.wide-table :deep(.el-table) {
  min-width: 900px;
}

.record {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

h1 {
  font-size: 26px;
}

@media (width <= 600px) {
  h1 {
    font-size: 22px;
  }

  .toolbar {
    display: grid;
  }

  .governance-page {
    min-width: 0;
  }
}
</style>
<style>
.governance-dialog .el-dialog__body {
  max-height: 58vh;
  overflow-y: auto;
}
</style>
