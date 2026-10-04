package cn.iocoder.yudao.module.hrm.service.payroll.preparation;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.preparation.HrmPayrollPreparationRespVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.preparation.HrmPayrollPreparationRespVO.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.intake.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.rule.HrmPayrollRuleDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.intake.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.rule.HrmPayrollRuleMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Service
public class HrmPayrollPreparationServiceImpl implements HrmPayrollPreparationService {
    @Resource private PermissionApi permissionApi;
    @Resource private HrmPayrollRequirementMapper requirementMapper;
    @Resource private HrmPayrollSourceMapper sourceMapper;
    @Resource private HrmPayrollContractMapper contractMapper;
    @Resource private HrmPayrollRuleMapper ruleMapper;
    @Resource private HrmPayrollImportBatchMapper batchMapper;
    private static final String[] MODULE_CODES={"overview","plan","calc","tax","attendance","overtime","hours","insurance","report","req"};
    private static final String[] MODULE_NAMES={"薪酬总览","薪酬方案","工资核算","个税工资条","考勤数据","加班管理","工时管理","社保公积金","报表分析","需求征集"};

    @Override
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public HrmPayrollPreparationRespVO summary() {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        Long user=Objects.requireNonNull(getLoginUserId(),"Authenticated actor required");
        HrmPayrollPreparationRespVO response=new HrmPayrollPreparationRespVO().setGeneratedAt(Instant.now().toString());
        boolean requirements=permissionApi.hasAnyPermissions(user,"hrm:payroll:requirements:query");
        boolean intake=permissionApi.hasAnyPermissions(user,"hrm:payroll:intake:query");
        boolean rules=permissionApi.hasAnyPermissions(user,"hrm:payroll:rule:query");
        response.setRequirements(new Section().setAuthorized(requirements));
        response.setSources(new Section().setAuthorized(requirements));
        response.setContracts(new Section().setAuthorized(intake));
        response.setBatches(new Section().setAuthorized(intake));
        response.setRules(new Section().setAuthorized(rules));
        if(requirements) {
            // Select only bounded requirement metadata, never descriptions, mappings or review evidence.
            List<HrmPayrollRequirementDO> rows=requirementMapper.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementDO>()
                    .eq(HrmPayrollRequirementDO::getTenantId,tenant)
                    .select(HrmPayrollRequirementDO::getModuleCode,HrmPayrollRequirementDO::getStatus,HrmPayrollRequirementDO::getScopeDecision));
            response.getRequirements().setCounts(requirementCounts(rows));
            LinkedHashMap<String,String> modules=new LinkedHashMap<>();
            for(int i=0;i<MODULE_CODES.length;i++) modules.put(MODULE_CODES[i],MODULE_NAMES[i]);
            rows.forEach(row->modules.putIfAbsent(row.getModuleCode(),row.getModuleCode()));
            response.getRequirements().setModules(modules.entrySet().stream().map(entry->{
                List<HrmPayrollRequirementDO> group=rows.stream().filter(row->Objects.equals(entry.getKey(),row.getModuleCode())).collect(Collectors.toList());
                Map<String,Long> counts=requirementCounts(group);
                return new ModuleCount().setCode(entry.getKey()).setName(entry.getValue()).setTotal(counts.get("total"))
                        .setPending(counts.get("pending")).setConfirmed(counts.get("confirmed")).setDisputed(counts.get("disputed"))
                        .setInScope(counts.get("inScope")).setInScopeUnconfirmed(counts.get("inScopeUnconfirmed")).setUndecided(counts.get("undecided"));
            }).collect(Collectors.toList()));
            List<HrmPayrollSourceDO> sources=sourceMapper.selectList(new LambdaQueryWrapperX<HrmPayrollSourceDO>()
                    .eq(HrmPayrollSourceDO::getTenantId,tenant).select(HrmPayrollSourceDO::getCode,HrmPayrollSourceDO::getName,HrmPayrollSourceDO::getReadiness)
                    .orderByAsc(HrmPayrollSourceDO::getCode));
            Map<String,Long> counts=new LinkedHashMap<>();counts.put("total",(long)sources.size());
            counts.put("pending",sources.stream().filter(s->Objects.equals(s.getReadiness(),0)).count());
            counts.put("ready",sources.stream().filter(s->Objects.equals(s.getReadiness(),1)).count());
            counts.put("gap",sources.stream().filter(s->Objects.equals(s.getReadiness(),2)).count());
            response.getSources().setCounts(counts).setSources(BeanUtils.toBean(sources,SourceState.class));
        }
        if(intake) {
            Map<String,Long> contracts=new LinkedHashMap<>();
            contracts.put("total",contractMapper.selectCount(contractQuery(tenant)));
            contracts.put("draft",contractMapper.selectCount(contractQuery(tenant).eq(HrmPayrollContractDO::getStatus,0)));
            contracts.put("confirmed",contractMapper.selectCount(contractQuery(tenant).eq(HrmPayrollContractDO::getStatus,1)));
            contracts.put("retired",contractMapper.selectCount(contractQuery(tenant).eq(HrmPayrollContractDO::getStatus,2)));
            response.getContracts().setCounts(contracts);
            Map<String,Long> batches=new LinkedHashMap<>();
            batches.put("total",batchMapper.selectCount(batchQuery(tenant,user)));
            batches.put("passed",batchMapper.selectCount(batchQuery(tenant,user).eq(HrmPayrollImportBatchDO::getStatus,0)));
            batches.put("failed",batchMapper.selectCount(batchQuery(tenant,user).eq(HrmPayrollImportBatchDO::getStatus,1)));
            response.getBatches().setCounts(batches).setRecentBatches(BeanUtils.toBean(batchMapper.selectList(batchQuery(tenant,user)
                    .select(HrmPayrollImportBatchDO::getId,HrmPayrollImportBatchDO::getSourceCode,HrmPayrollImportBatchDO::getContractVersion,
                            HrmPayrollImportBatchDO::getPeriodStart,HrmPayrollImportBatchDO::getPeriodEnd,HrmPayrollImportBatchDO::getRowCount,
                            HrmPayrollImportBatchDO::getValidCount,HrmPayrollImportBatchDO::getErrorCount,HrmPayrollImportBatchDO::getStatus)
                    .orderByDesc(HrmPayrollImportBatchDO::getId).last("LIMIT 5")),Batch.class));
        }
        if(rules) {
            Map<String,Long> counts=new LinkedHashMap<>();counts.put("total",ruleMapper.selectCount(ruleQuery(tenant)));
            counts.put("draft",ruleMapper.selectCount(ruleQuery(tenant).eq(HrmPayrollRuleDO::getStatus,0)));
            counts.put("confirmed",ruleMapper.selectCount(ruleQuery(tenant).eq(HrmPayrollRuleDO::getStatus,1)));
            counts.put("retired",ruleMapper.selectCount(ruleQuery(tenant).eq(HrmPayrollRuleDO::getStatus,2)));
            response.getRules().setCounts(counts);
        }
        return response;
    }
    private Map<String,Long> requirementCounts(List<HrmPayrollRequirementDO> rows) {
        Map<String,Long> counts=new LinkedHashMap<>();counts.put("total",(long)rows.size());
        counts.put("pending",count(rows,r->Objects.equals(r.getStatus(),0)));
        counts.put("confirmed",count(rows,r->Objects.equals(r.getStatus(),1)));
        counts.put("disputed",count(rows,r->Objects.equals(r.getStatus(),2)));
        counts.put("inScope",count(rows,r->Objects.equals(r.getScopeDecision(),1)));
        counts.put("inScopeUnconfirmed",count(rows,r->Objects.equals(r.getScopeDecision(),1)&&!Objects.equals(r.getStatus(),1)));
        counts.put("undecided",count(rows,r->Objects.equals(r.getScopeDecision(),0)));
        counts.put("deferred",count(rows,r->Objects.equals(r.getScopeDecision(),2)));
        return counts;
    }
    private long count(List<HrmPayrollRequirementDO> rows,Predicate<HrmPayrollRequirementDO> predicate) { return rows.stream().filter(predicate).count(); }
    private LambdaQueryWrapperX<HrmPayrollContractDO> contractQuery(Long tenant) { return new LambdaQueryWrapperX<HrmPayrollContractDO>().eq(HrmPayrollContractDO::getTenantId,tenant); }
    private LambdaQueryWrapperX<HrmPayrollRuleDO> ruleQuery(Long tenant) { return new LambdaQueryWrapperX<HrmPayrollRuleDO>().eq(HrmPayrollRuleDO::getTenantId,tenant); }
    private LambdaQueryWrapperX<HrmPayrollImportBatchDO> batchQuery(Long tenant,Long user) {
        return new LambdaQueryWrapperX<HrmPayrollImportBatchDO>().eq(HrmPayrollImportBatchDO::getTenantId,tenant).eq(HrmPayrollImportBatchDO::getCreatedBy,user);
    }
}
