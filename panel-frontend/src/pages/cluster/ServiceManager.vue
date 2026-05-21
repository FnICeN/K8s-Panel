<script setup lang="ts">
import { getNamespaces, getServicesAll } from '@/api/clusterController.ts'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()
const services = ref<(API.ServiceVO | undefined)[]>([])
const namespaces = ref<string[]>([])
const selectedNamespace = ref<string>('')
const loading = ref(false)

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
        const namespaceFromQuery = route.query.namespace as string
        selectedNamespace.value =
          namespaceFromQuery && namespaces.value.includes(namespaceFromQuery)
            ? namespaceFromQuery
            : namespaces.value[0]!
        loadServices()
      }
    }
  } catch (error) {
    console.error('Failed to load namespaces:', error)
  }
}

const loadServices = async () => {
  if (!selectedNamespace.value) return
  try {
    loading.value = true
    const response = await getServicesAll({ namespace: selectedNamespace.value })
    services.value = response.data.data?.items || []
    router.replace({ path: '/services', query: { namespace: selectedNamespace.value } })
  } catch (error) {
    console.error('Failed to load services:', error)
  } finally {
    loading.value = false
  }
}

const handleServiceClick = (service: API.ServiceVO) => {
  if (service.metadata?.namespace && service.metadata?.name) {
    router.push(`/services/${service.metadata.namespace}/${service.metadata.name}`)
  }
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

const getClusterIP = (service: API.ServiceVO) => {
  return service.spec?.clusterIP || '-'
}

const getExternalIP = (service: API.ServiceVO) => {
  const loadBalancerIngress = service.status?.loadBalancer?.ingress || []
  const loadBalancerIPs = loadBalancerIngress
    .map((ingress) => ingress.ip || ingress.hostname)
    .filter((value): value is string => !!value)
  const externalIPs = service.spec?.externalIPs || []
  const allIPs = [...externalIPs, ...loadBalancerIPs]

  if (service.spec?.externalName) {
    allIPs.push(service.spec.externalName)
  }
  if (service.spec?.loadBalancerIP) {
    allIPs.push(service.spec.loadBalancerIP)
  }
  return allIPs.length > 0 ? allIPs.join(', ') : '-'
}

const formatTargetPort = (targetPort?: Record<string, any>) => {
  if (targetPort === undefined || targetPort === null) return ''
  return String(targetPort)
}

const getPorts = (service: API.ServiceVO) => {
  const ports = service.spec?.ports || []
  if (ports.length === 0) return '-'

  return ports
    .map((port) => {
      const protocol = port.protocol || 'TCP'
      const nodePort = port.nodePort ? `:${port.nodePort}` : ''
      const targetPort = formatTargetPort(port.targetPort)
      const target = targetPort && targetPort !== String(port.port) ? `>${targetPort}` : ''
      return `${port.port || '-'}${nodePort}${target}/${protocol}`
    })
    .join(', ')
}
</script>

<template>
  <div class="service-manager">
    <div class="page-header">
      <h2>Services</h2>
      <a-select
        v-model:value="selectedNamespace"
        class="namespace-select"
        placeholder="选择命名空间"
        @change="loadServices"
      >
        <a-select-option v-for="ns in namespaces" :key="ns" :value="ns">
          {{ ns }}
        </a-select-option>
      </a-select>
    </div>

    <a-table
      :data-source="services"
      row-key="metadata.name"
      :pagination="{ pageSize: 20 }"
      :loading="loading"
    >
      <a-table-column title="名称" data-index="metadata.name" key="name">
        <template #default="{ record }">
          <a @click="handleServiceClick(record)">{{ record.metadata?.name }}</a>
        </template>
      </a-table-column>
      <a-table-column title="类型" key="type">
        <template #default="{ record }">
          <a-tag :color="getServiceTypeColor(record.spec?.type)">
            {{ record.spec?.type || '-' }}
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="集群IP" key="clusterIP">
        <template #default="{ record }">
          {{ getClusterIP(record) }}
        </template>
      </a-table-column>
      <a-table-column title="外部IP" key="externalIP">
        <template #default="{ record }">
          {{ getExternalIP(record) }}
        </template>
      </a-table-column>
      <a-table-column title="端口" key="ports">
        <template #default="{ record }">
          {{ getPorts(record) }}
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
.service-manager {
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

.namespace-select {
  width: 200px;
}
</style>
