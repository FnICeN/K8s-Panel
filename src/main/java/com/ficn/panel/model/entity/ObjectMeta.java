package com.ficn.panel.model.entity;

import lombok.Data;

import java.util.Map;

@Data
public class ObjectMeta {
    private String name;
    private String uid;
    private String creationTimestamp;
    private String namespace;
    private Map<String, String> labels;
    private Map<String, String> annotations;
}
