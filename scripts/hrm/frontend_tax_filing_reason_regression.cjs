// Run with Node.js 22 after installing the Vue frontend dependencies.
const fs = require("node:fs");
const vm = require("node:vm");
const assert = require("node:assert/strict");
const root = require("node:path").resolve(__dirname, "../..");
const ui = root + "/yudao-ui/yudao-ui-admin-vue3";
const ts = require(ui + "/node_modules/typescript");
const file =
  "yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/tax-policy/TaxFilings.vue";
function harness(source, behavior, apiSource) {
  const calls = [];
  const request = { post: async (req) => calls.push(req), get: async () => [] };
  const apiContext = { exports: {}, require: () => request };
  vm.runInNewContext(
    ts.transpileModule(apiSource, {
      compilerOptions: { module: ts.ModuleKind.CommonJS },
    }).outputText,
    apiContext,
  );
  const api = {
    ...apiContext.exports,
    list: async () => [],
    detail: async () => ({ filing: { id: 6 }, receipts: [] }),
  };
  const context = {
    exports: {},
    require: (name) =>
      name.includes("tax-filing")
        ? api
        : name === "dayjs"
          ? require(ui + "/node_modules/dayjs")
          : {},
    defineProps: () => ({ entityId: 4, year: 2026 }),
    ref: (value) => ({ value }),
    computed: (f) => ({
      get value() {
        return f();
      },
    }),
    useMessage: () => ({ prompt: async () => behavior(context.exports.state) }),
    watch: () => {},
    onBeforeUnmount: () => {},
  };
  const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
  const expose =
    "\nexports.state={review,reviewReceipt,reconcile,data,detailVisible,invalidateList:()=>sequence++,invalidateDetail:()=>detailSequence++};";
  vm.runInNewContext(
    ts.transpileModule(body + expose, {
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        target: ts.ScriptTarget.ES2022,
      },
    }).outputText,
    context,
  );
  context.exports.state.data.value = { filing: { id: 6 }, receipts: [] };
  return { s: context.exports.state, calls };
}
(async () => {
  const apiPath =
    "yudao-ui/yudao-ui-admin-vue3/src/api/hrm/payroll/tax-filing.ts";
  const source = fs.readFileSync(root + "/" + file, "utf8"),
    apiSource = fs.readFileSync(root + "/" + apiPath, "utf8");
  const actions = [
    ["review", [6, true], "review"],
    ["review", [6, false], "review"],
    ["reviewReceipt", [9, true], "receipt-review"],
    ["reviewReceipt", [9, false], "receipt-review"],
    ["reconcile", [], "reconcile"],
  ];
  let scenarios = 0;
  for (const [fn, args, path] of actions) {
    let h = harness(
      source,
      () => ({ value: "  核对依据  ", action: "confirm" }),
      apiSource,
    );
    await h.s[fn](...args);
    assert.equal(h.calls.length, 1);
    assert.equal(h.calls[0].params.reason, "核对依据");
    assert.ok(h.calls[0].url.endsWith("/" + path));
    assert.equal(h.calls[0].params.id, args[0] || 6);
    if (args.length > 1) assert.equal(h.calls[0].params.approved, args[1]);
    scenarios++;
    h = harness(
      source,
      () => ({ value: " \n\t ", action: "confirm" }),
      apiSource,
    );
    await h.s[fn](...args);
    assert.equal(h.calls.length, 0);
    scenarios++;
    h = harness(
      source,
      () => {
        throw "cancel";
      },
      apiSource,
    );
    await assert.rejects(h.s[fn](...args), () => true);
    assert.equal(h.calls.length, 0);
    scenarios++;
    h = harness(
      source,
      (s) => {
        fn === "review" ? s.invalidateList() : s.invalidateDetail();
        return { value: "核对依据", action: "confirm" };
      },
      apiSource,
    );
    await h.s[fn](...args);
    assert.equal(h.calls.length, 0);
    scenarios++;
  }
  console.log(
    "Passed: " +
      scenarios +
      " scenarios passed using actual SFC handlers and API wrappers: trimmed text, blank, cancel, stale context.",
  );
  console.log(
    "Scope: isolated mocked requests only; no browser, tax filing, database or external communication.",
  );
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
