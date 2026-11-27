<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNamespaces } from '@/api/clusterController'
import { getRbac, listRbac, type RbacKind, type RbacResource } from '@/api/rbac'

const route = useRoute()
const router = useRouter()
const kinds: RbacKind[] = ['roles', 'clusterroles', 'rolebindings']
const activeKind = ref<RbacKind>(kinds.includes(route.query.kind as RbacKind) ? route.query.kind as RbacKind : 'roles')
const namespace = ref(typeof route.query.namespace === 'string' ? route.query.namespace : '')
const namespaces = ref<string[]>([])
const records = ref<RbacResource[]>([])
const detail = ref<RbacResource | null>(null)
const detailOpen = ref(false)
const loading = ref(false)
const detailLoading = ref(false)
const error = ref('')
let listRequest = 0
let detailRequest = 0

const isClusterRole = computed(() => activeKind.value === 'clusterroles')

async function loadNamespaces() {
  try {
    const response = await getNamespaces()
    if (response.data.code === 0) {
      namespaces.value = (response.data.data?.items || [])
        .map((item) => item.metadata?.name)
        .filter((name): name is string => !!name)
    }
  } catch (cause) {
    console.error('Failed to load namespaces:', cause)
  }
}

async function loadRecords() {
  const requestId = ++listRequest
  loading.value = true
  error.value = ''
  records.value = []
  try {
    const response = await listRbac(activeKind.value, isClusterRole.value ? undefined : namespace.value)
    if (requestId !== listRequest) return
    if (response.data.code !== 0) throw new Error(response.data.msg || '查询失败')
    records.value = response.data.data?.items || []
  } catch (cause) {
    if (requestId === listRequest) {
      error.value = cause instanceof Error ? cause.message : '查询失败，请重试'
    }
  } finally {
    if (requestId === listRequest) loading.value = false
  }
}

async function openDetail(record: RbacResource) {
  const name = record.metadata?.name
  const recordNamespace = record.metadata?.namespace || namespace.value
  if (!name || (!isClusterRole.value && !recordNamespace)) return
  const requestId = ++detailRequest
  detail.value = null
  detailOpen.value = true
  detailLoading.value = true
  try {
    const response = await getRbac(activeKind.value, name, recordNamespace)
    if (requestId !== detailRequest) return
    if (response.data.code !== 0) throw new Error(response.data.msg || '查询详情失败')
    detail.value = response.data.data || null
  } catch (cause) {
    if (requestId === detailRequest) error.value = cause instanceof Error ? cause.message : '查询详情失败'
  } finally {
    if (requestId === detailRequest) detailLoading.value = false
  }
}

function formatList(values?: string[]) {
  return values?.length ? values.join(', ') : '-'
}

watch([activeKind, namespace], () => {
  detailOpen.value = false
  router.replace({ path: '/roles', query: {
    kind: activeKind.value,
    ...(namespace.value ? { namespace: namespace.value } : {}),
  } })
  loadRecords()
})

onMounted(() => {
  loadNamespaces()
  loadRecords()
})
</script>

