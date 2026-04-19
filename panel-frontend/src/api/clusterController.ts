// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /cluster/nodes */
export async function getNodes(options?: { [key: string]: any }) {
  return request<API.NodeListResponse>('/cluster/nodes', {
    method: 'GET',
    ...(options || {}),
  })
}
