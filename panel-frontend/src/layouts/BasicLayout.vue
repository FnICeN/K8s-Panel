<template>
  <a-layout class="basic-layout">
    <a-layout-header class="header">
      <div class="header-left">
        <h1 class="platform-name">天基云网安全集控平台</h1>
        <a-breadcrumb class="breadcrumb" separator=">">
          <a-breadcrumb-item v-for="(item, index) in breadcrumbItems" :key="index">
            <router-link v-if="item.path" :to="item.path">{{ item.title }}</router-link>
            <a v-else>{{ item.title }}</a>
          </a-breadcrumb-item>
        </a-breadcrumb>
      </div>
      <div class="header-right">
        <a-tooltip title="创建资源">
          <a-button
            class="create-button"
            shape="circle"
            :icon="h(PlusOutlined)"
            @click="createResourceDialogRef?.open()"
          />
        </a-tooltip>
        <a-popover placement="bottomRight" trigger="hover" overlay-class-name="alert-popover">
          <template #content>
            <div class="alert-dropdown">
              <div
                v-for="alert in mockAuditAlerts"
                :key="alert.id"
                class="alert-dropdown-item"
                :class="getAlertPriorityClass(alert.priority)"
              >
                <div class="alert-dropdown-title">
                  <span>{{ alert.rule }}</span>
                  <a-tag :color="getAlertPriorityColor(alert.priority)">
                    {{ alert.priority }}
                  </a-tag>
                </div>
                <div class="alert-dropdown-meta">
                  {{ getAlertNamespace(alert) }} · {{ getAlertResource(alert) }}
                </div>
                <div class="alert-dropdown-output">{{ alert.output }}</div>
              </div>
            </div>
          </template>
          <a-badge :count="mockAuditAlerts.length" class="alert-badge">
            <a-tooltip title="审计告警">
              <a-button
                class="alert-button"
                shape="circle"
                :icon="h(BellOutlined)"
                @click="goToAuditAlerts"
              />
            </a-tooltip>
          </a-badge>
        </a-popover>
        <a-button type="primary" @click="loginDialogRef?.open()">登录</a-button>
      </div>
    </a-layout-header>
    <a-layout>
      <a-layout-sider width="200" style="background: #fff">
        <div class="logo" />
        <a-menu
          id="main-menu"
          v-model:openKeys="openKeys"
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          :items="items"
          style="height: 100%; border-right: 0"
          @click="handleClick"
        ></a-menu>
      </a-layout-sider>
      <a-layout>
        <a-layout-content
          :style="{ margin: '24px 16px', padding: '24px', background: '#fff', minHeight: '280px' }"
        >
          <RouterView />
        </a-layout-content>
      </a-layout>
    </a-layout>
    <LoginDialog ref="loginDialogRef" />
    <CreateResourceDialog ref="createResourceDialogRef" />
  </a-layout>
</template>

