// Node.js 22; actual SFC and Vue reactivity, mocked requests; no HTTP or database writes.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_OVERVIEW_SOURCE ||
    ui + "/src/views/hrm/payroll/overview/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
function harness() {
  const gets = [],
    unmounts = [],
    stops = [];
  const request = {
    get: (args) => {
      let resolve, reject;
      const promise = new Promise((a, b) => {
        resolve = a;
        reject = b;
      });
      gets.push({ args, resolve, reject });
      return promise;
    },
  };
  const ctx = {
    exports: {},
    defineOptions: () => {},
    ref: vue.ref,
    computed: vue.computed,
    watch: (...args) => stops.push(vue.watch(...args)),
    onMounted: () => {},
    onBeforeUnmount: (f) => unmounts.push(f),
    require: (name) =>
      name.includes("config/axios")
        ? request
        : name.includes("input-issue/labels")
          ? { labels: {} }
          : {},
  };
  vm.runInNewContext(
    ts.transpileModule(
      body +
        "\nexports.s={entityId,year,month,type,status,rows,loading,detailLoading,detail,batchId,employeeIds,load,select,loadDetail,reset};",
      {
        compilerOptions: {
          module: ts.ModuleKind.CommonJS,
          target: ts.ScriptTarget.ES2022,
        },
      },
    ).outputText,
    ctx,
  );
  const s = ctx.exports.s;
  s.entityId.value = 4;
  s.year.value = 2026;
  return {
    s,
    gets,
    stop: () => {
      unmounts.forEach((f) => f());
      stops.forEach((f) => f());
    },
  };
}
(async () => {
  let count = 0;
  for (const field of ["entityId", "year", "month", "type", "status"]) {
    const h = harness();
    const task = h.s.load();
    h.s[field].value = {
      entityId: 2,
      year: 2027,
      month: 11,
      type: "SUPPLEMENT",
      status: 5,
    }[field];
    h.gets[0].resolve([{ batchId: 6 }]);
    await task;
    assert.equal(h.s.rows.value.length, 0, field + ": stale history");
    assert.equal(h.s.loading.value, false);
    h.stop();
    count++;
  }
  for (const mode of [
    "entity",
    "reload",
    "otherBatch",
    "employees",
    "roundtrip",
    "unmount",
  ]) {
    const h = harness();
    h.s.rows.value = [{ batchId: 6 }, { batchId: 7 }];
    const task = h.s.select(h.s.rows.value[0]);
    if (mode === "entity") h.s.entityId.value = 2;
    if (mode === "reload") h.s.load();
    if (mode === "otherBatch") h.s.select(h.s.rows.value[1]);
    if (mode === "employees") h.s.employeeIds.value = [8];
    if (mode === "roundtrip") {
      h.s.entityId.value = 2;
      h.s.entityId.value = 4;
    }
    if (mode === "unmount") h.stop();
    h.gets[0].resolve({ summary: { batchId: 6, gross: 5980 }, signals: [] });
    await task;
    assert.equal(h.s.detail.value, undefined, mode + ": stale detail");
    if (mode === "otherBatch") {
      assert.equal(h.s.detailLoading.value, true);
      h.gets[1].resolve({ summary: { batchId: 7 }, signals: [] });
      await new Promise((r) => setImmediate(r));
      assert.equal(h.s.detail.value.summary.batchId, 7);
    } else assert.equal(h.s.detailLoading.value, false);
    h.gets.forEach((g) => g.resolve([]));
    h.stop();
    count++;
  }
  {
    const h = harness();
    const first = h.s.load(),
      second = h.s.load();
    h.gets[0].resolve([{ batchId: 1 }]);
    await first;
    assert.equal(h.s.loading.value, true);
    assert.equal(h.s.rows.value.length, 0);
    h.gets[1].resolve([{ batchId: 6 }]);
    await second;
    assert.equal(h.s.rows.value[0].batchId, 6);
    h.stop();
    count++;
  }
  {
    const h = harness();
    h.s.rows.value = [{ batchId: 6 }];
    const task = h.s.select(h.s.rows.value[0]);
    h.gets[0].resolve({ summary: { batchId: 6, gross: 5980 }, signals: [] });
    await task;
    assert.equal(h.s.detail.value.summary.gross, 5980);
    h.s.employeeIds.value = [8];
    const filter = h.s.loadDetail();
    assert.equal(h.gets[1].args.params.employeeIds, "8");
    h.gets[1].resolve({
      summary: { batchId: 6, employeeCount: 1 },
      signals: [],
    });
    await filter;
    assert.equal(h.s.detail.value.summary.employeeCount, 1);
    h.stop();
    count++;
  }
  {
    const h = harness();
    await h.s.select({ batchId: 999 });
    assert.equal(h.gets.length, 0);
    h.stop();
    count++;
  }
  {
    const h = harness();
    const task = h.s.load();
    h.gets[0].reject(new Error("synthetic error"));
    await assert.rejects(task, /synthetic error/);
    assert.equal(h.s.loading.value, false);
    h.stop();
    count++;
  }
  console.log(`PASS ${count} overview context scenarios`);
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
