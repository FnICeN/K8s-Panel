import { ref } from 'vue'
import { defineStore } from 'pinia'
import { notification } from 'ant-design-vue'
import {
  getAuditAlertCount,
  getAuditAlerts,
  getAuditStreamUrl,
  type AuditAlert,
} from '@/api/auditController'
import { formatAuditTime, getPriorityColor, isHighRisk } from '@/utils/audit'

/** 铃铛下拉/预览保留的最大条数 */
const MAX_PREVIEW = 50

export const useAuditStore = defineStore('audit', () => {
  /** 最近告警列表（用于铃铛下拉预览与实时插入） */
  const alerts = ref<AuditAlert[]>([])
  /** 近 24h 告警总数（铃铛角标） */
  const count = ref(0)
  /** SSE 连接状态 */
  const connected = ref(false)

  let eventSource: EventSource | null = null

  /** 初次加载：拉取近24h计数与最近列表 */
  const init = async () => {
    try {
      const [listResp, countResp] = await Promise.all([
        getAuditAlerts({ limit: MAX_PREVIEW }),
        getAuditAlertCount(),
      ])
      if (listResp.data.code === 0 && Array.isArray(listResp.data.data)) {
        alerts.value = listResp.data.data
      }
      if (countResp.data.code === 0) {
        count.value = countResp.data.data ?? 0
      }
    } catch (error) {
      console.error('加载审计告警失败:', error)
    }
  }

  /** 收到一条新告警：去重后插入队首，更新角标，高危弹窗提醒 */
  const handleIncoming = (alert: AuditAlert) => {
    if (!alert || !alert.uuid) return
    if (alerts.value.some((item) => item.uuid === alert.uuid)) return
    alerts.value = [alert, ...alerts.value].slice(0, MAX_PREVIEW)
    count.value += 1
    if (isHighRisk(alert.priority)) {
      notification.error({
        message: alert.ruleCn || alert.rule,
        description: `${alert.namespace || '-'} · ${alert.subjectName || '-'}\n${alert.output}`,
        duration: 6,
      })
    } else if (getPriorityColor(alert.priority) === 'orange') {
      notification.warning({
        message: alert.ruleCn || alert.rule,
        description: formatAuditTime(alert.timeMs, alert.time),
        duration: 4,
      })
    }
  }

  /** 建立 SSE 连接（幂等） */
  const connect = () => {
    if (eventSource) return
    try {
      eventSource = new EventSource(getAuditStreamUrl(), { withCredentials: true })
      eventSource.addEventListener('connected', () => {
        connected.value = true
      })
      eventSource.addEventListener('audit', (event) => {
        try {
          const alert = JSON.parse((event as MessageEvent).data) as AuditAlert
          handleIncoming(alert)
        } catch (error) {
          console.error('解析审计推送失败:', error)
        }
      })
      eventSource.onerror = () => {
        connected.value = false
        // EventSource 会自动重连，这里无需手动处理
      }
    } catch (error) {
      console.error('建立审计告警 SSE 连接失败:', error)
    }
  }

  /** 关闭 SSE 连接 */
  const disconnect = () => {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    connected.value = false
  }

  return {
    alerts,
    count,
    connected,
    init,
    connect,
    disconnect,
  }
})
