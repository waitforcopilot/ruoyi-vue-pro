<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '维护契约草稿' : '新建契约版本'"
    width="min(960px, calc(100% - 24px))"
    :close-on-click-modal="false"
  >
    <el-alert
      title="确认前请核实字段、单位和数据范围；确认后通过新版本修改。"
      type="info"
      :closable="false"
    />
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
    <el-form label-position="top" class="mt-4">
      <div class="meta-grid">
        <el-form-item label="数据来源 *">
          <el-select
            v-model="form.sourceId"
            :disabled="!!form.id"
            filterable
            placeholder="选择已登记来源"
          >
            <el-option
              v-for="s in sources"
              :key="s.id"
              :value="s.id"
              :label="s.code + ' · ' + s.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="契约名称 *"
          ><el-input v-model="form.title" maxlength="200"
        /></el-form-item>
        <el-form-item label="实际系统（确认必填）"
          ><el-input v-model="form.actualSystem" maxlength="200"
        /></el-form-item>
        <el-form-item label="来源负责人（确认必填）"
          ><el-input v-model="form.ownerName" maxlength="120"
        /></el-form-item>
      </div>
      <el-form-item label="适用范围（确认必填）"
        ><el-input v-model="form.applicableScope" maxlength="500" type="textarea" :rows="2"
      /></el-form-item>
      <div class="section-title"
        ><strong>字段清单 · {{ form.schema.fields.length }}/32</strong
        ><el-button :disabled="form.schema.fields.length >= 32" @click="addField"
          >添加字段</el-button
        ></div
      >
      <div v-for="(field, index) in form.schema.fields" :key="index" class="field-card">
        <div class="field-title"
          ><span>字段 {{ index + 1 }}</span
          ><el-button
            link
            type="danger"
            :disabled="form.schema.fields.length <= 1"
            @click="removeField(index)"
            >移除字段</el-button
          ></div
        >
        <div class="field-grid">
          <el-form-item label="列名 *"
            ><el-input v-model="field.key" maxlength="64" placeholder="ASCII 字母开头"
          /></el-form-item>
          <el-form-item label="业务名称 *"
            ><el-input v-model="field.label" maxlength="120"
          /></el-form-item>
          <el-form-item label="类型 *"
            ><el-select v-model="field.type"
              ><el-option
                v-for="t in types"
                :key="t.value"
                :label="t.label"
                :value="t.value" /></el-select
          ></el-form-item>
          <el-form-item label="必填"><el-switch v-model="field.required" /></el-form-item>
          <el-form-item v-if="field.type === 'TEXT'" label="最大长度（确认必填）"
            ><el-input-number
              v-model="field.maxLength"
              :min="1"
              :max="1024"
              :precision="0"
              controls-position="right"
          /></el-form-item>
          <el-form-item
            v-if="field.type === 'INTEGER' || field.type === 'DECIMAL'"
            label="单位（确认必填）"
            ><el-input v-model="field.unit" maxlength="40" placeholder="由业务核实"
          /></el-form-item>
          <el-form-item v-if="field.type === 'DECIMAL'" label="小数位（确认必填）"
            ><el-input-number
              v-model="field.scale"
              :min="0"
              :max="8"
              :precision="0"
              controls-position="right"
          /></el-form-item>
        </div>
      </div>
      <el-form-item label="复合唯一键 *（1–8 个必填字段）"
        ><el-select v-model="form.schema.keyFields" multiple :multiple-limit="8"
          ><el-option
            v-for="f in requiredFields"
            :key="f.key"
            :value="f.key"
            :label="f.key + ' · ' + f.label" /></el-select
      ></el-form-item>
      <div class="meta-grid">
        <el-form-item label="期间校验字段（可选）"
          ><el-select v-model="form.schema.periodField" clearable placeholder="不启用期间列核对"
            ><el-option
              v-for="f in dateFields"
              :key="f.key"
              :value="f.key"
              :label="f.label + ' · ' + f.key" /></el-select
        ></el-form-item>
        <el-form-item label="主体校验字段（可选）"
          ><el-select v-model="form.schema.subjectField" clearable placeholder="不启用主体列核对"
            ><el-option
              v-for="f in textFields"
              :key="f.key"
              :value="f.key"
              :label="f.label + ' · ' + f.key" /></el-select
        ></el-form-item>
        <el-form-item label="HRM 工号字段（可选）"
          ><el-select v-model="form.schema.employeeField" clearable placeholder="人员映射待核定"
            ><el-option
              v-for="f in textFields"
              :key="f.key"
              :value="f.key"
              :label="f.label + ' · ' + f.key" /></el-select
        ></el-form-item>
      </div>
      <p class="note"
        >工号核对按当前租户 HRM 主档匹配，需人员查询权限。员工适用资格和历史人员映射需另行核定。</p
      >
      <el-form-item label="草稿备注"
        ><el-input v-model="form.evidence" type="textarea" :rows="2" maxlength="2000"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template
    >
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import * as Api from '@/api/hrm/payroll/intake'
const props = defineProps<{
  modelValue: boolean
  contract?: Api.Contract
  sources: Api.SourceOption[]
}>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [] }>()
const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })
const blankField = (): Api.ContractField => ({ key: '', label: '', type: 'TEXT', required: true })
const blank = (): Api.Contract => ({ title: '', schema: { fields: [blankField()], keyFields: [] } })
const form = ref<Api.Contract>(blank())
const error = ref('')
const saving = ref(false)
const types = [
  { value: 'TEXT', label: '文本' },
  { value: 'INTEGER', label: '整数' },
  { value: 'DECIMAL', label: '小数' },
  { value: 'DATE', label: '日期' }
]
const requiredFields = computed(() => form.value.schema.fields.filter((f) => f.required && f.key))
const textFields = computed(() => requiredFields.value.filter((f) => f.type === 'TEXT'))
const dateFields = computed(() => requiredFields.value.filter((f) => f.type === 'DATE'))
watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      form.value = props.contract ? JSON.parse(JSON.stringify(props.contract)) : blank()
      error.value = ''
    }
  }
)
const addField = () => form.value.schema.fields.push(blankField())
const removeField = (index: number) => {
  const key = form.value.schema.fields[index].key
  form.value.schema.fields.splice(index, 1)
  form.value.schema.keyFields = form.value.schema.keyFields.filter((k) => k !== key)
  for (const name of ['periodField', 'subjectField', 'employeeField'] as const)
    if (form.value.schema[name] === key) form.value.schema[name] = undefined
}
const save = async () => {
  error.value = ''
  if (!form.value.sourceId || !form.value.title.trim() || !form.value.schema.keyFields.length) {
    error.value = '请填写来源、契约名称和复合唯一键。'
    return
  }
  saving.value = true
  try {
    if (form.value.id) await Api.updateContract(form.value)
    else await Api.createContract(form.value)
    visible.value = false
    emit('saved')
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    saving.value = false
  }
}
</script>
<style scoped>
.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}
.field-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0 12px;
}
.field-card {
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  margin: 12px 0;
}
.section-title,
.field-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.field-title {
  margin-bottom: 8px;
  color: var(--el-text-color-secondary);
}
.note {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}
:deep(.el-select),
:deep(.el-input-number) {
  width: 100%;
}
@media (max-width: 640px) {
  .meta-grid,
  .field-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
