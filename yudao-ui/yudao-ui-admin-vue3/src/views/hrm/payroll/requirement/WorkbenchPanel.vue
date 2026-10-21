<template>
  <ContentWrap v-if="projectId" class="mt-4">
    <el-alert
      title="实际来源与候选目录分开；功能确认、语义核验、QS/QC和问题关闭分别操作。只登记规格或虚构样例引用，不填写口令或真实工资明细。"
      :closable="false"
    />
    <el-space class="my-3" wrap>
      <el-button @click="load">刷新来源工作台</el-button>
      <el-button v-hasPermi="['hrm:payroll:requirement:export']" @click="exportCurrent"
        >导出当前来源关系</el-button
      >
      <el-button v-hasPermi="['hrm:payroll:requirement:source-edit']" @click="editSource()"
        >登记实际来源</el-button
      >
      <el-button v-hasPermi="['hrm:payroll:requirement:update']" @click="editRelation()"
        >建立消费关系</el-button
      >
      <el-button v-hasPermi="['hrm:payroll:requirement:issue-manage']" @click="createIssue"
        >登记评审问题</el-button
      >
    </el-space>
    <el-alert
      v-if="error"
      title="工作台加载失败，请重试；未显示的对象不推定为空或通过。"
      type="error"
      :closable="false"
    />
    <el-tabs v-model="activeTab">
      <el-tab-pane label="实际来源" name="sources">
        <el-table :data="sources" row-key="id">
          <el-table-column prop="id" label="sourceId" width="90" />
          <el-table-column prop="data.name" label="名称" min-width="160" />
          <el-table-column label="语义/运营/交付版本" width="170"
            ><template #default="{ row }"
              >{{ row.data.semanticVersion }} / {{ row.data.operationalVersion }} /
              {{ row.data.deliveryVersion }}</template
            ></el-table-column
          >
          <el-table-column label="供给 / 语义 / QS" min-width="210"
            ><template #default="{ row }"
              >{{ supplyNames[row.data.supply] }} /
              {{ row.sourceSemanticVerified ? '已核验' : '待核验' }} /
              {{ row.QSResult ? '通过' : '未通过或待检查' }}</template
            ></el-table-column
          >
          <el-table-column label="操作" min-width="360"
            ><template #default="{ row }">
              <el-button link @click="editSource(row)">规格详情</el-button>
              <el-button
                v-hasPermi="['hrm:payroll:requirement:source-edit']"
                link
                @click="supply(row)"
                >供给交付</el-button
              >
              <el-button
                v-hasPermi="['hrm:payroll:requirement:source-verify']"
                link
                @click="verify(row, 'SOURCE_SEMANTIC')"
                >规格核验</el-button
              >
              <el-button
                v-hasPermi="['hrm:payroll:requirement:source-verify']"
                link
                @click="verify(row, 'QS')"
                >QS检查</el-button
              >
              <el-button link @click="propose(row)">变更建议</el-button>
              <el-button link @click="copySource(row)">跨项目复制</el-button>
              <el-button link @click="history(row)">来源历史</el-button>
            </template></el-table-column
          >
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="消费关系与映射" name="relations">
        <el-alert
          title="可用仅表示当前关系满足供给、范围、语义、QS/QC及未决问题条件，不表示功能已经确认或系统已实施。"
          :closable="false"
        />
        <el-table :data="relations" row-key="id">
          <el-table-column prop="id" label="relationId" width="100" />
          <el-table-column prop="data.requirementId" label="需求ID" width="90" />
          <el-table-column prop="data.sourceId" label="来源ID" width="90" />
          <el-table-column prop="data.mappingVersion" label="映射版本" width="100" />
          <el-table-column label="状态" min-width="160"
            ><template #default="{ row }">{{
              !row.data.active
                ? '历史失活'
                : row.available
                  ? '已核验可用'
                  : row.semanticVerified
                    ? '语义通过，质量或供给待核验'
                    : '待核验'
            }}</template></el-table-column
          >
          <el-table-column label="操作" min-width="300"
            ><template #default="{ row }">
              <el-button link @click="editRelation(row)">映射详情</el-button>
              <template v-if="row.data.active">
                <el-button
                  v-hasPermi="['hrm:payroll:requirement:source-verify']"
                  link
                  @click="verify(row, 'RELATION_SEMANTIC')"
                  >映射核验</el-button
                >
                <el-button
                  v-hasPermi="['hrm:payroll:requirement:source-verify']"
                  link
                  @click="verify(row, 'QC')"
                  >QC检查</el-button
                >
                <el-button v-hasPermi="['hrm:payroll:requirement:update']" link @click="unlink(row)"
                  >解除关联</el-button
                >
              </template>
              <el-button link @click="history(row)">关系历史</el-button>
            </template></el-table-column
          >
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="问题处理与独立复核" name="issues">
        <el-table :data="issues" row-key="id">
          <el-table-column prop="id" label="issueId" width="85" />
          <el-table-column prop="data.description" label="问题" min-width="180" />
          <el-table-column label="状态 / 类别 / 期限" min-width="260"
            ><template #default="{ row }"
              >{{ issueStates[row.data.state] }} / {{ row.data.category }} / {{ row.data.dueDate }}
              {{ row.dates?.overdue ? '已逾期' : '' }}</template
            ></el-table-column
          >
          <el-table-column label="责任人 / 复核人" width="160"
            ><template #default="{ row }"
              >{{ row.data.handlerId }} / {{ row.data.reviewerId }}</template
            ></el-table-column
          >
          <el-table-column label="操作" min-width="300"
            ><template #default="{ row }">
              <el-button
                v-hasPermi="['hrm:payroll:requirement:issue-handle']"
                v-if="row.data.state === 'OPEN'"
                link
                @click="issueAction(row, 'START')"
                >开始处理</el-button
              >
              <el-button
                v-hasPermi="['hrm:payroll:requirement:issue-handle']"
                v-if="row.data.state === 'PROCESSING'"
                link
                @click="issueAction(row, 'SUBMIT')"
                >提交复核</el-button
              >
              <template v-if="row.data.state === 'REVIEW'">
                <el-button
                  v-hasPermi="['hrm:payroll:requirement:issue-review']"
                  link
                  @click="issueAction(row, 'REJECT')"
                  >驳回</el-button
                >
                <el-button
                  v-hasPermi="['hrm:payroll:requirement:issue-review']"
                  link
                  @click="issueAction(row, 'CLOSE')"
                  >独立复核关闭</el-button
                >
              </template>
              <el-button v-if="row.data.state === 'CLOSED'" link @click="issueAction(row, 'REOPEN')"
                >重开问题</el-button
              >
              <el-button
                v-hasPermi="['hrm:payroll:requirement:issue-review']"
                v-if="row.data.state !== 'CLOSED'"
                link
                @click="issueAction(row, 'RECLASSIFY')"
                >复核分类及影响</el-button
              >
              <el-button
                v-hasPermi="['hrm:payroll:requirement:issue-manage']"
                v-if="row.data.state !== 'CLOSED'"
                link
                @click="issueAction(row, 'REASSIGN')"
                >重新分派</el-button
              >
              <el-button link @click="history(row)">问题历史</el-button>
            </template></el-table-column
          >
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="核验证据" name="evidence"
        ><el-table :data="evidence"
          ><el-table-column prop="id" label="evidenceId" width="100" /><el-table-column
            prop="data.layer"
            label="层级"
            width="180"
          /><el-table-column prop="data.objectId" label="对象ID" width="90" /><el-table-column
            prop="data.result"
            label="结果"
            width="90"
          /><el-table-column
            prop="data.reference"
            label="执行依据"
            min-width="200"
          /><el-table-column prop="data.executedAt" label="执行时间" width="190" /><el-table-column
            label="绑定元组"
            min-width="220"
            ><template #default="{ row }">
              <pre>{{ JSON.stringify(row.data.binding, null, 2) }}</pre>
            </template></el-table-column
          ></el-table
        ></el-tab-pane
      >
      <el-tab-pane label="站内待办与建议" name="todos"
        ><el-table :data="[...todos, ...proposals]"
          ><el-table-column prop="id" label="编号" width="80" /><el-table-column
            prop="data.reason"
            label="原因"
            min-width="220" /><el-table-column
            prop="data.suggestion"
            label="建议"
            min-width="180" /><el-table-column
            prop="data.affectedRequirementIds"
            label="当前可见影响需求"
            min-width="180" /><el-table-column
            prop="data.state"
            label="状态"
            width="160" /></el-table
      ></el-tab-pane>
    </el-tabs>
  </ContentWrap>
  <Dialog v-model="opened" :title="title" width="min(1100px, 95vw)">
    <el-alert v-if="failed" :title="failureMessage" type="error" :closable="false" class="mb-3" />
    <el-form label-width="140px" :model="draft">
      <template v-if="mode === 'source'">
        <el-form-item label="来源名称"><el-input v-model="draft.source.name" /></el-form-item>
        <el-form-item label="模块"
          ><el-select v-model="draft.source.module"
            ><el-option v-for="(v, k) in modules" :key="k" :label="v" :value="k" /></el-select
        ></el-form-item>
        <ScopeFields v-model="draft.source.scopeJson" />
        <el-form-item label="业务负责人"
          ><el-select v-model="draft.source.ownerId" filterable
            ><el-option
              v-for="u in users"
              :key="u.id"
              :value="u.id"
              :label="`${u.nickname} (${u.id})`" /></el-select
        ></el-form-item>
        <el-form-item v-for="(v, k) in sourceLabels" :key="k" :label="v"
          ><el-input v-model="draft.source[k]"
        /></el-form-item>
        <el-form-item label="字段规格"
          ><el-button @click="draft.source.fields.push(newField())"
            >添加规格字段</el-button
          ></el-form-item
        >
        <el-table :data="draft.source.fields"
          ><el-table-column v-for="(v, k) in fieldLabels" :key="k" :label="v" min-width="130"
            ><template #default="{ row }"><el-input v-model="row[k]" /></template></el-table-column
          ><el-table-column label="必填" width="70"
            ><template #default="{ row }"
              ><el-switch v-model="row.required" /></template></el-table-column
          ><el-table-column label="操作" width="80"
            ><template #default="{ $index }"
              ><el-button link @click="draft.source.fields.splice($index, 1)"
                >移除</el-button
              ></template
            ></el-table-column
          ></el-table
        >
      </template>
      <template v-if="mode === 'relation'">
        <el-form-item label="需求"
          ><el-select v-model="draft.relation.requirementId" :disabled="!!draft.id"
            ><el-option
              v-for="r in requirements"
              :key="r.id"
              :value="r.id"
              :label="`${r.id} ${r.description}`" /></el-select
        ></el-form-item>
        <el-form-item label="实际来源"
          ><el-select v-model="draft.relation.sourceId" :disabled="!!draft.id"
            ><el-option
              v-for="s in sources"
              :key="s.id"
              :value="s.id"
              :label="`${s.id} ${s.data.name}`" /></el-select
        ></el-form-item>
        <el-form-item label="消费检查规则版本"
          ><el-input v-model="draft.relation.consumptionRule"
        /></el-form-item>
        <el-button @click="draft.relation.fields.push(newMapping())">添加消费字段</el-button>
        <div v-for="(f, i) in draft.relation.fields" :key="i" class="mapping-row">
          <el-form-item v-for="(v, k) in mappingLabels" :key="k" :label="v"
            ><el-input v-model="f[k]"
          /></el-form-item>
          <el-form-item label="必填"><el-switch v-model="f.required" /></el-form-item
          ><el-button @click="draft.relation.fields.splice(i, 1)">移除该映射</el-button>
        </div>
      </template>
      <template v-if="mode === 'supply'">
        <el-form-item label="供给状态"
          ><el-select v-model="draft.supply"
            ><el-option v-for="(v, k) in supplyNames" :key="k" :label="v" :value="k" /></el-select
        ></el-form-item>
        <el-form-item label="替换交付内容"
          ><el-switch v-model="draft.replaceDelivery"
        /></el-form-item>
        <template v-if="draft.replaceDelivery"
          ><el-form-item label="交付内容摘要"
            ><el-input v-model="draft.deliveryDigest" /></el-form-item
          ><el-form-item label="受控样例引用"
            ><el-input v-model="draft.deliveryReference" /></el-form-item
        ></template>
      </template>
      <template v-if="mode === 'verify'">
        <el-alert
          :title="`当前 ${draft.layer} 绑定摘要：${draft.expectedBinding}。本次生成新证据，不能改挂旧检查结果。`"
          :closable="false"
        />
        <el-form-item label="检查结果"
          ><el-select v-model="draft.result"
            ><el-option label="通过" value="PASS" /><el-option
              label="失败"
              value="FAIL" /></el-select
        ></el-form-item>
        <el-form-item label="执行依据"><el-input v-model="draft.reference" /></el-form-item
        ><el-form-item label="样本内容摘要"><el-input v-model="draft.sampleDigest" /></el-form-item>
      </template>
      <template
        v-if="
          mode === 'issue' ||
          (mode === 'action' && ['RECLASSIFY', 'REASSIGN'].includes(draft.action))
        "
      >
        <template v-if="mode === 'issue' || draft.action === 'RECLASSIFY'">
          <el-form-item v-if="mode === 'issue'" label="问题描述"
            ><el-input v-model="draft.issue.description" type="textarea"
          /></el-form-item>
          <el-form-item label="关联需求"
            ><el-select v-model="issueDraft.requirementIds" multiple
              ><el-option
                v-for="r in requirements"
                :key="r.id"
                :value="r.id"
                :label="`${r.id} ${r.description}`" /></el-select
          ></el-form-item>
          <el-form-item label="关联来源"
            ><el-select v-model="issueDraft.sourceIds" multiple
              ><el-option
                v-for="s in sources"
                :key="s.id"
                :value="s.id"
                :label="s.data.name" /></el-select
          ></el-form-item>
          <el-form-item v-if="mode === 'issue'" label="关联消费关系"
            ><el-select v-model="draft.issue.relationIds" multiple
              ><el-option
                v-for="r in relations.filter((r) => r.data.active)"
                :key="r.id"
                :value="r.id"
                :label="`${r.id} · 需求${r.data.requirementId} / 来源${r.data.sourceId}`" /></el-select
          ></el-form-item>
          <el-form-item label="阻断类别"
            ><el-select v-model="issueDraft.category"
              ><el-option
                v-for="c in ['BASELINE_BLOCK', 'DATA_READINESS', 'ADVISORY']"
                :key="c"
                :value="c"
                :label="c" /></el-select
          ></el-form-item>
          <el-form-item label="影响全项目"
            ><el-switch v-model="issueDraft.wholeProject"
          /></el-form-item>
        </template>
        <template v-if="mode === 'issue' || draft.action === 'REASSIGN'">
          <el-form-item label="处理责任人"
            ><el-select v-model="issueDraft.handlerId" filterable
              ><el-option
                v-for="u in users"
                :key="u.id"
                :value="u.id"
                :label="`${u.nickname} (${u.id})`" /></el-select
          ></el-form-item>
          <el-form-item label="独立复核人"
            ><el-select v-model="issueDraft.reviewerId" filterable
              ><el-option
                v-for="u in users"
                :key="u.id"
                :value="u.id"
                :label="`${u.nickname} (${u.id})`" /></el-select
          ></el-form-item>
        </template>
        <el-form-item v-if="mode === 'issue'" label="处理期限"
          ><el-date-picker v-model="draft.issue.dueDate" value-format="YYYY-MM-DD"
        /></el-form-item>
      </template>
      <template v-if="mode === 'action'"
        ><el-form-item
          label="处理或复核依据"
          :error="fieldErrors.evidence ? '请填写处理或复核依据' : ''"
          ><el-input
            v-model="draft.evidence"
            aria-label="处理或复核依据"
            type="textarea" /></el-form-item
        ><el-form-item
          v-if="draft.action === 'REOPEN'"
          label="新期限"
          :error="fieldErrors.dueDate ? '请填写本轮新期限' : ''"
          ><el-date-picker
            v-model="draft.dueDate"
            aria-label="新期限"
            value-format="YYYY-MM-DD" /></el-form-item
      ></template>
      <template v-if="mode === 'proposal'"
        ><el-form-item label="建议内容"
          ><el-input v-model="draft.suggestion" type="textarea" /></el-form-item
      ></template>
      <template v-if="mode === 'copy'"
        ><el-form-item label="目标项目ID"
          ><el-input-number v-model="draft.projectId" :min="1" /></el-form-item
        ><el-form-item label="目标当前范围版本"
          ><el-input-number v-model="draft.projectScopeVersion" :min="1" /></el-form-item
        ><el-alert
          title="仅复制规格，新来源不继承供给、核验、质量结论或签字；需具备两端权限。"
          :closable="false"
      /></template>
      <el-form-item label="本次操作原因" :error="fieldErrors.reason ? '请填写本次操作原因' : ''"
        ><el-input v-model="draft.reason" aria-label="本次操作原因" type="textarea"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button :disabled="saving" @click="opened = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save">提交并保存</el-button></template
    >
  </Dialog>
  <Dialog v-model="historyOpen" title="工作台对象历史" width="min(1100px,95vw)"
    ><el-table :data="historyRows"
      ><el-table-column prop="data.action" label="动作" width="190" /><el-table-column
        prop="data.objectVersion"
        label="对象版本"
        width="90"
      /><el-table-column prop="data.actor" label="操作者" width="90" /><el-table-column
        prop="data.reason"
        label="依据"
        min-width="220"
      /><el-table-column label="快照与影响集合" min-width="300"
        ><template #default="{ row }">
          <pre>{{ JSON.stringify(row.data, null, 2) }}</pre>
        </template></el-table-column
      ></el-table
    ><template #footer
      ><el-button @click="historyOpen = false">关闭对象历史</el-button></template
    ></Dialog
  >
