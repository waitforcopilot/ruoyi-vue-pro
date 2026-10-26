// Run from frontend root: node --test tests/hrm-review-ins.test.cjs
const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync } = require('node:fs')
const { tmpdir } = require('node:os')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const out = mkdtempSync(path.join(tmpdir(), 'hrm-ins-'))
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
    path.join(root, 'src/views/hrm/payroll/review/ins-model.ts')
  ],
  { cwd: root, stdio: 'pipe' }
)
const { insExamples, calculateIns, insDeltaLabel } = require(path.join(out, 'review/ins-model.js'))
const { demo, result, calculateDemo } = require(path.join(out, 'review/demo.js'))
test('main individual800+600 and employer1500+600 enter separate payroll/cost chains', () => {
  const s = calculateIns(insExamples.MAIN.after)
  assert.equal(s.personalInsurance, 800)
  assert.equal(s.personalFund, 600)
  assert.equal(s.personal, 1400)
  assert.equal(s.employerInsurance, 1500)
  assert.equal(s.employerFund, 600)
  assert.equal(s.employer, 2100)
  assert.equal(s.candidateCost, 13850)
  assert.equal(s.personalDelta, null)
  assert.equal(s.employerDelta, null)
  assert.equal(s.verified, false)
  assert.match(s.status, /未对账/)
})
test('annual and city changes select independent period-specific bases/rates', () => {
  const annual = calculateIns(insExamples.ANNUAL.after)
  assert.equal(annual.personal, 1680)
  assert.equal(annual.employer, 2520)
  const city = calculateIns(insExamples.CITY.after)
  assert.equal(city.activeCount, 1)
  assert.equal(city.personal, 1600)
  assert.equal(city.employer, 2300)
  assert.equal(calculateIns(insExamples.CITY.before).personal, 1400)
})
test('expired policy, foreign subject, invalid bases/rates and missing eligibility remain unknown', () => {
  for (const key of ['EXPIRED', 'BASE_INVALID']) {
    const s = calculateIns(insExamples[key].after)
    assert.equal(s.personal, null)
    assert.equal(s.employer, null)
    assert.equal(s.candidateCost, null)
    assert.equal(s.verified, false)
  }
  const main = insExamples.MAIN.after
  assert.equal(calculateIns({ ...main, subject: 'TEST-OTHER-SUBJECT' }).personal, null)
  assert.equal(calculateIns({ ...main, period: '2026-99' }).personal, null)
  assert.equal(
    calculateIns({ ...insExamples.EXIT.after, subject: 'TEST-OTHER-SUBJECT' }).personal,
    null
  )
  assert.equal(calculateIns(insExamples.JOIN.before).personal, null)
  const row = main.enrollments[0]
  for (const invalid of [NaN, -1, 4999, 30001])
    assert.equal(
      calculateIns({ ...main, enrollments: [{ ...row, insuranceBase: invalid }] }).personal,
      null
    )
  assert.equal(
    calculateIns({ ...main, enrollments: [{ ...row, policy: { ...row.policy, personalFund: 2 } }] })
      .personal,
    null
  )
  for (const boundary of [5000, 30000])
    assert.notEqual(
      calculateIns({
        ...main,
        enrollments: [{ ...row, insuranceBase: boundary, fundBase: boundary }]
      }).personal,
      null
    )
})
test('join qualification explicit TEST full month and exit known end are distinct from unknown', () => {
  assert.equal(calculateIns(insExamples.JOIN.before).personal, null)
  assert.equal(calculateIns(insExamples.JOIN.after).personal, 1400)
  assert.equal(calculateIns(insExamples.EXIT.before).personal, 1400)
  assert.equal(calculateIns(insExamples.EXIT.after).personal, 0)
  assert.equal(calculateIns(insExamples.EXIT.after).employer, 0)
  assert.equal(calculateIns({ ...insExamples.MAIN.after, enrollments: [] }).personal, null)
})
test('positive due-minus-paid requires supplement; negative requires refund without executing either', () => {
  const less = calculateIns(insExamples.SUPPLEMENT.after)
  const more = calculateIns(insExamples.REFUND.after)
  assert.equal(less.personalDelta, 100)
  assert.equal(less.employerDelta, 100)
  assert.equal(more.personalDelta, -100)
  assert.equal(more.employerDelta, -100)
  assert.match(insDeltaLabel(less.personalDelta), /待补缴/)
  assert.match(insDeltaLabel(more.employerDelta), /待退款/)
  assert.match(less.status, /未执行/)
  assert.match(more.status, /未执行/)
  const offset = calculateIns({ ...insExamples.SUPPLEMENT.after, paidEmployer: 2200 })
  assert.equal(offset.personalDelta, 100)
  assert.equal(offset.employerDelta, -100)
  assert.doesNotMatch(offset.status, /核对一致/)
})
test('unknown/invalid paid and missing evidence never become matched; known zero is distinct', () => {
  const noReceipt = calculateIns(insExamples.NO_RECEIPT.after)
  assert.equal(noReceipt.personalDelta, 0)
  assert.equal(noReceipt.verified, false)
  assert.match(noReceipt.status, /未对账/)
  const matched = calculateIns(insExamples.MATCHED.after)
  assert.equal(matched.verified, true)
  assert.match(matched.status, /TEST金额核对一致/)
  for (const paid of [null, NaN, -1]) {
    const s = calculateIns({ ...insExamples.MATCHED.after, paidPersonal: paid })
    assert.equal(s.personalDelta, null)
    assert.equal(s.verified, false)
  }
  assert.equal(calculateIns({ ...insExamples.MATCHED.after, paidPersonal: 0 }).personalDelta, 1400)
})
test('identical enrollment duplicate counts once; conflicting same ID or two cities blocks double charge', () => {
  const main = insExamples.MAIN.after
  const row = main.enrollments[0]
  const twice = calculateIns({ ...main, enrollments: [row, { ...row }] })
  assert.equal(twice.activeCount, 1)
  assert.equal(twice.personal, 1400)
  assert.equal(twice.employer, 2100)
  assert.equal(
    calculateIns({ ...main, enrollments: [row, { ...row, fundBase: 12000 }] }).personal,
    null
  )
  assert.equal(calculateIns(insExamples.DUPLICATE_CITY.after).personal, null)
  assert.equal(calculateIns(insExamples.DUPLICATE_CITY.after).employer, null)
})
test('candidate adjustments and reconciliation do not mutate fixtures or the October payroll', () => {
  const before = JSON.stringify({ insExamples, demo, result })
  for (const e of Object.values(insExamples)) {
    calculateIns(e.before)
    calculateIns(e.after)
  }
  assert.equal(JSON.stringify({ insExamples, demo, result }), before)
  assert.deepEqual(calculateDemo(), result)
  assert.equal(demo.base, 10000)
  assert.equal(result.personal, 1400)
  assert.equal(result.employer, 2100)
  assert.equal(result.gross, 11750)
  assert.equal(result.net, 10219.5)
  assert.equal(result.cost, 13850)
})
