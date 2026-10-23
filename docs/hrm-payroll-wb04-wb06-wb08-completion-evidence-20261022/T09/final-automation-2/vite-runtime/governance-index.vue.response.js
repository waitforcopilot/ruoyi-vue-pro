import { createHotContext as __vite__createHotContext } from "/@vite/client";import.meta.hot = __vite__createHotContext("/src/views/hrm/payroll/governance/index.vue");/* unplugin-vue-components disabled */import __unplugin_components_15 from "/src/components/ContentWrap/src/ContentWrap.vue";
import { ElDialog as __unplugin_components_14 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_dialog_style_css.js?v=d4fc3075";
import { ElCheckboxGroup as __unplugin_components_13 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_checkbox-group_style_css.js?v=d4fc3075";
import { ElCheckbox as __unplugin_components_12 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_checkbox_style_css.js?v=d4fc3075";
import { ElInputNumber as __unplugin_components_11 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_input-number_style_css.js?v=d4fc3075";
import { ElTabs as __unplugin_components_10 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_tabs_style_css.js?v=d4fc3075";
import { ElTabPane as __unplugin_components_9 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_tab-pane_style_css.js?v=d4fc3075";
import { ElForm as __unplugin_components_8 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_form_style_css.js?v=d4fc3075";
import { ElFormItem as __unplugin_components_7 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_form-item_style_css.js?v=d4fc3075";
import { ElInput as __unplugin_components_6 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_input_style_css.js?v=d4fc3075";
import { ElTable as __unplugin_components_5 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_table_style_css.js?v=d4fc3075";
import { ElTableColumn as __unplugin_components_4 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_table-column_style_css.js?v=d4fc3075";
import { ElButton as __unplugin_components_3 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_button_style_css.js?v=d4fc3075";
import { ElSelect as __unplugin_components_2 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_select_style_css.js?v=d4fc3075";
import { ElOption as __unplugin_components_1 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_option_style_css.js?v=d4fc3075";
import { ElAlert as __unplugin_components_0 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_alert_style_css.js?v=d4fc3075";
import { defineComponent as _defineComponent } from "/node_modules/.vite/deps/vue.js?v=d4fc3075";
import * as api from "/src/api/hrm/payroll/governance.ts?t=1792713748290";
import { listRequirementProjects, requirementCapabilities } from "/src/api/hrm/payroll/requirement.ts?t=1792713748290";
import download from "/src/utils/download.ts";
import { checkPermi } from "/src/utils/permission.ts?t=1792713748290";

import { useRouter, useRoute } from "/node_modules/.vite/deps/vue-router.js?v=d4fc3075";
import { computed, ref, watch, onBeforeUnmount } from "/node_modules/.vite/deps/vue.js?v=d4fc3075";
const _sfc_main = /*@__PURE__*/ _defineComponent({
	__name: "index",
	setup(__props, { expose: __expose }) {
		__expose();
		const canPlatform = (action) => checkPermi(["hrm:payroll:requirement:" + action]);
		const router = useRouter(), route = useRoute();
		const projectId = computed(() => route.query.projectId == null ? undefined : Number(route.query.projectId));
		const projects = ref([]), anchors = ref([]), roots = ref([]), decisions = ref([]), releases = ref([]);
		const caps = ref({}), gate = ref(), frozen = ref(), jobs = ref([]);
		const project = computed(() => projects.value.find((p) => p.id === projectId.value));
		const loading = ref(false), busy = ref(false), error = ref(""), success = ref(""), publishReason = ref(""), requestKey = ref(crypto.randomUUID()), exportIds = ref(""), lastPublish = ref();
		const rootOpen = ref(false), decisionOpen = ref(false), actionOpen = ref(false), recordOpen = ref(false), recordText = ref(""), selectedSheet = ref("评审版本说明");
		const rootForm = ref({}), decisionForm = ref({}), actionForm = ref({ reason: "" });
		const questions = Array.from({ length: 10 }, (_, n) => `Q-${String(n + 1).padStart(2, "0")}`), otherQuestions = [
			"Q-03",
			"Q-06",
			"Q-08",
			"Q-09"
		];
		const actionLabels = {
			READ: "读取项目",
			META: "修改项目名称",
			EDIT: "编辑需求",
			ASSIGN: "分派评审",
			CONFIRM: "本人确认需求",
			EXPORT: "导出",
			MEMBER: "管理成员",
			SCOPE: "修订项目范围",
			SCOPE_DECIDE: "需求范围取舍",
			SOURCE_EDIT: "维护来源",
			SOURCE_VERIFY: "核验来源质量",
			ISSUE_MANAGE: "管理问题",
			ISSUE_HANDLE: "处理问题",
			ISSUE_REVIEW: "独立复核问题",
			DECISION_READ: "读取决议正文",
			GRANT_READ: "读取授权依据",
			PERSON_READ: "读取关联人员显示名",
			DECISION_EDIT: "起草决议",
			DECISION_APPROVE: "独立批准决议",
			BASELINE_PUBLISH: "发布基线",
			PROJECT_CREATE: "创建项目"
		};
		const rootFields = [
			{
				key: "scopeJson",
				label: "授权范围正文",
				long: true
			},
			{
				key: "validFrom",
				label: "授权开始日期"
			},
			{
				key: "validTo",
				label: "授权结束日期"
			},
			{
				key: "materialVersion",
				label: "受控材料版本"
			},
			{
				key: "body",
				label: "受控签署材料正文",
				long: true
			},
			{
				key: "reason",
				label: "登记依据",
				long: true
			}
		];
		const decisionFields = [
			{
				key: "clauses",
				label: "覆盖条款",
				long: true
			},
			{
				key: "clauseVersion",
				label: "条款引用版本"
			},
			{
				key: "body",
				label: "决议受控正文",
				long: true
			},
			{
				key: "scopeJson",
				label: "决议适用范围正文",
				long: true
			},
			{
				key: "validFrom",
				label: "决议开始日期"
			},
			{
				key: "validTo",
				label: "决议结束日期"
			},
			{
				key: "approvalConditions",
				label: "批准条件",
				long: true
			},
			{
				key: "reason",
				label: "起草或修订依据",
				long: true
			}
		];
		const blockLabels = {
			PROJECT_ARCHIVED: "项目已归档",
			FULL_CLOSURE_READ_REQUIRED: "缺少完整快照闭包读权",
			BASELINE_PUBLISH_REQUIRED: "缺少独立发布权限",
			CURRENT_CONTROLLED_PUBLISH_AUTHORITY_REQUIRED: "缺少当前有效的受控发布授权",
			CURRENT_DECISION_COVERAGE_REQUIRED: "当前范围的强制决议覆盖不完整或冲突",
			UNRESOLVED_QUESTION_CLASSIFICATION: "其他未决问题尚未明确分类",
			UNRESOLVED_BASELINE_QUESTION: "仍有基线阻断问题",
			EMPTY_INCLUDED_SET: "本期纳入集合为空",
			SCOPE_MISMATCH: "需求范围与当前项目不匹配",
			CURRENT_CONFIRMATION_REQUIRED: "需要当前有效的本人确认",
			CONFIRMATION_GOVERNANCE_FACT_REQUIRED: "旧确认缺少S5批准及授权事实，需重新确认",
			SPECIFICATION_NOT_VERIFIED: "来源或映射规格尚未核验",
			EXCLUSION_BASIS_INCOMPLETE: "排除理由、责任人或重新评估条件不齐备",
			DUPLICATE_TARGET_INVALID: "重复移除的保留项无效",
			BASELINE_ISSUES_OPEN: "存在相关未关闭基线阻断问题"
		};
		let ticket = 0;
		async function load() {
			success.value = "";
			const own = ++ticket, id = projectId.value;
			loading.value = true;
			error.value = "";
			roots.value = [];
			decisions.value = [];
			releases.value = [];
			caps.value = {};
			gate.value = undefined;
			frozen.value = undefined;
			try {
				const [ps, as] = await Promise.all([listRequirementProjects(), api.get("/anchors")]);
				if (own !== ticket) return;
				projects.value = ps;
				anchors.value = as;
				if (id == null) return;
				const loadedRoots = await api.get("/roots", { projectId: id });
				if (own !== ticket) return;
				roots.value = loadedRoots;
				if (id) {
					const c = await requirementCapabilities(id);
					const [ds, rs] = await Promise.all([c.DECISION_READ ? api.get("/decisions", { projectId: id }) : [], api.get("/releases", { projectId: id })]);
					if (own !== ticket) return;
					caps.value = c;
					decisions.value = ds;
					releases.value = rs;
				}
			} catch {
				if (own === ticket) error.value = "真实决议或发布数据未加载：请核对当前权限或重试，未返回内容不推定为空或通过。";
			} finally {
				if (own === ticket) loading.value = false;
			}
		}
		function switchProject(id) {
			lastPublish.value = undefined;
			jobs.value = [];
			success.value = "";
			rootOpen.value = decisionOpen.value = actionOpen.value = false;
			router.replace({
				path: route.path,
				query: { projectId: id }
			});
		}
		function goReview() {
			router.push({
				path: "/hrm-payroll/review",
				query: {
					projectId: projectId.value,
					module: "COL"
				}
			});
		}
		function showRecord(row) {
			recordText.value = JSON.stringify(row, null, 2);
			recordOpen.value = true;
		}
		async function history(row) {
			success.value = "";
			showRecord(await api.get("/decision/history", {
				projectId: projectId.value,
				id: row.id
			}));
		}
		function openRoot() {
			success.value = "";
			rootForm.value = {
				anchorId: anchors.value[0]?.id,
				userId: undefined,
				actions: [],
				delegableActions: [],
				scopeJson: project.value?.scopeJson || "",
				validFrom: "",
				validTo: "",
				materialVersion: "",
				body: "",
				reason: ""
			};
			rootOpen.value = true;
		}
		function openDecision(row) {
			success.value = "";
			decisionForm.value = row ? {
				...JSON.parse(JSON.stringify(row.data)),
				previousId: row.id,
				reason: "",
				projectScopeVersion: project.value?.projectScopeVersion
			} : {
				projectScopeVersion: project.value?.projectScopeVersion,
				title: "",
				questions: [],
				clauses: "",
				clauseVersion: "",
				body: "",
				scopeJson: project.value?.scopeJson || "",
				validFrom: "",
				validTo: "",
				approverId: undefined,
				approvalConditions: "",
				semanticChange: false,
				otherQuestions: {},
				reason: ""
			};
			decisionOpen.value = true;
		}
		async function saveRoot() {
			success.value = "";
			busy.value = true;
			error.value = "";
			try {
				const id = await api.post("/root/register", {
					...rootForm.value,
					projectId: projectId.value
				});
				rootOpen.value = false;
				await load();
				success.value = `已登记待签署授权根${id}，尚未生效`;
			} catch {
				error.value = "登记未完成；当前输入已保留，请核对独立材料、人员、动作和范围。";
			} finally {
				busy.value = false;
			}
		}
		async function saveDecision() {
			success.value = "";
			busy.value = true;
			error.value = "";
			try {
				const id = await api.post("/decision/draft", {
					...decisionForm.value,
					projectId: projectId.value
				});
				decisionOpen.value = false;
				await load();
				success.value = `已保存决议草稿记录${id}，尚未批准`;
			} catch {
				error.value = "决议草稿未保存，输入已保留；请核对必填项、当前范围和独立授权。";
			} finally {
				busy.value = false;
			}
		}
		function openAction(kind, row, action) {
			success.value = "";
			actionForm.value = {
				kind,
				id: row.id,
				version: row.version,
				action,
				reason: ""
			};
			actionOpen.value = true;
		}
		async function saveAction() {
			success.value = "";
			busy.value = true;
			error.value = "";
			try {
				await api.post(`/${actionForm.value.kind}/action`, {
					...actionForm.value,
					projectId: projectId.value
				});
				actionOpen.value = false;
				await load();
				success.value = "本人动作已保存，生命周期按有效期和当前状态计算";
			} catch {
				error.value = "本人动作被拒绝或版本已变；依据输入已保留，不视为批准成功。";
			} finally {
				busy.value = false;
			}
		}
		async function precheck() {
			success.value = "";
			const own = ticket, target = projectId.value;
			busy.value = true;
			error.value = "";
			try {
				const result = await api.get("/precheck", { projectId: target });
				if (own === ticket && target === projectId.value) gate.value = result;
			} catch {
				gate.value = undefined;
				error.value = "发布预检查未完成；不能把未知结果视为通过。";
			} finally {
				busy.value = false;
			}
		}
		async function publish(draft) {
			lastPublish.value = {
				projectId: projectId.value,
				projectScopeVersion: gate.value.projectScopeVersion,
				requestKey: requestKey.value,
				dependencyToken: gate.value.dependencyToken,
				reason: publishReason.value,
				draft
			};
			await retryPublish();
		}
		async function retryPublish() {
			success.value = "";
			busy.value = true;
			error.value = "";
			try {
				const id = await api.post("/publish", lastPublish.value);
				await load();
				success.value = `固定快照ID：${id}。相同请求重试仍须当前资格有效。`;
			} catch {
				error.value = "发布或重试未完成，说明及请求号已保留；请重新检查当前授权、决议、范围和依赖版本。";
			} finally {
				busy.value = false;
			}
		}
		async function readRelease(releaseId) {
			success.value = "";
			const own = ticket, target = projectId.value;
			try {
				const result = await api.get("/release", {
					projectId: target,
					releaseId
				});
				if (own !== ticket || target !== projectId.value) return;
				frozen.value = result;
				selectedSheet.value = "评审版本说明";
			} catch {
				frozen.value = undefined;
				error.value = "当前权限不能读取该快照，未展示旧缓存内容。";
			}
		}
		async function createJob(releaseId) {
			success.value = "";
			const own = ticket, target = projectId.value;
			try {
				const ids = exportIds.value.trim() ? exportIds.value.split(",").map((x) => Number(x.trim())) : undefined;
				if (ids?.some((x) => !Number.isSafeInteger(x) || x <= 0)) throw new Error("invalid ids");
				const id = await api.post("/export/create", {
					projectId: target,
					releaseId,
					requirementIds: ids
				});
				if (own !== ticket || target !== projectId.value) return;
				const row = {
					id,
					projectId: target,
					releaseId,
					state: "QUEUED",
					label: "",
					error: ""
				};
				jobs.value.push(row);
				await refreshJob(row);
			} catch {
				error.value = "导出任务未创建，请核对筛选需求ID及当前导出权限。";
			}
		}
		async function refreshJob(row) {
			success.value = "";
			try {
				const result = await api.get("/export/status", {
					projectId: row.projectId,
					id: row.id
				});
				jobs.value = jobs.value.map((job) => job.id === row.id ? {
					...job,
					...result
				} : job);
			} catch {
				jobs.value = jobs.value.map((job) => job.id === row.id ? {
					...job,
					state: "UNKNOWN",
					error: "当前权限或版本已变化，无法交付旧任务。"
				} : job);
			}
		}
		async function claim(row) {
			success.value = "";
			try {
				const blob = await api.download({
					projectId: row.projectId,
					id: row.id
				});
				download.excel(blob, `薪酬评审-快照${row.releaseId}-任务${row.id}.xlsx`);
			} catch {
				frozen.value = undefined;
				gate.value = undefined;
				jobs.value = jobs.value.map((job) => job.id === row.id ? {
					...job,
					state: "DENIED",
					error: "领取被拒绝；旧任务或地址不能绕过当前权限。"
				} : job);
			}
		}
		watch(() => route.query.projectId, load, { immediate: true });
		onBeforeUnmount(() => {
			ticket++;
		});
		const __returned__ = {
			canPlatform,
			router,
			route,
			projectId,
			projects,
			anchors,
			roots,
			decisions,
			releases,
			caps,
			gate,
			frozen,
			jobs,
			project,
			loading,
			busy,
			error,
			success,
			publishReason,
			requestKey,
			exportIds,
			lastPublish,
			rootOpen,
			decisionOpen,
			actionOpen,
			recordOpen,
			recordText,
			selectedSheet,
			rootForm,
			decisionForm,
			actionForm,
			questions,
			otherQuestions,
			actionLabels,
			rootFields,
			decisionFields,
			blockLabels,
			get ticket() {
				return ticket;
			},
			set ticket(v) {
				ticket = v;
			},
			load,
			switchProject,
			goReview,
			showRecord,
			history,
			openRoot,
			openDecision,
			saveRoot,
			saveDecision,
			openAction,
			saveAction,
			precheck,
			publish,
			retryPublish,
			readRelease,
			createJob,
			refreshJob,
			claim
		};
		Object.defineProperty(__returned__, "__isScriptSetup", {
			enumerable: false,
			value: true
		});
		return __returned__;
	}
});
import { createElementVNode as _createElementVNode, resolveComponent as _resolveComponent, createVNode as _createVNode, openBlock as _openBlock, createBlock as _createBlock, createCommentVNode as _createCommentVNode, renderList as _renderList, Fragment as _Fragment, createElementBlock as _createElementBlock, withCtx as _withCtx, createTextVNode as _createTextVNode, toDisplayString as _toDisplayString } from "/node_modules/.vite/deps/vue.js?v=d4fc3075";
const _hoisted_1 = { class: "toolbar" };
const _hoisted_2 = { "aria-label": "独立业务授权根" };
const _hoisted_3 = { key: 1 };
const _hoisted_4 = {
	class: "wide-table",
	tabindex: "0",
	"aria-label": "授权根表，可横向滚动"
};
const _hoisted_5 = {
	key: 2,
	"aria-label": "业务决议版本"
};
const _hoisted_6 = { key: 1 };
const _hoisted_7 = {
	class: "wide-table",
	tabindex: "0",
	"aria-label": "决议表，可横向滚动"
};
const _hoisted_8 = {
	key: 3,
	"aria-label": "发布检查与固定快照"
};
const _hoisted_9 = { key: 0 };
const _hoisted_10 = { key: 1 };
const _hoisted_11 = { class: "toolbar" };
const _hoisted_12 = {
	class: "wide-table",
	tabindex: "0",
	"aria-label": "发布快照表，可横向滚动"
};
const _hoisted_13 = {
	key: 4,
	"aria-label": "冻结六表内容"
};
const _hoisted_14 = {
	class: "wide-table",
	tabindex: "0"
};
const _hoisted_15 = { class: "record" };
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
	const _component_el_alert = __unplugin_components_0;
	const _component_el_option = __unplugin_components_1;
	const _component_el_select = __unplugin_components_2;
	const _component_el_button = __unplugin_components_3;
	const _component_el_table_column = __unplugin_components_4;
	const _component_el_table = __unplugin_components_5;
	const _component_el_input = __unplugin_components_6;
	const _component_el_form_item = __unplugin_components_7;
	const _component_el_form = __unplugin_components_8;
	const _component_el_tab_pane = __unplugin_components_9;
	const _component_el_tabs = __unplugin_components_10;
	const _component_el_input_number = __unplugin_components_11;
	const _component_el_checkbox = __unplugin_components_12;
	const _component_el_checkbox_group = __unplugin_components_13;
	const _component_el_dialog = __unplugin_components_14;
	const _component_ContentWrap = __unplugin_components_15;
	return _openBlock(), _createBlock(_component_ContentWrap, { class: "governance-page" }, {
		default: _withCtx(() => [
			_cache[64] || (_cache[64] = _createElementVNode(
				"h1",
				null,
				"业务决议、发布与快照导出",
				-1
				/* CACHED */
			)),
			_createVNode(_component_el_alert, {
				title: "TEST材料及合成签署仅验证系统实现，不构成真实业务批准。正式授权根必须有实际有权业务人员提供的受控签署材料。",
				type: "warning",
				closable: false
			}),
			_createElementVNode("div", _hoisted_1, [
				_createVNode(_component_el_select, {
					"aria-label": "治理项目",
					"model-value": $setup.projectId,
					onChange: $setup.switchProject
				}, {
					default: _withCtx(() => [$setup.anchors.length ? (_openBlock(), _createBlock(_component_el_option, {
						key: 0,
						label: "租户项目创建授权",
						value: 0
					})) : _createCommentVNode("v-if", true), (_openBlock(true), _createElementBlock(
						_Fragment,
						null,
						_renderList($setup.projects, (p) => {
							return _openBlock(), _createBlock(_component_el_option, {
								key: p.id,
								label: `${p.id} · ${p.name}`,
								value: p.id
							}, null, 8, ["label", "value"]);
						}),
						128
						/* KEYED_FRAGMENT */
					))]),
					_: 1
				}, 8, ["model-value"]),
				_createVNode(_component_el_button, {
					loading: $setup.loading,
					onClick: $setup.load
				}, {
					default: _withCtx(() => [..._cache[25] || (_cache[25] = [_createTextVNode(
						"刷新决议与发布",
						-1
						/* CACHED */
					)])]),
					_: 1
				}, 8, ["loading"]),
				$setup.projectId ? (_openBlock(), _createBlock(_component_el_button, {
					key: 0,
					onClick: $setup.goReview
				}, {
					default: _withCtx(() => [..._cache[26] || (_cache[26] = [_createTextVNode(
						"返回十视图评审",
						-1
						/* CACHED */
					)])]),
					_: 1
				})) : _createCommentVNode("v-if", true),
				$setup.projectId ? (_openBlock(), _createBlock(_component_el_button, {
					key: 1,
					onClick: _cache[0] || (_cache[0] = ($event) => $setup.router.push({
						path: "/hrm-payroll/requirement",
						query: {
							projectId: $setup.projectId,
							module: "COL"
						}
					}))
				}, {
					default: _withCtx(() => [..._cache[27] || (_cache[27] = [_createTextVNode(
						"需求、成员及实际来源",
						-1
						/* CACHED */
					)])]),
					_: 1
				})) : _createCommentVNode("v-if", true)
			]),
			_createElementVNode(
				"p",
				null,
				_toDisplayString($setup.project?.name || ($setup.projectId === 0 ? "租户创建授权" : "选择有权项目")) + " · 范围版本 " + _toDisplayString($setup.project?.projectScopeVersion ?? "未定义"),
				1
				/* TEXT */
			),
			$setup.error ? (_openBlock(), _createBlock(_component_el_alert, {
				key: 0,
				title: $setup.error,
				type: "error",
				closable: false
			}, null, 8, ["title"])) : _createCommentVNode("v-if", true),
			$setup.success ? (_openBlock(), _createBlock(_component_el_alert, {
				key: 1,
				title: $setup.success,
				type: "success",
				closable: false
			}, null, 8, ["title"])) : _createCommentVNode("v-if", true),
			_createElementVNode("section", _hoisted_2, [
				_cache[32] || (_cache[32] = _createElementVNode(
					"h2",
					null,
					"独立业务授权根",
					-1
					/* CACHED */
				)),
				_cache[33] || (_cache[33] = _createElementVNode(
					"p",
					null,
					"登记与签署分开。授权依据读权独立，旧测试授权不会自动升级为正式授权。",
					-1
					/* CACHED */
				)),
				$setup.anchors.length && $setup.canPlatform("root-register") ? (_openBlock(), _createBlock(_component_el_button, {
					key: 0,
					onClick: $setup.openRoot
				}, {
					default: _withCtx(() => [..._cache[28] || (_cache[28] = [_createTextVNode(
						"登记授权根材料",
						-1
						/* CACHED */
					)])]),
					_: 1
				})) : _createCommentVNode("v-if", true),
				!$setup.anchors.length && !$setup.roots.length ? (_openBlock(), _createElementBlock("p", _hoisted_3, "当前没有可见的独立登记材料；技术管理身份不能替代业务签署。")) : _createCommentVNode("v-if", true),
				_createElementVNode("div", _hoisted_4, [_createVNode(_component_el_table, {
					data: $setup.roots,
					"row-key": "id"
				}, {
					default: _withCtx(() => [
						_createVNode(_component_el_table_column, {
							prop: "id",
							label: "授权根ID",
							width: "105"
						}),
						_createVNode(_component_el_table_column, {
							prop: "version",
							label: "记录版本",
							width: "100"
						}),
						_createVNode(_component_el_table_column, {
							prop: "data.materialVersion",
							label: "材料版本",
							"min-width": "150"
						}),
						_createVNode(_component_el_table_column, {
							prop: "data.mode",
							label: "材料性质",
							width: "100"
						}),
						_createVNode(_component_el_table_column, {
							prop: "data.state",
							label: "签署状态",
							width: "170"
						}),
						_createVNode(_component_el_table_column, {
							prop: "data.userId",
							label: "被授权人ID",
							width: "115"
						}),
						_createVNode(_component_el_table_column, {
							label: "操作",
							"min-width": "310"
						}, {
							default: _withCtx(({ row }) => [
								_createVNode(_component_el_button, { onClick: ($event) => $setup.showRecord(row) }, {
									default: _withCtx(() => [..._cache[29] || (_cache[29] = [_createTextVNode(
										"查看受控材料",
										-1
										/* CACHED */
									)])]),
									_: 1
								}, 8, ["onClick"]),
								row.data.state === "PENDING_SIGNATURE" && $setup.canPlatform("root-sign") ? (_openBlock(), _createBlock(_component_el_button, {
									key: 0,
									onClick: ($event) => $setup.openAction("root", row, "SIGN")
								}, {
									default: _withCtx(() => [..._cache[30] || (_cache[30] = [_createTextVNode(
										"独立签署",
										-1
										/* CACHED */
									)])]),
									_: 1
								}, 8, ["onClick"])) : _createCommentVNode("v-if", true),
								row.data.state === "ACTIVE" && $setup.canPlatform("root-sign") ? (_openBlock(), _createBlock(_component_el_button, {
									key: 1,
									onClick: ($event) => $setup.openAction("root", row, "REVOKE")
								}, {
									default: _withCtx(() => [..._cache[31] || (_cache[31] = [_createTextVNode(
										"撤销授权根",
										-1
										/* CACHED */
									)])]),
									_: 1
								}, 8, ["onClick"])) : _createCommentVNode("v-if", true)
							]),
							_: 1
						})
					]),
					_: 1
				}, 8, ["data"])])
			]),
			$setup.projectId ? (_openBlock(), _createElementBlock("section", _hoisted_5, [
				_cache[41] || (_cache[41] = _createElementVNode(
					"h2",
					null,
					"业务决议版本与独立批准",
					-1
					/* CACHED */
				)),
				_cache[42] || (_cache[42] = _createElementVNode(
					"p",
					null,
					"必须覆盖Q-01/02/04/05/07/10。Q-03/06/08/09另行决议或明确分类；正文修订建立新版本，批准和撤销保留原事实。",
					-1
					/* CACHED */
				)),
				$setup.caps.DECISION_EDIT && $setup.canPlatform("decision-edit") ? (_openBlock(), _createBlock(_component_el_button, {
					key: 0,
					onClick: _cache[1] || (_cache[1] = ($event) => $setup.openDecision())
				}, {
					default: _withCtx(() => [..._cache[34] || (_cache[34] = [_createTextVNode(
						"起草决议版本",
						-1
						/* CACHED */
					)])]),
					_: 1
				})) : _createCommentVNode("v-if", true),
				!$setup.caps.DECISION_READ ? (_openBlock(), _createElementBlock("p", _hoisted_6, "当前没有独立决议读权，不能依据需求读权查看决议正文。")) : _createCommentVNode("v-if", true),
				_createElementVNode("div", _hoisted_7, [_createVNode(_component_el_table, {
					data: $setup.decisions,
					"row-key": "id"
				}, {
					default: _withCtx(() => [
						_createVNode(_component_el_table_column, {
							prop: "id",
							label: "版本记录ID",
							width: "115"
						}),
						_createVNode(_component_el_table_column, {
							prop: "data.title",
							label: "决议标题",
							"min-width": "220"
						}),
						_createVNode(_component_el_table_column, {
							prop: "data.decisionVersion",
							label: "决议版本",
							width: "100"
						}),
						_createVNode(_component_el_table_column, {
							prop: "data.questions",
							label: "覆盖问题",
							"min-width": "200"
						}),
						_createVNode(_component_el_table_column, {
							prop: "lifecycle",
							label: "当前生命周期",
							width: "170"
						}),
						_createVNode(_component_el_table_column, {
							prop: "data.approverId",
							label: "指定批准人ID",
							width: "130"
						}),
						_createVNode(_component_el_table_column, {
							label: "操作",
							"min-width": "510"
						}, {
							default: _withCtx(({ row }) => [
								_createVNode(_component_el_button, { onClick: ($event) => $setup.showRecord(row) }, {
									default: _withCtx(() => [..._cache[35] || (_cache[35] = [_createTextVNode(
										"查看版本正文",
										-1
										/* CACHED */
									)])]),
									_: 1
								}, 8, ["onClick"]),
								_createVNode(_component_el_button, { onClick: ($event) => $setup.history(row) }, {
									default: _withCtx(() => [..._cache[36] || (_cache[36] = [_createTextVNode(
										"决议事件历史",
										-1
										/* CACHED */
									)])]),
									_: 1
								}, 8, ["onClick"]),
								$setup.caps.DECISION_EDIT && $setup.canPlatform("decision-edit") ? (_openBlock(), _createBlock(_component_el_button, {
									key: 0,
									onClick: ($event) => $setup.openDecision(row)
								}, {
									default: _withCtx(() => [..._cache[37] || (_cache[37] = [_createTextVNode(
										"基于此版本修订",
										-1
										/* CACHED */
									)])]),
									_: 1
								}, 8, ["onClick"])) : _createCommentVNode("v-if", true),
								$setup.caps.DECISION_EDIT && row.data.state === "DRAFT" && $setup.canPlatform("decision-edit") ? (_openBlock(), _createBlock(_component_el_button, {
									key: 1,
									onClick: ($event) => $setup.openAction("decision", row, "SUBMIT")
								}, {
									default: _withCtx(() => [..._cache[38] || (_cache[38] = [_createTextVNode(
										"提交批准",
										-1
										/* CACHED */
									)])]),
									_: 1
								}, 8, ["onClick"])) : _createCommentVNode("v-if", true),
								$setup.caps.DECISION_APPROVE && row.data.state === "PENDING_APPROVAL" && $setup.canPlatform("decision-approve") ? (_openBlock(), _createBlock(_component_el_button, {
									key: 2,
									onClick: ($event) => $setup.openAction("decision", row, "APPROVE")
								}, {
									default: _withCtx(() => [..._cache[39] || (_cache[39] = [_createTextVNode(
										"本人独立批准",
										-1
										/* CACHED */
									)])]),
									_: 1
								}, 8, ["onClick"])) : _createCommentVNode("v-if", true),
								$setup.caps.DECISION_APPROVE && row.data.state === "APPROVED" && $setup.canPlatform("decision-approve") ? (_openBlock(), _createBlock(_component_el_button, {
									key: 3,
									onClick: ($event) => $setup.openAction("decision", row, "REVOKE")
								}, {
									default: _withCtx(() => [..._cache[40] || (_cache[40] = [_createTextVNode(
										"撤销决议",
										-1
										/* CACHED */
									)])]),
									_: 1
								}, 8, ["onClick"])) : _createCommentVNode("v-if", true)
							]),
							_: 1
						})
					]),
					_: 1
				}, 8, ["data"])])
			])) : _createCommentVNode("v-if", true),
			$setup.projectId ? (_openBlock(), _createElementBlock("section", _hoisted_8, [
				_cache[51] || (_cache[51] = _createElementVNode(
					"h2",
					null,
					"发布检查与固定快照",
					-1
					/* CACHED */
				)),
				_cache[52] || (_cache[52] = _createElementVNode(
					"p",
					null,
					"规格基线与数据可投产分开；供数待提供不会自动阻止规格明确的需求基线。正式发布须完整闭包可见及当前有效决议。",
					-1
					/* CACHED */
				)),
				_createVNode(_component_el_button, {
					disabled: $setup.busy || $setup.loading,
					onClick: $setup.precheck
				}, {
					default: _withCtx(() => [..._cache[43] || (_cache[43] = [_createTextVNode(
						"执行发布预检查",
						-1
						/* CACHED */
					)])]),
					_: 1
				}, 8, ["disabled"]),
				$setup.gate ? (_openBlock(), _createElementBlock(
					"p",
					_hoisted_9,
					_toDisplayString($setup.gate.allowed ? "当前发布条件齐备，尚未因此发布" : "发布条件未齐备") + " · 范围版本 " + _toDisplayString($setup.gate.projectScopeVersion),
					1
					/* TEXT */
				)) : _createCommentVNode("v-if", true),
				$setup.gate ? (_openBlock(), _createElementBlock("ul", _hoisted_10, [(_openBlock(true), _createElementBlock(
					_Fragment,
					null,
					_renderList($setup.gate.blocks, (b, i) => {
						return _openBlock(), _createElementBlock(
							"li",
							{ key: i },
							_toDisplayString($setup.blockLabels[b.code] || b.code) + _toDisplayString(b.requirementId ? ` · 需求${b.requirementId}` : "") + _toDisplayString(b.question ? ` · ${b.question}` : ""),
							1
							/* TEXT */
						);
					}),
					128
					/* KEYED_FRAGMENT */
				))])) : _createCommentVNode("v-if", true),
				_createVNode(_component_el_form, { "label-position": "top" }, {
					default: _withCtx(() => [_createVNode(_component_el_form_item, { label: "发布依据或草稿说明" }, {
						default: _withCtx(() => [_createVNode(_component_el_input, {
							modelValue: $setup.publishReason,
							"onUpdate:modelValue": _cache[2] || (_cache[2] = ($event) => $setup.publishReason = $event),
							type: "textarea",
							rows: 3,
							"aria-label": "发布依据或草稿说明"
						}, null, 8, ["modelValue"])]),
						_: 1
					}), _createVNode(_component_el_form_item, { label: "幂等请求号" }, {
						default: _withCtx(() => [_createVNode(_component_el_input, {
							modelValue: $setup.requestKey,
							"onUpdate:modelValue": _cache[3] || (_cache[3] = ($event) => $setup.requestKey = $event),
							"aria-label": "幂等请求号"
						}, null, 8, ["modelValue"])]),
						_: 1
					})]),
					_: 1
				}),
				_createElementVNode("div", _hoisted_11, [
					_createVNode(_component_el_button, {
						loading: $setup.busy,
						disabled: !$setup.gate?.allowed || !$setup.caps.BASELINE_PUBLISH || !$setup.publishReason,
						onClick: _cache[4] || (_cache[4] = ($event) => $setup.publish(false))
					}, {
						default: _withCtx(() => [..._cache[44] || (_cache[44] = [_createTextVNode(
							"发布本期完整需求基线",
							-1
							/* CACHED */
						)])]),
						_: 1
					}, 8, ["loading", "disabled"]),
					_createVNode(_component_el_button, {
						loading: $setup.busy,
						disabled: !$setup.gate || !$setup.caps.EXPORT || !$setup.publishReason,
						onClick: _cache[5] || (_cache[5] = ($event) => $setup.publish(true))
					}, {
						default: _withCtx(() => [..._cache[45] || (_cache[45] = [_createTextVNode(
							"保存固定草稿快照",
							-1
							/* CACHED */
						)])]),
						_: 1
					}, 8, ["loading", "disabled"]),
					$setup.lastPublish ? (_openBlock(), _createBlock(_component_el_button, {
						key: 0,
						disabled: $setup.busy,
						onClick: $setup.retryPublish
					}, {
						default: _withCtx(() => [..._cache[46] || (_cache[46] = [_createTextVNode(
							"重试相同发布请求",
							-1
							/* CACHED */
						)])]),
						_: 1
					}, 8, ["disabled"])) : _createCommentVNode("v-if", true)
				]),
				_createElementVNode("div", _hoisted_12, [_createVNode(_component_el_table, {
					data: $setup.releases,
					"row-key": "id"
				}, {
					default: _withCtx(() => [
						_createVNode(_component_el_table_column, {
							prop: "id",
							label: "固定快照ID",
							width: "120"
						}),
						_createVNode(_component_el_table_column, {
							prop: "version",
							label: "发布版本",
							width: "100"
						}),
						_createVNode(_component_el_table_column, {
							prop: "mode",
							label: "合成与正式",
							"min-width": "220"
						}),
						_createVNode(_component_el_table_column, {
							label: "类型",
							width: "110"
						}, {
							default: _withCtx(({ row }) => [_createTextVNode(
								_toDisplayString(row.draft ? "固定草稿" : "需求基线"),
								1
								/* TEXT */
							)]),
							_: 1
						}),
						_createVNode(_component_el_table_column, {
							prop: "createdAt",
							label: "创建时间",
							"min-width": "200"
						}),
						_createVNode(_component_el_table_column, {
							label: "操作",
							"min-width": "320"
						}, {
							default: _withCtx(({ row }) => [_createVNode(_component_el_button, { onClick: ($event) => $setup.readRelease(row.id) }, {
								default: _withCtx(() => [..._cache[47] || (_cache[47] = [_createTextVNode(
									"查看冻结六表",
									-1
									/* CACHED */
								)])]),
								_: 1
							}, 8, ["onClick"]), $setup.caps.EXPORT ? (_openBlock(), _createBlock(_component_el_button, {
								key: 0,
								onClick: ($event) => $setup.createJob(row.id)
							}, {
								default: _withCtx(() => [..._cache[48] || (_cache[48] = [_createTextVNode(
									"生成六表下载",
									-1
									/* CACHED */
								)])]),
								_: 1
							}, 8, ["onClick"])) : _createCommentVNode("v-if", true)]),
							_: 1
						})
					]),
					_: 1
				}, 8, ["data"])]),
				_createVNode(_component_el_form, { inline: "" }, {
					default: _withCtx(() => [_createVNode(_component_el_form_item, { label: "导出筛选需求ID（留空为全量可见）" }, {
						default: _withCtx(() => [_createVNode(_component_el_input, {
							modelValue: $setup.exportIds,
							"onUpdate:modelValue": _cache[6] || (_cache[6] = ($event) => $setup.exportIds = $event),
							"aria-label": "导出筛选需求ID",
							placeholder: "例如 101,102"
						}, null, 8, ["modelValue"])]),
						_: 1
					})]),
					_: 1
				}),
				_cache[53] || (_cache[53] = _createElementVNode(
					"h3",
					null,
					"本轮下载任务",
					-1
					/* CACHED */
				)),
				_cache[54] || (_cache[54] = _createElementVNode(
					"p",
					null,
					"任务创建、生成、结果交付及领取均按当前权限检查；旧地址不能替代重新鉴权。",
					-1
					/* CACHED */
				)),
				_createVNode(_component_el_table, {
					data: $setup.jobs,
					"row-key": "id"
				}, {
					default: _withCtx(() => [
						_createVNode(_component_el_table_column, {
							prop: "id",
							label: "任务ID"
						}),
						_createVNode(_component_el_table_column, {
							prop: "releaseId",
							label: "快照ID"
						}),
						_createVNode(_component_el_table_column, {
							prop: "state",
							label: "状态"
						}),
						_createVNode(_component_el_table_column, {
							prop: "label",
							label: "文件范围"
						}),
						_createVNode(_component_el_table_column, {
							prop: "error",
							label: "提示"
						}),
						_createVNode(_component_el_table_column, {
							label: "操作",
							"min-width": "210"
						}, {
							default: _withCtx(({ row }) => [_createVNode(_component_el_button, { onClick: ($event) => $setup.refreshJob(row) }, {
								default: _withCtx(() => [..._cache[49] || (_cache[49] = [_createTextVNode(
									"刷新下载任务",
									-1
									/* CACHED */
								)])]),
								_: 1
							}, 8, ["onClick"]), row.state === "READY" ? (_openBlock(), _createBlock(_component_el_button, {
								key: 0,
								onClick: ($event) => $setup.claim(row)
							}, {
								default: _withCtx(() => [..._cache[50] || (_cache[50] = [_createTextVNode(
									"领取XLSX",
									-1
									/* CACHED */
								)])]),
								_: 1
							}, 8, ["onClick"])) : _createCommentVNode("v-if", true)]),
							_: 1
						})
					]),
					_: 1
				}, 8, ["data"])
			])) : _createCommentVNode("v-if", true),
			$setup.frozen ? (_openBlock(), _createElementBlock("section", _hoisted_13, [_createElementVNode(
				"h2",
				null,
				"快照" + _toDisplayString($setup.frozen.releaseId) + " · " + _toDisplayString($setup.frozen.label),
				1
				/* TEXT */
			), _createVNode(_component_el_tabs, {
				modelValue: $setup.selectedSheet,
				"onUpdate:modelValue": _cache[7] || (_cache[7] = ($event) => $setup.selectedSheet = $event)
			}, {
				default: _withCtx(() => [(_openBlock(true), _createElementBlock(
					_Fragment,
					null,
					_renderList($setup.frozen.sheets, (rows, name) => {
						return _openBlock(), _createBlock(_component_el_tab_pane, {
							key: name,
							label: String(name),
							name: String(name)
						}, {
							default: _withCtx(() => [_createElementVNode("div", _hoisted_14, [_createVNode(_component_el_table, { data: rows }, {
								default: _withCtx(() => [
									_createVNode(_component_el_table_column, {
										prop: "记录类型",
										label: "记录类型",
										width: "140"
									}),
									_createVNode(_component_el_table_column, {
										prop: "记录号",
										label: "快照关联号",
										width: "180"
									}),
									_createVNode(_component_el_table_column, {
										label: "内容",
										"min-width": "500"
									}, {
										default: _withCtx(({ row }) => [_createVNode(_component_el_button, { onClick: ($event) => $setup.showRecord(row) }, {
											default: _withCtx(() => [..._cache[55] || (_cache[55] = [_createTextVNode(
												"展开冻结记录",
												-1
												/* CACHED */
											)])]),
											_: 1
										}, 8, ["onClick"]), _createElementVNode(
											"span",
											null,
											_toDisplayString(row["功能"] || row["名称"] || row["完整性"] || row["说明"] || row["编号"] || ""),
											1
											/* TEXT */
										)]),
										_: 1
									})
								]),
								_: 1
							}, 8, ["data"])])]),
							_: 2
						}, 1032, ["label", "name"]);
					}),
					128
					/* KEYED_FRAGMENT */
				))]),
				_: 1
			}, 8, ["modelValue"])])) : _createCommentVNode("v-if", true),
			_createVNode(_component_el_dialog, {
				modelValue: $setup.rootOpen,
				"onUpdate:modelValue": _cache[13] || (_cache[13] = ($event) => $setup.rootOpen = $event),
				title: "登记授权根材料",
				width: "min(900px, 94vw)",
				class: "governance-dialog",
				"close-on-click-modal": false
			}, {
				footer: _withCtx(() => [_createVNode(_component_el_button, { onClick: _cache[12] || (_cache[12] = ($event) => $setup.rootOpen = false) }, {
					default: _withCtx(() => [..._cache[56] || (_cache[56] = [_createTextVNode(
						"取消登记",
						-1
						/* CACHED */
					)])]),
					_: 1
				}), _createVNode(_component_el_button, {
					loading: $setup.busy,
					onClick: $setup.saveRoot
				}, {
					default: _withCtx(() => [..._cache[57] || (_cache[57] = [_createTextVNode(
						"保存待签署材料",
						-1
						/* CACHED */
					)])]),
					_: 1
				}, 8, ["loading"])]),
				default: _withCtx(() => [_createVNode(_component_el_form, { "label-position": "top" }, {
					default: _withCtx(() => [
						_createVNode(_component_el_form_item, { label: "独立登记材料" }, {
							default: _withCtx(() => [_createVNode(_component_el_select, {
								modelValue: $setup.rootForm.anchorId,
								"onUpdate:modelValue": _cache[8] || (_cache[8] = ($event) => $setup.rootForm.anchorId = $event),
								"aria-label": "独立登记材料"
							}, {
								default: _withCtx(() => [(_openBlock(true), _createElementBlock(
									_Fragment,
									null,
									_renderList($setup.anchors, (a) => {
										return _openBlock(), _createBlock(_component_el_option, {
											key: a.id,
											label: `${a.id} · ${a.data.mode} · ${a.data.materialVersion}`,
											value: a.id
										}, null, 8, ["label", "value"]);
									}),
									128
									/* KEYED_FRAGMENT */
								))]),
								_: 1
							}, 8, ["modelValue"])]),
							_: 1
						}),
						_createVNode(_component_el_form_item, { label: "被授权人ID" }, {
							default: _withCtx(() => [_createVNode(_component_el_input_number, {
								modelValue: $setup.rootForm.userId,
								"onUpdate:modelValue": _cache[9] || (_cache[9] = ($event) => $setup.rootForm.userId = $event),
								"aria-label": "被授权人ID",
								min: 1
							}, null, 8, ["modelValue"])]),
							_: 1
						}),
						_createVNode(_component_el_form_item, { label: "许可动作" }, {
							default: _withCtx(() => [_createVNode(_component_el_checkbox_group, {
								modelValue: $setup.rootForm.actions,
								"onUpdate:modelValue": _cache[10] || (_cache[10] = ($event) => $setup.rootForm.actions = $event),
								"aria-label": "许可动作"
							}, {
								default: _withCtx(() => [(_openBlock(), _createElementBlock(
									_Fragment,
									null,
									_renderList($setup.actionLabels, (name, key) => {
										return _createVNode(_component_el_checkbox, {
											key,
											label: key
										}, {
											default: _withCtx(() => [_createTextVNode(
												_toDisplayString(name),
												1
												/* TEXT */
											)]),
											_: 2
										}, 1032, ["label"]);
									}),
									64
									/* STABLE_FRAGMENT */
								))]),
								_: 1
							}, 8, ["modelValue"])]),
							_: 1
						}),
						_createVNode(_component_el_form_item, { label: "可继续委派动作" }, {
							default: _withCtx(() => [_createVNode(_component_el_select, {
								modelValue: $setup.rootForm.delegableActions,
								"onUpdate:modelValue": _cache[11] || (_cache[11] = ($event) => $setup.rootForm.delegableActions = $event),
								multiple: "",
								"aria-label": "可继续委派动作"
							}, {
								default: _withCtx(() => [(_openBlock(true), _createElementBlock(
									_Fragment,
									null,
									_renderList($setup.rootForm.actions, (key) => {
										return _openBlock(), _createBlock(_component_el_option, {
											key,
											label: $setup.actionLabels[key],
											value: key
										}, null, 8, ["label", "value"]);
									}),
									128
									/* KEYED_FRAGMENT */
								))]),
								_: 1
							}, 8, ["modelValue"])]),
							_: 1
						}),
						(_openBlock(), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.rootFields, (f) => {
								return _createVNode(_component_el_form_item, {
									key: f.key,
									label: f.label
								}, {
									default: _withCtx(() => [_createVNode(_component_el_input, {
										modelValue: $setup.rootForm[f.key],
										"onUpdate:modelValue": ($event) => $setup.rootForm[f.key] = $event,
										type: f.long ? "textarea" : "text",
										rows: f.long ? 5 : undefined,
										"aria-label": f.label
									}, null, 8, [
										"modelValue",
										"onUpdate:modelValue",
										"type",
										"rows",
										"aria-label"
									])]),
									_: 2
								}, 1032, ["label"]);
							}),
							64
							/* STABLE_FRAGMENT */
						))
					]),
					_: 1
				})]),
				_: 1
			}, 8, ["modelValue"]),
			_createVNode(_component_el_dialog, {
				modelValue: $setup.decisionOpen,
				"onUpdate:modelValue": _cache[19] || (_cache[19] = ($event) => $setup.decisionOpen = $event),
				title: "起草不可变决议版本",
				width: "min(950px, 94vw)",
				class: "governance-dialog",
				"close-on-click-modal": false
			}, {
				footer: _withCtx(() => [_createVNode(_component_el_button, { onClick: _cache[18] || (_cache[18] = ($event) => $setup.decisionOpen = false) }, {
					default: _withCtx(() => [..._cache[59] || (_cache[59] = [_createTextVNode(
						"取消起草",
						-1
						/* CACHED */
					)])]),
					_: 1
				}), _createVNode(_component_el_button, {
					loading: $setup.busy,
					onClick: $setup.saveDecision
				}, {
					default: _withCtx(() => [..._cache[60] || (_cache[60] = [_createTextVNode(
						"保存决议草稿",
						-1
						/* CACHED */
					)])]),
					_: 1
				}, 8, ["loading"])]),
				default: _withCtx(() => [_createElementVNode(
					"p",
					null,
					"当前项目" + _toDisplayString($setup.projectId) + "，范围版本" + _toDisplayString($setup.decisionForm.projectScopeVersion) + "。保存为新版本，不覆盖旧正文或签署。",
					1
					/* TEXT */
				), _createVNode(_component_el_form, { "label-position": "top" }, {
					default: _withCtx(() => [
						_createVNode(_component_el_form_item, { label: "决议标题" }, {
							default: _withCtx(() => [_createVNode(_component_el_input, {
								modelValue: $setup.decisionForm.title,
								"onUpdate:modelValue": _cache[14] || (_cache[14] = ($event) => $setup.decisionForm.title = $event),
								"aria-label": "决议标题"
							}, null, 8, ["modelValue"])]),
							_: 1
						}),
						_createVNode(_component_el_form_item, { label: "覆盖问题" }, {
							default: _withCtx(() => [_createVNode(_component_el_checkbox_group, {
								modelValue: $setup.decisionForm.questions,
								"onUpdate:modelValue": _cache[15] || (_cache[15] = ($event) => $setup.decisionForm.questions = $event),
								"aria-label": "覆盖问题"
							}, {
								default: _withCtx(() => [(_openBlock(true), _createElementBlock(
									_Fragment,
									null,
									_renderList($setup.questions, (q) => {
										return _openBlock(), _createBlock(_component_el_checkbox, {
											key: q,
											label: q
										}, {
											default: _withCtx(() => [_createTextVNode(
												_toDisplayString(q),
												1
												/* TEXT */
											)]),
											_: 2
										}, 1032, ["label"]);
									}),
									128
									/* KEYED_FRAGMENT */
								))]),
								_: 1
							}, 8, ["modelValue"])]),
							_: 1
						}),
						(_openBlock(), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.decisionFields, (f) => {
								return _createVNode(_component_el_form_item, {
									key: f.key,
									label: f.label
								}, {
									default: _withCtx(() => [_createVNode(_component_el_input, {
										modelValue: $setup.decisionForm[f.key],
										"onUpdate:modelValue": ($event) => $setup.decisionForm[f.key] = $event,
										type: f.long ? "textarea" : "text",
										rows: f.long ? 6 : undefined,
										"aria-label": f.label
									}, null, 8, [
										"modelValue",
										"onUpdate:modelValue",
										"type",
										"rows",
										"aria-label"
									])]),
									_: 2
								}, 1032, ["label"]);
							}),
							64
							/* STABLE_FRAGMENT */
						)),
						_createVNode(_component_el_form_item, { label: "指定独立批准人ID" }, {
							default: _withCtx(() => [_createVNode(_component_el_input_number, {
								modelValue: $setup.decisionForm.approverId,
								"onUpdate:modelValue": _cache[16] || (_cache[16] = ($event) => $setup.decisionForm.approverId = $event),
								min: 1,
								"aria-label": "指定独立批准人ID"
							}, null, 8, ["modelValue"])]),
							_: 1
						}),
						_createVNode(_component_el_form_item, { label: "其他未决问题分类" }, {
							default: _withCtx(() => [(_openBlock(), _createElementBlock(
								_Fragment,
								null,
								_renderList($setup.otherQuestions, (q) => {
									return _createElementVNode("div", { key: q }, [_createElementVNode(
										"label",
										null,
										_toDisplayString(q),
										1
										/* TEXT */
									), _createVNode(_component_el_select, {
										modelValue: $setup.decisionForm.otherQuestions[q],
										"onUpdate:modelValue": ($event) => $setup.decisionForm.otherQuestions[q] = $event,
										"aria-label": `${q}分类`
									}, {
										default: _withCtx(() => [
											_createVNode(_component_el_option, {
												label: "基线阻断",
												value: "BASELINE_BLOCK"
											}),
											_createVNode(_component_el_option, {
												label: "数据准备",
												value: "DATA_READINESS"
											}),
											_createVNode(_component_el_option, {
												label: "建议",
												value: "ADVISORY"
											})
										]),
										_: 1
									}, 8, [
										"modelValue",
										"onUpdate:modelValue",
										"aria-label"
									])]);
								}),
								64
								/* STABLE_FRAGMENT */
							))]),
							_: 1
						}),
						_createVNode(_component_el_checkbox, {
							modelValue: $setup.decisionForm.semanticChange,
							"onUpdate:modelValue": _cache[17] || (_cache[17] = ($event) => $setup.decisionForm.semanticChange = $event)
						}, {
							default: _withCtx(() => [..._cache[58] || (_cache[58] = [_createTextVNode(
								"本版本改变业务语义或验收条件",
								-1
								/* CACHED */
							)])]),
							_: 1
						}, 8, ["modelValue"])
					]),
					_: 1
				})]),
				_: 1
			}, 8, ["modelValue"]),
			_createVNode(_component_el_dialog, {
				modelValue: $setup.actionOpen,
				"onUpdate:modelValue": _cache[22] || (_cache[22] = ($event) => $setup.actionOpen = $event),
				title: "记录本人动作与依据",
				width: "min(700px, 94vw)",
				class: "governance-dialog",
				"close-on-click-modal": false
			}, {
				footer: _withCtx(() => [_createVNode(_component_el_button, { onClick: _cache[21] || (_cache[21] = ($event) => $setup.actionOpen = false) }, {
					default: _withCtx(() => [..._cache[61] || (_cache[61] = [_createTextVNode(
						"取消本人动作",
						-1
						/* CACHED */
					)])]),
					_: 1
				}), _createVNode(_component_el_button, {
					loading: $setup.busy,
					disabled: !$setup.actionForm.reason.trim(),
					onClick: $setup.saveAction
				}, {
					default: _withCtx(() => [..._cache[62] || (_cache[62] = [_createTextVNode(
						"提交本人动作",
						-1
						/* CACHED */
					)])]),
					_: 1
				}, 8, ["loading", "disabled"])]),
				default: _withCtx(() => [_createElementVNode(
					"p",
					null,
					_toDisplayString($setup.actionForm.kind) + " · 记录" + _toDisplayString($setup.actionForm.id) + " · " + _toDisplayString($setup.actionForm.action) + " · 版本" + _toDisplayString($setup.actionForm.version),
					1
					/* TEXT */
				), _createVNode(_component_el_input, {
					modelValue: $setup.actionForm.reason,
					"onUpdate:modelValue": _cache[20] || (_cache[20] = ($event) => $setup.actionForm.reason = $event),
					type: "textarea",
					rows: 6,
					"aria-label": "本人动作依据"
				}, null, 8, ["modelValue"])]),
				_: 1
			}, 8, ["modelValue"]),
			_createVNode(_component_el_dialog, {
				modelValue: $setup.recordOpen,
				"onUpdate:modelValue": _cache[24] || (_cache[24] = ($event) => $setup.recordOpen = $event),
				title: "冻结记录与受控正文",
				width: "min(1100px, 94vw)",
				class: "governance-dialog"
			}, {
				footer: _withCtx(() => [_createVNode(_component_el_button, { onClick: _cache[23] || (_cache[23] = ($event) => $setup.recordOpen = false) }, {
					default: _withCtx(() => [..._cache[63] || (_cache[63] = [_createTextVNode(
						"关闭记录",
						-1
						/* CACHED */
					)])]),
					_: 1
				})]),
				default: _withCtx(() => [_createElementVNode(
					"pre",
					_hoisted_15,
					_toDisplayString($setup.recordText),
					1
					/* TEXT */
				)]),
				_: 1
			}, 8, ["modelValue"])
		]),
		_: 1
	});
}
import "/src/views/hrm/payroll/governance/index.vue?t=1792406946093&vue&type=style&index=0&scoped=706feab4&lang.css";
import "/src/views/hrm/payroll/governance/index.vue?vue&type=style&index=1&lang.css";
_sfc_main.__hmrId = "706feab4";
typeof __VUE_HMR_RUNTIME__ !== "undefined" && __VUE_HMR_RUNTIME__.createRecord(_sfc_main.__hmrId, _sfc_main);
import.meta.hot.on("file-changed", ({ file }) => {
	__VUE_HMR_RUNTIME__.CHANGED_FILE = file;
});
import.meta.hot.accept((mod) => {
	if (!mod) return;
	const { default: updated, _rerender_only } = mod;
	if (_rerender_only) {
		__VUE_HMR_RUNTIME__.rerender(updated.__hmrId, updated.render);
	} else {
		__VUE_HMR_RUNTIME__.reload(updated.__hmrId, updated);
	}
});
import _export_sfc from "/@id/__x00__plugin-vue:export-helper";
export default /*#__PURE__*/ _export_sfc(_sfc_main, [
	["render", _sfc_render],
	["__scopeId", "data-v-706feab4"],
	["__file", "/Users/waitforpro/source/github-yudao/ruoyi-vue-pro/yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/governance/index.vue"]
]);

//# sourceMappingURL=data:application/json;base64,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QUNOLE9BQU07SUFDTixPQUFNOztJQUdNLFFBQU0sZUFDd0MsQ0FBdkQsYUFBdUQsd0JBQTNDLFNBQUssd0NBQUUsb0JBQVU7NkJBQWM7TUFBSjtNQUFJOztLQUFBOzs7NEJBRDdDLENBREQ7S0FDQztLQUREO0tBQ0MsaUJBRHNCLGlCQUFVO0tBQUE7O0lBQUEiLCJuYW1lcyI6W10sImlnbm9yZUxpc3QiOltdLCJzb3VyY2VzIjpbImluZGV4LnZ1ZSJdLCJzb3VyY2VzQ29udGVudCI6WyI8dGVtcGxhdGU+XG4gIDxDb250ZW50V3JhcCBjbGFzcz1cImdvdmVybmFuY2UtcGFnZVwiPlxuICAgIDxoMT7kuJrliqHlhrPorq7jgIHlj5HluIPkuI7lv6vnhaflr7zlh7o8L2gxPlxuICAgIDxlbC1hbGVydFxuICAgICAgdGl0bGU9XCJURVNU5p2Q5paZ5Y+K5ZCI5oiQ562+572y5LuF6aqM6K+B57O757uf5a6e546w77yM5LiN5p6E5oiQ55yf5a6e5Lia5Yqh5om55YeG44CC5q2j5byP5o6I5p2D5qC55b+F6aG75pyJ5a6e6ZmF5pyJ5p2D5Lia5Yqh5Lq65ZGY5o+Q5L6b55qE5Y+X5o6n562+572y5p2Q5paZ44CCXCJcbiAgICAgIHR5cGU9XCJ3YXJuaW5nXCJcbiAgICAgIDpjbG9zYWJsZT1cImZhbHNlXCJcbiAgICAvPlxuICAgIDxkaXYgY2xhc3M9XCJ0b29sYmFyXCI+XG4gICAgICA8ZWwtc2VsZWN0IGFyaWEtbGFiZWw9XCLmsrvnkIbpobnnm65cIiA6bW9kZWwtdmFsdWU9XCJwcm9qZWN0SWRcIiBAY2hhbmdlPVwic3dpdGNoUHJvamVjdFwiPlxuICAgICAgICA8ZWwtb3B0aW9uIHYtaWY9XCJhbmNob3JzLmxlbmd0aFwiIGxhYmVsPVwi56ef5oi36aG555uu5Yib5bu65o6I5p2DXCIgOnZhbHVlPVwiMFwiIC8+XG4gICAgICAgIDxlbC1vcHRpb24gdi1mb3I9XCJwIGluIHByb2plY3RzXCIgOmtleT1cInAuaWRcIiA6bGFiZWw9XCJgJHtwLmlkfSDCtyAke3AubmFtZX1gXCIgOnZhbHVlPVwicC5pZFwiIC8+XG4gICAgICA8L2VsLXNlbGVjdD5cbiAgICAgIDxlbC1idXR0b24gOmxvYWRpbmc9XCJsb2FkaW5nXCIgQGNsaWNrPVwibG9hZFwiPuWIt+aWsOWGs+iuruS4juWPkeW4gzwvZWwtYnV0dG9uPlxuICAgICAgPGVsLWJ1dHRvbiB2LWlmPVwicHJvamVjdElkXCIgQGNsaWNrPVwiZ29SZXZpZXdcIj7ov5Tlm57ljYHop4blm77or4TlrqE8L2VsLWJ1dHRvbj5cbiAgICAgIDxlbC1idXR0b25cbiAgICAgICAgdi1pZj1cInByb2plY3RJZFwiXG4gICAgICAgIEBjbGljaz1cIlxuICAgICAgICAgIHJvdXRlci5wdXNoKHsgcGF0aDogJy9ocm0tcGF5cm9sbC9yZXF1aXJlbWVudCcsIHF1ZXJ5OiB7IHByb2plY3RJZCwgbW9kdWxlOiAnQ09MJyB9IH0pXG4gICAgICAgIFwiXG4gICAgICAgID7pnIDmsYLjgIHmiJDlkZjlj4rlrp7pmYXmnaXmupA8L2VsLWJ1dHRvblxuICAgICAgPlxuICAgIDwvZGl2PlxuICAgIDxwXG4gICAgICA+e3sgcHJvamVjdD8ubmFtZSB8fCAocHJvamVjdElkID09PSAwID8gJ+enn+aIt+WIm+W7uuaOiOadgycgOiAn6YCJ5oup5pyJ5p2D6aG555uuJykgfX0gwrcg6IyD5Zu054mI5pysXG4gICAgICB7eyBwcm9qZWN0Py5wcm9qZWN0U2NvcGVWZXJzaW9uID8/ICfmnKrlrprkuYknIH19PC9wXG4gICAgPlxuICAgIDxlbC1hbGVydCB2LWlmPVwiZXJyb3JcIiA6dGl0bGU9XCJlcnJvclwiIHR5cGU9XCJlcnJvclwiIDpjbG9zYWJsZT1cImZhbHNlXCIgLz5cbiAgICA8ZWwtYWxlcnQgdi1pZj1cInN1Y2Nlc3NcIiA6dGl0bGU9XCJzdWNjZXNzXCIgdHlwZT1cInN1Y2Nlc3NcIiA6Y2xvc2FibGU9XCJmYWxzZVwiIC8+XG4gICAgPHNlY3Rpb24gYXJpYS1sYWJlbD1cIueLrOeri+S4muWKoeaOiOadg+aguVwiPlxuICAgICAgPGgyPueLrOeri+S4muWKoeaOiOadg+aguTwvaDI+XG4gICAgICA8cD7nmbvorrDkuI7nrb7nvbLliIblvIDjgILmjojmnYPkvp3mja7or7vmnYPni6znq4vvvIzml6fmtYvor5XmjojmnYPkuI3kvJroh6rliqjljYfnuqfkuLrmraPlvI/mjojmnYPjgII8L3A+XG4gICAgICA8ZWwtYnV0dG9uIHYtaWY9XCJhbmNob3JzLmxlbmd0aCAmJiBjYW5QbGF0Zm9ybSgncm9vdC1yZWdpc3RlcicpXCIgQGNsaWNrPVwib3BlblJvb3RcIlxuICAgICAgICA+55m76K6w5o6I5p2D5qC55p2Q5paZPC9lbC1idXR0b25cbiAgICAgID5cbiAgICAgIDxwIHYtaWY9XCIhYW5jaG9ycy5sZW5ndGggJiYgIXJvb3RzLmxlbmd0aFwiXG4gICAgICAgID7lvZPliY3msqHmnInlj6/op4HnmoTni6znq4vnmbvorrDmnZDmlpnvvJvmioDmnK/nrqHnkIbouqvku73kuI3og73mm7/ku6PkuJrliqHnrb7nvbLjgII8L3BcbiAgICAgID5cbiAgICAgIDxkaXYgY2xhc3M9XCJ3aWRlLXRhYmxlXCIgdGFiaW5kZXg9XCIwXCIgYXJpYS1sYWJlbD1cIuaOiOadg+agueihqO+8jOWPr+aoquWQkea7muWKqFwiPlxuICAgICAgICA8ZWwtdGFibGUgOmRhdGE9XCJyb290c1wiIHJvdy1rZXk9XCJpZFwiPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gcHJvcD1cImlkXCIgbGFiZWw9XCLmjojmnYPmoLlJRFwiIHdpZHRoPVwiMTA1XCIgLz5cbiAgICAgICAgICA8ZWwtdGFibGUtY29sdW1uIHByb3A9XCJ2ZXJzaW9uXCIgbGFiZWw9XCLorrDlvZXniYjmnKxcIiB3aWR0aD1cIjEwMFwiIC8+XG4gICAgICAgICAgPGVsLXRhYmxlLWNvbHVtbiBwcm9wPVwiZGF0YS5tYXRlcmlhbFZlcnNpb25cIiBsYWJlbD1cIuadkOaWmeeJiOacrFwiIG1pbi13aWR0aD1cIjE1MFwiIC8+XG4gICAgICAgICAgPGVsLXRhYmxlLWNvbHVtbiBwcm9wPVwiZGF0YS5tb2RlXCIgbGFiZWw9XCLmnZDmlpnmgKfotKhcIiB3aWR0aD1cIjEwMFwiIC8+XG4gICAgICAgICAgPGVsLXRhYmxlLWNvbHVtbiBwcm9wPVwiZGF0YS5zdGF0ZVwiIGxhYmVsPVwi562+572y54q25oCBXCIgd2lkdGg9XCIxNzBcIiAvPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gcHJvcD1cImRhdGEudXNlcklkXCIgbGFiZWw9XCLooqvmjojmnYPkurpJRFwiIHdpZHRoPVwiMTE1XCIgLz5cbiAgICAgICAgICA8ZWwtdGFibGUtY29sdW1uIGxhYmVsPVwi5pON5L2cXCIgbWluLXdpZHRoPVwiMzEwXCJcbiAgICAgICAgICAgID48dGVtcGxhdGUgI2RlZmF1bHQ9XCJ7IHJvdyB9XCI+XG4gICAgICAgICAgICAgIDxlbC1idXR0b24gQGNsaWNrPVwic2hvd1JlY29yZChyb3cpXCI+5p+l55yL5Y+X5o6n5p2Q5paZPC9lbC1idXR0b24+XG4gICAgICAgICAgICAgIDxlbC1idXR0b25cbiAgICAgICAgICAgICAgICB2LWlmPVwicm93LmRhdGEuc3RhdGUgPT09ICdQRU5ESU5HX1NJR05BVFVSRScgJiYgY2FuUGxhdGZvcm0oJ3Jvb3Qtc2lnbicpXCJcbiAgICAgICAgICAgICAgICBAY2xpY2s9XCJvcGVuQWN0aW9uKCdyb290Jywgcm93LCAnU0lHTicpXCJcbiAgICAgICAgICAgICAgICA+54us56uL562+572yPC9lbC1idXR0b25cbiAgICAgICAgICAgICAgPlxuICAgICAgICAgICAgICA8ZWwtYnV0dG9uXG4gICAgICAgICAgICAgICAgdi1pZj1cInJvdy5kYXRhLnN0YXRlID09PSAnQUNUSVZFJyAmJiBjYW5QbGF0Zm9ybSgncm9vdC1zaWduJylcIlxuICAgICAgICAgICAgICAgIEBjbGljaz1cIm9wZW5BY3Rpb24oJ3Jvb3QnLCByb3csICdSRVZPS0UnKVwiXG4gICAgICAgICAgICAgICAgPuaSpOmUgOaOiOadg+aguTwvZWwtYnV0dG9uXG4gICAgICAgICAgICAgID5cbiAgICAgICAgICAgIDwvdGVtcGxhdGU+PC9lbC10YWJsZS1jb2x1bW5cbiAgICAgICAgICA+XG4gICAgICAgIDwvZWwtdGFibGU+XG4gICAgICA8L2Rpdj5cbiAgICA8L3NlY3Rpb24+XG4gICAgPHNlY3Rpb24gdi1pZj1cInByb2plY3RJZFwiIGFyaWEtbGFiZWw9XCLkuJrliqHlhrPorq7niYjmnKxcIj5cbiAgICAgIDxoMj7kuJrliqHlhrPorq7niYjmnKzkuI7ni6znq4vmibnlh4Y8L2gyPlxuICAgICAgPHBcbiAgICAgICAgPuW/hemhu+imhuebllEtMDEvMDIvMDQvMDUvMDcvMTDjgIJRLTAzLzA2LzA4LzA55Y+m6KGM5Yaz6K6u5oiW5piO56Gu5YiG57G777yb5q2j5paH5L+u6K6i5bu656uL5paw54mI5pys77yM5om55YeG5ZKM5pKk6ZSA5L+d55WZ5Y6f5LqL5a6e44CCPC9wXG4gICAgICA+XG4gICAgICA8ZWwtYnV0dG9uIHYtaWY9XCJjYXBzLkRFQ0lTSU9OX0VESVQgJiYgY2FuUGxhdGZvcm0oJ2RlY2lzaW9uLWVkaXQnKVwiIEBjbGljaz1cIm9wZW5EZWNpc2lvbigpXCJcbiAgICAgICAgPui1t+iNieWGs+iurueJiOacrDwvZWwtYnV0dG9uXG4gICAgICA+XG4gICAgICA8cCB2LWlmPVwiIWNhcHMuREVDSVNJT05fUkVBRFwiPuW9k+WJjeayoeacieeLrOeri+WGs+iuruivu+adg++8jOS4jeiDveS+neaNrumcgOaxguivu+adg+afpeeci+WGs+iuruato+aWh+OAgjwvcD5cbiAgICAgIDxkaXYgY2xhc3M9XCJ3aWRlLXRhYmxlXCIgdGFiaW5kZXg9XCIwXCIgYXJpYS1sYWJlbD1cIuWGs+iuruihqO+8jOWPr+aoquWQkea7muWKqFwiXG4gICAgICAgID48ZWwtdGFibGUgOmRhdGE9XCJkZWNpc2lvbnNcIiByb3cta2V5PVwiaWRcIj5cbiAgICAgICAgICA8ZWwtdGFibGUtY29sdW1uIHByb3A9XCJpZFwiIGxhYmVsPVwi54mI5pys6K6w5b2VSURcIiB3aWR0aD1cIjExNVwiIC8+XG4gICAgICAgICAgPGVsLXRhYmxlLWNvbHVtbiBwcm9wPVwiZGF0YS50aXRsZVwiIGxhYmVsPVwi5Yaz6K6u5qCH6aKYXCIgbWluLXdpZHRoPVwiMjIwXCIgLz5cbiAgICAgICAgICA8ZWwtdGFibGUtY29sdW1uIHByb3A9XCJkYXRhLmRlY2lzaW9uVmVyc2lvblwiIGxhYmVsPVwi5Yaz6K6u54mI5pysXCIgd2lkdGg9XCIxMDBcIiAvPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gcHJvcD1cImRhdGEucXVlc3Rpb25zXCIgbGFiZWw9XCLopobnm5bpl67pophcIiBtaW4td2lkdGg9XCIyMDBcIiAvPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gcHJvcD1cImxpZmVjeWNsZVwiIGxhYmVsPVwi5b2T5YmN55Sf5ZG95ZGo5pyfXCIgd2lkdGg9XCIxNzBcIiAvPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gcHJvcD1cImRhdGEuYXBwcm92ZXJJZFwiIGxhYmVsPVwi5oyH5a6a5om55YeG5Lq6SURcIiB3aWR0aD1cIjEzMFwiIC8+XG4gICAgICAgICAgPGVsLXRhYmxlLWNvbHVtbiBsYWJlbD1cIuaTjeS9nFwiIG1pbi13aWR0aD1cIjUxMFwiXG4gICAgICAgICAgICA+PHRlbXBsYXRlICNkZWZhdWx0PVwieyByb3cgfVwiPlxuICAgICAgICAgICAgICA8ZWwtYnV0dG9uIEBjbGljaz1cInNob3dSZWNvcmQocm93KVwiPuafpeeci+eJiOacrOato+aWhzwvZWwtYnV0dG9uPlxuICAgICAgICAgICAgICA8ZWwtYnV0dG9uIEBjbGljaz1cImhpc3Rvcnkocm93KVwiPuWGs+iuruS6i+S7tuWOhuWPsjwvZWwtYnV0dG9uPlxuICAgICAgICAgICAgICA8ZWwtYnV0dG9uXG4gICAgICAgICAgICAgICAgdi1pZj1cImNhcHMuREVDSVNJT05fRURJVCAmJiBjYW5QbGF0Zm9ybSgnZGVjaXNpb24tZWRpdCcpXCJcbiAgICAgICAgICAgICAgICBAY2xpY2s9XCJvcGVuRGVjaXNpb24ocm93KVwiXG4gICAgICAgICAgICAgICAgPuWfuuS6juatpOeJiOacrOS/ruiuojwvZWwtYnV0dG9uXG4gICAgICAgICAgICAgID5cbiAgICAgICAgICAgICAgPGVsLWJ1dHRvblxuICAgICAgICAgICAgICAgIHYtaWY9XCJcbiAgICAgICAgICAgICAgICAgIGNhcHMuREVDSVNJT05fRURJVCAmJiByb3cuZGF0YS5zdGF0ZSA9PT0gJ0RSQUZUJyAmJiBjYW5QbGF0Zm9ybSgnZGVjaXNpb24tZWRpdCcpXG4gICAgICAgICAgICAgICAgXCJcbiAgICAgICAgICAgICAgICBAY2xpY2s9XCJvcGVuQWN0aW9uKCdkZWNpc2lvbicsIHJvdywgJ1NVQk1JVCcpXCJcbiAgICAgICAgICAgICAgICA+5o+Q5Lqk5om55YeGPC9lbC1idXR0b25cbiAgICAgICAgICAgICAgPlxuICAgICAgICAgICAgICA8ZWwtYnV0dG9uXG4gICAgICAgICAgICAgICAgdi1pZj1cIlxuICAgICAgICAgICAgICAgICAgY2Fwcy5ERUNJU0lPTl9BUFBST1ZFICYmXG4gICAgICAgICAgICAgICAgICByb3cuZGF0YS5zdGF0ZSA9PT0gJ1BFTkRJTkdfQVBQUk9WQUwnICYmXG4gICAgICAgICAgICAgICAgICBjYW5QbGF0Zm9ybSgnZGVjaXNpb24tYXBwcm92ZScpXG4gICAgICAgICAgICAgICAgXCJcbiAgICAgICAgICAgICAgICBAY2xpY2s9XCJvcGVuQWN0aW9uKCdkZWNpc2lvbicsIHJvdywgJ0FQUFJPVkUnKVwiXG4gICAgICAgICAgICAgICAgPuacrOS6uueLrOeri+aJueWHhjwvZWwtYnV0dG9uXG4gICAgICAgICAgICAgID5cbiAgICAgICAgICAgICAgPGVsLWJ1dHRvblxuICAgICAgICAgICAgICAgIHYtaWY9XCJcbiAgICAgICAgICAgICAgICAgIGNhcHMuREVDSVNJT05fQVBQUk9WRSAmJlxuICAgICAgICAgICAgICAgICAgcm93LmRhdGEuc3RhdGUgPT09ICdBUFBST1ZFRCcgJiZcbiAgICAgICAgICAgICAgICAgIGNhblBsYXRmb3JtKCdkZWNpc2lvbi1hcHByb3ZlJylcbiAgICAgICAgICAgICAgICBcIlxuICAgICAgICAgICAgICAgIEBjbGljaz1cIm9wZW5BY3Rpb24oJ2RlY2lzaW9uJywgcm93LCAnUkVWT0tFJylcIlxuICAgICAgICAgICAgICAgID7mkqTplIDlhrPorq48L2VsLWJ1dHRvblxuICAgICAgICAgICAgICA+XG4gICAgICAgICAgICA8L3RlbXBsYXRlPjwvZWwtdGFibGUtY29sdW1uXG4gICAgICAgICAgPlxuICAgICAgICA8L2VsLXRhYmxlPjwvZGl2XG4gICAgICA+XG4gICAgPC9zZWN0aW9uPlxuICAgIDxzZWN0aW9uIHYtaWY9XCJwcm9qZWN0SWRcIiBhcmlhLWxhYmVsPVwi5Y+R5biD5qOA5p+l5LiO5Zu65a6a5b+r54WnXCI+XG4gICAgICA8aDI+5Y+R5biD5qOA5p+l5LiO5Zu65a6a5b+r54WnPC9oMj5cbiAgICAgIDxwXG4gICAgICAgID7op4TmoLzln7rnur/kuI7mlbDmja7lj6/mipXkuqfliIblvIDvvJvkvpvmlbDlvoXmj5DkvpvkuI3kvJroh6rliqjpmLvmraLop4TmoLzmmI7noa7nmoTpnIDmsYLln7rnur/jgILmraPlvI/lj5HluIPpobvlrozmlbTpl63ljIXlj6/op4Hlj4rlvZPliY3mnInmlYjlhrPorq7jgII8L3BcbiAgICAgID5cbiAgICAgIDxlbC1idXR0b24gOmRpc2FibGVkPVwiYnVzeSB8fCBsb2FkaW5nXCIgQGNsaWNrPVwicHJlY2hlY2tcIj7miafooYzlj5HluIPpooTmo4Dmn6U8L2VsLWJ1dHRvbj5cbiAgICAgIDxwIHYtaWY9XCJnYXRlXCJcbiAgICAgICAgPnt7IGdhdGUuYWxsb3dlZCA/ICflvZPliY3lj5HluIPmnaHku7bpvZDlpIfvvIzlsJrmnKrlm6DmraTlj5HluIMnIDogJ+WPkeW4g+adoeS7tuacqum9kOWkhycgfX0gwrcg6IyD5Zu054mI5pysXG4gICAgICAgIHt7IGdhdGUucHJvamVjdFNjb3BlVmVyc2lvbiB9fTwvcFxuICAgICAgPlxuICAgICAgPHVsIHYtaWY9XCJnYXRlXCJcbiAgICAgICAgPjxsaSB2LWZvcj1cIihiLCBpKSBpbiBnYXRlLmJsb2Nrc1wiIDprZXk9XCJpXCJcbiAgICAgICAgICA+e3sgYmxvY2tMYWJlbHNbYi5jb2RlXSB8fCBiLmNvZGUgfX17eyBiLnJlcXVpcmVtZW50SWQgPyBgIMK3IOmcgOaxgiR7Yi5yZXF1aXJlbWVudElkfWAgOiAnJ1xuICAgICAgICAgIH19e3sgYi5xdWVzdGlvbiA/IGAgwrcgJHtiLnF1ZXN0aW9ufWAgOiAnJyB9fTwvbGlcbiAgICAgICAgPjwvdWxcbiAgICAgID5cbiAgICAgIDxlbC1mb3JtIGxhYmVsLXBvc2l0aW9uPVwidG9wXCI+XG4gICAgICAgIDxlbC1mb3JtLWl0ZW0gbGFiZWw9XCLlj5HluIPkvp3mja7miJbojYnnqL/or7TmmI5cIlxuICAgICAgICAgID48ZWwtaW5wdXRcbiAgICAgICAgICAgIHYtbW9kZWw9XCJwdWJsaXNoUmVhc29uXCJcbiAgICAgICAgICAgIHR5cGU9XCJ0ZXh0YXJlYVwiXG4gICAgICAgICAgICA6cm93cz1cIjNcIlxuICAgICAgICAgICAgYXJpYS1sYWJlbD1cIuWPkeW4g+S+neaNruaIluiNieeov+ivtOaYjlwiXG4gICAgICAgIC8+PC9lbC1mb3JtLWl0ZW0+XG4gICAgICAgIDxlbC1mb3JtLWl0ZW0gbGFiZWw9XCLluYLnrYnor7fmsYLlj7dcIlxuICAgICAgICAgID48ZWwtaW5wdXQgdi1tb2RlbD1cInJlcXVlc3RLZXlcIiBhcmlhLWxhYmVsPVwi5bmC562J6K+35rGC5Y+3XCJcbiAgICAgICAgLz48L2VsLWZvcm0taXRlbT5cbiAgICAgIDwvZWwtZm9ybT5cbiAgICAgIDxkaXYgY2xhc3M9XCJ0b29sYmFyXCI+XG4gICAgICAgIDxlbC1idXR0b25cbiAgICAgICAgICA6bG9hZGluZz1cImJ1c3lcIlxuICAgICAgICAgIDpkaXNhYmxlZD1cIiFnYXRlPy5hbGxvd2VkIHx8ICFjYXBzLkJBU0VMSU5FX1BVQkxJU0ggfHwgIXB1Ymxpc2hSZWFzb25cIlxuICAgICAgICAgIEBjbGljaz1cInB1Ymxpc2goZmFsc2UpXCJcbiAgICAgICAgICA+5Y+R5biD5pys5pyf5a6M5pW06ZyA5rGC5Z+657q/PC9lbC1idXR0b25cbiAgICAgICAgPlxuICAgICAgICA8ZWwtYnV0dG9uXG4gICAgICAgICAgOmxvYWRpbmc9XCJidXN5XCJcbiAgICAgICAgICA6ZGlzYWJsZWQ9XCIhZ2F0ZSB8fCAhY2Fwcy5FWFBPUlQgfHwgIXB1Ymxpc2hSZWFzb25cIlxuICAgICAgICAgIEBjbGljaz1cInB1Ymxpc2godHJ1ZSlcIlxuICAgICAgICAgID7kv53lrZjlm7rlrprojYnnqL/lv6vnhac8L2VsLWJ1dHRvblxuICAgICAgICA+XG4gICAgICAgIDxlbC1idXR0b24gdi1pZj1cImxhc3RQdWJsaXNoXCIgOmRpc2FibGVkPVwiYnVzeVwiIEBjbGljaz1cInJldHJ5UHVibGlzaFwiXG4gICAgICAgICAgPumHjeivleebuOWQjOWPkeW4g+ivt+axgjwvZWwtYnV0dG9uXG4gICAgICAgID5cbiAgICAgIDwvZGl2PlxuICAgICAgPGRpdiBjbGFzcz1cIndpZGUtdGFibGVcIiB0YWJpbmRleD1cIjBcIiBhcmlhLWxhYmVsPVwi5Y+R5biD5b+r54Wn6KGo77yM5Y+v5qiq5ZCR5rua5YqoXCJcbiAgICAgICAgPjxlbC10YWJsZSA6ZGF0YT1cInJlbGVhc2VzXCIgcm93LWtleT1cImlkXCI+XG4gICAgICAgICAgPGVsLXRhYmxlLWNvbHVtbiBwcm9wPVwiaWRcIiBsYWJlbD1cIuWbuuWumuW/q+eFp0lEXCIgd2lkdGg9XCIxMjBcIiAvPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gcHJvcD1cInZlcnNpb25cIiBsYWJlbD1cIuWPkeW4g+eJiOacrFwiIHdpZHRoPVwiMTAwXCIgLz5cbiAgICAgICAgICA8ZWwtdGFibGUtY29sdW1uIHByb3A9XCJtb2RlXCIgbGFiZWw9XCLlkIjmiJDkuI7mraPlvI9cIiBtaW4td2lkdGg9XCIyMjBcIiAvPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gbGFiZWw9XCLnsbvlnotcIiB3aWR0aD1cIjExMFwiXG4gICAgICAgICAgICA+PHRlbXBsYXRlICNkZWZhdWx0PVwieyByb3cgfVwiPnt7XG4gICAgICAgICAgICAgIHJvdy5kcmFmdCA/ICflm7rlrprojYnnqL8nIDogJ+mcgOaxguWfuue6vydcbiAgICAgICAgICAgIH19PC90ZW1wbGF0ZT48L2VsLXRhYmxlLWNvbHVtblxuICAgICAgICAgID5cbiAgICAgICAgICA8ZWwtdGFibGUtY29sdW1uIHByb3A9XCJjcmVhdGVkQXRcIiBsYWJlbD1cIuWIm+W7uuaXtumXtFwiIG1pbi13aWR0aD1cIjIwMFwiIC8+XG4gICAgICAgICAgPGVsLXRhYmxlLWNvbHVtbiBsYWJlbD1cIuaTjeS9nFwiIG1pbi13aWR0aD1cIjMyMFwiXG4gICAgICAgICAgICA+PHRlbXBsYXRlICNkZWZhdWx0PVwieyByb3cgfVwiPlxuICAgICAgICAgICAgICA8ZWwtYnV0dG9uIEBjbGljaz1cInJlYWRSZWxlYXNlKHJvdy5pZClcIj7mn6XnnIvlhrvnu5Plha3ooag8L2VsLWJ1dHRvbj5cbiAgICAgICAgICAgICAgPGVsLWJ1dHRvbiB2LWlmPVwiY2Fwcy5FWFBPUlRcIiBAY2xpY2s9XCJjcmVhdGVKb2Iocm93LmlkKVwiPueUn+aIkOWFreihqOS4i+i9vTwvZWwtYnV0dG9uPlxuICAgICAgICAgICAgPC90ZW1wbGF0ZT48L2VsLXRhYmxlLWNvbHVtblxuICAgICAgICAgID5cbiAgICAgICAgPC9lbC10YWJsZT48L2RpdlxuICAgICAgPlxuICAgICAgPGVsLWZvcm0gaW5saW5lXG4gICAgICAgID48ZWwtZm9ybS1pdGVtIGxhYmVsPVwi5a+85Ye6562b6YCJ6ZyA5rGCSUTvvIjnlZnnqbrkuLrlhajph4/lj6/op4HvvIlcIlxuICAgICAgICAgID48ZWwtaW5wdXRcbiAgICAgICAgICAgIHYtbW9kZWw9XCJleHBvcnRJZHNcIlxuICAgICAgICAgICAgYXJpYS1sYWJlbD1cIuWvvOWHuuetm+mAiemcgOaxgklEXCJcbiAgICAgICAgICAgIHBsYWNlaG9sZGVyPVwi5L6L5aaCIDEwMSwxMDJcIiAvPjwvZWwtZm9ybS1pdGVtXG4gICAgICA+PC9lbC1mb3JtPlxuICAgICAgPGgzPuacrOi9ruS4i+i9veS7u+WKoTwvaDM+XG4gICAgICA8cD7ku7vliqHliJvlu7rjgIHnlJ/miJDjgIHnu5PmnpzkuqTku5jlj4rpooblj5blnYfmjInlvZPliY3mnYPpmZDmo4Dmn6XvvJvml6flnLDlnYDkuI3og73mm7/ku6Pph43mlrDpibTmnYPjgII8L3A+XG4gICAgICA8ZWwtdGFibGUgOmRhdGE9XCJqb2JzXCIgcm93LWtleT1cImlkXCJcbiAgICAgICAgPjxlbC10YWJsZS1jb2x1bW4gcHJvcD1cImlkXCIgbGFiZWw9XCLku7vliqFJRFwiIC8+PGVsLXRhYmxlLWNvbHVtblxuICAgICAgICAgIHByb3A9XCJyZWxlYXNlSWRcIlxuICAgICAgICAgIGxhYmVsPVwi5b+r54WnSURcIlxuICAgICAgICAvPjxlbC10YWJsZS1jb2x1bW4gcHJvcD1cInN0YXRlXCIgbGFiZWw9XCLnirbmgIFcIiAvPjxlbC10YWJsZS1jb2x1bW5cbiAgICAgICAgICBwcm9wPVwibGFiZWxcIlxuICAgICAgICAgIGxhYmVsPVwi5paH5Lu26IyD5Zu0XCJcbiAgICAgICAgLz48ZWwtdGFibGUtY29sdW1uIHByb3A9XCJlcnJvclwiIGxhYmVsPVwi5o+Q56S6XCIgLz48ZWwtdGFibGUtY29sdW1uIGxhYmVsPVwi5pON5L2cXCIgbWluLXdpZHRoPVwiMjEwXCJcbiAgICAgICAgICA+PHRlbXBsYXRlICNkZWZhdWx0PVwieyByb3cgfVwiXG4gICAgICAgICAgICA+PGVsLWJ1dHRvbiBAY2xpY2s9XCJyZWZyZXNoSm9iKHJvdylcIj7liLfmlrDkuIvovb3ku7vliqE8L2VsLWJ1dHRvblxuICAgICAgICAgICAgPjxlbC1idXR0b24gdi1pZj1cInJvdy5zdGF0ZSA9PT0gJ1JFQURZJ1wiIEBjbGljaz1cImNsYWltKHJvdylcIlxuICAgICAgICAgICAgICA+6aKG5Y+WWExTWDwvZWwtYnV0dG9uXG4gICAgICAgICAgICA+PC90ZW1wbGF0ZVxuICAgICAgICAgID48L2VsLXRhYmxlLWNvbHVtblxuICAgICAgICA+PC9lbC10YWJsZVxuICAgICAgPlxuICAgIDwvc2VjdGlvbj5cbiAgICA8c2VjdGlvbiB2LWlmPVwiZnJvemVuXCIgYXJpYS1sYWJlbD1cIuWGu+e7k+WFreihqOWGheWuuVwiPlxuICAgICAgPGgyPuW/q+eFp3t7IGZyb3plbi5yZWxlYXNlSWQgfX0gwrcge3sgZnJvemVuLmxhYmVsIH19PC9oMj5cbiAgICAgIDxlbC10YWJzIHYtbW9kZWw9XCJzZWxlY3RlZFNoZWV0XCJcbiAgICAgICAgPjxlbC10YWItcGFuZVxuICAgICAgICAgIHYtZm9yPVwiKHJvd3MsIG5hbWUpIGluIGZyb3plbi5zaGVldHNcIlxuICAgICAgICAgIDprZXk9XCJuYW1lXCJcbiAgICAgICAgICA6bGFiZWw9XCJTdHJpbmcobmFtZSlcIlxuICAgICAgICAgIDpuYW1lPVwiU3RyaW5nKG5hbWUpXCJcbiAgICAgICAgPlxuICAgICAgICAgIDxkaXYgY2xhc3M9XCJ3aWRlLXRhYmxlXCIgdGFiaW5kZXg9XCIwXCJcbiAgICAgICAgICAgID48ZWwtdGFibGUgOmRhdGE9XCJyb3dzXCJcbiAgICAgICAgICAgICAgPjxlbC10YWJsZS1jb2x1bW4gcHJvcD1cIuiusOW9leexu+Wei1wiIGxhYmVsPVwi6K6w5b2V57G75Z6LXCIgd2lkdGg9XCIxNDBcIiAvPjxlbC10YWJsZS1jb2x1bW5cbiAgICAgICAgICAgICAgICBwcm9wPVwi6K6w5b2V5Y+3XCJcbiAgICAgICAgICAgICAgICBsYWJlbD1cIuW/q+eFp+WFs+iBlOWPt1wiXG4gICAgICAgICAgICAgICAgd2lkdGg9XCIxODBcIlxuICAgICAgICAgICAgICAvPjxlbC10YWJsZS1jb2x1bW4gbGFiZWw9XCLlhoXlrrlcIiBtaW4td2lkdGg9XCI1MDBcIlxuICAgICAgICAgICAgICAgID48dGVtcGxhdGUgI2RlZmF1bHQ9XCJ7IHJvdyB9XCJcbiAgICAgICAgICAgICAgICAgID48ZWwtYnV0dG9uIEBjbGljaz1cInNob3dSZWNvcmQocm93KVwiPuWxleW8gOWGu+e7k+iusOW9lTwvZWwtYnV0dG9uXG4gICAgICAgICAgICAgICAgICA+PHNwYW4+e3tcbiAgICAgICAgICAgICAgICAgICAgcm93Wyflip/og70nXSB8fCByb3dbJ+WQjeensCddIHx8IHJvd1sn5a6M5pW05oCnJ10gfHwgcm93Wyfor7TmmI4nXSB8fCByb3dbJ+e8luWPtyddIHx8ICcnXG4gICAgICAgICAgICAgICAgICB9fTwvc3Bhbj48L3RlbXBsYXRlXG4gICAgICAgICAgICAgICAgPjwvZWwtdGFibGUtY29sdW1uXG4gICAgICAgICAgICAgID48L2VsLXRhYmxlXG4gICAgICAgICAgICA+PC9kaXZcbiAgICAgICAgICA+XG4gICAgICAgIDwvZWwtdGFiLXBhbmU+PC9lbC10YWJzXG4gICAgICA+XG4gICAgPC9zZWN0aW9uPlxuICAgIDxlbC1kaWFsb2dcbiAgICAgIHYtbW9kZWw9XCJyb290T3BlblwiXG4gICAgICB0aXRsZT1cIueZu+iusOaOiOadg+agueadkOaWmVwiXG4gICAgICB3aWR0aD1cIm1pbig5MDBweCwgOTR2dylcIlxuICAgICAgY2xhc3M9XCJnb3Zlcm5hbmNlLWRpYWxvZ1wiXG4gICAgICA6Y2xvc2Utb24tY2xpY2stbW9kYWw9XCJmYWxzZVwiXG4gICAgPlxuICAgICAgPGVsLWZvcm0gbGFiZWwtcG9zaXRpb249XCJ0b3BcIj5cbiAgICAgICAgPGVsLWZvcm0taXRlbSBsYWJlbD1cIueLrOeri+eZu+iusOadkOaWmVwiXG4gICAgICAgICAgPjxlbC1zZWxlY3Qgdi1tb2RlbD1cInJvb3RGb3JtLmFuY2hvcklkXCIgYXJpYS1sYWJlbD1cIueLrOeri+eZu+iusOadkOaWmVwiXG4gICAgICAgICAgICA+PGVsLW9wdGlvblxuICAgICAgICAgICAgICB2LWZvcj1cImEgaW4gYW5jaG9yc1wiXG4gICAgICAgICAgICAgIDprZXk9XCJhLmlkXCJcbiAgICAgICAgICAgICAgOmxhYmVsPVwiYCR7YS5pZH0gwrcgJHthLmRhdGEubW9kZX0gwrcgJHthLmRhdGEubWF0ZXJpYWxWZXJzaW9ufWBcIlxuICAgICAgICAgICAgICA6dmFsdWU9XCJhLmlkXCIgLz48L2VsLXNlbGVjdFxuICAgICAgICA+PC9lbC1mb3JtLWl0ZW0+XG4gICAgICAgIDxlbC1mb3JtLWl0ZW0gbGFiZWw9XCLooqvmjojmnYPkurpJRFwiXG4gICAgICAgICAgPjxlbC1pbnB1dC1udW1iZXIgdi1tb2RlbD1cInJvb3RGb3JtLnVzZXJJZFwiIGFyaWEtbGFiZWw9XCLooqvmjojmnYPkurpJRFwiIDptaW49XCIxXCJcbiAgICAgICAgLz48L2VsLWZvcm0taXRlbT5cbiAgICAgICAgPGVsLWZvcm0taXRlbSBsYWJlbD1cIuiuuOWPr+WKqOS9nFwiXG4gICAgICAgICAgPjxlbC1jaGVja2JveC1ncm91cCB2LW1vZGVsPVwicm9vdEZvcm0uYWN0aW9uc1wiIGFyaWEtbGFiZWw9XCLorrjlj6/liqjkvZxcIlxuICAgICAgICAgICAgPjxlbC1jaGVja2JveCB2LWZvcj1cIihuYW1lLCBrZXkpIGluIGFjdGlvbkxhYmVsc1wiIDprZXk9XCJrZXlcIiA6bGFiZWw9XCJrZXlcIj57e1xuICAgICAgICAgICAgICBuYW1lXG4gICAgICAgICAgICB9fTwvZWwtY2hlY2tib3g+PC9lbC1jaGVja2JveC1ncm91cFxuICAgICAgICAgID48L2VsLWZvcm0taXRlbVxuICAgICAgICA+XG4gICAgICAgIDxlbC1mb3JtLWl0ZW0gbGFiZWw9XCLlj6/nu6fnu63lp5TmtL7liqjkvZxcIlxuICAgICAgICAgID48ZWwtc2VsZWN0IHYtbW9kZWw9XCJyb290Rm9ybS5kZWxlZ2FibGVBY3Rpb25zXCIgbXVsdGlwbGUgYXJpYS1sYWJlbD1cIuWPr+e7p+e7reWnlOa0vuWKqOS9nFwiXG4gICAgICAgICAgICA+PGVsLW9wdGlvblxuICAgICAgICAgICAgICB2LWZvcj1cImtleSBpbiByb290Rm9ybS5hY3Rpb25zXCJcbiAgICAgICAgICAgICAgOmtleT1cImtleVwiXG4gICAgICAgICAgICAgIDpsYWJlbD1cImFjdGlvbkxhYmVsc1trZXldXCJcbiAgICAgICAgICAgICAgOnZhbHVlPVwia2V5XCIgLz48L2VsLXNlbGVjdFxuICAgICAgICA+PC9lbC1mb3JtLWl0ZW0+XG4gICAgICAgIDxlbC1mb3JtLWl0ZW0gdi1mb3I9XCJmIGluIHJvb3RGaWVsZHNcIiA6a2V5PVwiZi5rZXlcIiA6bGFiZWw9XCJmLmxhYmVsXCJcbiAgICAgICAgICA+PGVsLWlucHV0XG4gICAgICAgICAgICB2LW1vZGVsPVwicm9vdEZvcm1bZi5rZXldXCJcbiAgICAgICAgICAgIDp0eXBlPVwiZi5sb25nID8gJ3RleHRhcmVhJyA6ICd0ZXh0J1wiXG4gICAgICAgICAgICA6cm93cz1cImYubG9uZyA/IDUgOiB1bmRlZmluZWRcIlxuICAgICAgICAgICAgOmFyaWEtbGFiZWw9XCJmLmxhYmVsXCJcbiAgICAgICAgLz48L2VsLWZvcm0taXRlbT4gPC9lbC1mb3JtXG4gICAgICA+PHRlbXBsYXRlICNmb290ZXJcbiAgICAgICAgPjxlbC1idXR0b24gQGNsaWNrPVwicm9vdE9wZW4gPSBmYWxzZVwiPuWPlua2iOeZu+iusDwvZWwtYnV0dG9uXG4gICAgICAgID48ZWwtYnV0dG9uIDpsb2FkaW5nPVwiYnVzeVwiIEBjbGljaz1cInNhdmVSb290XCI+5L+d5a2Y5b6F562+572y5p2Q5paZPC9lbC1idXR0b24+PC90ZW1wbGF0ZVxuICAgICAgPlxuICAgIDwvZWwtZGlhbG9nPlxuICAgIDxlbC1kaWFsb2dcbiAgICAgIHYtbW9kZWw9XCJkZWNpc2lvbk9wZW5cIlxuICAgICAgdGl0bGU9XCLotbfojYnkuI3lj6/lj5jlhrPorq7niYjmnKxcIlxuICAgICAgd2lkdGg9XCJtaW4oOTUwcHgsIDk0dncpXCJcbiAgICAgIGNsYXNzPVwiZ292ZXJuYW5jZS1kaWFsb2dcIlxuICAgICAgOmNsb3NlLW9uLWNsaWNrLW1vZGFsPVwiZmFsc2VcIlxuICAgID5cbiAgICAgIDxwXG4gICAgICAgID7lvZPliY3pobnnm657eyBwcm9qZWN0SWQgfX3vvIzojIPlm7TniYjmnKx7e1xuICAgICAgICAgIGRlY2lzaW9uRm9ybS5wcm9qZWN0U2NvcGVWZXJzaW9uXG4gICAgICAgIH1944CC5L+d5a2Y5Li65paw54mI5pys77yM5LiN6KaG55uW5pen5q2j5paH5oiW562+572y44CCPC9wXG4gICAgICA+XG4gICAgICA8ZWwtZm9ybSBsYWJlbC1wb3NpdGlvbj1cInRvcFwiPlxuICAgICAgICA8ZWwtZm9ybS1pdGVtIGxhYmVsPVwi5Yaz6K6u5qCH6aKYXCJcbiAgICAgICAgICA+PGVsLWlucHV0IHYtbW9kZWw9XCJkZWNpc2lvbkZvcm0udGl0bGVcIiBhcmlhLWxhYmVsPVwi5Yaz6K6u5qCH6aKYXCJcbiAgICAgICAgLz48L2VsLWZvcm0taXRlbT5cbiAgICAgICAgPGVsLWZvcm0taXRlbSBsYWJlbD1cIuimhueblumXrumimFwiXG4gICAgICAgICAgPjxlbC1jaGVja2JveC1ncm91cCB2LW1vZGVsPVwiZGVjaXNpb25Gb3JtLnF1ZXN0aW9uc1wiIGFyaWEtbGFiZWw9XCLopobnm5bpl67pophcIlxuICAgICAgICAgICAgPjxlbC1jaGVja2JveCB2LWZvcj1cInEgaW4gcXVlc3Rpb25zXCIgOmtleT1cInFcIiA6bGFiZWw9XCJxXCI+e3tcbiAgICAgICAgICAgICAgcVxuICAgICAgICAgICAgfX08L2VsLWNoZWNrYm94PjwvZWwtY2hlY2tib3gtZ3JvdXBcbiAgICAgICAgICA+PC9lbC1mb3JtLWl0ZW1cbiAgICAgICAgPlxuICAgICAgICA8ZWwtZm9ybS1pdGVtIHYtZm9yPVwiZiBpbiBkZWNpc2lvbkZpZWxkc1wiIDprZXk9XCJmLmtleVwiIDpsYWJlbD1cImYubGFiZWxcIlxuICAgICAgICAgID48ZWwtaW5wdXRcbiAgICAgICAgICAgIHYtbW9kZWw9XCJkZWNpc2lvbkZvcm1bZi5rZXldXCJcbiAgICAgICAgICAgIDp0eXBlPVwiZi5sb25nID8gJ3RleHRhcmVhJyA6ICd0ZXh0J1wiXG4gICAgICAgICAgICA6cm93cz1cImYubG9uZyA/IDYgOiB1bmRlZmluZWRcIlxuICAgICAgICAgICAgOmFyaWEtbGFiZWw9XCJmLmxhYmVsXCJcbiAgICAgICAgLz48L2VsLWZvcm0taXRlbT5cbiAgICAgICAgPGVsLWZvcm0taXRlbSBsYWJlbD1cIuaMh+WumueLrOeri+aJueWHhuS6uklEXCJcbiAgICAgICAgICA+PGVsLWlucHV0LW51bWJlclxuICAgICAgICAgICAgdi1tb2RlbD1cImRlY2lzaW9uRm9ybS5hcHByb3ZlcklkXCJcbiAgICAgICAgICAgIDptaW49XCIxXCJcbiAgICAgICAgICAgIGFyaWEtbGFiZWw9XCLmjIflrprni6znq4vmibnlh4bkurpJRFwiXG4gICAgICAgIC8+PC9lbC1mb3JtLWl0ZW0+XG4gICAgICAgIDxlbC1mb3JtLWl0ZW0gbGFiZWw9XCLlhbbku5bmnKrlhrPpl67popjliIbnsbtcIlxuICAgICAgICAgID48ZGl2IHYtZm9yPVwicSBpbiBvdGhlclF1ZXN0aW9uc1wiIDprZXk9XCJxXCJcbiAgICAgICAgICAgID48bGFiZWw+e3sgcSB9fTwvbGFiZWxcbiAgICAgICAgICAgID48ZWwtc2VsZWN0IHYtbW9kZWw9XCJkZWNpc2lvbkZvcm0ub3RoZXJRdWVzdGlvbnNbcV1cIiA6YXJpYS1sYWJlbD1cImAke3F95YiG57G7YFwiXG4gICAgICAgICAgICAgID48ZWwtb3B0aW9uIGxhYmVsPVwi5Z+657q/6Zi75patXCIgdmFsdWU9XCJCQVNFTElORV9CTE9DS1wiIC8+PGVsLW9wdGlvblxuICAgICAgICAgICAgICAgIGxhYmVsPVwi5pWw5o2u5YeG5aSHXCJcbiAgICAgICAgICAgICAgICB2YWx1ZT1cIkRBVEFfUkVBRElORVNTXCIgLz48ZWwtb3B0aW9uXG4gICAgICAgICAgICAgICAgbGFiZWw9XCLlu7rorq5cIlxuICAgICAgICAgICAgICAgIHZhbHVlPVwiQURWSVNPUllcIiAvPjwvZWwtc2VsZWN0PjwvZGl2XG4gICAgICAgID48L2VsLWZvcm0taXRlbT5cbiAgICAgICAgPGVsLWNoZWNrYm94IHYtbW9kZWw9XCJkZWNpc2lvbkZvcm0uc2VtYW50aWNDaGFuZ2VcIlxuICAgICAgICAgID7mnKzniYjmnKzmlLnlj5jkuJrliqHor63kuYnmiJbpqozmlLbmnaHku7Y8L2VsLWNoZWNrYm94XG4gICAgICAgID4gPC9lbC1mb3JtXG4gICAgICA+PHRlbXBsYXRlICNmb290ZXJcbiAgICAgICAgPjxlbC1idXR0b24gQGNsaWNrPVwiZGVjaXNpb25PcGVuID0gZmFsc2VcIj7lj5bmtojotbfojYk8L2VsLWJ1dHRvblxuICAgICAgICA+PGVsLWJ1dHRvbiA6bG9hZGluZz1cImJ1c3lcIiBAY2xpY2s9XCJzYXZlRGVjaXNpb25cIj7kv53lrZjlhrPorq7ojYnnqL88L2VsLWJ1dHRvbj48L3RlbXBsYXRlXG4gICAgICA+XG4gICAgPC9lbC1kaWFsb2c+XG4gICAgPGVsLWRpYWxvZ1xuICAgICAgdi1tb2RlbD1cImFjdGlvbk9wZW5cIlxuICAgICAgdGl0bGU9XCLorrDlvZXmnKzkurrliqjkvZzkuI7kvp3mja5cIlxuICAgICAgd2lkdGg9XCJtaW4oNzAwcHgsIDk0dncpXCJcbiAgICAgIGNsYXNzPVwiZ292ZXJuYW5jZS1kaWFsb2dcIlxuICAgICAgOmNsb3NlLW9uLWNsaWNrLW1vZGFsPVwiZmFsc2VcIlxuICAgICAgPjxwXG4gICAgICAgID57eyBhY3Rpb25Gb3JtLmtpbmQgfX0gwrcg6K6w5b2Ve3sgYWN0aW9uRm9ybS5pZCB9fSDCtyB7eyBhY3Rpb25Gb3JtLmFjdGlvbiB9fSDCtyDniYjmnKx7e1xuICAgICAgICAgIGFjdGlvbkZvcm0udmVyc2lvblxuICAgICAgICB9fTwvcFxuICAgICAgPjxlbC1pbnB1dFxuICAgICAgICB2LW1vZGVsPVwiYWN0aW9uRm9ybS5yZWFzb25cIlxuICAgICAgICB0eXBlPVwidGV4dGFyZWFcIlxuICAgICAgICA6cm93cz1cIjZcIlxuICAgICAgICBhcmlhLWxhYmVsPVwi5pys5Lq65Yqo5L2c5L6d5o2uXCJcbiAgICAgIC8+PHRlbXBsYXRlICNmb290ZXJcbiAgICAgICAgPjxlbC1idXR0b24gQGNsaWNrPVwiYWN0aW9uT3BlbiA9IGZhbHNlXCI+5Y+W5raI5pys5Lq65Yqo5L2cPC9lbC1idXR0b25cbiAgICAgICAgPjxlbC1idXR0b24gOmxvYWRpbmc9XCJidXN5XCIgOmRpc2FibGVkPVwiIWFjdGlvbkZvcm0ucmVhc29uLnRyaW0oKVwiIEBjbGljaz1cInNhdmVBY3Rpb25cIlxuICAgICAgICAgID7mj5DkuqTmnKzkurrliqjkvZw8L2VsLWJ1dHRvblxuICAgICAgICA+PC90ZW1wbGF0ZVxuICAgICAgPjwvZWwtZGlhbG9nXG4gICAgPlxuICAgIDxlbC1kaWFsb2dcbiAgICAgIHYtbW9kZWw9XCJyZWNvcmRPcGVuXCJcbiAgICAgIHRpdGxlPVwi5Ya757uT6K6w5b2V5LiO5Y+X5o6n5q2j5paHXCJcbiAgICAgIHdpZHRoPVwibWluKDExMDBweCwgOTR2dylcIlxuICAgICAgY2xhc3M9XCJnb3Zlcm5hbmNlLWRpYWxvZ1wiXG4gICAgPlxuICAgICAgPHByZSBjbGFzcz1cInJlY29yZFwiPnt7IHJlY29yZFRleHQgfX08L3ByZVxuICAgICAgPjx0ZW1wbGF0ZSAjZm9vdGVyXG4gICAgICAgID48ZWwtYnV0dG9uIEBjbGljaz1cInJlY29yZE9wZW4gPSBmYWxzZVwiPuWFs+mXreiusOW9lTwvZWwtYnV0dG9uPjwvdGVtcGxhdGVcbiAgICAgID48L2VsLWRpYWxvZ1xuICAgID5cbiAgPC9Db250ZW50V3JhcD5cbjwvdGVtcGxhdGU+XG5cbjxzY3JpcHQgc2V0dXAgbGFuZz1cInRzXCI+XG5pbXBvcnQgKiBhcyBhcGkgZnJvbSAnQC9hcGkvaHJtL3BheXJvbGwvZ292ZXJuYW5jZSdcbmltcG9ydCB7IGxpc3RSZXF1aXJlbWVudFByb2plY3RzLCByZXF1aXJlbWVudENhcGFiaWxpdGllcyB9IGZyb20gJ0AvYXBpL2hybS9wYXlyb2xsL3JlcXVpcmVtZW50J1xuaW1wb3J0IGRvd25sb2FkIGZyb20gJ0AvdXRpbHMvZG93bmxvYWQnXG5pbXBvcnQgeyBjaGVja1Blcm1pIH0gZnJvbSAnQC91dGlscy9wZXJtaXNzaW9uJ1xuY29uc3QgY2FuUGxhdGZvcm0gPSAoYWN0aW9uOiBzdHJpbmcpID0+IGNoZWNrUGVybWkoWydocm06cGF5cm9sbDpyZXF1aXJlbWVudDonICsgYWN0aW9uXSlcbmNvbnN0IHJvdXRlciA9IHVzZVJvdXRlcigpLFxuICByb3V0ZSA9IHVzZVJvdXRlKClcbmNvbnN0IHByb2plY3RJZCA9IGNvbXB1dGVkKCgpID0+XG4gIHJvdXRlLnF1ZXJ5LnByb2plY3RJZCA9PSBudWxsID8gdW5kZWZpbmVkIDogTnVtYmVyKHJvdXRlLnF1ZXJ5LnByb2plY3RJZClcbilcbmNvbnN0IHByb2plY3RzID0gcmVmPGFueVtdPihbXSksXG4gIGFuY2hvcnMgPSByZWY8YW55W10+KFtdKSxcbiAgcm9vdHMgPSByZWY8YW55W10+KFtdKSxcbiAgZGVjaXNpb25zID0gcmVmPGFueVtdPihbXSksXG4gIHJlbGVhc2VzID0gcmVmPGFueVtdPihbXSlcbmNvbnN0IGNhcHMgPSByZWY8YW55Pih7fSksXG4gIGdhdGUgPSByZWY8YW55PigpLFxuICBmcm96ZW4gPSByZWY8YW55PigpLFxuICBqb2JzID0gcmVmPGFueVtdPihbXSlcbmNvbnN0IHByb2plY3QgPSBjb21wdXRlZCgoKSA9PiBwcm9qZWN0cy52YWx1ZS5maW5kKChwKSA9PiBwLmlkID09PSBwcm9qZWN0SWQudmFsdWUpKVxuY29uc3QgbG9hZGluZyA9IHJlZihmYWxzZSksXG4gIGJ1c3kgPSByZWYoZmFsc2UpLFxuICBlcnJvciA9IHJlZignJyksXG4gIHN1Y2Nlc3MgPSByZWYoJycpLFxuICBwdWJsaXNoUmVhc29uID0gcmVmKCcnKSxcbiAgcmVxdWVzdEtleSA9IHJlZihjcnlwdG8ucmFuZG9tVVVJRCgpKSxcbiAgZXhwb3J0SWRzID0gcmVmKCcnKSxcbiAgbGFzdFB1Ymxpc2ggPSByZWY8YW55PigpXG5jb25zdCByb290T3BlbiA9IHJlZihmYWxzZSksXG4gIGRlY2lzaW9uT3BlbiA9IHJlZihmYWxzZSksXG4gIGFjdGlvbk9wZW4gPSByZWYoZmFsc2UpLFxuICByZWNvcmRPcGVuID0gcmVmKGZhbHNlKSxcbiAgcmVjb3JkVGV4dCA9IHJlZignJyksXG4gIHNlbGVjdGVkU2hlZXQgPSByZWYoJ+ivhOWuoeeJiOacrOivtOaYjicpXG5jb25zdCByb290Rm9ybSA9IHJlZjxhbnk+KHt9KSxcbiAgZGVjaXNpb25Gb3JtID0gcmVmPGFueT4oe30pLFxuICBhY3Rpb25Gb3JtID0gcmVmPGFueT4oeyByZWFzb246ICcnIH0pXG5jb25zdCBxdWVzdGlvbnMgPSBBcnJheS5mcm9tKHsgbGVuZ3RoOiAxMCB9LCAoXywgbikgPT4gYFEtJHtTdHJpbmcobiArIDEpLnBhZFN0YXJ0KDIsICcwJyl9YCksXG4gIG90aGVyUXVlc3Rpb25zID0gWydRLTAzJywgJ1EtMDYnLCAnUS0wOCcsICdRLTA5J11cbmNvbnN0IGFjdGlvbkxhYmVsczogUmVjb3JkPHN0cmluZywgc3RyaW5nPiA9IHtcbiAgUkVBRDogJ+ivu+WPlumhueebricsXG4gIE1FVEE6ICfkv67mlLnpobnnm67lkI3np7AnLFxuICBFRElUOiAn57yW6L6R6ZyA5rGCJyxcbiAgQVNTSUdOOiAn5YiG5rS+6K+E5a6hJyxcbiAgQ09ORklSTTogJ+acrOS6uuehruiupOmcgOaxgicsXG4gIEVYUE9SVDogJ+WvvOWHuicsXG4gIE1FTUJFUjogJ+euoeeQhuaIkOWRmCcsXG4gIFNDT1BFOiAn5L+u6K6i6aG555uu6IyD5Zu0JyxcbiAgU0NPUEVfREVDSURFOiAn6ZyA5rGC6IyD5Zu05Y+W6IiNJyxcbiAgU09VUkNFX0VESVQ6ICfnu7TmiqTmnaXmupAnLFxuICBTT1VSQ0VfVkVSSUZZOiAn5qC46aqM5p2l5rqQ6LSo6YePJyxcbiAgSVNTVUVfTUFOQUdFOiAn566h55CG6Zeu6aKYJyxcbiAgSVNTVUVfSEFORExFOiAn5aSE55CG6Zeu6aKYJyxcbiAgSVNTVUVfUkVWSUVXOiAn54us56uL5aSN5qC46Zeu6aKYJyxcbiAgREVDSVNJT05fUkVBRDogJ+ivu+WPluWGs+iuruato+aWhycsXG4gIEdSQU5UX1JFQUQ6ICfor7vlj5bmjojmnYPkvp3mja4nLFxuICBQRVJTT05fUkVBRDogJ+ivu+WPluWFs+iBlOS6uuWRmOaYvuekuuWQjScsXG4gIERFQ0lTSU9OX0VESVQ6ICfotbfojYnlhrPorq4nLFxuICBERUNJU0lPTl9BUFBST1ZFOiAn54us56uL5om55YeG5Yaz6K6uJyxcbiAgQkFTRUxJTkVfUFVCTElTSDogJ+WPkeW4g+Wfuue6vycsXG4gIFBST0pFQ1RfQ1JFQVRFOiAn5Yib5bu66aG555uuJ1xufVxuY29uc3Qgcm9vdEZpZWxkcyA9IFtcbiAgeyBrZXk6ICdzY29wZUpzb24nLCBsYWJlbDogJ+aOiOadg+iMg+WbtOato+aWhycsIGxvbmc6IHRydWUgfSxcbiAgeyBrZXk6ICd2YWxpZEZyb20nLCBsYWJlbDogJ+aOiOadg+W8gOWni+aXpeacnycgfSxcbiAgeyBrZXk6ICd2YWxpZFRvJywgbGFiZWw6ICfmjojmnYPnu5PmnZ/ml6XmnJ8nIH0sXG4gIHsga2V5OiAnbWF0ZXJpYWxWZXJzaW9uJywgbGFiZWw6ICflj5fmjqfmnZDmlpnniYjmnKwnIH0sXG4gIHsga2V5OiAnYm9keScsIGxhYmVsOiAn5Y+X5o6n562+572y5p2Q5paZ5q2j5paHJywgbG9uZzogdHJ1ZSB9LFxuICB7IGtleTogJ3JlYXNvbicsIGxhYmVsOiAn55m76K6w5L6d5o2uJywgbG9uZzogdHJ1ZSB9XG5dXG5jb25zdCBkZWNpc2lvbkZpZWxkcyA9IFtcbiAgeyBrZXk6ICdjbGF1c2VzJywgbGFiZWw6ICfopobnm5bmnaHmrL4nLCBsb25nOiB0cnVlIH0sXG4gIHsga2V5OiAnY2xhdXNlVmVyc2lvbicsIGxhYmVsOiAn5p2h5qy+5byV55So54mI5pysJyB9LFxuICB7IGtleTogJ2JvZHknLCBsYWJlbDogJ+WGs+iuruWPl+aOp+ato+aWhycsIGxvbmc6IHRydWUgfSxcbiAgeyBrZXk6ICdzY29wZUpzb24nLCBsYWJlbDogJ+WGs+iurumAgueUqOiMg+WbtOato+aWhycsIGxvbmc6IHRydWUgfSxcbiAgeyBrZXk6ICd2YWxpZEZyb20nLCBsYWJlbDogJ+WGs+iuruW8gOWni+aXpeacnycgfSxcbiAgeyBrZXk6ICd2YWxpZFRvJywgbGFiZWw6ICflhrPorq7nu5PmnZ/ml6XmnJ8nIH0sXG4gIHsga2V5OiAnYXBwcm92YWxDb25kaXRpb25zJywgbGFiZWw6ICfmibnlh4bmnaHku7YnLCBsb25nOiB0cnVlIH0sXG4gIHsga2V5OiAncmVhc29uJywgbGFiZWw6ICfotbfojYnmiJbkv67orqLkvp3mja4nLCBsb25nOiB0cnVlIH1cbl1cbmNvbnN0IGJsb2NrTGFiZWxzOiBSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+ID0ge1xuICBQUk9KRUNUX0FSQ0hJVkVEOiAn6aG555uu5bey5b2S5qGjJyxcbiAgRlVMTF9DTE9TVVJFX1JFQURfUkVRVUlSRUQ6ICfnvLrlsJHlrozmlbTlv6vnhafpl63ljIXor7vmnYMnLFxuICBCQVNFTElORV9QVUJMSVNIX1JFUVVJUkVEOiAn57y65bCR54us56uL5Y+R5biD5p2D6ZmQJyxcbiAgQ1VSUkVOVF9DT05UUk9MTEVEX1BVQkxJU0hfQVVUSE9SSVRZX1JFUVVJUkVEOiAn57y65bCR5b2T5YmN5pyJ5pWI55qE5Y+X5o6n5Y+R5biD5o6I5p2DJyxcbiAgQ1VSUkVOVF9ERUNJU0lPTl9DT1ZFUkFHRV9SRVFVSVJFRDogJ+W9k+WJjeiMg+WbtOeahOW8uuWItuWGs+iuruimhuebluS4jeWujOaVtOaIluWGsueqgScsXG4gIFVOUkVTT0xWRURfUVVFU1RJT05fQ0xBU1NJRklDQVRJT046ICflhbbku5bmnKrlhrPpl67popjlsJrmnKrmmI7noa7liIbnsbsnLFxuICBVTlJFU09MVkVEX0JBU0VMSU5FX1FVRVNUSU9OOiAn5LuN5pyJ5Z+657q/6Zi75pat6Zeu6aKYJyxcbiAgRU1QVFlfSU5DTFVERURfU0VUOiAn5pys5pyf57qz5YWl6ZuG5ZCI5Li656m6JyxcbiAgU0NPUEVfTUlTTUFUQ0g6ICfpnIDmsYLojIPlm7TkuI7lvZPliY3pobnnm67kuI3ljLnphY0nLFxuICBDVVJSRU5UX0NPTkZJUk1BVElPTl9SRVFVSVJFRDogJ+mcgOimgeW9k+WJjeacieaViOeahOacrOS6uuehruiupCcsXG4gIENPTkZJUk1BVElPTl9HT1ZFUk5BTkNFX0ZBQ1RfUkVRVUlSRUQ6ICfml6fnoa7orqTnvLrlsJFTNeaJueWHhuWPiuaOiOadg+S6i+Wunu+8jOmcgOmHjeaWsOehruiupCcsXG4gIFNQRUNJRklDQVRJT05fTk9UX1ZFUklGSUVEOiAn5p2l5rqQ5oiW5pig5bCE6KeE5qC85bCa5pyq5qC46aqMJyxcbiAgRVhDTFVTSU9OX0JBU0lTX0lOQ09NUExFVEU6ICfmjpLpmaTnkIbnlLHjgIHotKPku7vkurrmiJbph43mlrDor4TkvLDmnaHku7bkuI3pvZDlpIcnLFxuICBEVVBMSUNBVEVfVEFSR0VUX0lOVkFMSUQ6ICfph43lpI3np7vpmaTnmoTkv53nlZnpobnml6DmlYgnLFxuICBCQVNFTElORV9JU1NVRVNfT1BFTjogJ+WtmOWcqOebuOWFs+acquWFs+mXreWfuue6v+mYu+aWremXrumimCdcbn1cbmxldCB0aWNrZXQgPSAwXG5hc3luYyBmdW5jdGlvbiBsb2FkKCkge1xuICBzdWNjZXNzLnZhbHVlID0gJydcbiAgY29uc3Qgb3duID0gKyt0aWNrZXQsXG4gICAgaWQgPSBwcm9qZWN0SWQudmFsdWVcbiAgbG9hZGluZy52YWx1ZSA9IHRydWVcbiAgZXJyb3IudmFsdWUgPSAnJ1xuICByb290cy52YWx1ZSA9IFtdXG4gIGRlY2lzaW9ucy52YWx1ZSA9IFtdXG4gIHJlbGVhc2VzLnZhbHVlID0gW11cbiAgY2Fwcy52YWx1ZSA9IHt9XG4gIGdhdGUudmFsdWUgPSB1bmRlZmluZWRcbiAgZnJvemVuLnZhbHVlID0gdW5kZWZpbmVkXG4gIHRyeSB7XG4gICAgY29uc3QgW3BzLCBhc10gPSBhd2FpdCBQcm9taXNlLmFsbChbbGlzdFJlcXVpcmVtZW50UHJvamVjdHMoKSwgYXBpLmdldCgnL2FuY2hvcnMnKV0pXG4gICAgaWYgKG93biAhPT0gdGlja2V0KSByZXR1cm5cbiAgICBwcm9qZWN0cy52YWx1ZSA9IHBzXG4gICAgYW5jaG9ycy52YWx1ZSA9IGFzXG4gICAgaWYgKGlkID09IG51bGwpIHJldHVyblxuICAgIGNvbnN0IGxvYWRlZFJvb3RzID0gYXdhaXQgYXBpLmdldCgnL3Jvb3RzJywgeyBwcm9qZWN0SWQ6IGlkIH0pXG4gICAgaWYgKG93biAhPT0gdGlja2V0KSByZXR1cm5cbiAgICByb290cy52YWx1ZSA9IGxvYWRlZFJvb3RzXG4gICAgaWYgKGlkKSB7XG4gICAgICBjb25zdCBjID0gYXdhaXQgcmVxdWlyZW1lbnRDYXBhYmlsaXRpZXMoaWQpXG4gICAgICBjb25zdCBbZHMsIHJzXSA9IGF3YWl0IFByb21pc2UuYWxsKFtcbiAgICAgICAgYy5ERUNJU0lPTl9SRUFEID8gYXBpLmdldCgnL2RlY2lzaW9ucycsIHsgcHJvamVjdElkOiBpZCB9KSA6IFtdLFxuICAgICAgICBhcGkuZ2V0KCcvcmVsZWFzZXMnLCB7IHByb2plY3RJZDogaWQgfSlcbiAgICAgIF0pXG4gICAgICBpZiAob3duICE9PSB0aWNrZXQpIHJldHVyblxuICAgICAgY2Fwcy52YWx1ZSA9IGNcbiAgICAgIGRlY2lzaW9ucy52YWx1ZSA9IGRzXG4gICAgICByZWxlYXNlcy52YWx1ZSA9IHJzXG4gICAgfVxuICB9IGNhdGNoIHtcbiAgICBpZiAob3duID09PSB0aWNrZXQpXG4gICAgICBlcnJvci52YWx1ZSA9ICfnnJ/lrp7lhrPorq7miJblj5HluIPmlbDmja7mnKrliqDovb3vvJror7fmoLjlr7nlvZPliY3mnYPpmZDmiJbph43or5XvvIzmnKrov5Tlm57lhoXlrrnkuI3mjqjlrprkuLrnqbrmiJbpgJrov4fjgIInXG4gIH0gZmluYWxseSB7XG4gICAgaWYgKG93biA9PT0gdGlja2V0KSBsb2FkaW5nLnZhbHVlID0gZmFsc2VcbiAgfVxufVxuZnVuY3Rpb24gc3dpdGNoUHJvamVjdChpZDogbnVtYmVyKSB7XG4gIGxhc3RQdWJsaXNoLnZhbHVlID0gdW5kZWZpbmVkXG4gIGpvYnMudmFsdWUgPSBbXVxuICBzdWNjZXNzLnZhbHVlID0gJydcbiAgcm9vdE9wZW4udmFsdWUgPSBkZWNpc2lvbk9wZW4udmFsdWUgPSBhY3Rpb25PcGVuLnZhbHVlID0gZmFsc2VcbiAgcm91dGVyLnJlcGxhY2UoeyBwYXRoOiByb3V0ZS5wYXRoLCBxdWVyeTogeyBwcm9qZWN0SWQ6IGlkIH0gfSlcbn1cbmZ1bmN0aW9uIGdvUmV2aWV3KCkge1xuICByb3V0ZXIucHVzaCh7IHBhdGg6ICcvaHJtLXBheXJvbGwvcmV2aWV3JywgcXVlcnk6IHsgcHJvamVjdElkOiBwcm9qZWN0SWQudmFsdWUsIG1vZHVsZTogJ0NPTCcgfSB9KVxufVxuZnVuY3Rpb24gc2hvd1JlY29yZChyb3c6IGFueSkge1xuICByZWNvcmRUZXh0LnZhbHVlID0gSlNPTi5zdHJpbmdpZnkocm93LCBudWxsLCAyKVxuICByZWNvcmRPcGVuLnZhbHVlID0gdHJ1ZVxufVxuYXN5bmMgZnVuY3Rpb24gaGlzdG9yeShyb3c6IGFueSkge1xuICBzdWNjZXNzLnZhbHVlID0gJydcbiAgc2hvd1JlY29yZChhd2FpdCBhcGkuZ2V0KCcvZGVjaXNpb24vaGlzdG9yeScsIHsgcHJvamVjdElkOiBwcm9qZWN0SWQudmFsdWUsIGlkOiByb3cuaWQgfSkpXG59XG5mdW5jdGlvbiBvcGVuUm9vdCgpIHtcbiAgc3VjY2Vzcy52YWx1ZSA9ICcnXG4gIHJvb3RGb3JtLnZhbHVlID0ge1xuICAgIGFuY2hvcklkOiBhbmNob3JzLnZhbHVlWzBdPy5pZCxcbiAgICB1c2VySWQ6IHVuZGVmaW5lZCxcbiAgICBhY3Rpb25zOiBbXSxcbiAgICBkZWxlZ2FibGVBY3Rpb25zOiBbXSxcbiAgICBzY29wZUpzb246IHByb2plY3QudmFsdWU/LnNjb3BlSnNvbiB8fCAnJyxcbiAgICB2YWxpZEZyb206ICcnLFxuICAgIHZhbGlkVG86ICcnLFxuICAgIG1hdGVyaWFsVmVyc2lvbjogJycsXG4gICAgYm9keTogJycsXG4gICAgcmVhc29uOiAnJ1xuICB9XG4gIHJvb3RPcGVuLnZhbHVlID0gdHJ1ZVxufVxuZnVuY3Rpb24gb3BlbkRlY2lzaW9uKHJvdz86IGFueSkge1xuICBzdWNjZXNzLnZhbHVlID0gJydcbiAgZGVjaXNpb25Gb3JtLnZhbHVlID0gcm93XG4gICAgPyB7XG4gICAgICAgIC4uLkpTT04ucGFyc2UoSlNPTi5zdHJpbmdpZnkocm93LmRhdGEpKSxcbiAgICAgICAgcHJldmlvdXNJZDogcm93LmlkLFxuICAgICAgICByZWFzb246ICcnLFxuICAgICAgICBwcm9qZWN0U2NvcGVWZXJzaW9uOiBwcm9qZWN0LnZhbHVlPy5wcm9qZWN0U2NvcGVWZXJzaW9uXG4gICAgICB9XG4gICAgOiB7XG4gICAgICAgIHByb2plY3RTY29wZVZlcnNpb246IHByb2plY3QudmFsdWU/LnByb2plY3RTY29wZVZlcnNpb24sXG4gICAgICAgIHRpdGxlOiAnJyxcbiAgICAgICAgcXVlc3Rpb25zOiBbXSxcbiAgICAgICAgY2xhdXNlczogJycsXG4gICAgICAgIGNsYXVzZVZlcnNpb246ICcnLFxuICAgICAgICBib2R5OiAnJyxcbiAgICAgICAgc2NvcGVKc29uOiBwcm9qZWN0LnZhbHVlPy5zY29wZUpzb24gfHwgJycsXG4gICAgICAgIHZhbGlkRnJvbTogJycsXG4gICAgICAgIHZhbGlkVG86ICcnLFxuICAgICAgICBhcHByb3ZlcklkOiB1bmRlZmluZWQsXG4gICAgICAgIGFwcHJvdmFsQ29uZGl0aW9uczogJycsXG4gICAgICAgIHNlbWFudGljQ2hhbmdlOiBmYWxzZSxcbiAgICAgICAgb3RoZXJRdWVzdGlvbnM6IHt9LFxuICAgICAgICByZWFzb246ICcnXG4gICAgICB9XG4gIGRlY2lzaW9uT3Blbi52YWx1ZSA9IHRydWVcbn1cbmFzeW5jIGZ1bmN0aW9uIHNhdmVSb290KCkge1xuICBzdWNjZXNzLnZhbHVlID0gJydcbiAgYnVzeS52YWx1ZSA9IHRydWVcbiAgZXJyb3IudmFsdWUgPSAnJ1xuICB0cnkge1xuICAgIGNvbnN0IGlkID0gYXdhaXQgYXBpLnBvc3QoJy9yb290L3JlZ2lzdGVyJywgeyAuLi5yb290Rm9ybS52YWx1ZSwgcHJvamVjdElkOiBwcm9qZWN0SWQudmFsdWUgfSlcbiAgICByb290T3Blbi52YWx1ZSA9IGZhbHNlXG4gICAgYXdhaXQgbG9hZCgpXG4gICAgc3VjY2Vzcy52YWx1ZSA9IGDlt7LnmbvorrDlvoXnrb7nvbLmjojmnYPmoLkke2lkfe+8jOWwmuacqueUn+aViGBcbiAgfSBjYXRjaCB7XG4gICAgZXJyb3IudmFsdWUgPSAn55m76K6w5pyq5a6M5oiQ77yb5b2T5YmN6L6T5YWl5bey5L+d55WZ77yM6K+35qC45a+554us56uL5p2Q5paZ44CB5Lq65ZGY44CB5Yqo5L2c5ZKM6IyD5Zu044CCJ1xuICB9IGZpbmFsbHkge1xuICAgIGJ1c3kudmFsdWUgPSBmYWxzZVxuICB9XG59XG5hc3luYyBmdW5jdGlvbiBzYXZlRGVjaXNpb24oKSB7XG4gIHN1Y2Nlc3MudmFsdWUgPSAnJ1xuICBidXN5LnZhbHVlID0gdHJ1ZVxuICBlcnJvci52YWx1ZSA9ICcnXG4gIHRyeSB7XG4gICAgY29uc3QgaWQgPSBhd2FpdCBhcGkucG9zdCgnL2RlY2lzaW9uL2RyYWZ0Jywge1xuICAgICAgLi4uZGVjaXNpb25Gb3JtLnZhbHVlLFxuICAgICAgcHJvamVjdElkOiBwcm9qZWN0SWQudmFsdWVcbiAgICB9KVxuICAgIGRlY2lzaW9uT3Blbi52YWx1ZSA9IGZhbHNlXG4gICAgYXdhaXQgbG9hZCgpXG4gICAgc3VjY2Vzcy52YWx1ZSA9IGDlt7Lkv53lrZjlhrPorq7ojYnnqL/orrDlvZUke2lkfe+8jOWwmuacquaJueWHhmBcbiAgfSBjYXRjaCB7XG4gICAgZXJyb3IudmFsdWUgPSAn5Yaz6K6u6I2J56i/5pyq5L+d5a2Y77yM6L6T5YWl5bey5L+d55WZ77yb6K+35qC45a+55b+F5aGr6aG544CB5b2T5YmN6IyD5Zu05ZKM54us56uL5o6I5p2D44CCJ1xuICB9IGZpbmFsbHkge1xuICAgIGJ1c3kudmFsdWUgPSBmYWxzZVxuICB9XG59XG5mdW5jdGlvbiBvcGVuQWN0aW9uKGtpbmQ6IHN0cmluZywgcm93OiBhbnksIGFjdGlvbjogc3RyaW5nKSB7XG4gIHN1Y2Nlc3MudmFsdWUgPSAnJ1xuICBhY3Rpb25Gb3JtLnZhbHVlID0geyBraW5kLCBpZDogcm93LmlkLCB2ZXJzaW9uOiByb3cudmVyc2lvbiwgYWN0aW9uLCByZWFzb246ICcnIH1cbiAgYWN0aW9uT3Blbi52YWx1ZSA9IHRydWVcbn1cbmFzeW5jIGZ1bmN0aW9uIHNhdmVBY3Rpb24oKSB7XG4gIHN1Y2Nlc3MudmFsdWUgPSAnJ1xuICBidXN5LnZhbHVlID0gdHJ1ZVxuICBlcnJvci52YWx1ZSA9ICcnXG4gIHRyeSB7XG4gICAgYXdhaXQgYXBpLnBvc3QoYC8ke2FjdGlvbkZvcm0udmFsdWUua2luZH0vYWN0aW9uYCwge1xuICAgICAgLi4uYWN0aW9uRm9ybS52YWx1ZSxcbiAgICAgIHByb2plY3RJZDogcHJvamVjdElkLnZhbHVlXG4gICAgfSlcbiAgICBhY3Rpb25PcGVuLnZhbHVlID0gZmFsc2VcbiAgICBhd2FpdCBsb2FkKClcbiAgICBzdWNjZXNzLnZhbHVlID0gJ+acrOS6uuWKqOS9nOW3suS/neWtmO+8jOeUn+WRveWRqOacn+aMieacieaViOacn+WSjOW9k+WJjeeKtuaAgeiuoeeulydcbiAgfSBjYXRjaCB7XG4gICAgZXJyb3IudmFsdWUgPSAn5pys5Lq65Yqo5L2c6KKr5ouS57ud5oiW54mI5pys5bey5Y+Y77yb5L6d5o2u6L6T5YWl5bey5L+d55WZ77yM5LiN6KeG5Li65om55YeG5oiQ5Yqf44CCJ1xuICB9IGZpbmFsbHkge1xuICAgIGJ1c3kudmFsdWUgPSBmYWxzZVxuICB9XG59XG5hc3luYyBmdW5jdGlvbiBwcmVjaGVjaygpIHtcbiAgc3VjY2Vzcy52YWx1ZSA9ICcnXG4gIGNvbnN0IG93biA9IHRpY2tldCxcbiAgICB0YXJnZXQgPSBwcm9qZWN0SWQudmFsdWVcbiAgYnVzeS52YWx1ZSA9IHRydWVcbiAgZXJyb3IudmFsdWUgPSAnJ1xuICB0cnkge1xuICAgIGNvbnN0IHJlc3VsdCA9IGF3YWl0IGFwaS5nZXQoJy9wcmVjaGVjaycsIHsgcHJvamVjdElkOiB0YXJnZXQgfSlcbiAgICBpZiAob3duID09PSB0aWNrZXQgJiYgdGFyZ2V0ID09PSBwcm9qZWN0SWQudmFsdWUpIGdhdGUudmFsdWUgPSByZXN1bHRcbiAgfSBjYXRjaCB7XG4gICAgZ2F0ZS52YWx1ZSA9IHVuZGVmaW5lZFxuICAgIGVycm9yLnZhbHVlID0gJ+WPkeW4g+mihOajgOafpeacquWujOaIkO+8m+S4jeiDveaKiuacquefpee7k+aenOinhuS4uumAmui/h+OAgidcbiAgfSBmaW5hbGx5IHtcbiAgICBidXN5LnZhbHVlID0gZmFsc2VcbiAgfVxufVxuYXN5bmMgZnVuY3Rpb24gcHVibGlzaChkcmFmdDogYm9vbGVhbikge1xuICBsYXN0UHVibGlzaC52YWx1ZSA9IHtcbiAgICBwcm9qZWN0SWQ6IHByb2plY3RJZC52YWx1ZSxcbiAgICBwcm9qZWN0U2NvcGVWZXJzaW9uOiBnYXRlLnZhbHVlLnByb2plY3RTY29wZVZlcnNpb24sXG4gICAgcmVxdWVzdEtleTogcmVxdWVzdEtleS52YWx1ZSxcbiAgICBkZXBlbmRlbmN5VG9rZW46IGdhdGUudmFsdWUuZGVwZW5kZW5jeVRva2VuLFxuICAgIHJlYXNvbjogcHVibGlzaFJlYXNvbi52YWx1ZSxcbiAgICBkcmFmdFxuICB9XG4gIGF3YWl0IHJldHJ5UHVibGlzaCgpXG59XG5hc3luYyBmdW5jdGlvbiByZXRyeVB1Ymxpc2goKSB7XG4gIHN1Y2Nlc3MudmFsdWUgPSAnJ1xuICBidXN5LnZhbHVlID0gdHJ1ZVxuICBlcnJvci52YWx1ZSA9ICcnXG4gIHRyeSB7XG4gICAgY29uc3QgaWQgPSBhd2FpdCBhcGkucG9zdCgnL3B1Ymxpc2gnLCBsYXN0UHVibGlzaC52YWx1ZSlcbiAgICBhd2FpdCBsb2FkKClcbiAgICBzdWNjZXNzLnZhbHVlID0gYOWbuuWumuW/q+eFp0lE77yaJHtpZH3jgILnm7jlkIzor7fmsYLph43or5Xku43pobvlvZPliY3otYTmoLzmnInmlYjjgIJgXG4gIH0gY2F0Y2gge1xuICAgIGVycm9yLnZhbHVlID0gJ+WPkeW4g+aIlumHjeivleacquWujOaIkO+8jOivtOaYjuWPiuivt+axguWPt+W3suS/neeVme+8m+ivt+mHjeaWsOajgOafpeW9k+WJjeaOiOadg+OAgeWGs+iuruOAgeiMg+WbtOWSjOS+nei1lueJiOacrOOAgidcbiAgfSBmaW5hbGx5IHtcbiAgICBidXN5LnZhbHVlID0gZmFsc2VcbiAgfVxufVxuYXN5bmMgZnVuY3Rpb24gcmVhZFJlbGVhc2UocmVsZWFzZUlkOiBudW1iZXIpIHtcbiAgc3VjY2Vzcy52YWx1ZSA9ICcnXG4gIGNvbnN0IG93biA9IHRpY2tldCxcbiAgICB0YXJnZXQgPSBwcm9qZWN0SWQudmFsdWVcbiAgdHJ5IHtcbiAgICBjb25zdCByZXN1bHQgPSBhd2FpdCBhcGkuZ2V0KCcvcmVsZWFzZScsIHsgcHJvamVjdElkOiB0YXJnZXQsIHJlbGVhc2VJZCB9KVxuICAgIGlmIChvd24gIT09IHRpY2tldCB8fCB0YXJnZXQgIT09IHByb2plY3RJZC52YWx1ZSkgcmV0dXJuXG4gICAgZnJvemVuLnZhbHVlID0gcmVzdWx0XG4gICAgc2VsZWN0ZWRTaGVldC52YWx1ZSA9ICfor4TlrqHniYjmnKzor7TmmI4nXG4gIH0gY2F0Y2gge1xuICAgIGZyb3plbi52YWx1ZSA9IHVuZGVmaW5lZFxuICAgIGVycm9yLnZhbHVlID0gJ+W9k+WJjeadg+mZkOS4jeiDveivu+WPluivpeW/q+eFp++8jOacquWxleekuuaXp+e8k+WtmOWGheWuueOAgidcbiAgfVxufVxuYXN5bmMgZnVuY3Rpb24gY3JlYXRlSm9iKHJlbGVhc2VJZDogbnVtYmVyKSB7XG4gIHN1Y2Nlc3MudmFsdWUgPSAnJ1xuICBjb25zdCBvd24gPSB0aWNrZXQsXG4gICAgdGFyZ2V0ID0gcHJvamVjdElkLnZhbHVlXG4gIHRyeSB7XG4gICAgY29uc3QgaWRzID0gZXhwb3J0SWRzLnZhbHVlLnRyaW0oKVxuICAgICAgPyBleHBvcnRJZHMudmFsdWUuc3BsaXQoJywnKS5tYXAoKHgpID0+IE51bWJlcih4LnRyaW0oKSkpXG4gICAgICA6IHVuZGVmaW5lZFxuICAgIGlmIChpZHM/LnNvbWUoKHgpID0+ICFOdW1iZXIuaXNTYWZlSW50ZWdlcih4KSB8fCB4IDw9IDApKSB0aHJvdyBuZXcgRXJyb3IoJ2ludmFsaWQgaWRzJylcbiAgICBjb25zdCBpZCA9IGF3YWl0IGFwaS5wb3N0KCcvZXhwb3J0L2NyZWF0ZScsIHtcbiAgICAgIHByb2plY3RJZDogdGFyZ2V0LFxuICAgICAgcmVsZWFzZUlkLFxuICAgICAgcmVxdWlyZW1lbnRJZHM6IGlkc1xuICAgIH0pXG4gICAgaWYgKG93biAhPT0gdGlja2V0IHx8IHRhcmdldCAhPT0gcHJvamVjdElkLnZhbHVlKSByZXR1cm5cbiAgICBjb25zdCByb3cgPSB7IGlkLCBwcm9qZWN0SWQ6IHRhcmdldCwgcmVsZWFzZUlkLCBzdGF0ZTogJ1FVRVVFRCcsIGxhYmVsOiAnJywgZXJyb3I6ICcnIH1cbiAgICBqb2JzLnZhbHVlLnB1c2gocm93KVxuICAgIGF3YWl0IHJlZnJlc2hKb2Iocm93KVxuICB9IGNhdGNoIHtcbiAgICBlcnJvci52YWx1ZSA9ICflr7zlh7rku7vliqHmnKrliJvlu7rvvIzor7fmoLjlr7nnrZvpgInpnIDmsYJJROWPiuW9k+WJjeWvvOWHuuadg+mZkOOAgidcbiAgfVxufVxuYXN5bmMgZnVuY3Rpb24gcmVmcmVzaEpvYihyb3c6IGFueSkge1xuICBzdWNjZXNzLnZhbHVlID0gJydcbiAgdHJ5IHtcbiAgICBjb25zdCByZXN1bHQgPSBhd2FpdCBhcGkuZ2V0KCcvZXhwb3J0L3N0YXR1cycsIHsgcHJvamVjdElkOiByb3cucHJvamVjdElkLCBpZDogcm93LmlkIH0pXG4gICAgam9icy52YWx1ZSA9IGpvYnMudmFsdWUubWFwKChqb2IpID0+IChqb2IuaWQgPT09IHJvdy5pZCA/IHsgLi4uam9iLCAuLi5yZXN1bHQgfSA6IGpvYikpXG4gIH0gY2F0Y2gge1xuICAgIGpvYnMudmFsdWUgPSBqb2JzLnZhbHVlLm1hcCgoam9iKSA9PlxuICAgICAgam9iLmlkID09PSByb3cuaWRcbiAgICAgICAgPyB7IC4uLmpvYiwgc3RhdGU6ICdVTktOT1dOJywgZXJyb3I6ICflvZPliY3mnYPpmZDmiJbniYjmnKzlt7Llj5jljJbvvIzml6Dms5XkuqTku5jml6fku7vliqHjgIInIH1cbiAgICAgICAgOiBqb2JcbiAgICApXG4gIH1cbn1cbmFzeW5jIGZ1bmN0aW9uIGNsYWltKHJvdzogYW55KSB7XG4gIHN1Y2Nlc3MudmFsdWUgPSAnJ1xuICB0cnkge1xuICAgIGNvbnN0IGJsb2IgPSBhd2FpdCBhcGkuZG93bmxvYWQoeyBwcm9qZWN0SWQ6IHJvdy5wcm9qZWN0SWQsIGlkOiByb3cuaWQgfSlcbiAgICBkb3dubG9hZC5leGNlbChibG9iLCBg6Jaq6YWs6K+E5a6hLeW/q+eFpyR7cm93LnJlbGVhc2VJZH0t5Lu75YqhJHtyb3cuaWR9Lnhsc3hgKVxuICB9IGNhdGNoIHtcbiAgICBmcm96ZW4udmFsdWUgPSB1bmRlZmluZWRcbiAgICBnYXRlLnZhbHVlID0gdW5kZWZpbmVkXG4gICAgam9icy52YWx1ZSA9IGpvYnMudmFsdWUubWFwKChqb2IpID0+XG4gICAgICBqb2IuaWQgPT09IHJvdy5pZFxuICAgICAgICA/IHsgLi4uam9iLCBzdGF0ZTogJ0RFTklFRCcsIGVycm9yOiAn6aKG5Y+W6KKr5ouS57ud77yb5pen5Lu75Yqh5oiW5Zyw5Z2A5LiN6IO957uV6L+H5b2T5YmN5p2D6ZmQ44CCJyB9XG4gICAgICAgIDogam9iXG4gICAgKVxuICB9XG59XG53YXRjaCgoKSA9PiByb3V0ZS5xdWVyeS5wcm9qZWN0SWQsIGxvYWQsIHsgaW1tZWRpYXRlOiB0cnVlIH0pXG5vbkJlZm9yZVVubW91bnQoKCkgPT4ge1xuICB0aWNrZXQrK1xufSlcbjwvc2NyaXB0PlxuXG48c3R5bGUgc2NvcGVkPlxuLnRvb2xiYXIge1xuICBkaXNwbGF5OiBmbGV4O1xuICBmbGV4LXdyYXA6IHdyYXA7XG4gIGdhcDogOHB4O1xuICBtYXJnaW46IDE0cHggMDtcbn1cblxuLnRvb2xiYXIgOmRlZXAoLmVsLXNlbGVjdCkge1xuICBtaW4td2lkdGg6IDIzMHB4O1xufVxuXG5zZWN0aW9uIHtcbiAgbWFyZ2luLXRvcDogMzBweDtcbn1cblxuLndpZGUtdGFibGUge1xuICBtYXgtd2lkdGg6IDEwMCU7XG4gIG92ZXJmbG93LXg6IGF1dG87XG59XG5cbi53aWRlLXRhYmxlIDpkZWVwKC5lbC10YWJsZSkge1xuICBtaW4td2lkdGg6IDkwMHB4O1xufVxuXG4ucmVjb3JkIHtcbiAgd2hpdGUtc3BhY2U6IHByZS13cmFwO1xuICBvdmVyZmxvdy13cmFwOiBhbnl3aGVyZTtcbn1cblxuaDEge1xuICBmb250LXNpemU6IDI2cHg7XG59XG5cbkBtZWRpYSAod2lkdGggPD0gNjAwcHgpIHtcbiAgaDEge1xuICAgIGZvbnQtc2l6ZTogMjJweDtcbiAgfVxuXG4gIC50b29sYmFyIHtcbiAgICBkaXNwbGF5OiBncmlkO1xuICB9XG5cbiAgLmdvdmVybmFuY2UtcGFnZSB7XG4gICAgbWluLXdpZHRoOiAwO1xuICB9XG59XG48L3N0eWxlPlxuPHN0eWxlPlxuLmdvdmVybmFuY2UtZGlhbG9nIC5lbC1kaWFsb2dfX2JvZHkge1xuICBtYXgtaGVpZ2h0OiA1OHZoO1xuICBvdmVyZmxvdy15OiBhdXRvO1xufVxuPC9zdHlsZT5cbiJdLCJmaWxlIjoiL1VzZXJzL3dhaXRmb3Jwcm8vc291cmNlL2dpdGh1Yi15dWRhby9ydW95aS12dWUtcHJvL3l1ZGFvLXVpL3l1ZGFvLXVpLWFkbWluLXZ1ZTMvc3JjL3ZpZXdzL2hybS9wYXlyb2xsL2dvdmVybmFuY2UvaW5kZXgudnVlIn0=