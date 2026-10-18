// Run with Node.js 22 after installing the Vue frontend dependencies. No HTTP or database access.
const fs = require("node:fs");
const vm = require("node:vm");
const assert = require("node:assert/strict");
const path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript");
const vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_FINANCIAL_SOURCE ||
    ui + "/src/views/hrm/payroll/analysis/index.vue",
  "utf8",
);
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
const flush = () => new Promise((resolve) => setImmediate(resolve));
function harness() {
  const gets = [],
    uploads = [],
    downloads = [],
    files = [],
    messages = [],
    unmounts = [];
  function pending(queue, args) {
    let resolve, reject;
    const promise = new Promise((yes, no) => {
      resolve = yes;
      reject = no;
    });
    queue.push({ args, resolve, reject });
    return promise;
  }
  const request = {
    get: (args) => pending(gets, args),
    upload: (args) => pending(uploads, args),
    download: (args) => pending(downloads, args),
  };
  const context = {
    exports: {},
    ref: vue.ref,
    reactive: vue.reactive,
    computed: vue.computed,
    onMounted: () => {},
    onBeforeUnmount: (fn) => unmounts.push(fn),
    useMessage: () => ({
      warning: () => {},
      success: (text) => messages.push(text),
    }),
    crypto: { randomUUID: () => "test-key" },
    FormData,
    require: (name) =>
      name.includes("config/axios")
        ? request
        : name.includes("utils/download")
          ? { excel: (...args) => files.push(args) }
          : name === "dayjs"
            ? require(ui + "/node_modules/dayjs")
            : {},
  };
  const expose =
    "\nexports.s={entityId,period,file,requestKey,previewRows,saving,loading,options,analysis,plans,history,tab,changeScope,chooseFile,removeFile,preview,importFile,exportErrors,load};";
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
  s.entityId.value = 4;
  s.period.value = "2026-10";
  const choose = (name) => s.chooseFile({ raw: new File(["synthetic"], name) });
  const settleGets = () =>
    gets.forEach((g) =>
      g.resolve(
        g.args.url.endsWith("/options")
          ? { entities: [], departments: [] }
          : [],
      ),
    );
  return {
    s,
    gets,
    uploads,
    downloads,
    files,
    messages,
    unmounts,
    choose,
    settleGets,
  };
}
(async () => {
  let count = 0;
  for (const action of [
    "entity",
    "period",
    "roundtrip",
    "remove",
    "replace",
    "unmount",
  ]) {
    const h = harness();
    h.choose("old.xlsx");
    const task = h.s.preview();
    let scopeTask;
    if (action === "entity" || action === "roundtrip") {
      h.s.entityId.value = 2;
      scopeTask = h.s.changeScope();
    }
    if (action === "roundtrip") {
      h.s.entityId.value = 4;
      h.s.changeScope();
    }
    if (action === "period") {
      h.s.period.value = "2026-11";
      scopeTask = h.s.changeScope();
    }
    if (action === "remove") h.s.removeFile();
    if (action === "replace") h.choose("new.xlsx");
    if (action === "unmount") h.unmounts.forEach((fn) => fn());
    h.uploads[0].resolve({
      data: [{ dimension: "ENTITY", dimensionKey: "ALL" }],
    });
    await task;
    assert.equal(
      h.s.previewRows.value.length,
      0,
      action + ": stale preview must not appear",
    );
    if (action !== "unmount")
      assert.equal(
        h.s.saving.value,
        false,
        action + ": old operation must not retain busy state",
      );
    h.settleGets();
    await flush();
    h.settleGets();
    await flush();
    count++;
  }
  {
    const h = harness();
    h.choose("normal.xlsx");
    const task = h.s.preview();
    h.uploads[0].resolve({ data: [{ dimensionKey: "ALL" }] });
    await task;
    assert.equal(h.s.previewRows.value[0].dimensionKey, "ALL");
    assert.equal(h.s.saving.value, false);
    count++;
  }
  {
    const h = harness();
    h.choose("same.xlsx");
    const first = h.s.preview();
    const second = h.s.preview();
    h.uploads[0].resolve({ data: [{ dimensionKey: "OLD" }] });
    await first;
    assert.equal(h.s.previewRows.value.length, 0);
    assert.equal(h.s.saving.value, true);
    h.uploads[1].resolve({ data: [{ dimensionKey: "NEW" }] });
    await second;
    assert.equal(h.s.previewRows.value[0].dimensionKey, "NEW");
    count++;
  }
  {
    const h = harness();
    h.choose("old.xlsx");
    h.s.previewRows.value = [{ dimensionKey: "ALL" }];
    const task = h.s.importFile();
    h.s.entityId.value = 2;
    h.s.changeScope();
    h.choose("new.xlsx");
    h.uploads[0].resolve({ data: [123] });
    await task;
    assert.equal(h.messages.length, 0);
    assert.equal(h.s.file.value.name, "new.xlsx");
    assert.equal(h.s.tab.value, "analysis");
    h.settleGets();
    await flush();
    h.settleGets();
    count++;
  }
  {
    const h = harness();
    h.choose("old.xlsx");
    const task = h.s.exportErrors();
    h.s.removeFile();
    h.downloads[0].resolve(new Blob(["old"]));
    await task;
    assert.equal(h.files.length, 0);
    count++;
  }
  {
    const h = harness();
    const first = h.s.changeScope();
    h.s.entityId.value = 2;
    const second = h.s.changeScope();
    h.gets[1].resolve({ entities: [], departments: [{ name: "NEW" }] });
    await flush();
    h.gets[0].resolve({ entities: [], departments: [{ name: "OLD" }] });
    await first;
    assert.equal(h.s.options.value.departments[0].name, "NEW");
    h.settleGets();
    await second;
    count++;
  }
  {
    const h = harness();
    const first = h.s.load();
    const second = h.s.load();
    h.gets
      .slice(3)
      .forEach((g, i) => g.resolve(i === 0 ? { marker: "NEW" } : []));
    await second;
    h.gets
      .slice(0, 3)
      .forEach((g, i) => g.resolve(i === 0 ? { marker: "OLD" } : []));
    await first;
    assert.equal(h.s.analysis.value.marker, "NEW");
    count++;
  }
  {
    const h = harness();
    h.choose("normal.xlsx");
    h.s.previewRows.value = [{ dimensionKey: "ALL" }];
    const task = h.s.importFile();
    h.uploads[0].resolve({ data: [123] });
    await flush();
    h.settleGets();
    await task;
    assert.equal(h.messages.length, 1);
    assert.equal(h.s.file.value, undefined);
    assert.equal(h.s.previewRows.value.length, 0);
    assert.equal(h.s.saving.value, false);
    assert.equal(h.s.tab.value, "plans");
    count++;
  }
  {
    const h = harness();
    h.choose("normal.xlsx");
    const task = h.s.preview();
    h.uploads[0].reject(new Error("synthetic failure"));
    await assert.rejects(task, /synthetic failure/);
    assert.equal(h.s.saving.value, false);
    count++;
  }
  {
    const h = harness();
    await h.s.importFile();
    assert.equal(h.uploads.length, 0);
    h.choose("invalid.xlsx");
    h.s.previewRows.value = [{ error: "invalid" }];
    await h.s.importFile();
    assert.equal(h.uploads.length, 0);
    count++;
  }
  console.log(`PASS ${count} financial context regression scenarios`);
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
