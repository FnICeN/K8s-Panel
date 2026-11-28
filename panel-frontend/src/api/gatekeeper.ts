import request from '@/request'

export interface GatekeeperObject {
  apiVersion?: string
  kind?: string
  metadata?: { name?: string; creationTimestamp?: string; annotations?: Record<string, string> }
  spec?: {
    crd?: { spec?: { names?: { kind?: string } } }
    enforcementAction?: string
    match?: unknown
  }
  status?: { totalViolations?: number }
}

export interface GatekeeperConstraint {
  resource: string
  object: GatekeeperObject
}

interface ApiResponse<T> {
  code: number
  msg?: string
  data?: T
}

export function listGatekeeperPolicies() {
  return request.get<ApiResponse<{
    templates: GatekeeperObject[]
    constraints: GatekeeperConstraint[]
    mutations: GatekeeperConstraint[]
  }>>('/cluster/gatekeeper/policies')
}

export function previewGatekeeperAnnotation(resource: string, name: string, key: string, value: string) {
  return request.patch<ApiResponse<{ object: GatekeeperObject; yaml: string }>>(
    `/cluster/gatekeeper/constraints/${encodeURIComponent(resource)}/${encodeURIComponent(name)}/annotations/preview`,
    { key, value },
  )
}

export function deleteGatekeeperConstraint(resource: string, name: string) {
  return request.delete<ApiResponse<boolean>>(
    `/cluster/gatekeeper/constraints/${encodeURIComponent(resource)}/${encodeURIComponent(name)}`,
  )
}
