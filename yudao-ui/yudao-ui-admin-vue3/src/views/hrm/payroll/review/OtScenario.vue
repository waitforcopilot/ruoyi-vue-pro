<template>
  <section
    id="operation-ot-scenario"
    class="ot-scenario"
    tabindex="-1"
    aria-label="TEST加班认可与补偿"
  >
    <h2>OT-01～05 · TEST认可、工资与调休</h2>
    <p
      >{{ demo.employee }} · TEST-SUBJECT-001 ·
      {{ demo.period }}。主管与财务讨论视角，以下切换不提交真实审批或核算。</p
    >
    <div class="ot-controls">
      <label
        >主管正常/异常场景<el-select v-model="scenario" aria-label="加班正常与例外场景"
          ><el-option
            v-for="(item, key) in otExamples"
            :key="key"
            :label="item.label"
            :value="key" /></el-select
      ></label>
      <el-button :disabled="!canCreate" @click="emit('opinion')">补充加班补偿与去重意见</el-button>
    </div>
    <p role="status">{{
      example.basis ?? '缺跨期归属、打卡和批准依据：保持未知，不套0、不产生补偿。'
    }}</p>
    <div class="ot-table" tabindex="0" aria-label="申请打卡认可前后，可横向滚动"
      ><table>
        <thead
          ><tr
            ><th>单号</th><th>申请</th><th>原始打卡</th><th>批准前认可</th><th>当前决定</th
            ><th>当前有效认可</th><th>工资预览</th></tr
          ></thead
        >
        <tbody
          ><tr
            ><td>{{ example.id }}</td
            ><td>{{ hours(example.requested) }}</td
            ><td>{{ hours(example.punched) }}</td
            ><td>未知（待批准）</td><td>{{ decisions[example.decision] }}</td
            ><td>{{ hours(resolved.hours) }}</td
            ><td>{{ money(resolved.wage) }}</td></tr
          ></tbody
        >
      </table></div
    >
    <p
      >认可不能超过申请或打卡；驳回保留原始打卡9小时，认可0、工资0。TEST单价{{
        result.hourly
      }}元/小时，倍率{{ example.multiplier ?? '未知' }}；批准主例8 × 62.5 × 1.5＝750元。</p
    >
    <h3>跨日/跨期分段与假期依据</h3>
    <div class="ot-table" tabindex="0" aria-label="加班分段及期间，可横向滚动"
      ><table
        ><thead
          ><tr><th>时间段（虚构）</th><th>归属期间</th><th>分段时长</th></tr></thead
        ><tbody
          ><tr v-for="slice in example.slices" :key="slice.interval"
            ><td>{{ slice.interval }}</td
            ><td
              >{{ slice.period
              }}{{ scenario === 'CROSS_PERIOD' ? '（待确认）' : '（TEST规则）' }}</td
            ><td>{{ hours(slice.hours) }}</td></tr
          ></tbody
        ></table
      ></div
    >
    <p
      >分段只用于核对整单，不把整单8小时再加一次；假期倍率仅TEST参数，不是法律结论，缺依据不默认标准规则。</p
    >
    <div id="operation-ot-limit" tabindex="-1" class="ot-limit">
      <h3>OT-05 · 可定位的上限提醒</h3>
      <p
        >TEST规则：月内既有认可30小时，上限36小时；本单后{{ hours(resolved.monthly) }}。{{
          resolved.overLimit
            ? '超出TEST上限，待TEST主管-001在2026-10-31前核对 TEST-OT-001；不自动改认可或金额。'
            : resolved.monthly === null
              ? '本单未知，上限待核对。'
              : '未超过TEST上限。'
        }}</p
      >
      <a href="#field-ot-05">定位OT-05上限候选讨论</a>
    </div>
    <h3>OT-03 / OT-04 · 财务补偿台账（本场景独立）</h3>
    <p
      >选择一个补偿方式后，再点同方式验证重复拦截，点另一方式验证互斥。切换场景清空本地TEST台账；主工资链不改。调休单位小时、工资单位元。</p
    >
    <div class="ot-controls">
      <el-button @click="compensate('WAGE')">登记TEST工资补偿</el-button>
      <el-button @click="compensate('LEAVE')">登记TEST调休补偿</el-button>
      <el-button @click="useLeave">使用TEST-LEAVE-001（4小时）</el-button>
    </div>
    <p role="status" aria-live="polite">{{ feedback }}</p>
    <div class="ot-table" tabindex="0" aria-label="工资与调休互斥台账，可横向滚动"
      ><table
        ><thead
          ><tr
            ><th>已登记单号/方式</th><th>工资一次性入账</th><th>调休授予</th><th>已使用</th
            ><th>剩余</th><th>使用单</th></tr
          ></thead
        ><tbody
          ><tr
            ><td
              >{{ ledger.orderId ?? '未登记' }} /
              {{
                ledger.mode === 'WAGE' ? '工资' : ledger.mode === 'LEAVE' ? '调休' : '未选择'
              }}</td
            ><td>{{ money(ledger.wage) }}</td
            ><td>{{ hours(ledger.granted) }}</td
            ><td>{{ hours(ledger.used) }}</td
            ><td>{{ hours(ledger.granted - ledger.used) }}</td
            ><td>{{ ledger.usageIds.join('、') || '无' }}</td></tr
          ></tbody
        ></table
      ></div
    >
    <p
      >固定主链：基本10000 ÷ 分母160＝62.5元/小时；加班750只入薪一次，应发{{
        result.gross
      }}、个人缴费{{ result.personal }}、税{{ result.tax }}、实发{{ result.net }}、成本{{
        result.cost
      }}。调休、驳回、假期和跨期是独立候选分支，不回写主链。</p
    >
    <p
      >专用意见复用真实评审保存与历史入口，返回OT-04；本地补偿台账刷新即重置，不能当正式审批、工资或调休余额。</p
    >
  </section>
