<template>
  <ContentWrap class="review-page">
    <h1 tabindex="-1" id="review-heading">薪酬需求评审工作台 · {{ modules[module] }}</h1>
    <el-alert
      title="示例数据，仅用于讨论。意见与评审记录真实保存；演示核算、审批及工资条只作用于虚构样例。"
      type="warning"
      :closable="false"
    />
    <div class="toolbar">
      <el-select
        aria-label="评审项目"
        :model-value="projectId"
        placeholder="选择有权评审项目"
        @change="selectProject"
      >
        <el-option v-for="p in projects" :key="p.id" :label="`${p.id} · ${p.name}`" :value="p.id" />
      </el-select>
      <el-button @click="load">刷新工作台</el-button>
      <el-button @click="central">集中清单</el-button>
      <el-button @click="router.push({ path: '/hrm-payroll/governance', query: { projectId } })"
        >业务决议与发布</el-button
      >
      <el-button @click="openSourceWorkbench">实际来源与核验</el-button>
      <el-button @click="original">原有正式业务入口</el-button>
    </div>
    <p
      >{{ project?.name || '未选择项目' }} · 项目 {{ projectId || '待选择' }} · 范围版本
      {{ project?.projectScopeVersion ?? '尚未定义' }}</p
    >
    <p>范围：{{ project?.scopeJson || '尚未定义或当前权限不可见；不据此推定全项目范围' }}</p>
    <nav aria-label="十视图评审导航" class="view-nav">
      <el-button
        v-for="m in order"
        :key="m"
        :type="m === module ? 'primary' : 'default'"
        @click="go(m)"
        >{{ modules[m] }}</el-button
      >
    </nav>
    <div class="toolbar">
      <el-button :disabled="step === 0" @click="go(order[step - 1])">上一步</el-button>
      <span>第 {{ step + 1 }}/10 视图</span>
      <el-button :disabled="step === 9" @click="go(order[step + 1])">下一步</el-button>
      <el-button v-for="m in view.related" :key="m" link @click="go(m)"
        >关联：{{ modules[m] }}</el-button
      >
    </div>
    <p>{{ view.purpose }}</p>
    <section aria-label="虚构业务展示">
      <h2>业务场景与字段</h2>
      <p
        >{{ demo.employee }} · {{ demo.department }} ·
        {{ demo.period }}；全为虚构，未绑定员工档案。</p
      >
      <div class="wide-table" tabindex="0" aria-label="示例业务表，左右方向可横向滚动">
        <table
          ><thead
            ><tr
              ><th v-for="c in view.columns" :key="c">{{ c }}</th></tr
            ></thead
          >
          <tbody
            ><tr v-for="(row, i) in view.rows" :key="i"
              ><td v-for="(cell, k) in row" :key="k">{{ cell }}</td></tr
            ></tbody
          ></table
        >
      </div>
      <p v-for="note in view.notes" :key="note">{{ note }}</p>
      <div :id="`field-${view.field}`" tabindex="-1" class="anchor">
        <strong>{{ view.fieldLabel }}</strong>
        <el-button :disabled="!canCreate" @click="newOpinion('FIELD', `field-${view.field}`)"
          >补充字段说明：{{ view.fieldLabel }}</el-button
        >
        <el-button :disabled="!canCreate" @click="newOpinion('FIELD', `field-${view.field}`)"
          >这个字段从哪里来</el-button
        >
      </div>
      <div id="operation-simulate" tabindex="-1" class="toolbar anchor">
        <el-button @click="simulate">演示核算</el-button
        ><el-button @click="demoState = '演示复核完成，仅页面内存状态；未提交正式审批'"
          >演示复核</el-button
        >
        <el-button @click="slipVisible = true">预览示例工资条</el-button>
        <el-button :disabled="!canCreate" @click="newOpinion('OPERATION', 'operation-simulate')"
          >补充操作规则或例外</el-button
        >
      </div>
      <p role="status">{{ demoState }}</p>
      <div id="flow-approval" tabindex="-1" class="anchor"
        >示例流程：准备 → 试算 → 复核 → 发放讨论 → 归档讨论
        <el-button :disabled="!canCreate" @click="newOpinion('FLOW', 'flow-approval')"
          >补充流程角色与异常</el-button
        >
      </div>
    </section>
    <section v-if="module === 'OV' || module === 'RPT'" aria-label="虚构图表示例">
      <h2>{{ module === 'OV' ? '虚构应发期间趋势' : '虚构用工成本结构' }}</h2>
      <p>示例图仅讨论口径；10月应发11750、成本13850，与主样例一致。历史趋势不是实际工资数据。</p>
      <div
        v-for="item in module === 'OV'
          ? demoTrend.map((x) => ({ name: x.period, amount: x.gross }))
          : demoCostStructure"
        :key="item.name"
        class="source-card"
      >
        <strong>{{ item.name }} · {{ item.amount }} 元</strong
        ><div class="demo-bar-track"
          ><div class="demo-bar" :style="{ width: `${(item.amount / 14000) * 100}%` }"></div
        ></div>
      </div>
    </section>
    <section
      v-if="module === 'PLAN'"
      id="operation-adjustment"
      tabindex="-1"
      aria-label="虚构调薪表单"
    >
      <h2>虚构薪档与调薪比对</h2
      ><p>当前V1：TEST-A/TEST-01，基本10000元；V2仅为下期讨论候选，主链仍用V1。</p>
      <div class="toolbar"
        ><span>{{ demo.employee }}</span
        ><el-input-number
          v-model="demoAdjustment"
          aria-label="示例调薪金额"
          :min="0"
          :precision="2"
        /><el-button
          @click="
            demoState = `示例V2新金额${demoAdjustment}元，2026-11-01拟生效；仅页面预览，不保存正式档案或提交审批，本期仍10000元。`
          "
          >预览演示调薪</el-button
        ><el-button :disabled="!canCreate" @click="newOpinion('OPERATION', 'operation-adjustment')"
          >补充调薪操作意见</el-button
        ></div
      ><p>{{ demoState }}</p>
    </section>
    <section aria-label="候选功能讨论场景">
      <h2>逐项功能场景与例外（示例）</h2>
      <div
        v-for="topic in reviewTopics[module]"
        :id="topic.anchor"
        :key="topic.code"
        tabindex="-1"
        class="source-card anchor"
      >
        <strong>{{ topic.code }} · {{ topic.title }}</strong
        ><p>正常/预期：{{ topic.normal }}</p
        ><p>异常/待确认：{{ topic.exception }}</p>
        <el-button :disabled="!canCreate" @click="newOpinion('FIELD', topic.anchor)"
          >围绕{{ topic.code }}提意见</el-button
        >
      </div>
    </section>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="loading" role="status">正在读取真实评审数据，未返回内容不推定为空或通过。</p>
    <section id="module" tabindex="-1" class="anchor" aria-label="页面内功能需求征集">
      <h2>功能需求与意见征集（真实保存）</h2>
      <div class="toolbar"
        ><el-button :disabled="!canCreate" type="primary" @click="newOpinion('MODULE', 'module')"
          >补充本模块需求</el-button
        >
        <el-button :disabled="!capabilities.FULL_EDIT" @click="initialize"
          >初始化46项候选模板</el-button
        >
        <el-button
          v-hasPermi="['hrm:payroll:requirement:export']"
          :disabled="!projectId"
          @click="download"
          >导出当前筛选评审清单</el-button
        >
        <el-button
          v-hasPermi="['hrm:payroll:requirement:assign']"
          :disabled="!selected.length"
          @click="batchVisible = true"
          >批量分派所选需求</el-button
        >
      </div>
      <div class="toolbar">
        <el-input
          v-model="filters.text"
          aria-label="查找需求"
          placeholder="编号或功能文本"
          clearable
          @keyup.enter="search"
        />
        <el-select
          v-model="filters.status"
          aria-label="评审状态筛选"
          clearable
          placeholder="全部评审状态"
          ><el-option v-for="(name, key) in statuses" :key="key" :label="name" :value="key"
        /></el-select>
        <el-select
          v-model="filters.decision"
          aria-label="范围决策筛选"
          clearable
          placeholder="全部范围决策"
          ><el-option v-for="(name, key) in decisions" :key="key" :label="name" :value="key"
        /></el-select>
        <el-select v-model="sort" aria-label="排序"
          ><el-option label="编号升序" value="code" /><el-option
            label="编号降序"
            value="-code" /><el-option label="版本降序" value="-version"
        /></el-select>
        <el-button @click="search">应用筛选</el-button
        ><el-button @click="clearFilters">清除筛选</el-button>
      </div>
      <div v-if="!loading && !error && !rows.length" role="status"
        >当前筛选暂无可见需求；不代表全项目无需求。</div
      >
      <div v-if="!loading && !error" class="wide-table" tabindex="0" aria-label="真实需求清单">
        <el-table :data="rows" row-key="id" @selection-change="selected = $event">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="id" label="需求ID" width="85" />
          <el-table-column prop="code" label="编号" width="130" />
          <el-table-column prop="description" label="功能/意见" min-width="240" />
          <el-table-column label="功能评审" width="110"
            ><template #default="s"
              >{{ statuses[s.row.status]
              }}{{
                s.row.status === 'CONFIRMED' && !s.row.currentConfirmationValid
                  ? '（当前绑定失效）'
                  : ''
              }}</template
            ></el-table-column
          >
          <el-table-column label="范围决策" width="105"
            ><template #default="s">{{
              decisions[s.row.scopeDecision || 'CANDIDATE']
            }}</template></el-table-column
          >
          <el-table-column label="来源" width="100"
            ><template #default="s">{{ readiness[s.row.readiness] }}</template></el-table-column
          >
          <el-table-column label="实施" width="110"
            ><template #default="s">{{
              implementations[parse(s.row.implementationJson).state || 'UNSCHEDULED']
            }}</template></el-table-column
          >
          <el-table-column prop="version" label="版本" width="70" />
          <el-table-column label="操作" min-width="380"
            ><template #default="s">
              <el-button link :disabled="!allowed(s.row, 'EDIT')" @click="edit(s.row)"
                >编辑意见</el-button
              >
              <el-button link @click="showHistory(s.row)">历史/比较</el-button>
              <el-button link @click="returnTo(s.row)">返回原位置</el-button>
              <el-button
                v-hasPermi="['hrm:payroll:requirement:scope-decide']"
                link
                :disabled="!allowed(s.row, 'SCOPE_DECIDE')"
                @click="openAction(s.row, 'decision')"
                >范围决策</el-button
              >
              <el-button
                link
                :disabled="!allowed(s.row, 'EDIT')"
                @click="openAction(s.row, 'implementation')"
                >实施关联</el-button
              >
              <el-button
                link
                :disabled="!allowed(s.row, 'EDIT')"
                @click="openAction(s.row, 'collaboration')"
                >会议与站内提醒</el-button
              >
              <el-button
                v-hasPermi="['hrm:payroll:requirement:confirm']"
                link
                :disabled="!allowed(s.row, 'CONFIRM') || s.row.status === 'CONFIRMED'"
                @click="confirm(s.row)"
                >本人确认</el-button
              >
            </template></el-table-column
          >
        </el-table>
      </div>
      <el-pagination
        v-if="!loading && !error"
        v-model:current-page="pageNo"
        :page-size="10"
        layout="prev, pager, next, total"
        :total="total"
        @current-change="load"
      />
    </section>
    <section aria-label="数据来源征集">
      <h2>依赖数据来源与盘点</h2>
      <p>候选目录是征集线索，不能冒充实际已提供来源或质量通过。实际来源需按权限另行登记与核验。</p>
      <div v-for="source in candidates" :key="source.id" class="source-card"
        ><strong>{{ source.id }} · {{ source.name }}</strong
        ><p>{{ source.fields.join('、') }}；候选确认方：{{ source.owner }}</p></div
      >
      <label
        >当前可见实际来源
        <el-select
          v-model="sourceId"
          aria-label="选择实际来源"
          clearable
          placeholder="无来源时保持缺口"
          ><el-option
            v-for="source in sources"
            :key="source.id"
            :label="`${source.id} · ${source.data.name}`"
            :value="source.id" /></el-select
      ></label>
      <p v-if="sourceId">{{ sourceDescription }}</p>
      <el-button @click="openSourceWorkbench">维护实际来源、映射和QS/QC</el-button>
    </section>
    <section aria-label="我的站内提醒">
      <h2>我的站内提醒（无外发）</h2>
      <div v-for="(note, i) in reminders" :key="i" class="source-card">
        <strong>{{ note.code }} · 需求{{ note.requirementId }}</strong
        ><p>{{ note.reference }} · {{ note.time }}</p>
        <el-button @click="go(note.module, note.anchor)">返回提醒原位置</el-button>
      </div>
      <p v-if="!loading && !error && !reminders.length"
        >当前有权范围内暂无发给我的站内提醒；会议纪要及全部协作历史可在需求历史中查看。</p
      >
    </section>
    <section aria-label="真实统计与下钻">
      <h2>范围、确认及来源统计（服务器集合）</h2>
      <p
        >U全部需求（含移除）；E未移除；I纳入本期；C当前确认有效；T暂缓；D有异议。暂缓与异议仍计入有效集合，范围决策不代替确认。</p
      >
      <p v-if="stats">{{ stats.label }} · 筛选条件 {{ JSON.stringify(stats.filters) }}</p>
      <div v-if="stats" class="stats-grid">
        <button
          v-for="key in ['U', 'E', 'I', 'C', 'T', 'D']"
          :key="key"
          class="stat"
          @click="drill(key)"
          >{{ key }} · {{ stats.counts[key] }} · 查看集合</button
        >
        <div v-for="key in ratioKeys" :key="key" class="stat"
          >{{ ratioLabels[key] }}：{{ stats[key].label }}</div
        >
      </div>
      <p v-if="stats"
        >去重实际来源 {{ stats.sourceCount }}；有效不适用 {{ stats.exempt }}；未决问题
        {{ stats.unresolvedIssues }}，逾期
        {{ stats.overdueIssues }}。关系和需求缺口以独立ID计数。</p
      >
      <p v-if="stats?.projectTotals"
        >全项目功能确认 {{ stats.projectTotals.confirmation.label }}，本期确认
        {{ stats.projectTotals.includedConfirmation.label }}；当前筛选不改变全项目门槛。</p
      >
      <p
        >当前统计不是发布成功或已实施证明；业务决议、发布检查与六表快照请使用独立入口核实。受限账号只展示可见集合，隐藏对象数量不输出。</p
      >
      <el-button @click="drill('L')" :disabled="!stats">查看关系与缺口L</el-button>
    </section>
  </ContentWrap>
  <el-dialog
    class="hrm-review-dialog"
    v-model="formVisible"
    title="页面意见与业务场景"
    width="min(880px, 96vw)"
    :close-on-click-modal="false"
    @closed="resetFocus"
  >
    <p
      >项目 {{ form.projectId }} · {{ form.module }} · {{ business.targetType }} ·
      {{
        business.anchor
      }}。草稿可逐步补齐；评审中须场景/期望，确认还须角色/目标、正常异常、规则/单位/精度/有效期/例外、验收依据。</p
    >
    <el-alert v-if="saveError" :title="saveError" type="error" :closable="false" />
    <details v-if="conflictLatest"
      ><summary>最新保存内容（与保留输入人工合并）</summary
      ><pre>{{ JSON.stringify(conflictLatest, null, 2) }}</pre>
    </details>
    <el-form label-position="top">
      <el-form-item label="功能说明" :error="opinionErrors.description ? '请填写功能说明' : ''"
        ><el-input
          v-model="form.description"
          aria-label="功能说明"
          type="textarea"
          maxlength="1000"
      /></el-form-item>
      <div class="form-grid"
        ><el-form-item
          v-for="field in businessFields"
          :key="field.key"
          :label="field.label"
          :error="opinionErrors[field.key] ? `请填写${field.label}` : ''"
          ><el-input
            v-model="business[field.key]"
            :aria-label="field.label"
            type="textarea" /></el-form-item
      ></div>
      <ScopeFields v-if="form.scopeMode === 'SUBSET'" v-model="form.scopeJson" />
      <el-form-item label="范围方式"
        ><el-select v-model="form.scopeMode" aria-label="范围方式"
          ><el-option label="继承项目范围" value="INHERIT" /><el-option
            label="显式子集"
            value="SUBSET" /></el-select
      ></el-form-item>
      <el-form-item label="来源适用性"
        ><el-select v-model="form.sourceApplicability" aria-label="来源适用性"
          ><el-option label="适用" value="APPLICABLE" /><el-option
            label="不适用"
            value="NOT_APPLICABLE" /></el-select
      ></el-form-item>
      <el-form-item
        v-if="form.sourceApplicability === 'NOT_APPLICABLE'"
        label="不适用理由"
        :error="opinionErrors.applicabilityReason ? '请填写至少8个字符的具体不适用理由' : ''"
        ><el-input v-model="form.applicabilityReason" aria-label="不适用理由" type="textarea"
      /></el-form-item>
      <el-form-item
        label="规则引用版本"
        :error="opinionErrors.ruleVersion ? '请填写规则引用版本' : ''"
        ><el-input v-model="form.ruleVersion" aria-label="规则引用版本"
      /></el-form-item>
      <el-form-item label="验收条件" :error="opinionErrors.acceptance ? '请填写验收条件' : ''"
        ><el-input v-model="form.acceptance" aria-label="验收条件" type="textarea"
      /></el-form-item>
      <el-form-item
        label="依据或异议说明"
        :error="opinionErrors.evidence ? '请填写依据或异议说明' : ''"
        ><el-input v-model="form.evidence" aria-label="依据或异议说明" type="textarea"
      /></el-form-item>
      <el-form-item label="功能评审状态"
        ><el-select v-model="form.status" aria-label="功能评审状态"
          ><el-option label="待确认（草稿）" value="PENDING" /><el-option
            label="评审中"
            value="REVIEWING" /><el-option label="有异议" value="DISPUTED" /></el-select
      ></el-form-item>
      <template v-if="form.status === 'DISPUTED'"
        ><el-form-item
          label="异议责任人ID"
          :error="opinionErrors.disputeOwnerId ? '请填写异议责任人ID' : ''"
          ><el-input-number
            v-model="business.disputeOwnerId"
            aria-label="异议责任人ID"
            :min="1" /></el-form-item
        ><el-form-item label="异议下一步" :error="opinionErrors.nextStep ? '请填写异议下一步' : ''"
          ><el-input v-model="business.nextStep" aria-label="异议下一步" /></el-form-item
      ></template>
    </el-form>
    <template #footer
      ><div class="dialog-actions"
        ><el-button @click="formVisible = false">取消</el-button
        ><el-button :disabled="!form.id" @click="reloadConflict"
          >读取最新版本（保留当前输入）</el-button
        ><el-button type="primary" :loading="saving" @click="saveOpinion">保存意见</el-button></div
      ></template
    >
  </el-dialog>
  <el-dialog
    class="hrm-review-dialog"
    v-model="actionVisible"
    :title="actionTitles[actionKind]"
    width="min(680px, 96vw)"
    :close-on-click-modal="false"
  >
    <p>需求 {{ active?.id }} · 当前版本 {{ active?.version }}；操作独立留历史，不代替功能确认。</p
    ><el-alert v-if="saveError" :title="saveError" type="error" :closable="false" />
    <el-form label-position="top">
      <template v-if="actionKind === 'decision'">
        <el-form-item label="范围决策"
          ><el-select v-model="action.decision" aria-label="范围决策"
            ><el-option
              v-for="(name, key) in decisions"
              :key="key"
              :value="key"
              :label="name" /></el-select
        ></el-form-item>
        <template v-if="action.decision === 'DEFERRED'"
          ><el-form-item label="暂缓责任人ID"
            ><el-input-number
              v-model="action.responsibleId"
              aria-label="暂缓责任人ID"
              :min="1" /></el-form-item
          ><el-form-item label="重新评估条件"
            ><el-input v-model="action.reevaluate" aria-label="重新评估条件" /></el-form-item
        ></template>
        <template v-if="action.decision === 'REMOVED'"
          ><el-form-item label="移除类型"
            ><el-select v-model="action.removalKind" aria-label="移除类型"
              ><el-option label="重复" value="DUPLICATE" /><el-option
                label="误录"
                value="MISTAKE" /><el-option
                label="超出边界"
                value="OUT_OF_SCOPE" /></el-select></el-form-item
          ><el-form-item v-if="action.removalKind === 'DUPLICATE'" label="同项目保留项ID"
            ><el-input-number
              v-model="action.replacementId"
              aria-label="同项目保留项ID"
              :min="1" /></el-form-item
        ></template>
      </template>
      <template v-else-if="actionKind === 'implementation'"
        ><el-form-item label="实施状态"
          ><el-select v-model="action.state" aria-label="实施状态"
            ><el-option
              v-for="(name, key) in implementations"
              :key="key"
              :label="name"
              :value="key" /></el-select></el-form-item
        ><el-form-item v-for="field in implementationFields" :key="field.key" :label="field.label"
          ><el-input v-model="action[field.key]" :aria-label="field.label" /></el-form-item
        ><p>只记录人工关联和声明，引用存在不证明验收通过；系统不自动核实外部结论。</p></template
      >
      <template v-else
        ><el-form-item label="协作类型"
          ><el-select v-model="action.kind" aria-label="协作类型"
            ><el-option label="关联会议纪要" value="MEETING" /><el-option
              label="站内提醒"
              value="REMINDER" /></el-select></el-form-item
        ><el-form-item label="纪要或提醒内容引用"
          ><el-input
            v-model="action.reference"
            aria-label="纪要或提醒内容引用"
            type="textarea" /></el-form-item
        ><el-form-item v-if="action.kind === 'REMINDER'" label="站内接收人ID"
          ><el-input-number
            v-model="action.recipientId"
            aria-label="站内接收人ID"
            :min="1" /></el-form-item
        ><p
          >提醒仅保存到需求协作记录，对有权接收人可见；不调用短信、邮件或正式消息外发。</p
        ></template
      >
      <el-form-item label="本次操作依据"
        ><el-input v-model="action.reason" aria-label="本次操作依据" type="textarea"
      /></el-form-item>
    </el-form>
    <template #footer
      ><div class="dialog-actions"
        ><el-button @click="actionVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveAction"
          >保存独立操作</el-button
        ></div
      ></template
    >
  </el-dialog>
  <el-dialog
    class="hrm-review-dialog"
    v-model="historyVisible"
    title="真实历史与版本差异"
    width="min(900px, 96vw)"
  >
    <div class="toolbar"
      ><el-select v-model="compareA" aria-label="比较旧版"
        ><el-option
          v-for="h in histories"
          :key="h.id"
          :label="`版本${h.version}`"
          :value="h.version" /></el-select
      ><el-select v-model="compareB" aria-label="比较新版"
        ><el-option
          v-for="h in histories"
          :key="h.id"
          :label="`版本${h.version}`"
          :value="h.version" /></el-select
    ></div>
    <div class="wide-table"
      ><table
        ><thead
          ><tr><th>字段</th><th>旧版</th><th>新版</th></tr></thead
        ><tbody
          ><tr v-for="diff in differences" :key="diff.key"
            ><td>{{ diff.key }}</td
            ><td>{{ diff.before }}</td
            ><td>{{ diff.after }}</td></tr
          ></tbody
        ></table
      ></div
    >
    <div v-for="h in histories" :key="h.id" class="source-card"
      ><strong>版本{{ h.version }} · 操作人{{ h.actorId }}</strong
      ><pre>{{ JSON.stringify(parse(h.snapshot), null, 2) }}</pre>
    </div>
    <template #footer><el-button @click="historyVisible = false">关闭历史</el-button></template>
  </el-dialog>
  <el-dialog
    class="hrm-review-dialog"
    v-model="drillVisible"
    title="统计集合下钻"
    width="min(760px, 96vw)"
  >
    <pre>{{ drillData }}</pre
    ><template #footer
      ><el-button @click="drillVisible = false">关闭集合</el-button></template
    ></el-dialog
  >
  <el-dialog
    class="hrm-review-dialog"
    v-model="batchVisible"
    title="逐对象批量分派"
    width="min(620px, 96vw)"
    ><p>每条独立检查分派人动作、目标资格和版本；部分失败不撤销其他合法成功。</p
    ><el-input-number v-model="batchReviewer" aria-label="批量评审负责人ID" :min="1" /><el-button
      :loading="saving"
      @click="assignBatch"
      >执行批量分派</el-button
    ><pre>{{ batchResult }}</pre>
  </el-dialog>
  <el-dialog
    class="hrm-review-dialog"
    v-model="slipVisible"
    title="虚构工资条预览"
    width="min(620px, 96vw)"
    ><p>示例数据，仅用于讨论 · {{ demo.employee }} · {{ demo.period }}</p
    ><p
      >应发 {{ result.gross }} 元；个人扣款 {{ result.personal }} 元；税 {{ result.tax }} 元；实发
      {{ result.net }} 元。</p
    ><p>单位成本 {{ result.cost }} 元。未产生真实工资条、投递、审批或付款。</p
    ><template #footer
      ><el-button @click="slipVisible = false">关闭工资条预览</el-button></template
    ></el-dialog
  >
