<template>
  <ContentWrap>
    <el-form inline>
      <el-form-item label="法人主体"
        ><el-select v-model="entityId" placeholder="请选择主体" style="width: 240px">
          <el-option v-for="e in entities" :key="e.id" :value="e.id" :label="e.name" /> </el-select
      ></el-form-item>
      <el-form-item label="员工"
        ><el-select v-model="employeeId" filterable placeholder="请选择员工" style="width: 240px"
          ><el-option
            v-for="e in employees"
            :key="e.id"
            :label="`${e.jobNumber} ${e.name}`"
            :value="e.id" /></el-select
      ></el-form-item>
      <el-form-item label="统计区间"
        ><el-date-picker v-model="period" type="daterange" value-format="YYYY-MM-DD"
      /></el-form-item>
      <el-button :loading="loading" @click="load">查询</el-button>
      <el-button v-hasPermi="['hrm:payroll:work:update']" @click="open('rule')"
        >新建工时制度</el-button
      >
      <el-button v-hasPermi="['hrm:payroll:work:confirm']" @click="open('entry')"
        >核实加班</el-button
      >
      <el-button v-hasPermi="['hrm:payroll:work:confirm']" @click="open('zero')"
        >登记加班未发生差异</el-button
      >
      <el-button v-hasPermi="['hrm:payroll:work:confirm']" @click="open('reserve')"
        >占用调休</el-button
      >
    </el-form>
    <el-alert
      v-if="stats.limitExceeded"
      type="warning"
      title="员工月度认可加班超过制度上限，请复核排班和审批。"
      :closable="false"
    />
    <p v-if="stats.recognizedMinutes != null"
      >认可加班：{{ stats.recognizedMinutes || 0 }} 分钟；加班费：{{
        stats.amount || 0
      }}
      元；有效调休余额：{{ stats.timeOffBalance || 0 }} 分钟</p
    >
    <el-tabs>
      <el-tab-pane label="工时制度">
        <el-table :data="rules"
          ><el-table-column prop="systemType" label="制度" /><el-table-column
            prop="effectiveFrom"
            label="生效日"
          /><el-table-column prop="effectiveTo" label="原结束日" />
          <el-table-column prop="id" label="版本编号" />
          <el-table-column prop="priorRuleId" label="原批准版本" />
          <el-table-column prop="supersededFrom" label="后续版本生效日" /><el-table-column
            prop="status"
            label="审批状态"
          /><el-table-column
            prop="rejectionReason"
            label="驳回依据"
            show-overflow-tooltip
          /><el-table-column prop="cycleMonths" label="周期（月）" /><el-table-column
            prop="cycleMinutes"
            label="周期标准分钟"
          /><el-table-column prop="hourlyBase" label="时薪基数" /><el-table-column
            prop="legalBasis"
            label="适用依据"
            show-overflow-tooltip
          /><el-table-column label="操作" width="180"
            ><template #default="{ row }"
              ><el-button
                v-if="row.status === 'APPROVED' && !row.supersededFrom"
                v-hasPermi="['hrm:payroll:work:update']"
                link
                @click="openRuleVersion(row)"
                >申请后续版本</el-button
              ><el-button
                v-if="row.status === 'PENDING'"
                v-hasPermi="['hrm:payroll:work:approve']"
                link
                @click="approve(row.id)"
                >审批通过</el-button
              ><el-button
                v-if="row.status === 'PENDING'"
                v-hasPermi="['hrm:payroll:work:approve']"
                link
                @click="rejectRule(row.id)"
                >驳回</el-button
              ><el-button
                v-if="row.status === 'APPROVED' && row.systemType === 'COMPREHENSIVE'"
                v-hasPermi="['hrm:payroll:work:confirm']"
                link
                @click="openCycle(row.id)"
                >周期结算</el-button
              ></template
            ></el-table-column
          ></el-table
        >
      </el-tab-pane>
      <el-tab-pane label="加班认可记录">
        <el-table :data="entries"
          ><el-table-column prop="sourceApplyId" label="OA 审批编号" /><el-table-column
            prop="startTime"
            label="开始"
            :formatter="formatTime"
            width="165"
          /><el-table-column
            prop="endTime"
            label="结束"
            :formatter="formatTime"
            width="165"
          /><el-table-column prop="dayType" label="日类型" /><el-table-column
            prop="recognizedMinutes"
            label="认可分钟"
          /><el-table-column prop="compensation" label="补偿方式" /><el-table-column
            prop="amount"
            label="日计薪金额"
          /><el-table-column label="证据"
            ><template #default="{ row }"
              ><el-button link @click="showProof(row.sourceSnapshot)"
                >审批及打卡快照</el-button
              ></template
            ></el-table-column
          ></el-table
        >
      </el-tab-pane>
      <el-tab-pane label="加班未发生差异">
        <el-alert
          title="OA批准不代表实际发生。申请需提供事实依据，另一名HR复核后才生成零发生记录；已有有效加班不能确认零值。来源变化会阻断核算。"
          type="info"
          :closable="false"
        />
        <el-table :data="zeroCases">
          <el-table-column prop="sourceApplyId" label="原OA申请" />
          <el-table-column prop="workDate" label="日期" />
          <el-table-column prop="status" label="复核状态" />
          <el-table-column label="来源就绪" width="150"
            ><template #default="{ row }">{{
              row.sourceFresh === false
                ? '来源变更，需重新核实'
                : row.sourceFresh === true
                  ? '与提交一致'
                  : '不适用'
            }}</template></el-table-column
          >
          <el-table-column prop="reason" label="事实说明" show-overflow-tooltip />
          <el-table-column prop="evidence" label="核实依据" show-overflow-tooltip />
          <el-table-column prop="reviewOpinion" label="复核意见" show-overflow-tooltip />
          <el-table-column label="操作" width="210"
            ><template #default="{ row }">
              <el-button link @click="showProof(row.sourceSnapshot)">来源快照</el-button>
              <el-button
                v-if="row.status === 'PENDING'"
                v-hasPermi="['hrm:payroll:work:approve']"
                link
                :disabled="row.sourceFresh === false"
                @click="reviewZero(row.id, true)"
                >复核通过</el-button
              >
              <el-button
                v-if="row.status === 'PENDING'"
                v-hasPermi="['hrm:payroll:work:approve']"
                link
                @click="reviewZero(row.id, false)"
                >驳回</el-button
              >
            </template></el-table-column
          >
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="调休台账">
        <el-table :data="ledger"
          ><el-table-column prop="id" label="编号" /><el-table-column
            prop="status"
            label="状态"
          /><el-table-column prop="minutes" label="分钟变动" /><el-table-column
            prop="expiresOn"
            label="原额度到期日"
          /><el-table-column prop="requestKey" label="申请凭证" /><el-table-column
            prop="reason"
            label="原因"
          /><el-table-column label="操作" width="160"
            ><template #default="{ row }"
              ><span v-if="row.requestKey?.startsWith('BPM-LEAVE-')">由请假审批处理</span
              ><el-button
                v-if="row.status === 'RESERVED' && !row.requestKey?.startsWith('BPM-LEAVE-')"
                v-hasPermi="['hrm:payroll:work:confirm']"
                link
                @click="consume(row.id)"
                >核销</el-button
              ><el-button
                v-if="
                  ['RESERVED', 'CONSUMED'].includes(row.status) &&
                  !row.requestKey?.startsWith('BPM-LEAVE-')
                "
                v-hasPermi="['hrm:payroll:work:confirm']"
                link
                @click="refund(row.id)"
                >退回</el-button
              ></template
            ></el-table-column
          ></el-table
        >
      </el-tab-pane>
      <el-tab-pane label="综合工时周期对账">
        <el-table :data="stats.cycles || []"
          ><el-table-column prop="cycleFrom" label="开始" /><el-table-column
            prop="cycleTo"
            label="结束"
          /><el-table-column prop="plannedMinutes" label="排班分钟" /><el-table-column
            prop="actualMinutes"
            label="实际非节假日分钟"
          /><el-table-column prop="recognizedMinutes" label="批准认可分钟" /><el-table-column
            prop="payableMinutes"
            label="超标准计薪分钟"
          /><el-table-column prop="amount" label="结算加班费" /><el-table-column
            label="来源校验"
            width="160"
          >
            <template #default="{ row }"
              ><el-tag :type="row.sourceFresh === true ? 'success' : 'danger'">
                {{ row.sourceFresh === true ? '来源一致' : '来源变化，需更正' }}
              </el-tag></template
            ></el-table-column
          ><el-table-column label="证据"
            ><template #default="{ row }"
              ><el-button link @click="showProof(row.sourceSnapshot)">结算快照</el-button></template
            ></el-table-column
          ></el-table
        >
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
  <Dialog v-model="visible" :title="titles[kind]" width="650px">
    <el-form label-width="140px">
      <template v-if="kind === 'rule'">
        <el-form-item v-if="draft.priorRuleId" label="原批准版本">{{
          draft.priorRuleId
        }}</el-form-item>
        <el-form-item label="工时制度"
          ><el-select v-model="draft.systemType"
            ><el-option label="标准工时" value="STANDARD" /><el-option
              label="综合工时"
              value="COMPREHENSIVE" /><el-option label="不定时工时" value="IRREGULAR" /></el-select
        ></el-form-item>
        <el-form-item label="生效日"
          ><el-date-picker v-model="draft.effectiveFrom" value-format="YYYY-MM-DD" /></el-form-item
        ><el-form-item label="结束日"
          ><el-date-picker v-model="draft.effectiveTo" value-format="YYYY-MM-DD"
        /></el-form-item>
        <el-form-item label="制度批准依据"
          ><el-input v-model="draft.legalBasis" type="textarea" maxlength="1000"
        /></el-form-item>
        <el-form-item v-for="f in ruleNumbers" :key="f.key" :label="f.label"
          ><el-input-number v-model="draft[f.key]" :min="0" :precision="f.precision"
        /></el-form-item>
      </template>
      <template v-if="kind === 'entry'">
        <el-alert
          title="须提供已批准的 OA 加班编号和真实上下班打卡编号。日类型由排班和法定节假日日历确定；跨日含休息时长的申请请按日分别核实。"
          type="info"
          :closable="false"
        />
        <el-form-item label="OA 加班编号"><el-input v-model="draft.sourceRecordId" /></el-form-item
        ><el-form-item label="上班打卡编号"
          ><el-input-number v-model="draft.clockInId" :min="1" :precision="0" /></el-form-item
        ><el-form-item label="下班打卡编号"
          ><el-input-number v-model="draft.clockOutId" :min="1" :precision="0"
        /></el-form-item>
        <el-form-item label="开始时间"
          ><el-date-picker
            v-model="draft.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item
        ><el-form-item label="结束时间"
          ><el-date-picker
            v-model="draft.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
        /></el-form-item>
        <el-form-item label="休息分钟"
          ><el-input-number v-model="draft.breakMinutes" :min="0" :precision="0" /></el-form-item
        ><el-form-item label="补偿方式"
          ><el-select v-model="draft.compensation"
            ><el-option label="加班费" value="PAY" /><el-option
              label="调休（标准工时休息日）"
              value="TIME_OFF" /></el-select></el-form-item
        ><el-form-item label="核实依据"
          ><el-input v-model="draft.evidence" type="textarea"
        /></el-form-item>
      </template>
      <template v-if="kind === 'reserve'"
        ><el-form-item label="分钟"
          ><el-input-number v-model="draft.minutes" :min="1" :precision="0" /></el-form-item
        ><el-form-item label="调休申请唯一编号"
          ><el-input v-model="draft.key" maxlength="100" /></el-form-item
        ><el-form-item label="审批及占用依据"
          ><el-input v-model="draft.reason" type="textarea" /></el-form-item
      ></template>
      <template v-if="kind === 'zero'">
        <el-form-item label="原OA申请编号"><el-input v-model="draft.sourceApplyId" /></el-form-item>
        <el-form-item label="核实日期"
          ><el-date-picker v-model="draft.workDate" type="date" value-format="YYYY-MM-DD"
        /></el-form-item>
        <el-form-item label="事实说明"
          ><el-input v-model="draft.reason" type="textarea" maxlength="1000"
        /></el-form-item>
        <el-form-item label="核实依据"
          ><el-input
            v-model="draft.evidence"
            type="textarea"
            maxlength="1000"
            placeholder="主管确认单、任务取消凭证等"
        /></el-form-item>
      </template>
      <template v-if="kind === 'cycle'"
        ><el-form-item label="周期开始日"
          ><el-date-picker v-model="draft.from" value-format="YYYY-MM-DD" /></el-form-item
        ><el-alert
          title="选择制度生效日起的完整周期。仅已结束周期可以结算，法定节假日加班独立计薪。"
          type="info"
          :closable="false"
      /></template>
    </el-form>
    <template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save">提交</el-button></template
    >
  </Dialog>
  <Dialog v-model="proofVisible" title="来源与规则快照">
    <pre style="white-space: pre-wrap; word-break: break-all">{{ proof }}</pre>
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/work'
import dayjs from 'dayjs'
import * as entityApi from '@/api/hrm/payroll/entity'
defineOptions({ name: 'HrmPayrollWork' })
const message = useMessage()
const entityId = ref<number>()
const entities = ref<entityApi.Entity[]>([])
const employeeId = ref<number>()
const employees = ref<{ id: number; name: string; jobNumber: string }[]>([])
const period = ref([
  dayjs().startOf('month').format('YYYY-MM-DD'),
  dayjs().endOf('month').format('YYYY-MM-DD')
])
const rules = ref<any[]>([]),
  entries = ref<any[]>([]),
  ledger = ref<any[]>([]),
  zeroCases = ref<any[]>([]),
  stats = ref<any>({})
