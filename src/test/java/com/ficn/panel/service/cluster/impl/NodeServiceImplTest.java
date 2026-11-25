package com.ficn.panel.service.cluster.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ficn.panel.client.k8s.NamespaceApi;
import com.ficn.panel.client.k8s.NodeApi;
import com.ficn.panel.client.k8s.PodApi;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.entity.vo.NodeMetricsVO;
import com.ficn.panel.model.entity.vo.NodeVO;
import com.ficn.panel.model.entity.vo.PodVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeServiceImplTest {

    @Mock
    private NodeApi nodeApi;
    @Mock
    private PodApi podApi;
    @Mock
    private NamespaceApi namespaceApi;

    private NodeServiceImpl nodeService;

    @BeforeEach
    void setUp() {
        nodeService = new NodeServiceImpl();
        ReflectionTestUtils.setField(nodeService, "nodeApi", nodeApi);
        ReflectionTestUtils.setField(nodeService, "podApi", podApi);
        ReflectionTestUtils.setField(nodeService, "namespaceApi", namespaceApi);
    }

    @Test
    void fallsBackToKubeletStatsWhenMetricsApiMissing() {
        when(nodeApi.listMetrics("token")).thenReturn(null);
        when(nodeApi.list("token")).thenReturn(nodes("worker2"));
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode stats = mapper.createObjectNode();
        ObjectNode node = stats.putObject("node");
        node.putObject("cpu").put("usageNanoCores", 500_000_000L);
        node.putObject("memory").put("workingSetBytes", 128L * 1024 * 1024);
        when(nodeApi.getStatsSummary(eq("token"), eq("worker2"))).thenReturn(stats);

        K8sListResponse<NodeMetricsVO> metrics = nodeService.getNodeMetrics("token");

        assertThat(metrics.getItems()).hasSize(1);
        assertThat(metrics.getItems().getFirst().getSource()).isEqualTo("kubelet");
        assertThat(metrics.getItems().getFirst().getUsage().getMemory()).isEqualTo(String.valueOf(128L * 1024 * 1024));
        assertThat(metrics.getItems().getFirst().getUsage().getCpu()).isEqualTo("500m");
    }

    @Test
    void fallsBackToPodRequestsWhenMetricsAndKubeletUnavailable() {
        when(nodeApi.listMetrics("token")).thenThrow(new BusinessException(ErrorCode.OPERATION_ERROR, "metrics gone"));
        when(nodeApi.list("token")).thenReturn(nodes("worker2"));
        when(nodeApi.getStatsSummary(eq("token"), eq("worker2")))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN_ERROR, "no proxy"));
        when(podApi.listAll("token")).thenReturn(pods("worker2", "250m", "128Mi"));

        K8sListResponse<NodeMetricsVO> metrics = nodeService.getNodeMetrics("token");

        assertThat(metrics.getItems()).hasSize(1);
        assertThat(metrics.getItems().getFirst().getSource()).isEqualTo("allocated");
        assertThat(metrics.getItems().getFirst().getUsage().getCpu()).isEqualTo("250m");
        assertThat(metrics.getItems().getFirst().getUsage().getMemory()).isEqualTo(String.valueOf(128L * 1024 * 1024));
    }

    @Test
    void returnsEmptyListWhenAllSourcesUnavailable() {
        when(nodeApi.listMetrics("token")).thenReturn(null);
        when(nodeApi.list("token")).thenReturn(nodes("worker2"));
        when(nodeApi.getStatsSummary(eq("token"), eq("worker2"))).thenReturn(null);
        when(podApi.listAll("token")).thenReturn(null);

        K8sListResponse<NodeMetricsVO> metrics = nodeService.getNodeMetrics("token");

        assertThat(metrics.getItems()).isEmpty();
    }

    private static K8sListResponse<NodeVO> nodes(String name) {
        NodeVO.MetadataVO metadata = new NodeVO.MetadataVO();
        metadata.setName(name);
        NodeVO node = new NodeVO();
        node.setMetadata(metadata);
        K8sListResponse<NodeVO> response = new K8sListResponse<>();
        response.setItems(List.of(node));
        return response;
    }

    private static K8sListResponse<PodVO> pods(String nodeName, String cpu, String memory) {
        PodVO.ResourceRequirementsVO resources = new PodVO.ResourceRequirementsVO();
        resources.setRequests(Map.of("cpu", cpu, "memory", memory));
        PodVO.ContainerVO container = new PodVO.ContainerVO();
        container.setResources(resources);
        PodVO.SpecVO spec = new PodVO.SpecVO();
        spec.setNodeName(nodeName);
        spec.setContainers(List.of(container));
        PodVO.StatusVO status = new PodVO.StatusVO();
        status.setPhase("Running");
        PodVO pod = new PodVO();
        pod.setSpec(spec);
        pod.setStatus(status);
        K8sListResponse<PodVO> response = new K8sListResponse<>();
        response.setItems(List.of(pod));
        return response;
    }
}
