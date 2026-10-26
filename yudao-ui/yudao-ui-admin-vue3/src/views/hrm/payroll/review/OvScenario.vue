<template>
  <section class="ov-scenario" tabindex="-1" aria-label="TEST总览期间与准备缺口">
    <h2>OV-01～04 · TEST期间、金额与准备总览</h2>
    <p
      >上方固定主例仅作参照；下方选择结果是虚构业务样例，不是实际工资统计。{{ overview.employee }} ·
      TEST-SUBJECT-001；切换不改变主链。</p
    >
    <div class="ov-controls">
      <label
        >样例期间<el-select v-model="period" aria-label="总览样例期间"
          ><el-option label="2026-10 主样例" value="2026-10" /><el-option
            label="2026-11 缺本期来源"
            value="2026-11" /></el-select
      ></label>
      <label
        >样例范围<el-select v-model="scope" aria-label="总览样例范围"
          ><el-option label="同一员工：正常主样例" value="MAIN" /><el-option
            label="同一员工：缺卡出差待批"
            value="MISSING_ATT" /></el-select
      ></label>
      <el-button :disabled="!canCreate" @click="emit('opinion')"
        >补充OV-04准备缺口与责任意见</el-button
      >
    </div>
    <h3>OV-01 · 当前选择金额</h3>
    <p role="status"
      >{{ period }} / {{ scope === 'MAIN' ? '正常范围' : '缺卡待批范围' }}；{{
        overview.measurable
          ? 'TEST应缴口径可测；实缴、发放与完税未核验'
          : '本期输入缺口，工资影响未知，不套0'
      }}。</p
    >
    <div class="ov-table" tabindex="0" aria-label="总览当前金额，可横向滚动"
      ><table
        ><thead
          ><tr
            ><th>应发</th><th>个人应缴扣款</th><th>TEST税</th><th>实发</th><th>单位成本</th></tr
          ></thead
        ><tbody
          ><tr
            ><td>{{ money(overview.amounts.gross) }}</td
            ><td>{{ money(overview.amounts.personal) }}</td
            ><td>{{ money(overview.amounts.tax) }}</td
            ><td>{{ money(overview.amounts.net) }}</td
            ><td>{{ money(overview.amounts.cost) }}</td></tr
          ></tbody
        ></table
      ></div
    >
    <h3>OV-02 · 同口径期间趋势</h3>
    <p
      >仅应发趋势；8/9月是独立虚构历史，没有净额/成本来源。当前选择点与上表一致；其他点标明参照，缺数据不画成0。</p
    >
    <div v-for="row in overview.trend" :key="row.period" class="trend-row"
      ><strong>{{ row.period }} · {{ money(row.gross) }} · {{ row.kind }}</strong
      ><div v-if="row.gross !== null" class="trend-track"
        ><div class="trend-bar" :style="{ width: `${(row.gross / 14000) * 100}%` }"></div></div
    ></div>
    <h3>OV-03 · TEST准备来源</h3>
    <p
      >派生自既有工时/考勤/加班/参保TEST计算模型。可测应缴不代表正式批准；实缴缺凭据不抹去已知应缴。加班上限提示与金额可测分别保留。</p
    >
    <div class="ov-table" tabindex="0" aria-label="总览准备与缺口，可横向滚动"
      ><table
        ><thead
          ><tr
            ><th>来源模块/对象</th><th>状态</th><th>责任角色</th><th>讨论期限</th><th>定位</th></tr
          ></thead
        ><tbody
          ><tr v-for="row in overview.readiness" :key="row.module"
            ><td>{{ row.module }} / {{ row.object }}</td
            ><td>{{ row.status }}</td
            ><td>{{ row.responsible }}</td
            ><td>{{ row.deadline }}</td
            ><td
              ><el-button link @click="emit('locate', row.module, row.anchor)"
                >定位{{ row.module }}依据</el-button
              ></td
            ></tr
          ></tbody
        ></table
      ></div
    >
    <h3>OV-04 · 异常与提醒讨论</h3>
    <p>以下是TEST缺口清单，未创建真实提醒或待办；责任与期限仅讨论样例，提示不代表问题关闭。</p>
    <ul
      ><li v-for="row in overview.gaps" :key="row.module"
        >{{ row.object }} · {{ row.responsible }} · {{ row.deadline }} · {{ row.status }}
        <el-button link @click="emit('locate', row.module, row.anchor)"
          >返回{{ row.module }}缺口</el-button
        ></li
      ></ul
    >
    <h3>真实需求统计（独立范围）</h3>
    <p role="status">{{ overviewRealState(realState) }}</p>
    <p
      >本区使用当前真实读取状态及服务端范围标签；不模拟无权、空结果或接口失败。逐需求对象、实际来源、质量与问题沿用下方“来源映射、质量与问题”入口。</p
    >
  </section>
</template>
<script setup lang="ts">
import {
  resolveOverview,
  overviewRealState,
  type OvPeriod,
  type OvScope,
  type OvRealState
} from './ov-model'
import type { PayrollModule } from '../requirements'
defineProps<{ canCreate: boolean; realState: OvRealState }>()
const emit = defineEmits<{ opinion: []; locate: [module: PayrollModule, anchor: string] }>()
const period = ref<OvPeriod>('2026-10')
const scope = ref<OvScope>('MAIN')
const overview = computed(() => resolveOverview(period.value, scope.value))
const money = (value: number | null) => (value === null ? '未知' : `${value.toFixed(2)}元`)
</script>
<style scoped>
.ov-scenario {
  min-width: 0;
}

.ov-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 16px 0;
  align-items: flex-end;
}

.ov-controls label {
  display: flex;
  flex-direction: column;
  width: 300px;
  max-width: 100%;
  gap: 8px;
}

.el-select {
  width: 100%;
}

.ov-table {
  width: 100%;
  margin: 12px 0;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 950px;
  border-collapse: collapse;
}

th,
td {
  padding: 10px;
  text-align: left;
  border: 1px solid var(--el-border-color);
}

p,
li,
strong {
  overflow-wrap: anywhere;
}

.trend-row {
  margin: 12px 0;
}

.trend-track {
  height: 10px;
  margin-top: 8px;
  background: var(--el-fill-color);
}

.trend-bar {
  height: 100%;
  background: var(--el-color-primary);
}

.ov-table:focus {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

@media (width <= 600px) {
  .ov-controls label {
    width: 100%;
  }

  .ov-controls .el-button {
    height: auto;
    max-width: 100%;
    min-height: 32px;
    margin-left: 0;
    white-space: normal;
  }
}
</style>
