package cn.iocoder.yudao.module.hrm.service.payroll;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollInputImportMapper;
import java.math.BigDecimal;
import java.util.*;
import javax.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrmPayrollInputImportService {
  @Resource private HrmPayrollInputImportMapper imports;
  @Resource private HrmPayrollInputService input;
  @Resource private HrmPayrollEntityService entities;

  @Data
  public static class Proposal {
    private HrmPayrollInputReqVO request;
    private HrmPayrollInputService.Prepared prepared;
  }

  private String s(String v) {
    return v == null ? "" : v.trim();
  }

  private static class Group {
    HrmPayrollInputReqVO request;
    String fingerprint;
    int firstRow;
    String job;
  }

  @Transactional(rollbackFor = Exception.class)
  public HrmPayrollInputImportDO preview(
      Long batchId, String fileName, String digest, List<HrmPayrollInputRowVO> rows, Long actor) {
    HrmSalaryMonthRecordDO b = input.batch(batchId, "MANAGE", actor, true);
    if (fileName == null
        || fileName.length() > 255
        || digest == null
        || !digest.matches("[a-f0-9]{64}")
        || rows == null
        || rows.isEmpty()
        || rows.size() > 50000) throw exception(PAYROLL_INPUT_INVALID);
    HrmPayrollInputImportDO previous =
        imports.selectOne(
            new LambdaQueryWrapperX<HrmPayrollInputImportDO>()
                .eq(HrmPayrollInputImportDO::getBatchId, batchId)
                .eq(HrmPayrollInputImportDO::getDigest, digest)
                .orderByDesc(HrmPayrollInputImportDO::getVersion)
                .last("LIMIT 1"));
    if (previous != null) return previous;
    return build(b, fileName, digest, rows, actor, 1);
  }

  private HrmPayrollInputImportDO build(
      HrmSalaryMonthRecordDO b,
      String fileName,
      String digest,
      List<HrmPayrollInputRowVO> rows,
      Long actor,
      int version) {
    Map<String, List<HrmEmployeeDO>> roster = new HashMap<>();
    for (HrmEmployeeDO employee : input.roster(b))
      roster
          .computeIfAbsent(
              s(employee.getJobNumber()).toUpperCase(Locale.ROOT), k -> new ArrayList<>())
          .add(employee);
    String code = entities.approved(b.getEntityId()).getCode();
    Map<String, Group> groups = new LinkedHashMap<>();
    List<HrmPayrollInsuranceImportService.RowError> errors = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      HrmPayrollInputRowVO row = rows.get(i);
      String job = s(row.getJobNumber());
      int line = i + 2;
      try {
        if (job.isEmpty()) throw new IllegalArgumentException("工号不能为空");
        List<HrmEmployeeDO> matches =
            roster.getOrDefault(job.toUpperCase(Locale.ROOT), Collections.emptyList());
        if (matches.size() != 1) throw new IllegalArgumentException("工号未唯一匹配本主体本期间的计薪员工");
        if (!code.equals(s(row.getEntityCode()))
            || !b.getStartTime().toLocalDate().toString().equals(s(row.getPeriodFrom()))
            || !b.getEndTime().toLocalDate().toString().equals(s(row.getPeriodTo())))
          throw new IllegalArgumentException("主体或期间与所选工资批次不一致");
        HrmPayrollInputReqVO r =
            new HrmPayrollInputReqVO()
                .setBatchId(b.getId())
                .setEmployeeId(matches.get(0).getId())
                .setKind(s(row.getKind()))
                .setMode(s(row.getMode()))
                .setEvidence(s(row.getEvidence()))
                .setValues(new LinkedHashMap<>());
        if (!HrmPayrollInputService.KINDS.contains(r.getKind())
            || !Arrays.asList("SYNC", "ZERO", "IMPORT").contains(r.getMode()))
          throw new IllegalArgumentException("数据类型或处理方式不合法");
        String fingerprint = JsonUtils.toJsonString(r), key = r.getEmployeeId() + ":" + r.getKind();
        Group g = groups.get(key);
        if (g == null) {
          g = new Group();
          g.request = r;
          g.fingerprint = fingerprint;
          g.firstRow = line;
          g.job = job;
          groups.put(key, g);
        } else if (!Objects.equals(g.fingerprint, fingerprint))
          throw new IllegalArgumentException("同员工同数据类型的处理方式及来源依据须一致");
        if ("IMPORT".equals(r.getMode())) {
          String field = s(row.getField());
          if (!input.fields(r.getKind()).contains(field))
            throw new IllegalArgumentException("字段编码不属于该数据类型");
          if (g.request.getValues().containsKey(field))
            throw new IllegalArgumentException("同员工同数据类型的字段重复");
          if (s(row.getValue()).isEmpty()) throw new IllegalArgumentException("数值不能为空，真实零值请明确填 0");
          BigDecimal value;
          try {
            value = new BigDecimal(s(row.getValue()));
          } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("数值须为元或天，最多两位小数");
          }
          g.request.getValues().put(field, value);
        } else if (!s(row.getField()).isEmpty() || !s(row.getValue()).isEmpty())
          throw new IllegalArgumentException("同步或确认零值行不填写导入字段及金额");
        else if (g.firstRow != line) throw new IllegalArgumentException("同员工同数据类型的同步或零值确认重复");
      } catch (IllegalArgumentException ex) {
        errors.add(new HrmPayrollInsuranceImportService.RowError(line, job, ex.getMessage()));
      }
    }
    List<Proposal> proposals = new ArrayList<>();
    for (Group g : groups.values())
      try {
        Proposal p = new Proposal();
        p.setRequest(g.request);
        p.setPrepared(input.inspect(g.request, actor));
        proposals.add(p);
      } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
        errors.add(
            new HrmPayrollInsuranceImportService.RowError(g.firstRow, g.job, ex.getMessage()));
      }
    HrmPayrollInputImportDO record = new HrmPayrollInputImportDO();
    record.setBatchId(b.getId());
    record.setEntityId(b.getEntityId());
    record.setVersion(version);
    record.setFileName(fileName);
    record.setDigest(digest);
    record.setRowsSnapshot(JsonUtils.toJsonString(rows));
    record.setErrors(JsonUtils.toJsonString(errors));
    record.setRequests(JsonUtils.toJsonString(proposals));
    record.setCreator(String.valueOf(actor));
    record.setStatus(errors.isEmpty() ? "VALIDATED" : "FAILED");
    imports.insert(record);
    return record;
  }

  public HrmPayrollInputImportDO get(Long id, Long actor) {
    HrmPayrollInputImportDO row = imports.selectById(id);
    if (row == null) throw exception(PAYROLL_INPUT_INVALID);
    input.batch(row.getBatchId(), "READ", actor, false);
    return row;
  }

  @Transactional(rollbackFor = Exception.class)
  public List<Long> commit(Long id, Long actor) {
    HrmPayrollInputImportDO row = imports.selectById(id);
    if (row == null) throw exception(PAYROLL_INPUT_INVALID);
    input.batch(row.getBatchId(), "MANAGE", actor, true);
    row = imports.selectOneForUpdate(HrmPayrollInputImportDO::getId, id);
    if (!Objects.equals(row.getCreator(), String.valueOf(actor)))
      throw exception(PAYROLL_INPUT_INVALID);
    if ("COMMITTED".equals(row.getStatus()))
      return JsonUtils.parseArray(row.getInputIds(), Long.class);
    if (!"VALIDATED".equals(row.getStatus())) throw exception(PAYROLL_INPUT_INVALID);
    List<Proposal> proposals = JsonUtils.parseArray(row.getRequests(), Proposal.class);
    List<Long> ids = new ArrayList<>();
    for (Proposal p : proposals) {
      HrmPayrollInputService.Prepared now = input.inspect(p.getRequest(), actor);
      if (!JsonUtils.parseTree(JsonUtils.toJsonString(p.getPrepared()))
          .equals(JsonUtils.parseTree(JsonUtils.toJsonString(now))))
        throw exception(PAYROLL_INPUT_INVALID);
      ids.add(input.submit(p.getRequest(), actor));
    }
    row.setInputIds(JsonUtils.toJsonString(ids));
    row.setStatus("COMMITTED");
    imports.updateById(row);
    return ids;
  }

  @Transactional(rollbackFor = Exception.class)
  public HrmPayrollInputImportDO revalidate(Long id, Long actor) {
    HrmPayrollInputImportDO prior = imports.selectById(id);
    if (prior == null) throw exception(PAYROLL_INPUT_INVALID);
    HrmSalaryMonthRecordDO b = input.batch(prior.getBatchId(), "MANAGE", actor, true);
    prior = imports.selectOneForUpdate(HrmPayrollInputImportDO::getId, id);
    if (!Objects.equals(prior.getCreator(), String.valueOf(actor))
        || !Arrays.asList("VALIDATED", "FAILED").contains(prior.getStatus()))
      throw exception(PAYROLL_INPUT_INVALID);
    HrmPayrollInputImportDO next =
        build(
            b,
            prior.getFileName(),
            prior.getDigest(),
            JsonUtils.parseArray(prior.getRowsSnapshot(), HrmPayrollInputRowVO.class),
            actor,
            prior.getVersion() + 1);
    prior.setStatus("SUPERSEDED");
    imports.updateById(prior);
    return next;
  }

  public List<HrmPayrollInputImportDO> list(Long batchId, Long actor) {
    input.batch(batchId, "READ", actor, false);
    List<HrmPayrollInputImportDO> rows =
        imports.selectList(
            new LambdaQueryWrapperX<HrmPayrollInputImportDO>()
                .eq(HrmPayrollInputImportDO::getBatchId, batchId)
                .orderByDesc(HrmPayrollInputImportDO::getId));
    for (HrmPayrollInputImportDO r : rows) {
      r.setRowsSnapshot(null);
      r.setRequests(null);
    }
    return rows;
  }

  public HrmPayrollInputImportDO getForExport(Long id, Long actor) {
    HrmPayrollInputImportDO r = imports.selectById(id);
    if (r == null) throw exception(PAYROLL_INPUT_INVALID);
    input.batch(r.getBatchId(), "EXPORT", actor, false);
    return r;
  }
}
