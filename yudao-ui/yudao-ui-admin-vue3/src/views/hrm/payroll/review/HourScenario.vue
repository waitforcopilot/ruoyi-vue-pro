<template>
  <section
    id="operation-hour-scenario"
    class="hour-scenario"
    tabindex="-1"
    aria-label="TEST工时制度与周期对账"
  >
    <h2>HOUR-01～05 · TEST工时制度与周期对账</h2>
    <p>全部为虚构规则及记录，非法律结论或真实批准。仅用于讨论，不写正式排班、考勤、工时或工资。</p>
    <div class="hour-form">
      <label
        >工时制度<el-select v-model="regime" aria-label="工时制度示例"
          ><el-option
            v-for="(item, key) in hourRegimes"
            :key="key"
            :label="item.label"
            :value="key" /></el-select
      ></label>
      <label
        >周期/依据场景<el-select v-model="scenario" aria-label="工时周期与依据场景"
          ><el-option
            v-for="key in hourRegimes[regime].options"
            :key="key"
            :label="hourExamples[key].label"
            :value="key" /></el-select
      ></label>
      <el-button :disabled="!canCreate" @click="emit('opinion')">补充工时周期与依据意见</el-button>
    </div>
    <p>{{ example.subject }} · {{ example.employee }} · 单位统一为小时</p>
    <p
      >制度适用有效期：{{ example.effectiveFrom }} → {{ example.effectiveTo }}；结算月份：{{
        example.expectedPeriods.join('、')
      }}。</p
    >
    <p>适用/批准依据：{{ example.basis || '缺失，待补证；不得推定制度已批准适用' }}</p>
    <div class="hour-table" tabindex="0" aria-label="分月排班实际认可对照，可横向滚动">
      <table
        ><thead
          ><tr
            ><th>期间</th><th>排班应计</th><th>实际出勤</th><th>批准带薪假</th><th>出勤＋假记录</th
            ><th>独立认可加班记录</th><th>结清/排班依据</th></tr
          ></thead
        >
        <tbody
          ><tr v-for="month in example.months" :key="month.period"
            ><td>{{ month.period }}</td
            ><td>{{ hourLabel(month.scheduled) }}</td
            ><td>{{ hourLabel(month.attended) }}</td
            ><td>{{ hourLabel(month.paidLeave) }}</td
            ><td>{{ hourLabel(sumKnownHours([month.attended, month.paidLeave])) }}</td
            ><td>{{ hourLabel(month.overtime) }}</td
            ><td
              >{{ month.closed ? '该月记录已结清（TEST）' : '尚未结清' }}；{{ month.schedule }}</td
            ></tr
          ></tbody
        >
      </table>
    </div>
    <div aria-live="polite">
      <p role="status">{{ summary.status }}</p>
      <p>{{ example.explanation }}</p>
      <div class="hour-table" tabindex="0" aria-label="周期记录与可认可结果对照，可横向滚动">
        <table
          ><thead
            ><tr
              ><th>核对项</th><th>已提供记录小计（不补缺期）</th><th>完整周期/认可结果</th
              ><th>判定依据</th></tr
            ></thead
          >
          <tbody>
            <tr
              ><td>排班应计</td><td>{{ hourLabel(summary.observed.scheduled) }}</td
              ><td>{{ hourLabel(summary.scheduled) }}</td
              ><td>{{
                summary.completeCoverage ? '已含预期全部月份' : '缺预期月份，不把小计当周期合计'
              }}</td></tr
            >
            <tr
              ><td>实际出勤＋带薪假</td
              ><td
                >{{ hourLabel(summary.observed.attended) }} ＋
                {{ hourLabel(summary.observed.paidLeave) }} ＝ {{ hourLabel(summary.recorded) }}</td
              ><td>认可基础{{ hourLabel(summary.regular) }}</td
              ><td>需依据齐备、周期记录结清；不定时不自动认可计薪</td></tr
            >
            <tr
              ><td>基础差异（认可－应计）</td><td>加班不混入基础差异</td
              ><td>{{ hourLabel(summary.difference) }}</td
              ><td>未知不归零，不把缺卡或缺期直接当缺勤</td></tr
            >
            <tr
              ><td>加班</td><td>{{ hourLabel(summary.observed.overtime) }}</td
              ><td>周期认可{{ hourLabel(summary.overtime) }}</td
              ><td>与基础分开；未结清或缺依据不能自动认定</td></tr
            >
            <tr
              ><td>基本工资分母</td><td>标准主链{{ demo.scheduledHours }}小时</td
              ><td>{{ hourLabel(summary.salaryDenominator) }}</td
              ><td>只有标准示例使用160；综合/不定时计薪规则另评审，不套标准分母</td></tr
            >
          </tbody>
        </table>
      </div>
    </div>
    <p
      >与ATT/OT主样例对账：排班{{ demo.scheduledHours }}＝出勤{{ demo.attendedHours }}＋带薪假{{
        demo.paidLeaveHours
      }}；加班{{ demo.overtimeHours }}小时另列，{{ result.hourly }}元/小时×{{
        demo.overtimeHours
      }}×TEST倍率{{ demo.multiplier }}＝{{ result.overtime }}元，仅入薪一次且不同时给调休。</p
    >
    <p
      >任何制度/周期切换均不修改10月主链：基本{{ demo.base }}、应发{{ result.gross }}、实发{{
        result.net
      }}。综合/不定时的候选工资影响仍待规则核定。</p
    >
  </section>
</template>
<script setup lang="ts">
import { demo, result } from './demo'
import {
  hourExamples,
  hourRegimes,
  summarizeHours,
  sumKnownHours,
  hourLabel,
  type HourRegime,
  type HourExampleKey
} from './hour-model'
defineProps<{ canCreate: boolean }>()
const emit = defineEmits<{ opinion: [] }>()
const regime = ref<HourRegime>('STANDARD'),
  scenario = ref<HourExampleKey>('STANDARD')
watch(
  regime,
  (value) => {
    scenario.value = hourRegimes[value].options[0]
  },
  { flush: 'sync' }
)
const example = computed(() => hourExamples[scenario.value])
const summary = computed(() => summarizeHours(example.value))
</script>
<style scoped>
.hour-scenario {
  min-width: 0;
}

.hour-form {
  display: flex;
  margin: 16px 0;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.hour-form label {
  display: flex;
  width: 300px;
  max-width: 100%;
  flex-direction: column;
  gap: 8px;
}

.hour-form .el-select {
  width: 100%;
}

.hour-table {
  width: 100%;
  margin: 12px 0;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 900px;
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

.hour-scenario:focus,
.hour-table:focus {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

@media (width <= 600px) {
  .hour-form label {
    width: 100%;
  }

  .hour-form .el-button {
    height: auto;
    max-width: 100%;
    min-height: 32px;
    margin-left: 0;
    white-space: normal;
  }
}
</style>
