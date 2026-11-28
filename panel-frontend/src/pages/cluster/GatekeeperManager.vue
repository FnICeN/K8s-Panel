<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  deleteGatekeeperConstraint, listGatekeeperPolicies, previewGatekeeperAnnotation,
  type GatekeeperConstraint, type GatekeeperObject,
} from '@/api/gatekeeper'

const loading = ref(false)
const error = ref('')
const templates = ref<GatekeeperObject[]>([])
const constraints = ref<GatekeeperConstraint[]>([])
const mutations = ref<GatekeeperConstraint[]>([])
const activeTab = ref('constraints')
const search = ref('')
const selected = ref<GatekeeperConstraint | null>(null)
const previewOpen = ref(false)
const previewing = ref(false)
const annotationKey = ref('loa-test')
const annotationValue = ref('true')
const previewYaml = ref('')
const deletingKey = ref('')

const visibleTemplates = computed(() => templates.value.filter(item =>
  `${item.metadata?.name || ''} ${item.spec?.crd?.spec?.names?.kind || ''}`
    .toLowerCase().includes(search.value.toLowerCase()),
))
const visibleConstraints = computed(() => constraints.value.filter(item =>
  `${item.object.metadata?.name || ''} ${item.object.kind || ''}`
    .toLowerCase().includes(search.value.toLowerCase()),
))
const visibleMutations = computed(() => mutations.value.filter(item =>
  `${item.object.metadata?.name || ''} ${item.object.kind || ''}`
    .toLowerCase().includes(search.value.toLowerCase()),
))

async function loadPolicies() {
  loading.value = true
  error.value = ''
  try {
    const response = await listGatekeeperPolicies()
    if (response.data.code !== 0) throw new Error(response.data.msg || '查询失败')
    templates.value = response.data.data?.templates || []
    constraints.value = response.data.data?.constraints || []
    mutations.value = response.data.data?.mutations || []
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '查询 Gatekeeper 策略失败'
    templates.value = []
    constraints.value = []
    mutations.value = []
  } finally {
    loading.value = false
  }
}

function openPreview(record: GatekeeperConstraint) {
  selected.value = record
  annotationKey.value = 'loa-test'
  annotationValue.value = 'true'
  previewYaml.value = ''
  previewOpen.value = true
}

async function runPreview() {
  const record = selected.value
  const name = record?.object.metadata?.name
  if (!record || !name || !annotationKey.value.trim()) {
    message.warning('请输入注解键')
    return
  }
  previewing.value = true
  previewYaml.value = ''
  try {
    const response = await previewGatekeeperAnnotation(
      record.resource, name, annotationKey.value.trim(), annotationValue.value,
    )
    if (response.data.code !== 0) throw new Error(response.data.msg || '试运行失败')
    previewYaml.value = response.data.data?.yaml || ''
    message.success('服务端试运行通过')
  } catch (cause) {
    message.error(cause instanceof Error ? cause.message : '试运行失败')
  } finally {
    previewing.value = false
  }
}

function confirmDelete(record: GatekeeperConstraint) {
  const name = record.object.metadata?.name
  if (!name) return
  const resource = record.resource
  const key = `${resource}/${name}`
  Modal.confirm({
    title: '下线 Gatekeeper 约束策略',
    content: `确认删除 ${key} 吗？这将停止该约束实例的策略执行，不会删除策略模板或同类型的其他约束。`,
    okText: '确认下线',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      deletingKey.value = key
      try {
        const response = await deleteGatekeeperConstraint(resource, name)
        if (response.data.code !== 0) throw new Error(response.data.msg || '下线失败')
        constraints.value = constraints.value.filter(item =>
          !(item.resource === resource && item.object.metadata?.name === name),
        )
        message.success(`${key} 已下线`)
        await loadPolicies()
      } catch (cause) {
        message.error(cause instanceof Error ? cause.message : '下线失败')
        throw cause
      } finally {
        deletingKey.value = ''
      }
    },
  })
}

onMounted(loadPolicies)
</script>

