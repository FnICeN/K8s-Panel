package com.ficn.panel.model.dto.cluster;

import com.ficn.panel.model.entity.vo.NamespaceVO;
import lombok.Data;

import java.util.List;

@Data
public class NamespacesListResponse {
    private String kind;
    private String apiVersion;
    private List<NamespaceVO> items;
}
