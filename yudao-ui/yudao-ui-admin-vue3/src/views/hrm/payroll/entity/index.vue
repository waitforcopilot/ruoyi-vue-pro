<template>
  <ContentWrap>
    <RequirementContext module="CALC" />
    <el-button v-hasPermi="['hrm:payroll:entity:update']" @click="open('entity')"
      >新建法人主体</el-button
    >
    <el-button :loading="loading" @click="load">刷新</el-button>
    <el-table :data="entities" highlight-current-row @current-change="select">
      <el-table-column prop="code" label="主体编码" />
      <el-table-column prop="name" label="主体名称" />
      <el-table-column prop="taxRegistrationNumber" label="纳税识别号" />
      <el-table-column prop="status" label="审批状态" />
      <el-table-column prop="evidence" label="登记依据" show-overflow-tooltip />
      <el-table-column label="操作" width="180"
        ><template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING' && row.allowedActions.includes('MANAGE')"
            v-hasPermi="['hrm:payroll:entity:update']"
            link
            @click.stop="edit(row)"
            >修改草稿</el-button
          >
          <el-button
            v-if="row.status === 'PENDING' && row.allowedActions.includes('APPROVE')"
            v-hasPermi="['hrm:payroll:entity:approve']"
            link
            @click.stop="act('entity', row.id)"
            >审批</el-button
          >
        </template></el-table-column
      >
    </el-table>
  </ContentWrap>
  <ContentWrap v-if="selected">
    <p>{{ selected.name }}：人员归属按批准的生效期间适用，调动或更正须创建并审批替代版本。</p>
    <el-button
      v-if="manage && selected.status === 'APPROVED'"
      v-hasPermi="['hrm:payroll:entity:update']"
      @click="open('membership')"
      >新增人员归属</el-button
    >
    <el-button v-if="manage" v-hasPermi="['hrm:payroll:entity:grant']" @click="open('grant')"
      >新增授权</el-button
    >
    <el-tabs>
      <el-tab-pane label="人员归属及历史">
        <el-table :data="memberships"
          ><el-table-column prop="id" label="版本编号" width="95" /><el-table-column
            prop="employeeId"
            label="员工编号"
          /><el-table-column prop="deptId" label="部门编号" /><el-table-column
            prop="employeeCategory"
            label="员工类别"
          /><el-table-column prop="workCity" label="工作城市" /><el-table-column
            prop="effectiveFrom"
            label="起始日"
          /><el-table-column prop="effectiveTo" label="截止日" /><el-table-column
            prop="status"
            label="状态"
          /><el-table-column prop="replacesId" label="替代版本" /><el-table-column
            prop="evidence"
            label="依据"
            show-overflow-tooltip
          />
          <el-table-column label="操作" width="190"
            ><template #default="{ row }">
              <template v-if="row.status === 'PENDING' && canApprove"
                ><el-button
                  v-hasPermi="['hrm:payroll:entity:approve']"
                  link
                  @click="act('membership', row.id)"
                  >批准</el-button
                ><el-button
                  v-hasPermi="['hrm:payroll:entity:approve']"
                  link
                  @click="act('reject', row.id)"
                  >退回</el-button
                ></template
              >
              <el-button
                v-if="row.status === 'APPROVED' && manage"
                v-hasPermi="['hrm:payroll:entity:update']"
                link
                @click="replace(row)"
                >截止或更正</el-button
              >
            </template></el-table-column
          >
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="manage" label="有效授权">
        <el-alert
          title="多个有效授权合并生效；撤销某条授权后，其他有效授权仍保留。"
          type="info"
          :closable="false"
        />
        <el-table :data="grants"
          ><el-table-column prop="userId" label="用户编号" /><el-table-column
            prop="actions"
            label="授权动作"
          /><el-table-column prop="effectiveFrom" label="起始日" /><el-table-column
            prop="effectiveTo"
            label="截止日"
          /><el-table-column prop="reason" label="授权原因" /><el-table-column label="操作"
            ><template #default="{ row }"
              ><el-button
                v-hasPermi="['hrm:payroll:entity:grant']"
                link
                @click="act('revoke', row.id)"
                >撤销</el-button
              ></template
            ></el-table-column
          ></el-table
        >
      </el-tab-pane>
      <el-tab-pane v-if="selected.allowedActions.includes('READ')" label="审计履历"
        ><el-table :data="events"
          ><el-table-column prop="action" label="动作" /><el-table-column
            prop="actorId"
            label="操作人"
          /><el-table-column prop="reason" label="依据" /><el-table-column label="快照"
            ><template #default="{ row }"
              ><el-button link @click="showProof(row.snapshot)">查看</el-button></template
            ></el-table-column
          ></el-table
        ></el-tab-pane
      >
    </el-tabs>
  </ContentWrap>
  <el-dialog v-model="visible" :title="titles[mode]" width="650px">
    <el-form ref="formRef" :model="form" label-width="110px">
      <template v-if="mode === 'entity'">
        <el-form-item label="主体编码" prop="code" :rules="required"
          ><el-input v-model="form.code" :disabled="!!editingId" maxlength="64"
        /></el-form-item>
        <el-form-item label="主体名称" prop="name" :rules="required"
          ><el-input v-model="form.name" maxlength="128"
        /></el-form-item>
        <el-form-item label="纳税识别号" prop="taxRegistrationNumber" :rules="required"
          ><el-input v-model="form.taxRegistrationNumber" maxlength="20"
        /></el-form-item>
      </template>
      <template v-if="mode === 'grant'">
        <el-form-item label="授权用户" prop="userId" :rules="required"
          ><el-select v-model="form.userId" filterable
            ><el-option
              v-for="u in choices.users"
              :key="u.id"
              :label="`${u.id} ${u.name}`"
              :value="u.id" /></el-select
        ></el-form-item>
        <el-form-item label="授权动作" prop="actions" :rules="required"
          ><el-checkbox-group v-model="form.actions"
            ><el-checkbox v-for="a in actions" :key="a[0]" :label="a[0]">{{
              a[1]
            }}</el-checkbox></el-checkbox-group
          ></el-form-item
        >
      </template>
      <template v-if="mode === 'membership'">
        <el-alert
          v-if="form.replacesId"
          :title="`替代版本 ${form.replacesId}；起始日期沿用原记录，截止日期可提前。新版本批准后原版本保留。`"
          :closable="false"
        />
        <el-form-item label="员工" prop="employeeId" :rules="required"
          ><el-select v-model="form.employeeId" filterable :disabled="!!form.replacesId"
            ><el-option
              v-for="e in choices.employees"
              :key="e.id"
              :label="`${e.jobNumber} ${e.name}`"
              :value="e.id" /></el-select
        ></el-form-item>
        <el-form-item label="归属部门" prop="deptId" :rules="required"
          ><el-select v-model="form.deptId" filterable
            ><el-option
              v-for="d in departments"
              :key="d.id"
              :label="d.name"
              :value="d.id" /></el-select
        ></el-form-item>
        <el-form-item label="员工类别" prop="employeeCategory" :rules="required"
          ><el-input v-model="form.employeeCategory" maxlength="64"
        /></el-form-item>
        <el-form-item label="工作城市" prop="workCity" :rules="required"
          ><el-input v-model="form.workCity" maxlength="128"
        /></el-form-item>
      </template>
      <template v-if="mode !== 'entity'">
        <el-form-item label="起始日期" prop="effectiveFrom" :rules="required"
          ><el-date-picker
            v-model="form.effectiveFrom"
            value-format="YYYY-MM-DD"
            :disabled="!!form.replacesId"
        /></el-form-item>
        <el-form-item label="截止日期" prop="effectiveTo" :rules="required"
          ><el-date-picker v-model="form.effectiveTo" value-format="YYYY-MM-DD"
        /></el-form-item>
      </template>
      <el-form-item
        :label="mode === 'grant' ? '授权原因' : '证明依据'"
        :prop="mode === 'grant' ? 'reason' : 'evidence'"
        :rules="required"
        ><el-input
          v-if="mode === 'grant'"
          v-model="form.reason"
          type="textarea"
          maxlength="1000" /><el-input
          v-else
          v-model="form.evidence"
          type="textarea"
          maxlength="1000"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save">提交</el-button></template
    >
  </el-dialog>
  <el-dialog v-model="proofVisible" title="审计快照">
    <pre style="white-space: pre-wrap">{{ proof }}</pre>
  </el-dialog>
