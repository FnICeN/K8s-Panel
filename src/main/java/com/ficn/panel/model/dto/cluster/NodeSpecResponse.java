package com.ficn.panel.model.dto.cluster;

import com.ficn.panel.model.entity.Node;
import com.ficn.panel.model.entity.ObjectMeta;
import lombok.Data;

@Data
public class NodeSpecResponse {
    private String kind;
    private String apiVersion;
    private ObjectMeta metadata;
    private NodeSpec spec;
    private Node.NodeStatus status;

    @Data
    public static class NodeSpec {
        private String podCIDR;
    }
}
