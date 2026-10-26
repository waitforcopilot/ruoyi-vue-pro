<template>
  <section
    id="operation-calc-scenario"
    class="calc-scenario"
    tabindex="-1"
    aria-label="TEST工资核算完整讨论流程"
  >
    <h2>CALC-01～05 · 同一TEST试算版本的完整讨论流程</h2>
    <p
      >项目{{ state.context.projectId ?? '未选' }} · {{ state.context.period }} ·
      {{
        state.context.scenario === 'ADJUSTMENT' ? '独立人工调整候选' : '固定主样例'
      }}。顶部核算、复核、工资条与本区共用一个状态，切项目/模块/期间/场景须重新准备与试算。</p
    >
    <div class="calc-controls">
      <label
        >TEST期间<el-select
          :model-value="state.context.period"
          :disabled="state.frozen"
          aria-label="工资核算TEST期间"
          @change="emit('period', $event)"
          ><el-option label="2026-10主期" value="2026-10" /><el-option
            label="2026-11独立TEST期"
            value="2026-11" /></el-select
      ></label>
      <label
        >主例/候选<el-select
          :model-value="state.context.scenario"
          :disabled="state.frozen"
          aria-label="工资核算TEST场景"
          @change="emit('scenario', $event)"
          ><el-option label="固定主样例（无人工调整）" value="MAIN-V1" /><el-option
            label="独立人工调整候选"
            value="ADJUSTMENT" /></el-select
      ></label>
      <el-button :disabled="!canCreate" @click="emit('opinion')">补充核算复核与回盘意见</el-button>
    </div>
    <p
      >状态：{{ calcStageLabels[state.stage] }} · 试算版本{{ state.revision }} · 当前复核版本{{
        state.reviewedRevision ?? '无'
      }}
      · {{ state.frozen ? '已冻结' : '未冻结' }}。{{ calcBatchLabel(state) }}</p
    >
    <p role="status" aria-live="polite">{{ state.notice }}</p>
    <h3>CALC-01 / CALC-02 · 准备缺口、人工调整与试算前后</h3>
    <p
      >正常准备：排班160＝认可出勤152＋带薪假8，加班8小时/750元，个人1400、单位2100。11月仅复制同构TEST输入用于失效检查，非真实11月记录。</p
    >
    <div class="calc-controls">
      <el-checkbox
        :model-value="state.missing"
        aria-label="模拟考勤输入缺失"
        @change="emit('action', 'SET_MISSING', Boolean($event))"
        >模拟缺DS-02考勤认可输入</el-checkbox
      >
      <label
        >独立候选人工调整（元）<el-input-number
          :key="`adjustment-${state.context.scenario}-${state.frozen ? 'frozen' : 'editable'}`"
          :model-value="state.adjustment"
          :min="-1000"
          :max="1000"
          :precision="2"
          :disabled="state.context.scenario !== 'ADJUSTMENT' || state.frozen"
          aria-label="TEST独立候选人工调整金额"
          @change="onAdjustment"
      /></label>
    </div>
    <p
      >人工调整仅用于TEST补差单TEST-ADJ-001；候选税额＝主税130.50＋调整额×TEST3%，净额增加调整额×97%，非真实税务规则。输入变更、驳回或重算都会清除原复核及回盘。</p
    >
    <div class="calc-controls">
      <el-button @click="emit('action', 'PREPARE')">核对TEST准备</el-button>
      <el-button @click="emit('action', 'CALCULATE')">试算/重算TEST当前版本</el-button>
      <el-button @click="emit('action', 'REVIEW')">复核TEST当前版本</el-button>
      <el-button @click="emit('action', 'REJECT')">TEST复核驳回</el-button>
      <el-button @click="emit('action', 'FREEZE')">冻结TEST快照</el-button>
      <el-button @click="emit('action', 'UNFREEZE')">解冻并清除复核</el-button>
    </div>
    <p
      >冻结仅锁定本地TEST快照，禁止修改、重算或继续推进；解冻使原复核失效。项目/模块切换清空本地状态。所有操作无正式工资、审批、银行、税务或消息接口。</p
    >
    <div class="calc-table" tabindex="0" aria-label="主链与当前试算前后，可横向滚动"
      ><table
        ><thead
          ><tr
            ><th>范围</th><th>人工调整</th><th>应发</th><th>个人扣款</th><th>TEST税</th><th>实发</th
            ><th>成本</th></tr
          ></thead
        ><tbody>
          <tr
            ><td>固定10月主链（始终保留）</td><td>0元</td><td>{{ result.gross }}</td
            ><td>{{ result.personal }}</td
            ><td>{{ result.tax }}</td
            ><td>{{ result.net }}</td
            ><td>{{ result.cost }}</td></tr
          >
          <tr
            ><td>当前TEST试算版本{{ state.revision }} / {{ state.context.period }}</td
            ><td>{{ state.calculatedAdjustment ?? '未试算' }}</td
            ><td>{{ amounts?.gross ?? '未知' }}</td
            ><td>{{ amounts?.personal ?? '未知' }}</td
            ><td>{{ amounts?.tax ?? '未知' }}</td
            ><td>{{ amounts?.net ?? '未知' }}</td
            ><td>{{ amounts?.cost ?? '未知' }}</td></tr
          >
        </tbody></table
      ></div
    >
    <el-button :disabled="state.calculatedAdjustment === null" @click="emit('slip')"
      >预览当前TEST试算工资条</el-button
    >
    <h3>CALC-03 / CALC-04 · 文件列预览与部分回盘</h3>
    <p
      >以下仅列预览：员工ID、期间、TEST账户标识、实发金额元、币种CNY。TEST-EMP-002为独立失败员工，5000元不计入主链；批次预览总额{{
        batchAmount.toFixed(2)
      }}元不能当主员工工资。正式银行格式待业务确认，不生成代发文件。</p
    >
    <div class="calc-table" tabindex="0" aria-label="TEST代发列与人员金额匹配，可横向滚动"
      ><table
        ><thead
          ><tr
            ><th>员工ID</th><th>期间</th><th>账户（TEST）</th><th>列预览实发/元</th><th>币种</th
            ><th>TEST回盘状态</th><th>回盘金额/元及依据</th></tr
          ></thead
        ><tbody
          ><tr v-for="row in paymentRows" :key="row.employee"
            ><td>{{ row.employee }}</td
            ><td>{{ row.period }}</td
            ><td>{{ row.account }}</td
            ><td>{{
              state.calculatedAdjustment === null
                ? '未试算，列示范不用于发放'
                : row.amount.toFixed(2)
            }}</td
            ><td>CNY</td
            ><td>{{
              returnFor(row.employee)?.status === 'SUCCESS'
                ? 'TEST成功'
                : returnFor(row.employee)?.status === 'FAILED'
                  ? 'TEST失败，待处理'
                  : '无有效回盘'
            }}</td
            ><td
              >{{ returnFor(row.employee)?.amount ?? '未知' }}；{{
                returnFor(row.employee)?.reason ?? '待载入虚构回盘'
              }}</td
            ></tr
          ></tbody
        ></table
      ></div
    >
    <p
      >回盘逐人核对ID＋期间＋实发金额，拒绝重复人员、错人或错额；载入两次按当前版本替换，不重复累计。部分成功不得标全批已发放。</p
    >
    <div class="calc-controls">
      <el-button @click="emit('action', 'PAYMENT')">进入TEST发放讨论</el-button>
      <el-button @click="emit('action', 'RETURN_PARTIAL')">载入TEST一成一败回盘</el-button>
      <el-button @click="emit('action', 'RETURN_INVALID')">载入TEST错人错额回盘（拦截）</el-button>
      <el-button @click="emit('action', 'ARCHIVE')">记录TEST归档讨论</el-button>
    </div>
    <p
      >归档讨论保留一成一败及失败待处理状态，不代表全付、业务结清或真实归档。专用意见从既有保存/历史返回CALC-03，真实需求与TEST流程状态分别保存。</p
    >
  </section>
