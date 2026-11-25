package com.ficn.panel.model.entity.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PodVO {
    private MetadataVO metadata;
    private SpecVO spec;
    private StatusVO status;

    @Data
    public static class MetadataVO {
        private String name;
        private String creationTimestamp;
        private String namespace;
    }

    @Data
    public static class SpecVO {
        private String nodeName;
        private List<ContainerVO> containers;
        private List<ContainerVO> initContainers;
    }

    @Data
    public static class ContainerVO {
        private String name;
        private ResourceRequirementsVO resources;
    }

    @Data
    public static class ResourceRequirementsVO {
        private Map<String, String> requests;
        private Map<String, String> limits;
    }

    @Data
    public static class StatusVO {
        private String phase;
        private List<ContainerStatusVO> containerStatuses;
        private String podIP;
        private String cpu;
        private String memory;
        @Data
        public static class ContainerStatusVO {
            private Boolean ready;
            private Integer restartCount;
        }
    }
}
