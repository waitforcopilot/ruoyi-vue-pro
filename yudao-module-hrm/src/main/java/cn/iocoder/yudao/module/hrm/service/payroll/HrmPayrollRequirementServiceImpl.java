package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollRequirementSaveReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Service
public class HrmPayrollRequirementServiceImpl implements HrmPayrollRequirementService {
    @Resource private HrmPayrollGovernanceStore governanceStore;
    @Resource @org.springframework.context.annotation.Lazy private HrmPayrollGovernanceDecisionService governanceDecisions;
    @Resource private cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    @Resource private HrmPayrollRequirementMapper mapper;
    @Resource private cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
    @Resource private HrmPayrollRequirementHistoryMapper historyMapper;
    @Resource private HrmPayrollRequirementProjectMapper projectMapper;
    @Resource private HrmPayrollRequirementProjectService projectService;
    @Resource private HrmPayrollWorkbenchService workbench;
    private static final String[][] SEEDS = {
        {"OV-01", "总览层汇总指标（应发/实发/法定成本/个税）的统计口径定义"},
        {"OV-02", "核算期间切换与历史批次查询"},
        {"OV-03", "各数据要素(考勤/加班/工时/社保)归集完成度看板"},
        {"OV-04", "异常数据触发核对的规则与提醒"},
        {"PLAN-01", "薪酬结构与各项目的规则可配置化"},
        {"PLAN-02", "薪级薪档表的维护与批量导入"},
        {"PLAN-03", "定薪/调薪流程（含审批链与生效追溯）"},
        {"PLAN-04", "薪酬方案版本管理与历史对比"},
        {"CALC-01", "核算批次生命周期（草稿→试算→审批→发放→归档）"},
        {"CALC-02", "算薪公式引擎（各薪酬项目自动计算与调整）"},
        {"CALC-03", "异常数据拦截与人工核对"},
        {"CALC-04", "银行代发文件生成与回盘核对"},
        {"CALC-05", "工资条生成与员工自助查看"},
        {"TAX-01", "个税累计预扣法自动计算"},
        {"TAX-02", "专项附加扣除采集与同步"},
        {"TAX-03", "工资条模板配置与多渠道推送(自助端/邮件/短信)"},
        {"TAX-04", "个税申报表与完税数据导出"},
        {"TAX-05", "年终奖单独计税处理"},
        {"ATT-01", "考勤数据按日/按月归集与汇总"},
        {"ATT-02", "请假/出差/外出单据与打卡数据自动关联"},
        {"ATT-03", "迟到/早退/缺勤/旷工规则判定"},
        {"ATT-04", "异常考勤提醒与人工处理闭环"},
        {"ATT-05", "考勤结果与薪酬计算的联动"},
        {"OT-01", "加班申请与审批流（关联考勤打卡时长）"},
        {"OT-02", "加班时长统计与倍率规则计算"},
        {"OT-03", "加班费自动生成并进入工资核算"},
        {"OT-04", "调休/补休管理"},
        {"OT-05", "加班合规上限提醒"},
        {"HOUR-01", "不同工时制度的员工适用维护"},
        {"HOUR-02", "综合/不定时工时周期结算"},
        {"HOUR-03", "工时报表生成与异常提醒"},
        {"HOUR-04", "工时数据与加班费/考勤扣款的联动"},
        {"HOUR-05", "工时与排班表关联核对"},
        {"INS-01", "缴存基数申报与年度调基"},
        {"INS-02", "比例与基数按城市/政策配置"},
        {"INS-03", "个人扣款自动进入工资核算"},
        {"INS-04", "单位缴费计入用工成本报表"},
        {"INS-05", "社保公积金月度缴纳台账与对账"},
        {"RPT-01", "薪酬成本按部门/成本中心/项目分摊"},
        {"RPT-02", "人力成本率与人均指标计算"},
        {"RPT-03", "薪酬结构多维分析(占比/变动)"},
        {"RPT-04", "工资总额预算与实际对比"},
        {"RPT-05", "报表口径与导出格式"},
    };
    @Override public HrmPayrollRequirementProjectDO legacyProject() {
        return projectMapper.selectOne(HrmPayrollRequirementProjectDO::getProjectKey,"LEGACY");
    }
    private Long ensureLegacyProject() {
        HrmPayrollRequirementProjectDO project=legacyProject();
        if(project!=null) return project.getId();
        project=new HrmPayrollRequirementProjectDO().setProjectKey("LEGACY").setName("历史需求迁移项目").setMetadataVersion(1);
        try { projectMapper.insert(project); }
        catch(org.springframework.dao.DuplicateKeyException duplicate) {
            // Concurrent first writes share the tenant's single legacy project.
            HrmPayrollRequirementProjectDO existing=projectMapper.selectOneForUpdate(HrmPayrollRequirementProjectDO::getProjectKey,"LEGACY");
            if(existing==null) throw duplicate;
            return existing.getId();
        }
        return project.getId();
    }
    @Override public List<HrmPayrollRequirementDO> list(String module) {
        HrmPayrollRequirementProjectDO p=legacyProject();
        if(p==null) return java.util.Collections.emptyList();
        return projectService.rows(p.getId(),module,cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId(),"READ");
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Long save(HrmPayrollRequirementSaveReqVO request, Long actorId) {
        return saveInternal(request,actorId,false,"EDIT");
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void confirm(Long id,Integer version,Long actorId) {
        if(id==null||version==null||version<1||actorId==null) throw exception(PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);
        governanceStore.lock();guardTarget(id,actorId,"CONFIRM");
        HrmPayrollRequirementDO preliminary=mapper.selectById(id);if(preliminary==null)throw exception(PAYROLL_REQUIREMENT_NOT_EXISTS);
        // Scope feedback is only available after guardTarget has authorized this requirement.
        // Reject before governance coverage can obscure an undefined or invalidated scope.
        HrmPayrollRequirementProjectDO currentProject=projectMapper.selectById(preliminary.getProjectId());
        if(currentProject==null)throw exception(PAYROLL_PROJECT_DENIED);
        if(currentProject.getProjectScopeVersion()==null || StrUtil.isBlank(currentProject.getScopeJson())
            || !"MATCH".equals(preliminary.getScopeState())
            || !Objects.equals(currentProject.getProjectScopeVersion(),preliminary.getProjectScopeVersion()))
            throw exception(PAYROLL_PROJECT_SCOPE_STALE);
        governanceDecisions.refreshSemantics(preliminary.getProjectId(),actorId);governanceDecisions.forRequirement(preliminary,actorId);
        HrmPayrollRequirementDO row=mapper.selectOneForUpdate(HrmPayrollRequirementDO::getId,id);
        if(row==null) throw exception(PAYROLL_REQUIREMENT_NOT_EXISTS);
        if(!Objects.equals(row.getVersion(),version)) throw exception(PAYROLL_VERSION_CONFLICT);
        if(!java.util.Arrays.asList("PENDING","REVIEWING","DISPUTED").contains(row.getStatus()))
            throw exception(PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);
        HrmPayrollRequirementSaveReqVO request=BeanUtils.toBean(row,HrmPayrollRequirementSaveReqVO.class);
        if("LEGACY".equals(projectMapper.selectById(row.getProjectId()).getProjectKey()))request.setProjectId(null);
        request.setStatus("CONFIRMED");
        saveInternal(request,actorId,true,"CONFIRM");
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void assign(Long id,Integer version,Long reviewerId,Long actorId) {
        governanceStore.lock();
        if(actorId==null || !permissions.hasAnyPermissions(actorId,"hrm:payroll:requirement:assign"))
            throw exception(PAYROLL_REQUIREMENT_ASSIGN_FORBIDDEN);
        if(id==null || version==null || version<1) throw exception(PAYROLL_VERSION_CONFLICT);
        guardTarget(id,actorId,"ASSIGN");
        HrmPayrollRequirementDO row=mapper.selectOneForUpdate(HrmPayrollRequirementDO::getId,id);
        if(row==null) throw exception(PAYROLL_REQUIREMENT_NOT_EXISTS);
        if(!Objects.equals(row.getVersion(),version)) throw exception(PAYROLL_VERSION_CONFLICT);
        if(Objects.equals(row.getReviewerId(),reviewerId)) return;
        HrmPayrollRequirementSaveReqVO request=BeanUtils.toBean(row,HrmPayrollRequirementSaveReqVO.class);
        if("LEGACY".equals(projectMapper.selectById(row.getProjectId()).getProjectKey()))request.setProjectId(null);
        request.setReviewerId(reviewerId);
        saveInternal(request,actorId,false,"ASSIGN");
    }
    private Long saveInternal(HrmPayrollRequirementSaveReqVO request,Long actorId,boolean confirming,String action) {
        governanceStore.lock();
        HrmPayrollRequirementProjectDO selected=null;
        if(request.getId()!=null) guardTarget(request.getId(),actorId,action);
        if(request.getProjectId()!=null) {
            selected=projectService.check(request.getProjectId(),actorId,action,true);
            projectService.version(selected,request.getProjectScopeVersion());
            // S5 validates independently registered roots and current decisions in confirmationTuple.
            // TEST project names never bypass that policy.
        }
        if(request.getProjectId()==null)projectService.check(ensureLegacyProject(),actorId,action,true);
        HrmPayrollRequirementDO previous = request.getId()==null ? null : mapper.selectOneForUpdate(HrmPayrollRequirementDO::getId, request.getId());
        if (request.getId()!=null && previous==null) throw exception(PAYROLL_REQUIREMENT_NOT_EXISTS);
        if (previous!=null && !Objects.equals(previous.getVersion(),request.getVersion())) throw exception(PAYROLL_VERSION_CONFLICT);
        if(previous!=null && request.getProjectId()!=null && !Objects.equals(previous.getProjectId(),request.getProjectId()))throw exception(PAYROLL_PROJECT_DENIED);
        if(previous!=null && !"LEGACY".equals(projectMapper.selectById(previous.getProjectId()).getProjectKey()) && request.getProjectId()==null)throw exception(PAYROLL_PROJECT_DENIED);
        if(selected!=null && previous!=null && !Objects.equals(previous.getCode(),request.getCode()))throw exception(PAYROLL_PROJECT_DENIED);
        if(selected!=null && previous==null) request.setCode(projectService.nextCode(selected));
        if(previous!=null && request.getSourceApplicability()!=null && !Objects.equals(previous.getSourceApplicability(),request.getSourceApplicability())) {
            request=BeanUtils.toBean(request,HrmPayrollRequirementSaveReqVO.class);request.setStatus("REVIEWING");
        }
        boolean invalidate=previous!=null && "CONFIRMED".equals(previous.getStatus()) && reviewContractChanged(previous,request);
        if(invalidate && "CONFIRMED".equals(request.getStatus())) {
            request=BeanUtils.toBean(request,HrmPayrollRequirementSaveReqVO.class);
            request.setStatus("REVIEWING");
        }
        if(!confirming && "CONFIRMED".equals(request.getStatus())
            && (previous==null || !"CONFIRMED".equals(previous.getStatus())))
            throw exception(PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);
        if (!Objects.equals(previous==null ? null : previous.getReviewerId(),request.getReviewerId())) {
            // The saved reviewer is an assignment, not an editable identity assertion.
            if(selected!=null && previous!=null)projectService.requireRow(previous,actorId,"ASSIGN");
            if(actorId==null || !permissions.hasAnyPermissions(actorId,"hrm:payroll:requirement:assign")
                || Objects.equals(actorId,request.getReviewerId()))
                throw exception(PAYROLL_REQUIREMENT_ASSIGN_FORBIDDEN);
            if(request.getReviewerId()!=null) {
                requireActiveUser(request.getReviewerId());
                if(selected!=null && previous!=null)projectService.requireRow(previous,actorId,"ASSIGN");
                if(selected!=null){HrmPayrollRequirementDO target=BeanUtils.toBean(request,HrmPayrollRequirementDO.class);target.setProjectId(selected.getId());projectService.bind(target,selected,request.getScopeMode(),request.getScopeJson());if(!projectService.reviewer(target,request.getReviewerId()))throw exception(PAYROLL_REQUIREMENT_ASSIGNEE_FORBIDDEN);}
                if(!permissions.hasAnyPermissions(request.getReviewerId(),"hrm:payroll:requirement:query")
                    || !permissions.hasAnyPermissions(request.getReviewerId(),"hrm:payroll:requirement:confirm"))
                    throw exception(PAYROLL_REQUIREMENT_ASSIGNEE_FORBIDDEN);
            }
        }
        if (java.util.Arrays.asList("CONFIRMED","DISPUTED","DEFERRED").contains(request.getStatus()))
            HrmPayrollReviewService.required("依据或异议说明",request.getEvidence(),"评审负责人",request.getReviewerId());
        if ((selected==null || "LEGACY".equals(selected.getProjectKey())) && "CONFIRMED".equals(request.getStatus()) && (StrUtil.isBlank(request.getAcceptance())
            || StrUtil.isBlank(request.getSourceSystem()) || request.getSourceOwnerId()==null)) throw exception(PAYROLL_REVIEW_INCOMPLETE);
        if (java.util.Arrays.asList("CONFIRMED", "DISPUTED", "DEFERRED").contains(request.getStatus())) {
            requireActiveUser(request.getReviewerId());
        }
        if ((selected==null || "LEGACY".equals(selected.getProjectKey())) && "CONFIRMED".equals(request.getStatus())) {
            requireActiveUser(request.getSourceOwnerId());
            if (!Objects.equals(actorId, request.getReviewerId()) || !meaningfulMapping(request.getFieldMapping()))
                throw exception(PAYROLL_REQUIREMENT_CONFIRMATION_INVALID);
        }
        Long targetProject=selected==null ? (previous==null?ensureLegacyProject():previous.getProjectId()) : selected.getId();
        HrmPayrollRequirementDO duplicate=mapper.selectOne(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,targetProject).eq(HrmPayrollRequirementDO::getCode,request.getCode()));
        if (duplicate!=null && !Objects.equals(duplicate.getId(),request.getId())) throw exception(PAYROLL_REQUIREMENT_DUPLICATE);
        HrmPayrollRequirementDO record=BeanUtils.toBean(request,HrmPayrollRequirementDO.class);
        // Project identity is server-owned. The legacy API cannot move an existing requirement.
        record.setProjectId(targetProject);
        if(selected==null){HrmPayrollRequirementProjectDO legacyScope=projectMapper.selectById(targetProject);projectService.bind(record,legacyScope,request.getScopeMode(),request.getScopeJson());if("CONFIRMED".equals(record.getStatus())){record.setConfirmedScopeVersion(legacyScope.getProjectScopeVersion());record.setConfirmedScopeJson(record.getScopeJson());}projectService.requireRow(record,actorId,action);}
        if(previous!=null){record.setTemplateItemId(previous.getTemplateItemId());record.setTemplateVersion(previous.getTemplateVersion());record.setScopeDecision(previous.getScopeDecision());}
        if(selected!=null) {
            projectService.bind(record,selected,request.getScopeMode(),request.getScopeJson());
            projectService.requireRow(record,actorId,action);
            if("CONFIRMED".equals(record.getStatus()) && (selected.getProjectScopeVersion()==null || !"MATCH".equals(record.getScopeState())))throw exception(PAYROLL_PROJECT_SCOPE_STALE);
            if("CONFIRMED".equals(record.getStatus())){record.setConfirmedScopeVersion(selected.getProjectScopeVersion());record.setConfirmedScopeJson(record.getScopeJson());}
        }
        if(record.getProjectId()==null) throw exception(PAYROLL_REVIEW_INCOMPLETE);
        boolean structured=selected!=null && !"LEGACY".equals(selected.getProjectKey());
        record.setBusinessContextJson(request.getBusinessContextJson()==null && previous!=null?previous.getBusinessContextJson():request.getBusinessContextJson());
        if(previous!=null){record.setScopeDecisionJson(previous.getScopeDecisionJson());record.setImplementationJson(previous.getImplementationJson());record.setCollaborationJson(previous.getCollaborationJson());record.setLegacyReviewStatus(previous.getLegacyReviewStatus());}
        if(structured){HrmPayrollReviewService.validateBusiness(record,confirming);
            if("DEFERRED".equals(record.getStatus()))throw exception(PAYROLL_REVIEW_INCOMPLETE);
            if("DISPUTED".equals(record.getStatus()) && record.getBusinessContextJson()!=null && !projectService.visible(record,HrmPayrollReviewService.business(record.getBusinessContextJson()).getDisputeOwnerId(),"READ"))throw exception(PAYROLL_PROJECT_DENIED);
        }
        record.setSourceApplicability(request.getSourceApplicability()==null?(previous==null?"APPLICABLE":previous.getSourceApplicability()):request.getSourceApplicability());
        record.setApplicabilityReason(request.getApplicabilityReason()==null && previous!=null?previous.getApplicabilityReason():request.getApplicabilityReason());record.setRuleVersion(request.getRuleVersion()==null && previous!=null?previous.getRuleVersion():request.getRuleVersion());
        boolean semanticChanged=previous!=null && reviewContractChanged(previous,request);
        record.setSemanticVersion(previous==null?1:(previous.getSemanticVersion()==null?1:previous.getSemanticVersion())+(semanticChanged?1:0));
        record.setConfirmedTupleJson(previous==null?null:previous.getConfirmedTupleJson());
        if(confirming){if(!Objects.equals(actorId,record.getReviewerId()))throw exception(PAYROLL_REQUIREMENT_CONFIRMATION_INVALID);HrmPayrollRequirementProjectDO bound=projectMapper.selectById(targetProject);record.setConfirmedScopeVersion(bound.getProjectScopeVersion());record.setConfirmedScopeJson(record.getScopeJson());HrmPayrollReviewService.validateBusiness(record,true);record.setConfirmedTupleJson(workbench.confirmationTuple(record,actorId));}
        if(!confirming&&(semanticChanged||!"CONFIRMED".equals(record.getStatus())))record.setConfirmedTupleJson(null);
        if(structured){
            // Readiness is computed after persistence; ordinary forms cannot assert quality.
            record.setReadiness("MISSING");
        }
        record.setReadiness("MISSING"); // Legacy text is inventory too; no ordinary save can assert READY.
        record.setVersion(previous==null ? 1 : previous.getVersion()+1);
        if ("CONFIRMED".equals(record.getStatus())) {
            if(previous!=null && "CONFIRMED".equals(previous.getStatus()))
                record.setConfirmedBy(previous.getConfirmedBy()).setConfirmedAt(previous.getConfirmedAt());
            else {record.setConfirmedBy(actorId);record.setConfirmedAt(LocalDateTime.now());}
        }
        if (previous==null) mapper.insert(record);
        else {
            record.setUpdater(String.valueOf(actorId));
            record.setUpdateTime(LocalDateTime.now());
            // Persist scope fields explicitly alongside the existing nullable review contract.
            mapper.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getId,record.getId()).set(HrmPayrollRequirementDO::getScopeMode,record.getScopeMode()).set(HrmPayrollRequirementDO::getScopeJson,record.getScopeJson()).set(HrmPayrollRequirementDO::getScopeState,record.getScopeState()).set(HrmPayrollRequirementDO::getProjectScopeVersion,record.getProjectScopeVersion()).set(HrmPayrollRequirementDO::getConfirmedScopeVersion,record.getConfirmedScopeVersion()).set(HrmPayrollRequirementDO::getConfirmedScopeJson,record.getConfirmedScopeJson()));
            // Replace nullable review fields explicitly when returning to pending/reviewing.
            mapper.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmPayrollRequirementDO>()
                .eq(HrmPayrollRequirementDO::getId,record.getId())
                .set(HrmPayrollRequirementDO::getUpdater,record.getUpdater()).set(HrmPayrollRequirementDO::getUpdateTime,record.getUpdateTime())
                .set(HrmPayrollRequirementDO::getCode,record.getCode()).set(HrmPayrollRequirementDO::getModule,record.getModule())
                .set(HrmPayrollRequirementDO::getDescription,record.getDescription()).set(HrmPayrollRequirementDO::getFieldMapping,record.getFieldMapping())
                .set(HrmPayrollRequirementDO::getSourceSystem,record.getSourceSystem()).set(HrmPayrollRequirementDO::getSourceOwnerId,record.getSourceOwnerId())
                .set(HrmPayrollRequirementDO::getReviewerId,record.getReviewerId()).set(HrmPayrollRequirementDO::getPriority,record.getPriority())
                .set(HrmPayrollRequirementDO::getStatus,record.getStatus()).set(HrmPayrollRequirementDO::getReadiness,record.getReadiness())
                .set(HrmPayrollRequirementDO::getAcceptance,record.getAcceptance()).set(HrmPayrollRequirementDO::getEvidence,record.getEvidence())
                .set(HrmPayrollRequirementDO::getVersion,record.getVersion()).set(HrmPayrollRequirementDO::getConfirmedBy,record.getConfirmedBy())
                .set(HrmPayrollRequirementDO::getConfirmedAt,record.getConfirmedAt()));
        }
        mapper.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getId,record.getId()).set(HrmPayrollRequirementDO::getSourceApplicability,record.getSourceApplicability()).set(HrmPayrollRequirementDO::getApplicabilityReason,record.getApplicabilityReason()).set(HrmPayrollRequirementDO::getRuleVersion,record.getRuleVersion()).set(HrmPayrollRequirementDO::getSemanticVersion,record.getSemanticVersion()).set(HrmPayrollRequirementDO::getConfirmedTupleJson,record.getConfirmedTupleJson()).set(HrmPayrollRequirementDO::getBusinessContextJson,record.getBusinessContextJson()));
        if(structured)workbench.applicabilityChanged(previous,record,actorId);
        HrmPayrollRequirementHistoryDO history=new HrmPayrollRequirementHistoryDO();
        history.setRequirementId(record.getId());history.setVersion(record.getVersion());
        history.setActorId(actorId);history.setSnapshot(JsonUtils.toJsonString(record));historyMapper.insert(history);
        return record.getId();
    }
    private void guardTarget(Long id,Long actor,String action) {
        HrmPayrollRequirementDO r=mapper.selectById(id);
        if(r==null)return;
        HrmPayrollRequirementProjectDO p=projectMapper.selectById(r.getProjectId());
        if(p==null)throw exception(PAYROLL_PROJECT_DENIED);
        projectService.requireRow(r,actor,action);
    }
    private boolean reviewContractChanged(HrmPayrollRequirementDO before,HrmPayrollRequirementSaveReqVO after) {
        return (after.getBusinessContextJson()!=null && !Objects.equals(before.getBusinessContextJson(),after.getBusinessContextJson())) || (after.getSourceApplicability()!=null && !Objects.equals(before.getSourceApplicability(),after.getSourceApplicability()))
            || (after.getApplicabilityReason()!=null && !Objects.equals(before.getApplicabilityReason(),after.getApplicabilityReason()))
            || (after.getRuleVersion()!=null && !Objects.equals(before.getRuleVersion(),after.getRuleVersion()))
            || (after.getProjectId()!=null && !Objects.equals(before.getScopeMode(),after.getScopeMode()))
            || (after.getProjectId()!=null && !Objects.equals(before.getScopeJson(),after.getScopeJson()))
            || !Objects.equals(before.getCode(),after.getCode())
            || !Objects.equals(before.getModule(),after.getModule())
            || !Objects.equals(before.getDescription(),after.getDescription())
            || !Objects.equals(before.getFieldMapping(),after.getFieldMapping())
            || !Objects.equals(before.getSourceSystem(),after.getSourceSystem())
            || !Objects.equals(before.getSourceOwnerId(),after.getSourceOwnerId())
            || !Objects.equals(before.getReviewerId(),after.getReviewerId())
            || !Objects.equals(before.getAcceptance(),after.getAcceptance())
            || !Objects.equals(before.getEvidence(),after.getEvidence())
            || !Objects.equals(before.getPriority(),after.getPriority());
    }
    private void requireActiveUser(Long id) {
        cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO user = id == null ? null : users.getUser(id);
        if (user == null || !Objects.equals(user.getStatus(), 0)) throw exception(PAYROLL_REQUIREMENT_PERSON_INVALID);
    }

    private boolean meaningfulMapping(String mapping) {
        if (StrUtil.isBlank(mapping)) return false;
        String text = mapping.trim();
        if (text.toUpperCase(java.util.Locale.ROOT).startsWith("N/A") || text.startsWith("不适用")
                || text.equalsIgnoreCase("NA") || text.matches("(?is)^NA\\s*[:：].*")) {
            return text.matches("(?is)^(N/A|NA|不适用)\\s*[:：]\\s*\\S.*$");
        }
        return true;
    }

    @Override public List<HrmPayrollRequirementHistoryDO> history(Long id) {
        guardTarget(id,cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId(),"READ");
        if (mapper.selectById(id)==null) throw exception(PAYROLL_REQUIREMENT_NOT_EXISTS);
        List<HrmPayrollRequirementHistoryDO> result=historyMapper.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementHistoryDO>().eq(HrmPayrollRequirementHistoryDO::getRequirementId,id).orderByDesc(HrmPayrollRequirementHistoryDO::getVersion));
        HrmPayrollRequirementDO current=mapper.selectById(id);
        boolean legacy="LEGACY".equals(projectMapper.selectById(current.getProjectId()).getProjectKey());
        Long actor=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId();
        return result.stream().filter(h->{try{HrmPayrollRequirementDO snapshot=JsonUtils.parseObject(h.getSnapshot(),HrmPayrollRequirementDO.class);if(legacy && snapshot.getProjectId()==null)snapshot.setProjectId(current.getProjectId());return projectService.visible(snapshot,actor,"READ");}catch(RuntimeException ex){return false;}}).peek(h->{HrmPayrollRequirementDO snapshot=JsonUtils.parseObject(h.getSnapshot(),HrmPayrollRequirementDO.class);if(snapshot.getConfirmedTupleJson()!=null){snapshot.setConfirmedTupleJson(workbench.visibleConfirmationTuple(snapshot.getConfirmedTupleJson(),current.getProjectId(),actor));}projectService.sanitizeReviewMetadata(snapshot,actor);h.setSnapshot(JsonUtils.toJsonString(snapshot));}).collect(java.util.stream.Collectors.toList());
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public int initialize(Long actorId) {
        int count=0;Long legacy=ensureLegacyProject();
        projectService.check(legacy,actorId,"EDIT",true);
        if(!projectService.full(legacy,actorId,"READ") || !projectService.full(legacy,actorId,"EDIT"))throw exception(PAYROLL_PROJECT_DENIED);
        for(String[] seed:SEEDS) {
            if(mapper.selectOne(new LambdaQueryWrapperX<HrmPayrollRequirementDO>().eq(HrmPayrollRequirementDO::getProjectId,legacy).eq(HrmPayrollRequirementDO::getCode,seed[0]))!=null) continue;
            HrmPayrollRequirementSaveReqVO request=new HrmPayrollRequirementSaveReqVO();
            request.setCode(seed[0]);request.setModule(seed[0].split("-")[0]);request.setDescription(seed[1]);
            request.setPriority(2);request.setStatus("PENDING");request.setReadiness("MISSING");save(request,actorId);count++;
        }
        return count;
    }
}
