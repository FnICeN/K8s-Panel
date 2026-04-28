// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取所有命名空间 GET /cluster/namespaces */
export async function getNamespaces(options?: { [key: string]: any }) {
  return request<API.BaseResponseNamespacesListResponse>('/cluster/namespaces', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 获取所有节点 GET /cluster/nodes */
export async function getNodes(options?: { [key: string]: any }) {
  return request<API.BaseResponseNodeListResponse>('/cluster/nodes', {
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

/** 获取某个命名空间下所有pod GET /cluster/pods/${param0} */
export async function getPodsAll(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPodsAllParams,
  options?: { [key: string]: any }
) {
  const { namespace: param0, ...queryParams } = params
  return request<API.BaseResponsePodListResponse>(`/cluster/pods/${param0}`, {
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
