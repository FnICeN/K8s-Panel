<script setup lang="ts">
import { getNamespaces, getPodsAll } from '@/api/clusterController.ts'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
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
        selectedNamespace.value = namespaces.value[4]
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

const getPodStatusColor = (pod: API.PodVO) => {
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

const getReadyContainers = (pod: API.PodVO) => {
  const containerStatuses = pod.status?.containerStatuses
  if (!containerStatuses) return '0 / 0'
  const ready = containerStatuses.filter((c) => c.ready).length
  const total = containerStatuses.length
  return `${ready} / ${total}`
}

const getRestartCount = (pod: API.PodVO) => {
  const containerStatuses = pod.status?.containerStatuses
  if (!containerStatuses) return 0
  return containerStatuses.reduce((sum, c) => sum + (c.restartCount || 0), 0)
}
</script>

<template>
  <div class="pod-manager">
    <div class="page-header">
      <h2>Pods</h2>
      <a-select
        v-model:value="selectedNamespace"
        style="width: 200px"
        placeholder="选择命名空间"
        @change="loadPods"
      >
        <a-select-option v-for="ns in namespaces" :key="ns" :value="ns">
          {{ ns }}
        </a-select-option>
      </a-select>
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
            {{ record.status?.phase || '-' }}
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
          {{ record.status?.podIP || '-' }}
        </template>
      </a-table-column>
      <a-table-column title="节点" key="node">
        <template #default="{ record }">
          {{ record.spec?.nodeName || '-' }}
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
  font-size: 20px;
  font-weight: 600;
}
</style>