</template>
<script setup lang="ts">
import { nextTick } from 'vue'
import ScopeFields from './ScopeFields.vue'
import { issueMissing } from './issueValidation'
import { failureGuidance } from '../review/validation'
import request from '@/config/axios'
import download from '@/utils/download'
import {
  listWorkbench,
  workbenchCommand,
  workbenchHistory,
  type WorkbenchRecord
} from '@/api/hrm/payroll/workbench'
import { getSimpleUserList } from '@/api/system/user'
import { payrollModules as modules } from '../requirements'
const props = defineProps<{
  projectId: number
  scopeVersion?: number
  scopeJson?: string
  requirements: any[]
}>()
const emit = defineEmits(['changed'])
const message = useMessage()
const sources = ref<WorkbenchRecord[]>([]),
  relations = ref<WorkbenchRecord[]>([]),
  issues = ref<WorkbenchRecord[]>([]),
  evidence = ref<WorkbenchRecord[]>([]),
  todos = ref<WorkbenchRecord[]>([]),
  proposals = ref<WorkbenchRecord[]>([])
const users = ref<any[]>([]),
  activeTab = ref('sources'),
  error = ref(false),
  opened = ref(false),
  failed = ref(false),
  saving = ref(false),
  mode = ref(''),
  title = ref(''),
  draft = ref<Record<string, any>>({})
