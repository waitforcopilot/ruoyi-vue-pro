<template>
  <Dialog
    v-model="visible"
    :title="form.id ? '维护试算草稿' : '登记试算批次'"
    width="min(1200px,96vw)"
  >
    <el-alert
      title="选择已确认规则，逐人登记金额和来源依据。空白须补录；零金额请明确填写 0.00。排除计薪的人员由已确认资格决定。"
      type="info"
      :closable="false"
    />
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
    <el-form label-position="top" class="metadata">
      <el-form-item label="批次编号 *"
        ><el-input v-model="form.code" :disabled="!!form.id" maxlength="64" aria-label="批次编号"
      /></el-form-item>
      <el-form-item label="批次名称 *"
        ><el-input v-model="form.title" maxlength="160" aria-label="批次名称"
      /></el-form-item>
      <el-form-item label="负责人"
        ><el-input v-model="form.ownerName" maxlength="120" aria-label="批次负责人"
      /></el-form-item>
      <el-form-item label="声明主体编号 *"
        ><el-input
          v-model="form.entityCode"
          :disabled="!!form.id"
          maxlength="64"
          aria-label="批次主体编号"
      /></el-form-item>
      <el-form-item label="声明主体名称 *"
        ><el-input v-model="form.entityName" maxlength="160" aria-label="批次主体名称"
      /></el-form-item>
      <el-form-item label="期间类型"
        ><el-select v-model="form.periodType" :disabled="!!form.id" aria-label="批次期间类型"
          ><el-option value="MONTHLY" label="自然月" /><el-option
            value="CUSTOM"
            label="自定义期间" /></el-select
      ></el-form-item>
      <el-form-item label="开始日期 *"
        ><el-date-picker
          v-model="form.periodStart"
          :disabled="!!form.id"
          type="date"
          value-format="YYYY-MM-DD"
          aria-label="批次开始日期"
      /></el-form-item>
      <el-form-item label="结束日期 *"
        ><el-date-picker
          v-model="form.periodEnd"
          :disabled="!!form.id"
          type="date"
          value-format="YYYY-MM-DD"
          aria-label="批次结束日期"
      /></el-form-item>
      <el-form-item label="计算规则版本 *"
        ><el-select
          v-model="form.definitionId"
          :loading="rulesLoading"
          filterable
          aria-label="批次计算规则"
          @change="loadDefinition"
          ><el-option
            v-for="r in definitions"
            :key="r.id"
            :value="r.id!"
            :label="`${r.title} · ${r.code} V${r.definitionVersion}`" /></el-select
      ></el-form-item>
      <el-form-item label="批次口径依据" class="wide"
        ><el-input
          v-model="form.reference"
          type="textarea"
          maxlength="2000"
          aria-label="批次口径依据"
      /></el-form-item>
    </el-form>
    <div class="bindings"
      ><label v-for="(label, role) in Api.roleLabels" :key="role"
        >{{ label }}结果<el-select
          v-model="form.configuration.roles[role]"
          :aria-label="label + '结果绑定'"
          ><el-option
            v-for="item in definition?.program?.items || []"
            :key="item.key"
            :value="item.key"
            :label="`${item.label} · ${item.key}`" /></el-select></label
    ></div>
    <div class="people-title"><h3>人员与金额来源</h3><span>最多 100 人 · CNY 金额文本</span></div>
    <div class="add-person"
      ><el-input
        v-model="candidate"
        placeholder="明确填写 HRM 人员 ID"
        aria-label="试算 HRM 人员 ID"
      /><el-button
        :loading="personLoading"
        :disabled="form.configuration.people.length >= 100"
        @click="addPerson"
        >核对档案并加入</el-button
      ><el-button :disabled="!definition?.program" @click="alignInputs"
        >按当前规则整理输入字段</el-button
      ></div
    >
    <el-empty v-if="!form.configuration.people.length" description="请加入有权查看的 HRM 人员" />
    <div
      v-for="person in form.configuration.people"
      :key="person.employeeId"
      class="person"
      data-testid="trial-editor-person"
    >
      <div class="people-title"
        ><h4
          >{{ person.snapshotName }} · HRM #{{ person.employeeId
          }}<small
            >工号 {{ person.snapshotJobNumber || '未登记' }} · 抓取部门
            {{ person.snapshotDeptId ?? '未登记' }}</small
          ></h4
        ><div
          ><el-button :loading="refreshing === person.employeeId" @click="refreshPerson(person)"
            >刷新档案快照</el-button
          ><el-button type="danger" link @click="remove(person.employeeId)"
            >移除人员</el-button
          ></div
        ></div
      >
      <div class="input-grid"
        ><label v-for="field in definition?.program?.inputs || []" :key="field.key"
          >{{ field.label }} · {{ field.unit }} · {{ field.scale }} 位<el-input
            v-model="person.inputs[field.key]"
            :aria-label="`HRM ${person.employeeId} ${field.label}`"
            maxlength="40"
            inputmode="decimal"
            placeholder="明确录入金额，零请填写 0.00" /></label
      ></div>
      <el-alert
        v-if="extra(person)"
        :title="'旧规则字段仍存在：' + extra(person) + '；请整理并核对金额。'"
        type="warning"
        :closable="false"
      />
      <label class="basis"
        >个人输入来源依据<el-input
          v-model="person.inputReference"
          type="textarea"
          maxlength="2000"
          :aria-label="`HRM ${person.employeeId} 输入依据`"
          placeholder="工资约定、出勤核对、个人社保/公积金、已核定个税等来源"
      /></label>
    </div>
    <template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button
        type="primary"
        :loading="saving"
        :disabled="rulesLoading || personLoading || !!refreshing || !definition?.program"
        @click="save"
        >保存草稿</el-button
      ></template
    >
  </Dialog>
