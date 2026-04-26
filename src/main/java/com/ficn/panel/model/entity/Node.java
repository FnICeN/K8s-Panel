package com.ficn.panel.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Node {
    private ObjectMeta metadata;
    private NodeStatus status;

    @Data
    public static class NodeStatus {
        private List<Condition> conditions;
        private List<Address> addresses;
        private Capacity capacity;
        private Allocatable allocatable;
        private NodeInfo nodeInfo;

        @Data
        public static class Condition {
            private String type;
            private String status;
            private String message;
        }

        @Data
        public static class Address {
            private String type;
            private String address;
        }

        @Data
        public static class Capacity {
            private String cpu;
            @JsonProperty("ephemeral-storage")
            private String ephemeralStorage;
            private String memory;
            private String pods;
        }

        @Data
        public static class Allocatable {
            private String cpu;
            @JsonProperty("ephemeral-storage")
            private String ephemeralStorage;
            private String memory;
            private String pods;
        }

        @Data
        public static class NodeInfo {
            private String osImage;
            private String kubeletVersion;
            private String architecture;
            private String kernelVersion;
        }
    }
}

