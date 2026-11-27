package com.ficn.panel.model.dto.cluster;

import java.util.Map;

public record GatekeeperAnnotationResponse(Map<String, Object> object, String yaml) {}
