package com.ficn.panel.model.dto.cluster;

import com.ficn.panel.model.entity.Deployment;
import com.ficn.panel.model.entity.ObjectMeta;
import lombok.Data;

@Data
public class DeploymentSpecResponse {
    private String kind;
    private String apiVersion;
    private ObjectMeta metadata;
    private Deployment.DeploymentSpec spec;
    private Deployment.DeploymentStatus status;
}
