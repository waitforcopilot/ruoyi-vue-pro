// Run from frontend root: node --test tests/hrm-review-att.test.cjs
const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync } = require('node:fs')
const { tmpdir } = require('node:os')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const out = mkdtempSync(path.join(tmpdir(), 'hrm-att-'))
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
    path.join(root, 'src/views/hrm/payroll/review/att-model.ts')
  ],
  { cwd: root, stdio: 'pipe' }
)
const {
  attendanceExamples,
  resolveAttendanceDay,
  summarizeAttendance,
  attendanceMainOvertime
} = require(path.join(out, 'review/att-model.js'))
const { demo, result, calculateDemo } = require(path.join(out, 'review/demo.js'))

test('daily records plus16 remaining shifts reconcile160 scheduled/152 attendance/8 leave, with overtime separate', () => {
  const s = summarizeAttendance(attendanceExamples.NORMAL)
  assert.equal(s.scheduled, 160)
  assert.equal(s.rawPunch, 152)
  assert.equal(s.recognized, 152)
  assert.equal(s.paidLeave, 8)
  assert.equal(s.covered, 160)
  assert.equal(s.uncovered, 0)
  assert.equal(s.wageDeduction, 0)
  assert.equal(attendanceMainOvertime, 8)
  assert.notEqual(s.scheduled, s.covered + attendanceMainOvertime)
})
test('pending and rejected documents cannot replace a missing original punch or produce known wage deduction', () => {
  for (const key of ['TRAVEL_PENDING', 'CORRECTION_PENDING']) {
    const s = summarizeAttendance(attendanceExamples[key])
    assert.equal(s.rawPunch, null)
    assert.equal(s.recognized, null)
    assert.equal(s.covered, null)
    assert.equal(s.wageDeduction, null)
  }
  const example = attendanceExamples.CORRECTION_PENDING
  const doc = { ...example.documents[1], status: 'REJECTED' }
  const day = example.days.find((d) => d.id === doc.recordId)
  assert.equal(resolveAttendanceDay(day, [doc]).recognized, null)
})
test('approved travel restores recognition without manufacturing original punch data', () => {
  const s = summarizeAttendance(attendanceExamples.TRAVEL_APPROVED)
  assert.equal(s.rawPunch, null)
  assert.equal(s.recognized, 152)
  assert.equal(s.paidLeave, 8)
  assert.equal(s.covered, 160)
  assert.equal(s.wageDeduction, 0)
  const day = s.days.find((d) => d.id === 'TEST-D03')
  assert.equal(day.rawPunch, null)
  assert.equal(day.recognized, 8)
})
test('correction and repeated document IDs replace one record exactly once, never increment monthly total twice', () => {
  const example = attendanceExamples.CORRECTION_APPROVED
  const once = summarizeAttendance({ ...example, documents: example.documents.slice(0, 2) })
  const twice = summarizeAttendance(example)
  assert.equal(once.recognized, 152)
  assert.equal(twice.recognized, 152)
  assert.equal(twice.days.find((d) => d.id === 'TEST-D04').approvedDocumentCount, 1)
  const again = summarizeAttendance(example)
  assert.deepEqual(again, twice)
})
test('approved outing recognizes raw4+document4, while unapproved outing preserves raw4 and wage impact remains unknown', () => {
  const approved = summarizeAttendance(attendanceExamples.OUTING_APPROVED)
  const pending = summarizeAttendance(attendanceExamples.OUTING_PENDING)
  assert.equal(approved.rawPunch, 148)
  assert.equal(approved.recognized, 152)
  assert.equal(approved.covered, 160)
  assert.equal(pending.rawPunch, 148)
  assert.equal(pending.recognized, 148)
  assert.equal(pending.covered, 156)
  assert.equal(pending.uncovered, 4)
  assert.equal(pending.wageDeduction, null)
})
test('late, early, missing and verified absence remain distinct and do not imply automatic wage deductions', () => {
  const s = summarizeAttendance(attendanceExamples.ANOMALIES)
  for (const kind of ['LATE', 'EARLY', 'MISSING', 'ABSENCE'])
    assert.ok(s.days.some((d) => d.kind === kind))
  assert.equal(s.scheduled, 160)
  assert.equal(s.days.find((d) => d.kind === 'MISSING').recognized, null)
  assert.equal(s.days.find((d) => d.kind === 'ABSENCE').recognized, 0)
  assert.equal(s.wageDeduction, null)
})
test('conflicting or over-covering documents and duplicate day records preserve unknown instead of double counting', () => {
  const example = attendanceExamples.CORRECTION_APPROVED
  const doc = example.documents[1]
  const day = example.days.find((d) => d.id === doc.recordId)
  assert.equal(resolveAttendanceDay(day, [doc, { ...doc, hours: 7 }]).recognized, null)
  assert.equal(
    resolveAttendanceDay(day, [doc, { ...doc, id: 'TEST-LEAVE-OVERLAP', type: 'LEAVE' }]).covered,
    null
  )
  const duplicate = summarizeAttendance({ ...example, days: [...example.days, day] })
  assert.equal(duplicate.duplicateRecords, true)
  assert.equal(duplicate.recognized, null)
  assert.equal(duplicate.wageDeduction, null)
  assert.equal(resolveAttendanceDay({ ...day, rawPunch: NaN }, []).recognized, null)
})
test('scenario changes preserve original records, documents and October payroll sample', () => {
  const before = JSON.stringify({ attendanceExamples, demo, result })
  for (const example of Object.values(attendanceExamples)) summarizeAttendance(example)
  assert.equal(JSON.stringify({ attendanceExamples, demo, result }), before)
  assert.deepEqual(calculateDemo(), result)
  assert.equal(result.gross, 11750)
  assert.equal(result.net, 10219.5)
})
