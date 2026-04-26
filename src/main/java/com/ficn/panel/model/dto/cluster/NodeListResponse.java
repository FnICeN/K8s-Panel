package com.ficn.panel.model.dto.cluster;

import com.ficn.panel.model.entity.vo.NodeVO;
import lombok.Data;

import java.util.List;

@Data
public class NodeListResponse {
    private String kind;
    private String apiVersion;
    private List<NodeVO> items;
}
