<template>
  <section
    id="operation-att-scenario"
    class="att-scenario"
    tabindex="-1"
    aria-label="TEST日记录到月考勤汇总"
  >
    <h2>ATT-01～05 · TEST日记录、单据与月汇总</h2>
    <p
      >{{ demo.employee }} · TEST-SUBJECT-001虚构主体 ·
      {{ demo.period }}。班次及异常阈值皆为TEST讨论规则，非真实日历、设备采集或政策。</p
    >
    <div class="att-form">
      <label
        >正常/异常场景<el-select v-model="scenario" aria-label="考勤正常与补正场景"
          ><el-option
            v-for="(item, key) in attendanceExamples"
            :key="key"
            :label="item.label"
            :value="key" /></el-select
      ></label>
      <el-button :disabled="!canCreate" @click="emit('opinion')"
        >补充考勤补正与工资影响意见</el-button
      >
    </div>
    <p role="status">{{ example.explanation }}</p>
    <div class="att-table" tabindex="0" aria-label="原始日记录与认可前后，可横向滚动">
      <table
        ><thead
          ><tr
            ><th>记录/日期</th><th>原始异常类别</th><th>排班应计</th><th>原始打卡（不改写）</th
            ><th>当前认可出勤</th><th>批准带薪假</th><th>合计覆盖</th><th>关联单据与依据</th></tr
          ></thead
        >
        <tbody
          ><tr v-for="day in summary.days" :key="day.id"
            ><td>{{ day.id }} / {{ day.date }}</td
            ><td>{{ attendanceKinds[day.kind] }}</td
            ><td>{{ attendanceHourLabel(day.scheduled) }}</td
            ><td>{{ attendanceHourLabel(day.rawPunch) }}</td
            ><td>{{ attendanceHourLabel(day.recognized) }}</td
            ><td>{{ attendanceHourLabel(day.paidLeave) }}</td
            ><td>{{ attendanceHourLabel(day.covered) }}</td
            ><td
              >{{ day.linked.map((doc) => doc.id).join('、') || '无关联单据' }}；{{ day.note }}；{{
                day.reason
              }}</td
            ></tr
          ></tbody
        >
      </table>
    </div>
    <h3>请假/出差/外出/补卡单据</h3>
    <p
      >单据关联同一记录ID。批准状态与认可结果分开，未批/驳回不新增认可；同记录补正替换认可，不对月汇总再加一次。同日重复覆盖出现矛盾时保留未知待核对。</p
    >
    <div class="att-table" tabindex="0" aria-label="关联考勤单据，可横向滚动">
      <table
        ><thead
          ><tr
            ><th>单据ID</th><th>关联记录</th><th>类型</th><th>时长</th><th>状态/认可口径</th></tr
          ></thead
        ><tbody
          ><tr v-for="(doc, i) in example.documents" :key="`${doc.id}-${i}`"
            ><td>{{ doc.id }}</td
            ><td>{{ doc.recordId }}</td
            ><td>{{ attendanceDocumentTypes[doc.type] }}</td
            ><td>{{ attendanceHourLabel(doc.hours) }}</td
            ><td
              >{{ attendanceDocumentStates[doc.status] }}；{{
                doc.type === 'OUTING'
                  ? '原打卡＋批准外出覆盖时长'
                  : doc.type === 'LEAVE'
                    ? '批准带薪假独立列示'
                    : '替换该记录认可出勤，原打卡保留'
              }}</td
            ></tr
          ></tbody
        ></table
      >
    </div>
    <h3>逐记录重算后的月汇总</h3>
    <div
      class="att-table"
      tabindex="0"
      aria-label="日到月及工资影响对账，可横向滚动"
      aria-live="polite"
    >
      <table
        ><thead
          ><tr
            ><th>排班应计</th><th>原始打卡合计</th><th>认可出勤合计</th><th>批准带薪假</th
            ><th>认可＋假</th><th>待核对缺口</th><th>独立加班</th></tr
          ></thead
        ><tbody
          ><tr
            ><td>{{ attendanceHourLabel(summary.scheduled) }}</td
            ><td>{{ attendanceHourLabel(summary.rawPunch) }}</td
            ><td>{{ attendanceHourLabel(summary.recognized) }}</td
            ><td>{{ attendanceHourLabel(summary.paidLeave) }}</td
            ><td>{{ attendanceHourLabel(summary.covered) }}</td
            ><td>{{ attendanceHourLabel(summary.uncovered) }}（不自动视为缺勤）</td
            ><td>{{ attendanceMainOvertime }}小时；不混入考勤分母</td></tr
          ></tbody
        ></table
      >
    </div>
    <p
      >工资影响：{{
        summary.wageDeduction === null
          ? '未知；未生成扣款金额'
          : `本TEST缺勤扣款${summary.wageDeduction}元`
      }}。{{ summary.wageExplanation }}</p
    >
    <p
      >主样例始终为认可出勤{{ demo.attendedHours }}＋带薪假{{ demo.paidLeaveHours }}＝{{
        demo.scheduledHours
      }}；加班{{ demo.overtimeHours }}小时另列、金额{{
        result.overtime
      }}只入薪一次。异常分支不改主样例：应发{{ result.gross }}、实发{{ result.net }}。</p
    >
    <p
      >补正状态只在页面虚构场景内切换，不提交真实补卡、请假、核算或审批；围绕原始记录/批准依据保存的TEST评审意见可从历史返回ATT-04。</p
    >
  </section>
</template>
<script setup lang="ts">
import { demo, result } from './demo'
import {
  attendanceExamples,
  attendanceKinds,
  attendanceDocumentTypes,
  attendanceDocumentStates,
  summarizeAttendance,
  attendanceHourLabel,
  attendanceMainOvertime,
  type AttendanceExampleKey
} from './att-model'
defineProps<{ canCreate: boolean }>()
const emit = defineEmits<{ opinion: [] }>()
const scenario = ref<AttendanceExampleKey>('NORMAL')
const example = computed(() => attendanceExamples[scenario.value])
const summary = computed(() => summarizeAttendance(example.value))
</script>
<style scoped>
.att-scenario {
  min-width: 0;
}

.att-form {
  display: flex;
  margin: 16px 0;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.att-form label {
  display: flex;
  width: 360px;
  max-width: 100%;
  flex-direction: column;
  gap: 8px;
}

.att-form .el-select {
  width: 100%;
}

.att-table {
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

.att-scenario:focus,
.att-table:focus {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

@media (width <= 600px) {
  .att-form label {
    width: 100%;
  }

  .att-form .el-button {
    height: auto;
    max-width: 100%;
    min-height: 32px;
    margin-left: 0;
    white-space: normal;
  }
}
</style>
