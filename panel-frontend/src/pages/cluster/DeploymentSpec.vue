<script setup lang="ts">
import {
  deleteDeployment,
  getDeployment,
  getDeploymentAll,
  getDeploymentEvents,
  getDeploymentLog,
  getDeploymentPods,
} from '@/api/clusterController.ts'
import { getNodeDisplayName } from '@/utils/nodeDisplayName.ts'
import {
  DeleteOutlined,
  FileTextOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'
import { Modal, message } from 'ant-design-vue'
import { computed, h, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type Metadata = {
  name?: string
  namespace?: string
  uid?: string
  creationTimestamp?: string
  labels?: Record<string, string>
  annotations?: Record<string, string>
}

type Container = {
  name?: string
  image?: string
  imagePullPolicy?: string
  command?: string[]
  args?: string[]
  ports?: Record<string, unknown>[]
  env?: Record<string, unknown>[]
  resources?: Record<string, unknown>
  volumeMounts?: Record<string, unknown>[]
}

type DeploymentCondition = {
  type?: string
  status?: string
  reason?: string
  message?: string
  lastUpdateTime?: string
  lastTransitionTime?: string
}

type DeploymentDetail = API.DeploymentSpecResponse & {
  metadata?: Metadata
  spec?: {
    replicas?: number
    selector?: {
      matchLabels?: Record<string, string>
      matchExpressions?: Array<{ key?: string; operator?: string; values?: string[] }>
    }
    strategy?: {
      type?: string
      rollingUpdate?: Record<string, unknown>
    }
    template?: {
      metadata?: Metadata
      spec?: {
        initContainers?: Container[]
        containers?: Container[]
        volumes?: Record<string, unknown>[]
      }
    }
  }
  status?: {
    replicas?: number
    readyReplicas?: number
    updatedReplicas?: number
    availableReplicas?: number
    unavailableReplicas?: number
    conditions?: DeploymentCondition[]
  }
}

type PodItem = API.PodVO & {
  metadata?: Metadata
  spec?: {
    nodeName?: string
  }
  status?: {
    phase?: string
    podIP?: string
    cpu?: string
    memory?: string
    containerStatuses?: Array<{
      ready?: boolean
      restartCount?: number
    }>
  }
}

type EventItem = API.EventVO & {
  metadata?: Metadata
  involvedObject?: {
    kind?: string
    name?: string
    fieldPath?: string
  }
}

const route = useRoute()
const router = useRouter()
const namespace = ref(route.params.namespace as string)
const deploymentName = ref(route.params.name as string)
const activeTab = ref('overview')
const loading = ref(false)
const deleting = ref(false)
const yamlLoading = ref(false)
const podsLoading = ref(false)
const eventsLoading = ref(false)
const logsLoading = ref(false)
const deploymentData = ref<DeploymentDetail>({})
const yamlContent = ref('')
const pods = ref<PodItem[]>([])
const events = ref<EventItem[]>([])
const selectedPodName = ref<string>()
const selectedContainerName = ref<string>()
const logKeyword = ref('')
const logs = ref('')

const containers = computed(() => deploymentData.value.spec?.template?.spec?.containers || [])
const initContainers = computed(() => deploymentData.value.spec?.template?.spec?.initContainers || [])
const volumes = computed(() => deploymentData.value.spec?.template?.spec?.volumes || [])
const filteredLogs = computed(() => {
  const keyword = logKeyword.value.trim()
  if (!keyword) return logs.value
  return logs.value
    .split('\n')
    .filter((line) => line.includes(keyword))
    .join('\n')
})

onMounted(() => {
  loadDeploymentData()
})

watch(activeTab, (newTab) => {
  if (newTab === 'yaml') {
    loadYamlData()
  }
  if (newTab === 'pods') {
    loadPods()
  }
  if (newTab === 'logs') {
    loadLogsTab()
  }
  if (newTab === 'events') {
    loadEvents()
  }
})

watch(selectedPodName, () => {
  if (activeTab.value === 'logs') {
    loadLogs()
  }
})

watch(selectedContainerName, () => {
  if (activeTab.value === 'logs' && selectedPodName.value) {
    loadLogs()
  }
})

const loadDeploymentData = async () => {
  try {
    loading.value = true
    const response = await getDeployment({
      namespace: namespace.value,
      deployment_name: deploymentName.value,
    } as API.getDeploymentParams)
    deploymentData.value = (response.data.data || {}) as DeploymentDetail
    if (!selectedContainerName.value && containers.value.length > 0) {
      selectedContainerName.value = containers.value[0]?.name
    }
  } catch (error) {
    console.error('Failed to load deployment data:', error)
    message.error('加载 Deployment 失败')
  } finally {
    loading.value = false
  }
}

const loadYamlData = async () => {
  if (yamlContent.value && !yamlLoading.value) return
  try {
    yamlLoading.value = true
    const response = await getDeploymentAll({
      namespace: namespace.value,
      deployment_name: deploymentName.value,
    } as API.getDeploymentAllParams)
    yamlContent.value = response.data.data || ''
  } catch (error) {
    console.error('Failed to load deployment YAML:', error)
  } finally {
    yamlLoading.value = false
  }
}

const loadPods = async () => {
  try {
    podsLoading.value = true
    const response = await getDeploymentPods({
      namespace: namespace.value,
      deployment_name: deploymentName.value,
    } as API.getDeploymentPodsParams)
    pods.value = (response.data.data?.items || []) as PodItem[]
    if (!selectedPodName.value && pods.value.length > 0) {
      selectedPodName.value = pods.value[0]?.metadata?.name
    }
  } catch (error) {
    console.error('Failed to load deployment pods:', error)
  } finally {
    podsLoading.value = false
  }
}

const loadEvents = async () => {
  try {
    eventsLoading.value = true
    const response = await getDeploymentEvents({
      namespace: namespace.value,
      deployment_name: deploymentName.value,
    } as API.getDeploymentEventsParams)
    events.value = (response.data.data?.items || []) as EventItem[]
  } catch (error) {
    console.error('Failed to load deployment events:', error)
  } finally {
    eventsLoading.value = false
  }
}

const loadLogsTab = async () => {
  if (pods.value.length === 0) {
    await loadPods()
  }
  if (!selectedContainerName.value && containers.value.length > 0) {
    selectedContainerName.value = containers.value[0]?.name
  }
  await loadLogs()
}

const loadLogs = async () => {
  if (!selectedPodName.value) {
    logs.value = ''
    return
  }
  try {
    logsLoading.value = true
    const params: API.getDeploymentLogParams = {
      namespace: namespace.value,
      deployment_name: deploymentName.value,
      podName: selectedPodName.value,
      tailLines: 500,
    }
    if (selectedContainerName.value) {
      params.container = selectedContainerName.value
    }
    const response = await getDeploymentLog(params)
    logs.value = response.data.data || ''
  } catch (error) {
    console.error('Failed to load deployment logs:', error)
    message.error('加载日志失败')
  } finally {
    logsLoading.value = false
  }
}

const handleRefresh = () => {
  yamlContent.value = ''
  loadDeploymentData()
  if (activeTab.value === 'yaml') loadYamlData()
  if (activeTab.value === 'pods') loadPods()
  if (activeTab.value === 'logs') loadLogsTab()
  if (activeTab.value === 'events') loadEvents()
}

const handleDescribe = () => {
  activeTab.value = 'yaml'
  loadYamlData()
}

const handleDelete = () => {
  Modal.confirm({
    title: '删除 Deployment',
    content: `确认删除 ${namespace.value}/${deploymentName.value} 吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        deleting.value = true
        await deleteDeployment({
          namespace: namespace.value,
          deployment_name: deploymentName.value,
        } as API.deleteDeploymentParams)
        message.success('删除成功')
        router.push({ path: '/deployments', query: { namespace: namespace.value } })
      } catch (error) {
        console.error('Failed to delete deployment:', error)
        message.error('删除失败')
      } finally {
        deleting.value = false
      }
    },
  })
}

const getCondition = (type: string) => {
  return deploymentData.value.status?.conditions?.find((condition) => condition.type === type)
}

const getDeploymentStatus = () => {
  const available = getCondition('Available')
  if (available?.status === 'True') return '可用'
  const progressing = getCondition('Progressing')
  if (progressing?.status === 'True') return '更新中'
  return '不可用'
}

const getStatusColor = () => {
  const status = getDeploymentStatus()
  if (status === '可用') return 'green'
  if (status === '更新中') return 'blue'
  return 'red'
}

const formatSelector = () => {
  const labels = deploymentData.value.spec?.selector?.matchLabels || {}
  return Object.entries(labels)
}

const getReadyText = () => {
  const ready = deploymentData.value.status?.readyReplicas || 0
  const total = deploymentData.value.spec?.replicas ?? deploymentData.value.status?.replicas ?? 0
  return `${ready} / ${total}`
}

const getReadyContainers = (pod: PodItem) => {
  const statuses = pod.status?.containerStatuses || []
  if (statuses.length === 0) return '0 / 0'
  const ready = statuses.filter((status) => status.ready).length
  return `${ready} / ${statuses.length}`
}

const getRestartCount = (pod: PodItem) => {
  return (pod.status?.containerStatuses || []).reduce(
    (sum, status) => sum + (status.restartCount || 0),
    0,
  )
}

const getPodStatusColor = (pod: PodItem) => {
  if (pod.status?.phase === 'Running') return 'green'
  if (pod.status?.phase === 'Pending') return 'gold'
  if (pod.status?.phase === 'Failed') return 'red'
  if (pod.status?.phase === 'Succeeded') return 'blue'
  return 'default'
}

const formatRecord = (record?: Record<string, unknown>) => {
  if (!record || Object.keys(record).length === 0) return '-'
  return JSON.stringify(record)
}

const formatContainerPorts = (container: Container) => {
  const ports = container.ports || []
  if (ports.length === 0) return '-'
  return ports
    .map((port) => `${port.protocol || 'TCP'}:${port.containerPort || '-'}`)
    .join(', ')
}
</script>

<template>
  <div class="deployment-spec">
    <div class="deployment-header">
      <div>
        <h2>{{ deploymentName }}</h2>
        <div class="namespace-line">命名空间：{{ namespace }}</div>
      </div>
      <div class="deployment-actions">
        <a-button :loading="loading" :icon="h(ReloadOutlined)" @click="handleRefresh">
          刷新
        </a-button>
        <a-button :icon="h(FileTextOutlined)" @click="handleDescribe">查看YAML</a-button>
        <a-button
          danger
          type="primary"
          :loading="deleting"
          :icon="h(DeleteOutlined)"
          @click="handleDelete"
        >
          删除
        </a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="overview" tab="总览">
          <div class="overview-content">
            <a-card title="状态总览">
              <div class="status-grid">
                <div class="status-item">
                  <span class="status-label">状态</span>
                  <a-tag :color="getStatusColor()">{{ getDeploymentStatus() }}</a-tag>
                </div>
                <div class="status-item">
                  <span class="status-label">就绪副本</span>
                  <span class="status-value">{{ getReadyText() }}</span>
                </div>
                <div class="status-item">
                  <span class="status-label">已更新副本</span>
                  <span class="status-value">{{ deploymentData.status?.updatedReplicas || 0 }}</span>
                </div>
                <div class="status-item">
                  <span class="status-label">可用副本</span>
                  <span class="status-value">{{ deploymentData.status?.availableReplicas || 0 }}</span>
                </div>
              </div>
            </a-card>

            <a-card title="Deployment信息">
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">创建时间</span>
                  <span class="info-value">{{ deploymentData.metadata?.creationTimestamp || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">更新策略</span>
                  <span class="info-value">{{ deploymentData.spec?.strategy?.type || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">副本数</span>
                  <span class="info-value">{{ deploymentData.spec?.replicas ?? '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">选择器</span>
                  <span class="info-value">
                    <a-tag v-for="[key, value] in formatSelector()" :key="key">
                      {{ key }}: {{ value }}
                    </a-tag>
                    <span v-if="formatSelector().length === 0">-</span>
                  </span>
                </div>
                <div class="info-item info-wide">
                  <span class="info-label">注解</span>
                  <span class="info-value tag-list">
                    <a-tag
                      v-for="(value, key) in deploymentData.metadata?.annotations"
                      :key="key"
                    >
                      {{ key }}: {{ value }}
                    </a-tag>
                    <span v-if="!deploymentData.metadata?.annotations">-</span>
                  </span>
                </div>
              </div>
            </a-card>

            <a-card v-if="initContainers.length > 0" :title="`初始化容器（${initContainers.length}）`">
              <a-table :data-source="initContainers" :pagination="false" row-key="name" size="middle">
                <a-table-column title="名称" key="name">
                  <template #default="{ record }">
                    <a-tag color="blue">{{ record.name }}</a-tag>
                  </template>
                </a-table-column>
                <a-table-column title="镜像" key="image">
                  <template #default="{ record }">{{ record.image || '-' }}</template>
                </a-table-column>
                <a-table-column title="拉取策略" key="imagePullPolicy">
                  <template #default="{ record }">{{ record.imagePullPolicy || '-' }}</template>
                </a-table-column>
              </a-table>
            </a-card>

            <a-card :title="`容器（${containers.length}）`">
              <a-table :data-source="containers" :pagination="false" row-key="name" size="middle">
                <a-table-column title="名称" key="name">
                  <template #default="{ record }">
                    <a-tag color="blue">{{ record.name }}</a-tag>
                  </template>
                </a-table-column>
                <a-table-column title="镜像" key="image">
                  <template #default="{ record }">{{ record.image || '-' }}</template>
                </a-table-column>
                <a-table-column title="端口" key="ports">
                  <template #default="{ record }">{{ formatContainerPorts(record) }}</template>
                </a-table-column>
                <a-table-column title="拉取策略" key="imagePullPolicy">
                  <template #default="{ record }">{{ record.imagePullPolicy || '-' }}</template>
                </a-table-column>
              </a-table>
            </a-card>

            <a-card title="条件">
              <a-table
                :data-source="deploymentData.status?.conditions || []"
                :pagination="false"
                row-key="type"
                size="middle"
              >
                <a-table-column title="类型" key="type">
                  <template #default="{ record }">
                    <a-tag color="blue">{{ record.type }}</a-tag>
                  </template>
                </a-table-column>
                <a-table-column title="状态" key="status">
                  <template #default="{ record }">{{ record.status || '-' }}</template>
                </a-table-column>
                <a-table-column title="消息" key="message">
                  <template #default="{ record }">{{ record.message || '-' }}</template>
                </a-table-column>
                <a-table-column title="最近转换时间" key="lastTransitionTime">
                  <template #default="{ record }">{{ record.lastTransitionTime || '-' }}</template>
                </a-table-column>
              </a-table>
            </a-card>
          </div>
        </a-tab-pane>

        <a-tab-pane key="yaml" tab="YAML">
          <a-spin :spinning="yamlLoading">
            <a-textarea
              :value="yamlContent"
              :auto-size="{ minRows: 20, maxRows: 50 }"
              read-only
              class="yaml-textarea"
            />
          </a-spin>
        </a-tab-pane>

        <a-tab-pane key="pods" :tab="`Pod（${pods.length}）`">
          <a-table
            :data-source="pods"
            :loading="podsLoading"
            row-key="metadata.name"
            :pagination="{ pageSize: 10 }"
          >
            <a-table-column title="名称" key="name">
              <template #default="{ record }">
                <router-link
                  v-if="record.metadata?.name"
                  :to="`/pods/${namespace}/${record.metadata.name}`"
                >
                  {{ record.metadata.name }}
                </router-link>
                <span v-else>-</span>
              </template>
            </a-table-column>
            <a-table-column title="就绪" key="ready">
              <template #default="{ record }">{{ getReadyContainers(record) }}</template>
            </a-table-column>
            <a-table-column title="重启次数" key="restart">
              <template #default="{ record }">{{ getRestartCount(record) }}</template>
            </a-table-column>
            <a-table-column title="状态" key="status">
              <template #default="{ record }">
                <a-tag :color="getPodStatusColor(record)">{{ record.status?.phase || '-' }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column title="CPU" key="cpu">
              <template #default="{ record }">{{ record.status?.cpu || '-' }}</template>
            </a-table-column>
            <a-table-column title="内存" key="memory">
              <template #default="{ record }">{{ record.status?.memory || '-' }}</template>
            </a-table-column>
            <a-table-column title="IP" key="ip">
              <template #default="{ record }">{{ record.status?.podIP || '-' }}</template>
            </a-table-column>
            <a-table-column title="节点" key="node">
              <template #default="{ record }">{{ getNodeDisplayName(record.spec?.nodeName) }}</template>
            </a-table-column>
            <a-table-column title="创建时间" key="created">
              <template #default="{ record }">
                {{ record.metadata?.creationTimestamp || '-' }}
              </template>
            </a-table-column>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="logs" tab="日志">
          <div class="logs-panel">
            <div class="logs-toolbar">
              <a-input-search
                v-model:value="logKeyword"
                class="logs-search"
                placeholder="过滤日志..."
                allow-clear
              />
              <a-select
                v-model:value="selectedContainerName"
                class="logs-select"
                allow-clear
                placeholder="容器"
              >
                <a-select-option
                  v-for="container in containers"
                  :key="container.name"
                  :value="container.name"
                >
                  {{ container.name }}
                </a-select-option>
              </a-select>
              <a-select v-model:value="selectedPodName" class="logs-pod-select" placeholder="Pod">
                <a-select-option
                  v-for="pod in pods"
                  :key="pod.metadata?.name"
                  :value="pod.metadata?.name"
                >
                  {{ pod.metadata?.name }}
                </a-select-option>
              </a-select>
              <a-button :loading="logsLoading" :icon="h(ReloadOutlined)" @click="loadLogs">
                刷新
              </a-button>
            </div>
            <a-spin :spinning="logsLoading">
              <pre class="logs-output">{{ filteredLogs }}</pre>
            </a-spin>
          </div>
        </a-tab-pane>

        <a-tab-pane v-if="volumes.length > 0" key="volumes" :tab="`存储卷（${volumes.length}）`">
          <a-table :data-source="volumes" :pagination="false" row-key="name">
            <a-table-column title="名称" key="name">
              <template #default="{ record }">{{ record.name || '-' }}</template>
            </a-table-column>
            <a-table-column title="来源" key="source">
              <template #default="{ record }">{{ formatRecord(record) }}</template>
            </a-table-column>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="events" tab="事件">
          <a-table
            :data-source="events"
            :loading="eventsLoading"
            row-key="metadata.name"
            :pagination="{ pageSize: 10 }"
          >
            <a-table-column title="类型" key="type">
              <template #default="{ record }">
                <a-tag :color="record.type === 'Warning' ? 'red' : 'blue'">
                  {{ record.type || '-' }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column title="原因" key="reason">
              <template #default="{ record }">{{ record.reason || '-' }}</template>
            </a-table-column>
            <a-table-column title="对象" key="object">
              <template #default="{ record }">
                {{ record.involvedObject?.kind || '-' }}/{{ record.involvedObject?.name || '-' }}
              </template>
            </a-table-column>
            <a-table-column title="消息" key="message">
              <template #default="{ record }">{{ record.message || '-' }}</template>
            </a-table-column>
            <a-table-column title="次数" key="count">
              <template #default="{ record }">{{ record.count || '-' }}</template>
            </a-table-column>
            <a-table-column title="最近出现" key="lastSeen">
              <template #default="{ record }">
                {{ record.lastTimestamp || record.eventTime || record.metadata?.creationTimestamp || '-' }}
              </template>
            </a-table-column>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </div>
</template>

<style scoped>
.deployment-spec {
  padding: 0 24px;
}

.deployment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.deployment-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
}

.namespace-line {
  margin-top: 6px;
  color: #666;
  font-size: 17px;
}

.deployment-actions,
.logs-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.overview-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-grid,
.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.info-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.status-item,
.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.info-wide {
  grid-column: 1 / -1;
}

.status-label,
.info-label {
  color: #666;
  font-size: 16px;
}

.status-value,
.info-value {
  font-weight: 500;
  word-break: break-all;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.yaml-textarea {
  font-family: 'Courier New', Courier, monospace;
  line-height: 1.5;
  color: #333;
}

.logs-panel {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.logs-toolbar {
  justify-content: flex-end;
  padding: 12px;
  background: #fff;
}

.logs-search {
  width: 280px;
}

.logs-select {
  width: 160px;
}

.logs-pod-select {
  width: 280px;
}

.logs-output {
  min-height: 420px;
  max-height: 640px;
  margin: 0;
  padding: 14px;
  overflow: auto;
  color: #38d26a;
  background: #050505;
  font-family: 'Courier New', Courier, monospace;
  font-size: 16px;
  line-height: 1.55;
  white-space: pre-wrap;
}

@media (max-width: 900px) {
  .deployment-header,
  .page-header,
  .logs-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .status-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .logs-search,
  .logs-select,
  .logs-pod-select {
    width: 100%;
  }
}
</style>
