// Actual SFC + Vue refs, deferred mocked requests. No HTTP/DB mutations.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3"),
  ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
    process.env.HRM_PORTAL_LOAD_SOURCE ||
      ui + "/src/views/hrm/portal/salary/input-issue/index.vue",
    "utf8",
  ),
  body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
const row = (id) => ({
  id,
  batchId: 7,
  employeeId: 8,
  status: "OPEN",
  canSubmit: true,
});
function harness() {
  const gets = [],
    prompts = [],
    off = [],
    on = [],
    unmount = [];
  const pending = (q, args) =>
    new Promise((resolve, reject) => q.push({ args, resolve, reject }));
  const ctx = {
    exports: {},
    ref: vue.ref,
    defineOptions: () => {},
    onMounted: () => {},
    onActivated: (f) => on.push(f),
    onDeactivated: (f) => off.push(f),
    onBeforeUnmount: (f) => unmount.push(f),
    useMessage: () => ({ prompt: (...args) => pending(prompts, args) }),
    require: (name) =>
      name.includes("config/axios")
        ? { get: (args) => pending(gets, args) }
        : name === "dayjs"
          ? require(ui + "/node_modules/dayjs")
          : name === "element-plus"
            ? { ElMessageBox: { close: () => {} } }
            : {},
  };
  vm.runInNewContext(
    ts.transpileModule(
      body +
        '\nexports.s={rows,loading,loadError:typeof loadError!=="undefined"?loadError:ref(false),load,submit,history};',
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
  s.rows.value = [row(1)];
  return { s, gets, prompts, off, on, unmount };
}
(async () => {
  let count = 0;
  {
    const h = harness(),
      p = h.s.load();
    assert.equal(
      h.s.rows.value.length,
      0,
      "refresh invalidates old rows immediately",
    );
    assert.equal(h.s.loading.value, true);
    await h.s.submit(1);
    assert.equal(h.prompts.length, 0);
    h.gets[0].reject(new Error("TEST timeout"));
    await p;
    assert.equal(h.s.loadError.value, true);
    assert.equal(h.s.loading.value, false);
    await h.s.submit(1);
    await h.s.history(1);
    assert.equal(h.prompts.length, 0);
    assert.equal(h.gets.length, 1);
    count++;
  }
  for (const result of [[], [row(2)]]) {
    const h = harness(),
      a = h.s.load();
    h.gets[0].reject(new Error("TEST timeout"));
    await a;
    const b = h.s.load();
    assert.equal(h.s.loadError.value, false);
    h.gets[1].resolve(result);
    await b;
    assert.equal(h.s.loadError.value, false);
    assert.equal(h.s.rows.value.length, result.length);
    assert.equal(h.s.loading.value, false);
    count++;
  }
  {
    const h = harness(),
      a = h.s.load(),
      b = h.s.load();
    h.gets[1].resolve([row(2)]);
    await b;
    h.gets[0].reject(new Error("TEST old failure"));
    await a;
    assert.equal(h.s.loadError.value, false);
    assert.equal(h.s.rows.value[0].id, 2);
    count++;
  }
  {
    const h = harness(),
      a = h.s.load(),
      b = h.s.load();
    h.gets[1].reject(new Error("TEST current failure"));
    await b;
    h.gets[0].resolve([row(1)]);
    await a;
    assert.equal(h.s.loadError.value, true);
    assert.equal(h.s.rows.value.length, 0);
    count++;
  }
  {
    const h = harness(),
      a = h.s.load(),
      b = h.s.load();
    h.gets[0].reject(new Error("TEST old failure"));
    await a;
    assert.equal(h.s.loading.value, true);
    assert.equal(h.s.loadError.value, false);
    h.gets[1].resolve([row(2)]);
    await b;
    assert.equal(h.s.loading.value, false);
    count++;
  }
  for (const kind of ["leave", "unmount"]) {
    const h = harness(),
      a = h.s.load();
    (kind === "leave" ? h.off : h.unmount).forEach((f) => f());
    h.gets[0].reject(new Error("TEST inactive failure"));
    await a;
    assert.equal(h.s.loadError.value, false);
    assert.equal(h.s.loading.value, false);
    count++;
  }
  {
    const h = harness(),
      a = h.s.load();
    h.off.forEach((f) => f());
    const b = h.on[0]();
    h.gets[0].reject(new Error("TEST preactivation failure"));
    await a;
    assert.equal(h.s.loadError.value, false);
    assert.equal(h.s.loading.value, true);
    h.gets[1].resolve([row(2)]);
    await b;
    assert.equal(h.s.rows.value[0].id, 2);
    count++;
  }
  console.log("PASS " + count + " employee portal load failure scenarios");
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
