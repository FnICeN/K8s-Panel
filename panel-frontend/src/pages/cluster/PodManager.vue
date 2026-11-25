<script setup lang="ts">
import { getNamespaces, getPodsAll } from '@/api/clusterController.ts'
import { getNodeDisplayName } from '@/utils/nodeDisplayName.ts'
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const pods = ref<(API.PodVO | undefined)[]>([])
const namespaces = ref<string[]>([])
const selectedNamespace = ref<string>('')

onMounted(() => {
  loadNamespaces()
})

const loadNamespaces = async () => {
  try {
    const response = await getNamespaces()
    if (response.data.data?.items) {
      namespaces.value = response.data.data.items
        .map((ns) => ns.metadata?.name)
        .filter((name): name is string => !!name)
      if (namespaces.value.length > 0) {
        // 优先使用路由参数中的 namespace
        const namespaceFromQuery = route.query.namespace as string
        if (namespaceFromQuery && namespaces.value.includes(namespaceFromQuery)) {
          selectedNamespace.value = namespaceFromQuery
        } else {
          selectedNamespace.value = namespaces.value[0]!
        }
        loadPods()
      }
    }
  } catch (error) {
    console.error('Failed to load namespaces:', error)
  }
}

const loadPods = async () => {
  if (!selectedNamespace.value) return
  try {
    const response = await getPodsAll({ namespace: selectedNamespace.value })
    pods.value = response.data.data?.items || []
  } catch (error) {
    console.error('Failed to load pods:', error)
  }
}

const handlePodClick = (pod: API.PodVO) => {
  if (pod.metadata?.namespace && pod.metadata?.name) {
    router.push(`/pods/${pod.metadata.namespace}/${pod.metadata.name}`)
  }
}

type PodListStatus = API.StatusVO & Pick<API.PodStatus, 'phase' | 'containerStatuses' | 'podIP'>

const getPodStatus = (pod: API.PodVO) => {
  return pod.status as PodListStatus | undefined
}

const getPodPhase = (pod: API.PodVO) => {
  return getPodStatus(pod)?.phase
}

const getPodIP = (pod: API.PodVO) => {
  return getPodStatus(pod)?.podIP
}

const getPodStatusColor = (pod: API.PodVO) => {
  const phase = getPodPhase(pod)
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

const getReadyContainers = (pod: API.PodVO) => {
  const containerStatuses = getPodStatus(pod)?.containerStatuses
  if (!containerStatuses) return '0 / 0'
  const ready = containerStatuses.filter((c) => c.ready).length
  const total = containerStatuses.length
  return `${ready} / ${total}`
}

const getRestartCount = (pod: API.PodVO) => {
  const containerStatuses = getPodStatus(pod)?.containerStatuses
  if (!containerStatuses) return 0
  return containerStatuses.reduce((sum, c) => sum + (c.restartCount || 0), 0)
}

const filterNamespaceOption = (input: string, option: any) => {
  const value = option?.value ?? option?.label ?? ''
  return String(value).toLowerCase().includes(input.toLowerCase())
}
</script>

<template>
  <div class="pod-manager">
    <div class="page-header">
      <h2>计算单元（Pod）</h2>
      <div class="namespace-filter">
        <span class="namespace-label">命名空间</span>
        <a-select
          v-model:value="selectedNamespace"
          class="namespace-select"
          placeholder="请选择命名空间"
          show-search
          :filter-option="filterNamespaceOption"
          @change="loadPods"
        >
          <a-select-option v-for="ns in namespaces" :key="ns" :value="ns">
            {{ ns }}
          </a-select-option>
        </a-select>
      </div>
    </div>
    <a-table :data-source="pods" row-key="metadata.name" :pagination="{ pageSize: 20 }">
      <a-table-column title="名称" data-index="metadata.name" key="name">
        <template #default="{ record }">
          <a @click="handlePodClick(record)">{{ record.metadata?.name }}</a>
        </template>
      </a-table-column>
      <a-table-column title="就绪" key="ready">
        <template #default="{ record }">
          {{ getReadyContainers(record) }}
        </template>
      </a-table-column>
      <a-table-column title="状态" key="status">
        <template #default="{ record }">
          <a-tag :color="getPodStatusColor(record)">
            {{ getPodPhase(record) || '-' }}
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="重启次数" key="restarts">
        <template #default="{ record }">
          {{ getRestartCount(record) }}
        </template>
      </a-table-column>
      <a-table-column title="IP" key="ip">
        <template #default="{ record }">
          {{ getPodIP(record) || '-' }}
        </template>
      </a-table-column>
      <a-table-column title="节点" key="node">
        <template #default="{ record }">
          {{ getNodeDisplayName(record.spec?.nodeName) }}
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
.pod-manager {
  padding: 0 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
}

.namespace-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.namespace-label {
  color: #666;
  font-size: 17px;
  white-space: nowrap;
}

.namespace-select {
  width: 220px;
}
</style>
