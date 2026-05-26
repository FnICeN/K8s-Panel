package com.ficn.panel.model.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Deployment {
    private ObjectMeta metadata;
    private DeploymentSpec spec;
    private DeploymentStatus status;

    @Data
    public static class DeploymentSpec {
        private Integer replicas;
        private LabelSelector selector;
        private DeploymentStrategy strategy;
        private PodTemplateSpec template;
        private Integer minReadySeconds;
        private Integer revisionHistoryLimit;
        private Integer progressDeadlineSeconds;
        private Boolean paused;
    }

    @Data
    public static class LabelSelector {
        private Map<String, String> matchLabels;
        private List<LabelSelectorRequirement> matchExpressions;

        @Data
        public static class LabelSelectorRequirement {
            private String key;
            private String operator;
            private List<String> values;
        }
    }

    @Data
    public static class DeploymentStrategy {
        private String type;
        private Map<String, Object> rollingUpdate;
    }

    @Data
    public static class PodTemplateSpec {
        private ObjectMeta metadata;
        private PodTemplateSpecSpec spec;
    }

    @Data
    public static class PodTemplateSpecSpec {
        private List<Container> initContainers;
        private List<Container> containers;
        private List<Map<String, Object>> volumes;
        private String restartPolicy;
        private String serviceAccountName;
        private List<Map<String, Object>> imagePullSecrets;
        private Map<String, Object> securityContext;
        private Map<String, Object> affinity;
        private Map<String, Object> nodeSelector;
        private List<Map<String, Object>> tolerations;
    }

    @Data
    public static class Container {
        private String name;
        private String image;
        private String imagePullPolicy;
        private List<String> command;
        private List<String> args;
        private List<Map<String, Object>> ports;
        private List<Map<String, Object>> env;
        private List<Map<String, Object>> envFrom;
        private Map<String, Object> resources;
        private List<Map<String, Object>> volumeMounts;
        private Map<String, Object> livenessProbe;
        private Map<String, Object> readinessProbe;
        private Map<String, Object> startupProbe;
    }

    @Data
    public static class DeploymentStatus {
        private Long observedGeneration;
        private Integer replicas;
        private Integer updatedReplicas;
        private Integer readyReplicas;
        private Integer availableReplicas;
        private Integer unavailableReplicas;
        private Integer collisionCount;
        private List<DeploymentCondition> conditions;

        @Data
        public static class DeploymentCondition {
            private String type;
            private String status;
            private String lastUpdateTime;
            private String lastTransitionTime;
            private String reason;
            private String message;
        }
    }
}
