// Node.js 22; actual SFC and Vue reactivity, mocked requests; no HTTP or database writes.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_WAGE_SOURCE ||
    ui + "/src/views/hrm/payroll/analysis/WageStructure.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
function harness() {
  const gets = [],
    downloads = [],
    files = [],
    stops = [],
    unmounts = [];
  const props = vue.reactive({
    entityId: 4,
    period: "2026-10",
    departments: [],
    categories: ["REGULAR"],
  });
  const pending = (queue, args) => {
    let resolve, reject;
    const promise = new Promise((a, b) => {
      resolve = a;
      reject = b;
    });
    queue.push({ args, resolve, reject });
    return promise;
  };
  const request = {
    get: (a) => pending(gets, a),
    download: (a) => pending(downloads, a),
  };
  const ctx = {
    exports: {},
    defineProps: () => props,
    ref: vue.ref,
    computed: vue.computed,
    watch: (...a) => stops.push(vue.watch(...a)),
    onBeforeUnmount: (f) => unmounts.push(f),
    require: (name) =>
      name.includes("config/axios")
        ? request
        : name.includes("utils/download")
          ? { excel: (...a) => files.push(a) }
          : {},
  };
  vm.runInNewContext(
    ts.transpileModule(
      body + "\nexports.s={deptId,category,loading,data,load,exportRows};",
      {
        compilerOptions: {
          module: ts.ModuleKind.CommonJS,
          target: ts.ScriptTarget.ES2022,
        },
      },
    ).outputText,
    ctx,
  );
  return {
    s: ctx.exports.s,
    props,
    gets,
    downloads,
    files,
    stop: () => {
      unmounts.forEach((f) => f());
      stops.forEach((f) => f());
    },
  };
}
const flush = () => new Promise((r) => setImmediate(r));
(async () => {
  let count = 0;
  for (const mode of [
    "category",
    "department",
    "entity",
    "period",
    "roundtrip",
    "unmount",
  ]) {
    const h = harness();
    await flush();
    const task = h.s.load();
    if (mode === "category") h.s.category.value = "TEST_EMPTY";
    if (mode === "department") h.s.deptId.value = 100;
    if (mode === "entity") h.props.entityId = 2;
    if (mode === "period") h.props.period = "2026-11";
    if (mode === "roundtrip") {
      h.s.category.value = "TEST_EMPTY";
      h.s.category.value = "";
    }
    if (mode === "unmount") h.stop();
    await flush();
    h.gets[1].resolve({ ready: true, gross: 5980 });
    await task;
    assert.equal(
      h.s.data.value,
      undefined,
      mode + ": old response must be discarded",
    );
    h.gets.forEach((g) => g.resolve({ ready: false }));
    await flush();
    h.stop();
    count++;
  }
  {
    const h = harness();
    h.gets[0].resolve({ ready: true, gross: 5980 });
    await flush();
    h.s.category.value = "REGULAR";
    assert.equal(h.s.data.value, undefined);
    assert.equal(h.s.loading.value, false);
    h.stop();
    count++;
  }
  {
    const h = harness();
    const first = h.s.load(),
      second = h.s.load();
    h.gets[1].resolve({ ready: true, gross: 1 });
    await first;
    assert.equal(h.s.loading.value, true);
    assert.equal(h.s.data.value, undefined);
    h.gets[2].resolve({ ready: true, gross: 2 });
    await second;
    assert.equal(h.s.data.value.gross, 2);
    h.gets[0].resolve({ ready: true, gross: 0 });
    await flush();
    assert.equal(h.s.data.value.gross, 2);
    h.stop();
    count++;
  }
  {
    const h = harness();
    h.s.category.value = "REGULAR";
    h.s.deptId.value = 100;
    const task = h.s.load();
    assert.equal(h.gets[1].args.params.employeeCategory, "REGULAR");
    assert.equal(h.gets[1].args.params.deptId, 100);
    h.gets[1].resolve({ ready: true, gross: 5980 });
    await task;
    assert.equal(h.s.data.value.gross, 5980);
    h.gets[0].resolve({ ready: true, gross: 0 });
    h.stop();
    count++;
  }
  for (const mode of ["normal", "filter", "unmount", "reload"]) {
    const h = harness();
    h.gets[0].resolve({ ready: true, gross: 5980 });
    await flush();
    const task = h.s.exportRows();
    if (mode === "filter") h.s.category.value = "TEST_EMPTY";
    if (mode === "unmount") h.stop();
    if (mode === "reload") h.s.load();
    h.downloads[0].resolve({ synthetic: true });
    await task;
    assert.equal(h.files.length, mode === "normal" ? 1 : 0);
    h.gets.forEach((g) => g.resolve({ ready: true }));
    await flush();
    h.stop();
    count++;
  }
  {
    const h = harness();
    await h.s.exportRows();
    assert.equal(h.downloads.length, 0);
    h.gets[0].resolve({ ready: false });
    await flush();
    await h.s.exportRows();
    assert.equal(h.downloads.length, 0);
    h.stop();
    count++;
  }
  console.log(`PASS ${count} wage structure context scenarios`);
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