</template>
<script setup lang="ts">
import * as Api from '@/api/hrm/payroll/trial'
import * as Calc from '@/api/hrm/payroll/calculation'
import * as Eligibility from '@/api/hrm/payroll/eligibility'
import type { Mapping } from '@/api/hrm/payroll/identity'
import { ElMessageBox } from 'element-plus'
const emit = defineEmits(['saved'])
const visible = ref(false),
  saving = ref(false),
  rulesLoading = ref(false),
  personLoading = ref(false),
  refreshing = ref<number>(),
  error = ref(''),
  candidate = ref('')
const definitions = ref<Calc.Definition[]>([]),
  definition = ref<Calc.Definition>()
const empty = (): Api.Batch => ({
  code: '',
  title: '',
  entityCode: '',
  entityName: '',
  periodType: 'MONTHLY',
  periodStart: '',
  periodEnd: '',
  configuration: {
    roles: { gross: 'gross', deductions: 'deductions', tax: 'tax', net: 'net' },
    people: []
  }
})
const form = ref<Api.Batch>(empty())
let epoch = 0,
  ruleEpoch = 0,
  personEpoch = 0
watch(candidate, () => personEpoch++)
watch(visible, (v) => {
  if (!v) {
    epoch++
    ruleEpoch++
    personEpoch++
  }
})
const loadDefinition = async () => {
  const ticket = ++ruleEpoch,
    session = epoch,
    id = form.value.definitionId
  definition.value = undefined
  if (!id) return
  rulesLoading.value = true
  try {
    const row = await Calc.get(id)
    if (ruleEpoch === ticket && session === epoch && visible.value) definition.value = row
  } catch (e: any) {
    if (ruleEpoch === ticket && session === epoch) error.value = e?.message || String(e)
  } finally {
    if (ruleEpoch === ticket && session === epoch) rulesLoading.value = false
  }
}
const open = async (id?: number) => {
  epoch++
  personEpoch++
  error.value = ''
  form.value = empty()
  candidate.value = ''
  definition.value = undefined
  personLoading.value = false
  refreshing.value = undefined
  visible.value = true
  const ticket = epoch
  rulesLoading.value = true
  try {
    const [rows, row] = await Promise.all([
      Calc.page({ pageNo: 1, pageSize: 100, status: 1 }),
      id ? Api.get(id) : Promise.resolve(undefined)
    ])
    if (ticket !== epoch || !visible.value) return
    definitions.value = rows.list
    if (row) {
      form.value = JSON.parse(JSON.stringify(row))
      if (!definitions.value.some((r) => r.id === row.definitionId)) {
        const r = await Calc.get(row.definitionId!)
        if (ticket !== epoch) return
        definitions.value.push(r)
      }
    }
    await loadDefinition()
  } catch (e: any) {
    if (ticket === epoch) error.value = e?.message || String(e)
  } finally {
    if (ticket === epoch) rulesLoading.value = false
  }
}
const capture = (row: Mapping, inputs: Record<string, string> = {}): Api.Person => ({
  employeeId: row.employeeId!,
  employeeFingerprint: row.employeeFingerprint,
  snapshotName: row.snapshotName,
  snapshotJobNumber: row.snapshotJobNumber,
  snapshotDeptId: row.snapshotDeptId,
  snapshotUserId: row.snapshotUserId,
  snapshotCapturedAt: row.snapshotCapturedAt,
  inputs
})
const addPerson = async () => {
  error.value = ''
  if (!/^[1-9][0-9]*$/.test(candidate.value) || !Number.isSafeInteger(Number(candidate.value))) {
    error.value = '请填写合法 HRM 人员 ID'
    return
  }
  const id = Number(candidate.value)
  if (form.value.configuration.people.some((p) => p.employeeId === id)) {
    error.value = '此人员已登记'
    return
  }
  const ticket = ++personEpoch
  personLoading.value = true
  try {
    const row = await Eligibility.employee(id)
    if (ticket !== personEpoch || !visible.value) return
    const inputs = Object.fromEntries(
      (definition.value?.program?.inputs || []).map((f) => [f.key, ''])
    )
    form.value.configuration.people.push(capture(row, inputs))
    candidate.value = ''
  } catch (e: any) {
    if (ticket === personEpoch) error.value = e?.message || String(e)
  } finally {
    personLoading.value = false
  }
}
const refreshPerson = async (person: Api.Person) => {
  const ticket = epoch
  refreshing.value = person.employeeId
  error.value = ''
  try {
    const row = await Eligibility.employee(person.employeeId)
    if (ticket !== epoch || !visible.value || !form.value.configuration.people.includes(person))
      return
    Object.assign(person, capture(row, person.inputs))
  } catch (e: any) {
    if (ticket === epoch) error.value = e?.message || String(e)
  } finally {
    refreshing.value = undefined
  }
}
const remove = (id: number) => {
  form.value.configuration.people = form.value.configuration.people.filter(
    (p) => p.employeeId !== id
  )
}
const extra = (person: Api.Person) =>
  Object.keys(person.inputs)
    .filter((k) => !definition.value?.program?.inputs.some((f) => f.key === k))
    .join('、')
