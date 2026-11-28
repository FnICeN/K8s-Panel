import request from '@/request'

export type AuditPriority =
  | 'Emergency'
  | 'Alert'
  | 'Critical'
  | 'Error'
  | 'Warning'
  | 'Notice'
  | 'Informational'
  | 'Debug'

export type AuditOutputFields = Record<string, string | number | boolean | string[]>

export interface AuditAlert {
  uuid: string
  time: string
  timeMs: number
  rule: string
  ruleCn: string
  priority: AuditPriority
  priorityCn: string
  hostname: string
  source: string
  namespace: string
  subjectName: string
  problemDesc: string
  output: string
  tags: string[]
  outputFields: AuditOutputFields
}

export interface AuditAlertListParams {
  rule?: string
  namespace?: string
  priority?: string
  limit?: number
  offset?: number
}

export interface AuditAlertSearchParams {
  rule?: string
  namespace?: string
  priority?: string
  start: string
  end: string
  limit?: number
  offset?: number
  includeSummary?: boolean
}

export interface AuditAlertPage {
  total: number | null
  redCount: number | null
  orangeCount: number | null
  blueCount: number | null
  items: AuditAlert[]
}

export interface AuditReportStatus {
  uuid: string
  available: boolean
}

export interface AuditReportListItem {
  uuid: string
  rule?: string
  ruleCn?: string
  priority?: AuditPriority
  priorityCn?: string
  namespace?: string
  subjectName?: string
  eventTime?: string
  eventTimeMs?: number
  generatedAt: string
  generatedAtMs: number
  sizeBytes: number
}

export interface AuditReportPage {
  total: number
  items: AuditReportListItem[]
}

interface BaseResponse<T> {
  code: number
  data: T
  msg: string
}

type RequestOptions = Record<string, unknown>

/** 分页查询审计告警（默认近24h） GET /audit/alerts */
export async function getAuditAlerts(
  params?: AuditAlertListParams,
  options?: RequestOptions,
) {
  return request<BaseResponse<AuditAlert[]>>('/audit/alerts', {
    method: 'GET',
    params: { ...(params || {}) },
    ...(options || {}),
  })
}

/** 首次查询统计总数；跳页可用 includeSummary=false 只读取当前页 */
export async function searchAuditAlerts(params: AuditAlertSearchParams, options?: RequestOptions) {
  return request<BaseResponse<AuditAlertPage>>('/audit/alerts/search', {
    method: 'GET',
    params,
    ...(options || {}),
  })
}

/** 近24h审计告警总数 GET /audit/alerts/count */
export async function getAuditAlertCount(options?: RequestOptions) {
  return request<BaseResponse<number>>('/audit/alerts/count', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 查询单条审计告警详情 GET /audit/alerts/{uuid} */
export async function getAuditAlertDetail(uuid: string, options?: RequestOptions) {
  return request<BaseResponse<AuditAlert>>(`/audit/alerts/${uuid}`, {
    method: 'GET',
    ...(options || {}),
  })
}

/** 查询告警 PDF 报告状态 GET /audit/alerts/{uuid}/report/status */
export async function getAuditReportStatus(uuid: string, options?: RequestOptions) {
  return request<BaseResponse<AuditReportStatus>>(`/audit/alerts/${encodeURIComponent(uuid)}/report/status`, {
    method: 'GET',
    ...(options || {}),
  })
}

/** 获取告警 PDF；历史报告不存在时由后端即时补生成 */
export async function getAuditReport(uuid: string, options?: RequestOptions) {
  return request<Blob>(`/audit/alerts/${encodeURIComponent(uuid)}/report`, {
    method: 'GET',
    responseType: 'blob',
    ...(options || {}),
  })
}

/** 分页查询已生成的历史审计报告 GET /audit/reports */
export async function getAuditReports(
  params?: { limit?: number; offset?: number },
  options?: RequestOptions,
) {
  return request<BaseResponse<AuditReportPage>>('/audit/reports', {
    method: 'GET',
    params: { ...(params || {}) },
    ...(options || {}),
  })
}

/** 手动删除指定历史报告 DELETE /audit/reports/{uuid} */
export async function deleteAuditReport(uuid: string, options?: RequestOptions) {
  return request<BaseResponse<boolean>>(`/audit/reports/${encodeURIComponent(uuid)}`, {
    method: 'DELETE',
    ...(options || {}),
  })
}

/** SSE 实时推送地址（EventSource 使用） */
export function getAuditStreamUrl() {
  const base = request.defaults.baseURL ?? ''
  return `${base}/audit/alerts/stream`
}
