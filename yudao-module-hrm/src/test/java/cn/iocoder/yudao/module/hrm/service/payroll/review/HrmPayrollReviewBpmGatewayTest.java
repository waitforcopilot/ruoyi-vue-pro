package cn.iocoder.yudao.module.hrm.service.payroll.review;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.hrm.enums.ErrorCodeConstants.PAYROLL_REVIEW_BPM_UNSUPPORTED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.hrm.dal.dataobject.payroll.review.HrmPayrollReviewCycleDO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.flowable.bpmn.model.*;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

class HrmPayrollReviewBpmGatewayTest {
    private final HrmPayrollReviewBpmGatewayImpl gateway = new HrmPayrollReviewBpmGatewayImpl();
    private final BpmProcessDefinitionService definitions = mock(BpmProcessDefinitionService.class);
    private final BpmProcessInstanceApi api = mock(BpmProcessInstanceApi.class);
    private String xml;
    private BpmnModel model;
    private BpmProcessDefinitionInfoDO info;

    @BeforeEach
    void setup() throws IOException {
        ReflectionTestUtils.setField(gateway, "definitions", definitions);
        ReflectionTestUtils.setField(gateway, "api", api);
        try (InputStream in = getClass().getResourceAsStream("/bpmn/payroll-review.bpmn20.xml")) {
            assertNotNull(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            xml = new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
        ProcessDefinition d = mock(ProcessDefinition.class);
        when(d.getId()).thenReturn("validated-deployment");
        when(definitions.getActiveProcessDefinition(PayrollBpmContext.KEY)).thenReturn(d);
        info =
                new BpmProcessDefinitionInfoDO()
                        .setAutoApprovalType(0)
                        .setFormType(20)
                        .setAllowCancelRunningProcess(true)
                        .setAllowWithdrawTask(false);
        when(definitions.getProcessDefinitionInfo("validated-deployment")).thenReturn(info);
        useXml(xml);
    }

    private void useXml(String text) {
        model = BpmnModelUtils.getBpmnModel(text.getBytes(StandardCharsets.UTF_8));
        when(definitions.getProcessDefinitionBpmnModel("validated-deployment")).thenReturn(model);
    }

    @Test
    void bundledDefinitionHasExactlyTwoManualSequentialTasks() {
        assertEquals("validated-deployment", gateway.definition());
    }

    @Test
    void missingOrSuspendedDeploymentCannotStart() {
        when(definitions.getActiveProcessDefinition(PayrollBpmContext.KEY)).thenReturn(null);
        assertServiceException(gateway::definition, PAYROLL_REVIEW_BPM_UNSUPPORTED);
    }

    @Test
    void removingFinanceStageCannotSilentlyDowngradeReview() {
        model.getMainProcess().removeFlowElement("financeReview");
        assertServiceException(gateway::definition, PAYROLL_REVIEW_BPM_UNSUPPORTED);
    }

    @Test
    void alteredRouteCannotSkipFinance() {
        ((SequenceFlow) model.getMainProcess().getFlowElement("toHr"))
                .setTargetRef("financeReview");
        assertServiceException(gateway::definition, PAYROLL_REVIEW_BPM_UNSUPPORTED);
    }

    @Test
    void automaticApprovalOrEmptyAssigneeApprovalIsRejected() {
        useXml(xml.replace("<flowable:approveType>1", "<flowable:approveType>2"));
        assertServiceException(gateway::definition, PAYROLL_REVIEW_BPM_UNSUPPORTED);
        useXml(
                xml.replace(
                        "<flowable:assignEmptyHandlerType>2",
                        "<flowable:assignEmptyHandlerType>1"));
        assertServiceException(gateway::definition, PAYROLL_REVIEW_BPM_UNSUPPORTED);
    }

    @Test
    void automaticDeduplicationOrWithdrawMetadataIsRejected() {
        info.setAutoApprovalType(1);
        assertServiceException(gateway::definition, PAYROLL_REVIEW_BPM_UNSUPPORTED);
        info.setAutoApprovalType(0).setAllowWithdrawTask(true);
        assertServiceException(gateway::definition, PAYROLL_REVIEW_BPM_UNSUPPORTED);
    }

    @Test
    void engineUsesValidatedDeploymentAndOnlyControlMetadata() {
        HrmPayrollReviewCycleDO c =
                new HrmPayrollReviewCycleDO()
                        .setId(3L)
                        .setBatchId(1L)
                        .setRunId(2L)
                        .setStartedBy(10L)
                        .setHrReviewerId(11L)
                        .setFinanceReviewerId(12L)
                        .setSourceHash("fingerprint")
                        .setProcessDefinitionId("validated-deployment")
                        .setSubmitEvidence("protected evidence");
        when(api.createProcessInstance(eq(10L), any()))
                .thenAnswer(
                        a -> {
                            BpmProcessInstanceCreateReqDTO r = a.getArgument(1);
                            assertEquals("validated-deployment", r.getProcessDefinitionId());
                            assertEquals(PayrollBpmContext.KEY, r.getProcessDefinitionKey());
                            assertEquals("3", r.getBusinessKey());
                            assertEquals(
                                    new HashSet<>(
                                            Arrays.asList(
                                                    "payrollBatchId",
                                                    "payrollRunId",
                                                    "payrollReviewId",
                                                    "payrollSourceHash")),
                                    r.getVariables().keySet());
                            assertEquals(
                                    Collections.singletonList(11L),
                                    r.getStartUserSelectAssignees().get("hrReview"));
                            assertEquals(
                                    Collections.singletonList(12L),
                                    r.getStartUserSelectAssignees().get("financeReview"));
                            assertEquals("submit", PayrollBpmContext.current().getAction());
                            return "instance";
                        });
        assertEquals("instance", gateway.start(c));
        assertNull(PayrollBpmContext.current());
    }
}
