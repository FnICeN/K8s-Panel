<script setup lang="ts">
import { ref, onMounted, h, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getPod, getPodAll } from '@/api/clusterController.ts'
import { RedoOutlined } from '@ant-design/icons-vue'

const route = useRoute()
const namespace = ref(route.params.namespace as string)
const podName = ref(route.params.name as string)
const activeTab = ref('overview')
const loading = ref(true)
const podData = ref<API.PodSpecResponse>({})
const yamlContent = ref('')
const yamlLoading = ref(false)

const loadPodData = async () => {
  try {
    loading.value = true
    const response = await getPod({ namespace: namespace.value, pod_name: podName.value })
    if (response.data.data) {
      podData.value = response.data.data
    }
  } catch (error) {
    console.error('Failed to load pod data:', error)
  } finally {
    loading.value = false
  }
}

const loadYamlData = async () => {
  if (yamlContent.value && !yamlLoading.value) return

  try {
    yamlLoading.value = true
    const response = await getPodAll({ namespace: namespace.value, pod_name: podName.value })
    if (response.data.data) {
      yamlContent.value = response.data.data
    }
  } catch (error) {
    console.error('Failed to load YAML data:', error)
  } finally {
    yamlLoading.value = false
  }
}

watch(activeTab, (newTab) => {
  if (newTab === 'yaml') {
    loadYamlData()
  }
})

onMounted(() => {
  loadPodData()
})

const handleRefresh = () => {
  loadPodData()
}

const getPodStatusColor = (pod: API.PodSpecResponse) => {
  const phase = pod.status?.phase
  switch (phase) {
    case 'Running':
      return 'green'
    case 'Pending':
      return 'gold'
    case 'Failed':
      return 'red'
    case 'Succeeded':
      return 'blue'
    default:
      return 'default'
  }
}

const getReadyContainers = (pod: API.PodSpecResponse) => {
  const containerStatuses = pod.status?.containerStatuses
  if (!containerStatuses) return '0 / 0'
  const ready = containerStatuses.filter((c) => c.ready).length
  const total = containerStatuses.length
  return `${ready} / ${total}`
}

const getRestartCount = (pod: API.PodSpecResponse) => {
  const containerStatuses = pod.status?.containerStatuses
  if (!containerStatuses) return 0
  return containerStatuses.reduce((sum, c) => sum + (c.restartCount || 0), 0)
}
</script>