const historyOpen = ref(false),
  historyRows = ref<WorkbenchRecord[]>([])
const issueDraft = computed(() => (mode.value === 'issue' ? draft.value.issue : draft.value))
const failureMessage = ref('')
const fieldErrors = ref<Record<string, string>>({})
const sourceLabels = {
  technicalContact: '技术联系人',
  acquisition: '获取方式',
  specification: '接口或文件规格版本',
  uniqueKey: '唯一键',
  employeeMatch: '员工匹配方式',
  frequency: '频率及周期粒度',
  cutoff: '截止时间',
  volume: '预计数据量',
  qualityRule: '来源检查规则版本/摘要'
}
const fieldLabels = { name: '字段名', meaning: '业务含义', type: '类型', unit: '单位' }
const mappingLabels = {
  pageField: '消费页面字段',
  sourceField: '来源字段',
  meaning: '业务含义',
  type: '类型',
  unit: '单位',
  transform: '转换或汇总规则',
  nullRule: '空值处理',
  duplicateRule: '重复处理',
  periodRule: '跨期处理',
  exceptionRule: '异常处理'
}
const supplyNames: Record<string, string> = {
  UNINVENTORIED: '未盘点',
  PENDING: '待提供',
  PROVIDED: '已提供',
  PROBLEM: '有问题'
}
const issueStates: Record<string, string> = {
  OPEN: '待处理',
  PROCESSING: '处理中',
  REVIEW: '待复核',
  CLOSED: '已关闭'
}
const newField = () => ({ name: '', meaning: '', type: '', unit: '', required: true })
const newMapping = () => ({
  pageField: '',
  sourceField: '',
  meaning: '',
  type: '',
  unit: '',
  required: true,
  transform: '',
  nullRule: '',
  duplicateRule: '',
  periodRule: '',
  exceptionRule: ''
})
const context = (o?: WorkbenchRecord) => ({
  projectId: props.projectId,
  projectScopeVersion: props.scopeVersion,
  id: o?.id,
  version: o?.version,
  reason: ''
})
const open = (m: string, t: string, d: Record<string, any>) => {
  fieldErrors.value = {}
  failureMessage.value = ''
  mode.value = m
  title.value = t
  draft.value = d
  failed.value = false
  opened.value = true
}
const editSource = (o?: WorkbenchRecord) =>
  open('source', o ? '实际来源规格' : '登记实际来源', {
    ...context(o),
    source: o
      ? JSON.parse(JSON.stringify(o.data))
      : {
          name: '',
          module: 'COL',
          scopeJson: props.scopeJson,
          ownerId: undefined,
          technicalContact: '',
          acquisition: '',
          specification: '',
          uniqueKey: '',
          employeeMatch: '',
          frequency: '',
          cutoff: '',
          volume: '',
          qualityRule: '',
          fields: [newField()]
        }
  })
