// Node.js 22; actual SFC and Vue refs, mocked requests; no HTTP or database writes.
const fs = require("node:fs"),
  vm = require("node:vm"),
  assert = require("node:assert/strict"),
  path = require("node:path");
const ui = path.resolve(__dirname, "../../yudao-ui/yudao-ui-admin-vue3");
const ts = require(ui + "/node_modules/typescript"),
  vue = require(ui + "/node_modules/vue");
const source = fs.readFileSync(
  process.env.HRM_ISSUE_FORM_SOURCE ||
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
    useMessage: () => ({ success: (m) => messages.push(m) }),
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
        "\nexports.s={entityId,batchId,employeeId,draft,dialog,saving,people,openAssign,openCreate,save,changeEntity,loadBatch,loadSignals};",
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
  for (const action of ["openAssign", "openCreate"])
    for (const mode of [
      "entity",
      "batch",
      "employee",
      "roundtrip",
      "unmount",
    ]) {
      const h = harness();
      const task = h.s[action](row(1));
      if (mode === "entity") {
        h.s.entityId.value = 2;
        await h.s.changeEntity();
      }
      if (mode === "batch") {
        h.s.batchId.value = 6;
        await h.s.loadBatch();
      }
      if (mode === "employee") {
        h.s.employeeId.value = 9;
        await h.s.loadSignals();
      }
      if (mode === "roundtrip") {
        h.s.entityId.value = 2;
        await h.s.changeEntity();
        h.s.entityId.value = 4;
        await h.s.changeEntity();
        h.s.batchId.value = 7;
      }
      if (mode === "unmount") h.unmounts.forEach((f) => f());
      h.options[0].resolve(people);
      await task;
      assert.equal(
        h.s.dialog.value,
        false,
        action + "/" + mode + ": stale form must not open",
      );
      assert.equal(h.s.people.owners.length, 0);
      count++;
    }
  {
    const h = harness();
    const a = h.s.openAssign(row(1)),
      b = h.s.openAssign(row(2));
    h.options[1].resolve(people);
    await b;
    h.options[0].resolve({ owners: [{ id: 999 }], reviewers: [] });
    await a;
    assert.equal(h.s.draft.id, 2);
    assert.equal(h.s.people.owners[0].id, 254);
    count++;
  }
  for (const action of ["openAssign", "openCreate"]) {
    const h = harness();
    const task = h.s[action](row(1));
    h.options[0].resolve(people);
    await task;
    h.s.draft.reason = "TEST";
    const saving = h.s.save();
    assert.ok(
      h.posts[0].args.url.endsWith(
        action === "openAssign" ? "/reassign" : "/create",
      ),
    );
    assert.equal(
      action === "openAssign"
        ? h.posts[0].args.data.id
        : h.posts[0].args.data.batchId,
      action === "openAssign" ? 1 : 7,
    );
    h.posts[0].resolve(1);
    await saving;
    assert.equal(h.s.dialog.value, false);
    assert.equal(h.s.saving.value, false);
    assert.equal(h.messages.length, 1);
    count++;
  }
  {
    const h = harness();
    await open(h);
    const oldSave = h.s.save();
    h.s.entityId.value = 2;
    await h.s.changeEntity();
    h.s.batchId.value = 7;
    await open(h, 2);
    const newSave = h.s.save();
    h.posts[0].resolve(1);
    await oldSave;
    assert.equal(h.s.dialog.value, true);
    assert.equal(h.s.draft.id, 2);
    assert.equal(h.s.saving.value, true);
    assert.equal(h.messages.length, 0);
    h.posts[1].resolve(1);
    await newSave;
    count++;
  }
  {
    const h = harness();
    await open(h);
    const a = h.s.save(),
      b = h.s.save();
    assert.equal(h.posts.length, 1);
    h.posts[0].resolve(1);
    await Promise.all([a, b]);
    count++;
  }
  {
    const h = harness();
    await h.s.openAssign({ ...row(1), batchId: 6 });
    assert.equal(h.options.length, 0);
    count++;
  }
  {
    const h = harness();
    h.s.entityId.value = undefined;
    await h.s.openAssign(row(1));
    await h.s.save();
    assert.equal(h.options.length, 0);
    assert.equal(h.posts.length, 0);
    count++;
  }
  {
    const h = harness();
    await open(h);
    const task = h.s.save();
    h.posts[0].reject(new Error("synthetic error"));
    await assert.rejects(task, /synthetic error/);
    assert.equal(h.s.saving.value, false);
    assert.equal(h.s.dialog.value, true);
    count++;
  }
  {
    const h = harness();
    await open(h);
    h.s.dialog.value = false;
    await h.s.save();
    assert.equal(h.posts.length, 0);
    count++;
  }
  {
    const h = harness();
    const selected = row(1);
    const task = h.s.openAssign(selected);
    selected.id = 99;
    h.options[0].resolve(people);
    await task;
    assert.equal(h.s.draft.id, 1);
    count++;
  }
  console.log(`PASS ${count} input issue form scenarios`);
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
