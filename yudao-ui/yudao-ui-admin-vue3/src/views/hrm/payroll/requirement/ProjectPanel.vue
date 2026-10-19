<template>
  <ContentWrap>
    <el-space wrap>
      <el-select
        v-model="selected"
        aria-label="评审项目"
        style="width: 280px"
        @change="selectProject"
      >
        <el-option :value="0" label="历史需求迁移项目" />
        <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="p.name" />
      </el-select>
      <el-button v-hasPermi="['hrm:payroll:requirement:update']" @click="openCreate"
        >新建评审项目</el-button
      >
      <template v-if="current">
        <el-button v-if="caps.META" @click="openName">修改项目名称</el-button>
        <el-button v-if="caps.SCOPE && caps.FULL_READ" @click="openScope">修订项目范围</el-button>
        <el-button @click="compare">模板版本比较</el-button>
        <el-button v-if="caps.FULL_READ" @click="showEvents">项目变更历史</el-button>
        <el-button v-if="caps.MEMBER" @click="membersVisible = true">项目成员与授权</el-button>
      </template>
    </el-space>
    <MembersPanel
      v-if="current"
      v-model="membersVisible"
      :project-id="current.id"
      :scope-json="current.scopeJson"
    />
    <el-alert
      v-if="error"
      title="项目列表读取失败，请重试；不会自动转入其他项目"
      type="error"
      :closable="false"
    />
    <el-button v-if="error" @click="load">重试项目列表</el-button>
    <Dialog
      v-model="dialog"
      :title="
        kind === 'create' ? '新建评审项目' : kind === 'name' ? '修改项目名称' : '修订项目范围'
      "
      width="650px"
      scroll
      max-height="65vh"
    >
      <el-form label-width="120px" @submit.prevent>
        <template v-if="kind !== 'scope'">
          <el-form-item label="项目代码"
            ><el-input
              v-model="form.projectKey"
              :disabled="kind === 'name'"
              placeholder="例如 TEST_ALPHA"
          /></el-form-item>
          <el-form-item label="项目名称"
            ><el-input v-model="form.name" maxlength="200"
          /></el-form-item>
          <el-alert
            v-if="kind === 'create'"
            title="初始业务边界须在独立创建授权内；创建不代表业务范围获批。草稿访问可撤销，范围修订仍需独立授权。"
            :closable="false"
          />
          <ScopeFields v-if="kind === 'create'" v-model="creationScopeJson" />
        </template>
        <template v-else>
          <p
            >预期范围版本：{{
              current?.projectScopeVersion ?? '未定义'
            }}；提交时校验授权上限和当前版本。</p
          >
          <el-form-item label="主体ID集合"
            ><el-input v-model="scope.entities" placeholder="逗号分隔"
          /></el-form-item>
          <el-form-item label="组织ID集合"
            ><el-input v-model="scope.organizations" placeholder="逗号分隔"
          /></el-form-item>
          <el-form-item label="人员类型"
            ><el-input v-model="scope.workerTypes" placeholder="逗号分隔"
          /></el-form-item>
          <el-form-item label="业务场景"
            ><el-input v-model="scope.scenarios" placeholder="逗号分隔"
          /></el-form-item>
          <el-form-item label="期间开始"
            ><el-input v-model="scope.from" placeholder="YYYY-MM-DD"
          /></el-form-item>
          <el-form-item label="期间结束"
            ><el-input v-model="scope.to" placeholder="YYYY-MM-DD"
          /></el-form-item>
          <el-form-item label="范围业务说明"
            ><el-input v-model="scope.explanation" type="textarea"
          /></el-form-item>
          <el-form-item label="修订理由"
            ><el-input v-model="reason" type="textarea"
          /></el-form-item>
          <el-alert
            title="影响当前项目全部未移除需求；已确认项转为评审中，显式子集失配须修正后重审。旧历史保留。"
            type="warning"
            :closable="false"
          />
        </template>
      </el-form>
      <template #footer
        ><el-button :disabled="busy" @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="busy" @click="submit">保存项目</el-button></template
      >
    </Dialog>
    <Dialog v-model="diffVisible" title="模板升级比较" width="850px" scroll max-height="65vh">
      <el-alert
        title="初始化只补缺，不覆盖人工内容。采纳前请比较原文；保留原文也记录决定。模板删除仅提示，不删除项目记录。"
        :closable="false"
      />
      <el-empty v-if="!differences.length" description="当前模板无待比较项" />
      <div v-for="d in differences" :key="d.template.templateItemId" class="my-4">
        <b
          >{{ d.template.originalCode }} · 模板版本 {{ d.template.templateVersion
          }}{{ d.template.withdrawn ? ' · 模板已撤回，仅提示' : '' }}</b
        >
        <p>当前：{{ d.requirement?.description || '项目尚无此模板项，请初始化补缺' }}</p>
        <p>建议：{{ d.template.description }}</p>
        <template v-if="d.requirement && !d.template.withdrawn">
          <el-button :disabled="busy" @click="decide(d, true)">采纳模板更新</el-button>
          <el-button :disabled="busy" @click="decide(d, false)">保留当前内容</el-button>
        </template>
      </div>
      <template #footer
        ><el-button :disabled="busy" @click="diffVisible = false">取消比较</el-button></template
      >
    </Dialog>
    <Dialog v-model="eventsVisible" title="项目变更历史" width="850px" scroll max-height="65vh">
      <div v-for="event in events" :key="event.id" class="my-4"
        ><b>{{ event.action }} · 操作人 {{ event.actorId }}</b
        ><pre style="white-space: pre-wrap">{{ event.snapshot }}</pre>
      </div>
      <template #footer><el-button @click="eventsVisible = false">关闭历史</el-button></template>
    </Dialog>
  </ContentWrap>
