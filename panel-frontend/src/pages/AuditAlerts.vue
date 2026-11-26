<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  deleteAuditReport,
  getAuditAlerts,
  getAuditReport,
  getAuditReports,
  type AuditAlert,
  type AuditReportListItem,
} from '@/api/auditController'
import { useAuditStore } from '@/stores/audit'
import {
  buildAuditDetailRows,
  formatAuditTime,
  getAlertSummary,
  getOutputField,
  getPriorityColor,
  getPriorityRowClass,
} from '@/utils/audit'

const auditStore = useAuditStore()
const alerts = ref<AuditAlert[]>([])
const loading = ref(false)
const reportLoading = ref<Record<string, boolean>>({})
const activeTab = ref('alerts')
const historyReports = ref<AuditReportListItem[]>([])
const historyLoading = ref(false)
const historyLoaded = ref(false)
const historyPage = ref(1)
const historyPageSize = ref(10)
const historyTotal = ref(0)

const filters = ref<{ rule: string; namespace: string; priority: string | undefined }>({
  rule: '',
  namespace: '',
  priority: undefined,
})

const priorityOptions = ['Critical', 'Error', 'Warning', 'Notice', 'Informational']

const hasFilter = computed(
  () => !!(filters.value.rule || filters.value.namespace || filters.value.priority),
)

const redAlertCount = computed(
  () => alerts.value.filter((a) => getPriorityColor(a.priority) === 'red').length,
)
const orangeAlertCount = computed(
  () => alerts.value.filter((a) => getPriorityColor(a.priority) === 'orange').length,
)
const blueAlertCount = computed(
  () => alerts.value.filter((a) => getPriorityColor(a.priority) === 'blue').length,
)
const latestAlert = computed(() => alerts.value[0])

