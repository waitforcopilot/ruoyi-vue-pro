<template>
  <div v-if="run?.result" data-testid="trial-run">
    <h3>试算 V{{ run.runVersion }} · {{ run.result.batch.title }}</h3>
    <p class="note"
      >{{ run.result.batch.entityName }} · {{ run.result.batch.periodStart }} 至
      {{ run.result.batch.periodEnd }} · {{ run.executedByName }} · {{ run.executedAt }}</p
    >
    <div class="totals"
      ><div v-for="(label, role) in Api.roleLabels" :key="role"
        ><small>{{ label }}合计 · CNY</small><strong>{{ run.result.totals[role] }}</strong></div
      ></div
    >
    <p
      >纳入 {{ run.includedCount }} 人 · 排除 {{ run.excludedCount }} 人 · 规则
      {{ run.result.definition.code }} V{{ run.result.definition.definitionVersion }}</p
    >
    <el-table :data="run.result.people" border data-testid="trial-run-people">
      <el-table-column label="人员快照" min-width="210"
        ><template #default="{ row }"
          >{{ row.input.snapshotName
          }}<small
            >HRM #{{ row.input.employeeId }} · 工号 {{ row.input.snapshotJobNumber }}</small
          ></template
        ></el-table-column
      >
      <el-table-column label="资格 / 版本" min-width="140"
        ><template #default="{ row }"
          ><el-tag :type="row.state === 'EXCLUDED' ? 'info' : 'success'"
            >{{ row.state === 'EXCLUDED' ? '排除计薪' : '纳入计薪' }} V{{
              row.eligibility?.eligibilityVersion
            }}</el-tag
          ></template
        ></el-table-column
      >
      <el-table-column
        v-for="(label, role) in Api.roleLabels"
        :key="role"
        :label="label + ' · CNY'"
        min-width="135"
        align="right"
        ><template #default="{ row }">{{ row.amounts[role] ?? '—' }}</template></el-table-column
      >
      <el-table-column label="对账" min-width="90"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="explain(row)">解释</el-button></template
        ></el-table-column
      >
    </el-table>
    <el-collapse class="mt-4"
      ><el-collapse-item title="批次口径与版本依据" name="source"
        ><p>负责人：{{ run.result.batch.ownerName }}</p
        ><p>批次依据：{{ run.result.batch.reference }}</p
        ><p>规则依据：{{ run.result.definition.reference }}</p
        ><p class="hash">规则指纹：{{ run.result.programHash }}</p
        ><p class="hash">资料指纹：{{ run.sourceHash }}</p></el-collapse-item
      ></el-collapse
    >
  </div>
  <Dialog v-model="visible" title="个人计算解释" width="min(1100px,96vw)">
    <div v-if="person" data-testid="trial-explanation">
      <h3>{{ person.input.snapshotName }} · HRM #{{ person.input.employeeId }}</h3>
      <p>金额输入依据：{{ person.input.inputReference || '排除计薪，不参与金额核算' }}</p
      ><p>资格依据：{{ person.eligibility?.reference }}</p
      ><p
        >资格结论：{{
          person.eligibility?.qualification === 'EXCLUDED' ? '排除计薪' : '纳入计薪'
        }}
        · {{ person.eligibility?.reason }}</p
      >
      <el-alert
        v-if="person.state === 'EXCLUDED'"
        title="此人员已明确排除，不生成工资金额，也不计入合计。"
        type="info"
        :closable="false"
      />
      <div v-else
        ><h4>输入金额</h4
        ><div class="input-values"
          ><p v-for="(value, key) in person.calculation?.inputs" :key="key"
            >{{ inputLabel(String(key)) }}<strong>{{ value }}</strong></p
          ></div
        >
        <div v-for="item in person.calculation?.items" :key="item.key" class="expression"
          ><h4>{{ item.label }} · {{ item.amount }} {{ item.unit }}</h4
          ><code>{{ item.key }} = {{ item.expression }}</code
          ><p>原始结果 {{ item.rawResult }} → {{ item.amountScale }} 位 / {{ item.roundingMode }}</p
          ><p v-for="(step, i) in item.steps" :key="i" class="note">{{ step }}</p></div
        >
      </div>
    </div>
  </Dialog>
</template>
<script setup lang="ts">
import * as Api from '@/api/hrm/payroll/trial'
const props = defineProps<{ run?: Api.Run }>()
type ResultPerson = NonNullable<NonNullable<Api.Run['result']>['people']>[number]
const visible = ref(false),
  person = ref<ResultPerson>()
const explain = (row: ResultPerson) => {
  person.value = row
  visible.value = true
}
const inputLabel = (key: string) =>
  props.run?.result?.definition.program?.inputs.find((f) => f.key === key)?.label || key
watch(
  () => props.run?.id,
  () => {
    visible.value = false
    person.value = undefined
  }
)
</script>
<style scoped>
.note,
small {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
small {
  display: block;
}
.totals {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin: 20px 0;
}
.totals > div {
  background: #f3f6fb;
  border: 1px solid #e1e7f0;
  border-radius: 8px;
  padding: 18px;
}
.totals strong {
  display: block;
  font-size: 24px;
  margin-top: 8px;
  color: #1e40af;
  font-variant-numeric: tabular-nums;
}
.hash {
  word-break: break-all;
  font-size: 12px;
}
.input-values {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.input-values p {
  background: #f6f8fc;
  margin: 0;
  padding: 12px;
  font-size: 13px;
}
.input-values strong {
  display: block;
  margin-top: 6px;
}
.expression {
  border-top: 1px solid #e2e8f0;
  margin-top: 18px;
  padding-top: 8px;
}
.expression code {
  word-break: break-word;
}
@media (max-width: 720px) {
  .totals,
  .input-values {
    grid-template-columns: 1fr;
  }
  .totals strong {
    font-size: 22px;
  }
}
</style>