</template>
<script setup lang="ts">
import RequirementContext from '../components/RequirementContext.vue'
import * as api from '@/api/hrm/payroll/entity'
import * as DeptApi from '@/api/system/dept'
import { ElMessageBox } from 'element-plus'
defineOptions({ name: 'HrmPayrollEntity' })
const message = useMessage()
const entities = ref<api.Entity[]>([]),
  selected = ref<api.Entity>(),
  memberships = ref<any[]>([]),
  grants = ref<any[]>([]),
  events = ref<any[]>([])
const loading = ref(false),
  saving = ref(false),
  visible = ref(false),
  proofVisible = ref(false),
  proof = ref('')
const mode = ref('entity'),
  editingId = ref<number>(),
  form = ref<any>({}),
  formRef = ref()
const choices = ref<{ employees: any[]; users: any[] }>({ employees: [], users: [] }),
  departments = ref<DeptApi.DeptVO[]>([])
const required = [{ required: true, message: '请填写必填项', trigger: 'blur' }]
const titles: Record<string, string> = {
  entity: '法人主体登记',
  membership: '人员归属申请',
  grant: '主体授权'
}
const actions = [
  ['READ', '查询'],
  ['CALCULATE', '核算'],
  ['REVIEW', '复核'],
  ['APPROVE', '审批'],
  ['EXPORT', '导出'],
  ['MANAGE', '维护']
]
const manage = computed(() => !!selected.value?.allowedActions.includes('MANAGE'))
const canApprove = computed(() => !!selected.value?.allowedActions.includes('APPROVE'))
let selectionVersion = 0
const select = async (row?: api.Entity) => {
  const version = ++selectionVersion
  selected.value = row
  memberships.value = []
  grants.value = []
  events.value = []
  if (!row) return
  const [m, g, e] = await Promise.all([
    row.allowedActions.includes('READ') ? api.memberships(row.id) : [],
    row.allowedActions.includes('MANAGE') ? api.grants(row.id) : [],
    row.allowedActions.includes('READ') ? api.history(row.id) : []
  ])
  if (version !== selectionVersion) return
  memberships.value = m
  grants.value = g
  events.value = e
}
const load = async () => {
  loading.value = true
  try {
    const id = selected.value?.id
    entities.value = await api.list()
    await select(entities.value.find((e) => e.id === id))
  } finally {
    loading.value = false
  }
}
const open = async (kind: string) => {
  mode.value = kind
  editingId.value = undefined
  form.value = { entityId: selected.value?.id, actions: [] }
  if (kind !== 'entity') {
    if (!selected.value || !manage.value) return
    choices.value = await api.options(selected.value.id)
    if (kind === 'membership') departments.value = await DeptApi.getSimpleDeptList()
  }
  visible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}
