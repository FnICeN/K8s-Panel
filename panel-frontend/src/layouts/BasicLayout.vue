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
          theme="dark"
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
          <a-config-provider :theme="clusterTheme">
            <RouterView />
          </a-config-provider>
        </a-layout-content>
      </a-layout>
    </a-layout>
    <LoginDialog ref="loginDialogRef" />
    <CreateResourceDialog ref="createResourceDialogRef" />
    <UploadResourceDialog ref="uploadResourceDialogRef" />
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
import { message, Modal, Empty, type MenuProps, type ItemType } from 'ant-design-vue'
import LoginDialog from '@/components/LoginDialog.vue'
import CreateResourceDialog from '@/components/CreateResourceDialog.vue'
import UploadResourceDialog from '@/components/UploadResourceDialog.vue'
import type { AuditAlert, AuditPriority } from '@/api/auditController'
import { getOutputField, getPriorityColor } from '@/utils/audit'
import { getNodeDisplayName } from '@/utils/nodeDisplayName'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'
import { logout } from '@/api/authController'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const auditStore = useAuditStore()
const { isLoggedIn } = storeToRefs(authStore)
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

const clusterTheme = computed(() =>
  isClusterPage.value ? { token: { fontSize: 17 } } : undefined,
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
  }
}

const handleLogout = () => {
  Modal.confirm({
    title: '确认退出',
    content: '退出后需要重新登录才能继续操作，确定退出吗？',
    okText: '退出',
    okType: 'danger',
    cancelText: '取消',
    onOk: () => doLogout(),
  })
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

<style>
.ant-breadcrumb .ant-breadcrumb-separator {
  color: #fff !important;
}
</style>
<style scoped>
.basic-layout {
  min-height: 100vh;
}

.main-content {
  min-height: 280px;
  margin: 24px 16px;
  padding: 24px;
  background: #fff;
}

.main-content.home-content {
  min-width: 0;
  margin: 0;
  padding: 0;
  background: #071422;
}

.main-content.cluster-content {
  font-size: 17px;
}

.main-content.cluster-content :deep(.ant-table),
.main-content.cluster-content :deep(.ant-pagination),
.main-content.cluster-content :deep(.ant-tag),
.main-content.cluster-content :deep(.ant-btn),
.main-content.cluster-content :deep(.ant-select),
.main-content.cluster-content :deep(.ant-input),
.main-content.cluster-content :deep(.ant-tabs-tab),
.main-content.cluster-content :deep(.ant-card-body),
.main-content.cluster-content :deep(.ant-empty) {
  font-size: 17px;
}

.main-content.cluster-content :deep(.ant-card-head-title) {
  font-size: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid rgba(98, 196, 230, 0.16);
  background: linear-gradient(180deg, #10283f 0%, #0b1d31 100%);
  color: #e9f6ff;
  box-shadow: 0 1px 0 rgba(120, 210, 255, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.platform-name {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
}

.breadcrumb {
  margin: 0;
  font-size: 19px;
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
  font-size: 19px;
}

.header :deep(.ant-btn) {
  font-size: 19px;
}

.logout-button {
  background: #ff4d4f;
  border-color: #ff4d4f;
}

.logout-button:hover,
.logout-button:focus {
  background: #ff7875;
  border-color: #ff7875;
}

.create-button {
  border-color: rgba(126, 214, 242, 0.32);
  background: rgba(10, 36, 58, 0.55);
  color: #dff6ff;
}

.create-button:hover,
.create-button:focus {
  border-color: rgba(126, 214, 242, 0.7);
  background: rgba(22, 64, 96, 0.85);
  color: #fff;
}

.alert-badge {
  line-height: 1;
}

:deep(.alert-badge .ant-badge-count) {
  box-shadow: none;
}

.alert-button {
  border-color: rgba(126, 214, 242, 0.32);
  background: rgba(10, 36, 58, 0.55);
  color: #dff6ff;
}

.alert-button:hover,
.alert-button:focus {
  border-color: rgba(126, 214, 242, 0.7);
  background: rgba(22, 64, 96, 0.85);
  color: #fff;
}

.header :deep(.login-button.ant-btn-primary) {
  border-color: rgba(126, 214, 242, 0.45);
  background: rgba(18, 58, 88, 0.85);
  box-shadow: none;
  color: #e9f7ff;
}

.header :deep(.login-button.ant-btn-primary:hover),
.header :deep(.login-button.ant-btn-primary:focus) {
  border-color: rgba(150, 226, 255, 0.75);
  background: rgba(28, 82, 120, 0.95);
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

.sider {
  border-right: 1px solid rgba(98, 196, 230, 0.12);
  background: linear-gradient(180deg, #0c2238 0%, #071422 100%);
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
  color: rgba(214, 236, 248, 0.82);
}

.sider :deep(.ant-menu) {
  background: transparent;
  color: rgba(214, 236, 248, 0.82);
}

.sider :deep(.ant-menu-item),
.sider :deep(.ant-menu-submenu-title) {
  height: auto;
  min-height: 54px;
  padding-top: 14px;
  padding-bottom: 14px;
  color: rgba(214, 236, 248, 0.78);
  font-size: 19px;
  line-height: 1.45;
  white-space: normal;
}

.sider :deep(.ant-menu-title-content) {
  line-height: 1.45;
  white-space: normal;
}

.sider :deep(.ant-menu-item .anticon),
.sider :deep(.ant-menu-submenu-title .anticon) {
  font-size: 21px;
}

.sider :deep(.ant-menu-item:hover),
.sider :deep(.ant-menu-submenu-title:hover) {
  background: rgba(90, 190, 235, 0.16) !important;
  color: #fff;
}

.sider :deep(.ant-menu-item-selected) {
  background: rgba(72, 186, 230, 0.16) !important;
  box-shadow: inset 3px 0 0 #67d6ff;
  color: #fff;
}

.sider :deep(.ant-menu-sub.ant-menu-inline) {
  background: rgba(6, 24, 46, 0.45) !important;
}

.logo {
  height: 2px;
  margin: 16px 18px 8px;
  border: 0;
  border-radius: 2px;
  background: linear-gradient(90deg, transparent, rgba(110, 214, 255, 0.72), transparent);
}
</style>
