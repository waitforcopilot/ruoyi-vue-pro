<template>
  <Dialog
    v-model="visible"
    :title="form.id ? '编辑计算草稿' : '登记计算草稿'"
    width="min(1180px, 96vw)"
  >
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-form label-position="top" class="metadata">
      <el-form-item label="规则编号"
        ><el-input v-model="form.code" :disabled="!!form.id" maxlength="64" aria-label="规则编号"
      /></el-form-item>
      <el-form-item label="规则名称"
        ><el-input v-model="form.title" maxlength="160" aria-label="规则名称"
      /></el-form-item>
      <el-form-item label="范围编号"
        ><el-input
          v-model="form.scopeCode"
          :disabled="!!form.id"
          maxlength="64"
          aria-label="范围编号"
      /></el-form-item>
      <el-form-item label="负责人"
        ><el-input v-model="form.ownerName" maxlength="120" aria-label="规则负责人"
      /></el-form-item>
      <el-form-item label="开始日期"
        ><el-date-picker
          v-model="form.effectiveFrom"
          type="date"
          value-format="YYYY-MM-DD"
          aria-label="规则开始日期"
      /></el-form-item>
      <el-form-item label="结束日期"
        ><el-date-picker
          v-model="form.effectiveTo"
          type="date"
          value-format="YYYY-MM-DD"
          aria-label="规则结束日期"
      /></el-form-item>
      <el-form-item label="适用范围说明" class="wide"
        ><el-input v-model="form.applicableScope" maxlength="500" aria-label="规则适用范围"
      /></el-form-item>
      <el-form-item label="规则依据" class="wide"
        ><el-input v-model="form.reference" type="textarea" maxlength="2000" aria-label="规则依据"
      /></el-form-item>
      <el-form-item label="说明" class="wide"
        ><el-input
          v-model="form.description"
          type="textarea"
          maxlength="4000"
          aria-label="规则说明"
      /></el-form-item>
    </el-form>
    <el-tabs v-model="tab">
      <el-tab-pane label="输入与表达式" name="program">
        <div class="section-title"
          ><h3>声明输入</h3
          ><el-button :disabled="program.inputs.length >= 32" @click="addInput"
            >增加输入</el-button
          ></div
        >
        <p class="note">编号供表达式引用；单位和精度须明确声明。整数输入选择 0 位小数。</p>
        <el-table :data="program.inputs" border data-testid="calculation-editor-inputs">
          <el-table-column label="编号" min-width="150"
            ><template #default="{ row }"><el-input v-model="row.key" maxlength="64" /></template
          ></el-table-column>
          <el-table-column label="名称" min-width="150"
            ><template #default="{ row }"><el-input v-model="row.label" maxlength="120" /></template
          ></el-table-column>
          <el-table-column label="类型" width="140"
            ><template #default="{ row }"
              ><el-select v-model="row.type" placeholder="明确选择"
                ><el-option label="十进制" value="DECIMAL" /><el-option
                  label="整数"
                  value="INTEGER" /></el-select></template
          ></el-table-column>
          <el-table-column label="单位" min-width="150"
            ><template #default="{ row }"><el-input v-model="row.unit" maxlength="40" /></template
          ></el-table-column>
          <el-table-column label="小数位" width="115"
            ><template #default="{ row }"
              ><el-select v-model="row.scale" placeholder="选择"
                ><el-option
                  v-for="n in scales"
                  :key="n"
                  :value="n"
                  :label="String(n)" /></el-select></template
          ></el-table-column>
          <el-table-column width="70"
            ><template #default="{ $index }"
              ><el-button link type="danger" @click="program.inputs.splice($index, 1)"
                >移除</el-button
              ></template
            ></el-table-column
          >
        </el-table>
        <div class="section-title"
          ><h3>计算项目</h3
          ><el-button :disabled="program.items.length >= 32" @click="addItem"
            >增加项目</el-button
          ></div
        >
        <p class="note"
          >支持 + − × ÷、括号、abs/min/max。项目按依赖执行；后项引用前项已舍入的结果。</p
        >
        <el-table :data="program.items" border data-testid="calculation-editor-items">
          <el-table-column label="编号" min-width="140"
            ><template #default="{ row }"><el-input v-model="row.key" maxlength="64" /></template
          ></el-table-column>
          <el-table-column label="名称" min-width="140"
            ><template #default="{ row }"><el-input v-model="row.label" maxlength="120" /></template
          ></el-table-column>
          <el-table-column label="表达式" min-width="250"
            ><template #default="{ row }"
              ><el-input v-model="row.expression" maxlength="256" /></template
          ></el-table-column>
          <el-table-column label="单位" min-width="120"
            ><template #default="{ row }"><el-input v-model="row.unit" maxlength="40" /></template
          ></el-table-column>
          <el-table-column label="小数位" width="110"
            ><template #default="{ row }"
              ><el-select v-model="row.amountScale" placeholder="选择"
                ><el-option
                  v-for="n in scales"
                  :key="n"
                  :value="n"
                  :label="String(n)" /></el-select></template
          ></el-table-column>
          <el-table-column label="舍入方式" min-width="170"
            ><template #default="{ row }"
              ><el-select v-model="row.roundingMode" placeholder="明确选择"
                ><el-option
                  v-for="(label, key) in Api.roundings"
                  :key="key"
                  :value="key"
                  :label="label" /></el-select></template
          ></el-table-column>
          <el-table-column width="70"
            ><template #default="{ $index }"
              ><el-button link type="danger" @click="program.items.splice($index, 1)"
                >移除</el-button
              ></template
            ></el-table-column
          >
        </el-table>
        <div class="division"
          ><label
            >每次除法的小数位<el-select
              v-model="program.divisionScale"
              clearable
              placeholder="含除法时明确选择"
              ><el-option
                v-for="n in scales"
                :key="n"
                :value="n"
                :label="String(n)" /></el-select></label
          ><label
            >每次除法的舍入方式<el-select
              v-model="program.divisionRoundingMode"
              clearable
              placeholder="含除法时明确选择"
              ><el-option
                v-for="(label, key) in Api.roundings"
                :key="key"
                :value="key"
                :label="label" /></el-select></label
        ></div>
      </el-tab-pane>
      <el-tab-pane label="业务样例与预期" name="cases">
        <div class="section-title"
          ><h3>逐项对账样例</h3
          ><div
            ><el-button :disabled="!keysValid" @click="alignCases">按当前编号整理字段</el-button
            ><el-button :disabled="!keysValid || program.cases.length >= 20" @click="addCase"
              >增加样例</el-button
            ></div
          ></div
        >
        <p class="note"
          >先声明输入/项目编号，再填写样例。金额均按十进制文本填写；空白表示缺失，不能代替零。全部项目都需预期值，样例通过后才可提交确认。</p
        >
        <el-empty v-if="!program.cases.length" description="尚未登记样例，不能确认规则" />
        <div v-for="(sample, index) in program.cases" :key="index" class="sample">
          <div class="section-title"
            ><el-input v-model="sample.title" placeholder="样例名称" maxlength="120" /><el-button
              link
              type="danger"
              @click="program.cases.splice(index, 1)"
              >移除样例</el-button
            ></div
          >
          <el-alert
            v-if="extraFields(sample)"
            :title="'样例有旧字段：' + extraFields(sample) + '；请整理并核对新字段。'"
            type="warning"
            :closable="false"
          />
          <h4>输入</h4
          ><div class="sample-grid"
            ><label v-for="field in program.inputs" :key="field.key"
              >{{ field.label || field.key }} · {{ field.unit
              }}<el-input
                v-model="sample.inputs[field.key]"
                inputmode="decimal"
                maxlength="40"
                :placeholder="field.key" /></label
          ></div>
          <h4>预期结果</h4
          ><div class="sample-grid"
            ><label v-for="field in program.items" :key="field.key"
              >{{ field.label || field.key }} · {{ field.unit
              }}<el-input
                v-model="sample.expected[field.key]"
                inputmode="decimal"
                maxlength="40"
                :placeholder="field.key" /></label
          ></div>
        </div>
      </el-tab-pane>
    </el-tabs>
    <template #footer
      ><el-button @click="loadWageTemplate">载入常规工资模板</el-button
      ><el-button @click="loadSynthetic">载入合成运算样例</el-button
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template
    >
  </Dialog>
