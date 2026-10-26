<template>
  <section id="operation-adjustment" tabindex="-1" aria-label="虚构调薪表单" class="plan-scenario">
    <h2>PLAN-01～04 · TEST薪酬结构、薪档及候选版本</h2>
    <p>全部为虚构讨论样例，仅页面预览；意见通过原评审入口真实保存。</p>
    <p
      >{{ planScope.subject }} · {{ planScope.employee }} · 当前期间{{
        demo.period
      }}；适用范围仅此TEST员工。</p
    >
    <div class="plan-table" tabindex="0" aria-label="工资结构规则示例，可横向滚动">
      <table>
        <thead
          ><tr
            ><th>工资项</th><th>结构/单位</th><th>TEST规则与金额</th><th>生效与依据状态</th></tr
          ></thead
        >
        <tbody>
          <tr
            ><td>基本工资</td><td>固定 · 元/月</td
            ><td>{{ demo.base }}；{{ planScope.gradeGroup }}/{{ planScope.gradeCode }}</td
            ><td>{{ planScope.currentEffective }}起的当前V1</td></tr
          >
          <tr
            ><td>绩效</td><td>浮动项讨论 · 元/月</td
            ><td>本期虚构固定{{ demo.performance }}；真实系数未知</td
            ><td>绩效来源、系数及批准规则待业务确认</td></tr
          >
          <tr
            ><td>加班工资</td><td>按认可小时 · 元</td
            ><td
              >{{ result.hourly }} × {{ demo.overtimeHours }} × {{ demo.multiplier }} =
              {{ result.overtime }}</td
            ><td>TEST规则；工资/调休不得重复补偿</td></tr
          >
          <tr
            ><td>扣款和税</td><td>个人缴费/演示税 · 元</td
            ><td>{{ result.personal }} / {{ result.tax }}</td
            ><td>独立于薪档基本工资；不并入薪档金额</td></tr
          >
        </tbody>
      </table>
    </div>
    <div class="plan-table" tabindex="0" aria-label="薪级薪档与适用生效示例，可横向滚动">
      <table>
        <thead
          ><tr
            ><th>版本</th><th>薪级/薪档</th><th>适用主体/人员</th><th>基本工资</th
            ><th>有效期/性质</th></tr
          ></thead
        >
        <tbody>
          <tr
            ><td>V1当前</td><td>TEST-A / TEST-01</td><td>TEST-SUBJECT-001 / TEST-EMP-001</td
            ><td>{{ demo.base }}元/月</td><td>2026-10-01起；本期主链固定</td></tr
          >
          <tr
            ><td>V2候选</td><td>TEST-A / TEST-02</td><td>同一虚构主体/员工</td
            ><td>{{ candidateCheck.valid ? candidateCheck.amount : '未知' }}元/月</td
            ><td>{{ planScenarios[scenario].effective }}拟生效；未发布</td></tr
          >
        </tbody>
      </table>
    </div>
    <div class="plan-form">
      <label
        >候选基本工资（元/月）<el-input
          v-model="candidateInput"
          aria-label="示例调薪金额"
          inputmode="decimal"
          placeholder="例如10500，最多两位小数"
      /></label>
      <label
        >候选场景<el-select v-model="scenario" aria-label="调薪候选场景"
          ><el-option
            v-for="(branch, key) in planScenarios"
            :key="key"
            :label="branch.label"
            :value="key" /></el-select
      ></label>
      <el-button type="primary" @click="preview">预览演示调薪</el-button>
      <el-button :disabled="!canCreate" @click="emit('opinion')">补充调薪操作意见</el-button>
    </div>
    <p v-if="!candidateCheck.valid" role="alert">{{ candidateCheck.error }}</p>
    <p v-if="!previewResult" role="status"
      >候选尚未预览或输入已变化，请重新预览。本期V1仍{{ demo.base }}元，应发{{
        result.gross
      }}元。</p
    >
    <div v-else aria-live="polite">
      <p v-if="!previewResult.valid" role="alert"
        >预览未生成：{{ previewResult.error }} 本期主链不变。</p
      >
      <template v-else>
        <h3>V1当前与V2候选并排对照</h3>
        <div class="plan-table" tabindex="0" aria-label="当前方案与候选差额，可横向滚动">
          <table
            ><thead
              ><tr><th>比较项</th><th>V1当前</th><th>V2候选</th><th>差异/结果</th></tr></thead
            >
            <tbody>
              <tr
                ><td>完整月基本工资</td><td>{{ previewResult.currentBase }}元</td
                ><td>{{ previewResult.candidateBase }}元</td
                ><td>{{ previewResult.fullMonthDifference }}元（仅基本工资）</td></tr
              >
              <tr
                ><td>生效日期</td><td>{{ planScope.currentEffective }}</td
                ><td>{{ previewResult.effective }}（拟）</td><td>本期{{ demo.period }}仍V1</td></tr
              >
              <tr
                ><td>审批/依据</td><td>当前TEST基线</td><td>{{ previewResult.approval }}</td
                ><td>{{ previewResult.basis }}</td></tr
              >
              <tr
                ><td>候选期间可应用差额</td><td>未变更</td
                ><td>{{
                  previewResult.applicableDifference === null
                    ? '未知或暂不应用'
                    : `${previewResult.applicableDifference}元（下期完整月演示）`
                }}</td
                ><td>{{ previewResult.outcome }}</td></tr
              >
            </tbody>
          </table>
        </div>
        <p
          >候选只比较基本工资；候选绩效、加班、个税和净额未计算。当前主链应发{{
            result.gross
          }}、实发{{ result.net }}，与其他视图保持一致。</p
        >
      </template>
    </div>
    <h3>薪档批量样例校验（无文件上传、无档案导入）</h3>
    <p
      >字段：主体、适用员工、薪级、薪档、金额、单位、生效日、失效日。重复按同主体＋薪级＋薪档识别，不静默覆盖。以下为内置TEST行，切换后可校验正常与异常反馈。</p
    >
    <div class="plan-form">
      <label
        >样例批次<el-select v-model="importSample" aria-label="薪档样例校验分支"
          ><el-option
            v-for="(sample, key) in planImportSamples"
            :key="key"
            :label="sample.label"
            :value="key" /></el-select
      ></label>
      <el-button @click="checkImport">校验薪档样例</el-button>
    </div>
    <div class="plan-table" tabindex="0" aria-label="薪档导入字段样例，可横向滚动">
      <table
        ><thead
          ><tr><th>主体/员工</th><th>薪级/薪档</th><th>金额/单位</th><th>有效期</th></tr></thead
        >
        <tbody
          ><tr v-for="(row, i) in planImportSamples[importSample].rows" :key="i"
            ><td>{{ row.subject }} / {{ row.employee }}</td
            ><td>{{ row.gradeGroup }} / {{ row.gradeCode }}</td
            ><td>{{ row.amount === '' ? '空值（未知）' : row.amount }} / {{ row.unit }}</td
            ><td>{{ row.validFrom }} → {{ row.validTo }}</td></tr
          ></tbody
        >
      </table>
    </div>
    <p v-if="!importResult" role="status">当前样例尚未校验；不推定可导入。</p>
    <div v-else aria-live="polite"
      ><p v-for="row in importResult" :key="row.line"
        >第{{ row.line }}行：{{
          row.valid ? '样例字段校验通过；未导入档案' : `未通过：${row.errors.join('；')}`
        }}</p
      ></div
    >
  </section>
