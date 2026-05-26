package com.ficn.panel.model.entity.vo;

import lombok.Data;

@Data
public class EventVO {
    private MetadataVO metadata;
    private String type;
    private String reason;
    private String message;
    private Integer count;
    private String firstTimestamp;
    private String lastTimestamp;
    private String eventTime;
    private InvolvedObjectVO involvedObject;

    @Data
    public static class MetadataVO {
        private String name;
        private String namespace;
        private String creationTimestamp;
    }

    @Data
    public static class InvolvedObjectVO {
        private String kind;
        private String namespace;
        private String name;
        private String uid;
        private String apiVersion;
        private String resourceVersion;
        private String fieldPath;
    }
}
