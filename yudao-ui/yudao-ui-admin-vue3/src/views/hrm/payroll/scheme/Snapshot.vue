<template>
  <div v-if="snapshot" class="snapshot">
    <el-alert
      v-for="(issue, i) in snapshot.issues"
      :key="i"
      :title="issue"
      type="warning"
      :closable="false"
      class="mb-2"
    />
    <h3>薪资组配置快照</h3
    ><el-descriptions :column="1" border>
      <el-descriptions-item label="薪资组"
        >{{ snapshot.group.name }} · #{{ snapshot.group.id }}</el-descriptions-item
      >
      <el-descriptions-item label="月计薪标准">{{
        missing(snapshot.group.salaryStandard)
      }}</el-descriptions-item>
      <el-descriptions-item label="薪资变更规则">{{
        missing(snapshot.group.changeRule)
      }}</el-descriptions-item>
    </el-descriptions>
    <h3>引用计税规则快照</h3
    ><el-descriptions v-if="snapshot.taxRule" :column="1" border>
      <el-descriptions-item label="规则名称"
        >{{ snapshot.taxRule.name }} · #{{ snapshot.taxRule.id }}</el-descriptions-item
      >
      <el-descriptions-item label="计税类型">{{
        taxTypes[snapshot.taxRule.type] || '未设置'
      }}</el-descriptions-item>
      <el-descriptions-item label="是否计税">{{
        flag(snapshot.taxRule.taxEnabled)
      }}</el-descriptions-item>
      <el-descriptions-item label="起征阈值 / 小数位数"
        >{{ missing(snapshot.taxRule.threshold) }} /
        {{ missing(snapshot.taxRule.decimalScale) }}</el-descriptions-item
      >
      <el-descriptions-item label="计税周期">{{
        snapshot.taxRule.cycleType ? cycles[snapshot.taxRule.cycleType] || '未识别' : '未设置'
      }}</el-descriptions-item> </el-descriptions
    ><el-empty v-else description="引用计税规则缺失，不能确认此方案" />
    <h3>租户共用薪资项目录快照</h3
    ><p class="note"
      >目录由当前租户共用，包含启用和停用项。此处保留项目定义，不包含个人金额或方案人员名单。</p
    >
    <el-table :data="snapshot.options" border max-height="500" data-testid="scheme-options">
      <el-table-column prop="code" label="编码" width="140" /><el-table-column
        prop="name"
        label="薪资项"
        min-width="170"
      />
      <el-table-column prop="parentCode" label="父编码" width="120" />
      <el-table-column label="类型" width="95"
        ><template #default="{ row }">{{
          optionTypes[row.type] || '未识别'
        }}</template></el-table-column
      >
      <el-table-column label="启用" width="85"
        ><template #default="{ row }">{{ flag(row.enabled) }}</template></el-table-column
      >
      <el-table-column label="计税" width="85"
        ><template #default="{ row }">{{ flag(row.taxEnabled) }}</template></el-table-column
      >
      <el-table-column label="参与计算" width="110"
        ><template #default="{ row }">{{ flag(row.calculateEnabled) }}</template></el-table-column
      >
      <el-table-column label="显示" width="85"
        ><template #default="{ row }">{{ flag(row.visible) }}</template></el-table-column
      >
      <el-table-column prop="remark" label="备注" min-width="170" /> </el-table
    ><el-empty v-if="!snapshot.options.length" description="没有薪资项定义" />
  </div>
</template>
<script setup lang="ts">
import type { Snapshot } from '@/api/hrm/payroll/scheme'
defineProps<{ snapshot?: Snapshot }>()
const missing = (v: unknown) => (v === null || v === undefined ? '未设置' : String(v))
const flag = (v: unknown) => (v === null || v === undefined ? '未设置' : v ? '是' : '否')
const optionTypes = ['减项', '加项', '计算项']
const taxTypes: Record<number, string> = { 1: '工资薪金所得税', 2: '劳务报酬所得税', 3: '不计税' }
const cycles: Record<number, string> = { 1: '上年 12 月至本年 11 月', 2: '本年 1 月至 12 月' }
</script>
<style scoped>
.note {
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}
h3 {
  margin: 20px 0 12px;
}
</style>
