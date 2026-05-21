package com.ficn.panel.service.cluster;

import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.ServiceSpecResponse;
import com.ficn.panel.model.entity.vo.ServiceVO;

public interface ServiceService {
    K8sListResponse<ServiceVO> getServices(String token, String namespace);
    ServiceSpecResponse getService(String token, String namespace, String serviceName);
    String getServiceAllSpec(String token, String namespace, String serviceName);
    void deleteService(String token, String namespace, String serviceName);
}
