// Actual SFC + Vue state; all requests mocked, no HTTP/database mutations.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_ISSUE_ACTION_SOURCE ||
    ui + "/src/views/hrm/payroll/input-issue/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
function harness() {
  const prompts = [],
    posts = [],
    gets = [],
    off = [],
    on = [],
    unmount = [];
  let closes = 0;
  const pending = (queue, args) =>
    new Promise((resolve, reject) => queue.push({ args, resolve, reject }));
  const request = {
    get: async (args) => {
      gets.push(args);
      return [];
    },
    post: (args) => pending(posts, args),
  };
  const ctx = {
    exports: {},
    ref: vue.ref,
    reactive: vue.reactive,
    computed: vue.computed,
    defineOptions: () => {},
    onMounted: () => {},
    onActivated: (f) => on.push(f),
    onDeactivated: (f) => off.push(f),
    onBeforeUnmount: (f) => unmount.push(f),
    useMessage: () => ({
      prompt: (...args) => pending(prompts, args),
      warning: () => {},
      success: () => {},
    }),
    require: (name) =>
      name.includes("config/axios")
        ? request
        : name === "element-plus"
          ? { ElMessageBox: { close: () => closes++ } }
          : name === "dayjs"
            ? require(ui + "/node_modules/dayjs")
            : {},
  };
  vm.runInNewContext(
    ts.transpileModule(
      body +
        "\nexports.s={entityId,batchId,employeeId,rows,submit,review,reopen,changeEntity,loadBatch,loadSignals};",
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
  s.rows.value = [{ id: 1, batchId: 7, status: "OPEN", sourceReady: true }];
  return {
    s,
    prompts,
    posts,
    gets,
    off,
    on,
    unmount,
    get closes() {
      return closes;
    },
  };
}
const tick = async () => {
  for (let i = 0; i < 6; i++) await Promise.resolve();
};
(async () => {
  let count = 0;
  const cases = [
    ["submit", (s) => s.submit(1), "/submit", "evidence"],
    ["approve", (s) => s.review(1, true), "/review", "reason"],
    ["reject", (s) => s.review(1, false), "/review", "reason"],
    ["reopen", (s) => s.reopen(1), "/reopen", "reason"],
  ];
  for (const [name, run, url, key] of cases) {
    for (const kind of [
      "leave",
      "unmount",
      "entity",
      "batch",
      "row",
      "refresh",
      "employee",
    ]) {
      const h = harness(),
        p = run(h.s);
      assert.equal(h.prompts.length, 1);
      if (kind === "leave") {
        h.off.forEach((f) => f());
        h.on.forEach((f) => f());
      }
      if (kind === "unmount") h.unmount.forEach((f) => f());
      if (kind === "entity") {
        h.s.entityId.value = 2;
        await h.s.changeEntity();
      }
      if (kind === "batch") h.s.batchId.value = 8;
      if (kind === "row") h.s.rows.value[0].status = "CHANGED";
      if (kind === "refresh") await h.s.loadBatch();
      if (kind === "employee") {
        h.s.employeeId.value = 9;
        await h.s.loadSignals();
      }
      h.prompts[0].resolve({ value: "TEST stale" });
      await tick();
      assert.equal(h.posts.length, 0, name + " stale " + kind);
      await p;
      if (["leave", "unmount", "entity", "refresh", "employee"].includes(kind))
        assert.equal(h.closes, 1);
      count++;
    }
    const h = harness(),
      p = run(h.s);
    await run(h.s);
    assert.equal(h.prompts.length, 1, name + " duplicate blocked");
    h.prompts[0].resolve({ value: "TEST valid" });
    await tick();
    assert.equal(h.posts.length, 1);
    assert.equal(h.posts[0].args.url, "/hrm/payroll/input-issue" + url);
    assert.equal(h.posts[0].args.data[key], "TEST valid");
    if (url === "/review")
      assert.equal(h.posts[0].args.data.approved, name === "approve");
    h.posts[0].resolve(1);
    await p;
    assert.equal(h.gets.length, 2);
    count++;
    for (const cancel of ["cancel", "close"]) {
      const h = harness(),
        p = run(h.s);
      h.prompts[0].reject(cancel);
      await p;
      assert.equal(h.posts.length, 0);
      const again = run(h.s);
      assert.equal(h.prompts.length, 2);
      h.prompts[1].reject("cancel");
      await again;
      count++;
    }
    const h2 = harness(),
      p2 = run(h2.s);
    h2.prompts[0].resolve({ value: "TEST sent" });
    await tick();
    assert.equal(h2.posts.length, 1);
    h2.off.forEach((f) => f());
    h2.on.forEach((f) => f());
    h2.posts[0].resolve(1);
    await p2;
    assert.equal(
      h2.gets.length,
      0,
      name + " sent response cannot reload reactivated page",
    );
    count++;
  }
  console.log("PASS " + count + " input-issue action regression scenarios");
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
