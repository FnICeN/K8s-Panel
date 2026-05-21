package com.ficn.panel.model.entity.vo;

import lombok.Data;

import java.util.List;

@Data
public class ServiceVO {
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
        private String type;
        private String clusterIP;
        private List<String> externalIPs;
        private String loadBalancerIP;
        private String externalName;
        private List<ServicePortVO> ports;

        @Data
        public static class ServicePortVO {
            private String name;
            private String protocol;
            private Integer port;
            private Object targetPort;
            private Integer nodePort;
        }
    }

    @Data
    public static class StatusVO {
        private LoadBalancerStatusVO loadBalancer;

        @Data
        public static class LoadBalancerStatusVO {
            private List<LoadBalancerIngressVO> ingress;

            @Data
            public static class LoadBalancerIngressVO {
                private String ip;
                private String hostname;
            }
        }
    }
}
