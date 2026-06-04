<script setup lang="ts">
import {
  getDeploymentsAll,
  getDeploymentsByNamespace,
  getNamespaces,
} from '@/api/clusterController.ts'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { computed, h, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type DeploymentMetadata = {
  name?: string
  namespace?: string
  creationTimestamp?: string
}

type DeploymentCondition = {
  type?: string
  status?: string
  reason?: string
  message?: string
}

type DeploymentListItem = API.DeploymentVO & {
  metadata?: DeploymentMetadata
  spec?: {
    replicas?: number
  }
  status?: {
    replicas?: number
    readyReplicas?: number
    availableReplicas?: number
    updatedReplicas?: number
    conditions?: DeploymentCondition[]
  }
}

const ALL_NAMESPACES = '__all__'

const router = useRouter()
const route = useRoute()
const deployments = ref<DeploymentListItem[]>([])
const namespaces = ref<string[]>([])
const selectedNamespace = ref<string>(ALL_NAMESPACES)
const searchText = ref('')
const loading = ref(false)

onMounted(() => {
  loadNamespaces()
})

const filteredDeployments = computed(() => {
  const keyword = searchText.value.trim().toLowerCase()
  if (!keyword) {
    return deployments.value
  }
  return deployments.value.filter((deployment) => {
    const name = deployment.metadata?.name?.toLowerCase() || ''
    const namespace = deployment.metadata?.namespace?.toLowerCase() || ''
    return name.includes(keyword) || namespace.includes(keyword)
  })
})

const loadNamespaces = async () => {
  try {
    const response = await getNamespaces()
    namespaces.value = (response.data.data?.items || [])
      .map((namespace) => namespace.metadata?.name)
      .filter((name): name is string => !!name)

    const namespaceFromQuery = route.query.namespace as string | undefined
    if (namespaceFromQuery && namespaces.value.includes(namespaceFromQuery)) {
      selectedNamespace.value = namespaceFromQuery
    } else {
      selectedNamespace.value = ALL_NAMESPACES
    }
    await loadDeployments()
  } catch (error) {
    console.error('Failed to load namespaces:', error)
  }
}

const loadDeployments = async () => {
  try {
    loading.value = true
    if (selectedNamespace.value === ALL_NAMESPACES) {
      const response = await getDeploymentsAll()
      deployments.value = (response.data.data?.items || []) as DeploymentListItem[]
      router.replace({ path: '/deployments' })
    } else {
      const response = await getDeploymentsByNamespace({
        namespace: selectedNamespace.value,
      } as API.getDeploymentsByNamespaceParams)
      deployments.value = (response.data.data?.items || []) as DeploymentListItem[]
      router.replace({ path: '/deployments', query: { namespace: selectedNamespace.value } })
    }
  } catch (error) {
    console.error('Failed to load deployments:', error)
  } finally {
    loading.value = false
  }
}

const handleDeploymentClick = (deployment: DeploymentListItem) => {
  const namespace = deployment.metadata?.namespace
  const name = deployment.metadata?.name
  if (namespace && name) {
    router.push(`/deployments/${namespace}/${name}`)
  }
}

const getReadyText = (deployment: DeploymentListItem) => {
  const ready = deployment.status?.readyReplicas || 0
  const total = deployment.spec?.replicas ?? deployment.status?.replicas ?? 0
  return `${ready} / ${total}`
}

const getDeploymentCondition = (deployment: DeploymentListItem, type: string) => {
  return deployment.status?.conditions?.find((condition) => condition.type === type)
}

const getDeploymentStatus = (deployment: DeploymentListItem) => {
  const available = getDeploymentCondition(deployment, 'Available')
  if (available?.status === 'True') {
    return '可用'
  }
  const progressing = getDeploymentCondition(deployment, 'Progressing')
  if (progressing?.status === 'True') {
    return '更新中'
  }
  return deployment.status?.availableReplicas ? '可用' : '不可用'
}

const getDeploymentStatusColor = (deployment: DeploymentListItem) => {
  const status = getDeploymentStatus(deployment)
  if (status === '可用') return 'green'
  if (status === '更新中') return 'blue'
  return 'red'
}
</script>

<template>
  <div class="deployment-manager">
    <div class="page-header">
      <div>
        <h2>Deployment管理</h2>
        <div class="namespace-line">
          命名空间：
          <a-select
            v-model:value="selectedNamespace"
            class="namespace-select"
            @change="loadDeployments"
          >
            <a-select-option :value="ALL_NAMESPACES">全部命名空间</a-select-option>
            <a-select-option v-for="namespace in namespaces" :key="namespace" :value="namespace">
              {{ namespace }}
            </a-select-option>
          </a-select>
        </div>
      </div>
      <div class="page-actions">
        <a-button :loading="loading" :icon="h(ReloadOutlined)" @click="loadDeployments">
          刷新
        </a-button>
        <a-input-search
          v-model:value="searchText"
          class="search-input"
          placeholder="搜索 Deployment"
          allow-clear
        />
      </div>
    </div>

    <a-table
      :data-source="filteredDeployments"
      :loading="loading"
      row-key="metadata.name"
      :pagination="{ pageSize: 20 }"
    >
      <a-table-column title="名称" key="name">
        <template #default="{ record }">
          <a @click="handleDeploymentClick(record)">{{ record.metadata?.name }}</a>
        </template>
      </a-table-column>
      <a-table-column title="命名空间" key="namespace">
        <template #default="{ record }">
          <a-tag>{{ record.metadata?.namespace || '-' }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="就绪" key="ready">
        <template #default="{ record }">
          {{ getReadyText(record) }}
        </template>
      </a-table-column>
      <a-table-column title="状态" key="status">
        <template #default="{ record }">
          <a-tag :color="getDeploymentStatusColor(record)">
            {{ getDeploymentStatus(record) }}
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="创建时间" key="created">
        <template #default="{ record }">
          {{ record.metadata?.creationTimestamp || '-' }}
        </template>
      </a-table-column>
    </a-table>
  </div>
</template>

<style scoped>
.deployment-manager {
  padding: 0 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}

.namespace-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  color: #666;
}

.namespace-select {
  width: 220px;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 260px;
}
</style>
