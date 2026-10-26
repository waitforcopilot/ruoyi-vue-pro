<template>
  <section
    id="operation-tax-scenario"
    class="tax-scenario"
    tabindex="-1"
    aria-label="TEST累计个税与候选工资条"
  >
    <h2>TAX-01～05 · TEST累计来源、奖金与工资条候选</h2>
    <p
      >{{ demo.employee }} · {{ demo.period }} ·
      TEST-SUBJECT-001。上方TAX表格是固定主例；下方是独立候选，不回写工资、税额或实发。全部税率为虚构TEST讨论参数，不是现行税法。</p
    >
    <div class="tax-controls">
      <label
        >累计来源范围<el-select v-model="scope" aria-label="个税累计来源范围"
          ><el-option
            v-for="(label, key) in taxScopes"
            :key="key"
            :label="label"
            :value="key" /></el-select
      ></label>
      <label
        >工资条候选版本<el-select v-model="version" aria-label="工资条更正版本"
          ><el-option label="V1 原始TEST候选" value="V1" /><el-option
            label="V2 扣除更正TEST候选"
            value="V2" /></el-select
      ></label>
      <el-button :disabled="!canCreate" @click="emit('opinion')"
        >补充TAX-03工资条更正与投递意见</el-button
      >
    </div>
    <h3>TAX-01 / TAX-02 · 累计来源和扣除更正</h3>
    <p
      >{{ input.year }}年度 / {{ input.subject }}；来源：{{
        input.basis
      }}。其他扣除是TEST汇总字段，真实专项及其他扣除分类、凭据和批准仍待提供。</p
    >
    <div class="tax-table" tabindex="0" aria-label="累计所得扣除来源，可横向滚动"
      ><table>
        <thead
          ><tr
            ><th>前期收入</th><th>本期收入</th><th>前期个人缴费</th><th>本期个人缴费</th
            ><th>累计免税扣除</th><th>累计其他扣除</th><th>前期已扣税</th></tr
          ></thead
        >
        <tbody
          ><tr
            ><td>{{ money(input.priorIncome) }} / TEST-INCOME-09</td
            ><td>{{ money(input.currentIncome) }} / TEST-PAY-10</td
            ><td>{{ money(input.priorPersonal) }} / TEST-INS-09</td
            ><td>{{ money(input.currentPersonal) }} / TEST-INS-10</td
            ><td>{{ money(input.allowance) }} / TEST-ALLOW-10</td
            ><td
              >{{ money(input.otherDeduction) }} /
              {{ scope === 'MISSING_DEDUCTION' ? '缺来源' : 'TEST-DEDUCT-10' }}</td
            ><td>{{ money(input.priorTax) }} / TEST-TAX-09</td></tr
          ></tbody
        >
      </table></div
    >
    <p
      >固定主例公式：90000＋11750－12600－1400－50000－10000＝27750；×TEST
      3%＝832.50；－前期702＝本期130.50；11750－1400－130.50＝实发10219.50。</p
    >
    <p role="status"
      >当前候选：累计收入{{ money(tax.income) }}；累计个人缴费{{ money(tax.personal) }}；应税所得{{
        money(tax.taxable)
      }}；累计税{{ money(tax.cumulativeTax) }}；本期税{{ money(tax.tax) }}；净额{{
        money(tax.net)
      }}。{{ tax.reason }}</p
    >
    <h3>TAX-05 · 12000元奖金独立讨论</h3>
    <label class="bonus-select"
      >奖金TEST方案及依据<el-select v-model="bonusChoice" aria-label="奖金独立TEST方案"
        ><el-option label="未选择：税额未知" value="UNSELECTED" /><el-option
          v-for="(plan, key) in bonusPlans"
          :key="key"
          :label="plan.label"
          :value="key" /></el-select
    ></label>
    <p
      >独立奖金{{ money(bonus.amount) }}；税{{ money(bonus.tax) }}；奖金净额{{
        money(bonus.net)
      }}。{{ bonus.basis }}。未选择前保持未知；选择A/B不改变工资主链或候选工资条。</p
    >
    <h3>TAX-03 · 工资条更正前后、字段可见与撤回</h3>
    <p
      >V2仅将累计其他扣除10000更正为11000，依据TEST-DEDUCT-CORRECTION-001。本页每次切换累计范围或版本均新建未投递内存候选；撤回保留当前字段作对照，不能再投递。</p
    >
    <div class="tax-table" tabindex="0" aria-label="工资条更正和撤回前后，可横向滚动"
      ><table>
        <thead
          ><tr
            ><th>字段</th><th>更正前 V1</th><th>更正后 V2</th
            ><th>当前{{ version }} / {{ slip.withdrawn ? '撤回后（字段保留）' : '撤回前' }}</th
            ><th>字段可见规则（TEST）</th></tr
          ></thead
        >
        <tbody
          ><tr v-for="row in slipRows" :key="row.label"
            ><td>{{ row.label }}</td
            ><td>{{ row.before }}</td
            ><td>{{ row.after }}</td
            ><td>{{ row.current }}</td
            ><td>{{ row.visibility }}</td></tr
          ></tbody
        >
      </table></div
    >
    <div class="tax-actions">
      <el-button @click="act('FAIL')">模拟TEST投递失败</el-button>
      <el-button @click="act('QUEUE_RETRY')">标记TEST待重试</el-button>
      <el-button @click="act('SUCCESS')">模拟TEST投递成功</el-button>
      <el-button @click="act('WITHDRAW')">撤回当前TEST版本</el-button>
    </div>
    <p role="status"
      >{{ taxDeliveryLabels[slip.delivery] }} · 尝试{{ slip.attempts }}次；{{ slip.notice }}</p
    >
    <p
      >失败→待重试→成功均为纯内存标记，不生成后台任务，不调用工资条发布、邮件、短信、申报、付款或撤回接口。成功不代表真实送达；刷新或离开本模块丢弃演示状态。实际意见保存仍使用现有评审API。</p
    >
    <h3>TAX-04 · 税务/完税列预览</h3>
    <div class="tax-table" tabindex="0" aria-label="税务完税列预览，可横向滚动"
      ><table
        ><thead
          ><tr
            ><th>TEST年度/期间/主体</th><th>员工</th><th>所得/扣除</th><th>候选税额</th
            ><th>申报回执</th><th>完税凭据/实缴</th><th>状态</th></tr
          ></thead
        ><tbody
          ><tr
            ><td>{{ input.year }} / {{ demo.period }} / {{ input.subject }}</td
            ><td>{{ demo.employee }}</td
            ><td>{{ money(tax.income) }} / {{ money(input.otherDeduction) }}</td
            ><td>{{ money(tax.tax) }}</td
            ><td>未知：缺回执</td><td>未知：缺凭据，不套0</td
            ><td>仅列预览，非申报文件；未申报、未核验完税</td></tr
          ></tbody
        ></table
      ></div
    >
  </section>
