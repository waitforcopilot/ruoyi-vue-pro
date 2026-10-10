<template>
  <ContentWrap>
    <el-alert
      title="代发使用冻结版本。成功明细不可再次发放；重试只包含失败明细。回盘需匹配明细编号、发放次数、银行账户和金额。"
      :closable="false"
      type="info"
    />
    <el-space class="my-4"
      ><el-button @click="load">刷新</el-button
      ><el-button v-hasPermi="['hrm:payroll:payment:config']" @click="openTemplate"
        >新增银行模板版本</el-button
      ></el-space
    >
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="title" label="批次" min-width="180" />
      <el-table-column label="状态" width="120"
        ><template #default="{ row }">{{ statuses[row.status] }}</template></el-table-column
      >
      <el-table-column prop="employeeCount" label="人数" /><el-table-column
        prop="realPaySalary"
        label="实发总额"
      />
      <el-table-column label="操作"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="open(row)">代发与回盘</el-button></template
        ></el-table-column
      >
    </el-table>
    <Pagination
      v-model:page="query.pageNo"
      v-model:limit="query.pageSize"
      :total="total"
      @pagination="load"
    />
  </ContentWrap>
  <Dialog v-model="visible" :title="selected?.title" width="1000px">
    <template v-if="selected">
      <el-space wrap class="mb-4">
        <el-select v-model="templateId" clearable placeholder="标准银行模板" style="width: 220px"
          ><el-option
            v-for="template in templates"
            :key="template.id"
            :value="template.id!"
            :label="`${template.name}（版本 ${template.id}）`"
        /></el-select>
        <el-button
          v-if="selected.status === 15"
          v-hasPermi="['hrm:payroll:batch:pay']"
          :loading="saving"
          type="primary"
          @click="prepare"
          >生成代发明细</el-button
        >
        <el-button
          v-if="selected.status === 16"
          v-hasPermi="['hrm:payroll:payment:export']"
          @click="exportFile"
          >导出待发明细</el-button
        >
        <el-button @click="downloadTemplate">下载标准回盘模板</el-button>
        <el-upload
          v-if="selected.status === 16"
          v-hasPermi="['hrm:payroll:payment:reconcile']"
          :show-file-list="false"
          :auto-upload="false"
          accept=".xlsx,.xls"
          :on-change="importFile"
          ><el-button :loading="saving">导入银行回盘</el-button></el-upload
        >
      </el-space>
      <el-alert
        v-if="templateId"
        class="mb-4"
        title="自定义回盘文件应使用所选模板的列顺序、列名和成功/失败取值；配置不一致时整批拒绝导入。"
        :closable="false"
        type="info"
      />
      <el-table :data="payments">
        <el-table-column prop="employeeName" label="员工" /><el-table-column
          prop="bankAccount"
          label="银行账户"
          min-width="140"
        /><el-table-column prop="amount" label="金额" />
        <el-table-column label="状态"
          ><template #default="{ row }">{{
            paymentStatuses[row.status]
          }}</template></el-table-column
        >
        <el-table-column prop="attempt" label="发放次数" width="100" /><el-table-column
          prop="failureReason"
          label="失败原因"
          min-width="180"
        />
        <el-table-column label="操作" width="90"
          ><template #default="{ row }"
            ><el-button
              v-if="row.status === 'FAILED' && selected.status === 16"
              v-hasPermi="['hrm:payroll:batch:pay']"
              link
              type="primary"
              :disabled="saving"
              @click="retry(row)"
              >重试</el-button
            ></template
          ></el-table-column
        >
      </el-table>
      <h4>回盘导入记录</h4>
      <el-table :data="receipts"
        ><el-table-column prop="fileName" label="文件" /><el-table-column
          prop="rowCount"
          label="明细数量" /><el-table-column prop="actorId" label="操作人" /><el-table-column
          prop="createTime"
          label="导入时间"
      /></el-table>
    </template>
  </Dialog>
  <Dialog v-model="differenceVisible" title="回盘差异：未更新代发台账" width="750px">
    <el-table :data="differences"
      ><el-table-column prop="row" label="文件行" width="90" /><el-table-column
        prop="paymentId"
        label="代发明细"
        width="140" /><el-table-column prop="reason" label="差异说明"
    /></el-table>
  </Dialog>
  <Dialog v-model="templateVisible" title="新增银行模板版本" width="850px">
    <el-form label-width="100px"
      ><el-form-item label="模板名称"
        ><el-input v-model="templateForm.name" maxlength="100"
      /></el-form-item>
      <el-form-item label="成功取值"
        ><el-input v-model="templateForm.successValue" maxlength="30" /></el-form-item
      ><el-form-item label="失败取值"
        ><el-input v-model="templateForm.failedValue" maxlength="30"
      /></el-form-item>
    </el-form>
    <el-tabs
      ><el-tab-pane v-for="kind in kinds" :key="kind.key" :label="kind.label">
        <el-table :data="templateForm[kind.key]"
          ><el-table-column label="字段"
            ><template #default="{ row }">{{ fieldNames[row.field] }}</template></el-table-column
          ><el-table-column label="文件列名"
            ><template #default="{ row }"
              ><el-input v-model="row.label" maxlength="100" /></template></el-table-column
          ><el-table-column label="顺序" width="150"
            ><template #default="{ $index }"
              ><el-button :disabled="$index === 0" link @click="move(kind.key, $index, -1)"
                >上移</el-button
              ><el-button
                :disabled="$index === templateForm[kind.key].length - 1"
                link
                @click="move(kind.key, $index, 1)"
                >下移</el-button
              ></template
            ></el-table-column
          ></el-table
        >
      </el-tab-pane></el-tabs
    >
    <template #footer
      ><el-button @click="templateVisible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="saveTemplate"
        >保存新版本</el-button
      ></template
    >
  </Dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/hrm/payroll/payment'
