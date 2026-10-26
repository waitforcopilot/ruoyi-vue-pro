// Run from frontend root: node --test tests/hrm-review-ov.test.cjs
const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync } = require('node:fs')
const { tmpdir } = require('node:os')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const out = mkdtempSync(path.join(tmpdir(), 'hrm-ov-'))
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
    path.join(root, 'src/views/hrm/payroll/review/ov-model.ts')
  ],
  { cwd: root, stdio: 'pipe' }
)
const { resolveOverview, overviewRealState } = require(path.join(out, 'review/ov-model.js'))
const { demo, result, demoTrend } = require(path.join(out, 'review/demo.js'))
test('normal same employee and October amounts match fixed chain; receipt gap does not erase due', () => {
  const o = resolveOverview('2026-10', 'MAIN')
  assert.equal(o.employee, demo.employee)
  assert.equal(o.period, demo.period)
  assert.equal(o.measurable, true)
  assert.deepEqual(o.amounts, {
    gross: 11750,
    personal: 1400,
    tax: 130.5,
    net: 10219.5,
    cost: 13850
  })
  assert.equal(o.trend.find((r) => r.period === o.period).gross, o.amounts.gross)
  assert.equal(o.readiness.find((r) => r.module === 'HOUR').gap, false)
  assert.equal(o.readiness.find((r) => r.module === 'ATT').gap, false)
  assert.match(o.readiness.find((r) => r.module === 'OT').status, /认可8小时.*工资750元/)
  const ins = o.readiness.find((r) => r.module === 'INS')
  assert.equal(ins.gap, true)
  assert.match(ins.status, /应缴可测，实缴待核对/)
})
test('pending attendance makes current pay and current trend unknown, not zero or stale main', () => {
  const o = resolveOverview('2026-10', 'MISSING_ATT')
  assert.equal(o.measurable, false)
  for (const value of Object.values(o.amounts)) assert.equal(value, null)
  assert.equal(o.trend.find((r) => r.period === o.period).gross, null)
  const gap = o.gaps.find((r) => r.module === 'ATT')
  assert.match(gap.object, /TEST-D03/)
  assert.match(gap.status, /未知/)
  assert.deepEqual(
    o.trend.filter((r) => r.period !== o.period).map((r) => r.gross),
    [10000, 11000]
  )
})
test('November missing period sources never borrow October readiness or amounts', () => {
  for (const scope of ['MAIN', 'MISSING_ATT']) {
    const o = resolveOverview('2026-11', scope)
    assert.equal(o.measurable, false)
    for (const n of Object.values(o.amounts)) assert.equal(n, null)
    assert.equal(o.gaps.length, 4)
    assert.equal(o.trend.find((r) => r.period === o.period).gross, null)
    for (const row of o.readiness) {
      assert.equal(row.gap, true)
      assert.match(row.object, /11/)
      assert.match(row.status, /未知/)
      assert.match(row.deadline, /2026-11/)
    }
  }
})
test('every gap has explicit TEST object, role, deadline and original module anchor', () => {
  for (const period of ['2026-10', '2026-11'])
    for (const scope of ['MAIN', 'MISSING_ATT']) {
      for (const gap of resolveOverview(period, scope).gaps) {
        assert.match(gap.object, /TEST/)
        assert.match(gap.responsible, /TEST/)
        assert.match(gap.deadline, /讨论期限/)
        assert.match(gap.anchor, new RegExp(`^field-${gap.module.toLowerCase()}-`))
      }
    }
})
test('actual reading states take precedence; empty and visible subsets do not claim hidden counts', () => {
  assert.match(overviewRealState({ loading: true, error: 'x', visibleRows: 0 }), /加载中/)
  assert.match(overviewRealState({ loading: false, error: 'x', visibleRows: 0 }), /读取失败或无权/)
  const empty = overviewRealState({
    loading: false,
    error: '',
    label: '受限可见集合TEST',
    visibleRows: 0
  })
  assert.match(empty, /受限可见集合TEST/)
  assert.match(empty, /不代表全项目为空/)
  const subset = overviewRealState({
    loading: false,
    error: '',
    label: '服务端TEST范围',
    visibleRows: 3
  })
  assert.match(subset, /本页可见3行/)
  assert.match(subset, /隐藏对象和数量不推测/)
  assert.match(overviewRealState({ loading: false, error: '', visibleRows: 0 }), /不推定全项目权限/)
})
test('repeated period/scope selections are isolated and do not mutate existing fixtures', () => {
  const snapshot = JSON.stringify({ demo, result, demoTrend })
  const normal = resolveOverview('2026-10', 'MAIN')
  resolveOverview('2026-11', 'MAIN')
  resolveOverview('2026-10', 'MISSING_ATT')
  assert.deepEqual(resolveOverview('2026-10', 'MAIN'), normal)
  assert.equal(JSON.stringify({ demo, result, demoTrend }), snapshot)
})