<template>
  <div class="pod-spec">
    <div class="pod-header">
      <h2>{{ podName }}</h2>
      <div class="pod-actions">
        <a-button @click="handleRefresh" :loading="loading" :icon="h(RedoOutlined)">刷新</a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="overview" tab="总览">
          <div class="overview-content">
            <a-card title="状态总览" class="status-overview">
              <div class="status-grid">
                <div class="status-item">
                  <span class="status-label">状态</span>
                  <a-tag :color="getPodStatusColor(podData)">
                    {{ podData.status?.phase || '-' }}
                  </a-tag>
                </div>
                <div class="status-item">
                  <span class="status-label">就绪容器</span>
                  <span class="status-value">{{ getReadyContainers(podData) }}</span>
                </div>
                <div class="status-item">
                  <span class="status-label">重启次数总和</span>
                  <span class="status-value">{{ getRestartCount(podData) }}</span>
                </div>
                <div class="status-item">
                  <span class="status-label">节点</span>
                  <a :href="`/nodes/${podData.spec?.nodeName}`" v-if="podData.spec?.nodeName">
                    {{ podData.spec?.nodeName }}
                  </a>
                  <span v-else>-</span>
                </div>
              </div>
            </a-card>

            <a-card title="Pod 详情" class="pod-info">
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">创建时间</span>
                  <span class="info-value">{{ podData.metadata?.creationTimestamp || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">启动时间</span>
                  <span class="info-value">{{ podData.status?.startTime || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">Pod IP</span>
                  <span class="info-value">{{ podData.status?.podIP || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">主机 IP</span>
                  <span class="info-value">{{ podData.status?.hostIP || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">所有者</span>
                  <span class="info-value">-</span>
                </div>
                <div class="info-item">
                  <span class="info-label">QoS 类</span>
                  <span class="info-value">-</span>
                </div>
              </div>

              <div class="labels-annotations">
                <div class="labels">
                  <h4>标签</h4>
                  <div class="label-list">
                    <a-tag v-for="(value, key) in podData.metadata?.labels" :key="key">
                      {{ key }}: {{ value }}
                    </a-tag>
                  </div>
                </div>
                <div class="annotations">
                  <h4>附注</h4>
                  <div class="annotation-list">
                    <a-tag v-for="(value, key) in podData.metadata?.annotations" :key="key">
                      {{ key }}: {{ value }}
                    </a-tag>
                  </div>
                </div>
              </div>
            </a-card>

            <a-card title="容器" class="containers-overview">
              <a-collapse>
                <a-collapse-panel
                  v-for="(container, index) in podData.spec?.containers"
                  :key="index"
                >
                  <template #header>
                    <span>{{ container.name }}</span>
                  </template>
                  <div class="container-preview">
                    <span class="container-image">{{ container.image }}</span>
                    <a-tag v-if="podData.status?.containerStatuses?.[index]?.ready" color="green"
                      >Ready</a-tag
                    >
                  </div>
                </a-collapse-panel>
              </a-collapse>
            </a-card>
          </div>
        </a-tab-pane>

        <a-tab-pane key="yaml" tab="YAML">
          <div class="yaml-container">
            <a-spin :spinning="yamlLoading">
              <a-textarea
                :value="yamlContent"
                :auto-size="{ minRows: 20, maxRows: 50 }"
                placeholder="YAML 内容加载中..."
                read-only
                class="yaml-textarea"
              />
            </a-spin>
          </div>
        </a-tab-pane>

        <a-tab-pane key="containers" tab="容器">
          <div class="containers-content">
            <a-card
              v-for="(container, index) in podData.spec?.containers"
              :key="index"
              class="container-card"
            >
              <div class="container-header">
                <h3>{{ container.name }}</h3>
                <a-tag v-if="podData.status?.containerStatuses?.[index]?.ready" color="green"
                  >Ready</a-tag
                >
                <a-tag v-else color="red">Not Ready</a-tag>
              </div>
              <div class="container-details">
                <div class="detail-row">
                  <span class="detail-label">镜像</span>
                  <span class="detail-value">{{ container.image }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">镜像拉取策略</span>
                  <span class="detail-value">Always</span>
                </div>
                <div class="detail-section" v-if="container.ports && container.ports.length > 0">
                  <h4>端口列表</h4>
                  <div class="port-list">
                    <div v-for="(port, pIndex) in container.ports" :key="pIndex" class="port-item">
                      <span class="port-name">{{ port.name || '-' }}</span>
                      <span class="port-number"
                        >{{ port.containerPort }}/{{ port.protocol || 'TCP' }}</span
                      >
                    </div>
                  </div>
                </div>
                <div class="detail-section" v-if="container.args && container.args.length > 0">
                  <h4>参数列表</h4>
                  <div class="args-list">
                    <div v-for="(arg, aIndex) in container.args" :key="aIndex" class="arg-item">
                      {{ arg }}
                    </div>
                  </div>
                </div>
                <div class="detail-section">
                  <h4>状态</h4>
                  <div class="state-info">
                    <a-tag color="green">
                      {{
                        podData.status?.containerStatuses?.[index]?.state
                          ? Object.keys(podData.status.containerStatuses[index].state)[0]
                          : 'Unknown'
                      }}
                    </a-tag>
                    <span class="state-time">
                      {{
                        podData.status?.containerStatuses?.[index]?.state?.running?.startedAt || '-'
                      }}
                    </span>
                  </div>
                </div>
                <a-collapse>
                  <a-collapse-panel header="更多详情">
                    <div class="more-details">
                      <div class="detail-row">
                        <span class="detail-label">重启次数</span>
                        <span class="detail-value">{{
                          podData.status?.containerStatuses?.[index]?.restartCount || 0
                        }}</span>
                      </div>
                    </div>
                  </a-collapse-panel>
                </a-collapse>
              </div>
            </a-card>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </div>
</template>

<style scoped>
.pod-spec {
  padding: 0 24px;
}

.pod-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.pod-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.pod-actions {
  display: flex;
  gap: 8px;
}

.overview-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-overview {
  margin-bottom: 16px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.status-label {
  font-size: 14px;
  color: #666;
}

.status-value {
  font-size: 14px;
  font-weight: 500;
}

.pod-info {
  margin-bottom: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 14px;
  color: #666;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
}

.labels-annotations {
  display: flex;
  gap: 24px;
}

.labels,
.annotations {
  flex: 1;
}

.labels h4,
.annotations h4 {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
}

.label-list,
.annotation-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.containers-overview {
  margin-bottom: 16px;
}

.container-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.container-image {
  color: #666;
  font-size: 14px;
}

.tab-placeholder {
  padding: 48px 0;
  text-align: center;
  color: #999;
  font-size: 16px;
}

.yaml-container {
  width: 100%;
}

.yaml-textarea {
  font-family: 'Courier New', Courier, monospace;
  line-height: 1.5;
  color: #333;
}

.containers-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.container-card {
  margin-bottom: 0;
}

.container-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.container-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.container-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
}

.detail-label {
  font-size: 14px;
  color: #666;
}

.detail-value {
  font-size: 14px;
  font-weight: 500;
}

.detail-section {
  margin-top: 8px;
}

.detail-section h4 {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
}

.port-list,
.args-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.port-item {
  display: flex;
  gap: 8px;
}

.port-name {
  color: #666;
}

.port-number {
  font-weight: 500;
}

.arg-item {
  background: #f5f5f5;
  padding: 4px 8px;
  border-radius: 4px;
  font-family: monospace;
}

.state-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.state-time {
  color: #666;
  font-size: 14px;
}

.more-details {
  padding-top: 8px;
}
</style>
