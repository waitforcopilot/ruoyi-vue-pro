<template>
  <el-alert
    title="按冻结工资及原计薪日的历史部门、人员类别统计，环比对照上一计薪月。只纳入计入应发的工资项，扣减为负；原始考勤明细、个人代扣和单位成本不重复相加。"
    type="info"
    :closable="false"
  />
  <el-form inline class="mt-4"
    ><el-form-item label="历史部门"
      ><el-select v-model="deptId" clearable filterable style="width: 220px"
        ><el-option v-for="d in departments" :key="d.id" :value="d.id" :label="d.name" /></el-select
    ></el-form-item>
    <el-form-item label="人员类别"
      ><el-select
        v-model="category"
        clearable
        filterable
        allow-create
        default-first-option
        placeholder="全部历史人员类别"
        ><el-option
          v-for="item in categories"
          :key="item"
          :value="item"
          :label="item" /></el-select></el-form-item
    ><el-button :disabled="!entityId || !period" :loading="loading" @click="load"
      >查询结构</el-button
    ><el-button
      v-hasPermi="['hrm:payroll:analysis:export']"
      :disabled="!data?.ready"
      @click="exportRows"
      >导出结构及环比</el-button
    >
  </el-form>
  <el-alert
    v-if="data"
    :title="data.sourceStatus"
    :type="data.ready ? 'success' : 'warning'"
    :closable="false"
  />
  <el-descriptions v-if="data" class="my-4" :column="4" border
    ><el-descriptions-item label="本期应发（元）">{{ display(data.gross) }}</el-descriptions-item
    ><el-descriptions-item label="上期应发（元）">{{
      display(data.previousGross)
    }}</el-descriptions-item
    ><el-descriptions-item label="应发变化（元）">{{ display(data.delta) }}</el-descriptions-item
    ><el-descriptions-item label="变化率%">{{
      display(data.deltaRate)
    }}</el-descriptions-item></el-descriptions
  >
  <Echart
    v-if="data?.ready && data.rows.length"
    :options="chart"
    :height="Math.min(900, Math.max(300, data.rows.length * 40))"
  />
  <el-table :data="data?.rows || []" v-loading="loading"
    ><el-table-column prop="code" label="工资项编码" width="130" /><el-table-column
      prop="name"
      label="本期工资项"
      min-width="160"
    /><el-table-column prop="previousName" label="上期名称" min-width="160" />
    <el-table-column v-for="c in columns" :key="c.key" :label="c.label" min-width="130"
      ><template #default="{ row }">{{ display(row[c.key]) }}</template></el-table-column
    ><el-table-column prop="sourceStatus" label="对比说明" min-width="250" show-overflow-tooltip />
  </el-table>
  <p v-if="data" class="mt-4"
    >本期核算来源：{{ data.runIds.join('、') || '暂无' }}；上期核算来源：{{
      data.previousRunIds.join('、') || '暂无'
    }}。缺失期间、零或负分母不生成占比/变化率。</p
  >
</template>
<script setup lang="ts">
import request from '@/config/axios'
import download from '@/utils/download'
import type { EChartsOption } from 'echarts'
const props = defineProps<{
  entityId?: number
  period: string
  departments: any[]
  categories: string[]
}>()
const deptId = ref<number>(),
  category = ref(''),
  loading = ref(false),
  data = ref<any>()
const columns = [
  { key: 'amount', label: '本期金额（元）' },
  { key: 'share', label: '占应发比例%' },
  { key: 'previous', label: '上期金额（元）' },
  { key: 'delta', label: '环比金额（元）' },
  { key: 'deltaRate', label: '变化率%' }
]
const display = (value: any) =>
  value == null ? '不可计算' : Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
const params = () => {
  const [year, month] = props.period.split('-').map(Number)
  return {
    entityId: props.entityId,
    year,
    month,
    deptId: deptId.value,
    employeeCategory: category.value.trim() || undefined
  }
}
async function load() {
  data.value = undefined
  if (!props.entityId || !props.period) return
  const selected = params()
  loading.value = true
  try {
    const result = await request.get({
      url: '/hrm/payroll/financial/wage-structure',
      params: selected
    })
    if (
      props.entityId === selected.entityId &&
      props.period === `${selected.year}-${String(selected.month).padStart(2, '0')}`
    )
      data.value = result
  } finally {
    loading.value = false
  }
}
const chart = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: data.value?.previousReady ? ['本期', '上期'] : ['本期'] },
  grid: { left: 180, right: 40, top: 50, bottom: 30 },
  xAxis: { type: 'value', name: '元' },
  yAxis: { type: 'category', data: data.value?.rows?.map((row) => row.name) || [] },
  series: [
    { type: 'bar', name: '本期', data: data.value?.rows?.map((row) => Number(row.amount)) || [] },
    ...(data.value?.previousReady
      ? [
          {
            type: 'bar' as const,
            name: '上期',
            data: data.value.rows.map((row) => Number(row.previous))
          }
        ]
      : [])
  ]
}))
async function exportRows() {
  const blob = await request.download({
    url: '/hrm/payroll/financial/wage-structure-export',
    params: params()
  })
  download.excel(blob, '冻结工资结构及环比.xlsx')
}
watch(
  () => [props.entityId, props.period],
  () => {
    deptId.value = undefined
    category.value = ''
    load()
  },
  { immediate: true }
)
</script>
