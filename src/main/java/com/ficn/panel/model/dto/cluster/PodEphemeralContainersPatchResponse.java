package com.ficn.panel.model.dto.cluster;

import java.util.Map;

public record PodEphemeralContainersPatchResponse(Map<String, Object> resource, String yaml) {
}
