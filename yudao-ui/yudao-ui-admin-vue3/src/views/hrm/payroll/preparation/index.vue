<template>
  <div class="preparation-page">
    <ContentWrap>
      <div class="heading"
        ><div><h2>薪酬资料准备总览</h2><p>查看真实资料状态，定位待确认口径与来源缺口。</p></div>
        <el-button :loading="loading" @click="load">刷新状态</el-button></div
      >
      <el-alert
        title="本页汇总资料登记与预检状态，尚不提供工资计算。契约确认、规则确认或格式通过，均不代表人员、金额和来源完整性已经核实。"
        type="warning"
        :closable="false"
      />
      <p v-if="data" class="updated" data-testid="updated"
        >更新时间：{{ new Date(data.generatedAt).toLocaleString('zh-CN') }}</p
      >
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
      <div v-if="!data && !loading" class="mt-3">暂未取得状态，请刷新重试。</div>
      <div v-loading="loading" v-if="data" class="cards">
        <div v-for="card in cards" :key="card.key" class="card" :data-testid="'card-' + card.key">
          <h3>{{ card.title }}</h3>
          <template v-if="data[card.key].authorized">
            <strong>{{ data[card.key].counts?.[card.metric] ?? '—' }}</strong
            ><span>{{ card.unit }}</span>
            <p>{{ card.subtitle }} {{ data[card.key].counts?.total ?? 0 }} {{ card.totalUnit }}</p>
            <el-button link type="primary" @click="navigate(card.path)">{{
              card.action
            }}</el-button>
          </template>
          <p v-else class="unauthorized">未授权查看此板块</p>
          <p class="note">{{ card.note }}</p>
        </div>
      </div>
    </ContentWrap>
    <template v-if="data">
      <ContentWrap
        ><div class="section-title"
          ><h3>各模块需求与首期范围</h3><el-tag type="info">需求确认与范围决定分别统计</el-tag></div
        >
        <template v-if="data.requirements.authorized">
          <div class="caption"
            >首期纳入 {{ data.requirements.counts?.inScope }} 项，其中未确认
            {{ data.requirements.counts?.inScopeUnconfirmed }} 项；范围未决定
            {{ data.requirements.counts?.undecided }} 项，暂缓
            {{ data.requirements.counts?.deferred }} 项。</div
          >
          <el-table :data="data.requirements.modules || []" border data-testid="module-table">
            <el-table-column prop="name" label="模块" min-width="140" />
            <el-table-column prop="total" label="已登记" width="90" />
            <el-table-column prop="pending" label="待确认" width="90" />
            <el-table-column prop="disputed" label="异议" width="80" />
            <el-table-column prop="confirmed" label="已确认" width="90" />
            <el-table-column prop="inScope" label="首期纳入" width="100" />
            <el-table-column prop="inScopeUnconfirmed" label="首期尚未确认" width="120" />
            <el-table-column prop="undecided" label="范围未决定" width="110" />
            <el-table-column label="核对" width="110"
              ><template #default="{ row }"
                ><el-button
                  link
                  type="primary"
                  @click="navigate('/hrm/payroll-requirements', { module: row.code })"
                  >查看需求</el-button
                ></template
              ></el-table-column
            >
          </el-table>
          <el-empty
            v-if="!data.requirements.counts?.total"
            description="当前租户尚未登记候选需求；请在需求征集页登记并填写实际范围。"
          /> </template
        ><el-empty v-else description="未授权查看需求和范围资料" />
      </ContentWrap>
      <div class="two-columns">
        <ContentWrap
          ><h3>来源登记状态</h3
          ><template v-if="data.sources.authorized">
            <div class="caption"
              >待核实 {{ data.sources.counts?.pending }} 项；待补齐
              {{ data.sources.counts?.gap }}
              项。状态来自来源负责人登记，不推定各来源已完整覆盖工资期间。</div
            >
            <el-table :data="data.sources.sources || []" border data-testid="source-table">
              <el-table-column prop="code" label="编号" width="90" /><el-table-column
                prop="name"
                label="来源"
                min-width="150"
              />
              <el-table-column label="登记状态" width="110"
                ><template #default="{ row }"
                  ><el-tag
                    :type="
                      row.readiness === 1 ? 'success' : row.readiness === 2 ? 'danger' : 'warning'
                    "
                    >{{ readinessLabels[row.readiness] || '状态待核实' }}</el-tag
                  ></template
                ></el-table-column
              >
              <el-table-column label="核对" width="110"
                ><template #default="{ row }"
                  ><el-button
                    link
                    type="primary"
                    @click="navigate('/hrm/payroll-requirements', { sourceCode: row.code })"
                    >关联需求</el-button
                  ></template
                ></el-table-column
              > </el-table
            ><el-empty
              v-if="!data.sources.counts?.total"
              description="当前租户尚未登记来源"
            /> </template
          ><el-empty v-else description="未授权查看来源目录" />
        </ContentWrap>
        <ContentWrap
          ><h3>字段与规则版本</h3>
          <div
            v-for="section in versionSections"
            :key="section.key"
            class="version-box"
            :data-testid="'versions-' + section.key"
          >
            <h4>{{ section.title }}</h4
            ><template v-if="data[section.key].authorized">
              <p
                >草稿 {{ data[section.key].counts?.draft }} · 已确认
                {{ data[section.key].counts?.confirmed }} · 已停用
                {{ data[section.key].counts?.retired }}</p
              >
              <el-button link type="primary" @click="navigate(section.path)">{{
                section.action
              }}</el-button> </template
            ><p v-else>未授权查看此板块</p>
          </div>
          <p class="note"
            >统计全部版本，包括历史期间。已确认规则版本数不表示当前期间有完整可执行的规则。</p
          >
          <el-divider /><h4>进入正式核算前需要补齐</h4>
          <p class="note"
            >实际主体与人员范围、工资期间和截止、来源完整性、金额精度与舍入、HR/财务认可的预期样例。请在需求、契约和规则页面登记这些资料。</p
          >
        </ContentWrap>
      </div>
      <ContentWrap
        ><div class="section-title"
          ><h3>我最近的预检批次</h3><el-tag type="info">仅本人 · 最近 5 批</el-tag></div
        >
        <template v-if="data.batches.authorized">
          <p class="caption"
            >共 {{ data.batches.counts?.total }} 批，格式通过
            {{ data.batches.counts?.passed }} 批，发现问题
            {{
              data.batches.counts?.failed
            }}
            批。通过只表示该文件符合已确认字段契约。问题数包含问题行及文件级错误。</p
          >
          <el-table :data="data.batches.recentBatches || []" border data-testid="batch-table">
            <el-table-column prop="id" label="批次" width="100" /><el-table-column
              prop="sourceCode"
              label="来源"
              width="100"
            />
            <el-table-column label="契约版本" width="100"
              ><template #default="{ row }">V{{ row.contractVersion }}</template></el-table-column
            >
            <el-table-column label="声明期间" min-width="215"
              ><template #default="{ row }"
                >{{ row.periodStart }} 至 {{ row.periodEnd }}</template
              ></el-table-column
            >
            <el-table-column prop="rowCount" label="总行数" width="85" /><el-table-column
              prop="validCount"
              label="有效行"
              width="85"
            /><el-table-column prop="errorCount" label="问题数" width="85" />
            <el-table-column label="预检状态" width="115"
              ><template #default="{ row }"
                ><el-tag :type="row.status === 0 ? 'success' : 'danger'">{{
                  row.status === 0 ? '格式通过' : '发现问题'
                }}</el-tag></template
              ></el-table-column
            > </el-table
          ><el-empty
            v-if="!data.batches.counts?.total"
            description="尚无本人预检批次；可在数据接入页上传合成或脱敏样本。"
          />
          <el-button class="mt-3" @click="navigate('/hrm/payroll-intake')"
            >查看本人批次与行级问题</el-button
          > </template
        ><el-empty v-else description="未授权查看本人预检批次" />
      </ContentWrap>
    </template>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as Api from '@/api/hrm/payroll/preparation'
