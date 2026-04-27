package com.ficn.panel.model.entity.vo;

import lombok.Data;

@Data
public class NamespaceVO {

    private MetadataVO metadata;
    private StatusVO status;

        @Data
        public static class MetadataVO {
            private String name;
            private String creationTimestamp;
        }

        @Data
        public static class StatusVO {
            private String phase;
        }
}
