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
  </a-layout>
</template>

<script setup lang="ts">
import { reactive, ref, watch, h, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { HomeOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import type { MenuProps, ItemType } from 'ant-design-vue'
import LoginDialog from '@/components/LoginDialog.vue'

const router = useRouter()
const route = useRoute()
const loginDialogRef = ref<InstanceType<typeof LoginDialog> | null>(null)

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
  getItem('集群管理', '/health', () => h(CheckCircleOutlined), [
    getItem('节点管理', '/nodes'),
    getItem('Pod管理', '/pods'),
    getItem('命名空间管理', '/namespaces'),
    getItem('服务管理', '/services'),
  ]),
])

const handleClick: MenuProps['onClick'] = (e) => {
  console.log('click', e)
  router.push(e.key.toString())
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
  } else if (path.startsWith('/services')) {
    items.push({ title: '集群管理', path: '/' })
    items.push({ title: '服务管理', path: '/services' })
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
}

.logo {
  height: 32px;
  margin: 16px;
  background: rgba(255, 255, 255, 0.3);
}
</style>
