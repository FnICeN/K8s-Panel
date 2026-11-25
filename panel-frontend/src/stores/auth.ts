import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getLoginStatus } from '@/api/authController'

declare global {
  interface Window {
    showLoginDialog?: () => void
  }
}

export const useAuthStore = defineStore('auth', () => {
  /** null 表示尚未查询过 */
  const isLoggedIn = ref<boolean | null>(null)

  /** 路由守卫可能在登录弹窗挂载前触发，此时先挂起，等布局挂载后再弹 */
  const loginDialogPending = ref(false)

  const refreshLoginStatus = async (): Promise<boolean> => {
    try {
      const response = await getLoginStatus()
      isLoggedIn.value = response.data.code === 0 && response.data.data === true
    } catch (error) {
      console.error('查询登录状态失败:', error)
      isLoggedIn.value = false
    }
    return isLoggedIn.value
  }

  const setLoggedIn = (value: boolean) => {
    isLoggedIn.value = value
  }

  const requestLoginDialog = () => {
    if (window.showLoginDialog) {
      window.showLoginDialog()
    } else {
      loginDialogPending.value = true
    }
  }

  const consumePendingLoginDialog = () => {
    if (loginDialogPending.value) {
      loginDialogPending.value = false
      window.showLoginDialog?.()
    }
  }

  return {
    isLoggedIn,
    refreshLoginStatus,
    setLoggedIn,
    requestLoginDialog,
    consumePendingLoginDialog,
  }
})
