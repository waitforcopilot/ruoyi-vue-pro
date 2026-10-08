<template>
  <div v-loading="loading" class="review-panel" data-testid="payroll-review-panel">
    <div class="review-heading">
      <div><h3>版本复核与冻结</h3><p>HR、财务依次独立复核；冻结保存本次核对的试算版本。</p></div>
      <el-tag v-if="view" :type="view.batchStatus === 4 ? 'success' : 'info'">{{
        Trial.states[view.batchStatus]
      }}</el-tag>
    </div>
    <el-alert
      title="核算人和发起人不能担任复核人，两名复核人必须不同。资料变化须重新试算；驳回、撤销或解冻保留历史记录。冻结完成后仍须另行安排发放。"
      type="info"
      :closable="false"
    />
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
    <template v-if="view">
      <div class="review-binding">
        <span>当前试算：{{ currentVersion }}</span
        ><span>冻结版本：{{ version(view.frozenRunId) }}</span>
        <span v-if="view.cycle"
          >本轮复核 #{{ view.cycle.cycleVersion }} · {{ version(view.cycle.runId) }}</span
        >
      </div>
      <div v-if="view.cycle" class="review-stages" data-testid="review-stages">
        <article
          ><h4>1 · HR 复核</h4><p>{{ view.cycle.hrReviewerName }}</p
          ><strong>{{
            closedLabel || (view.cycle.hrReviewedAt ? '已记录复核意见' : '待 HR 复核')
          }}</strong
          ><p v-if="view.cycle.hrEvidence" class="evidence">{{ view.cycle.hrEvidence }}</p
          ><small>{{ view.cycle.hrReviewedAt }}</small></article
        >
        <article
          ><h4>2 · 财务复核</h4><p>{{ view.cycle.financeReviewerName }}</p
          ><strong>{{
            closedLabel ||
            (view.cycle.financeReviewedAt
              ? '已记录复核意见'
              : view.cycle.hrReviewedAt
                ? '待财务复核'
                : '等待 HR 复核')
          }}</strong
          ><p v-if="view.cycle.financeEvidence" class="evidence">{{ view.cycle.financeEvidence }}</p
          ><small>{{ view.cycle.financeReviewedAt }}</small></article
        >
        <article
          ><h4>3 · 版本冻结</h4
          ><strong>{{
            closedLabel ||
            (view.batchStatus === 4
              ? '版本已冻结'
              : view.batchStatus === 3
                ? '两级复核通过，待冻结'
                : '等待两级复核通过')
          }}</strong
          ><p v-if="view.cycle.freezeEvidence" class="evidence">{{ view.cycle.freezeEvidence }}</p
          ><small>{{ view.cycle.frozenByName }} {{ view.cycle.frozenAt }}</small></article
        >
      </div>
      <el-alert
        v-if="view.batchStatus === 0"
        title="尚无当前有效试算，请先核验资料并保存新试算版本。"
        type="warning"
        :closable="false"
      />
      <el-alert
        v-if="view.batchStatus === 2 && !view.tasks.length"
        title="流程正在同步，请核对真实流程状态后继续。"
        type="warning"
        :closable="false"
      />
      <div v-if="available.length || pending" class="review-form">
        <el-form label-position="top" :disabled="busy || !!pending">
          <div v-if="available.includes('submit')" class="review-picker">
            <el-form-item label="HR 复核人 *"
              ><el-select
                v-model="hrReviewerId"
                filterable
                placeholder="选择 HR 复核人"
                aria-label="HR 复核人"
                ><el-option
                  v-for="u in choices"
                  :key="u.id"
                  :label="u.nickname"
                  :value="u.id"
                  :disabled="u.id === financeReviewerId" /></el-select
            ></el-form-item>
            <el-form-item label="财务复核人 *"
              ><el-select
                v-model="financeReviewerId"
                filterable
                placeholder="选择财务复核人"
                aria-label="财务复核人"
                ><el-option
                  v-for="u in choices"
                  :key="u.id"
                  :label="u.nickname"
                  :value="u.id"
                  :disabled="u.id === hrReviewerId" /></el-select
            ></el-form-item>
          </div>
          <p v-if="available.includes('submit')" class="hint"
            >请选择具备本批次资料查看和对应复核权限的人员。</p
          >
          <el-form-item label="本次核对意见与依据 *"
            ><el-input
              v-model="evidence"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              aria-label="复核操作依据"
              placeholder="记录已核对事项、差异原因或撤销/解冻依据"
          /></el-form-item>
        </el-form>
        <div class="review-actions">
          <el-button v-if="pending" type="primary" :loading="busy" @click="send(pending.action)"
            >重试{{ Review.actionLabels[pending.action] }}</el-button
          >
          <template v-else
            ><el-button
              v-for="a in available"
              :key="a"
              :type="
                a === 'reject' || a === 'cancel' || a === 'admin-cancel' ? 'warning' : 'primary'
              "
              :loading="busy"
              @click="send(a)"
              >{{ Review.actionLabels[a] }}</el-button
            ></template
          >
        </div>
        <el-alert
          v-if="pending && !busy"
          title="上次操作尚未确认结果，重试保留原请求编号、版本和依据。"
          type="warning"
          :closable="false"
        />
      </div>
      <p v-else-if="view.batchStatus > 0" class="hint"
        >当前登录人员可查看本轮记录；对应环节由有权限的指定人员办理。</p
      >
      <div class="review-actions"
        ><el-button :disabled="busy" @click="refreshAndReset">刷新并重新核对</el-button
        ><el-button
          v-if="view.batchStatus === 2 && !view.tasks.length"
          :loading="busy"
          @click="sync"
          >同步流程状态</el-button
        ></div
      >
      <h4>复核历史 · {{ view.cycles.length }} 轮</h4>
      <el-empty v-if="!view.cycles.length" description="尚未提交复核" :image-size="70" />
      <el-collapse v-else v-model="opened" data-testid="review-history">
        <el-collapse-item
          v-for="c in view.cycles"
          :key="c.id"
          :name="c.id"
          :title="`第 ${c.cycleVersion} 轮 · ${version(c.runId)} · ${Review.cycleStates[c.status]}`"
        >
          <dl class="review-history-detail">
            <dt>提交依据</dt
            ><dd
              >{{ c.submitEvidence }}<small>{{ c.startedByName }} · {{ c.startedAt }}</small></dd
            >
            <dt>HR 意见</dt
            ><dd
              >{{ c.hrEvidence || '尚未记录'
              }}<small>{{ c.hrReviewerName }} · {{ c.hrReviewedAt }}</small></dd
            >
            <dt>财务意见</dt
            ><dd
              >{{ c.financeEvidence || '尚未记录'
              }}<small>{{ c.financeReviewerName }} · {{ c.financeReviewedAt }}</small></dd
            >
            <template v-if="c.freezeEvidence"
              ><dt>冻结依据</dt
              ><dd
                >{{ c.freezeEvidence }}<small>{{ c.frozenByName }} · {{ c.frozenAt }}</small></dd
              ></template
            >
            <template v-if="c.unfreezeEvidence"
              ><dt>解冻依据</dt
              ><dd
                >{{ c.unfreezeEvidence
                }}<small>{{ c.unfrozenByName }} · {{ c.unfrozenAt }}</small></dd
              ></template
            >
            <dt>流程记录</dt
            ><dd
              >{{ c.processInstanceId }}<small>{{ c.finishedAt || '复核进行中' }}</small></dd
            >
          </dl>
        </el-collapse-item>
      </el-collapse>
    </template>
  </div>
