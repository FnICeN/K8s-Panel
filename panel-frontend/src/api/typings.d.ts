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

  type BaseResponseDeviceCodeResponse = {
    code?: number
    data?: DeviceCodeResponse
    msg?: string
  }

  type BaseResponseNamespacesListResponse = {
    code?: number
    data?: NamespacesListResponse
    msg?: string
  }

  type BaseResponseNodeListResponse = {
    code?: number
    data?: NodeListResponse
    msg?: string
  }

  type BaseResponseNodeSpecResponse = {
    code?: number
    data?: NodeSpecResponse
    msg?: string
  }

  type BaseResponsePodListResponse = {
    code?: number
    data?: PodListResponse
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

  type ContainerStatusVO = {
    ready?: boolean
    restartCount?: number
  }

  type DeviceCodeResponse = {
    interval?: number
    device_code?: string
    user_code?: string
    verification_uri?: string
    verification_uri_complete?: string
    expires_in?: number
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

  type MetadataVO = {
    name?: string
    creationTimestamp?: string
    namespace?: string
  }

  type NamespacesListResponse = {
    kind?: string
    apiVersion?: string
    items?: NamespaceVO[]
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

  type NodeListResponse = {
    kind?: string
    apiVersion?: string
    items?: NodeVO[]
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
    nodeInfo?: NodeInfoVO
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

  type PodListResponse = {
    kind?: string
    apiVersion?: string
    items?: PodVO[]
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

  type SpecVO = {
    nodeName?: string
  }

  type StatusVO = {
    phase?: string
    containerStatuses?: ContainerStatusVO[]
    podIP?: string
  }
}
