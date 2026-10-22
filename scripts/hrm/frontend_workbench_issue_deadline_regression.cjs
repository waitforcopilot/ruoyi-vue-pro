// Actual WorkbenchPanel SFC deadline bindings + save handler, real Vue/Element Plus ElInput identity.
// Compiler-produced v-model update handlers feed raw text into the actual validation and mocked transport.
// No HTTP/DB/native browser. This does not replace the visible create/reopen regression.
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const { createRequire } = require('node:module')
const ui = path.resolve(__dirname, '../../yudao-ui/yudao-ui-admin-vue3')
const fromVue = createRequire(require.resolve(path.join(ui, 'node_modules/vue/package.json')))
const compiler = fromVue('@vue/compiler-dom')
const vue = require(path.join(ui, 'node_modules/vue'))
const { ElInput } = require(path.join(ui, 'node_modules/element-plus'))
const ts = require(path.join(ui, 'node_modules/typescript'))
const sfc = fs.readFileSync(path.join(ui, 'src/views/hrm/payroll/requirement/WorkbenchPanel.vue'), 'utf8')
const template = sfc.slice(sfc.indexOf('<template>') + '<template>'.length, sfc.indexOf('<script setup'))
const ast = compiler.parse(template.slice(0, template.lastIndexOf('</template>')))
const controls = new Map()
function visit(node) {
  if (node.type === 1) {
    const model = node.props.find((p) => p.type === 7 && p.name === 'model')
    if (model && ['draft.issue.dueDate', 'draft.dueDate'].includes(model.exp?.content)) {
      assert.equal(node.tag, 'el-input', 'Deadline must preserve raw text rather than use a normalizing date picker')
      assert.ok(!model.modifiers?.length, 'No trim/number normalization before validation')
      assert.ok(!node.props.some((p) => ['maxlength', 'formatter', 'parser', 'value-format'].includes(p.name)))
      const type = node.props.find((p) => p.type === 6 && p.name === 'type')
      assert.ok(!type || type.value?.content === 'text')
      controls.set(model.exp.content, node.loc.source)
    }
  }
  for (const child of node.children || []) visit(child)
}
visit(ast)
assert.equal(controls.size, 2, 'Both create and reopen must use the checked raw model binding')
function moduleFrom(file) {
  const ctx = { exports: {} }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(ui, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, ctx)
  return ctx.exports
}
const validation = moduleFrom('src/views/hrm/payroll/requirement/issueValidation.ts')
const guidance = moduleFrom('src/views/hrm/payroll/review/validation.ts')
const script = sfc.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
function harness() {
  const calls = []
  let failure = null
  const ctx = {
    exports: {}, ref: vue.ref, computed: vue.computed, watch: () => {}, onMounted: () => {},
    defineProps: () => ({ projectId: 153, scopeVersion: 5, requirements: [] }), defineEmits: () => () => {},
    useMessage: () => ({ success: () => {} }),
    document: { querySelector: () => ({ scrollIntoView: () => {}, focus: () => {} }) },
    require: (name) => {
      if (name === 'vue') return vue
      if (name === './issueValidation') return validation
      if (name === '../review/validation') return guidance
      if (name === '@/api/hrm/payroll/workbench') return {
        listWorkbench: async () => [], workbenchHistory: async () => [],
        workbenchCommand: async (route, payload) => {
          calls.push({ route, payload: JSON.parse(JSON.stringify(payload)) })
          if (failure) throw failure
        }
      }
      if (name === '@/api/system/user') return { getSimpleUserList: async () => [] }
      if (name === '../requirements') return { payrollModules: [] }
      return {}
    }
  }
  vm.runInNewContext(ts.transpileModule(script + '\nexports.s={draft,mode,opened,failed,failureMessage,fieldErrors,saving,createIssue,issueAction,save};', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, ctx)
  return { s: ctx.exports.s, calls, fail: (error) => { failure = error } }
}
function open(h, mode) {
  if (mode === 'create') {
    h.s.createIssue()
    Object.assign(h.s.draft.value.issue, {
      description: 'TEST raw deadline', requirementIds: [1844], sourceIds: [], relationIds: [],
      wholeProject: false, category: 'DATA_READINESS', handlerId: 269, reviewerId: 279
    })
  } else {
    h.s.issueAction({ id: 3531, version: 9, binding: 'TEST current binding', data: {
      category: 'DATA_READINESS', wholeProject: false, requirementIds: [1844], sourceIds: [], handlerId: 269, reviewerId: 279
    } }, 'REOPEN')
    h.s.draft.value.evidence = 'TEST new recurrence evidence'
  }
  h.s.draft.value.reason = 'TEST raw date verification'
}
function bindRaw(h, mode, value) {
  const key = mode === 'create' ? 'draft.issue.dueDate' : 'draft.dueDate'
  const code = compiler.compile(controls.get(key), { mode: 'function' }).code
  const runtime = { ...vue, resolveComponent: (name) => { assert.equal(name, 'el-input'); return ElInput } }
  const render = new Function('Vue', code)(runtime)
  const context = vue.proxyRefs({ draft: h.s.draft })
  const vnode = render(context, [])
  assert.equal(vnode.type, ElInput, 'Compile the actual SFC control as real Element Plus text input')
  assert.equal(vnode.props.type ?? 'text', 'text')
  vnode.props['onUpdate:modelValue'](value)
  assert.equal(mode === 'create' ? h.s.draft.value.issue.dueDate : h.s.draft.value.dueDate, value, 'v-model preserves exact raw input')
}
const checks = []
;(async () => {
  for (const mode of ['create', 'reopen']) {
    for (const date of [undefined, null, '', '2026-13-40', '2026-02-30', '2026-02-29', '1900-02-29', '2026-00-10', '2026-01-00', '2026-04-31', '2026-2-01', '2026-01-011', ' 2026-01-01 ']) {
      const h = harness(); open(h, mode); bindRaw(h, mode, date)
      const original = JSON.stringify(h.s.draft.value)
      await h.s.save()
      assert.equal(h.calls.length, 0)
      assert.equal(h.s.opened.value, true)
      assert.equal(h.s.saving.value, false)
      assert.equal(JSON.stringify(h.s.draft.value), original)
      assert.ok(h.s.fieldErrors.value.dueDate)
      assert.match(h.s.failureMessage.value, /输入已保留/)
      if (date) assert.match(h.s.fieldErrors.value.dueDate, /有效的YYYY-MM-DD/)
      else assert.doesNotMatch(h.s.fieldErrors.value.dueDate, /有效的YYYY-MM-DD/)
      bindRaw(h, mode, '2028-02-29'); await h.s.save()
      assert.equal(h.calls.length, 1)
      assert.equal(h.calls[0].route, mode === 'create' ? '/issue/create' : '/issue/action')
      assert.equal(mode === 'create' ? h.calls[0].payload.issue.dueDate : h.calls[0].payload.dueDate, '2028-02-29')
      assert.equal(h.s.opened.value, false)
      checks.push(`${mode}: raw ${JSON.stringify(date)} rejected and corrected leap date submitted`)
    }
    for (const date of ['2026-10-23', '2024-02-29', '2000-02-29', '2026-02-28']) {
      const h = harness(); open(h, mode); bindRaw(h, mode, date); await h.s.save()
      assert.equal(h.calls.length, 1)
      assert.equal(mode === 'create' ? h.calls[0].payload.issue.dueDate : h.calls[0].payload.dueDate, date)
      checks.push(`${mode}: valid ${date} submitted unchanged`)
    }
    const h = harness(); open(h, mode); bindRaw(h, mode, '2028-02-29')
    h.fail('记录已被更新，请刷新后重试'); await h.s.save()
    assert.equal(h.s.opened.value, true)
    assert.equal(mode === 'create' ? h.s.draft.value.issue.dueDate : h.s.draft.value.dueDate, '2028-02-29')
    assert.match(h.s.failureMessage.value, /输入已保留/)
    checks.push(`${mode}: server rejection retains raw date and dialog`)
  }
  console.log(JSON.stringify({ status: 'PASS', count: checks.length, checks }, null, 2))
})().catch((error) => { console.error(error); process.exitCode = 1 })