const loading = ref(false),
  saving = ref(false),
  visible = ref(false),
  proofVisible = ref(false),
  proof = ref('')
type Kind = 'rule' | 'entry' | 'reserve' | 'cycle' | 'zero'
const kind = ref<Kind>('rule'),
  draft = ref<any>({})
const titles = {
  rule: '新建工时制度（待审批）',
  zero: '核实批准加班未实际发生（待独立复核）',
  entry: '核实加班',
  reserve: '调休申请占用',
  cycle: '综合工时周期结算'
}
const ruleNumbers = [
  { key: 'cycleMonths', label: '周期月数', precision: 0 },
  { key: 'cycleMinutes', label: '周期标准分钟', precision: 0 },
  { key: 'limitMinutes', label: '月加班上限分钟', precision: 0 },
  { key: 'timeOffExpiryDays', label: '调休有效天数', precision: 0 },
  { key: 'hourlyBase', label: '时薪基数', precision: 2 },
  { key: 'workdayRate', label: '工作日倍率', precision: 2 },
  { key: 'restdayRate', label: '休息日倍率', precision: 2 },
  { key: 'holidayRate', label: '法定节假日倍率', precision: 2 }
]
const formatTime = (_row: unknown, _col: unknown, value: any) =>
  value ? dayjs(value).format('YYYY-MM-DD HH:mm') : ''
