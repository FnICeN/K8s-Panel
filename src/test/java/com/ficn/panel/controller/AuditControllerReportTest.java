package com.ficn.panel.controller;

import com.ficn.panel.common.BaseResponse;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.model.entity.User;
import com.ficn.panel.model.entity.vo.AuditAlertVO;
import com.ficn.panel.model.entity.vo.AuditReportPageVO;
import com.ficn.panel.repository.AuditRepository;
import com.ficn.panel.service.audit.AuditReportService;
import com.ficn.panel.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerReportTest {

    private static final String UUID = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private UserService userService;
    @Mock
    private AuditRepository auditRepository;
    @Mock
    private AuditReportService auditReportService;

    @TempDir
    Path tempDir;

    private AuditController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new AuditController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "auditRepository", auditRepository);
        ReflectionTestUtils.setField(controller, "auditReportService", auditReportService);
        request = new MockHttpServletRequest();
        when(userService.getLoginUser(request)).thenReturn(new User());
    }

    @Test
    void downloadLazilyGeneratesHistoricalReport() throws Exception {
        AuditAlertVO alert = new AuditAlertVO();
        alert.setUuid(UUID);
        Path report = tempDir.resolve(UUID + ".pdf");
        Files.writeString(report, "%PDF-test");
        when(auditRepository.getAlert(UUID)).thenReturn(alert);
        when(auditReportService.generateIfAbsent(alert)).thenReturn(report);

        ResponseEntity<org.springframework.core.io.Resource> response =
                controller.downloadReport(UUID, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("audit-report-" + UUID + ".pdf");
        assertThat(response.getBody()).isNotNull();
        verify(auditReportService).generateIfAbsent(alert);
    }

    @Test
    void statusRejectsMissingAlert() {
        when(auditRepository.getAlert(UUID)).thenReturn(null);

        assertThatThrownBy(() -> controller.getReportStatus(UUID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void listsStoredReportsWithAlertMetadata() {
        AuditReportService.StoredReport storedReport =
                new AuditReportService.StoredReport(UUID, 2_000L, 4_096L);
        when(auditReportService.listReports(10, 0))
                .thenReturn(new AuditReportService.StoredReportPage(1, List.of(storedReport)));
        AuditAlertVO alert = new AuditAlertVO();
        alert.setUuid(UUID);
        alert.setRuleCn("读取敏感密钥");
        alert.setPriority("Warning");
        alert.setNamespace("kube-app");
        when(auditRepository.getAlert(UUID)).thenReturn(alert);

        BaseResponse<AuditReportPageVO> response =
                controller.listReports(10, 0, request);

        assertThat(response.getData().getTotal()).isEqualTo(1);
        assertThat(response.getData().getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getUuid()).isEqualTo(UUID);
                    assertThat(item.getRuleCn()).isEqualTo("读取敏感密钥");
                    assertThat(item.getSizeBytes()).isEqualTo(4_096L);
                });
    }

    @Test
    void manuallyDeletesStoredReport() {
        when(auditReportService.deleteReport(UUID)).thenReturn(true);

        BaseResponse<Boolean> response = controller.deleteReport(UUID, request);

        assertThat(response.getData()).isTrue();
        verify(auditReportService).deleteReport(UUID);
    }
}
