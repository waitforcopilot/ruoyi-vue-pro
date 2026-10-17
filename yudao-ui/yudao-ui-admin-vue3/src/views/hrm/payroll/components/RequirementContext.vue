<template>
  <el-space class="mb-4" wrap>
    <el-button
      v-for="code in selected"
      :key="code"
      v-hasPermi="['hrm:payroll:requirement:query']"
      type="primary"
      link
      @click="open(code)"
    >
      本模块需求（{{ payrollModules[code] }}）
    </el-button>
    <el-popover
      trigger="click"
      placement="bottom-start"
      :width="Math.min(480, Math.max(240, width - 32))"
    >
      <template #reference
        ><el-button link>候选数据源（{{ sources.length }}）</el-button></template
      >
      <div class="max-h-80 overflow-y-auto">
        <div v-for="source in sources" :key="source.id" class="mb-3">
          <strong>{{ source.id }} · {{ source.name }}</strong>
          <div>{{ source.fields.join('、') }}</div>
          <div class="text-gray-500">候选确认方：{{ source.owner }}</div>
        </div>
        <div>实际系统、字段对应和负责人需在需求评审中确认。</div>
      </div>
    </el-popover>
  </el-space>
</template>
<script setup lang="ts">
import { useWindowSize } from '@vueuse/core'
import { payrollModules, sourcesForModules, type PayrollModule } from '../requirements'
const props = defineProps<{ module: PayrollModule | PayrollModule[] }>()
const route = useRoute()
const router = useRouter()
const { width } = useWindowSize()
const selected = computed(() => (Array.isArray(props.module) ? props.module : [props.module]))
const sources = computed(() => sourcesForModules(selected.value))
const open = (module: PayrollModule) =>
  router.push({ path: '/hrm-payroll/requirement', query: { module, from: route.fullPath } })
</script>