import { statuses } from '@/api/hrm/payroll/batch'
import download from '@/utils/download'
import type { UploadFile } from 'element-plus'
defineOptions({ name: 'HrmPayrollPayment' })
const receipts = ref<api.Receipt[]>([]),
  differences = ref<api.Difference[]>([]),
  differenceVisible = ref(false)
const message = useMessage(),
  loading = ref(false),
  saving = ref(false),
  visible = ref(false),
  templateVisible = ref(false)
const query = reactive({ pageNo: 1, pageSize: 10 }),
  total = ref(0),
  rows = ref<api.PaymentBatch[]>([]),
  selected = ref<api.PaymentBatch>(),
  payments = ref<api.Payment[]>([])
const templates = ref<api.BankTemplate[]>([]),
  templateId = ref<number>()
const paymentStatuses: Record<string, string> = {
  PENDING: '待发放',
  SUCCESS: '成功',
  FAILED: '失败'
}
const fieldNames: Record<string, string> = {
  id: '代发明细编号',
  attempt: '发放次数',
  employeeName: '员工姓名',
  bankName: '银行',
  bankAccount: '银行账户',
  amount: '金额',
  status: '回盘状态',
  failureReason: '失败原因'
}
type Form = {
  name: string
  successValue: string
  failedValue: string
  columns: api.Column[]
  returnColumns: api.Column[]
}
const kinds: { key: 'columns' | 'returnColumns'; label: string }[] = [
  { key: 'columns', label: '代发文件' },
  { key: 'returnColumns', label: '回盘文件' }
]
const defaults = (): Form => ({
  name: '',
  successValue: 'SUCCESS',
  failedValue: 'FAILED',
  columns: ['id', 'attempt', 'employeeName', 'bankName', 'bankAccount', 'amount'].map((field) => ({
    field,
    label: fieldNames[field]
  })),
  returnColumns: ['id', 'attempt', 'bankAccount', 'amount', 'status', 'failureReason'].map(
    (field) => ({ field, label: fieldNames[field] })
  )
})
const templateForm = ref<Form>(defaults())
const loadTemplates = async () => {
  templates.value = await api.templates()
}
const load = async () => {
  loading.value = true
  try {
    const data = await api.batches(query)
    rows.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const open = async (row: api.PaymentBatch) => {
  selected.value = row
  const data = await Promise.all([api.list(row.id), api.receipts(row.id)])
  payments.value = data[0]
  receipts.value = data[1]
  visible.value = true
}
const refresh = async () => {
  await load()
  const batch = rows.value.find((row) => row.id === selected.value?.id)
  if (batch) await open(batch)
  else visible.value = false
}
const prepare = async () => {
  saving.value = true
  try {
    await api.prepare(selected.value!.id, selected.value!.runId)
    await refresh()
    message.success('代发明细已生成')
  } finally {
    saving.value = false
  }
}
const retry = async (row: api.Payment) => {
  saving.value = true
  try {
    await api.retry(row.id)
    await refresh()
    message.success('已进入待发放')
  } finally {
    saving.value = false
  }
}
const exportFile = async () => {
  download.excel(await api.exportFile(selected.value!.id, templateId.value), '银行代发.xlsx')
}
const downloadTemplate = async () => {
  download.excel(await api.importTemplate(), '标准银行回盘.xlsx')
}
const importFile = async (file: UploadFile) => {
  if (!file.raw) return
  saving.value = true
  try {
    const form = new FormData()
    form.append('batchId', String(selected.value!.id))
    if (templateId.value) form.append('templateId', String(templateId.value))
    form.append('file', file.raw)
    differences.value = await api.validateFile(form)
    if (differences.value.length) {
      differenceVisible.value = true
      return
    }
    await api.reconcile(form)
    await refresh()
    message.success('回盘已核对')
  } finally {
    saving.value = false
  }
}
const openTemplate = () => {
  const source = templates.value.find((item) => item.id === templateId.value)
  templateForm.value = source
    ? {
        name: source.name,
        successValue: source.successValue,
        failedValue: source.failedValue,
        columns:
          typeof source.columns === 'string'
            ? JSON.parse(source.columns)
            : source.columns.map((column) => ({ ...column })),
        returnColumns:
          typeof source.returnColumns === 'string'
            ? JSON.parse(source.returnColumns)
            : source.returnColumns.map((column) => ({ ...column }))
      }
    : defaults()
  templateVisible.value = true
}
const move = (kind: 'columns' | 'returnColumns', index: number, offset: number) => {
  const columns = templateForm.value[kind]
  ;[columns[index], columns[index + offset]] = [columns[index + offset], columns[index]]
}
const saveTemplate = async () => {
  const form = templateForm.value
  if (
    !form.name.trim() ||
    !form.successValue.trim() ||
    !form.failedValue.trim() ||
    form.successValue === form.failedValue
  ) {
    message.warning('请填写名称及不同的成功、失败取值')
    return
  }
  if (kinds.some((kind) => form[kind.key].some((column) => !column.label.trim()))) {
    message.warning('请填写所有文件列名')
    return
  }
  saving.value = true
  try {
    templateId.value = await api.createTemplate(form)
    await loadTemplates()
    templateVisible.value = false
    message.success('新模板版本已保存')
  } finally {
    saving.value = false
  }
}
onMounted(async () => {
  await Promise.all([load(), loadTemplates()])
})
</script>
