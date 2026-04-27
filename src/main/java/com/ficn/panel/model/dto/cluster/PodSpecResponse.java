package com.ficn.panel.model.dto.cluster;

import com.ficn.panel.model.entity.ObjectMeta;
import com.ficn.panel.model.entity.Pod;
import lombok.Data;

@Data
public class PodSpecResponse {
    private String kind;
    private String apiVersion;
    private ObjectMeta metadata;
    private Pod.PodSpec spec;
    private Pod.PodStatus status;
}
