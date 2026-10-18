// Run with Node.js 22 after installing the Vue frontend dependencies.
const fs = require("node:fs");
const vm = require("node:vm");
const assert = require("node:assert/strict");
const root = require("node:path").resolve(__dirname, "../..");
const ui = root + "/yudao-ui/yudao-ui-admin-vue3";
const ts = require(ui + "/node_modules/typescript");
const vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_COST_SOURCE || ui + "/src/views/hrm/payroll/cost/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
const flush = () => new Promise((resolve) => setImmediate(resolve));
function deferred() {
  let resolve, reject;
  const promise = new Promise((yes, no) => {
    resolve = yes;
    reject = no;
  });
  return { promise, resolve, reject };
}
function harness() {
  const gets = [],
    posts = [],
    downloads = [],
    files = [],
    mounts = [],
    unmounts = [],
    prompts = [];
  const pending = (queue, args) => {
    const d = deferred();
    queue.push({ ...d, args });
    return d.promise;
  };
  const request = {
    get: (args) => pending(gets, args),
    post: (args) => pending(posts, args),
    download: (args) => pending(downloads, args),
  };
  const stops = [];
  const context = {
    exports: {},
    ref: vue.ref,
    reactive: vue.reactive,
    computed: vue.computed,
    watch: (...args) => stops.push(vue.watch(...args)),
    onMounted: (fn) => mounts.push(fn),
    onBeforeUnmount: (fn) => unmounts.push(fn),
    useMessage: () => ({
      prompt: () => pending(prompts, {}),
      warning: () => {},
    }),
    crypto: { randomUUID: () => "test-only" },
    require: (name) =>
      name.includes("config/axios")
        ? request
        : name.includes("utils/download")
          ? { excel: (...args) => files.push(args) }
          : {},
  };
  const expose =
    "\nexports.s={entityId,batchId,options,rules,reports,rows,events,proof,selectedReport,historyVisible,loading,saving,changeEntity,loadRules,loadReports,viewReport,history,build,review,exportReport,saveRule,draft,ruleVisible};";
  vm.runInNewContext(
    ts.transpileModule(body + expose, {
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        target: ts.ScriptTarget.ES2022,
      },
    }).outputText,
    context,
  );
  const s = context.exports.s;
  return {
    s,
    gets,
    posts,
    downloads,
    files,
    prompts,
    mounts,
    unmount: () => {
      unmounts.forEach((fn) => fn());
      stops.forEach((stop) => stop());
    },
  };
}
const reportA = { id: 10, version: 1 },
  reportB = { id: 20, version: 2 };
