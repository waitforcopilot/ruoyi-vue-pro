package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollGovernanceRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollGovernanceRecordMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

@Service
public class HrmPayrollGovernanceStore {
    @Resource private HrmPayrollGovernanceRecordMapper records;
    public static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
    public static String now(){return LocalDateTime.now(ZONE).toString();}
    public static boolean blank(Object x){return x==null||String.valueOf(x).trim().isEmpty();}
    public static void need(boolean value){if(!value)throw exception(PAYROLL_REVIEW_INCOMPLETE);}
    public static void allowed(boolean value){if(!value)throw exception(PAYROLL_PROJECT_DENIED);}
    public static Long id(Object x){return x==null?null:Long.valueOf(String.valueOf(x));}
    public static Map<String,Object> map(Object... values){Map<String,Object> r=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)r.put((String)values[i],values[i+1]);return r;}
    public static String digest(Object x){try{byte[] h=MessageDigest.getInstance("SHA-256").digest(JsonUtils.toJsonString(x).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:h)s.append(String.format("%02x",b&255));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    public static String bodyDigest(String text){try{byte[] h=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:h)s.append(String.format("%02x",b&255));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    public Map<String,Object> data(HrmPayrollGovernanceRecordDO r){return JsonUtils.parseObject(r.getDataJson(),Map.class);}
    /** All governed writes acquire this tenant lock before any project/object lock. */
    public void lock(){need(records.selectOne(new LambdaQueryWrapperX<HrmPayrollGovernanceRecordDO>().eq(HrmPayrollGovernanceRecordDO::getKind,"LOCK").eq(HrmPayrollGovernanceRecordDO::getProjectId,0L).last("FOR UPDATE"))!=null);}
    public List<HrmPayrollGovernanceRecordDO> all(Long project,String kind){return records.selectList(new LambdaQueryWrapperX<HrmPayrollGovernanceRecordDO>().eqIfPresent(HrmPayrollGovernanceRecordDO::getProjectId,project).eq(HrmPayrollGovernanceRecordDO::getKind,kind).orderByAsc(HrmPayrollGovernanceRecordDO::getId));}
    public HrmPayrollGovernanceRecordDO record(Long id,String kind){HrmPayrollGovernanceRecordDO r=id==null?null:records.selectById(id);allowed(r!=null&&kind.equals(r.getKind()));return r;}
    public HrmPayrollGovernanceRecordDO insert(Long project,String kind,String key,Object data){HrmPayrollGovernanceRecordDO r=new HrmPayrollGovernanceRecordDO();r.setProjectId(project);r.setKind(kind);r.setObjectKey(key);r.setVersion(1);r.setDataJson(JsonUtils.toJsonString(data));records.insert(r);return r;}
    public void state(HrmPayrollGovernanceRecordDO r,Object data){r.setVersion(r.getVersion()+1);r.setDataJson(JsonUtils.toJsonString(data));records.updateById(r);}
    public void version(HrmPayrollGovernanceRecordDO r,Integer expected){if(!Objects.equals(r.getVersion(),expected))throw exception(PAYROLL_VERSION_CONFLICT);}
    public void event(HrmPayrollGovernanceRecordDO r,String action,Long actor,String reason){insert(r.getProjectId(),"EVENT",UUID.randomUUID().toString(),map("objectId",r.getId(),"kind",r.getKind(),"version",r.getVersion(),"action",action,"actorId",actor,"reason",reason,"time",now(),"snapshot",data(r)));}
    public List<Map<String,Object>> userFacts(){return records.lockUserFacts();}
    public static boolean within(Map<String,Object> d){try{LocalDate today=LocalDate.now(ZONE);return !today.isBefore(LocalDate.parse(String.valueOf(d.get("validFrom"))))&&!today.isAfter(LocalDate.parse(String.valueOf(d.get("validTo"))));}catch(RuntimeException e){return false;}}
}
