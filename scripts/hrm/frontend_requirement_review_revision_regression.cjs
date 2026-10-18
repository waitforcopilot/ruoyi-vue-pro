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
    warnings = [];
  const ctx = {
    exports: {},
    ref: vue.ref,
    computed: vue.computed,
    watch: () => {},
    onMounted: () => {},
    defineOptions: () => {},
    useRoute: () => ({ query: {} }),
    useRouter: () => ({}),
    useMessage: () => ({ warning: (x) => warnings.push(x), success: () => {} }),
    require: (name) =>
      name.includes("requirement") && name.includes("api")
        ? {
            saveRequirement: async (data) => posts.push(data),
            listRequirements: async () => [],
            getLegacyRequirementProject: async () => ({ id: 1 }),
          }
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
      body + "\nexports.s={edit,form,formRef,reviewRevision,save};",
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
  return { s, posts, warnings };
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
    assert.equal(h.posts[0].status, "CONFIRMED");
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
  console.log(`PASS ${count} requirement revision form scenarios`);
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
