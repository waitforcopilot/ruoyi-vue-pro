<template>
  <doc-alert title="【薪资】月度工资、工资条" url="https://doc.iocoder.cn/hrm/salary/payroll/" />

  <!-- 搜索工作栏 -->
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="84px"
    >
      <el-form-item label="工资月份" prop="month">
        <el-date-picker
          v-model="queryParams.month"
          class="!w-180px"
          clearable
          placeholder="请选择工资月份"
          type="month"
          value-format="YYYY-MM"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon class="mr-5px" icon="ep:search" />搜索</el-button>
        <el-button @click="resetQuery"><Icon class="mr-5px" icon="ep:refresh" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 工资条发放记录 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column align="center" label="工资月份" width="120">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">
            {{ formatHrmYearMonth(scope.row.year, scope.row.month) }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="核算版本" prop="runVersion" width="100" />
      <el-table-column label="发布状态" width="100"
        ><template #default="{ row }">{{
          row.withdrawn ? '已撤回' : '已发布'
        }}</template></el-table-column
      >
      <el-table-column label="撤回时间" prop="withdrawnAt" :formatter="dateFormatter" width="180" />
      <el-table-column label="创建人" min-width="120" prop="creatorName" show-overflow-tooltip />
      <el-table-column
        align="center"
        label="发放时间"
        prop="createTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column align="center" label="工资表总人数" prop="employeeCount" width="130" />
      <el-table-column align="center" label="发放人数" prop="sendEmployeeCount" width="110" />
      <el-table-column align="center" label="已查看人数" prop="readCount" width="110" />
      <el-table-column align="center" fixed="right" label="操作" width="200">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">详情</el-button>
          <el-button link type="primary" @click="openDeliveries(scope.row.id)">投递</el-button>
          <el-button
            v-if="!scope.row.withdrawn"
            v-hasPermi="['hrm:salary:slip:delete']"
            link
            type="danger"
            @click="handleDeleteRecord(scope.row.id)"
          >
            撤回
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>
  <Dialog v-model="deliveryVisible" title="工资条通知投递" width="1000px">
    <el-alert
      title="已受理表示通知已交给渠道；工资条阅读情况请查看已查看人数。结果待核实的投递不会自动重发。"
      type="info"
      :closable="false"
    />
    <el-button :loading="deliveryLoading" class="my-12px" @click="loadDeliveries">刷新</el-button>
    <el-table :data="deliveryRows" v-loading="deliveryLoading">
      <el-table-column type="expand"
        ><template #default="{ row }"><SalarySlipDeliveryHistory :id="row.id" /></template
      ></el-table-column>
      <el-table-column label="接收账号" prop="userId" width="110" />
      <el-table-column label="渠道"
        ><template #default="{ row }">{{ channelNames[row.channel] }}</template></el-table-column
      >
      <el-table-column label="状态"
        ><template #default="{ row }">{{
          deliveryNames[row.status] || row.status
        }}</template></el-table-column
      >
      <el-table-column label="尝试次数" prop="attempts" width="100" />
      <el-table-column label="渠道日志" prop="providerLogId" width="110" />
      <el-table-column label="原因" prop="failureCode" min-width="150" />
      <el-table-column
        label="最后尝试"
        prop="lastAttemptAt"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" width="90"
        ><template #default="{ row }">
          <el-button
            v-if="row.status === 'FAILED' && row.attempts < 5"
            v-hasPermi="['hrm:salary:slip:create']"
            link
            type="primary"
            :disabled="deliveryLoading"
            @click="retryDelivery(row.id)"
            >重试</el-button
          >
        </template></el-table-column
      >
    </el-table>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormInstance } from 'element-plus'
import SalarySlipDeliveryHistory from './SalarySlipDeliveryHistory.vue'
import { dateFormatter, formatDate } from '@/utils/formatTime'
import * as SalarySlipSendRecordApi from '@/api/hrm/salary/slip/send-record'
import { formatHrmYearMonth } from '@/views/hrm/utils/format'

defineOptions({ name: 'HrmSalarySlipSendRecord' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const { push } = useRouter() // 路由
const loading = ref(false) // 列表加载中
const total = ref(0) // 列表总数
const list = ref<SalarySlipSendRecordApi.SalarySlipSendRecordVO[]>([]) // 发放记录列表
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  month: formatDate(new Date(), 'YYYY-MM')
}) // 查询参数
const queryFormRef = ref<FormInstance>() // 查询表单

/** 获得工资条发放记录 */
async function getList() {
  loading.value = true
  try {
    const [year, month] = queryParams.month ? queryParams.month.split('-').map(Number) : []
    const params = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      year,
      month
    }
    const data = await SalarySlipSendRecordApi.getSalarySlipSendRecordPage(params)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleQuery() {
  queryParams.pageNo = 1
  getList()
}

/** 重置搜索 */
function resetQuery() {
  queryFormRef.value?.resetFields()
  queryParams.month = formatDate(new Date(), 'YYYY-MM')
  handleQuery()
}

/** 打开工资条详情 */
function openDetail(id?: number) {
  if (!id) {
    return
  }
  push({ name: 'HrmSalarySlipSendRecordDetail', params: { id } })
}

/** 删除发放记录 */
async function handleDeleteRecord(id?: number) {
  if (!id) {
    return
  }
  try {
    await message.delConfirm('撤回后员工无法继续查看本次工资条，发放记录仍保留，是否继续？')
    await SalarySlipSendRecordApi.deleteSalarySlipSendRecord(id)
    message.success('工资条已撤回')
    await getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getList()
})

const deliveryVisible = ref(false)
const deliveryLoading = ref(false)
const deliveryRows = ref<SalarySlipSendRecordApi.DeliveryVO[]>([])
const deliveryPublication = ref<number>()
const channelNames: Record<string, string> = { STATION: '站内', EMAIL: '邮件', SMS: '短信' }
const deliveryNames: Record<string, string> = {
  QUEUED: '待发送',
  SENDING: '发送中',
  WAITING: '等待渠道结果',
  DELIVERED: '渠道已受理',
  FAILED: '失败',
  UNKNOWN: '结果待核实',
  CANCELLED: '已撤回'
}
let deliverySequence = 0
async function openDeliveries(id: number) {
  deliveryPublication.value = id
  deliveryRows.value = []
  deliveryVisible.value = true
  await loadDeliveries()
}
async function loadDeliveries() {
  const id = deliveryPublication.value
  if (!id) return
  const sequence = ++deliverySequence
  deliveryLoading.value = true
  try {
    const rows = await SalarySlipSendRecordApi.getDeliveries(id)
    if (sequence === deliverySequence) deliveryRows.value = rows
  } finally {
    if (sequence === deliverySequence) deliveryLoading.value = false
  }
}
async function retryDelivery(id: number) {
  deliveryLoading.value = true
  try {
    await SalarySlipSendRecordApi.retryDelivery(id)
    await loadDeliveries()
  } finally {
    deliveryLoading.value = false
  }
}
</script>
