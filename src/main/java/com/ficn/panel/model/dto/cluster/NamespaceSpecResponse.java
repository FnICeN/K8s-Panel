package com.ficn.panel.model.dto.cluster;

import com.ficn.panel.model.entity.ObjectMeta;
import lombok.Data;

@Data
public class NamespaceSpecResponse {
    private String kind;
    private String apiVersion;
    private ObjectMeta metadata;
    private NamespaceStatus status;

    @Data
    public static class NamespaceStatus {
        private String phase;
    }
}