<template>
  <div class="role-manager">
    <div class="page-header">
      <div>
        <h2>角色与角色绑定（Role）</h2>
        <p>查看集群中的角色权限与主体绑定关系</p>
      </div>
      <a-button :loading="loading" @click="loadRecords">刷新</a-button>
    </div>

    <div class="toolbar">
      <a-radio-group v-model:value="activeKind" button-style="solid">
        <a-radio-button value="roles">Role</a-radio-button>
        <a-radio-button value="clusterroles">ClusterRole</a-radio-button>
        <a-radio-button value="rolebindings">RoleBinding</a-radio-button>
      </a-radio-group>
      <a-select v-if="!isClusterRole" v-model:value="namespace" class="namespace-select" show-search
                placeholder="全部命名空间" :filter-option="(input: string, option: any) => String(option?.value || '').toLowerCase().includes(input.toLowerCase())">
        <a-select-option value="">全部命名空间</a-select-option>
        <a-select-option v-for="item in namespaces" :key="item" :value="item">{{ item }}</a-select-option>
      </a-select>
    </div>

    <a-alert v-if="error" type="error" :message="error" show-icon class="error-alert" />
    <a-table :data-source="records" :loading="loading" :row-key="(record: RbacResource) => `${record.metadata?.namespace || ''}/${record.metadata?.name}`"
             :pagination="{ pageSize: 20 }" :scroll="{ x: 850 }">
      <a-table-column title="名称" key="name">
        <template #default="{ record }"><a @click="openDetail(record)">{{ record.metadata?.name || '-' }}</a></template>
      </a-table-column>
      <a-table-column v-if="!isClusterRole" title="命名空间" key="namespace">
        <template #default="{ record }">{{ record.metadata?.namespace || '-' }}</template>
      </a-table-column>
      <a-table-column v-if="activeKind !== 'rolebindings'" title="规则数" key="rules">
        <template #default="{ record }">{{ record.rules?.length || 0 }}</template>
      </a-table-column>
      <a-table-column v-if="activeKind === 'rolebindings'" title="引用角色" key="roleRef">
        <template #default="{ record }">{{ record.roleRef?.kind || '-' }} / {{ record.roleRef?.name || '-' }}</template>
      </a-table-column>
      <a-table-column v-if="activeKind === 'rolebindings'" title="主体数" key="subjects">
        <template #default="{ record }">{{ record.subjects?.length || 0 }}</template>
      </a-table-column>
      <a-table-column title="创建时间" key="created">
        <template #default="{ record }">{{ record.metadata?.creationTimestamp || '-' }}</template>
      </a-table-column>
    </a-table>

    <a-drawer v-model:open="detailOpen" :title="`${detail?.kind || '资源'}：${detail?.metadata?.name || ''}`" width="680" @close="detailRequest++">
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions bordered :column="1" size="small">
            <a-descriptions-item label="名称">{{ detail.metadata?.name || '-' }}</a-descriptions-item>
            <a-descriptions-item label="命名空间" v-if="detail.kind !== 'ClusterRole'">{{ detail.metadata?.namespace || '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detail.metadata?.creationTimestamp || '-' }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.roleRef" label="引用角色">{{ detail.roleRef.kind }} / {{ detail.roleRef.name }}</a-descriptions-item>
          </a-descriptions>
          <template v-if="detail.kind === 'RoleBinding'">
            <h3>绑定主体</h3>
            <a-table :data-source="detail.subjects || []" :pagination="false" size="small" :row-key="(item: any, index: number) => `${item.kind}/${item.name}/${index}`">
              <a-table-column title="类型" data-index="kind" />
              <a-table-column title="名称" data-index="name" />
              <a-table-column title="命名空间" key="namespace"><template #default="{ record }">{{ record.namespace || '-' }}</template></a-table-column>
            </a-table>
          </template>
          <template v-else>
            <h3>权限规则</h3>
            <a-empty v-if="!detail.rules?.length" description="暂无规则" />
            <a-card v-for="(rule, index) in detail.rules || []" :key="index" size="small" class="rule-card" :title="`规则 ${index + 1}`">
              <a-descriptions :column="1" size="small">
                <a-descriptions-item label="API 组">{{ formatList(rule.apiGroups) }}</a-descriptions-item>
                <a-descriptions-item label="资源">{{ formatList(rule.resources) }}</a-descriptions-item>
                <a-descriptions-item label="资源名称">{{ formatList(rule.resourceNames) }}</a-descriptions-item>
                <a-descriptions-item label="非资源 URL">{{ formatList(rule.nonResourceURLs) }}</a-descriptions-item>
                <a-descriptions-item label="操作">{{ formatList(rule.verbs) }}</a-descriptions-item>
              </a-descriptions>
            </a-card>
          </template>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>

<style scoped>
.role-manager { padding: 0 24px; }
.page-header, .toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 26px; font-weight: 600; }
.page-header p { margin: 4px 0 0; color: var(--app-text-secondary); }
.toolbar { margin-bottom: 16px; flex-wrap: wrap; }
.namespace-select { width: 230px; }
.error-alert { margin-bottom: 16px; }
h3 { margin: 24px 0 12px; }
.rule-card { margin-bottom: 12px; }
@media (max-width: 700px) { .role-manager { padding: 0; } }
</style>
