// Node.js 22; actual SFC, Vue refs, deferred mock requests. No HTTP/database access.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_ISSUE_READ_SOURCE ||
    ui + "/src/views/hrm/payroll/input-issue/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
const flush = () => new Promise((r) => setImmediate(r));
function harness() {
  const gets = [],
    mounts = [],
    unmounts = [];
  const request = {
    get: (args) =>
      new Promise((resolve, reject) => gets.push({ args, resolve, reject })),
  };
  const ctx = {
    exports: {},
    ref: vue.ref,
    reactive: vue.reactive,
    computed: vue.computed,
    defineOptions: () => {},
    onActivated: () => {},
    onDeactivated: () => {},
    onMounted: (f) => mounts.push(f),
    onBeforeUnmount: (f) => unmounts.push(f),
    useMessage: () => ({}),
    require: (name) =>
      name.includes("config/axios")
        ? request
        : name === "dayjs"
          ? require(ui + "/node_modules/dayjs")
          : name.includes("labels")
            ? { labels: {}, states: {}, actions: {} }
            : {},
  };
  vm.runInNewContext(
    ts.transpileModule(
      body +
        "\nexports.s={entityId,batchId,employeeId,rows,signals,employees,selectors,loading,changeEntity,loadBatch,loadSignals};",
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
  s.batchId.value = 7;
  s.employeeId.value = 8;
  return {
    s,
    gets,
    mounts,
    stop: () => unmounts.forEach((f) => f()),
    settle: () =>
      gets.forEach((g) =>
        g.resolve(
          g.args.url.endsWith("/selectors")
            ? { entities: [], batches: [] }
            : [],
        ),
      ),
  };
}
(async () => {
  let count = 0;
  for (const mode of ["entity", "batch", "refresh", "roundtrip", "unmount"]) {
    const h = harness();
    const task = h.s.loadBatch();
    if (mode === "entity") {
      h.s.entityId.value = 2;
      h.s.changeEntity();
    }
    if (mode === "batch") {
      h.s.batchId.value = 6;
      h.s.loadBatch();
    }
    if (mode === "refresh") h.s.loadBatch();
    if (mode === "roundtrip") {
      h.s.entityId.value = 2;
      h.s.changeEntity();
      h.s.entityId.value = 4;
      h.s.changeEntity();
      h.s.batchId.value = 7;
    }
    if (mode === "unmount") h.stop();
    h.gets[0].resolve([{ id: 1, batchId: 7 }]);
    h.gets[1].resolve([{ id: 8 }]);
    await task;
    assert.equal(h.s.rows.value.length, 0, mode + ": old list must not appear");
    assert.equal(h.s.employees.value.length, 0);
    h.settle();
    await flush();
    h.stop();
    count++;
  }
  {
    const h = harness();
    const a = h.s.changeEntity();
    h.s.entityId.value = 2;
    const b = h.s.changeEntity();
    h.gets[1].resolve({ entities: [], batches: [{ id: 2 }] });
    await b;
    h.gets[0].resolve({ entities: [], batches: [{ id: 7 }] });
    await a;
    assert.equal(h.s.selectors.batches[0].id, 2);
    h.stop();
    count++;
  }
  {
    const h = harness();
    const a = h.mounts[0]();
    h.s.entityId.value = 2;
    const b = h.s.changeEntity();
    h.gets[1].resolve({ entities: [], batches: [{ id: 2 }] });
    await b;
    h.gets[0].resolve({ entities: [], batches: [] });
    await a;
    assert.equal(h.s.selectors.batches[0].id, 2);
    h.stop();
    count++;
  }
  for (const mode of [
    "entity",
    "batch",
    "employee",
    "refresh",
    "roundtrip",
    "unmount",
  ]) {
    const h = harness();
    const task = h.s.loadSignals();
    if (mode === "entity") {
      h.s.entityId.value = 2;
      h.s.changeEntity();
    }
    if (mode === "batch") {
      h.s.batchId.value = 6;
      h.s.loadBatch();
    }
    if (mode === "employee") {
      h.s.employeeId.value = 9;
      h.s.loadSignals();
    }
    if (mode === "refresh") h.s.loadBatch();
    if (mode === "roundtrip") {
      h.s.employeeId.value = 9;
      h.s.loadSignals();
      h.s.employeeId.value = 8;
      h.s.loadSignals();
    }
    if (mode === "unmount") h.stop();
    h.gets[0].resolve([{ kind: "ATTENDANCE", ready: false }]);
    await task;
    assert.equal(
      h.s.signals.value.length,
      0,
      mode + ": old signals must not appear",
    );
    h.settle();
    await flush();
    h.stop();
    count++;
  }
  {
    const h = harness();
    h.s.rows.value = [{ id: 99 }];
    const task = h.s.loadBatch();
    assert.equal(h.s.rows.value.length, 0);
    h.gets[0].resolve([{ id: 1 }]);
    h.gets[1].resolve([{ id: 8 }]);
    await task;
    assert.equal(h.s.rows.value[0].id, 1);
    assert.equal(h.s.employees.value[0].id, 8);
    h.stop();
    count++;
  }
  {
    const h = harness();
    const task = h.s.loadSignals();
    assert.equal(h.gets[0].args.params.employeeId, 8);
    h.gets[0].resolve([{ ready: true }]);
    await task;
    assert.equal(h.s.signals.value[0].ready, true);
    assert.equal(h.s.loading.value, false);
    h.stop();
    count++;
  }
  {
    const h = harness();
    const a = h.s.loadBatch(),
      b = h.s.loadBatch();
    h.gets[0].resolve([]);
    h.gets[1].resolve([]);
    await a;
    assert.equal(h.s.loading.value, true);
    h.gets[2].resolve([]);
    h.gets[3].resolve([]);
    await b;
    assert.equal(h.s.loading.value, false);
    h.stop();
    count++;
  }
  {
    const h = harness();
    const a = h.s.loadSignals(),
      b = h.s.loadSignals();
    h.gets[0].resolve([]);
    await a;
    assert.equal(h.s.loading.value, true);
    h.gets[1].resolve([]);
    await b;
    assert.equal(h.s.loading.value, false);
    h.stop();
    count++;
  }
  {
    const h = harness();
    h.s.rows.value = [{ id: 99 }];
    const task = h.s.loadBatch();
    h.gets[0].reject(new Error("synthetic error"));
    h.gets[1].resolve([]);
    await task; // Batch loading now displays a persistent failure state instead of rethrowing.
    assert.equal(h.s.rows.value.length, 0);
    assert.equal(h.s.loading.value, false);
    h.stop();
    count++;
  }
  {
    const h = harness();
    h.s.batchId.value = undefined;
    await h.s.loadBatch();
    await h.s.loadSignals();
    assert.equal(h.gets.length, 0);
    h.stop();
    count++;
  }
  {
    const h = harness();
    const list = h.s.loadBatch();
    h.s.employeeId.value = 8;
    const signals = h.s.loadSignals();
    h.gets[2].resolve([{ ready: true }]);
    await signals;
    h.gets[0].resolve([{ id: 1 }]);
    h.gets[1].resolve([{ id: 8 }]);
    await list;
    assert.equal(h.s.employeeId.value, 8);
    assert.equal(h.s.signals.value[0].ready, true);
    h.stop();
    count++;
  }
  console.log(`PASS ${count} input issue read context scenarios`);
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