const editRelation = (o?: WorkbenchRecord) =>
  open('relation', '消费字段映射', {
    ...context(o),
    relation: o
      ? JSON.parse(JSON.stringify(o.data))
      : {
          sourceId: undefined,
          requirementId: undefined,
          fields: [newMapping()],
          consumptionRule: ''
        }
  })
const supply = (o: WorkbenchRecord) =>
  open('supply', '供给事实与交付内容', {
    ...context(o),
    supply: o.data.supply,
    replaceDelivery: false,
    deliveryDigest: '',
    deliveryReference: ''
  })
const verify = (o: WorkbenchRecord, layer: string) =>
  open('verify', layer + '独立执行核验', {
    ...context(o),
    layer,
    result: 'PASS',
    reference: '',
    sampleDigest: '',
    expectedBinding: o.bindings?.[layer]
  })
const propose = (o: WorkbenchRecord) =>
  open('proposal', '提交来源变更建议', { ...context(o), suggestion: '' })
const copySource = (o: WorkbenchRecord) =>
  open('copy', '跨项目复制规格', {
    ...context(),
    sourceProjectId: props.projectId,
    sourceId: o.id,
    sourceVersion: o.version,
    projectId: undefined,
    projectScopeVersion: undefined
  })
const createIssue = () =>
  open('issue', '登记独立评审问题', {
    ...context(),
    issue: {
      description: '',
      requirementIds: [],
      sourceIds: [],
      relationIds: [],
      wholeProject: false,
      category: 'DATA_READINESS',
      handlerId: undefined,
      reviewerId: undefined,
      dueDate: ''
    }
  })
