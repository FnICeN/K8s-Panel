package com.ficn.panel.model.dto.cluster;

import com.ficn.panel.model.entity.ObjectMeta;
import com.ficn.panel.model.entity.Service;
import lombok.Data;

@Data
public class ServiceSpecResponse {
    private String kind;
    private String apiVersion;
    private ObjectMeta metadata;
    private Service.ServiceSpec spec;
    private Service.ServiceStatus status;
}
