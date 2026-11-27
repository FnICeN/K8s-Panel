package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.NamespaceApi;
import com.ficn.panel.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NamespaceServiceImplTest {

    @Mock
    private NamespaceApi namespaceApi;

    private NamespaceServiceImpl namespaceService;

    @BeforeEach
    void setUp() {
        namespaceService = new NamespaceServiceImpl();
        ReflectionTestUtils.setField(namespaceService, "namespaceApi", namespaceApi);
    }

    @Test
    void deleteNamespaceDeletesClusterScopedNamespace() {
        namespaceService.deleteNamespace("token", "demo");

        verify(namespaceApi).delete("token", "demo");
    }

    @Test
    void deleteNamespaceRejectsSystemNamespace() {
        assertThatThrownBy(() -> namespaceService.deleteNamespace("token", "kube-system"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("系统命名空间不能删除");

        verify(namespaceApi, never()).delete("token", "kube-system");
    }
}