const issueAction = (o: WorkbenchRecord, action: string) =>
  open('action', '问题动作 ' + action, {
    ...context(o),
    action,
    evidence: '',
    expectedBinding: o.binding,
    category: o.data.category,
    wholeProject: o.data.wholeProject,
    requirementIds: [...o.data.requirementIds],
    sourceIds: [...o.data.sourceIds],
    handlerId: o.data.handlerId,
    reviewerId: o.data.reviewerId,
    dueDate: undefined
  })
const unlink = async (o: WorkbenchRecord) => {
  const { value } = await message.prompt('解除当前关联并保留历史，请填写原因')
  await workbenchCommand('/relation/unlink', { ...context(o), reason: value })
  await load()
  emit('changed')
}
const history = async (o: WorkbenchRecord) => {
  historyRows.value = await workbenchHistory(props.projectId, o.kind, o.id)
  historyOpen.value = true
}
const save = async () => {
  if (saving.value) return
  fieldErrors.value = issueMissing(mode.value, draft.value)
  if (Object.keys(fieldErrors.value).length) {
    failed.value = true
    failureMessage.value = `请补齐以下字段：${Object.values(fieldErrors.value).join('、')}。输入已保留。`
    await nextTick()
    const label = Object.values(fieldErrors.value)[0]
    const control = document.querySelector<HTMLElement>(`[aria-label="${label}"]`)
    control?.scrollIntoView({ block: 'center' })
    control?.focus()
    return
  }
  saving.value = true
  failed.value = false
  try {
    const paths: Record<string, string> = {
      source: '/source/save',
      relation: '/relation/save',
      supply: '/source/supply',
      verify: '/verify',
      issue: '/issue/create',
      action: '/issue/action',
      proposal: '/source/propose',
      copy: '/source/copy'
    }
    await workbenchCommand(paths[mode.value], draft.value)
    opened.value = false
    message.success('已保存，状态按当前证据重新计算')
    await load()
    emit('changed')
  } catch (error) {
    failed.value = true
    failureMessage.value = failureGuidance(error)
  } finally {
    saving.value = false
  }
}
const exportCurrent = async () => {
  const value = await request.get({
    url: '/hrm/payroll/workbench/export',
    params: { projectId: props.projectId }
  })
  download.json(
    new Blob([JSON.stringify(value, null, 2)], { type: 'application/json' }),
    '当前来源关系工作清单.json'
  )
}
const load = async () => {
  error.value = false
  try {
    const data = await Promise.all(
      ['SOURCE', 'RELATION', 'ISSUE', 'EVIDENCE', 'TODO', 'PROPOSAL'].map((k) =>
        listWorkbench(props.projectId, k)
      )
    )
    ;[sources.value, relations.value, issues.value, evidence.value, todos.value, proposals.value] =
      data
  } catch {
    error.value = true
  }
}
watch(
  () => props.scopeVersion,
  () => load()
)

onMounted(async () => {
  await Promise.all([load(), getSimpleUserList().then((d) => (users.value = d))])
})
</script>
<style scoped>
.mapping-row {
  border: 1px solid var(--el-border-color);
  padding: 16px;
  margin: 12px 0;
}
pre {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 12px;
  max-height: 280px;
  overflow: auto;
}
</style>
