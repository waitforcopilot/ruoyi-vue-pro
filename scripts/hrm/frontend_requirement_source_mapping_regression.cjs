// Contract: PRD v0.4 section 5, ten-view candidate source matrix.
// Independent expected matrix; no HTTP or database writes and no untracked PRD dependency.
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const assert = require("node:assert/strict");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript");
const file =
  process.env.HRM_REQUIREMENT_SOURCE ||
  ui + "/src/views/hrm/payroll/requirements.ts";
const context = { exports: {} };
vm.runInNewContext(
  ts.transpileModule(fs.readFileSync(file, "utf8"), {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
    },
  }).outputText,
  context,
);
const { payrollModules, payrollDataSources, sourcesForModules } =
  context.exports;
const expected = {
  OV: [1, 2, 3, 4, 5, 6, 7],
  PLAN: [1, 5, 9],
  CALC: [1, 2, 3, 4, 5, 6, 7, 9, 12],
  TAX: [5, 6, 7, 11, 12],
  ATT: [1, 2, 3],
  OT: [1, 2, 3, 4, 5],
  HOUR: [1, 2, 4],
  INS: [1, 6],
  RPT: [1, 5, 6, 10],
  COL: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
};
const ids = (list) => Array.from(list, (s) => s.id);
const ds = (list) => list.map((n) => "DS-" + String(n).padStart(2, "0"));
assert.deepEqual(
  Object.keys(payrollModules).sort(),
  Object.keys(expected).sort(),
);
for (const [module, values] of Object.entries(expected)) {
  assert.deepEqual(
    ids(sourcesForModules([module])),
    ds(values),
    module + ": PRD candidate mapping",
  );
}
assert.equal(new Set(ids(payrollDataSources)).size, 12);
assert.deepEqual(ids(sourcesForModules(["OT", "HOUR", "OT"])), ds(expected.OT));
assert.deepEqual(
  ids(sourcesForModules(Object.keys(expected))),
  ds(expected.COL),
);
assert.deepEqual(ids(sourcesForModules([])), []);
const input = Object.freeze(["CALC", "TAX"]);
assert.deepEqual(
  ids(sourcesForModules(input)),
  ds([1, 2, 3, 4, 5, 6, 7, 9, 11, 12]),
);
console.log(
  "PASS ten module contracts and five source catalog/union scenarios",
);
