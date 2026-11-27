package com.ficn.panel.controller;

import com.ficn.panel.common.BaseResponse;
import com.ficn.panel.common.ResultUtils;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.exception.ThrowUtils;
import com.ficn.panel.model.dto.cluster.*;
import com.ficn.panel.model.entity.User;
import com.ficn.panel.model.entity.vo.DeploymentVO;
import com.ficn.panel.model.entity.vo.EventVO;
import com.ficn.panel.model.entity.vo.NamespaceVO;
import com.ficn.panel.model.entity.vo.NodeMetricsVO;
import com.ficn.panel.model.entity.vo.NodeVO;
import com.ficn.panel.model.entity.vo.PodVO;
import com.ficn.panel.model.entity.vo.RbacResourceVO;
import com.ficn.panel.model.entity.vo.ServiceVO;
import com.ficn.panel.service.cluster.DeploymentService;
import com.ficn.panel.service.cluster.ConfigMapService;
import com.ficn.panel.service.cluster.NamespaceService;
import com.ficn.panel.service.cluster.NodeService;
import com.ficn.panel.service.cluster.PodService;
import com.ficn.panel.service.cluster.RbacService;
import com.ficn.panel.service.cluster.GatekeeperService;
import com.ficn.panel.service.cluster.ServiceService;
import com.ficn.panel.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    @Resource
    private DeploymentService deploymentService;
    @Resource
    private ConfigMapService configMapService;
    @Resource
    private RbacService rbacService;
    @Resource
    private GatekeeperService gatekeeperService;

    @GetMapping("/gatekeeper/policies")
    @Operation(summary = "列出 Gatekeeper 策略模板和全部约束实例")
    public BaseResponse<GatekeeperPoliciesResponse> getGatekeeperPolicies(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        return ResultUtils.success(gatekeeperService.policies(user.getIdToken()));
    }

    @PatchMapping("/gatekeeper/constraints/{resource}/{name}/annotations/preview")
    @Operation(summary = "服务端试运行 Gatekeeper 约束注解更新")
    public BaseResponse<GatekeeperAnnotationResponse> previewGatekeeperAnnotation(
            @PathVariable String resource, @PathVariable String name,
            @RequestBody GatekeeperAnnotationRequest body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        ThrowUtils.throwIf(body == null, ErrorCode.PARAMS_ERROR, "注解内容不能为空");
        return ResultUtils.success(gatekeeperService.previewAnnotation(user.getIdToken(), resource, name,
                body.key(), body.value()));
    }

    @GetMapping("/roles")
    @Operation(summary = "查询 Role 列表，可按命名空间筛选")
    public BaseResponse<K8sListResponse<RbacResourceVO>> getRoles(
            @RequestParam(required = false) String namespace, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<RbacResourceVO> result = rbacService.roles(user.getIdToken(), namespace);
        ThrowUtils.throwIf(result == null, ErrorCode.OPERATION_ERROR, "获取 Role 列表失败");
        return ResultUtils.success(result);
    }

    @PostMapping("/roles")
    @Operation(summary = "创建 Role，支持服务端试运行")
    public BaseResponse<RbacResourceVO> createRole(@RequestBody(required = false) CreateRoleRequest body,
                                                   @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun,
                                                   HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        RbacResourceVO result = rbacService.createRole(user.getIdToken(), body == null ? null : body.getYaml(), dryRun);
        ThrowUtils.throwIf(result == null, ErrorCode.OPERATION_ERROR, "创建 Role 未返回资源");
        return ResultUtils.success(result);
    }

    @GetMapping("/roles/{namespace}/{name}")
    @Operation(summary = "查询 Role 详情")
    public BaseResponse<RbacResourceVO> getRole(@PathVariable String namespace, @PathVariable String name,
                                                HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        RbacResourceVO result = rbacService.role(user.getIdToken(), namespace, name);
        ThrowUtils.throwIf(result == null, ErrorCode.NOT_FOUND_ERROR, "Role 不存在");
        return ResultUtils.success(result);
    }

    @GetMapping("/clusterroles")
    @Operation(summary = "查询 ClusterRole 列表")
    public BaseResponse<K8sListResponse<RbacResourceVO>> getClusterRoles(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<RbacResourceVO> result = rbacService.clusterRoles(user.getIdToken());
        ThrowUtils.throwIf(result == null, ErrorCode.OPERATION_ERROR, "获取 ClusterRole 列表失败");
        return ResultUtils.success(result);
    }

    @GetMapping("/clusterroles/{name}")
    @Operation(summary = "查询 ClusterRole 详情")
    public BaseResponse<RbacResourceVO> getClusterRole(@PathVariable String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        RbacResourceVO result = rbacService.clusterRole(user.getIdToken(), name);
        ThrowUtils.throwIf(result == null, ErrorCode.NOT_FOUND_ERROR, "ClusterRole 不存在");
        return ResultUtils.success(result);
    }

    @GetMapping("/rolebindings")
    @Operation(summary = "查询 RoleBinding 列表，可按命名空间筛选")
    public BaseResponse<K8sListResponse<RbacResourceVO>> getRoleBindings(
            @RequestParam(required = false) String namespace, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<RbacResourceVO> result = rbacService.roleBindings(user.getIdToken(), namespace);
        ThrowUtils.throwIf(result == null, ErrorCode.OPERATION_ERROR, "获取 RoleBinding 列表失败");
        return ResultUtils.success(result);
    }

    @GetMapping("/rolebindings/{namespace}/{name}")
    @Operation(summary = "查询 RoleBinding 详情")
    public BaseResponse<RbacResourceVO> getRoleBinding(@PathVariable String namespace, @PathVariable String name,
                                                       HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        RbacResourceVO result = rbacService.roleBinding(user.getIdToken(), namespace, name);
        ThrowUtils.throwIf(result == null, ErrorCode.NOT_FOUND_ERROR, "RoleBinding 不存在");
        return ResultUtils.success(result);
    }

    @PostMapping(value="/configmaps", produces = "application/json")
    @Operation(summary = "创建 ConfigMap，支持服务端试运行")
    public BaseResponse<ConfigMapCreateResponse> createConfigMap(
            @RequestBody(required = false) CreateConfigMapRequest createConfigMapRequest,
            @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun,
            HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String yaml = createConfigMapRequest == null ? null : createConfigMapRequest.getYaml();
        return ResultUtils.success(configMapService.createConfigMap(user.getIdToken(), yaml, dryRun));
    }

    @GetMapping("/nodes")
    @Operation(summary = "获取所有节点")
    public BaseResponse<K8sListResponse<NodeVO>> getNodes(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<NodeVO> nodes = nodeService.getNodes(user.getIdToken());
        ThrowUtils.throwIf(nodes == null, ErrorCode.OPERATION_ERROR, "获取节点列表失败");
        return ResultUtils.success(nodes);
    }

    @GetMapping("/node-metrics")
    @Operation(summary = "获取所有节点实时资源用量")
    public BaseResponse<K8sListResponse<NodeMetricsVO>> getNodeMetrics(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<NodeMetricsVO> metrics = nodeService.getNodeMetrics(user.getIdToken());
        return ResultUtils.success(metrics);
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

    @PostMapping(value="/namespaces", produces = "application/json")
    @Operation(summary = "创建Namespace")
    public BaseResponse<NamespaceSpecResponse> createNamespace(@RequestBody(required = false) CreateNamespaceRequest createNamespaceRequest, @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String yaml = createNamespaceRequest == null ? null : createNamespaceRequest.getYaml();
        NamespaceSpecResponse namespace = NamespaceService.createNamespace(user.getIdToken(), yaml, dryRun);
        return ResultUtils.success(namespace);
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

    @PostMapping(value="/pods", produces = "application/json")
    @Operation(summary = "创建Pod")
    public BaseResponse<PodSpecResponse> createPod(@RequestBody(required = false) CreatePodRequest createPodRequest, @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String yaml = createPodRequest == null ? null : createPodRequest.getYaml();
        PodSpecResponse pod = podService.createPod(user.getIdToken(), yaml, dryRun);
        return ResultUtils.success(pod);
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

    @GetMapping(value="/pods/{namespace}/{pod_name}/logs", produces = "application/json")
    @Operation(summary = "获取某个 Pod 的容器日志")
    public BaseResponse<String> getPodLog(@PathVariable("namespace") String namespace,
                                          @PathVariable("pod_name") String name,
                                          @RequestParam(value = "container", required = false) String container,
                                          @RequestParam(value = "tailLines", required = false) Integer tailLines,
                                          HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String logs = podService.getPodLog(user.getIdToken(), namespace, name, container, tailLines);
        ThrowUtils.throwIf(logs == null, ErrorCode.NOT_FOUND_ERROR, "获取 Pod 日志失败");
        return ResultUtils.success(logs);
    }

    @PatchMapping(value="/pods/{namespace}/{pod_name}/ephemeralcontainers", produces = "application/json")
    @Operation(summary = "补丁更新 Pod 临时容器，默认服务端试运行")
    public BaseResponse<PodEphemeralContainersPatchResponse> patchPodEphemeralContainers(
            @PathVariable("namespace") String namespace,
            @PathVariable("pod_name") String name,
            @RequestBody Map<String, Object> patch,
            @RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun,
            HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        return ResultUtils.success(podService.patchEphemeralContainers(
                user.getIdToken(), namespace, name, patch, dryRun));
    }

    @GetMapping(value="/deployments", produces = "application/json")
    @Operation(summary = "获取所有deployment")
    public BaseResponse<K8sListResponse<DeploymentVO>> getDeploymentsAll(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<DeploymentVO> deployments = deploymentService.getDeployments(user.getIdToken());
        ThrowUtils.throwIf(deployments == null, ErrorCode.OPERATION_ERROR, "获取deployment列表失败");
        return ResultUtils.success(deployments);
    }

    @PostMapping(value="/deployments", produces = "application/json")
    @Operation(summary = "创建Deployment")
    public BaseResponse<DeploymentSpecResponse> createDeployment(@RequestBody(required = false) CreateDeploymentRequest createDeploymentRequest, @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String yaml = createDeploymentRequest == null ? null : createDeploymentRequest.getYaml();
        DeploymentSpecResponse deployment = deploymentService.createDeployment(user.getIdToken(), yaml, dryRun);
        return ResultUtils.success(deployment);
    }

    @GetMapping(value="/deployments/{namespace}", produces = "application/json")
    @Operation(summary = "获取某个命名空间下所有deployment")
    public BaseResponse<K8sListResponse<DeploymentVO>> getDeploymentsByNamespace(@PathVariable("namespace") String namespace, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<DeploymentVO> deployments = deploymentService.getDeployments(user.getIdToken(), namespace);
        ThrowUtils.throwIf(deployments == null, ErrorCode.NOT_FOUND_ERROR, "获取deployment列表失败，命名空间可能不存在");
        return ResultUtils.success(deployments);
    }

    @GetMapping(value="/deployments/{namespace}/{deployment_name}", produces = "application/json")
    @Operation(summary = "获取某个deployment")
    public BaseResponse<DeploymentSpecResponse> getDeployment(@PathVariable("namespace") String namespace, @PathVariable("deployment_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        DeploymentSpecResponse deployment = deploymentService.getDeployment(user.getIdToken(), namespace, name);
        ThrowUtils.throwIf(deployment == null, ErrorCode.NOT_FOUND_ERROR, "获取deployment详情失败，deployment可能不存在");
        return ResultUtils.success(deployment);
    }

    @GetMapping(value="/deployments/{namespace}/{deployment_name}/all", produces = "application/json")
    @Operation(summary = "获取某个deployment全部信息，yaml形式")
    public BaseResponse<String> getDeploymentAll(@PathVariable("namespace") String namespace, @PathVariable("deployment_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String deploymentAllSpec = deploymentService.getDeploymentAllSpec(user.getIdToken(), namespace, name);
        ThrowUtils.throwIf(deploymentAllSpec == null, ErrorCode.NOT_FOUND_ERROR, "获取deployment详情失败，deployment可能不存在");
        return ResultUtils.success(deploymentAllSpec);
    }

    @GetMapping(value="/deployments/{namespace}/{deployment_name}/pods", produces = "application/json")
    @Operation(summary = "获取某个deployment对应的pod")
    public BaseResponse<K8sListResponse<PodVO>> getDeploymentPods(@PathVariable("namespace") String namespace, @PathVariable("deployment_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<PodVO> pods = deploymentService.getDeploymentPods(user.getIdToken(), namespace, name);
        ThrowUtils.throwIf(pods == null, ErrorCode.NOT_FOUND_ERROR, "获取deployment pod列表失败，deployment可能不存在");
        return ResultUtils.success(pods);
    }

    @GetMapping(value="/deployments/{namespace}/{deployment_name}/logs", produces = "application/json")
    @Operation(summary = "获取某个deployment下pod的日志")
    public BaseResponse<String> getDeploymentLog(@PathVariable("namespace") String namespace,
                                                 @PathVariable("deployment_name") String name,
                                                 @RequestParam("podName") String podName,
                                                 @RequestParam(value = "container", required = false) String container,
                                                 @RequestParam(value = "tailLines", required = false) Integer tailLines,
                                                 HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String logs = deploymentService.getDeploymentLog(user.getIdToken(), namespace, name, podName, container, tailLines);
        ThrowUtils.throwIf(logs == null, ErrorCode.NOT_FOUND_ERROR, "获取deployment pod日志失败");
        return ResultUtils.success(logs);
    }

    @GetMapping(value="/deployments/{namespace}/{deployment_name}/events", produces = "application/json")
    @Operation(summary = "获取某个deployment相关事件")
    public BaseResponse<K8sListResponse<EventVO>> getDeploymentEvents(@PathVariable("namespace") String namespace, @PathVariable("deployment_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        K8sListResponse<EventVO> events = deploymentService.getDeploymentEvents(user.getIdToken(), namespace, name);
        ThrowUtils.throwIf(events == null, ErrorCode.NOT_FOUND_ERROR, "获取deployment事件失败，deployment可能不存在");
        return ResultUtils.success(events);
    }

    @DeleteMapping(value="/deployments/{namespace}/{deployment_name}", produces = "application/json")
    @Operation(summary = "删除某个deployment")
    public BaseResponse<Boolean> deleteDeployment(@PathVariable("namespace") String namespace, @PathVariable("deployment_name") String name, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        deploymentService.deleteDeployment(user.getIdToken(), namespace, name);
        return ResultUtils.success(true);
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

    @PostMapping(value="/services", produces = "application/json")
    @Operation(summary = "创建Service")
    public BaseResponse<ServiceSpecResponse> createService(@RequestBody(required = false) CreateServiceRequest createServiceRequest, @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String yaml = createServiceRequest == null ? null : createServiceRequest.getYaml();
        ServiceSpecResponse service = serviceService.createService(user.getIdToken(), yaml, dryRun);
        return ResultUtils.success(service);
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
