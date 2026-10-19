#!/usr/bin/env node
// Independent fixed amounts for the fictional S4 review adapter, never a payroll acceptance.
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict')
const root = path.resolve(__dirname, '../..')
const ts = require(path.join(root, 'yudao-ui/yudao-ui-admin-vue3/node_modules/typescript'))
const base = path.join(root, 'yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll')
function load(file, imports) { const m={exports:{}};new Function('require','module','exports',ts.transpileModule(fs.readFileSync(file,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{if(!(name in imports))throw Error('Unexpected import '+name);return imports[name]},m,m.exports);return m.exports }
const directory=load(path.join(base,'requirements.ts'),{})
const adapter=load(path.join(base,'review/demo.ts'),{'../requirements':directory})
const expected={hourly:62.5,overtime:750,gross:11750,personal:1400,cumulativeTaxable:27750,cumulativeTax:832.5,tax:130.5,net:10219.5,employer:2100,cost:13850}
assert.deepEqual(adapter.calculateDemo(),expected)
assert.equal(adapter.demo.attendedHours+adapter.demo.paidLeaveHours,160)
assert.equal(9695+4155,expected.cost)
assert.equal(expected.gross-expected.personal-expected.tax,expected.net)
assert.equal(Object.keys(adapter.reviewViews).length,10)
assert.equal(adapter.reviewOrder.length,10)
const topics=Object.values(adapter.reviewTopics).flat();assert.equal(topics.length,46);assert.equal(new Set(topics.map(x=>x.code)).size,46)
assert.equal(adapter.demoTrend.at(-1).gross,expected.gross)
assert.equal(adapter.demoCostStructure.reduce((n,x)=>n+x.amount,0),expected.cost)
for(const code of adapter.reviewOrder){assert(adapter.reviewViews[code].rows.length>0);assert(adapter.reviewViews[code].notes.length>0);assert(directory.sourcesForModules([code]).length>0)}
const text=fs.readFileSync(path.join(base,'review/demo.ts'),'utf8');assert(!text.includes('request.post'));assert(!text.includes('from \'@/api'))
const result={fictionalOnly:true,employee:'TEST-EMP-001',expected,tenViews:10,allViewsHaveFieldsRowsAndCandidateSources:true,productionWriterImports:false}
if(process.argv[2])fs.writeFileSync(process.argv[2],JSON.stringify(result,null,2)+'\n')
console.log('PASS: independent fictional amounts, cross-view consistency, ten views and pure adapter imports')