const edit = (row: api.Entity) => {
  mode.value = 'entity'
  editingId.value = row.id
  form.value = { ...row }
  visible.value = true
  nextTick(() => formRef.value?.clearValidate())
}
const replace = async (row: any) => {
  await open('membership')
  form.value = { ...row, replacesId: row.id, evidence: '' }
}
const save = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (mode.value === 'entity') {
      if (editingId.value) await api.revise(editingId.value, form.value)
      else await api.create(form.value)
    } else if (mode.value === 'grant') await api.grant(form.value)
    else await api.assign(form.value)
    message.success('已提交')
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
const showProof = (snapshot: string) => {
  proof.value = snapshot
  proofVisible.value = true
}
const act = async (kind: string, id: number) => {
  const { value: reason } = await ElMessageBox.prompt('请输入审批或变更依据', '确认操作', {
    inputValidator: (v: string) => !!v?.trim() && v.length <= 1000,
    inputErrorMessage: '依据须为 1 至 1000 字'
  })
  const params = { id, reason }
  if (kind === 'entity') await api.approve(params)
  else if (kind === 'membership') await api.approveMembership(params)
  else if (kind === 'reject') await api.rejectMembership(params)
  else await api.revoke(params)
  message.success('操作成功')
  await load()
}
onMounted(load)
</script>
