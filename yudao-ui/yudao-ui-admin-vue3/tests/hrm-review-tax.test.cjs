// Run from frontend root: node --test tests/hrm-review-tax.test.cjs
const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync } = require('node:fs')
const { tmpdir } = require('node:os')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const out = mkdtempSync(path.join(tmpdir(), 'hrm-tax-'))
after(() => rmSync(out, { recursive: true, force: true }))
execFileSync(
  process.execPath,
  [
    require.resolve('typescript/bin/tsc'),
    '--ignoreConfig',
    '--target',
    'ES2022',
    '--module',
    'commonjs',
    '--skipLibCheck',
    '--outDir',
    out,
    path.join(root, 'src/views/hrm/payroll/review/tax-model.ts')
  ],
  { cwd: root, stdio: 'pipe' }
)
const { taxInputs, resolveTax, resolveBonus, createTaxSlip, transitionTaxSlip } = require(
  path.join(out, 'review/tax-model.js')
)
const { demo, result, calculateDemo } = require(path.join(out, 'review/demo.js'))
test('matching cumulative sources reconcile 27750 -> 832.50 -> 130.50 and net10219.50', () => {
  const i = taxInputs('VALID', 'V1')
  assert.deepEqual(
    [
      i.priorIncome,
      i.currentIncome,
      i.priorPersonal,
      i.currentPersonal,
      i.allowance,
      i.otherDeduction,
      i.priorTax
    ],
    [90000, 11750, 12600, 1400, 50000, 10000, 702]
  )
  const t = resolveTax(i)
  assert.deepEqual(
    [t.income, t.personal, t.taxable, t.cumulativeTax, t.tax, t.net],
    [101750, 14000, 27750, 832.5, 130.5, 10219.5]
  )
})
test('foreign year/subject and missing deduction stay unknown for either version', () => {
  for (const scope of ['WRONG_YEAR', 'WRONG_SUBJECT', 'MISSING_DEDUCTION'])
    for (const version of ['V1', 'V2']) {
      const t = resolveTax(taxInputs(scope, version))
      assert.equal(t.tax, null)
      assert.equal(t.net, null)
    }
})
test('invalid numeric and absent basis stay unknown; known zero remains distinct', () => {
  const i = taxInputs('VALID', 'V1')
  for (const field of [
    'priorIncome',
    'currentIncome',
    'priorPersonal',
    'currentPersonal',
    'allowance',
    'otherDeduction',
    'priorTax'
  ])
    for (const n of [null, NaN, Infinity, -1])
      assert.equal(resolveTax({ ...i, [field]: n }).tax, null)
  assert.equal(resolveTax({ ...i, basis: null }).tax, null)
  assert.equal(resolveTax({ ...i, currentPersonal: 0 }).tax, 172.5)
})
test('deduction correction has independent before/after; unsupported refund unknown', () => {
  const t = resolveTax(taxInputs('VALID', 'V2'))
  assert.deepEqual([t.taxable, t.cumulativeTax, t.tax, t.net], [26750, 802.5, 100.5, 10249.5])
  const refund = resolveTax({ ...taxInputs('VALID', 'V1'), priorTax: 900 })
  assert.equal(refund.tax, null)
  assert.equal(refund.net, null)
  assert.match(refund.reason, /退款/)
})
test('bonus unknown until explicit fictional A/B basis; choice never uses salary tax', () => {
  assert.equal(resolveBonus('UNSELECTED').tax, null)
  assert.equal(resolveBonus('A', false).tax, null)
  assert.deepEqual(
    [resolveBonus('A').amount, resolveBonus('A').tax, resolveBonus('A').net],
    [12000, 360, 11640]
  )
  assert.deepEqual([resolveBonus('B').tax, resolveBonus('B').net], [600, 11400])
  assert.match(resolveBonus('B').basis, /TEST-BONUS-B/)
})
test('failed -> queued retry -> success; invalid order and duplicate success do not count', () => {
  let s = createTaxSlip()
  s = transitionTaxSlip(s, true, 'QUEUE_RETRY')
  assert.equal(s.delivery, 'NOT_SENT')
  s = transitionTaxSlip(s, true, 'FAIL')
  assert.equal(s.delivery, 'FAILED')
  assert.equal(s.attempts, 1)
  s = transitionTaxSlip(s, true, 'SUCCESS')
  assert.equal(s.delivery, 'FAILED')
  assert.equal(s.attempts, 1)
  s = transitionTaxSlip(s, true, 'QUEUE_RETRY')
  assert.equal(s.delivery, 'WAITING_RETRY')
  s = transitionTaxSlip(s, true, 'SUCCESS')
  assert.equal(s.delivery, 'SUCCESS')
  assert.equal(s.attempts, 2)
  assert.equal(transitionTaxSlip(s, true, 'SUCCESS').attempts, 2)
})
test('unknown tax blocks delivery; withdrawn version blocks all delivery actions', () => {
  for (const action of ['FAIL', 'QUEUE_RETRY', 'SUCCESS']) {
    const b = transitionTaxSlip(createTaxSlip(), false, action)
    assert.equal(b.delivery, 'NOT_SENT')
    assert.equal(b.attempts, 0)
  }
  for (const initial of [createTaxSlip(), transitionTaxSlip(createTaxSlip(), true, 'SUCCESS')]) {
    const w = transitionTaxSlip(initial, true, 'WITHDRAW')
    assert.equal(w.withdrawn, true)
    for (const action of ['FAIL', 'QUEUE_RETRY', 'SUCCESS']) {
      const b = transitionTaxSlip(w, true, action)
      assert.equal(b.delivery, 'WITHDRAWN')
      assert.equal(b.attempts, w.attempts)
    }
  }
})
test('fresh candidates and immutable transitions do not retain delivery or mutate payroll', () => {
  const before = JSON.stringify({ demo, result })
  const old = createTaxSlip()
  const saved = JSON.stringify(old)
  transitionTaxSlip(old, true, 'FAIL')
  assert.equal(JSON.stringify(old), saved)
  const fresh = createTaxSlip()
  assert.notEqual(old, fresh)
  assert.equal(fresh.delivery, 'NOT_SENT')
  assert.equal(fresh.attempts, 0)
  resolveTax(taxInputs('VALID', 'V2'))
  resolveBonus('A')
  resolveBonus('B')
  assert.equal(JSON.stringify({ demo, result }), before)
  assert.deepEqual(calculateDemo(), result)
  assert.deepEqual(
    [result.gross, result.personal, result.tax, result.net, result.cost],
    [11750, 1400, 130.5, 10219.5, 13850]
  )
})
