package cn.iocoder.yudao.module.hrm.service.payroll.identity;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.identity.HrmPayrollMappingLookupVO.Match;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.identity.HrmPayrollEmployeeMappingDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.identity.HrmPayrollEmployeeMappingMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

@Service
public class HrmPayrollEmployeeMappingServiceImpl implements HrmPayrollEmployeeMappingService {
    @Resource private HrmPayrollEmployeeMappingMapper mapper;
    @Resource private HrmPayrollSourceMapper sourceMapper;
    @Resource private HrmPayrollReviewMapper reviewMapper;
    @Resource private HrmPayrollEmployeeAccess access;
    @Resource private HrmEmployeeMapper employeeMapper;
    @Resource private PermissionApi permissionApi;
    @Resource private AdminUserApi adminUserApi;
    private Long tenant() { return TenantContextHolder.getRequiredTenantId(); }
    private LambdaQueryWrapperX<HrmPayrollEmployeeMappingDO> query() {
        return new LambdaQueryWrapperX<HrmPayrollEmployeeMappingDO>().eq(HrmPayrollEmployeeMappingDO::getTenantId,tenant());
    }
    @Override
    @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public Long create(HrmPayrollMappingSaveReqVO request) {
        validate(request,false);
        HrmPayrollSourceDO source=sourceMapper.selectTenantById(request.getSourceId(),tenant(),true);
        check(source!=null,"来源不存在或不可访问");
        String key=digest(Arrays.asList(tenant(),source.getId(),request.getNamespace(),request.getExternalCode()));
        if(mapper.root(key,tenant(),false)!=null) throw exception(PAYROLL_MAPPING_DUPLICATE);
        HrmPayrollEmployeeMappingDO row=editable(request).setId(null).setSourceCode(source.getCode()).setIdentityKey(key)
                .setMappingVersion(1).setRevision(1).setStatus(0);
        try { mapper.insert(row); } catch(DuplicateKeyException e) { throw exception(PAYROLL_MAPPING_DUPLICATE); }
        audit("create",null,row,"登记编号映射草稿，尚未确认");return row.getId();
    }
    @Override
    @Transactional(rollbackFor=Exception.class)
    public void update(HrmPayrollMappingSaveReqVO request) {
        HrmPayrollEmployeeMappingDO before=require(request.getId(),true);revision(before,request.getRevision());
        if(before.getStatus()!=0) throw exception(PAYROLL_MAPPING_IMMUTABLE);
        validate(request,false);
        check(Objects.equals(before.getSourceId(),request.getSourceId())&&Objects.equals(before.getNamespace(),request.getNamespace())
                &&Objects.equals(before.getExternalCode(),request.getExternalCode()),"来源、命名空间和外部编号固定，不能修改");
        HrmPayrollEmployeeMappingDO after=editable(request).setIdentityKey(before.getIdentityKey()).setSourceCode(before.getSourceCode())
                .setMappingVersion(before.getMappingVersion()).setRevision(before.getRevision()+1).setStatus(0);
        mapper.updateById(after);audit("update",before,after,"核对目标人员并刷新草稿快照");
    }
    @Override
    @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public Long newVersion(Long id,Integer expected,Long employeeId) {
        HrmPayrollEmployeeMappingDO selected=require(id,false);
        mapper.root(selected.getIdentityKey(),tenant(),true);selected=require(id,true);revision(selected,expected);
        HrmPayrollEmployeeMappingDO latest=mapper.selectOne(query().eq(HrmPayrollEmployeeMappingDO::getIdentityKey,selected.getIdentityKey())
                .orderByDesc(HrmPayrollEmployeeMappingDO::getMappingVersion).last("LIMIT 1"));
        HrmPayrollMappingSaveReqVO request=BeanUtils.toBean(selected,HrmPayrollMappingSaveReqVO.class)
                .setEmployeeId(employeeId==null?selected.getEmployeeId():employeeId).setEmployeeFingerprint(null);
        HrmPayrollEmployeeMappingDO row=editable(request).setId(null).setSourceCode(selected.getSourceCode()).setIdentityKey(selected.getIdentityKey())
                .setMappingVersion(latest.getMappingVersion()+1).setRevision(1).setStatus(0);
        mapper.insert(row);audit("new-version",null,row,"从 V"+selected.getMappingVersion()+" 新建草稿，刷新人员快照且不继承评审结论");return row.getId();
    }
    @Override
    @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public void review(HrmPayrollMappingReviewReqVO request) {
        check(request!=null && StrUtil.isNotBlank(request.getEvidence()),"必须填写评审依据");
        check("confirm".equals(request.getAction())||"retire".equals(request.getAction()),"评审操作不合法");
        HrmPayrollEmployeeMappingDO selected=require(request.getId(),false);mapper.root(selected.getIdentityKey(),tenant(),true);
        HrmPayrollEmployeeMappingDO before=require(request.getId(),true);revision(before,request.getRevision());
        boolean confirm="confirm".equals(request.getAction());
        if(confirm) {
            check(before.getStatus()==0,"只有草稿可确认");validate(BeanUtils.toBean(before,HrmPayrollMappingSaveReqVO.class),true);
            HrmEmployeeDO person=access.employee(before.getEmployeeId(),true);
            if(!Objects.equals(before.getEmployeeFingerprint(),fingerprint(person))) throw exception(PAYROLL_MAPPING_PERSON_CHANGED);
            LambdaQueryWrapperX<HrmPayrollEmployeeMappingDO> q=query().eq(HrmPayrollEmployeeMappingDO::getIdentityKey,before.getIdentityKey())
                    .eq(HrmPayrollEmployeeMappingDO::getStatus,1);
            if(before.getEffectiveTo()!=null)q.le(HrmPayrollEmployeeMappingDO::getEffectiveFrom,before.getEffectiveTo());
            q.and(w->w.isNull(HrmPayrollEmployeeMappingDO::getEffectiveTo).or().ge(HrmPayrollEmployeeMappingDO::getEffectiveTo,before.getEffectiveFrom()));
            if(mapper.selectCount(q)>0)throw exception(PAYROLL_MAPPING_OVERLAP);
        } else check(before.getStatus()==1,"只有已确认映射可停用");
        HrmPayrollEmployeeMappingDO after=BeanUtils.toBean(before,HrmPayrollEmployeeMappingDO.class).setStatus(confirm?1:2)
                .setRevision(before.getRevision()+1).setReviewedBy(getLoginUserId()).setReviewedByName(actorName())
                .setReviewedTime(LocalDateTime.now()).setEvidence(request.getEvidence().trim());
        mapper.updateById(after);audit(request.getAction(),before,after,request.getEvidence());
    }
    private HrmPayrollEmployeeMappingDO editable(HrmPayrollMappingSaveReqVO request) {
        HrmEmployeeDO person=access.employee(request.getEmployeeId(),true);
        if(request.getEmployeeFingerprint()!=null&&!request.getEmployeeFingerprint().equals(fingerprint(person)))throw exception(PAYROLL_MAPPING_PERSON_CHANGED);
        HrmPayrollEmployeeMappingDO row=new HrmPayrollEmployeeMappingDO().setId(request.getId()).setSourceId(request.getSourceId())
                .setNamespace(request.getNamespace()).setExternalCode(request.getExternalCode()).setEmployeeId(person.getId())
                .setOwnerName(StrUtil.trimToNull(request.getOwnerName())).setReference(StrUtil.trimToNull(request.getReference()))
                .setEffectiveFrom(request.getEffectiveFrom()).setEffectiveTo(request.getEffectiveTo());
        row.setTenantId(tenant());capture(row,person);return row;
    }
    private void capture(HrmPayrollEmployeeMappingDO row,HrmEmployeeDO person) {
        row.setSnapshotName(person.getName()).setSnapshotJobNumber(person.getJobNumber()).setSnapshotDeptId(person.getDeptId())
                .setSnapshotUserId(person.getUserId()).setSnapshotEntryTime(person.getEntryTime()).setSnapshotLeaveTime(person.getLeaveTime())
                .setSnapshotEmployeeStatus(person.getStatus()).setSnapshotCapturedAt(LocalDateTime.now()).setEmployeeFingerprint(fingerprint(person));
    }
    private String fingerprint(HrmEmployeeDO person) {
        return digest(Arrays.asList(person.getId(),person.getName(),person.getJobNumber(),person.getDeptId(),person.getUserId(),
                person.getEntryTime(),person.getLeaveTime(),person.getStatus()));
    }
    private String digest(Object value) {
        try { byte[] hash=MessageDigest.getInstance("SHA-256").digest(JsonUtils.toJsonString(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();for(byte b:hash)out.append(String.format("%02x",b&255));return out.toString();
        } catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private void validate(HrmPayrollMappingSaveReqVO request,boolean confirm) {
        check(request!=null,"请求不能为空");check(request.getNamespace()!=null&&request.getNamespace().matches("[A-Z][A-Z0-9_-]{0,63}"),"命名空间须为稳定英文编号");
        request.setExternalCode(StrUtil.trimToNull(request.getExternalCode()));check(validCode(request.getExternalCode()),"外部编号不能为空、超过 128 字符或包含控制字符");
        date(request.getEffectiveFrom());date(request.getEffectiveTo());
        check(request.getEffectiveTo()==null||(request.getEffectiveFrom()!=null&&!request.getEffectiveFrom().isAfter(request.getEffectiveTo())),"有效期结束不能早于开始，且须先填写开始日期");
        if(confirm) { check(StrUtil.isNotBlank(request.getOwnerName()),"确认必须填写映射负责人");check(StrUtil.isNotBlank(request.getReference()),"确认必须填写来源依据");check(request.getEffectiveFrom()!=null,"确认必须填写有效期开始日期"); }
    }
    private boolean validCode(String code) { return code!=null&&!code.isEmpty()&&code.length()<=128&&!code.matches("(?s).*\\p{Cntrl}.*"); }
    private void date(LocalDate date) { check(date==null||(date.getYear()>=1000&&date.getYear()<=9999),"日期超过数据库支持范围"); }
    private void revision(HrmPayrollEmployeeMappingDO row,Integer expected) { if(!Objects.equals(row.getRevision(),expected))throw exception(PAYROLL_MAPPING_STALE); }
    private void check(boolean valid,String message) { if(!valid)throw exception(PAYROLL_MAPPING_INVALID,message); }
    private HrmPayrollEmployeeMappingDO require(Long id,boolean lock) {
        LambdaQueryWrapperX<HrmPayrollEmployeeMappingDO> q=query().eq(HrmPayrollEmployeeMappingDO::getId,id);if(lock)q.last("FOR UPDATE");
        HrmPayrollEmployeeMappingDO row=id==null?null:mapper.selectOne(q);if(row==null)throw exception(PAYROLL_MAPPING_NOT_EXISTS);access.require(row);return row;
    }
    @Override public HrmPayrollMappingRespVO get(Long id) { return BeanUtils.toBean(require(id,false),HrmPayrollMappingRespVO.class); }
    @Override public HrmPayrollMappingRespVO employee(Long id) {
        HrmEmployeeDO person=access.employee(id,false);HrmPayrollEmployeeMappingDO row=new HrmPayrollEmployeeMappingDO().setEmployeeId(person.getId());capture(row,person);
        return BeanUtils.toBean(row,HrmPayrollMappingRespVO.class);
    }
    @Override public PageResult<HrmPayrollMappingRespVO> page(HrmPayrollMappingPageReqVO request) {
        return BeanUtils.toBean(mapper.selectPage(request,access.filter(query()).eqIfPresent(HrmPayrollEmployeeMappingDO::getSourceId,request.getSourceId())
                .eqIfPresent(HrmPayrollEmployeeMappingDO::getNamespace,request.getNamespace()).eqIfPresent(HrmPayrollEmployeeMappingDO::getExternalCode,StrUtil.trimToNull(request.getExternalCode()))
                .eqIfPresent(HrmPayrollEmployeeMappingDO::getStatus,request.getStatus()).orderByDesc(HrmPayrollEmployeeMappingDO::getId)),HrmPayrollMappingRespVO.class);
    }
    @Override public List<HrmPayrollReviewDO> history(Long id) {
        require(id,false);boolean full=Boolean.TRUE.equals(access.scope().getAll());
        List<HrmPayrollReviewDO> rows=reviewMapper.selectList(new LambdaQueryWrapperX<HrmPayrollReviewDO>().eq(HrmPayrollReviewDO::getTenantId,tenant())
                .eq(HrmPayrollReviewDO::getObjectType,"employee-mapping").eq(HrmPayrollReviewDO::getObjectId,id).orderByDesc(HrmPayrollReviewDO::getId));
        if(!full)rows.forEach(row->row.setBeforeSnapshot(null).setAfterSnapshot(null).setReason(null));return rows;
    }
    @Override
    @Transactional(readOnly=true)
    public List<Match> resolve(Long sourceId,String namespace,List<HrmPayrollMappingLookupVO> lookups) {
        if(!permissionApi.hasAnyPermissions(getLoginUserId(),"hrm:payroll:identity:query"))throw exception(PAYROLL_MAPPING_PERMISSION);
        check(namespace!=null&&namespace.matches("[A-Z][A-Z0-9_-]{0,63}"),"命名空间不合法");check(lookups!=null&&lookups.size()<=500,"单次最多核对 500 个编号");
        access.scope();if(lookups.isEmpty())return Collections.emptyList();
        for(HrmPayrollMappingLookupVO lookup:lookups) { check(lookup!=null&&lookup.getStart()!=null&&lookup.getEnd()!=null,"核对必须声明开始和结束日期");
            date(lookup.getStart());date(lookup.getEnd());check(!lookup.getStart().isAfter(lookup.getEnd()),"核对期间颠倒"); }
        Set<String> codes=lookups.stream().map(HrmPayrollMappingLookupVO::getExternalCode).filter(this::validCode).collect(Collectors.toSet());
        LocalDate min=lookups.stream().map(HrmPayrollMappingLookupVO::getStart).min(LocalDate::compareTo).get();
        LocalDate max=lookups.stream().map(HrmPayrollMappingLookupVO::getEnd).max(LocalDate::compareTo).get();
        LambdaQueryWrapperX<HrmPayrollEmployeeMappingDO> q=access.filter(query()).eq(HrmPayrollEmployeeMappingDO::getSourceId,sourceId)
                .eq(HrmPayrollEmployeeMappingDO::getNamespace,namespace).eq(HrmPayrollEmployeeMappingDO::getStatus,1);
        q.le(HrmPayrollEmployeeMappingDO::getEffectiveFrom,max);
        q.and(w->w.isNull(HrmPayrollEmployeeMappingDO::getEffectiveTo).or().ge(HrmPayrollEmployeeMappingDO::getEffectiveTo,min));
        List<HrmPayrollEmployeeMappingDO> rows=codes.isEmpty()?Collections.emptyList():mapper.selectList(q.in(HrmPayrollEmployeeMappingDO::getExternalCode,codes).last("LIMIT 5001"));
        check(rows.size()<=5000,"期间匹配版本过多，请缩小期间或拆分核对");
        Set<Long> ids=rows.stream().map(HrmPayrollEmployeeMappingDO::getEmployeeId).collect(Collectors.toSet());
        Set<Long> present=ids.isEmpty()?Collections.emptySet():employeeMapper.selectList(new QueryWrapper<HrmEmployeeDO>().eq("tenant_id",tenant()).in("id",ids).select("id"))
                .stream().map(HrmEmployeeDO::getId).collect(Collectors.toSet());
        List<Match> results=new ArrayList<>();
        for(HrmPayrollMappingLookupVO lookup:lookups) {
            Match match=new Match().setExternalCode(lookup.getExternalCode());
            List<HrmPayrollEmployeeMappingDO> choices=rows.stream().filter(row->Objects.equals(row.getExternalCode(),lookup.getExternalCode())&&present.contains(row.getEmployeeId())
                    &&!row.getEffectiveFrom().isAfter(lookup.getStart())&&(row.getEffectiveTo()==null||!row.getEffectiveTo().isBefore(lookup.getEnd()))).collect(Collectors.toList());
            if(choices.size()==1) { HrmPayrollEmployeeMappingDO row=choices.get(0);match.setMappingId(row.getId()).setMappingVersion(row.getMappingVersion()).setEmployeeId(row.getEmployeeId()).setSnapshotCapturedAt(row.getSnapshotCapturedAt()); }
            else match.setIssueCode(choices.size()>1?"AMBIGUOUS_MAPPING":"UNRESOLVED_MAPPING")
                    .setMessage(choices.size()>1?"期间内有多个已确认映射，请核实版本冲突":"期间内未找到唯一、已确认且有权查看的编号映射；不会自动拼接版本或回退 HRM 工号");
            results.add(match);
        }
        return results;
    }
    private String actorName() { return Optional.ofNullable(adminUserApi.getUser(getLoginUserId())).map(u->u.getNickname()).orElse("当前登录人员"); }
    private void audit(String action,HrmPayrollEmployeeMappingDO before,HrmPayrollEmployeeMappingDO after,String reason) {
        HrmPayrollReviewDO row=new HrmPayrollReviewDO().setObjectType("employee-mapping").setObjectId(after.getId()).setAction(action)
                .setFromVersion(before==null?null:before.getRevision()).setToVersion(after.getRevision()).setActorId(getLoginUserId())
                .setActorName(actorName()).setReason(reason).setBeforeSnapshot(before==null?null:JsonUtils.toJsonString(before)).setAfterSnapshot(JsonUtils.toJsonString(after));
        row.setTenantId(tenant());reviewMapper.insert(row);
    }
}