</template>
<script setup lang="ts">
import * as Trial from '@/api/hrm/payroll/trial'
import * as Review from '@/api/hrm/payroll/review'
import * as UserApi from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'
import { checkPermi } from '@/utils/permission'
const props = defineProps<{ batch: Trial.Batch; runs: Trial.Run[] }>()
const emit = defineEmits<{ changed: [id: number] }>()
const user = useUserStore()
const view = ref<Review.View>(),
  loading = ref(false),
  busy = ref(false),
  error = ref('')
const evidence = ref(''),
  hrReviewerId = ref<number>(),
  financeReviewerId = ref<number>(),
  users = ref<UserApi.UserVO[]>([])
const pending = ref<Review.Command>(),
  opened = ref<number[]>([])
let ticket = 0
const core = [
  'hrm:employee:query',
  'hrm:payroll:trial:query',
  'hrm:payroll:calculation:query',
  'hrm:payroll:eligibility:query'
]
const allowed = (p: string) =>
  core.every((s) => checkPermi([s])) && checkPermi(['hrm:payroll:trial:' + p])
const actor = computed(() => user.getUser.id)
const maker = computed(() => props.runs.find((r) => r.id === props.batch.currentRunId)?.executedBy)
const choices = computed(() =>
  users.value.filter((u) => u.id !== actor.value && u.id !== maker.value)
)
const version = (id?: number) =>
  id ? 'V' + (props.runs.find((r) => r.id === id)?.runVersion ?? '？') : '—'
