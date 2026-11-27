// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** Merge Patch Pod 的 ephemeralcontainers 子资源 */
export async function patchPodEphemeralContainers(
  params: { namespace: string; pod_name: string; dryRun?: boolean },
  patch: Record<string, unknown>,
) {
  const { namespace, pod_name, dryRun = true } = params
  return request<{
    code?: number
    data?: { resource?: Record<string, unknown>; yaml?: string }
  }>(`/cluster/pods/${encodeURIComponent(namespace)}/${encodeURIComponent(pod_name)}/ephemeralcontainers`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    params: { dryRun },
    data: patch,
  })
}

/** 创建 ConfigMap POST /cluster/configmaps */
export async function createConfigMap(
  body: { yaml: string },
  options?: { [key: string]: any }
) {
  return request<{
    code?: number
    data?: { resource?: { metadata?: { name?: string; namespace?: string } }; yaml?: string }
  }>('/cluster/configmaps', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 获取所有deployment GET /cluster/deployments */
export async function getDeploymentsAll(options?: { [key: string]: any }) {
  return request<API.BaseResponseK8sListResponseDeploymentVO>('/cluster/deployments', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 创建Deployment POST /cluster/deployments */
export async function createDeployment(
  body: API.CreateDeploymentRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseDeploymentSpecResponse>('/cluster/deployments', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取某个命名空间下所有deployment GET /cluster/deployments/${param0} */
export async function getDeploymentsByNamespace(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDeploymentsByNamespaceParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, ...queryParams } = params
  return request<API.BaseResponseK8sListResponseDeploymentVO>(`/cluster/deployments/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 获取某个deployment GET /cluster/deployments/${param0}/${param1} */
export async function getDeployment(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDeploymentParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, deployment_name: param1, ...queryParams } = params
  return request<API.BaseResponseDeploymentSpecResponse>(
    `/cluster/deployments/${param0}/${param1}`,
    {
      method: 'GET',
      params: { ...queryParams },
      ...(options || {}),
    }
  )
}

/** 删除某个deployment DELETE /cluster/deployments/${param0}/${param1} */
export async function deleteDeployment(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteDeploymentParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, deployment_name: param1, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/cluster/deployments/${param0}/${param1}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 获取某个deployment全部信息，yaml形式 GET /cluster/deployments/${param0}/${param1}/all */
export async function getDeploymentAll(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDeploymentAllParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, deployment_name: param1, ...queryParams } = params
  return request<API.BaseResponseString>(`/cluster/deployments/${param0}/${param1}/all`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 获取某个deployment相关事件 GET /cluster/deployments/${param0}/${param1}/events */
export async function getDeploymentEvents(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDeploymentEventsParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, deployment_name: param1, ...queryParams } = params
  return request<API.BaseResponseK8sListResponseEventVO>(
    `/cluster/deployments/${param0}/${param1}/events`,
    {
      method: 'GET',
      params: { ...queryParams },
      ...(options || {}),
    }
  )
}

/** 获取某个deployment下pod的日志 GET /cluster/deployments/${param0}/${param1}/logs */
export async function getDeploymentLog(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDeploymentLogParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, deployment_name: param1, ...queryParams } = params
  return request<API.BaseResponseString>(`/cluster/deployments/${param0}/${param1}/logs`, {
    method: 'GET',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** 获取某个deployment对应的pod GET /cluster/deployments/${param0}/${param1}/pods */
export async function getDeploymentPods(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getDeploymentPodsParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, deployment_name: param1, ...queryParams } = params
  return request<API.BaseResponseK8sListResponsePodVO>(
    `/cluster/deployments/${param0}/${param1}/pods`,
    {
      method: 'GET',
      params: { ...queryParams },
      ...(options || {}),
    }
  )
}

/** 获取所有命名空间 GET /cluster/namespaces */
export async function getNamespaces(options?: { [key: string]: any }) {
  return request<API.BaseResponseK8sListResponseNamespaceVO>('/cluster/namespaces', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 删除某个命名空间 DELETE /cluster/namespaces/${param0} */
export async function deleteNamespace(
  params: API.deleteNamespaceParams,
  options?: { [key: string]: any }
) {
  const { name: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/cluster/namespaces/${encodeURIComponent(param0)}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 创建Namespace POST /cluster/namespaces */
export async function createNamespace(
  body: API.CreateNamespaceRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseNamespaceSpecResponse>('/cluster/namespaces', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取所有节点 GET /cluster/nodes */
export async function getNodes(options?: { [key: string]: any }) {
  return request<API.BaseResponseK8sListResponseNodeVO>('/cluster/nodes', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 获取所有节点实时资源用量 GET /cluster/node-metrics */
export async function getNodeMetrics(options?: { [key: string]: any }) {
  return request<API.BaseResponseK8sListResponseNodeMetricsVO>('/cluster/node-metrics', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 获取某个节点部分信息 GET /cluster/nodes/${param0} */
export async function getNode(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getNodeParams,
  options?: { [key: string]: any }
) {
  const { node_name: param0, ...queryParams } = params
  return request<API.BaseResponseNodeSpecResponse>(`/cluster/nodes/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 获取某个节点全部信息，yaml形式 GET /cluster/nodes/${param0}/all */
export async function getNodeAll(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getNodeAllParams,
  options?: { [key: string]: any }
) {
  const { node_name: param0, ...queryParams } = params
  return request<API.BaseResponseString>(`/cluster/nodes/${param0}/all`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 创建Pod POST /cluster/pods */
export async function createPod(body: API.CreatePodRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponsePodSpecResponse>('/cluster/pods', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取某个命名空间下所有pod GET /cluster/pods/${param0} */
export async function getPodsAll(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPodsAllParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, ...queryParams } = params
  return request<API.BaseResponseK8sListResponsePodVO>(`/cluster/pods/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 获取某个pod GET /cluster/pods/${param0}/${param1} */
export async function getPod(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPodParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, pod_name: param1, ...queryParams } = params
  return request<API.BaseResponsePodSpecResponse>(`/cluster/pods/${param0}/${param1}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 获取某个pod全部信息，yaml形式 GET /cluster/pods/${param0}/${param1}/all */
export async function getPodAll(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPodAllParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, pod_name: param1, ...queryParams } = params
  return request<API.BaseResponseString>(`/cluster/pods/${param0}/${param1}/all`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 获取指定 Pod 的容器日志 GET /cluster/pods/{namespace}/{pod_name}/logs */
export async function getPodLog(
  params: { namespace: string; pod_name: string; container?: string; tailLines?: number },
  options?: { [key: string]: any }
) {
  const { namespace, pod_name, ...queryParams } = params
  return request<API.BaseResponseString>(
    `/cluster/pods/${encodeURIComponent(namespace)}/${encodeURIComponent(pod_name)}/logs`,
    {
      method: 'GET',
      params: queryParams,
      ...(options || {}),
    }
  )
}

/** 创建Service POST /cluster/services */
export async function createService(
  body: API.CreateServiceRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseServiceSpecResponse>('/cluster/services', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取某个命名空间下所有service GET /cluster/services/${param0} */
export async function getServicesAll(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getServicesAllParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, ...queryParams } = params
  return request<API.BaseResponseK8sListResponseServiceVO>(`/cluster/services/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 获取某个service GET /cluster/services/${param0}/${param1} */
export async function getService(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getServiceParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, service_name: param1, ...queryParams } = params
  return request<API.BaseResponseServiceSpecResponse>(`/cluster/services/${param0}/${param1}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 删除某个service DELETE /cluster/services/${param0}/${param1} */
export async function deleteService(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteServiceParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, service_name: param1, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/cluster/services/${param0}/${param1}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 获取某个service全部信息，yaml形式 GET /cluster/services/${param0}/${param1}/all */
export async function getServiceAll(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getServiceAllParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, service_name: param1, ...queryParams } = params
  return request<API.BaseResponseString>(`/cluster/services/${param0}/${param1}/all`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}