</template>
<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import * as Api from '@/api/hrm/payroll/calculation'
const emit = defineEmits(['saved'])
const visible = ref(false),
  saving = ref(false),
  error = ref(''),
  tab = ref('program')
const scales = Array.from({ length: 9 }, (_, n) => n)
const empty = (): Api.Definition => ({
  code: '',
  title: '',
  scopeCode: '',
  ownerName: '',
  applicableScope: '',
  description: '',
  reference: '',
  effectiveFrom: '',
  effectiveTo: '',
  program: { inputs: [], items: [], cases: [] }
})
const form = ref<Api.Definition>(empty())
const program = computed(() => form.value.program!)
const keysValid = computed(() => {
  const keys = [...program.value.inputs, ...program.value.items].map((x) => x.key)
  return (
    program.value.items.length > 0 &&
    keys.every((k) => /^[A-Za-z][A-Za-z0-9_]{0,63}$/.test(k)) &&
    new Set(keys).size === keys.length
  )
})
const addInput = () => program.value.inputs.push({ key: '', label: '', type: '', unit: '' })
const addItem = () =>
  program.value.items.push({ key: '', label: '', unit: '', expression: '', roundingMode: '' })
const fields = (keys: string[], old: Record<string, string> = {}) =>
  Object.fromEntries(keys.map((key) => [key, old[key] ?? '']))
