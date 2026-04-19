package com.ficn.panel.model.entity;

import lombok.Data;

@Data
public class Node {
    private Metadata metadata;
}

@Data
class Metadata {
    private String name;
}
