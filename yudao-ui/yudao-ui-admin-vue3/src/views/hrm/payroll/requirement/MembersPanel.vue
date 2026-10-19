<template>
  <Dialog
    v-model="opened"
    title="项目成员与授权"
    width="min(1000px, calc(100vw - 24px))"
    scroll
    max-height="65vh"
  >
    <el-alert
      title="授权受当前委派上限约束；创建人和技术管理员不自动拥有成员管理或业务确认权。撤销上游授权同时使其委派失效，历史签署保留。"
      :closable="false"
    />
    <el-table :data="members" class="my-4">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="p-4">
            <p
              >授权版本：{{ row.grant.grantVersion }}；上游授权：{{
                row.grant.parentGrantId || '独立登记'
              }}；授予人：{{ row.grant.grantorId || '登记记录' }}</p
            >
            <p>授权有效期：{{ row.grant.validFrom }} 至 {{ row.grant.validTo }}（上海日期）</p>
            <p>可继续委派：{{ row.grant.delegableActions || '无' }}</p>
            <p>授权依据：{{ row.grant.evidence }}</p>
            <pre style="white-space: pre-wrap">
业务边界：{{ row.grant.ceilingJson || '历史未定义；不代表业务批准' }}</pre
            >
            <p v-if="row.grant.revokedAt"
              >撤销时间：{{ formatDate(row.grant.revokedAt) }}；理由：{{
                row.grant.revokeReason
              }}</p
            >
          </div>
        </template>
      </el-table-column>
      <el-table-column label="授权编号" width="90"
        ><template #default="{ row }">{{ row.grant.id }}</template></el-table-column
      >
      <el-table-column label="成员" width="140"
        ><template #default="{ row }">{{
          users.find((u) => u.id === row.grant.userId)?.nickname || row.grant.userId
        }}</template></el-table-column
      >
      <el-table-column label="动作" min-width="180"
        ><template #default="{ row }">{{ row.grant.actions }}</template></el-table-column
      >
      <el-table-column label="当前状态" width="100"
        ><template #default="{ row }">{{
          row.effective ? '有效' : '已失效'
        }}</template></el-table-column
      >
      <el-table-column label="范围" min-width="150"
        ><template #default="{ row }">{{
          row.grant.fullProject
            ? '全项目模块/对象'
            : `${row.grant.modulesJson || '全部模块'} / ${row.grant.requirementIdsJson || '全部需求'}`
        }}</template></el-table-column
      >
      <el-table-column label="操作" width="90"
        ><template #default="{ row }"
          ><el-button
            v-if="
              row.effective &&
              (row.grant.parentGrantId || row.grant.evidence?.startsWith('PROJECT_CREATOR_ACCESS:'))
            "
            link
            @click="revoking = row.grant"
            >撤销</el-button
          ></template
        ></el-table-column
      >
    </el-table>
    <template v-if="revoking">
      <p>撤销授权 {{ revoking.id }}，预期版本 {{ revoking.grantVersion }}</p>
      <el-form label-width="110px"
        ><el-form-item label="撤销理由"
          ><el-input v-model="revokeReason" type="textarea" /></el-form-item
      ></el-form>
      <el-button :disabled="busy" @click="revoking = undefined">取消撤销</el-button
      ><el-button type="danger" :loading="busy" @click="revoke">确认撤销授权</el-button>
    </template>
    <el-divider>新增成员授权</el-divider>
    <el-form label-width="120px">
      <el-form-item label="被授权成员"
        ><el-select v-model="form.userId" filterable
          ><el-option v-for="u in users" :key="u.id" :value="u.id" :label="u.nickname" /></el-select
      ></el-form-item>
      <el-form-item label="许可动作"
        ><el-select v-model="form.actions" multiple
          ><el-option
            v-for="(label, key) in labels"
            :key="key"
            :value="key"
            :label="label" /></el-select
      ></el-form-item>
      <el-form-item label="允许继续委派"
        ><el-select v-model="form.delegable" multiple
          ><el-option
            v-for="(label, key) in labels"
            :key="key"
            :value="key"
            :label="label" /></el-select
      ></el-form-item>
      <el-form-item label="全模块及对象"><el-switch v-model="form.fullProject" /></el-form-item>
      <template v-if="!form.fullProject">
        <el-form-item label="模块集合"
          ><el-input v-model="form.modules" placeholder="例如 COL,TAX；留空代表全部模块"
        /></el-form-item>
        <el-form-item label="稳定需求ID集合"
          ><el-input v-model="form.ids" placeholder="逗号分隔；留空代表范围内全部需求"
        /></el-form-item>
      </template>
      <ScopeFields v-model="form.ceilingJson" />
      <el-form-item label="授权开始日期"
        ><el-input v-model="form.validFrom" placeholder="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="授权结束日期"
        ><el-input v-model="form.validTo" placeholder="YYYY-MM-DD"
      /></el-form-item>
      <el-form-item label="授权依据"
        ><el-input
          v-model="form.evidence"
          type="textarea"
          placeholder="填写实际有效的授权依据；合成测试必须标明TEST"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button :disabled="busy" @click="opened = false">关闭成员管理</el-button
      ><el-button type="primary" :loading="busy" @click="grant">保存成员授权</el-button></template
    >
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/requirement'
import { formatDate } from '@/utils/formatTime'
import { getSimpleUserList } from '@/api/system/user'
import ScopeFields from './ScopeFields.vue'
const props = defineProps<{ projectId: number; scopeJson?: string }>()
const opened = defineModel<boolean>({ default: false })
const members = ref<any[]>([]),
  users = ref<any[]>([]),
  busy = ref(false)
