package cn.iocoder.yudao.module.hrm.service.payroll.eligibility;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.eligibility.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity.HrmPayrollMappingRespVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.eligibility.HrmPayrollEligibilityDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollReviewMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.eligibility.HrmPayrollEligibilityMapper;
import cn.iocoder.yudao.module.hrm.service.payroll.identity.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

/** Explicit, bounded qualifications. Current personnel state never supplies a default qualification. */
@Service
public class HrmPayrollEligibilityServiceImpl implements HrmPayrollEligibilityService {
    @Resource private HrmPayrollEligibilityMapper mapper;
    @Resource private HrmPayrollReviewMapper reviewMapper;
    @Resource private HrmPayrollEmployeeAccess access;
    @Resource private HrmPayrollEmployeeMappingService personnel;
    @Resource private AdminUserApi adminUserApi;
    private Long tenant() { return TenantContextHolder.getRequiredTenantId(); }
    private LambdaQueryWrapperX<HrmPayrollEligibilityDO> query() {
        return new LambdaQueryWrapperX<HrmPayrollEligibilityDO>().eq(HrmPayrollEligibilityDO::getTenantId,tenant());
    }
    private LambdaQueryWrapperX<HrmPayrollEligibilityDO> series(HrmPayrollEligibilityDO row) {
        return query().eq(HrmPayrollEligibilityDO::getEntityCode,row.getEntityCode()).eq(HrmPayrollEligibilityDO::getEmployeeId,row.getEmployeeId());
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public Long create(HrmPayrollEligibilitySaveReqVO request) {
        validate(request,false);
        HrmPayrollEligibilityDO row=editable(request).setId(null).setEligibilityVersion(1).setRevision(1).setStatus(0);
        if(mapper.selectCount(series(row))>0) throw exception(PAYROLL_ELIGIBILITY_DUPLICATE);
        try { mapper.insert(row); } catch(DuplicateKeyException e) { throw exception(PAYROLL_ELIGIBILITY_DUPLICATE); }
        audit("create",null,row,"登记计薪资格草稿；资格结论尚未确认");return row.getId();
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public void update(HrmPayrollEligibilitySaveReqVO request) {
        HrmPayrollEligibilityDO before=locked(request.getId());revision(before,request.getRevision());
        if(before.getStatus()!=0) throw exception(PAYROLL_ELIGIBILITY_IMMUTABLE);
        validate(request,false);
        check(Objects.equals(before.getEntityCode(),request.getEntityCode())&&Objects.equals(before.getEmployeeId(),request.getEmployeeId()),"主体编号和 HRM 人员 ID 固定，不能修改");
        HrmPayrollEligibilityDO after=editable(request).setEligibilityVersion(before.getEligibilityVersion()).setRevision(before.getRevision()+1).setStatus(0);
        mapper.updateById(after);audit("update",before,after,"刷新草稿和人员快照；不自动推定计薪资格");
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public Long newVersion(Long id,Integer expected) {
        HrmPayrollEligibilityDO selected=locked(id);revision(selected,expected);
        HrmPayrollEligibilityDO latest=mapper.selectOne(series(selected).orderByDesc(HrmPayrollEligibilityDO::getEligibilityVersion).last("LIMIT 1"));
        HrmPayrollEligibilitySaveReqVO request=BeanUtils.toBean(selected,HrmPayrollEligibilitySaveReqVO.class).setEmployeeFingerprint(null);
        HrmPayrollEligibilityDO row=editable(request).setId(null).setEligibilityVersion(latest.getEligibilityVersion()+1).setRevision(1).setStatus(0);
        mapper.insert(row);audit("new-version",null,row,"从 V"+selected.getEligibilityVersion()+" 新建草稿，刷新档案快照，须重新核对资格与有效期");return row.getId();
    }
    @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public void review(HrmPayrollEligibilityReviewReqVO request) {
        check(request!=null,"请求不能为空");text(request.getEvidence(),4000,true,"评审依据");
        check("confirm".equals(request.getAction())||"retire".equals(request.getAction()),"评审操作不合法");
        HrmPayrollEligibilityDO before=locked(request.getId());revision(before,request.getRevision());boolean confirm="confirm".equals(request.getAction());
        if(confirm) {
            check(before.getStatus()==0,"只有草稿可确认");validate(BeanUtils.toBean(before,HrmPayrollEligibilitySaveReqVO.class),true);
            HrmPayrollMappingRespVO current=snapshot(before.getEmployeeId(),true);
            if(!Objects.equals(before.getEmployeeFingerprint(),current.getEmployeeFingerprint())) throw exception(PAYROLL_ELIGIBILITY_PERSON_CHANGED);
            if(mapper.selectCount(series(before).eq(HrmPayrollEligibilityDO::getStatus,1)
                    .le(HrmPayrollEligibilityDO::getEffectiveFrom,before.getEffectiveTo()).ge(HrmPayrollEligibilityDO::getEffectiveTo,before.getEffectiveFrom()))>0)
                throw exception(PAYROLL_ELIGIBILITY_OVERLAP);
        } else check(before.getStatus()==1,"只有已确认资格可停用");
        HrmPayrollEligibilityDO after=BeanUtils.toBean(before,HrmPayrollEligibilityDO.class).setStatus(confirm?1:2).setRevision(before.getRevision()+1)
                .setReviewedBy(getLoginUserId()).setReviewedByName(actorName()).setReviewedTime(LocalDateTime.now()).setEvidence(request.getEvidence().trim());
        mapper.updateById(after);audit(request.getAction(),before,after,request.getEvidence().trim());
    }
    private HrmPayrollEligibilityDO editable(HrmPayrollEligibilitySaveReqVO request) {
        HrmPayrollMappingRespVO person=snapshot(request.getEmployeeId(),true);
        if(request.getEmployeeFingerprint()!=null&&!Objects.equals(request.getEmployeeFingerprint(),person.getEmployeeFingerprint())) throw exception(PAYROLL_ELIGIBILITY_PERSON_CHANGED);
        HrmPayrollEligibilityDO row=BeanUtils.toBean(request,HrmPayrollEligibilityDO.class).setRevision(null)
                .setEntityName(request.getEntityName().trim()).setQualification(StrUtil.trimToNull(request.getQualification()))
                .setOwnerName(StrUtil.trimToNull(request.getOwnerName())).setReference(StrUtil.trimToNull(request.getReference())).setReason(StrUtil.trimToNull(request.getReason()))
                .setSnapshotName(person.getSnapshotName()).setSnapshotJobNumber(person.getSnapshotJobNumber()).setSnapshotDeptId(person.getSnapshotDeptId())
                .setSnapshotUserId(person.getSnapshotUserId()).setSnapshotEntryTime(person.getSnapshotEntryTime()).setSnapshotLeaveTime(person.getSnapshotLeaveTime())
                .setSnapshotEmployeeStatus(person.getSnapshotEmployeeStatus()).setSnapshotCapturedAt(person.getSnapshotCapturedAt()).setEmployeeFingerprint(person.getEmployeeFingerprint());
        row.setTenantId(tenant());return row;
    }
    private HrmPayrollMappingRespVO snapshot(Long id,boolean lock) {
        try { if(lock)access.employee(id,true);return personnel.employee(id); }
        catch(ServiceException e) { if(Objects.equals(e.getCode(),PAYROLL_MAPPING_NOT_EXISTS.getCode()))throw exception(PAYROLL_ELIGIBILITY_NOT_EXISTS);throw e; }
    }
    private HrmPayrollEligibilityDO require(Long id,boolean lock) {
        LambdaQueryWrapperX<HrmPayrollEligibilityDO> q=query().eq(HrmPayrollEligibilityDO::getId,id);if(lock)q.last("FOR UPDATE");
        HrmPayrollEligibilityDO row=id==null?null:mapper.selectOne(q);if(row==null)throw exception(PAYROLL_ELIGIBILITY_NOT_EXISTS);
        try { access.require(row.getEmployeeId(),row.getSnapshotDeptId(),row.getSnapshotUserId()); }
        catch(ServiceException e) { if(Objects.equals(e.getCode(),PAYROLL_MAPPING_NOT_EXISTS.getCode()))throw exception(PAYROLL_ELIGIBILITY_NOT_EXISTS);throw e; }
        return row;
    }
    private HrmPayrollEligibilityDO locked(Long id) {
        HrmPayrollEligibilityDO row=require(id,false);
        mapper.selectOne(series(row).eq(HrmPayrollEligibilityDO::getEligibilityVersion,1).last("FOR UPDATE"));
        return require(id,true);
    }
    private void validate(HrmPayrollEligibilitySaveReqVO request,boolean confirm) {
        check(request!=null,"请求不能为空");check(request.getEntityCode()!=null&&request.getEntityCode().matches("[A-Z0-9][A-Z0-9_-]{0,63}"),"主体编号须为稳定英文编号");
        text(request.getEntityName(),160,true,"声明主体名称");check(request.getEmployeeId()!=null&&request.getEmployeeId()>0,"必须选择 HRM 人员 ID");
        check(request.getQualification()==null||Arrays.asList("INCLUDED","EXCLUDED").contains(request.getQualification()),"资格只能明确选择纳入或排除");
        text(request.getOwnerName(),120,confirm,"资格负责人");text(request.getReference(),2000,confirm,"资格依据");text(request.getReason(),2000,confirm,"资格理由");
        check(request.getEmployeeFingerprint()==null||request.getEmployeeFingerprint().matches("[a-f0-9]{64}"),"人员快照校验值不合法");
        date(request.getEffectiveFrom());date(request.getEffectiveTo());
        check(request.getEffectiveTo()==null||(request.getEffectiveFrom()!=null&&!request.getEffectiveFrom().isAfter(request.getEffectiveTo())),"有效期结束不能早于开始，且须先填写开始日期");
        if(confirm) { check(request.getQualification()!=null,"确认必须明确计薪资格");check(request.getEffectiveFrom()!=null&&request.getEffectiveTo()!=null,"确认必须填写完整、有限的资格有效期"); }
    }
    private void text(String value,int max,boolean required,String label) {
        check(!required||StrUtil.isNotBlank(value),label+"不能为空");check(value==null||(value.length()<=max&&!value.matches("(?s).*[^\\P{Cntrl}\\n\\r\\t].*")),label+"超过长度或包含非法控制字符");
    }
    private void date(LocalDate date) { check(date==null||(date.getYear()>=1000&&date.getYear()<=9999),"日期超过数据库支持范围"); }
    private void check(boolean valid,String message) { if(!valid)throw exception(PAYROLL_ELIGIBILITY_INVALID,message); }
    private void revision(HrmPayrollEligibilityDO row,Integer expected) { if(!Objects.equals(row.getRevision(),expected))throw exception(PAYROLL_ELIGIBILITY_STALE); }
    @Override public HrmPayrollEligibilityRespVO get(Long id) { return BeanUtils.toBean(require(id,false),HrmPayrollEligibilityRespVO.class); }
    @Override public HrmPayrollMappingRespVO employee(Long id) { return snapshot(id,false); }
    @Override public PageResult<HrmPayrollEligibilityRespVO> page(HrmPayrollEligibilityPageReqVO request) {
        LambdaQueryWrapperX<HrmPayrollEligibilityDO> q=access.filter(query(),"hrm_payroll_employee_eligibility")
                .eqIfPresent(HrmPayrollEligibilityDO::getEntityCode,StrUtil.trimToNull(request.getEntityCode())).eqIfPresent(HrmPayrollEligibilityDO::getEmployeeId,request.getEmployeeId())
                .eqIfPresent(HrmPayrollEligibilityDO::getStatus,request.getStatus()).eqIfPresent(HrmPayrollEligibilityDO::getQualification,request.getQualification());
        String search=StrUtil.trimToNull(request.getSearch());if(search!=null)q.and(w->w.like(HrmPayrollEligibilityDO::getSnapshotName,search).or().like(HrmPayrollEligibilityDO::getSnapshotJobNumber,search));
        return BeanUtils.toBean(mapper.selectPage(request,q.orderByDesc(HrmPayrollEligibilityDO::getId)),HrmPayrollEligibilityRespVO.class);
    }
    @Override public List<HrmPayrollReviewDO> history(Long id) {
        require(id,false);List<HrmPayrollReviewDO> rows=reviewMapper.selectList(new LambdaQueryWrapperX<HrmPayrollReviewDO>().eq(HrmPayrollReviewDO::getTenantId,tenant())
                .eq(HrmPayrollReviewDO::getObjectType,"employee-eligibility").eq(HrmPayrollReviewDO::getObjectId,id).orderByDesc(HrmPayrollReviewDO::getId));
        if(!Boolean.TRUE.equals(access.scope().getAll()))rows.forEach(row->row.setBeforeSnapshot(null).setAfterSnapshot(null).setReason(null));return rows;
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public HrmPayrollEligibilityLookupVO lookup(HrmPayrollEligibilityLookupReqVO request) {
        check(request!=null&&request.getEntityCode()!=null&&request.getEntityCode().matches("[A-Z0-9][A-Z0-9_-]{0,63}")&&request.getEmployeeId()!=null&&request.getEmployeeId()>0,"必须提供主体编号和 HRM 人员 ID");
        check(request.getStart()!=null&&request.getEnd()!=null&&!request.getStart().isAfter(request.getEnd()),"必须提供合法的完整期间");date(request.getStart());date(request.getEnd());
        access.scope();HrmPayrollMappingRespVO current;
        try { current=snapshot(request.getEmployeeId(),false); }
        catch(ServiceException e) { if(Objects.equals(e.getCode(),PAYROLL_ELIGIBILITY_NOT_EXISTS.getCode()))return issue("PERSON_UNAVAILABLE","人员档案不存在或不可访问，不能判断资格",null,false);throw e; }
        List<HrmPayrollEligibilityDO> choices=mapper.selectList(access.filter(query(),"hrm_payroll_employee_eligibility")
                .eq(HrmPayrollEligibilityDO::getEntityCode,request.getEntityCode()).eq(HrmPayrollEligibilityDO::getEmployeeId,request.getEmployeeId())
                .eq(HrmPayrollEligibilityDO::getStatus,1).le(HrmPayrollEligibilityDO::getEffectiveFrom,request.getStart()).ge(HrmPayrollEligibilityDO::getEffectiveTo,request.getEnd()));
        if(choices.size()!=1)return issue(choices.isEmpty()?"UNRESOLVED_QUALIFICATION":"AMBIGUOUS_QUALIFICATION","期间内须有唯一、已确认且有权查看的资格版本；请核对有效期和版本",null,false);
        HrmPayrollEligibilityRespVO row=BeanUtils.toBean(choices.get(0),HrmPayrollEligibilityRespVO.class);
        if(!Objects.equals(row.getEmployeeFingerprint(),current.getEmployeeFingerprint()))return issue("PERSON_CHANGED","当前人员档案与确认快照不同，须核对变化并重新登记或评审",row,true);
        return new HrmPayrollEligibilityLookupVO().setMatched(true).setPersonChanged(false).setEligibility(row)
                .setIssueCode("EXCLUDED".equals(row.getQualification())?"EXCLUDED":null)
                .setExplanation("EXCLUDED".equals(row.getQualification())?"该已确认版本明确排除此人在声明期间内的计薪资格":"匹配到声明期间内的已确认纳入资格；工资输入与计算准备须另行核验");
    }
    private HrmPayrollEligibilityLookupVO issue(String code,String explanation,HrmPayrollEligibilityRespVO row,boolean changed) {
        return new HrmPayrollEligibilityLookupVO().setMatched(false).setIssueCode(code).setExplanation(explanation).setEligibility(row).setPersonChanged(changed);
    }
    private String actorName() { return Optional.ofNullable(adminUserApi.getUser(getLoginUserId())).map(u->u.getNickname()).orElse("当前登录人员"); }
    private void audit(String action,HrmPayrollEligibilityDO before,HrmPayrollEligibilityDO after,String reason) {
        HrmPayrollReviewDO row=new HrmPayrollReviewDO().setObjectType("employee-eligibility").setObjectId(after.getId()).setAction(action)
                .setFromVersion(before==null?null:before.getRevision()).setToVersion(after.getRevision()).setActorId(getLoginUserId()).setActorName(actorName()).setReason(reason)
                .setBeforeSnapshot(before==null?null:JsonUtils.toJsonString(before)).setAfterSnapshot(JsonUtils.toJsonString(after));
        row.setTenantId(tenant());reviewMapper.insert(row);
    }
}
