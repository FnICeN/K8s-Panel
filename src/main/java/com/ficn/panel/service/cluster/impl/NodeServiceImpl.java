package com.ficn.panel.service.cluster.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.ficn.panel.client.k8s.NamespaceApi;
import com.ficn.panel.client.k8s.NodeApi;
import com.ficn.panel.client.k8s.PodApi;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.NodeSpecResponse;
import com.ficn.panel.model.entity.vo.NamespaceVO;
import com.ficn.panel.model.entity.vo.NodeMetricsVO;
import com.ficn.panel.model.entity.vo.NodeVO;
import com.ficn.panel.model.entity.vo.PodVO;
import com.ficn.panel.parser.KubernetesQuantity;
import com.ficn.panel.service.cluster.NodeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NodeServiceImpl implements NodeService {
    private static final String SOURCE_METRICS = "metrics";
    private static final String SOURCE_KUBELET = "kubelet";
    private static final String SOURCE_ALLOCATED = "allocated";

    @Resource
    private NodeApi nodeApi;
    @Resource
    private PodApi podApi;
    @Resource
    private NamespaceApi namespaceApi;

    @Override
    public K8sListResponse<NodeVO> getNodes(String token) {
        return nodeApi.list(token);
    }

    @Override
    public K8sListResponse<NodeMetricsVO> getNodeMetrics(String token) {
        K8sListResponse<NodeMetricsVO> metrics = tryMetricsApi(token);
        if (hasItems(metrics)) {
            metrics.getItems().forEach(item -> {
                if (item != null && !StringUtils.hasText(item.getSource())) {
                    item.setSource(SOURCE_METRICS);
                }
            });
            return metrics;
        }

        K8sListResponse<NodeVO> nodes = safeListNodes(token);
        K8sListResponse<NodeMetricsVO> kubeletMetrics = collectKubeletStats(token, nodes);
        if (hasItems(kubeletMetrics)) {
            log.info("metrics.k8s.io unavailable, using kubelet stats for {} nodes",
                    kubeletMetrics.getItems().size());
            return kubeletMetrics;
        }

        K8sListResponse<NodeMetricsVO> allocated = collectAllocatedFromPods(token, nodes);
        if (hasItems(allocated)) {
            log.info("metrics.k8s.io and kubelet stats unavailable, using pod requests for {} nodes",
                    allocated.getItems().size());
            return allocated;
        }

        return emptyMetrics();
    }

    @Override
    public NodeSpecResponse getNodeSpec(String token, String nodeName) {
        return nodeApi.get(token, nodeName);
    }

    @Override
    public String getNodeAllSpec(String token, String nodeName) {
        return nodeApi.getYaml(token, nodeName);
    }

    private K8sListResponse<NodeMetricsVO> tryMetricsApi(String token) {
        try {
            return nodeApi.listMetrics(token);
        } catch (BusinessException e) {
            if (isAuthError(e)) {
                throw e;
            }
            log.warn("metrics.k8s.io request failed: {}", e.getMessage());
            return null;
        }
    }

    private K8sListResponse<NodeVO> safeListNodes(String token) {
        try {
            return nodeApi.list(token);
        } catch (BusinessException e) {
            if (isAuthError(e)) {
                throw e;
            }
            log.warn("list nodes for metrics fallback failed: {}", e.getMessage());
            return null;
        }
    }

    private K8sListResponse<NodeMetricsVO> collectKubeletStats(String token, K8sListResponse<NodeVO> nodes) {
        if (!hasItems(nodes)) {
            return emptyMetrics();
        }
        List<NodeMetricsVO> items = new ArrayList<>();
        for (NodeVO node : nodes.getItems()) {
            String nodeName = nodeName(node);
            if (!StringUtils.hasText(nodeName)) {
                continue;
            }
            try {
                JsonNode stats = nodeApi.getStatsSummary(token, nodeName);
                NodeMetricsVO metrics = fromKubeletStats(nodeName, stats);
                if (metrics != null) {
                    items.add(metrics);
                }
            } catch (BusinessException e) {
                if (isAuthError(e)) {
                    throw e;
                }
                log.debug("kubelet stats unavailable for node {}: {}", nodeName, e.getMessage());
            }
        }
        return wrapMetrics(items);
    }

    private K8sListResponse<NodeMetricsVO> collectAllocatedFromPods(String token, K8sListResponse<NodeVO> nodes) {
        List<PodVO> pods = listAllPods(token);
        if (pods.isEmpty()) {
            return emptyMetrics();
        }

        Map<String, UsageAccumulator> usageByNode = new HashMap<>();
        for (PodVO pod : pods) {
            if (pod == null || pod.getSpec() == null || !StringUtils.hasText(pod.getSpec().getNodeName())) {
                continue;
            }
            if (isTerminalPod(pod)) {
                continue;
            }
            UsageAccumulator accumulator = usageByNode.computeIfAbsent(pod.getSpec().getNodeName(),
                    ignored -> new UsageAccumulator());
            addContainerRequests(accumulator, pod.getSpec().getContainers());
            addContainerRequests(accumulator, pod.getSpec().getInitContainers());
        }

        if (hasItems(nodes)) {
            for (NodeVO node : nodes.getItems()) {
                String nodeName = nodeName(node);
                if (StringUtils.hasText(nodeName)) {
                    usageByNode.putIfAbsent(nodeName, new UsageAccumulator());
                }
            }
        }

        List<NodeMetricsVO> items = new ArrayList<>();
        usageByNode.forEach((nodeName, usage) -> items.add(metricsOf(
                nodeName,
                KubernetesQuantity.milliCpu(usage.cpuCores),
                KubernetesQuantity.bytes(usage.memoryBytes),
                SOURCE_ALLOCATED)));
        return wrapMetrics(items);
    }

    private List<PodVO> listAllPods(String token) {
        try {
            K8sListResponse<PodVO> allPods = podApi.listAll(token);
            if (hasItems(allPods)) {
                return allPods.getItems();
            }
        } catch (BusinessException e) {
            if (isAuthError(e)) {
                throw e;
            }
            log.warn("cluster-wide pod list failed, falling back to namespaced lists: {}", e.getMessage());
        }
        return listPodsByNamespace(token);
    }

    private List<PodVO> listPodsByNamespace(String token) {
        K8sListResponse<NamespaceVO> namespaces;
        try {
            namespaces = namespaceApi.list(token);
        } catch (BusinessException e) {
            if (isAuthError(e)) {
                throw e;
            }
            log.warn("list namespaces for metrics fallback failed: {}", e.getMessage());
            return List.of();
        }
        if (!hasItems(namespaces)) {
            return List.of();
        }
        List<PodVO> pods = new ArrayList<>();
        for (NamespaceVO namespace : namespaces.getItems()) {
            String namespaceName = namespace != null && namespace.getMetadata() != null
                    ? namespace.getMetadata().getName()
                    : null;
            if (!StringUtils.hasText(namespaceName)) {
                continue;
            }
            try {
                K8sListResponse<PodVO> namespacedPods = podApi.listByNamespace(token, namespaceName);
                if (hasItems(namespacedPods)) {
                    pods.addAll(namespacedPods.getItems());
                }
            } catch (BusinessException e) {
                if (isAuthError(e)) {
                    throw e;
                }
                log.debug("list pods in namespace {} failed: {}", namespaceName, e.getMessage());
            }
        }
        return pods;
    }

    private NodeMetricsVO fromKubeletStats(String nodeName, JsonNode stats) {
        if (stats == null) {
            return null;
        }
        JsonNode node = stats.path("node");
        long workingSetBytes = node.path("memory").path("workingSetBytes").asLong(0L);
        if (workingSetBytes <= 0L) {
            workingSetBytes = node.path("memory").path("usageBytes").asLong(0L);
        }
        long usageNanoCores = node.path("cpu").path("usageNanoCores").asLong(0L);
        if (workingSetBytes <= 0L && usageNanoCores <= 0L) {
            return null;
        }
        return metricsOf(
                nodeName,
                KubernetesQuantity.milliCpu(usageNanoCores / 1_000_000_000D),
                KubernetesQuantity.bytes(workingSetBytes),
                SOURCE_KUBELET);
    }

    private static void addContainerRequests(UsageAccumulator accumulator, List<PodVO.ContainerVO> containers) {
        if (containers == null) {
            return;
        }
        for (PodVO.ContainerVO container : containers) {
            if (container == null || container.getResources() == null || container.getResources().getRequests() == null) {
                continue;
            }
            Map<String, String> requests = container.getResources().getRequests();
            accumulator.cpuCores += KubernetesQuantity.toCores(requests.get("cpu"));
            accumulator.memoryBytes += KubernetesQuantity.toBytes(requests.get("memory"));
        }
    }

    private static boolean isTerminalPod(PodVO pod) {
        String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : null;
        return "Succeeded".equals(phase) || "Failed".equals(phase);
    }

    private static NodeMetricsVO metricsOf(String nodeName, String cpu, String memory, String source) {
        NodeMetricsVO.MetadataVO metadata = new NodeMetricsVO.MetadataVO();
        metadata.setName(nodeName);
        NodeMetricsVO.UsageVO usage = new NodeMetricsVO.UsageVO();
        usage.setCpu(cpu);
        usage.setMemory(memory);
        NodeMetricsVO metrics = new NodeMetricsVO();
        metrics.setMetadata(metadata);
        metrics.setUsage(usage);
        metrics.setSource(source);
        return metrics;
    }

    private static K8sListResponse<NodeMetricsVO> wrapMetrics(List<NodeMetricsVO> items) {
        K8sListResponse<NodeMetricsVO> response = emptyMetrics();
        response.setItems(items);
        return response;
    }

    private static K8sListResponse<NodeMetricsVO> emptyMetrics() {
        K8sListResponse<NodeMetricsVO> response = new K8sListResponse<>();
        response.setKind("NodeMetricsList");
        response.setApiVersion("metrics.k8s.io/v1beta1");
        response.setItems(List.of());
        return response;
    }

    private static boolean hasItems(K8sListResponse<?> response) {
        return response != null && response.getItems() != null && !response.getItems().isEmpty();
    }

    private static String nodeName(NodeVO node) {
        return node != null && node.getMetadata() != null ? node.getMetadata().getName() : null;
    }

    private static boolean isAuthError(BusinessException e) {
        return e.getCode() == ErrorCode.NOT_LOGIN_ERROR.getCode();
    }

    private static final class UsageAccumulator {
        private double cpuCores;
        private long memoryBytes;
    }
}
