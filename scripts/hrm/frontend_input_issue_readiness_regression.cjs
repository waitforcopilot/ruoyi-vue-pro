// Execute the actual SFC script with Vue refs. Mocked requests only, no HTTP/DB.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_ISSUE_READINESS_SOURCE ||
    ui + "/src/views/hrm/payroll/input-issue/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
function harness(ready) {
  const prompts = [],
    posts = [],
    warnings = [];
  const pending = (q, args) =>
    new Promise((resolve, reject) => q.push({ args, resolve, reject }));
  const request = { get: async () => [], post: (args) => pending(posts, args) };
  const ctx = {
    exports: {},
    ref: vue.ref,
    reactive: vue.reactive,
    computed: vue.computed,
    defineOptions: () => {},
    onMounted: () => {},
    onActivated: () => {},
    onDeactivated: () => {},
    onBeforeUnmount: () => {},
    useMessage: () => ({
      prompt: (...args) => pending(prompts, args),
      warning: (m) => warnings.push(m),
    }),
    require: (name) =>
      name.includes("config/axios")
        ? request
        : name === "element-plus"
          ? { ElMessageBox: { close: () => {} } }
          : name === "dayjs"
            ? require(ui + "/node_modules/dayjs")
            : {},
  };
  vm.runInNewContext(
    ts.transpileModule(
      body + "\nexports.s={entityId,batchId,rows,review,submit,reopen};",
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
  s.rows.value = [
    { id: 1, batchId: 7, status: "SUBMITTED", sourceReady: ready },
  ];
  return { s, prompts, posts, warnings };
}
const tick = async () => {
  for (let i = 0; i < 6; i++) await Promise.resolve();
};
(async () => {
  let count = 0;
  for (const ready of [false, null, undefined, 0, "true"]) {
    const h = harness(ready);
    const p = h.s.review(1, true);
    assert.equal(
      h.prompts.length,
      0,
      "unready must not ask for approval opinion",
    );
    await p;
    assert.equal(h.posts.length, 0);
    assert.ok(h.warnings[0].includes("来源"));
    count++;
  }
  for (const ready of [false, null, undefined]) {
    const h = harness(ready),
      p = h.s.review(1, false);
    assert.equal(h.prompts.length, 1);
    h.prompts[0].resolve({ value: "TEST return for correction" });
    await tick();
    assert.equal(h.posts.length, 1);
    assert.equal(h.posts[0].args.data.approved, false);
    assert.equal(h.posts[0].args.data.reason, "TEST return for correction");
    h.posts[0].resolve(1);
    await p;
    count++;
  }
  for (const ready of [false, null, undefined]) {
    const h = harness(true),
      p = h.s.review(1, true);
    assert.equal(h.prompts.length, 1);
    h.s.rows.value[0].sourceReady = ready;
    h.prompts[0].resolve({ value: "TEST stale source" });
    await tick();
    assert.equal(h.posts.length, 0, "changed readiness must prevent approval");
    await p;
    assert.ok(h.warnings[0].includes("来源"));
    count++;
  }
  {
    const h = harness(true),
      p = h.s.review(1, true);
    h.prompts[0].resolve({ value: "TEST actual source verified" });
    await tick();
    assert.equal(h.posts.length, 1);
    assert.equal(h.posts[0].args.data.approved, true);
    assert.equal(h.posts[0].args.data.reason, "TEST actual source verified");
    h.posts[0].resolve(1);
    await p;
    count++;
  }
  {
    const h = harness(false);
    await h.s.review(1, true);
    h.s.rows.value[0].sourceReady = true;
    const p = h.s.review(1, true);
    assert.equal(h.prompts.length, 1, "fresh ready source permits retry");
    h.prompts[0].resolve({ value: "TEST fresh" });
    await tick();
    assert.equal(h.posts.length, 1);
    h.posts[0].resolve(1);
    await p;
    count++;
  }
  for (const method of ["submit", "reopen"]) {
    const h = harness(false),
      p = h.s[method](1);
    assert.equal(h.prompts.length, 1, method + " remains available");
    h.prompts[0].resolve({ value: "TEST other action" });
    await tick();
    assert.equal(h.posts.length, 1);
    h.posts[0].resolve(1);
    await p;
    count++;
  }
  console.log("PASS " + count + " input-issue readiness scenarios");
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
