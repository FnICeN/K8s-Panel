<script setup lang="ts">
import { getNamespaces } from '@/api/clusterController.ts'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const namespaces = ref<(API.NamespaceVO | undefined)[]>([])

onMounted(() => {
  loadNamespaces()
})

const loadNamespaces = async () => {
  try {
    const response = await getNamespaces()
    namespaces.value = response.data.data?.items || []
  } catch (error) {
    console.error('Failed to load namespaces:', error)
  }
}

const handleNamespaceClick = (namespace: API.NamespaceVO) => {
  if (namespace.metadata?.name) {
    router.push({
      path: '/pods',
      query: { namespace: namespace.metadata.name }
    })
  }
}

const getNamespaceStatusColor = (namespace: API.NamespaceVO) => {
  const phase = namespace.status?.phase
  switch (phase) {
    case 'Active':
      return 'green'
    case 'Terminating':
      return 'orange'
    default:
      return 'default'
  }
}
</script>

<template>
  <div class="namespace-manager">
    <h2>Namespaces</h2>
    <a-table :data-source="namespaces" row-key="metadata.name" :pagination="{ pageSize: 20 }">
      <a-table-column title="名称" data-index="metadata.name" key="name">
        <template #default="{ record }">
          <a @click="handleNamespaceClick(record)">{{ record.metadata?.name }}</a>
        </template>
      </a-table-column>
      <a-table-column title="状态" key="status">
        <template #default="{ record }">
          <a-tag :color="getNamespaceStatusColor(record)">
            {{ record.status?.phase || '-' }}
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
.namespace-manager {
  padding: 0 24px;
}

h2 {
  margin-bottom: 16px;
  font-size: 20px;
  font-weight: 600;
}
</style>
