package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceBaseRowVO;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollInsuranceReqVO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollInsuranceImportDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollInsuranceImportMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrmPayrollInsuranceImportService {
  @Resource private HrmPayrollInsuranceImportMapper imports;
  @Resource private HrmEmployeeMapper employees;
  @Resource private HrmPayrollInsurancePolicyService policies;
  @Resource private HrmPayrollEntityService entities;

  @Data
  public static class RowError {
    @cn.idev.excel.annotation.ExcelProperty("文件行")
    private int row;

    @cn.idev.excel.annotation.ExcelProperty("工号")
    private String jobNumber;

    @cn.idev.excel.annotation.ExcelProperty("错误说明")
    private String reason;

    public RowError() {}

    public RowError(int row, String job, String reason) {
      this.row = row;
      this.jobNumber = job;
      this.reason = reason;
    }
  }

  private String s(String value) {
    return value == null ? "" : value.trim();
  }

  private static class Group {
    HrmPayrollInsuranceReqVO.Base request;
    String fingerprint;
    int firstRow;
    String job;
  }

  @Transactional(rollbackFor = Exception.class)
  public HrmPayrollInsuranceImportDO preview(
      Long entityId,
      String fileName,
      String digest,
      List<HrmPayrollInsuranceBaseRowVO> rows,
      Long actor) {
    entities.check(entityId, "MANAGE", actor);
    entities.lockApproved(entityId);
    if (rows == null
        || rows.isEmpty()
        || rows.size() > 50000
        || digest == null
        || !digest.matches("[a-f0-9]{64}")
        || fileName == null
        || fileName.length() > 255) throw exception(PAYROLL_INSURANCE_INVALID);
    HrmPayrollInsuranceImportDO existing =
        imports.selectOne(
            new LambdaQueryWrapperX<HrmPayrollInsuranceImportDO>()
                .eq(HrmPayrollInsuranceImportDO::getEntityId, entityId)
                .eq(HrmPayrollInsuranceImportDO::getDigest, digest));
    if (existing != null) return existing;
    List<RowError> errors = new ArrayList<>();
    Map<String, Group> groups = new LinkedHashMap<>();
    for (int index = 0; index < rows.size(); index++) {
      HrmPayrollInsuranceBaseRowVO row = rows.get(index);
      String job = s(row.getJobNumber());
      int line = index + 2;
      try {
        if (job.isEmpty()) throw new IllegalArgumentException("工号不能为空");
        List<HrmEmployeeDO> matching =
            employees.selectList(
                new LambdaQueryWrapperX<HrmEmployeeDO>().eq(HrmEmployeeDO::getJobNumber, job));
        if (matching.size() != 1)
          throw new IllegalArgumentException(matching.isEmpty() ? "工号未匹配到员工" : "工号匹配多个员工，请先修正员工档案");
        HrmEmployeeDO employee = matching.get(0);
        String coverage = s(row.getCoverage());
        if (!Arrays.asList("INSURED", "EXEMPT").contains(coverage))
          throw new IllegalArgumentException("缴费方式须为 INSURED 或 EXEMPT");
        YearMonth from, to;
        try {
          from = YearMonth.parse(s(row.getFromMonth()));
          to = YearMonth.parse(s(row.getToMonth()));
        } catch (RuntimeException ex) {
          throw new IllegalArgumentException("生效和截止月份须为 YYYY-MM");
        }
        Integer income = null;
        if (!s(row.getIncomeYear()).isEmpty()) {
          try {
            income = Integer.valueOf(s(row.getIncomeYear()));
          } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("收入依据年度须为四位整数");
          }
        }
        Long policyId = null;
        if ("INSURED".equals(coverage)) {
          if (s(row.getPolicyCode()).isEmpty())
            throw new IllegalArgumentException("正常缴费必须填写批准政策编码");
          policyId = policies.resolvePolicy(entityId, s(row.getPolicyCode()), from.atDay(1));
        } else if (!s(row.getPolicyCode()).isEmpty()
            || !s(row.getProjectCode()).isEmpty()
            || !s(row.getDeclaredBase()).isEmpty())
          throw new IllegalArgumentException("豁免行不填写政策、项目或申报基数");
        HrmPayrollInsuranceReqVO.Base request =
            new HrmPayrollInsuranceReqVO.Base()
                .setEntityId(entityId)
                .setEmployeeId(employee.getId())
                .setCoverage(coverage)
                .setPolicyId(policyId)
                .setIncomeYear(income)
                .setEffectiveFrom(from.atDay(1))
                .setEffectiveTo(to.atEndOfMonth())
                .setEvidence(s(row.getEvidence()))
                .setDeclaredBases(new LinkedHashMap<>());
        if (!s(row.getReplacesId()).isEmpty()) {
          try {
            request.setReplacesId(Long.valueOf(s(row.getReplacesId())));
          } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("替代申报编号须为整数");
          }
        }
        String fingerprint = JsonUtils.toJsonString(request);
        String groupKey = String.valueOf(employee.getId());
        Group group = groups.get(groupKey);
        if (group == null) {
          group = new Group();
          group.request = request;
          group.fingerprint = fingerprint;
          group.firstRow = line;
          group.job = job;
          groups.put(groupKey, group);
        } else if (!Objects.equals(group.fingerprint, fingerprint))
          throw new IllegalArgumentException("同工号的政策、方式、年度、生效期及依据须一致");
        if ("EXEMPT".equals(coverage)) {
          if (group.firstRow != line) throw new IllegalArgumentException("同工号的豁免行重复");
        } else {
          String project = s(row.getProjectCode());
          if (project.isEmpty() || s(row.getDeclaredBase()).isEmpty())
            throw new IllegalArgumentException("项目和申报基数不能为空，真实零值请明确填 0");
          if (group.request.getDeclaredBases().containsKey(project))
            throw new IllegalArgumentException("同工号同项目重复");
          BigDecimal amount;
          try {
            amount = new BigDecimal(s(row.getDeclaredBase()));
          } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("申报基数必须为元，最多两位小数");
          }
          group.request.getDeclaredBases().put(project, amount);
        }
      } catch (IllegalArgumentException ex) {
        errors.add(new RowError(line, job, ex.getMessage()));
      } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
        errors.add(new RowError(line, job, ex.getMessage()));
      }
    }
    List<HrmPayrollInsuranceReqVO.Base> requests = new ArrayList<>();
    for (Group group : groups.values())
      try {
        policies.inspectBase(group.request, actor);
        requests.add(group.request);
      } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
        errors.add(new RowError(group.firstRow, group.job, ex.getMessage()));
      }
    HrmPayrollInsuranceImportDO record = new HrmPayrollInsuranceImportDO();
    record.setEntityId(entityId);
    record.setFileName(fileName);
    record.setDigest(digest);
    record.setCreator(String.valueOf(actor));
    record.setRowsSnapshot(JsonUtils.toJsonString(rows));
    record.setErrors(JsonUtils.toJsonString(errors));
    record.setRequests(JsonUtils.toJsonString(requests));
    record.setStatus(errors.isEmpty() ? "VALIDATED" : "FAILED");
    imports.insert(record);
    return record;
  }

  public HrmPayrollInsuranceImportDO get(Long id, Long actor) {
    HrmPayrollInsuranceImportDO record = imports.selectById(id);
    if (record == null) throw exception(PAYROLL_INSURANCE_INVALID);
    entities.check(record.getEntityId(), "READ", actor);
    return record;
  }

  @Transactional(rollbackFor = Exception.class)
  public List<Long> commit(Long id, Long actor) {
    HrmPayrollInsuranceImportDO record = imports.selectById(id);
    if (record == null) throw exception(PAYROLL_INSURANCE_INVALID);
    entities.check(record.getEntityId(), "MANAGE", actor);
    entities.lockApproved(record.getEntityId());
    record = imports.selectOneForUpdate(HrmPayrollInsuranceImportDO::getId, id);
    if (!Objects.equals(record.getCreator(), String.valueOf(actor)))
      throw exception(PAYROLL_INSURANCE_INVALID);
    if ("COMMITTED".equals(record.getStatus()))
      return JsonUtils.parseArray(record.getProposalIds(), Long.class);
    if (!"VALIDATED".equals(record.getStatus())) throw exception(PAYROLL_INSURANCE_INVALID);
    List<Long> ids = new ArrayList<>();
    for (HrmPayrollInsuranceReqVO.Base request :
        JsonUtils.parseArray(record.getRequests(), HrmPayrollInsuranceReqVO.Base.class)) {
      policies.inspectBase(request, actor);
      ids.add(policies.proposeBase(request, actor));
    }
    record.setProposalIds(JsonUtils.toJsonString(ids));
    record.setStatus("COMMITTED");
    imports.updateById(record);
    return ids;
  }

  public List<HrmPayrollInsuranceImportDO> list(Long entityId, Long actor) {
    entities.check(entityId, "READ", actor);
    List<HrmPayrollInsuranceImportDO> records =
        imports.selectList(
            new LambdaQueryWrapperX<HrmPayrollInsuranceImportDO>()
                .eq(HrmPayrollInsuranceImportDO::getEntityId, entityId)
                .orderByDesc(HrmPayrollInsuranceImportDO::getId));
    for (HrmPayrollInsuranceImportDO r : records) {
      r.setRowsSnapshot(null);
      r.setRequests(null);
    }
    return records;
  }
}
