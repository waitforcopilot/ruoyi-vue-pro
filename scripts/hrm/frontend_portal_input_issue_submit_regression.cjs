// Actual SFC + Vue refs, deferred mocked requests; no HTTP/database writes.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_PORTAL_ISSUE_SOURCE ||
    ui + "/src/views/hrm/portal/salary/input-issue/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
const row = () => ({
  id: 1,
  batchId: 7,
  employeeId: 8,
  status: "OPEN",
  canSubmit: true,
});
function harness(manualGets = false) {
  const prompts = [],
    posts = [],
    gets = [],
    success = [],
    off = [],
    on = [],
    unmount = [];
  let closes = 0;
  const pending = (q, args) =>
    new Promise((resolve, reject) => q.push({ args, resolve, reject }));
  const request = {
    get: (args) =>
      manualGets
        ? pending(gets, args)
        : (gets.push({ args }), Promise.resolve([row()])),
    post: (args) => pending(posts, args),
  };
  const ctx = {
    exports: {},
    ref: vue.ref,
    defineOptions: () => {},
    onMounted: () => {},
    onActivated: (f) => on.push(f),
    onDeactivated: (f) => off.push(f),
    onBeforeUnmount: (f) => unmount.push(f),
    useMessage: () => ({
      prompt: (...args) => pending(prompts, args),
      success: (m) => success.push(m),
    }),
    require: (name) =>
      name.includes("config/axios")
        ? request
        : name === "dayjs"
          ? require(ui + "/node_modules/dayjs")
          : name === "element-plus"
            ? { ElMessageBox: { close: () => closes++ } }
            : { labels: {}, states: {}, actions: {} },
  };
  vm.runInNewContext(
    ts.transpileModule(body + "\nexports.s={rows,loading,submit,load};", {
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        target: ts.ScriptTarget.ES2022,
      },
    }).outputText,
    ctx,
  );
  const s = ctx.exports.s;
  s.rows.value = [row()];
  return {
    s,
    prompts,
    posts,
    gets,
    success,
    off,
    on,
    unmount,
    get closes() {
      return closes;
    },
  };
}
const tick = async () => {
  for (let i = 0; i < 8; i++) await Promise.resolve();
};
(async () => {
  let count = 0;
  for (const kind of [
    "leave",
    "unmount",
    "refresh",
    "remove",
    "status",
    "permission",
    "batch",
    "employee",
  ]) {
    const h = harness(),
      p = h.s.submit(1);
    if (kind === "leave") {
      h.off.forEach((f) => f());
      for (const f of h.on) await f();
    }
    if (kind === "unmount") h.unmount.forEach((f) => f());
    if (kind === "refresh") await h.s.load();
    if (kind === "remove") h.s.rows.value = [];
    if (kind === "status") h.s.rows.value[0].status = "SUBMITTED";
    if (kind === "permission") h.s.rows.value[0].canSubmit = false;
    if (kind === "batch") h.s.rows.value[0].batchId = 9;
    if (kind === "employee") h.s.rows.value[0].employeeId = 9;
    h.prompts[0].resolve({ value: "TEST stale" });
    await tick();
    assert.equal(h.posts.length, 0, "stale " + kind + " must not POST");
    await p;
    if (["leave", "unmount", "refresh"].includes(kind))
      assert.equal(h.closes, 1);
    count++;
  }
  for (const kind of [
    "missing",
    "permission",
    "status",
    "loading",
    "inactive",
  ]) {
    const h = harness();
    if (kind === "missing") h.s.rows.value = [];
    if (kind === "permission") h.s.rows.value[0].canSubmit = false;
    if (kind === "status") h.s.rows.value[0].status = "SUBMITTED";
    if (kind === "loading") h.s.loading.value = true;
    if (kind === "inactive") h.off.forEach((f) => f());
    const p = h.s.submit(1);
    assert.equal(h.prompts.length, 0, "guard " + kind);
    await p;
    count++;
  }
  for (const cancel of ["cancel", "close"]) {
    const h = harness(),
      p = h.s.submit(1);
    h.prompts[0].reject(cancel);
    await p;
    assert.equal(h.posts.length, 0);
    const retry = h.s.submit(1);
    assert.equal(h.prompts.length, 2);
    h.prompts[1].reject("cancel");
    await retry;
    count++;
  }
  {
    const h = harness(),
      p = h.s.submit(1);
    await h.s.submit(1);
    assert.equal(h.prompts.length, 1);
    h.prompts[0].resolve({ value: "TEST valid" });
    await tick();
    assert.equal(h.posts.length, 1);
    await h.s.submit(1);
    assert.equal(h.prompts.length, 1);
    assert.equal(h.posts[0].args.url, "/hrm/portal/payroll-input-issue/submit");
    assert.equal(h.posts[0].args.data.evidence, "TEST valid");
    h.posts[0].resolve(true);
    await p;
    assert.equal(h.success.length, 1);
    assert.equal(h.gets.length, 1);
    count++;
  }
  {
    const h = harness(),
      p = h.s.submit(1);
    h.prompts[0].resolve({ value: "TEST sent" });
    await tick();
    h.off.forEach((f) => f());
    for (const f of h.on) await f();
    const gets = h.gets.length;
    h.posts[0].resolve(true);
    await p;
    assert.equal(h.success.length, 0);
    assert.equal(h.gets.length, gets, "old completion cannot reload new page");
    count++;
  }
  {
    const h = harness(),
      old = h.s.submit(1);
    h.off.forEach((f) => f());
    for (const f of h.on) await f();
    const fresh = h.s.submit(1);
    h.prompts[0].resolve({ value: "TEST old" });
    await old;
    await h.s.submit(1);
    assert.equal(
      h.prompts.length,
      2,
      "old finally must not unlock current prompt",
    );
    h.prompts[1].reject("cancel");
    await fresh;
    count++;
  }
  {
    const h = harness(),
      p = h.s.submit(1);
    h.prompts[0].resolve({ value: "TEST failure" });
    await tick();
    h.posts[0].reject(new Error("TEST"));
    await assert.rejects(p, /TEST/);
    const retry = h.s.submit(1);
    assert.equal(h.prompts.length, 2);
    h.prompts[1].reject("cancel");
    await retry;
    count++;
  }
  {
    const h = harness(true),
      a = h.s.load(),
      b = h.s.load();
    h.gets[1].resolve([{ ...row(), id: 2 }]);
    await b;
    h.gets[0].resolve([row()]);
    await a;
    assert.equal(h.s.rows.value[0].id, 2);
    assert.equal(h.s.loading.value, false);
    count++;
  }
  {
    const h = harness(true),
      a = h.s.load();
    h.off.forEach((f) => f());
    const b = h.on[0]();
    h.gets[0].resolve([{ ...row(), id: 99 }]);
    await a;
    assert.equal(h.s.rows.value.length, 0);
    assert.equal(h.s.loading.value, true);
    h.gets[1].resolve([row()]);
    await b;
    assert.equal(h.s.loading.value, false);
    count++;
  }
  console.log(
    "PASS " + count + " employee portal input-issue submit scenarios",
  );
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
