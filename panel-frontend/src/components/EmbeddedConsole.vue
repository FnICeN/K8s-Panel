<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ExportOutlined, LockOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  /** 内嵌控制台的地址 */
  url: string
  /** 工具栏与 iframe 标题 */
  title: string
  /** 未登录提示中的功能名称，如“备份管理” */
  featureName: string
}>()

// 外部控制台可能引用不可达资源，iframe load 事件会延迟很久，
// 因此加载提示最多显示几秒，不阻塞页面展示
const LOADING_HINT_TIMEOUT_MS = 5000

// 登录状态由路由守卫在进入页面前刷新，这里直接消费 store 状态
const authStore = useAuthStore()
const authState = computed<'checking' | 'authorized' | 'unauthorized'>(() => {
  if (authStore.isLoggedIn === null) return 'checking'
  return authStore.isLoggedIn ? 'authorized' : 'unauthorized'
})

const loading = ref(false)
const iframeRef = ref<HTMLIFrameElement | null>(null)
let loadingTimer: ReturnType<typeof setTimeout> | undefined

const checkLogin = () => {
  authStore.refreshLoginStatus()
}

const openLoginDialog = () => {
  authStore.requestLoginDialog()
}

const startLoadingHint = () => {
  loading.value = true
  clearTimeout(loadingTimer)
  loadingTimer = setTimeout(() => {
    loading.value = false
  }, LOADING_HINT_TIMEOUT_MS)
}

watch(
  authState,
  (state) => {
    if (state === 'authorized') {
      startLoadingHint()
    }
  },
  { immediate: true },
)

const handleLoad = () => {
  clearTimeout(loadingTimer)
  loading.value = false
}

const handleRefresh = () => {
  if (iframeRef.value) {
    startLoadingHint()
    // 重设 src 以强制刷新 iframe 内容
    iframeRef.value.src = props.url
  }
}

onBeforeUnmount(() => {
  clearTimeout(loadingTimer)
})
</script>

<template>
  <div class="embedded-console">
    <template v-if="authState === 'authorized'">
      <div class="toolbar">
        <span class="title">{{ title }}</span>
        <div class="actions">
          <a-tooltip title="刷新">
            <a-button size="small" @click="handleRefresh">
              <template #icon><ReloadOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="在新标签页打开">
            <a-button size="small" :href="url" target="_blank">
              <template #icon><ExportOutlined /></template>
            </a-button>
          </a-tooltip>
        </div>
      </div>
      <a-spin :spinning="loading" wrapper-class-name="iframe-spin">
        <iframe
          ref="iframeRef"
          class="console-frame"
          :src="url"
          :title="title"
          @load="handleLoad"
        ></iframe>
      </a-spin>
    </template>

    <div v-else-if="authState === 'checking'" class="state-panel">
      <a-spin tip="正在校验登录状态..." />
    </div>

    <div v-else class="state-panel">
      <LockOutlined class="lock-icon" />
      <p class="state-title">需要先登录安全集控平台</p>
      <p class="state-desc">{{ featureName }}功能仅对已登录用户开放，请先完成 OIDC 登录。</p>
      <div class="state-actions">
        <a-button type="primary" @click="openLoginDialog">立即登录</a-button>
        <a-button @click="checkLogin">我已登录，重新检查</a-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.embedded-console {
  display: flex;
  flex-direction: column;
  /* 100vh - 顶栏64px - 内容区外边距48px - 内容区内边距48px */
  height: calc(100vh - 64px - 48px - 48px);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 8px;
}

.iframe-spin {
  flex: 1;
  min-height: 0;
}

.iframe-spin :deep(.ant-spin-container) {
  height: 100%;
}

.console-frame {
  width: 100%;
  height: 100%;
  border: 1px solid var(--app-border-soft);
  border-radius: 6px;
  background: var(--app-surface);
}

.state-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.lock-icon {
  margin-bottom: 8px;
  color: var(--app-text-muted);
  font-size: 48px;
}

.state-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.state-desc {
  margin: 0;
  color: var(--app-text-muted);
}

.state-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}
</style>
