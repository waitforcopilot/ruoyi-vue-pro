// Run with Node.js 22 after installing the Vue frontend dependencies.
const fs = require("node:fs");
const vm = require("node:vm");
const assert = require("node:assert/strict");
const root = require("node:path").resolve(__dirname, "../..");
const ui = root + "/yudao-ui/yudao-ui-admin-vue3";
const ts = require(ui + "/node_modules/typescript");
const componentPath = ui + "/src/views/hrm/payroll/batch/Bonuses.vue";
const apiPath = ui + "/src/api/hrm/payroll/bonus.ts";
function harness(source, behavior) {
  const calls = [];
  const request = { post: async (req) => calls.push(req), get: async () => [] };
  const apiContext = { exports: {}, require: () => request };
  const compile = (code) =>
    ts.transpileModule(code, {
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        target: ts.ScriptTarget.ES2022,
      },
    }).outputText;
  vm.runInNewContext(compile(fs.readFileSync(apiPath, "utf8")), apiContext);
  const props = { batchId: 6, status: 0 };
  const unmountCallbacks = [];
  const watchers = [];
  const context = {
    exports: {},
    require: () => apiContext.exports,
    watchers,
    unmountCallbacks,
    defineProps: () => props,
    ref: (value) => ({ value }),
    useMessage: () => ({
      prompt: async () => behavior(context.exports.state),
      success: () => {},
    }),
    watch: (_, callback) => watchers.push(callback),
    onBeforeUnmount: (callback) => unmountCallbacks.push(callback),
  };
  const script = source.match(
    /<script setup lang="ts">([\s\S]*?)<\/script>/,
  )[1];
  const expose = `
    exports.state = {
      review,
      changeBatch: async id => { props.batchId = id; for (const cb of watchers) await cb() },
      unmount: () => unmountCallbacks.forEach(cb => cb())
    }`;
  vm.runInNewContext(compile(script + expose), context);
  return { s: context.exports.state, calls };
}
async function verify(source) {
  let count = 0;
  for (const approved of [true, false]) {
    let h = harness(source, () => ({
      value: "  独立核对奖金依据  ",
      action: "confirm",
    }));
    await h.s.review(9, approved);
    assert.equal(h.calls.length, 1);
    assert.equal(h.calls[0].url, "/hrm/payroll/bonus/review");
    assert.equal(h.calls[0].params.id, 9);
    assert.equal(h.calls[0].params.approved, approved);
    assert.equal(h.calls[0].params.reason, "独立核对奖金依据");
    count++;
    h = harness(source, () => ({ value: " \n\t ", action: "confirm" }));
    await h.s.review(9, approved);
    assert.equal(h.calls.length, 0);
    count++;
    h = harness(source, () => {
      throw "cancel";
    });
    await assert.rejects(h.s.review(9, approved), () => true);
    assert.equal(h.calls.length, 0);
    count++;
    h = harness(source, async (s) => {
      await s.changeBatch(7);
      return { value: "依据", action: "confirm" };
    });
    await h.s.review(9, approved);
    assert.equal(h.calls.length, 0);
    count++;
    h = harness(source, async (s) => {
      await s.changeBatch(7);
      await s.changeBatch(6);
      return { value: "依据", action: "confirm" };
    });
    await h.s.review(9, approved);
    assert.equal(h.calls.length, 0);
    count++;
    h = harness(source, (s) => {
      s.unmount();
      return { value: "依据", action: "confirm" };
    });
    await h.s.review(9, approved);
    assert.equal(h.calls.length, 0);
    count++;
  }
  return count;
}
(async () => {
  const source = fs.readFileSync(componentPath, "utf8");
  const count = await verify(source);
  console.log(
    `Passed: ${count} bonus approval/rejection scenarios: trimmed text, blank, cancel, batch switch, away-and-back, unmount.`,
  );
  console.log(
    "Scope: actual SFC handlers and API wrapper with mocked requests; no browser or business API calls.",
  );
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
