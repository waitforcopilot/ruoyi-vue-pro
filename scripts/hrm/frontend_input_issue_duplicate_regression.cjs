// Node.js 22; actual SFC and Vue refs, mocked requests; no HTTP or database writes.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_ISSUE_DUPLICATE_SOURCE ||
    ui + "/src/views/hrm/payroll/input-issue/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
function harness() {
  const options = [],
    posts = [],
    messages = [],
    unmounts = [];
  const pending = (q, args) =>
    new Promise((resolve, reject) => q.push({ args, resolve, reject }));
  const request = {
    get: (args) =>
      args.url.endsWith("/options")
        ? pending(options, args)
        : Promise.resolve(
            args.url.endsWith("/selectors")
              ? { entities: [], batches: [] }
              : [],
          ),
    post: (args) => pending(posts, args),
  };
  const ctx = {
    exports: {},
    ref: vue.ref,
    reactive: vue.reactive,
    computed: vue.computed,
    defineOptions: () => {},
    onMounted: () => {},
    onBeforeUnmount: (f) => unmounts.push(f),
    useMessage: () => ({
      success: (m) => messages.push(m),
      warning: (m) => messages.push(m),
    }),
    crypto: { randomUUID: () => "TEST" },
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
        "\nexports.s={entityId,batchId,employeeId,rows,draft,dialog,saving,people,openAssign,openCreate,save,changeEntity,loadBatch,loadSignals};",
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
  return { s, options, posts, messages, unmounts };
}
const row = (id) => ({
  id,
  batchId: 7,
  employeeId: 8,
  kind: "ATTENDANCE",
  ownerId: 254,
  reviewerId: 249,
  dueTime: "2026-10-25T18:00:00",
});
const people = { owners: [{ id: 254 }], reviewers: [{ id: 249 }] };
async function open(h, id = 1) {
  const p = h.s.openAssign(row(id));
  h.options.at(-1).resolve(people);
  await p;
}
(async () => {
  let count = 0;
  const existing = (overrides = {}) => ({
    ...row(1),
    status: "OPEN",
    ...overrides,
  });
  for (const status of ["OPEN", "SUBMITTED"]) {
    const h = harness();
    h.s.rows.value = [existing({ status })];
    const opening = h.s.openCreate({
      employeeId: 8,
      kind: "ATTENDANCE",
      ready: false,
    });
    assert.equal(h.options.length, 0, status + ": duplicate must not open");
    await opening;
    assert.equal(h.s.dialog.value, false);
    assert.ok(h.messages[0].includes("#1"));
    count++;
  }
  for (const overrides of [
    { status: "CLOSED" },
    { batchId: 6 },
    { employeeId: 9 },
    { kind: "OVERTIME" },
  ]) {
    const h = harness();
    h.s.rows.value = [existing(overrides)];
    const task = h.s.openCreate({
      employeeId: 8,
      kind: "ATTENDANCE",
      ready: false,
    });
    assert.equal(h.options.length, 1);
    h.options[0].resolve(people);
    await task;
    assert.equal(h.s.dialog.value, true);
    count++;
  }
  {
    const h = harness();
    const task = h.s.openCreate({
      employeeId: 8,
      kind: "ATTENDANCE",
      ready: false,
    });
    h.s.rows.value = [existing()];
    h.options[0].resolve(people);
    await task;
    assert.equal(h.s.dialog.value, false);
    assert.ok(h.messages[0].includes("#1"));
    count++;
  }
  {
    const h = harness();
    const task = h.s.openCreate({
      employeeId: 8,
      kind: "ATTENDANCE",
      ready: false,
    });
    h.options[0].resolve(people);
    await task;
    h.s.rows.value = [existing()];
    h.s.draft.ownerId = 253;
    h.s.draft.reviewerId = 249;
    h.s.draft.reason = "TEST";
    await h.s.save();
    assert.equal(h.posts.length, 0);
    assert.ok(h.messages[0].includes("#1"));
    count++;
  }
  {
    const h = harness();
    await h.s.openCreate({ employeeId: 8, kind: "ATTENDANCE", ready: true });
    assert.equal(h.options.length, 0);
    count++;
  }
  {
    const h = harness();
    h.s.rows.value = [existing()];
    const task = h.s.openCreate({
      employeeId: 8,
      kind: "OVERTIME",
      ready: false,
    });
    h.options[0].resolve(people);
    await task;
    h.s.draft.ownerId = 253;
    h.s.draft.reviewerId = 249;
    h.s.draft.reason = "TEST";
    const save = h.s.save();
    assert.equal(h.posts[0].args.data.kind, "OVERTIME");
    h.posts[0].resolve(2);
    await save;
    assert.equal(h.s.dialog.value, false);
    count++;
  }
  {
    const h = harness();
    h.s.rows.value = [existing()];
    await open(h);
    h.s.draft.ownerId = 253;
    h.s.draft.reason = "TEST";
    const save = h.s.save();
    assert.ok(h.posts[0].args.url.endsWith("/reassign"));
    h.posts[0].resolve(1);
    await save;
    count++;
  }
  console.log(`PASS ${count} input issue duplicate scenarios`);
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
