// Node.js 22; actual SFC, Vue refs, deferred mock requests. No HTTP/database access.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_ISSUE_BATCH_LOAD_SOURCE ||
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
        '\nexports.s={entityId,batchId,employeeId,rows,signals,employees,selectors,loading,changeEntity,loadBatch,loadSignals,batchLoadError:typeof batchLoadError!=="undefined"?batchLoadError:ref(false)};',
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
  for (const failed of [0, 1]) {
    const h = harness();
    h.s.rows.value = [{ id: 1 }];
    h.s.employees.value = [{ id: 8 }];
    const task = h.s.loadBatch();
    assert.equal(h.s.rows.value.length, 0);
    assert.equal(h.s.employees.value.length, 0);
    assert.equal(h.s.loading.value, true);
    h.gets[failed].reject(new Error("TEST timeout"));
    h.gets[1 - failed].resolve([]);
    await task.catch(() => {});
    assert.equal(
      h.s.batchLoadError.value,
      true,
      "current batch failure must remain explicit",
    );
    assert.equal(h.s.loading.value, false);
    count++;
    for (const result of [[], [{ id: 2 }]]) {
      const offset = h.gets.length,
        retry = h.s.loadBatch();
      assert.equal(h.s.batchLoadError.value, false);
      h.gets[offset].resolve(result);
      h.gets[offset + 1].resolve([{ id: 8 }]);
      await retry;
      assert.equal(h.s.rows.value.length, result.length);
      assert.equal(h.s.batchLoadError.value, false);
      count++;
    }
  }
  for (const olderFails of [true, false]) {
    const h = harness(),
      old = h.s.loadBatch(),
      current = h.s.loadBatch();
    if (olderFails) {
      h.gets[2].resolve([{ id: 2 }]);
      h.gets[3].resolve([{ id: 8 }]);
    } else {
      h.gets[2].reject(new Error("current failure"));
      h.gets[3].resolve([]);
    }
    await current.catch(() => {});
    if (olderFails) h.gets[0].reject(new Error("old failure"));
    else h.gets[0].resolve([{ id: 1 }]);
    h.gets[1].resolve([]);
    await old.catch(() => {});
    assert.equal(h.s.batchLoadError.value, !olderFails);
    assert.equal(h.s.rows.value.length, olderFails ? 1 : 0);
    count++;
  }
  for (const mode of ["entity", "batch", "unmount", "refresh"]) {
    const h = harness(),
      old = h.s.loadBatch();
    if (mode === "entity") {
      h.s.entityId.value = 2;
      h.s.changeEntity();
    }
    if (mode === "batch") {
      h.s.batchId.value = 6;
      h.s.loadBatch();
    }
    if (mode === "unmount") h.stop();
    if (mode === "refresh") h.s.loadBatch();
    h.gets[0].reject(new Error("late failure"));
    h.gets[1].resolve([]);
    await old.catch(() => {});
    assert.equal(
      h.s.batchLoadError.value,
      false,
      mode + ": stale failure must not apply",
    );
    if (mode === "batch" || mode === "refresh")
      assert.equal(h.s.loading.value, true);
    h.settle();
    await flush();
    count++;
  }
  {
    const h = harness();
    h.s.batchLoadError.value = true;
    h.s.entityId.value = 2;
    const p = h.s.changeEntity();
    assert.equal(h.s.batchLoadError.value, false);
    h.gets[0].resolve({ entities: [], batches: [] });
    await p;
    count++;
  }
  console.log(`PASS ${count} admin input-issue batch-load failure scenarios`);
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
