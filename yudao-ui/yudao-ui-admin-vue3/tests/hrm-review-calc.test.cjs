// Run from frontend root: node --test tests/hrm-review-calc.test.cjs
const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync } = require('node:fs')
const { tmpdir } = require('node:os')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const out = mkdtempSync(path.join(tmpdir(), 'hrm-calc-'))
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
    path.join(root, 'src/views/hrm/payroll/review/calc-model.ts')
  ],
  { cwd: root, stdio: 'pipe' }
)
const {
  createCalcState,
  inCalcContext,
  transitionCalc,
  calcAmounts,
  calcPaymentRows,
  validateCalcReturns,
  calcBatchLabel
} = require(path.join(out, 'review/calc-model.js'))
const { demo, result, calculateDemo } = require(path.join(out, 'review/demo.js'))
const context = { projectId: 153, module: 'CALC', period: '2026-10', scenario: 'MAIN-V1' }
const act = (s, a, v) => transitionCalc(s, s.context, a, v)
const trial = () => act(act(createCalcState(context), 'PREPARE'), 'CALCULATE')
const reviewed = () => act(trial(), 'REVIEW')
test('preparation gates trial; trial gates review/payment, without mutating previous state', () => {
  const ready = createCalcState(context)
  assert.equal(act(ready, 'CALCULATE').stage, 'READY')
  assert.equal(act(ready, 'REVIEW').stage, 'READY')
  assert.equal(act(ready, 'PAYMENT').stage, 'READY')
  assert.equal(ready.prepared, false)
  const s = reviewed()
  assert.equal(s.stage, 'REVIEWED')
  assert.equal(s.reviewedRevision, s.revision)
  assert.equal(s.calculatedAdjustment, 0)
  assert.deepEqual(calcAmounts(s.calculatedAdjustment), {
    gross: 11750,
    personal: 1400,
    tax: 130.5,
    net: 10219.5,
    cost: 13850
  })
})
test('missing attendance inputs remain blocked instead of being treated as zero', () => {
  const missing = act(createCalcState(context), 'SET_MISSING', true)
  const prepared = act(missing, 'PREPARE')
  assert.equal(prepared.prepared, false)
  assert.match(prepared.notice, /DS-02/)
  assert.equal(act(prepared, 'CALCULATE').calculatedAdjustment, null)
  const restored = act(act(act(prepared, 'SET_MISSING', false), 'PREPARE'), 'CALCULATE')
  assert.equal(restored.stage, 'CALCULATED')
})
test('independent100 adjustment produces11850/133.5/10316.5; input changes invalidate calculation/review/returns', () => {
  const target = { ...context, scenario: 'ADJUSTMENT' }
  let s = transitionCalc(reviewed(), target, 'SET_ADJUSTMENT', 100)
  assert.equal(s.stage, 'READY')
  assert.equal(s.reviewedRevision, null)
  s = act(act(s, 'PREPARE'), 'CALCULATE')
  assert.equal(s.calculatedAdjustment, 100)
  assert.deepEqual(calcAmounts(s.calculatedAdjustment), {
    gross: 11850,
    personal: 1400,
    tax: 133.5,
    net: 10316.5,
    cost: 13950
  })
  s = act(act(act(s, 'REVIEW'), 'PAYMENT'), 'RETURN_PARTIAL')
  const changed = act(s, 'SET_ADJUSTMENT', 200)
  assert.equal(changed.calculatedAdjustment, null)
  assert.equal(changed.reviewedRevision, null)
  assert.deepEqual(changed.returns, [])
  assert.equal(act(reviewed(), 'SET_ADJUSTMENT', 100).adjustment, 0)
  for (const value of [null, NaN, 1001, -1001])
    assert.equal(act(s, 'SET_ADJUSTMENT', value).adjustment, 100)
})
test('rejection requires preparation/recalculation and recalculation clears review and prior batch state', () => {
  const rejected = act(reviewed(), 'REJECT')
  assert.equal(rejected.stage, 'READY')
  assert.equal(rejected.prepared, false)
  assert.equal(rejected.calculatedAdjustment, null)
  assert.equal(act(rejected, 'REVIEW').stage, 'READY')
  const payment = act(act(reviewed(), 'PAYMENT'), 'RETURN_PARTIAL')
  const recalc = act(payment, 'CALCULATE')
  assert.equal(recalc.stage, 'CALCULATED')
  assert.equal(recalc.revision, payment.revision + 1)
  assert.equal(recalc.reviewedRevision, null)
  assert.deepEqual(recalc.returns, [])
  assert.equal(act(recalc, 'PAYMENT').stage, 'CALCULATED')
})
test('freeze blocks changes/recalculation/advancement; unfreeze clears review and keeps snapshot for fresh review', () => {
  const frozen = act(reviewed(), 'FREEZE')
  assert.equal(frozen.frozen, true)
  for (const action of ['SET_MISSING', 'CALCULATE', 'REJECT', 'PAYMENT']) {
    const blocked = act(frozen, action, true)
    assert.equal(blocked.stage, frozen.stage)
    assert.equal(blocked.revision, frozen.revision)
    assert.equal(blocked.missing, frozen.missing)
    assert.match(blocked.notice, /冻结/)
  }
  const thawed = act(frozen, 'UNFREEZE')
  assert.equal(thawed.frozen, false)
  assert.equal(thawed.stage, 'CALCULATED')
  assert.equal(thawed.reviewedRevision, null)
  assert.equal(act(thawed, 'PAYMENT').stage, 'CALCULATED')
  assert.equal(act(act(thawed, 'REVIEW'), 'PAYMENT').stage, 'PAYMENT')
})
test('matched TEST return is partial, repeats replace, and archive discussion retains failure instead of marking fully paid', () => {
  const payment = act(reviewed(), 'PAYMENT')
  assert.equal(act(payment, 'ARCHIVE').stage, 'PAYMENT')
  const partial = act(payment, 'RETURN_PARTIAL')
  assert.equal(partial.returns.length, 2)
  assert.equal(partial.returns[0].amount, 10219.5)
  assert.equal(partial.returns[1].amount, 5000)
  assert.equal(partial.returns[1].status, 'FAILED')
  assert.match(calcBatchLabel(partial), /部分成功/)
  assert.deepEqual(act(partial, 'RETURN_PARTIAL').returns, partial.returns)
  const archived = act(partial, 'ARCHIVE')
  assert.equal(archived.stage, 'ARCHIVED')
  assert.match(calcBatchLabel(archived), /未结清/)
  assert.equal(archived.returns[1].status, 'FAILED')
  assert.match(archived.notice, /非正式归档或结清/)
})
test('wrong person/period/amount and duplicate employee returns are rejected without replacing valid return', () => {
  const payment = act(reviewed(), 'PAYMENT')
  const partial = act(payment, 'RETURN_PARTIAL')
  assert.equal(validateCalcReturns(partial, partial.returns), true)
  for (const change of [{ employee: 'WRONG' }, { period: '2026-11' }, { amount: 1 }]) {
    const rows = [{ ...partial.returns[0], ...change }, partial.returns[1]]
    assert.equal(validateCalcReturns(partial, rows), false)
  }
  assert.equal(validateCalcReturns(partial, [partial.returns[0], partial.returns[0]]), false)
  assert.deepEqual(act(partial, 'RETURN_INVALID').returns, partial.returns)
  assert.deepEqual(act(payment, 'RETURN_INVALID').returns, [])
})
test('project/module/period/scenario changes discard prior success and cannot skip review in new context', () => {
  const state = act(act(reviewed(), 'PAYMENT'), 'RETURN_PARTIAL')
  for (const change of [
    { projectId: 154 },
    { module: 'PLAN' },
    { period: '2026-11' },
    { scenario: 'ADJUSTMENT' }
  ]) {
    const next = { ...context, ...change }
    const reset = inCalcContext(state, next)
    assert.equal(reset.stage, 'READY')
    assert.equal(reset.prepared, false)
    assert.equal(reset.revision, 0)
    assert.deepEqual(reset.returns, [])
    assert.equal(transitionCalc(state, next, 'REVIEW').stage, 'READY')
  }
  assert.equal(
    transitionCalc(
      createCalcState({ ...context, projectId: undefined }),
      { ...context, projectId: undefined },
      'PREPARE'
    ).prepared,
    false
  )
})
test('batch column preview separates second employee5000 from fixed primary payroll; all transitions preserve main sample', () => {
  const before = JSON.stringify({ demo, result })
  const rows = calcPaymentRows(trial())
  assert.equal(rows[0].employee, 'TEST-EMP-001')
  assert.equal(rows[0].amount, result.net)
  assert.equal(rows[1].employee, 'TEST-EMP-002')
  assert.equal(
    rows.reduce((n, r) => n + r.amount, 0),
    15219.5
  )
  assert.equal(JSON.stringify({ demo, result }), before)
  assert.deepEqual(calculateDemo(), result)
})
