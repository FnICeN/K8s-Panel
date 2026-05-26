package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.DeploymentApi;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.model.dto.cluster.DeploymentSpecResponse;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.entity.Deployment;
import com.ficn.panel.model.entity.vo.DeploymentVO;
import com.ficn.panel.model.entity.vo.EventVO;
import com.ficn.panel.model.entity.vo.PodVO;
import com.ficn.panel.service.cluster.DeploymentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class DeploymentServiceImpl implements DeploymentService {
    @Resource
    private DeploymentApi deploymentApi;

    @Override
    public K8sListResponse<DeploymentVO> getDeployments(String token) {
        return deploymentApi.listAll(token);
    }

    @Override
    public K8sListResponse<DeploymentVO> getDeployments(String token, String namespace) {
        return deploymentApi.listByNamespace(token, namespace);
    }

    @Override
    public DeploymentSpecResponse getDeployment(String token, String namespace, String deploymentName) {
        return deploymentApi.get(token, namespace, deploymentName);
    }

    @Override
    public String getDeploymentAllSpec(String token, String namespace, String deploymentName) {
        return deploymentApi.getYaml(token, namespace, deploymentName);
    }

    @Override
    public K8sListResponse<PodVO> getDeploymentPods(String token, String namespace, String deploymentName) {
        DeploymentSpecResponse deployment = deploymentApi.get(token, namespace, deploymentName);
        if (deployment == null) {
            return null;
        }
        String labelSelector = getLabelSelector(deployment);
        if (!StringUtils.hasText(labelSelector)) {
            K8sListResponse<PodVO> response = new K8sListResponse<>();
            response.setKind("PodList");
            response.setApiVersion("v1");
            response.setItems(Collections.emptyList());
            return response;
        }
        return deploymentApi.listPodsBySelector(token, namespace, labelSelector);
    }

    @Override
    public String getDeploymentLog(String token, String namespace, String deploymentName, String podName,
                                   String container, Integer tailLines) {
        if (!StringUtils.hasText(podName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "podName must not be blank");
        }
        K8sListResponse<PodVO> pods = getDeploymentPods(token, namespace, deploymentName);
        if (pods == null) {
            return null;
        }
        List<PodVO> items = pods.getItems() == null ? Collections.emptyList() : pods.getItems();
        boolean podBelongsToDeployment = items.stream()
                .anyMatch(pod -> pod != null
                        && pod.getMetadata() != null
                        && podName.equals(pod.getMetadata().getName()));
        if (!podBelongsToDeployment) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "podName does not belong to deployment");
        }
        return deploymentApi.getPodLog(token, namespace, podName, container, tailLines);
    }

    @Override
    public K8sListResponse<EventVO> getDeploymentEvents(String token, String namespace, String deploymentName) {
        DeploymentSpecResponse deployment = deploymentApi.get(token, namespace, deploymentName);
        if (deployment == null) {
            return null;
        }
        K8sListResponse<EventVO> events = deploymentApi.listEvents(token, namespace, deploymentName);
        List<EventVO> items = new ArrayList<>();
        if (events != null && events.getItems() != null) {
            items.addAll(events.getItems());
        }
        K8sListResponse<PodVO> pods = getDeploymentPods(token, namespace, deploymentName);
        if (pods != null && pods.getItems() != null) {
            pods.getItems().stream()
                    .filter(pod -> pod != null && pod.getMetadata() != null && StringUtils.hasText(pod.getMetadata().getName()))
                    .map(pod -> deploymentApi.listEventsByObject(token, namespace, "Pod", pod.getMetadata().getName()))
                    .filter(podEvents -> podEvents != null && podEvents.getItems() != null)
                    .forEach(podEvents -> items.addAll(podEvents.getItems()));
        }
        K8sListResponse<EventVO> response = new K8sListResponse<>();
        response.setKind(events == null ? "EventList" : events.getKind());
        response.setApiVersion(events == null ? "v1" : events.getApiVersion());
        response.setItems(items);
        return response;
    }

    @Override
    public void deleteDeployment(String token, String namespace, String deploymentName) {
        DeploymentSpecResponse deployment = deploymentApi.get(token, namespace, deploymentName);
        if (deployment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "deployment may not exist");
        }
        deploymentApi.delete(token, namespace, deploymentName);
    }

    private String getLabelSelector(DeploymentSpecResponse deployment) {
        Deployment.DeploymentSpec spec = deployment.getSpec();
        if (spec == null || spec.getSelector() == null) {
            return "";
        }
        List<String> requirements = new ArrayList<>();
        Map<String, String> matchLabels = spec.getSelector().getMatchLabels();
        if (!CollectionUtils.isEmpty(matchLabels)) {
            matchLabels.forEach((key, value) -> requirements.add(key + "=" + value));
        }
        List<Deployment.LabelSelector.LabelSelectorRequirement> matchExpressions = spec.getSelector().getMatchExpressions();
        if (!CollectionUtils.isEmpty(matchExpressions)) {
            matchExpressions.stream()
                    .map(this::toLabelSelectorRequirement)
                    .filter(StringUtils::hasText)
                    .forEach(requirements::add);
        }
        return String.join(",", requirements);
    }

    private String toLabelSelectorRequirement(Deployment.LabelSelector.LabelSelectorRequirement requirement) {
        if (requirement == null || !StringUtils.hasText(requirement.getKey())) {
            return "";
        }
        String operator = requirement.getOperator();
        List<String> values = requirement.getValues() == null ? Collections.emptyList() : requirement.getValues();
        if ("In".equals(operator)) {
            return requirement.getKey() + " in (" + String.join(",", values) + ")";
        }
        if ("NotIn".equals(operator)) {
            return requirement.getKey() + " notin (" + String.join(",", values) + ")";
        }
        if ("Exists".equals(operator)) {
            return requirement.getKey();
        }
        if ("DoesNotExist".equals(operator)) {
            return "!" + requirement.getKey();
        }
        return "";
    }
}
