<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { startDeviceFlow, pollForToken, loading } = useAuth()

const visible = ref(false)
const polling = ref(false)
const deviceCodeData = ref<API.DeviceCodeResponse | null>(null)
const error = ref<string | null>(null)
const pollingIntervalId = ref<number | null>(null)

const emit = defineEmits<{
  (e: 'loginSuccess'): void
}>()

watch(visible, (val) => {
  if (!val) {
    resetState()
  }
})

onMounted(() => {
  checkLoginStatus()
})

const checkLoginStatus = () => {
  if (window.location.search.includes('code=')) {
    visible.value = true
  }
}

const resetState = () => {
  deviceCodeData.value = null
  error.value = null
  polling.value = false
  if (pollingIntervalId.value) {
    clearInterval(pollingIntervalId.value)
    pollingIntervalId.value = null
  }
}

const handleOpenLogin = () => {
  visible.value = true
  handleGetDeviceCode()
}

const handleGetDeviceCode = async () => {
  error.value = null
  const data = await startDeviceFlow()

  if (data) {
    deviceCodeData.value = data
  } else {
    error.value = '获取认证信息失败，请重试'
  }
}

const handleOpenAuthPage = () => {
  if (deviceCodeData.value?.verification_uri_complete) {
    window.open(deviceCodeData.value.verification_uri_complete, '_blank')
  } else if (deviceCodeData.value?.verification_uri) {
    window.open(deviceCodeData.value.verification_uri, '_blank')
  }
}

const handlePollToken = async () => {
  if (!deviceCodeData.value?.interval) {
    error.value = '缺少轮询间隔信息'
    return
  }

  polling.value = true
  error.value = null

  const intervalMs = deviceCodeData.value.interval * 1000

  pollingIntervalId.value = window.setInterval(async () => {
    const success = await pollForToken(deviceCodeData.value?.interval || 5)
    if (success) {
      if (pollingIntervalId.value) {
        clearInterval(pollingIntervalId.value)
        pollingIntervalId.value = null
      }
      polling.value = false
      visible.value = false
      message.success('登录成功')
      emit('loginSuccess')
      router.push('/')
    }
  }, intervalMs)

  const firstPoll = await pollForToken(deviceCodeData.value.interval)
  if (firstPoll) {
    if (pollingIntervalId.value) {
      clearInterval(pollingIntervalId.value)
      pollingIntervalId.value = null
    }
    polling.value = false
    visible.value = false
    message.success('登录成功')
    emit('loginSuccess')
    router.push('/')
  }
}

const handleCancel = () => {
  visible.value = false
}

defineExpose({
  open: handleOpenLogin,
  show: () => {
    visible.value = true
    handleGetDeviceCode()
  },
})
</script>

<template>
  <a-modal
    v-model:open="visible"
    title="OIDC登录"
    :footer="null"
    :maskClosable="false"
    width="450px"
    @cancel="handleCancel"
  >
    <div class="login-content">
      <a-spin :spinning="loading">
        <div v-if="deviceCodeData" class="device-code-section">
          <a-alert v-if="error" :message="error" type="error" show-icon closable class="mb-16" />
          <div class="info-item">
            <span class="label">用户代码：</span>
            <span class="value user-code">{{ deviceCodeData.user_code }}</span>
          </div>
          <div class="info-item">
            <span class="label">认证地址：</span>
            <a :href="deviceCodeData.verification_uri" target="_blank" class="auth-link">
              {{ deviceCodeData.verification_uri }}
            </a>
          </div>
          <div class="button-group">
            <a-button type="primary" block @click="handleOpenAuthPage"> 前往认证 </a-button>
            <a-button
              type="default"
              block
              :loading="polling"
              :disabled="polling"
              @click="handlePollToken"
            >
              {{ polling ? '认证中...' : '获取Token' }}
            </a-button>
          </div>
          <div class="tips">
            <p>1. 点击"前往认证"按钮访问Keycloak认证页面</p>
            <p>2. 在认证页面输入用户代码完成认证</p>
            <p>3. 认证完成后，点击"获取Token"按钮完成登录</p>
          </div>
        </div>
        <div v-else-if="error" class="error-section">
          <a-alert :message="error" type="error" show-icon closable class="mb-16" />
          <a-button type="primary" block @click="handleGetDeviceCode"> 重试 </a-button>
        </div>
        <div v-else class="loading-section">
          <p>正在获取认证信息...</p>
        </div>
      </a-spin>
    </div>
  </a-modal>
</template>

<style scoped>
.login-content {
  min-height: 200px;
}

.device-code-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mb-16 {
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.label {
  font-weight: 500;
  color: #666;
  flex-shrink: 0;
}

.value {
  word-break: break-all;
}

.user-code {
  font-size: 18px;
  font-weight: bold;
  color: #1890ff;
  letter-spacing: 2px;
}

.auth-link {
  word-break: break-all;
  color: #1890ff;
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
}

.tips {
  margin-top: 16px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

.tips p {
  margin: 4px 0;
}

.error-section {
  text-align: center;
  padding: 20px 0;
}

.loading-section {
  text-align: center;
  padding: 60px 0;
  color: #999;
}
</style>
