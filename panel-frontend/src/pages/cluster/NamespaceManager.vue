<script setup lang="ts">
import { getNamespaces } from '@/api/clusterController.ts'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { computed, h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
type NamespaceItem = API.NamespaceVO & {
  status?: {
    phase?: string
  }
}

const namespaces = ref<NamespaceItem[]>([])
const searchText = ref('')
const loading = ref(false)

onMounted(() => {
  loadNamespaces()
})

const filteredNamespaces = computed(() => {
  const keyword = searchText.value.trim().toLowerCase()
  if (!keyword) {
    return namespaces.value
  }
  return namespaces.value.filter((namespace) => {
    const name = namespace.metadata?.name?.toLowerCase() || ''
    const phase = getNamespacePhase(namespace)?.toLowerCase() || ''
    return name.includes(keyword) || phase.includes(keyword)
  })
})

const loadNamespaces = async () => {
  try {
    loading.value = true
    const response = await getNamespaces()
    namespaces.value = response.data.data?.items || []
  } catch (error) {
    console.error('Failed to load namespaces:', error)
  } finally {
    loading.value = false
  }
}

const handleNamespaceClick = (namespace: API.NamespaceVO) => {
  if (namespace.metadata?.name) {
    router.push({
      path: '/pods',
      query: { namespace: namespace.metadata.name },
    })
  }
}

type NamespaceStatus = API.StatusVO & { phase?: string }

const getNamespacePhase = (namespace: API.NamespaceVO) => {
  return (namespace.status as NamespaceStatus | undefined)?.phase
}

const getNamespaceStatusColor = (namespace: API.NamespaceVO) => {
  const phase = getNamespacePhase(namespace)
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
    <div class="page-header">
      <h2>命名空间管理（Namespace）</h2>
      <div class="page-actions">
        <a-button :loading="loading" :icon="h(ReloadOutlined)" @click="loadNamespaces">
          刷新
        </a-button>
        <a-input-search
          v-model:value="searchText"
          class="search-input"
          placeholder="搜索 Namespace"
          allow-clear
        />
      </div>
    </div>
    <a-table
      :data-source="filteredNamespaces"
      :loading="loading"
      row-key="metadata.name"
      :pagination="{ pageSize: 20 }"
    >
      <a-table-column title="名称" data-index="metadata.name" key="name">
        <template #default="{ record }">
          <a @click="handleNamespaceClick(record)">{{ record.metadata?.name }}</a>
        </template>
      </a-table-column>
      <a-table-column title="状态" key="status">
        <template #default="{ record }">
          <a-tag :color="getNamespaceStatusColor(record)">
            {{ getNamespacePhase(record) || '-' }}
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
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