</template>

<script setup lang="ts">
import { nextTick } from 'vue'
import { opinionMissing, failureGuidance } from './validation'
import { checkPermi } from '@/utils/permission'
import * as api from '@/api/hrm/payroll/requirement'
import * as review from '@/api/hrm/payroll/review'
import { listWorkbench, type WorkbenchRecord } from '@/api/hrm/payroll/workbench'
import { payrollModules as modules, sourcesForModules, type PayrollModule } from '../requirements'
import {
  demo,
  result,
  calculateDemo,
  reviewOrder as order,
  reviewViews,
  reviewTopics,
  demoTrend,
  demoCostStructure
} from './demo'
import ScopeFields from '../requirement/ScopeFields.vue'
const route = useRoute(),
  router = useRouter(),
  message = useMessage()
const module = computed<PayrollModule>(() =>
  order.includes(route.query.module as PayrollModule) ? (route.query.module as PayrollModule) : 'OV'
)
const projectId = computed(() => Number(route.query.projectId) || undefined)
const view = computed(() => reviewViews[module.value]),
  step = computed(() => order.indexOf(module.value))
const projects = ref<api.PayrollRequirementProject[]>([]),
  project = computed(() => projects.value.find((p) => p.id === projectId.value))
const rows = ref<any[]>([]),
  sources = ref<WorkbenchRecord[]>([]),
  sourceId = ref<number>(),
  stats = ref<any>(),
  capabilities = ref<Record<string, boolean>>({})
