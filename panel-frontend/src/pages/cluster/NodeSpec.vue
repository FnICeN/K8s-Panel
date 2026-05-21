<script setup lang="ts">
import { ref, onMounted, h, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getNode, getNodeAll } from '@/api/clusterController.ts'
import {
  BgColorsOutlined,
  InfoCircleOutlined,
  LineOutlined,
  RedoOutlined,
  StopOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const nodeName = ref(route.params.name as string)
const activeTab = ref('overview')
const loading = ref(true)
const nodeData = ref<API.NodeSpecResponse>({})
const yamlContent = ref('')
const yamlLoading = ref(false)

const loadNodeData = async () => {
  try {
    loading.value = true
    const response = await getNode({ node_name: nodeName.value })
    if (response.data.data) {
      nodeData.value = response.data.data
    }
  } catch (error) {
    console.error('Failed to load node data:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadNodeData()
})

// 操作按钮处理函数
const handleRefresh = () => {
  loadNodeData()
  loadYamlData()
}

const handleDescribe = () => {
  console.log('Describe clicked')
}

const handleDrain = () => {
  console.log('Drain clicked')
}

const handleCordon = () => {
  console.log('Cordon clicked')
}

const handleTaint = () => {
  console.log('Taint clicked')
}

const getNodeStatus = (node: API.NodeSpecResponse) => {
  const condition = node.status?.conditions?.find((c) => c.type === 'Ready')
  return condition?.status === 'True' ? 'Ready' : 'NotReady'
}

const getNodeStatusColor = (node: API.NodeSpecResponse) => {
  const condition = node.status?.conditions?.find((c) => c.type === 'Ready')
  return condition?.status === 'True' ? 'green' : 'red'
}

const getNodeRole = (node: API.NodeSpecResponse) => {
  const labels = node.metadata?.labels
  if (labels && 'node-role.kubernetes.io/control-plane' in labels) {
    return 'control-plane'
  } else {
    return 'node'
  }
}

const loadYamlData = async () => {
  if (yamlContent.value && !yamlLoading.value) return

  try {
    yamlLoading.value = true
    const response = await getNodeAll({ node_name: nodeName.value })
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
</script>

<template>
  <div class="node-spec">
    <div class="node-header">
      <h2>{{ nodeName }}</h2>
      <div class="node-actions">
        <a-button @click="handleRefresh" :loading="loading" :icon="h(RedoOutlined)">刷新</a-button>
        <a-button @click="handleDescribe" :icon="h(InfoCircleOutlined)">描述</a-button>
        <a-button @click="handleDrain" :icon="h(LineOutlined)">驱逐</a-button>
        <a-button @click="handleCordon" :icon="h(StopOutlined)">禁入</a-button>
        <a-button @click="handleTaint" :icon="h(BgColorsOutlined)">污点</a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="overview" tab="总览">
          <div class="overview-content">
            <a-card title="状态总览" class="status-overview">
              <div class="status-item">
                <a-tag :color="getNodeStatusColor(nodeData)">{{ getNodeStatus(nodeData) }}</a-tag>
                <span class="status-label">角色:</span>
                <span class="status-value">{{ getNodeRole(nodeData) }}</span>
                <span class="status-label">内部IP:</span>
                <span class="status-value">{{
                  nodeData.status?.addresses?.find((a) => a.type === 'InternalIP')?.address
                }}</span>
                <span class="status-label">Pod CIDR:</span>
                <span class="status-value">{{ nodeData.spec?.podCIDR }}</span>
              </div>
            </a-card>

            <a-card title="节点信息" class="node-info">
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">创建时间:</span>
                  <span class="info-value">{{ nodeData.metadata?.creationTimestamp }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">Kubelet 版本:</span>
                  <span class="info-value">{{ nodeData.status?.nodeInfo?.kubeletVersion }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">名称:</span>
                  <span class="info-value">{{ nodeName }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">外部IP:</span>
                  <span class="info-value"></span>
                </div>
                <div class="info-item">
                  <span class="info-label">操作系统:</span>
                  <span class="info-value">{{ nodeData.status?.nodeInfo?.osImage }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">内核版本:</span>
                  <span class="info-value">{{ nodeData.status?.nodeInfo?.kernelVersion }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">架构:</span>
                  <span class="info-value">{{ nodeData.status?.nodeInfo?.architecture }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">容器运行时:</span>
                  <span class="info-value">2.20.6</span>
                </div>
                <div class="info-item">
                  <span class="info-label">Kube Proxy 版本:</span>
                  <span class="info-value">{{ nodeData.status?.nodeInfo?.kubeletVersion }}</span>
                </div>
              </div>

              <div class="labels-annotations">
                <div class="labels">
                  <h4>标签</h4>
                  <div class="label-list">
                    <a-tag v-for="(value, key) in nodeData.metadata?.labels" :key="key">
                      {{ key }}: {{ value }}
                    </a-tag>
                  </div>
                </div>
                <div class="annotations">
                  <h4>附注</h4>
                  <div class="annotation-list">
                    <a-tag v-for="(value, key) in nodeData.metadata?.annotations" :key="key">
                      {{ key }}: {{ value }}
                    </a-tag>
                  </div>
                </div>
              </div>
            </a-card>

            <a-card title="资源可用性" class="resource-capacity">
              <div class="resource-grid">
                <div class="resource-item">
                  <h4>CPU & Memory</h4>
                  <div class="resource-detail">
                    <div class="resource-row">
                      <span class="resource-label">CPU:</span>
                      <!--                      <span class="resource-value">8 cores</span>-->
                    </div>
                    <div class="resource-row">
                      <span class="resource-label">上限:</span>
                      <span class="resource-value">{{ nodeData.status?.capacity?.cpu }} cores</span>
                    </div>
                    <div class="resource-row">
                      <span class="resource-label">可分配:</span>
                      <span class="resource-value"
                        >{{ nodeData.status?.allocatable?.cpu }} cores</span
                      >
                    </div>
                    <div class="resource-row">
                      <span class="resource-label">Memory:</span>
                      <!--                      <span class="resource-value">14.95 GiB</span>-->
                    </div>
                    <div class="resource-row">
                      <span class="resource-label">上限:</span>
                      <span class="resource-value">{{ nodeData.status?.capacity?.memory }}</span>
                    </div>
                    <div class="resource-row">
                      <span class="resource-label">可分配:</span>
                      <span class="resource-value">{{ nodeData.status?.allocatable?.memory }}</span>
                    </div>
                  </div>
                </div>
                <div class="resource-item">
                  <h4>Pods & Storage</h4>
                  <div class="resource-detail">
                    <div class="resource-row">
                      <span class="resource-label">Pods:</span>
                      <!--                      <span class="resource-value">110</span>-->
                    </div>
                    <div class="resource-row">
                      <span class="resource-label">上限:</span>
                      <span class="resource-value">{{ nodeData.status?.capacity?.pods }}</span>
                    </div>
                    <div class="resource-row">
                      <span class="resource-label">可分配:</span>
                      <span class="resource-value">{{ nodeData.status?.allocatable?.pods }}</span>
                    </div>
                    <div class="resource-row">
                      <span class="resource-label">Storage:</span>
                      <!--                      <span class="resource-value">823.3 GiB</span>-->
                    </div>
                    <div class="resource-row">
                      <span class="resource-label">上限:</span>
                      <span class="resource-value">{{
                        nodeData.status?.capacity?.['ephemeral-storage']
                      }}</span>
                    </div>
                    <div class="resource-row">
                      <span class="resource-label">可分配:</span>
                      <span class="resource-value">{{
                        nodeData.status?.allocatable?.['ephemeral-storage']
                      }}</span>
                    </div>
                  </div>
                </div>
              </div>
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
                readOnly
                class="yaml-textarea"
              />
            </a-spin>
          </div>
        </a-tab-pane>
        <a-tab-pane key="pods" tab="Pods">
          <div class="tab-placeholder">Pods 列表将在此处显示</div>
        </a-tab-pane>
        <a-tab-pane key="events" tab="事件">
          <div class="tab-placeholder">事件列表将在此处显示</div>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </div>
</template>

<style scoped>
.node-spec {
  padding: 0 24px;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.node-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.node-actions {
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

.status-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-label {
  font-weight: 500;
  margin-left: 16px;
}

.status-value {
  margin-left: 8px;
}

.node-info {
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

.resource-capacity {
  margin-bottom: 16px;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.resource-item h4 {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
}

.resource-detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resource-row {
  display: flex;
  justify-content: space-between;
}

.resource-label {
  font-size: 14px;
  color: #666;
}

.resource-value {
  font-size: 14px;
  font-weight: 500;
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
</style>
