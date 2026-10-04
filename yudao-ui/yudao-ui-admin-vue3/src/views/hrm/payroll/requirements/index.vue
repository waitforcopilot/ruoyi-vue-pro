<template>
  <RequirementCollectionPanel
    :module-code="moduleCode"
    :source-code="sourceCode"
    @module-change="changeModule"
    @source-change="changeSource"
  />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { modules } from '@/api/hrm/payroll/requirements'
import RequirementCollectionPanel from './RequirementCollectionPanel.vue'
defineOptions({ name: 'HrmPayrollRequirements' })
const route = useRoute()
const router = useRouter()
const moduleCode = computed(() =>
  modules.some((m) => m.code === route.query.module) ? String(route.query.module) : ''
)
const sourceCode = computed(() => {
  const value = route.query.sourceCode
  return typeof value === 'string' && value.length <= 64 && /^DS-[A-Z0-9-]+$/.test(value)
    ? value
    : ''
})
const changeModule = (module: string) =>
  router.replace({ query: { ...route.query, module: module || undefined } })
const changeSource = (source: string) =>
  router.replace({ query: { ...route.query, module: undefined, sourceCode: source || undefined } })
</script>