async function run() {
  let count = 0;
  for (const awayAndBack of [false, true]) {
    const h = harness(),
      { s } = h;
    s.entityId.value = 4;
    s.batchId.value = 6;
    const p = s.viewReport(reportA);
    s.entityId.value = 2;
    s.batchId.value = undefined;
    if (awayAndBack) {
      s.entityId.value = 4;
      s.batchId.value = 6;
    }
    h.gets[0].resolve([{ total: 6780 }]);
    await p;
    assert.equal(s.selectedReport.value, undefined);
    assert.equal(s.rows.value.length, 0);
    h.unmount();
    count++;
  }
  {
    const h = harness(),
      { s } = h;
    s.entityId.value = 4;
    s.batchId.value = 6;
    const first = s.viewReport(reportA),
      second = s.viewReport(reportB);
    h.gets[1].resolve([{ total: 20 }]);
    await second;
    h.gets[0].resolve([{ total: 10 }]);
    await first;
    assert.equal(s.selectedReport.value.id, 20);
    assert.equal(s.rows.value[0].total, 20);
    h.unmount();
    count++;
  }
  for (const kind of ["loadReports", "loadRules"]) {
    const h = harness(),
      { s } = h;
    s.entityId.value = 4;
    s.batchId.value = 6;
    const first = s[kind]();
    s.entityId.value = 2;
    s.batchId.value = 3;
    const second = s[kind]();
    h.gets[0].resolve([{ id: 10 }]);
    await first;
    assert.equal(s.loading.value, true);
    h.gets[1].resolve([{ id: 20 }]);
    await second;
    assert.equal(
      (kind === "loadReports" ? s.reports : s.rules).value[0].id,
      20,
    );
    assert.equal(s.loading.value, false);
    h.unmount();
    count++;
  }
  {
    const h = harness(),
      { s } = h;
    s.entityId.value = 4;
    const first = s.changeEntity();
    s.entityId.value = 2;
    const second = s.changeEntity();
    h.gets[1].resolve({
      entities: [],
      employees: [{ id: 2 }],
      departments: [],
      batches: [],
    });
    await flush();
    h.gets[0].resolve({
      entities: [],
      employees: [{ id: 8 }],
      departments: [],
      batches: [{ id: 6 }],
    });
    await first;
    h.gets[2].resolve([]);
    await second;
    assert.equal(s.options.value.employees[0].id, 2);
    assert.equal(s.options.value.batches.length, 0);
    h.unmount();
    count++;
  }
  {
    const h = harness(),
      { s } = h;
    const initial = h.mounts[0]();
    s.entityId.value = 4;
    const later = s.changeEntity();
    h.gets[1].resolve({
      entities: [],
      employees: [{ id: 8 }],
      departments: [],
      batches: [],
    });
    await flush();
    h.gets[0].resolve({
      entities: [],
      employees: [],
      departments: [],
      batches: [{ id: 99 }],
    });
    await initial;
    h.gets[2].resolve([]);
    await later;
    assert.equal(s.options.value.employees[0].id, 8);
    h.unmount();
    count++;
  }
  {
    const h = harness(),
      { s } = h;
    s.entityId.value = 4;
    s.batchId.value = 6;
    s.reports.value = [reportA];
    s.rows.value = [{ total: 6780 }];
    s.selectedReport.value = reportA;
    s.batchId.value = undefined;
    assert.equal(s.reports.value.length, 0);
    assert.equal(s.rows.value.length, 0);
    assert.equal(s.selectedReport.value, undefined);
    h.unmount();
    count++;
  }
  {
    const h = harness(),
      { s } = h;
    s.entityId.value = 4;
    s.batchId.value = 6;
    const p = s.history("REPORT", reportA);
    s.entityId.value = 2;
    h.gets[0].resolve([{ id: 1 }]);
    await p;
    assert.equal(s.historyVisible.value, false);
    assert.equal(s.events.value.length, 0);
    // A history opened while another is visible must still load normally.
    const q = s.history("RULE", reportB);
    h.gets[1].resolve([{ id: 2 }]);
    await q;
    const r = s.history("RULE", reportA);
    h.gets[2].resolve([{ id: 3 }]);
    await r;
    assert.equal(s.historyVisible.value, true);
    assert.equal(s.events.value[0].id, 3);
    h.unmount();
    count++;
  }
  for (const fn of ["build", "review"]) {
    const h = harness(),
      { s } = h;
    s.entityId.value = 4;
    s.batchId.value = 6;
    const p = fn === "build" ? s.build() : s.review("report", 10, true);
    h.prompts[0].resolve({ value: "  current reason  ", action: "confirm" });
    await flush();
    assert.equal(h.posts.length, 1);
    assert.equal(h.posts[0].args.params.reason, "current reason");
    assert.equal(
      h.posts[0].args.params[fn === "build" ? "batchId" : "id"],
      fn === "build" ? 6 : 10,
    );
    h.posts[0].resolve(true);
    await flush();
    h.gets[0].resolve([reportA]);
    await p;
    assert.equal(s.reports.value[0].id, reportA.id);
    h.unmount();
    count++;
  }
  {
    const h = harness(),
      { s } = h;
    s.entityId.value = 4;
    s.batchId.value = 6;
    s.draft.employeeId = 8;
    s.ruleVisible.value = true;
    const p = s.saveRule();
    assert.equal(h.posts[0].args.data.entityId, 4);
    s.entityId.value = 2;
    h.posts[0].resolve(true);
    await p;
    assert.equal(h.gets.length, 0);
    assert.equal(s.ruleVisible.value, false);
    h.unmount();
    count++;
  }
  for (const fn of ["build", "review"]) {
    const h = harness(),
      { s } = h;
    s.entityId.value = 4;
    s.batchId.value = 6;
    const p = fn === "build" ? s.build() : s.review("report", 10, true);
    s.entityId.value = 2;
    h.prompts[0].resolve({ value: "reason", action: "confirm" });
    await p;
    assert.equal(h.posts.length, 0);
    h.unmount();
    count++;
  }
  {
    const h = harness(),
      { s } = h;
    s.entityId.value = 4;
    s.batchId.value = 6;
    const p = s.exportReport(10);
    s.entityId.value = 2;
    h.downloads[0].resolve("blob");
    await p;
    assert.equal(h.files.length, 0);
    const q = s.exportReport(20);
    h.downloads[1].resolve("current-blob");
    await q;
    assert.equal(h.files[0][0], "current-blob");
    h.unmount();
    count++;
  }
  {
    const h = harness(),
      { s } = h;
    s.entityId.value = 4;
    s.batchId.value = 6;
    const p = s.viewReport(reportA);
    h.unmount();
    h.gets[0].resolve([{ total: 6780 }]);
    await p;
    assert.equal(s.selectedReport.value, undefined);
    count++;
  }
  console.log(
    `Passed: ${count} cost context scenarios using actual Vue refs/watchers and SFC handlers with delayed mock requests.`,
  );
  console.log(
    "No browser or business API calls; browser before/after evidence is recorded separately.",
  );
}
run().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
