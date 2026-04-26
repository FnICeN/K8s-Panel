package com.ficn.panel.model.entity.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NodeVO {

    private MetadataVO metadata;
    private NodeStatusVO status;

    @Data
    public static class MetadataVO {
        private String name;
        private String creationTimestamp;
        private Map<String, String> labels;
    }

    @Data
    public static class NodeStatusVO {
        private List<ConditionVO> conditions;
        private List<AddressVO> addresses;
        private NodeInfoVO nodeInfo;
    }

    @Data
    public static class ConditionVO {
        private String type;
        private String status;
    }

    @Data
    public static class AddressVO {
        private String type;
        private String address;
    }

    @Data
    public static class NodeInfoVO {
        private String kubeletVersion;
    }
}
