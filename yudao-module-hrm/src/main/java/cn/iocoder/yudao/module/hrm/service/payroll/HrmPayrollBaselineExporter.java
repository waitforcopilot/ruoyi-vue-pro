package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollBaselineDO;
import com.fasterxml.jackson.databind.JsonNode;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/** Exports the persisted snapshot, so downloading an old baseline is repeatable. */
public final class HrmPayrollBaselineExporter {
    private HrmPayrollBaselineExporter() { }

    public static void write(HrmPayrollBaselineDO baseline, HttpServletResponse response) throws IOException {
        JsonNode snapshot = JsonUtils.getObjectMapper().readTree(baseline.getSnapshot());
        List<List<Object>> requirements = new ArrayList<>();
        for (JsonNode node : snapshot.get("requirements")) {
            HrmPayrollRequirementRespVO r = JsonUtils.parseObject(node.toString(), HrmPayrollRequirementRespVO.class);
            requirements.add(Arrays.asList(r.getCode(), r.getModuleCode(), r.getTitle(), r.getDescription(),
                    r.getOrigin(), label(r.getStatus(), "待确认", "已确认", "异议"), r.getOwnerName(),
                    r.getPriority() == null ? "未设定" : "P" + r.getPriority(),
                    label(r.getScopeDecision(), "未决定", "首期纳入", "暂缓"), r.getScopeReason(), r.getApplicableScope(),
                    String.join(", ", r.getSourceCodes() == null ? Collections.emptyList() : r.getSourceCodes()),
                    r.getFieldMapping(), r.getAcceptanceCriteria(), r.getRemark(), r.getVersion(),
                    r.getReviewedByName(), r.getReviewedTime() == null ? "" : r.getReviewedTime().toString(), r.getEvidence()));
        }
        List<List<Object>> sources = new ArrayList<>();
        for (JsonNode node : snapshot.get("sources")) {
            HrmPayrollSourceRespVO s = JsonUtils.parseObject(node.toString(), HrmPayrollSourceRespVO.class);
            List<String> linked = new ArrayList<>();
            for (JsonNode r : snapshot.get("requirements")) {
                if (r.has("sourceCodes")) for (JsonNode code : r.get("sourceCodes")) {
                    if (s.getCode().equals(code.asText())) linked.add(r.get("code").asText());
                }
            }
            sources.add(Arrays.asList(s.getCode(), s.getName(), s.getDescription(), s.getRequiredFields(),
                    s.getActualSystem(), s.getFieldMapping(), s.getOwnerName(),
                    label(s.getReadiness(), "待核实", "已就绪", "待补齐"), s.getEvidence(), s.getVersion(),
                    s.getConfirmedByName(), s.getConfirmedTime() == null ? "" : s.getConfirmedTime().toString(), String.join("\n", linked)));
        }
        List<List<Object>> metadata = Arrays.asList(
                Arrays.asList("基线编号", "B" + baseline.getId()),
                Arrays.asList("生成时间", String.valueOf(baseline.getCreateTime())),
                Arrays.asList("生成人", baseline.getExportedByName()),
                Arrays.asList("需求数", baseline.getRequirementCount()),
                Arrays.asList("来源数", baseline.getSourceCount()),
                Arrays.asList("状态说明", "功能确认与数据就绪分别统计；原型候选全部保留，未决项不视为已验收"),
                Arrays.asList("版本说明", "导出来自该基线的冻结快照；后续修改不会改写此文件的数据"));
        // A workbook may exceed the servlet buffer; set download headers before writing it.
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8("薪酬评审基线-B" + baseline.getId() + ".xlsx"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream()).autoCloseStream(false).build()) {
            writer.write(requirements, FastExcelFactory.writerSheet(0, "需求评审").head(head(
                    "编号", "模块", "功能需求", "补充说明", "原始来源", "评审状态", "负责人", "优先级", "范围决定", "范围原因", "适用主体与人群",
                    "数据来源编号", "字段映射", "验收条件", "备注及未决问题", "记录版本", "评审人", "评审时间", "评审依据")).build());
            writer.write(sources, FastExcelFactory.writerSheet(1, "数据来源").head(head(
                    "编号", "来源名称", "来源说明", "必需字段", "实际系统", "字段映射与单位", "负责人", "就绪状态", "核验依据及缺口", "记录版本",
                    "核验人", "核验时间", "关联需求编号")).build());
            writer.write(metadata, FastExcelFactory.writerSheet(2, "基线说明").head(head("项目", "内容")).build());
        }
    }

    private static List<List<String>> head(String... names) {
        return Arrays.stream(names).map(Collections::singletonList).collect(Collectors.toList());
    }

    private static String label(Integer value, String... labels) {
        return value == null || value < 0 || value >= labels.length ? "未设定" : labels[value];
    }
}
