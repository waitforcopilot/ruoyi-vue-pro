// Run from frontend root: node --test tests/hrm-review-hour.test.cjs
const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync } = require('node:fs')
const { tmpdir } = require('node:os')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const out = mkdtempSync(path.join(tmpdir(), 'hrm-hour-'))
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
    path.join(root, 'src/views/hrm/payroll/review/hour-model.ts')
  ],
  { cwd: root, stdio: 'pipe' }
)
const { hourExamples, summarizeHours, sumKnownHours, hourLabel } = require(
  path.join(out, 'review/hour-model.js')
)
const { demo, result, calculateDemo } = require(path.join(out, 'review/demo.js'))

test('standard 152+8 reconciles to160, while8 overtime never inflates the base denominator', () => {
  const s = summarizeHours(hourExamples.STANDARD)
  assert.equal(s.scheduled, 160)
  assert.equal(s.regular, 160)
  assert.equal(s.overtime, 8)
  assert.equal(s.salaryDenominator, 160)
  assert.equal(s.difference, 0)
  assert.notEqual(s.salaryDenominator, s.regular + s.overtime)
  assert.equal(result.overtime, 750)
  assert.equal(result.gross, demo.base + demo.performance + result.overtime)
})
test('complete comprehensive cycle independently totals two months and does not assume standard payroll denominator', () => {
  const s = summarizeHours(hourExamples.COMP_COMPLETE)
  assert.equal(s.completeCoverage, true)
  assert.equal(s.closed, true)
  assert.equal(s.scheduled, 320)
  assert.equal(s.observed.attended, 312)
  assert.equal(s.observed.paidLeave, 8)
  assert.equal(s.regular, 320)
  assert.equal(s.overtime, 10)
  assert.equal(s.difference, 0)
  assert.equal(s.salaryDenominator, null)
})
test('cross-month open records show known subtotal but preserve unknown final recognition and overtime', () => {
  const s = summarizeHours(hourExamples.COMP_OPEN)
  assert.equal(s.completeCoverage, true)
  assert.equal(s.closed, false)
  assert.equal(s.observed.attended, 232)
  assert.equal(s.recorded, 240)
  assert.equal(s.scheduled, 320)
  assert.equal(s.regular, null)
  assert.equal(s.overtime, null)
  assert.equal(s.difference, null)
  assert.match(s.status, /未结清/)
})
test('missing full month, duplicate month and extra month cannot be treated as complete cycle', () => {
  const s = summarizeHours(hourExamples.COMP_SHORT)
  assert.equal(s.observed.scheduled, 160)
  assert.equal(s.scheduled, null)
  assert.equal(s.regular, null)
  assert.equal(s.overtime, null)
  assert.match(s.status, /周期不足/)
  const full = hourExamples.COMP_COMPLETE
  for (const months of [
    [full.months[0], full.months[0]],
    [...full.months, { ...full.months[1], period: '2026-12' }]
  ]) {
    const invalid = summarizeHours({ ...full, months })
    assert.equal(invalid.completeCoverage, false)
    assert.equal(invalid.regular, null)
  }
})
test('irregular with and without basis differs in applicability but never supplies standard denominator or overtime', () => {
  const approved = summarizeHours(hourExamples.IRREGULAR_BASIS)
  const missing = summarizeHours(hourExamples.IRREGULAR_MISSING)
  assert.equal(approved.basisPresent, true)
  assert.equal(missing.basisPresent, false)
  assert.notEqual(approved.status, missing.status)
  for (const s of [approved, missing]) {
    assert.equal(s.recorded, 152)
    assert.equal(s.regular, null)
    assert.equal(s.overtime, null)
    assert.equal(s.salaryDenominator, null)
    assert.equal(s.difference, null)
  }
  const missingStandard = summarizeHours({ ...hourExamples.STANDARD, basis: null })
  assert.equal(missingStandard.regular, null)
  assert.equal(missingStandard.salaryDenominator, null)
})
test('unknown/invalid hours never become zero; explicit zero and valid hour sums remain known', () => {
  assert.equal(sumKnownHours([]), null)
  for (const input of [[null], [8, null], [NaN], [Infinity], [-1]])
    assert.equal(sumKnownHours(input), null)
  assert.equal(sumKnownHours([0]), 0)
  assert.equal(sumKnownHours([0.1, 0.2]), 0.3)
  assert.match(hourLabel(null), /未知/)
  assert.equal(hourLabel(0), '0小时')
  const base = hourExamples.STANDARD
  assert.equal(
    summarizeHours({ ...base, months: [{ ...base.months[0], scheduled: 0 }] }).salaryDenominator,
    null
  )
})
test('all scenario summaries preserve source records and the10-month payroll chain', () => {
  const before = JSON.stringify({ hourExamples, demo, result })
  for (const example of Object.values(hourExamples)) summarizeHours(example)
  assert.equal(JSON.stringify({ hourExamples, demo, result }), before)
  assert.deepEqual(calculateDemo(), result)
  assert.equal(result.gross, 11750)
  assert.equal(result.net, 10219.5)
  assert.equal(result.cost, 13850)
})
