<script setup lang="ts">
import { getNodes } from '@/api/clusterController.ts'
import { getNodeDisplayName, isSimKubeVirtualNode } from '@/utils/nodeDisplayName.ts'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const SIMKUBE_GROUP_KEY = '__simkube-virtual-group__'

type NodeTableRow = API.NodeVO & {
  isGroup?: boolean
  children?: NodeTableRow[]
}

const router = useRouter()
const nodes = ref<(API.NodeVO | undefined)[]>([])

onMounted(() => {
  loadNodes()
})

const loadNodes = () => {
  getNodes().then((res) => {
    nodes.value = res.data?.data?.items || []
  })
}

const tableData = computed<NodeTableRow[]>(() => {
  const list = nodes.value.filter((node): node is API.NodeVO => !!node)
  const physical: NodeTableRow[] = []
  const virtual: NodeTableRow[] = []

  for (const node of list) {
    if (isSimKubeVirtualNode(node.metadata?.name)) {
      virtual.push(node)
    } else {
      physical.push(node)
    }
  }

  virtual.sort((a, b) =>
    (a.metadata?.name || '').localeCompare(b.metadata?.name || '', undefined, { numeric: true }),
  )

  if (virtual.length === 0) {
    return physical
  }

  physical.push({
    isGroup: true,
    metadata: { name: SIMKUBE_GROUP_KEY },
    children: virtual,
  })
  return physical
})

const handleNodeClick = (node: NodeTableRow) => {
  if (node.isGroup || !node.metadata?.name) return
  router.push(`/nodes/${node.metadata.name}`)
}

const getNodeStatus = (node: NodeTableRow): string => {
  if (node.isGroup) {
    const children = node.children || []
    const ready = children.filter((child) => getNodeStatus(child) === 'Ready').length
    return `${ready} / ${children.length} Ready`
  }
  const condition = node.status?.conditions?.find((c) => c.type === 'Ready')
  return condition?.status === 'True' ? 'Ready' : 'NotReady'
}

const getNodeStatusColor = (node: NodeTableRow) => {
  if (node.isGroup) {
    const children = node.children || []
    if (children.length === 0) return 'default'
    const allReady = children.every((child) => getNodeStatus(child) === 'Ready')
    return allReady ? 'green' : 'gold'
  }
  const condition = node.status?.conditions?.find((c) => c.type === 'Ready')
  return condition?.status === 'True' ? 'green' : 'red'
}

const getNodeIP = (node: NodeTableRow) => {
  if (node.isGroup) return '-'
  const address = node.status?.addresses?.find((a) => a.type === 'InternalIP')
  return address?.address || ''
}

const getNodeVersion = (node: NodeTableRow) => {
  if (node.isGroup) return '-'
  return node.status?.nodeInfo?.kubeletVersion || ''
}

type NodeMetadata = API.MetadataVO & { labels?: Record<string, any> }

const getNodeRole = (node: NodeTableRow) => {
  if (node.isGroup) return 'SimKube'
  const labels = (node.metadata as NodeMetadata | undefined)?.labels
  if (labels && 'node-role.kubernetes.io/control-plane' in labels) {
    return 'control-plane'
  }
  return 'node'
}

const getRowKey = (record: NodeTableRow) => record.metadata?.name || ''
</script>

<template>
  <div class="nodes-manager">
    <h2>星载计算节点管理</h2>
    <a-table
      :data-source="tableData"
      :row-key="getRowKey"
      :pagination="{ pageSize: 20 }"
      :indent-size="24"
    >
      <a-table-column title="名称" data-index="metadata.name" key="name">
        <template #default="{ record }">
          <span v-if="record.isGroup" class="group-name">SimKube虚拟节点</span>
          <a v-else @click="handleNodeClick(record)">{{
            getNodeDisplayName(record.metadata?.name)
          }}</a>
        </template>
      </a-table-column>
      <a-table-column title="状态" key="status">
        <template #default="{ record }">
          <a-tag :color="getNodeStatusColor(record)">{{ getNodeStatus(record) }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="Roles" key="roles">
        <template #default="{ record }">
          <a-tag :color="record.isGroup ? 'purple' : 'blue'">{{ getNodeRole(record) }}</a-tag>
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
          {{ record.isGroup ? '-' : record.metadata?.creationTimestamp || '-' }}
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
  font-size: 26px;
  font-weight: 600;
}

.group-name {
  font-weight: 600;
}
</style>
