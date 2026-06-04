<script setup lang="ts">
import { getNodes } from '@/api/clusterController.ts'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
type NodeItem = API.NodeVO & {
  metadata?: API.MetadataVO & {
    labels?: Record<string, string>
  }
}

const nodes = ref<NodeItem[]>([])

onMounted(() => {
  loadNodes()
})

const loadNodes = () => {
  getNodes().then((res) => {
    console.log(res.data?.data?.items)
    nodes.value = (res.data?.data?.items || []) as NodeItem[]
  })
}

const handleNodeClick = (node: NodeItem) => {
  if (node.metadata?.name) {
    router.push(`/nodes/${node.metadata.name}`)
  }
}

const getNodeStatus = (node: NodeItem) => {
  const condition = node.status?.conditions?.find((c) => c.type === 'Ready')
  return condition?.status === 'True' ? 'Ready' : 'NotReady'
}

const getNodeStatusColor = (node: NodeItem) => {
  const condition = node.status?.conditions?.find((c) => c.type === 'Ready')
  return condition?.status === 'True' ? 'green' : 'red'
}

const getNodeIP = (node: NodeItem) => {
  const address = node.status?.addresses?.find((a) => a.type === 'InternalIP')
  return address?.address || ''
}

const getNodeVersion = (node: NodeItem) => {
  return node.status?.nodeInfo?.kubeletVersion || ''
}

const getNodeRole = (node: NodeItem) => {
  const labels = node.metadata?.labels
  if (labels && 'node-role.kubernetes.io/control-plane' in labels) {
    return 'control-plane'
  } else {
    return 'node'
  }
}
</script>

<template>
  <div class="nodes-manager">
    <h2>Nodes</h2>
    <a-table :data-source="nodes" row-key="metadata.name" :pagination="{ pageSize: 20 }">
      <a-table-column title="名称" data-index="metadata.name" key="name">
        <template #default="{ record }">
          <a @click="handleNodeClick(record)">{{ record.metadata?.name }}</a>
        </template>
      </a-table-column>
      <a-table-column title="状态" key="status">
        <template #default="{ record }">
          <a-tag :color="getNodeStatusColor(record)">{{ getNodeStatus(record) }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="Roles" key="roles">
        <template #default="{ record }">
          <a-tag color="blue">{{ getNodeRole(record) }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="IP Address" key="ip">
        <template #default="{ record }">
          {{ getNodeIP(record) }}
        </template>
      </a-table-column>
      <a-table-column title="Version" key="version">
        <template #default="{ record }">
          {{ getNodeVersion(record) }}
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
.nodes-manager {
  padding: 0 24px;
}

h2 {
  margin-bottom: 16px;
  font-size: 20px;
  font-weight: 600;
}
</style>
