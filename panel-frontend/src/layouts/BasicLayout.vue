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
        <a-button
          class="theme-button"
          :aria-label="colorMode === 'dark' ? '切换至浅色模式' : '切换至深色模式'"
          :aria-pressed="colorMode === 'dark'"
          @click="toggleTheme"
        >{{ colorMode === 'dark' ? '浅色模式' : '深色模式' }}</a-button>
        <a-tooltip title="创建资源">
          <a-button
            class="create-button"
            shape="circle"
            :icon="h(PlusOutlined)"
            @click="createResourceDialogRef?.open()"
          />
        </a-tooltip>
        <a-tooltip title="上传 YAML 部署">
          <a-button
            class="create-button"
            aria-label="上传 YAML 部署"
            shape="circle"
            :icon="h(UploadOutlined)"
            @click="uploadResourceDialogRef?.open()"
          />
        </a-tooltip>
        <a-popover placement="bottomRight" trigger="hover" overlay-class-name="alert-popover">
          <template #content>
            <div class="alert-dropdown">
              <a-empty
                v-if="auditStore.alerts.length === 0"
                :image="Empty.PRESENTED_IMAGE_SIMPLE"
                description="暂无审计告警"
              />
              <div
                v-for="alert in auditStore.alerts"
                :key="alert.uuid"
                class="alert-dropdown-item"
                :class="getAlertPriorityClass(alert.priority)"
              >
                <div class="alert-dropdown-title">
                  <span>{{ alert.ruleCn || alert.rule }}</span>
                  <a-tag :color="getPriorityColor(alert.priority)">
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
          <a-badge :count="auditStore.count" class="alert-badge">
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
        <a-button
          v-if="isLoggedIn === true"
          class="logout-button"
          type="primary"
          danger
          :loading="loggingOut"
          @click="handleLogout"
        >
          退出
        </a-button>
        <a-button v-else class="login-button" type="primary" @click="loginDialogRef?.open()">登录</a-button>
      </div>
    </a-layout-header>
    <a-layout>
      <a-layout-sider class="sider" width="310">
        <div class="logo" />
        <a-menu
          id="main-menu"
          v-model:openKeys="openKeys"
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          :theme="colorMode"
          :items="items"
          class="sider-menu"
          @click="handleClick"
        ></a-menu>
      </a-layout-sider>
      <a-layout>
        <a-layout-content
          class="main-content"
          :class="{ 'home-content': route.path === '/', 'cluster-content': isClusterPage }"
        >
          <RouterView />
        </a-layout-content>
      </a-layout>
    </a-layout>
    <LoginDialog ref="loginDialogRef" />
    <CreateResourceDialog ref="createResourceDialogRef" />
    <UploadResourceDialog ref="uploadResourceDialogRef" />
    <a-modal
      v-model:open="logoutConfirmOpen"
      title="确认退出"
      ok-text="退出"
      ok-type="danger"
      cancel-text="取消"
      :confirm-loading="loggingOut"
      @ok="doLogout"
    >
      退出后需要重新登录才能继续操作，确定退出吗？
    </a-modal>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, h, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter, useRoute } from 'vue-router'
import {
  HomeOutlined,
  CheckCircleOutlined,
  PlusOutlined,
  UploadOutlined,
  BellOutlined,
  DatabaseOutlined,
  DashboardOutlined,
  FundOutlined,
} from '@ant-design/icons-vue'
import { message, Empty, type MenuProps, type ItemType } from 'ant-design-vue'
import LoginDialog from '@/components/LoginDialog.vue'
import CreateResourceDialog from '@/components/CreateResourceDialog.vue'
import UploadResourceDialog from '@/components/UploadResourceDialog.vue'
import type { AuditAlert, AuditPriority } from '@/api/auditController'
import { getOutputField, getPriorityColor } from '@/utils/audit'
import { getNodeDisplayName } from '@/utils/nodeDisplayName'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'
import { logout } from '@/api/authController'
import { useTheme } from '@/composables/useTheme'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const auditStore = useAuditStore()
const { isLoggedIn } = storeToRefs(authStore)
const { colorMode, toggleTheme } = useTheme()
const loginDialogRef = ref<InstanceType<typeof LoginDialog> | null>(null)
const createResourceDialogRef = ref<InstanceType<typeof CreateResourceDialog> | null>(null)
const uploadResourceDialogRef = ref<InstanceType<typeof UploadResourceDialog> | null>(null)

