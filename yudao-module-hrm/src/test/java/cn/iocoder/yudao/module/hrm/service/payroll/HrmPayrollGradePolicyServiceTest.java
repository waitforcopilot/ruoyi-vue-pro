package cn.iocoder.yudao.module.hrm.service.payroll;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.*;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.HrmPayrollGradePolicyReqVO.*;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.HrmPayrollGradePolicyDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import cn.iocoder.yudao.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO;
import cn.iocoder.yudao.module.hrm.dal.mysql.payroll.HrmPayrollGradePolicyMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.salary.config.HrmSalaryOptionMapper;
import cn.iocoder.yudao.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import cn.iocoder.yudao.module.hrm.service.salary.employee.HrmSalaryEmployeeInfoService;
import cn.iocoder.yudao.module.hrm.controller.admin.salary.vo.employeeinfo.HrmSalaryEmployeeInfoUpdateReqVO;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.*;
import javax.annotation.Resource;
import java.util.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.*;
@Import({HrmPayrollGradePolicyService.class,org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class})
class HrmPayrollGradePolicyServiceTest extends BaseDbUnitTest {
    @Resource private HrmPayrollGradePolicyService service;
    @Resource private HrmPayrollGradePolicyMapper mapper;
    @Resource private HrmSalaryOptionMapper options;
    @Resource private HrmEmployeeMapper employees;
    @MockBean private HrmPayrollFormulaService formulas;
    @MockBean private HrmSalaryEmployeeInfoService salaries;
    @BeforeEach void catalog(){HrmSalaryOptionDO option=HrmSalaryOptionDO.builder().code(10101).parentCode(10).name("基本工资").enabled(true).build();options.insert(option);when(formulas.variables()).thenReturn(Collections.singletonList(option));}
    private HrmPayrollGradePolicyReqVO request(){
        HrmPayrollGradePolicyReqVO request=new HrmPayrollGradePolicyReqVO();request.setCode("PRODUCTION");request.setName("生产薪级");request.setOptionCode(10101);request.setEffectiveFrom(LocalDate.of(2026,10,1));request.setEffectiveTo(LocalDate.of(2026,12,31));request.setEvidence("批准的薪酬标准附件");
        Grade grade=new Grade();grade.setGradeCode("L1");grade.setStepCode("S1");grade.setMinimum(new BigDecimal("5000"));grade.setStandard(new BigDecimal("6000"));grade.setMaximum(new BigDecimal("7000"));request.setGrades(Collections.singletonList(grade));
        PostCoefficient post=new PostCoefficient();post.setPostName("操作工");post.setCoefficient(new BigDecimal("1.2"));request.setPostCoefficients(Collections.singletonList(post));return request;
    }
    private Long approved(){Long id=service.create(request());HrmPayrollGradePolicyDO policy=mapper.selectById(id);policy.setCreator("1");mapper.updateById(policy);service.approve(id,2L);return id;}
    @Test void duplicateGradesAndBadRangesCannotCreateVersion(){
        HrmPayrollGradePolicyReqVO request=request();request.setGrades(Arrays.asList(request.getGrades().get(0),request.getGrades().get(0)));
        assertServiceException(()->service.create(request),PAYROLL_GRADE_INVALID);assertEquals(0L,mapper.selectCount());
        request.setGrades(Collections.singletonList(request.getGrades().get(0)));request.getGrades().get(0).setMinimum(new BigDecimal("8000"));
        assertServiceException(()->service.create(request),PAYROLL_GRADE_INVALID);
    }
    @Test void approvedVersionsAreSeparateAndCannotOverlap(){
        Long id=approved();assertEquals(1,mapper.selectById(id).getVersion());Long second=service.create(request());assertEquals(2,mapper.selectById(second).getVersion());
        assertServiceException(()->service.approve(second,2L),PAYROLL_GRADE_INVALID);
        HrmPayrollGradePolicyDO pending=mapper.selectById(second);pending.setCreator("2");mapper.updateById(pending);assertServiceException(()->service.approve(second,2L),PAYROLL_SELF_APPROVAL);
    }
    @Test void importingDuplicateRowsReportsLineAndDoesNotPersist(){
        HrmPayrollGradeImportRowVO row=new HrmPayrollGradeImportRowVO();row.setType("GRADE");row.setGradeCode("L1");row.setStepCode("S1");row.setMinimum(new BigDecimal("5000"));row.setStandard(new BigDecimal("6000"));row.setMaximum(new BigDecimal("7000"));
        List<HrmPayrollGradeImportRowVO> rows=Arrays.asList(row,row);assertEquals(3,service.validateRows(rows).get(0).get("row"));
        assertServiceException(()->service.importRows(request(),rows),PAYROLL_GRADE_INVALID);assertEquals(0L,mapper.selectCount());
    }
    @Test void excelExportRoundTripsIntoAnotherImmutableVersion() throws Exception {
        Long first=service.create(request());
        org.springframework.mock.web.MockHttpServletResponse response=new org.springframework.mock.web.MockHttpServletResponse();
        cn.iocoder.yudao.framework.excel.core.util.ExcelUtils.write(response,"grades.xlsx","标准",HrmPayrollGradeImportRowVO.class,service.exportRows(first));
        byte[] bytes=response.getContentAsByteArray();assertEquals('P',bytes[0]);assertEquals('K',bytes[1]);
        org.springframework.mock.web.MockMultipartFile file=new org.springframework.mock.web.MockMultipartFile("file","grades.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes);
        List<HrmPayrollGradeImportRowVO> rows=cn.iocoder.yudao.framework.excel.core.util.ExcelUtils.read(file,HrmPayrollGradeImportRowVO.class,2001);
        assertEquals(2,rows.size());assertTrue(service.validateRows(rows).isEmpty());
        Long second=service.importRows(request(),rows);assertEquals(2,mapper.selectById(second).getVersion());
        assertEquals(mapper.selectById(first).getGrades(),mapper.selectById(second).getGrades());
    }
    @Test void gradeApplicationCreatesSalaryApprovalWithPositionCoefficient(){
        Long id=approved();HrmEmployeeDO employee=new HrmEmployeeDO().setName("薪级员工").setJobNumber("GRADE-01").setPostName("操作工");employees.insert(employee);
        Apply request=new Apply();request.setPolicyId(id);request.setEmployeeId(employee.getId());request.setGradeCode("L1");request.setStepCode("S1");request.setEffectiveDate(LocalDate.of(2026,10,1));request.setProbationFactor(new BigDecimal("0.8"));
        when(salaries.updateSalaryEmployeeInfo(any())).thenReturn(888L);assertEquals(888L,service.apply(request));
        org.mockito.ArgumentCaptor<HrmSalaryEmployeeInfoUpdateReqVO> proposal=org.mockito.ArgumentCaptor.forClass(HrmSalaryEmployeeInfoUpdateReqVO.class);verify(salaries).updateSalaryEmployeeInfo(proposal.capture());
        assertEquals(0,new BigDecimal("7200").compareTo(proposal.getValue().getSalaryOptions().get(0).getValue()));assertEquals(0,new BigDecimal("5760").compareTo(proposal.getValue().getProbationSalaryOptions().get(0).getValue()));
        assertTrue(proposal.getValue().getRemark().contains("v1"));verify(salaries,never()).applyApprovedSalaryEmployeeInfo(any());
        request.setBaseAmount(new BigDecimal("8000"));assertServiceException(()->service.apply(request),PAYROLL_GRADE_INVALID);
        request.setBaseAmount(null);request.setEffectiveDate(LocalDate.of(2027,1,1));assertServiceException(()->service.apply(request),PAYROLL_GRADE_INVALID);
    }
    @Test void unmatchedPositionCannotSilentlyUseCoefficientOne(){
        Long id=approved();HrmEmployeeDO employee=new HrmEmployeeDO().setName("未映射岗位").setJobNumber("GRADE-02").setPostName("其他岗位");employees.insert(employee);
        Apply request=new Apply();request.setPolicyId(id);request.setEmployeeId(employee.getId());request.setGradeCode("L1");request.setStepCode("S1");request.setEffectiveDate(LocalDate.of(2026,10,1));
        assertServiceException(()->service.apply(request),PAYROLL_GRADE_INVALID);verify(salaries,never()).updateSalaryEmployeeInfo(any());
    }
}
