// Node.js 22; actual SFC with mocked requests. No HTTP or database writes.
process.env.TZ = "Asia/Shanghai";
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue"),
  dayjs = require(ui + "/node_modules/dayjs");
const source = fs.readFileSync(
  process.env.HRM_ISSUE_SOURCE ||
    ui + "/src/views/hrm/payroll/input-issue/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
function harness() {
  const posts = [];
  const request = {
    get: async ({ url }) =>
      url.endsWith("/options") ? { owners: [], reviewers: [] } : [],
    post: async (args) => {
      posts.push(JSON.parse(JSON.stringify(args)));
      return 1;
    },
  };
  const ctx = {
    exports: {},
    ref: vue.ref,
    reactive: vue.reactive,
    computed: vue.computed,
    defineOptions: () => {},
    onMounted: () => {},
    onBeforeUnmount: () => {},
    useMessage: () => ({ success: () => {} }),
    crypto: { randomUUID: () => "test-key" },
    require: (name) =>
      name.includes("config/axios")
        ? request
        : name === "dayjs"
          ? dayjs
          : name.includes("labels")
            ? { labels: {}, states: {}, actions: {} }
            : {},
  };
  vm.runInNewContext(
    ts.transpileModule(
      body +
        "\nexports.s={entityId,batchId,draft,dialog,openAssign,openCreate,save};",
      {
        compilerOptions: {
          module: ts.ModuleKind.CommonJS,
          target: ts.ScriptTarget.ES2022,
        },
      },
    ).outputText,
    ctx,
  );
  ctx.exports.s.entityId.value = 4;
  ctx.exports.s.batchId.value = 7;
  return { s: ctx.exports.s, posts };
}
(async () => {
  let count = 0;
  for (const value of [
    1792922400000,
    "2026-10-25T18:00:00",
    "2026-10-25 18:00:00",
    "2026-01-01T09:30:00",
    0,
  ]) {
    const h = harness();
    const original = {
      id: 1,
      batchId: 7,
      employeeId: 8,
      kind: "ATTENDANCE",
      ownerId: 253,
      reviewerId: 249,
      dueTime: value,
    };
    const snapshot = JSON.stringify(original);
    await h.s.openAssign(original);
    const expected = dayjs(value).format("YYYY-MM-DDTHH:mm:ss");
    assert.equal(h.s.draft.dueTime, expected, "handoff must retain deadline");
    assert.equal(JSON.stringify(original), snapshot);
    h.s.draft.reason = "TEST handoff";
    await h.s.save();
    assert.equal(h.posts[0].url, "/hrm/payroll/input-issue/reassign");
    assert.equal(h.posts[0].data.dueTime, expected);
    count++;
  }
  {
    const h = harness();
    await h.s.openAssign({ id: 1, dueTime: 1792922400000 });
    h.s.draft.dueTime = "2026-10-26T19:00:00";
    h.s.draft.reason = "TEST deliberate deadline change";
    await h.s.save();
    assert.equal(h.posts[0].data.dueTime, "2026-10-26T19:00:00");
    count++;
  }
  {
    const h = harness();
    await h.s.openAssign({ id: 1, dueTime: 1792922400000 });
    h.s.draft.dueTime = "2027-01-01T00:00:00";
    await h.s.openAssign({ id: 2, dueTime: "2026-11-01T12:00:00" });
    assert.equal(h.s.draft.dueTime, "2026-11-01T12:00:00");
    count++;
  }
  for (const value of [null, undefined, "", "invalid"]) {
    const h = harness();
    await h.s.openAssign({ id: 1, dueTime: value });
    assert.equal(
      h.s.draft.dueTime,
      undefined,
      "missing deadline must not be silently invented",
    );
    count++;
  }
  {
    const h = harness();
    const before = dayjs().add(2, "day");
    await h.s.openCreate({ employeeId: 8, kind: "ATTENDANCE" });
    assert.ok(Math.abs(dayjs(h.s.draft.dueTime).diff(before, "second")) <= 1);
    assert.equal(h.s.draft.id, undefined);
    count++;
  }
  console.log(`PASS ${count} input issue deadline scenarios`);
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