// 全局登录对话框触发函数
if (typeof window !== 'undefined') {
  ;(window as any).showLoginDialog = () => {
    loginDialogRef.value?.show()
  }
}

const clusterPrefixes = ['/health', '/nodes', '/pods', '/deployments', '/namespaces', '/services']

const isClusterPage = computed(() =>
  clusterPrefixes.some((prefix) => route.path === prefix || route.path.startsWith(`${prefix}/`)),
)

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
  getItem('天基云网管理', '/health', () => h(CheckCircleOutlined), [
    getItem('星载计算节点管理（Node）', '/nodes'),
    getItem('计算单元（Pod）', '/pods'),
    getItem('计算单元部署控制器（Deployment）', '/deployments'),
    getItem('命名空间管理（Namespace）', '/namespaces'),
    getItem('服务发现（Service）', '/services'),
  ]),
  getItem('备份管理', '/backup', () => h(DatabaseOutlined)),
  getItem('云网监控仪表盘', '/dashboard', () => h(FundOutlined)),
  getItem('监控告警管理', '/monitor', () => h(DashboardOutlined)),
])

const loggingOut = ref(false)
const logoutConfirmOpen = ref(false)

const doLogout = async () => {
  loggingOut.value = true
  try {
    const response = await logout()
    // 无论后端是否仍持有会话，前端都切回未登录态
    authStore.setLoggedIn(false)
    if (response.data.code === 0) {
      message.success('已退出登录')
    }
  } catch (error) {
    console.error('退出登录失败:', error)
    authStore.setLoggedIn(false)
    message.error('退出登录失败')
  } finally {
    loggingOut.value = false
    logoutConfirmOpen.value = false
  }
}

const handleLogout = () => {
  logoutConfirmOpen.value = true
}

const handleClick: MenuProps['onClick'] = (e) => {
  router.push(e.key.toString())
}

function goToAuditAlerts() {
  router.push('/audit-alerts')
}

function getAlertPriorityClass(priority: AuditPriority) {
  return `alert-${getPriorityColor(priority)}`
}

function getAlertNamespace(alert: AuditAlert) {
  return alert.namespace || getOutputField(alert.outputFields, 'ka.target.namespace')
}

function getAlertResource(alert: AuditAlert) {
  return `${getOutputField(alert.outputFields, 'ka.target.resource')}/${getOutputField(alert.outputFields, 'ka.target.name')}`
}

onMounted(() => {
  selectedKeys.value = [route.path]
  // 处理嵌套路由的 openKeys
  updateOpenKeys(route.path)
  // 更新面包屑
  updateBreadcrumb(route.path)
  // 首次进入时路由守卫早于弹窗挂载，此处补弹登录对话框
  authStore.consumePendingLoginDialog()
  // 加载审计告警并建立实时推送
  auditStore.init()
  auditStore.connect()
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
    items.push({ title: '天基云网管理', path: '/' })
    items.push({ title: '星载计算节点管理（Node）', path: '/nodes' })

    // 处理节点详情页
    const match = path.match(/\/nodes\/(.*)/)
    if (match && match[1]) {
      items.push({ title: getNodeDisplayName(decodeURIComponent(match[1])) })
    }
  } else if (path.startsWith('/pods')) {
    items.push({ title: '天基云网管理', path: '/' })
    items.push({ title: '计算单元（Pod）', path: '/pods' })

    // 处理Pod详情页
    const match = path.match(/\/pods\/([^/]+)\/(.*)/)
    if (match && match[2]) {
      items.push({ title: match[2] })
    }
  } else if (path.startsWith('/namespaces')) {
    items.push({ title: '天基云网管理', path: '/' })
    items.push({ title: '命名空间管理（Namespace）', path: '/namespaces' })
  } else if (path.startsWith('/deployments')) {
    items.push({ title: '天基云网管理', path: '/' })
    items.push({ title: '计算单元部署控制器（Deployment）', path: '/deployments' })

    const match = path.match(/\/deployments\/([^/]+)\/(.*)/)
    if (match && match[2]) {
      items.push({ title: match[2] })
    }
  } else if (path.startsWith('/services')) {
    items.push({ title: '天基云网管理', path: '/' })
    items.push({ title: '服务发现（Service）', path: '/services' })
  } else if (path === '/audit-alerts') {
    items.push({ title: '审计告警', path: '/audit-alerts' })
  } else if (path === '/backup') {
    items.push({ title: '备份管理', path: '/backup' })
  } else if (path === '/dashboard') {
    items.push({ title: '云网监控仪表盘', path: '/dashboard' })
  } else if (path === '/monitor') {
    items.push({ title: '监控告警管理', path: '/monitor' })
  } else if (path === '/health') {
    items.push({ title: '天基云网管理', path: '/health' })
  }

  breadcrumbItems.value = items
}
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: var(--app-bg);
}