<script setup lang="ts">
import { reactive, ref, watch, h, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { HomeOutlined, CheckCircleOutlined, PlusOutlined, BellOutlined } from '@ant-design/icons-vue'
import type { MenuProps, ItemType } from 'ant-design-vue'
import LoginDialog from '@/components/LoginDialog.vue'
import CreateResourceDialog from '@/components/CreateResourceDialog.vue'
import { mockAuditAlerts, type AuditAlert, type AuditPriority } from '@/mocks/auditAlerts'

const router = useRouter()
const route = useRoute()
const loginDialogRef = ref<InstanceType<typeof LoginDialog> | null>(null)
const createResourceDialogRef = ref<InstanceType<typeof CreateResourceDialog> | null>(null)

// 全局登录对话框触发函数
if (typeof window !== 'undefined') {
  ;(window as any).showLoginDialog = () => {
    loginDialogRef.value?.show()
  }
}

const selectedKeys = ref<string[]>(['/'])
const openKeys = ref<string[]>([])
const breadcrumbItems = ref<Array<{ title: string; path?: string }>>([])

function getItem(
  label: string,
  key: string,
  icon?: any,
  children?: ItemType[],
  type?: 'group',
): ItemType {
  return {
    key,
    icon,
    children,
    label,
    type,
  } as ItemType
}

const items: ItemType[] = reactive([
  getItem('首页', '/', () => h(HomeOutlined)),
  getItem('审计告警', '/audit-alerts', () => h(BellOutlined)),
  getItem('集群管理', '/health', () => h(CheckCircleOutlined), [
    getItem('节点管理', '/nodes'),
    getItem('Pod管理', '/pods'),
    getItem('Deployment管理', '/deployments'),
    getItem('命名空间管理', '/namespaces'),
    getItem('服务管理', '/services'),
  ]),
])

const handleClick: MenuProps['onClick'] = (e) => {
  console.log('click', e)
  router.push(e.key.toString())
}

function goToAuditAlerts() {
  router.push('/audit-alerts')
}

function getAlertPriorityColor(priority: AuditPriority) {
  if (priority === 'Critical' || priority === 'Error') return 'red'
  if (priority === 'Warning') return 'orange'
  return 'blue'
}

function getAlertPriorityClass(priority: AuditPriority) {
  return `alert-${getAlertPriorityColor(priority)}`
}

function getAlertOutputField(alert: AuditAlert, field: string) {
  const value = alert.output_fields[field]
  if (Array.isArray(value)) return value.join(', ')
  return value ?? '-'
}

function getAlertNamespace(alert: AuditAlert) {
  return getAlertOutputField(alert, 'ka.target.namespace')
}

function getAlertResource(alert: AuditAlert) {
  return `${getAlertOutputField(alert, 'ka.target.resource')}/${getAlertOutputField(alert, 'ka.target.name')}`
}

onMounted(() => {
  selectedKeys.value = [route.path]
  // 处理嵌套路由的 openKeys
  updateOpenKeys(route.path)
  // 更新面包屑
  updateBreadcrumb(route.path)
})

watch(
  () => route.path,
  (newPath) => {
    selectedKeys.value = [newPath]
    // 处理嵌套路由的 openKeys
    updateOpenKeys(newPath)
    // 更新面包屑
    updateBreadcrumb(newPath)
  },
)

function updateOpenKeys(path: string) {
  // 根据当前路径更新 openKeys
  if (
    path === '/health' ||
    path.startsWith('/nodes') ||
    path.startsWith('/pods') ||
    path.startsWith('/deployments') ||
    path.startsWith('/namespaces') ||
    path.startsWith('/services')
  ) {
    openKeys.value = ['/health']
  } else {
    openKeys.value = []
  }
}

function updateBreadcrumb(path: string) {
  // 根据当前路径更新面包屑
  const items: Array<{ title: string; path?: string }> = []
  if (path === '/') items.push({ title: '首页', path: '/' })

  if (path.startsWith('/nodes')) {
    items.push({ title: '集群管理', path: '/' })
    items.push({ title: '节点管理', path: '/nodes' })

    // 处理节点详情页
    const match = path.match(/\/nodes\/(.*)/)
    if (match && match[1]) {
      items.push({ title: match[1] })
    }
  } else if (path.startsWith('/pods')) {
    items.push({ title: '集群管理', path: '/' })
    items.push({ title: 'Pod管理', path: '/pods' })

    // 处理Pod详情页
    const match = path.match(/\/pods\/([^/]+)\/(.*)/)
    if (match && match[2]) {
      items.push({ title: match[2] })
    }
  } else if (path.startsWith('/namespaces')) {
    items.push({ title: '集群管理', path: '/' })
    items.push({ title: '命名空间管理', path: '/namespaces' })
  } else if (path.startsWith('/deployments')) {
    items.push({ title: '集群管理', path: '/' })
    items.push({ title: 'Deployment管理', path: '/deployments' })

    const match = path.match(/\/deployments\/([^/]+)\/(.*)/)
    if (match && match[2]) {
      items.push({ title: match[2] })
    }
  } else if (path.startsWith('/services')) {
    items.push({ title: '集群管理', path: '/' })
    items.push({ title: '服务管理', path: '/services' })
  } else if (path === '/audit-alerts') {
    items.push({ title: '审计告警', path: '/audit-alerts' })
  } else if (path === '/health') {
    items.push({ title: '集群管理', path: '/health' })
  }

  breadcrumbItems.value = items
}
</script>

<style>
.ant-breadcrumb .ant-breadcrumb-separator {
  color: #fff !important;
}
</style>
<style scoped>
.basic-layout {
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: #001529;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.platform-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.breadcrumb {
  margin: 0;
}

.breadcrumb .ant-breadcrumb-link a {
  color: rgba(255, 255, 255, 0.85);
}

.breadcrumb .ant-breadcrumb-link a:hover {
  color: #fff;
}

:deep(.breadcrumb .ant-breadcrumb-separator) {
  color: #fff !important;
}

:deep(.ant-breadcrumb .ant-breadcrumb-separator) {
  color: #fff !important;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.create-button {
  border-color: rgba(255, 255, 255, 0.45);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.create-button:hover,
.create-button:focus {
  border-color: #69b1ff;
  background: #1677ff;
  color: #fff;
}

.alert-badge {
  line-height: 1;
}

:deep(.alert-badge .ant-badge-count) {
  box-shadow: none;
}

.alert-button {
  border-color: rgba(255, 255, 255, 0.45);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.alert-button:hover,
.alert-button:focus {
  border-color: #69b1ff;
  background: #1677ff;
  color: #fff;
}

.alert-dropdown {
  width: 360px;
  max-height: 420px;
  overflow-y: auto;
}

.alert-dropdown-item {
  padding: 10px 12px;
  border-left: 4px solid #1677ff;
  border-bottom: 1px solid #f0f0f0;
}

.alert-dropdown-item:last-child {
  border-bottom: 0;
}

.alert-red {
  border-left-color: #ff4d4f;
}

.alert-orange {
  border-left-color: #fa8c16;
}

.alert-blue {
  border-left-color: #1677ff;
}

.alert-dropdown-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
  font-weight: 600;
}

.alert-dropdown-title span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-dropdown-meta {
  margin-bottom: 6px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}

.alert-dropdown-output {
  display: -webkit-box;
  overflow: hidden;
  color: rgba(0, 0, 0, 0.65);
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.logo {
  height: 32px;
  margin: 16px;
  background: rgba(255, 255, 255, 0.3);
}
</style>