</template>
<script setup lang="ts">
import MembersPanel from './MembersPanel.vue'
import ScopeFields from './ScopeFields.vue'
import * as api from '@/api/hrm/payroll/requirement'
const props = defineProps<{ projectId?: number }>()
const emit = defineEmits<{ changed: [] }>()
const router = useRouter(),
  route = useRoute(),
  message = useMessage()
const selected = ref(props.projectId || 0)
const projects = ref<api.PayrollRequirementProject[]>([])
const current = computed(() => projects.value.find((p) => p.id === props.projectId))
const caps = ref<Record<string, boolean>>({})
const membersVisible = ref(false)
const error = ref(false),
  busy = ref(false),
  dialog = ref(false),
  diffVisible = ref(false),
  eventsVisible = ref(false)
const kind = ref<'create' | 'name' | 'scope'>('create')
const form = ref({ projectKey: '', name: '' })
const creationScopeJson = ref<string>()
const scope = ref({
  entities: '',
  organizations: '',
  workerTypes: '',
  scenarios: '',
  from: '',
  to: '',
  explanation: ''
})
const reason = ref(''),
  differences = ref<any[]>([]),
  events = ref<any[]>([])
const load = async () => {
  error.value = false
  try {
    projects.value = await api.listRequirementProjects()
    caps.value = props.projectId ? await api.requirementCapabilities(props.projectId) : {}
  } catch {
    error.value = true
  }
}
const selectProject = () =>
  router.replace({ query: { ...route.query, projectId: selected.value || undefined } })
const openCreate = () => {
  creationScopeJson.value = undefined
  kind.value = 'create'
  form.value = { projectKey: '', name: '' }
  dialog.value = true
}
const openName = () => {
  if (!current.value) return
  kind.value = 'name'
  form.value = { projectKey: current.value.projectKey!, name: current.value.name }
  dialog.value = true
}
const openScope = () => {
  kind.value = 'scope'
  reason.value = ''
  const s = current.value?.scopeJson ? JSON.parse(current.value.scopeJson) : {}
  scope.value = {
    entities: (s.entities || []).join(','),
    organizations: (s.organizations || []).join(','),
    workerTypes: (s.workerTypes || []).join(','),
    scenarios: (s.scenarios || []).join(','),
    from: Array.isArray(s.from)
      ? s.from.map((v, i) => (i ? String(v).padStart(2, '0') : v)).join('-')
      : s.from || '',
    to: Array.isArray(s.to)
      ? s.to.map((v, i) => (i ? String(v).padStart(2, '0') : v)).join('-')
      : s.to || '',
    explanation: s.explanation || ''
  }
  dialog.value = true
}
const submit = async () => {
  if (busy.value) return
  busy.value = true
  try {
    if (kind.value === 'create') {
      const id = await api.createRequirementProject({
        ...form.value,
        creationScopeJson: creationScopeJson.value
      })
      dialog.value = false
      await router.replace({ query: { ...route.query, projectId: id } })
      return
    }
    if (!current.value) return
    if (kind.value === 'name')
      await api.renameRequirementProject({
        ...form.value,
        id: current.value.id,
        metadataVersion: current.value.metadataVersion
      })
    else {
      const split = (s: string) =>
        s
          .split(/[,，]/)
          .map((x) => x.trim())
          .filter(Boolean)
      const s = {
        ...scope.value,
        entities: split(scope.value.entities).map(Number),
        organizations: split(scope.value.organizations).map(Number),
        workerTypes: split(scope.value.workerTypes),
        scenarios: split(scope.value.scenarios)
      }
      await api.saveRequirementProjectScope({
        projectId: current.value.id,
        expectedVersion: current.value.projectScopeVersion,
        scopeJson: JSON.stringify(s),
        reason: reason.value
      })
    }
    dialog.value = false
    await load()
    emit('changed')
    message.success('项目已保存')
  } finally {
    busy.value = false
  }
}
const compare = async () => {
  if (!props.projectId) return
  differences.value = await api.requirementTemplateDifferences(props.projectId)
  diffVisible.value = true
}
const decide = async (d: any, accept: boolean) => {
  if (!props.projectId || busy.value) return
  busy.value = true
  try {
    await api.decideRequirementTemplate({
      projectId: props.projectId,
      requirementId: d.requirement.id,
      version: d.requirement.version,
      templateVersion: d.template.templateVersion,
      accept
    })
    await compare()
    emit('changed')
  } finally {
    busy.value = false
  }
}
const showEvents = async () => {
  if (!props.projectId) return
  events.value = await api.requirementProjectEvents(props.projectId)
  eventsVisible.value = true
}
onMounted(load)
</script>
