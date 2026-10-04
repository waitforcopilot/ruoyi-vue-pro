<template>
  <div v-if="batch" class="result" data-testid="batch-result">
    <div class="result-heading"
      ><div
        ><h3>预检批次 #{{ batch.id }}</h3
        ><p>{{ batch.sourceCode }} · V{{ batch.contractVersion }} · {{ batch.fileName }}</p></div
      ><el-tag :type="batch.status === 0 ? 'success' : 'danger'">{{
        batch.status === 0 ? '格式预检通过' : '发现问题'
      }}</el-tag></div
    >
    <div class="stats"
      ><div
        ><span>数据行</span><strong>{{ batch.rowCount }}</strong></div
      ><div
        ><span>无问题行</span><strong>{{ batch.validCount }}</strong></div
      ><div
        ><span>问题数</span><strong>{{ batch.errorCount }}</strong></div
      ></div
    >
    <el-alert
      type="info"
      :closable="false"
      :title="
        batch.result.employeeMatchMode === 'EXTERNAL_MAPPING'
          ? '已按所保存的外部编号映射版本核对。计薪资格、来源完整性和薪酬规则仍需确认。'
          : batch.result.employeeMatchEnabled
            ? '已按当前 HRM 工号核对。期间人员资格、来源完整性和薪酬规则仍需确认。'
            : '人员映射未启用。格式预检通过后，仍需确认人员映射、来源完整性和薪酬规则。'
      "
    />
    <p class="context"
      >声明范围：{{ batch.declaredScope }} · {{ batch.periodStart }} 至 {{ batch.periodEnd }}</p
    >
    <el-alert
      v-for="(issue, index) in batch.result.globalIssues"
      :key="index"
      type="error"
      :closable="false"
      :title="issue.code + ' · ' + issue.message"
      class="mb-2"
    />
    <div class="result-filters"
      ><el-switch v-model="onlyIssues" active-text="只看问题行" /><span
        >缺失值显示“缺失”，数值零显示 0；无效值显示“无有效值”。</span
      ></div
    >
    <el-table :data="displayRows" border max-height="480">
      <el-table-column prop="line" label="文件行" width="80" fixed />
      <el-table-column
        v-for="field in batch.contractSnapshot.schema.fields"
        :key="field.key"
        :label="field.label + (field.unit ? '（' + field.unit + '）' : '')"
        min-width="130"
      >
        <template #default="{ row }"
          ><span :class="row.values[field.key] === null ? 'missing' : ''">{{
            cell(row, field.key)
          }}</span></template
        >
      </el-table-column>
      <el-table-column
        v-if="batch.result.employeeMatchMode === 'EXTERNAL_MAPPING'"
        label="人员映射版本"
        min-width="195"
        ><template #default="{ row }"
          ><span v-if="row.employeeMapping"
            >HRM #{{ row.employeeId }}<br />映射 #{{ row.employeeMapping.mappingId }} · V{{
              row.employeeMapping.mappingVersion
            }}</span
          ><span v-else>未匹配</span></template
        ></el-table-column
      >
      <el-table-column label="检查结果" min-width="300"
        ><template #default="{ row }"
          ><span v-if="!row.issues.length" class="passed">该行无问题</span
          ><div v-for="(issue, index) in row.issues" :key="index" class="issue"
            >{{ issue.field ? issue.field + '：' : '' }}{{ issue.message }}
            <small>({{ issue.code }})</small></div
          ></template
        ></el-table-column
      >
    </el-table>
    <el-empty
      v-if="!batch.result.rows.length"
      description="文件结构未通过检查，暂无可展示的数据行"
    />
    <p class="hash">文件 SHA-256：{{ batch.fileHash }}</p>
    <p class="hash"
      >本批次保留当时的契约与结果。相同内容、声明和映射结果返回原批次；外部映射版本或可访问的核对结果变化时生成新批次，旧结果不覆盖。</p
    >
  </div>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { BatchDetail, PreviewRow } from '@/api/hrm/payroll/intake'
const props = defineProps<{ batch?: BatchDetail }>()
const onlyIssues = ref(false)
watch(
  () => props.batch?.id,
  () => {
    onlyIssues.value = false
  }
)
const displayRows = computed(
  () => props.batch?.result.rows.filter((r) => !onlyIssues.value || r.issues.length) || []
)
const cell = (row: PreviewRow, key: string) =>
  row.values[key] === null ? '缺失' : row.values[key] === undefined ? '无有效值' : row.values[key]
</script>
<style scoped>
.result-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
h3 {
  margin: 10px 0;
}
p {
  line-height: 1.6;
}
.result-heading p,
.context {
  color: var(--el-text-color-secondary);
}
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin: 16px 0;
}
.stats > div {
  background: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 8px;
}
.stats span {
  display: block;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.stats strong {
  display: block;
  font-size: 24px;
  margin-top: 6px;
}
.result-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin: 16px 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.issue,
.missing {
  color: var(--el-color-danger);
}
.passed {
  color: var(--el-color-success);
}
.hash {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow-wrap: anywhere;
}
@media (max-width: 640px) {
  .result-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