</template>
<script setup lang="ts">
import { demo, result } from './demo'
import {
  checkPlanAmount,
  planScope,
  planScenarios,
  previewPlan,
  planImportSamples,
  validatePlanImport,
  type PlanScenario,
  type PlanImportSample
} from './plan-model'
defineProps<{ canCreate: boolean }>()
const emit = defineEmits<{ opinion: [] }>()
const candidateInput = ref('10500'),
  scenario = ref<PlanScenario>('NORMAL')
const candidateCheck = computed(() => checkPlanAmount(candidateInput.value))
const previewResult = ref<ReturnType<typeof previewPlan>>()
function preview() {
  previewResult.value = previewPlan(candidateInput.value, scenario.value)
}
watch([candidateInput, scenario], () => {
  previewResult.value = undefined
})
const importSample = ref<PlanImportSample>('VALID')
const importResult = ref<ReturnType<typeof validatePlanImport>>()
function checkImport() {
  importResult.value = validatePlanImport(planImportSamples[importSample.value].rows)
}
watch(importSample, () => {
  importResult.value = undefined
})
</script>
<style scoped>
.plan-scenario {
  min-width: 0;
}

.plan-form {
  display: flex;
  margin: 16px 0;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.plan-form label {
  display: flex;
  width: 280px;
  max-width: 100%;
  flex-direction: column;
  gap: 8px;
}

.plan-form .el-select {
  width: 100%;
}

.plan-table {
  width: 100%;
  margin: 12px 0;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 780px;
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

.plan-scenario:focus,
.plan-table:focus {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

@media (width <= 600px) {
  .plan-form label {
    width: 100%;
  }

  .plan-form .el-button {
    height: auto;
    max-width: 100%;
    min-height: 32px;
    margin-left: 0;
    white-space: normal;
  }
}
</style>
