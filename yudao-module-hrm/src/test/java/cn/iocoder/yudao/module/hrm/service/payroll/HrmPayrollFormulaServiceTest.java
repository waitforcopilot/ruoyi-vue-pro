package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollFormulaDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.config.*;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollFormulaMapper;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import javax.annotation.Resource;
import java.util.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;

@Import(HrmPayrollFormulaService.class)
class HrmPayrollFormulaServiceTest extends BaseDbUnitTest {
    @Resource private HrmPayrollFormulaService service;
    @Resource private HrmPayrollFormulaMapper mapper;
    @Resource private HrmSalaryOptionMapper optionMapper;
    @Resource private HrmSalaryGroupMapper groupMapper;
    private final LocalDate start=LocalDate.of(2026,10,1),end=LocalDate.of(2026,12,31);
    @BeforeEach void inputs(){for(int code:new int[]{1000000001,1000000002,1000000003})optionMapper.insert(HrmSalaryOptionDO.builder()
            .code(code).name("工资项"+code).parentCode(0).enabled(true).build());}
    private Long create(int code,String expression,Long group){return service.create(code,expression,start,end,"薪酬政策附件",group);}
    private void approve(Long id){HrmPayrollFormulaDO rule=mapper.selectById(id);rule.setCreator("1");mapper.updateById(rule);service.approve(id,2L);}
    @Test void syntaxValidationDoesNotUseInventedDenominators(){
        Long id=create(1000000002,"item_1000000001 / (need_work_days - actual_work_days)",null);
        approve(id);
        Map<String,BigDecimal> inputs=new HashMap<>();inputs.put("item_1000000001",new BigDecimal("1000"));inputs.put("need_work_days",new BigDecimal("22"));inputs.put("actual_work_days",new BigDecimal("20"));
        assertEquals(0,new BigDecimal("500").compareTo(service.preview(id,start,null,inputs).get("item_1000000002")));
        inputs.put("actual_work_days",new BigDecimal("22"));assertServiceException(()->service.preview(id,start,null,inputs),PAYROLL_FORMULA_INVALID);
    }
    @Test void approvalIsSeparateAndEffectiveVersionsCannotOverlap(){
        Long id=create(1000000002,"item_1000000001*2",null);HrmPayrollFormulaDO rule=mapper.selectById(id);rule.setCreator("1");mapper.updateById(rule);
        assertServiceException(()->service.approve(id,1L),PAYROLL_SELF_APPROVAL);service.approve(id,2L);
        Long duplicate=create(1000000002,"item_1000000001*3",null);assertServiceException(()->service.approve(duplicate,2L),PAYROLL_FORMULA_INVALID);
        assertTrue(service.effective(start.minusDays(1)).isEmpty());assertEquals(1,service.effective(end).size());assertTrue(service.effective(end.plusDays(1)).isEmpty());
    }
    @Test void cyclesAcrossOutputsAreRejectedAtApproval(){
        Long first=create(1000000002,"item_1000000003+1",null);approve(first);
        Long second=create(1000000003,"item_1000000002+1",null);
        assertServiceException(()->service.approve(second,2L),PAYROLL_FORMULA_INVALID);
        assertEquals("PENDING",mapper.selectById(second).getStatus());
    }
    @Test void salaryGroupOverrideDoesNotAffectOtherGroups(){
        HrmSalaryGroupDO group=new HrmSalaryGroupDO().setName("生产组");groupMapper.insert(group);
        Long global=create(1000000002,"item_1000000001*2",null);approve(global);
        Long scoped=create(1000000002,"item_1000000001*3",group.getId());approve(scoped);
        Map<String,BigDecimal> inputs=Collections.singletonMap("item_1000000001",BigDecimal.TEN);
        assertEquals(0,new BigDecimal("30").compareTo(service.preview(scoped,start,group.getId(),inputs).get("item_1000000002")));
        assertServiceException(()->service.preview(scoped,start,null,inputs),PAYROLL_FORMULA_INVALID);
        assertEquals(0,new BigDecimal("20").compareTo(service.preview(global,start,null,inputs).get("item_1000000002")));
    }
    @Test void calculationUsesPeriodStartAndGroupOverride() {
        HrmSalaryGroupDO group=new HrmSalaryGroupDO().setName("测试组");groupMapper.insert(group);
        Long global=create(1000000002,"item_1000000001*2",null);approve(global);
        Long scoped=create(1000000002,"item_1000000001*3",group.getId());approve(scoped);
        java.util.List<cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.option.HrmSalaryOptionValueVO> input=Collections.singletonList(
                new cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.option.HrmSalaryOptionValueVO().setCode(1000000001).setValue(BigDecimal.TEN));
        Map<Integer,HrmSalaryOptionDO> catalog=new HashMap<>();optionMapper.selectList().forEach(option->catalog.put(option.getCode(),option));
        assertEquals(1,service.calculate(input,catalog,start.minusDays(1),group.getId(),null,null).size());
        java.util.List<cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.option.HrmSalaryOptionValueVO> output=service.calculate(input,catalog,start,group.getId(),null,null);
        assertEquals(0,new BigDecimal("30").compareTo(output.stream().filter(value->value.getCode()==1000000002).findFirst().get().getValue()));
        output=service.calculate(input,catalog,start,null,null,null);
        assertEquals(0,new BigDecimal("20").compareTo(output.stream().filter(value->value.getCode()==1000000002).findFirst().get().getValue()));
        assertEquals(1,input.size());
    }
    @Test void unknownDisabledAndSelfReferencedVariablesAreRejected(){
        assertServiceException(()->create(1000000002,"item_1000000002+1",null),PAYROLL_FORMULA_INVALID);
        assertServiceException(()->create(1000000002,"unknown+1",null),PAYROLL_FORMULA_INVALID);
        HrmSalaryOptionDO option=optionMapper.selectOne(HrmSalaryOptionDO::getCode,1000000001);option.setEnabled(false);optionMapper.updateById(option);
        assertServiceException(()->create(1000000002,"item_1000000001+1",null),PAYROLL_FORMULA_INVALID);
    }
    @Test void approvedInsuranceAmountsCannotBeOverriddenByRulesButCanBeReferenced() {
        optionMapper.insert(HrmSalaryOptionDO.builder().code(100101).name("个人社保").parentCode(100).enabled(true).build());
        assertServiceException(()->create(100101,"item_1000000001*2",null),PAYROLL_FORMULA_INVALID);
        Long id=create(1000000002,"item_100101+10",null);approve(id);
        assertEquals(0,new BigDecimal("110").compareTo(service.preview(id,start,null,Collections.singletonMap("item_100101",new BigDecimal("100"))).get("item_1000000002")));
        HrmPayrollFormulaDO legacy=new HrmPayrollFormulaDO();legacy.setOptionCode(100101);legacy.setExpression("item_1000000001*2");legacy.setEffectiveFrom(start);legacy.setEffectiveTo(end);legacy.setStatus("APPROVED");legacy.setEvidence("历史规则依据");mapper.insert(legacy);
        assertServiceException(()->service.calculate(Collections.emptyList(),Collections.emptyMap(),start,null,null,null),PAYROLL_FORMULA_INVALID);
    }

    @Test void customStatutoryCategoryCannotBypassSourceControl(){
        optionMapper.insert(HrmSalaryOptionDO.builder().code(4001000).name("自定义个人社保").parentCode(100).enabled(true).build());
        assertServiceException(()->create(4001000,"item_1000000001*2",null),PAYROLL_FORMULA_INVALID);
        Long id=create(1000000002,"item_4001000+10",null);approve(id);
        assertEquals(0,new BigDecimal("110").compareTo(service.preview(id,start,null,Collections.singletonMap("item_4001000",new BigDecimal("100"))).get("item_1000000002")));
    }

}