import { readinessLabels } from '@/api/hrm/payroll/requirements'
defineOptions({ name: 'HrmPayrollPreparation' })
const data = ref<Api.Summary>()
const loading = ref(false)
const error = ref('')
const router = useRouter()
const cards: {
  key: 'requirements' | 'sources' | 'contracts' | 'rules' | 'batches'
  title: string
  metric: string
  unit: string
  subtitle: string
  totalUnit: string
  path: string
  action: string
  note: string
}[] = [
  {
    key: 'requirements',
    title: '需求待确认',
    metric: 'pending',
    unit: '项',
    subtitle: '已登记',
    totalUnit: '项需求',
    path: '/hrm/payroll-requirements',
    action: '核对需求与范围',
    note: '异议与首期范围另行统计。'
  },
  {
    key: 'sources',
    title: '来源已登记就绪',
    metric: 'ready',
    unit: '项',
    subtitle: '已登记',
    totalUnit: '项来源',
    path: '/hrm/payroll-requirements',
    action: '核对来源',
    note: '来源就绪独立于字段契约确认。'
  },
  {
    key: 'contracts',
    title: '字段契约已确认',
    metric: 'confirmed',
    unit: '版',
    subtitle: '累计',
    totalUnit: '版契约',
    path: '/hrm/payroll-intake',
    action: '查看字段契约',
    note: '包含不同来源和历史版本。'
  },
  {
    key: 'rules',
    title: '规则口径已确认',
    metric: 'confirmed',
    unit: '版',
    subtitle: '累计',
    totalUnit: '版口径',
    path: '/hrm/payroll-rules',
    action: '核对规则与样例',
    note: '规则台账尚未接入正式计算。'
  },
  {
    key: 'batches',
    title: '本人预检发现问题',
    metric: 'failed',
    unit: '批',
    subtitle: '本人累计',
    totalUnit: '批预检',
    path: '/hrm/payroll-intake',
    action: '核对预检结果',
    note: '保留历史结果，其他人的批次不计入。'
  }
]
const versionSections: {
  key: 'contracts' | 'rules'
  title: string
  path: string
  action: string
}[] = [
  { key: 'contracts', title: '字段契约版本', path: '/hrm/payroll-intake', action: '核对契约' },
  { key: 'rules', title: '业务口径版本', path: '/hrm/payroll-rules', action: '核对参数与样例' }
]
const navigate = (path: string, query?: Record<string, string>) => router.push({ path, query })
const load = async () => {
  loading.value = true
  error.value = ''
  data.value = undefined
  try {
    data.value = await Api.summary()
  } catch {
    error.value = '未能取得最新资料状态，请刷新重试。'
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
<style scoped>
.heading,
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.heading h2 {
  margin: 0;
}
.heading p,
.updated,
.caption,
.note {
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}
.cards {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}
.card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 16px;
}
.card h3 {
  font-size: 15px;
  margin: 0 0 12px;
}
.card strong {
  font-size: 30px;
  color: var(--el-color-primary);
  margin-right: 8px;
}
.note {
  font-size: 13px;
}
.unauthorized {
  color: var(--el-text-color-secondary);
}
.two-columns {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr);
  gap: 16px;
}
.version-box {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 12px;
}
@media (max-width: 1280px) {
  .cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 800px) {
  .cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .two-columns {
    grid-template-columns: minmax(0, 1fr);
  }
}
@media (max-width: 500px) {
  .cards {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
