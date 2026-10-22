import { createHotContext as __vite__createHotContext } from "/@vite/client";import.meta.hot = __vite__createHotContext("/src/views/hrm/payroll/review/index.vue");/* unplugin-vue-components disabled */import { ElDialog as __unplugin_components_12 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_dialog_style_css.js?v=d4fc3075";
import { ElForm as __unplugin_components_11 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_form_style_css.js?v=d4fc3075";
import { ElFormItem as __unplugin_components_10 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_form-item_style_css.js?v=d4fc3075";
import __unplugin_components_9 from "/src/components/ContentWrap/src/ContentWrap.vue";
import { ElPagination as __unplugin_components_8 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_pagination_style_css.js?v=d4fc3075";
import { ElTable as __unplugin_components_7 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_table_style_css.js?v=d4fc3075";
import { ElTableColumn as __unplugin_components_6 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_table-column_style_css.js?v=d4fc3075";
import { ElInput as __unplugin_components_5 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_input_style_css.js?v=d4fc3075";
import { ElInputNumber as __unplugin_components_4 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_input-number_style_css.js?v=d4fc3075";
import { ElButton as __unplugin_components_3 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_button_style_css.js?v=d4fc3075";
import { ElSelect as __unplugin_components_2 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_select_style_css.js?v=d4fc3075";
import { ElOption as __unplugin_components_1 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_option_style_css.js?v=d4fc3075";
import { ElAlert as __unplugin_components_0 } from "/node_modules/.vite/deps/element-plus_es.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_base_style_css.js?v=d4fc3075";import "/node_modules/.vite/deps/element-plus_es_components_alert_style_css.js?v=d4fc3075";
import { defineComponent as _defineComponent } from "/node_modules/.vite/deps/vue.js?v=d4fc3075";
import { nextTick } from "/node_modules/.vite/deps/vue.js?v=d4fc3075";
import { opinionMissing, failureGuidance } from "/src/views/hrm/payroll/review/validation.ts?t=1792639123101";
import { checkPermi } from "/src/utils/permission.ts?t=1792639332730";
import * as api from "/src/api/hrm/payroll/requirement.ts?t=1792639332730";
import * as review from "/src/api/hrm/payroll/review.ts?t=1792639332730";
import { listWorkbench } from "/src/api/hrm/payroll/workbench.ts?t=1792639332730";
import { payrollModules as modules, sourcesForModules } from "/src/views/hrm/payroll/requirements.ts?t=1792317640204";
import { demo, result, calculateDemo, reviewOrder as order, reviewViews, reviewTopics, demoTrend, demoCostStructure } from "/src/views/hrm/payroll/review/demo.ts?t=1792391042795";
import ScopeFields from "/src/views/hrm/payroll/requirement/ScopeFields.vue?t=1792370936741";

import { useRoute, useRouter } from "/node_modules/.vite/deps/vue-router.js?v=d4fc3075";
import { useMessage } from "/src/hooks/web/useMessage.ts";
import { computed, ref, reactive, h, watch } from "/node_modules/.vite/deps/vue.js?v=d4fc3075";
const _sfc_main = /*@__PURE__*/ _defineComponent({
	__name: "index",
	setup(__props, { expose: __expose }) {
		__expose();
		const route = useRoute(), router = useRouter(), message = useMessage();
		const module = computed(() => order.includes(route.query.module) ? route.query.module : "OV");
		const projectId = computed(() => Number(route.query.projectId) || undefined);
		const view = computed(() => reviewViews[module.value]), step = computed(() => order.indexOf(module.value));
		const projects = ref([]), project = computed(() => projects.value.find((p) => p.id === projectId.value));
		const rows = ref([]), sources = ref([]), sourceId = ref(), stats = ref(), capabilities = ref({});
		const reminders = ref([]);
		const total = ref(0), pageNo = ref(1), sort = ref("code"), selected = ref([]), error = ref(""), loading = ref(false);
		const filters = reactive({
			text: "",
			status: "",
			decision: ""
		}), applied = ref({
			text: "",
			status: "",
			decision: ""
		});
		const candidates = computed(() => sourcesForModules([module.value]));
		const sourceDescription = computed(() => {
			const s = sources.value.find((s) => s.id === sourceId.value);
			return s ? `来源${s.id} · 供给${s.data.supply} · 来源负责人${s.data.ownerId} · 语义/运营/交付${s.data.semanticVersion}/${s.data.operationalVersion}/${s.data.deliveryVersion}` : "当前来源不可见或已撤销，请刷新";
		});
		const canCreate = computed(() => !!projectId.value && !!capabilities.value.EDIT && checkPermi(["hrm:payroll:requirement:update"]));
		const statuses = {
			PENDING: "待确认",
			REVIEWING: "评审中",
			CONFIRMED: "已确认",
			DISPUTED: "有异议"
		};
		const decisions = {
			CANDIDATE: "候选",
			INCLUDED: "纳入本期",
			DEFERRED: "暂缓",
			REMOVED: "移除作废"
		};
		const implementations = {
			UNSCHEDULED: "未排期",
			DEVELOPING: "开发中",
			ACCEPTANCE_PENDING: "待验收",
			ACCEPTED: "验收通过（人工记录）"
		};
		const readiness = {
			MISSING: "未就绪",
			PARTIAL: "部分就绪",
			READY: "已核验可用",
			EXEMPT: "不适用"
		};
		const ratioKeys = [
			"confirmation",
			"scopeDisposition",
			"includedConfirmation",
			"readiness",
			"includedReadiness"
		];
		const ratioLabels = {
			confirmation: "功能确认率",
			scopeDisposition: "范围处置率",
			includedConfirmation: "本期确认率",
			readiness: "来源可用率",
			includedReadiness: "本期来源可用率"
		};
		const parse = (value) => {
			try {
				return value ? JSON.parse(value) : {};
			} catch {
				return {};
			}
		};
		const params = () => ({
			projectId: projectId.value,
			module: module.value === "COL" ? undefined : module.value,
			...Object.fromEntries(Object.entries(applied.value).map(([k, v]) => [k, v || undefined]))
		});
		let loadTicket = 0;
		async function load() {
			const ticket = ++loadTicket;
			loading.value = true;
			error.value = "";
			rows.value = [];
			stats.value = undefined;
			sources.value = [];
			capabilities.value = {};
			reminders.value = [];
			selected.value = [];
			try {
				const ps = await api.listRequirementProjects();
				if (ticket !== loadTicket) return;
				projects.value = ps;
				if (!projectId.value) {
					error.value = "请选择有权评审项目；未推定任何范围或批准。";
					return;
				}
				const [page, stat, src, caps, inbox] = await Promise.all([
					review.reviewPage({
						...params(),
						pageNo: pageNo.value,
						pageSize: 10,
						sort: sort.value
					}),
					review.reviewStatistics(params()),
					listWorkbench(projectId.value, "SOURCE"),
					api.requirementCapabilities(projectId.value),
					review.reviewReminders(projectId.value)
				]);
				if (ticket !== loadTicket) return;
				rows.value = page.list;
				total.value = page.total;
				stats.value = stat;
				sources.value = src;
				capabilities.value = caps;
				reminders.value = inbox;
				await nextTick();
				focusAnchor();
			} catch {
				if (ticket === loadTicket) error.value = "真实评审数据加载失败：可能无权或接口异常。请重试；未加载内容不显示为零或通过。";
			} finally {
				if (ticket === loadTicket) loading.value = false;
			}
		}
		async function go(m, anchor) {
			pageNo.value = 1;
			Object.assign(filters, {
				text: "",
				status: "",
				decision: ""
			});
			applied.value = { ...filters };
			if (module.value === m && String(route.query.anchor || "") === String(anchor || "")) {
				await load();
			} else {
				await router.push({
					path: "/hrm-payroll/review",
					query: {
						projectId: projectId.value,
						module: m,
						anchor
					}
				});
			}
			await nextTick();
			focusAnchor();
		}
		function selectProject(id) {
			formVisible.value = false;
			actionVisible.value = false;
			historyVisible.value = false;
			return router.push({
				path: "/hrm-payroll/review",
				query: {
					projectId: id,
					module: module.value
				}
			});
		}
		function central() {
			return go("COL");
		}
		function original() {
			return router.push("/hrm-payroll/overview");
		}
		function openSourceWorkbench() {
			return router.push({
				path: "/hrm-payroll/requirement",
				query: {
					projectId: projectId.value,
					module: module.value,
					from: route.fullPath
				}
			});
		}
		function focusAnchor() {
			const anchor = String(route.query.anchor || "");
			if (/^(module|field-[a-z0-9-]+|operation-[a-z0-9-]+|flow-[a-z0-9-]+)$/.test(anchor)) {
				const el = document.getElementById(anchor);
				el?.scrollIntoView({ block: "center" });
				el?.focus();
			}
		}
		function resetFocus() {
			document.getElementById("module")?.focus();
		}
		function search() {
			pageNo.value = 1;
			applied.value = { ...filters };
			load();
		}
		function clearFilters() {
			Object.assign(filters, {
				text: "",
				status: "",
				decision: ""
			});
			search();
		}
		function allowed(row, action) {
			return row.permittedActions?.includes(action) && (action !== "EDIT" || checkPermi(["hrm:payroll:requirement:update"]));
		}
		function returnTo(row) {
			const c = parse(row.businessContextJson);
			return go(order.includes(c.contextModule) ? c.contextModule : row.module, c.anchor || "module");
		}
		async function initialize() {
			if (projectId.value) {
				await api.initializeRequirements(projectId.value);
				await load();
			}
		}
		async function download() {
			const data = await review.reviewExport(params());
			const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: "application/json" }));
			const a = document.createElement("a");
			a.href = url;
			a.download = "当前筛选评审清单.json";
			a.click();
			URL.revokeObjectURL(url);
		}
		const demoAdjustment = ref(10500);
		const demoState = ref("示例尚未执行；不代表真实薪酬状态"), slipVisible = ref(false);
		function simulate() {
			const x = calculateDemo();
			demoState.value = `示例试算：应发${x.gross}元，个税${x.tax}元，实发${x.net}元；仅页面内存，无正式业务写入。`;
		}
		const conflictLatest = ref();
		const formVisible = ref(false), form = ref({}), business = ref({}), saving = ref(false), saveError = ref("");
		const businessFields = [
			{
				key: "role",
				label: "业务角色"
			},
			{
				key: "scenario",
				label: "触发场景"
			},
			{
				key: "goal",
				label: "业务目标"
			},
			{
				key: "expected",
				label: "期望行为"
			},
			{
				key: "normalCase",
				label: "正常样例"
			},
			{
				key: "exceptionCase",
				label: "异常样例"
			},
			{
				key: "calculation",
				label: "计算或业务规则（不适用须说明）"
			},
			{
				key: "unit",
				label: "单位（不适用须说明）"
			},
			{
				key: "precision",
				label: "精度与舍入（不适用须说明）"
			},
			{
				key: "validity",
				label: "有效期或适用依据"
			},
			{
				key: "ruleException",
				label: "规则例外与处理"
			}
		];
		const opinionErrors = ref({});
		async function focusOpinionField(label) {
			await nextTick();
			const field = document.querySelector(`.hrm-review-dialog [aria-label="${label}"]`);
			field?.scrollIntoView({ block: "center" });
			field?.focus();
		}
		function newOpinion(targetType, anchor) {
			opinionErrors.value = {};
			conflictLatest.value = undefined;
			form.value = {
				projectId: projectId.value,
				projectScopeVersion: project.value?.projectScopeVersion,
				code: "CUSTOM-0000",
				module: module.value,
				scopeMode: "INHERIT",
				status: "PENDING",
				priority: 2,
				readiness: "MISSING",
				sourceApplicability: "APPLICABLE",
				description: ""
			};
			business.value = {
				page: "/hrm-payroll/review",
				contextModule: module.value,
				targetType,
				anchor
			};
			saveError.value = "";
			formVisible.value = true;
		}
		function edit(row) {
			opinionErrors.value = {};
			conflictLatest.value = undefined;
			form.value = {
				...row,
				projectScopeVersion: project.value?.projectScopeVersion,
				status: row.status === "CONFIRMED" ? "REVIEWING" : row.status
			};
			business.value = {
				page: "/hrm-payroll/review",
				contextModule: row.module,
				targetType: "MODULE",
				anchor: "module",
				...parse(row.businessContextJson)
			};
			saveError.value = "";
			formVisible.value = true;
		}
		async function saveOpinion() {
			opinionErrors.value = opinionMissing(form.value, business.value);
			if (Object.keys(opinionErrors.value).length) {
				saveError.value = `请补齐以下字段：${Object.values(opinionErrors.value).join("、")}。输入已保留。`;
				await focusOpinionField(Object.values(opinionErrors.value)[0]);
				return;
			}
			saving.value = true;
			saveError.value = "";
			try {
				if (form.value.projectId !== projectId.value) throw Error("changed project");
				await api.saveRequirement({
					...form.value,
					businessContextJson: JSON.stringify(business.value)
				});
				formVisible.value = false;
				message.success("意见已真实保存");
				await load();
			} catch (error) {
				saveError.value = failureGuidance(error);
			} finally {
				saving.value = false;
			}
		}
		async function reloadConflict() {
			try {
				const latest = (await api.listRequirements(undefined, projectId.value)).find((x) => x.id === form.value.id);
				if (latest) {
					conflictLatest.value = latest;
					form.value.version = latest.version;
					form.value.projectScopeVersion = latest.projectScopeVersion;
					saveError.value = `已读取最新版本${latest.version}，输入仍保留；请核对最新功能：${latest.description}，合并后再保存。`;
				}
			} catch {
				saveError.value = "读取最新版本失败，输入仍保留。";
			}
		}
		const actionVisible = ref(false), actionKind = ref("decision"), active = ref(), action = ref({});
		const actionTitles = {
			decision: "独立范围决策",
			implementation: "独立实施关联",
			collaboration: "会议纪要与受控站内提醒"
		};
		const implementationFields = [
			{
				key: "page",
				label: "实现页面引用"
			},
			{
				key: "api",
				label: "接口引用"
			},
			{
				key: "task",
				label: "开发任务引用"
			},
			{
				key: "evidence",
				label: "验收证据引用"
			}
		];
		function openAction(row, kind) {
			active.value = row;
			actionKind.value = kind;
			action.value = kind === "decision" ? {
				decision: row.scopeDecision || "CANDIDATE",
				removalKind: "DUPLICATE"
			} : kind === "implementation" ? {
				state: "UNSCHEDULED",
				...parse(row.implementationJson)
			} : { kind: "MEETING" };
			saveError.value = "";
			actionVisible.value = true;
		}
		function context(row) {
			return {
				projectId: projectId.value,
				projectScopeVersion: project.value?.projectScopeVersion,
				id: row.id,
				version: row.version,
				reason: "页面评审操作"
			};
		}
		async function saveAction() {
			saving.value = true;
			saveError.value = "";
			try {
				const command = {
					...context(active.value),
					...action.value
				};
				const fn = actionKind.value === "decision" ? review.scopeDecision : actionKind.value === "implementation" ? review.saveImplementation : review.saveCollaboration;
				await fn(command);
				actionVisible.value = false;
				await load();
			} catch {
				saveError.value = "操作未完成：缺必填、无权、引用无效或版本冲突。输入已保留，请核对。";
			} finally {
				saving.value = false;
			}
		}
		const historyVisible = ref(false), histories = ref([]), compareA = ref(), compareB = ref();
		async function showHistory(row) {
			try {
				histories.value = await api.requirementHistory(row.id, projectId.value);
				const v = histories.value.map((h) => h.version).sort((a, b) => a - b);
				compareA.value = v.at(-2) ?? v[0];
				compareB.value = v.at(-1);
				historyVisible.value = true;
			} catch {
				message.error("历史读取失败，不能推定无历史");
			}
		}
		const differences = computed(() => {
			const a = parse(histories.value.find((h) => h.version === compareA.value)?.snapshot), b = parse(histories.value.find((h) => h.version === compareB.value)?.snapshot);
			return [...new Set([...Object.keys(a), ...Object.keys(b)])].filter((k) => JSON.stringify(a[k]) !== JSON.stringify(b[k])).map((key) => ({
				key,
				before: JSON.stringify(a[key]),
				after: JSON.stringify(b[key])
			}));
		});
		const drillVisible = ref(false), drillData = ref("");
		function drill(key) {
			if (stats.value) {
				drillData.value = JSON.stringify(key === "L" ? stats.value.L : stats.value.sets[key], null, 2);
				drillVisible.value = true;
			}
		}
		const batchVisible = ref(false), batchReviewer = ref(), batchResult = ref("");
		async function assignBatch() {
			saving.value = true;
			try {
				batchResult.value = JSON.stringify(await review.batchAssign({
					projectId: projectId.value,
					projectScopeVersion: project.value?.projectScopeVersion,
					reviewerId: batchReviewer.value,
					items: selected.value.map((row) => context(row))
				}), null, 2);
				await load();
			} catch {
				batchResult.value = "批量请求未完成；未推定任何分派成功。请核对权限和请求。";
			} finally {
				saving.value = false;
			}
		}
		async function confirm(row) {
			const missing = opinionMissing(row, parse(row.businessContextJson) || {}, true);
			if (Object.keys(missing).length) {
				message.warning(`确认前请补齐以下字段：${Object.values(missing).join("、")}；尚未产生签字`);
				return;
			}
			try {
				await message.confirm(`本人确认需求${row.id}的当前版本${row.version}？不会保存未提交表单。`);
				await api.confirmRequirement(row.id, row.version, projectId.value, project.value?.projectScopeVersion);
				await load();
			} catch (error) {
				if (error !== "cancel" && error !== "close") message.warning(failureGuidance(error));
			}
		}
		watch(() => route.fullPath, () => {
			formVisible.value = false;
			actionVisible.value = false;
			historyVisible.value = false;
			drillVisible.value = false;
			batchVisible.value = false;
			sourceId.value = undefined;
			pageNo.value = 1;
			load();
		}, { immediate: true });
		const __returned__ = {
			route,
			router,
			message,
			module,
			projectId,
			view,
			step,
			projects,
			project,
			rows,
			sources,
			sourceId,
			stats,
			capabilities,
			reminders,
			total,
			pageNo,
			sort,
			selected,
			error,
			loading,
			filters,
			applied,
			candidates,
			sourceDescription,
			canCreate,
			statuses,
			decisions,
			implementations,
			readiness,
			ratioKeys,
			ratioLabels,
			parse,
			params,
			get loadTicket() {
				return loadTicket;
			},
			set loadTicket(v) {
				loadTicket = v;
			},
			load,
			go,
			selectProject,
			central,
			original,
			openSourceWorkbench,
			focusAnchor,
			resetFocus,
			search,
			clearFilters,
			allowed,
			returnTo,
			initialize,
			download,
			demoAdjustment,
			demoState,
			slipVisible,
			simulate,
			conflictLatest,
			formVisible,
			form,
			business,
			saving,
			saveError,
			businessFields,
			opinionErrors,
			focusOpinionField,
			newOpinion,
			edit,
			saveOpinion,
			reloadConflict,
			actionVisible,
			actionKind,
			active,
			action,
			actionTitles,
			implementationFields,
			openAction,
			context,
			saveAction,
			historyVisible,
			histories,
			compareA,
			compareB,
			showHistory,
			differences,
			drillVisible,
			drillData,
			drill,
			batchVisible,
			batchReviewer,
			batchResult,
			assignBatch,
			confirm,
			get modules() {
				return modules;
			},
			get demo() {
				return demo;
			},
			get result() {
				return result;
			},
			get order() {
				return order;
			},
			get reviewTopics() {
				return reviewTopics;
			},
			get demoTrend() {
				return demoTrend;
			},
			get demoCostStructure() {
				return demoCostStructure;
			},
			ScopeFields
		};
		Object.defineProperty(__returned__, "__isScriptSetup", {
			enumerable: false,
			value: true
		});
		return __returned__;
	}
});
import { toDisplayString as _toDisplayString, createElementVNode as _createElementVNode, resolveComponent as _resolveComponent, createVNode as _createVNode, renderList as _renderList, Fragment as _Fragment, openBlock as _openBlock, createElementBlock as _createElementBlock, createBlock as _createBlock, withCtx as _withCtx, createTextVNode as _createTextVNode, normalizeStyle as _normalizeStyle, createCommentVNode as _createCommentVNode, resolveDirective as _resolveDirective, withDirectives as _withDirectives, withKeys as _withKeys } from "/node_modules/.vite/deps/vue.js?v=d4fc3075";
const _hoisted_1 = {
	tabindex: "-1",
	id: "review-heading"
};
const _hoisted_2 = { class: "toolbar" };
const _hoisted_3 = {
	"aria-label": "十视图评审导航",
	class: "view-nav"
};
const _hoisted_4 = { class: "toolbar" };
const _hoisted_5 = { "aria-label": "虚构业务展示" };
const _hoisted_6 = {
	class: "wide-table",
	tabindex: "0",
	"aria-label": "示例业务表，左右方向可横向滚动"
};
const _hoisted_7 = ["id"];
const _hoisted_8 = {
	id: "operation-simulate",
	tabindex: "-1",
	class: "toolbar anchor"
};
const _hoisted_9 = { role: "status" };
const _hoisted_10 = {
	id: "flow-approval",
	tabindex: "-1",
	class: "anchor"
};
const _hoisted_11 = {
	key: 0,
	"aria-label": "虚构图表示例"
};
const _hoisted_12 = { class: "demo-bar-track" };
const _hoisted_13 = {
	key: 1,
	id: "operation-adjustment",
	tabindex: "-1",
	"aria-label": "虚构调薪表单"
};
const _hoisted_14 = { class: "toolbar" };
const _hoisted_15 = { "aria-label": "候选功能讨论场景" };
const _hoisted_16 = ["id"];
const _hoisted_17 = {
	key: 3,
	role: "status"
};
const _hoisted_18 = {
	id: "module",
	tabindex: "-1",
	class: "anchor",
	"aria-label": "页面内功能需求征集"
};
const _hoisted_19 = { class: "toolbar" };
const _hoisted_20 = { class: "toolbar" };
const _hoisted_21 = {
	key: 0,
	role: "status"
};
const _hoisted_22 = {
	key: 1,
	class: "wide-table",
	tabindex: "0",
	"aria-label": "真实需求清单"
};
const _hoisted_23 = { "aria-label": "数据来源征集" };
const _hoisted_24 = { key: 0 };
const _hoisted_25 = { "aria-label": "我的站内提醒" };
const _hoisted_26 = { key: 0 };
const _hoisted_27 = { "aria-label": "真实统计与下钻" };
const _hoisted_28 = { key: 0 };
const _hoisted_29 = {
	key: 1,
	class: "stats-grid"
};
const _hoisted_30 = ["onClick"];
const _hoisted_31 = { key: 2 };
const _hoisted_32 = { key: 3 };
const _hoisted_33 = { key: 1 };
const _hoisted_34 = { class: "form-grid" };
const _hoisted_35 = { class: "dialog-actions" };
const _hoisted_36 = { class: "dialog-actions" };
const _hoisted_37 = { class: "toolbar" };
const _hoisted_38 = { class: "wide-table" };
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
	const _component_el_alert = __unplugin_components_0;
	const _component_el_option = __unplugin_components_1;
	const _component_el_select = __unplugin_components_2;
	const _component_el_button = __unplugin_components_3;
	const _component_el_input_number = __unplugin_components_4;
	const _component_el_input = __unplugin_components_5;
	const _component_el_table_column = __unplugin_components_6;
	const _component_el_table = __unplugin_components_7;
	const _component_el_pagination = __unplugin_components_8;
	const _component_ContentWrap = __unplugin_components_9;
	const _component_el_form_item = __unplugin_components_10;
	const _component_el_form = __unplugin_components_11;
	const _component_el_dialog = __unplugin_components_12;
	const _directive_hasPermi = _resolveDirective("hasPermi");
	return _openBlock(), _createElementBlock(
		_Fragment,
		null,
		[
			_createVNode(_component_ContentWrap, { class: "review-page" }, {
				default: _withCtx(() => [
					_createElementVNode(
						"h1",
						_hoisted_1,
						"薪酬需求评审工作台 · " + _toDisplayString($setup.modules[$setup.module]),
						1
						/* TEXT */
					),
					_createVNode(_component_el_alert, {
						title: "示例数据，仅用于讨论。意见与评审记录真实保存；演示核算、审批及工资条只作用于虚构样例。",
						type: "warning",
						closable: false
					}),
					_createElementVNode("div", _hoisted_2, [
						_createVNode(_component_el_select, {
							"aria-label": "评审项目",
							"model-value": $setup.projectId,
							placeholder: "选择有权评审项目",
							onChange: $setup.selectProject
						}, {
							default: _withCtx(() => [(_openBlock(true), _createElementBlock(
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
						_createVNode(_component_el_button, { onClick: $setup.load }, {
							default: _withCtx(() => [..._cache[57] || (_cache[57] = [_createTextVNode(
								"刷新工作台",
								-1
								/* CACHED */
							)])]),
							_: 1
						}),
						_createVNode(_component_el_button, { onClick: $setup.central }, {
							default: _withCtx(() => [..._cache[58] || (_cache[58] = [_createTextVNode(
								"集中清单",
								-1
								/* CACHED */
							)])]),
							_: 1
						}),
						_createVNode(_component_el_button, { onClick: _cache[0] || (_cache[0] = ($event) => $setup.router.push({
							path: "/hrm-payroll/governance",
							query: { projectId: $setup.projectId }
						})) }, {
							default: _withCtx(() => [..._cache[59] || (_cache[59] = [_createTextVNode(
								"业务决议与发布",
								-1
								/* CACHED */
							)])]),
							_: 1
						}),
						_createVNode(_component_el_button, { onClick: $setup.openSourceWorkbench }, {
							default: _withCtx(() => [..._cache[60] || (_cache[60] = [_createTextVNode(
								"实际来源与核验",
								-1
								/* CACHED */
							)])]),
							_: 1
						}),
						_createVNode(_component_el_button, { onClick: $setup.original }, {
							default: _withCtx(() => [..._cache[61] || (_cache[61] = [_createTextVNode(
								"原有正式业务入口",
								-1
								/* CACHED */
							)])]),
							_: 1
						})
					]),
					_createElementVNode(
						"p",
						null,
						_toDisplayString($setup.project?.name || "未选择项目") + " · 项目 " + _toDisplayString($setup.projectId || "待选择") + " · 范围版本 " + _toDisplayString($setup.project?.projectScopeVersion ?? "尚未定义"),
						1
						/* TEXT */
					),
					_createElementVNode(
						"p",
						null,
						"范围：" + _toDisplayString($setup.project?.scopeJson || "尚未定义或当前权限不可见；不据此推定全项目范围"),
						1
						/* TEXT */
					),
					_createElementVNode("nav", _hoisted_3, [(_openBlock(true), _createElementBlock(
						_Fragment,
						null,
						_renderList($setup.order, (m) => {
							return _openBlock(), _createBlock(_component_el_button, {
								key: m,
								type: m === $setup.module ? "primary" : "default",
								onClick: ($event) => $setup.go(m)
							}, {
								default: _withCtx(() => [_createTextVNode(
									_toDisplayString($setup.modules[m]),
									1
									/* TEXT */
								)]),
								_: 2
							}, 1032, ["type", "onClick"]);
						}),
						128
						/* KEYED_FRAGMENT */
					))]),
					_createElementVNode("div", _hoisted_4, [
						_createVNode(_component_el_button, {
							disabled: $setup.step === 0,
							onClick: _cache[1] || (_cache[1] = ($event) => $setup.go($setup.order[$setup.step - 1]))
						}, {
							default: _withCtx(() => [..._cache[62] || (_cache[62] = [_createTextVNode(
								"上一步",
								-1
								/* CACHED */
							)])]),
							_: 1
						}, 8, ["disabled"]),
						_createElementVNode(
							"span",
							null,
							"第 " + _toDisplayString($setup.step + 1) + "/10 视图",
							1
							/* TEXT */
						),
						_createVNode(_component_el_button, {
							disabled: $setup.step === 9,
							onClick: _cache[2] || (_cache[2] = ($event) => $setup.go($setup.order[$setup.step + 1]))
						}, {
							default: _withCtx(() => [..._cache[63] || (_cache[63] = [_createTextVNode(
								"下一步",
								-1
								/* CACHED */
							)])]),
							_: 1
						}, 8, ["disabled"]),
						(_openBlock(true), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.view.related, (m) => {
								return _openBlock(), _createBlock(_component_el_button, {
									key: m,
									link: "",
									onClick: ($event) => $setup.go(m)
								}, {
									default: _withCtx(() => [_createTextVNode(
										"关联：" + _toDisplayString($setup.modules[m]),
										1
										/* TEXT */
									)]),
									_: 2
								}, 1032, ["onClick"]);
							}),
							128
							/* KEYED_FRAGMENT */
						))
					]),
					_createElementVNode(
						"p",
						null,
						_toDisplayString($setup.view.purpose),
						1
						/* TEXT */
					),
					_createElementVNode("section", _hoisted_5, [
						_cache[71] || (_cache[71] = _createElementVNode(
							"h2",
							null,
							"业务场景与字段",
							-1
							/* CACHED */
						)),
						_createElementVNode(
							"p",
							null,
							_toDisplayString($setup.demo.employee) + " · " + _toDisplayString($setup.demo.department) + " · " + _toDisplayString($setup.demo.period) + "；全为虚构，未绑定员工档案。",
							1
							/* TEXT */
						),
						_createElementVNode("div", _hoisted_6, [_createElementVNode("table", null, [_createElementVNode("thead", null, [_createElementVNode("tr", null, [(_openBlock(true), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.view.columns, (c) => {
								return _openBlock(), _createElementBlock(
									"th",
									{ key: c },
									_toDisplayString(c),
									1
									/* TEXT */
								);
							}),
							128
							/* KEYED_FRAGMENT */
						))])]), _createElementVNode("tbody", null, [(_openBlock(true), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.view.rows, (row, i) => {
								return _openBlock(), _createElementBlock("tr", { key: i }, [(_openBlock(true), _createElementBlock(
									_Fragment,
									null,
									_renderList(row, (cell, k) => {
										return _openBlock(), _createElementBlock(
											"td",
											{ key: k },
											_toDisplayString(cell),
											1
											/* TEXT */
										);
									}),
									128
									/* KEYED_FRAGMENT */
								))]);
							}),
							128
							/* KEYED_FRAGMENT */
						))])])]),
						(_openBlock(true), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.view.notes, (note) => {
								return _openBlock(), _createElementBlock(
									"p",
									{ key: note },
									_toDisplayString(note),
									1
									/* TEXT */
								);
							}),
							128
							/* KEYED_FRAGMENT */
						)),
						_createElementVNode("div", {
							id: `field-${$setup.view.field}`,
							tabindex: "-1",
							class: "anchor"
						}, [
							_createElementVNode(
								"strong",
								null,
								_toDisplayString($setup.view.fieldLabel),
								1
								/* TEXT */
							),
							_createVNode(_component_el_button, {
								disabled: !$setup.canCreate,
								onClick: _cache[3] || (_cache[3] = ($event) => $setup.newOpinion("FIELD", `field-${$setup.view.field}`))
							}, {
								default: _withCtx(() => [_createTextVNode(
									"补充字段说明：" + _toDisplayString($setup.view.fieldLabel),
									1
									/* TEXT */
								)]),
								_: 1
							}, 8, ["disabled"]),
							_createVNode(_component_el_button, {
								disabled: !$setup.canCreate,
								onClick: _cache[4] || (_cache[4] = ($event) => $setup.newOpinion("FIELD", `field-${$setup.view.field}`))
							}, {
								default: _withCtx(() => [..._cache[64] || (_cache[64] = [_createTextVNode(
									"这个字段从哪里来",
									-1
									/* CACHED */
								)])]),
								_: 1
							}, 8, ["disabled"])
						], 8, _hoisted_7),
						_createElementVNode("div", _hoisted_8, [
							_createVNode(_component_el_button, { onClick: $setup.simulate }, {
								default: _withCtx(() => [..._cache[65] || (_cache[65] = [_createTextVNode(
									"演示核算",
									-1
									/* CACHED */
								)])]),
								_: 1
							}),
							_createVNode(_component_el_button, { onClick: _cache[5] || (_cache[5] = ($event) => $setup.demoState = "演示复核完成，仅页面内存状态；未提交正式审批") }, {
								default: _withCtx(() => [..._cache[66] || (_cache[66] = [_createTextVNode(
									"演示复核",
									-1
									/* CACHED */
								)])]),
								_: 1
							}),
							_createVNode(_component_el_button, { onClick: _cache[6] || (_cache[6] = ($event) => $setup.slipVisible = true) }, {
								default: _withCtx(() => [..._cache[67] || (_cache[67] = [_createTextVNode(
									"预览示例工资条",
									-1
									/* CACHED */
								)])]),
								_: 1
							}),
							_createVNode(_component_el_button, {
								disabled: !$setup.canCreate,
								onClick: _cache[7] || (_cache[7] = ($event) => $setup.newOpinion("OPERATION", "operation-simulate"))
							}, {
								default: _withCtx(() => [..._cache[68] || (_cache[68] = [_createTextVNode(
									"补充操作规则或例外",
									-1
									/* CACHED */
								)])]),
								_: 1
							}, 8, ["disabled"])
						]),
						_createElementVNode(
							"p",
							_hoisted_9,
							_toDisplayString($setup.demoState),
							1
							/* TEXT */
						),
						_createElementVNode("div", _hoisted_10, [_cache[70] || (_cache[70] = _createTextVNode(
							"示例流程：准备 → 试算 → 复核 → 发放讨论 → 归档讨论 ",
							-1
							/* CACHED */
						)), _createVNode(_component_el_button, {
							disabled: !$setup.canCreate,
							onClick: _cache[8] || (_cache[8] = ($event) => $setup.newOpinion("FLOW", "flow-approval"))
						}, {
							default: _withCtx(() => [..._cache[69] || (_cache[69] = [_createTextVNode(
								"补充流程角色与异常",
								-1
								/* CACHED */
							)])]),
							_: 1
						}, 8, ["disabled"])])
					]),
					$setup.module === "OV" || $setup.module === "RPT" ? (_openBlock(), _createElementBlock("section", _hoisted_11, [
						_createElementVNode(
							"h2",
							null,
							_toDisplayString($setup.module === "OV" ? "虚构应发期间趋势" : "虚构用工成本结构"),
							1
							/* TEXT */
						),
						_cache[72] || (_cache[72] = _createElementVNode(
							"p",
							null,
							"示例图仅讨论口径；10月应发11750、成本13850，与主样例一致。历史趋势不是实际工资数据。",
							-1
							/* CACHED */
						)),
						(_openBlock(true), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.module === "OV" ? $setup.demoTrend.map((x) => ({
								name: x.period,
								amount: x.gross
							})) : $setup.demoCostStructure, (item) => {
								return _openBlock(), _createElementBlock("div", {
									key: item.name,
									class: "source-card"
								}, [_createElementVNode(
									"strong",
									null,
									_toDisplayString(item.name) + " · " + _toDisplayString(item.amount) + " 元",
									1
									/* TEXT */
								), _createElementVNode("div", _hoisted_12, [_createElementVNode(
									"div",
									{
										class: "demo-bar",
										style: _normalizeStyle({ width: `${item.amount / 14e3 * 100}%` })
									},
									null,
									4
									/* STYLE */
								)])]);
							}),
							128
							/* KEYED_FRAGMENT */
						))
					])) : _createCommentVNode("v-if", true),
					$setup.module === "PLAN" ? (_openBlock(), _createElementBlock("section", _hoisted_13, [
						_cache[75] || (_cache[75] = _createElementVNode(
							"h2",
							null,
							"虚构薪档与调薪比对",
							-1
							/* CACHED */
						)),
						_cache[76] || (_cache[76] = _createElementVNode(
							"p",
							null,
							"当前V1：TEST-A/TEST-01，基本10000元；V2仅为下期讨论候选，主链仍用V1。",
							-1
							/* CACHED */
						)),
						_createElementVNode("div", _hoisted_14, [
							_createElementVNode(
								"span",
								null,
								_toDisplayString($setup.demo.employee),
								1
								/* TEXT */
							),
							_createVNode(_component_el_input_number, {
								modelValue: $setup.demoAdjustment,
								"onUpdate:modelValue": _cache[9] || (_cache[9] = ($event) => $setup.demoAdjustment = $event),
								"aria-label": "示例调薪金额",
								min: 0,
								precision: 2
							}, null, 8, ["modelValue"]),
							_createVNode(_component_el_button, { onClick: _cache[10] || (_cache[10] = ($event) => $setup.demoState = `示例V2新金额${$setup.demoAdjustment}元，2026-11-01拟生效；仅页面预览，不保存正式档案或提交审批，本期仍10000元。`) }, {
								default: _withCtx(() => [..._cache[73] || (_cache[73] = [_createTextVNode(
									"预览演示调薪",
									-1
									/* CACHED */
								)])]),
								_: 1
							}),
							_createVNode(_component_el_button, {
								disabled: !$setup.canCreate,
								onClick: _cache[11] || (_cache[11] = ($event) => $setup.newOpinion("OPERATION", "operation-adjustment"))
							}, {
								default: _withCtx(() => [..._cache[74] || (_cache[74] = [_createTextVNode(
									"补充调薪操作意见",
									-1
									/* CACHED */
								)])]),
								_: 1
							}, 8, ["disabled"])
						]),
						_createElementVNode(
							"p",
							null,
							_toDisplayString($setup.demoState),
							1
							/* TEXT */
						)
					])) : _createCommentVNode("v-if", true),
					_createElementVNode("section", _hoisted_15, [_cache[77] || (_cache[77] = _createElementVNode(
						"h2",
						null,
						"逐项功能场景与例外（示例）",
						-1
						/* CACHED */
					)), (_openBlock(true), _createElementBlock(
						_Fragment,
						null,
						_renderList($setup.reviewTopics[$setup.module], (topic) => {
							return _openBlock(), _createElementBlock("div", {
								id: topic.anchor,
								key: topic.code,
								tabindex: "-1",
								class: "source-card anchor"
							}, [
								_createElementVNode(
									"strong",
									null,
									_toDisplayString(topic.code) + " · " + _toDisplayString(topic.title),
									1
									/* TEXT */
								),
								_createElementVNode(
									"p",
									null,
									"正常/预期：" + _toDisplayString(topic.normal),
									1
									/* TEXT */
								),
								_createElementVNode(
									"p",
									null,
									"异常/待确认：" + _toDisplayString(topic.exception),
									1
									/* TEXT */
								),
								_createVNode(_component_el_button, {
									disabled: !$setup.canCreate,
									onClick: ($event) => $setup.newOpinion("FIELD", topic.anchor)
								}, {
									default: _withCtx(() => [_createTextVNode(
										"围绕" + _toDisplayString(topic.code) + "提意见",
										1
										/* TEXT */
									)]),
									_: 2
								}, 1032, ["disabled", "onClick"])
							], 8, _hoisted_16);
						}),
						128
						/* KEYED_FRAGMENT */
					))]),
					$setup.error ? (_openBlock(), _createBlock(_component_el_alert, {
						key: 2,
						title: $setup.error,
						type: "error",
						closable: false
					}, null, 8, ["title"])) : _createCommentVNode("v-if", true),
					$setup.loading ? (_openBlock(), _createElementBlock("p", _hoisted_17, "正在读取真实评审数据，未返回内容不推定为空或通过。")) : _createCommentVNode("v-if", true),
					_createElementVNode("section", _hoisted_18, [
						_cache[91] || (_cache[91] = _createElementVNode(
							"h2",
							null,
							"功能需求与意见征集（真实保存）",
							-1
							/* CACHED */
						)),
						_createElementVNode("div", _hoisted_19, [
							_createVNode(_component_el_button, {
								disabled: !$setup.canCreate,
								type: "primary",
								onClick: _cache[12] || (_cache[12] = ($event) => $setup.newOpinion("MODULE", "module"))
							}, {
								default: _withCtx(() => [..._cache[78] || (_cache[78] = [_createTextVNode(
									"补充本模块需求",
									-1
									/* CACHED */
								)])]),
								_: 1
							}, 8, ["disabled"]),
							_createVNode(_component_el_button, {
								disabled: !$setup.capabilities.FULL_EDIT,
								onClick: $setup.initialize
							}, {
								default: _withCtx(() => [..._cache[79] || (_cache[79] = [_createTextVNode(
									"初始化46项候选模板",
									-1
									/* CACHED */
								)])]),
								_: 1
							}, 8, ["disabled"]),
							_withDirectives((_openBlock(), _createBlock(_component_el_button, {
								disabled: !$setup.projectId,
								onClick: $setup.download
							}, {
								default: _withCtx(() => [..._cache[80] || (_cache[80] = [_createTextVNode(
									"导出当前筛选评审清单",
									-1
									/* CACHED */
								)])]),
								_: 1
							}, 8, ["disabled"])), [[_directive_hasPermi, ["hrm:payroll:requirement:export"]]]),
							_withDirectives((_openBlock(), _createBlock(_component_el_button, {
								disabled: !$setup.selected.length,
								onClick: _cache[13] || (_cache[13] = ($event) => $setup.batchVisible = true)
							}, {
								default: _withCtx(() => [..._cache[81] || (_cache[81] = [_createTextVNode(
									"批量分派所选需求",
									-1
									/* CACHED */
								)])]),
								_: 1
							}, 8, ["disabled"])), [[_directive_hasPermi, ["hrm:payroll:requirement:assign"]]])
						]),
						_createElementVNode("div", _hoisted_20, [
							_createVNode(_component_el_input, {
								modelValue: $setup.filters.text,
								"onUpdate:modelValue": _cache[14] || (_cache[14] = ($event) => $setup.filters.text = $event),
								"aria-label": "查找需求",
								placeholder: "编号或功能文本",
								clearable: "",
								onKeyup: _withKeys($setup.search, ["enter"])
							}, null, 8, ["modelValue"]),
							_createVNode(_component_el_select, {
								modelValue: $setup.filters.status,
								"onUpdate:modelValue": _cache[15] || (_cache[15] = ($event) => $setup.filters.status = $event),
								"aria-label": "评审状态筛选",
								clearable: "",
								placeholder: "全部评审状态"
							}, {
								default: _withCtx(() => [(_openBlock(), _createElementBlock(
									_Fragment,
									null,
									_renderList($setup.statuses, (name, key) => {
										return _createVNode(_component_el_option, {
											key,
											label: name,
											value: key
										}, null, 8, ["label", "value"]);
									}),
									64
									/* STABLE_FRAGMENT */
								))]),
								_: 1
							}, 8, ["modelValue"]),
							_createVNode(_component_el_select, {
								modelValue: $setup.filters.decision,
								"onUpdate:modelValue": _cache[16] || (_cache[16] = ($event) => $setup.filters.decision = $event),
								"aria-label": "范围决策筛选",
								clearable: "",
								placeholder: "全部范围决策"
							}, {
								default: _withCtx(() => [(_openBlock(), _createElementBlock(
									_Fragment,
									null,
									_renderList($setup.decisions, (name, key) => {
										return _createVNode(_component_el_option, {
											key,
											label: name,
											value: key
										}, null, 8, ["label", "value"]);
									}),
									64
									/* STABLE_FRAGMENT */
								))]),
								_: 1
							}, 8, ["modelValue"]),
							_createVNode(_component_el_select, {
								modelValue: $setup.sort,
								"onUpdate:modelValue": _cache[17] || (_cache[17] = ($event) => $setup.sort = $event),
								"aria-label": "排序"
							}, {
								default: _withCtx(() => [
									_createVNode(_component_el_option, {
										label: "编号升序",
										value: "code"
									}),
									_createVNode(_component_el_option, {
										label: "编号降序",
										value: "-code"
									}),
									_createVNode(_component_el_option, {
										label: "版本降序",
										value: "-version"
									})
								]),
								_: 1
							}, 8, ["modelValue"]),
							_createVNode(_component_el_button, { onClick: $setup.search }, {
								default: _withCtx(() => [..._cache[82] || (_cache[82] = [_createTextVNode(
									"应用筛选",
									-1
									/* CACHED */
								)])]),
								_: 1
							}),
							_createVNode(_component_el_button, { onClick: $setup.clearFilters }, {
								default: _withCtx(() => [..._cache[83] || (_cache[83] = [_createTextVNode(
									"清除筛选",
									-1
									/* CACHED */
								)])]),
								_: 1
							})
						]),
						!$setup.loading && !$setup.error && !$setup.rows.length ? (_openBlock(), _createElementBlock("div", _hoisted_21, "当前筛选暂无可见需求；不代表全项目无需求。")) : _createCommentVNode("v-if", true),
						!$setup.loading && !$setup.error ? (_openBlock(), _createElementBlock("div", _hoisted_22, [_createVNode(_component_el_table, {
							data: $setup.rows,
							"row-key": "id",
							onSelectionChange: _cache[18] || (_cache[18] = ($event) => $setup.selected = $event)
						}, {
							default: _withCtx(() => [
								_createVNode(_component_el_table_column, {
									type: "selection",
									width: "45"
								}),
								_createVNode(_component_el_table_column, {
									prop: "id",
									label: "需求ID",
									width: "85"
								}),
								_createVNode(_component_el_table_column, {
									prop: "code",
									label: "编号",
									width: "130"
								}),
								_createVNode(_component_el_table_column, {
									prop: "description",
									label: "功能/意见",
									"min-width": "240"
								}),
								_createVNode(_component_el_table_column, {
									label: "功能评审",
									width: "110"
								}, {
									default: _withCtx((s) => [_createTextVNode(
										_toDisplayString($setup.statuses[s.row.status]) + _toDisplayString(s.row.status === "CONFIRMED" && !s.row.currentConfirmationValid ? "（当前绑定失效）" : ""),
										1
										/* TEXT */
									)]),
									_: 1
								}),
								_createVNode(_component_el_table_column, {
									label: "范围决策",
									width: "105"
								}, {
									default: _withCtx((s) => [_createTextVNode(
										_toDisplayString($setup.decisions[s.row.scopeDecision || "CANDIDATE"]),
										1
										/* TEXT */
									)]),
									_: 1
								}),
								_createVNode(_component_el_table_column, {
									label: "来源",
									width: "100"
								}, {
									default: _withCtx((s) => [_createTextVNode(
										_toDisplayString($setup.readiness[s.row.readiness]),
										1
										/* TEXT */
									)]),
									_: 1
								}),
								_createVNode(_component_el_table_column, {
									label: "实施",
									width: "110"
								}, {
									default: _withCtx((s) => [_createTextVNode(
										_toDisplayString($setup.implementations[$setup.parse(s.row.implementationJson).state || "UNSCHEDULED"]),
										1
										/* TEXT */
									)]),
									_: 1
								}),
								_createVNode(_component_el_table_column, {
									prop: "version",
									label: "版本",
									width: "70"
								}),
								_createVNode(_component_el_table_column, {
									label: "操作",
									"min-width": "380"
								}, {
									default: _withCtx((s) => [
										_createVNode(_component_el_button, {
											link: "",
											disabled: !$setup.allowed(s.row, "EDIT"),
											onClick: ($event) => $setup.edit(s.row)
										}, {
											default: _withCtx(() => [..._cache[84] || (_cache[84] = [_createTextVNode(
												"编辑意见",
												-1
												/* CACHED */
											)])]),
											_: 1
										}, 8, ["disabled", "onClick"]),
										_createVNode(_component_el_button, {
											link: "",
											onClick: ($event) => $setup.showHistory(s.row)
										}, {
											default: _withCtx(() => [..._cache[85] || (_cache[85] = [_createTextVNode(
												"历史/比较",
												-1
												/* CACHED */
											)])]),
											_: 1
										}, 8, ["onClick"]),
										_createVNode(_component_el_button, {
											link: "",
											onClick: ($event) => $setup.returnTo(s.row)
										}, {
											default: _withCtx(() => [..._cache[86] || (_cache[86] = [_createTextVNode(
												"返回原位置",
												-1
												/* CACHED */
											)])]),
											_: 1
										}, 8, ["onClick"]),
										_withDirectives((_openBlock(), _createBlock(_component_el_button, {
											link: "",
											disabled: !$setup.allowed(s.row, "SCOPE_DECIDE"),
											onClick: ($event) => $setup.openAction(s.row, "decision")
										}, {
											default: _withCtx(() => [..._cache[87] || (_cache[87] = [_createTextVNode(
												"范围决策",
												-1
												/* CACHED */
											)])]),
											_: 1
										}, 8, ["disabled", "onClick"])), [[_directive_hasPermi, ["hrm:payroll:requirement:scope-decide"]]]),
										_createVNode(_component_el_button, {
											link: "",
											disabled: !$setup.allowed(s.row, "EDIT"),
											onClick: ($event) => $setup.openAction(s.row, "implementation")
										}, {
											default: _withCtx(() => [..._cache[88] || (_cache[88] = [_createTextVNode(
												"实施关联",
												-1
												/* CACHED */
											)])]),
											_: 1
										}, 8, ["disabled", "onClick"]),
										_createVNode(_component_el_button, {
											link: "",
											disabled: !$setup.allowed(s.row, "EDIT"),
											onClick: ($event) => $setup.openAction(s.row, "collaboration")
										}, {
											default: _withCtx(() => [..._cache[89] || (_cache[89] = [_createTextVNode(
												"会议与站内提醒",
												-1
												/* CACHED */
											)])]),
											_: 1
										}, 8, ["disabled", "onClick"]),
										_withDirectives((_openBlock(), _createBlock(_component_el_button, {
											link: "",
											disabled: !$setup.allowed(s.row, "CONFIRM") || s.row.status === "CONFIRMED",
											onClick: ($event) => $setup.confirm(s.row)
										}, {
											default: _withCtx(() => [..._cache[90] || (_cache[90] = [_createTextVNode(
												"本人确认",
												-1
												/* CACHED */
											)])]),
											_: 1
										}, 8, ["disabled", "onClick"])), [[_directive_hasPermi, ["hrm:payroll:requirement:confirm"]]])
									]),
									_: 1
								})
							]),
							_: 1
						}, 8, ["data"])])) : _createCommentVNode("v-if", true),
						!$setup.loading && !$setup.error ? (_openBlock(), _createBlock(_component_el_pagination, {
							key: 2,
							"current-page": $setup.pageNo,
							"onUpdate:currentPage": _cache[19] || (_cache[19] = ($event) => $setup.pageNo = $event),
							"page-size": 10,
							layout: "prev, pager, next, total",
							total: $setup.total,
							onCurrentChange: $setup.load
						}, null, 8, ["current-page", "total"])) : _createCommentVNode("v-if", true)
					]),
					_createElementVNode("section", _hoisted_23, [
						_cache[94] || (_cache[94] = _createElementVNode(
							"h2",
							null,
							"依赖数据来源与盘点",
							-1
							/* CACHED */
						)),
						_cache[95] || (_cache[95] = _createElementVNode(
							"p",
							null,
							"候选目录是征集线索，不能冒充实际已提供来源或质量通过。实际来源需按权限另行登记与核验。",
							-1
							/* CACHED */
						)),
						(_openBlock(true), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.candidates, (source) => {
								return _openBlock(), _createElementBlock("div", {
									key: source.id,
									class: "source-card"
								}, [_createElementVNode(
									"strong",
									null,
									_toDisplayString(source.id) + " · " + _toDisplayString(source.name),
									1
									/* TEXT */
								), _createElementVNode(
									"p",
									null,
									_toDisplayString(source.fields.join("、")) + "；候选确认方：" + _toDisplayString(source.owner),
									1
									/* TEXT */
								)]);
							}),
							128
							/* KEYED_FRAGMENT */
						)),
						_createElementVNode("label", null, [_cache[92] || (_cache[92] = _createTextVNode(
							"当前可见实际来源 ",
							-1
							/* CACHED */
						)), _createVNode(_component_el_select, {
							modelValue: $setup.sourceId,
							"onUpdate:modelValue": _cache[20] || (_cache[20] = ($event) => $setup.sourceId = $event),
							"aria-label": "选择实际来源",
							clearable: "",
							placeholder: "无来源时保持缺口"
						}, {
							default: _withCtx(() => [(_openBlock(true), _createElementBlock(
								_Fragment,
								null,
								_renderList($setup.sources, (source) => {
									return _openBlock(), _createBlock(_component_el_option, {
										key: source.id,
										label: `${source.id} · ${source.data.name}`,
										value: source.id
									}, null, 8, ["label", "value"]);
								}),
								128
								/* KEYED_FRAGMENT */
							))]),
							_: 1
						}, 8, ["modelValue"])]),
						$setup.sourceId ? (_openBlock(), _createElementBlock(
							"p",
							_hoisted_24,
							_toDisplayString($setup.sourceDescription),
							1
							/* TEXT */
						)) : _createCommentVNode("v-if", true),
						_createVNode(_component_el_button, { onClick: $setup.openSourceWorkbench }, {
							default: _withCtx(() => [..._cache[93] || (_cache[93] = [_createTextVNode(
								"维护实际来源、映射和QS/QC",
								-1
								/* CACHED */
							)])]),
							_: 1
						})
					]),
					_createElementVNode("section", _hoisted_25, [
						_cache[97] || (_cache[97] = _createElementVNode(
							"h2",
							null,
							"我的站内提醒（无外发）",
							-1
							/* CACHED */
						)),
						(_openBlock(true), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.reminders, (note, i) => {
								return _openBlock(), _createElementBlock("div", {
									key: i,
									class: "source-card"
								}, [
									_createElementVNode(
										"strong",
										null,
										_toDisplayString(note.code) + " · 需求" + _toDisplayString(note.requirementId),
										1
										/* TEXT */
									),
									_createElementVNode(
										"p",
										null,
										_toDisplayString(note.reference) + " · " + _toDisplayString(note.time),
										1
										/* TEXT */
									),
									_createVNode(_component_el_button, { onClick: ($event) => $setup.go(note.module, note.anchor) }, {
										default: _withCtx(() => [..._cache[96] || (_cache[96] = [_createTextVNode(
											"返回提醒原位置",
											-1
											/* CACHED */
										)])]),
										_: 1
									}, 8, ["onClick"])
								]);
							}),
							128
							/* KEYED_FRAGMENT */
						)),
						!$setup.loading && !$setup.error && !$setup.reminders.length ? (_openBlock(), _createElementBlock("p", _hoisted_26, "当前有权范围内暂无发给我的站内提醒；会议纪要及全部协作历史可在需求历史中查看。")) : _createCommentVNode("v-if", true)
					]),
					_createElementVNode("section", _hoisted_27, [
						_cache[99] || (_cache[99] = _createElementVNode(
							"h2",
							null,
							"范围、确认及来源统计（服务器集合）",
							-1
							/* CACHED */
						)),
						_cache[100] || (_cache[100] = _createElementVNode(
							"p",
							null,
							"U全部需求（含移除）；E未移除；I纳入本期；C当前确认有效；T暂缓；D有异议。暂缓与异议仍计入有效集合，范围决策不代替确认。",
							-1
							/* CACHED */
						)),
						$setup.stats ? (_openBlock(), _createElementBlock(
							"p",
							_hoisted_28,
							_toDisplayString($setup.stats.label) + " · 筛选条件 " + _toDisplayString(JSON.stringify($setup.stats.filters)),
							1
							/* TEXT */
						)) : _createCommentVNode("v-if", true),
						$setup.stats ? (_openBlock(), _createElementBlock("div", _hoisted_29, [(_openBlock(), _createElementBlock(
							_Fragment,
							null,
							_renderList([
								"U",
								"E",
								"I",
								"C",
								"T",
								"D"
							], (key) => {
								return _createElementVNode("button", {
									key,
									class: "stat",
									onClick: ($event) => $setup.drill(key)
								}, _toDisplayString(key) + " · " + _toDisplayString($setup.stats.counts[key]) + " · 查看集合", 9, _hoisted_30);
							}),
							64
							/* STABLE_FRAGMENT */
						)), (_openBlock(), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.ratioKeys, (key) => {
								return _createElementVNode(
									"div",
									{
										key,
										class: "stat"
									},
									_toDisplayString($setup.ratioLabels[key]) + "：" + _toDisplayString($setup.stats[key].label),
									1
									/* TEXT */
								);
							}),
							64
							/* STABLE_FRAGMENT */
						))])) : _createCommentVNode("v-if", true),
						$setup.stats ? (_openBlock(), _createElementBlock(
							"p",
							_hoisted_31,
							"去重实际来源 " + _toDisplayString($setup.stats.sourceCount) + "；有效不适用 " + _toDisplayString($setup.stats.exempt) + "；未决问题 " + _toDisplayString($setup.stats.unresolvedIssues) + "，逾期 " + _toDisplayString($setup.stats.overdueIssues) + "。关系和需求缺口以独立ID计数。",
							1
							/* TEXT */
						)) : _createCommentVNode("v-if", true),
						$setup.stats?.projectTotals ? (_openBlock(), _createElementBlock(
							"p",
							_hoisted_32,
							"全项目功能确认 " + _toDisplayString($setup.stats.projectTotals.confirmation.label) + "，本期确认 " + _toDisplayString($setup.stats.projectTotals.includedConfirmation.label) + "；当前筛选不改变全项目门槛。",
							1
							/* TEXT */
						)) : _createCommentVNode("v-if", true),
						_cache[101] || (_cache[101] = _createElementVNode(
							"p",
							null,
							"当前统计不是发布成功或已实施证明；业务决议、发布检查与六表快照请使用独立入口核实。受限账号只展示可见集合，隐藏对象数量不输出。",
							-1
							/* CACHED */
						)),
						_createVNode(_component_el_button, {
							onClick: _cache[21] || (_cache[21] = ($event) => $setup.drill("L")),
							disabled: !$setup.stats
						}, {
							default: _withCtx(() => [..._cache[98] || (_cache[98] = [_createTextVNode(
								"查看关系与缺口L",
								-1
								/* CACHED */
							)])]),
							_: 1
						}, 8, ["disabled"])
					])
				]),
				_: 1
			}),
			_createVNode(_component_el_dialog, {
				class: "hrm-review-dialog",
				modelValue: $setup.formVisible,
				"onUpdate:modelValue": _cache[34] || (_cache[34] = ($event) => $setup.formVisible = $event),
				title: "页面意见与业务场景",
				width: "min(880px, 96vw)",
				"close-on-click-modal": false,
				onClosed: $setup.resetFocus
			}, {
				footer: _withCtx(() => [_createElementVNode("div", _hoisted_35, [
					_createVNode(_component_el_button, { onClick: _cache[33] || (_cache[33] = ($event) => $setup.formVisible = false) }, {
						default: _withCtx(() => [..._cache[103] || (_cache[103] = [_createTextVNode(
							"取消",
							-1
							/* CACHED */
						)])]),
						_: 1
					}),
					_createVNode(_component_el_button, {
						disabled: !$setup.form.id,
						onClick: $setup.reloadConflict
					}, {
						default: _withCtx(() => [..._cache[104] || (_cache[104] = [_createTextVNode(
							"读取最新版本（保留当前输入）",
							-1
							/* CACHED */
						)])]),
						_: 1
					}, 8, ["disabled"]),
					_createVNode(_component_el_button, {
						type: "primary",
						loading: $setup.saving,
						onClick: $setup.saveOpinion
					}, {
						default: _withCtx(() => [..._cache[105] || (_cache[105] = [_createTextVNode(
							"保存意见",
							-1
							/* CACHED */
						)])]),
						_: 1
					}, 8, ["loading"])
				])]),
				default: _withCtx(() => [
					_createElementVNode(
						"p",
						null,
						"项目 " + _toDisplayString($setup.form.projectId) + " · " + _toDisplayString($setup.form.module) + " · " + _toDisplayString($setup.business.targetType) + " · " + _toDisplayString($setup.business.anchor) + "。草稿可逐步补齐；评审中须场景/期望，确认还须角色/目标、正常异常、规则/单位/精度/有效期/例外、验收依据。",
						1
						/* TEXT */
					),
					$setup.saveError ? (_openBlock(), _createBlock(_component_el_alert, {
						key: 0,
						title: $setup.saveError,
						type: "error",
						closable: false
					}, null, 8, ["title"])) : _createCommentVNode("v-if", true),
					$setup.conflictLatest ? (_openBlock(), _createElementBlock("details", _hoisted_33, [_cache[102] || (_cache[102] = _createElementVNode(
						"summary",
						null,
						"最新保存内容（与保留输入人工合并）",
						-1
						/* CACHED */
					)), _createElementVNode(
						"pre",
						null,
						_toDisplayString(JSON.stringify($setup.conflictLatest, null, 2)),
						1
						/* TEXT */
					)])) : _createCommentVNode("v-if", true),
					_createVNode(_component_el_form, { "label-position": "top" }, {
						default: _withCtx(() => [
							_createVNode(_component_el_form_item, {
								label: "功能说明",
								error: $setup.opinionErrors.description ? "请填写功能说明" : ""
							}, {
								default: _withCtx(() => [_createVNode(_component_el_input, {
									modelValue: $setup.form.description,
									"onUpdate:modelValue": _cache[22] || (_cache[22] = ($event) => $setup.form.description = $event),
									"aria-label": "功能说明",
									type: "textarea",
									maxlength: "1000"
								}, null, 8, ["modelValue"])]),
								_: 1
							}, 8, ["error"]),
							_createElementVNode("div", _hoisted_34, [(_openBlock(), _createElementBlock(
								_Fragment,
								null,
								_renderList($setup.businessFields, (field) => {
									return _createVNode(_component_el_form_item, {
										key: field.key,
										label: field.label,
										error: $setup.opinionErrors[field.key] ? `请填写${field.label}` : ""
									}, {
										default: _withCtx(() => [_createVNode(_component_el_input, {
											modelValue: $setup.business[field.key],
											"onUpdate:modelValue": ($event) => $setup.business[field.key] = $event,
											"aria-label": field.label,
											type: "textarea"
										}, null, 8, [
											"modelValue",
											"onUpdate:modelValue",
											"aria-label"
										])]),
										_: 2
									}, 1032, ["label", "error"]);
								}),
								64
								/* STABLE_FRAGMENT */
							))]),
							$setup.form.scopeMode === "SUBSET" ? (_openBlock(), _createBlock($setup["ScopeFields"], {
								key: 0,
								modelValue: $setup.form.scopeJson,
								"onUpdate:modelValue": _cache[23] || (_cache[23] = ($event) => $setup.form.scopeJson = $event)
							}, null, 8, ["modelValue"])) : _createCommentVNode("v-if", true),
							_createVNode(_component_el_form_item, { label: "范围方式" }, {
								default: _withCtx(() => [_createVNode(_component_el_select, {
									modelValue: $setup.form.scopeMode,
									"onUpdate:modelValue": _cache[24] || (_cache[24] = ($event) => $setup.form.scopeMode = $event),
									"aria-label": "范围方式"
								}, {
									default: _withCtx(() => [_createVNode(_component_el_option, {
										label: "继承项目范围",
										value: "INHERIT"
									}), _createVNode(_component_el_option, {
										label: "显式子集",
										value: "SUBSET"
									})]),
									_: 1
								}, 8, ["modelValue"])]),
								_: 1
							}),
							_createVNode(_component_el_form_item, { label: "来源适用性" }, {
								default: _withCtx(() => [_createVNode(_component_el_select, {
									modelValue: $setup.form.sourceApplicability,
									"onUpdate:modelValue": _cache[25] || (_cache[25] = ($event) => $setup.form.sourceApplicability = $event),
									"aria-label": "来源适用性"
								}, {
									default: _withCtx(() => [_createVNode(_component_el_option, {
										label: "适用",
										value: "APPLICABLE"
									}), _createVNode(_component_el_option, {
										label: "不适用",
										value: "NOT_APPLICABLE"
									})]),
									_: 1
								}, 8, ["modelValue"])]),
								_: 1
							}),
							$setup.form.sourceApplicability === "NOT_APPLICABLE" ? (_openBlock(), _createBlock(_component_el_form_item, {
								key: 1,
								label: "不适用理由",
								error: $setup.opinionErrors.applicabilityReason ? "请填写至少8个字符的具体不适用理由" : ""
							}, {
								default: _withCtx(() => [_createVNode(_component_el_input, {
									modelValue: $setup.form.applicabilityReason,
									"onUpdate:modelValue": _cache[26] || (_cache[26] = ($event) => $setup.form.applicabilityReason = $event),
									"aria-label": "不适用理由",
									type: "textarea"
								}, null, 8, ["modelValue"])]),
								_: 1
							}, 8, ["error"])) : _createCommentVNode("v-if", true),
							_createVNode(_component_el_form_item, {
								label: "规则引用版本",
								error: $setup.opinionErrors.ruleVersion ? "请填写规则引用版本" : ""
							}, {
								default: _withCtx(() => [_createVNode(_component_el_input, {
									modelValue: $setup.form.ruleVersion,
									"onUpdate:modelValue": _cache[27] || (_cache[27] = ($event) => $setup.form.ruleVersion = $event),
									"aria-label": "规则引用版本"
								}, null, 8, ["modelValue"])]),
								_: 1
							}, 8, ["error"]),
							_createVNode(_component_el_form_item, {
								label: "验收条件",
								error: $setup.opinionErrors.acceptance ? "请填写验收条件" : ""
							}, {
								default: _withCtx(() => [_createVNode(_component_el_input, {
									modelValue: $setup.form.acceptance,
									"onUpdate:modelValue": _cache[28] || (_cache[28] = ($event) => $setup.form.acceptance = $event),
									"aria-label": "验收条件",
									type: "textarea"
								}, null, 8, ["modelValue"])]),
								_: 1
							}, 8, ["error"]),
							_createVNode(_component_el_form_item, {
								label: "依据或异议说明",
								error: $setup.opinionErrors.evidence ? "请填写依据或异议说明" : ""
							}, {
								default: _withCtx(() => [_createVNode(_component_el_input, {
									modelValue: $setup.form.evidence,
									"onUpdate:modelValue": _cache[29] || (_cache[29] = ($event) => $setup.form.evidence = $event),
									"aria-label": "依据或异议说明",
									type: "textarea"
								}, null, 8, ["modelValue"])]),
								_: 1
							}, 8, ["error"]),
							_createVNode(_component_el_form_item, { label: "功能评审状态" }, {
								default: _withCtx(() => [_createVNode(_component_el_select, {
									modelValue: $setup.form.status,
									"onUpdate:modelValue": _cache[30] || (_cache[30] = ($event) => $setup.form.status = $event),
									"aria-label": "功能评审状态"
								}, {
									default: _withCtx(() => [
										_createVNode(_component_el_option, {
											label: "待确认（草稿）",
											value: "PENDING"
										}),
										_createVNode(_component_el_option, {
											label: "评审中",
											value: "REVIEWING"
										}),
										_createVNode(_component_el_option, {
											label: "有异议",
											value: "DISPUTED"
										})
									]),
									_: 1
								}, 8, ["modelValue"])]),
								_: 1
							}),
							$setup.form.status === "DISPUTED" ? (_openBlock(), _createElementBlock(
								_Fragment,
								{ key: 2 },
								[_createVNode(_component_el_form_item, {
									label: "异议责任人ID",
									error: $setup.opinionErrors.disputeOwnerId ? "请填写异议责任人ID" : ""
								}, {
									default: _withCtx(() => [_createVNode(_component_el_input_number, {
										modelValue: $setup.business.disputeOwnerId,
										"onUpdate:modelValue": _cache[31] || (_cache[31] = ($event) => $setup.business.disputeOwnerId = $event),
										"aria-label": "异议责任人ID",
										min: 1
									}, null, 8, ["modelValue"])]),
									_: 1
								}, 8, ["error"]), _createVNode(_component_el_form_item, {
									label: "异议下一步",
									error: $setup.opinionErrors.nextStep ? "请填写异议下一步" : ""
								}, {
									default: _withCtx(() => [_createVNode(_component_el_input, {
										modelValue: $setup.business.nextStep,
										"onUpdate:modelValue": _cache[32] || (_cache[32] = ($event) => $setup.business.nextStep = $event),
										"aria-label": "异议下一步"
									}, null, 8, ["modelValue"])]),
									_: 1
								}, 8, ["error"])],
								64
								/* STABLE_FRAGMENT */
							)) : _createCommentVNode("v-if", true)
						]),
						_: 1
					})
				]),
				_: 1
			}, 8, ["modelValue"]),
			_createVNode(_component_el_dialog, {
				class: "hrm-review-dialog",
				modelValue: $setup.actionVisible,
				"onUpdate:modelValue": _cache[46] || (_cache[46] = ($event) => $setup.actionVisible = $event),
				title: $setup.actionTitles[$setup.actionKind],
				width: "min(680px, 96vw)",
				"close-on-click-modal": false
			}, {
				footer: _withCtx(() => [_createElementVNode("div", _hoisted_36, [_createVNode(_component_el_button, { onClick: _cache[45] || (_cache[45] = ($event) => $setup.actionVisible = false) }, {
					default: _withCtx(() => [..._cache[108] || (_cache[108] = [_createTextVNode(
						"取消",
						-1
						/* CACHED */
					)])]),
					_: 1
				}), _createVNode(_component_el_button, {
					type: "primary",
					loading: $setup.saving,
					onClick: $setup.saveAction
				}, {
					default: _withCtx(() => [..._cache[109] || (_cache[109] = [_createTextVNode(
						"保存独立操作",
						-1
						/* CACHED */
					)])]),
					_: 1
				}, 8, ["loading"])])]),
				default: _withCtx(() => [
					_createElementVNode(
						"p",
						null,
						"需求 " + _toDisplayString($setup.active?.id) + " · 当前版本 " + _toDisplayString($setup.active?.version) + "；操作独立留历史，不代替功能确认。",
						1
						/* TEXT */
					),
					$setup.saveError ? (_openBlock(), _createBlock(_component_el_alert, {
						key: 0,
						title: $setup.saveError,
						type: "error",
						closable: false
					}, null, 8, ["title"])) : _createCommentVNode("v-if", true),
					_createVNode(_component_el_form, { "label-position": "top" }, {
						default: _withCtx(() => [$setup.actionKind === "decision" ? (_openBlock(), _createElementBlock(
							_Fragment,
							{ key: 0 },
							[
								_createVNode(_component_el_form_item, { label: "范围决策" }, {
									default: _withCtx(() => [_createVNode(_component_el_select, {
										modelValue: $setup.action.decision,
										"onUpdate:modelValue": _cache[35] || (_cache[35] = ($event) => $setup.action.decision = $event),
										"aria-label": "范围决策"
									}, {
										default: _withCtx(() => [(_openBlock(), _createElementBlock(
											_Fragment,
											null,
											_renderList($setup.decisions, (name, key) => {
												return _createVNode(_component_el_option, {
													key,
													value: key,
													label: name
												}, null, 8, ["value", "label"]);
											}),
											64
											/* STABLE_FRAGMENT */
										))]),
										_: 1
									}, 8, ["modelValue"])]),
									_: 1
								}),
								$setup.action.decision === "DEFERRED" ? (_openBlock(), _createElementBlock(
									_Fragment,
									{ key: 0 },
									[_createVNode(_component_el_form_item, { label: "暂缓责任人ID" }, {
										default: _withCtx(() => [_createVNode(_component_el_input_number, {
											modelValue: $setup.action.responsibleId,
											"onUpdate:modelValue": _cache[36] || (_cache[36] = ($event) => $setup.action.responsibleId = $event),
											"aria-label": "暂缓责任人ID",
											min: 1
										}, null, 8, ["modelValue"])]),
										_: 1
									}), _createVNode(_component_el_form_item, { label: "重新评估条件" }, {
										default: _withCtx(() => [_createVNode(_component_el_input, {
											modelValue: $setup.action.reevaluate,
											"onUpdate:modelValue": _cache[37] || (_cache[37] = ($event) => $setup.action.reevaluate = $event),
											"aria-label": "重新评估条件"
										}, null, 8, ["modelValue"])]),
										_: 1
									})],
									64
									/* STABLE_FRAGMENT */
								)) : _createCommentVNode("v-if", true),
								$setup.action.decision === "REMOVED" ? (_openBlock(), _createElementBlock(
									_Fragment,
									{ key: 1 },
									[_createVNode(_component_el_form_item, { label: "移除类型" }, {
										default: _withCtx(() => [_createVNode(_component_el_select, {
											modelValue: $setup.action.removalKind,
											"onUpdate:modelValue": _cache[38] || (_cache[38] = ($event) => $setup.action.removalKind = $event),
											"aria-label": "移除类型"
										}, {
											default: _withCtx(() => [
												_createVNode(_component_el_option, {
													label: "重复",
													value: "DUPLICATE"
												}),
												_createVNode(_component_el_option, {
													label: "误录",
													value: "MISTAKE"
												}),
												_createVNode(_component_el_option, {
													label: "超出边界",
													value: "OUT_OF_SCOPE"
												})
											]),
											_: 1
										}, 8, ["modelValue"])]),
										_: 1
									}), $setup.action.removalKind === "DUPLICATE" ? (_openBlock(), _createBlock(_component_el_form_item, {
										key: 0,
										label: "同项目保留项ID"
									}, {
										default: _withCtx(() => [_createVNode(_component_el_input_number, {
											modelValue: $setup.action.replacementId,
											"onUpdate:modelValue": _cache[39] || (_cache[39] = ($event) => $setup.action.replacementId = $event),
											"aria-label": "同项目保留项ID",
											min: 1
										}, null, 8, ["modelValue"])]),
										_: 1
									})) : _createCommentVNode("v-if", true)],
									64
									/* STABLE_FRAGMENT */
								)) : _createCommentVNode("v-if", true)
							],
							64
							/* STABLE_FRAGMENT */
						)) : $setup.actionKind === "implementation" ? (_openBlock(), _createElementBlock(
							_Fragment,
							{ key: 1 },
							[
								_createVNode(_component_el_form_item, { label: "实施状态" }, {
									default: _withCtx(() => [_createVNode(_component_el_select, {
										modelValue: $setup.action.state,
										"onUpdate:modelValue": _cache[40] || (_cache[40] = ($event) => $setup.action.state = $event),
										"aria-label": "实施状态"
									}, {
										default: _withCtx(() => [(_openBlock(), _createElementBlock(
											_Fragment,
											null,
											_renderList($setup.implementations, (name, key) => {
												return _createVNode(_component_el_option, {
													key,
													label: name,
													value: key
												}, null, 8, ["label", "value"]);
											}),
											64
											/* STABLE_FRAGMENT */
										))]),
										_: 1
									}, 8, ["modelValue"])]),
									_: 1
								}),
								(_openBlock(), _createElementBlock(
									_Fragment,
									null,
									_renderList($setup.implementationFields, (field) => {
										return _createVNode(_component_el_form_item, {
											key: field.key,
											label: field.label
										}, {
											default: _withCtx(() => [_createVNode(_component_el_input, {
												modelValue: $setup.action[field.key],
												"onUpdate:modelValue": ($event) => $setup.action[field.key] = $event,
												"aria-label": field.label
											}, null, 8, [
												"modelValue",
												"onUpdate:modelValue",
												"aria-label"
											])]),
											_: 2
										}, 1032, ["label"]);
									}),
									64
									/* STABLE_FRAGMENT */
								)),
								_cache[106] || (_cache[106] = _createElementVNode(
									"p",
									null,
									"只记录人工关联和声明，引用存在不证明验收通过；系统不自动核实外部结论。",
									-1
									/* CACHED */
								))
							],
							64
							/* STABLE_FRAGMENT */
						)) : (_openBlock(), _createElementBlock(
							_Fragment,
							{ key: 2 },
							[
								_createVNode(_component_el_form_item, { label: "协作类型" }, {
									default: _withCtx(() => [_createVNode(_component_el_select, {
										modelValue: $setup.action.kind,
										"onUpdate:modelValue": _cache[41] || (_cache[41] = ($event) => $setup.action.kind = $event),
										"aria-label": "协作类型"
									}, {
										default: _withCtx(() => [_createVNode(_component_el_option, {
											label: "关联会议纪要",
											value: "MEETING"
										}), _createVNode(_component_el_option, {
											label: "站内提醒",
											value: "REMINDER"
										})]),
										_: 1
									}, 8, ["modelValue"])]),
									_: 1
								}),
								_createVNode(_component_el_form_item, { label: "纪要或提醒内容引用" }, {
									default: _withCtx(() => [_createVNode(_component_el_input, {
										modelValue: $setup.action.reference,
										"onUpdate:modelValue": _cache[42] || (_cache[42] = ($event) => $setup.action.reference = $event),
										"aria-label": "纪要或提醒内容引用",
										type: "textarea"
									}, null, 8, ["modelValue"])]),
									_: 1
								}),
								$setup.action.kind === "REMINDER" ? (_openBlock(), _createBlock(_component_el_form_item, {
									key: 0,
									label: "站内接收人ID"
								}, {
									default: _withCtx(() => [_createVNode(_component_el_input_number, {
										modelValue: $setup.action.recipientId,
										"onUpdate:modelValue": _cache[43] || (_cache[43] = ($event) => $setup.action.recipientId = $event),
										"aria-label": "站内接收人ID",
										min: 1
									}, null, 8, ["modelValue"])]),
									_: 1
								})) : _createCommentVNode("v-if", true),
								_cache[107] || (_cache[107] = _createElementVNode(
									"p",
									null,
									"提醒仅保存到需求协作记录，对有权接收人可见；不调用短信、邮件或正式消息外发。",
									-1
									/* CACHED */
								))
							],
							64
							/* STABLE_FRAGMENT */
						)), _createVNode(_component_el_form_item, { label: "本次操作依据" }, {
							default: _withCtx(() => [_createVNode(_component_el_input, {
								modelValue: $setup.action.reason,
								"onUpdate:modelValue": _cache[44] || (_cache[44] = ($event) => $setup.action.reason = $event),
								"aria-label": "本次操作依据",
								type: "textarea"
							}, null, 8, ["modelValue"])]),
							_: 1
						})]),
						_: 1
					})
				]),
				_: 1
			}, 8, ["modelValue", "title"]),
			_createVNode(_component_el_dialog, {
				class: "hrm-review-dialog",
				modelValue: $setup.historyVisible,
				"onUpdate:modelValue": _cache[50] || (_cache[50] = ($event) => $setup.historyVisible = $event),
				title: "真实历史与版本差异",
				width: "min(900px, 96vw)"
			}, {
				footer: _withCtx(() => [_createVNode(_component_el_button, { onClick: _cache[49] || (_cache[49] = ($event) => $setup.historyVisible = false) }, {
					default: _withCtx(() => [..._cache[111] || (_cache[111] = [_createTextVNode(
						"关闭历史",
						-1
						/* CACHED */
					)])]),
					_: 1
				})]),
				default: _withCtx(() => [
					_createElementVNode("div", _hoisted_37, [_createVNode(_component_el_select, {
						modelValue: $setup.compareA,
						"onUpdate:modelValue": _cache[47] || (_cache[47] = ($event) => $setup.compareA = $event),
						"aria-label": "比较旧版"
					}, {
						default: _withCtx(() => [(_openBlock(true), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.histories, (h) => {
								return _openBlock(), _createBlock(_component_el_option, {
									key: h.id,
									label: `版本${h.version}`,
									value: h.version
								}, null, 8, ["label", "value"]);
							}),
							128
							/* KEYED_FRAGMENT */
						))]),
						_: 1
					}, 8, ["modelValue"]), _createVNode(_component_el_select, {
						modelValue: $setup.compareB,
						"onUpdate:modelValue": _cache[48] || (_cache[48] = ($event) => $setup.compareB = $event),
						"aria-label": "比较新版"
					}, {
						default: _withCtx(() => [(_openBlock(true), _createElementBlock(
							_Fragment,
							null,
							_renderList($setup.histories, (h) => {
								return _openBlock(), _createBlock(_component_el_option, {
									key: h.id,
									label: `版本${h.version}`,
									value: h.version
								}, null, 8, ["label", "value"]);
							}),
							128
							/* KEYED_FRAGMENT */
						))]),
						_: 1
					}, 8, ["modelValue"])]),
					_createElementVNode("div", _hoisted_38, [_createElementVNode("table", null, [_cache[110] || (_cache[110] = _createElementVNode(
						"thead",
						null,
						[_createElementVNode("tr", null, [
							_createElementVNode("th", null, "字段"),
							_createElementVNode("th", null, "旧版"),
							_createElementVNode("th", null, "新版")
						])],
						-1
						/* CACHED */
					)), _createElementVNode("tbody", null, [(_openBlock(true), _createElementBlock(
						_Fragment,
						null,
						_renderList($setup.differences, (diff) => {
							return _openBlock(), _createElementBlock("tr", { key: diff.key }, [
								_createElementVNode(
									"td",
									null,
									_toDisplayString(diff.key),
									1
									/* TEXT */
								),
								_createElementVNode(
									"td",
									null,
									_toDisplayString(diff.before),
									1
									/* TEXT */
								),
								_createElementVNode(
									"td",
									null,
									_toDisplayString(diff.after),
									1
									/* TEXT */
								)
							]);
						}),
						128
						/* KEYED_FRAGMENT */
					))])])]),
					(_openBlock(true), _createElementBlock(
						_Fragment,
						null,
						_renderList($setup.histories, (h) => {
							return _openBlock(), _createElementBlock("div", {
								key: h.id,
								class: "source-card"
							}, [_createElementVNode(
								"strong",
								null,
								"版本" + _toDisplayString(h.version) + " · 操作人" + _toDisplayString(h.actorId),
								1
								/* TEXT */
							), _createElementVNode(
								"pre",
								null,
								_toDisplayString(JSON.stringify($setup.parse(h.snapshot), null, 2)),
								1
								/* TEXT */
							)]);
						}),
						128
						/* KEYED_FRAGMENT */
					))
				]),
				_: 1
			}, 8, ["modelValue"]),
			_createVNode(_component_el_dialog, {
				class: "hrm-review-dialog",
				modelValue: $setup.drillVisible,
				"onUpdate:modelValue": _cache[52] || (_cache[52] = ($event) => $setup.drillVisible = $event),
				title: "统计集合下钻",
				width: "min(760px, 96vw)"
			}, {
				footer: _withCtx(() => [_createVNode(_component_el_button, { onClick: _cache[51] || (_cache[51] = ($event) => $setup.drillVisible = false) }, {
					default: _withCtx(() => [..._cache[112] || (_cache[112] = [_createTextVNode(
						"关闭集合",
						-1
						/* CACHED */
					)])]),
					_: 1
				})]),
				default: _withCtx(() => [_createElementVNode(
					"pre",
					null,
					_toDisplayString($setup.drillData),
					1
					/* TEXT */
				)]),
				_: 1
			}, 8, ["modelValue"]),
			_createVNode(_component_el_dialog, {
				class: "hrm-review-dialog",
				modelValue: $setup.batchVisible,
				"onUpdate:modelValue": _cache[54] || (_cache[54] = ($event) => $setup.batchVisible = $event),
				title: "逐对象批量分派",
				width: "min(620px, 96vw)"
			}, {
				default: _withCtx(() => [
					_cache[114] || (_cache[114] = _createElementVNode(
						"p",
						null,
						"每条独立检查分派人动作、目标资格和版本；部分失败不撤销其他合法成功。",
						-1
						/* CACHED */
					)),
					_createVNode(_component_el_input_number, {
						modelValue: $setup.batchReviewer,
						"onUpdate:modelValue": _cache[53] || (_cache[53] = ($event) => $setup.batchReviewer = $event),
						"aria-label": "批量评审负责人ID",
						min: 1
					}, null, 8, ["modelValue"]),
					_createVNode(_component_el_button, {
						loading: $setup.saving,
						onClick: $setup.assignBatch
					}, {
						default: _withCtx(() => [..._cache[113] || (_cache[113] = [_createTextVNode(
							"执行批量分派",
							-1
							/* CACHED */
						)])]),
						_: 1
					}, 8, ["loading"]),
					_createElementVNode(
						"pre",
						null,
						_toDisplayString($setup.batchResult),
						1
						/* TEXT */
					)
				]),
				_: 1
			}, 8, ["modelValue"]),
			_createVNode(_component_el_dialog, {
				class: "hrm-review-dialog",
				modelValue: $setup.slipVisible,
				"onUpdate:modelValue": _cache[56] || (_cache[56] = ($event) => $setup.slipVisible = $event),
				title: "虚构工资条预览",
				width: "min(620px, 96vw)"
			}, {
				footer: _withCtx(() => [_createVNode(_component_el_button, { onClick: _cache[55] || (_cache[55] = ($event) => $setup.slipVisible = false) }, {
					default: _withCtx(() => [..._cache[115] || (_cache[115] = [_createTextVNode(
						"关闭工资条预览",
						-1
						/* CACHED */
					)])]),
					_: 1
				})]),
				default: _withCtx(() => [
					_createElementVNode(
						"p",
						null,
						"示例数据，仅用于讨论 · " + _toDisplayString($setup.demo.employee) + " · " + _toDisplayString($setup.demo.period),
						1
						/* TEXT */
					),
					_createElementVNode(
						"p",
						null,
						"应发 " + _toDisplayString($setup.result.gross) + " 元；个人扣款 " + _toDisplayString($setup.result.personal) + " 元；税 " + _toDisplayString($setup.result.tax) + " 元；实发 " + _toDisplayString($setup.result.net) + " 元。",
						1
						/* TEXT */
					),
					_createElementVNode(
						"p",
						null,
						"单位成本 " + _toDisplayString($setup.result.cost) + " 元。未产生真实工资条、投递、审批或付款。",
						1
						/* TEXT */
					)
				]),
				_: 1
			}, 8, ["modelValue"])
		],
		64
		/* STABLE_FRAGMENT */
	);
}
import "/src/views/hrm/payroll/review/index.vue?t=1792391042793&vue&type=style&index=0&scoped=329d63d4&lang.css";
_sfc_main.__hmrId = "329d63d4";
typeof __VUE_HMR_RUNTIME__ !== "undefined" && __VUE_HMR_RUNTIME__.createRecord(_sfc_main.__hmrId, _sfc_main);
import.meta.hot.on("file-changed", ({ file }) => {
	__VUE_HMR_RUNTIME__.CHANGED_FILE = file;
});
export const _rerender_only = __VUE_HMR_RUNTIME__.CHANGED_FILE === "/Users/waitforpro/source/github-yudao/ruoyi-vue-pro/yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue";
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
	["__scopeId", "data-v-329d63d4"],
	["__file", "/Users/waitforpro/source/github-yudao/ruoyi-vue-pro/yudao-ui/yudao-ui-admin-vue3/src/views/hrm/payroll/review/index.vue"]
]);

