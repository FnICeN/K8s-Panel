package com.ficn.panel.model.dto.cluster;

import java.util.List;
import java.util.Map;

public record GatekeeperPoliciesResponse(
        List<Map<String, Object>> templates,
        List<Constraint> constraints,
        List<Mutation> mutations) {
    public record Constraint(String resource, Map<String, Object> object) {}
    public record Mutation(String resource, Map<String, Object> object) {}
}