.main-content {
  min-height: 280px;
  margin: 24px 16px;
  padding: 24px;
  background: var(--app-surface);
  border: 1px solid var(--app-border-soft);
  border-radius: 4px;
}

.main-content.home-content {
  min-width: 0;
  margin: 0;
  padding: 0;
  background: #071422;
  border: 0;
  border-radius: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  line-height: normal;
  border-bottom: 1px solid var(--app-border);
  background: var(--app-surface);
  color: var(--app-text);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.platform-name {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.breadcrumb {
  margin: 0;
  font-size: 14px;
}

.breadcrumb :deep(.ant-breadcrumb-link a) {
  color: var(--app-text-secondary);
}

.breadcrumb :deep(.ant-breadcrumb-link a:hover) {
  color: var(--app-accent);
}

:deep(.breadcrumb .ant-breadcrumb-separator) {
  color: var(--app-text-muted);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  line-height: 1;
}

.header :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  line-height: 1;
  font-size: 14px;
  vertical-align: middle;
}

.header :deep(.ant-btn .anticon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.header :deep(.ant-btn > span) {
  line-height: 1;
}

.create-button,
.alert-button {
  color: var(--app-text-secondary);
}

.alert-badge {
  line-height: 1;
}

:deep(.alert-badge .ant-badge-count) {
  box-shadow: none;
}

.alert-dropdown {
  width: 360px;
  max-height: 420px;
  overflow-y: auto;
}

.alert-dropdown-item {
  padding: 10px 12px;
  border-left: 4px solid #409eff;
  border-bottom: 1px solid var(--app-border-soft);
}

.alert-dropdown-item:last-child {
  border-bottom: 0;
}

.alert-red {
  border-left-color: #f56c6c;
}

.alert-orange {
  border-left-color: #e6a23c;
}

.alert-blue {
  border-left-color: #409eff;
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
  color: var(--app-text-muted);
  font-size: 12px;
}

.alert-dropdown-output {
  display: -webkit-box;
  overflow: hidden;
  color: var(--app-text-secondary);
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.sider {
  border-right: 1px solid var(--app-border);
  background: var(--app-surface);
}

.sider :deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sider-menu {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border-right: 0;
  background: transparent;
  color: var(--app-text-secondary);
}

.sider :deep(.ant-menu) {
  background: var(--app-surface);
  color: var(--app-text-secondary);
}

.sider :deep(.ant-menu-item),
.sider :deep(.ant-menu-submenu-title) {
  height: auto;
  min-height: 48px;
  padding-top: 12px;
  padding-bottom: 12px;
  color: var(--app-text-secondary);
  font-size: 14px;
  line-height: 1.45;
  white-space: normal;
}

.sider :deep(.ant-menu-title-content) {
  line-height: 1.45;
  white-space: normal;
}

.sider :deep(.ant-menu-item .anticon),
.sider :deep(.ant-menu-submenu-title .anticon) {
  font-size: 16px;
}

.sider :deep(.ant-menu-item:hover),
.sider :deep(.ant-menu-submenu-title:hover) {
  background: var(--app-accent-soft) !important;
  color: var(--app-accent);
}

.sider :deep(.ant-menu-item-selected) {
  background: var(--app-accent-soft) !important;
  box-shadow: inset 3px 0 0 var(--app-accent);
  color: var(--app-accent);
}

.sider :deep(.ant-menu-sub.ant-menu-inline) {
  background: var(--app-surface-muted) !important;
}

.logo {
  height: 2px;
  margin: 16px 18px 8px;
  border: 0;
  border-radius: 2px;
  background: var(--app-border);
}
</style>
