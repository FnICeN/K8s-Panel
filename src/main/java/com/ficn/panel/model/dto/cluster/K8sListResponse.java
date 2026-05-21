package com.ficn.panel.model.dto.cluster;

import lombok.Data;

import java.util.List;

@Data
public class K8sListResponse<T> {
    private String kind;
    private String apiVersion;
    private List<T> items;
}
