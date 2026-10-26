// Run from frontend root: node --test tests/hrm-review-rpt.test.cjs
const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync } = require('node:fs')
const { tmpdir } = require('node:os')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const out = mkdtempSync(path.join(tmpdir(), 'hrm-rpt-'))
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
    path.join(root, 'src/views/hrm/payroll/review/rpt-model.ts')
  ],
  { cwd: root, stdio: 'pipe' }
)
const {
  rptDimensions,
  allocateReport,
  reportMetrics,
  rptBudget,
  compareReportBudget,
  rptReferences
} = require(path.join(out, 'review/rpt-model.js'))
const { demo, result, demoAllocation, demoMetrics } = require(path.join(out, 'review/demo.js'))
test('project 70/30 shares reconcile to 13850 and existing allocation constants', () => {
  const a = allocateReport(rptDimensions.PROJECT.rows)
  assert.equal(a.valid, true)
  assert.equal(a.total, 13850)
  assert.deepEqual(
    a.rows.map((r) => r.cost),
    [9695, 4155]
  )
  assert.deepEqual(
    a.rows.map((r) => r.cost),
    [demoAllocation.first, demoAllocation.second]
  )
})
test('department, cost center and project drill into same employee/period without adding people', () => {
  for (const d of Object.values(rptDimensions)) {
    const a = allocateReport(d.rows)
    assert.equal(a.total, 13850)
    assert.deepEqual([...new Set(a.rows.map((r) => r.employee))], [demo.employee])
    assert.deepEqual([...new Set(a.rows.map((r) => r.period))], [demo.period])
  }
})
test('duplicate allocation keys block rather than double count; bad totals/keys/percentages block', () => {
  const base = rptDimensions.PROJECT.rows
  for (const rows of [
    [...base, { ...base[0] }],
    [
      { ...base[0], percent: 50 },
      { ...base[0], percent: 50 }
    ],
    [],
    [{ ...base[0], percent: 90 }],
    [{ ...base[0], percent: 110 }],
    [{ ...base[0], percent: NaN }],
    [{ ...base[0], percent: 0 }],
    [{ ...base[0], key: '', percent: 100 }]
  ]) {
    const a = allocateReport(rows)
    assert.equal(a.valid, false)
    assert.equal(a.total, null)
    assert.deepEqual(a.rows, [])
  }
  const exact = allocateReport([
    { key: 'A', label: 'A', percent: 33.33 },
    { key: 'B', label: 'B', percent: 33.33 },
    { key: 'C', label: 'C', percent: 33.34 }
  ])
  assert.equal(exact.valid, true)
  assert.equal(
    exact.rows.reduce((s, r) => s + r.cost, 0),
    13850
  )
})
test('known metric denominators agree with main model; missing/zero/nonfinite never divide', () => {
  assert.deepEqual(reportMetrics(1, 100000), { average: 13850, costRate: 13.85 })
  assert.equal(reportMetrics(1, 100000).costRate, demoMetrics.costRate)
  for (const unknown of [null, 0, -1, NaN, Infinity]) {
    assert.equal(reportMetrics(unknown, 100000).average, null)
    assert.equal(reportMetrics(1, unknown).costRate, null)
  }
  assert.equal(reportMetrics(1.5, 100000).average, null)
  assert.equal(reportMetrics(null, 100000).costRate, 13.85)
  assert.equal(reportMetrics(1, null).average, 13850)
})
test('budget comparison requires same period, subject and explicit matching version', () => {
  const b = compareReportBudget(rptBudget)
  assert.equal(b.comparable, true)
  assert.equal(b.delta, -150)
  assert.equal(b.delta, demoMetrics.budgetVariance)
  for (const patch of [
    { period: '2026-09' },
    { subject: 'OTHER' },
    { version: null },
    { version: 'TEST-BUDGET-V2' },
    { amount: null },
    { amount: NaN },
    { amount: -1 }
  ]) {
    const other = compareReportBudget({ ...rptBudget, ...patch })
    assert.equal(other.comparable, false)
    assert.equal(other.delta, null)
  }
  assert.equal(compareReportBudget({ ...rptBudget, amount: 0 }).delta, 13850)
})
test('structure is main cost and all calculations preserve fixtures across repeated selections', () => {
  const before = JSON.stringify({ demo, result, rptDimensions, rptBudget, rptReferences })
  assert.equal(
    rptReferences.structure.reduce((s, r) => s + r.amount, 0),
    13850
  )
  for (const d of Object.values(rptDimensions)) allocateReport(d.rows)
  allocateReport([...rptDimensions.PROJECT.rows, ...rptDimensions.PROJECT.rows])
  reportMetrics(null, null)
  compareReportBudget({ ...rptBudget, version: null })
  assert.equal(JSON.stringify({ demo, result, rptDimensions, rptBudget, rptReferences }), before)
  assert.deepEqual(
    [result.gross, result.tax, result.net, result.cost],
    [11750, 130.5, 10219.5, 13850]
  )
})
