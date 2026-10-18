// Node.js 22; actual SFC and Vue refs, mocked requests; no HTTP or database writes.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_ISSUE_HISTORY_SOURCE ||
    ui + "/src/views/hrm/payroll/input-issue/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
function harness() {
  const gets = [],
    unmounts = [];
  const request = {
    get: (args) =>
      args.url.endsWith("/history")
        ? new Promise((resolve, reject) => gets.push({ args, resolve, reject }))
        : Promise.resolve(
            args.url.endsWith("/selectors")
              ? { entities: [], batches: [] }
              : [],
          ),
  };
  const ctx = {
    exports: {},
    ref: vue.ref,
    reactive: vue.reactive,
    computed: vue.computed,
    defineOptions: () => {},
    onActivated: () => {},
    onDeactivated: () => {},
    onMounted: () => {},
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
        "\nexports.s={entityId,batchId,rows,events,historyVisible,history,changeEntity,loadBatch};",
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
  s.rows.value = [{ id: 1 }, { id: 2 }];
  return { s, gets, unmounts };
}
(async () => {
  let count = 0;
  for (const mode of ["entity", "batch", "refresh", "roundtrip", "unmount"]) {
    const h = harness();
    const task = h.s.history(1);
    if (mode === "entity") {
      h.s.entityId.value = 2;
      await h.s.changeEntity();
    }
    if (mode === "batch") {
      h.s.batchId.value = 6;
      await h.s.loadBatch();
    }
    if (mode === "refresh") await h.s.loadBatch();
    if (mode === "roundtrip") {
      h.s.entityId.value = 2;
      await h.s.changeEntity();
      h.s.entityId.value = 4;
      await h.s.changeEntity();
      h.s.batchId.value = 7;
      h.s.rows.value = [{ id: 1 }];
    }
    if (mode === "unmount") h.unmounts.forEach((f) => f());
    h.gets[0].resolve([{ action: "CREATE", reason: "OLD" }]);
    await task;
    assert.equal(
      h.s.historyVisible.value,
      false,
      mode + ": old history must not open",
    );
    assert.equal(h.s.events.value.length, 0);
    count++;
  }
  {
    const h = harness();
    const first = h.s.history(1),
      second = h.s.history(2);
    h.gets[1].resolve([{ reason: "NEW" }]);
    await second;
    h.gets[0].resolve([{ reason: "OLD" }]);
    await first;
    assert.equal(h.s.events.value[0].reason, "NEW");
    assert.equal(h.s.historyVisible.value, true);
    count++;
  }
  {
    const h = harness();
    const task = h.s.history(1);
    h.gets[0].resolve([{ action: "CREATE", actorId: 1 }]);
    await task;
    assert.equal(h.s.events.value[0].action, "CREATE");
    assert.equal(h.s.historyVisible.value, true);
    await h.s.loadBatch();
    assert.equal(h.s.events.value.length, 0);
    assert.equal(h.s.historyVisible.value, false);
    count++;
  }
  {
    const h = harness();
    await h.s.history(999);
    assert.equal(h.gets.length, 0);
    count++;
  }
  {
    const h = harness();
    h.s.entityId.value = undefined;
    await h.s.history(1);
    assert.equal(h.gets.length, 0);
    h.s.entityId.value = 4;
    h.s.batchId.value = undefined;
    await h.s.history(1);
    assert.equal(h.gets.length, 0);
    count++;
  }
  {
    const h = harness();
    const task = h.s.history(1);
    h.gets[0].reject(new Error("synthetic failure"));
    await assert.rejects(task, /synthetic failure/);
    assert.equal(h.s.historyVisible.value, false);
    assert.equal(h.s.events.value.length, 0);
    count++;
  }
  console.log(`PASS ${count} input issue history scenarios`);
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
