package com.ficn.panel.controller;

import com.ficn.panel.common.BaseResponse;
import com.ficn.panel.common.ResultUtils;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.exception.ThrowUtils;
import com.ficn.panel.model.dto.cluster.*;
import com.ficn.panel.model.entity.User;
import com.ficn.panel.service.cluster.NamespaceService;
import com.ficn.panel.service.cluster.NodeService;
import com.ficn.panel.service.cluster.PodService;
import com.ficn.panel.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cluster")
public class ClusterController {

    @Resource
    private UserService userService;
    @Resource
    private NodeService nodeService;
    @Resource
    private NamespaceService NamespaceService;
    @Resource
    private PodService podService;

    @GetMapping("/nodes")
    @Operation(summary = "获取所有节点")
    public BaseResponse<NodeListResponse> getNodes(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        NodeListResponse nodes = nodeService.getNodes(user.getIdToken());
        ThrowUtils.throwIf(nodes == null, ErrorCode.OPERATION_ERROR, "获取节点列表失败");
        return ResultUtils.success(nodes);
    }

    @GetMapping(value = "/nodes/{node_name}", produces = "application/json", params = "!all")
    @Operation(summary = "获取某个节点部分信息")
    public BaseResponse<NodeSpecResponse> getNode(@PathVariable("node_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        NodeSpecResponse nodeSpec = nodeService.getNodeSpec(user.getIdToken(), name);
        ThrowUtils.throwIf(nodeSpec == null, ErrorCode.NOT_FOUND_ERROR, "获取节点详情失败，节点可能不存在");
        return ResultUtils.success(nodeSpec);
    }

    @GetMapping(value = "/nodes/{node_name}/all", produces = "application/json")
    @Operation(summary = "获取某个节点全部信息，yaml形式")
    public BaseResponse<String> getNodeAll(@PathVariable("node_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String nodeAllSpec = nodeService.getNodeAllSpec(user.getIdToken(), name);
        ThrowUtils.throwIf(nodeAllSpec == null, ErrorCode.NOT_FOUND_ERROR, "获取节点详情失败，节点可能不存在");
        return ResultUtils.success(nodeAllSpec);
    }

    @GetMapping(value="/namespaces", produces = "application/json")
    @Operation(summary = "获取所有命名空间")
    public BaseResponse<NamespacesListResponse> getNamespaces(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        NamespacesListResponse namespaces = NamespaceService.getNamespaces(user.getIdToken());
        ThrowUtils.throwIf(namespaces == null, ErrorCode.OPERATION_ERROR, "获取命名空间列表失败");
        return ResultUtils.success(namespaces);
    }

    @GetMapping(value="/pods/{namespace}/all", produces = "application/json")
    @Operation(summary = "获取某个命名空间下所有pod")
    public BaseResponse<PodListResponse> getPodsAll(@PathVariable("namespace") String namespace, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        PodListResponse pods = podService.getPods(user.getIdToken(), namespace);
        ThrowUtils.throwIf(pods == null, ErrorCode.NOT_FOUND_ERROR, "获取pod列表失败，命名空间可能不存在");
        return ResultUtils.success(pods);
    }

    @GetMapping(value="/pods/{namespace}/{pod_name}", produces = "application/json")
    @Operation(summary = "获取某个pod")
    public BaseResponse<PodSpecResponse> getPod(@PathVariable("namespace") String namespace, @PathVariable("pod_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        PodSpecResponse pod = podService.getPod(user.getIdToken(), namespace, name);
        ThrowUtils.throwIf(pod == null, ErrorCode.NOT_FOUND_ERROR, "获取pod详情失败，pod可能不存在");
        return ResultUtils.success(pod);
    }
}
