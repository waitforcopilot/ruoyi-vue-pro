<template>
  <section class="scheme-sources" data-testid="trial-scheme-editor">
    <h3>方案版本与输入来源</h3>
    <el-alert
      v-if="!allowed"
      title="当前身份未获方案及全部源配置查询权限，可继续登记独立输入批次。"
      type="info"
      :closable="false"
    />
    <template v-else>
      <p
        >选用已确认方案后，为每个规则输入明确工资项关联或独立录入来源。方案目录不提供个人金额；录入后仍须完成批次复核。</p
      >
      <el-select
        :model-value="schemeId"
        filterable
        remote
        :remote-method="search"
        :loading="loading"
        :clearable="!locked"
        aria-label="试算方案版本"
        placeholder="选择方案版本；留空为独立输入批次"
        @change="select"
      >
        <el-option
          v-for="row in choices"
          :key="row.id"
          :value="row.id!"
          :label="`${row.title} · ${row.groupName} V${row.schemeVersion}${row.status !== 1 ? '（已停用或未确认）' : ''}`"
        />
      </el-select>
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
      <el-alert
        v-if="scheme && scheme.status !== 1"
        title="所选方案已停用或未确认；请选择覆盖批次期间的已确认版本后保存。"
        type="warning"
        :closable="false"
        class="mt-3"
      />
      <p v-if="locked"
        >此批次已有关联方案，维护时可更换方案版本；已保存历史保留相同的资料权限要求。</p
      >
      <div v-if="schemeId && definition?.program" class="mt-4">
        <p v-if="scheme"
          >方案依据：{{ scheme.reference }} · {{ scheme.effectiveFrom }} 至
          {{ scheme.effectiveTo || '未指定结束' }}</p
        >
        <el-button @click="align">按当前规则整理来源字段</el-button>
        <el-alert
          v-if="extraKeys.length"
          :title="'旧规则来源字段仍保留：' + extraKeys.join('、') + '；请整理并重新核对。'"
          type="warning"
          :closable="false"
          class="mt-3"
        />
        <div class="source-grid">
          <article v-for="entry in sources || []" :key="entry.inputKey" class="source-card">
            <h4>{{ label(entry.inputKey) }} · {{ entry.inputKey }}</h4>
            <label
              >来源类型 *<el-select
                v-model="entry.sourceType"
                :aria-label="entry.inputKey + ' 来源类型'"
                placeholder="明确选择来源"
                @change="entry.optionId = undefined"
                ><el-option value="SCHEME_ITEM" label="方案工资项关联" /><el-option
                  value="MANUAL"
                  label="独立录入来源" /></el-select
            ></label>
            <label v-if="entry.sourceType === 'SCHEME_ITEM'"
              >方案快照中的工资项 *<el-select
                v-model="entry.optionId"
                filterable
                :aria-label="entry.inputKey + ' 工资项'"
                placeholder="选择启用项"
                ><el-option
                  v-for="option in enabledOptions"
                  :key="option.id"
                  :value="option.id"
                  :label="`${option.name} · #${option.id}`" /></el-select
            ></label>
            <label
              >声明单位 *<el-input
                v-model="entry.unit"
                maxlength="40"
                :aria-label="entry.inputKey + ' 来源单位'"
            /></label>
            <label
              >来源口径依据 *<el-input
                v-model="entry.reference"
                type="textarea"
                maxlength="2000"
                :aria-label="entry.inputKey + ' 来源依据'"
                placeholder="说明此项如何对应个人输入；不填入实际凭据或账号"
            /></label>
          </article>
        </div>
      </div>
    </template>
  </section>
