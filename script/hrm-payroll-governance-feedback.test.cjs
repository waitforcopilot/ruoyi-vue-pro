// Execute the actual governance SFC script handlers with Vue refs and mocked API boundaries.
// No browser, network, database, copied handler implementation or DOM assertions.
const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const { randomUUID } = require('node:crypto')
const uiRoot = path.resolve(__dirname, '../yudao-ui/yudao-ui-admin-vue3')
const uiRequire = createRequire(path.join(uiRoot, 'package.json'))
const ts = uiRequire('typescript')
const { parse } = uiRequire('vue/compiler-sfc')
const { ref, computed } = uiRequire('vue')
const filename = path.join(uiRoot, 'src/views/hrm/payroll/governance/index.vue')
const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
assert.equal(errors.length, 0)
assert.ok(descriptor.scriptSetup)
// Remove imports only: the rest of the real script (including every handler body) runs unmodified.
const parsed = ts.createSourceFile(filename + '.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const source = parsed.statements.filter((node) => !ts.isImportDeclaration(node)).map((node) => node.getFullText(parsed)).join('\n')
const bindings = ['load', 'openRoot', 'openDecision', 'openAction', 'saveRoot', 'saveDecision', 'saveAction', 'precheck', 'retryPublish', 'publish', 'history', 'readRelease', 'createJob', 'refreshJob', 'claim', 'success', 'error', 'busy', 'actionForm', 'actionOpen', 'rootForm', 'rootOpen', 'decisionForm', 'decisionOpen', 'lastPublish', 'publishReason', 'requestKey', 'exportIds', 'jobs', 'frozen', 'gate', 'projects', 'anchors']
const js = ts.transpileModule(source + '\nglobalThis.subject = {' + bindings.join(',') + '}\n', { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
function harness() {
  const calls = []
  const api = {
    get: async (url, data) => { calls.push({ method: 'get', url, data }); return [] },
    post: async (url, data) => { calls.push({ method: 'post', url, data }); return 1430 },
    download: async () => ({})
  }
  const context = {
    api, ref, computed, crypto: { randomUUID },
    useRouter: () => ({ push() {}, replace() {} }),
    useRoute: () => ({ path: '/hrm-payroll/governance', query: { projectId: 153 } }),
    listRequirementProjects: async () => [{ id: 153, projectScopeVersion: 5, scopeJson: 'TEST scope5' }],
    requirementCapabilities: async () => ({ DECISION_READ: true }),
    checkPermi: () => true,
    download: { excel() {} },
    // Do not mount or start the page's immediate route watcher in the isolated handler test.
    watch() {}, onBeforeUnmount() {}
  }
  vm.runInNewContext(js, context, { filename })
  return { page: context.subject, api, calls }
}
const previousSuccess = '本人动作已保存，生命周期按有效期和当前状态计算'

test('root SIGN success followed by conflicting decision APPROVE rejection never retains prior success', async () => {
  const { page, api, calls } = harness()
  page.openAction('root', { id: 1430, version: 1 }, 'SIGN')
  page.actionForm.value.reason = 'TEST 267 independent SIGN'
  await page.saveAction()
  assert.equal(page.success.value, previousSuccess)
  assert.equal(page.actionOpen.value, false)
  page.openAction('decision', { id: 1426, version: 2 }, 'APPROVE')
  assert.equal(page.success.value, '')
  page.actionForm.value.reason = 'TEST overlap with 1407; preserve this approval basis'
  const input = JSON.stringify(page.actionForm.value)
  api.post = async (url, data) => { calls.push({ method: 'post', url, data }); throw new Error('overlap') }
  await page.saveAction()
  assert.match(page.error.value, /本人动作被拒绝.*不视为批准成功/)
  assert.equal(page.success.value, '')
  assert.equal(page.actionOpen.value, true)
  assert.equal(JSON.stringify(page.actionForm.value), input)
  assert.equal(calls.at(-1).url, '/decision/action')
  assert.equal(calls.at(-1).data.projectId, 153)
  // The template's cancel button only closes actionOpen. It must not reveal old success.
  page.actionOpen.value = false
  assert.equal(page.success.value, '')
  assert.match(page.error.value, /不视为批准成功/)
})

for (const [open, args] of [['openRoot', []], ['openDecision', []], ['openAction', ['decision', { id: 1426, version: 2 }, 'APPROVE']]]) {
  test(open + ' expires old success while keeping the previous refusal visible', () => {
    const { page } = harness()
    page.success.value = previousSuccess
    page.error.value = 'TEST previous refusal must remain visible'
    page[open](...args)
    assert.equal(page.success.value, '')
    assert.equal(page.error.value, 'TEST previous refusal must remain visible')
  })
}

for (const [save, form, dialog, errorPattern] of [
  ['saveRoot', 'rootForm', 'rootOpen', /登记未完成/],
  ['saveDecision', 'decisionForm', 'decisionOpen', /决议草稿未保存/],
  ['saveAction', 'actionForm', 'actionOpen', /不视为批准成功/]
]) {
  test(save + ' clears success before awaiting a rejected request and preserves the entire input', async () => {
    const { page, api } = harness()
    const request = deferred()
    api.post = () => request.promise
    page.success.value = previousSuccess
    page[form].value = { kind: 'decision', id: 1426, reason: 'TEST retained basis', body: 'TEST controlled body' }
    page[dialog].value = true
    const input = JSON.stringify(page[form].value)
    const running = page[save]()
    assert.equal(page.success.value, '')
    assert.equal(page.busy.value, true)
    request.reject(new Error('current scope or authority refused'))
    await running
    assert.equal(page.success.value, '')
    assert.match(page.error.value, errorPattern)
    assert.equal(page[dialog].value, true)
    assert.equal(JSON.stringify(page[form].value), input)
    assert.equal(page.busy.value, false)
    // Retry using the same preserved data can succeed and publish a new, current success.
    api.post = async () => 1431
    await page[save]()
    assert.equal(page.error.value, '')
    assert.notEqual(page.success.value, '')
    assert.equal(page[dialog].value, false)
  })
}

test('failed publish retry expires prior snapshot success without losing request identity or reason', async () => {
  const { page, api } = harness()
  page.lastPublish.value = { projectId: 153, requestKey: 'TEST-one-request', dependencyToken: 'TEST-old-token', reason: 'TEST original basis' }
  const input = JSON.stringify(page.lastPublish.value)
  page.success.value = '固定快照ID：TEST-old'
  api.post = async () => { throw new Error('changed dependency') }
  await page.retryPublish()
  assert.equal(page.success.value, '')
  assert.match(page.error.value, /说明及请求号已保留/)
  assert.equal(JSON.stringify(page.lastPublish.value), input)
})

for (const [method, args] of [
  ['load', []], ['history', [{ id: 1426 }]], ['precheck', []], ['readRelease', [1384]],
  ['createJob', [1384]], ['refreshJob', [{ id: 1429, projectId: 153 }]],
  ['claim', [{ id: 1429, projectId: 153, releaseId: 1384 }]]
]) {
  test(method + ' cannot leave stale mutation success beside a subsequent failed read or download', async () => {
    const { page, api } = harness()
    page.success.value = previousSuccess
    page.jobs.value = [{ id: 1429, projectId: 153, state: 'READY' }]
    page.exportIds.value = '1844'
    api.get = api.post = api.download = async () => { throw new Error('denied') }
    // history already propagates errors; do not change unrelated error handling for this fix.
    if (method === 'history') await assert.rejects(page[method](...args), /denied/)
    else await page[method](...args)
    assert.equal(page.success.value, '')
    if (method === 'claim') assert.equal(page.jobs.value[0].state, 'DENIED')
    if (method === 'refreshJob') assert.equal(page.jobs.value[0].state, 'UNKNOWN')
    if (method === 'createJob') assert.equal(page.exportIds.value, '1844')
  })
}
