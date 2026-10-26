// Run: node --test tests/hrm-review-demo-state.test.cjs (uses the project's installed TypeScript).
const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync } = require('node:fs')
const { tmpdir } = require('node:os')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const out = mkdtempSync(path.join(tmpdir(), 'hrm-demo-state-'))
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
    path.join(root, 'src/views/hrm/payroll/review/demo-state.ts'),
    path.join(root, 'src/views/hrm/payroll/review/demo.ts')
  ],
  { cwd: root, stdio: 'pipe' }
)
const { createDemoState, transitionDemo, inDemoContext, qualityLabel } = require(
  path.join(out, 'review/demo-state.js')
)
const { demo, result, reviewViews, demoAllocation, demoMetrics, demoCostStructure } = require(
  path.join(out, 'review/demo.js')
)
const context = { projectId: 153, module: 'CALC', period: '2026-10', scenario: 'MAIN-V1' }
const calculated = () => transitionDemo(createDemoState(context), context, 'CALCULATE')

test('review cannot skip calculation and does not mutate its input', () => {
  const ready = createDemoState(context)
  const blocked = transitionDemo(ready, context, 'REVIEW')
  assert.equal(blocked.stage, 'READY')
  assert.match(blocked.notice, /请先/)
  assert.equal(ready.stage, 'READY')
})
test('calculate -> review; repeated review is not a new approval; recalculation requires review again', () => {
  const first = calculated()
  const reviewed = transitionDemo(first, context, 'REVIEW')
  assert.equal(first.stage, 'CALCULATED')
  assert.equal(reviewed.stage, 'REVIEWED')
  assert.match(transitionDemo(reviewed, context, 'REVIEW').notice, /先重算/)
  assert.equal(transitionDemo(reviewed, context, 'CALCULATE').stage, 'CALCULATED')
})
test('every context boundary discards previous success, including attempt to review in a new project', () => {
  const reviewed = transitionDemo(calculated(), context, 'REVIEW')
  for (const change of [
    { projectId: 154 },
    { module: 'PLAN' },
    { period: '2026-11' },
    { scenario: 'CANDIDATE-V2' }
  ]) {
    const target = { ...context, ...change }
    assert.equal(inDemoContext(reviewed, target).stage, 'READY')
    assert.equal(transitionDemo(reviewed, target, 'REVIEW').stage, 'READY')
  }
  assert.equal(inDemoContext(reviewed, { ...context }), reviewed)
})
test('non-calculation views and no-project context cannot calculate', () => {
  for (const module of ['OV', 'PLAN', 'TAX', 'ATT', 'OT', 'HOUR', 'INS', 'RPT', 'COL']) {
    const target = { ...context, module }
    assert.equal(transitionDemo(createDemoState(target), target, 'CALCULATE').stage, 'READY')
  }
  const target = { ...context, projectId: undefined }
  assert.equal(transitionDemo(createDemoState(target), target, 'CALCULATE').stage, 'READY')
})
test('unreturned quality remains unknown, distinct from false and true', () => {
  assert.match(qualityLabel(undefined), /未知/)
  assert.notEqual(qualityLabel(undefined), qualityLabel(false))
  assert.equal(qualityLabel(true), '通过')
})
test('main sample reconciles gross, deductions, net, cost and allocation without double counting', () => {
  assert.equal(result.gross, 11750)
  assert.equal(result.personal, 1400)
  assert.equal(result.tax, 130.5)
  assert.equal(result.net, 10219.5)
  assert.equal(result.cost, 13850)
  assert.equal(result.gross - result.personal - result.tax, result.net)
  assert.equal(result.gross + result.employer, result.cost)
  assert.equal(demoAllocation.first, 9695)
  assert.equal(demoAllocation.second, 4155)
  assert.equal(demoAllocation.first + demoAllocation.second, result.cost)
  assert.equal(demoMetrics.budgetVariance, -150)
  assert.equal(
    demoCostStructure.reduce((sum, r) => sum + r.amount, 0),
    result.cost
  )
  assert.equal(demo.attendedHours + demo.paidLeaveHours, demo.scheduledHours)
})
test('numeric presentation rows reconcile to the same sample consumed by the slip', () => {
  assert.equal(reviewViews.OV.rows[0][2], result.gross)
  assert.equal(reviewViews.CALC.rows[0][4], result.gross)
  assert.equal(reviewViews.CALC.rows[0][7], result.net)
  assert.equal(reviewViews.TAX.rows[0][8], result.net)
  assert.equal(reviewViews.OT.rows[0][6], result.overtime)
  assert.equal(reviewViews.RPT.rows[0][2] + reviewViews.RPT.rows[1][2], result.cost)
  assert.equal(
    reviewViews.INS.rows.reduce((sum, r) => sum + r[3], 0),
    result.personal
  )
  assert.equal(
    reviewViews.INS.rows.reduce((sum, r) => sum + r[5], 0),
    result.employer
  )
})
