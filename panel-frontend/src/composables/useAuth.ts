import { ref } from 'vue'
import { getDeviceCode, pollToken } from '@/api/authController'

const loading = ref(false)
const error = ref<string | null>(null)

export function useAuth() {
  const startDeviceFlow = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await getDeviceCode()
      if (response.data?.data) {
        return response.data.data
      } else {
        error.value = response.data?.msg || '获取认证信息失败'
        return null
      }
    } catch (err: unknown) {
      error.value = (err as Error).message || '网络错误'
      return null
    } finally {
      loading.value = false
    }
  }

  const pollForToken = async (_interval: number = 5) => {
    loading.value = true
    error.value = null
    try {
      const response = await pollToken()
      if (response.data?.data?.access_token) {
        return true
      } else if (response.data?.code === 0) {
        error.value = '认证失败或已过期'
        return false
      }
      return false
    } catch (err: unknown) {
      error.value = (err as Error).message || '网络错误'
      return false
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    error,
    startDeviceFlow,
    pollForToken,
  }
}