let viewSequence = 0
async function load() {
  if (!entityId.value || !employeeId.value || period.value?.length !== 2) {
    message.warning('请选择主体、员工和统计区间')
    return
  }
  const sequence = ++viewSequence
  const selection = JSON.stringify([entityId.value, employeeId.value, period.value])
  loading.value = true
  try {
    const params = {
      entityId: entityId.value,
      employeeId: employeeId.value,
      from: period.value[0],
      to: period.value[1]
    }
    const result = await Promise.all([
      api.rules(entityId.value, employeeId.value),
      api.entries(params),
      api.ledger(entityId.value, employeeId.value),
      api.summary(params),
      api.noOvertimeCases({ entityId: entityId.value, employeeId: employeeId.value })
    ])
    if (
      sequence !== viewSequence ||
      selection !== JSON.stringify([entityId.value, employeeId.value, period.value])
    )
      return
    ;[rules.value, entries.value, ledger.value, stats.value, zeroCases.value] = result
  } finally {
    if (sequence === viewSequence) loading.value = false
  }
}
function open(type: Kind) {
  if (!entityId.value || !employeeId.value) {
    message.warning('请先选择主体和员工')
    return
  }
  kind.value = type
  draft.value = {
    entityId: entityId.value,
    employeeId: employeeId.value,
    workDate: period.value[0],
    requestKey: `ZERO-${Date.now()}-${Math.random().toString(36).slice(2)}`,
    systemType: 'STANDARD',
    effectiveFrom: period.value[0],
    effectiveTo: period.value[1],
    cycleMonths: 1,
    cycleMinutes: 10440,
    limitMinutes: 2160,
    timeOffExpiryDays: 90,
    hourlyBase: 0,
    workdayRate: 1.5,
    restdayRate: 2,
    holidayRate: 3,
    sourceSystem: 'OA',
    dayType: 'WORKDAY',
    compensation: 'PAY',
    breakMinutes: 0,
    minutes: 60
  }
  visible.value = true
}
function openRuleVersion(row: any) {
  let next = dayjs().add(1, 'day').startOf('day')
  if (row.systemType === 'COMPREHENSIVE') {
    next = dayjs(row.effectiveFrom)
    while (!next.isAfter(dayjs().startOf('day'))) next = next.add(row.cycleMonths, 'month')
  }
  if (next.isAfter(dayjs(row.effectiveTo).add(1, 'day'))) {
    message.warning('原制度已结束或剩余周期需先处理差异，请新建适用制度或处理周期差异')
    return
  }
  open('rule')
  draft.value = {
    ...row,
    priorRuleId: row.id,
    effectiveFrom: next.format('YYYY-MM-DD'),
    effectiveTo: dayjs(row.effectiveTo).isBefore(next)
      ? next.endOf('month').format('YYYY-MM-DD')
      : row.effectiveTo,
    legalBasis: ''
  }
}
function openCycle(ruleId: number) {
  open('cycle')
  draft.value.ruleId = ruleId
  draft.value.from = period.value[0]
}
async function save() {
  saving.value = true
  try {
    if (kind.value === 'rule') await api.createRule(draft.value)
    else if (kind.value === 'entry') await api.confirm(draft.value)
    else if (kind.value === 'zero') await api.submitNoOvertime(draft.value)
    else if (kind.value === 'reserve') await api.reserve(draft.value)
    else await api.settle({ ruleId: draft.value.ruleId, from: draft.value.from })
    visible.value = false
    message.success('提交成功')
    await load()
  } finally {
    saving.value = false
  }
}
async function approve(id: number) {
  await message.confirm('确认该员工适用制度及批准依据？提交人不能审批自己的制度。')
  await api.approveRule(id)
  await load()
}
async function reviewZero(id: number, approved: boolean) {
  const { value } = await message.prompt(
    '请输入事实复核意见',
    approved ? '确认未发生有效加班' : '驳回差异申请'
  )
  await api.reviewNoOvertime({ id, approved, opinion: value })
  await load()
}
async function rejectRule(id: number) {
  const { value } = await message.prompt('请输入驳回依据', '驳回工时制度')
  await api.rejectRule(id, value)
  await load()
}
async function consume(id: number) {
  await message.confirm('确认调休已实际使用并核销？')
  await api.consume(entityId.value!, id)
  await load()
}
async function refund(id: number) {
  const { value } = await message.prompt('请输入撤销或退回依据', '退回调休')
  await api.refund({ entityId: entityId.value, debitId: id, key: `REFUND-${id}`, reason: value })
  await load()
}
function showProof(value: string) {
  proof.value = JSON.stringify(JSON.parse(value), null, 2)
  proofVisible.value = true
}
watch(
  () => [entityId.value, ...(period.value || [])],
  async () => {
    const sequence = ++viewSequence
    loading.value = false
    employeeId.value = undefined
    employees.value = []
    rules.value = []
    entries.value = []
    ledger.value = []
    zeroCases.value = []
    stats.value = {}
    if (!entityId.value || period.value?.length !== 2) return
    const selected = entityId.value
    const rows = await api.employees({
      entityId: selected,
      from: period.value[0],
      to: period.value[1]
    })
    if (sequence === viewSequence && entityId.value === selected) employees.value = rows
  }
)
onMounted(async () => {
  entities.value = (await entityApi.list()).filter(
    (e) => e.status === 'APPROVED' && e.allowedActions.includes('READ')
  )
})
</script>
