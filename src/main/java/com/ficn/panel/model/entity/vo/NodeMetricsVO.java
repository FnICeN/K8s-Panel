package com.ficn.panel.model.entity.vo;

import lombok.Data;

@Data
public class NodeMetricsVO {

    private MetadataVO metadata;
    private UsageVO usage;
    private String timestamp;
    private String window;
    /**
     * metrics: metrics-server 实时用量；kubelet: kubelet stats；allocated: Pod 请求量汇总
     */
    private String source;

    @Data
    public static class MetadataVO {
        private String name;
    }

    @Data
    public static class UsageVO {
        private String cpu;
        private String memory;
    }
}
