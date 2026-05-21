package com.ficn.panel.model.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Service {
    private ObjectMeta metadata;
    private ServiceSpec spec;
    private ServiceStatus status;

    @Data
    public static class ServiceSpec {
        private String type;
        private String clusterIP;
        private List<String> clusterIPs;
        private List<String> externalIPs;
        private String loadBalancerIP;
        private List<String> loadBalancerSourceRanges;
        private String loadBalancerClass;
        private List<ServicePort> ports;
        private Map<String, String> selector;
        private String sessionAffinity;
        private String externalName;
        private String externalTrafficPolicy;
        private String internalTrafficPolicy;

        @Data
        public static class ServicePort {
            private String name;
            private String protocol;
            private Integer port;
            private Object targetPort;
            private Integer nodePort;
            private String appProtocol;
        }
    }

    @Data
    public static class ServiceStatus {
        private LoadBalancerStatus loadBalancer;

        @Data
        public static class LoadBalancerStatus {
            private List<LoadBalancerIngress> ingress;

            @Data
            public static class LoadBalancerIngress {
                private String ip;
                private String hostname;
                private String ipMode;
                private List<PortStatus> ports;

                @Data
                public static class PortStatus {
                    private Integer port;
                    private String protocol;
                    private String error;
                }
            }
        }
    }
}