async function loadAlerts() {
  loading.value = true
  try {
    const resp = await getAuditAlerts({
      rule: filters.value.rule || undefined,
      namespace: filters.value.namespace || undefined,
      priority: filters.value.priority || undefined,
      limit: 200,
    })
    if (resp.data.code === 0 && Array.isArray(resp.data.data)) {
      alerts.value = resp.data.data
    }
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = { rule: '', namespace: '', priority: undefined }
  loadAlerts()
}

function priorityText(record: AuditAlert) {
  return record.priorityCn || record.priority
}

function formatTime(record: AuditAlert) {
  return formatAuditTime(record.timeMs, record.time)
}

function detailRows(record: AuditAlert) {
  return buildAuditDetailRows(record)
}

function summary(record: AuditAlert) {
  return getAlertSummary(record)
}

function getAlertRowClassName(record: AuditAlert) {
  return getPriorityRowClass(record.priority)
}

function field(record: AuditAlert, key: string) {
  return getOutputField(record.outputFields, key)
}

async function loadHistoryReports() {
  historyLoading.value = true
  try {
    const response = await getAuditReports({
      limit: historyPageSize.value,
      offset: (historyPage.value - 1) * historyPageSize.value,
    })
    if (response.data.code === 0) {
      historyReports.value = response.data.data.items || []
      historyTotal.value = response.data.data.total || 0
      historyLoaded.value = true
    }
  } finally {
    historyLoading.value = false
  }
}

function handleTabChange(key: string | number) {
  if (key === 'reports' && !historyLoaded.value) {
    loadHistoryReports()
  }
}

function changeHistoryPage(page: number, pageSize: number) {
  historyPage.value = pageSize === historyPageSize.value ? page : 1
  historyPageSize.value = pageSize
  loadHistoryReports()
}

function refreshHistoryReports() {
  historyPage.value = 1
  loadHistoryReports()
}

function formatReportGeneratedTime(record: AuditReportListItem) {
  return formatAuditTime(record.generatedAtMs, record.generatedAt)
}

function formatReportEventTime(record: AuditReportListItem) {
  return formatAuditTime(record.eventTimeMs, record.eventTime)
}

function formatFileSize(bytes: number) {
  if (!bytes) return '0 KB'
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function showHistoryTotal(total: number) {
  return `共 ${total} 份报告`
}

async function deleteHistoryReport(record: AuditReportListItem) {
  reportLoading.value = { ...reportLoading.value, [record.uuid]: true }
  try {
    const response = await deleteAuditReport(record.uuid)
    if (response.data.code === 0) {
      message.success('报告已删除')
      if (historyReports.value.length === 1 && historyPage.value > 1) {
        historyPage.value -= 1
      }
      await loadHistoryReports()
    }
  } catch {
    message.error('删除报告失败')
  } finally {
    const next = { ...reportLoading.value }
    delete next[record.uuid]
    reportLoading.value = next
  }
}

async function openReport(record: { uuid: string }, download: boolean) {
  let previewWindow: Window | null = null
  if (!download) {
    previewWindow = window.open('about:blank', '_blank')
  }
  reportLoading.value = { ...reportLoading.value, [record.uuid]: true }
  try {
    const response = await getAuditReport(record.uuid)
    const blob = response.data
    if (blob.type.includes('application/json')) {
      const payload = JSON.parse(await blob.text()) as { msg?: string }
      throw new Error(payload.msg || '报告生成失败')
    }
    if (!blob.size) {
      throw new Error('报告内容为空')
    }

    const objectUrl = URL.createObjectURL(blob)
    if (download) {
      const link = document.createElement('a')
      link.href = objectUrl
      link.download = `audit-report-${record.uuid}.pdf`
      document.body.appendChild(link)
      link.click()
      link.remove()
    } else if (previewWindow) {
      previewWindow.location.href = objectUrl
    } else {
      message.warning('浏览器阻止了报告预览窗口，请允许弹出窗口后重试')
    }
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
  } catch (error) {
    previewWindow?.close()
    message.error(error instanceof Error ? error.message : '获取审计报告失败')
  } finally {
    const next = { ...reportLoading.value }
    delete next[record.uuid]
    reportLoading.value = next
  }
}

// 实时：把 store 收到的新告警合并进当前列表（有筛选时不打扰）
watch(
  () => auditStore.alerts,
  (incoming) => {
    if (hasFilter.value) return
    const existing = new Set(alerts.value.map((a) => a.uuid))
    const fresh = incoming.filter((a) => !existing.has(a.uuid))
    if (fresh.length) {
      alerts.value = [...fresh, ...alerts.value]
    }
  },
  { deep: true },
)

onMounted(() => {
  loadAlerts()
  auditStore.init()
  auditStore.connect()
})
</script>

<template>
  <div class="audit-alert-page">
    <div class="alert-hero">
      <div class="alert-hero-main">
        <a-tag color="blue">Falco Webhook · 实时</a-tag>
        <h2>审计告警</h2>
        <p>数据源：ClickHouse falco.events（source = k8s_audit）</p>
      </div>
      <div class="latest-alert" v-if="latestAlert">
        <span>最新告警</span>
        <strong>{{ latestAlert.ruleCn || latestAlert.rule }}</strong>
        <small>{{ formatTime(latestAlert) }}</small>
      </div>
    </div>

    <div class="severity-grid">
      <div class="severity-card severity-total">
        <span>当前展示</span>
        <strong>{{ alerts.length }}</strong>
      </div>
      <div class="severity-card severity-red">
        <span>红色告警</span>
        <strong>{{ redAlertCount }}</strong>
      </div>
      <div class="severity-card severity-orange">
        <span>橙色告警</span>
        <strong>{{ orangeAlertCount }}</strong>
      </div>
      <div class="severity-card severity-blue">
        <span>蓝色告警</span>
        <strong>{{ blueAlertCount }}</strong>
      </div>
    </div>

    <a-tabs v-model:active-key="activeTab" class="audit-tabs" @change="handleTabChange">
      <a-tab-pane key="alerts" tab="实时告警">
        <div class="filter-bar">
      <a-input
        v-model:value="filters.rule"
        placeholder="按规则名筛选"
        allow-clear
        style="width: 220px"
        @press-enter="loadAlerts"
      />
      <a-input
        v-model:value="filters.namespace"
        placeholder="按命名空间筛选"
        allow-clear
        style="width: 180px"
        @press-enter="loadAlerts"
      />
      <a-select
        v-model:value="filters.priority"
        placeholder="优先级"
        allow-clear
        style="width: 160px"
        :options="priorityOptions.map((p) => ({ label: p, value: p }))"
      />
      <a-button type="primary" :loading="loading" @click="loadAlerts">查询</a-button>
      <a-button @click="resetFilters">重置</a-button>
        </div>

        <a-table
      class="alert-table"
      :data-source="alerts"
      :loading="loading"
      row-key="uuid"
      :pagination="{ pageSize: 10 }"
      :row-class-name="getAlertRowClassName"
    >
      <a-table-column title="规则" data-index="rule" key="rule">
        <template #default="{ record }">
          <div class="rule-cell">
            <strong>{{ record.ruleCn || record.rule }}</strong>
            <span v-if="summary(record)">{{ summary(record) }}</span>
          </div>
        </template>
      </a-table-column>
      <a-table-column title="严重程度" data-index="priority" key="priority" :width="100">
        <template #default="{ record }">
          <a-tag :color="getPriorityColor(record.priority)">
            {{ priorityText(record) }}
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="命名空间" key="namespace" :width="120">
        <template #default="{ record }">
          {{ record.namespace || '-' }}
        </template>
      </a-table-column>
      <a-table-column title="资源" key="resource" :width="180">
        <template #default="{ record }">
          {{ field(record, 'ka.target.resource') }} /
          {{ field(record, 'ka.target.name') }}
        </template>
      </a-table-column>
      <a-table-column title="操作用户" key="user" :width="260">
        <template #default="{ record }">
          {{ record.subjectName || field(record, 'ka.user.name') }}
        </template>
      </a-table-column>
      <a-table-column title="时间" data-index="time" key="time" :width="170">
        <template #default="{ record }">
          {{ formatTime(record) }}
        </template>
      </a-table-column>
      <a-table-column title="报告" key="report" :width="130">
        <template #default="{ record }">
          <a-space :size="4">
            <a-button
              type="link"
              size="small"
              :loading="!!reportLoading[record.uuid]"
              @click="openReport(record, false)"
            >
              预览
            </a-button>
            <a-button
              type="link"
              size="small"
              :disabled="!!reportLoading[record.uuid]"
              @click="openReport(record, true)"
            >
              下载
            </a-button>
          </a-space>
        </template>
      </a-table-column>

      <template #expandedRowRender="{ record }">
        <div class="alert-detail">
          <div class="detail-output" v-if="summary(record)">
            <span>问题说明</span>
            <p>{{ summary(record) }}</p>
          </div>

          <a-descriptions
            bordered
            size="small"
            :column="{ xxl: 2, xl: 2, lg: 2, md: 1, sm: 1, xs: 1 }"
          >
            <a-descriptions-item
              v-for="row in detailRows(record)"
              :key="row.label"
              :label="row.label"
            >
              <span :class="{ 'detail-danger': row.danger }">{{ row.value }}</span>
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="reports" tab="历史报告">
        <div class="report-toolbar">
          <div>
            <strong>已生成报告</strong>
            <span>共 {{ historyTotal }} 份，自动保留 7 天，按生成时间倒序排列</span>
          </div>
          <a-button :loading="historyLoading" @click="refreshHistoryReports">刷新</a-button>
        </div>

        <a-table
          class="alert-table report-table"
          :data-source="historyReports"
          :loading="historyLoading"
          row-key="uuid"
          :pagination="false"
        >
          <a-table-column title="报告编号" key="reportId" :width="210">
            <template #default="{ record }">
              <span class="report-id">AUD-{{ record.uuid }}</span>
            </template>
          </a-table-column>
          <a-table-column title="告警规则" key="rule">
            <template #default="{ record }">
              <strong>{{ record.ruleCn || record.rule || '原告警记录已不存在' }}</strong>
            </template>
          </a-table-column>
          <a-table-column title="级别" key="priority" :width="100">
            <template #default="{ record }">
              <a-tag :color="getPriorityColor(record.priority || 'Informational')">
                {{ record.priorityCn || record.priority || '-' }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="命名空间" key="namespace" :width="120">
            <template #default="{ record }">{{ record.namespace || '-' }}</template>
          </a-table-column>
          <a-table-column title="事件时间" key="eventTime" :width="170">
            <template #default="{ record }">{{ formatReportEventTime(record) }}</template>
          </a-table-column>
          <a-table-column title="生成时间" key="generatedAt" :width="170">
            <template #default="{ record }">{{ formatReportGeneratedTime(record) }}</template>
          </a-table-column>
          <a-table-column title="大小" key="size" :width="90">
            <template #default="{ record }">{{ formatFileSize(record.sizeBytes) }}</template>
          </a-table-column>
          <a-table-column title="操作" key="actions" :width="180">
            <template #default="{ record }">
              <a-space :size="4">
                <a-button
                  type="link"
                  size="small"
                  :loading="!!reportLoading[record.uuid]"
                  @click="openReport(record, false)"
                >
                  预览
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  :disabled="!!reportLoading[record.uuid]"
                  @click="openReport(record, true)"
                >
                  下载
                </a-button>
                <a-popconfirm
                  title="确定删除这份报告吗？原始审计告警不会被删除。"
                  ok-text="删除"
                  cancel-text="取消"
                  @confirm="deleteHistoryReport(record)"
                >
                  <a-button
                    type="link"
                    danger
                    size="small"
                    :disabled="!!reportLoading[record.uuid]"
                  >
                    删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </a-table-column>
        </a-table>

        <div class="report-pagination">
          <a-pagination
            :current="historyPage"
            :page-size="historyPageSize"
            :total="historyTotal"
            :show-size-changer="true"
            :show-total="showHistoryTotal"
            @change="changeHistoryPage"
            @show-size-change="changeHistoryPage"
          />
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<style scoped>
.audit-alert-page {
  padding: 0 24px;
}

.alert-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  margin-bottom: 18px;
  padding: 22px 24px;
  color: var(--app-text);
  background: var(--app-accent-soft);
  border: 1px solid var(--app-accent-border);
  border-radius: 4px;
}

.alert-hero-main h2 {
  margin: 10px 0 4px;
  font-size: 24px;
  font-weight: 600;
}

.alert-hero-main p {
  margin: 0;
  color: var(--app-text-secondary);
}

.latest-alert {
  min-width: 260px;
  padding: 14px 16px;
  background: var(--app-surface);
  border: 1px solid var(--app-accent-border);
  border-radius: 4px;
}

.latest-alert span,
.latest-alert small {
  display: block;
  color: var(--app-text-muted);
}

.latest-alert strong {
  display: block;
  margin: 4px 0;
  font-size: 16px;
}

.severity-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.severity-card {
  position: relative;
  padding: 16px 18px;
  overflow: hidden;
  background: var(--app-surface);
  border: 1px solid var(--app-border-soft);
  border-radius: 4px;
}

.severity-card::before {
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  content: '';
  background: #409eff;
}

.severity-card span {
  display: block;
  color: var(--app-text-muted);
}

.severity-card strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  line-height: 1;
}

.severity-red::before {
  background: #f56c6c;
}

.severity-orange::before {
  background: #e6a23c;
}

.severity-blue::before,
.severity-total::before {
  background: #409eff;
}

.audit-tabs {
  padding: 0 18px 18px;
  background: var(--app-surface);
  border: 1px solid var(--app-border-soft);
  border-radius: 4px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.report-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.report-toolbar div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.report-toolbar span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.report-id {
  color: var(--app-accent);
  font-family: Consolas, monospace;
  font-size: 12px;
}

.report-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.alert-table {
  max-width: 100%;
  overflow: hidden;
  border: 1px solid var(--app-border-soft);
  border-radius: 4px;
}

:deep(.alert-table .ant-table table) {
  table-layout: fixed !important;
}

:deep(.alert-row-red td:first-child) {
  border-left: 4px solid #f56c6c;
}

:deep(.alert-row-orange td:first-child) {
  border-left: 4px solid #e6a23c;
}

:deep(.alert-row-blue td:first-child) {
  border-left: 4px solid #409eff;
}

:deep(.ant-table-cell) {
  vertical-align: top;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.rule-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.rule-cell strong {
  font-weight: 600;
}

.rule-cell span {
  display: -webkit-box;
  overflow: hidden;
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.alert-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}

.detail-output {
  padding: 12px 14px;
  background: var(--app-accent-soft);
  border: 1px solid var(--app-accent-border);
  border-radius: 6px;
}

.detail-output span {
  display: block;
  margin-bottom: 6px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.detail-output p {
  margin: 0;
  overflow-wrap: anywhere;
  line-height: 1.6;
}

:deep(.alert-detail .ant-descriptions) {
  max-width: 100%;
}

:deep(.alert-detail .ant-descriptions-view) {
  overflow: hidden;
}

:deep(.alert-detail .ant-descriptions-item-label),
:deep(.alert-detail .ant-descriptions-item-content) {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.detail-danger {
  color: #f56c6c;
  font-weight: 600;
}

@media (max-width: 960px) {
  .alert-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .severity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