const alignInputs = async () => {
  try {
    await ElMessageBox.confirm(
      '按当前规则移除旧字段并增加空白字段。已有同名金额保留，请重新核对单位、精度和来源依据。',
      '整理输入字段'
    )
  } catch {
    return
  }
  for (const person of form.value.configuration.people)
    person.inputs = Object.fromEntries(
      (definition.value?.program?.inputs || []).map((f) => [f.key, person.inputs[f.key] ?? ''])
    )
}
const save = async () => {
  if (!form.value.definitionId || !form.value.configuration.people.length) {
    error.value = '请选择规则并登记至少一名人员'
    return
  }
  saving.value = true
  error.value = ''
  try {
    const data = JSON.parse(JSON.stringify(form.value))
    const id = data.id ? (await Api.update(data), data.id) : await Api.create(data)
    visible.value = false
    emit('saved', id)
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 18px;
  margin-top: 16px;
}
.wide {
  grid-column: span 3;
}
.bindings {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.bindings label,
.input-grid label,
.basis {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}
.people-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.people-title span,
small {
  color: #64748b;
  font-size: 12px;
}
small {
  display: block;
  margin-top: 5px;
}
.add-person {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.add-person > .el-input {
  width: 240px;
}
.person {
  border: 1px solid #dce3ed;
  border-radius: 8px;
  padding: 16px;
  margin-top: 18px;
}
.input-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.basis {
  margin-top: 12px;
}
.el-date-editor,
.el-select {
  width: 100% !important;
}
@media (max-width: 720px) {
  .metadata,
  .input-grid,
  .bindings {
    grid-template-columns: 1fr;
  }
  .wide {
    grid-column: auto;
  }
  .add-person > .el-input {
    width: 100%;
  }
}
</style>
