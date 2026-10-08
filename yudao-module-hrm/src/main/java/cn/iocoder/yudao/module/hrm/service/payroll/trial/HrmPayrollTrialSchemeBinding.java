package cn.iocoder.yudao.module.hrm.service.payroll.trial;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_TRIAL_INVALID;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.trial.HrmPayrollTrialConfigVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.scheme.HrmPayrollSchemeDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.scheme.HrmPayrollSchemeMapper;
import cn.iocoder.yudao.module.hrm.service.payroll.calculation.HrmPayrollCalculationEngine;
import cn.iocoder.yudao.module.hrm.service.payroll.scheme.HrmPayrollSchemeService;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * Explicit associations of versioned policy items with typed manual inputs; never a wage import.
 */
@Component
public class HrmPayrollTrialSchemeBinding {
    @Resource private HrmPayrollSchemeService schemes;
    @Resource private HrmPayrollSchemeMapper mapper;
    @Resource private HrmPayrollCalculationEngine engine;

    private void valid(boolean condition, String message) {
        if (!condition) throw exception(PAYROLL_TRIAL_INVALID, message);
    }

    private boolean covers(LocalDate from, LocalDate to, LocalDate start, LocalDate end) {
        return from != null && !from.isAfter(start) && (to == null || !to.isBefore(end));
    }

    public HrmPayrollSchemeRespVO validate(
            Long id,
            HrmPayrollCalculationRespVO definition,
            String entity,
            LocalDate start,
            LocalDate end,
            List<HrmPayrollTrialConfigVO.SourceBinding> bindings,
            HrmPayrollTrialConfigVO.Roles roles) {
        HrmPayrollSchemeRespVO scheme = schemes.get(id);
        valid(
                Objects.equals(scheme.getStatus(), 1)
                        && covers(scheme.getEffectiveFrom(), scheme.getEffectiveTo(), start, end),
                "方案须已确认且单个版本完整覆盖批次期间");
        valid(
                scheme.getSnapshot() != null && scheme.getSnapshot().getIssues().isEmpty(),
                "方案配置快照不完整");
        valid(
                Objects.equals(definition.getStatus(), 1)
                        && covers(
                                definition.getEffectiveFrom(),
                                definition.getEffectiveTo(),
                                start,
                                end),
                "绑定规则须已确认且单个版本完整覆盖批次期间");
        valid(Objects.equals(entity, definition.getScopeCode()), "绑定规则的声明范围编号须与批次主体编号一致");
        valid(
                Boolean.TRUE.equals(
                        engine.compile(definition.getProgram()).verifyCases().getAllPassed()),
                "绑定规则样例须全部通过");
        Map<String, HrmPayrollCalculationSpecVO.Input> inputs =
                definition.getProgram().getInputs().stream()
                        .collect(
                                Collectors.toMap(
                                        HrmPayrollCalculationSpecVO.Input::getKey, v -> v));
        valid(bindings != null && bindings.size() == inputs.size(), "每个规则输入须明确登记一项来源绑定");
        Set<String> seen = new HashSet<>();
        Set<Long> options = new HashSet<>();
        Map<Long, HrmPayrollSchemeSnapshotVO.Option> catalogue =
                scheme.getSnapshot().getOptions().stream()
                        .collect(
                                Collectors.toMap(HrmPayrollSchemeSnapshotVO.Option::getId, v -> v));
        for (HrmPayrollTrialConfigVO.SourceBinding binding : bindings) {
            valid(
                    binding != null
                            && inputs.containsKey(binding.getInputKey())
                            && seen.add(binding.getInputKey()),
                    "输入绑定不能缺失、重复或含未知字段");
            HrmPayrollCalculationSpecVO.Input input = inputs.get(binding.getInputKey());
            valid(
                    Objects.equals(input.getUnit(), binding.getUnit())
                            && StrUtil.isNotBlank(binding.getReference())
                            && binding.getReference().length() <= 2000,
                    "绑定单位须与规则一致并填写来源口径依据");
            if ("SCHEME_ITEM".equals(binding.getSourceType())) {
                HrmPayrollSchemeSnapshotVO.Option option = catalogue.get(binding.getOptionId());
                valid(
                        option != null && Boolean.TRUE.equals(option.getEnabled()),
                        "所选工资项须存在于选定方案快照且已启用");
                valid(options.add(option.getId()), "同一工资项不能重复绑定多个输入");
                valid(
                        "CNY".equals(input.getUnit())
                                && "DECIMAL".equals(input.getType())
                                && Objects.equals(input.getScale(), 2),
                        "工资项绑定仅接受 CNY/DECIMAL/2 位金额输入");
            } else {
                valid(
                        "MANUAL".equals(binding.getSourceType()) && binding.getOptionId() == null,
                        "独立录入来源不得携带工资项 ID");
            }
        }
        valid(!options.isEmpty(), "方案绑定须至少明确关联一个工资项");
        Map<String, HrmPayrollCalculationSpecVO.Item> outputs =
                definition.getProgram().getItems().stream()
                        .collect(
                                Collectors.toMap(HrmPayrollCalculationSpecVO.Item::getKey, v -> v));
        List<String> keys =
                roles == null
                        ? Collections.emptyList()
                        : Arrays.asList(
                                roles.getGross(),
                                roles.getDeductions(),
                                roles.getTax(),
                                roles.getNet());
        valid(
                keys.size() == 4
                        && new HashSet<>(keys).size() == 4
                        && keys.stream()
                                .allMatch(
                                        k ->
                                                outputs.containsKey(k)
                                                        && "CNY".equals(outputs.get(k).getUnit())
                                                        && Objects.equals(
                                                                outputs.get(k).getAmountScale(),
                                                                2)),
                "应发、扣款、个税和实发须绑定不同的 CNY/2 位结果");
        return scheme;
    }

    /** Same order as scheme review: source group, chosen version; then trial locks calculation. */
    public void lock(Long id) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        HrmPayrollSchemeRespVO scheme = schemes.get(id);
        // Include a soft-deleted group: its surviving history and retirement use this same mutex.
        mapper.lockGroup(scheme.getGroupId(), tenant);
        HrmPayrollSchemeDO row =
                mapper.selectOne(
                        new LambdaQueryWrapperX<HrmPayrollSchemeDO>()
                                .eq(HrmPayrollSchemeDO::getTenantId, tenant)
                                .eq(HrmPayrollSchemeDO::getId, id)
                                .last("FOR UPDATE"));
        valid(row != null, "方案不存在或不可访问");
    }
}
