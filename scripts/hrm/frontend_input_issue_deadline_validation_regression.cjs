// Actual SFC and Vue refs, controlled clock and mocked requests; no HTTP/DB.
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
  process.env.HRM_ISSUE_DEADLINE_VALIDATION_SOURCE ||
    ui + "/src/views/hrm/payroll/input-issue/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
function harness() {
  let now = "2026-10-18T15:00:00";
  const posts = [],
    warnings = [];
  const request = {
    get: async () => ({ owners: [], reviewers: [] }),
    post: async (args) => {
      posts.push(args);
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
    onActivated: () => {},
    onDeactivated: () => {},
    onBeforeUnmount: () => {},
    useMessage: () => ({ warning: (m) => warnings.push(m), success: () => {} }),
    crypto: { randomUUID: () => "TEST" },
    require: (name) =>
      name.includes("config/axios")
        ? request
        : name === "dayjs"
          ? (value) => dayjs(value === undefined ? now : value)
          : name === "element-plus"
            ? { ElMessageBox: { close: () => {} } }
            : {},
  };
  vm.runInNewContext(
    ts.transpileModule(
      body +
        "\nexports.s={entityId,batchId,draft,dialog,saving,openAssign,openCreate,save};",
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
  return { s, posts, warnings, setNow: (value) => (now = value) };
}
async function open(h, mode) {
  if (mode === "assign")
    await h.s.openAssign({
      id: 1,
      batchId: 7,
      employeeId: 8,
      kind: "ATTENDANCE",
      ownerId: 253,
      reviewerId: 249,
      dueTime: "2026-10-25T18:00:00",
    });
  else await h.s.openCreate({ employeeId: 8, kind: "OVERTIME", ready: false });
  h.s.draft.ownerId = 253;
  h.s.draft.reviewerId = 249;
  h.s.draft.reason = "TEST deadline validation";
}
(async () => {
  let count = 0;
  for (const mode of ["assign", "create"]) {
    for (const due of [
      undefined,
      null,
      "",
      "invalid",
      "2026-10-18T14:59:59",
      "2026-10-18T15:00:00",
    ]) {
      const h = harness();
      await open(h, mode);
      h.s.draft.dueTime = due;
      await h.s.save();
      assert.equal(h.posts.length, 0, mode + " invalid deadline must not POST");
      assert.equal(h.s.saving.value, false);
      assert.equal(h.s.dialog.value, true);
      assert.ok(h.warnings[0].includes("截止时间"));
      count++;
    }
    for (const due of ["2026-10-18T15:00:01", "2026-10-25T18:00:00"]) {
      const h = harness();
      await open(h, mode);
      h.s.draft.dueTime = due;
      await h.s.save();
      assert.equal(h.posts.length, 1);
      assert.equal(h.posts[0].data.dueTime, due);
      assert.equal(
        h.posts[0].url,
        "/hrm/payroll/input-issue/" +
          (mode === "assign" ? "reassign" : "create"),
      );
      count++;
    }
    {
      const h = harness();
      await open(h, mode);
      h.s.draft.dueTime = "2026-10-18T15:00:01";
      h.setNow("2026-10-18T15:00:02");
      await h.s.save();
      assert.equal(h.posts.length, 0, "deadline expired while dialog open");
      assert.equal(
        h.s.draft.dueTime,
        "2026-10-18T15:00:01",
        "must not silently extend",
      );
      h.s.draft.dueTime = "2026-10-25T18:00:00";
      await h.s.save();
      assert.equal(h.posts.length, 1, "user correction allows retry");
      count++;
    }
  }
  console.log("PASS " + count + " input-issue deadline validation scenarios");
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
