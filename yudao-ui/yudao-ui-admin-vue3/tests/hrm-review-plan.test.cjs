// Run from frontend root: node --test tests/hrm-review-plan.test.cjs
const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync } = require('node:fs')
const { tmpdir } = require('node:os')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const out = mkdtempSync(path.join(tmpdir(), 'hrm-plan-'))
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
    path.join(root, 'src/views/hrm/payroll/review/plan-model.ts')
  ],
  { cwd: root, stdio: 'pipe' }
)
const { checkPlanAmount, previewPlan, planImportSamples, validatePlanImport } = require(
  path.join(out, 'review/plan-model.js')
)
const { demo, result, calculateDemo } = require(path.join(out, 'review/demo.js'))

test('blank, unknown, NaN, infinity, negatives, scientific notation, excess precision and unsafe amount never become zero/NaN previews', () => {
  for (const input of [
    '',
    '  ',
    undefined,
    null,
    NaN,
    Infinity,
    -1,
    '-1',
    '1e6',
    '10500.001',
    '1000000.01',
    '9'.repeat(400),
    {},
    'abc'
  ]) {
    assert.equal(checkPlanAmount(input).valid, false, String(input))
    const preview = previewPlan(input, 'NORMAL')
    assert.equal(preview.valid, false)
    assert.equal(preview.candidateBase, null)
    assert.equal(preview.fullMonthDifference, null)
    assert.equal(preview.applicableDifference, null)
    assert.equal(preview.currentBase, 10000)
  }
})
test('explicit zero, two-decimal money and input limit are valid and retain signed cent-accurate deltas', () => {
  assert.deepEqual(checkPlanAmount('0'), { valid: true, amount: 0 })
  assert.deepEqual(checkPlanAmount('1000000.00'), { valid: true, amount: 1000000 })
  assert.equal(previewPlan('10000.01', 'NORMAL').fullMonthDifference, 0.01)
  assert.equal(previewPlan('9999.99', 'NORMAL').fullMonthDifference, -0.01)
  assert.equal(previewPlan('0', 'NORMAL').fullMonthDifference, -10000)
})
test('V2 10500 exposes 500 delta without modifying current sample or payroll result', () => {
  const before = JSON.stringify({ demo, result })
  const preview = previewPlan('10500', 'NORMAL')
  assert.equal(preview.candidateBase, 10500)
  assert.equal(preview.fullMonthDifference, 500)
  assert.equal(preview.applicableDifference, 500)
  assert.equal(preview.effective, '2026-11-01')
  assert.equal(preview.currentBase, 10000)
  assert.equal(preview.currentGross, 11750)
  assert.equal(JSON.stringify({ demo, result }), before)
  assert.deepEqual(calculateDemo(), result)
})
test('month-mid, retroactive, pending and rejected cases show distinct missing basis and cannot apply full-month difference', () => {
  const descriptions = new Set()
  for (const scenario of ['MID_MONTH', 'RETRO', 'PENDING', 'REJECTED']) {
    const preview = previewPlan('10500', scenario)
    assert.equal(preview.valid, true)
    assert.equal(preview.fullMonthDifference, 500)
    assert.equal(preview.applicableDifference, null)
    assert.equal(preview.currentBase, 10000)
    assert.ok(preview.basis.length > 0)
    descriptions.add(preview.outcome)
  }
  assert.equal(descriptions.size, 4)
})
test('import validation distinguishes valid, duplicate, amount and date examples without mutating fixtures', () => {
  const before = JSON.stringify(planImportSamples)
  assert.equal(validatePlanImport(planImportSamples.VALID.rows)[0].valid, true)
  assert.ok(
    validatePlanImport(planImportSamples.DUPLICATE.rows).every(
      (r) => !r.valid && r.errors.some((e) => e.includes('重复'))
    )
  )
  assert.ok(validatePlanImport(planImportSamples.AMOUNT.rows).every((r) => !r.valid))
  const dates = validatePlanImport(planImportSamples.DATE.rows)
  assert.ok(dates[0].errors.some((e) => e.includes('真实日期')))
  assert.ok(dates[1].errors.some((e) => e.includes('晚于')))
  assert.equal(JSON.stringify(planImportSamples), before)
})
test('import requires scope/grade/unit fields and valid calendar dates; another subject is not a duplicate', () => {
  const base = planImportSamples.VALID.rows[0]
  for (const field of ['subject', 'employee', 'gradeGroup', 'gradeCode', 'unit']) {
    assert.equal(validatePlanImport([{ ...base, [field]: '' }])[0].valid, false)
  }
  assert.equal(
    validatePlanImport([{ ...base, validFrom: '2028-02-29', validTo: '2028-03-01' }])[0].valid,
    true
  )
  assert.equal(validatePlanImport([{ ...base, validFrom: '2027-02-29' }])[0].valid, false)
  assert.ok(
    validatePlanImport([base, { ...base, subject: 'TEST-SUBJECT-002' }]).every((r) => r.valid)
  )
})