const currentVersion = computed(() => version(props.batch.currentRunId))
const closedLabel = computed(() =>
  view.value?.cycle?.status === 2
    ? '本轮已驳回'
    : view.value?.cycle?.status === 3
      ? '本轮已撤销'
      : ''
)
const available = computed<Review.Action[]>(() => {
  const v = view.value,
    c = v?.cycle
  if (!v || v.revision !== props.batch.revision) return []
  if (v.batchStatus === 1 && props.batch.currentRunId && allowed('review-submit')) return ['submit']
  const out: Review.Action[] = []
  if (v.batchStatus === 2 && c) {
    const t = v.tasks.find((t) => t.assigneeId === actor.value)
    const expected = c.hrReviewedAt ? c.financeReviewerId : c.hrReviewerId
    if (
      t &&
      expected === actor.value &&
      actor.value !== maker.value &&
      actor.value !== c.startedBy &&
      allowed(t.key === 'hrReview' ? 'hr-review' : 'finance-review')
    )
      out.push('approve', 'reject')
    if (c.startedBy === actor.value && allowed('review-submit')) out.push('cancel')
    if (allowed('admin-cancel')) out.push('admin-cancel')
  }
  if (
    v.batchStatus === 3 &&
    c &&
    actor.value !== maker.value &&
    actor.value !== c.startedBy &&
    allowed('freeze')
  )
    out.push('freeze')
  if (v.batchStatus === 4 && allowed('unfreeze')) out.push('unfreeze')
  return out
})
const load = async () => {
  const id = props.batch.id!,
    generation = ++ticket
  loading.value = true
  error.value = ''
  view.value = undefined
  try {
    const data = await Review.get(id)
    if (generation !== ticket) return
    view.value = data
    if (data.batchStatus === 1 && allowed('review-submit')) {
      const options = await UserApi.getSimpleUserList()
      if (generation === ticket) users.value = options
    }
  } catch (e: any) {
    if (generation === ticket) error.value = e?.message || String(e)
  } finally {
    if (generation === ticket) loading.value = false
  }
}
watch(
  () => [props.batch.id, props.batch.revision],
  () => {
    load()
  },
  { immediate: true }
)
onBeforeUnmount(() => {
  ticket++
})
const send = async (action: Review.Action) => {
  if (busy.value || !view.value) return
  if (!pending.value) {
    if (!available.value.includes(action)) return
    if (!evidence.value.trim()) {
      error.value = '请填写本次核对意见与依据'
      return
    }
    if (
      action === 'submit' &&
      (!hrReviewerId.value ||
        !financeReviewerId.value ||
        hrReviewerId.value === financeReviewerId.value)
    ) {
      error.value = '请选择两名不同的复核人'
      return
    }
    pending.value = {
      batchId: props.batch.id!,
      runId: props.batch.currentRunId!,
      revision: view.value.revision,
      cycleId: action === 'submit' ? undefined : view.value.activeReviewId,
      requestKey: crypto.randomUUID(),
      action,
      evidence: evidence.value.trim(),
      taskId:
        action === 'approve' || action === 'reject'
          ? view.value.tasks.find((t) => t.assigneeId === actor.value)?.id
          : undefined,
      hrReviewerId: action === 'submit' ? hrReviewerId.value : undefined,
      financeReviewerId: action === 'submit' ? financeReviewerId.value : undefined
    }
  }
  const command = { ...pending.value },
    generation = ticket
  busy.value = true
  error.value = ''
  try {
    const data = await Review.action(command)
    if (generation !== ticket) return
    pending.value = undefined
    evidence.value = ''
    view.value = data
    emit('changed', command.batchId)
  } catch (e: any) {
    if (generation === ticket) error.value = e?.message || String(e)
  } finally {
    busy.value = false
  }
}
const refreshAndReset = async () => {
  if (busy.value) return
  await load()
  if (view.value) {
    pending.value = undefined
    emit('changed', props.batch.id!)
  }
}
const sync = async () => {
  if (busy.value) return
  const generation = ticket,
    id = props.batch.id!
  busy.value = true
  error.value = ''
  try {
    const data = await Review.sync(id)
    if (generation === ticket) {
      view.value = data
      emit('changed', id)
    }
  } catch (e: any) {
    if (generation === ticket) error.value = e?.message || String(e)
  } finally {
    busy.value = false
  }
}
</script>
<style scoped>
.review-panel {
  min-width: 0;
}
.review-heading,
.review-binding,
.review-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}
.review-heading {
  justify-content: space-between;
}
.review-heading h3 {
  font-size: 19px;
  margin: 8px 0;
}
.review-heading p,
.hint,
small {
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}
.review-binding {
  margin: 18px 0;
  font-size: 14px;
}
.review-stages {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0;
}
.review-stages article {
  background: #f4f7fc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  min-width: 0;
}
.review-stages h4 {
  margin: 0 0 12px;
}
.review-stages strong {
  color: #1e40af;
  font-size: 14px;
}
.evidence,
.review-history-detail dd {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.7;
}
small {
  display: block;
  overflow-wrap: anywhere;
}
.review-form {
  margin: 20px 0;
}
.review-picker {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.review-picker .el-select {
  width: 100%;
}
.review-actions {
  margin: 14px 0;
}
.review-history-detail {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  gap: 12px;
}
.review-history-detail dt {
  color: #64748b;
}
.review-history-detail dd {
  margin: 0;
}
@media (max-width: 720px) {
  .review-stages,
  .review-picker {
    grid-template-columns: 1fr;
  }
  .review-history-detail {
    grid-template-columns: 1fr;
    gap: 4px;
  }
  .review-history-detail dd {
    margin-bottom: 12px;
  }
}
</style>
