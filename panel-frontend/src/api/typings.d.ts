declare namespace API {
  type Address = {
    type?: string
    address?: string
  }

  type AddressVO = {
    type?: string
    address?: string
  }

  type Allocatable = {
    cpu?: string
    memory?: string
    pods?: string
    'ephemeral-storage'?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    msg?: string
  }

  type BaseResponseDeploymentSpecResponse = {
    code?: number
    data?: DeploymentSpecResponse
    msg?: string
  }

  type BaseResponseDeviceCodeResponse = {
    code?: number
    data?: DeviceCodeResponse
    msg?: string
  }

  type BaseResponseK8sListResponseDeploymentVO = {
    code?: number
    data?: K8sListResponseDeploymentVO
    msg?: string
  }

  type BaseResponseK8sListResponseEventVO = {
    code?: number
    data?: K8sListResponseEventVO
    msg?: string
  }

  type BaseResponseK8sListResponseNamespaceVO = {
    code?: number
    data?: K8sListResponseNamespaceVO
    msg?: string
  }

  type BaseResponseK8sListResponseNodeVO = {
    code?: number
    data?: K8sListResponseNodeVO
    msg?: string
  }

  type BaseResponseK8sListResponseNodeMetricsVO = {
    code?: number
    data?: K8sListResponseNodeMetricsVO
    msg?: string
  }

  type BaseResponseK8sListResponsePodVO = {
    code?: number
    data?: K8sListResponsePodVO
    msg?: string
  }

  type BaseResponseK8sListResponseServiceVO = {
    code?: number
    data?: K8sListResponseServiceVO
    msg?: string
  }

  type BaseResponseNamespaceSpecResponse = {
    code?: number
    data?: NamespaceSpecResponse
    msg?: string
  }

  type BaseResponseNodeSpecResponse = {
    code?: number
    data?: NodeSpecResponse
    msg?: string
  }

  type BaseResponsePodSpecResponse = {
    code?: number
    data?: PodSpecResponse
    msg?: string
  }

  type BaseResponsePollTokenResponse = {
    code?: number
    data?: PollTokenResponse
    msg?: string
  }

  type BaseResponseServiceSpecResponse = {
    code?: number
    data?: ServiceSpecResponse
    msg?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    msg?: string
  }

  type Capacity = {
    cpu?: string
    memory?: string
    pods?: string
    'ephemeral-storage'?: string
  }

  type Condition = {
    type?: string
    status?: string
    message?: string
  }

  type ConditionVO = {
    type?: string
    status?: string
  }

  type Container = {
    name?: string
    image?: string
    args?: string[]
    ports?: Port[]
  }

  type ContainerStatus = {
    ready?: boolean
    restartCount?: number
    state?: Record<string, any>
  }

  type CreateDeploymentRequest = {
    yaml?: string
  }

  type CreateNamespaceRequest = {
    yaml?: string
  }

  type CreatePodRequest = {
    yaml?: string
  }

  type CreateServiceRequest = {
    yaml?: string
  }

  type DeploymentCondition = {
    type?: string
    status?: string
    lastUpdateTime?: string
    lastTransitionTime?: string
    reason?: string
    message?: string
  }

  type DeploymentSpec = {
    replicas?: number
    selector?: LabelSelector
    strategy?: Record<string, any>
    template?: Record<string, any>
    minReadySeconds?: number
    revisionHistoryLimit?: number
    progressDeadlineSeconds?: number
    paused?: boolean
  }

  type DeploymentSpecResponse = {
    kind?: string
    apiVersion?: string
    metadata?: ObjectMeta
    spec?: DeploymentSpec
    status?: DeploymentStatus
  }

  type DeploymentStatus = {
    observedGeneration?: number
    replicas?: number
    updatedReplicas?: number
    readyReplicas?: number
    availableReplicas?: number
    unavailableReplicas?: number
    collisionCount?: number
    conditions?: DeploymentCondition[]
  }

  type DeploymentVO = {
    metadata?: MetadataVO
    spec?: DeploymentVOSpecVO
    status?: DeploymentVOStatusVO
  }

  type DeploymentVOSpecVO = {
    replicas?: number
    selector?: { matchLabels?: Record<string, any> }
  }

  type DeploymentVOStatusVO = {
    replicas?: number
    updatedReplicas?: number
    readyReplicas?: number
    availableReplicas?: number
    unavailableReplicas?: number
    conditions?: DeploymentCondition[]
  }

  type deleteDeploymentParams = {
    namespace: string
    deployment_name: string
  }

  type deleteNamespaceParams = {
    name: string
  }

  type deleteServiceParams = {
    namespace: string
    service_name: string
  }

  type DeviceCodeResponse = {
    interval?: number
    device_code?: string
    user_code?: string
    verification_uri?: string
    verification_uri_complete?: string
    expires_in?: number
  }

  type EventVO = {
    metadata?: MetadataVO
    type?: string
    reason?: string
    message?: string
    count?: number
    firstTimestamp?: string
    lastTimestamp?: string
    eventTime?: string
    involvedObject?: InvolvedObjectVO
  }

  type getDeploymentAllParams = {
    namespace: string
    deployment_name: string
  }

  type getDeploymentEventsParams = {
    namespace: string
    deployment_name: string
  }

  type getDeploymentLogParams = {
    namespace: string
    deployment_name: string
    podName: string
    container?: string
    tailLines?: number
  }

  type getDeploymentParams = {
    namespace: string
    deployment_name: string
  }

  type getDeploymentPodsParams = {
    namespace: string
    deployment_name: string
  }

  type getDeploymentsByNamespaceParams = {
    namespace: string
  }

  type getNodeAllParams = {
    node_name: string
  }

  type getNodeParams = {
    node_name: string
  }

  type getPodAllParams = {
    namespace: string
    pod_name: string
  }

  type getPodParams = {
    namespace: string
    pod_name: string
  }

  type getPodsAllParams = {
    namespace: string
  }

  type getServiceAllParams = {
    namespace: string
    service_name: string
  }

  type getServiceParams = {
    namespace: string
    service_name: string
  }

  type getServicesAllParams = {
    namespace: string
  }

  type InvolvedObjectVO = {
    kind?: string
    namespace?: string
    name?: string
    uid?: string
    apiVersion?: string
    resourceVersion?: string
    fieldPath?: string
  }

  type K8sListResponseDeploymentVO = {
    kind?: string
    apiVersion?: string
    items?: DeploymentVO[]
  }

  type K8sListResponseEventVO = {
    kind?: string
    apiVersion?: string
    items?: EventVO[]
  }

  type K8sListResponseNamespaceVO = {
    kind?: string
    apiVersion?: string
    items?: NamespaceVO[]
  }

  type K8sListResponseNodeVO = {
    kind?: string
    apiVersion?: string
    items?: NodeVO[]
  }

  type K8sListResponseNodeMetricsVO = {
    kind?: string
    apiVersion?: string
    items?: NodeMetricsVO[]
  }

  type K8sListResponsePodVO = {
    kind?: string
    apiVersion?: string
    items?: PodVO[]
  }

  type K8sListResponseServiceVO = {
    kind?: string
    apiVersion?: string
    items?: ServiceVO[]
  }

  type LabelSelector = {
    matchLabels?: Record<string, any>
    matchExpressions?: Array<{ key?: string; operator?: string; values?: string[] }>
  }

  type LoadBalancerIngress = {
    ip?: string
    hostname?: string
    ipMode?: string
    ports?: PortStatus[]
  }

  type LoadBalancerIngressVO = {
    ip?: string
    hostname?: string
  }

  type LoadBalancerStatus = {
    ingress?: LoadBalancerIngress[]
  }

  type LoadBalancerStatusVO = {
    ingress?: LoadBalancerIngressVO[]
  }

  type MetadataVO = {
    name?: string
    namespace?: string
    creationTimestamp?: string
    labels?: Record<string, any>
  }

  type NamespaceSpecResponse = {
    kind?: string
    apiVersion?: string
    metadata?: ObjectMeta
    status?: NamespaceStatus
  }

  type NamespaceStatus = {
    phase?: string
  }

  type NamespaceVO = {
    metadata?: MetadataVO
    status?: StatusVO
  }

  type NodeInfo = {
    osImage?: string
    kubeletVersion?: string
    architecture?: string
    kernelVersion?: string
  }

  type NodeInfoVO = {
    kubeletVersion?: string
  }

  type NodeSpec = {
    podCIDR?: string
  }

  type NodeSpecResponse = {
    kind?: string
    apiVersion?: string
    metadata?: ObjectMeta
    spec?: NodeSpec
    status?: NodeStatus
  }

  type NodeStatus = {
    conditions?: Condition[]
    addresses?: Address[]
    capacity?: Capacity
    allocatable?: Allocatable
    nodeInfo?: NodeInfo
  }

  type NodeStatusVO = {
    conditions?: ConditionVO[]
    addresses?: AddressVO[]
    capacity?: Capacity
    allocatable?: Allocatable
    nodeInfo?: NodeInfoVO
  }

  type NodeMetricsVO = {
    metadata?: MetadataVO
    usage?: {
      cpu?: string
      memory?: string
    }
    timestamp?: string
    window?: string
    source?: 'metrics' | 'kubelet' | 'allocated'
  }

  type NodeVO = {
    metadata?: MetadataVO
    status?: NodeStatusVO
  }

  type ObjectMeta = {
    name?: string
    uid?: string
    creationTimestamp?: string
    namespace?: string
    labels?: Record<string, any>
    annotations?: Record<string, any>
  }

  type PodSpec = {
    nodeName?: string
    containers?: Container[]
  }

  type PodSpecResponse = {
    kind?: string
    apiVersion?: string
    metadata?: ObjectMeta
    spec?: PodSpec
    status?: PodStatus
  }

  type PodStatus = {
    phase?: string
    containerStatuses?: ContainerStatus[]
    hostIP?: string
    podIP?: string
    startTime?: string
  }

  type PodVO = {
    metadata?: MetadataVO
    spec?: SpecVO
    status?: StatusVO
  }

  type PollTokenResponse = {
    scope?: string
    access_token?: string
    expires_in?: number
    refresh_expires_in?: number
    refresh_token?: string
    token_type?: string
    id_token?: string
    'not-before-policy'?: number
    session_state?: string
  }

  type Port = {
    name?: string
    containerPort?: number
    protocol?: string
  }

  type PortStatus = {
    port?: number
    protocol?: string
    error?: string
  }

  type ServicePort = {
    name?: string
    protocol?: string
    port?: number
    targetPort?: Record<string, any>
    nodePort?: number
    appProtocol?: string
  }

  type ServicePortVO = {
    name?: string
    protocol?: string
    port?: number
    targetPort?: Record<string, any>
    nodePort?: number
  }

  type ServiceSpec = {
    type?: string
    clusterIP?: string
    clusterIPs?: string[]
    externalIPs?: string[]
    loadBalancerIP?: string
    loadBalancerSourceRanges?: string[]
    loadBalancerClass?: string
    ports?: ServicePort[]
    selector?: Record<string, any>
    sessionAffinity?: string
    externalName?: string
    externalTrafficPolicy?: string
    internalTrafficPolicy?: string
  }

  type ServiceSpecResponse = {
    kind?: string
    apiVersion?: string
    metadata?: ObjectMeta
    spec?: ServiceSpec
    status?: ServiceStatus
  }

  type ServiceStatus = {
    loadBalancer?: LoadBalancerStatus
  }

  type ServiceVO = {
    metadata?: MetadataVO
    spec?: SpecVO
    status?: StatusVO
  }

  type SpecVO = {
    nodeName?: string
    type?: string
    clusterIP?: string
    externalIPs?: string[]
    loadBalancerIP?: string
    externalName?: string
    ports?: ServicePortVO[]
  }

  type StatusVO = {
    phase?: string
    containerStatuses?: ContainerStatus[]
    podIP?: string
    loadBalancer?: LoadBalancerStatusVO
  }
}
