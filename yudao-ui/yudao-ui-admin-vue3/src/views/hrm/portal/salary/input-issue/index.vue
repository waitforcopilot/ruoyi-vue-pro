<template>
  <ContentWrap>
    <el-alert
      title="请先补正对应业务资料，再提交资料编号或说明。指定复核人核实真实来源后关闭工单。"
      type="info"
      :closable="false"
    />
    <el-button class="my-4" :loading="loading" @click="load">刷新</el-button>
    <el-table :data="rows" v-loading="loading"
      ><el-table-column prop="id" label="工单编号" /><el-table-column
        prop="batchId"
        label="工资批次编号"
      /><el-table-column prop="employeeId" label="员工编号" /><el-table-column label="项目"
        ><template #default="{ row }">{{ labels[row.kind] }}</template></el-table-column
      ><el-table-column label="状态"
        ><template #default="{ row }"
          >{{ states[row.status] }}<el-tag v-if="row.overdue" type="danger">逾期</el-tag></template
        ></el-table-column
      ><el-table-column prop="dueTime" label="截止时间" min-width="165" /><el-table-column
        prop="reason"
        label="处理要求"
        show-overflow-tooltip
      /><el-table-column
        prop="reviewOpinion"
        label="复核意见"
        show-overflow-tooltip
      /><el-table-column label="操作" width="180"
        ><template #default="{ row }"
          ><el-button v-if="row.canSubmit" link @click="submit(row.id)">补充材料</el-button
          ><el-button link @click="history(row.id)">履历</el-button></template
        ></el-table-column
      ></el-table
    >
  </ContentWrap>
  <Dialog v-model="historyVisible" title="工单处理履历"
    ><el-table :data="events"
      ><el-table-column label="动作"
        ><template #default="{ row }">{{
          actions[row.action] || row.action
        }}</template></el-table-column
      ><el-table-column prop="actorId" label="操作人编号" /><el-table-column
        prop="reason"
        label="依据" /><el-table-column prop="createTime" label="时间" min-width="165" /></el-table
  ></Dialog>
</template>
<script setup lang="ts">
import request from '@/config/axios'
import { labels, states, actions } from '@/views/hrm/payroll/input-issue/labels'
defineOptions({ name: 'HrmPortalPayrollInputIssue' })
const base = '/hrm/portal/payroll-input-issue',
  message = useMessage(),
  loading = ref(false),
  historyVisible = ref(false),
  rows = ref<any[]>([]),
  events = ref<any[]>([])
async function load() {
  loading.value = true
  try {
    rows.value = await request.get({ url: base + '/list' })
  } finally {
    loading.value = false
  }
}
async function submit(id: number) {
  const { value } = await message.prompt(
    '请提供补正资料编号或说明，不要填写工资、证件或银行卡明文',
    '补充材料',
    {
      inputType: 'textarea',
      inputValidator: (v: string) => (!!v?.trim() && v.length <= 1000) || '请填写1～1000字的依据'
    }
  )
  await request.post({ url: base + '/submit', data: { id, evidence: value } })
  message.success('材料已提交，等待指定复核人核实')
  await load()
}
async function history(id: number) {
  events.value = await request.get({ url: base + '/history', params: { id } })
  historyVisible.value = true
}
onMounted(load)
</script>
