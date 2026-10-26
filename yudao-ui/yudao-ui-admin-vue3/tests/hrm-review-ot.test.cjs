// Run from frontend root: node --test tests/hrm-review-ot.test.cjs
const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync } = require('node:fs')
const { tmpdir } = require('node:os')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const out = mkdtempSync(path.join(tmpdir(), 'hrm-ot-'))
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
    path.join(root, 'src/views/hrm/payroll/review/ot-model.ts')
  ],
  { cwd: root, stdio: 'pipe' }
)
const { otExamples, resolveOt, emptyOtLedger, compensateOt, useOtLeave } = require(
  path.join(out, 'review/ot-model.js')
)
const { demo, result, calculateDemo } = require(path.join(out, 'review/demo.js'))
test('pending/approved/rejected retain application8/punch9 and recognize unknown/8/0', () => {
  assert.equal(resolveOt(otExamples.PENDING).hours, null)
  assert.equal(resolveOt(otExamples.PENDING).wage, null)
  assert.equal(resolveOt(otExamples.APPROVED).hours, 8)
  assert.equal(resolveOt(otExamples.APPROVED).wage, 750)
  assert.equal(resolveOt(otExamples.REJECTED).hours, 0)
  assert.equal(resolveOt(otExamples.REJECTED).wage, 0)
  assert.equal(otExamples.REJECTED.punched, 9)
  assert.equal(otExamples.REJECTED.requested, 8)
  for (const key of ['PENDING', 'REJECTED', 'CROSS_PERIOD'])
    assert.equal(compensateOt(emptyOtLedger(), otExamples[key], 'WAGE').state.orderId, null)
})
test('recognition never exceeds application/punch; malformed or missing rules stay unknown', () => {
  assert.equal(resolveOt({ ...otExamples.APPROVED, requested: 7 }).hours, 7)
  assert.equal(resolveOt({ ...otExamples.APPROVED, punched: 6 }).hours, 6)
  for (const changes of [{ punched: NaN }, { recognized: -1 }, { basis: null }, { slices: [] }])
    assert.equal(resolveOt({ ...otExamples.APPROVED, ...changes }).hours, null)
  assert.equal(resolveOt({ ...otExamples.APPROVED, multiplier: null }).wage, null)
})
test('cross-day counts once; missing cross-period basis stays unknown; holiday is independent', () => {
  assert.equal(resolveOt(otExamples.CROSS_DAY).hours, 8)
  assert.equal(resolveOt(otExamples.CROSS_DAY).wage, 750)
  assert.equal(resolveOt(otExamples.CROSS_PERIOD).hours, null)
  assert.equal(resolveOt(otExamples.CROSS_PERIOD).wage, null)
  assert.equal(resolveOt(otExamples.CROSS_PERIOD).monthly, null)
  assert.equal(resolveOt(otExamples.HOLIDAY).wage, 1000)
  assert.equal(resolveOt(otExamples.APPROVED).overLimit, true)
  assert.equal(resolveOt(otExamples.APPROVED).monthly, 38)
})
test('wage750 enters once and blocks repeated wage and leave compensation', () => {
  const once = compensateOt(emptyOtLedger(), otExamples.APPROVED, 'WAGE').state
  assert.equal(once.wage, 750)
  assert.equal(once.granted, 0)
  const twice = compensateOt(once, otExamples.APPROVED, 'WAGE')
  assert.equal(twice.state, once)
  assert.match(twice.message, /重复/)
  const other = compensateOt(once, otExamples.APPROVED, 'LEAVE')
  assert.equal(other.state, once)
  assert.match(other.message, /双计/)
  assert.equal(useOtLeave(once, 'USE', 4).state, once)
})
test('leave grants8 once, uses4 once, rejects duplicate use/overdraw and wage', () => {
  const grant = compensateOt(emptyOtLedger(), otExamples.APPROVED, 'LEAVE').state
  assert.equal(grant.granted, 8)
  assert.equal(grant.wage, 0)
  assert.equal(compensateOt(grant, otExamples.APPROVED, 'LEAVE').state, grant)
  assert.equal(compensateOt(grant, otExamples.APPROVED, 'WAGE').state, grant)
  const use = useOtLeave(grant, 'TEST-LEAVE-001', 4).state
  assert.equal(use.granted - use.used, 4)
  assert.equal(useOtLeave(use, 'TEST-LEAVE-001', 4).state, use)
  assert.equal(useOtLeave(use, 'OTHER', 5).state, use)
  assert.equal(useOtLeave(use, 'OTHER', -1).state, use)
  assert.equal(grant.used, 0)
})
test('candidate branches preserve October main payroll and input fixtures', () => {
  const before = JSON.stringify({ otExamples, demo, result })
  for (const example of Object.values(otExamples)) {
    resolveOt(example)
    compensateOt(emptyOtLedger(), example, 'WAGE')
    compensateOt(emptyOtLedger(), example, 'LEAVE')
  }
  assert.equal(JSON.stringify({ otExamples, demo, result }), before)
  assert.deepEqual(calculateDemo(), result)
  assert.equal(result.hourly, 62.5)
  assert.equal(result.overtime, 750)
  assert.equal(result.gross, 11750)
  assert.equal(result.personal, 1400)
  assert.equal(result.tax, 130.5)
  assert.equal(result.net, 10219.5)
  assert.equal(result.cost, 13850)
})
