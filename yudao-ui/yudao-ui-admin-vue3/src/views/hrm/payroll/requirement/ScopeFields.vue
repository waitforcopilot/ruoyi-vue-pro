<template>
  <div>
    <el-form-item label="主体ID集合"
      ><el-input v-model="fields.entities" placeholder="主体ID，逗号分隔" @input="changed"
    /></el-form-item>
    <el-form-item label="组织ID集合"
      ><el-input v-model="fields.organizations" placeholder="组织ID，逗号分隔" @input="changed"
    /></el-form-item>
    <el-form-item label="人员类型"
      ><el-input v-model="fields.workerTypes" placeholder="人员类型，逗号分隔" @input="changed"
    /></el-form-item>
    <el-form-item label="业务场景"
      ><el-input v-model="fields.scenarios" placeholder="业务场景，逗号分隔" @input="changed"
    /></el-form-item>
    <el-form-item label="期间开始"
      ><el-input v-model="fields.from" placeholder="YYYY-MM-DD" @input="changed"
    /></el-form-item>
    <el-form-item label="期间结束"
      ><el-input v-model="fields.to" placeholder="YYYY-MM-DD" @input="changed"
    /></el-form-item>
    <el-form-item label="范围业务说明"
      ><el-input v-model="fields.explanation" type="textarea" @input="changed"
    /></el-form-item>
  </div>
</template>
<script setup lang="ts">
const props = defineProps<{ modelValue?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const fields = ref({
  entities: '',
  organizations: '',
  workerTypes: '',
  scenarios: '',
  from: '',
  to: '',
  explanation: ''
})
let lastEmitted: string | undefined
watch(
  () => props.modelValue,
  (value) => {
    if (value === lastEmitted) return
    const s = value ? JSON.parse(value) : {}
    fields.value = {
      entities: (s.entities || []).join(','),
      organizations: (s.organizations || []).join(','),
      workerTypes: (s.workerTypes || []).join(','),
      scenarios: (s.scenarios || []).join(','),
      from: Array.isArray(s.from)
        ? s.from.map((v, i) => (i ? String(v).padStart(2, '0') : v)).join('-')
        : s.from || '',
      to: Array.isArray(s.to)
        ? s.to.map((v, i) => (i ? String(v).padStart(2, '0') : v)).join('-')
        : s.to || '',
      explanation: s.explanation || ''
    }
  },
  { immediate: true }
)
const changed = () => {
  const split = (value: string) =>
    value
      .split(/[,，]/)
      .map((x) => x.trim())
      .filter(Boolean)
  lastEmitted = JSON.stringify({
    ...fields.value,
    entities: split(fields.value.entities).map(Number),
    organizations: split(fields.value.organizations).map(Number),
    workerTypes: split(fields.value.workerTypes),
    scenarios: split(fields.value.scenarios)
  })
  emit('update:modelValue', lastEmitted)
}
</script>
