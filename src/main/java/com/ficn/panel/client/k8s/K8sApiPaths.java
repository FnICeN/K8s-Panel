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
    static final String NODE_METRICS = "/apis/metrics.k8s.io/v1beta1/nodes";
    static final String PODS = "/api/v1/pods";
    static final String NAMESPACES = "/api/v1/namespaces";
    static final String DEPLOYMENTS = "/apis/apps/v1/deployments";
    static final String ROLES = "/apis/rbac.authorization.k8s.io/v1/roles";
    static final String CLUSTER_ROLES = "/apis/rbac.authorization.k8s.io/v1/clusterroles";
    static final String ROLE_BINDINGS = "/apis/rbac.authorization.k8s.io/v1/rolebindings";
    static final String GATEKEEPER_TEMPLATES = "/apis/templates.gatekeeper.sh/v1/constrainttemplates";
    static final String GATEKEEPER_CONSTRAINT_GROUP = "/apis/constraints.gatekeeper.sh";
    static final String GATEKEEPER_MUTATION_GROUP = "/apis/mutations.gatekeeper.sh";

    static String withDryRun(String path, boolean dryRun) {
        return dryRun ? path + "?dryRun=All" : path;
    }

    static String gatekeeperConstraints(String version, String resource) {
        return GATEKEEPER_CONSTRAINT_GROUP + "/" + encodeRequired(version, "version")
                + "/" + encodeRequired(resource, "resource");
    }

    static String gatekeeperConstraint(String version, String resource, String name) {
        return gatekeeperConstraints(version, resource) + "/" + encodeRequired(name, "name");
    }

    private K8sApiPaths() {
    }

    static String node(String nodeName) {
        return NODES + "/" + encodeRequired(nodeName, "nodeName");
    }

    static String nodeProxyStats(String nodeName) {
        return node(nodeName) + "/proxy/stats/summary";
    }

    static String namespacePods(String namespace) {
        return "/api/v1/namespaces/" + encodeRequired(namespace, "namespace") + "/pods";
    }

    static String pod(String namespace, String podName) {
        return namespacePods(namespace) + "/" + encodeRequired(podName, "podName");
    }

    static String podEphemeralContainers(String namespace, String podName) {
        return pod(namespace, podName) + "/ephemeralcontainers";
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

    static String namespaceConfigMaps(String namespace) {
        return "/api/v1/namespaces/" + encodeRequired(namespace, "namespace") + "/configmaps";
    }

    static String service(String namespace, String serviceName) {
        return namespaceServices(namespace) + "/" + encodeRequired(serviceName, "serviceName");
    }

    static String namespaceDeployments(String namespace) {
        return "/apis/apps/v1/namespaces/" + encodeRequired(namespace, "namespace") + "/deployments";
    }

    static String namespaceRoles(String namespace) {
        return "/apis/rbac.authorization.k8s.io/v1/namespaces/" + encodeRequired(namespace, "namespace") + "/roles";
    }

    static String role(String namespace, String name) {
        return namespaceRoles(namespace) + "/" + encodeRequired(name, "name");
    }

    static String clusterRole(String name) {
        return CLUSTER_ROLES + "/" + encodeRequired(name, "name");
    }

    static String namespaceRoleBindings(String namespace) {
        return "/apis/rbac.authorization.k8s.io/v1/namespaces/" + encodeRequired(namespace, "namespace") + "/rolebindings";
    }

    static String roleBinding(String namespace, String name) {
        return namespaceRoleBindings(namespace) + "/" + encodeRequired(name, "name");
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