const reminders = ref<any[]>([])
const total = ref(0),
  pageNo = ref(1),
  sort = ref('code'),
  selected = ref<any[]>([]),
  error = ref(''),
  loading = ref(false)
const filters = reactive({ text: '', status: '', decision: '' }),
  applied = ref({ text: '', status: '', decision: '' })
const candidates = computed(() => sourcesForModules([module.value]))
const sourceDescription = computed(() => {
  const s = sources.value.find((s) => s.id === sourceId.value)
  return s
    ? `来源${s.id} · 供给${s.data.supply} · 来源负责人${s.data.ownerId} · 语义/运营/交付${s.data.semanticVersion}/${s.data.operationalVersion}/${s.data.deliveryVersion}`
    : '当前来源不可见或已撤销，请刷新'
})
const canCreate = computed(
  () =>
    !!projectId.value && !!capabilities.value.EDIT && checkPermi(['hrm:payroll:requirement:update'])
)
const statuses: Record<string, string> = {
  PENDING: '待确认',
  REVIEWING: '评审中',
  CONFIRMED: '已确认',
  DISPUTED: '有异议'
}
const decisions: Record<string, string> = {
  CANDIDATE: '候选',
  INCLUDED: '纳入本期',
  DEFERRED: '暂缓',
  REMOVED: '移除作废'
}
const implementations: Record<string, string> = {
  UNSCHEDULED: '未排期',
  DEVELOPING: '开发中',
  ACCEPTANCE_PENDING: '待验收',
  ACCEPTED: '验收通过（人工记录）'
}
const readiness: Record<string, string> = {
  MISSING: '未就绪',
  PARTIAL: '部分就绪',
  READY: '已核验可用',
  EXEMPT: '不适用'
}
const ratioKeys = [
  'confirmation',
  'scopeDisposition',
  'includedConfirmation',
  'readiness',
  'includedReadiness'
]
const ratioLabels: Record<string, string> = {
  confirmation: '功能确认率',
  scopeDisposition: '范围处置率',
  includedConfirmation: '本期确认率',
  readiness: '来源可用率',
  includedReadiness: '本期来源可用率'
}
const parse = (value?: string) => {
  try {
    return value ? JSON.parse(value) : {}
  } catch {
    return {}
  }
}
const params = () => ({
  projectId: projectId.value,
  module: module.value === 'COL' ? undefined : module.value,
  ...Object.fromEntries(Object.entries(applied.value).map(([k, v]) => [k, v || undefined]))
})
let loadTicket = 0
async function load() {
  const ticket = ++loadTicket
  loading.value = true
  error.value = ''
  rows.value = []
  stats.value = undefined
  sources.value = []
  capabilities.value = {}
  reminders.value = []
  selected.value = []
  try {
    const ps = await api.listRequirementProjects()
    if (ticket !== loadTicket) return
    projects.value = ps
    if (!projectId.value) {
      error.value = '请选择有权评审项目；未推定任何范围或批准。'
      return
    }
    const [page, stat, src, caps, inbox] = await Promise.all([
      review.reviewPage({ ...params(), pageNo: pageNo.value, pageSize: 10, sort: sort.value }),
      review.reviewStatistics(params()),
      listWorkbench(projectId.value, 'SOURCE'),
      api.requirementCapabilities(projectId.value),
      review.reviewReminders(projectId.value)
    ])
    if (ticket !== loadTicket) return
    rows.value = page.list
    total.value = page.total
    stats.value = stat
    sources.value = src
    capabilities.value = caps
    reminders.value = inbox
    await nextTick()
    focusAnchor()
  } catch {
    if (ticket === loadTicket)
      error.value = '真实评审数据加载失败：可能无权或接口异常。请重试；未加载内容不显示为零或通过。'
  } finally {
    if (ticket === loadTicket) loading.value = false
  }
}
async function go(m: PayrollModule, anchor?: string) {
  pageNo.value = 1
  Object.assign(filters, { text: '', status: '', decision: '' })
  applied.value = { ...filters }
  if (module.value === m && String(route.query.anchor || '') === String(anchor || '')) {
    await load()
  } else {
    await router.push({
      path: '/hrm-payroll/review',
      query: { projectId: projectId.value, module: m, anchor }
    })
  }
  await nextTick()
  focusAnchor()
}
function selectProject(id: number) {
  formVisible.value = false
  actionVisible.value = false
  historyVisible.value = false
  return router.push({
    path: '/hrm-payroll/review',
    query: { projectId: id, module: module.value }
  })
}
function central() {
  return go('COL')
}
function original() {
  return router.push('/hrm-payroll/overview')
}
function openSourceWorkbench() {
  return router.push({
    path: '/hrm-payroll/requirement',
    query: { projectId: projectId.value, module: module.value, from: route.fullPath }
  })
}
function focusAnchor() {
  const anchor = String(route.query.anchor || '')
  if (/^(module|field-[a-z0-9-]+|operation-[a-z0-9-]+|flow-[a-z0-9-]+)$/.test(anchor)) {
    const el = document.getElementById(anchor)
    el?.scrollIntoView({ block: 'center' })
    el?.focus()
  }
}
function resetFocus() {
  document.getElementById('module')?.focus()
}
function search() {
  pageNo.value = 1
  applied.value = { ...filters }
  load()
}
function clearFilters() {
  Object.assign(filters, { text: '', status: '', decision: '' })
  search()
}
function allowed(row: any, action: string) {
  return (
    row.permittedActions?.includes(action) &&
    (action !== 'EDIT' || checkPermi(['hrm:payroll:requirement:update']))
  )
}
function returnTo(row: any) {
  const c = parse(row.businessContextJson)
  return go(order.includes(c.contextModule) ? c.contextModule : row.module, c.anchor || 'module')
}
async function initialize() {
  if (projectId.value) {
    await api.initializeRequirements(projectId.value)
    await load()
  }
}
async function download() {
  const data = await review.reviewExport(params())
  const url = URL.createObjectURL(
    new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  )
  const a = document.createElement('a')
  a.href = url
  a.download = '当前筛选评审清单.json'
  a.click()
  URL.revokeObjectURL(url)
}
const demoAdjustment = ref(10500)
const demoState = ref('示例尚未执行；不代表真实薪酬状态'),
  slipVisible = ref(false)
