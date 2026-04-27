package com.ficn.panel.service.cluster;

import com.ficn.panel.model.dto.cluster.NamespacesListResponse;

public interface NamespaceService {
    NamespacesListResponse getNamespaces(String token);
}
