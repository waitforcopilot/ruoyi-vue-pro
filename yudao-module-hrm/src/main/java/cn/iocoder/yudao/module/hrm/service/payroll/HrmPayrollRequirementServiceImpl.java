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
    @Resource private cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    @Resource private HrmPayrollRequirementMapper mapper;
    @Resource private cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
    @Resource private HrmPayrollRequirementHistoryMapper historyMapper;
    @Resource private HrmPayrollRequirementProjectMapper projectMapper;
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
        return mapper.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementDO>()
            .eqIfPresent(HrmPayrollRequirementDO::getModule, module).orderByAsc(HrmPayrollRequirementDO::getCode));
    }
    @Override @Transactional(rollbackFor=Exception.class)
    public Long save(HrmPayrollRequirementSaveReqVO request, Long actorId) {
        return saveInternal(request,actorId,false);
    }
    @Override @Transactional(rollbackFor=Exception.class)
    public void confirm(Long id,Integer version,Long actorId) {
        if(id==null||version==null||version<1||actorId==null) throw exception(PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);
        HrmPayrollRequirementDO row=mapper.selectOneForUpdate(HrmPayrollRequirementDO::getId,id);
        if(row==null) throw exception(PAYROLL_REQUIREMENT_NOT_EXISTS);
        if(!Objects.equals(row.getVersion(),version)) throw exception(PAYROLL_VERSION_CONFLICT);
        if(!java.util.Arrays.asList("PENDING","REVIEWING","DISPUTED").contains(row.getStatus()))
            throw exception(PAYROLL_REQUIREMENT_CONFIRM_ACTION_REQUIRED);
        HrmPayrollRequirementSaveReqVO request=BeanUtils.toBean(row,HrmPayrollRequirementSaveReqVO.class);
        request.setStatus("CONFIRMED");
        saveInternal(request,actorId,true);
    }
    private Long saveInternal(HrmPayrollRequirementSaveReqVO request,Long actorId,boolean confirming) {
        HrmPayrollRequirementDO previous = request.getId()==null ? null : mapper.selectOneForUpdate(HrmPayrollRequirementDO::getId, request.getId());
        if (request.getId()!=null && previous==null) throw exception(PAYROLL_REQUIREMENT_NOT_EXISTS);
        if (previous!=null && !Objects.equals(previous.getVersion(),request.getVersion())) throw exception(PAYROLL_VERSION_CONFLICT);
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
            if(actorId==null || !permissions.hasAnyPermissions(actorId,"hrm:payroll:requirement:assign")
                || Objects.equals(actorId,request.getReviewerId()))
                throw exception(PAYROLL_REQUIREMENT_ASSIGN_FORBIDDEN);
            if(request.getReviewerId()!=null) {
                requireActiveUser(request.getReviewerId());
                if(!permissions.hasAnyPermissions(request.getReviewerId(),"hrm:payroll:requirement:query")
                    || !permissions.hasAnyPermissions(request.getReviewerId(),"hrm:payroll:requirement:confirm"))
                    throw exception(PAYROLL_REQUIREMENT_ASSIGNEE_FORBIDDEN);
            }
        }
        if (("CONFIRMED".equals(request.getStatus()) || "DISPUTED".equals(request.getStatus()) || "DEFERRED".equals(request.getStatus()))
            && (StrUtil.isBlank(request.getEvidence()) || request.getReviewerId()==null)) throw exception(PAYROLL_REVIEW_INCOMPLETE);
        if ("CONFIRMED".equals(request.getStatus()) && (StrUtil.isBlank(request.getAcceptance())
            || StrUtil.isBlank(request.getSourceSystem()) || request.getSourceOwnerId()==null)) throw exception(PAYROLL_REVIEW_INCOMPLETE);
        if (java.util.Arrays.asList("CONFIRMED", "DISPUTED", "DEFERRED").contains(request.getStatus())) {
            requireActiveUser(request.getReviewerId());
        }
        if ("CONFIRMED".equals(request.getStatus())) {
            requireActiveUser(request.getSourceOwnerId());
            if (!Objects.equals(actorId, request.getReviewerId()) || !meaningfulMapping(request.getFieldMapping()))
                throw exception(PAYROLL_REQUIREMENT_CONFIRMATION_INVALID);
        }
        HrmPayrollRequirementDO duplicate=mapper.selectOne(HrmPayrollRequirementDO::getCode,request.getCode());
        if (duplicate!=null && !Objects.equals(duplicate.getId(),request.getId())) throw exception(PAYROLL_REQUIREMENT_DUPLICATE);
        HrmPayrollRequirementDO record=BeanUtils.toBean(request,HrmPayrollRequirementDO.class);
        // Project identity is server-owned. The legacy API cannot move an existing requirement.
        record.setProjectId(previous==null ? ensureLegacyProject() : previous.getProjectId());
        if(record.getProjectId()==null) throw exception(PAYROLL_REVIEW_INCOMPLETE);
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
        HrmPayrollRequirementHistoryDO history=new HrmPayrollRequirementHistoryDO();
        history.setRequirementId(record.getId());history.setVersion(record.getVersion());
        history.setActorId(actorId);history.setSnapshot(JsonUtils.toJsonString(record));historyMapper.insert(history);
        return record.getId();
    }
    private boolean reviewContractChanged(HrmPayrollRequirementDO before,HrmPayrollRequirementSaveReqVO after) {
        return !Objects.equals(before.getCode(),after.getCode())
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
        if (mapper.selectById(id)==null) throw exception(PAYROLL_REQUIREMENT_NOT_EXISTS);
        return historyMapper.selectList(new LambdaQueryWrapperX<HrmPayrollRequirementHistoryDO>()
            .eq(HrmPayrollRequirementHistoryDO::getRequirementId,id).orderByDesc(HrmPayrollRequirementHistoryDO::getVersion));
    }
    @Override @Transactional(rollbackFor=Exception.class)
    public int initialize(Long actorId) {
        int count=0;
        for(String[] seed:SEEDS) {
            if(mapper.selectOne(HrmPayrollRequirementDO::getCode,seed[0])!=null) continue;
            HrmPayrollRequirementSaveReqVO request=new HrmPayrollRequirementSaveReqVO();
            request.setCode(seed[0]);request.setModule(seed[0].split("-")[0]);request.setDescription(seed[1]);
            request.setPriority(2);request.setStatus("PENDING");request.setReadiness("MISSING");save(request,actorId);count++;
        }
        return count;
    }
}
