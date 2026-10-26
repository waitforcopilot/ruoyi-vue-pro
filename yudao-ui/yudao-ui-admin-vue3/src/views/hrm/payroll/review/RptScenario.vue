<template>
  <section class="rpt-scenario" tabindex="-1" aria-label="TEST工资业务报表口径预览">
    <h2>RPT-01～05 · TEST成本分摊与报表口径</h2>
    <p
      >{{ demo.employee }} · {{ demo.period }} ·
      TEST-SUBJECT-001；本区为独立讨论，不生成工资业务报表文件。顶部固定主例仅作参照，真实评审统计在共用征集区独立展示。</p
    >
    <div class="rpt-controls">
      <label
        >报表维度<el-select v-model="dimension" aria-label="报表分摊维度"
          ><el-option
            v-for="(item, key) in rptDimensions"
            :key="key"
            :label="item.label"
            :value="key" /></el-select
      ></label>
      <label
        >分摊规则样例<el-select v-model="allocationCase" aria-label="报表分摊规则样例"
          ><el-option label="正常合计100%" value="NORMAL" /><el-option
            label="重复分配键：阻断"
            value="DUPLICATE" /><el-option
            label="比例合计90%：阻断"
            value="INCOMPLETE" /></el-select
      ></label>
      <el-button :disabled="!canCreate" @click="emit('opinion')"
        >补充RPT-05报表列与导出用途意见</el-button
      >
    </div>
    <h3>RPT-01 · 单一维度成本分摊及人员下钻</h3>
    <p role="status"
      >{{ allocation.reason }}；当前合计{{ money(allocation.total) }}。项目正常70%/30%＝{{
        money(rptReferences.allocation.first)
      }}/{{ money(rptReferences.allocation.second) }}；只计同一员工一次。</p
    >
    <div class="rpt-table" tabindex="0" aria-label="分摊成本与人员下钻，可横向滚动"
      ><table
        ><thead
          ><tr><th>维度/分配键</th><th>输入比例</th><th>分摊成本</th><th>人员入口</th></tr></thead
        ><tbody
          ><tr v-for="row in inputRows" :key="row.inputId"
            ><td>{{ rptDimensions[dimension].label }} / {{ row.label }} / {{ row.key }}</td
            ><td>{{ row.percent }}%</td
            ><td>{{ money(allocation.rows.find((r) => r.key === row.key)?.cost ?? null) }}</td
            ><td
              ><el-button @click="drill(row.key)">查看{{ row.label }}人员</el-button></td
            ></tr
          ></tbody
        ></table
      ></div
    >
    <p role="status">{{ drillNotice }}</p>
    <div v-if="detail" class="rpt-table" tabindex="0" aria-label="同一员工分摊明细，可横向滚动"
      ><table
        ><thead
          ><tr
            ><th>员工/期间/主体</th><th>全量成本</th><th>当前分摊</th><th>原工资链</th></tr
          ></thead
        ><tbody
          ><tr
            ><td>{{ detail.employee }} / {{ detail.period }} / TEST-SUBJECT-001</td
            ><td>{{ money(result.cost) }}</td
            ><td>{{ detail.percent }}% / {{ money(detail.cost) }}</td
            ><td
              >应发{{ money(result.gross) }}；实发{{ money(result.net) }}；单位缴费{{
                money(result.employer)
              }}</td
            ></tr
          ></tbody
        ></table
      ></div
    >
    <h3>RPT-02 · 人数和营收分母</h3>
    <label class="rpt-select"
      >指标分母样例<el-select v-model="denominatorCase" aria-label="报表指标分母样例"
        ><el-option label="正常：1人/营收100000" value="NORMAL" /><el-option
          label="人数缺失"
          value="MISSING_COUNT" /><el-option label="营收缺失" value="MISSING_REVENUE" /><el-option
          label="两项均为0：不除"
          value="ZERO" /></el-select
    ></label>
    <p role="status"
      >去重人数{{ headcount === null ? '未知' : headcount }}；营收{{ money(revenue) }}；人均{{
        money(metrics.average)
      }}；成本率{{
        metrics.costRate === null ? '未知' : `${metrics.costRate.toFixed(2)}%`
      }}。分母缺失或为0时相关指标未知；维度切换不增加人数，不套0或100%。</p
    >
    <h3>RPT-03 · 结构与期间变动口径</h3>
    <p v-for="row in rptReferences.structure" :key="row.name"
      >{{ row.name }}：{{ money(row.amount) }}</p
    >
    <p
      >基本10000＋绩效1000＋加班750＋单位缴费2100＝成本13850。仅有2026-10同一员工结构，缺上期同口径来源，环比变动未知；不把实发或个人扣款重复计入成本。</p
    >
    <h3>RPT-04 · 预算版本与可比性</h3>
    <label class="rpt-select"
      >预算样例<el-select v-model="budgetCase" aria-label="报表预算比较样例"
        ><el-option label="同期间同版本预算14000" value="NORMAL" /><el-option
          label="期间不匹配2026-09"
          value="WRONG_PERIOD" /><el-option
          label="预算版本不匹配V2"
          value="WRONG_VERSION" /><el-option
          label="缺预算版本"
          value="MISSING_VERSION" /></el-select
    ></label>
    <p role="status"
      >预算{{ budget.period }} / {{ budget.subject }} / {{ budget.version ?? '版本未知' }} /
      {{ money(budget.amount) }}；成本{{ money(result.cost) }}；差额{{
        money(comparison.delta)
      }}。{{ comparison.reason }}。</p
    >
    <h3>RPT-05 · 三类输出用途和列预览</h3>
    <div class="rpt-table" tabindex="0" aria-label="报表与评审导出列用途预览，可横向滚动"
      ><table
        ><thead
          ><tr><th>输出类型</th><th>列/内容预览</th><th>范围及入口</th></tr></thead
        ><tbody
          ><tr
            ><td>工资业务报表列预览（无文件生成）</td
            ><td
              >主体、期间、员工、维度、分配键、比例%、应发元、个人扣款元、税元、实发元、单位缴费元、分摊成本元、预算版本、差额元</td
            ><td>当前TEST样例，仅讨论列定义，不代表工资导出/验收</td></tr
          ><tr
            ><td>当前筛选评审清单JSON</td
            ><td>真实需求/意见及当前筛选和权限范围；不是工资金额报表</td
            ><td>复用下方共用“导出当前筛选评审清单”；下载核对交COL</td></tr
          ><tr
            ><td>历史六表快照</td><td>发布时固定版本与受控快照内容；不是当前筛选或实时工资表</td
            ><td>既有独立治理入口；本区不创建发布或六表导出</td></tr
          ></tbody
        ></table
      ></div
    >
  </section>