function simulate() {
  const x = calculateDemo()
  demoState.value = `示例试算：应发${x.gross}元，个税${x.tax}元，实发${x.net}元；仅页面内存，无正式业务写入。`
}
const conflictLatest = ref<any>()
const formVisible = ref(false),
  form = ref<any>({}),
  business = ref<Record<string, any>>({}),
  saving = ref(false),
  saveError = ref('')
const businessFields = [
  { key: 'role', label: '业务角色' },
  { key: 'scenario', label: '触发场景' },
  { key: 'goal', label: '业务目标' },
  { key: 'expected', label: '期望行为' },
  { key: 'normalCase', label: '正常样例' },
  { key: 'exceptionCase', label: '异常样例' },
  { key: 'calculation', label: '计算或业务规则（不适用须说明）' },
  { key: 'unit', label: '单位（不适用须说明）' },
  { key: 'precision', label: '精度与舍入（不适用须说明）' },
  { key: 'validity', label: '有效期或适用依据' },
  { key: 'ruleException', label: '规则例外与处理' }
]
const opinionErrors = ref<Record<string, string>>({})
async function focusOpinionField(label: string) {
  await nextTick()
  const field = document.querySelector<HTMLElement>(`.hrm-review-dialog [aria-label="${label}"]`)
  field?.scrollIntoView({ block: 'center' })
  field?.focus()
}
function newOpinion(targetType: string, anchor: string) {
  opinionErrors.value = {}
  conflictLatest.value = undefined
  form.value = {
    projectId: projectId.value,
    projectScopeVersion: project.value?.projectScopeVersion,
    code: 'CUSTOM-0000',
    module: module.value,
    scopeMode: 'INHERIT',
    status: 'PENDING',
    priority: 2,
    readiness: 'MISSING',
    sourceApplicability: 'APPLICABLE',
    description: ''
  }
  business.value = { page: '/hrm-payroll/review', contextModule: module.value, targetType, anchor }
  saveError.value = ''
  formVisible.value = true
}
function edit(row: any) {
  opinionErrors.value = {}
  conflictLatest.value = undefined
  form.value = {
    ...row,
    projectScopeVersion: project.value?.projectScopeVersion,
    status: row.status === 'CONFIRMED' ? 'REVIEWING' : row.status
  }
  business.value = {
    page: '/hrm-payroll/review',
    contextModule: row.module,
    targetType: 'MODULE',
    anchor: 'module',
    ...parse(row.businessContextJson)
  }
  saveError.value = ''
  formVisible.value = true
}
async function saveOpinion() {
  opinionErrors.value = opinionMissing(form.value, business.value)
  if (Object.keys(opinionErrors.value).length) {
    saveError.value = `请补齐以下字段：${Object.values(opinionErrors.value).join('、')}。输入已保留。`
    await focusOpinionField(Object.values(opinionErrors.value)[0])
    return
  }
  saving.value = true
  saveError.value = ''
  try {
    if (form.value.projectId !== projectId.value) throw Error('changed project')
    await api.saveRequirement({
      ...form.value,
      businessContextJson: JSON.stringify(business.value)
    })
    formVisible.value = false
    message.success('意见已真实保存')
    await load()
  } catch (error) {
    saveError.value = failureGuidance(error)
  } finally {
    saving.value = false
  }
}
async function reloadConflict() {
  try {
    const latest = (await api.listRequirements(undefined, projectId.value)).find(
      (x) => x.id === form.value.id
    )
    if (latest) {
      conflictLatest.value = latest
      form.value.version = latest.version
      form.value.projectScopeVersion = latest.projectScopeVersion
      saveError.value = `已读取最新版本${latest.version}，输入仍保留；请核对最新功能：${latest.description}，合并后再保存。`
    }
  } catch {
    saveError.value = '读取最新版本失败，输入仍保留。'
  }
}
const actionVisible = ref(false),
  actionKind = ref('decision'),
  active = ref<any>(),
  action = ref<any>({})