<template>
  <div class="gatekeeper-manager">
    <div class="page-header">
      <div>
        <h2>Gatekeeper 策略管理</h2>
        <p>查看可访问的验证策略模板、约束实例和变更策略</p>
      </div>
      <a-button :loading="loading" @click="loadPolicies">刷新</a-button>
    </div>
    <a-alert v-if="error" type="error" show-icon :message="error" class="error-alert" />
    <a-tabs v-model:active-key="activeTab">
      <a-tab-pane key="constraints" :tab="`约束实例 (${constraints.length})`">
        <a-input-search v-model:value="search" placeholder="搜索约束名称或类型" class="search" allow-clear />
        <a-table :data-source="visibleConstraints" :loading="loading" :pagination="{ pageSize: 20 }"
                 :row-key="(record: GatekeeperConstraint) => `${record.resource}/${record.object.metadata?.name}`"
                 :scroll="{ x: 750 }">
          <a-table-column title="名称" key="name"><template #default="{ record }">{{ record.object.metadata?.name || '-' }}</template></a-table-column>
          <a-table-column title="约束类型" key="kind"><template #default="{ record }">{{ record.object.kind || record.resource }}</template></a-table-column>
          <a-table-column title="执行方式" key="action"><template #default="{ record }">{{ record.object.spec?.enforcementAction || 'deny（默认）' }}</template></a-table-column>
          <a-table-column title="违规数" key="violations"><template #default="{ record }">{{ record.object.status?.totalViolations ?? '-' }}</template></a-table-column>
          <a-table-column title="操作" key="actions">
            <template #default="{ record }">
              <a-space>
                <a @click="openPreview(record)">编辑注解</a>
                <a :aria-disabled="!!deletingKey" class="delete-action" @click="!deletingKey && confirmDelete(record)">下线</a>
              </a-space>
            </template>
          </a-table-column>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="templates" :tab="`策略模板 (${templates.length})`">
        <a-input-search v-model:value="search" placeholder="搜索模板名称或约束类型" class="search" allow-clear />
        <a-table :data-source="visibleTemplates" :loading="loading" :pagination="{ pageSize: 20 }"
                 :row-key="(record: GatekeeperObject) => record.metadata?.name || ''" :scroll="{ x: 650 }">
          <a-table-column title="模板名称" key="name"><template #default="{ record }">{{ record.metadata?.name || '-' }}</template></a-table-column>
          <a-table-column title="生成的约束类型" key="kind"><template #default="{ record }">{{ record.spec?.crd?.spec?.names?.kind || '-' }}</template></a-table-column>
          <a-table-column title="创建时间" key="created"><template #default="{ record }">{{ record.metadata?.creationTimestamp || '-' }}</template></a-table-column>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="mutations" :tab="`变更策略 (${mutations.length})`">
        <a-input-search v-model:value="search" placeholder="搜索变更策略名称或类型" class="search" allow-clear />
        <a-table :data-source="visibleMutations" :loading="loading" :pagination="{ pageSize: 20 }"
                 :row-key="(record: GatekeeperConstraint) => `${record.resource}/${record.object.metadata?.name}`"
                 :scroll="{ x: 650 }">
          <a-table-column title="名称" key="name"><template #default="{ record }">{{ record.object.metadata?.name || '-' }}</template></a-table-column>
          <a-table-column title="类型" key="kind"><template #default="{ record }">{{ record.object.kind || record.resource }}</template></a-table-column>
          <a-table-column title="创建时间" key="created"><template #default="{ record }">{{ record.object.metadata?.creationTimestamp || '-' }}</template></a-table-column>
        </a-table>
      </a-tab-pane>
    </a-tabs>
    <a-modal v-model:open="previewOpen" :title="`添加约束注解：${selected?.object.metadata?.name || ''}`"
             width="760px" :footer="null">
      <p>请求将经过 Kubernetes 服务端校验</p>
      <div class="annotation-fields">
        <a-input v-model:value="annotationKey" placeholder="注解键" aria-label="注解键" />
        <a-input v-model:value="annotationValue" placeholder="注解值" aria-label="注解值" />
        <a-button type="primary" :loading="previewing" @click="runPreview">试运行</a-button>
      </div>
      <a-alert v-if="previewYaml" type="success" show-icon message="试运行通过" class="result-alert" />
      <pre v-if="previewYaml" class="yaml-result">{{ previewYaml }}</pre>
    </a-modal>
  </div>
</template>

<style scoped>
.gatekeeper-manager { padding: 0 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 26px; font-weight: 600; }
.page-header p { margin: 4px 0 0; color: var(--app-text-secondary); }
.error-alert { margin-bottom: 16px; }
.search { width: 280px; max-width: 100%; margin-bottom: 16px; }
.annotation-fields { display: flex; gap: 8px; }
.annotation-fields :deep(.ant-input) { min-width: 0; }
.result-alert { margin: 16px 0; }
.yaml-result { max-height: 420px; overflow: auto; padding: 12px; background: #f5f5f5; white-space: pre; }
.delete-action { color: #cf1322; }
@media (max-width: 700px) { .gatekeeper-manager { padding: 0; } .annotation-fields { flex-wrap: wrap; } }
</style>
