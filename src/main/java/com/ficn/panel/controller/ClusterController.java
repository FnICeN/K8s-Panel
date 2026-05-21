package com.ficn.panel.controller;

import com.ficn.panel.common.BaseResponse;
import com.ficn.panel.common.ResultUtils;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.exception.ThrowUtils;
import com.ficn.panel.model.dto.cluster.*;
import com.ficn.panel.model.entity.User;
import com.ficn.panel.model.entity.vo.NamespaceVO;
import com.ficn.panel.model.entity.vo.NodeVO;
import com.ficn.panel.model.entity.vo.PodVO;
import com.ficn.panel.model.entity.vo.ServiceVO;
import com.ficn.panel.service.cluster.NamespaceService;
import com.ficn.panel.service.cluster.NodeService;
import com.ficn.panel.service.cluster.PodService;
import com.ficn.panel.service.cluster.ServiceService;
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
    @Resource
    private ServiceService serviceService;

    @GetMapping("/nodes")
    @Operation(summary = "获取所有节点")
    public BaseResponse<K8sListResponse<NodeVO>> getNodes(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<NodeVO> nodes = nodeService.getNodes(user.getIdToken());
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
    public BaseResponse<K8sListResponse<NamespaceVO>> getNamespaces(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<NamespaceVO> namespaces = NamespaceService.getNamespaces(user.getIdToken());
        ThrowUtils.throwIf(namespaces == null, ErrorCode.OPERATION_ERROR, "获取命名空间列表失败");
        return ResultUtils.success(namespaces);
    }

    @GetMapping(value="/pods/{namespace}", produces = "application/json")
    @Operation(summary = "获取某个命名空间下所有pod")
    public BaseResponse<K8sListResponse<PodVO>> getPodsAll(@PathVariable("namespace") String namespace, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<PodVO> pods = podService.getPods(user.getIdToken(), namespace);
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

    @GetMapping(value="/pods/{namespace}/{pod_name}/all", produces = "application/json")
    @Operation(summary = "获取某个pod全部信息，yaml形式")
    public BaseResponse<String> getPodAll(@PathVariable("namespace") String namespace, @PathVariable("pod_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String podAllSpec = podService.getPodAllSpec(user.getIdToken(), namespace, name);
        ThrowUtils.throwIf(podAllSpec == null, ErrorCode.NOT_FOUND_ERROR, "获取pod详情失败，pod可能不存在");
        return ResultUtils.success(podAllSpec);
    }

    @GetMapping(value="/services/{namespace}", produces = "application/json")
    @Operation(summary = "获取某个命名空间下所有service")
    public BaseResponse<K8sListResponse<ServiceVO>> getServicesAll(@PathVariable("namespace") String namespace, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<ServiceVO> services = serviceService.getServices(user.getIdToken(), namespace);
        ThrowUtils.throwIf(services == null, ErrorCode.NOT_FOUND_ERROR, "获取service列表失败，命名空间可能不存在");
        return ResultUtils.success(services);
    }

    @GetMapping(value="/services/{namespace}/{service_name}", produces = "application/json")
    @Operation(summary = "获取某个service")
    public BaseResponse<ServiceSpecResponse> getService(@PathVariable("namespace") String namespace, @PathVariable("service_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        ServiceSpecResponse service = serviceService.getService(user.getIdToken(), namespace, name);
        ThrowUtils.throwIf(service == null, ErrorCode.NOT_FOUND_ERROR, "获取service详情失败，service可能不存在");
        return ResultUtils.success(service);
    }

    @GetMapping(value="/services/{namespace}/{service_name}/all", produces = "application/json")
    @Operation(summary = "获取某个service全部信息，yaml形式")
    public BaseResponse<String> getServiceAll(@PathVariable("namespace") String namespace, @PathVariable("service_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String serviceAllSpec = serviceService.getServiceAllSpec(user.getIdToken(), namespace, name);
        ThrowUtils.throwIf(serviceAllSpec == null, ErrorCode.NOT_FOUND_ERROR, "获取service详情失败，service可能不存在");
        return ResultUtils.success(serviceAllSpec);
    }

    @DeleteMapping(value="/services/{namespace}/{service_name}", produces = "application/json")
    @Operation(summary = "删除某个service")
    public BaseResponse<Boolean> deleteService(@PathVariable("namespace") String namespace, @PathVariable("service_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        serviceService.deleteService(user.getIdToken(), namespace, name);
        return ResultUtils.success(true);
    }
}
