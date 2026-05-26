package com.ficn.panel.model.entity.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DeploymentVO {
    private MetadataVO metadata;
    private SpecVO spec;
    private StatusVO status;

    @Data
    public static class MetadataVO {
        private String name;
        private String namespace;
        private String creationTimestamp;
    }

    @Data
    public static class SpecVO {
        private Integer replicas;
        private SelectorVO selector;

        @Data
        public static class SelectorVO {
            private Map<String, String> matchLabels;
        }
    }

    @Data
    public static class StatusVO {
        private Integer replicas;
        private Integer updatedReplicas;
        private Integer readyReplicas;
        private Integer availableReplicas;
        private Integer unavailableReplicas;
        private List<ConditionVO> conditions;

        @Data
        public static class ConditionVO {
            private String type;
            private String status;
            private String reason;
            private String message;
        }
    }
}
