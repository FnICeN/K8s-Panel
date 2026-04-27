package com.ficn.panel.model.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Pod {
    private ObjectMeta metadata;

    @Data
    public static class PodSpec {
        private String nodeName;
        private List<Container> containers;
        @Data
        public static class Container {
            private String name;
            private String image;
            private List<String> args;
            private List<Port> ports;
            @Data
            public static class Port {
                private String name;
                private Integer containerPort;
                private String protocol;
            }
        }
    }
    @Data
    public static class PodStatus {
        private String phase;
        private List<ContainerStatus> containerStatuses;
        private String hostIP;
        private String podIP;
        private String startTime;
        @Data
        public static class ContainerStatus {
            private Boolean ready;
            private Integer restartCount;
            private Map<String, Object> state;
        }
    }
}
