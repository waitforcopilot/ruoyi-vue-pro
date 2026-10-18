// Actual SFC/Vue state, mocked API. No browser or database mutations.
const fs = require("node:fs"),
  vm = require("node:vm"),
  path = require("node:path"),
  assert = require("node:assert/strict");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  ui + "/src/views/hrm/payroll/requirement/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
function harness() {
  const posts = [],
    warnings = [],
    prompts = [],
    confirmations = [],
    assignments = [],
    off = [];
  const route = { query: {}, fullPath: "/hrm-payroll/requirement?module=COL" };
  const ctx = {
    exports: {},
    ref: vue.ref,
    computed: vue.computed,
    watch: () => {},
    onMounted: () => {},
    onActivated: () => {},
    onDeactivated: (f) => off.push(f),
    onBeforeUnmount: (f) => off.push(f),
    defineOptions: () => {},
    useRoute: () => route,
    useRouter: () => ({}),
    useMessage: () => ({
      warning: (x) => warnings.push(x),
      success: () => {},
      confirm: (...args) =>
        new Promise((resolve, reject) =>
          prompts.push({ args, resolve, reject }),
        ),
    }),
    require: (name) =>
      name.includes("requirement") && name.includes("api")
        ? {
            assignRequirement: async (id, version, reviewerId) =>
              assignments.push({ id, version, reviewerId }),
            confirmRequirement: async (id, version) =>
              confirmations.push({ id, version }),
            saveRequirement: async (data) => posts.push(data),
            listRequirements: async () => [],
            getLegacyRequirementProject: async () => ({ id: 1 }),
          }
        : name === "@/utils/permission"
          ? { checkPermi: () => true }
          : name === "@vueuse/core"
            ? { useWindowSize: () => ({ width: vue.ref(1000) }) }
            : name.includes("user") && name.includes("store")
              ? { useUserStore: () => ({ getUser: { id: 2 } }) }
              : name === "../requirements"
                ? {
                    payrollModules: { COL: "需求征集" },
                    payrollModule: () => undefined,
                  }
                : {},
  };
  vm.runInNewContext(
    ts.transpileModule(
      body +
        "\nexports.s={edit,form,formRef,reviewRevision,save,confirmVersion,rows,module,confirming,openAssignment,submitAssignment,assignmentVisible,assignmentRow,assignmentReviewerId};",
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
  s.formRef.value = { validate: async () => true };
  return {
    s,
    posts,
    warnings,
    prompts,
    confirmations,
    assignments,
    route,
    off,
  };
}
const original = {
  id: 47,
  projectId: 1,
  code: "COL-900001",
  module: "COL",
  description: "old",
  fieldMapping: "old -> field",
  sourceSystem: "TEST",
  sourceOwnerId: 3,
  reviewerId: 2,
  priority: 2,
  status: "CONFIRMED",
  readiness: "MISSING",
  acceptance: "test",
  evidence: "proof",
  version: 4,
};
(async () => {
  let count = 0;
  for (const field of [
    "code",
    "module",
    "description",
    "fieldMapping",
    "sourceSystem",
    "sourceOwnerId",
    "reviewerId",
    "acceptance",
    "evidence",
    "priority",
  ]) {
    const h = harness();
    h.s.edit(original);
    h.s.form.value[field] =
      typeof original[field] === "number" ? 999 : "CHANGED";
    assert.equal(h.s.reviewRevision.value, true, field);
    await h.s.save();
    assert.equal(h.posts[0].status, "REVIEWING");
    assert.equal(original.status, "CONFIRMED");
    count++;
  }
  {
    const h = harness();
    h.s.edit(original);
    await h.s.save();
    assert.equal(h.posts[0].status, "CONFIRMED");
    assert.equal(h.s.reviewRevision.value, false);
    count++;
  }
  {
    const h = harness();
    h.s.edit({ ...original, status: "REVIEWING" });
    h.s.form.value.status = "CONFIRMED";
    await h.s.save();
    assert.equal(h.posts.length, 0);
    assert.equal(h.warnings.length, 1);
    count++;
  }
  {
    const h = harness();
    h.s.edit({ ...original, status: "REVIEWING", reviewerId: 3 });
    h.s.form.value.status = "CONFIRMED";
    await h.s.save();
    assert.equal(h.posts.length, 0);
    assert.equal(h.warnings.length, 1);
    count++;
  }
  {
    const h = harness();
    h.s.edit(original);
    h.s.form.value.description = "changed";
    h.s.form.value.status = "DISPUTED";
    await h.s.save();
    assert.equal(h.posts[0].status, "DISPUTED");
    count++;
  }
  {
    const h = harness();
    h.s.edit(original);
    h.s.form.value.readiness = "PARTIAL";
    assert.equal(h.s.reviewRevision.value, false);
    count++;
  }
  for (const mode of [
    "confirm",
    "cancel",
    "close",
    "route",
    "version",
    "leave",
    "duplicate",
    "wrongReviewer",
  ]) {
    const h = harness();
    const row = { ...original, status: "REVIEWING" };
    h.s.rows.value = [row];
    if (mode === "wrongReviewer") row.reviewerId = 3;
    const task = h.s.confirmVersion(row);
    if (mode === "wrongReviewer") {
      await task;
      assert.equal(h.prompts.length, 0);
      count++;
      continue;
    }
    assert.equal(h.prompts.length, 1);
    assert.equal(h.posts.length, 0, "confirmation must not save form fields");
    if (mode === "route") h.route.fullPath = "/index";
    if (mode === "version") h.s.rows.value = [{ ...row, version: 99 }];
    if (mode === "leave") h.off[0]();
    if (mode === "duplicate") await h.s.confirmVersion(row);
    if (mode === "cancel" || mode === "close") h.prompts[0].reject(mode);
    else h.prompts[0].resolve();
    await task;
    const valid = mode === "confirm" || mode === "duplicate";
    assert.equal(h.confirmations.length, valid ? 1 : 0, mode);
    if (valid) assert.deepEqual(h.confirmations[0], { id: 47, version: 4 });
    assert.equal(h.s.confirming.value, false);
    count++;
  }
  for (const mode of [
    "assign",
    "clear",
    "cancel",
    "route",
    "module",
    "version",
    "leave",
    "self",
    "duplicate",
  ]) {
    const h = harness(),
      row = { ...original, status: "REVIEWING", reviewerId: 3, version: 4 };
    h.s.rows.value = [row];
    h.s.openAssignment(row);
    h.s.assignmentReviewerId.value = mode === "clear" ? undefined : 1;
    if (mode === "cancel") h.s.assignmentVisible.value = false;
    if (mode === "route") h.route.fullPath = "/index";
    if (mode === "module") h.s.module.value = "TAX";
    if (mode === "version") h.s.rows.value = [{ ...row, version: 5 }];
    if (mode === "leave") h.off[0]();
    if (mode === "self") h.s.assignmentReviewerId.value = 2;
    const task = h.s.submitAssignment();
    if (mode === "duplicate") await h.s.submitAssignment();
    await task;
    const valid = ["assign", "clear", "duplicate"].includes(mode);
    assert.equal(h.assignments.length, valid ? 1 : 0, mode);
    if (valid)
      assert.deepEqual(h.assignments[0], {
        id: 47,
        version: 4,
        reviewerId: mode === "clear" ? undefined : 1,
      });
    assert.equal(h.posts.length, 0, "assignment must not save form content");
    count++;
  }
  console.log(
    `PASS ${count} requirement revision and explicit confirmation scenarios`,
  );
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
