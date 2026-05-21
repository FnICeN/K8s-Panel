package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.ServiceApi;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.ServiceSpecResponse;
import com.ficn.panel.model.entity.vo.ServiceVO;
import com.ficn.panel.service.cluster.ServiceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ServiceServiceImpl implements ServiceService {
    @Resource
    private ServiceApi serviceApi;

    @Override
    public K8sListResponse<ServiceVO> getServices(String token, String namespace) {
        return serviceApi.listByNamespace(token, namespace);
    }

    @Override
    public ServiceSpecResponse getService(String token, String namespace, String serviceName) {
        return serviceApi.get(token, namespace, serviceName);
    }

    @Override
    public String getServiceAllSpec(String token, String namespace, String serviceName) {
        return serviceApi.getYaml(token, namespace, serviceName);
    }

    @Override
    public void deleteService(String token, String namespace, String serviceName) {
        serviceApi.delete(token, namespace, serviceName);
    }
}