const actionTitles: Record<string, string> = {
  decision: '独立范围决策',
  implementation: '独立实施关联',
  collaboration: '会议纪要与受控站内提醒'
}
const implementationFields = [
  { key: 'page', label: '实现页面引用' },
  { key: 'api', label: '接口引用' },
  { key: 'task', label: '开发任务引用' },
  { key: 'evidence', label: '验收证据引用' }
]
function openAction(row: any, kind: string) {
  active.value = row
  actionKind.value = kind
  action.value =
    kind === 'decision'
      ? { decision: row.scopeDecision || 'CANDIDATE', removalKind: 'DUPLICATE' }
      : kind === 'implementation'
        ? { state: 'UNSCHEDULED', ...parse(row.implementationJson) }
        : { kind: 'MEETING' }
  saveError.value = ''
  actionVisible.value = true
}
function context(row: any) {
  return {
    projectId: projectId.value,
    projectScopeVersion: project.value?.projectScopeVersion,
    id: row.id,
    version: row.version,
    reason: '页面评审操作'
  }
}
async function saveAction() {
  saving.value = true
  saveError.value = ''
  try {
    const command = { ...context(active.value), ...action.value }
    const fn =
      actionKind.value === 'decision'
        ? review.scopeDecision
        : actionKind.value === 'implementation'
          ? review.saveImplementation
          : review.saveCollaboration
    await fn(command)
    actionVisible.value = false
    await load()
  } catch {
    saveError.value = '操作未完成：缺必填、无权、引用无效或版本冲突。输入已保留，请核对。'
  } finally {
    saving.value = false
  }
}
const historyVisible = ref(false),
  histories = ref<any[]>([]),
  compareA = ref<number>(),
  compareB = ref<number>()
