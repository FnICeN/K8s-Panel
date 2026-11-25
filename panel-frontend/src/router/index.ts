import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: BasicLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('../pages/HomePage.vue'),
        },
        {
          path: 'health',
          name: 'health',
          component: () => import('../pages/Health.vue'),
        },
        {
          path: 'audit-alerts',
          name: 'auditAlerts',
          component: () => import('../pages/AuditAlerts.vue'),
        },
        {
          path: 'nodes',
          name: 'nodes',
          component: () => import('../pages/cluster/NodesManager.vue'),
        },
        {
          path: 'nodes/:name',
          name: 'nodeSpec',
          component: () => import('../pages/cluster/NodeSpec.vue'),
        },
        {
          path: 'pods',
          name: 'pods',
          component: () => import('../pages/cluster/PodManager.vue'),
        },
        {
          path: 'pods/:namespace/:name',
          name: 'podSpec',
          component: () => import('../pages/cluster/PodSpec.vue'),
        },
        {
          path: 'services',
          name: 'services',
          component: () => import('../pages/cluster/ServiceManager.vue'),
        },
        {
          path: 'services/:namespace/:name',
          name: 'serviceSpec',
          component: () => import('../pages/cluster/ServiceSpec.vue'),
        },
        {
          path: 'deployments',
          name: 'deployments',
          component: () => import('../pages/cluster/DeploymentManager.vue'),
        },
        {
          path: 'deployments/:namespace/:name',
          name: 'deploymentSpec',
          component: () => import('../pages/cluster/DeploymentSpec.vue'),
        },
        {
          path: 'namespaces',
          name: 'namespaces',
          component: () => import('../pages/cluster/NamespaceManager.vue'),
        },
        {
          path: 'backup',
          name: 'backup',
          component: () => import('../pages/BackupConsole.vue'),
        },
        {
          path: 'monitor',
          name: 'monitor',
          component: () => import('../pages/MonitorConsole.vue'),
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../pages/DashboardConsole.vue'),
        },
      ],
    },
  ],
})

// 路由级登录守卫：未登录时放行导航（页面自身会做空态处理），但统一弹出登录对话框
router.beforeEach(async () => {
  const authStore = useAuthStore()
  const loggedIn = await authStore.refreshLoginStatus()
  if (!loggedIn) {
    authStore.requestLoginDialog()
  }
  return true
})

export default router