</template>
<script setup lang="ts">
import { demo, result } from './demo'
import {
  rptDimensions,
  allocateReport,
  reportMetrics,
  rptBudget,
  compareReportBudget,
  rptReferences,
  type RptDimension
} from './rpt-model'
defineProps<{ canCreate: boolean }>()
const emit = defineEmits<{ opinion: [] }>()
const dimension = ref<RptDimension>('PROJECT')
const allocationCase = ref<'NORMAL' | 'DUPLICATE' | 'INCOMPLETE'>('NORMAL')
const denominatorCase = ref<'NORMAL' | 'MISSING_COUNT' | 'MISSING_REVENUE' | 'ZERO'>('NORMAL')
const budgetCase = ref<'NORMAL' | 'WRONG_PERIOD' | 'WRONG_VERSION' | 'MISSING_VERSION'>('NORMAL')
const inputRows = computed(() => {
  const rows = rptDimensions[dimension.value].rows.map((r) => ({ ...r }))
  if (allocationCase.value === 'DUPLICATE') rows.push({ ...rows[0] })
  if (allocationCase.value === 'INCOMPLETE') rows[0].percent -= 10
  return rows.map((row, index) => ({ ...row, inputId: index }))
})
const allocation = computed(() => allocateReport(inputRows.value))
const selectedKey = ref('')
const drillNotice = ref('选择分配行查看同一员工明细；跨维度是替代视图。')
const detail = computed(() =>
  allocation.value.valid
    ? allocation.value.rows.find((r) => r.key === selectedKey.value)
    : undefined
)
watch(
  [dimension, allocationCase],
  () => {
    selectedKey.value = ''
    drillNotice.value = '维度或分摊规则已切换，请重新选择当前分配行。'
  },
  { flush: 'sync' }
)
function drill(key: string) {
  if (!allocation.value.valid) {
    drillNotice.value = '分摊规则已阻断，不能展示有效分摊明细。'
    return
  }
  selectedKey.value = key
  drillNotice.value = '已展开同一TEST员工的当前分配行；全量成本只作参照，不与分摊重复相加。'
}
const headcount = computed(() =>
  denominatorCase.value === 'MISSING_COUNT' ? null : denominatorCase.value === 'ZERO' ? 0 : 1
)
const revenue = computed(() =>
  denominatorCase.value === 'MISSING_REVENUE'
    ? null
    : denominatorCase.value === 'ZERO'
      ? 0
      : demo.revenue
)
const metrics = computed(() => reportMetrics(headcount.value, revenue.value))
const budget = computed(() => ({
  ...rptBudget,
  period: budgetCase.value === 'WRONG_PERIOD' ? '2026-09' : rptBudget.period,
  version:
    budgetCase.value === 'MISSING_VERSION'
      ? null
      : budgetCase.value === 'WRONG_VERSION'
        ? 'TEST-BUDGET-V2'
        : rptBudget.version
}))
const comparison = computed(() => compareReportBudget(budget.value))
const money = (value: number | null) => (value === null ? '未知' : `${value.toFixed(2)}元`)
</script>
<style scoped>
.rpt-scenario {
  min-width: 0;
}

.rpt-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 16px 0;
  align-items: flex-end;
}

.rpt-controls label,
.rpt-select {
  display: flex;
  flex-direction: column;
  width: 320px;
  max-width: 100%;
  gap: 8px;
}

.el-select {
  width: 100%;
}

.rpt-table {
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

.rpt-table:focus {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

@media (width <= 600px) {
  .rpt-controls label {
    width: 100%;
  }

  .rpt-controls .el-button {
    height: auto;
    max-width: 100%;
    min-height: 32px;
    margin-left: 0;
    white-space: normal;
  }
}
</style>
