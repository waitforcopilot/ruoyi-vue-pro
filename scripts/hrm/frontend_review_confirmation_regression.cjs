// Actual validation module/save handler with Vue refs and mocked transport.
// These assertions do not replace browser or server-side authorization tests.
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const ui = path.resolve(__dirname, '../../yudao-ui/yudao-ui-admin-vue3')
const ts = require(path.join(ui, 'node_modules/typescript'))
const vue = require(path.join(ui, 'node_modules/vue'))
const source = fs.readFileSync(path.join(ui, 'src/views/hrm/payroll/review/validation.ts'), 'utf8')
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
const moduleContext = { exports: {} }
vm.runInNewContext(compiled, moduleContext)
const { opinionMissing, businessLabels, failureGuidance } = moduleContext.exports
const business = Object.fromEntries(Object.keys(businessLabels).map((key) => [key, 'TEST ' + key]))
const fullRow = {
  id: 1, version: 1, projectId: 10, projectScopeVersion: 1, description: 'TEST review',
  status: 'PENDING', reviewerId: 267, evidence: 'TEST synthetic basis',
  acceptance: 'TEST expected result', ruleVersion: 'TEST-v1',
  sourceApplicability: 'NOT_APPLICABLE', applicabilityReason: '仅调整菜单导航，不消费业务数据'
}
const checks = []
function test(name, body) { body(); checks.push(name) }
test('incomplete draft including exemption reason remains saveable', () => {
  const draft = { description: 'TEST draft', status: 'PENDING', sourceApplicability: 'NOT_APPLICABLE', applicabilityReason: 'N/A' }
  assert.equal(Object.keys(opinionMissing(draft, {})).length, 0)
})
test('reviewing requires scenario and expected behavior', () => {
  const missing = opinionMissing({ ...fullRow, status: 'REVIEWING' }, {})
  assert.deepEqual(Object.keys(missing).sort(), ['expected', 'scenario'])
})
for (const key of Object.keys(businessLabels)) {
  test('confirmation missing business field ' + key, () => {
    const value = { ...business }; delete value[key]
    assert.ok(opinionMissing(fullRow, value, true)[key])
  })
}
for (const key of ['description', 'reviewerId', 'evidence', 'ruleVersion', 'acceptance']) {
  test('confirmation missing record field ' + key, () => {
    assert.ok(opinionMissing({ ...fullRow, [key]: '' }, business, true)[key])
  })
}
for (const reason of [undefined, null, '', '  ', 'N/A', 'n/a', 'NA', '不适用', '仅导航']) {
  test('invalid exemption reason ' + String(reason), () => {
    assert.match(opinionMissing({ ...fullRow, applicabilityReason: reason }, business, true).applicabilityReason, /至少8个字符/)
  })
}
test('valid exemption does not require source owner or mapping', () => {
  assert.equal(Object.keys(opinionMissing(fullRow, business, true)).length, 0)
})
test('applicable source leaves server-owned mapping checks to server', () => {
  assert.equal(Object.keys(opinionMissing({ ...fullRow, sourceApplicability: 'APPLICABLE', applicabilityReason: '' }, business, true)).length, 0)
})
test('dispute requires reviewer, evidence, owner and next step', () => {
  const missing = opinionMissing({ ...fullRow, status: 'DISPUTED', reviewerId: null, evidence: '' }, business)
  assert.deepEqual(Object.keys(missing).sort(), ['disputeOwnerId', 'evidence', 'nextStep', 'reviewerId'])
  assert.equal(Object.keys(opinionMissing({ ...fullRow, status: 'DISPUTED' }, { ...business, disputeOwnerId: 266, nextStep: 'TEST independent follow-up' })).length, 0)
})
const sfc = fs.readFileSync(path.join(ui, 'src/views/hrm/payroll/review/index.vue'), 'utf8')
const handler = sfc.slice(sfc.indexOf('async function saveOpinion()'), sfc.indexOf('async function reloadConflict()'))
assert.ok(handler.startsWith('async function saveOpinion()'))
function harness(error) {
  const calls = [], successes = []
  let currentError = error
  const ctx = {
    exports: {}, opinionMissing, failureGuidance,
    form: vue.ref({ ...fullRow }), business: vue.ref({ ...business }),
    projectId: vue.ref(10), opinionErrors: vue.ref({}), saving: vue.ref(false),
    saveError: vue.ref(''), formVisible: vue.ref(true),
    focusOpinionField: async () => {},
    api: { saveRequirement: async (value) => { calls.push(value); if (currentError) throw currentError } },
    load: async () => {}, message: { success: (value) => successes.push(value) }
  }
  vm.runInNewContext(ts.transpileModule(handler + '\nexports.saveOpinion = saveOpinion;', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, ctx)
  return { ctx, calls, successes, clearError: () => { currentError = null } }
}
;(async () => {
  for (const error of ['Network Error', '版本冲突', '请补齐以下字段：验收条件']) {
    const h = harness(error)
    const before = JSON.stringify(h.ctx.form.value)
    await h.ctx.exports.saveOpinion()
    assert.equal(h.ctx.formVisible.value, true)
    assert.equal(h.ctx.saving.value, false)
    assert.equal(JSON.stringify(h.ctx.form.value), before)
    assert.match(h.ctx.saveError.value, /输入已保留/)
    assert.equal(h.successes.length, 0)
    h.clearError()
    await h.ctx.exports.saveOpinion()
    assert.equal(h.calls.length, 2)
    assert.equal(h.ctx.formVisible.value, false)
    assert.equal(h.successes.length, 1)
    assert.equal(h.calls[1].description, fullRow.description)
    checks.push('retained draft and successful retry: ' + error)
  }
  const invalid = harness(null)
  invalid.ctx.form.value.status = 'DISPUTED'
  await invalid.ctx.exports.saveOpinion()
  assert.equal(invalid.calls.length, 0)
  assert.equal(invalid.ctx.formVisible.value, true)
  assert.ok(invalid.ctx.opinionErrors.value.nextStep)
  checks.push('invalid dispute is blocked before transport')
  console.log(JSON.stringify({ status: 'PASS', count: checks.length, checks }, null, 2))
})().catch((error) => { console.error(error); process.exitCode = 1 })