async function showHistory(row: any) {
  try {
    histories.value = await api.requirementHistory(row.id, projectId.value)
    const v = histories.value.map((h) => h.version).sort((a, b) => a - b)
    compareA.value = v.at(-2) ?? v[0]
    compareB.value = v.at(-1)
    historyVisible.value = true
  } catch {
    message.error('历史读取失败，不能推定无历史')
  }
}
const differences = computed(() => {
  const a = parse(histories.value.find((h) => h.version === compareA.value)?.snapshot),
    b = parse(histories.value.find((h) => h.version === compareB.value)?.snapshot)
  return [...new Set([...Object.keys(a), ...Object.keys(b)])]
    .filter((k) => JSON.stringify(a[k]) !== JSON.stringify(b[k]))
    .map((key) => ({ key, before: JSON.stringify(a[key]), after: JSON.stringify(b[key]) }))
})
const drillVisible = ref(false),
  drillData = ref('')
function drill(key: string) {
  if (stats.value) {
    drillData.value = JSON.stringify(key === 'L' ? stats.value.L : stats.value.sets[key], null, 2)
    drillVisible.value = true
  }
}
const batchVisible = ref(false),
  batchReviewer = ref<number>(),
  batchResult = ref('')
async function assignBatch() {
  saving.value = true
  try {
    batchResult.value = JSON.stringify(
      await review.batchAssign({
        projectId: projectId.value,
        projectScopeVersion: project.value?.projectScopeVersion,
        reviewerId: batchReviewer.value,
        items: selected.value.map((row) => context(row))
      }),
      null,
      2
    )
    await load()
  } catch {
    batchResult.value = '批量请求未完成；未推定任何分派成功。请核对权限和请求。'
  } finally {
    saving.value = false
  }
}
async function confirm(row: any) {
  const missing = opinionMissing(row, parse(row.businessContextJson) || {}, true)
  if (Object.keys(missing).length) {
    message.warning(`确认前请补齐以下字段：${Object.values(missing).join('、')}；尚未产生签字`)
    return
  }
  try {
    await message.confirm(`本人确认需求${row.id}的当前版本${row.version}？不会保存未提交表单。`)
    await api.confirmRequirement(
      row.id,
      row.version,
      projectId.value,
      project.value?.projectScopeVersion
    )
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') message.warning(failureGuidance(error))
  }
}
watch(
  () => route.fullPath,
  () => {
    formVisible.value = false
    actionVisible.value = false
    historyVisible.value = false
    drillVisible.value = false
    batchVisible.value = false
    sourceId.value = undefined
    pageNo.value = 1
    load()
  },
  { immediate: true }
)
</script>
<style scoped>
.review-page {
  min-width: 0;
}
.toolbar,
.view-nav,
:global(.hrm-review-dialog .el-dialog__body) {
  max-height: 55vh;
  overflow-y: auto;
}
.dialog-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  margin: 14px 0;
}
.toolbar .el-select,
.toolbar .el-input {
  width: 210px;
  max-width: 100%;
}
.view-nav .el-button {
  margin-left: 0;
}
.wide-table {
  max-width: 100%;
  overflow-x: auto;
  margin: 12px 0;
}
table {
  border-collapse: collapse;
  min-width: 650px;
  width: 100%;
}
th,
td {
  border: 1px solid var(--el-border-color);
  padding: 10px;
  text-align: left;
  white-space: nowrap;
}
.demo-bar-track {
  background: var(--el-fill-color);
  height: 12px;
  border-radius: 6px;
  margin-top: 8px;
}
.demo-bar {
  background: var(--el-color-primary);
  height: 12px;
  border-radius: 6px;
}
.source-card,
.stat {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 12px;
  margin: 8px 0;
}
.stats-grid,
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.stat {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  text-align: left;
}
.anchor:focus {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 4px;
}
pre {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  max-width: 100%;
}
section {
  margin-top: 28px;
}
p {
  overflow-wrap: anywhere;
}
.dialog-actions {
  justify-content: flex-end;
  margin: 0;
}
@media (max-width: 600px) {
  .stats-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
  .toolbar .el-select,
  .toolbar .el-input {
    width: 100%;
  }
  .dialog-actions .el-button {
    margin-left: 0;
    white-space: normal;
  }
  .review-page :deep(.el-pagination) {
    flex-wrap: wrap;
  }
  .review-page :deep(.el-button) {
    max-width: 100%;
    height: auto;
    min-height: 32px;
    white-space: normal;
  }
}
</style>
