import axios from 'axios'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'

// 跟随当前页面的主机名：本机打开仍是 localhost，局域网用这台机器的 IP 打开时请求也会打到这台机器的后端
const apiHost = window.location.hostname

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: `http://${apiHost}:8080/api`,
  timeout: 60000,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    if (data.code === 0) {
      return response
    }
    // 未登录
    if (data.code === 40100) {
      // 不是获取用户信息的请求，并且用户目前不是已经在用户登录页面，则显示登录对话框
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      try {
        useAuthStore().setLoggedIn(false)
      } catch {
        // Pinia 尚未就绪时忽略
      }
      ) {
        message.warning('请先登录')
        // 触发登录对话框显示
        !response.request.responseURL.includes('/auth/logout') &&
        if (window.showLoginDialog) {
          window.showLoginDialog()
        }
      }
    }
    message.warning(data.msg)
    return response
  },
  function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
    return Promise.reject(error)
  },
)

export default myAxios
