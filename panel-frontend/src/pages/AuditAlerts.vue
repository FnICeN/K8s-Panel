<script setup lang="ts">
import { computed } from 'vue'
import { mockAuditAlerts, type AuditAlert, type AuditPriority } from '@/mocks/auditAlerts'

const alerts = mockAuditAlerts

const priorityMeta: Record<AuditPriority, { color: string; text: string }> = {
  Critical: { color: 'red', text: '严重' },
  Error: { color: 'red', text: '错误' },
  Warning: { color: 'orange', text: '警告' },
  Notice: { color: 'blue', text: '通知' },
  Informational: { color: 'blue', text: '信息' },
}

const redAlertCount = computed(
  () => alerts.filter((alert) => alert.priority === 'Critical' || alert.priority === 'Error').length,
)
const orangeAlertCount = computed(() => alerts.filter((alert) => alert.priority === 'Warning').length)
const blueAlertCount = computed(
  () => alerts.filter((alert) => alert.priority === 'Notice' || alert.priority === 'Informational').length,
)
const latestAlert = computed(() => alerts[2])

function getOutputField(alert: AuditAlert, field: string) {
  const value = alert.output_fields[field]
  if (Array.isArray(value)) return value.join(', ')
  return value ?? '-'
}

function getPriorityMeta(priority: AuditPriority) {
  return priorityMeta[priority]
}

function formatTime(time: string) {
  return new Date(time).toLocaleString('zh-CN', { hour12: false })
}

function stringifyAlert(alert: AuditAlert) {
  return JSON.stringify(alert, null, 2)
}

function getAlertRowClassName(record: AuditAlert) {
  if (record.priority === 'Critical' || record.priority === 'Error') return 'alert-row-red'
  if (record.priority === 'Warning') return 'alert-row-orange'
  return 'alert-row-blue'
}
</script>

<template>
  <div class="audit-alert-page">
    <div class="alert-hero">
      <div class="alert-hero-main">
        <a-tag color="blue">Falco Webhook</a-tag>
        <h2>审计告警</h2>
      </div>
      <div class="latest-alert" v-if="latestAlert">
        <span>最新告警</span>
        <strong>{{ latestAlert.rule }}</strong>
        <small>{{ formatTime(latestAlert.time) }}</small>
      </div>
    </div>

    <div class="severity-grid">
      <div class="severity-card severity-total">
        <span>告警总数</span>
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

    <a-table
      class="alert-table"
      :data-source="alerts"
      row-key="id"
      :pagination="{ pageSize: 10 }"
      :row-class-name="getAlertRowClassName"
    >
      <a-table-column title="规则" data-index="rule" key="rule">
        <template #default="{ record }">
          <div class="rule-cell">
            <strong>{{ record.rule }}</strong>
            <span>{{ record.output }}</span>
          </div>
        </template>
      </a-table-column>
      <a-table-column title="严重程度" data-index="priority" key="priority" :width="140">
        <template #default="{ record }">
          <a-tag :color="getPriorityMeta(record.priority).color">
            {{ getPriorityMeta(record.priority).text }} / {{ record.priority }}
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="命名空间" key="namespace" :width="120">
        <template #default="{ record }">
          {{ getOutputField(record, 'ka.target.namespace') }}
        </template>
      </a-table-column>
      <a-table-column title="资源" key="resource" :width="180">
        <template #default="{ record }">
          {{ getOutputField(record, 'ka.target.resource') }} /
          {{ getOutputField(record, 'ka.target.name') }}
        </template>
      </a-table-column>
      <a-table-column title="操作用户" key="user" :width="260">
        <template #default="{ record }">
          {{ getOutputField(record, 'ka.user.name') }}
        </template>
      </a-table-column>
      <a-table-column title="时间" data-index="time" key="time" :width="170">
        <template #default="{ record }">
          {{ formatTime(record.time) }}
        </template>
      </a-table-column>

      <template #expandedRowRender="{ record }">
        <div class="alert-detail">
          <div class="detail-output">
            <span>告警摘要</span>
            <p>{{ record.output }}</p>
          </div>
          <a-descriptions bordered size="small" :column="{ xxl: 2, xl: 2, lg: 1, md: 1, sm: 1, xs: 1 }">
            <a-descriptions-item label="verb">
              {{ getOutputField(record, 'ka.verb') }}
            </a-descriptions-item>
            <a-descriptions-item label="user">
              {{ getOutputField(record, 'ka.user.name') }}
            </a-descriptions-item>
            <a-descriptions-item label="namespace">
              {{ getOutputField(record, 'ka.target.namespace') }}
            </a-descriptions-item>
            <a-descriptions-item label="resource">
              {{ getOutputField(record, 'ka.target.resource') }}
            </a-descriptions-item>
            <a-descriptions-item label="name">
              {{ getOutputField(record, 'ka.target.name') }}
            </a-descriptions-item>
            <a-descriptions-item label="response code">
              {{ getOutputField(record, 'ka.response.code') }}
            </a-descriptions-item>
          </a-descriptions>
          <pre>{{ stringifyAlert(record) }}</pre>
        </div>
      </template>
    </a-table>
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
  color: #fff;
  background: linear-gradient(135deg, #1677ff, #102a43);
  border-radius: 8px;
}

.alert-hero-main h2 {
  margin: 10px 0 4px;
  font-size: 24px;
  font-weight: 600;
}

.alert-hero-main p {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
}

.latest-alert {
  min-width: 260px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 8px;
}

.latest-alert span,
.latest-alert small {
  display: block;
  color: rgba(255, 255, 255, 0.72);
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
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.severity-card::before {
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  content: '';
  background: #1677ff;
}

.severity-card span {
  display: block;
  color: rgba(0, 0, 0, 0.45);
}

.severity-card strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  line-height: 1;
}

.severity-red::before {
  background: #ff4d4f;
}

.severity-orange::before {
  background: #fa8c16;
}

.severity-blue::before,
.severity-total::before {
  background: #1677ff;
}

.alert-table {
  max-width: 100%;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

:deep(.alert-table .ant-table table) {
  table-layout: fixed !important;
}

:deep(.alert-row-red td:first-child) {
  border-left: 4px solid #ff4d4f;
}

:deep(.alert-row-orange td:first-child) {
  border-left: 4px solid #fa8c16;
}

:deep(.alert-row-blue td:first-child) {
  border-left: 4px solid #1677ff;
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
  color: rgba(0, 0, 0, 0.45);
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
  background: #f8fbff;
  border: 1px solid #d6e4ff;
  border-radius: 6px;
}

.detail-output span {
  display: block;
  margin-bottom: 6px;
  color: rgba(0, 0, 0, 0.45);
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

.alert-detail pre {
  max-height: 420px;
  margin: 0;
  padding: 16px;
  overflow: auto;
  color: #d6e4ff;
  background: #111827;
  border-radius: 6px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
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
