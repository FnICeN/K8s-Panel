package com.ficn.panel.controller;

import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.exception.ThrowUtils;
import com.ficn.panel.model.dto.cluster.NodeListResponse;
import com.ficn.panel.service.cluster.NodeService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cluster")
public class ClusterController {
    @Resource
    private NodeService nodeService;

    @GetMapping("/nodes")
    public NodeListResponse getNodes(HttpSession  session) {
        String token = (String) session.getAttribute("id_token");
        NodeListResponse nodes = nodeService.getNodes(token);
        ThrowUtils.throwIf(nodes == null, ErrorCode.OPERATION_ERROR, "获取节点列表失败");
        return nodes;
    }
}
