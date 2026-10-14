<template>
  <el-table :data="rows" v-loading="loading" class="px-20px">
    <el-table-column label="尝试次数" prop="attemptNumber" width="100" />
    <el-table-column label="状态" width="150"
      ><template #default="{ row }">{{
        names[row.status] || row.status
      }}</template></el-table-column
    >
    <el-table-column label="渠道日志" prop="providerLogId" width="130" />
    <el-table-column label="原因" prop="failureCode" min-width="180" />
    <el-table-column label="尝试时间" prop="startedAt" :formatter="dateFormatter" width="180" />
  </el-table>
</template>
<script setup lang="ts">
import { getDeliveryHistory, type DeliveryAttemptVO } from '@/api/hrm/salary/slip/send-record'
import { dateFormatter } from '@/utils/formatTime'
const props = defineProps<{ id: number }>()
const rows = ref<DeliveryAttemptVO[]>([])
const loading = ref(false)
const names: Record<string, string> = {
  SENDING: '发送中',
  WAITING: '等待渠道结果',
  DELIVERED: '渠道已受理',
  FAILED: '失败',
  UNKNOWN: '结果待核实',
  CANCELLED: '已撤回'
}
onMounted(async () => {
  loading.value = true
  try {
    rows.value = await getDeliveryHistory(props.id)
  } finally {
    loading.value = false
  }
})
</script>
