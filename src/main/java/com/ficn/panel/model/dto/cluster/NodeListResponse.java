package com.ficn.panel.model.dto.cluster;

import com.ficn.panel.model.entity.Node;
import lombok.Data;

import java.util.List;

@Data
public class NodeListResponse {
    private String kind;
    private String apiVersion;
    private List<Node> items;
}
