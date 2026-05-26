package com.ficn.panel.model.entity.vo;

import lombok.Data;

import java.util.List;

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