const addCase = () =>
  program.value.cases.push({
    title: '',
    inputs: fields(program.value.inputs.map((x) => x.key)),
    expected: fields(program.value.items.map((x) => x.key))
  })
const extraFields = (sample: Api.BusinessCase) =>
  [
    ...Object.keys(sample.inputs).filter((k) => !program.value.inputs.some((x) => x.key === k)),
    ...Object.keys(sample.expected).filter((k) => !program.value.items.some((x) => x.key === k))
  ].join('、')
const alignCases = async () => {
  if (program.value.cases.some((c) => extraFields(c))) {
    try {
      await ElMessageBox.confirm(
        '将移除已不存在的样例字段，新增字段保持空白。整理后请重新核对输入及预期。',
        '整理样例字段'
      )
    } catch {
      return
    }
  }
  for (const sample of program.value.cases) {
    sample.inputs = fields(
      program.value.inputs.map((x) => x.key),
      sample.inputs
    )
    sample.expected = fields(
      program.value.items.map((x) => x.key),
      sample.expected
    )
  }
}
const open = async (id?: number) => {
  error.value = ''
  tab.value = 'program'
  form.value = empty()
  if (id) form.value = JSON.parse(JSON.stringify(await Api.get(id)))
  visible.value = true
}
const loadWageTemplate = async () => {
  try {
    await ElMessageBox.confirm(
      '将替换当前输入、表达式和样例。基本工资、扣款和已核定个税均需明确填写，合成样例仅用于对账。请核对后保存并评审。',
      '载入常规工资模板'
    )
  } catch {
    return
  }
  try {
    form.value.program = await Api.wageTemplate()
  } catch (e: any) {
    error.value = e?.message || String(e)
  }
}
const loadSynthetic = async () => {
  try {
    await ElMessageBox.confirm(
      '将替换当前表单中的输入、表达式和样例。此处为合成数学数据，规则依据及真实适用范围须另行登记。',
      '载入合成运算样例'
    )
  } catch {
    return
  }
  const input = (key: string, label: string, unit: string): Api.Input => ({
    key,
    label,
    unit,
    type: 'DECIMAL',
    scale: 2
  })
  const item = (key: string, label: string, expression: string): Api.Item => ({
    key,
    label,
    expression,
    unit: '合成元',
    amountScale: 2,
    roundingMode: 'HALF_UP'
  })
  form.value.program = {
    inputs: [
      input('base', '合成基础金额', '合成元'),
      input('days', '合成参与天数', '天'),
      input('cycleDays', '合成期间天数', '天')
    ],
    items: [
      item('net', '合成结果', 'prorated - deduction'),
      item('prorated', '合成折算额', 'base * days / cycleDays'),
      item('deduction', '合成调整', '0')
    ],
    divisionScale: 8,
    divisionRoundingMode: 'HALF_UP',
    cases: [
      {
        title: '合成三分之一运算',
        inputs: { base: '1000.00', days: '1', cycleDays: '3' },
        expected: { net: '333.33', prorated: '333.33', deduction: '0.00' }
      }
    ]
  }
}
const save = async () => {
  saving.value = true
  error.value = ''
  try {
    const data = JSON.parse(JSON.stringify(form.value)) as Api.Definition
    if (!data.effectiveFrom) delete data.effectiveFrom
    if (!data.effectiveTo) delete data.effectiveTo
    if (data.program?.divisionScale == null) delete data.program!.divisionScale
    if (!data.program?.divisionRoundingMode) delete data.program!.divisionRoundingMode
    if (data.id) await Api.update(data)
    else await Api.create(data)
    visible.value = false
    emit('saved')
  } catch (e: any) {
    error.value = e?.message || String(e)
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
<style scoped>
.metadata {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0 18px;
}
.wide {
  grid-column: span 3;
}
.note {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin: 20px 0 12px;
}
.division {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 20px;
}
.division label,
.sample-grid label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}
.sample {
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin: 16px 0;
}
.sample-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
:deep(.el-date-editor),
:deep(.el-select) {
  width: 100%;
}
@media (max-width: 720px) {
  .metadata,
  .sample-grid,
  .division {
    grid-template-columns: 1fr;
  }
  .wide {
    grid-column: span 1;
  }
  .section-title {
    flex-wrap: wrap;
  }
}
</style>