</template>
<script setup lang="ts">
import * as Schemes from '@/api/hrm/payroll/scheme'
import type { Definition } from '@/api/hrm/payroll/calculation'
import type { SourceBinding } from '@/api/hrm/payroll/trial'
import { checkPermi } from '@/utils/permission'
import { ElMessageBox } from 'element-plus'
const props = defineProps<{
  schemeId?: number
  sources?: SourceBinding[]
  definition?: Definition
  locked: boolean
}>()
const emit = defineEmits<{
  (e: 'update:schemeId', value?: number): void
  (e: 'update:sources', value?: SourceBinding[]): void
}>()
const allowed = [
  'hrm:payroll:scheme:query',
  'hrm:salary:group:query',
  'hrm:salary:option:query',
  'hrm:salary:tax-rule:query'
].every((p) => checkPermi([p]))
const choices = ref<Schemes.Scheme[]>([]),
  scheme = ref<Schemes.Scheme>(),
  loading = ref(false),
  error = ref('')
let loadEpoch = 0,
  searchEpoch = 0
const enabledOptions = computed(
  () => scheme.value?.snapshot?.options.filter((o) => o.enabled) || []
)
const extraKeys = computed(() =>
  (props.sources || [])
    .map((s) => s.inputKey)
    .filter((k) => !props.definition?.program?.inputs.some((i) => i.key === k))
)
const label = (key: string) =>
  props.definition?.program?.inputs.find((i) => i.key === key)?.label || key
const alignSources = () => {
  const previous = new Map((props.sources || []).map((s) => [s.inputKey, s]))
  emit(
    'update:sources',
    (props.definition?.program?.inputs || []).map(
      (input) =>
        previous.get(input.key) || {
          inputKey: input.key,
          sourceType: '',
          unit: input.unit,
          reference: ''
        }
    )
  )
}
const align = async () => {
  try {
    await ElMessageBox.confirm(
      '保留同名来源，移除旧字段并增加未登记字段。请核对工资项、单位和来源依据。',
      '整理来源字段'
    )
  } catch {
    return
  }
  alignSources()
}
const search = async (value = '') => {
  if (!allowed) return
  const ticket = ++searchEpoch
  loading.value = true
  try {
    const result = await Schemes.page({ pageNo: 1, pageSize: 100, status: 1, search: value })
    if (ticket !== searchEpoch) return
    choices.value = result.list
    if (scheme.value && !choices.value.some((r) => r.id === scheme.value?.id))
      choices.value.unshift(scheme.value)
  } catch (e: any) {
    if (ticket === searchEpoch) error.value = e?.message || String(e)
  } finally {
    if (ticket === searchEpoch) loading.value = false
  }
}
const select = (id?: number) => {
  emit('update:schemeId', id || undefined)
  if (!id) emit('update:sources', undefined)
  else if (!props.sources?.length) alignSources()
}
watch(
  () => props.schemeId,
  async (id) => {
    const ticket = ++loadEpoch
    scheme.value = undefined
    error.value = ''
    if (!id || !allowed) return
    try {
      const result = await Schemes.get(id)
      if (ticket !== loadEpoch) return
      scheme.value = result
      if (!choices.value.some((r) => r.id === id)) choices.value.unshift(result)
    } catch (e: any) {
      if (ticket === loadEpoch) error.value = e?.message || String(e)
    }
  },
  { immediate: true }
)
watch(
  () => props.definition?.id,
  () => {
    if (props.schemeId && !props.sources?.length) alignSources()
  }
)
onMounted(() => search())
onBeforeUnmount(() => {
  loadEpoch++
  searchEpoch++
})
</script>
<style scoped>
.scheme-sources {
  margin-top: 24px;
  padding: 18px;
  background: #f8fafc;
  border: 1px solid #dce3ed;
  border-radius: 8px;
}
.scheme-sources p {
  font-size: 13px;
  line-height: 1.8;
  color: #475569;
  overflow-wrap: anywhere;
}
.scheme-sources h3,
.source-card h4 {
  margin-top: 0;
}
.source-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}
.source-card {
  padding: 16px;
  background: #fff;
  border: 1px solid #dce3ed;
  border-radius: 6px;
  min-width: 0;
}
.source-card label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 12px;
  font-size: 13px;
}
.el-select {
  width: 100%;
}
@media (max-width: 720px) {
  .source-grid {
    grid-template-columns: 1fr;
  }
  .scheme-sources {
    padding: 12px;
  }
}
</style>