const revoking = ref<any>(),
  revokeReason = ref('')
const labels = {
  READ: '读取',
  META: '修改项目名称',
  EDIT: '编辑需求',
  ASSIGN: '分派评审人',
  CONFIRM: '本人确认',
  EXPORT: '导出',
  MEMBER: '管理成员',
  SCOPE: '修订项目范围'
}
const form = ref({
  userId: undefined as number | undefined,
  actions: ['READ'],
  delegable: [] as string[],
  fullProject: false,
  modules: '',
  ids: '',
  ceilingJson: props.scopeJson,
  validFrom: '',
  validTo: '',
  evidence: ''
})
const load = async () => {
  members.value = await api.requirementMembers(props.projectId)
}
watch(opened, async (value) => {
  if (value) {
    revoking.value = undefined
    revokeReason.value = ''
    form.value.ceilingJson = props.scopeJson
    await Promise.all([load(), getSimpleUserList().then((data) => (users.value = data))])
  }
})
const split = (s: string) =>
  s
    .split(/[,，]/)
    .map((x) => x.trim())
    .filter(Boolean)
const grant = async () => {
  if (busy.value) return
  busy.value = true
  try {
    await api.grantRequirementMember({
      projectId: props.projectId,
      userId: form.value.userId,
      actions: form.value.actions.join(','),
      delegableActions: form.value.delegable.join(','),
      fullProject: form.value.fullProject,
      modulesJson: form.value.fullProject ? null : JSON.stringify(split(form.value.modules)),
      requirementIdsJson: form.value.fullProject
        ? null
        : JSON.stringify(split(form.value.ids).map(Number)),
      ceilingJson: form.value.ceilingJson,
      validFrom: form.value.validFrom,
      validTo: form.value.validTo,
      evidence: form.value.evidence
    })
    await load()
    useMessage().success('成员授权已保存')
  } finally {
    busy.value = false
  }
}
const revoke = async () => {
  if (!revoking.value || busy.value) return
  busy.value = true
  try {
    await api.revokeRequirementMember({
      projectId: props.projectId,
      grantId: revoking.value.id,
      version: revoking.value.grantVersion,
      reason: revokeReason.value
    })
    revoking.value = undefined
    revokeReason.value = ''
    await load()
    useMessage().success('授权已撤销，后续请求重新校验')
  } finally {
    busy.value = false
  }
}
</script>
