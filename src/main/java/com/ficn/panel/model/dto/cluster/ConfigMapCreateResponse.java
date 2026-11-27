package com.ficn.panel.model.dto.cluster;

import java.util.Map;

public record ConfigMapCreateResponse(Map<String, Object> resource, String yaml) {
}
