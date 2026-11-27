package com.ficn.panel.service.cluster;

import com.ficn.panel.model.dto.cluster.ConfigMapCreateResponse;

public interface ConfigMapService {
    ConfigMapCreateResponse createConfigMap(String token, String yaml, boolean dryRun);
}