//# sourceMappingURL=data:application/json;base64,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OzZCQUFjO01BQUo7TUFBSTs7S0FBQTs7OzRCQUQvQyxDQUREO0tBQ0M7S0FBQTtLQUFBLGlCQURPLGdCQUFTO0tBQUE7O0lBQUE7OztHQUtuQixhQVdZO0lBVlYsT0FBTTtnQkFDRzttRUFBQSxzQkFBWTtJQUNyQixPQUFNO0lBQ04sT0FBTTs7NEJBRUw7bUNBREE7TUFDQTtNQUFBO01BREc7TUFBa0M7O0tBQUE7S0FDckMsYUFBMkU7a0JBQWpEO3FFQUFBLHVCQUFhO01BQUUsY0FBVztNQUFhLEtBQUs7O0tBQUssYUFJM0U7TUFIRSxTQUFTO01BQ1QsU0FBTzs7OEJBQ0Q7T0FBTjtPQUFNOztNQUFBOzs7S0FDUjtNQUE0QjtNQUFBO01BQUEsaUJBQXBCLGtCQUFXO01BQUE7O0tBQUE7Ozs7R0FFdEIsYUFhQztJQVpDLE9BQU07Z0JBQ0c7bUVBQUEscUJBQVc7SUFDcEIsT0FBTTtJQUNOLE9BQU07O0lBTU0sUUFBTSxlQUM0QyxDQUEzRCxhQUEyRCx3QkFBL0MsU0FBSyx3Q0FBRSxxQkFBVzs2QkFBaUI7TUFBUDtNQUFPOztLQUFBOzs7NEJBTGpEO0tBREE7TUFDQTtNQUFBO01BREcsa0JBQWEsaUJBQUcsWUFBSyxRQUFRLElBQUcsUUFBRyxpQkFBRyxZQUFLLE1BQU07TUFBQTs7S0FBQTtLQUNwRDtNQUdBO01BQUE7TUFGRSxRQUFHLGlCQUFHLGNBQU8sS0FBSyxJQUFHLGFBQVEsaUJBQUcsY0FBTyxRQUFRLElBQUcsVUFBSyxpQkFBRyxjQUFPLEdBQUcsSUFBRyxXQUN4RSxpQkFBRyxjQUFPLEdBQUcsSUFBRztNQUFHOztLQUFBO0tBQ3BCO01BQ0E7TUFBQTtNQURHLFVBQUssaUJBQUcsY0FBTyxJQUFJLElBQUc7TUFBcUI7O0tBQUEiLCJuYW1lcyI6W10sImlnbm9yZUxpc3QiOltdLCJzb3VyY2VzIjpbImluZGV4LnZ1ZSJdLCJzb3VyY2VzQ29udGVudCI6WyI8dGVtcGxhdGU+XG4gIDxDb250ZW50V3JhcCBjbGFzcz1cInJldmlldy1wYWdlXCI+XG4gICAgPGgxIHRhYmluZGV4PVwiLTFcIiBpZD1cInJldmlldy1oZWFkaW5nXCI+6Jaq6YWs6ZyA5rGC6K+E5a6h5bel5L2c5Y+wIMK3IHt7IG1vZHVsZXNbbW9kdWxlXSB9fTwvaDE+XG4gICAgPGVsLWFsZXJ0XG4gICAgICB0aXRsZT1cIuekuuS+i+aVsOaNru+8jOS7heeUqOS6juiuqOiuuuOAguaEj+ingeS4juivhOWuoeiusOW9leecn+WunuS/neWtmO+8m+a8lOekuuaguOeul+OAgeWuoeaJueWPiuW3pei1hOadoeWPquS9nOeUqOS6juiZmuaehOagt+S+i+OAglwiXG4gICAgICB0eXBlPVwid2FybmluZ1wiXG4gICAgICA6Y2xvc2FibGU9XCJmYWxzZVwiXG4gICAgLz5cbiAgICA8ZGl2IGNsYXNzPVwidG9vbGJhclwiPlxuICAgICAgPGVsLXNlbGVjdFxuICAgICAgICBhcmlhLWxhYmVsPVwi6K+E5a6h6aG555uuXCJcbiAgICAgICAgOm1vZGVsLXZhbHVlPVwicHJvamVjdElkXCJcbiAgICAgICAgcGxhY2Vob2xkZXI9XCLpgInmi6nmnInmnYPor4TlrqHpobnnm65cIlxuICAgICAgICBAY2hhbmdlPVwic2VsZWN0UHJvamVjdFwiXG4gICAgICA+XG4gICAgICAgIDxlbC1vcHRpb24gdi1mb3I9XCJwIGluIHByb2plY3RzXCIgOmtleT1cInAuaWRcIiA6bGFiZWw9XCJgJHtwLmlkfSDCtyAke3AubmFtZX1gXCIgOnZhbHVlPVwicC5pZFwiIC8+XG4gICAgICA8L2VsLXNlbGVjdD5cbiAgICAgIDxlbC1idXR0b24gQGNsaWNrPVwibG9hZFwiPuWIt+aWsOW3peS9nOWPsDwvZWwtYnV0dG9uPlxuICAgICAgPGVsLWJ1dHRvbiBAY2xpY2s9XCJjZW50cmFsXCI+6ZuG5Lit5riF5Y2VPC9lbC1idXR0b24+XG4gICAgICA8ZWwtYnV0dG9uIEBjbGljaz1cInJvdXRlci5wdXNoKHsgcGF0aDogJy9ocm0tcGF5cm9sbC9nb3Zlcm5hbmNlJywgcXVlcnk6IHsgcHJvamVjdElkIH0gfSlcIlxuICAgICAgICA+5Lia5Yqh5Yaz6K6u5LiO5Y+R5biDPC9lbC1idXR0b25cbiAgICAgID5cbiAgICAgIDxlbC1idXR0b24gQGNsaWNrPVwib3BlblNvdXJjZVdvcmtiZW5jaFwiPuWunumZheadpea6kOS4juaguOmqjDwvZWwtYnV0dG9uPlxuICAgICAgPGVsLWJ1dHRvbiBAY2xpY2s9XCJvcmlnaW5hbFwiPuWOn+acieato+W8j+S4muWKoeWFpeWPozwvZWwtYnV0dG9uPlxuICAgIDwvZGl2PlxuICAgIDxwXG4gICAgICA+e3sgcHJvamVjdD8ubmFtZSB8fCAn5pyq6YCJ5oup6aG555uuJyB9fSDCtyDpobnnm64ge3sgcHJvamVjdElkIHx8ICflvoXpgInmi6knIH19IMK3IOiMg+WbtOeJiOacrFxuICAgICAge3sgcHJvamVjdD8ucHJvamVjdFNjb3BlVmVyc2lvbiA/PyAn5bCa5pyq5a6a5LmJJyB9fTwvcFxuICAgID5cbiAgICA8cD7ojIPlm7TvvJp7eyBwcm9qZWN0Py5zY29wZUpzb24gfHwgJ+WwmuacquWumuS5ieaIluW9k+WJjeadg+mZkOS4jeWPr+inge+8m+S4jeaNruatpOaOqOWumuWFqOmhueebruiMg+WbtCcgfX08L3A+XG4gICAgPG5hdiBhcmlhLWxhYmVsPVwi5Y2B6KeG5Zu+6K+E5a6h5a+86IiqXCIgY2xhc3M9XCJ2aWV3LW5hdlwiPlxuICAgICAgPGVsLWJ1dHRvblxuICAgICAgICB2LWZvcj1cIm0gaW4gb3JkZXJcIlxuICAgICAgICA6a2V5PVwibVwiXG4gICAgICAgIDp0eXBlPVwibSA9PT0gbW9kdWxlID8gJ3ByaW1hcnknIDogJ2RlZmF1bHQnXCJcbiAgICAgICAgQGNsaWNrPVwiZ28obSlcIlxuICAgICAgICA+e3sgbW9kdWxlc1ttXSB9fTwvZWwtYnV0dG9uXG4gICAgICA+XG4gICAgPC9uYXY+XG4gICAgPGRpdiBjbGFzcz1cInRvb2xiYXJcIj5cbiAgICAgIDxlbC1idXR0b24gOmRpc2FibGVkPVwic3RlcCA9PT0gMFwiIEBjbGljaz1cImdvKG9yZGVyW3N0ZXAgLSAxXSlcIj7kuIrkuIDmraU8L2VsLWJ1dHRvbj5cbiAgICAgIDxzcGFuPuesrCB7eyBzdGVwICsgMSB9fS8xMCDop4blm748L3NwYW4+XG4gICAgICA8ZWwtYnV0dG9uIDpkaXNhYmxlZD1cInN0ZXAgPT09IDlcIiBAY2xpY2s9XCJnbyhvcmRlcltzdGVwICsgMV0pXCI+5LiL5LiA5q2lPC9lbC1idXR0b24+XG4gICAgICA8ZWwtYnV0dG9uIHYtZm9yPVwibSBpbiB2aWV3LnJlbGF0ZWRcIiA6a2V5PVwibVwiIGxpbmsgQGNsaWNrPVwiZ28obSlcIlxuICAgICAgICA+5YWz6IGU77yae3sgbW9kdWxlc1ttXSB9fTwvZWwtYnV0dG9uXG4gICAgICA+XG4gICAgPC9kaXY+XG4gICAgPHA+e3sgdmlldy5wdXJwb3NlIH19PC9wPlxuICAgIDxzZWN0aW9uIGFyaWEtbGFiZWw9XCLomZrmnoTkuJrliqHlsZXnpLpcIj5cbiAgICAgIDxoMj7kuJrliqHlnLrmma/kuI7lrZfmrrU8L2gyPlxuICAgICAgPHBcbiAgICAgICAgPnt7IGRlbW8uZW1wbG95ZWUgfX0gwrcge3sgZGVtby5kZXBhcnRtZW50IH19IMK3XG4gICAgICAgIHt7IGRlbW8ucGVyaW9kIH1977yb5YWo5Li66Jma5p6E77yM5pyq57uR5a6a5ZGY5bel5qGj5qGI44CCPC9wXG4gICAgICA+XG4gICAgICA8ZGl2IGNsYXNzPVwid2lkZS10YWJsZVwiIHRhYmluZGV4PVwiMFwiIGFyaWEtbGFiZWw9XCLnpLrkvovkuJrliqHooajvvIzlt6blj7PmlrnlkJHlj6/mqKrlkJHmu5rliqhcIj5cbiAgICAgICAgPHRhYmxlXG4gICAgICAgICAgPjx0aGVhZFxuICAgICAgICAgICAgPjx0clxuICAgICAgICAgICAgICA+PHRoIHYtZm9yPVwiYyBpbiB2aWV3LmNvbHVtbnNcIiA6a2V5PVwiY1wiPnt7IGMgfX08L3RoPjwvdHJcbiAgICAgICAgICAgID48L3RoZWFkXG4gICAgICAgICAgPlxuICAgICAgICAgIDx0Ym9keVxuICAgICAgICAgICAgPjx0ciB2LWZvcj1cIihyb3csIGkpIGluIHZpZXcucm93c1wiIDprZXk9XCJpXCJcbiAgICAgICAgICAgICAgPjx0ZCB2LWZvcj1cIihjZWxsLCBrKSBpbiByb3dcIiA6a2V5PVwia1wiPnt7IGNlbGwgfX08L3RkPjwvdHJcbiAgICAgICAgICAgID48L3Rib2R5XG4gICAgICAgICAgPjwvdGFibGVcbiAgICAgICAgPlxuICAgICAgPC9kaXY+XG4gICAgICA8cCB2LWZvcj1cIm5vdGUgaW4gdmlldy5ub3Rlc1wiIDprZXk9XCJub3RlXCI+e3sgbm90ZSB9fTwvcD5cbiAgICAgIDxkaXYgOmlkPVwiYGZpZWxkLSR7dmlldy5maWVsZH1gXCIgdGFiaW5kZXg9XCItMVwiIGNsYXNzPVwiYW5jaG9yXCI+XG4gICAgICAgIDxzdHJvbmc+e3sgdmlldy5maWVsZExhYmVsIH19PC9zdHJvbmc+XG4gICAgICAgIDxlbC1idXR0b24gOmRpc2FibGVkPVwiIWNhbkNyZWF0ZVwiIEBjbGljaz1cIm5ld09waW5pb24oJ0ZJRUxEJywgYGZpZWxkLSR7dmlldy5maWVsZH1gKVwiXG4gICAgICAgICAgPuihpeWFheWtl+auteivtOaYju+8mnt7IHZpZXcuZmllbGRMYWJlbCB9fTwvZWwtYnV0dG9uXG4gICAgICAgID5cbiAgICAgICAgPGVsLWJ1dHRvbiA6ZGlzYWJsZWQ9XCIhY2FuQ3JlYXRlXCIgQGNsaWNrPVwibmV3T3BpbmlvbignRklFTEQnLCBgZmllbGQtJHt2aWV3LmZpZWxkfWApXCJcbiAgICAgICAgICA+6L+Z5Liq5a2X5q615LuO5ZOq6YeM5p2lPC9lbC1idXR0b25cbiAgICAgICAgPlxuICAgICAgPC9kaXY+XG4gICAgICA8ZGl2IGlkPVwib3BlcmF0aW9uLXNpbXVsYXRlXCIgdGFiaW5kZXg9XCItMVwiIGNsYXNzPVwidG9vbGJhciBhbmNob3JcIj5cbiAgICAgICAgPGVsLWJ1dHRvbiBAY2xpY2s9XCJzaW11bGF0ZVwiPua8lOekuuaguOeulzwvZWwtYnV0dG9uXG4gICAgICAgID48ZWwtYnV0dG9uIEBjbGljaz1cImRlbW9TdGF0ZSA9ICfmvJTnpLrlpI3moLjlrozmiJDvvIzku4XpobXpnaLlhoXlrZjnirbmgIHvvJvmnKrmj5DkuqTmraPlvI/lrqHmibknXCJcbiAgICAgICAgICA+5ryU56S65aSN5qC4PC9lbC1idXR0b25cbiAgICAgICAgPlxuICAgICAgICA8ZWwtYnV0dG9uIEBjbGljaz1cInNsaXBWaXNpYmxlID0gdHJ1ZVwiPumihOiniOekuuS+i+W3pei1hOadoTwvZWwtYnV0dG9uPlxuICAgICAgICA8ZWwtYnV0dG9uIDpkaXNhYmxlZD1cIiFjYW5DcmVhdGVcIiBAY2xpY2s9XCJuZXdPcGluaW9uKCdPUEVSQVRJT04nLCAnb3BlcmF0aW9uLXNpbXVsYXRlJylcIlxuICAgICAgICAgID7ooaXlhYXmk43kvZzop4TliJnmiJbkvovlpJY8L2VsLWJ1dHRvblxuICAgICAgICA+XG4gICAgICA8L2Rpdj5cbiAgICAgIDxwIHJvbGU9XCJzdGF0dXNcIj57eyBkZW1vU3RhdGUgfX08L3A+XG4gICAgICA8ZGl2IGlkPVwiZmxvdy1hcHByb3ZhbFwiIHRhYmluZGV4PVwiLTFcIiBjbGFzcz1cImFuY2hvclwiXG4gICAgICAgID7npLrkvovmtYHnqIvvvJrlh4blpIcg4oaSIOivleeulyDihpIg5aSN5qC4IOKGkiDlj5HmlL7orqjorrog4oaSIOW9kuaho+iuqOiuulxuICAgICAgICA8ZWwtYnV0dG9uIDpkaXNhYmxlZD1cIiFjYW5DcmVhdGVcIiBAY2xpY2s9XCJuZXdPcGluaW9uKCdGTE9XJywgJ2Zsb3ctYXBwcm92YWwnKVwiXG4gICAgICAgICAgPuihpeWFhea1geeoi+inkuiJsuS4juW8guW4uDwvZWwtYnV0dG9uXG4gICAgICAgID5cbiAgICAgIDwvZGl2PlxuICAgIDwvc2VjdGlvbj5cbiAgICA8c2VjdGlvbiB2LWlmPVwibW9kdWxlID09PSAnT1YnIHx8IG1vZHVsZSA9PT0gJ1JQVCdcIiBhcmlhLWxhYmVsPVwi6Jma5p6E5Zu+6KGo56S65L6LXCI+XG4gICAgICA8aDI+e3sgbW9kdWxlID09PSAnT1YnID8gJ+iZmuaehOW6lOWPkeacn+mXtOi2i+WKvycgOiAn6Jma5p6E55So5bel5oiQ5pys57uT5p6EJyB9fTwvaDI+XG4gICAgICA8cD7npLrkvovlm77ku4Xorqjorrrlj6PlvoTvvJsxMOaciOW6lOWPkTExNzUw44CB5oiQ5pysMTM4NTDvvIzkuI7kuLvmoLfkvovkuIDoh7TjgILljoblj7Lotovlir/kuI3mmK/lrp7pmYXlt6XotYTmlbDmja7jgII8L3A+XG4gICAgICA8ZGl2XG4gICAgICAgIHYtZm9yPVwiaXRlbSBpbiBtb2R1bGUgPT09ICdPVidcbiAgICAgICAgICA/IGRlbW9UcmVuZC5tYXAoKHgpID0+ICh7IG5hbWU6IHgucGVyaW9kLCBhbW91bnQ6IHguZ3Jvc3MgfSkpXG4gICAgICAgICAgOiBkZW1vQ29zdFN0cnVjdHVyZVwiXG4gICAgICAgIDprZXk9XCJpdGVtLm5hbWVcIlxuICAgICAgICBjbGFzcz1cInNvdXJjZS1jYXJkXCJcbiAgICAgID5cbiAgICAgICAgPHN0cm9uZz57eyBpdGVtLm5hbWUgfX0gwrcge3sgaXRlbS5hbW91bnQgfX0g5YWDPC9zdHJvbmdcbiAgICAgICAgPjxkaXYgY2xhc3M9XCJkZW1vLWJhci10cmFja1wiXG4gICAgICAgICAgPjxkaXYgY2xhc3M9XCJkZW1vLWJhclwiIDpzdHlsZT1cInsgd2lkdGg6IGAkeyhpdGVtLmFtb3VudCAvIDE0MDAwKSAqIDEwMH0lYCB9XCI+PC9kaXZcbiAgICAgICAgPjwvZGl2PlxuICAgICAgPC9kaXY+XG4gICAgPC9zZWN0aW9uPlxuICAgIDxzZWN0aW9uXG4gICAgICB2LWlmPVwibW9kdWxlID09PSAnUExBTidcIlxuICAgICAgaWQ9XCJvcGVyYXRpb24tYWRqdXN0bWVudFwiXG4gICAgICB0YWJpbmRleD1cIi0xXCJcbiAgICAgIGFyaWEtbGFiZWw9XCLomZrmnoTosIPolqrooajljZVcIlxuICAgID5cbiAgICAgIDxoMj7omZrmnoTolqrmoaPkuI7osIPolqrmr5Tlr7k8L2gyXG4gICAgICA+PHA+5b2T5YmNVjHvvJpURVNULUEvVEVTVC0wMe+8jOWfuuacrDEwMDAw5YWD77ybVjLku4XkuLrkuIvmnJ/orqjorrrlgJnpgInvvIzkuLvpk77ku43nlKhWMeOAgjwvcD5cbiAgICAgIDxkaXYgY2xhc3M9XCJ0b29sYmFyXCJcbiAgICAgICAgPjxzcGFuPnt7IGRlbW8uZW1wbG95ZWUgfX08L3NwYW5cbiAgICAgICAgPjxlbC1pbnB1dC1udW1iZXJcbiAgICAgICAgICB2LW1vZGVsPVwiZGVtb0FkanVzdG1lbnRcIlxuICAgICAgICAgIGFyaWEtbGFiZWw9XCLnpLrkvovosIPolqrph5Hpop1cIlxuICAgICAgICAgIDptaW49XCIwXCJcbiAgICAgICAgICA6cHJlY2lzaW9uPVwiMlwiXG4gICAgICAgIC8+PGVsLWJ1dHRvblxuICAgICAgICAgIEBjbGljaz1cIlxuICAgICAgICAgICAgZGVtb1N0YXRlID0gYOekuuS+i1Yy5paw6YeR6aKdJHtkZW1vQWRqdXN0bWVudH3lhYPvvIwyMDI2LTExLTAx5ouf55Sf5pWI77yb5LuF6aG16Z2i6aKE6KeI77yM5LiN5L+d5a2Y5q2j5byP5qGj5qGI5oiW5o+Q5Lqk5a6h5om577yM5pys5pyf5LuNMTAwMDDlhYPjgIJgXG4gICAgICAgICAgXCJcbiAgICAgICAgICA+6aKE6KeI5ryU56S66LCD6JaqPC9lbC1idXR0b25cbiAgICAgICAgPjxlbC1idXR0b24gOmRpc2FibGVkPVwiIWNhbkNyZWF0ZVwiIEBjbGljaz1cIm5ld09waW5pb24oJ09QRVJBVElPTicsICdvcGVyYXRpb24tYWRqdXN0bWVudCcpXCJcbiAgICAgICAgICA+6KGl5YWF6LCD6Jaq5pON5L2c5oSP6KeBPC9lbC1idXR0b25cbiAgICAgICAgPjwvZGl2XG4gICAgICA+PHA+e3sgZGVtb1N0YXRlIH19PC9wPlxuICAgIDwvc2VjdGlvbj5cbiAgICA8c2VjdGlvbiBhcmlhLWxhYmVsPVwi5YCZ6YCJ5Yqf6IO96K6o6K665Zy65pmvXCI+XG4gICAgICA8aDI+6YCQ6aG55Yqf6IO95Zy65pmv5LiO5L6L5aSW77yI56S65L6L77yJPC9oMj5cbiAgICAgIDxkaXZcbiAgICAgICAgdi1mb3I9XCJ0b3BpYyBpbiByZXZpZXdUb3BpY3NbbW9kdWxlXVwiXG4gICAgICAgIDppZD1cInRvcGljLmFuY2hvclwiXG4gICAgICAgIDprZXk9XCJ0b3BpYy5jb2RlXCJcbiAgICAgICAgdGFiaW5kZXg9XCItMVwiXG4gICAgICAgIGNsYXNzPVwic291cmNlLWNhcmQgYW5jaG9yXCJcbiAgICAgID5cbiAgICAgICAgPHN0cm9uZz57eyB0b3BpYy5jb2RlIH19IMK3IHt7IHRvcGljLnRpdGxlIH19PC9zdHJvbmdcbiAgICAgICAgPjxwPuato+W4uC/pooTmnJ/vvJp7eyB0b3BpYy5ub3JtYWwgfX08L3BcbiAgICAgICAgPjxwPuW8guW4uC/lvoXnoa7orqTvvJp7eyB0b3BpYy5leGNlcHRpb24gfX08L3A+XG4gICAgICAgIDxlbC1idXR0b24gOmRpc2FibGVkPVwiIWNhbkNyZWF0ZVwiIEBjbGljaz1cIm5ld09waW5pb24oJ0ZJRUxEJywgdG9waWMuYW5jaG9yKVwiXG4gICAgICAgICAgPuWbtOe7lXt7IHRvcGljLmNvZGUgfX3mj5DmhI/op4E8L2VsLWJ1dHRvblxuICAgICAgICA+XG4gICAgICA8L2Rpdj5cbiAgICA8L3NlY3Rpb24+XG4gICAgPGVsLWFsZXJ0IHYtaWY9XCJlcnJvclwiIDp0aXRsZT1cImVycm9yXCIgdHlwZT1cImVycm9yXCIgOmNsb3NhYmxlPVwiZmFsc2VcIiAvPlxuICAgIDxwIHYtaWY9XCJsb2FkaW5nXCIgcm9sZT1cInN0YXR1c1wiPuato+WcqOivu+WPluecn+WunuivhOWuoeaVsOaNru+8jOacqui/lOWbnuWGheWuueS4jeaOqOWumuS4uuepuuaIlumAmui/h+OAgjwvcD5cbiAgICA8c2VjdGlvbiBpZD1cIm1vZHVsZVwiIHRhYmluZGV4PVwiLTFcIiBjbGFzcz1cImFuY2hvclwiIGFyaWEtbGFiZWw9XCLpobXpnaLlhoXlip/og73pnIDmsYLlvoHpm4ZcIj5cbiAgICAgIDxoMj7lip/og73pnIDmsYLkuI7mhI/op4HlvoHpm4bvvIjnnJ/lrp7kv53lrZjvvIk8L2gyPlxuICAgICAgPGRpdiBjbGFzcz1cInRvb2xiYXJcIlxuICAgICAgICA+PGVsLWJ1dHRvbiA6ZGlzYWJsZWQ9XCIhY2FuQ3JlYXRlXCIgdHlwZT1cInByaW1hcnlcIiBAY2xpY2s9XCJuZXdPcGluaW9uKCdNT0RVTEUnLCAnbW9kdWxlJylcIlxuICAgICAgICAgID7ooaXlhYXmnKzmqKHlnZfpnIDmsYI8L2VsLWJ1dHRvblxuICAgICAgICA+XG4gICAgICAgIDxlbC1idXR0b24gOmRpc2FibGVkPVwiIWNhcGFiaWxpdGllcy5GVUxMX0VESVRcIiBAY2xpY2s9XCJpbml0aWFsaXplXCJcbiAgICAgICAgICA+5Yid5aeL5YyWNDbpobnlgJnpgInmqKHmnb88L2VsLWJ1dHRvblxuICAgICAgICA+XG4gICAgICAgIDxlbC1idXR0b25cbiAgICAgICAgICB2LWhhc1Blcm1pPVwiWydocm06cGF5cm9sbDpyZXF1aXJlbWVudDpleHBvcnQnXVwiXG4gICAgICAgICAgOmRpc2FibGVkPVwiIXByb2plY3RJZFwiXG4gICAgICAgICAgQGNsaWNrPVwiZG93bmxvYWRcIlxuICAgICAgICAgID7lr7zlh7rlvZPliY3nrZvpgInor4TlrqHmuIXljZU8L2VsLWJ1dHRvblxuICAgICAgICA+XG4gICAgICAgIDxlbC1idXR0b25cbiAgICAgICAgICB2LWhhc1Blcm1pPVwiWydocm06cGF5cm9sbDpyZXF1aXJlbWVudDphc3NpZ24nXVwiXG4gICAgICAgICAgOmRpc2FibGVkPVwiIXNlbGVjdGVkLmxlbmd0aFwiXG4gICAgICAgICAgQGNsaWNrPVwiYmF0Y2hWaXNpYmxlID0gdHJ1ZVwiXG4gICAgICAgICAgPuaJuemHj+WIhua0vuaJgOmAiemcgOaxgjwvZWwtYnV0dG9uXG4gICAgICAgID5cbiAgICAgIDwvZGl2PlxuICAgICAgPGRpdiBjbGFzcz1cInRvb2xiYXJcIj5cbiAgICAgICAgPGVsLWlucHV0XG4gICAgICAgICAgdi1tb2RlbD1cImZpbHRlcnMudGV4dFwiXG4gICAgICAgICAgYXJpYS1sYWJlbD1cIuafpeaJvumcgOaxglwiXG4gICAgICAgICAgcGxhY2Vob2xkZXI9XCLnvJblj7fmiJblip/og73mlofmnKxcIlxuICAgICAgICAgIGNsZWFyYWJsZVxuICAgICAgICAgIEBrZXl1cC5lbnRlcj1cInNlYXJjaFwiXG4gICAgICAgIC8+XG4gICAgICAgIDxlbC1zZWxlY3RcbiAgICAgICAgICB2LW1vZGVsPVwiZmlsdGVycy5zdGF0dXNcIlxuICAgICAgICAgIGFyaWEtbGFiZWw9XCLor4TlrqHnirbmgIHnrZvpgIlcIlxuICAgICAgICAgIGNsZWFyYWJsZVxuICAgICAgICAgIHBsYWNlaG9sZGVyPVwi5YWo6YOo6K+E5a6h54q25oCBXCJcbiAgICAgICAgICA+PGVsLW9wdGlvbiB2LWZvcj1cIihuYW1lLCBrZXkpIGluIHN0YXR1c2VzXCIgOmtleT1cImtleVwiIDpsYWJlbD1cIm5hbWVcIiA6dmFsdWU9XCJrZXlcIlxuICAgICAgICAvPjwvZWwtc2VsZWN0PlxuICAgICAgICA8ZWwtc2VsZWN0XG4gICAgICAgICAgdi1tb2RlbD1cImZpbHRlcnMuZGVjaXNpb25cIlxuICAgICAgICAgIGFyaWEtbGFiZWw9XCLojIPlm7TlhrPnrZbnrZvpgIlcIlxuICAgICAgICAgIGNsZWFyYWJsZVxuICAgICAgICAgIHBsYWNlaG9sZGVyPVwi5YWo6YOo6IyD5Zu05Yaz562WXCJcbiAgICAgICAgICA+PGVsLW9wdGlvbiB2LWZvcj1cIihuYW1lLCBrZXkpIGluIGRlY2lzaW9uc1wiIDprZXk9XCJrZXlcIiA6bGFiZWw9XCJuYW1lXCIgOnZhbHVlPVwia2V5XCJcbiAgICAgICAgLz48L2VsLXNlbGVjdD5cbiAgICAgICAgPGVsLXNlbGVjdCB2LW1vZGVsPVwic29ydFwiIGFyaWEtbGFiZWw9XCLmjpLluo9cIlxuICAgICAgICAgID48ZWwtb3B0aW9uIGxhYmVsPVwi57yW5Y+35Y2H5bqPXCIgdmFsdWU9XCJjb2RlXCIgLz48ZWwtb3B0aW9uXG4gICAgICAgICAgICBsYWJlbD1cIue8luWPt+mZjeW6j1wiXG4gICAgICAgICAgICB2YWx1ZT1cIi1jb2RlXCIgLz48ZWwtb3B0aW9uIGxhYmVsPVwi54mI5pys6ZmN5bqPXCIgdmFsdWU9XCItdmVyc2lvblwiXG4gICAgICAgIC8+PC9lbC1zZWxlY3Q+XG4gICAgICAgIDxlbC1idXR0b24gQGNsaWNrPVwic2VhcmNoXCI+5bqU55So562b6YCJPC9lbC1idXR0b25cbiAgICAgICAgPjxlbC1idXR0b24gQGNsaWNrPVwiY2xlYXJGaWx0ZXJzXCI+5riF6Zmk562b6YCJPC9lbC1idXR0b24+XG4gICAgICA8L2Rpdj5cbiAgICAgIDxkaXYgdi1pZj1cIiFsb2FkaW5nICYmICFlcnJvciAmJiAhcm93cy5sZW5ndGhcIiByb2xlPVwic3RhdHVzXCJcbiAgICAgICAgPuW9k+WJjeetm+mAieaaguaXoOWPr+ingemcgOaxgu+8m+S4jeS7o+ihqOWFqOmhueebruaXoOmcgOaxguOAgjwvZGl2XG4gICAgICA+XG4gICAgICA8ZGl2IHYtaWY9XCIhbG9hZGluZyAmJiAhZXJyb3JcIiBjbGFzcz1cIndpZGUtdGFibGVcIiB0YWJpbmRleD1cIjBcIiBhcmlhLWxhYmVsPVwi55yf5a6e6ZyA5rGC5riF5Y2VXCI+XG4gICAgICAgIDxlbC10YWJsZSA6ZGF0YT1cInJvd3NcIiByb3cta2V5PVwiaWRcIiBAc2VsZWN0aW9uLWNoYW5nZT1cInNlbGVjdGVkID0gJGV2ZW50XCI+XG4gICAgICAgICAgPGVsLXRhYmxlLWNvbHVtbiB0eXBlPVwic2VsZWN0aW9uXCIgd2lkdGg9XCI0NVwiIC8+XG4gICAgICAgICAgPGVsLXRhYmxlLWNvbHVtbiBwcm9wPVwiaWRcIiBsYWJlbD1cIumcgOaxgklEXCIgd2lkdGg9XCI4NVwiIC8+XG4gICAgICAgICAgPGVsLXRhYmxlLWNvbHVtbiBwcm9wPVwiY29kZVwiIGxhYmVsPVwi57yW5Y+3XCIgd2lkdGg9XCIxMzBcIiAvPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gcHJvcD1cImRlc2NyaXB0aW9uXCIgbGFiZWw9XCLlip/og70v5oSP6KeBXCIgbWluLXdpZHRoPVwiMjQwXCIgLz5cbiAgICAgICAgICA8ZWwtdGFibGUtY29sdW1uIGxhYmVsPVwi5Yqf6IO96K+E5a6hXCIgd2lkdGg9XCIxMTBcIlxuICAgICAgICAgICAgPjx0ZW1wbGF0ZSAjZGVmYXVsdD1cInNcIlxuICAgICAgICAgICAgICA+e3sgc3RhdHVzZXNbcy5yb3cuc3RhdHVzXVxuICAgICAgICAgICAgICB9fXt7XG4gICAgICAgICAgICAgICAgcy5yb3cuc3RhdHVzID09PSAnQ09ORklSTUVEJyAmJiAhcy5yb3cuY3VycmVudENvbmZpcm1hdGlvblZhbGlkXG4gICAgICAgICAgICAgICAgICA/ICfvvIjlvZPliY3nu5HlrprlpLHmlYjvvIknXG4gICAgICAgICAgICAgICAgICA6ICcnXG4gICAgICAgICAgICAgIH19PC90ZW1wbGF0ZVxuICAgICAgICAgICAgPjwvZWwtdGFibGUtY29sdW1uXG4gICAgICAgICAgPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gbGFiZWw9XCLojIPlm7TlhrPnrZZcIiB3aWR0aD1cIjEwNVwiXG4gICAgICAgICAgICA+PHRlbXBsYXRlICNkZWZhdWx0PVwic1wiPnt7XG4gICAgICAgICAgICAgIGRlY2lzaW9uc1tzLnJvdy5zY29wZURlY2lzaW9uIHx8ICdDQU5ESURBVEUnXVxuICAgICAgICAgICAgfX08L3RlbXBsYXRlPjwvZWwtdGFibGUtY29sdW1uXG4gICAgICAgICAgPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gbGFiZWw9XCLmnaXmupBcIiB3aWR0aD1cIjEwMFwiXG4gICAgICAgICAgICA+PHRlbXBsYXRlICNkZWZhdWx0PVwic1wiPnt7IHJlYWRpbmVzc1tzLnJvdy5yZWFkaW5lc3NdIH19PC90ZW1wbGF0ZT48L2VsLXRhYmxlLWNvbHVtblxuICAgICAgICAgID5cbiAgICAgICAgICA8ZWwtdGFibGUtY29sdW1uIGxhYmVsPVwi5a6e5pa9XCIgd2lkdGg9XCIxMTBcIlxuICAgICAgICAgICAgPjx0ZW1wbGF0ZSAjZGVmYXVsdD1cInNcIj57e1xuICAgICAgICAgICAgICBpbXBsZW1lbnRhdGlvbnNbcGFyc2Uocy5yb3cuaW1wbGVtZW50YXRpb25Kc29uKS5zdGF0ZSB8fCAnVU5TQ0hFRFVMRUQnXVxuICAgICAgICAgICAgfX08L3RlbXBsYXRlPjwvZWwtdGFibGUtY29sdW1uXG4gICAgICAgICAgPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gcHJvcD1cInZlcnNpb25cIiBsYWJlbD1cIueJiOacrFwiIHdpZHRoPVwiNzBcIiAvPlxuICAgICAgICAgIDxlbC10YWJsZS1jb2x1bW4gbGFiZWw9XCLmk43kvZxcIiBtaW4td2lkdGg9XCIzODBcIlxuICAgICAgICAgICAgPjx0ZW1wbGF0ZSAjZGVmYXVsdD1cInNcIj5cbiAgICAgICAgICAgICAgPGVsLWJ1dHRvbiBsaW5rIDpkaXNhYmxlZD1cIiFhbGxvd2VkKHMucm93LCAnRURJVCcpXCIgQGNsaWNrPVwiZWRpdChzLnJvdylcIlxuICAgICAgICAgICAgICAgID7nvJbovpHmhI/op4E8L2VsLWJ1dHRvblxuICAgICAgICAgICAgICA+XG4gICAgICAgICAgICAgIDxlbC1idXR0b24gbGluayBAY2xpY2s9XCJzaG93SGlzdG9yeShzLnJvdylcIj7ljoblj7Iv5q+U6L6DPC9lbC1idXR0b24+XG4gICAgICAgICAgICAgIDxlbC1idXR0b24gbGluayBAY2xpY2s9XCJyZXR1cm5UbyhzLnJvdylcIj7ov5Tlm57ljp/kvY3nva48L2VsLWJ1dHRvbj5cbiAgICAgICAgICAgICAgPGVsLWJ1dHRvblxuICAgICAgICAgICAgICAgIHYtaGFzUGVybWk9XCJbJ2hybTpwYXlyb2xsOnJlcXVpcmVtZW50OnNjb3BlLWRlY2lkZSddXCJcbiAgICAgICAgICAgICAgICBsaW5rXG4gICAgICAgICAgICAgICAgOmRpc2FibGVkPVwiIWFsbG93ZWQocy5yb3csICdTQ09QRV9ERUNJREUnKVwiXG4gICAgICAgICAgICAgICAgQGNsaWNrPVwib3BlbkFjdGlvbihzLnJvdywgJ2RlY2lzaW9uJylcIlxuICAgICAgICAgICAgICAgID7ojIPlm7TlhrPnrZY8L2VsLWJ1dHRvblxuICAgICAgICAgICAgICA+XG4gICAgICAgICAgICAgIDxlbC1idXR0b25cbiAgICAgICAgICAgICAgICBsaW5rXG4gICAgICAgICAgICAgICAgOmRpc2FibGVkPVwiIWFsbG93ZWQocy5yb3csICdFRElUJylcIlxuICAgICAgICAgICAgICAgIEBjbGljaz1cIm9wZW5BY3Rpb24ocy5yb3csICdpbXBsZW1lbnRhdGlvbicpXCJcbiAgICAgICAgICAgICAgICA+5a6e5pa95YWz6IGUPC9lbC1idXR0b25cbiAgICAgICAgICAgICAgPlxuICAgICAgICAgICAgICA8ZWwtYnV0dG9uXG4gICAgICAgICAgICAgICAgbGlua1xuICAgICAgICAgICAgICAgIDpkaXNhYmxlZD1cIiFhbGxvd2VkKHMucm93LCAnRURJVCcpXCJcbiAgICAgICAgICAgICAgICBAY2xpY2s9XCJvcGVuQWN0aW9uKHMucm93LCAnY29sbGFib3JhdGlvbicpXCJcbiAgICAgICAgICAgICAgICA+5Lya6K6u5LiO56uZ5YaF5o+Q6YaSPC9lbC1idXR0b25cbiAgICAgICAgICAgICAgPlxuICAgICAgICAgICAgICA8ZWwtYnV0dG9uXG4gICAgICAgICAgICAgICAgdi1oYXNQZXJtaT1cIlsnaHJtOnBheXJvbGw6cmVxdWlyZW1lbnQ6Y29uZmlybSddXCJcbiAgICAgICAgICAgICAgICBsaW5rXG4gICAgICAgICAgICAgICAgOmRpc2FibGVkPVwiIWFsbG93ZWQocy5yb3csICdDT05GSVJNJykgfHwgcy5yb3cuc3RhdHVzID09PSAnQ09ORklSTUVEJ1wiXG4gICAgICAgICAgICAgICAgQGNsaWNrPVwiY29uZmlybShzLnJvdylcIlxuICAgICAgICAgICAgICAgID7mnKzkurrnoa7orqQ8L2VsLWJ1dHRvblxuICAgICAgICAgICAgICA+XG4gICAgICAgICAgICA8L3RlbXBsYXRlPjwvZWwtdGFibGUtY29sdW1uXG4gICAgICAgICAgPlxuICAgICAgICA8L2VsLXRhYmxlPlxuICAgICAgPC9kaXY+XG4gICAgICA8ZWwtcGFnaW5hdGlvblxuICAgICAgICB2LWlmPVwiIWxvYWRpbmcgJiYgIWVycm9yXCJcbiAgICAgICAgdi1tb2RlbDpjdXJyZW50LXBhZ2U9XCJwYWdlTm9cIlxuICAgICAgICA6cGFnZS1zaXplPVwiMTBcIlxuICAgICAgICBsYXlvdXQ9XCJwcmV2LCBwYWdlciwgbmV4dCwgdG90YWxcIlxuICAgICAgICA6dG90YWw9XCJ0b3RhbFwiXG4gICAgICAgIEBjdXJyZW50LWNoYW5nZT1cImxvYWRcIlxuICAgICAgLz5cbiAgICA8L3NlY3Rpb24+XG4gICAgPHNlY3Rpb24gYXJpYS1sYWJlbD1cIuaVsOaNruadpea6kOW+gembhlwiPlxuICAgICAgPGgyPuS+nei1luaVsOaNruadpea6kOS4juebmOeCuTwvaDI+XG4gICAgICA8cD7lgJnpgInnm67lvZXmmK/lvoHpm4bnur/ntKLvvIzkuI3og73lhpLlhYXlrp7pmYXlt7Lmj5DkvpvmnaXmupDmiJbotKjph4/pgJrov4fjgILlrp7pmYXmnaXmupDpnIDmjInmnYPpmZDlj6booYznmbvorrDkuI7moLjpqozjgII8L3A+XG4gICAgICA8ZGl2IHYtZm9yPVwic291cmNlIGluIGNhbmRpZGF0ZXNcIiA6a2V5PVwic291cmNlLmlkXCIgY2xhc3M9XCJzb3VyY2UtY2FyZFwiXG4gICAgICAgID48c3Ryb25nPnt7IHNvdXJjZS5pZCB9fSDCtyB7eyBzb3VyY2UubmFtZSB9fTwvc3Ryb25nXG4gICAgICAgID48cD57eyBzb3VyY2UuZmllbGRzLmpvaW4oJ+OAgScpIH1977yb5YCZ6YCJ56Gu6K6k5pa577yae3sgc291cmNlLm93bmVyIH19PC9wPjwvZGl2XG4gICAgICA+XG4gICAgICA8bGFiZWxcbiAgICAgICAgPuW9k+WJjeWPr+ingeWunumZheadpea6kFxuICAgICAgICA8ZWwtc2VsZWN0XG4gICAgICAgICAgdi1tb2RlbD1cInNvdXJjZUlkXCJcbiAgICAgICAgICBhcmlhLWxhYmVsPVwi6YCJ5oup5a6e6ZmF5p2l5rqQXCJcbiAgICAgICAgICBjbGVhcmFibGVcbiAgICAgICAgICBwbGFjZWhvbGRlcj1cIuaXoOadpea6kOaXtuS/neaMgee8uuWPo1wiXG4gICAgICAgICAgPjxlbC1vcHRpb25cbiAgICAgICAgICAgIHYtZm9yPVwic291cmNlIGluIHNvdXJjZXNcIlxuICAgICAgICAgICAgOmtleT1cInNvdXJjZS5pZFwiXG4gICAgICAgICAgICA6bGFiZWw9XCJgJHtzb3VyY2UuaWR9IMK3ICR7c291cmNlLmRhdGEubmFtZX1gXCJcbiAgICAgICAgICAgIDp2YWx1ZT1cInNvdXJjZS5pZFwiIC8+PC9lbC1zZWxlY3RcbiAgICAgID48L2xhYmVsPlxuICAgICAgPHAgdi1pZj1cInNvdXJjZUlkXCI+e3sgc291cmNlRGVzY3JpcHRpb24gfX08L3A+XG4gICAgICA8ZWwtYnV0dG9uIEBjbGljaz1cIm9wZW5Tb3VyY2VXb3JrYmVuY2hcIj7nu7TmiqTlrp7pmYXmnaXmupDjgIHmmKDlsITlkoxRUy9RQzwvZWwtYnV0dG9uPlxuICAgIDwvc2VjdGlvbj5cbiAgICA8c2VjdGlvbiBhcmlhLWxhYmVsPVwi5oiR55qE56uZ5YaF5o+Q6YaSXCI+XG4gICAgICA8aDI+5oiR55qE56uZ5YaF5o+Q6YaS77yI5peg5aSW5Y+R77yJPC9oMj5cbiAgICAgIDxkaXYgdi1mb3I9XCIobm90ZSwgaSkgaW4gcmVtaW5kZXJzXCIgOmtleT1cImlcIiBjbGFzcz1cInNvdXJjZS1jYXJkXCI+XG4gICAgICAgIDxzdHJvbmc+e3sgbm90ZS5jb2RlIH19IMK3IOmcgOaxgnt7IG5vdGUucmVxdWlyZW1lbnRJZCB9fTwvc3Ryb25nXG4gICAgICAgID48cD57eyBub3RlLnJlZmVyZW5jZSB9fSDCtyB7eyBub3RlLnRpbWUgfX08L3A+XG4gICAgICAgIDxlbC1idXR0b24gQGNsaWNrPVwiZ28obm90ZS5tb2R1bGUsIG5vdGUuYW5jaG9yKVwiPui/lOWbnuaPkOmGkuWOn+S9jee9rjwvZWwtYnV0dG9uPlxuICAgICAgPC9kaXY+XG4gICAgICA8cCB2LWlmPVwiIWxvYWRpbmcgJiYgIWVycm9yICYmICFyZW1pbmRlcnMubGVuZ3RoXCJcbiAgICAgICAgPuW9k+WJjeacieadg+iMg+WbtOWGheaaguaXoOWPkee7meaIkeeahOermeWGheaPkOmGku+8m+S8muiurue6quimgeWPiuWFqOmDqOWNj+S9nOWOhuWPsuWPr+WcqOmcgOaxguWOhuWPsuS4reafpeeci+OAgjwvcFxuICAgICAgPlxuICAgIDwvc2VjdGlvbj5cbiAgICA8c2VjdGlvbiBhcmlhLWxhYmVsPVwi55yf5a6e57uf6K6h5LiO5LiL6ZK7XCI+XG4gICAgICA8aDI+6IyD5Zu044CB56Gu6K6k5Y+K5p2l5rqQ57uf6K6h77yI5pyN5Yqh5Zmo6ZuG5ZCI77yJPC9oMj5cbiAgICAgIDxwXG4gICAgICAgID5V5YWo6YOo6ZyA5rGC77yI5ZCr56e76Zmk77yJ77ybReacquenu+mZpO+8m0nnurPlhaXmnKzmnJ/vvJtD5b2T5YmN56Gu6K6k5pyJ5pWI77ybVOaague8k++8m0TmnInlvILorq7jgILmmoLnvJPkuI7lvILorq7ku43orqHlhaXmnInmlYjpm4blkIjvvIzojIPlm7TlhrPnrZbkuI3ku6Pmm7/noa7orqTjgII8L3BcbiAgICAgID5cbiAgICAgIDxwIHYtaWY9XCJzdGF0c1wiPnt7IHN0YXRzLmxhYmVsIH19IMK3IOetm+mAieadoeS7tiB7eyBKU09OLnN0cmluZ2lmeShzdGF0cy5maWx0ZXJzKSB9fTwvcD5cbiAgICAgIDxkaXYgdi1pZj1cInN0YXRzXCIgY2xhc3M9XCJzdGF0cy1ncmlkXCI+XG4gICAgICAgIDxidXR0b25cbiAgICAgICAgICB2LWZvcj1cImtleSBpbiBbJ1UnLCAnRScsICdJJywgJ0MnLCAnVCcsICdEJ11cIlxuICAgICAgICAgIDprZXk9XCJrZXlcIlxuICAgICAgICAgIGNsYXNzPVwic3RhdFwiXG4gICAgICAgICAgQGNsaWNrPVwiZHJpbGwoa2V5KVwiXG4gICAgICAgICAgPnt7IGtleSB9fSDCtyB7eyBzdGF0cy5jb3VudHNba2V5XSB9fSDCtyDmn6XnnIvpm4blkIg8L2J1dHRvblxuICAgICAgICA+XG4gICAgICAgIDxkaXYgdi1mb3I9XCJrZXkgaW4gcmF0aW9LZXlzXCIgOmtleT1cImtleVwiIGNsYXNzPVwic3RhdFwiXG4gICAgICAgICAgPnt7IHJhdGlvTGFiZWxzW2tleV0gfX3vvJp7eyBzdGF0c1trZXldLmxhYmVsIH19PC9kaXZcbiAgICAgICAgPlxuICAgICAgPC9kaXY+XG4gICAgICA8cCB2LWlmPVwic3RhdHNcIlxuICAgICAgICA+5Y676YeN5a6e6ZmF5p2l5rqQIHt7IHN0YXRzLnNvdXJjZUNvdW50IH1977yb5pyJ5pWI5LiN6YCC55SoIHt7IHN0YXRzLmV4ZW1wdCB9fe+8m+acquWGs+mXrumimFxuICAgICAgICB7eyBzdGF0cy51bnJlc29sdmVkSXNzdWVzIH1977yM6YC+5pyfXG4gICAgICAgIHt7IHN0YXRzLm92ZXJkdWVJc3N1ZXMgfX3jgILlhbPns7vlkozpnIDmsYLnvLrlj6Pku6Xni6znq4tJROiuoeaVsOOAgjwvcFxuICAgICAgPlxuICAgICAgPHAgdi1pZj1cInN0YXRzPy5wcm9qZWN0VG90YWxzXCJcbiAgICAgICAgPuWFqOmhueebruWKn+iDveehruiupCB7eyBzdGF0cy5wcm9qZWN0VG90YWxzLmNvbmZpcm1hdGlvbi5sYWJlbCB9fe+8jOacrOacn+ehruiupFxuICAgICAgICB7eyBzdGF0cy5wcm9qZWN0VG90YWxzLmluY2x1ZGVkQ29uZmlybWF0aW9uLmxhYmVsIH1977yb5b2T5YmN562b6YCJ5LiN5pS55Y+Y5YWo6aG555uu6Zeo5qeb44CCPC9wXG4gICAgICA+XG4gICAgICA8cFxuICAgICAgICA+5b2T5YmN57uf6K6h5LiN5piv5Y+R5biD5oiQ5Yqf5oiW5bey5a6e5pa96K+B5piO77yb5Lia5Yqh5Yaz6K6u44CB5Y+R5biD5qOA5p+l5LiO5YWt6KGo5b+r54Wn6K+35L2/55So54us56uL5YWl5Y+j5qC45a6e44CC5Y+X6ZmQ6LSm5Y+35Y+q5bGV56S65Y+v6KeB6ZuG5ZCI77yM6ZqQ6JeP5a+56LGh5pWw6YeP5LiN6L6T5Ye644CCPC9wXG4gICAgICA+XG4gICAgICA8ZWwtYnV0dG9uIEBjbGljaz1cImRyaWxsKCdMJylcIiA6ZGlzYWJsZWQ9XCIhc3RhdHNcIj7mn6XnnIvlhbPns7vkuI7nvLrlj6NMPC9lbC1idXR0b24+XG4gICAgPC9zZWN0aW9uPlxuICA8L0NvbnRlbnRXcmFwPlxuICA8ZWwtZGlhbG9nXG4gICAgY2xhc3M9XCJocm0tcmV2aWV3LWRpYWxvZ1wiXG4gICAgdi1tb2RlbD1cImZvcm1WaXNpYmxlXCJcbiAgICB0aXRsZT1cIumhtemdouaEj+ingeS4juS4muWKoeWcuuaZr1wiXG4gICAgd2lkdGg9XCJtaW4oODgwcHgsIDk2dncpXCJcbiAgICA6Y2xvc2Utb24tY2xpY2stbW9kYWw9XCJmYWxzZVwiXG4gICAgQGNsb3NlZD1cInJlc2V0Rm9jdXNcIlxuICA+XG4gICAgPHBcbiAgICAgID7pobnnm64ge3sgZm9ybS5wcm9qZWN0SWQgfX0gwrcge3sgZm9ybS5tb2R1bGUgfX0gwrcge3sgYnVzaW5lc3MudGFyZ2V0VHlwZSB9fSDCt1xuICAgICAge3tcbiAgICAgICAgYnVzaW5lc3MuYW5jaG9yXG4gICAgICB9feOAguiNieeov+WPr+mAkOatpeihpem9kO+8m+ivhOWuoeS4remhu+WcuuaZry/mnJ/mnJvvvIznoa7orqTov5jpobvop5LoibIv55uu5qCH44CB5q2j5bi45byC5bi444CB6KeE5YiZL+WNleS9jS/nsr7luqYv5pyJ5pWI5pyfL+S+i+WkluOAgemqjOaUtuS+neaNruOAgjwvcFxuICAgID5cbiAgICA8ZWwtYWxlcnQgdi1pZj1cInNhdmVFcnJvclwiIDp0aXRsZT1cInNhdmVFcnJvclwiIHR5cGU9XCJlcnJvclwiIDpjbG9zYWJsZT1cImZhbHNlXCIgLz5cbiAgICA8ZGV0YWlscyB2LWlmPVwiY29uZmxpY3RMYXRlc3RcIlxuICAgICAgPjxzdW1tYXJ5PuacgOaWsOS/neWtmOWGheWuue+8iOS4juS/neeVmei+k+WFpeS6uuW3peWQiOW5tu+8iTwvc3VtbWFyeVxuICAgICAgPjxwcmU+e3sgSlNPTi5zdHJpbmdpZnkoY29uZmxpY3RMYXRlc3QsIG51bGwsIDIpIH19PC9wcmU+XG4gICAgPC9kZXRhaWxzPlxuICAgIDxlbC1mb3JtIGxhYmVsLXBvc2l0aW9uPVwidG9wXCI+XG4gICAgICA8ZWwtZm9ybS1pdGVtIGxhYmVsPVwi5Yqf6IO96K+05piOXCIgOmVycm9yPVwib3BpbmlvbkVycm9ycy5kZXNjcmlwdGlvbiA/ICfor7floavlhpnlip/og73or7TmmI4nIDogJydcIlxuICAgICAgICA+PGVsLWlucHV0XG4gICAgICAgICAgdi1tb2RlbD1cImZvcm0uZGVzY3JpcHRpb25cIlxuICAgICAgICAgIGFyaWEtbGFiZWw9XCLlip/og73or7TmmI5cIlxuICAgICAgICAgIHR5cGU9XCJ0ZXh0YXJlYVwiXG4gICAgICAgICAgbWF4bGVuZ3RoPVwiMTAwMFwiXG4gICAgICAvPjwvZWwtZm9ybS1pdGVtPlxuICAgICAgPGRpdiBjbGFzcz1cImZvcm0tZ3JpZFwiXG4gICAgICAgID48ZWwtZm9ybS1pdGVtXG4gICAgICAgICAgdi1mb3I9XCJmaWVsZCBpbiBidXNpbmVzc0ZpZWxkc1wiXG4gICAgICAgICAgOmtleT1cImZpZWxkLmtleVwiXG4gICAgICAgICAgOmxhYmVsPVwiZmllbGQubGFiZWxcIlxuICAgICAgICAgIDplcnJvcj1cIm9waW5pb25FcnJvcnNbZmllbGQua2V5XSA/IGDor7floavlhpkke2ZpZWxkLmxhYmVsfWAgOiAnJ1wiXG4gICAgICAgICAgPjxlbC1pbnB1dFxuICAgICAgICAgICAgdi1tb2RlbD1cImJ1c2luZXNzW2ZpZWxkLmtleV1cIlxuICAgICAgICAgICAgOmFyaWEtbGFiZWw9XCJmaWVsZC5sYWJlbFwiXG4gICAgICAgICAgICB0eXBlPVwidGV4dGFyZWFcIiAvPjwvZWwtZm9ybS1pdGVtXG4gICAgICA+PC9kaXY+XG4gICAgICA8U2NvcGVGaWVsZHMgdi1pZj1cImZvcm0uc2NvcGVNb2RlID09PSAnU1VCU0VUJ1wiIHYtbW9kZWw9XCJmb3JtLnNjb3BlSnNvblwiIC8+XG4gICAgICA8ZWwtZm9ybS1pdGVtIGxhYmVsPVwi6IyD5Zu05pa55byPXCJcbiAgICAgICAgPjxlbC1zZWxlY3Qgdi1tb2RlbD1cImZvcm0uc2NvcGVNb2RlXCIgYXJpYS1sYWJlbD1cIuiMg+WbtOaWueW8j1wiXG4gICAgICAgICAgPjxlbC1vcHRpb24gbGFiZWw9XCLnu6fmib/pobnnm67ojIPlm7RcIiB2YWx1ZT1cIklOSEVSSVRcIiAvPjxlbC1vcHRpb25cbiAgICAgICAgICAgIGxhYmVsPVwi5pi+5byP5a2Q6ZuGXCJcbiAgICAgICAgICAgIHZhbHVlPVwiU1VCU0VUXCIgLz48L2VsLXNlbGVjdFxuICAgICAgPjwvZWwtZm9ybS1pdGVtPlxuICAgICAgPGVsLWZvcm0taXRlbSBsYWJlbD1cIuadpea6kOmAgueUqOaAp1wiXG4gICAgICAgID48ZWwtc2VsZWN0IHYtbW9kZWw9XCJmb3JtLnNvdXJjZUFwcGxpY2FiaWxpdHlcIiBhcmlhLWxhYmVsPVwi5p2l5rqQ6YCC55So5oCnXCJcbiAgICAgICAgICA+PGVsLW9wdGlvbiBsYWJlbD1cIumAgueUqFwiIHZhbHVlPVwiQVBQTElDQUJMRVwiIC8+PGVsLW9wdGlvblxuICAgICAgICAgICAgbGFiZWw9XCLkuI3pgILnlKhcIlxuICAgICAgICAgICAgdmFsdWU9XCJOT1RfQVBQTElDQUJMRVwiIC8+PC9lbC1zZWxlY3RcbiAgICAgID48L2VsLWZvcm0taXRlbT5cbiAgICAgIDxlbC1mb3JtLWl0ZW1cbiAgICAgICAgdi1pZj1cImZvcm0uc291cmNlQXBwbGljYWJpbGl0eSA9PT0gJ05PVF9BUFBMSUNBQkxFJ1wiXG4gICAgICAgIGxhYmVsPVwi5LiN6YCC55So55CG55SxXCJcbiAgICAgICAgOmVycm9yPVwib3BpbmlvbkVycm9ycy5hcHBsaWNhYmlsaXR5UmVhc29uID8gJ+ivt+Whq+WGmeiHs+WwkTjkuKrlrZfnrKbnmoTlhbfkvZPkuI3pgILnlKjnkIbnlLEnIDogJydcIlxuICAgICAgICA+PGVsLWlucHV0IHYtbW9kZWw9XCJmb3JtLmFwcGxpY2FiaWxpdHlSZWFzb25cIiBhcmlhLWxhYmVsPVwi5LiN6YCC55So55CG55SxXCIgdHlwZT1cInRleHRhcmVhXCJcbiAgICAgIC8+PC9lbC1mb3JtLWl0ZW0+XG4gICAgICA8ZWwtZm9ybS1pdGVtXG4gICAgICAgIGxhYmVsPVwi6KeE5YiZ5byV55So54mI5pysXCJcbiAgICAgICAgOmVycm9yPVwib3BpbmlvbkVycm9ycy5ydWxlVmVyc2lvbiA/ICfor7floavlhpnop4TliJnlvJXnlKjniYjmnKwnIDogJydcIlxuICAgICAgICA+PGVsLWlucHV0IHYtbW9kZWw9XCJmb3JtLnJ1bGVWZXJzaW9uXCIgYXJpYS1sYWJlbD1cIuinhOWImeW8leeUqOeJiOacrFwiXG4gICAgICAvPjwvZWwtZm9ybS1pdGVtPlxuICAgICAgPGVsLWZvcm0taXRlbSBsYWJlbD1cIumqjOaUtuadoeS7tlwiIDplcnJvcj1cIm9waW5pb25FcnJvcnMuYWNjZXB0YW5jZSA/ICfor7floavlhpnpqozmlLbmnaHku7YnIDogJydcIlxuICAgICAgICA+PGVsLWlucHV0IHYtbW9kZWw9XCJmb3JtLmFjY2VwdGFuY2VcIiBhcmlhLWxhYmVsPVwi6aqM5pS25p2h5Lu2XCIgdHlwZT1cInRleHRhcmVhXCJcbiAgICAgIC8+PC9lbC1mb3JtLWl0ZW0+XG4gICAgICA8ZWwtZm9ybS1pdGVtXG4gICAgICAgIGxhYmVsPVwi5L6d5o2u5oiW5byC6K6u6K+05piOXCJcbiAgICAgICAgOmVycm9yPVwib3BpbmlvbkVycm9ycy5ldmlkZW5jZSA/ICfor7floavlhpnkvp3mja7miJblvILorq7or7TmmI4nIDogJydcIlxuICAgICAgICA+PGVsLWlucHV0IHYtbW9kZWw9XCJmb3JtLmV2aWRlbmNlXCIgYXJpYS1sYWJlbD1cIuS+neaNruaIluW8guiuruivtOaYjlwiIHR5cGU9XCJ0ZXh0YXJlYVwiXG4gICAgICAvPjwvZWwtZm9ybS1pdGVtPlxuICAgICAgPGVsLWZvcm0taXRlbSBsYWJlbD1cIuWKn+iDveivhOWuoeeKtuaAgVwiXG4gICAgICAgID48ZWwtc2VsZWN0IHYtbW9kZWw9XCJmb3JtLnN0YXR1c1wiIGFyaWEtbGFiZWw9XCLlip/og73or4TlrqHnirbmgIFcIlxuICAgICAgICAgID48ZWwtb3B0aW9uIGxhYmVsPVwi5b6F56Gu6K6k77yI6I2J56i/77yJXCIgdmFsdWU9XCJQRU5ESU5HXCIgLz48ZWwtb3B0aW9uXG4gICAgICAgICAgICBsYWJlbD1cIuivhOWuoeS4rVwiXG4gICAgICAgICAgICB2YWx1ZT1cIlJFVklFV0lOR1wiIC8+PGVsLW9wdGlvbiBsYWJlbD1cIuacieW8guiurlwiIHZhbHVlPVwiRElTUFVURURcIiAvPjwvZWwtc2VsZWN0XG4gICAgICA+PC9lbC1mb3JtLWl0ZW0+XG4gICAgICA8dGVtcGxhdGUgdi1pZj1cImZvcm0uc3RhdHVzID09PSAnRElTUFVURUQnXCJcbiAgICAgICAgPjxlbC1mb3JtLWl0ZW1cbiAgICAgICAgICBsYWJlbD1cIuW8guiurui0o+S7u+S6uklEXCJcbiAgICAgICAgICA6ZXJyb3I9XCJvcGluaW9uRXJyb3JzLmRpc3B1dGVPd25lcklkID8gJ+ivt+Whq+WGmeW8guiurui0o+S7u+S6uklEJyA6ICcnXCJcbiAgICAgICAgICA+PGVsLWlucHV0LW51bWJlclxuICAgICAgICAgICAgdi1tb2RlbD1cImJ1c2luZXNzLmRpc3B1dGVPd25lcklkXCJcbiAgICAgICAgICAgIGFyaWEtbGFiZWw9XCLlvILorq7otKPku7vkurpJRFwiXG4gICAgICAgICAgICA6bWluPVwiMVwiIC8+PC9lbC1mb3JtLWl0ZW1cbiAgICAgICAgPjxlbC1mb3JtLWl0ZW0gbGFiZWw9XCLlvILorq7kuIvkuIDmraVcIiA6ZXJyb3I9XCJvcGluaW9uRXJyb3JzLm5leHRTdGVwID8gJ+ivt+Whq+WGmeW8guiuruS4i+S4gOatpScgOiAnJ1wiXG4gICAgICAgICAgPjxlbC1pbnB1dCB2LW1vZGVsPVwiYnVzaW5lc3MubmV4dFN0ZXBcIiBhcmlhLWxhYmVsPVwi5byC6K6u5LiL5LiA5q2lXCIgLz48L2VsLWZvcm0taXRlbVxuICAgICAgPjwvdGVtcGxhdGU+XG4gICAgPC9lbC1mb3JtPlxuICAgIDx0ZW1wbGF0ZSAjZm9vdGVyXG4gICAgICA+PGRpdiBjbGFzcz1cImRpYWxvZy1hY3Rpb25zXCJcbiAgICAgICAgPjxlbC1idXR0b24gQGNsaWNrPVwiZm9ybVZpc2libGUgPSBmYWxzZVwiPuWPlua2iDwvZWwtYnV0dG9uXG4gICAgICAgID48ZWwtYnV0dG9uIDpkaXNhYmxlZD1cIiFmb3JtLmlkXCIgQGNsaWNrPVwicmVsb2FkQ29uZmxpY3RcIlxuICAgICAgICAgID7or7vlj5bmnIDmlrDniYjmnKzvvIjkv53nlZnlvZPliY3ovpPlhaXvvIk8L2VsLWJ1dHRvblxuICAgICAgICA+PGVsLWJ1dHRvbiB0eXBlPVwicHJpbWFyeVwiIDpsb2FkaW5nPVwic2F2aW5nXCIgQGNsaWNrPVwic2F2ZU9waW5pb25cIj7kv53lrZjmhI/op4E8L2VsLWJ1dHRvbj48L2RpdlxuICAgICAgPjwvdGVtcGxhdGVcbiAgICA+XG4gIDwvZWwtZGlhbG9nPlxuICA8ZWwtZGlhbG9nXG4gICAgY2xhc3M9XCJocm0tcmV2aWV3LWRpYWxvZ1wiXG4gICAgdi1tb2RlbD1cImFjdGlvblZpc2libGVcIlxuICAgIDp0aXRsZT1cImFjdGlvblRpdGxlc1thY3Rpb25LaW5kXVwiXG4gICAgd2lkdGg9XCJtaW4oNjgwcHgsIDk2dncpXCJcbiAgICA6Y2xvc2Utb24tY2xpY2stbW9kYWw9XCJmYWxzZVwiXG4gID5cbiAgICA8cD7pnIDmsYIge3sgYWN0aXZlPy5pZCB9fSDCtyDlvZPliY3niYjmnKwge3sgYWN0aXZlPy52ZXJzaW9uIH1977yb5pON5L2c54us56uL55WZ5Y6G5Y+y77yM5LiN5Luj5pu/5Yqf6IO956Gu6K6k44CCPC9wXG4gICAgPjxlbC1hbGVydCB2LWlmPVwic2F2ZUVycm9yXCIgOnRpdGxlPVwic2F2ZUVycm9yXCIgdHlwZT1cImVycm9yXCIgOmNsb3NhYmxlPVwiZmFsc2VcIiAvPlxuICAgIDxlbC1mb3JtIGxhYmVsLXBvc2l0aW9uPVwidG9wXCI+XG4gICAgICA8dGVtcGxhdGUgdi1pZj1cImFjdGlvbktpbmQgPT09ICdkZWNpc2lvbidcIj5cbiAgICAgICAgPGVsLWZvcm0taXRlbSBsYWJlbD1cIuiMg+WbtOWGs+etllwiXG4gICAgICAgICAgPjxlbC1zZWxlY3Qgdi1tb2RlbD1cImFjdGlvbi5kZWNpc2lvblwiIGFyaWEtbGFiZWw9XCLojIPlm7TlhrPnrZZcIlxuICAgICAgICAgICAgPjxlbC1vcHRpb25cbiAgICAgICAgICAgICAgdi1mb3I9XCIobmFtZSwga2V5KSBpbiBkZWNpc2lvbnNcIlxuICAgICAgICAgICAgICA6a2V5PVwia2V5XCJcbiAgICAgICAgICAgICAgOnZhbHVlPVwia2V5XCJcbiAgICAgICAgICAgICAgOmxhYmVsPVwibmFtZVwiIC8+PC9lbC1zZWxlY3RcbiAgICAgICAgPjwvZWwtZm9ybS1pdGVtPlxuICAgICAgICA8dGVtcGxhdGUgdi1pZj1cImFjdGlvbi5kZWNpc2lvbiA9PT0gJ0RFRkVSUkVEJ1wiXG4gICAgICAgICAgPjxlbC1mb3JtLWl0ZW0gbGFiZWw9XCLmmoLnvJPotKPku7vkurpJRFwiXG4gICAgICAgICAgICA+PGVsLWlucHV0LW51bWJlclxuICAgICAgICAgICAgICB2LW1vZGVsPVwiYWN0aW9uLnJlc3BvbnNpYmxlSWRcIlxuICAgICAgICAgICAgICBhcmlhLWxhYmVsPVwi5pqC57yT6LSj5Lu75Lq6SURcIlxuICAgICAgICAgICAgICA6bWluPVwiMVwiIC8+PC9lbC1mb3JtLWl0ZW1cbiAgICAgICAgICA+PGVsLWZvcm0taXRlbSBsYWJlbD1cIumHjeaWsOivhOS8sOadoeS7tlwiXG4gICAgICAgICAgICA+PGVsLWlucHV0IHYtbW9kZWw9XCJhY3Rpb24ucmVldmFsdWF0ZVwiIGFyaWEtbGFiZWw9XCLph43mlrDor4TkvLDmnaHku7ZcIiAvPjwvZWwtZm9ybS1pdGVtXG4gICAgICAgID48L3RlbXBsYXRlPlxuICAgICAgICA8dGVtcGxhdGUgdi1pZj1cImFjdGlvbi5kZWNpc2lvbiA9PT0gJ1JFTU9WRUQnXCJcbiAgICAgICAgICA+PGVsLWZvcm0taXRlbSBsYWJlbD1cIuenu+mZpOexu+Wei1wiXG4gICAgICAgICAgICA+PGVsLXNlbGVjdCB2LW1vZGVsPVwiYWN0aW9uLnJlbW92YWxLaW5kXCIgYXJpYS1sYWJlbD1cIuenu+mZpOexu+Wei1wiXG4gICAgICAgICAgICAgID48ZWwtb3B0aW9uIGxhYmVsPVwi6YeN5aSNXCIgdmFsdWU9XCJEVVBMSUNBVEVcIiAvPjxlbC1vcHRpb25cbiAgICAgICAgICAgICAgICBsYWJlbD1cIuivr+W9lVwiXG4gICAgICAgICAgICAgICAgdmFsdWU9XCJNSVNUQUtFXCIgLz48ZWwtb3B0aW9uXG4gICAgICAgICAgICAgICAgbGFiZWw9XCLotoXlh7rovrnnlYxcIlxuICAgICAgICAgICAgICAgIHZhbHVlPVwiT1VUX09GX1NDT1BFXCIgLz48L2VsLXNlbGVjdD48L2VsLWZvcm0taXRlbVxuICAgICAgICAgID48ZWwtZm9ybS1pdGVtIHYtaWY9XCJhY3Rpb24ucmVtb3ZhbEtpbmQgPT09ICdEVVBMSUNBVEUnXCIgbGFiZWw9XCLlkIzpobnnm67kv53nlZnpoblJRFwiXG4gICAgICAgICAgICA+PGVsLWlucHV0LW51bWJlclxuICAgICAgICAgICAgICB2LW1vZGVsPVwiYWN0aW9uLnJlcGxhY2VtZW50SWRcIlxuICAgICAgICAgICAgICBhcmlhLWxhYmVsPVwi5ZCM6aG555uu5L+d55WZ6aG5SURcIlxuICAgICAgICAgICAgICA6bWluPVwiMVwiIC8+PC9lbC1mb3JtLWl0ZW1cbiAgICAgICAgPjwvdGVtcGxhdGU+XG4gICAgICA8L3RlbXBsYXRlPlxuICAgICAgPHRlbXBsYXRlIHYtZWxzZS1pZj1cImFjdGlvbktpbmQgPT09ICdpbXBsZW1lbnRhdGlvbidcIlxuICAgICAgICA+PGVsLWZvcm0taXRlbSBsYWJlbD1cIuWunuaWveeKtuaAgVwiXG4gICAgICAgICAgPjxlbC1zZWxlY3Qgdi1tb2RlbD1cImFjdGlvbi5zdGF0ZVwiIGFyaWEtbGFiZWw9XCLlrp7mlr3nirbmgIFcIlxuICAgICAgICAgICAgPjxlbC1vcHRpb25cbiAgICAgICAgICAgICAgdi1mb3I9XCIobmFtZSwga2V5KSBpbiBpbXBsZW1lbnRhdGlvbnNcIlxuICAgICAgICAgICAgICA6a2V5PVwia2V5XCJcbiAgICAgICAgICAgICAgOmxhYmVsPVwibmFtZVwiXG4gICAgICAgICAgICAgIDp2YWx1ZT1cImtleVwiIC8+PC9lbC1zZWxlY3Q+PC9lbC1mb3JtLWl0ZW1cbiAgICAgICAgPjxlbC1mb3JtLWl0ZW0gdi1mb3I9XCJmaWVsZCBpbiBpbXBsZW1lbnRhdGlvbkZpZWxkc1wiIDprZXk9XCJmaWVsZC5rZXlcIiA6bGFiZWw9XCJmaWVsZC5sYWJlbFwiXG4gICAgICAgICAgPjxlbC1pbnB1dCB2LW1vZGVsPVwiYWN0aW9uW2ZpZWxkLmtleV1cIiA6YXJpYS1sYWJlbD1cImZpZWxkLmxhYmVsXCIgLz48L2VsLWZvcm0taXRlbVxuICAgICAgICA+PHA+5Y+q6K6w5b2V5Lq65bel5YWz6IGU5ZKM5aOw5piO77yM5byV55So5a2Y5Zyo5LiN6K+B5piO6aqM5pS26YCa6L+H77yb57O757uf5LiN6Ieq5Yqo5qC45a6e5aSW6YOo57uT6K6644CCPC9wPjwvdGVtcGxhdGVcbiAgICAgID5cbiAgICAgIDx0ZW1wbGF0ZSB2LWVsc2VcbiAgICAgICAgPjxlbC1mb3JtLWl0ZW0gbGFiZWw9XCLljY/kvZznsbvlnotcIlxuICAgICAgICAgID48ZWwtc2VsZWN0IHYtbW9kZWw9XCJhY3Rpb24ua2luZFwiIGFyaWEtbGFiZWw9XCLljY/kvZznsbvlnotcIlxuICAgICAgICAgICAgPjxlbC1vcHRpb24gbGFiZWw9XCLlhbPogZTkvJrorq7nuqropoFcIiB2YWx1ZT1cIk1FRVRJTkdcIiAvPjxlbC1vcHRpb25cbiAgICAgICAgICAgICAgbGFiZWw9XCLnq5nlhoXmj5DphpJcIlxuICAgICAgICAgICAgICB2YWx1ZT1cIlJFTUlOREVSXCIgLz48L2VsLXNlbGVjdD48L2VsLWZvcm0taXRlbVxuICAgICAgICA+PGVsLWZvcm0taXRlbSBsYWJlbD1cIue6quimgeaIluaPkOmGkuWGheWuueW8leeUqFwiXG4gICAgICAgICAgPjxlbC1pbnB1dFxuICAgICAgICAgICAgdi1tb2RlbD1cImFjdGlvbi5yZWZlcmVuY2VcIlxuICAgICAgICAgICAgYXJpYS1sYWJlbD1cIue6quimgeaIluaPkOmGkuWGheWuueW8leeUqFwiXG4gICAgICAgICAgICB0eXBlPVwidGV4dGFyZWFcIiAvPjwvZWwtZm9ybS1pdGVtXG4gICAgICAgID48ZWwtZm9ybS1pdGVtIHYtaWY9XCJhY3Rpb24ua2luZCA9PT0gJ1JFTUlOREVSJ1wiIGxhYmVsPVwi56uZ5YaF5o6l5pS25Lq6SURcIlxuICAgICAgICAgID48ZWwtaW5wdXQtbnVtYmVyXG4gICAgICAgICAgICB2LW1vZGVsPVwiYWN0aW9uLnJlY2lwaWVudElkXCJcbiAgICAgICAgICAgIGFyaWEtbGFiZWw9XCLnq5nlhoXmjqXmlLbkurpJRFwiXG4gICAgICAgICAgICA6bWluPVwiMVwiIC8+PC9lbC1mb3JtLWl0ZW1cbiAgICAgICAgPjxwXG4gICAgICAgICAgPuaPkOmGkuS7heS/neWtmOWIsOmcgOaxguWNj+S9nOiusOW9le+8jOWvueacieadg+aOpeaUtuS6uuWPr+inge+8m+S4jeiwg+eUqOefreS/oeOAgemCruS7tuaIluato+W8j+a2iOaBr+WkluWPkeOAgjwvcFxuICAgICAgICA+PC90ZW1wbGF0ZVxuICAgICAgPlxuICAgICAgPGVsLWZvcm0taXRlbSBsYWJlbD1cIuacrOasoeaTjeS9nOS+neaNrlwiXG4gICAgICAgID48ZWwtaW5wdXQgdi1tb2RlbD1cImFjdGlvbi5yZWFzb25cIiBhcmlhLWxhYmVsPVwi5pys5qyh5pON5L2c5L6d5o2uXCIgdHlwZT1cInRleHRhcmVhXCJcbiAgICAgIC8+PC9lbC1mb3JtLWl0ZW0+XG4gICAgPC9lbC1mb3JtPlxuICAgIDx0ZW1wbGF0ZSAjZm9vdGVyXG4gICAgICA+PGRpdiBjbGFzcz1cImRpYWxvZy1hY3Rpb25zXCJcbiAgICAgICAgPjxlbC1idXR0b24gQGNsaWNrPVwiYWN0aW9uVmlzaWJsZSA9IGZhbHNlXCI+5Y+W5raIPC9lbC1idXR0b25cbiAgICAgICAgPjxlbC1idXR0b24gdHlwZT1cInByaW1hcnlcIiA6bG9hZGluZz1cInNhdmluZ1wiIEBjbGljaz1cInNhdmVBY3Rpb25cIlxuICAgICAgICAgID7kv53lrZjni6znq4vmk43kvZw8L2VsLWJ1dHRvblxuICAgICAgICA+PC9kaXZcbiAgICAgID48L3RlbXBsYXRlXG4gICAgPlxuICA8L2VsLWRpYWxvZz5cbiAgPGVsLWRpYWxvZ1xuICAgIGNsYXNzPVwiaHJtLXJldmlldy1kaWFsb2dcIlxuICAgIHYtbW9kZWw9XCJoaXN0b3J5VmlzaWJsZVwiXG4gICAgdGl0bGU9XCLnnJ/lrp7ljoblj7LkuI7niYjmnKzlt67lvIJcIlxuICAgIHdpZHRoPVwibWluKDkwMHB4LCA5NnZ3KVwiXG4gID5cbiAgICA8ZGl2IGNsYXNzPVwidG9vbGJhclwiXG4gICAgICA+PGVsLXNlbGVjdCB2LW1vZGVsPVwiY29tcGFyZUFcIiBhcmlhLWxhYmVsPVwi5q+U6L6D5pen54mIXCJcbiAgICAgICAgPjxlbC1vcHRpb25cbiAgICAgICAgICB2LWZvcj1cImggaW4gaGlzdG9yaWVzXCJcbiAgICAgICAgICA6a2V5PVwiaC5pZFwiXG4gICAgICAgICAgOmxhYmVsPVwiYOeJiOacrCR7aC52ZXJzaW9ufWBcIlxuICAgICAgICAgIDp2YWx1ZT1cImgudmVyc2lvblwiIC8+PC9lbC1zZWxlY3RcbiAgICAgID48ZWwtc2VsZWN0IHYtbW9kZWw9XCJjb21wYXJlQlwiIGFyaWEtbGFiZWw9XCLmr5TovoPmlrDniYhcIlxuICAgICAgICA+PGVsLW9wdGlvblxuICAgICAgICAgIHYtZm9yPVwiaCBpbiBoaXN0b3JpZXNcIlxuICAgICAgICAgIDprZXk9XCJoLmlkXCJcbiAgICAgICAgICA6bGFiZWw9XCJg54mI5pysJHtoLnZlcnNpb259YFwiXG4gICAgICAgICAgOnZhbHVlPVwiaC52ZXJzaW9uXCIgLz48L2VsLXNlbGVjdFxuICAgID48L2Rpdj5cbiAgICA8ZGl2IGNsYXNzPVwid2lkZS10YWJsZVwiXG4gICAgICA+PHRhYmxlXG4gICAgICAgID48dGhlYWRcbiAgICAgICAgICA+PHRyPjx0aD7lrZfmrrU8L3RoPjx0aD7ml6fniYg8L3RoPjx0aD7mlrDniYg8L3RoPjwvdHI+PC90aGVhZFxuICAgICAgICA+PHRib2R5XG4gICAgICAgICAgPjx0ciB2LWZvcj1cImRpZmYgaW4gZGlmZmVyZW5jZXNcIiA6a2V5PVwiZGlmZi5rZXlcIlxuICAgICAgICAgICAgPjx0ZD57eyBkaWZmLmtleSB9fTwvdGRcbiAgICAgICAgICAgID48dGQ+e3sgZGlmZi5iZWZvcmUgfX08L3RkXG4gICAgICAgICAgICA+PHRkPnt7IGRpZmYuYWZ0ZXIgfX08L3RkPjwvdHJcbiAgICAgICAgICA+PC90Ym9keVxuICAgICAgICA+PC90YWJsZVxuICAgICAgPjwvZGl2XG4gICAgPlxuICAgIDxkaXYgdi1mb3I9XCJoIGluIGhpc3Rvcmllc1wiIDprZXk9XCJoLmlkXCIgY2xhc3M9XCJzb3VyY2UtY2FyZFwiXG4gICAgICA+PHN0cm9uZz7niYjmnKx7eyBoLnZlcnNpb24gfX0gwrcg5pON5L2c5Lq6e3sgaC5hY3RvcklkIH19PC9zdHJvbmdcbiAgICAgID48cHJlPnt7IEpTT04uc3RyaW5naWZ5KHBhcnNlKGguc25hcHNob3QpLCBudWxsLCAyKSB9fTwvcHJlPlxuICAgIDwvZGl2PlxuICAgIDx0ZW1wbGF0ZSAjZm9vdGVyPjxlbC1idXR0b24gQGNsaWNrPVwiaGlzdG9yeVZpc2libGUgPSBmYWxzZVwiPuWFs+mXreWOhuWPsjwvZWwtYnV0dG9uPjwvdGVtcGxhdGU+XG4gIDwvZWwtZGlhbG9nPlxuICA8ZWwtZGlhbG9nXG4gICAgY2xhc3M9XCJocm0tcmV2aWV3LWRpYWxvZ1wiXG4gICAgdi1tb2RlbD1cImRyaWxsVmlzaWJsZVwiXG4gICAgdGl0bGU9XCLnu5/orqHpm4blkIjkuIvpkrtcIlxuICAgIHdpZHRoPVwibWluKDc2MHB4LCA5NnZ3KVwiXG4gID5cbiAgICA8cHJlPnt7IGRyaWxsRGF0YSB9fTwvcHJlXG4gICAgPjx0ZW1wbGF0ZSAjZm9vdGVyXG4gICAgICA+PGVsLWJ1dHRvbiBAY2xpY2s9XCJkcmlsbFZpc2libGUgPSBmYWxzZVwiPuWFs+mXrembhuWQiDwvZWwtYnV0dG9uPjwvdGVtcGxhdGVcbiAgICA+PC9lbC1kaWFsb2dcbiAgPlxuICA8ZWwtZGlhbG9nXG4gICAgY2xhc3M9XCJocm0tcmV2aWV3LWRpYWxvZ1wiXG4gICAgdi1tb2RlbD1cImJhdGNoVmlzaWJsZVwiXG4gICAgdGl0bGU9XCLpgJDlr7nosaHmibnph4/liIbmtL5cIlxuICAgIHdpZHRoPVwibWluKDYyMHB4LCA5NnZ3KVwiXG4gICAgPjxwPuavj+adoeeLrOeri+ajgOafpeWIhua0vuS6uuWKqOS9nOOAgeebruagh+i1hOagvOWSjOeJiOacrO+8m+mDqOWIhuWksei0peS4jeaSpOmUgOWFtuS7luWQiOazleaIkOWKn+OAgjwvcFxuICAgID48ZWwtaW5wdXQtbnVtYmVyIHYtbW9kZWw9XCJiYXRjaFJldmlld2VyXCIgYXJpYS1sYWJlbD1cIuaJuemHj+ivhOWuoei0n+i0o+S6uklEXCIgOm1pbj1cIjFcIiAvPjxlbC1idXR0b25cbiAgICAgIDpsb2FkaW5nPVwic2F2aW5nXCJcbiAgICAgIEBjbGljaz1cImFzc2lnbkJhdGNoXCJcbiAgICAgID7miafooYzmibnph4/liIbmtL48L2VsLWJ1dHRvblxuICAgID48cHJlPnt7IGJhdGNoUmVzdWx0IH19PC9wcmU+XG4gIDwvZWwtZGlhbG9nPlxuICA8ZWwtZGlhbG9nXG4gICAgY2xhc3M9XCJocm0tcmV2aWV3LWRpYWxvZ1wiXG4gICAgdi1tb2RlbD1cInNsaXBWaXNpYmxlXCJcbiAgICB0aXRsZT1cIuiZmuaehOW3pei1hOadoemihOiniFwiXG4gICAgd2lkdGg9XCJtaW4oNjIwcHgsIDk2dncpXCJcbiAgICA+PHA+56S65L6L5pWw5o2u77yM5LuF55So5LqO6K6o6K66IMK3IHt7IGRlbW8uZW1wbG95ZWUgfX0gwrcge3sgZGVtby5wZXJpb2QgfX08L3BcbiAgICA+PHBcbiAgICAgID7lupTlj5Ege3sgcmVzdWx0Lmdyb3NzIH19IOWFg++8m+S4quS6uuaJo+asviB7eyByZXN1bHQucGVyc29uYWwgfX0g5YWD77yb56iOIHt7IHJlc3VsdC50YXggfX0g5YWD77yb5a6e5Y+RXG4gICAgICB7eyByZXN1bHQubmV0IH19IOWFg+OAgjwvcFxuICAgID48cD7ljZXkvY3miJDmnKwge3sgcmVzdWx0LmNvc3QgfX0g5YWD44CC5pyq5Lqn55Sf55yf5a6e5bel6LWE5p2h44CB5oqV6YCS44CB5a6h5om55oiW5LuY5qy+44CCPC9wXG4gICAgPjx0ZW1wbGF0ZSAjZm9vdGVyXG4gICAgICA+PGVsLWJ1dHRvbiBAY2xpY2s9XCJzbGlwVmlzaWJsZSA9IGZhbHNlXCI+5YWz6Zet5bel6LWE5p2h6aKE6KeIPC9lbC1idXR0b24+PC90ZW1wbGF0ZVxuICAgID48L2VsLWRpYWxvZ1xuICA+XG48L3RlbXBsYXRlPlxuXG48c2NyaXB0IHNldHVwIGxhbmc9XCJ0c1wiPlxuaW1wb3J0IHsgbmV4dFRpY2sgfSBmcm9tICd2dWUnXG5pbXBvcnQgeyBvcGluaW9uTWlzc2luZywgZmFpbHVyZUd1aWRhbmNlIH0gZnJvbSAnLi92YWxpZGF0aW9uJ1xuaW1wb3J0IHsgY2hlY2tQZXJtaSB9IGZyb20gJ0AvdXRpbHMvcGVybWlzc2lvbidcbmltcG9ydCAqIGFzIGFwaSBmcm9tICdAL2FwaS9ocm0vcGF5cm9sbC9yZXF1aXJlbWVudCdcbmltcG9ydCAqIGFzIHJldmlldyBmcm9tICdAL2FwaS9ocm0vcGF5cm9sbC9yZXZpZXcnXG5pbXBvcnQgeyBsaXN0V29ya2JlbmNoLCB0eXBlIFdvcmtiZW5jaFJlY29yZCB9IGZyb20gJ0AvYXBpL2hybS9wYXlyb2xsL3dvcmtiZW5jaCdcbmltcG9ydCB7IHBheXJvbGxNb2R1bGVzIGFzIG1vZHVsZXMsIHNvdXJjZXNGb3JNb2R1bGVzLCB0eXBlIFBheXJvbGxNb2R1bGUgfSBmcm9tICcuLi9yZXF1aXJlbWVudHMnXG5pbXBvcnQge1xuICBkZW1vLFxuICByZXN1bHQsXG4gIGNhbGN1bGF0ZURlbW8sXG4gIHJldmlld09yZGVyIGFzIG9yZGVyLFxuICByZXZpZXdWaWV3cyxcbiAgcmV2aWV3VG9waWNzLFxuICBkZW1vVHJlbmQsXG4gIGRlbW9Db3N0U3RydWN0dXJlXG59IGZyb20gJy4vZGVtbydcbmltcG9ydCBTY29wZUZpZWxkcyBmcm9tICcuLi9yZXF1aXJlbWVudC9TY29wZUZpZWxkcy52dWUnXG5jb25zdCByb3V0ZSA9IHVzZVJvdXRlKCksXG4gIHJvdXRlciA9IHVzZVJvdXRlcigpLFxuICBtZXNzYWdlID0gdXNlTWVzc2FnZSgpXG5jb25zdCBtb2R1bGUgPSBjb21wdXRlZDxQYXlyb2xsTW9kdWxlPigoKSA9PlxuICBvcmRlci5pbmNsdWRlcyhyb3V0ZS5xdWVyeS5tb2R1bGUgYXMgUGF5cm9sbE1vZHVsZSkgPyAocm91dGUucXVlcnkubW9kdWxlIGFzIFBheXJvbGxNb2R1bGUpIDogJ09WJ1xuKVxuY29uc3QgcHJvamVjdElkID0gY29tcHV0ZWQoKCkgPT4gTnVtYmVyKHJvdXRlLnF1ZXJ5LnByb2plY3RJZCkgfHwgdW5kZWZpbmVkKVxuY29uc3QgdmlldyA9IGNvbXB1dGVkKCgpID0+IHJldmlld1ZpZXdzW21vZHVsZS52YWx1ZV0pLFxuICBzdGVwID0gY29tcHV0ZWQoKCkgPT4gb3JkZXIuaW5kZXhPZihtb2R1bGUudmFsdWUpKVxuY29uc3QgcHJvamVjdHMgPSByZWY8YXBpLlBheXJvbGxSZXF1aXJlbWVudFByb2plY3RbXT4oW10pLFxuICBwcm9qZWN0ID0gY29tcHV0ZWQoKCkgPT4gcHJvamVjdHMudmFsdWUuZmluZCgocCkgPT4gcC5pZCA9PT0gcHJvamVjdElkLnZhbHVlKSlcbmNvbnN0IHJvd3MgPSByZWY8YW55W10+KFtdKSxcbiAgc291cmNlcyA9IHJlZjxXb3JrYmVuY2hSZWNvcmRbXT4oW10pLFxuICBzb3VyY2VJZCA9IHJlZjxudW1iZXI+KCksXG4gIHN0YXRzID0gcmVmPGFueT4oKSxcbiAgY2FwYWJpbGl0aWVzID0gcmVmPFJlY29yZDxzdHJpbmcsIGJvb2xlYW4+Pih7fSlcbmNvbnN0IHJlbWluZGVycyA9IHJlZjxhbnlbXT4oW10pXG5jb25zdCB0b3RhbCA9IHJlZigwKSxcbiAgcGFnZU5vID0gcmVmKDEpLFxuICBzb3J0ID0gcmVmKCdjb2RlJyksXG4gIHNlbGVjdGVkID0gcmVmPGFueVtdPihbXSksXG4gIGVycm9yID0gcmVmKCcnKSxcbiAgbG9hZGluZyA9IHJlZihmYWxzZSlcbmNvbnN0IGZpbHRlcnMgPSByZWFjdGl2ZSh7IHRleHQ6ICcnLCBzdGF0dXM6ICcnLCBkZWNpc2lvbjogJycgfSksXG4gIGFwcGxpZWQgPSByZWYoeyB0ZXh0OiAnJywgc3RhdHVzOiAnJywgZGVjaXNpb246ICcnIH0pXG5jb25zdCBjYW5kaWRhdGVzID0gY29tcHV0ZWQoKCkgPT4gc291cmNlc0Zvck1vZHVsZXMoW21vZHVsZS52YWx1ZV0pKVxuY29uc3Qgc291cmNlRGVzY3JpcHRpb24gPSBjb21wdXRlZCgoKSA9PiB7XG4gIGNvbnN0IHMgPSBzb3VyY2VzLnZhbHVlLmZpbmQoKHMpID0+IHMuaWQgPT09IHNvdXJjZUlkLnZhbHVlKVxuICByZXR1cm4gc1xuICAgID8gYOadpea6kCR7cy5pZH0gwrcg5L6b57uZJHtzLmRhdGEuc3VwcGx5fSDCtyDmnaXmupDotJ/otKPkuroke3MuZGF0YS5vd25lcklkfSDCtyDor63kuYkv6L+Q6JClL+S6pOS7mCR7cy5kYXRhLnNlbWFudGljVmVyc2lvbn0vJHtzLmRhdGEub3BlcmF0aW9uYWxWZXJzaW9ufS8ke3MuZGF0YS5kZWxpdmVyeVZlcnNpb259YFxuICAgIDogJ+W9k+WJjeadpea6kOS4jeWPr+ingeaIluW3suaSpOmUgO+8jOivt+WIt+aWsCdcbn0pXG5jb25zdCBjYW5DcmVhdGUgPSBjb21wdXRlZChcbiAgKCkgPT5cbiAgICAhIXByb2plY3RJZC52YWx1ZSAmJiAhIWNhcGFiaWxpdGllcy52YWx1ZS5FRElUICYmIGNoZWNrUGVybWkoWydocm06cGF5cm9sbDpyZXF1aXJlbWVudDp1cGRhdGUnXSlcbilcbmNvbnN0IHN0YXR1c2VzOiBSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+ID0ge1xuICBQRU5ESU5HOiAn5b6F56Gu6K6kJyxcbiAgUkVWSUVXSU5HOiAn6K+E5a6h5LitJyxcbiAgQ09ORklSTUVEOiAn5bey56Gu6K6kJyxcbiAgRElTUFVURUQ6ICfmnInlvILorq4nXG59XG5jb25zdCBkZWNpc2lvbnM6IFJlY29yZDxzdHJpbmcsIHN0cmluZz4gPSB7XG4gIENBTkRJREFURTogJ+WAmemAiScsXG4gIElOQ0xVREVEOiAn57qz5YWl5pys5pyfJyxcbiAgREVGRVJSRUQ6ICfmmoLnvJMnLFxuICBSRU1PVkVEOiAn56e76Zmk5L2c5bqfJ1xufVxuY29uc3QgaW1wbGVtZW50YXRpb25zOiBSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+ID0ge1xuICBVTlNDSEVEVUxFRDogJ+acquaOkuacnycsXG4gIERFVkVMT1BJTkc6ICflvIDlj5HkuK0nLFxuICBBQ0NFUFRBTkNFX1BFTkRJTkc6ICflvoXpqozmlLYnLFxuICBBQ0NFUFRFRDogJ+mqjOaUtumAmui/h++8iOS6uuW3peiusOW9le+8iSdcbn1cbmNvbnN0IHJlYWRpbmVzczogUmVjb3JkPHN0cmluZywgc3RyaW5nPiA9IHtcbiAgTUlTU0lORzogJ+acquWwsee7qicsXG4gIFBBUlRJQUw6ICfpg6jliIblsLHnu6onLFxuICBSRUFEWTogJ+W3suaguOmqjOWPr+eUqCcsXG4gIEVYRU1QVDogJ+S4jemAgueUqCdcbn1cbmNvbnN0IHJhdGlvS2V5cyA9IFtcbiAgJ2NvbmZpcm1hdGlvbicsXG4gICdzY29wZURpc3Bvc2l0aW9uJyxcbiAgJ2luY2x1ZGVkQ29uZmlybWF0aW9uJyxcbiAgJ3JlYWRpbmVzcycsXG4gICdpbmNsdWRlZFJlYWRpbmVzcydcbl1cbmNvbnN0IHJhdGlvTGFiZWxzOiBSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+ID0ge1xuICBjb25maXJtYXRpb246ICflip/og73noa7orqTnjocnLFxuICBzY29wZURpc3Bvc2l0aW9uOiAn6IyD5Zu05aSE572u546HJyxcbiAgaW5jbHVkZWRDb25maXJtYXRpb246ICfmnKzmnJ/noa7orqTnjocnLFxuICByZWFkaW5lc3M6ICfmnaXmupDlj6/nlKjnjocnLFxuICBpbmNsdWRlZFJlYWRpbmVzczogJ+acrOacn+adpea6kOWPr+eUqOeOhydcbn1cbmNvbnN0IHBhcnNlID0gKHZhbHVlPzogc3RyaW5nKSA9PiB7XG4gIHRyeSB7XG4gICAgcmV0dXJuIHZhbHVlID8gSlNPTi5wYXJzZSh2YWx1ZSkgOiB7fVxuICB9IGNhdGNoIHtcbiAgICByZXR1cm4ge31cbiAgfVxufVxuY29uc3QgcGFyYW1zID0gKCkgPT4gKHtcbiAgcHJvamVjdElkOiBwcm9qZWN0SWQudmFsdWUsXG4gIG1vZHVsZTogbW9kdWxlLnZhbHVlID09PSAnQ09MJyA/IHVuZGVmaW5lZCA6IG1vZHVsZS52YWx1ZSxcbiAgLi4uT2JqZWN0LmZyb21FbnRyaWVzKE9iamVjdC5lbnRyaWVzKGFwcGxpZWQudmFsdWUpLm1hcCgoW2ssIHZdKSA9PiBbaywgdiB8fCB1bmRlZmluZWRdKSlcbn0pXG5sZXQgbG9hZFRpY2tldCA9IDBcbmFzeW5jIGZ1bmN0aW9uIGxvYWQoKSB7XG4gIGNvbnN0IHRpY2tldCA9ICsrbG9hZFRpY2tldFxuICBsb2FkaW5nLnZhbHVlID0gdHJ1ZVxuICBlcnJvci52YWx1ZSA9ICcnXG4gIHJvd3MudmFsdWUgPSBbXVxuICBzdGF0cy52YWx1ZSA9IHVuZGVmaW5lZFxuICBzb3VyY2VzLnZhbHVlID0gW11cbiAgY2FwYWJpbGl0aWVzLnZhbHVlID0ge31cbiAgcmVtaW5kZXJzLnZhbHVlID0gW11cbiAgc2VsZWN0ZWQudmFsdWUgPSBbXVxuICB0cnkge1xuICAgIGNvbnN0IHBzID0gYXdhaXQgYXBpLmxpc3RSZXF1aXJlbWVudFByb2plY3RzKClcbiAgICBpZiAodGlja2V0ICE9PSBsb2FkVGlja2V0KSByZXR1cm5cbiAgICBwcm9qZWN0cy52YWx1ZSA9IHBzXG4gICAgaWYgKCFwcm9qZWN0SWQudmFsdWUpIHtcbiAgICAgIGVycm9yLnZhbHVlID0gJ+ivt+mAieaLqeacieadg+ivhOWuoemhueebru+8m+acquaOqOWumuS7u+S9leiMg+WbtOaIluaJueWHhuOAgidcbiAgICAgIHJldHVyblxuICAgIH1cbiAgICBjb25zdCBbcGFnZSwgc3RhdCwgc3JjLCBjYXBzLCBpbmJveF0gPSBhd2FpdCBQcm9taXNlLmFsbChbXG4gICAgICByZXZpZXcucmV2aWV3UGFnZSh7IC4uLnBhcmFtcygpLCBwYWdlTm86IHBhZ2VOby52YWx1ZSwgcGFnZVNpemU6IDEwLCBzb3J0OiBzb3J0LnZhbHVlIH0pLFxuICAgICAgcmV2aWV3LnJldmlld1N0YXRpc3RpY3MocGFyYW1zKCkpLFxuICAgICAgbGlzdFdvcmtiZW5jaChwcm9qZWN0SWQudmFsdWUsICdTT1VSQ0UnKSxcbiAgICAgIGFwaS5yZXF1aXJlbWVudENhcGFiaWxpdGllcyhwcm9qZWN0SWQudmFsdWUpLFxuICAgICAgcmV2aWV3LnJldmlld1JlbWluZGVycyhwcm9qZWN0SWQudmFsdWUpXG4gICAgXSlcbiAgICBpZiAodGlja2V0ICE9PSBsb2FkVGlja2V0KSByZXR1cm5cbiAgICByb3dzLnZhbHVlID0gcGFnZS5saXN0XG4gICAgdG90YWwudmFsdWUgPSBwYWdlLnRvdGFsXG4gICAgc3RhdHMudmFsdWUgPSBzdGF0XG4gICAgc291cmNlcy52YWx1ZSA9IHNyY1xuICAgIGNhcGFiaWxpdGllcy52YWx1ZSA9IGNhcHNcbiAgICByZW1pbmRlcnMudmFsdWUgPSBpbmJveFxuICAgIGF3YWl0IG5leHRUaWNrKClcbiAgICBmb2N1c0FuY2hvcigpXG4gIH0gY2F0Y2gge1xuICAgIGlmICh0aWNrZXQgPT09IGxvYWRUaWNrZXQpXG4gICAgICBlcnJvci52YWx1ZSA9ICfnnJ/lrp7or4TlrqHmlbDmja7liqDovb3lpLHotKXvvJrlj6/og73ml6DmnYPmiJbmjqXlj6PlvILluLjjgILor7fph43or5XvvJvmnKrliqDovb3lhoXlrrnkuI3mmL7npLrkuLrpm7bmiJbpgJrov4fjgIInXG4gIH0gZmluYWxseSB7XG4gICAgaWYgKHRpY2tldCA9PT0gbG9hZFRpY2tldCkgbG9hZGluZy52YWx1ZSA9IGZhbHNlXG4gIH1cbn1cbmFzeW5jIGZ1bmN0aW9uIGdvKG06IFBheXJvbGxNb2R1bGUsIGFuY2hvcj86IHN0cmluZykge1xuICBwYWdlTm8udmFsdWUgPSAxXG4gIE9iamVjdC5hc3NpZ24oZmlsdGVycywgeyB0ZXh0OiAnJywgc3RhdHVzOiAnJywgZGVjaXNpb246ICcnIH0pXG4gIGFwcGxpZWQudmFsdWUgPSB7IC4uLmZpbHRlcnMgfVxuICBpZiAobW9kdWxlLnZhbHVlID09PSBtICYmIFN0cmluZyhyb3V0ZS5xdWVyeS5hbmNob3IgfHwgJycpID09PSBTdHJpbmcoYW5jaG9yIHx8ICcnKSkge1xuICAgIGF3YWl0IGxvYWQoKVxuICB9IGVsc2Uge1xuICAgIGF3YWl0IHJvdXRlci5wdXNoKHtcbiAgICAgIHBhdGg6ICcvaHJtLXBheXJvbGwvcmV2aWV3JyxcbiAgICAgIHF1ZXJ5OiB7IHByb2plY3RJZDogcHJvamVjdElkLnZhbHVlLCBtb2R1bGU6IG0sIGFuY2hvciB9XG4gICAgfSlcbiAgfVxuICBhd2FpdCBuZXh0VGljaygpXG4gIGZvY3VzQW5jaG9yKClcbn1cbmZ1bmN0aW9uIHNlbGVjdFByb2plY3QoaWQ6IG51bWJlcikge1xuICBmb3JtVmlzaWJsZS52YWx1ZSA9IGZhbHNlXG4gIGFjdGlvblZpc2libGUudmFsdWUgPSBmYWxzZVxuICBoaXN0b3J5VmlzaWJsZS52YWx1ZSA9IGZhbHNlXG4gIHJldHVybiByb3V0ZXIucHVzaCh7XG4gICAgcGF0aDogJy9ocm0tcGF5cm9sbC9yZXZpZXcnLFxuICAgIHF1ZXJ5OiB7IHByb2plY3RJZDogaWQsIG1vZHVsZTogbW9kdWxlLnZhbHVlIH1cbiAgfSlcbn1cbmZ1bmN0aW9uIGNlbnRyYWwoKSB7XG4gIHJldHVybiBnbygnQ09MJylcbn1cbmZ1bmN0aW9uIG9yaWdpbmFsKCkge1xuICByZXR1cm4gcm91dGVyLnB1c2goJy9ocm0tcGF5cm9sbC9vdmVydmlldycpXG59XG5mdW5jdGlvbiBvcGVuU291cmNlV29ya2JlbmNoKCkge1xuICByZXR1cm4gcm91dGVyLnB1c2goe1xuICAgIHBhdGg6ICcvaHJtLXBheXJvbGwvcmVxdWlyZW1lbnQnLFxuICAgIHF1ZXJ5OiB7IHByb2plY3RJZDogcHJvamVjdElkLnZhbHVlLCBtb2R1bGU6IG1vZHVsZS52YWx1ZSwgZnJvbTogcm91dGUuZnVsbFBhdGggfVxuICB9KVxufVxuZnVuY3Rpb24gZm9jdXNBbmNob3IoKSB7XG4gIGNvbnN0IGFuY2hvciA9IFN0cmluZyhyb3V0ZS5xdWVyeS5hbmNob3IgfHwgJycpXG4gIGlmICgvXihtb2R1bGV8ZmllbGQtW2EtejAtOS1dK3xvcGVyYXRpb24tW2EtejAtOS1dK3xmbG93LVthLXowLTktXSspJC8udGVzdChhbmNob3IpKSB7XG4gICAgY29uc3QgZWwgPSBkb2N1bWVudC5nZXRFbGVtZW50QnlJZChhbmNob3IpXG4gICAgZWw/LnNjcm9sbEludG9WaWV3KHsgYmxvY2s6ICdjZW50ZXInIH0pXG4gICAgZWw/LmZvY3VzKClcbiAgfVxufVxuZnVuY3Rpb24gcmVzZXRGb2N1cygpIHtcbiAgZG9jdW1lbnQuZ2V0RWxlbWVudEJ5SWQoJ21vZHVsZScpPy5mb2N1cygpXG59XG5mdW5jdGlvbiBzZWFyY2goKSB7XG4gIHBhZ2VOby52YWx1ZSA9IDFcbiAgYXBwbGllZC52YWx1ZSA9IHsgLi4uZmlsdGVycyB9XG4gIGxvYWQoKVxufVxuZnVuY3Rpb24gY2xlYXJGaWx0ZXJzKCkge1xuICBPYmplY3QuYXNzaWduKGZpbHRlcnMsIHsgdGV4dDogJycsIHN0YXR1czogJycsIGRlY2lzaW9uOiAnJyB9KVxuICBzZWFyY2goKVxufVxuZnVuY3Rpb24gYWxsb3dlZChyb3c6IGFueSwgYWN0aW9uOiBzdHJpbmcpIHtcbiAgcmV0dXJuIChcbiAgICByb3cucGVybWl0dGVkQWN0aW9ucz8uaW5jbHVkZXMoYWN0aW9uKSAmJlxuICAgIChhY3Rpb24gIT09ICdFRElUJyB8fCBjaGVja1Blcm1pKFsnaHJtOnBheXJvbGw6cmVxdWlyZW1lbnQ6dXBkYXRlJ10pKVxuICApXG59XG5mdW5jdGlvbiByZXR1cm5Ubyhyb3c6IGFueSkge1xuICBjb25zdCBjID0gcGFyc2Uocm93LmJ1c2luZXNzQ29udGV4dEpzb24pXG4gIHJldHVybiBnbyhvcmRlci5pbmNsdWRlcyhjLmNvbnRleHRNb2R1bGUpID8gYy5jb250ZXh0TW9kdWxlIDogcm93Lm1vZHVsZSwgYy5hbmNob3IgfHwgJ21vZHVsZScpXG59XG5hc3luYyBmdW5jdGlvbiBpbml0aWFsaXplKCkge1xuICBpZiAocHJvamVjdElkLnZhbHVlKSB7XG4gICAgYXdhaXQgYXBpLmluaXRpYWxpemVSZXF1aXJlbWVudHMocHJvamVjdElkLnZhbHVlKVxuICAgIGF3YWl0IGxvYWQoKVxuICB9XG59XG5hc3luYyBmdW5jdGlvbiBkb3dubG9hZCgpIHtcbiAgY29uc3QgZGF0YSA9IGF3YWl0IHJldmlldy5yZXZpZXdFeHBvcnQocGFyYW1zKCkpXG4gIGNvbnN0IHVybCA9IFVSTC5jcmVhdGVPYmplY3RVUkwoXG4gICAgbmV3IEJsb2IoW0pTT04uc3RyaW5naWZ5KGRhdGEsIG51bGwsIDIpXSwgeyB0eXBlOiAnYXBwbGljYXRpb24vanNvbicgfSlcbiAgKVxuICBjb25zdCBhID0gZG9jdW1lbnQuY3JlYXRlRWxlbWVudCgnYScpXG4gIGEuaHJlZiA9IHVybFxuICBhLmRvd25sb2FkID0gJ+W9k+WJjeetm+mAieivhOWuoea4heWNlS5qc29uJ1xuICBhLmNsaWNrKClcbiAgVVJMLnJldm9rZU9iamVjdFVSTCh1cmwpXG59XG5jb25zdCBkZW1vQWRqdXN0bWVudCA9IHJlZigxMDUwMClcbmNvbnN0IGRlbW9TdGF0ZSA9IHJlZign56S65L6L5bCa5pyq5omn6KGM77yb5LiN5Luj6KGo55yf5a6e6Jaq6YWs54q25oCBJyksXG4gIHNsaXBWaXNpYmxlID0gcmVmKGZhbHNlKVxuZnVuY3Rpb24gc2ltdWxhdGUoKSB7XG4gIGNvbnN0IHggPSBjYWxjdWxhdGVEZW1vKClcbiAgZGVtb1N0YXRlLnZhbHVlID0gYOekuuS+i+ivleeul++8muW6lOWPkSR7eC5ncm9zc33lhYPvvIzkuKrnqI4ke3gudGF4feWFg++8jOWunuWPkSR7eC5uZXR95YWD77yb5LuF6aG16Z2i5YaF5a2Y77yM5peg5q2j5byP5Lia5Yqh5YaZ5YWl44CCYFxufVxuY29uc3QgY29uZmxpY3RMYXRlc3QgPSByZWY8YW55PigpXG5jb25zdCBmb3JtVmlzaWJsZSA9IHJlZihmYWxzZSksXG4gIGZvcm0gPSByZWY8YW55Pih7fSksXG4gIGJ1c2luZXNzID0gcmVmPFJlY29yZDxzdHJpbmcsIGFueT4+KHt9KSxcbiAgc2F2aW5nID0gcmVmKGZhbHNlKSxcbiAgc2F2ZUVycm9yID0gcmVmKCcnKVxuY29uc3QgYnVzaW5lc3NGaWVsZHMgPSBbXG4gIHsga2V5OiAncm9sZScsIGxhYmVsOiAn5Lia5Yqh6KeS6ImyJyB9LFxuICB7IGtleTogJ3NjZW5hcmlvJywgbGFiZWw6ICfop6blj5HlnLrmma8nIH0sXG4gIHsga2V5OiAnZ29hbCcsIGxhYmVsOiAn5Lia5Yqh55uu5qCHJyB9LFxuICB7IGtleTogJ2V4cGVjdGVkJywgbGFiZWw6ICfmnJ/mnJvooYzkuLonIH0sXG4gIHsga2V5OiAnbm9ybWFsQ2FzZScsIGxhYmVsOiAn5q2j5bi45qC35L6LJyB9LFxuICB7IGtleTogJ2V4Y2VwdGlvbkNhc2UnLCBsYWJlbDogJ+W8guW4uOagt+S+iycgfSxcbiAgeyBrZXk6ICdjYWxjdWxhdGlvbicsIGxhYmVsOiAn6K6h566X5oiW5Lia5Yqh6KeE5YiZ77yI5LiN6YCC55So6aG76K+05piO77yJJyB9LFxuICB7IGtleTogJ3VuaXQnLCBsYWJlbDogJ+WNleS9je+8iOS4jemAgueUqOmhu+ivtOaYju+8iScgfSxcbiAgeyBrZXk6ICdwcmVjaXNpb24nLCBsYWJlbDogJ+eyvuW6puS4juiIjeWFpe+8iOS4jemAgueUqOmhu+ivtOaYju+8iScgfSxcbiAgeyBrZXk6ICd2YWxpZGl0eScsIGxhYmVsOiAn5pyJ5pWI5pyf5oiW6YCC55So5L6d5o2uJyB9LFxuICB7IGtleTogJ3J1bGVFeGNlcHRpb24nLCBsYWJlbDogJ+inhOWImeS+i+WkluS4juWkhOeQhicgfVxuXVxuY29uc3Qgb3BpbmlvbkVycm9ycyA9IHJlZjxSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+Pih7fSlcbmFzeW5jIGZ1bmN0aW9uIGZvY3VzT3BpbmlvbkZpZWxkKGxhYmVsOiBzdHJpbmcpIHtcbiAgYXdhaXQgbmV4dFRpY2soKVxuICBjb25zdCBmaWVsZCA9IGRvY3VtZW50LnF1ZXJ5U2VsZWN0b3I8SFRNTEVsZW1lbnQ+KGAuaHJtLXJldmlldy1kaWFsb2cgW2FyaWEtbGFiZWw9XCIke2xhYmVsfVwiXWApXG4gIGZpZWxkPy5zY3JvbGxJbnRvVmlldyh7IGJsb2NrOiAnY2VudGVyJyB9KVxuICBmaWVsZD8uZm9jdXMoKVxufVxuZnVuY3Rpb24gbmV3T3Bpbmlvbih0YXJnZXRUeXBlOiBzdHJpbmcsIGFuY2hvcjogc3RyaW5nKSB7XG4gIG9waW5pb25FcnJvcnMudmFsdWUgPSB7fVxuICBjb25mbGljdExhdGVzdC52YWx1ZSA9IHVuZGVmaW5lZFxuICBmb3JtLnZhbHVlID0ge1xuICAgIHByb2plY3RJZDogcHJvamVjdElkLnZhbHVlLFxuICAgIHByb2plY3RTY29wZVZlcnNpb246IHByb2plY3QudmFsdWU/LnByb2plY3RTY29wZVZlcnNpb24sXG4gICAgY29kZTogJ0NVU1RPTS0wMDAwJyxcbiAgICBtb2R1bGU6IG1vZHVsZS52YWx1ZSxcbiAgICBzY29wZU1vZGU6ICdJTkhFUklUJyxcbiAgICBzdGF0dXM6ICdQRU5ESU5HJyxcbiAgICBwcmlvcml0eTogMixcbiAgICByZWFkaW5lc3M6ICdNSVNTSU5HJyxcbiAgICBzb3VyY2VBcHBsaWNhYmlsaXR5OiAnQVBQTElDQUJMRScsXG4gICAgZGVzY3JpcHRpb246ICcnXG4gIH1cbiAgYnVzaW5lc3MudmFsdWUgPSB7IHBhZ2U6ICcvaHJtLXBheXJvbGwvcmV2aWV3JywgY29udGV4dE1vZHVsZTogbW9kdWxlLnZhbHVlLCB0YXJnZXRUeXBlLCBhbmNob3IgfVxuICBzYXZlRXJyb3IudmFsdWUgPSAnJ1xuICBmb3JtVmlzaWJsZS52YWx1ZSA9IHRydWVcbn1cbmZ1bmN0aW9uIGVkaXQocm93OiBhbnkpIHtcbiAgb3BpbmlvbkVycm9ycy52YWx1ZSA9IHt9XG4gIGNvbmZsaWN0TGF0ZXN0LnZhbHVlID0gdW5kZWZpbmVkXG4gIGZvcm0udmFsdWUgPSB7XG4gICAgLi4ucm93LFxuICAgIHByb2plY3RTY29wZVZlcnNpb246IHByb2plY3QudmFsdWU/LnByb2plY3RTY29wZVZlcnNpb24sXG4gICAgc3RhdHVzOiByb3cuc3RhdHVzID09PSAnQ09ORklSTUVEJyA/ICdSRVZJRVdJTkcnIDogcm93LnN0YXR1c1xuICB9XG4gIGJ1c2luZXNzLnZhbHVlID0ge1xuICAgIHBhZ2U6ICcvaHJtLXBheXJvbGwvcmV2aWV3JyxcbiAgICBjb250ZXh0TW9kdWxlOiByb3cubW9kdWxlLFxuICAgIHRhcmdldFR5cGU6ICdNT0RVTEUnLFxuICAgIGFuY2hvcjogJ21vZHVsZScsXG4gICAgLi4ucGFyc2Uocm93LmJ1c2luZXNzQ29udGV4dEpzb24pXG4gIH1cbiAgc2F2ZUVycm9yLnZhbHVlID0gJydcbiAgZm9ybVZpc2libGUudmFsdWUgPSB0cnVlXG59XG5hc3luYyBmdW5jdGlvbiBzYXZlT3BpbmlvbigpIHtcbiAgb3BpbmlvbkVycm9ycy52YWx1ZSA9IG9waW5pb25NaXNzaW5nKGZvcm0udmFsdWUsIGJ1c2luZXNzLnZhbHVlKVxuICBpZiAoT2JqZWN0LmtleXMob3BpbmlvbkVycm9ycy52YWx1ZSkubGVuZ3RoKSB7XG4gICAgc2F2ZUVycm9yLnZhbHVlID0gYOivt+ihpem9kOS7peS4i+Wtl+aute+8miR7T2JqZWN0LnZhbHVlcyhvcGluaW9uRXJyb3JzLnZhbHVlKS5qb2luKCfjgIEnKX3jgILovpPlhaXlt7Lkv53nlZnjgIJgXG4gICAgYXdhaXQgZm9jdXNPcGluaW9uRmllbGQoT2JqZWN0LnZhbHVlcyhvcGluaW9uRXJyb3JzLnZhbHVlKVswXSlcbiAgICByZXR1cm5cbiAgfVxuICBzYXZpbmcudmFsdWUgPSB0cnVlXG4gIHNhdmVFcnJvci52YWx1ZSA9ICcnXG4gIHRyeSB7XG4gICAgaWYgKGZvcm0udmFsdWUucHJvamVjdElkICE9PSBwcm9qZWN0SWQudmFsdWUpIHRocm93IEVycm9yKCdjaGFuZ2VkIHByb2plY3QnKVxuICAgIGF3YWl0IGFwaS5zYXZlUmVxdWlyZW1lbnQoe1xuICAgICAgLi4uZm9ybS52YWx1ZSxcbiAgICAgIGJ1c2luZXNzQ29udGV4dEpzb246IEpTT04uc3RyaW5naWZ5KGJ1c2luZXNzLnZhbHVlKVxuICAgIH0pXG4gICAgZm9ybVZpc2libGUudmFsdWUgPSBmYWxzZVxuICAgIG1lc3NhZ2Uuc3VjY2Vzcygn5oSP6KeB5bey55yf5a6e5L+d5a2YJylcbiAgICBhd2FpdCBsb2FkKClcbiAgfSBjYXRjaCAoZXJyb3IpIHtcbiAgICBzYXZlRXJyb3IudmFsdWUgPSBmYWlsdXJlR3VpZGFuY2UoZXJyb3IpXG4gIH0gZmluYWxseSB7XG4gICAgc2F2aW5nLnZhbHVlID0gZmFsc2VcbiAgfVxufVxuYXN5bmMgZnVuY3Rpb24gcmVsb2FkQ29uZmxpY3QoKSB7XG4gIHRyeSB7XG4gICAgY29uc3QgbGF0ZXN0ID0gKGF3YWl0IGFwaS5saXN0UmVxdWlyZW1lbnRzKHVuZGVmaW5lZCwgcHJvamVjdElkLnZhbHVlKSkuZmluZChcbiAgICAgICh4KSA9PiB4LmlkID09PSBmb3JtLnZhbHVlLmlkXG4gICAgKVxuICAgIGlmIChsYXRlc3QpIHtcbiAgICAgIGNvbmZsaWN0TGF0ZXN0LnZhbHVlID0gbGF0ZXN0XG4gICAgICBmb3JtLnZhbHVlLnZlcnNpb24gPSBsYXRlc3QudmVyc2lvblxuICAgICAgZm9ybS52YWx1ZS5wcm9qZWN0U2NvcGVWZXJzaW9uID0gbGF0ZXN0LnByb2plY3RTY29wZVZlcnNpb25cbiAgICAgIHNhdmVFcnJvci52YWx1ZSA9IGDlt7Lor7vlj5bmnIDmlrDniYjmnKwke2xhdGVzdC52ZXJzaW9ufe+8jOi+k+WFpeS7jeS/neeVme+8m+ivt+aguOWvueacgOaWsOWKn+iDve+8miR7bGF0ZXN0LmRlc2NyaXB0aW9ufe+8jOWQiOW5tuWQjuWGjeS/neWtmOOAgmBcbiAgICB9XG4gIH0gY2F0Y2gge1xuICAgIHNhdmVFcnJvci52YWx1ZSA9ICfor7vlj5bmnIDmlrDniYjmnKzlpLHotKXvvIzovpPlhaXku43kv53nlZnjgIInXG4gIH1cbn1cbmNvbnN0IGFjdGlvblZpc2libGUgPSByZWYoZmFsc2UpLFxuICBhY3Rpb25LaW5kID0gcmVmKCdkZWNpc2lvbicpLFxuICBhY3RpdmUgPSByZWY8YW55PigpLFxuICBhY3Rpb24gPSByZWY8YW55Pih7fSlcbmNvbnN0IGFjdGlvblRpdGxlczogUmVjb3JkPHN0cmluZywgc3RyaW5nPiA9IHtcbiAgZGVjaXNpb246ICfni6znq4vojIPlm7TlhrPnrZYnLFxuICBpbXBsZW1lbnRhdGlvbjogJ+eLrOeri+WunuaWveWFs+iBlCcsXG4gIGNvbGxhYm9yYXRpb246ICfkvJrorq7nuqropoHkuI7lj5fmjqfnq5nlhoXmj5DphpInXG59XG5jb25zdCBpbXBsZW1lbnRhdGlvbkZpZWxkcyA9IFtcbiAgeyBrZXk6ICdwYWdlJywgbGFiZWw6ICflrp7njrDpobXpnaLlvJXnlKgnIH0sXG4gIHsga2V5OiAnYXBpJywgbGFiZWw6ICfmjqXlj6PlvJXnlKgnIH0sXG4gIHsga2V5OiAndGFzaycsIGxhYmVsOiAn5byA5Y+R5Lu75Yqh5byV55SoJyB9LFxuICB7IGtleTogJ2V2aWRlbmNlJywgbGFiZWw6ICfpqozmlLbor4Hmja7lvJXnlKgnIH1cbl1cbmZ1bmN0aW9uIG9wZW5BY3Rpb24ocm93OiBhbnksIGtpbmQ6IHN0cmluZykge1xuICBhY3RpdmUudmFsdWUgPSByb3dcbiAgYWN0aW9uS2luZC52YWx1ZSA9IGtpbmRcbiAgYWN0aW9uLnZhbHVlID1cbiAgICBraW5kID09PSAnZGVjaXNpb24nXG4gICAgICA/IHsgZGVjaXNpb246IHJvdy5zY29wZURlY2lzaW9uIHx8ICdDQU5ESURBVEUnLCByZW1vdmFsS2luZDogJ0RVUExJQ0FURScgfVxuICAgICAgOiBraW5kID09PSAnaW1wbGVtZW50YXRpb24nXG4gICAgICAgID8geyBzdGF0ZTogJ1VOU0NIRURVTEVEJywgLi4ucGFyc2Uocm93LmltcGxlbWVudGF0aW9uSnNvbikgfVxuICAgICAgICA6IHsga2luZDogJ01FRVRJTkcnIH1cbiAgc2F2ZUVycm9yLnZhbHVlID0gJydcbiAgYWN0aW9uVmlzaWJsZS52YWx1ZSA9IHRydWVcbn1cbmZ1bmN0aW9uIGNvbnRleHQocm93OiBhbnkpIHtcbiAgcmV0dXJuIHtcbiAgICBwcm9qZWN0SWQ6IHByb2plY3RJZC52YWx1ZSxcbiAgICBwcm9qZWN0U2NvcGVWZXJzaW9uOiBwcm9qZWN0LnZhbHVlPy5wcm9qZWN0U2NvcGVWZXJzaW9uLFxuICAgIGlkOiByb3cuaWQsXG4gICAgdmVyc2lvbjogcm93LnZlcnNpb24sXG4gICAgcmVhc29uOiAn6aG16Z2i6K+E5a6h5pON5L2cJ1xuICB9XG59XG5hc3luYyBmdW5jdGlvbiBzYXZlQWN0aW9uKCkge1xuICBzYXZpbmcudmFsdWUgPSB0cnVlXG4gIHNhdmVFcnJvci52YWx1ZSA9ICcnXG4gIHRyeSB7XG4gICAgY29uc3QgY29tbWFuZCA9IHsgLi4uY29udGV4dChhY3RpdmUudmFsdWUpLCAuLi5hY3Rpb24udmFsdWUgfVxuICAgIGNvbnN0IGZuID1cbiAgICAgIGFjdGlvbktpbmQudmFsdWUgPT09ICdkZWNpc2lvbidcbiAgICAgICAgPyByZXZpZXcuc2NvcGVEZWNpc2lvblxuICAgICAgICA6IGFjdGlvbktpbmQudmFsdWUgPT09ICdpbXBsZW1lbnRhdGlvbidcbiAgICAgICAgICA/IHJldmlldy5zYXZlSW1wbGVtZW50YXRpb25cbiAgICAgICAgICA6IHJldmlldy5zYXZlQ29sbGFib3JhdGlvblxuICAgIGF3YWl0IGZuKGNvbW1hbmQpXG4gICAgYWN0aW9uVmlzaWJsZS52YWx1ZSA9IGZhbHNlXG4gICAgYXdhaXQgbG9hZCgpXG4gIH0gY2F0Y2gge1xuICAgIHNhdmVFcnJvci52YWx1ZSA9ICfmk43kvZzmnKrlrozmiJDvvJrnvLrlv4XloavjgIHml6DmnYPjgIHlvJXnlKjml6DmlYjmiJbniYjmnKzlhrLnqoHjgILovpPlhaXlt7Lkv53nlZnvvIzor7fmoLjlr7njgIInXG4gIH0gZmluYWxseSB7XG4gICAgc2F2aW5nLnZhbHVlID0gZmFsc2VcbiAgfVxufVxuY29uc3QgaGlzdG9yeVZpc2libGUgPSByZWYoZmFsc2UpLFxuICBoaXN0b3JpZXMgPSByZWY8YW55W10+KFtdKSxcbiAgY29tcGFyZUEgPSByZWY8bnVtYmVyPigpLFxuICBjb21wYXJlQiA9IHJlZjxudW1iZXI+KClcbmFzeW5jIGZ1bmN0aW9uIHNob3dIaXN0b3J5KHJvdzogYW55KSB7XG4gIHRyeSB7XG4gICAgaGlzdG9yaWVzLnZhbHVlID0gYXdhaXQgYXBpLnJlcXVpcmVtZW50SGlzdG9yeShyb3cuaWQsIHByb2plY3RJZC52YWx1ZSlcbiAgICBjb25zdCB2ID0gaGlzdG9yaWVzLnZhbHVlLm1hcCgoaCkgPT4gaC52ZXJzaW9uKS5zb3J0KChhLCBiKSA9PiBhIC0gYilcbiAgICBjb21wYXJlQS52YWx1ZSA9IHYuYXQoLTIpID8/IHZbMF1cbiAgICBjb21wYXJlQi52YWx1ZSA9IHYuYXQoLTEpXG4gICAgaGlzdG9yeVZpc2libGUudmFsdWUgPSB0cnVlXG4gIH0gY2F0Y2gge1xuICAgIG1lc3NhZ2UuZXJyb3IoJ+WOhuWPsuivu+WPluWksei0pe+8jOS4jeiDveaOqOWumuaXoOWOhuWPsicpXG4gIH1cbn1cbmNvbnN0IGRpZmZlcmVuY2VzID0gY29tcHV0ZWQoKCkgPT4ge1xuICBjb25zdCBhID0gcGFyc2UoaGlzdG9yaWVzLnZhbHVlLmZpbmQoKGgpID0+IGgudmVyc2lvbiA9PT0gY29tcGFyZUEudmFsdWUpPy5zbmFwc2hvdCksXG4gICAgYiA9IHBhcnNlKGhpc3Rvcmllcy52YWx1ZS5maW5kKChoKSA9PiBoLnZlcnNpb24gPT09IGNvbXBhcmVCLnZhbHVlKT8uc25hcHNob3QpXG4gIHJldHVybiBbLi4ubmV3IFNldChbLi4uT2JqZWN0LmtleXMoYSksIC4uLk9iamVjdC5rZXlzKGIpXSldXG4gICAgLmZpbHRlcigoaykgPT4gSlNPTi5zdHJpbmdpZnkoYVtrXSkgIT09IEpTT04uc3RyaW5naWZ5KGJba10pKVxuICAgIC5tYXAoKGtleSkgPT4gKHsga2V5LCBiZWZvcmU6IEpTT04uc3RyaW5naWZ5KGFba2V5XSksIGFmdGVyOiBKU09OLnN0cmluZ2lmeShiW2tleV0pIH0pKVxufSlcbmNvbnN0IGRyaWxsVmlzaWJsZSA9IHJlZihmYWxzZSksXG4gIGRyaWxsRGF0YSA9IHJlZignJylcbmZ1bmN0aW9uIGRyaWxsKGtleTogc3RyaW5nKSB7XG4gIGlmIChzdGF0cy52YWx1ZSkge1xuICAgIGRyaWxsRGF0YS52YWx1ZSA9IEpTT04uc3RyaW5naWZ5KGtleSA9PT0gJ0wnID8gc3RhdHMudmFsdWUuTCA6IHN0YXRzLnZhbHVlLnNldHNba2V5XSwgbnVsbCwgMilcbiAgICBkcmlsbFZpc2libGUudmFsdWUgPSB0cnVlXG4gIH1cbn1cbmNvbnN0IGJhdGNoVmlzaWJsZSA9IHJlZihmYWxzZSksXG4gIGJhdGNoUmV2aWV3ZXIgPSByZWY8bnVtYmVyPigpLFxuICBiYXRjaFJlc3VsdCA9IHJlZignJylcbmFzeW5jIGZ1bmN0aW9uIGFzc2lnbkJhdGNoKCkge1xuICBzYXZpbmcudmFsdWUgPSB0cnVlXG4gIHRyeSB7XG4gICAgYmF0Y2hSZXN1bHQudmFsdWUgPSBKU09OLnN0cmluZ2lmeShcbiAgICAgIGF3YWl0IHJldmlldy5iYXRjaEFzc2lnbih7XG4gICAgICAgIHByb2plY3RJZDogcHJvamVjdElkLnZhbHVlLFxuICAgICAgICBwcm9qZWN0U2NvcGVWZXJzaW9uOiBwcm9qZWN0LnZhbHVlPy5wcm9qZWN0U2NvcGVWZXJzaW9uLFxuICAgICAgICByZXZpZXdlcklkOiBiYXRjaFJldmlld2VyLnZhbHVlLFxuICAgICAgICBpdGVtczogc2VsZWN0ZWQudmFsdWUubWFwKChyb3cpID0+IGNvbnRleHQocm93KSlcbiAgICAgIH0pLFxuICAgICAgbnVsbCxcbiAgICAgIDJcbiAgICApXG4gICAgYXdhaXQgbG9hZCgpXG4gIH0gY2F0Y2gge1xuICAgIGJhdGNoUmVzdWx0LnZhbHVlID0gJ+aJuemHj+ivt+axguacquWujOaIkO+8m+acquaOqOWumuS7u+S9leWIhua0vuaIkOWKn+OAguivt+aguOWvueadg+mZkOWSjOivt+axguOAgidcbiAgfSBmaW5hbGx5IHtcbiAgICBzYXZpbmcudmFsdWUgPSBmYWxzZVxuICB9XG59XG5hc3luYyBmdW5jdGlvbiBjb25maXJtKHJvdzogYW55KSB7XG4gIGNvbnN0IG1pc3NpbmcgPSBvcGluaW9uTWlzc2luZyhyb3csIHBhcnNlKHJvdy5idXNpbmVzc0NvbnRleHRKc29uKSB8fCB7fSwgdHJ1ZSlcbiAgaWYgKE9iamVjdC5rZXlzKG1pc3NpbmcpLmxlbmd0aCkge1xuICAgIG1lc3NhZ2Uud2FybmluZyhg56Gu6K6k5YmN6K+36KGl6b2Q5Lul5LiL5a2X5q6177yaJHtPYmplY3QudmFsdWVzKG1pc3NpbmcpLmpvaW4oJ+OAgScpfe+8m+WwmuacquS6p+eUn+etvuWtl2ApXG4gICAgcmV0dXJuXG4gIH1cbiAgdHJ5IHtcbiAgICBhd2FpdCBtZXNzYWdlLmNvbmZpcm0oYOacrOS6uuehruiupOmcgOaxgiR7cm93LmlkfeeahOW9k+WJjeeJiOacrCR7cm93LnZlcnNpb25977yf5LiN5Lya5L+d5a2Y5pyq5o+Q5Lqk6KGo5Y2V44CCYClcbiAgICBhd2FpdCBhcGkuY29uZmlybVJlcXVpcmVtZW50KFxuICAgICAgcm93LmlkLFxuICAgICAgcm93LnZlcnNpb24sXG4gICAgICBwcm9qZWN0SWQudmFsdWUsXG4gICAgICBwcm9qZWN0LnZhbHVlPy5wcm9qZWN0U2NvcGVWZXJzaW9uXG4gICAgKVxuICAgIGF3YWl0IGxvYWQoKVxuICB9IGNhdGNoIChlcnJvcikge1xuICAgIGlmIChlcnJvciAhPT0gJ2NhbmNlbCcgJiYgZXJyb3IgIT09ICdjbG9zZScpIG1lc3NhZ2Uud2FybmluZyhmYWlsdXJlR3VpZGFuY2UoZXJyb3IpKVxuICB9XG59XG53YXRjaChcbiAgKCkgPT4gcm91dGUuZnVsbFBhdGgsXG4gICgpID0+IHtcbiAgICBmb3JtVmlzaWJsZS52YWx1ZSA9IGZhbHNlXG4gICAgYWN0aW9uVmlzaWJsZS52YWx1ZSA9IGZhbHNlXG4gICAgaGlzdG9yeVZpc2libGUudmFsdWUgPSBmYWxzZVxuICAgIGRyaWxsVmlzaWJsZS52YWx1ZSA9IGZhbHNlXG4gICAgYmF0Y2hWaXNpYmxlLnZhbHVlID0gZmFsc2VcbiAgICBzb3VyY2VJZC52YWx1ZSA9IHVuZGVmaW5lZFxuICAgIHBhZ2VOby52YWx1ZSA9IDFcbiAgICBsb2FkKClcbiAgfSxcbiAgeyBpbW1lZGlhdGU6IHRydWUgfVxuKVxuPC9zY3JpcHQ+XG48c3R5bGUgc2NvcGVkPlxuLnJldmlldy1wYWdlIHtcbiAgbWluLXdpZHRoOiAwO1xufVxuLnRvb2xiYXIsXG4udmlldy1uYXYsXG46Z2xvYmFsKC5ocm0tcmV2aWV3LWRpYWxvZyAuZWwtZGlhbG9nX19ib2R5KSB7XG4gIG1heC1oZWlnaHQ6IDU1dmg7XG4gIG92ZXJmbG93LXk6IGF1dG87XG59XG4uZGlhbG9nLWFjdGlvbnMge1xuICBkaXNwbGF5OiBmbGV4O1xuICBnYXA6IDhweDtcbiAgZmxleC13cmFwOiB3cmFwO1xuICBhbGlnbi1pdGVtczogY2VudGVyO1xuICBtYXJnaW46IDE0cHggMDtcbn1cbi50b29sYmFyIC5lbC1zZWxlY3QsXG4udG9vbGJhciAuZWwtaW5wdXQge1xuICB3aWR0aDogMjEwcHg7XG4gIG1heC13aWR0aDogMTAwJTtcbn1cbi52aWV3LW5hdiAuZWwtYnV0dG9uIHtcbiAgbWFyZ2luLWxlZnQ6IDA7XG59XG4ud2lkZS10YWJsZSB7XG4gIG1heC13aWR0aDogMTAwJTtcbiAgb3ZlcmZsb3cteDogYXV0bztcbiAgbWFyZ2luOiAxMnB4IDA7XG59XG50YWJsZSB7XG4gIGJvcmRlci1jb2xsYXBzZTogY29sbGFwc2U7XG4gIG1pbi13aWR0aDogNjUwcHg7XG4gIHdpZHRoOiAxMDAlO1xufVxudGgsXG50ZCB7XG4gIGJvcmRlcjogMXB4IHNvbGlkIHZhcigtLWVsLWJvcmRlci1jb2xvcik7XG4gIHBhZGRpbmc6IDEwcHg7XG4gIHRleHQtYWxpZ246IGxlZnQ7XG4gIHdoaXRlLXNwYWNlOiBub3dyYXA7XG59XG4uZGVtby1iYXItdHJhY2sge1xuICBiYWNrZ3JvdW5kOiB2YXIoLS1lbC1maWxsLWNvbG9yKTtcbiAgaGVpZ2h0OiAxMnB4O1xuICBib3JkZXItcmFkaXVzOiA2cHg7XG4gIG1hcmdpbi10b3A6IDhweDtcbn1cbi5kZW1vLWJhciB7XG4gIGJhY2tncm91bmQ6IHZhcigtLWVsLWNvbG9yLXByaW1hcnkpO1xuICBoZWlnaHQ6IDEycHg7XG4gIGJvcmRlci1yYWRpdXM6IDZweDtcbn1cbi5zb3VyY2UtY2FyZCxcbi5zdGF0IHtcbiAgYm9yZGVyOiAxcHggc29saWQgdmFyKC0tZWwtYm9yZGVyLWNvbG9yKTtcbiAgYm9yZGVyLXJhZGl1czogNnB4O1xuICBwYWRkaW5nOiAxMnB4O1xuICBtYXJnaW46IDhweCAwO1xufVxuLnN0YXRzLWdyaWQsXG4uZm9ybS1ncmlkIHtcbiAgZGlzcGxheTogZ3JpZDtcbiAgZ3JpZC10ZW1wbGF0ZS1jb2x1bW5zOiByZXBlYXQoMiwgbWlubWF4KDAsIDFmcikpO1xuICBnYXA6IDEycHg7XG59XG4uc3RhdCB7XG4gIGJhY2tncm91bmQ6IHZhcigtLWVsLWZpbGwtY29sb3ItbGlnaHQpO1xuICBjb2xvcjogdmFyKC0tZWwtdGV4dC1jb2xvci1wcmltYXJ5KTtcbiAgdGV4dC1hbGlnbjogbGVmdDtcbn1cbi5hbmNob3I6Zm9jdXMge1xuICBvdXRsaW5lOiAycHggc29saWQgdmFyKC0tZWwtY29sb3ItcHJpbWFyeSk7XG4gIG91dGxpbmUtb2Zmc2V0OiA0cHg7XG59XG5wcmUge1xuICB3aGl0ZS1zcGFjZTogcHJlLXdyYXA7XG4gIG92ZXJmbG93LXdyYXA6IGFueXdoZXJlO1xuICBtYXgtd2lkdGg6IDEwMCU7XG59XG5zZWN0aW9uIHtcbiAgbWFyZ2luLXRvcDogMjhweDtcbn1cbnAge1xuICBvdmVyZmxvdy13cmFwOiBhbnl3aGVyZTtcbn1cbi5kaWFsb2ctYWN0aW9ucyB7XG4gIGp1c3RpZnktY29udGVudDogZmxleC1lbmQ7XG4gIG1hcmdpbjogMDtcbn1cbkBtZWRpYSAobWF4LXdpZHRoOiA2MDBweCkge1xuICAuc3RhdHMtZ3JpZCxcbiAgLmZvcm0tZ3JpZCB7XG4gICAgZ3JpZC10ZW1wbGF0ZS1jb2x1bW5zOiAxZnI7XG4gIH1cbiAgLnRvb2xiYXIgLmVsLXNlbGVjdCxcbiAgLnRvb2xiYXIgLmVsLWlucHV0IHtcbiAgICB3aWR0aDogMTAwJTtcbiAgfVxuICAuZGlhbG9nLWFjdGlvbnMgLmVsLWJ1dHRvbiB7XG4gICAgbWFyZ2luLWxlZnQ6IDA7XG4gICAgd2hpdGUtc3BhY2U6IG5vcm1hbDtcbiAgfVxuICAucmV2aWV3LXBhZ2UgOmRlZXAoLmVsLXBhZ2luYXRpb24pIHtcbiAgICBmbGV4LXdyYXA6IHdyYXA7XG4gIH1cbiAgLnJldmlldy1wYWdlIDpkZWVwKC5lbC1idXR0b24pIHtcbiAgICBtYXgtd2lkdGg6IDEwMCU7XG4gICAgaGVpZ2h0OiBhdXRvO1xuICAgIG1pbi1oZWlnaHQ6IDMycHg7XG4gICAgd2hpdGUtc3BhY2U6IG5vcm1hbDtcbiAgfVxufVxuPC9zdHlsZT5cbiJdLCJmaWxlIjoiL1VzZXJzL3dhaXRmb3Jwcm8vc291cmNlL2dpdGh1Yi15dWRhby9ydW95aS12dWUtcHJvL3l1ZGFvLXVpL3l1ZGFvLXVpLWFkbWluLXZ1ZTMvc3JjL3ZpZXdzL2hybS9wYXlyb2xsL3Jldmlldy9pbmRleC52dWUifQ==