import type { AuditAlert, AuditPriority } from '@/api/auditController'

/** 优先级 → 三色分级（红/橙/蓝） */
export function getPriorityColor(priority: AuditPriority): 'red' | 'orange' | 'blue' {
  if (
    priority === 'Emergency' ||
    priority === 'Alert' ||
    priority === 'Critical' ||
    priority === 'Error'
  ) {
    return 'red'
  }
  if (priority === 'Warning') return 'orange'
  return 'blue'
}

/** 优先级 → 表格行样式类 */
export function getPriorityRowClass(priority: AuditPriority): string {
  return `alert-row-${getPriorityColor(priority)}`
}

/** 是否为需要弹窗提醒的高危告警（Critical/Error 及以上） */
export function isHighRisk(priority: AuditPriority): boolean {
  return (
    priority === 'Emergency' ||
    priority === 'Alert' ||
    priority === 'Critical' ||
    priority === 'Error'
  )
}

/** 读取 outputFields 字段，数组以逗号拼接 */
export function getOutputField(
  fields: Record<string, unknown> | undefined,
  key: string,
): string {
  const value = fields?.[key]
  if (value === undefined || value === null) return '-'
  if (Array.isArray(value)) return value.join(', ')
  return String(value)
}

/** 格式化时间：优先用毫秒时间戳，其次 ISO 字符串 */
export function formatAuditTime(timeMs?: number, time?: string): string {
  const date = timeMs ? new Date(timeMs) : time ? new Date(time) : null
  if (!date || Number.isNaN(date.getTime())) return time ?? '-'
  return date.toLocaleString('zh-CN', { hour12: false })
}

/** K8s 操作动词 → 中文 */
const VERB_MAP: Record<string, string> = {
  get: '获取',
  list: '列举',
  watch: '监听',
  create: '创建',
  update: '更新',
  patch: '修改',
  delete: '删除',
  deletecollection: '批量删除',
  exec: '执行命令(exec)',
  attach: '接入终端(attach)',
  portforward: '端口转发',
  connect: '连接',
  approve: '审批',
  bind: '绑定',
  escalate: '权限提升',
  impersonate: '身份模拟',
}

/** K8s 资源类型 → 中文 */
const RESOURCE_MAP: Record<string, string> = {
  secrets: '密钥(Secret)',
  configmaps: '配置(ConfigMap)',
  pods: 'Pod',
  services: '服务(Service)',
  deployments: '部署(Deployment)',
  replicasets: '副本集(ReplicaSet)',
  daemonsets: '守护进程集(DaemonSet)',
  statefulsets: '有状态集(StatefulSet)',
  jobs: '任务(Job)',
  cronjobs: '定时任务(CronJob)',
  nodes: '节点(Node)',
  namespaces: '命名空间(Namespace)',
  serviceaccounts: '服务账号(ServiceAccount)',
  roles: '角色(Role)',
  rolebindings: '角色绑定(RoleBinding)',
  clusterroles: '集群角色(ClusterRole)',
  clusterrolebindings: '集群角色绑定(ClusterRoleBinding)',
  ingresses: '入口(Ingress)',
  events: '事件(Event)',
  persistentvolumeclaims: '存储卷声明(PVC)',
  persistentvolumes: '存储卷(PV)',
  validatingwebhookconfigurations: '校验准入Webhook',
  mutatingwebhookconfigurations: '变更准入Webhook',
}

/** 授权结果 → 中文 */
const DECISION_MAP: Record<string, string> = {
  allow: '允许',
  deny: '拒绝',
}

export function translateVerb(verb?: string): string {
  if (!verb) return '-'
  return VERB_MAP[verb.toLowerCase()] ?? verb
}

export function translateResource(resource?: string): string {
  if (!resource) return '-'
  return RESOURCE_MAP[resource.toLowerCase()] ?? resource
}

export function translateDecision(decision?: string): string {
  if (!decision) return '-'
  return DECISION_MAP[decision.toLowerCase()] ?? decision
}

export interface AuditDetailRow {
  label: string
  value: string
  /** 高危提示（如拒绝、deny）标红 */
  danger?: boolean
}

function pick(value: unknown): string {
  if (value === undefined || value === null) return ''
  if (Array.isArray(value)) {
    return value
      .map((item) => String(item).trim())
      .filter(Boolean)
      .join('、')
  }
  return String(value).trim()
}

/** HTTP 响应码 → 中文说明 */
function translateResponseCode(code?: string): string {
  if (!code) return ''
  const map: Record<string, string> = {
    '200': '成功',
    '201': '已创建',
    '204': '成功(无内容)',
    '400': '请求错误',
    '401': '未认证',
    '403': '禁止访问',
    '404': '未找到',
    '409': '冲突',
    '500': '服务端错误',
  }
  const text = map[code]
  return text ? `${text}（${code}）` : code
}

/**
 * 把一条审计告警整理成中文摘要行，只保留对排查有用的字段。
 * 不展示：uuid、hostname、cluster、tags、uri、用户组、英文原文 output 等噪声。
 */
export function buildAuditDetailRows(alert: AuditAlert): AuditDetailRow[] {
  const f = alert.outputFields ?? {}
  const decision = pick(f['ka.auth.decision'])
  const verb = pick(f['ka.verb'])
  const resource = pick(f['ka.target.resource'])
  const resourceName = pick(f['ka.target.name'])
  const subresource = pick(f['ka.target.subresource'])
  const namespace = pick(alert.namespace) || pick(f['ka.target.namespace'])
  const subject = pick(alert.subjectName) || pick(f['ka.user.name'])
  const reason = pick(f['ka.auth.reason'])
  const respCode = pick(f['ka.response.code'])

  // 「做了什么」拼成一句可读中文，例如：获取 · 密钥(Secret) / mysql-root-secret
  const actionParts: string[] = []
  if (verb) actionParts.push(translateVerb(verb))
  if (subresource) actionParts.push(subresource)
  const targetParts: string[] = []
  if (resource) targetParts.push(translateResource(resource))
  if (resourceName) targetParts.push(resourceName)
  const actionText =
    actionParts.length || targetParts.length
      ? [actionParts.join(' · '), targetParts.join(' / ')].filter(Boolean).join(' · ')
      : ''

  const resultParts: string[] = []
  if (decision) resultParts.push(translateDecision(decision))
  if (respCode) resultParts.push(translateResponseCode(respCode))

  const rows: AuditDetailRow[] = [
    { label: '规则', value: pick(alert.ruleCn) || pick(alert.rule) },
    { label: '严重程度', value: pick(alert.priorityCn) || pick(alert.priority) },
    { label: '发生时间', value: formatAuditTime(alert.timeMs, alert.time) },
    { label: '操作用户', value: subject },
    { label: '命名空间', value: namespace },
    { label: '操作内容', value: actionText },
    {
      label: '结果',
      value: resultParts.join(' · '),
      danger: decision.toLowerCase() === 'deny' || respCode === '403',
    },
    { label: '原因', value: reason },
  ]

  return rows.filter((row) => row.value && row.value !== '-')
}

/** 列表副标题：优先中文问题说明，避免展示英文 Falco output */
export function getAlertSummary(alert: AuditAlert): string {
  return pick(alert.problemDesc) || pick(alert.output) || ''
}
