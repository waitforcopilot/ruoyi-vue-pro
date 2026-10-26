<template>
  <section
    id="operation-ins-scenario"
    class="ins-scenario"
    tabindex="-1"
    aria-label="TEST参保政策与缴费对账"
  >
    <h2>INS-01～05 · TEST政策、变更与应缴/实缴</h2>
    <p
      >{{ demo.employee }} · 主样例{{ demo.period }} ·
      TEST-SUBJECT-001。城市、比例、上下限、资格及折算口径都是虚构讨论参数，不是现行政策。</p
    >
    <div class="ins-controls">
      <label
        >HR/财务正常与例外场景<el-select v-model="scenario" aria-label="参保政策与对账场景"
          ><el-option
            v-for="(item, key) in insExamples"
            :key="key"
            :label="item.label"
            :value="key" /></el-select
      ></label>
      <el-button :disabled="!canCreate" @click="emit('opinion')"
        >补充政策有效期与实缴对账意见</el-button
      >
    </div>
    <p role="status">{{ example.explanation }}</p>
    <h3>INS-01 / INS-02 · 变更前后政策与资格依据</h3>
    <p
      >仅在快照期间内选择有效参保记录；同ID同内容去重，冲突或两城资格重叠阻断测算。有效期外明确已终止资格才按TEST口径计0，缺资格或政策依据保持未知。</p
    >
    <div class="ins-table" tabindex="0" aria-label="参保政策和变更前后，可横向滚动"
      ><table>
        <thead
          ><tr
            ><th>前后/期间/主体</th><th>记录/城市/政策</th><th>资格起止与依据</th
            ><th>政策有效期/依据</th><th>基数上下限</th><th>社保/公积金基数</th
            ><th>社保个人/单位比例</th><th>公积金个人/单位比例</th></tr
          ></thead
        >
        <tbody
          ><template v-for="snapshot in comparisons" :key="snapshot.label"
            ><tr v-for="row in snapshot.data.enrollments" :key="`${snapshot.label}-${row.id}`">
              <td
                >{{ snapshot.label }} / {{ snapshot.data.period }} / {{ snapshot.data.subject }}</td
              >
              <td>{{ row.id }} / {{ row.policy.city }} / {{ row.policy.id }}</td>
              <td
                >{{ row.from }}～{{ row.through }}；{{
                  row.eligible === null
                    ? '资格未知'
                    : row.eligible
                      ? 'TEST资格已确认'
                      : 'TEST不具资格'
                }}；{{ row.basis ?? '缺资格依据' }}</td
              >
              <td>{{ row.policy.from }}～{{ row.policy.through }}；{{ row.policy.basis }}</td>
              <td>{{ row.policy.minimum }}～{{ row.policy.maximum }}元（TEST）</td>
              <td>{{ row.insuranceBase }} / {{ row.fundBase }}元</td>
              <td
                >{{ percent(row.policy.personalInsurance) }} /
                {{ percent(row.policy.employerInsurance) }}</td
              >
              <td
                >{{ percent(row.policy.personalFund) }} / {{ percent(row.policy.employerFund) }}</td
              >
            </tr></template
          ></tbody
        >
      </table></div
    >
    <h3>INS-03 / INS-04 · 应缴个人与单位分开核对</h3>
    <div
      class="ins-table"
      tabindex="0"
      aria-label="个人扣款与单位成本前后，可横向滚动"
      aria-live="polite"
      ><table>
        <thead
          ><tr
            ><th>前后/期间</th><th>个人社保</th><th>个人公积金</th><th>个人合计（工资扣款）</th
            ><th>单位社保</th><th>单位公积金</th><th>单位合计（成本）</th><th>独立候选成本</th
            ><th>资格/政策校验</th></tr
          ></thead
        >
        <tbody
          ><tr v-for="snapshot in comparisons" :key="snapshot.label"
            ><td>{{ snapshot.label }} / {{ snapshot.data.period }}</td
            ><td>{{ money(snapshot.calculated.personalInsurance) }}</td
            ><td>{{ money(snapshot.calculated.personalFund) }}</td
            ><td>{{ money(snapshot.calculated.personal) }}</td
            ><td>{{ money(snapshot.calculated.employerInsurance) }}</td
            ><td>{{ money(snapshot.calculated.employerFund) }}</td
            ><td>{{ money(snapshot.calculated.employer) }}</td
            ><td>{{ money(snapshot.calculated.candidateCost) }}（主应发11750＋候选单位）</td
            ><td>{{ snapshot.calculated.reason }}</td></tr
          ></tbody
        >
      </table></div
    >
    <h3>INS-05 · 实缴、凭据与补缴/退款候选</h3>
    <p
      >差额＝应缴－实缴。正差待补缴，负差待退款；金额均为元。个人与单位分别核对，不能用总额抵消双方差额。填数没有凭据仍是未对账，不生成扣款、补缴或退款。</p
    >
    <div class="ins-table" tabindex="0" aria-label="应缴实缴差额与凭据，可横向滚动"
      ><table>
        <thead
          ><tr
            ><th>前后/期间</th><th>个人应缴/实缴</th><th>个人差额</th><th>单位应缴/实缴</th
            ><th>单位差额</th><th>缴款凭据</th><th>状态</th></tr
          ></thead
        >
        <tbody
          ><tr v-for="snapshot in comparisons" :key="snapshot.label"
            ><td>{{ snapshot.label }} / {{ snapshot.data.period }}</td
            ><td
              >{{ money(snapshot.calculated.personal) }} /
              {{ money(snapshot.data.paidPersonal) }}</td
            ><td>{{ insDeltaLabel(snapshot.calculated.personalDelta) }}</td
            ><td
              >{{ money(snapshot.calculated.employer) }} /
              {{ money(snapshot.data.paidEmployer) }}</td
            ><td>{{ insDeltaLabel(snapshot.calculated.employerDelta) }}</td
            ><td>{{ snapshot.data.receipt ?? '缺凭据' }}</td
            ><td>{{ snapshot.calculated.status }}</td></tr
          ></tbody
        >
      </table></div
    >
    <p
      >固定主链始终保持基本{{ demo.base }}、个人社保{{ demo.personalInsurance }}＋公积金{{
        demo.personalFund
      }}＝{{ result.personal }}；单位社保{{ demo.employerInsurance }}＋公积金{{
        demo.employerFund
      }}＝{{ result.employer }}；应发{{ result.gross }}、税{{ result.tax }}、实发{{
        result.net
      }}、成本{{ result.cost }}。前后候选仅供讨论，不改变本链。</p
    >
    <p
      >本场景不调用参保、调基、缴款或付款接口。专用意见复用真实评审保存/历史入口，返回INS-05；真实政策及凭据需业务方提供DS-01/DS-06依据。</p
    >
  </section>