</template>
<script setup lang="ts">
import { demo } from './demo'
import {
  taxScopes,
  taxInputs,
  resolveTax,
  bonusPlans,
  resolveBonus,
  createTaxSlip,
  transitionTaxSlip,
  taxDeliveryLabels,
  type TaxScopeKey,
  type TaxVersion,
  type BonusChoice,
  type TaxSlipAction
} from './tax-model'
defineProps<{ canCreate: boolean }>()
const emit = defineEmits<{ opinion: [] }>()
const scope = ref<TaxScopeKey>('VALID')
const version = ref<TaxVersion>('V1')
const bonusChoice = ref<BonusChoice>('UNSELECTED')
const slip = ref(createTaxSlip())
const input = computed(() => taxInputs(scope.value, version.value))
const tax = computed(() => resolveTax(input.value))
watch(
  [scope, version],
  () => {
    slip.value = createTaxSlip()
  },
  { flush: 'sync' }
)
const bonus = computed(() => resolveBonus(bonusChoice.value))
const money = (n: number | null) => (n === null ? '未知' : `${n.toFixed(2)}元`)
function act(action: TaxSlipAction) {
  slip.value = transitionTaxSlip(slip.value, tax.value.tax !== null, action)
}
const slipRows = computed(() => {
  const before = resolveTax(taxInputs(scope.value, 'V1'))
  const after = resolveTax(taxInputs(scope.value, 'V2'))
  return [
    {
      label: '员工/期间',
      before: `${demo.employee} / ${demo.period}`,
      after: `${demo.employee} / ${demo.period}`,
      current: `${demo.employee} / ${demo.period}`,
      visibility: '员工本人/授权财务讨论可见；不是生产权限实现'
    },
    ...(['gross', 'personal', 'deduction', 'tax', 'net'] as const).map((key) => {
      const labels = {
        gross: '本期应发',
        personal: '本期个人扣款',
        deduction: '累计其他扣除',
        tax: '本期个税',
        net: '本期实发'
      }
      const value = (v: TaxVersion, t: ReturnType<typeof resolveTax>) =>
        key === 'gross'
          ? taxInputs(scope.value, v).currentIncome
          : key === 'personal'
            ? taxInputs(scope.value, v).currentPersonal
            : key === 'deduction'
              ? taxInputs(scope.value, v).otherDeduction
              : t[key]
      return {
        label: labels[key],
        before: money(value('V1', before)),
        after: money(value('V2', after)),
        current: money(value(version.value, tax.value)),
        visibility: 'TEST员工工资条展示字段；累计依据仅授权财务讨论'
      }
    }),
    {
      label: '身份证/银行卡/税务密钥',
      before: '不提供',
      after: '不提供',
      current: '不提供',
      visibility: 'TEST预览不展示敏感字段；未载入真实资料'
    }
  ]
})
</script>
<style scoped>
.tax-scenario {
  min-width: 0;
}

.tax-controls,
.tax-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 16px 0;
  align-items: flex-end;
}

.tax-controls label,
.bonus-select {
  display: flex;
  flex-direction: column;
  width: 340px;
  max-width: 100%;
  gap: 8px;
}

.el-select {
  width: 100%;
}

.tax-table {
  width: 100%;
  margin: 12px 0;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 1050px;
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

.tax-table:focus,
.tax-scenario:focus {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

@media (width <= 600px) {
  .tax-controls label {
    width: 100%;
  }

  .tax-actions .el-button,
  .tax-controls .el-button {
    height: auto;
    max-width: 100%;
    min-height: 32px;
    margin-left: 0;
    white-space: normal;
  }
}
</style>
