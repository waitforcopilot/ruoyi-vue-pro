<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '维护规则草稿' : '补充业务规则'"
    width="min(960px, calc(100% - 24px))"
    :close-on-click-modal="false"
  >
    <el-alert
      title="请记录已核实的口径、参数和脱敏样例。未决事项可保存草稿；确认后通过新版本修改。"
      type="info"
      :closable="false"
    />
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
    <el-form label-position="top" class="mt-4">
      <div class="grid">
        <el-form-item label="规则编号 *"
          ><el-input
            v-model="form.code"
            :disabled="!!form.id"
            maxlength="64"
            placeholder="RULE-CUSTOM-001"
        /></el-form-item>
        <el-form-item label="规则名称 *"
          ><el-input v-model="form.title" maxlength="200"
        /></el-form-item>
        <el-form-item label="分类 *"
          ><el-select v-model="form.category" :disabled="!!form.id"
            ><el-option
              v-for="c in Api.categories"
              :key="c.code"
              :value="c.code"
              :label="c.label" /></el-select
        ></el-form-item>
        <el-form-item label="关联 PRD 问题（可选）"
          ><el-input v-model="form.questionCode" maxlength="4" placeholder="Q-11"
        /></el-form-item>
        <el-form-item label="规则负责人（确认必填）"
          ><el-input v-model="form.ownerName" maxlength="120"
        /></el-form-item>
        <el-form-item label="范围编号（确认必填）"
          ><el-input v-model="form.scopeCode" maxlength="64" placeholder="大写字母、数字及 _ -"
        /></el-form-item>
        <el-form-item label="生效日期（确认必填）"
          ><el-date-picker v-model="form.effectiveFrom" value-format="YYYY-MM-DD" type="date"
        /></el-form-item>
        <el-form-item label="截止日期（可空）"
          ><el-date-picker
            v-model="form.effectiveTo"
            value-format="YYYY-MM-DD"
            type="date"
            placeholder="留空表示持续有效"
        /></el-form-item>
      </div>
      <el-form-item label="适用范围（确认必填）"
        ><el-input
          v-model="form.applicableScope"
          type="textarea"
          :rows="2"
          maxlength="500"
          placeholder="说明主体、人员、城市或其他适用条件"
      /></el-form-item>
      <el-form-item label="完整规则口径（确认必填）"
        ><el-input
          v-model="form.definition"
          type="textarea"
          :rows="4"
          maxlength="4000"
          placeholder="记录计算方式、例外处理、先后顺序及未决项"
      /></el-form-item>
      <el-form-item label="制度 / 政策出处（确认必填）"
        ><el-input
          v-model="form.reference"
          type="textarea"
          :rows="2"
          maxlength="2000"
          placeholder="说明制度版本、适用日期和可核验出处"
      /></el-form-item>
      <div class="section-title"
        ><strong>口径参数 · {{ form.parameters.length }}/32</strong
        ><el-button :disabled="form.parameters.length >= 32" @click="addParameter"
          >添加参数</el-button
        ></div
      >
      <p class="note"
        >参数值以明确类型登记，数值须注明单位；小数须明确精度。零填 0，未确定的值留空保留草稿。</p
      >
      <div v-for="(p, i) in form.parameters" :key="i" class="card">
        <div class="section-title"
          ><span>参数 {{ i + 1 }}</span
          ><el-button link type="danger" @click="form.parameters.splice(i, 1)"
            >移除参数</el-button
          ></div
        >
        <div class="parameter-grid">
          <el-form-item label="参数标识 *"
            ><el-input v-model="p.key" maxlength="64" placeholder="ASCII 字母开头"
          /></el-form-item>
          <el-form-item label="业务名称 *"
            ><el-input v-model="p.label" maxlength="120"
          /></el-form-item>
          <el-form-item label="类型 *"
            ><el-select v-model="p.type"
              ><el-option
                v-for="t in types"
                :key="t.value"
                :label="t.label"
                :value="t.value" /></el-select
          ></el-form-item>
          <el-form-item label="值（确认必填）"
            ><el-select v-if="p.type === 'BOOLEAN'" v-model="p.value" clearable
              ><el-option label="是 / true" value="true" /><el-option
                label="否 / false"
                value="false" /></el-select
            ><el-input
              v-else
              v-model="p.value"
              maxlength="1024"
              :placeholder="p.type === 'DATE' ? 'YYYY-MM-DD' : '未确定可留空'"
          /></el-form-item>
          <el-form-item v-if="p.type === 'DECIMAL' || p.type === 'INTEGER'" label="单位（确认必填）"
            ><el-input v-model="p.unit" maxlength="40"
          /></el-form-item>
          <el-form-item v-if="p.type === 'DECIMAL'" label="小数位（确认必填）"
            ><el-input-number
              v-model="p.scale"
              :min="0"
              :max="8"
              :precision="0"
              controls-position="right"
          /></el-form-item>
        </div>
      </div>
      <div class="section-title"
        ><strong>业务样例 · {{ form.cases.length }}/20</strong
        ><el-button :disabled="form.cases.length >= 20" @click="addCase">添加样例</el-button></div
      >
      <p class="note"
        >确认前至少提供一个脱敏输入与预期结果。样例用于评审；本页不会计算或核验预期金额。</p
      >
      <div v-for="(sample, i) in form.cases" :key="i" class="card">
        <div class="section-title"
          ><span>样例 {{ i + 1 }}</span
          ><el-button link type="danger" @click="form.cases.splice(i, 1)">移除样例</el-button></div
        >
        <el-form-item label="样例名称 *"
          ><el-input v-model="sample.title" maxlength="120"
        /></el-form-item>
        <el-form-item label="脱敏输入 JSON 对象 *"
          ><el-input
            v-model="sample.inputJson"
            type="textarea"
            :rows="4"
            maxlength="4000"
            placeholder='例如 {"days":0}，请使用合成或脱敏数据'
        /></el-form-item>
        <el-form-item label="业务确认的预期结果 *"
          ><el-input v-model="sample.expectedResult" type="textarea" :rows="3" maxlength="2000"
        /></el-form-item>
      </div>
    </el-form>
    <template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template
    >
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import * as Api from '@/api/hrm/payroll/rules'
const props = defineProps<{ modelValue: boolean; rule?: Api.Rule }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [] }>()
const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })
const blank = (): Api.Rule => ({
  code: '',
  title: '',
  category: 'OTHER',
  parameters: [],
  cases: []
})
const form = ref<Api.Rule>(blank())
const error = ref('')
const saving = ref(false)
const types = [
  { value: 'TEXT', label: '文本' },
  { value: 'INTEGER', label: '整数' },
  { value: 'DECIMAL', label: '小数' },
  { value: 'BOOLEAN', label: '布尔' },
  { value: 'DATE', label: '日期' }
]
watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      form.value = props.rule ? JSON.parse(JSON.stringify(props.rule)) : blank()
      error.value = ''
    }
  }
)
const addParameter = () =>
  form.value.parameters.push({ key: '', label: '', type: 'TEXT', value: '' })
const addCase = () => form.value.cases.push({ title: '', inputJson: '{}', expectedResult: '' })
const save = async () => {
  error.value = ''
  if (!form.value.code.trim() || !form.value.title.trim()) {
    error.value = '请填写规则编号和名称。'
    return
  }
  const data = {
    ...form.value,
    scopeCode: form.value.scopeCode?.trim() || undefined,
    questionCode: form.value.questionCode?.trim() || undefined
  }
  saving.value = true
  try {
    if (data.id) await Api.update(data)
    else await Api.create(data)
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
.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}
.parameter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0 12px;
}
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 12px;
  margin: 12px 0;
}
.note {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.7;
}
:deep(.el-select),
:deep(.el-date-editor),
:deep(.el-input-number) {
  width: 100%;
}
@media (max-width: 640px) {
  .grid,
  .parameter-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
