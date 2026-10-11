<template>
  <ContentWrap>
    <el-form inline>
      <el-form-item label="法人主体"
        ><el-select v-model="entityId" clearable filterable style="width: 240px" @change="load"
          ><el-option
            v-for="e in entities"
            :key="e.id"
            :label="`${e.code} ${e.name}`"
            :value="e.id" /></el-select
      ></el-form-item>
      <el-form-item label="年份"
        ><el-input-number v-model="year" :min="2000" :max="2100" @change="load"
      /></el-form-item>
      <el-form-item label="批次"
        ><el-select :model-value="modelValue" filterable style="width: 320px" @change="choose"
          ><el-option
            v-for="b in batches"
            :key="b.id"
            :value="b.id!"
            :label="`${b.entityId ? names[b.entityId] || b.entityId : '未归属'} · ${b.title} · #${b.id}`" /></el-select
      ></el-form-item>
      <el-button v-hasPermi="['hrm:salary:month-record:create']" @click="open"
        >创建工资批次</el-button
      >
      <el-button
        v-if="selected && !selected.entityId && [5, 11].includes(selected.status!)"
        v-hasPermi="['hrm:salary:month-record:update']"
        @click="bind"
        >确认旧草稿归属</el-button
      >
    </el-form>
    <el-alert
      v-if="selected && !selected.entityId"
      title="此历史批次尚未确认法人主体。管理员可查询；旧草稿需确认归属并重新核算，冻结或已发放的历史批次保留原记录。"
      type="warning"
      :closable="false"
    />
  </ContentWrap>
  <el-dialog v-model="visible" :title="binding ? '确认旧草稿归属' : '创建工资批次'" width="540px">
    <el-form label-width="100px">
      <el-form-item label="法人主体"
        ><el-select v-model="form.entityId" filterable
          ><el-option
            v-for="e in calculable"
            :key="e.id"
            :label="e.name"
            :value="e.id" /></el-select
      ></el-form-item>
      <el-form-item label="工资月份"
        ><el-date-picker v-model="form.period" type="month" value-format="YYYY-MM"
      /></el-form-item>
      <el-form-item label="计薪区间"
        ><el-date-picker
          v-model="form.dates"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="默认配置"
          end-placeholder="默认配置"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="create">创建</el-button></template
    >
  </el-dialog>
</template>
<script setup lang="ts">
import * as EntityApi from '@/api/hrm/payroll/entity'
import * as SalaryApi from '@/api/hrm/salary/month-record'
import request from '@/config/axios'
import dayjs from 'dayjs'
const props = defineProps<{ modelValue?: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: number | undefined]; change: [] }>()
const message = useMessage()
const entities = ref<EntityApi.Entity[]>([]),
  batches = ref<SalaryApi.SalaryMonthRecordVO[]>([])
const entityId = ref<number>(),
  year = ref(dayjs().year()),
  visible = ref(false),
  saving = ref(false)
const binding = ref(false)
const form = ref<any>({}),
  names = computed(() => Object.fromEntries(entities.value.map((e) => [e.id, e.name])))
const calculable = computed(() =>
  entities.value.filter((e) => e.status === 'APPROVED' && e.allowedActions.includes('CALCULATE'))
)
const selected = computed(() => batches.value.find((b) => b.id === props.modelValue))
const choose = (id?: number) => {
  emit('update:modelValue', id)
  emit('change')
}
const load = async () => {
  const result = await SalaryApi.getSalaryMonthRecordPage({
    pageNo: 1,
    pageSize: 100,
    entityId: entityId.value,
    year: year.value
  } as any)
  batches.value = result.list
  if (!batches.value.some((b) => b.id === props.modelValue)) choose(batches.value[0]?.id)
}
const open = () => {
  binding.value = false
  form.value = { entityId: entityId.value, period: dayjs().format('YYYY-MM'), dates: [] }
  visible.value = true
}
const create = async () => {
  if (binding.value) {
    if (!form.value.entityId || !form.value.reason?.trim()) {
      message.warning('请选择主体并填写归属依据')
      return
    }
    saving.value = true
    try {
      await request.post({
        url: '/hrm/payroll/batch/bind-subject',
        params: {
          batchId: props.modelValue,
          entityId: form.value.entityId,
          reason: form.value.reason
        }
      })
      visible.value = false
      await load()
      emit('change')
      message.success('已确认归属，请重新核算')
    } finally {
      saving.value = false
    }
    return
  }

  if (!form.value.entityId || !form.value.period) {
    message.warning('请选择已批准主体和工资月份')
    return
  }
  saving.value = true
  try {
    const [y, m] = form.value.period.split('-').map(Number),
      dates = form.value.dates
    const id = await request.post<number>({
      url: '/hrm/salary/month-record/create',
      data: {
        entityId: form.value.entityId,
        year: y,
        month: m,
        startTime: dates?.length ? dayjs(dates[0]).startOf('day').valueOf() : undefined,
        endTime: dates?.length ? dayjs(dates[1]).startOf('day').valueOf() : undefined
      }
    })
    visible.value = false
    entityId.value = form.value.entityId
    year.value = y
    await load()
    choose(id)
    message.success('已创建')
  } finally {
    saving.value = false
  }
}
const bind = () => {
  if (!props.modelValue) return
  binding.value = true
  form.value = { entityId: undefined, reason: '' }
  visible.value = true
}
defineExpose({ refresh: load })
onMounted(async () => {
  entities.value = await EntityApi.list()
  await load()
})
</script>
