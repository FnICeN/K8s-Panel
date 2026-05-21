package com.ficn.panel.client.k8s;

import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

final class K8sApiPaths {

    static final String NODES = "/api/v1/nodes";
    static final String NAMESPACES = "/api/v1/namespaces";

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

    static String namespaceServices(String namespace) {
        return "/api/v1/namespaces/" + encodeRequired(namespace, "namespace") + "/services";
    }

    static String service(String namespace, String serviceName) {
        return namespaceServices(namespace) + "/" + encodeRequired(serviceName, "serviceName");
    }

    private static String encodeRequired(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, name + " must not be blank");
        }
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }
}