</template>
<script setup lang="ts">
import { demo, result } from './demo'
import { insExamples, calculateIns, insDeltaLabel, type InsExampleKey } from './ins-model'
defineProps<{ canCreate: boolean }>()
const emit = defineEmits<{ opinion: [] }>()
const scenario = ref<InsExampleKey>('MAIN')
const example = computed(() => insExamples[scenario.value])
const comparisons = computed(() => [
  { label: '变更前', data: example.value.before, calculated: calculateIns(example.value.before) },
  { label: '当前候选', data: example.value.after, calculated: calculateIns(example.value.after) }
])
const percent = (n: number) => `${Math.round(n * 10000) / 100}%`
const money = (n: number | null) => (n === null ? '未知' : `${n.toFixed(2)}元`)
</script>
<style scoped>
.ins-scenario {
  min-width: 0;
}

.ins-controls {
  display: flex;
  margin: 16px 0;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.ins-controls label {
  display: flex;
  width: 420px;
  max-width: 100%;
  flex-direction: column;
  gap: 8px;
}

.ins-controls .el-select {
  width: 100%;
}

.ins-table {
  width: 100%;
  margin: 12px 0;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 1100px;
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

.ins-scenario:focus,
.ins-table:focus {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

@media (width <= 600px) {
  .ins-controls label {
    width: 100%;
  }

  .ins-controls .el-button {
    height: auto;
    max-width: 100%;
    min-height: 32px;
    margin-left: 0;
    white-space: normal;
  }
}
</style>
