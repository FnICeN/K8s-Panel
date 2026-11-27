package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.ConfigMapApi;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.exception.ThrowUtils;
import com.ficn.panel.model.dto.cluster.ConfigMapCreateResponse;
import com.ficn.panel.parser.K8sResourceYamlParser;
import com.ficn.panel.service.cluster.ConfigMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigMapServiceImpl implements ConfigMapService {
    private final ConfigMapApi configMapApi;

    @Override
    public ConfigMapCreateResponse createConfigMap(String token, String yaml, boolean dryRun) {
        K8sResourceYamlParser.ParsedResource resource =
                K8sResourceYamlParser.parseNamespaced(yaml, "ConfigMap", "v1");
        Map<String, Object> created = configMapApi.create(token, resource.namespace(), resource.body(), dryRun);
        ThrowUtils.throwIf(created == null, ErrorCode.OPERATION_ERROR, "创建 ConfigMap 未返回资源");

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        String outputYaml = new Yaml(options).dumpAsMap(created);
        return new ConfigMapCreateResponse(created, outputYaml);
    }
}
