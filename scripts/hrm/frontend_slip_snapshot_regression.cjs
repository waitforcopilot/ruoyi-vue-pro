// Run with Node.js 22 after installing the Vue frontend dependencies.
const fs = require("node:fs");
const vm = require("node:vm");
const assert = require("node:assert/strict");
const root = require("node:path").resolve(__dirname, "../..");
const ts = require(
  root + "/yudao-ui/yudao-ui-admin-vue3/node_modules/typescript",
);
const file =
  "yudao-ui/yudao-ui-admin-vue3/src/views/hrm/salary/slip/send-record/SalarySlipSendForm.vue";
function harness(source) {
  const script = source.match(
    /<script lang="ts" setup>([\s\S]*?)<\/script>/,
  )[1];
  const sent = [],
    warnings = [];
  const api = { sendSalarySlip: async (x) => sent.push(x) };
  const context = {
    exports: {},
    require: (name) =>
      name.includes("send-record")
        ? api
        : name.includes("constants")
          ? { HrmSalaryOptionCategoryCode: { ROOT: 0 } }
          : {},
    ref: (x) => ({ value: x }),
    reactive: (x) => x,
    computed: (f) => ({
      get value() {
        return f();
      },
    }),
    useMessage: () => ({ warning: (x) => warnings.push(x), success: () => {} }),
    useI18n: () => ({ t: (x) => x }),
    defineOptions: () => {},
    defineExpose: () => {},
    defineEmits: () => () => {},
  };
  const expose =
    "\nexports.state={sendTemplate,templateEditorRef,currentStep,employeeLoaded,monthRecordId,selectedEmployeeIdSet,handleNextStep,submitForm,getTemplateOptions};";
  vm.runInNewContext(
    ts.transpileModule(script + expose, {
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        target: ts.ScriptTarget.ES2022,
      },
    }).outputText,
    context,
  );
  const s = context.exports.state;
  s.sendTemplate.value = {
    name: "snapshot",
    options: [{ code: 1, name: "旧布局" }],
  };
  s.employeeLoaded.value = true;
  s.monthRecordId.value = 6;
  s.selectedEmployeeIdSet.value = new Set([8]);
  return { s, sent, warnings };
}
const options = [
  { code: -1, name: "收入", type: 1 },
  { code: 1, name: "基本工资", parentCode: -1, sort: 1 },
  { code: 2, name: "实发工资", sort: 2 },
];
async function sendAfterUnmount(h) {
  h.s.templateEditorRef.value = {
    validate: () => undefined,
    getNormalizedOptions: () => options.map((x) => ({ ...x })),
  };
  await h.s.handleNextStep();
  assert.equal(h.s.currentStep.value, 1);
  h.s.templateEditorRef.value = undefined;
  await h.s.submitForm(false);
  return h.sent[0];
}
(async () => {
  const source = fs.readFileSync(root + "/" + file, "utf8");
  const fixed = harness(source);
  const payload = await sendAfterUnmount(fixed);
  assert.equal(JSON.stringify(payload.options), JSON.stringify(options));
  assert.equal(payload.employeeIds[0], 8);
  assert.equal(JSON.stringify(payload.channels), JSON.stringify(["STATION"]));
  // Returning to step zero and editing must replace the previous publication snapshot.
  fixed.s.currentStep.value = 0;
  const edited = [{ code: 2, name: "实发工资", sort: 1 }];
  fixed.s.templateEditorRef.value = {
    validate: () => undefined,
    getNormalizedOptions: () => edited,
  };
  await fixed.s.handleNextStep();
  fixed.s.templateEditorRef.value = undefined;
  await fixed.s.submitForm(true);
  assert.equal(JSON.stringify(fixed.sent[1].options), JSON.stringify(edited));
  assert.equal(fixed.sent[1].all, true);
  const unready = harness(source);
  await unready.s.handleNextStep();
  assert.equal(unready.s.currentStep.value, 0);
  assert.equal(unready.warnings.length, 1);
  const invalid = harness(source);
  invalid.s.templateEditorRef.value = {
    validate: () => "invalid",
    getNormalizedOptions: () => {
      throw Error("must not snapshot invalid editor");
    },
  };
  await invalid.s.handleNextStep();
  assert.equal(invalid.s.currentStep.value, 0);
  console.log(
    "Passed: preserved layout after unmount; selected/all send, back/edit, unready and invalid guards passed.",
  );
  console.log(
    "Scope: actual SFC script with mocked dependencies; no browser or business API calls.",
  );
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
