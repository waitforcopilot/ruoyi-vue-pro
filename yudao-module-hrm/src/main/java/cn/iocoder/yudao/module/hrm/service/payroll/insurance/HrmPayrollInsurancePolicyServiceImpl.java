package cn.iocoder.yudao.module.hrm.service.payroll.insurance;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.ip.core.enums.AreaTypeEnum;
import cn.iocoder.yudao.framework.ip.core.utils.AreaUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.insurance.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.scheme.HrmPayrollSchemeCompareVO.Change;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollReviewDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.insurance.HrmPayrollInsurancePolicyDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollReviewMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.insurance.HrmPayrollInsurancePolicyMapper;
import cn.iocoder.yudao.module.hrm.enums.insurance.config.HrmInsuranceProjectTypeEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import javax.validation.Validator;
import java.math.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

@Service
public class HrmPayrollInsurancePolicyServiceImpl implements HrmPayrollInsurancePolicyService {
    @Resource private HrmPayrollInsurancePolicyMapper mapper;
    @Resource private HrmPayrollReviewMapper reviewMapper;
    @Resource private AdminUserApi adminUserApi;
    @Resource private Validator validator;

    private LambdaQueryWrapperX<HrmPayrollInsurancePolicyDO> query() {
        return new LambdaQueryWrapperX<HrmPayrollInsurancePolicyDO>()
                .eq(HrmPayrollInsurancePolicyDO::getTenantId, TenantContextHolder.getRequiredTenantId());
    }
    private void check(boolean valid, String reason) {
        if (!valid) throw exception(PAYROLL_INSURANCE_POLICY_INVALID, reason);
    }
    private void validateBean(Object value) {
        check(value != null, "请求资料缺失");
        String issues = validator.validate(value).stream().map(v -> v.getPropertyPath() + "：" + v.getMessage())
                .sorted().collect(Collectors.joining("；"));
        check(issues.isEmpty(), issues);
    }
    private void date(LocalDate value) {
        check(value == null || value.getYear() >= 1000 && value.getYear() <= 9999, "日期超过数据库支持范围");
    }
    private void period(LocalDate start, LocalDate end) {
        date(start); date(end);
        check(start != null && end != null && !start.isAfter(end), "请声明合法核对期间");
    }
    private HrmPayrollInsurancePolicyDO require(Long id, boolean lock) {
        LambdaQueryWrapperX<HrmPayrollInsurancePolicyDO> q = query().eq(HrmPayrollInsurancePolicyDO::getId, id);
        if (lock) q.last("FOR UPDATE");
        HrmPayrollInsurancePolicyDO row = id == null ? null : mapper.selectOne(q);
        if (row == null) throw exception(PAYROLL_INSURANCE_POLICY_NOT_EXISTS);
        return row;
    }
    private void lockSeries(HrmPayrollInsurancePolicyDO row) {
        // Every mutation locks the preserved first version, before locking a selected version.
        HrmPayrollInsurancePolicyDO first = mapper.selectOne(query()
                .eq(HrmPayrollInsurancePolicyDO::getIdentityKey, row.getIdentityKey())
                .eq(HrmPayrollInsurancePolicyDO::getPolicyVersion, 1).last("FOR UPDATE"));
        check(first != null, "政策首版本缺失，无法继续维护");
    }
    private void revision(HrmPayrollInsurancePolicyDO row, Integer expected) {
        if (!Objects.equals(row.getRevision(), expected)) throw exception(PAYROLL_INSURANCE_POLICY_STALE);
    }
    private String identity(Integer city, String scope, Integer type, String project) {
        try {
            String tuple = JsonUtils.toJsonString(Arrays.asList(city, scope, type, project));
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(tuple.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format("%02x", b & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private BigDecimal currency(BigDecimal value) { return value == null ? null : value.setScale(2); }
    private BigDecimal percent(BigDecimal value) { return value == null ? null : value.setScale(4); }
    private HrmPayrollInsuranceConfigVO normalize(HrmPayrollInsuranceConfigVO value) {
        HrmPayrollInsuranceConfigVO result = value == null ? new HrmPayrollInsuranceConfigVO()
                : BeanUtils.toBean(value, HrmPayrollInsuranceConfigVO.class);
        result.setLowerBase(currency(result.getLowerBase())).setUpperBase(currency(result.getUpperBase()))
                .setCorporateFixedAmount(currency(result.getCorporateFixedAmount()))
                .setPersonalFixedAmount(currency(result.getPersonalFixedAmount()))
                .setCorporateRatePercent(percent(result.getCorporateRatePercent()))
                .setPersonalRatePercent(percent(result.getPersonalRatePercent()));
        return result;
    }
    private void contribution(String side, String mode, BigDecimal rate, BigDecimal fixed, boolean confirm) {
        if (mode == null) { check(!confirm, side + "缴费方式缺失"); return; }
        boolean needsRate = !"FIXED".equals(mode), needsFixed = !"RATE".equals(mode);
        check(needsRate || rate == null, side + "固定方式不能同时填写比例");
        check(needsFixed || fixed == null, side + "比例方式不能同时填写固定额");
        if (confirm) {
            check(!needsRate || rate != null, side + "缴费比例缺失，零值须明确填写");
            check(!needsFixed || fixed != null, side + "固定缴费额缺失，零值须明确填写");
        }
    }
    private void config(HrmPayrollInsuranceConfigVO config, boolean confirm) {
        validateBean(config);
        if (config.getLowerBase() != null && config.getUpperBase() != null)
            check(config.getLowerBase().compareTo(config.getUpperBase()) <= 0, "基数上限不能小于下限");
        contribution("单位", config.getCorporateMode(), config.getCorporateRatePercent(), config.getCorporateFixedAmount(), confirm);
        contribution("个人", config.getPersonalMode(), config.getPersonalRatePercent(), config.getPersonalFixedAmount(), confirm);
        if (confirm) {
            check(config.getLowerBase() != null && config.getUpperBase() != null, "基数上下限须明确填写");
            check(config.getBaseUnit() != null, "基数单位及周期须明确填写");
            check(config.getAmountScale() != null && config.getRoundingMode() != null && config.getRoundingStage() != null,
                    "金额精度、舍入方式及舍入步骤须明确填写");
        }
    }
    private HrmPayrollInsurancePolicyDO editable(HrmPayrollInsuranceSaveReqVO req) {
        validateBean(req);
        Integer city = AreaUtils.getParentIdByType(req.getCityAreaId(), AreaTypeEnum.CITY);
        check(city != null, "参保地区必须归属到有效城市");
        HrmInsuranceProjectTypeEnum project = HrmInsuranceProjectTypeEnum.valueOf(req.getProjectType());
        check(project != null, "缴费项目类型不合法");
        check(!project.isCustom() || StrUtil.isNotBlank(req.getCustomProjectCode()), "自定义项目必须填写稳定项目编号");
        check(project.isCustom() || req.getCustomProjectCode() == null, "标准项目不填写自定义编号");
        String projectCode = project.isCustom() ? req.getCustomProjectCode() : "STANDARD-" + project.getType();
        date(req.getEffectiveFrom()); date(req.getEffectiveTo());
        check(req.getEffectiveTo() == null || req.getEffectiveFrom() != null && !req.getEffectiveFrom().isAfter(req.getEffectiveTo()),
                "有效期结束不能早于开始，且须先填写开始日期");
        String url = StrUtil.trimToNull(req.getSourceUrl());
        if (url != null) {
            try {
                URI parsed = new URI(url);
                check(("https".equalsIgnoreCase(parsed.getScheme()) || "http".equalsIgnoreCase(parsed.getScheme()))
                        && StrUtil.isNotBlank(parsed.getHost()) && parsed.getUserInfo() == null, "政策链接须为有效 HTTP(S) 地址且不含登录信息");
            } catch (java.net.URISyntaxException e) { throw exception(PAYROLL_INSURANCE_POLICY_INVALID, "政策链接格式不合法"); }
        }
        HrmPayrollInsuranceConfigVO config = normalize(req.getConfig());
        config(config, false);
        HrmPayrollInsurancePolicyDO row = new HrmPayrollInsurancePolicyDO()
                .setId(req.getId()).setCityAreaId(city).setCityName(AreaUtils.getArea(city).getName())
                .setScopeCode(req.getScopeCode()).setScopeName(StrUtil.trimToNull(req.getScopeName()))
                .setProjectType(project.getType()).setProjectCode(projectCode).setProjectName(project.getName())
                .setIdentityKey(identity(city, req.getScopeCode(), project.getType(), projectCode))
                .setTitle(req.getTitle().trim()).setOwnerName(StrUtil.trimToNull(req.getOwnerName()))
                .setReference(StrUtil.trimToNull(req.getReference())).setSourceUrl(url)
                .setEffectiveFrom(req.getEffectiveFrom()).setEffectiveTo(req.getEffectiveTo())
                .setConfigSchemaVersion(1).setConfigJson(JsonUtils.toJsonString(config));
        row.setTenantId(TenantContextHolder.getRequiredTenantId());
        return row;
    }
    private HrmPayrollInsuranceConfigVO config(HrmPayrollInsurancePolicyDO row) {
        check(Objects.equals(row.getConfigSchemaVersion(), 1), "当前政策结构版本不支持核对");
        return JsonUtils.parseObject(row.getConfigJson(), HrmPayrollInsuranceConfigVO.class);
    }
    private HrmPayrollInsuranceRespVO response(HrmPayrollInsurancePolicyDO row, boolean detail) {
        HrmPayrollInsuranceRespVO result = BeanUtils.toBean(row, HrmPayrollInsuranceRespVO.class);
        if (HrmInsuranceProjectTypeEnum.valueOf(row.getProjectType()).isCustom()) result.setCustomProjectCode(row.getProjectCode());
        if (detail) result.setConfig(config(row));
        return result;
    }
    @Override @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long create(HrmPayrollInsuranceSaveReqVO req) {
        HrmPayrollInsurancePolicyDO row = editable(req).setId(null).setPolicyVersion(1).setRevision(1).setStatus(0);
        if (mapper.selectCount(query().eq(HrmPayrollInsurancePolicyDO::getIdentityKey, row.getIdentityKey())) > 0)
            throw exception(PAYROLL_INSURANCE_POLICY_DUPLICATE);
        try { mapper.insert(row); } catch (DuplicateKeyException e) { throw exception(PAYROLL_INSURANCE_POLICY_DUPLICATE); }
        audit("create", null, row, "登记本地政策草稿，尚未确认");
        return row.getId();
    }
    @Override @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void update(HrmPayrollInsuranceSaveReqVO req) {
        HrmPayrollInsurancePolicyDO before = require(req.getId(), false);
        lockSeries(before); before = require(req.getId(), true); revision(before, req.getRevision());
        if (before.getStatus() != 0) throw exception(PAYROLL_INSURANCE_POLICY_IMMUTABLE);
        HrmPayrollInsurancePolicyDO after = editable(req);
        check(before.getIdentityKey().equals(after.getIdentityKey()), "城市、范围编号及项目固定，不能修改");
        after.setPolicyVersion(before.getPolicyVersion()).setRevision(before.getRevision() + 1).setStatus(0);
        mapper.updateById(after); audit("update", before, after, "维护本地政策草稿");
    }
    @Override @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long newVersion(Long id, Integer expected) {
        HrmPayrollInsurancePolicyDO selected = require(id, false);
        lockSeries(selected); selected = require(id, true); revision(selected, expected);
        HrmPayrollInsurancePolicyDO latest = mapper.selectOne(query()
                .eq(HrmPayrollInsurancePolicyDO::getIdentityKey, selected.getIdentityKey())
                .orderByDesc(HrmPayrollInsurancePolicyDO::getPolicyVersion).last("LIMIT 1"));
        HrmPayrollInsurancePolicyDO row = BeanUtils.toBean(selected, HrmPayrollInsurancePolicyDO.class)
                .setId(null).setPolicyVersion(latest.getPolicyVersion() + 1).setRevision(1).setStatus(0)
                .setReviewedBy(null).setReviewedByName(null).setReviewedTime(null).setEvidence(null);
        row.clean(); // The new row owns new creator/update timestamps, rather than copying history metadata.
        mapper.insert(row); audit("new-version", null, row, "从 V" + selected.getPolicyVersion() + " 复制参数，独立评审");
        return row.getId();
    }
    @Override @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void review(HrmPayrollInsuranceReviewReqVO req) {
        validateBean(req);
        HrmPayrollInsurancePolicyDO before = require(req.getId(), false);
        lockSeries(before); before = require(req.getId(), true); revision(before, req.getRevision());
        boolean confirm = "confirm".equals(req.getAction());
        if (confirm) {
            check(before.getStatus() == 0, "只有草稿可确认");
            check(StrUtil.isNotBlank(before.getScopeName()), "确认必须说明适用范围");
            check(StrUtil.isNotBlank(before.getOwnerName()), "确认必须填写负责人");
            check(StrUtil.isNotBlank(before.getReference()), "确认必须填写政策出处及依据");
            check(before.getEffectiveFrom() != null, "确认必须填写有效期开始日期");
            config(config(before), true);
            LambdaQueryWrapperX<HrmPayrollInsurancePolicyDO> q = query()
                    .eq(HrmPayrollInsurancePolicyDO::getIdentityKey, before.getIdentityKey())
                    .eq(HrmPayrollInsurancePolicyDO::getStatus, 1);
            if (before.getEffectiveTo() != null) q.le(HrmPayrollInsurancePolicyDO::getEffectiveFrom, before.getEffectiveTo());
            LocalDate start = before.getEffectiveFrom();
            q.and(w -> w.isNull(HrmPayrollInsurancePolicyDO::getEffectiveTo).or().ge(HrmPayrollInsurancePolicyDO::getEffectiveTo, start));
            if (mapper.selectCount(q) > 0) throw exception(PAYROLL_INSURANCE_POLICY_OVERLAP);
        } else check(before.getStatus() == 1, "只有已确认政策可停用");
        HrmPayrollInsurancePolicyDO after = BeanUtils.toBean(before, HrmPayrollInsurancePolicyDO.class)
                .setStatus(confirm ? 1 : 2).setRevision(before.getRevision() + 1)
                .setReviewedBy(getLoginUserId()).setReviewedByName(actorName()).setReviewedTime(LocalDateTime.now())
                .setEvidence(req.getEvidence().trim());
        mapper.updateById(after); audit(req.getAction(), before, after, req.getEvidence());
    }
    @Override public HrmPayrollInsuranceRespVO get(Long id) { return response(require(id, false), true); }
    @Override public PageResult<HrmPayrollInsuranceRespVO> page(HrmPayrollInsurancePageReqVO req) {
        validateBean(req);
        LambdaQueryWrapperX<HrmPayrollInsurancePolicyDO> q = query()
                .eqIfPresent(HrmPayrollInsurancePolicyDO::getCityAreaId, req.getCityAreaId())
                .eqIfPresent(HrmPayrollInsurancePolicyDO::getScopeCode, req.getScopeCode())
                .eqIfPresent(HrmPayrollInsurancePolicyDO::getProjectType, req.getProjectType())
                .eqIfPresent(HrmPayrollInsurancePolicyDO::getStatus, req.getStatus())
                .likeIfPresent(HrmPayrollInsurancePolicyDO::getTitle, req.getSearch()).orderByDesc(HrmPayrollInsurancePolicyDO::getId);
        q.select(HrmPayrollInsurancePolicyDO.class, f -> !Arrays.asList("configJson", "reference", "evidence").contains(f.getProperty()));
        PageResult<HrmPayrollInsurancePolicyDO> rows = mapper.selectPage(req, q);
        return new PageResult<>(rows.getList().stream().map(r -> response(r, false)).collect(Collectors.toList()), rows.getTotal());
    }
    @Override public List<HrmPayrollReviewDO> history(Long id) {
        require(id, false);
        return reviewMapper.selectList(new LambdaQueryWrapperX<HrmPayrollReviewDO>()
                .eq(HrmPayrollReviewDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(HrmPayrollReviewDO::getObjectType, "insurance-policy").eq(HrmPayrollReviewDO::getObjectId, id)
                .orderByDesc(HrmPayrollReviewDO::getId));
    }
    private void covers(HrmPayrollInsurancePolicyDO row, LocalDate start, LocalDate end) {
        period(start, end);
        check(row.getStatus() == 1, "只有已确认政策可用于声明基数核对");
        check(row.getEffectiveFrom() != null && !row.getEffectiveFrom().isAfter(start)
                && (row.getEffectiveTo() == null || !row.getEffectiveTo().isBefore(end)), "所选政策版本不能完整覆盖声明期间");
    }
    @Override @Transactional(readOnly = true)
    public HrmPayrollInsuranceRespVO resolve(Long id, LocalDate start, LocalDate end) {
        period(start, end); HrmPayrollInsurancePolicyDO selected = require(id, false);
        LambdaQueryWrapperX<HrmPayrollInsurancePolicyDO> q = query()
                .eq(HrmPayrollInsurancePolicyDO::getIdentityKey, selected.getIdentityKey())
                .eq(HrmPayrollInsurancePolicyDO::getStatus, 1);
        q.le(HrmPayrollInsurancePolicyDO::getEffectiveFrom, start)
                .and(w -> w.isNull(HrmPayrollInsurancePolicyDO::getEffectiveTo).or().ge(HrmPayrollInsurancePolicyDO::getEffectiveTo, end));
        List<HrmPayrollInsurancePolicyDO> rows = mapper.selectList(q.last("LIMIT 2"));
        check(rows.size() == 1, "未找到单个完整覆盖期间的确认政策，不自动拼接版本");
        return response(rows.get(0), true);
    }
    private static class Contribution {
        private final BigDecimal raw, rounded;
        private final String steps;
        private Contribution(BigDecimal raw, BigDecimal rounded, String steps) {
            this.raw = raw; this.rounded = rounded; this.steps = steps;
        }
    }
    private String readable(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
    private Contribution amounts(String mode, BigDecimal base, BigDecimal rate, BigDecimal fixed, HrmPayrollInsuranceConfigVO config) {
        BigDecimal proportional = "FIXED".equals(mode) ? null : base.multiply(rate).movePointLeft(2);
        BigDecimal flat = "RATE".equals(mode) ? null : fixed;
        BigDecimal raw = proportional == null ? flat : flat == null ? proportional : proportional.add(flat);
        RoundingMode rounding = RoundingMode.valueOf(config.getRoundingMode());
        int scale = config.getAmountScale();
        BigDecimal rounded = raw.setScale(scale, rounding);
        String steps;
        if ("COMPONENT".equals(config.getRoundingStage()) && proportional != null && flat != null) {
            BigDecimal roundedRate = proportional.setScale(scale, rounding), roundedFixed = flat.setScale(scale, rounding);
            rounded = roundedRate.add(roundedFixed);
            steps = "比例部分 " + readable(proportional) + " → " + roundedRate.toPlainString()
                    + "；固定部分 " + readable(flat) + " → " + roundedFixed.toPlainString() + "；合计 " + rounded.toPlainString();
        } else if (proportional != null && flat != null) {
            steps = "比例部分 " + readable(proportional) + " + 固定部分 " + readable(flat) + " = " + readable(raw)
                    + "；合计舍入 → " + rounded.toPlainString();
        } else {
            steps = (proportional == null ? "固定部分 " : "比例部分 ") + readable(raw) + " → " + rounded.toPlainString();
        }
        return new Contribution(raw, rounded, steps);
    }
    private String expression(String mode, BigDecimal base, BigDecimal rate, BigDecimal fixed) {
        String proportional = base.toPlainString() + " × " + (rate == null ? "" : rate.toPlainString()) + "%";
        return "FIXED".equals(mode) ? fixed.toPlainString() : "RATE".equals(mode) ? proportional : proportional + " + " + fixed.toPlainString();
    }
    @Override @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HrmPayrollInsurancePreviewVO preview(HrmPayrollInsurancePreviewReqVO req) {
        validateBean(req); HrmPayrollInsurancePolicyDO row = require(req.getPolicyId(), false); covers(row, req.getStart(), req.getEnd());
        HrmPayrollInsuranceConfigVO config = config(row); config(config, true);
        BigDecimal base = currency(req.getBaseAmount());
        if (base.compareTo(config.getLowerBase()) < 0 || base.compareTo(config.getUpperBase()) > 0)
            throw exception(PAYROLL_INSURANCE_POLICY_BASE_OUTSIDE);
        Contribution corporate = amounts(config.getCorporateMode(), base, config.getCorporateRatePercent(), config.getCorporateFixedAmount(), config);
        Contribution personal = amounts(config.getPersonalMode(), base, config.getPersonalRatePercent(), config.getPersonalFixedAmount(), config);
        return new HrmPayrollInsurancePreviewVO().setPolicy(response(row, true)).setBaseAmount(base.toPlainString())
                .setCorporateRawAmount(corporate.raw.toPlainString()).setPersonalRawAmount(personal.raw.toPlainString())
                .setCorporateAmount(corporate.rounded.toPlainString()).setPersonalAmount(personal.rounded.toPlainString())
                .setCorporateSteps(corporate.steps).setPersonalSteps(personal.steps)
                .setCorporateExpression(expression(config.getCorporateMode(), base, config.getCorporateRatePercent(), config.getCorporateFixedAmount()))
                .setPersonalExpression(expression(config.getPersonalMode(), base, config.getPersonalRatePercent(), config.getPersonalFixedAmount()))
                .setExplanation("仅按所选确认政策核对一次声明基数，不按期间天数分摊；未校验人员参保资格，不生成月账或工资扣款。");
    }
    private Map<String, String> fields(HrmPayrollInsurancePolicyDO row) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("title", row.getTitle()); result.put("scopeName", row.getScopeName());
        result.put("reference", row.getReference()); result.put("sourceUrl", row.getSourceUrl());
        JsonNode node = JsonUtils.parseTree(JsonUtils.toJsonString(config(row)));
        node.fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().isNull() ? null : e.getValue().asText()));
        return result;
    }
    @Override @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HrmPayrollInsuranceCompareVO compare(Long leftId, Long rightId) {
        HrmPayrollInsurancePolicyDO left = require(leftId, false), right = require(rightId, false);
        check(left.getIdentityKey().equals(right.getIdentityKey()), "只能对比同城市、同范围及同项目的政策版本");
        Map<String, String> l = fields(left), r = fields(right);
        List<Change> changes = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>(l.keySet()); keys.addAll(r.keySet());
        for (String key : keys) if (!Objects.equals(l.get(key), r.get(key)) || l.containsKey(key) != r.containsKey(key))
            changes.add(new Change().setPath(key).setLabel(label(key)).setLeft(l.get(key)).setRight(r.get(key))
                    .setKind(!l.containsKey(key) ? "ADDED" : !r.containsKey(key) ? "REMOVED" : "CHANGED"));
        return new HrmPayrollInsuranceCompareVO().setLeft(response(left, true)).setRight(response(right, true)).setChanges(changes);
    }
    private String label(String key) {
        Map<String, String> labels = new HashMap<>();
        labels.put("title", "政策名称"); labels.put("scopeName", "适用范围说明"); labels.put("reference", "政策出处与依据"); labels.put("sourceUrl", "政策链接");
        labels.put("lowerBase", "基数下限（元）"); labels.put("upperBase", "基数上限（元）"); labels.put("baseUnit", "基数单位及周期");
        labels.put("corporateMode", "单位缴费方式"); labels.put("personalMode", "个人缴费方式");
        labels.put("corporateRatePercent", "单位比例（%）"); labels.put("personalRatePercent", "个人比例（%）");
        labels.put("corporateFixedAmount", "单位固定额（元）"); labels.put("personalFixedAmount", "个人固定额（元）");
        labels.put("amountScale", "金额小数位数"); labels.put("roundingMode", "舍入方式"); labels.put("roundingStage", "舍入步骤");
        return labels.getOrDefault(key, key);
    }
    private String actorName() {
        return Optional.ofNullable(adminUserApi.getUser(getLoginUserId())).map(u -> u.getNickname()).orElse("当前登录人员");
    }
    private void audit(String action, HrmPayrollInsurancePolicyDO before, HrmPayrollInsurancePolicyDO after, String reason) {
        HrmPayrollReviewDO review = new HrmPayrollReviewDO().setObjectType("insurance-policy").setObjectId(after.getId())
                .setAction(action).setFromVersion(before == null ? null : before.getRevision()).setToVersion(after.getRevision())
                .setActorId(getLoginUserId()).setActorName(actorName()).setReason(reason.trim())
                .setBeforeSnapshot(before == null ? null : JsonUtils.toJsonString(before)).setAfterSnapshot(JsonUtils.toJsonString(after));
        review.setTenantId(TenantContextHolder.getRequiredTenantId()); reviewMapper.insert(review);
    }
}
