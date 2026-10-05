<template>
  <div v-if="config" class="parameters">
    <el-descriptions :column="1" border data-testid="insurance-parameters">
      <el-descriptions-item label="基数下限 / 上限（元）"
        >{{ missing(config.lowerBase) }} / {{ missing(config.upperBase) }}</el-descriptions-item
      >
      <el-descriptions-item label="基数单位及周期">{{
        named(config.baseUnit, Api.units)
      }}</el-descriptions-item>
      <el-descriptions-item label="单位缴费方式">{{
        named(config.corporateMode, Api.modes)
      }}</el-descriptions-item>
      <el-descriptions-item label="单位比例（%） / 固定额（元）"
        >{{ missing(config.corporateRatePercent) }} /
        {{ missing(config.corporateFixedAmount) }}</el-descriptions-item
      >
      <el-descriptions-item label="个人缴费方式">{{
        named(config.personalMode, Api.modes)
      }}</el-descriptions-item>
      <el-descriptions-item label="个人比例（%） / 固定额（元）"
        >{{ missing(config.personalRatePercent) }} /
        {{ missing(config.personalFixedAmount) }}</el-descriptions-item
      >
      <el-descriptions-item label="金额小数位数">{{
        missing(config.amountScale)
      }}</el-descriptions-item>
      <el-descriptions-item label="舍入方式">{{
        named(config.roundingMode, Api.roundings)
      }}</el-descriptions-item>
      <el-descriptions-item label="舍入步骤">{{
        named(config.roundingStage, Api.stages)
      }}</el-descriptions-item>
    </el-descriptions>
  </div>
</template>
<script setup lang="ts">
import * as Api from '@/api/hrm/payroll/insurance'
defineProps<{ config?: Api.Config }>()
const missing = (v: unknown) => (v === null || v === undefined ? '未设置' : String(v))
const named = (v: string | null | undefined, names: Record<string, string>) =>
  v ? names[v] || v : '未设置'
</script>
