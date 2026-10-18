// Execute actual SFC using Vue refs and deferred mocked requests. No HTTP/DB.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_PORTAL_HISTORY_SOURCE ||
    ui + "/src/views/hrm/portal/salary/input-issue/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
const row = (id) => ({
  id,
  batchId: 7,
  employeeId: id + 7,
  status: "OPEN",
  canSubmit: true,
});
function harness() {
  const history = [],
    lists = [],
    off = [],
    on = [],
    unmount = [];
  const pending = (q, args) =>
    new Promise((resolve, reject) => q.push({ args, resolve, reject }));
  const request = {
    get: (args) =>
      pending(args.url.endsWith("/history") ? history : lists, args),
  };
  const ctx = {
    exports: {},
    ref: vue.ref,
    defineOptions: () => {},
    onMounted: () => {},
    onActivated: (f) => on.push(f),
    onDeactivated: (f) => off.push(f),
    onBeforeUnmount: (f) => unmount.push(f),
    useMessage: () => ({}),
    require: (name) =>
      name.includes("config/axios")
        ? request
        : name === "dayjs"
          ? require(ui + "/node_modules/dayjs")
          : name === "element-plus"
            ? { ElMessageBox: { close: () => {} } }
            : {},
  };
  vm.runInNewContext(
    ts.transpileModule(
      body +
        '\nexports.s={rows,events,historyVisible,historyIssueId:typeof historyIssueId!=="undefined"?historyIssueId:ref(),loading,history,load};',
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
  s.rows.value = [row(1), row(2)];
  return { s, history, lists, off, on, unmount };
}
(async () => {
  let count = 0;
  for (const kind of [
    "leave",
    "unmount",
    "refresh",
    "remove",
    "batch",
    "employee",
  ]) {
    const h = harness(),
      p = h.s.history(1);
    if (kind === "leave") {
      h.off.forEach((f) => f());
      const refresh = h.on[0]();
      h.lists[0].resolve([row(1)]);
      await refresh;
    }
    if (kind === "unmount") h.unmount.forEach((f) => f());
    if (kind === "refresh") {
      const q = h.s.load();
      h.lists[0].resolve([row(1)]);
      await q;
    }
    if (kind === "remove") h.s.rows.value = [];
    if (kind === "batch") h.s.rows.value[0].batchId = 9;
    if (kind === "employee") h.s.rows.value[0].employeeId = 9;
    h.history[0].resolve([{ reason: "TEST old" }]);
    await p;
    assert.equal(h.s.historyVisible.value, false, "stale " + kind);
    assert.equal(h.s.events.value.length, 0);
    count++;
  }
  for (const kind of ["missing", "inactive", "loading"]) {
    const h = harness();
    if (kind === "missing") h.s.rows.value = [];
    if (kind === "inactive") h.off.forEach((f) => f());
    if (kind === "loading") h.s.loading.value = true;
    const p = h.s.history(1);
    assert.equal(h.history.length, 0, kind + " no history request");
    await p;
    count++;
  }
  {
    const h = harness(),
      a = h.s.history(1),
      b = h.s.history(2);
    h.history[1].resolve([{ reason: "TEST second" }]);
    await b;
    h.history[0].resolve([{ reason: "TEST first" }]);
    await a;
    assert.equal(h.s.historyIssueId.value, 2);
    assert.equal(h.s.events.value[0].reason, "TEST second");
    count++;
  }
  {
    const h = harness(),
      a = h.s.history(1);
    assert.equal(h.history[0].args.params.id, 1);
    h.history[0].resolve([{ reason: "TEST valid" }]);
    await a;
    assert.equal(h.s.historyVisible.value, true);
    assert.equal(h.s.historyIssueId.value, 1);
    const b = h.s.history(2);
    assert.equal(h.s.historyVisible.value, false);
    assert.equal(h.s.historyIssueId.value, undefined);
    assert.equal(h.s.events.value.length, 0);
    h.history[1].resolve([]);
    await b;
    assert.equal(h.s.historyIssueId.value, 2);
    assert.equal(h.s.historyVisible.value, true);
    count++;
  }
  for (const kind of ["refresh", "leave", "unmount"]) {
    const h = harness(),
      a = h.s.history(1);
    h.history[0].resolve([{ reason: "TEST valid" }]);
    await a;
    if (kind === "refresh") {
      const b = h.s.load();
      assert.equal(h.s.historyVisible.value, false);
      h.lists[0].resolve([row(1)]);
      await b;
    }
    if (kind === "leave") h.off.forEach((f) => f());
    if (kind === "unmount") h.unmount.forEach((f) => f());
    assert.equal(h.s.historyVisible.value, false);
    assert.equal(h.s.events.value.length, 0);
    assert.equal(h.s.historyIssueId.value, undefined);
    count++;
  }
  {
    const h = harness(),
      a = h.s.history(1);
    h.history[0].reject(new Error("TEST failure"));
    await assert.rejects(a, /TEST failure/);
    assert.equal(h.s.historyVisible.value, false);
    const b = h.s.history(1);
    h.history[1].resolve([]);
    await b;
    assert.equal(h.s.historyVisible.value, true);
    count++;
  }
  console.log("PASS " + count + " employee portal history scenarios");
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
