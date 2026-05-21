<script setup lang="ts">
import { deleteService, getService, getServiceAll } from '@/api/clusterController.ts'
import { DeleteOutlined, InfoCircleOutlined, RedoOutlined } from '@ant-design/icons-vue'
import { Modal, message } from 'ant-design-vue'
import { h, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const namespace = ref(route.params.namespace as string)
const serviceName = ref(route.params.name as string)
const activeTab = ref('overview')
const loading = ref(true)
const deleting = ref(false)
const serviceData = ref<API.ServiceSpecResponse>({})
const yamlContent = ref('')
const yamlLoading = ref(false)

onMounted(() => {
  loadServiceData()
})

const loadServiceData = async () => {
  try {
    loading.value = true
    const response = await getService({
      namespace: namespace.value,
      service_name: serviceName.value,
    })
    if (response.data.data) {
      serviceData.value = response.data.data
    }
  } catch (error) {
    console.error('Failed to load service data:', error)
  } finally {
    loading.value = false
  }
}

const loadYamlData = async () => {
  if (yamlContent.value && !yamlLoading.value) return

  try {
    yamlLoading.value = true
    const response = await getServiceAll({
      namespace: namespace.value,
      service_name: serviceName.value,
    })
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

const handleRefresh = () => {
  yamlContent.value = ''
  loadServiceData()
  if (activeTab.value === 'yaml') {
    loadYamlData()
  }
}

const handleDescribe = () => {
  activeTab.value = 'yaml'
  loadYamlData()
}

const handleDelete = () => {
  Modal.confirm({
    title: '删除 Service',
    content: `确认删除 ${namespace.value}/${serviceName.value} 吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        deleting.value = true
        await deleteService({
          namespace: namespace.value,
          service_name: serviceName.value,
        })
        message.success('删除成功')
        router.push({ path: '/services', query: { namespace: namespace.value } })
      } catch (error) {
        console.error('Failed to delete service:', error)
        message.error('删除失败')
      } finally {
        deleting.value = false
      }
    },
  })
}

const getServiceTypeColor = (type?: string) => {
  switch (type) {
    case 'ClusterIP':
      return 'blue'
    case 'NodePort':
      return 'green'
    case 'LoadBalancer':
      return 'purple'
    case 'ExternalName':
      return 'gold'
    default:
      return 'default'
  }
}

const formatTargetPort = (targetPort?: Record<string, any>) => {
  if (targetPort === undefined || targetPort === null) return '-'
  return String(targetPort)
}

const getExternalIP = () => {
  const loadBalancerIngress = serviceData.value.status?.loadBalancer?.ingress || []
  const loadBalancerIPs = loadBalancerIngress
    .map((ingress) => ingress.ip || ingress.hostname)
    .filter((value): value is string => !!value)
  const externalIPs = serviceData.value.spec?.externalIPs || []
  const allIPs = [...externalIPs, ...loadBalancerIPs]

  if (serviceData.value.spec?.externalName) {
    allIPs.push(serviceData.value.spec.externalName)
  }
  if (serviceData.value.spec?.loadBalancerIP) {
    allIPs.push(serviceData.value.spec.loadBalancerIP)
  }
  return allIPs.length > 0 ? allIPs.join(', ') : '-'
}

const getPortsText = () => {
  const ports = serviceData.value.spec?.ports || []
  if (ports.length === 0) return '-'
  return ports.map((port) => `${port.protocol || 'TCP'}:${port.port || '-'}`).join(', ')
}
</script>

<template>
  <div class="service-spec">
    <div class="service-header">
      <div>
        <h2>{{ serviceName }}</h2>
        <div class="namespace-line">命名空间：{{ namespace }}</div>
      </div>
      <div class="service-actions">
        <a-button @click="handleRefresh" :loading="loading" :icon="h(RedoOutlined)">刷新</a-button>
        <a-button @click="handleDescribe" :icon="h(InfoCircleOutlined)">描述</a-button>
        <a-button
          danger
          type="primary"
          @click="handleDelete"
          :loading="deleting"
          :icon="h(DeleteOutlined)"
        >
          删除
        </a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="overview" tab="总览">
          <div class="overview-content">
            <a-card title="Service 信息" class="service-info">
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">创建时间</span>
                  <span class="info-value">{{ serviceData.metadata?.creationTimestamp || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">UID</span>
                  <span class="info-value">{{ serviceData.metadata?.uid || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">类型</span>
                  <span class="info-value">
                    <a-tag :color="getServiceTypeColor(serviceData.spec?.type)">
                      {{ serviceData.spec?.type || '-' }}
                    </a-tag>
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">集群 IP</span>
                  <span class="info-value">{{ serviceData.spec?.clusterIP || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">外部 IP</span>
                  <span class="info-value">{{ getExternalIP() }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">端口</span>
                  <span class="info-value port-link">{{ getPortsText() }}</span>
                </div>
              </div>
            </a-card>

            <a-card title="端口" class="ports-card">
              <a-table
                :data-source="serviceData.spec?.ports || []"
                :pagination="false"
                row-key="name"
                size="middle"
              >
                <a-table-column title="名称" key="name">
                  <template #default="{ record }">
                    {{ record.name || '-' }}
                  </template>
                </a-table-column>
                <a-table-column title="协议" key="protocol">
                  <template #default="{ record }">
                    {{ record.protocol || 'TCP' }}
                  </template>
                </a-table-column>
                <a-table-column title="端口" key="port">
                  <template #default="{ record }">
                    {{ record.port || '-' }}
                  </template>
                </a-table-column>
                <a-table-column title="目标端口" key="targetPort">
                  <template #default="{ record }">
                    {{ formatTargetPort(record.targetPort) }}
                  </template>
                </a-table-column>
                <a-table-column title="节点端口" key="nodePort">
                  <template #default="{ record }">
                    {{ record.nodePort || '-' }}
                  </template>
                </a-table-column>
              </a-table>
            </a-card>

            <a-card title="选择器" class="selector-card">
              <div class="tag-list" v-if="serviceData.spec?.selector">
                <a-tag v-for="(value, key) in serviceData.spec.selector" :key="key">
                  {{ key }}: {{ value }}
                </a-tag>
              </div>
              <a-empty v-else description="无选择器" />
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

        <a-tab-pane key="related" tab="相关资源">
          <div class="tab-placeholder">相关资源将在此处展示</div>
        </a-tab-pane>

        <a-tab-pane key="events" tab="事件">
          <div class="tab-placeholder">事件列表将在此处展示</div>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </div>
</template>

<style scoped>
.service-spec {
  padding: 0 24px;
}

.service-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.service-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.namespace-line {
  margin-top: 4px;
  color: #666;
  font-size: 14px;
}

.service-actions {
  display: flex;
  gap: 8px;
}

.overview-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.service-info,
.ports-card,
.selector-card {
  margin-bottom: 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px 48px;
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
  word-break: break-all;
}

.port-link {
  color: #1677ff;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
