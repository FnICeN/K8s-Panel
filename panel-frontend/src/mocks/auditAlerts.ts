export type AuditPriority = 'Critical' | 'Error' | 'Warning' | 'Notice' | 'Informational'

export type AuditOutputFields = Record<string, string | number | boolean | string[]>

export interface AuditAlert {
  id: string
  output: string
  hostname: string
  priority: AuditPriority
  rule: string
  source: 'k8s_audit'
  tags: string[]
  time: string
  output_fields: AuditOutputFields
}

export const mockAuditAlerts: AuditAlert[] = [
  {
    id: 'falco-kube-app-role-create',
    output:
      '10:21:16.230000000: Error K8s Role Created (user=system:serviceaccount:kube-app:deployer role=pod-reader ns=kube-app resource=roles resp=201 decision=allow reason=)',
    hostname: 'falco-k8saudit-control-plane',
    priority: 'Notice',
    rule: 'K8s Role Created',
    source: 'k8s_audit',
    tags: ['k8s', 'rbac', 'kube-app'],
    time: '2026-06-04T02:21:16.230000000Z',
    output_fields: {
      'evt.time': 1780549276230000000,
      'ka.auditid': '0e84bf5b-7b08-47bd-b3af-c843fe9345a1',
      'ka.auth.decision': 'allow',
      'ka.auth.reason': '',
      'ka.response.code': '201',
      'ka.target.name': 'pod-reader',
      'ka.target.namespace': 'kube-app',
      'ka.target.resource': 'roles',
      'ka.uri': '/apis/rbac.authorization.k8s.io/v1/namespaces/kube-app/roles',
      'ka.user.name': 'system:serviceaccount:kube-app:deployer',
      'ka.verb': 'create',
      'ka.req.role.rules.resources': ['pods'],
      'ka.req.role.rules.verbs': ['get', 'list', 'watch'],
    },
  },
  {
    id: 'falco-kube-app-rolebinding-create',
    output:
      '10:24:41.884000000: Warning K8s RoleBinding Created (user=system:serviceaccount:kube-app:deployer rolebinding=pod-reader-binding role=pod-reader ns=kube-app resource=rolebindings resp=201 decision=allow reason=)',
    hostname: 'falco-k8saudit-control-plane',
    priority: 'Warning',
    rule: 'K8s RoleBinding Created',
    source: 'k8s_audit',
    tags: ['k8s', 'rbac', 'kube-app'],
    time: '2026-06-04T02:24:41.884000000Z',
    output_fields: {
      'evt.time': 1780549481884000000,
      'ka.auditid': '2d0bb26c-f911-4b22-9a1a-9b2bb7ea552a',
      'ka.auth.decision': 'allow',
      'ka.auth.reason': '',
      'ka.response.code': '201',
      'ka.target.name': 'pod-reader-binding',
      'ka.target.namespace': 'kube-app',
      'ka.target.resource': 'rolebindings',
      'ka.uri': '/apis/rbac.authorization.k8s.io/v1/namespaces/kube-app/rolebindings',
      'ka.user.name': 'system:serviceaccount:kube-app:deployer',
      'ka.verb': 'create',
      'ka.req.binding.role': 'pod-reader',
      'ka.req.binding.subjects': ['system:serviceaccount:kube-app:app-runtime'],
    },
  },
  {
    id: 'falco-kube-app-sa-list-pods',
    output:
      '10:29:08.515000000: Notice ServiceAccount listed pods in namespace (user=system:serviceaccount:kube-app:app-runtime ns=kube-app resource=pods verb=list resp=200 decision=allow reason=)',
    hostname: 'falco-k8saudit-control-plane',
    priority: 'Critical',
    rule: 'ServiceAccount Listed Pods',
    source: 'k8s_audit',
    tags: ['k8s', 'serviceaccount', 'kube-app'],
    time: '2026-06-04T02:29:08.515000000Z',
    output_fields: {
      'evt.time': 1780549748515000000,
      'ka.auditid': 'f75d8765-8f57-4793-afdc-336047d6ce23',
      'ka.auth.decision': 'allow',
      'ka.auth.reason': '',
      'ka.response.code': '200',
      'ka.target.name': 'pods',
      'ka.target.namespace': 'kube-app',
      'ka.target.resource': 'pods',
      'ka.uri': '/api/v1/namespaces/kube-app/pods',
      'ka.user.name': 'system:serviceaccount:kube-app:app-runtime',
      'ka.verb': 'list',
      'ka.user.groups': [
        'system:serviceaccounts',
        'system:serviceaccounts:kube-app',
        'system:authenticated',
      ],
    },
  },
]
