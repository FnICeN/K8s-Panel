package com.ficn.panel.model.entity.vo;

import com.ficn.panel.model.entity.ObjectMeta;
import lombok.Data;

import java.util.List;

@Data
public class RbacResourceVO {
    private String kind;
    private ObjectMeta metadata;
    private List<PolicyRuleVO> rules;
    private RoleRefVO roleRef;
    private List<SubjectVO> subjects;

    @Data
    public static class PolicyRuleVO {
        private List<String> apiGroups;
        private List<String> resources;
        private List<String> resourceNames;
        private List<String> nonResourceURLs;
        private List<String> verbs;
    }

    @Data
    public static class RoleRefVO {
        private String apiGroup;
        private String kind;
        private String name;
    }

    @Data
    public static class SubjectVO {
        private String kind;
        private String apiGroup;
        private String name;
        private String namespace;
    }
}