</template>
<script setup lang="ts">
import { result } from './demo'
import {
  calcStageLabels,
  calcBatchLabel,
  calcAmounts,
  calcPaymentRows,
  type CalcState,
  type CalcAction
} from './calc-model'
const props = defineProps<{ state: CalcState; canCreate: boolean }>()
const emit = defineEmits<{
  action: [action: CalcAction, value?: number | boolean | null]
  period: [period: string]
  scenario: [scenario: string]
  opinion: []
  slip: []
}>()
const amounts = computed(() =>
  props.state.calculatedAdjustment === null ? null : calcAmounts(props.state.calculatedAdjustment)
)
const paymentRows = computed(() => calcPaymentRows(props.state))
const batchAmount = computed(() => paymentRows.value.reduce((sum, row) => sum + row.amount, 0))
const returnFor = (employee: string) => props.state.returns.find((row) => row.employee === employee)
function onAdjustment(value: number | null | undefined) {
  emit('action', 'SET_ADJUSTMENT', value ?? null)
}
</script>
<style scoped>
.calc-scenario {
  min-width: 0;
}

.calc-controls {
  display: flex;
  margin: 16px 0;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.calc-controls label {
  display: flex;
  width: 300px;
  max-width: 100%;
  flex-direction: column;
  gap: 8px;
}

.calc-controls .el-select {
  width: 100%;
}

.calc-controls .el-button {
  margin-left: 0;
}

.calc-controls .el-checkbox {
  height: auto;
  white-space: normal;
}

.calc-table {
  width: 100%;
  margin: 12px 0;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 1000px;
  border-collapse: collapse;
}

th,
td {
  padding: 10px;
  text-align: left;
  border: 1px solid var(--el-border-color);
}

p {
  overflow-wrap: anywhere;
}

.calc-scenario:focus,
.calc-table:focus {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

@media (width <= 600px) {
  .calc-controls label {
    width: 100%;
  }

  .calc-controls .el-button {
    height: auto;
    max-width: 100%;
    min-height: 32px;
    white-space: normal;
  }
}
</style>
