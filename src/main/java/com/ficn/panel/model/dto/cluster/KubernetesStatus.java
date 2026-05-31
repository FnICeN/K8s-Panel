package com.ficn.panel.model.dto.cluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesStatus {

    private String kind;

    private String apiVersion;

    private String status;

    private String message;

    private String reason;

    private Integer code;
}
