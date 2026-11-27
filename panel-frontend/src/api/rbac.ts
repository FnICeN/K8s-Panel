import request from '@/request'

export interface RbacResource {
  kind?: string
  metadata?: { name?: string; namespace?: string; creationTimestamp?: string }
  rules?: Array<{
    apiGroups?: string[]
    resources?: string[]
    resourceNames?: string[]
    nonResourceURLs?: string[]
    verbs?: string[]
  }>
  roleRef?: { apiGroup?: string; kind?: string; name?: string }
  subjects?: Array<{ kind?: string; apiGroup?: string; name?: string; namespace?: string }>
}

interface ApiResponse<T> {
  code: number
  msg?: string
  data?: T
}

export type RbacKind = 'roles' | 'clusterroles' | 'rolebindings'

export function listRbac(kind: RbacKind, namespace?: string) {
  return request.get<ApiResponse<{ items?: RbacResource[] }>>(`/cluster/${kind}`, {
    params: namespace ? { namespace } : undefined,
  })
}

export function getRbac(kind: RbacKind, name: string, namespace?: string) {
  const path = kind === 'clusterroles'
    ? `/cluster/${kind}/${encodeURIComponent(name)}`
    : `/cluster/${kind}/${encodeURIComponent(namespace || '')}/${encodeURIComponent(name)}`
  return request.get<ApiResponse<RbacResource>>(path)
}

export function createRole(yaml: string, dryRun = false) {
  return request.post<ApiResponse<RbacResource>>('/cluster/roles', { yaml }, { params: { dryRun } })
}