</template>
<script setup lang="ts">
import { demo, result } from './demo'
import {
  otExamples,
  resolveOt,
  emptyOtLedger,
  compensateOt,
  useOtLeave,
  type OtExampleKey,
  type OtMode
} from './ot-model'
defineProps<{ canCreate: boolean }>()
const emit = defineEmits<{ opinion: [] }>()
const scenario = ref<OtExampleKey>('APPROVED')
const example = computed(() => otExamples[scenario.value])
const resolved = computed(() => resolveOt(example.value))
const ledger = ref(emptyOtLedger())
const feedback = ref('未登记补偿；批准主例可选工资750元或调休8小时。')
const decisions = { PENDING: '待批准', APPROVED: 'TEST批准', REJECTED: 'TEST驳回' }
const hours = (n: number | null) => (n === null ? '未知' : `${n}小时`)
const money = (n: number | null) => (n === null ? '未知（未生成金额）' : `${n.toFixed(2)}元`)
watch(scenario, () => {
  ledger.value = emptyOtLedger()
  feedback.value = '场景已切换，本地TEST台账已清空。'
})
function compensate(mode: OtMode) {
  const next = compensateOt(ledger.value, example.value, mode)
  ledger.value = next.state
  feedback.value = next.message
}
function useLeave() {
  const next = useOtLeave(ledger.value, 'TEST-LEAVE-001', 4)
  ledger.value = next.state
  feedback.value = next.message
}
</script>
<style scoped>
.ot-scenario {
  min-width: 0;
}

.ot-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
  margin: 16px 0;
}

.ot-controls label {
  display: flex;
  width: 420px;
  max-width: 100%;
  flex-direction: column;
  gap: 8px;
}

.ot-controls .el-select {
  width: 100%;
}

.ot-controls .el-button {
  margin-left: 0;
}

.ot-table {
  width: 100%;
  margin: 12px 0;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 850px;
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

.ot-scenario:focus,
.ot-limit:focus,
.ot-table:focus {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

@media (width <= 600px) {
  .ot-controls label {
    width: 100%;
  }

  .ot-controls .el-button {
    height: auto;
    max-width: 100%;
    min-height: 32px;
    white-space: normal;
  }
}
</style>
