package com.ficn.panel.client.k8s;

import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class K8sApiPaths {

    static final String NODES = "/api/v1/nodes";
    static final String NAMESPACES = "/api/v1/namespaces";
    static final String DEPLOYMENTS = "/apis/apps/v1/deployments";

    private K8sApiPaths() {
    }

    static String node(String nodeName) {
        return NODES + "/" + encodeRequired(nodeName, "nodeName");
    }

    static String namespacePods(String namespace) {
        return "/api/v1/namespaces/" + encodeRequired(namespace, "namespace") + "/pods";
    }

    static String pod(String namespace, String podName) {
        return namespacePods(namespace) + "/" + encodeRequired(podName, "podName");
    }

    static String namespacePodsByLabels(String namespace, String labelSelector) {
        return namespacePods(namespace) + queryParam("labelSelector", labelSelector);
    }

    static String podLog(String namespace, String podName, String container, Integer tailLines) {
        List<String> params = new ArrayList<>();
        if (StringUtils.hasText(container)) {
            params.add(queryPair("container", container));
        }
        if (tailLines != null && tailLines > 0) {
            params.add(queryPair("tailLines", String.valueOf(tailLines)));
        }
        return pod(namespace, podName) + "/log" + queryString(params);
    }

    static String namespaceServices(String namespace) {
        return "/api/v1/namespaces/" + encodeRequired(namespace, "namespace") + "/services";
    }

    static String service(String namespace, String serviceName) {
        return namespaceServices(namespace) + "/" + encodeRequired(serviceName, "serviceName");
    }

    static String namespaceDeployments(String namespace) {
        return "/apis/apps/v1/namespaces/" + encodeRequired(namespace, "namespace") + "/deployments";
    }

    static String deployment(String namespace, String deploymentName) {
        return namespaceDeployments(namespace) + "/" + encodeRequired(deploymentName, "deploymentName");
    }

    static String namespaceEventsByDeployment(String namespace, String deploymentName) {
        return namespaceEventsByObject(namespace, "Deployment", deploymentName);
    }

    static String namespaceEventsByObject(String namespace, String kind, String name) {
        String fieldSelector = "involvedObject.kind=" + requireText(kind, "kind")
                + ",involvedObject.name=" + requireText(name, "name");
        return "/api/v1/namespaces/" + encodeRequired(namespace, "namespace") + "/events"
                + queryParam("fieldSelector", fieldSelector);
    }

    private static String encodeRequired(String value, String name) {
        return UriUtils.encodePathSegment(requireText(value, name), StandardCharsets.UTF_8);
    }

    private static String requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, name + " must not be blank");
        }
        return value;
    }

    private static String queryParam(String name, String value) {
        return queryString(List.of(queryPair(name, value)));
    }

    private static String queryPair(String name, String value) {
        return UriUtils.encodeQueryParam(name, StandardCharsets.UTF_8)
                + "="
                + UriUtils.encodeQueryParam(requireText(value, name), StandardCharsets.UTF_8);
    }

    private static String queryString(List<String> params) {
        if (params.isEmpty()) {
            return "";
        }
        return "?" + String.join("&", params);
    }
}
